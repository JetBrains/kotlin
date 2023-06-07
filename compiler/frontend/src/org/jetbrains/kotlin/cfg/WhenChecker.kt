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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.*
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
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf


val List<WhenMissingCase>.hasUnknown: Boolean
    get() = firstOrNull() == WhenMissingCase.Unknown

private interface WhenExhaustivenessChecker {
    fun getMissingCases(
        expression: KtWhenExpression,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,
        nullable: Boolean
    ): List<WhenMissingCase>

    fun isApplicable(subjectType: KotlinType): Boolean = false
}

// It's not a regular exhaustiveness checker, invoke it only inside other checkers
private object WhenOnExpectExhaustivenessChecker {
    fun getMissingCase(subjectDescriptor: ClassDescriptor?): WhenMissingCase? {
        return runIf(subjectDescriptor?.isExpect == true) {
            when (subjectDescriptor!!.kind) {
                ClassKind.CLASS -> WhenMissingCase.ConditionTypeIsExpect.SealedClass
                ClassKind.INTERFACE -> WhenMissingCase.ConditionTypeIsExpect.SealedInterface
                ClassKind.ENUM_CLASS -> WhenMissingCase.ConditionTypeIsExpect.Enum
                else -> WhenMissingCase.Unknown
            }
        }
    }
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
        return listOf(WhenMissingCase.NullIsMissing)
    }
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
        return (if (!containsTrue) listOf(WhenMissingCase.BooleanIsMissing.TrueIsMissing) else listOf()) +
                (if (!containsFalse) listOf(WhenMissingCase.BooleanIsMissing.FalseIsMissing) else listOf()) +
                WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable)
    }

    override fun isApplicable(subjectType: KotlinType): Boolean {
        return KotlinBuiltIns.isBoolean(TypeUtils.makeNotNullable(subjectType))
    }
}


internal abstract class WhenOnClassExhaustivenessChecker : WhenExhaustivenessChecker {
    private fun getReference(expression: KtExpression?): KtSimpleNameExpression? =
        when (expression) {
            is KtSimpleNameExpression -> expression
            is KtQualifiedExpression -> getReference(expression.selectorExpression)
            else -> null
        }

    protected val ClassDescriptor.enumEntries: Set<ClassDescriptor>
        get() = DescriptorUtils.getAllDescriptors(this.unsubstitutedInnerClassesScope)
            .filter(::isEnumEntry)
            .filterIsInstance<ClassDescriptor>()
            .toSet()


    protected val ClassDescriptor.deepSealedSubclasses: Set<ClassDescriptor>
        get() = this.sealedSubclasses.flatMapTo(mutableSetOf()) {
            it.subclasses
        }

    private val ClassDescriptor.subclasses: Set<ClassDescriptor>
        get() = when {
            this.modality == Modality.SEALED -> this.deepSealedSubclasses
            this.kind == ClassKind.ENUM_CLASS -> this.enumEntries
            else -> setOf(this)
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
        if (subclasses.isEmpty()) return listOf(WhenMissingCase.Unknown)

        val checkedDescriptors = linkedSetOf<ClassDescriptor>()
        for (whenEntry in whenExpression.entries) {
            for (condition in whenEntry.conditions) {
                val negated = condition.negated
                val checkedDescriptor = condition.getCheckedDescriptor(context) ?: continue
                val checkedDescriptorSubclasses = checkedDescriptor.subclasses

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
        return (subclasses - checkedDescriptors).map(::createWhenMissingCaseForClassOrEnum)
    }

    private fun createWhenMissingCaseForClassOrEnum(classDescriptor: ClassDescriptor): WhenMissingCase {
        val classId = DescriptorUtils.getClassIdForNonLocalClass(classDescriptor)
        return if (classDescriptor.kind != ClassKind.ENUM_ENTRY) {
            WhenMissingCase.IsTypeCheckIsMissing(
                classId = DescriptorUtils.getClassIdForNonLocalClass(classDescriptor),
                isSingleton = classDescriptor.kind.isSingleton
            )
        } else {
            val enumClassId = classId.outerClassId ?: error("Enum should have class id")
            WhenMissingCase.EnumCheckIsMissing(CallableId(enumClassId, classId.shortClassName))
        }
    }
}

private object WhenOnEnumExhaustivenessChecker : WhenOnClassExhaustivenessChecker() {
    @OptIn(ExperimentalStdlibApi::class)
    override fun getMissingCases(
        expression: KtWhenExpression,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,
        nullable: Boolean
    ): List<WhenMissingCase> {
        assert(isEnumClass(subjectDescriptor)) { "isWhenOnEnumExhaustive should be called with an enum class descriptor" }
        return buildList {
            addAll(getMissingClassCases(expression, subjectDescriptor!!.enumEntries, context))
            addAll(WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable))
            addIfNotNull(WhenOnExpectExhaustivenessChecker.getMissingCase(subjectDescriptor))
        }
    }

    override fun isApplicable(subjectType: KotlinType): Boolean {
        return WhenChecker.getClassDescriptorOfTypeIfEnum(subjectType) != null
    }
}

internal object WhenOnSealedExhaustivenessChecker : WhenOnClassExhaustivenessChecker() {
    @OptIn(ExperimentalStdlibApi::class)
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
        return buildList {
            addAll(getMissingClassCases(expression, allSubclasses.toSet(), context))
            addAll(WhenOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable))
            addIfNotNull(WhenOnExpectExhaustivenessChecker.getMissingCase(subjectDescriptor))
        }
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
        val type = whenSubjectType(expression, context) ?: return listOf(WhenMissingCase.Unknown)
        val nullable = type.isMarkedNullable
        val checkers = exhaustivenessCheckers.filter { it.isApplicable(type) }
        if (checkers.isEmpty()) return listOf(WhenMissingCase.Unknown)
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

    fun checkDuplicatedLabels(
        expression: KtWhenExpression,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
    ) {
        if (expression.subjectExpression == null) return

        val checkedTypes = HashSet<Pair<KotlinType, Boolean>>()
        /*
         * `true` in map means that constant can be removed and nothing breaks
         * `false` means opposite
         *
         * Example:
         *   const val myF = false
         *   const val myT = true
         *
         *   fun test_1(someBoolean: Boolean) {
         *       val s = when (someBoolean) {
         *           myT -> 1
         *           myF -> 2
         *           true -> 3    // DUPLICATE_LABEL_IN_WHEN
         *           false -> 4   // DUPLICATE_LABEL_IN_WHEN
         *       }
         *   }
         *
         * In this case myT and myF actually are `true` and `false` correspondingly, but
         *   const vals are not treated by exhaustive checkers, so removal `true` or `false`
         *   branches will break code, so we need to report DUPLICATE_LABEL_IN_WHEN on `myT` and
         *   `myF`, not on `true` and `false`
         */
        val checkedConstants = mutableMapOf<CompileTimeConstant<*>, Boolean>()
        val notTrivialBranches = mutableMapOf<CompileTimeConstant<*>, KtExpression>()
        for (entry in expression.entries) {
            if (entry.isElse) continue

            conditions@ for (condition in entry.conditions) {
                when (condition) {
                    is KtWhenConditionWithExpression -> {
                        val constantExpression = condition.expression ?: continue@conditions
                        val constant = ConstantExpressionEvaluator.getConstant(
                            constantExpression, trace.bindingContext
                        ) ?: continue@conditions

                        fun report(reportOn: KtExpression) {
                            trace.report(Errors.DUPLICATE_LABEL_IN_WHEN.on(reportOn))
                        }

                        when (checkedConstants[constant]) {
                            true -> {
                                // already found trivial constant in previous branches
                                report(constantExpression)
                            }
                            false -> {
                                // already found bad constant in previous branches
                                val isTrivial = constant.isTrivial(constantExpression, languageVersionSettings)
                                if (isTrivial) {
                                    // this constant is trivial -> report on first non trivial constant
                                    val reportOn = notTrivialBranches.remove(constant)!!
                                    report(reportOn)
                                    checkedConstants[constant] = true
                                } else {
                                    // this constant is also not trivial -> report on it
                                    report(constantExpression)
                                }
                            }
                            null -> {
                                // met constant for a first time
                                val isTrivial = constant.isTrivial(constantExpression, languageVersionSettings)
                                checkedConstants[constant] = isTrivial
                                if (!isTrivial) {
                                    notTrivialBranches[constant] = constantExpression
                                }
                            }
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

    private fun CompileTimeConstant<*>.isTrivial(
        expression: KtExpression,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        if (usesVariableAsConstant) return false
        if (!languageVersionSettings.supportsFeature(LanguageFeature.ProhibitSimplificationOfNonTrivialConstBooleanExpressions)) {
            return !ConstantExpressionEvaluator.isComplexBooleanConstant(expression, this)
        }
        return true
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

    fun checkSealedWhenIsReserved(sink: DiagnosticSink, element: PsiElement) {
        KtPsiUtil.getPreviousWord(element, "sealed")?.let {
            sink.report(Errors.UNSUPPORTED_SEALED_WHEN.on(it))
        }
    }
}
