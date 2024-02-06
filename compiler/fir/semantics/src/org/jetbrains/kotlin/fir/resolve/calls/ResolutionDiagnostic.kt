/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.*
import org.jetbrains.kotlin.types.EmptyIntersectionTypeKind

abstract class ResolutionDiagnostic(val applicability: CandidateApplicability)

abstract class InapplicableArgumentDiagnostic : ResolutionDiagnostic(INAPPLICABLE) {
    abstract val argument: FirExpression
}

class MixingNamedAndPositionArguments(override val argument: FirExpression) : InapplicableArgumentDiagnostic()

class InferredEmptyIntersectionDiagnostic(
    val incompatibleTypes: Collection<ConeKotlinType>,
    val causingTypes: Collection<ConeKotlinType>,
    val typeVariable: ConeTypeVariable,
    val kind: EmptyIntersectionTypeKind,
    val isError: Boolean
) : ResolutionDiagnostic(INAPPLICABLE)

class TooManyArguments(
    val argument: FirExpression,
    val function: FirFunction
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NamedArgumentNotAllowed(
    val argument: FirExpression,
    val function: FirFunction,
    val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class ArgumentPassedTwice(
    override val argument: FirNamedArgumentExpression,
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

class NameForAmbiguousParameter(
    val argument: FirNamedArgumentExpression
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

object InapplicableCandidate : ResolutionDiagnostic(INAPPLICABLE)

object UnsuccessfulCallableReferenceAtom : ResolutionDiagnostic(INAPPLICABLE)

object ErrorTypeInArguments : ResolutionDiagnostic(INAPPLICABLE)

object HiddenCandidate : ResolutionDiagnostic(HIDDEN)

object VisibilityError : ResolutionDiagnostic(K2_VISIBILITY_ERROR)

object SetterVisibilityError : ResolutionDiagnostic(K2_VISIBILITY_ERROR)

object ResolvedWithLowPriority : ResolutionDiagnostic(RESOLVED_LOW_PRIORITY)

object ResolvedWithSynthetic : ResolutionDiagnostic(K2_SYNTHETIC_RESOLVED)

class InapplicableWrongReceiver(
    val expectedType: ConeKotlinType? = null,
    val actualType: ConeKotlinType? = null,
) : ResolutionDiagnostic(INAPPLICABLE_WRONG_RECEIVER)

class DynamicReceiverExpectedButWasNonDynamic(
    val actualType: ConeKotlinType,
) : ResolutionDiagnostic(INAPPLICABLE_WRONG_RECEIVER)

object NoCompanionObject : ResolutionDiagnostic(K2_NO_COMPANION_OBJECT)

class UnsafeCall(val actualType: ConeKotlinType) : ResolutionDiagnostic(UNSAFE_CALL)

object LowerPriorityToPreserveCompatibilityDiagnostic : ResolutionDiagnostic(RESOLVED_NEED_PRESERVE_COMPATIBILITY)

object LowerPriorityForDynamic : ResolutionDiagnostic(RESOLVED_LOW_PRIORITY)

object CandidateChosenUsingOverloadResolutionByLambdaAnnotation : ResolutionDiagnostic(RESOLVED)

class UnstableSmartCast(val argument: FirSmartCastExpression, val targetType: ConeKotlinType, val isCastToNotNull: Boolean) :
    ResolutionDiagnostic(UNSTABLE_SMARTCAST)

class ArgumentTypeMismatch(
    val expectedType: ConeKotlinType,
    val actualType: ConeKotlinType,
    val argument: FirExpression,
    val isMismatchDueToNullability: Boolean,
) : ResolutionDiagnostic(INAPPLICABLE)

class NullForNotNullType(
    val argument: FirExpression, val expectedType: ConeKotlinType
) : ResolutionDiagnostic(INAPPLICABLE)

class ManyLambdaExpressionArguments(
    val argument: FirExpression
) : ResolutionDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class InfixCallOfNonInfixFunction(val function: FirNamedFunctionSymbol) : ResolutionDiagnostic(CONVENTION_ERROR)
class OperatorCallOfNonOperatorFunction(val function: FirNamedFunctionSymbol) : ResolutionDiagnostic(CONVENTION_ERROR)
class OperatorCallOfConstructor(val constructor: FirConstructorSymbol) : ResolutionDiagnostic(CONVENTION_ERROR)

class InferenceError(val constraintError: ConstraintSystemError) : ResolutionDiagnostic(constraintError.applicability)
class Unsupported(val message: String, val source: KtSourceElement?) : ResolutionDiagnostic(K2_UNSUPPORTED)

class PropertyAsOperator(val propertySymbol: FirPropertySymbol) : ResolutionDiagnostic(K2_PROPERTY_AS_OPERATOR)

class DslScopeViolation(val calleeSymbol: FirBasedSymbol<*>) : ResolutionDiagnostic(RESOLVED_WITH_ERROR)

class MultipleContextReceiversApplicableForExtensionReceivers : ResolutionDiagnostic(INAPPLICABLE)

object NoReceiverAllowed : ResolutionDiagnostic(INAPPLICABLE)

class NoApplicableValueForContextReceiver(
    val expectedContextReceiverType: ConeKotlinType
) : ResolutionDiagnostic(INAPPLICABLE)

object UnsupportedContextualDeclarationCall : ResolutionDiagnostic(INAPPLICABLE)

class AmbiguousValuesForContextReceiverParameter(
    val expectedContextReceiverType: ConeKotlinType,
) : ResolutionDiagnostic(INAPPLICABLE)

object ResolutionResultOverridesOtherToPreserveCompatibility : ResolutionDiagnostic(RESOLVED)

object AdaptedCallableReferenceIsUsedWithReflection : ResolutionDiagnostic(RESOLVED_WITH_ERROR)

object TypeParameterAsExpression : ResolutionDiagnostic(INAPPLICABLE)

class TypeVariableAsExplicitReceiver(
    val explicitReceiver: FirExpression,
    val typeParameter: FirTypeParameter,
) : ResolutionDiagnostic(RESOLVED_WITH_ERROR)

object CallToDeprecatedOverrideOfHidden : ResolutionDiagnostic(RESOLVED)