/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.canNarrowDownGetterType
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.modality
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
import org.jetbrains.kotlin.fir.resolve.calls.FirPropertyWithExplicitBackingFieldResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.PropertyStability
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.utils.addIfNotNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun List<FirQualifierPart>.toTypeProjections(): Array<ConeTypeProjection> =
    asReversed().flatMap { it.typeArgumentList.typeArguments.map { typeArgument -> typeArgument.toConeTypeProjection() } }.toTypedArray()

fun FirFunction.constructFunctionalType(isSuspend: Boolean = false): ConeLookupTagBasedType {
    val receiverTypeRef = when (this) {
        is FirSimpleFunction -> receiverParameter
        is FirAnonymousFunction -> receiverParameter
        else -> null
    }?.typeRef

    val parameters = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeErrorType(
            ConeSimpleDiagnostic(
                "No type for parameter",
                DiagnosticKind.ValueParameterWithNoTypeAnnotation
            )
        )
    }
    val rawReturnType = (this as FirCallableDeclaration).returnTypeRef.coneType

    return createFunctionalType(
        parameters, receiverTypeRef?.coneType, rawReturnType, isSuspend = isSuspend,
        contextReceivers = contextReceivers.map { it.typeRef.coneType }
    )
}

fun FirFunction.constructFunctionalTypeRef(isSuspend: Boolean = false): FirResolvedTypeRef {
    return buildResolvedTypeRef {
        source = this@constructFunctionalTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
        type = constructFunctionalType(isSuspend)
    }
}

fun createFunctionalType(
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isSuspend: Boolean,
    contextReceivers: List<ConeKotlinType> = emptyList(),
    isKFunctionType: Boolean = false
): ConeLookupTagBasedType {
    val receiverAndParameterTypes =
        buildList {
            addAll(contextReceivers)
            addIfNotNull(receiverType)
            addAll(parameters)
            add(rawReturnType)
        }

    val kind = if (isSuspend) {
        if (isKFunctionType) FunctionClassKind.KSuspendFunction else FunctionClassKind.SuspendFunction
    } else {
        if (isKFunctionType) FunctionClassKind.KFunction else FunctionClassKind.Function
    }

    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(receiverAndParameterTypes.size - 1))
    val attributes = when {
        contextReceivers.isNotEmpty() -> ConeAttributes.create(
            buildList {
                add(CompilerConeAttributes.ContextFunctionTypeParams(contextReceivers.size))
                if (receiverType != null) {
                    add(CompilerConeAttributes.ExtensionFunctionType)
                }
            }
        )
        receiverType != null -> ConeAttributes.WithExtensionFunctionType
        else -> ConeAttributes.Empty
    }
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
    sourceElement: KtSourceElement? = null,
    // TODO: Clarify if we actually need type arguments for qualifier?
    typeArgumentsForQualifier: List<FirTypeProjection> = emptyList(),
    diagnostic: ConeDiagnostic? = null,
    nonFatalDiagnostics: List<ConeDiagnostic> = emptyList(),
    annotations: List<FirAnnotation> = emptyList()
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
        this.nonFatalDiagnostics.addAll(nonFatalDiagnostics)
        this.annotations.addAll(annotations)
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
        classSymbol.lazyResolveToPhase(FirResolvePhase.TYPES)
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
            val companionObjectSymbol = declaration.companionObjectSymbol
            if (companionObjectSymbol != null) {
                return resultType.resolvedTypeFromPrototype(
                    companionObjectSymbol.constructType(emptyArray(), false),
                )
            }
        }
    }
    return null
}

private fun FirPropertySymbol.isEffectivelyFinal(session: FirSession): Boolean {
    if (isFinal) return true
    val containingClass = dispatchReceiverType?.toRegularClassSymbol(session)
        ?: return false
    return containingClass.modality == Modality.FINAL && containingClass.classKind != ClassKind.ENUM_CLASS
}

private fun FirPropertyWithExplicitBackingFieldResolvedNamedReference.getNarrowedDownSymbol(session: FirSession): FirBasedSymbol<*> {
    val propertyReceiver = resolvedSymbol as? FirPropertySymbol ?: return resolvedSymbol

    // This can happen in case of 2 properties referencing
    // each other recursively. See: Jet81.fir.kt
    if (
        propertyReceiver.fir.returnTypeRef is FirImplicitTypeRef ||
        propertyReceiver.fir.backingField?.returnTypeRef is FirImplicitTypeRef
    ) {
        return resolvedSymbol
    }

    if (
        propertyReceiver.isEffectivelyFinal(session) &&
        hasVisibleBackingField &&
        propertyReceiver.canNarrowDownGetterType
    ) {
        return propertyReceiver.fir.backingField?.symbol ?: resolvedSymbol
    }

    return resolvedSymbol
}

fun <T : FirResolvable> BodyResolveComponents.typeFromCallee(access: T): FirResolvedTypeRef {
    return when (val newCallee = access.calleeReference) {
        is FirErrorNamedReference ->
            buildErrorTypeRef {
                source = access.source?.fakeElement(KtFakeSourceElementKind.ErrorTypeRef)
                diagnostic = ConeStubDiagnostic(newCallee.diagnostic)
            }
        is FirNamedReferenceWithCandidate -> {
            typeFromSymbol(newCallee.candidateSymbol, false)
        }
        is FirPropertyWithExplicitBackingFieldResolvedNamedReference -> {
            val symbol = newCallee.getNarrowedDownSymbol(session)
            typeFromSymbol(symbol, false)
        }
        is FirResolvedNamedReference -> {
            typeFromSymbol(newCallee.resolvedSymbol, false)
        }
        is FirThisReference -> {
            val labelName = newCallee.labelName
            val implicitReceiver = implicitReceiverStack[labelName]
            buildResolvedTypeRef {
                source = null
                type = implicitReceiver?.type ?: ConeErrorType(
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
                    KtFakeSourceElementKind.ImplicitTypeRef
                )
            } else {
                buildResolvedTypeRef {
                    source = returnTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
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
): FirExpression {
    val (stability, typesFromSmartCast) =
        dataFlowAnalyzer.getTypeUsingSmartcastInfo(qualifiedAccessExpression)
            ?: return qualifiedAccessExpression
    val builder = transformExpressionUsingSmartcastInfo(
        qualifiedAccessExpression,
        stability, typesFromSmartCast
    ) ?: return qualifiedAccessExpression
    return builder.build()
}

fun BodyResolveComponents.transformWhenSubjectExpressionUsingSmartcastInfo(
    whenSubjectExpression: FirWhenSubjectExpression
): FirExpression {
    val (stability, typesFromSmartCast) = dataFlowAnalyzer.getTypeUsingSmartcastInfo(whenSubjectExpression) ?: return whenSubjectExpression
    val builder = transformExpressionUsingSmartcastInfo(
        whenSubjectExpression,
        stability, typesFromSmartCast
    ) ?: return whenSubjectExpression
    return builder.build()
}

private val ConeKotlinType.isKindOfNothing
    get() = lowerBoundIfFlexible().let { it.isNothing || it.isNullableNothing }

private fun FirSmartCastExpressionBuilder.applyResultTypeRef() {
    typeRef =
        if (smartcastStability == SmartcastStability.STABLE_VALUE)
            smartcastType.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
        else
            originalExpression.typeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
}
private fun <T : FirExpression> BodyResolveComponents.transformExpressionUsingSmartcastInfo(
    expression: T,
    stability: PropertyStability,
    typesFromSmartCast: MutableList<ConeKotlinType>
): FirSmartCastExpressionBuilder? {
    val smartcastStability = stability.impliedSmartcastStability
        ?: if (dataFlowAnalyzer.isAccessToUnstableLocalVariable(expression)) {
            SmartcastStability.CAPTURED_VARIABLE
        } else {
            SmartcastStability.STABLE_VALUE
        }

    val originalType = expression.resultType.coneType.fullyExpandedType(session)
    val allTypes = typesFromSmartCast.also {
        if (originalType !is ConeStubType) {
            it += originalType.fullyExpandedType(session)
        }
    }
    if (allTypes.all { it is ConeDynamicType }) return null
    val intersectedType = ConeTypeIntersector.intersectTypes(session.typeContext, allTypes)
    if (intersectedType == originalType && intersectedType !is ConeDynamicType) return null
    val intersectedTypeRef = buildResolvedTypeRef {
        source = expression.resultType.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
        type = intersectedType
        annotations += expression.resultType.annotations
        delegatedTypeRef = expression.resultType
    }

    // Example (1): if (x is String) { ... }, where x: dynamic
    //   the dynamic type will "consume" all other, erasing information.
    // Example (2): if (x == null) { ... },
    //   we need to track the type without `Nothing?` so that resolution with this as receiver can go through properly.
    if (
        intersectedType.isKindOfNothing &&
        !originalType.isNullableNothing &&
        !originalType.isNothing &&
        originalType !is ConeStubType
    ) {
        val reducedTypes = typesFromSmartCast.filterTo(mutableListOf()) { !it.isKindOfNothing }
        val reducedIntersectedType = ConeTypeIntersector.intersectTypes(session.typeContext, reducedTypes)
        val reducedIntersectedTypeRef = buildResolvedTypeRef {
            source = expression.resultType.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
            type = reducedIntersectedType
            annotations += expression.resultType.annotations
            delegatedTypeRef = expression.resultType
        }
        return FirSmartCastExpressionBuilder().apply {
            originalExpression = expression
            source = originalExpression.source?.fakeElement(KtFakeSourceElementKind.SmartCastExpression)
            smartcastType = intersectedTypeRef
            smartcastTypeWithoutNullableNothing = reducedIntersectedTypeRef
            this.typesFromSmartCast = typesFromSmartCast
            this.smartcastStability = smartcastStability
            applyResultTypeRef()
        }
    }

    return FirSmartCastExpressionBuilder().apply {
        originalExpression = expression
        source = originalExpression.source?.fakeElement(KtFakeSourceElementKind.SmartCastExpression)
        smartcastType = intersectedTypeRef
        this.typesFromSmartCast = typesFromSmartCast
        this.smartcastStability = smartcastStability
        applyResultTypeRef()
    }
}

fun FirCheckedSafeCallSubject.propagateTypeFromOriginalReceiver(
    nullableReceiverExpression: FirExpression,
    session: FirSession,
    file: FirFile
) {
    // If the receiver expression is smartcast to `null`, it would have `Nothing?` as its type, which may not have members called by user
    // code. Hence, we fallback to the type before intersecting with `Nothing?`.
    val receiverType = ((nullableReceiverExpression as? FirSmartCastExpression)
        ?.takeIf { it.isStable }
        ?.smartcastTypeWithoutNullableNothing
        ?: nullableReceiverExpression.typeRef)
        .coneTypeSafe<ConeKotlinType>() ?: return

    val expandedReceiverType = if (receiverType is ConeClassLikeType) receiverType.fullyExpandedType(session) else receiverType

    val resolvedTypeRef =
        typeRef.resolvedTypeFromPrototype(expandedReceiverType.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext))
    replaceTypeRef(resolvedTypeRef)
    session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, source, file.source)
}

fun FirSafeCallExpression.propagateTypeFromQualifiedAccessAfterNullCheck(
    nullableReceiverExpression: FirExpression,
    session: FirSession,
    file: FirFile,
) {
    val receiverType = nullableReceiverExpression.typeRef.coneTypeSafe<ConeKotlinType>()
    val typeAfterNullCheck = selector.expressionTypeOrUnitForAssignment() ?: return
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
    session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, source, file.source)
}

private fun FirStatement.expressionTypeOrUnitForAssignment(): ConeKotlinType? {
    if (this is FirExpression) return typeRef.coneTypeSafe()

    require(this is FirVariableAssignment) {
        "The only non-expression FirQualifiedAccess is FirVariableAssignment, but ${this::class} was found"
    }
    return StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false)
}

fun FirAnnotation.getCorrespondingClassSymbolOrNull(session: FirSession): FirRegularClassSymbol? {
    return annotationTypeRef.coneType.fullyExpandedType(session).classId?.let {
        if (it.isLocal) {
            // TODO: How to retrieve local annotaiton's constructor?
            null
        } else {
            (session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol)
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
    val system = candidate.system
    val resultingSubstitutor = system.buildCurrentSubstitutor()
    return resultingSubstitutor.safeSubstitute(system, candidate.substitutor.substituteOrSelf(typeRef.type)) as ConeKotlinType
}

fun FirCallableDeclaration.getContainingClass(session: FirSession): FirRegularClass? =
    this.containingClassLookupTag()?.let { lookupTag ->
        session.symbolProvider.getSymbolByLookupTag(lookupTag)?.fir as? FirRegularClass
    }

fun FirFunction.getAsForbiddenNamedArgumentsTarget(
    session: FirSession,
    // NB: with originScope given this function will try to find overridden declaration with allowed parameter names
    // for intersection/substitution overrides
    originScope: FirTypeScope? = null
): ForbiddenNamedArgumentsTarget? {
    if (this is FirConstructor && this.isPrimary) {
        this.getContainingClass(session)?.let { containingClass ->
            if (containingClass.classKind == ClassKind.ANNOTATION_CLASS) {
                // Java annotation classes allow (actually require) named parameters.
                return null
            }
        }
    }
    return when (origin) {
        FirDeclarationOrigin.Source, FirDeclarationOrigin.Precompiled, FirDeclarationOrigin.Library -> null
        FirDeclarationOrigin.Delegated -> delegatedWrapperData?.wrapped?.getAsForbiddenNamedArgumentsTarget(session)
        FirDeclarationOrigin.ImportedFromObjectOrStatic -> importedFromObjectOrStaticData?.original?.getAsForbiddenNamedArgumentsTarget(session)
        is FirDeclarationOrigin.Java, FirDeclarationOrigin.Enhancement -> ForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION
        FirDeclarationOrigin.SamConstructor -> null
        FirDeclarationOrigin.IntersectionOverride, FirDeclarationOrigin.SubstitutionOverride -> {
            var result: ForbiddenNamedArgumentsTarget? =
                originalIfFakeOverride()?.getAsForbiddenNamedArgumentsTarget(session) ?: return null
            originScope?.processOverriddenFunctions(symbol as FirNamedFunctionSymbol) {
                if (it.fir.getAsForbiddenNamedArgumentsTarget(session) == null) {
                    result = null
                    ProcessorAction.STOP
                } else {
                    ProcessorAction.NEXT
                }
            }
            result
        }
        // referenced function of a Kotlin function type
        FirDeclarationOrigin.BuiltIns -> {
            if (dispatchReceiverClassLookupTagOrNull()?.isBuiltinFunctionalType() == true) {
                ForbiddenNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE
            } else {
                null
            }
        }
        FirDeclarationOrigin.Synthetic -> null
        FirDeclarationOrigin.DynamicScope -> null
        FirDeclarationOrigin.RenamedForOverride -> null
        FirDeclarationOrigin.WrappedIntegerOperator -> null
        is FirDeclarationOrigin.Plugin -> null // TODO: figure out what to do with plugin generated functions
    }
}

// TODO: handle functions with non-stable parameter names, see also
//  org.jetbrains.kotlin.fir.serialization.FirElementSerializer.functionProto
//  org.jetbrains.kotlin.fir.serialization.FirElementSerializer.constructorProto
fun FirFunction.getHasStableParameterNames(session: FirSession): Boolean = getAsForbiddenNamedArgumentsTarget(session) == null

@OptIn(ExperimentalContracts::class)
fun FirExpression?.isIntegerLiteralOrOperatorCall(): Boolean {
    contract {
        returns(true) implies (this@isIntegerLiteralOrOperatorCall != null)
    }
    return when (this) {
        is FirConstExpression<*> -> kind == ConstantValueKind.Int
                || kind == ConstantValueKind.IntegerLiteral
                || kind == ConstantValueKind.UnsignedInt
                || kind == ConstantValueKind.UnsignedIntegerLiteral

        is FirIntegerLiteralOperatorCall -> true
        else -> false
    }
}

fun createConeDiagnosticForCandidateWithError(
    applicability: CandidateApplicability,
    candidate: Candidate
): ConeDiagnostic {
    return when (applicability) {
        CandidateApplicability.HIDDEN -> ConeHiddenCandidateError(candidate)
        CandidateApplicability.K2_VISIBILITY_ERROR -> ConeVisibilityError(candidate.symbol)
        CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> ConeInapplicableWrongReceiver(listOf(candidate))
        CandidateApplicability.K2_NO_COMPANION_OBJECT -> ConeNoCompanionObject(candidate)
        else -> ConeInapplicableCandidateError(applicability, candidate)
    }
}
