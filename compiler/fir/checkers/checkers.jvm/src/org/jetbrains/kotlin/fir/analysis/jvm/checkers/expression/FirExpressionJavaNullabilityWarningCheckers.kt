/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.rendering.appendDeprecationWarningSuffix
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.finalApproximationOrSelf
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.dispatchReceiverScope
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.java.enhancement.EnhancedForWarningConeSubstitutor
import org.jetbrains.kotlin.fir.java.enhancement.isEnhancedTypeForWarningDeprecation
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processOverriddenProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*

// TODO reimplement using AdditionalTypeChecker KT-62864
object FirQualifiedAccessJavaNullabilityWarningChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val symbol = expression.toResolvedCallableSymbol() ?: return
        val substitutor = expression.createConeSubstitutorFromTypeArguments(symbol, context.session)

        checkDispatchReceiver(expression, symbol)

        expression.extensionReceiver?.checkExpressionForEnhancedTypeMismatch(
            expectedType = symbol.resolvedReceiverType?.let(substitutor::substituteOrSelf),
            FirJvmErrors.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
            suppressWarnings = { actualTypeForComparison -> shouldSuppressWarningForExtensionReceiver(symbol, actualTypeForComparison) }
        )

        for ((contextArgument, contextParameter) in expression.contextArguments.zip(symbol.contextParameterSymbols)) {
            contextArgument.checkExpressionForEnhancedTypeMismatch(
                expectedType = substitutor.substituteOrSelf(contextParameter.resolvedReturnType),
                FirJvmErrors.TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            )
        }

        if (expression is FirFunctionCall) {
            expression.resolvedArgumentMapping?.forEach { (argument, parameter) ->
                argument.checkExpressionForEnhancedTypeMismatch(
                    expectedType = substitutor.substituteOrSelf(parameter.returnTypeRef.coneType),
                    FirJvmErrors.TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
                )
            }
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkDispatchReceiver(
    expression: FirQualifiedAccessExpression,
    symbol: FirCallableSymbol<*>,
) {
    val actualDispatchReceiverType = expression.dispatchReceiver?.resolvedType
    val expectedDispatchReceiverType = symbol.dispatchReceiverType
    if (actualDispatchReceiverType == null || expectedDispatchReceiverType == null) return

    val substitutor = enhancedForWarningSubstitutor()
    val enhancedDispatchReceiverType = substitutor.substituteOrNull(actualDispatchReceiverType) ?: return

    if (!actualDispatchReceiverType.canLowerBoundBeNull() && enhancedDispatchReceiverType.canLowerBoundBeNull()) {

        val factory = if (actualDispatchReceiverType.isExplicitTypeArgumentMadeFlexibleSynthetically()) {
            FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA
        } else {
            FirJvmErrors.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
        }
        reporter.reportOn(
            expression.dispatchReceiver?.source,
            factory,
            enhancedDispatchReceiverType,
            expectedDispatchReceiverType,
            buildSuffix(actualDispatchReceiverType, expectedDispatchReceiverType)
        )
    }

    // To check for mutability mismatch, we check if the class ID of the enhanced type is different.
    // If it is, we process all overridden declarations and try to find the enhanced class ID.
    // A non-obvious invariant is actualDispatchReceiverType <: enhancedDispatchReceiverType.
    // This is true because we can only have two situations:
    // EFW(List) MutableList..List => MutableList <: List
    // MutableList..EFW(MutableList)List => MutableList <: MutableList
    // Therefore we don't need to handle the case where enhancedDispatchReceiverType is a real subtype of actualDispatchReceiverType.
    val actualApproximatedType = actualDispatchReceiverType.finalApproximationOrSelf()
    val actualClassId = actualApproximatedType.classId

    // TODO(KT-64024) replace with enhancedDispatchReceiverType.finalApproximationOrSelf().classId
    //  once substitution of captured types is fixed.
    val enhancedApproximatedType = substitutor.substituteOrSelf(actualApproximatedType)
    val enhancedClassId = enhancedApproximatedType.classId

    if (actualClassId != null &&
        enhancedClassId != actualClassId &&
        // symbol.dispatchReceiverType is only equal to actualDispatchReceiverType in case of fake overrides
        // Prominent counter examples are equals, hashCode and toString defined in Any.
        !enhancedApproximatedType.isSubtypeOf(
            expectedDispatchReceiverType.replaceArgumentsWithStarProjectionsOrNull() ?: expectedDispatchReceiverType, context.session
        )
    ) {
        val scope = symbol.dispatchReceiverScope(context.session, context.scopeSession)

        var found = false
        val processor = { it: FirCallableSymbol<*> ->
            if (it.dispatchReceiverType?.classId == enhancedClassId) {
                found = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        @OptIn(ScopeFunctionRequiresPrewarm::class)
        if (symbol is FirPropertySymbol) {
            scope.processPropertiesByName(symbol.name) {}
            scope.processOverriddenProperties(symbol, processor)
        } else if (symbol is FirNamedFunctionSymbol) {
            scope.processFunctionsByName(symbol.name) {}
            scope.processOverriddenFunctions(symbol, processor)
        }

        if (!found) {
            reporter.reportOn(
                expression.dispatchReceiver?.source,
                FirJvmErrors.RECEIVER_MUTABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
                enhancedDispatchReceiverType,
                actualClassId,
            )
        }
    }
}

context(context: CheckerContext)
private fun ConeKotlinType.canLowerBoundBeNull(): Boolean {
    return lowerBoundIfFlexible().canBeNull(context.session)
}

/**
 * Mutability enhancement for warning can lead to unfortunate situations like the following:
 *
 * ```kt
 * val list: EFW(List<String!>!) (Mutable)List<String!>!
 * list.asReversed()
 * ```
 *
 * The call resolves to `fun MutableList<T>.asReversed()`, we see that `List<String>` is not a subtype of `MutableList<String>`
 * and report a warning.
 *
 * But in fact, the warning is mostly useless because when we switch the strictness to error,
 * the call now resolves to the overload `fun List<T>.asReversed()`.
 *
 * As a crude workaround, we suppress the warning for extension functions in the `kotlin.collections` package
 * when an overload exists that has a compatible receiver type.
 */
context(context: CheckerContext)
private fun shouldSuppressWarningForExtensionReceiver(
    symbol: FirCallableSymbol<*>,
    actualTypeForComparison: ConeKotlinType,
): Boolean {
    val callableId = symbol.callableId
    if (callableId?.packageName != StandardNames.COLLECTIONS_PACKAGE_FQ_NAME) return false

    val topLevelCallableSymbols = context.session.symbolProvider.getTopLevelCallableSymbols(
        StandardNames.COLLECTIONS_PACKAGE_FQ_NAME,
        callableId.callableName
    )

    return topLevelCallableSymbols.any {
        val receiverType =
            (it.receiverParameterSymbol?.resolvedType as? ConeSimpleKotlinType)
                ?.replaceArgumentsWithStarProjectionsOrNull()
                ?: return@any false
        actualTypeForComparison.isSubtypeOf(receiverType, context.session)
    }
}

object FirThrowJavaNullabilityWarningChecker : FirThrowExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirThrowExpression) {
        expression.exception.checkExpressionForEnhancedTypeMismatch(
            expectedType = context.session.builtinTypes.throwableType.coneType,
            FirJvmErrors.TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
        )
    }
}

object FirAssignmentJavaNullabilityWarningChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        expression.rValue.checkExpressionForEnhancedTypeMismatch(
            expectedType = expression.lValue.resolvedType,
            FirJvmErrors.TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
        )
    }
}

object FirLogicExpressionTypeJavaNullabilityWarningChecker : FirBooleanOperatorExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirBooleanOperatorExpression) {
        expression.leftOperand.checkConditionForEnhancedTypeMismatch()
        expression.rightOperand.checkConditionForEnhancedTypeMismatch()
    }
}

object FirLoopConditionJavaNullabilityWarningChecker : FirLoopExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirLoop) {
        if (expression is FirErrorLoop) return
        val condition = expression.condition
        condition.checkConditionForEnhancedTypeMismatch()
    }
}

object FirWhenConditionJavaNullabilityWarningChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        for (branch in expression.branches) {
            val condition = branch.condition
            if (condition is FirElseIfTrueCondition) continue
            condition.checkConditionForEnhancedTypeMismatch()
        }
    }
}

object FirReturnJavaNullabilityWarningChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        expression.result.checkExpressionForEnhancedTypeMismatch(
            expectedType = expression.target.labeledElement.returnTypeRef.coneType,
            FirJvmErrors.TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
        )
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun FirExpression.checkConditionForEnhancedTypeMismatch() {
    checkExpressionForEnhancedTypeMismatch(
        context.session.builtinTypes.booleanType.coneType,
        FirJvmErrors.TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
    )
}

context(reporter: DiagnosticReporter, context: CheckerContext)
internal fun FirExpression.checkExpressionForEnhancedTypeMismatch(
    expectedType: ConeKotlinType?,
    factory: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String>,
    suppressWarnings: (actualTypeForComparison: ConeKotlinType) -> Boolean = { false },
) {
    if (expectedType == null) return
    val actualType = resolvedType

    val (actualTypeForComparison, expectedTypeForComparison) = getEnhancedTypesForComparison(actualType, expectedType)
        ?: return

    if (!actualTypeForComparison.isSubtypeOf(context.session.typeContext, expectedTypeForComparison) &&
        // Don't report anything if the original types didn't match.
        actualType.isSubtypeOf(context.session.typeContext, expectedType) &&
        !suppressWarnings(actualTypeForComparison)
    ) {
        val resultingFactory = when {
            actualType.isExplicitTypeArgumentMadeFlexibleSynthetically() ||
                    expectedType.isExplicitTypeArgumentMadeFlexibleSynthetically()
                -> FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA
            factory == FirJvmErrors.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS && !actualTypeForComparison.canLowerBoundBeNull()
                -> FirJvmErrors.TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            else -> factory
        }

        reporter.reportOn(
            source,
            resultingFactory,
            actualTypeForComparison,
            expectedTypeForComparison,
            buildSuffix(actualType, expectedType)
        )
    }
}

private fun buildSuffix(
    actualType: ConeKotlinType,
    expectedType: ConeKotlinType,
): String {
    return buildString {
        when {
            actualType.isEnhancedTypeForWarningDeprecation || expectedType.isEnhancedTypeForWarningDeprecation -> {
                appendDeprecationWarningSuffix(LanguageFeature.SupportJavaErrorEnhancementOfArgumentsOfWarningLevelEnhanced)
            }
            actualType.isExplicitTypeArgumentMadeFlexibleSynthetically() || expectedType.isExplicitTypeArgumentMadeFlexibleSynthetically() -> {
                appendDeprecationWarningSuffix(LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible)
            }
        }
    }
}

private fun ConeKotlinType.isExplicitTypeArgumentMadeFlexibleSynthetically(): Boolean =
    attributes.explicitTypeArgumentIfMadeFlexibleSynthetically?.relevantFeature == LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible

context(context: CheckerContext)
private fun getEnhancedTypesForComparison(
    actualType: ConeKotlinType?,
    expectedType: ConeKotlinType?,
): Pair<ConeKotlinType, ConeKotlinType>? {
    if (actualType == null || expectedType == null) return null
    if (actualType is ConeErrorType || expectedType is ConeErrorType) return null

    val substitutor = enhancedForWarningSubstitutor()

    val enhancedActualType = substitutor.substituteOrNull(actualType)
    val enhancedExpectedType = substitutor.substituteOrNull(expectedType)

    // No enhancement on either side, nothing to check.
    if (enhancedActualType == null && enhancedExpectedType == null) return null

    val actualTypeForComparison = enhancedActualType ?: actualType
    val expectedTypeForComparison = enhancedExpectedType ?: expectedType

    return actualTypeForComparison to expectedTypeForComparison
}

context(context: CheckerContext)
private fun enhancedForWarningSubstitutor(): EnhancedForWarningConeSubstitutor {
    return EnhancedForWarningConeSubstitutor(
        context.session.typeContext,
        useExplicitTypeArgumentIfMadeFlexibleSyntheticallyWithFeature = LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible
    )
}
