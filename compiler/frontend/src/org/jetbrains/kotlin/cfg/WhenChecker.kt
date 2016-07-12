/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cfg

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.checkReservedPrefixWord
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isFlexible
import java.util.*

interface WhenMissingCase {

    val branchConditionText: String
}

// Always must be first in the list
private object UnknownMissingCase : WhenMissingCase {
    override fun toString() = "unknown"

    override val branchConditionText = "else"
}

val List<WhenMissingCase>.hasUnknown: Boolean
    get() = firstOrNull() == UnknownMissingCase

private interface WhenExhaustivenessChecker {
    fun getMissingCases(
            expression: KtWhenExpression,
            context: BindingContext,
            subjectDescriptor: ClassDescriptor?,
            nullable: Boolean
    ): List<WhenMissingCase>

    fun isApplicable(subjectType: KotlinType): Boolean = false
}

private object NullMissingCase : WhenMissingCase {
    override fun toString() = branchConditionText

    override val branchConditionText = "null"
}

// It's not a regular exhaustiveness checker, invoke it only inside other checkers
private object WhenOnNullableExhaustivenessChecker /* : WhenExhaustivenessChecker*/ {
    fun getMissingCases(expression: KtWhenExpression, context: BindingContext, nullable: Boolean) =
            if (nullable) getNullCaseIfMissing(expression, context) else listOf()

    private fun getNullCaseIfMissing(expression: KtWhenExpression, context: BindingContext): List<WhenMissingCase> {
        for (entry in expression.entries) {
            for (condition in entry.conditions) {
                if (condition is KtWhenConditionWithExpression) {
                    condition.expression?.let {
                        val type = context.getType(it)
                        if (type != null && KotlinBuiltIns.isNullableNothing(type)) {
                            return listOf()
                        }
                    }
                }
            }
        }
        return listOf(NullMissingCase)
    }
}

private class BooleanMissingCase(val b: Boolean) : WhenMissingCase {
    override fun toString() = branchConditionText

    override val branchConditionText = b.toString()
}

private object WhenOnBooleanExhaustivenessChecker : WhenExhaustivenessChecker {
    override fun getMissingCases(
            expression: KtWhenExpression,
            context: BindingContext,
            subjectDescriptor: ClassDescriptor?,
            nullable: Boolean
    ): List<WhenMissingCase> {
        // It's assumed (and not checked) that expression is of the boolean type
        var containsFalse = false
        var containsTrue = false
        for (whenEntry in expression.entries) {
            for (whenCondition in whenEntry.conditions) {
                if (whenCondition is KtWhenConditionWithExpression) {
                    val whenExpression = whenCondition.expression
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, context, true)) containsTrue = true
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, context, false)) containsFalse = true
                }
            }
        }
        return (if (!containsTrue) listOf(BooleanMissingCase(true)) else listOf()) +
               (if (!containsFalse) listOf(BooleanMissingCase(false)) else listOf()) +
               WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable)
    }

    override fun isApplicable(subjectType: KotlinType): Boolean {
        return KotlinBuiltIns.isBoolean(TypeUtils.makeNotNullable(subjectType))
    }
}

private class ClassMissingCase(val descriptor: ClassDescriptor): WhenMissingCase {
    override fun toString() = descriptor.name.identifier.let { if (descriptor.kind.isSingleton) it else "is $it" }

    override val branchConditionText = DescriptorUtils.getFqNameFromTopLevelClass(descriptor).asString().let {
        if (descriptor.kind.isSingleton) it else "is $it"
    }
}

internal abstract class WhenOnClassExhaustivenessChecker : WhenExhaustivenessChecker {
    private fun getReference(expression: KtExpression?): KtSimpleNameExpression? =
            when (expression) {
                is KtSimpleNameExpression -> expression
                is KtQualifiedExpression -> getReference(expression.selectorExpression)
                else -> null
            }

    protected fun getMissingClassCases(
            whenExpression: KtWhenExpression,
            memberDescriptors: Set<ClassDescriptor>,
            context: BindingContext
    ): List<WhenMissingCase> {
        // when on empty enum / sealed is considered non-exhaustive, see test whenOnEmptySealed
        if (memberDescriptors.isEmpty()) return listOf(UnknownMissingCase)
        val checkedDescriptors = LinkedHashSet<ClassDescriptor>()
        for (whenEntry in whenExpression.entries) {
            for (condition in whenEntry.conditions) {
                var negated = false
                var checkedDescriptor: ClassDescriptor? = null
                if (condition is KtWhenConditionIsPattern) {
                    val checkedType = context.get(BindingContext.TYPE, condition.typeReference)
                    if (checkedType != null) {
                        checkedDescriptor = TypeUtils.getClassDescriptor(checkedType)
                    }
                    negated = condition.isNegated
                }
                else if (condition is KtWhenConditionWithExpression) {
                    if (condition.expression != null) {
                        val reference = getReference(condition.expression)
                        if (reference != null) {
                            val target = context.get(BindingContext.REFERENCE_TARGET, reference)
                            if (target is ClassDescriptor) {
                                checkedDescriptor = target
                            }
                        }
                    }
                }

                // Checks are important only for nested subclasses of the sealed class
                // In additional, check without "is" is important only for objects
                if (checkedDescriptor == null ||
                    !memberDescriptors.contains(checkedDescriptor) ||
                    (condition is KtWhenConditionWithExpression &&
                     !DescriptorUtils.isObject(checkedDescriptor) &&
                     !DescriptorUtils.isEnumEntry(checkedDescriptor))) {
                    continue
                }
                if (negated) {
                    if (checkedDescriptors.contains(checkedDescriptor)) return listOf() // all members are already there
                    checkedDescriptors.addAll(memberDescriptors)
                    checkedDescriptors.remove(checkedDescriptor)
                }
                else {
                    checkedDescriptors.add(checkedDescriptor)
                }
            }
        }
        return (memberDescriptors - checkedDescriptors).toList().map { ClassMissingCase(it) }
    }
}

private object WhenOnEnumExhaustivenessChecker : WhenOnClassExhaustivenessChecker() {
    override fun getMissingCases(
            expression: KtWhenExpression,
            context: BindingContext,
            subjectDescriptor: ClassDescriptor?,
            nullable: Boolean
    ): List<WhenMissingCase> {
        assert(isEnumClass(subjectDescriptor)) { "isWhenOnEnumExhaustive should be called with an enum class descriptor" }
        val entryDescriptors =
                DescriptorUtils.getAllDescriptors(subjectDescriptor!!.unsubstitutedInnerClassesScope)
                        .filter { isEnumEntry(it) }
                        .filterIsInstance<ClassDescriptor>()
                        .toSet()
        return getMissingClassCases(expression, entryDescriptors, context) +
               WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable)
    }

    override fun isApplicable(subjectType: KotlinType): Boolean {
        return WhenChecker.getClassDescriptorOfTypeIfEnum(subjectType) != null
    }
}

internal object WhenOnSealedExhaustivenessChecker : WhenOnClassExhaustivenessChecker() {
    override fun getMissingCases(
            expression: KtWhenExpression,
            context: BindingContext,
            subjectDescriptor: ClassDescriptor?,
            nullable: Boolean
    ): List<WhenMissingCase> {
        assert(DescriptorUtils.isSealedClass(subjectDescriptor)) {
            "isWhenOnSealedClassExhaustive should be called with a sealed class descriptor: $subjectDescriptor"
        }
        val memberClassDescriptors = getNestedSubclasses(subjectDescriptor!!)
        // When on a sealed class without derived members is considered non-exhaustive (see test WhenOnEmptySealed)
        return getMissingClassCases(expression, memberClassDescriptors, context) +
               WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable)
    }

    override fun isApplicable(subjectType: KotlinType): Boolean {
        return DescriptorUtils.isSealedClass(TypeUtils.getClassDescriptor(subjectType))
    }

    internal fun getNestedSubclasses(baseDescriptor: ClassDescriptor): Set<ClassDescriptor> {
        val memberClassDescriptors = LinkedHashSet<ClassDescriptor>()
        collectNestedSubclasses(baseDescriptor, baseDescriptor, memberClassDescriptors)
        return memberClassDescriptors
    }

    private fun collectNestedSubclasses(
            baseDescriptor: ClassDescriptor,
            currentDescriptor: ClassDescriptor,
            subclasses: MutableSet<ClassDescriptor>
    ) {
        fun collectSubclasses(scope: MemberScope, collectNested: Boolean) {
            for (descriptor in scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS)) {
                if (descriptor is ClassDescriptor) {
                    if (DescriptorUtils.isDirectSubclass(descriptor, baseDescriptor)) subclasses.add(descriptor)

                    if (collectNested) collectNestedSubclasses(baseDescriptor, descriptor, subclasses)
                }
            }
        }
        if (currentDescriptor == baseDescriptor && DescriptorUtils.isTopLevelDeclaration(currentDescriptor)) {
            collectSubclasses((currentDescriptor.containingDeclaration as PackageFragmentDescriptor).getMemberScope(), collectNested = false)
        }
        collectSubclasses(currentDescriptor.unsubstitutedInnerClassesScope, collectNested = true)
    }
}


object WhenChecker {

    private val exhaustivenessCheckers = listOf(WhenOnBooleanExhaustivenessChecker,
                                                WhenOnEnumExhaustivenessChecker,
                                                WhenOnSealedExhaustivenessChecker)

    @JvmStatic
    fun getNecessaryCases(expression: KtWhenExpression, context: BindingContext) =
            if (expression.isUsedAsExpression(context)) getMissingCases(expression, context)
            else listOf()

    @JvmStatic
    fun isWhenByEnum(expression: KtWhenExpression, context: BindingContext) =
            getClassDescriptorOfTypeIfEnum(whenSubjectType(expression, context)) != null

    @JvmStatic
    fun getClassDescriptorOfTypeIfEnum(type: KotlinType?): ClassDescriptor? {
        if (type == null) return null
        val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) return null

        return classDescriptor
    }

    private fun whenSubjectType(expression: KtWhenExpression, context: BindingContext) =
            expression.subjectExpression?.let { context.get(SMARTCAST, it) ?: context.getType(it) } ?: null

    @JvmStatic
    fun getEnumMissingCases(
            expression: KtWhenExpression,
            context: BindingContext,
            enumClassDescriptor: ClassDescriptor
    ) = WhenOnEnumExhaustivenessChecker.getMissingCases(expression, context, enumClassDescriptor, false)

    fun getMissingCases(expression: KtWhenExpression, context: BindingContext): List<WhenMissingCase> {
        val type = whenSubjectType(expression, context) ?: return listOf(UnknownMissingCase)
        val nullable = type.isMarkedNullable
        val checkers = exhaustivenessCheckers.filter { it.isApplicable(type) }
        if (checkers.isEmpty()) return listOf(UnknownMissingCase)
        return checkers.map { it.getMissingCases(expression, context, TypeUtils.getClassDescriptor(type), nullable) }.flatten()
    }

    @JvmStatic
    fun isWhenExhaustive(expression: KtWhenExpression, trace: BindingTrace) =
            if (getMissingCases(expression, trace.bindingContext).isEmpty()) {
                trace.record(BindingContext.EXHAUSTIVE_WHEN, expression)
                true
            } else {
                false
            }

    fun containsNullCase(expression: KtWhenExpression, context: BindingContext) =
            WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, true).isEmpty()

    fun checkDuplicatedLabels(expression: KtWhenExpression, trace: BindingTrace) {
        if (expression.subjectExpression == null) return

        val checkedTypes = HashSet<Pair<KotlinType, Boolean>>()
        val checkedConstants = HashSet<CompileTimeConstant<*>>()
        for (entry in expression.entries) {
            if (entry.isElse) continue

            conditions@ for (condition in entry.conditions) {
                when (condition) {
                    is KtWhenConditionWithExpression -> {
                        val constantExpression = condition.expression ?: continue@conditions
                        val constant = ConstantExpressionEvaluator.getConstant(
                                constantExpression, trace.bindingContext) ?: continue@conditions
                        if (checkedConstants.contains(constant)) {
                            trace.report(Errors.DUPLICATE_LABEL_IN_WHEN.on(constantExpression))
                        }
                        else {
                            checkedConstants.add(constant)
                        }

                    }
                    is KtWhenConditionIsPattern -> {
                        val typeReference = condition.typeReference ?: continue@conditions
                        val type = trace.get(BindingContext.TYPE, typeReference) ?: continue@conditions
                        val typeWithIsNegation = type to condition.isNegated
                        if (checkedTypes.contains(typeWithIsNegation)) {
                            trace.report(Errors.DUPLICATE_LABEL_IN_WHEN.on(typeReference))
                        }
                        else {
                            checkedTypes.add(typeWithIsNegation)
                        }
                    }
                    else -> {}
                }
            }
        }

    }

    fun checkDeprecatedWhenSyntax(trace: BindingTrace, expression: KtWhenExpression) {
        if (expression.subjectExpression != null) return

        for (entry in expression.entries) {
            if (entry.isElse) continue
            var child: PsiElement? = entry.firstChild
            while (child != null) {
                if (child.node.elementType === KtTokens.COMMA) {
                    trace.report(Errors.COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT.on(child))
                }
                if (child.node.elementType === KtTokens.ARROW) break
                child = child.nextSibling
            }
        }
    }

    fun checkReservedPrefix(trace: BindingTrace, expression: KtWhenExpression) {
        checkReservedPrefixWord(trace, expression.whenKeyword, "sealed", "sealed when")
    }
}
