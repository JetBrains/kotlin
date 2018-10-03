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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.checkReservedPrefixWord
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST
import org.jetbrains.kotlin.resolve.BindingContext.VARIABLE
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import java.util.*

sealed class WhenMissingCase {
    abstract val branchConditionText: String
}

// Always must be first in the list
object UnknownMissingCase : WhenMissingCase() {
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

object NullMissingCase : WhenMissingCase() {
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

class BooleanMissingCase(val b: Boolean) : WhenMissingCase() {
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

class ClassMissingCase(val descriptor: ClassDescriptor) : WhenMissingCase() {
    val classIsSingleton get() = descriptor.kind.isSingleton

    val classFqName get() = DescriptorUtils.getFqNameFromTopLevelClass(descriptor)

    override fun toString() = descriptor.name.identifier.let { if (classIsSingleton) it else "is $it" }

    override val branchConditionText = classFqName.asString().let { if (classIsSingleton) it else "is $it" }
}

internal abstract class WhenOnClassExhaustivenessChecker : WhenExhaustivenessChecker {
    private fun getReference(expression: KtExpression?): KtSimpleNameExpression? =
        when (expression) {
            is KtSimpleNameExpression -> expression
            is KtQualifiedExpression -> getReference(expression.selectorExpression)
            else -> null
        }

    protected val ClassDescriptor.deepSealedSubclasses: List<ClassDescriptor>
        get() = this.sealedSubclasses.flatMap {
            if (it.modality == Modality.SEALED) it.deepSealedSubclasses
            else setOf(it)
        }

    private val KtWhenCondition.negated
        get() = (this as? KtWhenConditionIsPattern)?.isNegated ?: false

    private fun KtWhenCondition.isRelevant(checkedDescriptor: ClassDescriptor) =
        this !is KtWhenConditionWithExpression ||
                DescriptorUtils.isObject(checkedDescriptor) ||
                DescriptorUtils.isEnumEntry(checkedDescriptor)

    private fun KtWhenCondition.getCheckedDescriptor(context: BindingContext): ClassDescriptor? {
        return when (this) {
            is KtWhenConditionIsPattern -> {
                val checkedType = context.get(BindingContext.TYPE, typeReference) ?: return null
                TypeUtils.getClassDescriptor(checkedType)
            }
            is KtWhenConditionWithExpression -> {
                val reference = expression?.let { getReference(it) } ?: return null
                context.get(BindingContext.REFERENCE_TARGET, reference) as? ClassDescriptor
            }
            else -> {
                null
            }
        }
    }

    protected fun getMissingClassCases(
        whenExpression: KtWhenExpression,
        subclasses: Set<ClassDescriptor>,
        context: BindingContext
    ): List<WhenMissingCase> {
        // when on empty enum / sealed is considered non-exhaustive, see test whenOnEmptySealed
        if (subclasses.isEmpty()) return listOf(UnknownMissingCase)

        val checkedDescriptors = linkedSetOf<ClassDescriptor>()
        for (whenEntry in whenExpression.entries) {
            for (condition in whenEntry.conditions) {
                val negated = condition.negated
                val checkedDescriptor = condition.getCheckedDescriptor(context) ?: continue
                val checkedDescriptorSubclasses =
                    if (checkedDescriptor.modality == Modality.SEALED) checkedDescriptor.deepSealedSubclasses
                    else listOf(checkedDescriptor)

                // Checks are important only for nested subclasses of the sealed class
                // In additional, check without "is" is important only for objects
                if (checkedDescriptorSubclasses.none { subclasses.contains(it) } ||
                    !condition.isRelevant(checkedDescriptor)) {
                    continue
                }
                if (negated) {
                    if (checkedDescriptors.containsAll(checkedDescriptorSubclasses)) return listOf()
                    checkedDescriptors.addAll(subclasses)
                    checkedDescriptors.removeAll(checkedDescriptorSubclasses)
                } else {
                    checkedDescriptors.addAll(checkedDescriptorSubclasses)
                }
            }
        }
        return (subclasses - checkedDescriptors).map(::ClassMissingCase)
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
                .filter(::isEnumEntry)
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

        val allSubclasses = subjectDescriptor!!.deepSealedSubclasses
        return getMissingClassCases(expression, allSubclasses.toSet(), context) +
                WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable)
    }

    override fun isApplicable(subjectType: KotlinType): Boolean {
        return DescriptorUtils.isSealedClass(TypeUtils.getClassDescriptor(subjectType))
    }
}


object WhenChecker {

    private val exhaustivenessCheckers = listOf(
        WhenOnBooleanExhaustivenessChecker,
        WhenOnEnumExhaustivenessChecker,
        WhenOnSealedExhaustivenessChecker
    )

    @JvmStatic
    fun getClassIdForEnumSubject(expression: KtWhenExpression, context: BindingContext) =
        getClassIdForTypeIfEnum(whenSubjectType(expression, context))

    @JvmStatic
    fun getClassIdForTypeIfEnum(type: KotlinType?) =
        getClassDescriptorOfTypeIfEnum(type)?.classId

    @JvmStatic
    fun getClassDescriptorOfTypeIfEnum(type: KotlinType?): ClassDescriptor? {
        if (type == null) return null
        val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) return null

        return classDescriptor
    }

    @JvmStatic
    fun getClassDescriptorOfTypeIfSealed(type: KotlinType?): ClassDescriptor? =
        type?.let { TypeUtils.getClassDescriptor(it) }?.takeIf { DescriptorUtils.isSealedClass(it) }


    @JvmStatic
    fun whenSubjectType(expression: KtWhenExpression, context: BindingContext): KotlinType? {
        val subjectVariable = expression.subjectVariable
        val subjectExpression = expression.subjectExpression
        return when {
            subjectVariable != null -> context.get(VARIABLE, subjectVariable)?.type
            subjectExpression != null -> context.get(SMARTCAST, subjectExpression)?.defaultType ?: context.getType(subjectExpression)
            else -> null
        }
    }

    fun whenSubjectTypeWithoutSmartCasts(expression: KtWhenExpression, context: BindingContext): KotlinType? {
        val subjectVariable = expression.subjectVariable
        val subjectExpression = expression.subjectExpression
        return when {
            subjectVariable != null -> context.get(VARIABLE, subjectVariable)?.type
            subjectExpression != null -> context.getType(subjectExpression)
            else -> null
        }
    }

    @JvmStatic
    fun getEnumMissingCases(
        expression: KtWhenExpression,
        context: BindingContext,
        enumClassDescriptor: ClassDescriptor
    ) = WhenOnEnumExhaustivenessChecker.getMissingCases(expression, context, enumClassDescriptor, false)

    @JvmStatic
    fun getSealedMissingCases(
        expression: KtWhenExpression,
        context: BindingContext,
        sealedClassDescriptor: ClassDescriptor
    ) = WhenOnSealedExhaustivenessChecker.getMissingCases(expression, context, sealedClassDescriptor, false)

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
                            constantExpression, trace.bindingContext
                        ) ?: continue@conditions
                        if (checkedConstants.contains(constant)) {
                            trace.report(Errors.DUPLICATE_LABEL_IN_WHEN.on(constantExpression))
                        } else {
                            checkedConstants.add(constant)
                        }

                    }
                    is KtWhenConditionIsPattern -> {
                        val typeReference = condition.typeReference ?: continue@conditions
                        val type = trace.get(BindingContext.TYPE, typeReference) ?: continue@conditions
                        val typeWithIsNegation = type to condition.isNegated
                        if (checkedTypes.contains(typeWithIsNegation)) {
                            trace.report(Errors.DUPLICATE_LABEL_IN_WHEN.on(typeReference))
                        } else {
                            checkedTypes.add(typeWithIsNegation)
                        }
                    }
                    else -> {
                    }
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
