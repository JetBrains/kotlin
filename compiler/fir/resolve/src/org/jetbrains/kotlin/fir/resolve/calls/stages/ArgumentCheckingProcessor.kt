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
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSink
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeVariableForLambdaParameterType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeVariableForLambdaReturnType
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeReceiverConstraintPosition
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.Name
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
        atom: ConeResolutionAtom,
        expectedType: ConeKotlinType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean
    ) {
        val argumentContext = ArgumentContext(candidate, candidate.csBuilder, expectedType, sink, context, isReceiver, isDispatch)
        argumentContext.resolveArgumentExpression(atom)
    }

    fun resolvePlainArgumentType(
        candidate: Candidate,
        atom: ConeResolutionAtom,
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        sourceForReceiver: KtSourceElement? = null,
    ) {
        val argumentContext = ArgumentContext(candidate, candidate.csBuilder, expectedType, sink, context, isReceiver, isDispatch)
        argumentContext.resolvePlainArgumentType(atom.expression, argumentType, sourceForReceiver = sourceForReceiver)
    }

    fun createResolvedLambdaAtomDuringCompletion(
        candidate: Candidate,
        csBuilder: ConstraintSystemBuilder,
        atom: ConeResolutionAtomWithPostponedChild,
        expectedType: ConeKotlinType?,
        context: ResolutionContext,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?
    ): ConeResolvedLambdaAtom {
        val argumentContext = ArgumentContext(
            candidate, csBuilder, expectedType, sink = null,
            context, isReceiver = false, isDispatch = false
        )
        return argumentContext.createResolvedLambdaAtom(atom, duringCompletion = true, returnTypeVariable)
    }

    // -------------------------------------------- Real implementation --------------------------------------------

    private fun ArgumentContext.resolveArgumentExpression(atom: ConeResolutionAtom) {
        when (atom) { // todo
            is ConeResolutionAtomWithPostponedChild -> when (atom.expression) {
                is FirAnonymousFunctionExpression -> preprocessLambdaArgument(atom)
                is FirCallableReferenceAccess -> preprocessCallableReference(atom)
            }
            // todo probably, this should be moved somewhere to "completion", but IDK how, and IDK why, it's just my intuition
            is ConeCollectionLiteralResolutionAtom -> {
                val collectionLiteralArgumentContext = this@resolveArgumentExpression
                if (collectionLiteralArgumentContext.expectedType == null) {
                    return
                }
                val scopeSession = collectionLiteralArgumentContext.context.bodyResolveComponents.scopeSession
                val collectionLiteralElementType = getCollectionLiteralElementType(
                    collectionLiteralArgumentContext.expectedType,
                    session,
                    scopeSession
                )
                if (collectionLiteralElementType == null) {
                    if (listOfNothingConeType.isSubtypeOf(expectedType, session)) {
                        return // Just don't infer anything. The candidate is successful
                    }
                    reportDiagnostic(ExpectedTypeDoesntContainCompanionOperatorOfFunction(expectedType, atom.expression))
                    return
                }
                val ofFunction = resolveVarargOfMemberFunction(collectionLiteralArgumentContext.expectedType, session, scopeSession)!!
                val s = candidate.substitutor.substituteOrSelf(ofFunction.resolvedReturnType)
                resolvePlainExpressionArgument(atom.expression, argumentType = s)
                val collectionLiteralElementContext = collectionLiteralArgumentContext.copy(
                    expectedType = candidate.substitutor.substituteOrSelf(collectionLiteralElementType),
                )
                for (atom in atom.subAtoms) {
                    collectionLiteralElementContext.resolveArgumentExpression(atom)
                }
            }

            is ConeSimpleLeafResolutionAtom, is ConeAtomWithCandidate -> resolvePlainExpressionArgument(atom.expression)

            is ConePostponedResolvedAtom -> error("Unexpected type of atom: ${atom::class.java}")
            is ConeResolutionAtomWithSingleChild -> {
                when (atom.expression) {
                    // x?.bar() is desugared to `x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() }`
                    //
                    // If we have a safe-call as argument like in a call "foo(x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() })"
                    // we obtain argument type (and argument's constraint system) from "$not-null-receiver$.bar()" (argument.regularQualifiedAccess)
                    // and then add constraint: typeOf(`$not-null-receiver$.bar()`).makeNullable() <: EXPECTED_TYPE
                    // NB: argument.regularQualifiedAccess is either a call or a qualified access
                    is FirSafeCallExpression -> when (val selectorAtom = atom.subAtom) {
                        // Assignment
                        null -> checkApplicabilityForArgumentType(
                            atom.expression,
                            StandardClassIds.Unit.constructClassLikeType(emptyArray(), isMarkedNullable = false),
                            SimpleConstraintSystemConstraintPosition,
                        )
                        else -> resolvePlainExpressionArgument(
                            selectorAtom.expression,
                            useNullableArgumentType = true
                        )
                    }
                    is FirBlock -> when (val lastExpression = atom.subAtom) {
                        null -> {
                            val newContext = this.copy(isReceiver = false, isDispatch = false)
                            newContext.checkApplicabilityForArgumentType(
                                atom.expression,
                                atom.expression.resolvedType,
                                SimpleConstraintSystemConstraintPosition,
                            )
                        }
                        else -> resolveArgumentExpression(lastExpression)
                    }
                    else -> when (val subAtom = atom.subAtom) {
                        null -> resolvePlainExpressionArgument(atom.expression)
                        else -> resolveArgumentExpression(subAtom)
                    }
                }
            }
        }
    }

    private fun ArgumentContext.resolvePlainExpressionArgument(
        expression: FirExpression,
        argumentType: ConeKotlinType? = null,
        useNullableArgumentType: Boolean = false
    ) {
        if (expectedType == null) return
        resolvePlainArgumentType(expression, argumentType ?: expression.resolvedType, useNullableArgumentType)
    }

    private fun ArgumentContext.resolvePlainArgumentType(
        expression: FirExpression,
        argumentType: ConeKotlinType,
        useNullableArgumentType: Boolean = false,
        sourceForReceiver: KtSourceElement? = null,
    ) {
        val position = when {
            isReceiver -> ConeReceiverConstraintPosition(expression, sourceForReceiver)
            else -> ConeArgumentConstraintPosition(expression)
        }

        val capturedType = prepareCapturedType(argumentType, context.session)

        var argumentTypeForApplicabilityCheck = capturedType.applyIf(useNullableArgumentType) {
            withNullability(nullable = true, session.typeContext)
        }

        // If the argument is of functional type and the expected type is a suspend function type, we need to do "suspend conversion."
        if (expectedType != null) {
            context.typeContext.argumentTypeWithCustomConversion(
                session = session,
                expectedType = expectedType,
                argumentType = argumentTypeForApplicabilityCheck,
            )?.let {
                argumentTypeForApplicabilityCheck = it
                candidate.substitutor.substituteOrSelf(argumentTypeForApplicabilityCheck) // todo drop unused expression?
                candidate.usesFunctionConversion = true
            }
        }

        checkApplicabilityForArgumentType(expression, argumentTypeForApplicabilityCheck, position)
    }

    private fun ArgumentContext.checkApplicabilityForArgumentType(
        expression: FirExpression,
        argumentTypeBeforeCapturing: ConeKotlinType,
        position: ConstraintPosition,
    ) {
        if (expectedType == null) return

        // todo. Fix behavior. argumentTypeBeforeCapturing is Array<Int>
        val argumentType = captureFromTypeParameterUpperBoundIfNeeded(argumentTypeBeforeCapturing, expectedType, session)

        fun subtypeError(actualExpectedType: ConeKotlinType): ResolutionDiagnostic {
            if (expression.isNullLiteral && !actualExpectedType.isMarkedOrFlexiblyNullable) {
                return NullForNotNullType(expression, actualExpectedType)
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
                        return ConeTypeParameterTypeImpl(originalTypeParameter, type.isMarkedNullable, type.attributes)
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
                expression,
                // Reaching here means argument types mismatch, and we want to record whether it's due to the nullability by checking a subtype
                // relation with nullable expected type.
                session.typeContext.isTypeMismatchDueToNullability(argumentType, actualExpectedType)
            )
        }

        when {
            isReceiver && isDispatch -> {
                if (!expectedType.isMarkedOrFlexiblyNullable && argumentType.isMarkedNullable) {
                    reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
                }
            }

            isReceiver && expectedType is ConeDynamicType && argumentType !is ConeDynamicType -> {
                reportDiagnostic(DynamicReceiverExpectedButWasNonDynamic(argumentType))
            }

            else -> {
                if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) return // no errors

                val smartcastExpression = expression as? FirSmartCastExpression
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

                val nullableExpectedType = expectedType.withNullability(nullable = true, session.typeContext)

                if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
                    reportDiagnostic(InapplicableNullableReceiver(argumentType))
                } else {
                    csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
                    reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
                }
            }
        }
    }

    private fun ArgumentContext.preprocessCallableReference(atom: ConeResolutionAtomWithPostponedChild) {
        val expression = atom.callableReferenceExpression
        val lhs = context.bodyResolveComponents.doubleColonExpressionResolver.resolveDoubleColonLHS(expression)
        val postponedAtom = ConeResolvedCallableReferenceAtom(expression, expectedType, lhs, context.session)
        atom.subAtom = postponedAtom
        candidate.addPostponedAtom(postponedAtom)
    }

    private fun ArgumentContext.preprocessLambdaArgument(atom: ConeResolutionAtomWithPostponedChild) {
        if (createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(atom)) {
            return
        }
        createResolvedLambdaAtom(atom, duringCompletion = false, returnTypeVariable = null)
    }

    /**
     * @return true in case [ConeLambdaWithTypeVariableAsExpectedTypeAtom] was created and set as
     * [ConeResolutionAtomWithPostponedChild.subAtom] of the [atom] and a postponed atom of the
     * [ArgumentCheckingProcessor.ArgumentContext.candidate].
     * In case of false result, this function works as pure (it does not change inference state).
     */
    private fun ArgumentContext.createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(
        atom: ConeResolutionAtomWithPostponedChild
    ): Boolean {
        if (expectedType == null || !csBuilder.isTypeVariable(expectedType)) return false
        val expectedTypeVariableWithConstraints = csBuilder.currentStorage()
            .notFixedTypeVariables[expectedType.typeConstructor(context.typeContext)]
            ?: return false

        val explicitTypeArgument = expectedTypeVariableWithConstraints.constraints.find {
            it.kind == ConstraintKind.EQUALITY && it.position.from is ConeExplicitTypeParameterConstraintPosition
        }?.type as ConeKotlinType?

        if (explicitTypeArgument != null && explicitTypeArgument.typeArguments.isEmpty()) {
            return false
        }
        ConeLambdaWithTypeVariableAsExpectedTypeAtom(atom.lambdaExpression, expectedType, candidate).also {
            candidate.addPostponedAtom(it)
            atom.subAtom = it
        }
        return true
    }

    private fun ArgumentContext.createResolvedLambdaAtom(
        atom: ConeResolutionAtomWithPostponedChild,
        duringCompletion: Boolean,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?
    ): ConeResolvedLambdaAtom {
        val expression = atom.lambdaExpression
        val anonymousFunction = expression.anonymousFunction

        val resolvedArgument = extractLambdaInfoFromFunctionType(
            expectedType,
            expression,
            anonymousFunction,
            returnTypeVariable,
            context.bodyResolveComponents,
            allowCoercionToExtensionReceiver = duringCompletion,
            sourceForFunctionExpression = expression.source,
        ) ?: extractLambdaInfo(expression, sourceForFunctionExpression = expression.source)

        atom.subAtom = resolvedArgument
        candidate.addPostponedAtom(resolvedArgument)

        if (expectedType != null) {
            val parameters = resolvedArgument.parameterTypes
            val functionTypeKind = context.session.functionTypeService.extractSingleSpecialKindForFunction(anonymousFunction.symbol)
                ?: resolvedArgument.expectedFunctionTypeKind?.nonReflectKind()
                ?: FunctionTypeKind.Function
            val lambdaType = createFunctionType(
                functionTypeKind,
                parameters,
                resolvedArgument.receiverType,
                resolvedArgument.returnType,
                contextParameters = resolvedArgument.contextParameterTypes,
            )

            val position = ConeArgumentConstraintPosition(resolvedArgument.anonymousFunction)
            if (duringCompletion) {
                csBuilder.addSubtypeConstraint(lambdaType, expectedType, position)
            } else {
                if (!csBuilder.addSubtypeConstraintIfCompatible(lambdaType, expectedType, position)) {
                    reportDiagnostic(
                        ArgumentTypeMismatch(
                            expectedType, lambdaType, expression,
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

        val contextParameters = lambda.contextParameters.mapIndexed { i, it ->
            it.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?: defaultType
                ?: ConeTypeVariableForLambdaParameterType("_C$i").apply { csBuilder.registerVariable(this) }.defaultType
        }

        val newTypeVariableUsed = returnType == typeVariable.defaultType
        if (newTypeVariableUsed) {
            csBuilder.registerVariable(typeVariable)
        }

        return ConeResolvedLambdaAtom(
            argument,
            expectedType,
            expectedFunctionTypeKind = lambda.typeRef.coneTypeSafe<ConeKotlinType>()?.lowerBoundIfFlexible()?.functionTypeKind(session),
            receiverType,
            contextParameters,
            parameters,
            returnType,
            typeVariable.takeIf { newTypeVariableUsed },
            coerceFirstParameterToExtensionReceiver = false,
            sourceForFunctionExpression,
        )
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

        val typeArguments = functionType.typeArguments.map { it.type ?: session.builtinTypes.nullableAnyType.coneType }.ifEmpty { return null }
        return createFunctionType(
            kind = expectedTypeKind,
            parameters = typeArguments.subList(0, typeArguments.lastIndex),
            receiverType = null,
            rawReturnType = typeArguments.last(),
        )
    }

    private val ConeResolutionAtomWithPostponedChild.lambdaExpression: FirAnonymousFunctionExpression
        get() = expression as? FirAnonymousFunctionExpression ?: error("Expected anonymous function expression")

    private val ConeResolutionAtomWithPostponedChild.callableReferenceExpression: FirCallableReferenceAccess
        get() = expression as? FirCallableReferenceAccess ?: error("Expected callable reference")
}

fun resolveVarargOfMemberFunction(expectedType: ConeKotlinType, session: FirSession, scopeSession: ScopeSession): FirNamedFunctionSymbol? {
    val symbol = expectedType.toSymbol(session)
    val clazz = symbol?.fir as? FirRegularClass ?: return null
    // run {
    //     error("WTF2 $expectedType ${symbol?.fir} ${symbol?.fir?.let { it::class }}")
    // }
    val companion = clazz.companionObjectSymbol
    val scope = companion?.unsubstitutedScope(
        session,
        scopeSession = scopeSession,
        withForcedTypeCalculator = false,
        FirResolvePhase.BODY_RESOLVE
    )
    return scope?.getFunctions(Name.identifier("of"))
        ?.singleOrNull<FirNamedFunctionSymbol> { it.valueParameterSymbols.singleOrNull()?.isVararg == true }
}

fun getCollectionLiteralElementType(ofFunction: FirNamedFunctionSymbol): ConeKotlinType {
    val collectionElementType = ofFunction.valueParameterSymbols.single().resolvedReturnType.arrayElementType()!!
    return collectionElementType
}

// todo drop after adding of functions in stdib
fun getHardcodedCollectionLiteralElementType(expectedType: ConeKotlinType): ConeKotlinType? {
    if (expectedType.isArrayType) {
        return expectedType.arrayElementType()
    }
    if (expectedType.isList || expectedType.isMutableList || expectedType.isSet || expectedType.isMutableSet) {
        return expectedType.typeArguments.firstOrNull()?.type
    }
    return null
}

fun getCollectionLiteralElementType(
    expectedType: ConeKotlinType,
    session: FirSession,
    scopeSession: ScopeSession,
): ConeKotlinType? {
    getHardcodedCollectionLiteralElementType(expectedType)?.let { return it }
    val ofFunction = resolveVarargOfMemberFunction(expectedType, session, scopeSession) ?: return null
    return getCollectionLiteralElementType(ofFunction)
}

internal val listOfNothingConeType = ConeClassLikeTypeImpl(
    StandardClassIds.List.toLookupTag(),
    arrayOf(ConeClassLikeTypeImpl(StandardClassIds.Nothing.toLookupTag(), arrayOf(), isMarkedNullable = false)),
    isMarkedNullable = false
)
