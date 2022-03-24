/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.*

abstract class ResolutionDiagnostic(val applicability: CandidateApplicability)

abstract class InapplicableArgumentDiagnostic : ResolutionDiagnostic(INAPPLICABLE) {
    abstract val argument: FirExpression
}

class MixingNamedAndPositionArguments(override val argument: FirExpression) : InapplicableArgumentDiagnostic()

class TooManyArguments(
    val argument: FirExpression,
    val function: FirFunction
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NamedArgumentNotAllowed(
    override val argument: FirExpression,
    val function: FirFunction,
    val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget
) : InapplicableArgumentDiagnostic()

class ArgumentPassedTwice(
    override val argument: FirExpression,
    val valueParameter: FirValueParameter,
    val firstOccurrence: ResolvedCallArgument
) : InapplicableArgumentDiagnostic()

class VarargArgumentOutsideParentheses(
    override val argument: FirExpression,
    val valueParameter: FirValueParameter
) : InapplicableArgumentDiagnostic()

class NonVarargSpread(override val argument: FirExpression) : InapplicableArgumentDiagnostic()

class NoValueForParameter(
    val valueParameter: FirValueParameter,
    val function: FirFunction
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NameNotFound(
    val argument: FirNamedArgumentExpression,
    val function: FirFunction
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

object InapplicableCandidate : ResolutionDiagnostic(INAPPLICABLE)

object HiddenCandidate : ResolutionDiagnostic(HIDDEN)

object VisibilityError : ResolutionDiagnostic(VISIBILITY_ERROR)

object ResolvedWithLowPriority : ResolutionDiagnostic(RESOLVED_LOW_PRIORITY)

class InapplicableWrongReceiver(
    val expectedType: ConeKotlinType? = null,
    val actualType: ConeKotlinType? = null,
) : ResolutionDiagnostic(INAPPLICABLE_WRONG_RECEIVER)

object NoCompanionObject : ResolutionDiagnostic(NO_COMPANION_OBJECT)

class UnsafeCall(val actualType: ConeKotlinType) : ResolutionDiagnostic(UNSAFE_CALL)

object LowerPriorityToPreserveCompatibilityDiagnostic : ResolutionDiagnostic(RESOLVED_NEED_PRESERVE_COMPATIBILITY)

object LowerPriorityForDynamic : ResolutionDiagnostic(RESOLVED_LOW_PRIORITY)

object CandidateChosenUsingOverloadResolutionByLambdaAnnotation : ResolutionDiagnostic(RESOLVED)

class UnstableSmartCast(val argument: FirExpressionWithSmartcast, val targetType: ConeKotlinType, val isCastToNotNull: Boolean) : ResolutionDiagnostic(UNSTABLE_SMARTCAST)

class ArgumentTypeMismatch(
    val expectedType: ConeKotlinType,
    val actualType: ConeKotlinType,
    val argument: FirExpression,
    val isMismatchDueToNullability: Boolean,
) : ResolutionDiagnostic(INAPPLICABLE)

class NullForNotNullType(
    val argument: FirExpression
) : ResolutionDiagnostic(INAPPLICABLE)

class ManyLambdaExpressionArguments(
    val argument: FirExpression
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class InfixCallOfNonInfixFunction(val function: FirNamedFunctionSymbol) : ResolutionDiagnostic(INAPPLICABLE_MODIFIER)
class OperatorCallOfNonOperatorFunction(val function: FirNamedFunctionSymbol) : ResolutionDiagnostic(INAPPLICABLE_MODIFIER)

class InferenceError(val constraintError: ConstraintSystemError) : ResolutionDiagnostic(constraintError.applicability)
class Unsupported(val message: String, val source: KtSourceElement? = null) : ResolutionDiagnostic(UNSUPPORTED)

object PropertyAsOperator : ResolutionDiagnostic(PROPERTY_AS_OPERATOR)

class DslScopeViolation(val calleeSymbol: FirBasedSymbol<*>) : ResolutionDiagnostic(DSL_SCOPE_VIOLATION)