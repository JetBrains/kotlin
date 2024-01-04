/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedErrorReference
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.dfa.PropertyStability
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun FirAnonymousFunction.shouldReturnUnit(returnStatements: Collection<FirExpression>): Boolean =
    isLambda && returnStatements.any { it is FirUnitExpression }

fun FirAnonymousFunction.addReturnToLastStatementIfNeeded(session: FirSession) {
    // If this lambda's resolved, expected return type is Unit, we don't need an explicit return statement.
    // During conversion (to backend IR), the last expression will be coerced to Unit if needed.
    if (returnTypeRef.coneType.fullyExpandedType(session).isUnit) return

    val body = this.body ?: return
    val lastStatement = body.statements.lastOrNull() as? FirExpression ?: return
    if (lastStatement is FirReturnExpression) return

    val returnType = body.resolvedType
    if (returnType.isNothing) return

    val returnTarget = FirFunctionTarget(null, isLambda = isLambda).also { it.bind(this) }
    val returnExpression = buildReturnExpression {
        source = lastStatement.source?.fakeElement(KtFakeSourceElementKind.ImplicitReturn.FromLastStatement)
        result = lastStatement
        target = returnTarget
    }
    body.transformStatements(
        object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E =
                @Suppress("UNCHECKED_CAST")
                if (element == lastStatement) returnExpression as E else element
        }, null
    )
}

/**
 * [kind] == null means that [FunctionTypeKind.Function] will be used
 */
fun FirFunction.constructFunctionType(kind: FunctionTypeKind? = null): ConeLookupTagBasedType {
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

    return createFunctionType(
        kind ?: FunctionTypeKind.Function, parameters, receiverTypeRef?.coneType, rawReturnType,
        contextReceivers = contextReceivers.map { it.typeRef.coneType }
    )
}

/**
 * [kind] == null means that [FunctionTypeKind.Function] will be used
 */
fun FirAnonymousFunction.constructFunctionTypeRef(session: FirSession, kind: FunctionTypeKind? = null): FirResolvedTypeRef {
    var diagnostic: ConeDiagnostic? = null
    val kinds = session.functionTypeService.extractAllSpecialKindsForFunction(symbol)
    val kindFromDeclaration = when (kinds.size) {
        0 -> null
        1 -> kinds.single()
        else -> {
            diagnostic = ConeAmbiguousFunctionTypeKinds(kinds)
            FunctionTypeKind.Function
        }
    }
    val type = constructFunctionType(kindFromDeclaration ?: kind)
    val source = this@constructFunctionTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
    return if (diagnostic == null) {
        buildResolvedTypeRef {
            this.source = source
            this.type = type
        }
    } else {
        buildErrorTypeRef {
            this.source = source
            this.type = type
            this.diagnostic = diagnostic
        }
    }
}

fun createFunctionType(
    kind: FunctionTypeKind,
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    contextReceivers: List<ConeKotlinType> = emptyList(),
): ConeLookupTagBasedType {
    val receiverAndParameterTypes =
        buildList {
            addAll(contextReceivers)
            addIfNotNull(receiverType)
            addAll(parameters)
            add(rawReturnType)
        }

    val functionTypeId = ClassId(kind.packageFqName, kind.numberedClassName(receiverAndParameterTypes.size - 1))
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
        functionTypeId.toLookupTag(),
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
    return ConeClassLikeTypeImpl(classId.toLookupTag(), arguments.toTypedArray(), isNullable = false)
}

fun BodyResolveComponents.buildResolvedQualifierForClass(
    regularClass: FirClassLikeSymbol<*>,
    sourceElement: KtSourceElement?,
    // Note: we need type arguments here, see e.g. testIncompleteConstructorCall in diagnostic group
    typeArgumentsForQualifier: List<FirTypeProjection> = emptyList(),
    diagnostic: ConeDiagnostic? = null,
    nonFatalDiagnostics: List<ConeDiagnostic> = emptyList(),
    annotations: List<FirAnnotation> = emptyList(),
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
        if (classId.isLocal) {
            resultType = typeForQualifierByDeclaration(regularClass.fir, session)
                ?.also { replaceCanBeValue(true) }
                ?: session.builtinTypes.unitType.type
        } else {
            setTypeOfQualifier(session)
        }
    }
}

fun FirResolvedQualifier.setTypeOfQualifier(session: FirSession) {
    val classSymbol = symbol
    if (classSymbol != null) {
        classSymbol.lazyResolveToPhase(FirResolvePhase.TYPES)
        val declaration = classSymbol.fir
        if (declaration !is FirTypeAlias || typeArguments.isEmpty()) {
            val typeByDeclaration = typeForQualifierByDeclaration(declaration, session)
            if (typeByDeclaration != null) {
                this.resultType = typeByDeclaration
                replaceCanBeValue(true)
                return
            }
        }
    }
    this.resultType = session.builtinTypes.unitType.type
}

internal fun typeForReifiedParameterReference(parameterReferenceBuilder: FirResolvedReifiedParameterReferenceBuilder): ConeLookupTagBasedType {
    val typeParameterSymbol = parameterReferenceBuilder.symbol
    return typeParameterSymbol.constructType(emptyArray(), false)
}

internal fun typeForQualifierByDeclaration(declaration: FirDeclaration, session: FirSession): ConeKotlinType? {
    if (declaration is FirTypeAlias) {
        val expandedDeclaration = declaration.expandedConeType?.lookupTag?.toSymbol(session)?.fir ?: return null
        return typeForQualifierByDeclaration(expandedDeclaration, session)
    }
    if (declaration is FirRegularClass) {
        if (declaration.classKind == ClassKind.OBJECT) {
            return declaration.symbol.constructType(emptyArray(), false)
        } else {
            val companionObjectSymbol = declaration.companionObjectSymbol
            if (companionObjectSymbol != null) {
                return companionObjectSymbol.constructType(emptyArray(), false)
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
    val calleeReference = access.calleeReference
    return typeFromCallee(access, calleeReference)
}

fun BodyResolveComponents.typeFromCallee(access: FirElement, calleeReference: FirReference): FirResolvedTypeRef {
    return when (calleeReference) {
        is FirErrorNamedReference ->
            buildErrorTypeRef {
                source = access.source?.fakeElement(KtFakeSourceElementKind.ErrorTypeRef)
                diagnostic = ConeStubDiagnostic(calleeReference.diagnostic)
            }
        is FirNamedReferenceWithCandidate -> {
            typeFromSymbol(calleeReference.candidateSymbol)
        }
        is FirPropertyWithExplicitBackingFieldResolvedNamedReference -> {
            val symbol = calleeReference.getNarrowedDownSymbol(session)
            typeFromSymbol(symbol)
        }
        is FirResolvedNamedReference -> {
            typeFromSymbol(calleeReference.resolvedSymbol)
        }
        is FirThisReference -> {
            val labelName = calleeReference.labelName
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
            val labelName = calleeReference.labelName
            val implicitReceiver =
                if (labelName != null) implicitReceiverStack[labelName] as? ImplicitDispatchReceiverValue
                else implicitReceiverStack.lastDispatchReceiver()
            val resolvedTypeRef =
                calleeReference.superTypeRef as? FirResolvedTypeRef
                    ?: implicitReceiver?.boundSymbol?.fir?.superTypeRefs?.singleOrNull() as? FirResolvedTypeRef
            resolvedTypeRef ?: buildErrorTypeRef {
                source = calleeReference.source
                diagnostic = ConeUnresolvedNameError(Name.identifier("super"))
            }
        }
        else -> errorWithAttachment("Failed to extract type from: ${calleeReference::class.simpleName}") {
            withFirEntry("reference", calleeReference)
        }
    }
}

private fun BodyResolveComponents.typeFromSymbol(symbol: FirBasedSymbol<*>): FirResolvedTypeRef {
    return when (symbol) {
        is FirCallableSymbol<*> -> {
            val returnTypeRef = returnTypeCalculator.tryCalculateReturnType(symbol.fir)
            returnTypeRef.copyWithNewSource(null)
        }
        is FirClassifierSymbol<*> -> {
            buildResolvedTypeRef {
                source = null
                type = symbol.constructType(emptyArray(), isNullable = false)
            }
        }
        else -> errorWithAttachment("Failed to extract type from symbol: ${symbol::class.java}") {
            withFirEntry("declaration", symbol.fir)
        }
    }
}

fun BodyResolveComponents.transformQualifiedAccessUsingSmartcastInfo(
    qualifiedAccessExpression: FirQualifiedAccessExpression,
    ignoreCallArguments: Boolean,
): FirExpression {
    val (stability, typesFromSmartCast) = dataFlowAnalyzer.getTypeUsingSmartcastInfo(qualifiedAccessExpression, ignoreCallArguments)
        ?: return qualifiedAccessExpression

    return transformExpressionUsingSmartcastInfo(qualifiedAccessExpression, stability, typesFromSmartCast) ?: qualifiedAccessExpression
}

fun BodyResolveComponents.transformWhenSubjectExpressionUsingSmartcastInfo(
    whenSubjectExpression: FirWhenSubjectExpression,
): FirExpression {
    val (stability, typesFromSmartCast) = dataFlowAnalyzer.getTypeUsingSmartcastInfo(whenSubjectExpression, ignoreCallArguments = false)
        ?: return whenSubjectExpression

    return transformExpressionUsingSmartcastInfo(whenSubjectExpression, stability, typesFromSmartCast) ?: whenSubjectExpression
}

fun BodyResolveComponents.transformDesugaredAssignmentValueUsingSmartcastInfo(
    expression: FirDesugaredAssignmentValueReferenceExpression,
): FirExpression {
    val (stability, typesFromSmartCast) =
        dataFlowAnalyzer.getTypeUsingSmartcastInfo(expression.expressionRef.value, ignoreCallArguments = false)
            ?: return expression

    return transformExpressionUsingSmartcastInfo(expression, stability, typesFromSmartCast) ?: expression
}

private val ConeKotlinType.isKindOfNothing
    get() = lowerBoundIfFlexible().let { it.isNothing || it.isNullableNothing }

private fun FirSmartCastExpressionBuilder.applyResultTypeRef() {
    coneTypeOrNull =
        if (smartcastStability == SmartcastStability.STABLE_VALUE)
            smartcastType.coneTypeOrNull
        else
            originalExpression.resolvedType
}

private fun <T : FirExpression> BodyResolveComponents.transformExpressionUsingSmartcastInfo(
    expression: T,
    stability: PropertyStability,
    typesFromSmartCast: MutableList<ConeKotlinType>,
): FirSmartCastExpression? {
    val originalType = expression.resolvedType.fullyExpandedType(session)
    val allTypes = typesFromSmartCast.also {
        if (originalType !is ConeStubType) {
            it += originalType.fullyExpandedType(session)
        }
    }
    if (allTypes.all { it is ConeDynamicType }) return null
    val intersectedType = ConeTypeIntersector.intersectTypes(session.typeContext, allTypes)
    if (intersectedType == originalType && intersectedType !is ConeDynamicType) return null
    val intersectedTypeRef = buildResolvedTypeRef {
        source = expression.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
        type = intersectedType
    }

    val smartcastStability = stability.impliedSmartcastStability
        ?: if (dataFlowAnalyzer.isAccessToUnstableLocalVariable(expression, intersectedType)) {
            SmartcastStability.CAPTURED_VARIABLE
        } else {
            SmartcastStability.STABLE_VALUE
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
            source = expression.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
            type = reducedIntersectedType
        }
        return buildSmartCastExpression {
            originalExpression = expression
            smartcastType = intersectedTypeRef
            smartcastTypeWithoutNullableNothing = reducedIntersectedTypeRef
            this.typesFromSmartCast = typesFromSmartCast
            this.smartcastStability = smartcastStability
            applyResultTypeRef()
        }
    }

    return buildSmartCastExpression {
        originalExpression = expression
        smartcastType = intersectedTypeRef
        this.typesFromSmartCast = typesFromSmartCast
        this.smartcastStability = smartcastStability
        applyResultTypeRef()
    }
}

fun FirCheckedSafeCallSubject.propagateTypeFromOriginalReceiver(
    nullableReceiverExpression: FirExpression,
    session: FirSession,
    file: FirFile,
) {
    // If the receiver expression is smartcast to `null`, it would have `Nothing?` as its type, which may not have members called by user
    // code. Hence, we fallback to the type before intersecting with `Nothing?`.
    val receiverType = (nullableReceiverExpression as? FirSmartCastExpression)
        ?.takeIf { it.isStable }
        ?.smartcastTypeWithoutNullableNothing
        ?.coneTypeSafe<ConeKotlinType>()
        ?: nullableReceiverExpression.resolvedType

    val expandedReceiverType = receiverType.fullyExpandedType(session).makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)
    replaceConeTypeOrNull(expandedReceiverType)
    session.lookupTracker?.recordTypeResolveAsLookup(expandedReceiverType, source, file.source)
}

fun FirSafeCallExpression.propagateTypeFromQualifiedAccessAfterNullCheck(
    session: FirSession,
    file: FirFile,
) {
    val selector = selector

    val resultingType = when {
        selector is FirExpression && !selector.isStatementLikeExpression -> {
            val type = selector.resolvedType
            type.withNullability(ConeNullability.NULLABLE, session.typeContext)
        }
        // Branch for things that shouldn't be used as expressions.
        // They are forced to return not-null `Unit`, regardless of the receiver.
        else -> {
            StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false)
        }
    }

    replaceConeTypeOrNull(resultingType)
    session.lookupTracker?.recordTypeResolveAsLookup(resultingType, source, file.source)
}

fun FirAnnotation.getCorrespondingClassSymbolOrNull(session: FirSession): FirRegularClassSymbol? {
    return annotationTypeRef.coneType.fullyExpandedType(session).toRegularClassSymbol(session)
}

fun BodyResolveComponents.initialTypeOfCandidate(candidate: Candidate): ConeKotlinType {
    val typeRef = typeFromSymbol(candidate.symbol)
    return typeRef.initialTypeOfCandidate(candidate)
}

fun FirResolvedTypeRef.initialTypeOfCandidate(candidate: Candidate): ConeKotlinType {
    val system = candidate.system
    val resultingSubstitutor = system.buildCurrentSubstitutor()
    return resultingSubstitutor.safeSubstitute(system, candidate.substitutor.substituteOrSelf(type)) as ConeKotlinType
}

fun FirCallableDeclaration.getContainingClass(session: FirSession): FirRegularClass? =
    this.containingClassLookupTag()?.let { lookupTag ->
        session.symbolProvider.getSymbolByLookupTag(lookupTag)?.fir as? FirRegularClass
    }

fun FirFunction.getAsForbiddenNamedArgumentsTarget(
    session: FirSession,
    // NB: with originScope given this function will try to find overridden declaration with allowed parameter names
    // for intersection/substitution overrides
    originScope: FirTypeScope? = null,
): ForbiddenNamedArgumentsTarget? {
    if (hasStableParameterNames) return null

    return when (origin) {
        FirDeclarationOrigin.ImportedFromObjectOrStatic ->
            importedFromObjectOrStaticData?.original?.getAsForbiddenNamedArgumentsTarget(session)

        FirDeclarationOrigin.IntersectionOverride, is FirDeclarationOrigin.SubstitutionOverride, FirDeclarationOrigin.Delegated -> {
            var result: ForbiddenNamedArgumentsTarget? =
                unwrapFakeOverridesOrDelegated().getAsForbiddenNamedArgumentsTarget(session) ?: return null
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

        FirDeclarationOrigin.BuiltIns -> ForbiddenNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE
        is FirDeclarationOrigin.Plugin -> null // TODO: figure out what to do with plugin generated functions
        else -> ForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION
    }
}

@OptIn(ExperimentalContracts::class)
fun FirExpression?.isIntegerLiteralOrOperatorCall(): Boolean {
    contract {
        returns(true) implies (this@isIntegerLiteralOrOperatorCall != null)
    }
    return when (this) {
        is FirLiteralExpression<*> -> kind == ConstantValueKind.Int
                || kind == ConstantValueKind.IntegerLiteral
                || kind == ConstantValueKind.UnsignedInt
                || kind == ConstantValueKind.UnsignedIntegerLiteral

        is FirIntegerLiteralOperatorCall -> true
        is FirNamedArgumentExpression -> this.expression.isIntegerLiteralOrOperatorCall()
        else -> false
    }
}

fun createConeDiagnosticForCandidateWithError(
    applicability: CandidateApplicability,
    candidate: Candidate,
): ConeDiagnostic {
    val symbol = candidate.symbol
    return when (applicability) {
        CandidateApplicability.HIDDEN -> ConeHiddenCandidateError(candidate)
        CandidateApplicability.K2_VISIBILITY_ERROR -> {
            val session = candidate.callInfo.session
            val declaration = symbol.fir
            if (declaration is FirMemberDeclaration &&
                session.visibilityChecker.isVisible(declaration, candidate, skipCheckForContainingClassVisibility = true)
            ) {
                // We can have declarations that are visible by themselves, but some containing declaration is invisible.
                // We report the nearest invisible containing declaration, otherwise we'll get a confusing diagnostic like
                // Cannot access 'foo', it is public in 'Bar'.
                declaration
                    .parentDeclarationSequence(session, candidate.dispatchReceiver, candidate.callInfo.containingDeclarations)
                    ?.firstOrNull {
                        !session.visibilityChecker.isVisible(
                            it,
                            session,
                            candidate.callInfo.containingFile,
                            candidate.callInfo.containingDeclarations,
                            dispatchReceiver = null,
                            skipCheckForContainingClassVisibility = true,
                        )
                    }?.let {
                        return ConeVisibilityError(it.symbol)
                    }
            }
            if (symbol is FirPropertySymbol && SetterVisibilityError in candidate.diagnostics) {
                ConeSetterVisibilityError(symbol)
            } else {
                ConeVisibilityError(symbol)
            }
        }
        CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> ConeInapplicableWrongReceiver(listOf(candidate))
        CandidateApplicability.K2_NO_COMPANION_OBJECT -> ConeNoCompanionObject(candidate)
        else -> {
            if (TypeParameterAsExpression in candidate.diagnostics) {
                ConeTypeParameterInQualifiedAccess(symbol as FirTypeParameterSymbol)
            } else {
                ConeInapplicableCandidateError(applicability, candidate)
            }
        }
    }
}

fun FirNamedReferenceWithCandidate.toErrorReference(diagnostic: ConeDiagnostic): FirNamedReference {
    val calleeReference = this
    return when (calleeReference.candidateSymbol) {
        is FirErrorPropertySymbol, is FirErrorFunctionSymbol -> buildErrorNamedReference {
            source = calleeReference.source
            this.diagnostic = diagnostic
        }
        else -> buildResolvedErrorReference {
            source = calleeReference.source
            name = calleeReference.name
            resolvedSymbol = calleeReference.candidateSymbol
            this.diagnostic = diagnostic
        }
    }
}

val FirTypeParameterSymbol.defaultType: ConeTypeParameterType
    get() = ConeTypeParameterTypeImpl(toLookupTag(), isNullable = false)

fun ConeClassLikeLookupTag.isRealOwnerOf(declarationSymbol: FirCallableSymbol<*>): Boolean =
    this == declarationSymbol.dispatchReceiverClassLookupTagOrNull()