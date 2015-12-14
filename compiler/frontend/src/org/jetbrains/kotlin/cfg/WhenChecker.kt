/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.psi.*;
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.checkReservedPrefixWord
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

import java.util.HashSet

import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

interface WhenMissingCase

private object UnknownMissingCase : WhenMissingCase {
    override fun toString() = "unknown"
}

private interface WhenExhaustivenessChecker {
    fun getMissingCases(
            expression: KtWhenExpression,
            trace: BindingTrace,
            subjectDescriptor: ClassDescriptor?,
            nullable: Boolean
    ): List<WhenMissingCase>

    fun isApplicable(subjectType: KotlinType, trace: BindingTrace): Boolean = false
}

private object NullMissingCase : WhenMissingCase {
    override fun toString() = "null"
}

private object WhenOnNullableExhaustivenessChecker : WhenExhaustivenessChecker {
    override fun getMissingCases(expression: KtWhenExpression, trace: BindingTrace, subjectDescriptor: ClassDescriptor?, nullable: Boolean) =
            if (nullable) getNullCaseIfMissing(expression, trace) else listOf()

    override fun isApplicable(subjectType: KotlinType, trace: BindingTrace): Boolean = TypeUtils.isNullableType(subjectType)

    private fun getNullCaseIfMissing(expression: KtWhenExpression, trace: BindingTrace): List<WhenMissingCase> {
        for (entry in expression.entries) {
            for (condition in entry.conditions) {
                if (condition is KtWhenConditionWithExpression) {
                    condition.expression?.let {
                        val type = trace.bindingContext.getType(it)
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
    override fun toString() = b.toString()
}

private object WhenOnBooleanExhaustivenessChecker : WhenExhaustivenessChecker {
    override fun getMissingCases(
            expression: KtWhenExpression,
            trace: BindingTrace,
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
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, true)) containsTrue = true
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, false)) containsFalse = true
                }
            }
        }
        return (if (!containsTrue) listOf(BooleanMissingCase(true)) else listOf()) +
               (if (!containsFalse) listOf(BooleanMissingCase(false)) else listOf())
    }

    override fun isApplicable(subjectType: KotlinType, trace: BindingTrace): Boolean {
        return KotlinBuiltIns.isBoolean(TypeUtils.makeNotNullable(subjectType))
    }
}

private class ClassMissingCase(val descriptor: ClassDescriptor): WhenMissingCase {
    override fun toString() = descriptor.name.identifier.let { if (descriptor.kind.isSingleton) it else "is $it" }
}

private abstract class WhenOnClassExhaustivenessChecker : WhenExhaustivenessChecker {
    private fun getReference(expression: KtExpression?): KtSimpleNameExpression? =
            when (expression) {
                is KtSimpleNameExpression -> expression
                is KtQualifiedExpression -> getReference(expression.selectorExpression)
                else -> null
            }

    protected fun getMissingClassCases(
            whenExpression: KtWhenExpression,
            memberDescriptors: Set<ClassDescriptor>,
            trace: BindingTrace
    ): List<WhenMissingCase> {
        // when on empty enum / sealed is considered non-exhaustive, see test whenOnEmptySealed
        if (memberDescriptors.isEmpty()) return listOf(UnknownMissingCase)
        val checkedDescriptors = HashSet<ClassDescriptor>()
        for (whenEntry in whenExpression.entries) {
            for (condition in whenEntry.conditions) {
                var negated = false
                var checkedDescriptor: ClassDescriptor? = null
                if (condition is KtWhenConditionIsPattern) {
                    val checkedType = trace.get(BindingContext.TYPE, condition.typeReference)
                    if (checkedType != null) {
                        checkedDescriptor = TypeUtils.getClassDescriptor(checkedType)
                    }
                    negated = condition.isNegated
                }
                else if (condition is KtWhenConditionWithExpression) {
                    if (condition.expression != null) {
                        val reference = getReference(condition.expression)
                        if (reference != null) {
                            val target = trace.get(BindingContext.REFERENCE_TARGET, reference)
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
            trace: BindingTrace,
            subjectDescriptor: ClassDescriptor?,
            nullable: Boolean
    ): List<WhenMissingCase> {
        assert(isEnumClass(subjectDescriptor)) { "isWhenOnEnumExhaustive should be called with an enum class descriptor" }
        val entryDescriptors =
                DescriptorUtils.getAllDescriptors(subjectDescriptor!!.unsubstitutedInnerClassesScope)
                        .filter { isEnumEntry(it) }
                        .filterIsInstance<ClassDescriptor>()
                        .toSet()
        return getMissingClassCases(expression, entryDescriptors, trace)
    }

    override fun isApplicable(subjectType: KotlinType, trace: BindingTrace): Boolean {
        return WhenChecker.getClassDescriptorOfTypeIfEnum(subjectType) != null
    }
}

private object WhenOnSealedExhaustivenessChecker : WhenOnClassExhaustivenessChecker() {
    override fun getMissingCases(
            expression: KtWhenExpression,
            trace: BindingTrace,
            subjectDescriptor: ClassDescriptor?,
            nullable: Boolean
    ): List<WhenMissingCase> {
        assert(subjectDescriptor != null) { "isWhenOnSealedClassExhaustive should be called with not-null subject class descriptor" }
        assert(subjectDescriptor!!.modality === Modality.SEALED) {
            "isWhenOnSealedClassExhaustive should be called with a sealed class descriptor"
        }
        val memberClassDescriptors = HashSet<ClassDescriptor>()
        collectNestedSubclasses(subjectDescriptor!!, subjectDescriptor, memberClassDescriptors)
        // When on a sealed class without derived members is considered non-exhaustive (see test WhenOnEmptySealed)
        return getMissingClassCases(expression, memberClassDescriptors, trace)
    }

    override fun isApplicable(subjectType: KotlinType, trace: BindingTrace): Boolean {
        return TypeUtils.getClassDescriptor(subjectType)?.modality == Modality.SEALED
    }

    private fun collectNestedSubclasses(
            baseDescriptor: ClassDescriptor,
            currentDescriptor: ClassDescriptor,
            subclasses: MutableSet<ClassDescriptor>) {
        for (descriptor in DescriptorUtils.getAllDescriptors(currentDescriptor.unsubstitutedInnerClassesScope)) {
            if (descriptor is ClassDescriptor) {
                if (DescriptorUtils.isDirectSubclass(descriptor, baseDescriptor)) {
                    subclasses.add(descriptor)
                }
                collectNestedSubclasses(baseDescriptor, descriptor, subclasses)
            }
        }
    }
}


object WhenChecker {

    private val exhaustivenessCheckers = listOf(WhenOnBooleanExhaustivenessChecker,
                                                WhenOnEnumExhaustivenessChecker,
                                                WhenOnSealedExhaustivenessChecker,
                                                WhenOnNullableExhaustivenessChecker)

    @JvmStatic
    fun mustHaveElse(expression: KtWhenExpression, trace: BindingTrace) =
            expression.isUsedAsExpression(trace.bindingContext) && !isWhenExhaustive(expression, trace)

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
            expression.subjectExpression?.let { context.getType(it) } ?: null

    @JvmStatic
    fun isWhenOnEnumExhaustive(
            expression: KtWhenExpression,
            trace: BindingTrace,
            enumClassDescriptor: ClassDescriptor
    ) = WhenOnEnumExhaustivenessChecker.getMissingCases(expression, trace, enumClassDescriptor, false).isEmpty()

    /**
     * It's assumed that function is called for a final type. In this case the only possible smart cast is to not nullable type.
     * @return true if type is nullable, and cannot be smart casted
     */
    private fun isNullableTypeWithoutPossibleSmartCast(
            expression: KtExpression?,
            type: KotlinType,
            trace: BindingTrace): Boolean {
        if (expression == null) return false // Normally should not happen
        if (!TypeUtils.isNullableType(type)) return false
        // We cannot read data flow information here due to lack of inputs (module descriptor is necessary)
        if (trace.get(BindingContext.SMARTCAST, expression) != null) {
            // We have smart cast from enum or boolean to something
            // Not very nice but we *can* decide it was smart cast to not-null
            // because both enum and boolean are final
            return false
        }
        return true
    }

    private fun getMissingCases(expression: KtWhenExpression, trace: BindingTrace): List<WhenMissingCase> {
        val type = whenSubjectType(expression, trace.bindingContext) ?: return listOf(UnknownMissingCase)
        val nullable = !type.isFlexible() && isNullableTypeWithoutPossibleSmartCast(expression.subjectExpression, type, trace)
        val checkers = exhaustivenessCheckers.filter { it.isApplicable(type, trace) }
        if (checkers.isEmpty()) return listOf(UnknownMissingCase)
        return checkers.map { it.getMissingCases(expression, trace, TypeUtils.getClassDescriptor(type), nullable) }.flatten()
    }

    @JvmStatic
    fun isWhenExhaustive(expression: KtWhenExpression, trace: BindingTrace) =
            if (getMissingCases(expression, trace).isEmpty()) {
                trace.record(BindingContext.EXHAUSTIVE_WHEN, expression)
                true
            } else {
                false
            }

    @JvmStatic
    fun containsNullCase(expression: KtWhenExpression, trace: BindingTrace) =
            WhenOnNullableExhaustivenessChecker.getMissingCases(expression, trace, null, true).isEmpty()

    @JvmStatic
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

    @JvmStatic
    fun checkReservedPrefix(trace: BindingTrace, expression: KtWhenExpression) {
        checkReservedPrefixWord(trace, expression.whenKeyword, "sealed", TokenSet.EMPTY, "sealed when")
    }
}
