/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.java.enhancement.EnhancedForWarningConeSubstitutor
import org.jetbrains.kotlin.fir.java.enhancement.enhancedTypeForWarning
import org.jetbrains.kotlin.fir.java.enhancement.isEnhancedTypeForWarningDeprecation
import org.jetbrains.kotlin.fir.types.*

// TODO reimplement using AdditionalTypeChecker KT-62864
object FirQualifiedAccessJavaNullabilityWarningChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.toResolvedCallableSymbol() ?: return
        val substitutor = expression.createConeSubstitutorFromTypeArguments(symbol, context.session)

        // This `if` shouldn't be necessary because we should get a type mismatch for the dispatch receiver iff the type for warning
        // has nullability NULLABLE.
        // Unfortunately, we can get situations when that's not true, when the expected type has captured arguments, see KT-66947.
        // As a workaround, we do an explicit check for the nullability.
        if (symbol.dispatchReceiverType != null &&
            expression.dispatchReceiver?.resolvedType?.willBeMarkedNullableInFuture() == true
        ) {
            expression.dispatchReceiver?.checkExpressionForEnhancedTypeMismatch(
                expectedType = symbol.dispatchReceiverType,
                reporter,
                context,
                FirJvmErrors.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            )
        }

        val receiverType = symbol.receiverParameter?.typeRef?.coneType
        expression.extensionReceiver?.checkExpressionForEnhancedTypeMismatch(
            expectedType = receiverType?.let(substitutor::substituteOrSelf),
            reporter,
            context,
            FirJvmErrors.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
        )

        for ((contextArgument, contextParameter) in expression.contextArguments.zip(symbol.resolvedContextParameters)) {
            contextArgument.checkExpressionForEnhancedTypeMismatch(
                expectedType = substitutor.substituteOrSelf(contextParameter.returnTypeRef.coneType),
                reporter,
                context,
                FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            )
        }

        if (expression is FirFunctionCall) {
            expression.resolvedArgumentMapping?.forEach { (argument, parameter) ->
                argument.checkExpressionForEnhancedTypeMismatch(
                    expectedType = substitutor.substituteOrSelf(parameter.returnTypeRef.coneType),
                    reporter,
                    context,
                    FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
                )
            }
        }
    }

    private fun ConeKotlinType.willBeMarkedNullableInFuture(): Boolean {
        return enhancedTypeForWarning?.isMarkedNullable == true ||
                attributes.explicitTypeArgumentIfMadeFlexibleSynthetically?.let {
                    it.coneType.isMarkedNullable && it.relevantFeature == LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible
                } == true
    }
}

object FirThrowJavaNullabilityWarningChecker : FirThrowExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirThrowExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        expression.exception.checkExpressionForEnhancedTypeMismatch(
            expectedType = context.session.builtinTypes.throwableType.coneType,
            reporter,
            context,
            FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
        )
    }
}

object FirAssignmentJavaNullabilityWarningChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        expression.rValue.checkExpressionForEnhancedTypeMismatch(
            expectedType = expression.lValue.resolvedType,
            reporter,
            context,
            FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
        )
    }
}

object FirLogicExpressionTypeJavaNullabilityWarningChecker : FirBooleanOperatorExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirBooleanOperatorExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        expression.leftOperand.checkConditionForEnhancedTypeMismatch(context, reporter)
        expression.rightOperand.checkConditionForEnhancedTypeMismatch(context, reporter)
    }
}

object FirLoopConditionJavaNullabilityWarningChecker : FirLoopExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirLoop, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression is FirErrorLoop) return
        val condition = expression.condition
        condition.checkConditionForEnhancedTypeMismatch(context, reporter)
    }
}

object FirWhenConditionJavaNullabilityWarningChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (branch in expression.branches) {
            val condition = branch.condition
            if (condition is FirElseIfTrueCondition) continue
            condition.checkConditionForEnhancedTypeMismatch(context, reporter)
        }
    }
}

object FirReturnJavaNullabilityWarningChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirReturnExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        expression.result.checkExpressionForEnhancedTypeMismatch(
            expectedType = expression.target.labeledElement.returnTypeRef.coneType,
            reporter,
            context,
            FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
        )
    }
}

private fun FirExpression.checkConditionForEnhancedTypeMismatch(context: CheckerContext, reporter: DiagnosticReporter) {
    checkExpressionForEnhancedTypeMismatch(
        context.session.builtinTypes.booleanType.coneType,
        reporter,
        context,
        FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
    )
}

internal fun FirExpression.checkExpressionForEnhancedTypeMismatch(
    expectedType: ConeKotlinType?,
    reporter: DiagnosticReporter,
    context: CheckerContext,
    factory: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String>,
) {
    if (expectedType == null) return
    val actualType = resolvedType

    val (actualTypeForComparison, expectedTypeForComparison) = getEnhancedTypesForComparison(actualType, expectedType, context)
        ?: return

    if (!actualTypeForComparison.isSubtypeOf(context.session.typeContext, expectedTypeForComparison) &&
        // Don't report anything if the original types didn't match.
        actualType.isSubtypeOf(context.session.typeContext, expectedType)
    ) {
        val suffix =
            when {
                actualType.isEnhancedTypeForWarningDeprecation || expectedType.isEnhancedTypeForWarningDeprecation -> {
                    val versionString =
                        LanguageFeature.SupportJavaErrorEnhancementOfArgumentsOfWarningLevelEnhanced.sinceVersion?.versionString
                    "This will become an error in Kotlin $versionString. See https://youtrack.jetbrains.com/issue/KT-63209"
                }
                actualType.isMadeFlexibleSynthetically() || expectedType.isMadeFlexibleSynthetically() -> {
                    val versionString =
                        LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible.sinceVersion?.versionString
                    "This will become an error in Kotlin $versionString. See https://youtrack.jetbrains.com/issue/KT-71718"
                }
                else -> {
                    ""
                }
            }
        reporter.reportOn(source, factory, actualTypeForComparison, expectedTypeForComparison, suffix, context)
    }
}

private fun ConeKotlinType.isMadeFlexibleSynthetically(): Boolean =
    attributes.explicitTypeArgumentIfMadeFlexibleSynthetically != null

private fun getEnhancedTypesForComparison(
    actualType: ConeKotlinType?,
    expectedType: ConeKotlinType?,
    context: CheckerContext,
): Pair<ConeKotlinType, ConeKotlinType>? {
    if (actualType == null || expectedType == null) return null
    if (actualType is ConeErrorType || expectedType is ConeErrorType) return null

    val substitutor = EnhancedForWarningConeSubstitutor(
        context.session.typeContext,
        useExplicitTypeArgumentIfMadeFlexibleSyntheticallyWithFeature = LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible
    )

    val enhancedActualType = substitutor.substituteOrNull(actualType)
    val enhancedExpectedType = substitutor.substituteOrNull(expectedType)

    // No enhancement on either side, nothing to check.
    if (enhancedActualType == null && enhancedExpectedType == null) return null

    val actualTypeForComparison = enhancedActualType ?: actualType
    val expectedTypeForComparison = enhancedExpectedType ?: expectedType

    return actualTypeForComparison to expectedTypeForComparison
}
