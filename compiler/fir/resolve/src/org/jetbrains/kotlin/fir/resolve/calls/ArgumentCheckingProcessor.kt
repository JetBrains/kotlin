/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeReceiverConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.preprocessCallableReference
import org.jetbrains.kotlin.fir.resolve.inference.preprocessLambdaArgument
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.TypeSystemCommonSuperTypesContext

val SAM_LOOKUP_NAME: Name = Name.special("<SAM-CONSTRUCTOR>")

internal object ArgumentCheckingProcessor {
    private data class ArgumentContext(
        val candidate: Candidate,
        val csBuilder: ConstraintSystemBuilder,
        val expectedType: ConeKotlinType?,
        val sink: CheckerSink,
        val context: ResolutionContext,
        val isReceiver: Boolean,
        val isDispatch: Boolean,
    ) {
        val session: FirSession
            get() = context.session
    }

    fun resolveArgumentExpression(
        candidate: Candidate,
        argument: FirExpression,
        expectedType: ConeKotlinType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean
    ) {
        val argumentContext = ArgumentContext(candidate, candidate.csBuilder, expectedType, sink, context, isReceiver, isDispatch)
        argumentContext.resolveArgumentExpression(argument)
    }

    fun resolvePlainArgumentType(
        candidate: Candidate,
        argument: FirExpression,
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        sourceForReceiver: KtSourceElement? = null,
    ) {
        val argumentContext = ArgumentContext(candidate, candidate.csBuilder, expectedType, sink, context, isReceiver, isDispatch)
        argumentContext.resolvePlainArgumentType(argument, argumentType, sourceForReceiver = sourceForReceiver)
    }

    private fun ArgumentContext.resolveArgumentExpression(argument: FirExpression) {
        when (argument) {
            // x?.bar() is desugared to `x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() }`
            //
            // If we have a safe-call as argument like in a call "foo(x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() })"
            // we obtain argument type (and argument's constraint system) from "$not-null-receiver$.bar()" (argument.regularQualifiedAccess)
            // and then add constraint: typeOf(`$not-null-receiver$.bar()`).makeNullable() <: EXPECTED_TYPE
            // NB: argument.regularQualifiedAccess is either a call or a qualified access
            is FirSafeCallExpression -> {
                val nestedQualifier = (argument.selector as? FirExpression)?.unwrapSmartcastExpression()
                if (nestedQualifier is FirQualifiedAccessExpression) {
                    resolvePlainExpressionArgument(
                        nestedQualifier,
                        useNullableArgumentType = true
                    )
                } else {
                    // Assignment
                    checkApplicabilityForArgumentType(
                        argument,
                        StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false),
                        SimpleConstraintSystemConstraintPosition,
                    )
                }
            }
            is FirCallableReferenceAccess ->
                if (argument.calleeReference is FirResolvedNamedReference)
                    resolvePlainExpressionArgument(argument)
                else
                    candidate.preprocessCallableReference(argument, expectedType, context)
            is FirAnonymousFunctionExpression -> candidate.preprocessLambdaArgument(csBuilder, argument, expectedType, context, sink)
            is FirWrappedArgumentExpression -> resolveArgumentExpression(argument.expression)
            is FirBlock -> resolveBlockArgument(argument)
            is FirErrorExpression -> {
                val wrappedExpression = argument.expression
                if (wrappedExpression != null) {
                    resolveArgumentExpression(wrappedExpression)
                } else {
                    resolvePlainExpressionArgument(argument)
                }
            }
            else -> resolvePlainExpressionArgument(argument)
        }
    }

    private fun ArgumentContext.resolveBlockArgument(block: FirBlock) {
        val returnArguments = block.returnExpressions()
        if (returnArguments.isEmpty()) {
            val newContext = this.copy(isReceiver = false, isDispatch = false)
            newContext.checkApplicabilityForArgumentType(
                block,
                block.resolvedType,
                SimpleConstraintSystemConstraintPosition,
            )
            return
        }
        for (argument in returnArguments) {
            resolveArgumentExpression(argument)
        }
    }

    private fun ArgumentContext.resolvePlainExpressionArgument(
        argument: FirExpression,
        useNullableArgumentType: Boolean = false
    ) {
        if (expectedType == null) return

        // TODO: this check should be eliminated, KT-65085
        if (argument is FirArrayLiteral && !argument.isResolved) return

        val argumentType = argument.resolvedType
        resolvePlainArgumentType(argument, argumentType, useNullableArgumentType)
    }

    private fun ArgumentContext.resolvePlainArgumentType(
        argument: FirExpression,
        argumentType: ConeKotlinType,
        useNullableArgumentType: Boolean = false,
        sourceForReceiver: KtSourceElement? = null,
    ) {
        val position = when {
            isReceiver -> ConeReceiverConstraintPosition(argument, sourceForReceiver)
            else -> ConeArgumentConstraintPosition(argument)
        }

        val capturedType = prepareCapturedType(argumentType, context)

        var argumentTypeForApplicabilityCheck =
            if (useNullableArgumentType)
                capturedType.withNullability(ConeNullability.NULLABLE, session.typeContext)
            else
                capturedType

        // If the argument is of functional type and the expected type is a suspend function type, we need to do "suspend conversion."
        if (expectedType != null) {
            context.typeContext.argumentTypeWithCustomConversion(
                session = session,
                expectedType = expectedType,
                argumentType = argumentTypeForApplicabilityCheck,
            )?.let {
                argumentTypeForApplicabilityCheck = it
                candidate.substitutor.substituteOrSelf(argumentTypeForApplicabilityCheck)
                candidate.usesFunctionConversion = true
            }
        }

        checkApplicabilityForArgumentType(argument, argumentTypeForApplicabilityCheck, position)
    }

    private fun ArgumentContext.checkApplicabilityForArgumentType(
        argument: FirExpression,
        argumentTypeBeforeCapturing: ConeKotlinType,
        position: ConstraintPosition,
    ) {
        if (expectedType == null) return

        val argumentType = captureFromTypeParameterUpperBoundIfNeeded(argumentTypeBeforeCapturing, expectedType, session)

        fun subtypeError(actualExpectedType: ConeKotlinType): ResolutionDiagnostic {
            if (argument.isNullLiteral && actualExpectedType.nullability == ConeNullability.NOT_NULL) {
                return NullForNotNullType(argument, actualExpectedType)
            }

            fun tryGetConeTypeThatCompatibleWithKtType(type: ConeKotlinType): ConeKotlinType {
                if (type is ConeTypeVariableType) {
                    val lookupTag = type.typeConstructor

                    val constraints = csBuilder.currentStorage().notFixedTypeVariables[lookupTag]?.constraints
                    val constraintTypes = constraints?.mapNotNull { it.type as? ConeKotlinType }
                    if (!constraintTypes.isNullOrEmpty()) {
                        return ConeTypeIntersector.intersectTypes(session.typeContext, constraintTypes)
                    }

                    val originalTypeParameter = lookupTag.originalTypeParameter as? ConeTypeParameterLookupTag
                    if (originalTypeParameter != null)
                        return ConeTypeParameterTypeImpl(originalTypeParameter, type.isNullable, type.attributes)
                } else if (type is ConeIntegerLiteralType) {
                    return type.possibleTypes.firstOrNull() ?: type
                }

                return type
            }

            if (argumentType is ConeErrorType || actualExpectedType is ConeErrorType) return ErrorTypeInArguments

            val preparedExpectedType = tryGetConeTypeThatCompatibleWithKtType(actualExpectedType)
            val preparedActualType = tryGetConeTypeThatCompatibleWithKtType(argumentType)
            return ArgumentTypeMismatch(
                preparedExpectedType,
                preparedActualType,
                argument,
                // Reaching here means argument types mismatch, and we want to record whether it's due to the nullability by checking a subtype
                // relation with nullable expected type.
                session.typeContext.isTypeMismatchDueToNullability(argumentType, actualExpectedType)
            )
        }

        if (isReceiver && isDispatch) {
            if (!expectedType.isNullable && argumentType.isMarkedNullable) {
                sink.reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
            }
            return
        }

        if (isReceiver && expectedType is ConeDynamicType && argumentType !is ConeDynamicType) {
            sink.reportDiagnostic(DynamicReceiverExpectedButWasNonDynamic(argumentType))
            return
        }

        if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
            val smartcastExpression = argument as? FirSmartCastExpression
            if (smartcastExpression != null && !smartcastExpression.isStable) {
                val unstableType = smartcastExpression.smartcastType.coneType
                if (csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
                    sink.reportDiagnostic(
                        UnstableSmartCast(
                            smartcastExpression,
                            expectedType,
                            session.typeContext.isTypeMismatchDueToNullability(
                                argumentType,
                                expectedType
                            ),
                            isImplicitInvokeReceiver = false,
                        )
                    )
                    return
                }
            }

            if (!isReceiver) {
                sink.reportDiagnostic(subtypeError(expectedType))
                return
            }

            val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE, session.typeContext)

            if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
                sink.reportDiagnostic(UnsafeCall(argumentType))
            } else {
                csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
                sink.reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
            }
        }
    }

    private fun ConeInferenceContext.argumentTypeWithCustomConversion(
        session: FirSession,
        expectedType: ConeKotlinType,
        argumentType: ConeKotlinType,
    ): ConeKotlinType? {
        // Expect the expected type to be a not regular functional type (e.g. suspend or custom)
        val expectedTypeKind = expectedType.functionTypeKind(session) ?: return null
        if (expectedTypeKind.isBasicFunctionOrKFunction) return null

        // We want to check the argument type against non-suspend functional type.
        val expectedFunctionType = expectedType.customFunctionTypeToSimpleFunctionType(session)

        val argumentTypeWithInvoke = argumentType.findSubtypeOfBasicFunctionType(session, expectedFunctionType) ?: return null
        val functionType = argumentTypeWithInvoke.unwrapLowerBound()
            .fastCorrespondingSupertypes(expectedFunctionType.typeConstructor())
            ?.firstOrNull() as? ConeKotlinType ?: return null

        val typeArguments = functionType.typeArguments.map { it.type ?: session.builtinTypes.nullableAnyType.type }.ifEmpty { return null }
        return createFunctionType(
            kind = expectedTypeKind,
            parameters = typeArguments.subList(0, typeArguments.lastIndex),
            receiverType = null,
            rawReturnType = typeArguments.last(),
        )
    }
}

internal fun prepareCapturedType(argumentType: ConeKotlinType, context: ResolutionContext): ConeKotlinType {
    if (argumentType.isRaw()) return argumentType
    return context.typeContext.captureFromExpression(argumentType.fullyExpandedType(context.session)) ?: argumentType
}

fun FirExpression.getExpectedType(
    session: FirSession,
    parameter: FirValueParameter/*, languageVersionSettings: LanguageVersionSettings*/
): ConeKotlinType {
    val shouldUnwrapVarargType = when (this) {
        is FirSpreadArgumentExpression, is FirNamedArgumentExpression -> false
        else -> parameter.isVararg
    }

    val expectedType = if (shouldUnwrapVarargType) {
        parameter.returnTypeRef.coneType.varargElementType()
    } else {
        parameter.returnTypeRef.coneType
    }
    if (!session.functionTypeService.hasExtensionKinds()) return expectedType
    return FunctionTypeKindSubstitutor(session).substituteOrSelf(expectedType)
}

/**
 * This class creates a type by recursively substituting function types of a given type if the function types have special function
 * type kinds.
 */
private class FunctionTypeKindSubstitutor(private val session: FirSession) : AbstractConeSubstitutor(session.typeContext) {
    /**
     * Returns a new type that applies the special function type kind to [type] if [type] has a special function type kind.
     */
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeClassLikeType) return null
        val classId = type.classId ?: return null
        return session.functionTypeService.extractSingleExtensionKindForDeserializedConeType(classId, type.customAnnotations)
            ?.let { functionTypeKind ->
                type.createFunctionTypeWithNewKind(session, functionTypeKind) {
                    // When `substituteType()` returns a non-null value, it does not recursively substitute type arguments,
                    // which is problematic for a nested function type kind like `@Composable () -> (@Composable -> Unit)`.
                    // To fix this issue, we manually substitute type arguments here.
                    this.mapIndexed { index, coneTypeProjection -> substituteArgument(coneTypeProjection, index) ?: coneTypeProjection }
                        .toTypedArray()
                }
            }
    }
}

/**
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val xr = bar(x)
 * }
 * Here we try to capture from upper bound from type parameter.
 * We replace type of `x` to `Inv<out Int>`(we chose supertype which contains supertype with expectedTypeConstructor) and capture from this type.
 * It is correct, because it is like this code:
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val inv: Inv<out Int> = x
 *      val xr = bar(inv)
 * }
 *
 */
internal fun captureFromTypeParameterUpperBoundIfNeeded(
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    session: FirSession
): ConeKotlinType {
    val expectedTypeClassId = expectedType.upperBoundIfFlexible().classId ?: return argumentType
    val simplifiedArgumentType = argumentType.lowerBoundIfFlexible() as? ConeTypeParameterType ?: return argumentType
    val context = session.typeContext

    val chosenSupertype = simplifiedArgumentType.collectUpperBounds()
        .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) } ?: return argumentType

    val capturedType = context.captureFromExpression(chosenSupertype) ?: return argumentType
    return if (argumentType is ConeDefinitelyNotNullType) {
        ConeDefinitelyNotNullType.create(capturedType, session.typeContext) ?: capturedType
    } else {
        capturedType
    }
}

private fun ConeKotlinType.hasSupertypeWithGivenClassId(classId: ClassId, context: TypeSystemCommonSuperTypesContext): Boolean {
    return with(context) {
        anySuperTypeConstructor {
            val typeConstructor = it.typeConstructor()
            typeConstructor is ConeClassLikeLookupTag && typeConstructor.classId == classId
        }
    }
}
