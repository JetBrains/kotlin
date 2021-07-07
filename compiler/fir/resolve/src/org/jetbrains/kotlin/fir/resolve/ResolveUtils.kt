/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.providers.getSymbolByTypeRef
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.types.SmartcastStability

fun List<FirQualifierPart>.toTypeProjections(): Array<ConeTypeProjection> =
    asReversed().flatMap { it.typeArgumentList.typeArguments.map { typeArgument -> typeArgument.toConeTypeProjection() } }.toTypedArray()

fun FirFunction.constructFunctionalTypeRef(isSuspend: Boolean = false): FirResolvedTypeRef {
    val receiverTypeRef = when (this) {
        is FirSimpleFunction -> receiverTypeRef
        is FirAnonymousFunction -> receiverTypeRef
        else -> null
    }
    val parameters = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType(
            ConeSimpleDiagnostic(
                "No type for parameter",
                DiagnosticKind.ValueParameterWithNoTypeAnnotation
            )
        )
    }
    val rawReturnType = (this as FirTypedDeclaration).returnTypeRef.coneType

    val functionalType = createFunctionalType(parameters, receiverTypeRef?.coneType, rawReturnType, isSuspend = isSuspend)

    return buildResolvedTypeRef {
        source = this@constructFunctionalTypeRef.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
        type = functionalType
    }
}

fun createFunctionalType(
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isSuspend: Boolean,
    isKFunctionType: Boolean = false
): ConeLookupTagBasedType {
    val receiverAndParameterTypes = listOfNotNull(receiverType) + parameters + listOf(rawReturnType)

    val kind = if (isSuspend) {
        if (isKFunctionType) FunctionClassKind.KSuspendFunction else FunctionClassKind.SuspendFunction
    } else {
        if (isKFunctionType) FunctionClassKind.KFunction else FunctionClassKind.Function
    }

    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(receiverAndParameterTypes.size - 1))
    val attributes = if (receiverType != null) ConeAttributes.WithExtensionFunctionType else ConeAttributes.Empty
    return ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(functionalTypeId),
        receiverAndParameterTypes.toTypedArray(),
        isNullable = false,
        attributes = attributes
    )
}

fun createKPropertyType(
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isMutable: Boolean,
): ConeLookupTagBasedType {
    val arguments = if (receiverType != null) listOf(receiverType, rawReturnType) else listOf(rawReturnType)
    val classId = StandardClassIds.reflectByName("K${if (isMutable) "Mutable" else ""}Property${arguments.size - 1}")
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(classId), arguments.toTypedArray(), isNullable = false)
}

fun BodyResolveComponents.buildResolvedQualifierForClass(
    regularClass: FirClassLikeSymbol<*>,
    sourceElement: FirSourceElement? = null,
    // TODO: Clarify if we actually need type arguments for qualifier?
    typeArgumentsForQualifier: List<FirTypeProjection> = emptyList(),
    diagnostic: ConeDiagnostic? = null
): FirResolvedQualifier {
    val classId = regularClass.classId

    val builder: FirAbstractResolvedQualifierBuilder = if (diagnostic == null) {
        FirResolvedQualifierBuilder()
    } else {
        FirErrorResolvedQualifierBuilder().apply { this.diagnostic = diagnostic }
    }

    return builder.apply {
        source = sourceElement
        packageFqName = classId.packageFqName
        relativeClassFqName = classId.relativeClassName
        typeArguments.addAll(typeArgumentsForQualifier)
        symbol = regularClass
    }.build().apply {
        resultType = if (classId.isLocal) {
            typeForQualifierByDeclaration(regularClass.fir, resultType, session)
                ?: session.builtinTypes.unitType
        } else {
            typeForQualifier(this)
        }
    }
}

fun BodyResolveComponents.typeForQualifier(resolvedQualifier: FirResolvedQualifier): FirTypeRef {
    val classSymbol = resolvedQualifier.symbol
    val resultType = resolvedQualifier.resultType
    if (classSymbol != null) {
        classSymbol.ensureResolved(FirResolvePhase.DECLARATIONS, session)
        val declaration = classSymbol.fir
        if (declaration !is FirTypeAlias || resolvedQualifier.typeArguments.isEmpty()) {
            typeForQualifierByDeclaration(declaration, resultType, session)?.let { return it }
        }
    }
    // TODO: Handle no value type here
    return session.builtinTypes.unitType
}

internal fun typeForReifiedParameterReference(parameterReferenceBuilder: FirResolvedReifiedParameterReferenceBuilder): FirTypeRef {
    val resultType = parameterReferenceBuilder.typeRef
    val typeParameterSymbol = parameterReferenceBuilder.symbol
    return resultType.resolvedTypeFromPrototype(typeParameterSymbol.constructType(emptyArray(), false))
}

internal fun typeForQualifierByDeclaration(declaration: FirDeclaration, resultType: FirTypeRef, session: FirSession): FirTypeRef? {
    if (declaration is FirTypeAlias) {
        val expandedDeclaration = declaration.expandedConeType?.lookupTag?.toSymbol(session)?.fir ?: return null
        return typeForQualifierByDeclaration(expandedDeclaration, resultType, session)
    }
    if (declaration is FirRegularClass) {
        if (declaration.classKind == ClassKind.OBJECT) {
            return resultType.resolvedTypeFromPrototype(
                declaration.symbol.constructType(emptyArray(), false),
            )
        } else {
            val companionObject = declaration.companionObject
            if (companionObject != null) {
                return resultType.resolvedTypeFromPrototype(
                    companionObject.symbol.constructType(emptyArray(), false),
                )
            }
        }
    }
    return null
}

fun <T : FirResolvable> BodyResolveComponents.typeFromCallee(access: T): FirResolvedTypeRef {
    return when (val newCallee = access.calleeReference) {
        is FirErrorNamedReference ->
            buildErrorTypeRef {
                source = access.source?.fakeElement(FirFakeSourceElementKind.ErrorTypeRef)
                diagnostic = ConeStubDiagnostic(newCallee.diagnostic)
            }
        is FirNamedReferenceWithCandidate -> {
            typeFromSymbol(newCallee.candidateSymbol, false)
        }
        is FirResolvedNamedReference -> {
            typeFromSymbol(newCallee.resolvedSymbol, false)
        }
        is FirThisReference -> {
            val labelName = newCallee.labelName
            val implicitReceiver = implicitReceiverStack[labelName]
            buildResolvedTypeRef {
                source = null
                type = implicitReceiver?.type ?: ConeKotlinErrorType(
                    ConeSimpleDiagnostic(
                        "Unresolved this@$labelName",
                        DiagnosticKind.UnresolvedLabel
                    )
                )
            }
        }
        is FirSuperReference -> {
            val labelName = newCallee.labelName
            val implicitReceiver =
                if (labelName != null) implicitReceiverStack[labelName] as? ImplicitDispatchReceiverValue
                else implicitReceiverStack.lastDispatchReceiver()
            val resolvedTypeRef =
                newCallee.superTypeRef as? FirResolvedTypeRef
                    ?: implicitReceiver?.boundSymbol?.fir?.superTypeRefs?.singleOrNull() as? FirResolvedTypeRef
            resolvedTypeRef ?: buildErrorTypeRef {
                source = newCallee.source
                diagnostic = ConeUnresolvedNameError(Name.identifier("super"))
            }
        }
        else -> error("Failed to extract type from: $newCallee")
    }
}

private fun BodyResolveComponents.typeFromSymbol(symbol: FirBasedSymbol<*>, makeNullable: Boolean): FirResolvedTypeRef {
    return when (symbol) {
        is FirCallableSymbol<*> -> {
            val returnTypeRef = returnTypeCalculator.tryCalculateReturnType(symbol.fir)
            if (makeNullable) {
                returnTypeRef.withReplacedConeType(
                    returnTypeRef.type.withNullability(ConeNullability.NULLABLE, session.typeContext),
                    FirFakeSourceElementKind.ImplicitTypeRef
                )
            } else {
                buildResolvedTypeRef {
                    source = returnTypeRef.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
                    type = returnTypeRef.type
                    annotations += returnTypeRef.annotations
                }
            }
        }
        is FirClassifierSymbol<*> -> {
            // TODO: unhack
            buildResolvedTypeRef {
                source = null
                type = symbol.constructType(emptyArray(), isNullable = false)
            }
        }
        else -> error("WTF ! $symbol")
    }
}

fun BodyResolveComponents.transformQualifiedAccessUsingSmartcastInfo(
    qualifiedAccessExpression: FirQualifiedAccessExpression
): FirQualifiedAccessExpression {
    val (stability, typesFromSmartCast) = dataFlowAnalyzer.getTypeUsingSmartcastInfo(qualifiedAccessExpression)
        ?: return qualifiedAccessExpression
    val smartcastStability = stability.impliedSmartcastStability
        ?: if (dataFlowAnalyzer.isAccessToUnstableLocalVariable(qualifiedAccessExpression)) {
            SmartcastStability.CAPTURED_VARIABLE
        } else {
            SmartcastStability.STABLE_VALUE
        }
    val originalType = qualifiedAccessExpression.resultType.coneType
    // For example, if (x == null) { ... },
    //   we don't want to smartcast to Nothing?, but we want to record the nullability to its own kind of node.
    // TODO: should we differentiate x == null v.s. x is Nothing?
    if (typesFromSmartCast.any { it.isNullableNothing }) {
        val typesFromSmartcastWithoutNullableNothing =
            typesFromSmartCast.filterTo(mutableListOf()) { !it.isNullableNothing }.also {
                it += originalType
            }
        val intersectedTypeWithoutNullableNothing =
            ConeTypeIntersector.intersectTypes(session.inferenceComponents.ctx, typesFromSmartcastWithoutNullableNothing)
        val intersectedTypeRefWithoutNullableNothing = buildResolvedTypeRef {
            source = qualifiedAccessExpression.resultType.source?.fakeElement(FirFakeSourceElementKind.SmartCastedTypeRef)
            type = intersectedTypeWithoutNullableNothing
            annotations += qualifiedAccessExpression.resultType.annotations
            delegatedTypeRef = qualifiedAccessExpression.resultType
        }
        return buildExpressionWithSmartcastToNull {
            originalExpression = qualifiedAccessExpression
            // TODO: Use Nothing? during resolution?
            smartcastType = intersectedTypeRefWithoutNullableNothing
            // NB: Nothing? in types from smartcast in DFA is recorded here (and the expression kind itself).
            this.typesFromSmartCast = typesFromSmartCast
            this.smartcastStability = smartcastStability
        }
    }
    val allTypes = typesFromSmartCast.also {
        it += originalType
    }
    val intersectedType = ConeTypeIntersector.intersectTypes(session.inferenceComponents.ctx, allTypes)
    if (intersectedType == originalType) return qualifiedAccessExpression
    val intersectedTypeRef = buildResolvedTypeRef {
        source = qualifiedAccessExpression.resultType.source?.fakeElement(FirFakeSourceElementKind.SmartCastedTypeRef)
        type = intersectedType
        annotations += qualifiedAccessExpression.resultType.annotations
        delegatedTypeRef = qualifiedAccessExpression.resultType
    }
    return buildExpressionWithSmartcast {
        originalExpression = qualifiedAccessExpression
        smartcastType = intersectedTypeRef
        this.typesFromSmartCast = typesFromSmartCast
        this.smartcastStability = smartcastStability
    }
}

fun CallableId.isInvoke(): Boolean =
    isKFunctionInvoke()
            || callableName.asString() == "invoke"
            && className?.asString()?.startsWith("Function") == true
            && packageName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME

fun CallableId.isKFunctionInvoke(): Boolean =
    callableName.asString() == "invoke"
            && className?.asString()?.startsWith("KFunction") == true
            && packageName.asString() == "kotlin.reflect"

fun CallableId.isIteratorNext(): Boolean =
    callableName.asString() == "next" && className?.asString()?.endsWith("Iterator") == true
            && packageName.asString() == "kotlin.collections"

fun CallableId.isIteratorHasNext(): Boolean =
    callableName.asString() == "hasNext" && className?.asString()?.endsWith("Iterator") == true
            && packageName.asString() == "kotlin.collections"

fun CallableId.isIterator(): Boolean =
    callableName.asString() == "iterator" && packageName.asString() in arrayOf("kotlin.collections", "kotlin.ranges")

fun FirAnnotationCall.fqName(session: FirSession): FqName? {
    val symbol = session.symbolProvider.getSymbolByTypeRef<FirRegularClassSymbol>(annotationTypeRef) ?: return null
    return symbol.classId.asSingleFqName()
}

fun FirCheckedSafeCallSubject.propagateTypeFromOriginalReceiver(nullableReceiverExpression: FirExpression, session: FirSession) {
    val receiverType = nullableReceiverExpression.typeRef.coneTypeSafe<ConeKotlinType>() ?: return

    val expandedReceiverType = if (receiverType is ConeClassLikeType) receiverType.fullyExpandedType(session) else receiverType

    val resolvedTypeRef =
        typeRef.resolvedTypeFromPrototype(expandedReceiverType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext))
    replaceTypeRef(resolvedTypeRef)
    session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, source, null)
}

fun FirSafeCallExpression.propagateTypeFromQualifiedAccessAfterNullCheck(
    nullableReceiverExpression: FirExpression,
    session: FirSession,
) {
    val receiverType = nullableReceiverExpression.typeRef.coneTypeSafe<ConeKotlinType>()
    val typeAfterNullCheck = regularQualifiedAccess.expressionTypeOrUnitForAssignment() ?: return
    val isReceiverActuallyNullable = if (session.languageVersionSettings.supportsFeature(LanguageFeature.SafeCallsAreAlwaysNullable)) {
        true
    } else {
        receiverType != null && session.typeContext.run { receiverType.isNullableType() }
    }
    val resultingType =
        if (isReceiverActuallyNullable)
            typeAfterNullCheck.withNullability(ConeNullability.NULLABLE, session.typeContext)
        else
            typeAfterNullCheck

    val resolvedTypeRef = typeRef.resolvedTypeFromPrototype(resultingType)
    replaceTypeRef(resolvedTypeRef)
    session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, source, null)
}

private fun FirQualifiedAccess.expressionTypeOrUnitForAssignment(): ConeKotlinType? {
    if (this is FirExpression) return typeRef.coneTypeSafe()

    require(this is FirVariableAssignment) {
        "The only non-expression FirQualifiedAccess is FirVariableAssignment, but ${this::class} was found"
    }
    return StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false)
}

fun FirAnnotationCall.getCorrespondingClassSymbolOrNull(session: FirSession): FirRegularClassSymbol? {
    return annotationTypeRef.coneType.fullyExpandedType(session).classId?.let {
        if (it.isLocal) {
            // TODO: How to retrieve local annotaiton's constructor?
            null
        } else {
            (session.symbolProvider.getClassLikeSymbolByFqName(it) as? FirRegularClassSymbol)
        }
    }
}

fun <T> BodyResolveComponents.initialTypeOfCandidate(
    candidate: Candidate,
    call: T
): ConeKotlinType where T : FirResolvable, T : FirStatement {
    return initialTypeOfCandidate(candidate, typeFromCallee(call))
}

fun BodyResolveComponents.initialTypeOfCandidate(candidate: Candidate): ConeKotlinType {
    val typeRef = typeFromSymbol(candidate.symbol, makeNullable = false)
    return initialTypeOfCandidate(candidate, typeRef)
}

private fun initialTypeOfCandidate(candidate: Candidate, typeRef: FirResolvedTypeRef): ConeKotlinType {
    return candidate.substitutor.substituteOrSelf(typeRef.type)
}

fun FirCallableDeclaration.getContainingClass(session: FirSession): FirRegularClass? = this.containingClassAttr?.let { lookupTag ->
    session.symbolProvider.getSymbolByLookupTag(lookupTag)?.fir as? FirRegularClass
}

fun FirFunction.getAsForbiddenNamedArgumentsTarget(session: FirSession): ForbiddenNamedArgumentsTarget? {
    if (this is FirConstructor && this.isPrimary) {
        this.getContainingClass(session)?.let { containingClass ->
            if (containingClass.classKind == ClassKind.ANNOTATION_CLASS) {
                // Java annotation classes allow (actually require) named parameters.
                return null
            }
        }
    }
    if (status.isExpect) {
        return ForbiddenNamedArgumentsTarget.EXPECTED_CLASS_MEMBER
    }
    return when (origin) {
        FirDeclarationOrigin.Source, FirDeclarationOrigin.Library -> null
        FirDeclarationOrigin.Delegated -> delegatedWrapperData?.wrapped?.getAsForbiddenNamedArgumentsTarget(session)
        FirDeclarationOrigin.ImportedFromObject -> importedFromObjectData?.original?.getAsForbiddenNamedArgumentsTarget(session)
        // For intersection overrides, the logic in
        // org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScope#selectMostSpecificMember picks the most specific one and store
        // it in originalForIntersectionOverrideAttr. This follows from FE1.0 behavior which selects the most specific function
        // (org.jetbrains.kotlin.resolve.OverridingUtil#selectMostSpecificMember), from which the `hasStableParameterNames` status is
        // copied.
        FirDeclarationOrigin.IntersectionOverride -> originalForIntersectionOverrideAttr?.getAsForbiddenNamedArgumentsTarget(session)
        FirDeclarationOrigin.Java, FirDeclarationOrigin.Enhancement -> ForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION
        FirDeclarationOrigin.SamConstructor -> null
        FirDeclarationOrigin.SubstitutionOverride -> originalForSubstitutionOverrideAttr?.getAsForbiddenNamedArgumentsTarget(session)
        // referenced function of a Kotlin function type
        FirDeclarationOrigin.BuiltIns -> {
            if (dispatchReceiverClassOrNull()?.isBuiltinFunctionalType() == true) {
                ForbiddenNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE
            } else {
                null
            }
        }
        FirDeclarationOrigin.Synthetic -> null
        is FirDeclarationOrigin.Plugin -> null // TODO: figure out what to do with plugin generated functions
    }
}

// TODO: handle functions with non-stable parameter names, see also
//  org.jetbrains.kotlin.fir.serialization.FirElementSerializer.functionProto
//  org.jetbrains.kotlin.fir.serialization.FirElementSerializer.constructorProto
fun FirFunction.getHasStableParameterNames(session: FirSession): Boolean = getAsForbiddenNamedArgumentsTarget(session) == null
