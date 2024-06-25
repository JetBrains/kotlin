/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSink
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeVariableForLambdaParameterType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeVariableForLambdaReturnType
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeReceiverConstraintPosition
import org.jetbrains.kotlin.fir.lastExpression
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal object ArgumentCheckingProcessor {
    private data class ArgumentContext(
        val candidate: Candidate,
        val csBuilder: ConstraintSystemBuilder,
        val expectedType: ConeKotlinType?,
        val sink: CheckerSink?,
        val context: ResolutionContext,
        val isReceiver: Boolean,
        val isDispatch: Boolean,
    ) {
        val session: FirSession
            get() = context.session

        fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
            sink?.reportDiagnostic(diagnostic)
        }
    }

    // -------------------------------------------- Public API --------------------------------------------

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

    fun createResolvedLambdaAtomDuringCompletion(
        candidate: Candidate,
        csBuilder: ConstraintSystemBuilder,
        argument: FirAnonymousFunctionExpression,
        expectedType: ConeKotlinType?,
        context: ResolutionContext,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?
    ): ConeResolvedLambdaAtom {
        val argumentContext = ArgumentContext(
            candidate, csBuilder, expectedType, sink = null,
            context, isReceiver = false, isDispatch = false
        )
        return argumentContext.createResolvedLambdaAtom(argument, duringCompletion = true, returnTypeVariable)
    }

    // -------------------------------------------- Real implementation --------------------------------------------

    private fun ArgumentContext.resolveArgumentExpression(argument: FirExpression) {
        when (argument) {
            // x?.bar() is desugared to `x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() }`
            //
            // If we have a safe-call as argument like in a call "foo(x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() })"
            // we obtain argument type (and argument's constraint system) from "$not-null-receiver$.bar()" (argument.regularQualifiedAccess)
            // and then add constraint: typeOf(`$not-null-receiver$.bar()`).makeNullable() <: EXPECTED_TYPE
            // NB: argument.regularQualifiedAccess is either a call or a qualified access
            is FirSafeCallExpression -> when (val nestedQualifier = (argument.selector as? FirExpression)?.unwrapSmartcastExpression()) {
                is FirQualifiedAccessExpression -> resolvePlainExpressionArgument(
                    nestedQualifier,
                    useNullableArgumentType = true
                )
                // Assignment
                else -> checkApplicabilityForArgumentType(
                    argument,
                    StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false),
                    SimpleConstraintSystemConstraintPosition,
                )
            }
            is FirCallableReferenceAccess -> when (argument.calleeReference) {
                is FirResolvedNamedReference -> resolvePlainExpressionArgument(argument)
                else -> preprocessCallableReference(argument)
            }

            is FirAnonymousFunctionExpression -> preprocessLambdaArgument(argument)
            is FirWrappedArgumentExpression -> resolveArgumentExpression(argument.expression)
            is FirBlock -> resolveBlockArgument(argument)
            is FirErrorExpression -> when (val wrappedExpression = argument.expression) {
                null -> resolvePlainExpressionArgument(argument)
                else -> resolveArgumentExpression(wrappedExpression)
            }
            else -> resolvePlainExpressionArgument(argument)
        }
    }

    private fun ArgumentContext.resolveBlockArgument(block: FirBlock) {
        val lastExpression = block.lastExpression
        if (lastExpression == null) {
            val newContext = this.copy(isReceiver = false, isDispatch = false)
            newContext.checkApplicabilityForArgumentType(
                block,
                block.resolvedType,
                SimpleConstraintSystemConstraintPosition,
            )
            return
        }
        resolveArgumentExpression(lastExpression)
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

        var argumentTypeForApplicabilityCheck = capturedType.applyIf(useNullableArgumentType) {
            withNullability(ConeNullability.NULLABLE, session.typeContext)
        }

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
                    if (originalTypeParameter != null) {
                        return ConeTypeParameterTypeImpl(originalTypeParameter, type.isNullable, type.attributes)
                    }
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

        when {
            isReceiver && isDispatch -> {
                if (!expectedType.isNullable && argumentType.isMarkedNullable) {
                    reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
                }
            }

            isReceiver && expectedType is ConeDynamicType && argumentType !is ConeDynamicType -> {
                reportDiagnostic(DynamicReceiverExpectedButWasNonDynamic(argumentType))
            }

            else -> {
                if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) return // no errors

                val smartcastExpression = argument as? FirSmartCastExpression
                if (smartcastExpression != null && !smartcastExpression.isStable) {
                    val unstableType = smartcastExpression.smartcastType.coneType
                    if (csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
                        reportDiagnostic(
                            UnstableSmartCast(
                                smartcastExpression,
                                expectedType,
                                isCastToNotNull = session.typeContext.isTypeMismatchDueToNullability(argumentType, expectedType),
                                isImplicitInvokeReceiver = false,
                            )
                        )
                        return
                    }
                }

                if (!isReceiver) {
                    reportDiagnostic(subtypeError(expectedType))
                    return
                }

                val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE, session.typeContext)

                if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
                    reportDiagnostic(UnsafeCall(argumentType))
                } else {
                    csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
                    reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
                }
            }
        }
    }

    private fun ArgumentContext.preprocessCallableReference(argument: FirCallableReferenceAccess) {
        val lhs = context.bodyResolveComponents.doubleColonExpressionResolver.resolveDoubleColonLHS(argument)
        candidate.addPostponedAtom(ConeResolvedCallableReferenceAtom(argument, expectedType, lhs, context.session))
    }

    private fun ArgumentContext.preprocessLambdaArgument(argument: FirAnonymousFunctionExpression): ConePostponedResolvedAtom {
        createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(argument)?.let { return it }
        return createResolvedLambdaAtom(argument, duringCompletion = false, returnTypeVariable = null)
    }

    private fun ArgumentContext.createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(
        argument: FirAnonymousFunctionExpression
    ): ConeLambdaWithTypeVariableAsExpectedTypeAtom? {
        if (expectedType == null || !csBuilder.isTypeVariable(expectedType)) return null
        val expectedTypeVariableWithConstraints = csBuilder.currentStorage()
            .notFixedTypeVariables[expectedType.typeConstructor(context.typeContext)]
            ?: return null

        val explicitTypeArgument = expectedTypeVariableWithConstraints.constraints.find {
            it.kind == ConstraintKind.EQUALITY && it.position.from is ConeExplicitTypeParameterConstraintPosition
        }?.type as ConeKotlinType?

        return runIf(explicitTypeArgument == null || explicitTypeArgument.typeArguments.isNotEmpty()) {
            ConeLambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType, candidate).also {
                candidate.addPostponedAtom(it)
            }
        }
    }

    private fun ArgumentContext.createResolvedLambdaAtom(
        argument: FirAnonymousFunctionExpression,
        duringCompletion: Boolean,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?
    ): ConeResolvedLambdaAtom {
        val anonymousFunction = argument.anonymousFunction

        val resolvedArgument = extractLambdaInfoFromFunctionType(
            expectedType,
            argument,
            argument.anonymousFunction,
            returnTypeVariable,
            context.bodyResolveComponents,
            candidate,
            allowCoercionToExtensionReceiver = duringCompletion,
            sourceForFunctionExpression = argument.source,
        ) ?: extractLambdaInfo(argument, sourceForFunctionExpression = argument.source)

        if (expectedType != null) {
            val parameters = resolvedArgument.parameters
            val functionTypeKind = context.session.functionTypeService.extractSingleSpecialKindForFunction(anonymousFunction.symbol)
                ?: resolvedArgument.expectedFunctionTypeKind?.nonReflectKind()
                ?: FunctionTypeKind.Function
            val lambdaType = createFunctionType(
                functionTypeKind,
                parameters,
                resolvedArgument.receiver,
                resolvedArgument.returnType,
                contextReceivers = resolvedArgument.contextReceivers,
            )

            val position = ConeArgumentConstraintPosition(resolvedArgument.fir)
            if (duringCompletion) {
                csBuilder.addSubtypeConstraint(lambdaType, expectedType, position)
            } else {
                if (!csBuilder.addSubtypeConstraintIfCompatible(lambdaType, expectedType, position)) {
                    reportDiagnostic(
                        ArgumentTypeMismatch(
                            expectedType, lambdaType, argument,
                            context.session.typeContext.isTypeMismatchDueToNullability(lambdaType, expectedType)
                        )
                    )
                }
            }
        }

        return resolvedArgument
    }

    private fun ArgumentContext.extractLambdaInfo(
        argument: FirAnonymousFunctionExpression,
        sourceForFunctionExpression: KtSourceElement?,
    ): ConeResolvedLambdaAtom {
        require(expectedType?.lowerBoundIfFlexible()?.functionTypeKind(session) == null) {
            "Currently, we only extract lambda info from its shape when expected type is not function, but $expectedType"
        }
        val lambda = argument.anonymousFunction
        val typeVariable = ConeTypeVariableForLambdaReturnType(lambda, "_L")

        val receiverType = lambda.receiverType
        val returnType = lambda.returnType ?: typeVariable.defaultType

        val defaultType = runIf(candidate.symbol.origin == FirDeclarationOrigin.DynamicScope) { ConeDynamicType.create(session) }

        val parameters = lambda.valueParameters.mapIndexed { i, it ->
            it.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?: defaultType
                ?: ConeTypeVariableForLambdaParameterType("_P$i").apply { csBuilder.registerVariable(this) }.defaultType
        }

        val contextReceivers = lambda.contextReceivers.mapIndexed { i, it ->
            it.typeRef.coneTypeSafe<ConeKotlinType>()
                ?: defaultType
                ?: ConeTypeVariableForLambdaParameterType("_C$i").apply { csBuilder.registerVariable(this) }.defaultType
        }

        val newTypeVariableUsed = returnType == typeVariable.defaultType
        if (newTypeVariableUsed) {
            csBuilder.registerVariable(typeVariable)
        }

        return ConeResolvedLambdaAtom(
            lambda,
            argument,
            expectedType,
            expectedFunctionTypeKind = lambda.typeRef.coneTypeSafe<ConeKotlinType>()?.lowerBoundIfFlexible()?.functionTypeKind(session),
            receiverType,
            contextReceivers,
            parameters,
            returnType,
            typeVariable.takeIf { newTypeVariableUsed },
            coerceFirstParameterToExtensionReceiver = false,
            sourceForFunctionExpression,
        ).also {
            candidate.addPostponedAtom(it)
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
