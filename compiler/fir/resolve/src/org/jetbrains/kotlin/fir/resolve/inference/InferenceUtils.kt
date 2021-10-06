/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
private fun ConeKotlinType.classId(session: FirSession): ClassId? {
    contract {
        returns(true) implies (this@classId is ConeClassLikeType)
    }
    if (this !is ConeClassLikeType) return null
    return fullyExpandedType(session).lookupTag.classId
}

fun ConeKotlinType.isKMutableProperty(session: FirSession): Boolean {
    val classId = classId(session) ?: return false
    return classId.packageFqName == StandardClassIds.BASE_REFLECT_PACKAGE &&
            classId.shortClassName.identifier.startsWith("KMutableProperty")
}

fun ConeKotlinType.functionClassKind(session: FirSession): FunctionClassKind? {
    return classId(session)?.toFunctionClassKind()
}

private fun ClassId.toFunctionClassKind(): FunctionClassKind? {
    return FunctionClassKind.byClassNamePrefix(packageFqName, relativeClassName.asString())
}

// Function, SuspendFunction, KFunction, KSuspendFunction
fun ConeKotlinType.isBuiltinFunctionalType(session: FirSession): Boolean {
    return functionClassKind(session) != null
}

// Function, SuspendFunction, KFunction, KSuspendFunction
fun ConeClassLikeLookupTag.isBuiltinFunctionalType(): Boolean {
    return classId.toFunctionClassKind() != null
}

inline fun ConeKotlinType.isFunctionalType(session: FirSession, predicate: (FunctionClassKind) -> Boolean): Boolean {
    val kind = functionClassKind(session) ?: return false
    return predicate(kind)
}

// Function
fun ConeKotlinType.isFunctionalType(session: FirSession): Boolean {
    return isFunctionalType(session) { it == FunctionClassKind.Function }
}

// SuspendFunction, KSuspendFunction
fun ConeKotlinType.isSuspendFunctionType(session: FirSession): Boolean {
    return isFunctionalType(session) { it.isSuspendType }
}

// KFunction, KSuspendFunction
fun ConeKotlinType.isKFunctionType(session: FirSession): Boolean {
    return isFunctionalType(session) { it.isReflectType }
}

fun ConeKotlinType.kFunctionTypeToFunctionType(session: FirSession): ConeClassLikeType {
    require(this.isKFunctionType(session))
    val kind =
        if (isSuspendFunctionType(session)) FunctionClassKind.SuspendFunction
        else FunctionClassKind.Function
    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size - 1))
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), typeArguments, isNullable = false)
}

fun ConeKotlinType.suspendFunctionTypeToFunctionType(session: FirSession): ConeClassLikeType {
    require(this.isSuspendFunctionType(session))
    val kind =
        if (isKFunctionType(session)) FunctionClassKind.KFunction
        else FunctionClassKind.Function
    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size - 1))
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), typeArguments, isNullable = false, attributes = attributes)
}

fun ConeKotlinType.suspendFunctionTypeToFunctionTypeWithContinuation(session: FirSession, continuationClassId: ClassId): ConeClassLikeType {
    require(this.isSuspendFunctionType(session))
    val kind =
        if (isKFunctionType(session)) FunctionClassKind.KFunction
        else FunctionClassKind.Function
    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size))
    return ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(functionalTypeId),
        typeArguments = (type.typeArguments.dropLast(1) + ConeClassLikeLookupTagImpl(continuationClassId).constructClassType(
            arrayOf(type.typeArguments.last()),
            isNullable = false
        ) + type.typeArguments.last()).toTypedArray(),
        isNullable = false,
        attributes = attributes
    )
}

fun ConeKotlinType.isSubtypeOfFunctionalType(session: FirSession, expectedFunctionalType: ConeClassLikeType): Boolean {
    require(expectedFunctionalType.isBuiltinFunctionalType(session))
    return AbstractTypeChecker.isSubtypeOf(session.typeContext, this, expectedFunctionalType.replaceArgumentsWithStarProjections())
}

fun ConeKotlinType.findSubtypeOfNonSuspendFunctionalType(session: FirSession, expectedFunctionalType: ConeClassLikeType): ConeKotlinType? {
    require(expectedFunctionalType.isBuiltinFunctionalType(session) && !expectedFunctionalType.isSuspendFunctionType(session))
    return when (this) {
        is ConeClassLikeType -> {
            // Expect the argument type is not a suspend functional type.
            if (isSuspendFunctionType(session) || !isSubtypeOfFunctionalType(session, expectedFunctionalType))
                null
            else
                this
        }
        is ConeIntersectionType -> {
            if (intersectedTypes.any { it.isSuspendFunctionType(session) })
                null
            else
                intersectedTypes.find { it.findSubtypeOfNonSuspendFunctionalType(session, expectedFunctionalType) != null }
        }
        is ConeTypeParameterType -> {
            val bounds = lookupTag.typeParameterSymbol.fir.bounds.map { it.coneType }
            if (bounds.any { it.isSuspendFunctionType(session) })
                null
            else
                bounds.find { it.findSubtypeOfNonSuspendFunctionalType(session, expectedFunctionalType) != null }
        }
        else -> null
    }
}

fun ConeClassLikeType.findBaseInvokeSymbol(session: FirSession, scopeSession: ScopeSession): FirNamedFunctionSymbol? {
    require(this.isBuiltinFunctionalType(session))
    val functionN = (lookupTag.toSymbol(session)?.fir as? FirClass) ?: return null
    var baseInvokeSymbol: FirNamedFunctionSymbol? = null
    functionN.unsubstitutedScope(
        session,
        scopeSession,
        withForcedTypeCalculator = false
    ).processFunctionsByName(OperatorNameConventions.INVOKE) { functionSymbol ->
        baseInvokeSymbol = functionSymbol
        return@processFunctionsByName
    }
    return baseInvokeSymbol
}

fun ConeKotlinType.findContributedInvokeSymbol(
    session: FirSession,
    scopeSession: ScopeSession,
    expectedFunctionalType: ConeClassLikeType,
    shouldCalculateReturnTypesOfFakeOverrides: Boolean
): FirFunctionSymbol<*>? {
    val baseInvokeSymbol = expectedFunctionalType.findBaseInvokeSymbol(session, scopeSession) ?: return null

    val fakeOverrideTypeCalculator = if (shouldCalculateReturnTypesOfFakeOverrides) {
        FakeOverrideTypeCalculator.Forced
    } else {
        FakeOverrideTypeCalculator.DoNothing
    }
    val scope = scope(session, scopeSession, fakeOverrideTypeCalculator) ?: return null
    var declaredInvoke: FirNamedFunctionSymbol? = null
    scope.processFunctionsByName(OperatorNameConventions.INVOKE) { functionSymbol ->
        if (functionSymbol.fir.valueParameters.size == baseInvokeSymbol.fir.valueParameters.size) {
            declaredInvoke = functionSymbol
            return@processFunctionsByName
        }
    }

    var overriddenInvoke: FirFunctionSymbol<*>? = null
    if (declaredInvoke != null) {
        // Make sure the user-contributed or type-substituted invoke we just found above is an override of base invoke.
        scope.processOverriddenFunctions(declaredInvoke!!) { functionSymbol ->
            if (functionSymbol == baseInvokeSymbol || functionSymbol.originalForSubstitutionOverride == baseInvokeSymbol) {
                overriddenInvoke = functionSymbol
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }
    }

    return if (overriddenInvoke != null) declaredInvoke else null
}

fun ConeKotlinType.isKClassType(): Boolean {
    return classId == StandardClassIds.KClass
}

private fun ConeTypeProjection.typeOrDefault(default: ConeKotlinType): ConeKotlinType =
    when (this) {
        is ConeKotlinTypeProjection -> type
        is ConeStarProjection -> default
    }

fun ConeKotlinType.receiverType(session: FirSession): ConeKotlinType? {
    if (!isBuiltinFunctionalType(session) || !isExtensionFunctionType(session)) return null
    return fullyExpandedType(session).typeArguments.first().typeOrDefault(session.builtinTypes.nothingType.type)
}

fun ConeKotlinType.returnType(session: FirSession): ConeKotlinType {
    require(this is ConeClassLikeType)
    return fullyExpandedType(session).typeArguments.last().typeOrDefault(session.builtinTypes.nullableAnyType.type)
}

fun ConeKotlinType.valueParameterTypesIncludingReceiver(session: FirSession): List<ConeKotlinType> {
    require(this is ConeClassLikeType)
    return fullyExpandedType(session).typeArguments.dropLast(1).map { it.typeOrDefault(session.builtinTypes.nothingType.type) }
}

val FirAnonymousFunction.returnType: ConeKotlinType? get() = returnTypeRef.coneTypeSafe()
val FirAnonymousFunction.receiverType: ConeKotlinType? get() = receiverTypeRef?.coneTypeSafe()

fun extractLambdaInfoFromFunctionalType(
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    argument: FirAnonymousFunction,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
    components: BodyResolveComponents,
    candidate: Candidate?,
    duringCompletion: Boolean,
): ResolvedLambdaAtom? {
    val session = components.session
    if (expectedType == null) return null
    if (expectedType is ConeFlexibleType) {
        return extractLambdaInfoFromFunctionalType(
            expectedType.lowerBound,
            expectedTypeRef,
            argument,
            returnTypeVariable,
            components,
            candidate,
            duringCompletion
        )
    }
    if (!expectedType.isBuiltinFunctionalType(session)) return null

    val singleStatement = argument.body?.statements?.singleOrNull() as? FirReturnExpression
    if (argument.returnType == null && singleStatement != null &&
        singleStatement.target.labeledElement == argument && singleStatement.result is FirUnitExpression
    ) {
        // Simply { }, i.e., function literals without body. Raw FIR added an implicit return with an implicit unit type ref.
        argument.replaceReturnTypeRef(session.builtinTypes.unitType)
    }
    val returnType = argument.returnType ?: expectedType.returnType(session)

    // `fun (x: T) = ...` and `fun T.() = ...` are both instances of `T.() -> V` and `(T) -> V`; `fun () = ...` is not.
    // For lambdas, the existence of the receiver is always implied by the expected type, and a value parameter
    // can never fill its role.
    val receiverType = if (argument.isLambda) expectedType.receiverType(session) else argument.receiverType
    val valueParametersTypesIncludingReceiver = expectedType.valueParameterTypesIncludingReceiver(session)
    val isExtensionFunctionType = expectedType.isExtensionFunctionType(session)
    val expectedParameters = valueParametersTypesIncludingReceiver.let {
        if (receiverType != null && isExtensionFunctionType) it.drop(1) else it
    }

    var coerceFirstParameterToExtensionReceiver = false
    val argumentValueParameters = argument.valueParameters
    val parameters = if (argument.isLambda && argumentValueParameters.isEmpty() && expectedParameters.size < 2) {
        expectedParameters // Infer existence of a parameter named `it` of an appropriate type.
    } else {
        if (duringCompletion &&
            argument.isLambda &&
            isExtensionFunctionType &&
            valueParametersTypesIncludingReceiver.size == argumentValueParameters.size
        ) {
            // (T, ...) -> V can be converter to T.(...) -> V
            val firstValueParameter = argumentValueParameters.firstOrNull()
            val extensionParameter = valueParametersTypesIncludingReceiver.firstOrNull()
            if (firstValueParameter?.returnTypeRef?.coneTypeSafe<ConeKotlinType>() == extensionParameter?.type) {
                coerceFirstParameterToExtensionReceiver = true
            }
        }

        argumentValueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeSafe()
                ?: expectedParameters.getOrNull(index)
                ?: ConeClassErrorType(
                    ConeSimpleDiagnostic("Cannot infer type for parameter ${parameter.name}", DiagnosticKind.CannotInferParameterType)
                )
        }
    }

    return ResolvedLambdaAtom(
        argument,
        expectedType,
        expectedType.isSuspendFunctionType(session),
        receiverType,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        candidate,
        coerceFirstParameterToExtensionReceiver
    )
}
