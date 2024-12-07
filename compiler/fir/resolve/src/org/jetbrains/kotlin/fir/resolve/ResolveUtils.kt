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
import org.jetbrains.kotlin.fir.resolve.calls.TypeParameterAsExpression
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirPropertyWithExplicitBackingFieldResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.isVisible
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FirAnonymousFunctionReturnExpressionInfo
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasForConstructor
import org.jetbrains.kotlin.fir.scopes.impl.outerTypeIfTypeAlias
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun FirAnonymousFunction.shouldReturnUnit(returnStatements: Collection<FirExpression>): Boolean =
    isLambda && returnStatements.any { it is FirUnitExpression }

/**
 * Infers the return type of an anonymous function from return expressions in its body.
 *
 * Note: this logic affects not only diagnostics, but also the runtime types of generated lambda classes (at least on JVM).
 * See [KT-62550](https://youtrack.jetbrains.com/issue/KT-62550) and `compiler/testData/codegen/box/callableReference/kt62550.kt`
 * for reference.
 * Namely, this code basically decides what `T` should be in the `FunctionN<T>` interface that the lambda class will implement.
 * This can be observed through reflection.
 *
 * It's important to understand that the logic is a bit different for lambdas passed to functions and for all other lambdas:
 * - the return type of a lambda passed to a function is always inferred to [expectedReturnType],
 *   except when the lambda's body implies that it's `Unit` — in that case, the return type is inferred to `Unit`;
 * - the return type of other lambda expressions (e.g., assigned to a variable) is generally computed as a common supertype of
 *   the expressions in its `return` statements.
 */
internal fun FirAnonymousFunction.computeReturnType(
    session: FirSession,
    expectedReturnType: ConeKotlinType?,
    isPassedAsFunctionArgument: Boolean,
    returnExpressions: Collection<FirAnonymousFunctionReturnExpressionInfo>,
): ConeKotlinType {
    val expandedExpectedReturnType = expectedReturnType?.fullyExpandedType(session)
    val unitType = session.builtinTypes.unitType.coneType
    if (isLambda) {
        if (expandedExpectedReturnType?.isUnitOrFlexibleUnit == true) {
            // If the expected type is Unit or flexible Unit, always infer the lambda's type to Unit.
            // If a return statement in a lambda has a different type, RETURN_TYPE_MISMATCH will be reported for that return statement
            // by FirFunctionReturnTypeMismatchChecker.
            //
            // For example:
            // val f: () -> Unit = l@ {
            //     return@l "" // RETURN_TYPE_MISMATCH reported here
            // }
            //
            // Without this check, INITIALIZER_TYPE_MISMATCH would be reported on the whole lambda expression,
            // because the return type of the lambda would be inferred to String.
            //
            // NOTE: If the lambda's expected type is flexible Unit, we forbid returning null from such a lambda.
            // See KT-66909.
            return unitType
        }

        if (returnExpressions.any { it.isExplicitEmptyReturn() }) {
            // If the expected type is not Unit, and we have an explicit expressionless return, don't infer the return type to Unit.
            // For this situation, RETURN_TYPE_MISMATCH will be reported later in FirFunctionReturnTypeMismatchChecker.
            //
            // For example:
            // val f: () -> Int = l@ {
            //    if ("".hashCode() == 42) return@l // RETURN_TYPE_MISMATCH reported here
            //    return@l Unit
            // }
            //
            // Without this check, INITIALIZER_TYPE_MISMATCH would be reported on the whole lambda expression,
            // because the return type of the lambda would be inferred to Unit.
            //
            // At the same time, there's no strong reason to report anything in the case like
            // TODO: Try to get rid of those vague condition once KT-67867 is resolved in some way
            // val f: () -> Any = l@ {
            //     if ("".hashCode() == 42) return@l // RETURN_TYPE_MISMATCH reported here
            //     return@l Unit
            // }
            return if (expandedExpectedReturnType != null) {
                expectedReturnType
            } else {
                unitType
            }
        }
    }

    // Here is a questionable moment where we could prefer the expected type over an inferred one.
    // In correct code this doesn't matter, as all return expression types should be subtypes of the expected type.
    // In incorrect code, this would change diagnostics: we can get errors either on the entire lambda, or only on its
    // return statements. The former kind of makes more sense, but the latter is more readable.
    // TODO: Consider simplifying the code once we've got some resolution on KT-67869
    val commonSuperType = session.typeContext.commonSuperTypeOrNull(returnExpressions.map { it.expression.resolvedType })
        ?: unitType

    // If both expected and return expression CSTs are error types, prefer the return expression CST.
    // This helps us skip duplicate diagnostics on implicit lambda return type refs.
    if (expandedExpectedReturnType is ConeErrorType && commonSuperType is ConeErrorType) {
        return commonSuperType
    }

    return if (isPassedAsFunctionArgument && !commonSuperType.fullyExpandedType(session).isUnit) {
        expectedReturnType ?: commonSuperType
    } else {
        commonSuperType
    }
}

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
        // Always set the source to something because ControlFlowGraphBuilder#returnExpressionsOfAnonymousFunction may query
        // the source kind for distinguishing an implicit return from the last statement.
        source = (lastStatement.source ?: body.source ?: this@addReturnToLastStatementIfNeeded.source)
            ?.fakeElement(KtFakeSourceElementKind.ImplicitReturn.FromLastStatement)

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
 * This function returns true only for the case of explicitly written empty `return` or `return@label`.
 * Not explicit Unit as last statement, not an implicit return for Unit-coercion or a synthetic expression for empty lambda
 */
private fun FirAnonymousFunctionReturnExpressionInfo.isExplicitEmptyReturn(): Boolean {
    // It's just a last statement (not explicit return)
    if (!isExplicit) return false

    // Currently, if the content of return is FirUnitExpression, it means that initially it was expressionless return
    // or a synthetic statement for empty lambda
    if (expression !is FirUnitExpression) return false

    // For case of empty lambdas, they are not counted as explicit returns, too
    if (expression.isImplicitUnitForEmptyLambda()) return false

    return true
}

fun FirExpression.isImplicitUnitForEmptyLambda(): Boolean =
    source?.kind == KtFakeSourceElementKind.ImplicitUnit.ForEmptyLambda

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
        contextParameters = contextParameters.map { it.returnTypeRef.coneType }
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

    /**
     * For lambdas, we drop reflect kinds here as lambda itself always has a non-reflect type
     * For anonymous functions, this is handled in [org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionType],
     * see calculation of actualFunctionKind there.
     */
    val type = constructFunctionType(kindFromDeclaration ?: kind?.applyIf(isLambda) { nonReflectKind() })
    val source = this@constructFunctionTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
    return if (diagnostic == null) {
        buildResolvedTypeRef {
            this.source = source
            this.coneType = type
        }
    } else {
        buildErrorTypeRef {
            this.source = source
            this.coneType = type
            this.diagnostic = diagnostic
        }
    }
}

fun createFunctionType(
    kind: FunctionTypeKind,
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    contextParameters: List<ConeKotlinType> = emptyList(),
): ConeLookupTagBasedType {
    val receiverAndParameterTypes =
        buildList {
            addAll(contextParameters)
            addIfNotNull(receiverType)
            addAll(parameters)
            add(rawReturnType)
        }

    val functionTypeId = ClassId(kind.packageFqName, kind.numberedClassName(receiverAndParameterTypes.size - 1))
    val attributes = when {
        contextParameters.isNotEmpty() -> ConeAttributes.create(
            buildList {
                add(CompilerConeAttributes.ContextFunctionTypeParams(contextParameters.size))
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
        isMarkedNullable = false,
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
    return ConeClassLikeTypeImpl(classId.toLookupTag(), arguments.toTypedArray(), isMarkedNullable = false)
}

fun BodyResolveComponents.buildResolvedQualifierForClass(
    symbol: FirClassLikeSymbol<*>,
    sourceElement: KtSourceElement?,
    // Note: we need type arguments here, see e.g. testIncompleteConstructorCall in diagnostic group
    typeArgumentsForQualifier: List<FirTypeProjection> = emptyList(),
    diagnostic: ConeDiagnostic? = null,
    nonFatalDiagnostics: List<ConeDiagnostic> = emptyList(),
    annotations: List<FirAnnotation> = emptyList(),
): FirResolvedQualifier {
    return buildResolvedQualifierForClass(
        symbol,
        sourceElement,
        symbol.classId.packageFqName,
        symbol.classId.relativeClassName,
        typeArgumentsForQualifier,
        diagnostic,
        nonFatalDiagnostics,
        annotations,
        explicitParent = null,
    )
}

fun BodyResolveComponents.buildResolvedQualifierForClass(
    symbol: FirClassLikeSymbol<*>?,
    sourceElement: KtSourceElement?,
    packageFqName: FqName,
    relativeClassName: FqName?,
    typeArgumentsForQualifier: List<FirTypeProjection>,
    diagnostic: ConeDiagnostic?,
    nonFatalDiagnostics: List<ConeDiagnostic>?,
    annotations: List<FirAnnotation>,
    explicitParent: FirResolvedQualifier?
): FirResolvedQualifier {
    val builder: FirAbstractResolvedQualifierBuilder = if (diagnostic == null) {
        FirResolvedQualifierBuilder()
    } else {
        FirErrorResolvedQualifierBuilder().apply { this.diagnostic = diagnostic }
    }

    // If we resolve to some qualifier, the parent can't have implicitly resolved to the companion object.
    // In a case like
    // class Foo { companion object { class Bar } }
    // Foo.Bar will be unresolved.
    if (explicitParent?.resolvedToCompanionObject == true) {
        explicitParent.replaceResolvedToCompanionObject(false)
        explicitParent.resultType = session.builtinTypes.unitType.coneType
    }

    return builder.apply {
        this.source = sourceElement
        this.packageFqName = packageFqName
        this.relativeClassFqName = relativeClassName
        this.typeArguments.addAll(typeArgumentsForQualifier)
        this.symbol = symbol
        nonFatalDiagnostics?.let(this.nonFatalDiagnostics::addAll)
        this.annotations.addAll(annotations)
        this.explicitParent = explicitParent
    }.build().apply {
        if (symbol?.classId?.isLocal == true) {
            resultType = typeForQualifierByDeclaration(symbol.fir, session, element = this@apply, file)
                ?.also { replaceCanBeValue(true) }
                ?: session.builtinTypes.unitType.coneType
        } else {
            setTypeOfQualifier(this@buildResolvedQualifierForClass)
        }
    }
}

fun FirResolvedQualifier.setTypeOfQualifier(components: BodyResolveComponents) {
    val classSymbol = symbol
    if (classSymbol != null) {
        classSymbol.lazyResolveToPhase(FirResolvePhase.TYPES)
        val declaration = classSymbol.fir
        if (declaration !is FirTypeAlias || typeArguments.isEmpty()) {
            val typeByDeclaration = typeForQualifierByDeclaration(declaration, components.session, element = this, components.file)
            if (typeByDeclaration != null) {
                this.resultType = typeByDeclaration
                replaceCanBeValue(true)
                return
            }
        }
    }
    this.resultType = components.session.builtinTypes.unitType.coneType
}

internal fun typeForReifiedParameterReference(parameterReferenceBuilder: FirResolvedReifiedParameterReferenceBuilder): ConeLookupTagBasedType {
    val typeParameterSymbol = parameterReferenceBuilder.symbol
    return typeParameterSymbol.constructType()
}

internal fun typeForQualifierByDeclaration(
    declaration: FirDeclaration, session: FirSession, element: FirElement, file: FirFile
): ConeKotlinType? {
    if (declaration is FirTypeAlias) {
        val expandedDeclaration = declaration.expandedConeType?.lookupTag?.toSymbol(session)?.fir ?: return null
        return typeForQualifierByDeclaration(expandedDeclaration, session, element, file)
    }
    if (declaration is FirRegularClass) {
        if (declaration.classKind == ClassKind.OBJECT) {
            return declaration.symbol.constructType()
        } else {
            val companionObjectSymbol = declaration.companionObjectSymbol
            if (companionObjectSymbol != null) {
                session.lookupTracker?.recordCompanionLookup(companionObjectSymbol.classId, element.source, file.source)
                return companionObjectSymbol.constructType()
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
                // The diagnostic is reported on the callee reference, no need to report it again on the error type ref.
                diagnostic = ConeUnreportedDuplicateDiagnostic(calleeReference.diagnostic)
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
            val possibleImplicitReceivers = implicitValueStorage[labelName]
            buildResolvedTypeRef {
                source = null
                coneType = when {
                    possibleImplicitReceivers.size >= 2 -> ConeErrorType(
                        ConeSimpleDiagnostic("Ambiguous this@$labelName", DiagnosticKind.AmbiguousLabel)
                    )
                    possibleImplicitReceivers.isEmpty() -> ConeErrorType(
                        ConeSimpleDiagnostic("Unresolved this@$labelName", DiagnosticKind.UnresolvedLabel)
                    )
                    else -> possibleImplicitReceivers.single().type
                }
            }
        }
        else -> errorWithAttachment("Failed to extract type from: ${calleeReference::class.simpleName}") {
            withFirEntry("reference", calleeReference)
        }
    }
}

private fun BodyResolveComponents.typeFromSymbol(symbol: FirBasedSymbol<*>): FirResolvedTypeRef {
    return when (symbol) {
        is FirSyntheticPropertySymbol -> typeFromSymbol(symbol.getterSymbol!!.delegateFunctionSymbol)
        is FirCallableSymbol<*> -> {
            val returnTypeRef = returnTypeCalculator.tryCalculateReturnType(symbol.fir)
            returnTypeRef.copyWithNewSource(null)
        }
        is FirClassifierSymbol<*> -> {
            buildResolvedTypeRef {
                source = null
                coneType = symbol.constructType()
            }
        }
        else -> errorWithAttachment("Failed to extract type from symbol: ${symbol::class.java}") {
            withFirEntry("declaration", symbol.fir)
        }
    }
}

private val ConeKotlinType.isKindOfNothing
    get() = lowerBoundIfFlexible().let { it.isNothing || it.isNullableNothing }

fun BodyResolveComponents.transformExpressionUsingSmartcastInfo(expression: FirExpression): FirExpression {
    val (stability, typesFromSmartCast) = dataFlowAnalyzer.getTypeUsingSmartcastInfo(expression) ?: return expression

    val originalTypeWithAliases = expression.resolvedType
    val originalType = originalTypeWithAliases.fullyExpandedType(session)

    val allTypes = if (originalType !is ConeStubType) typesFromSmartCast + originalType else typesFromSmartCast
    if (allTypes.all { it is ConeDynamicType }) return expression

    val intersectedType = ConeTypeIntersector.intersectTypes(session.typeContext, allTypes)
    if (intersectedType == originalType && intersectedType !is ConeDynamicType) return expression

    return buildSmartCastExpression {
        originalExpression = expression
        smartcastStability = stability
        smartcastType = buildResolvedTypeRef {
            source = expression.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
            coneType = intersectedType
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
            smartcastTypeWithoutNullableNothing = buildResolvedTypeRef {
                source = expression.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
                coneType = ConeTypeIntersector.intersectTypes(session.typeContext, allTypes.filter { !it.isKindOfNothing })
            }
        }
        this.typesFromSmartCast = typesFromSmartCast
        coneTypeOrNull = if (stability == SmartcastStability.STABLE_VALUE) intersectedType else originalTypeWithAliases
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
            type.withNullability(nullable = true, session.typeContext)
        }
        // Branch for things that shouldn't be used as expressions.
        // They are forced to return not-null `Unit`, regardless of the receiver.
        else -> {
            StandardClassIds.Unit.constructClassLikeType(emptyArray(), isMarkedNullable = false)
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
    return resultingSubstitutor.safeSubstitute(system, candidate.substitutor.substituteOrSelf(coneType)) as ConeKotlinType
}

/**
 * The containing symbol is resolved using the declaration-site session.
 * The semantics is similar to [FirBasedSymbol<*>.getContainingClassSymbol][org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol],
 * see its KDoc for an example.
 */
fun FirCallableDeclaration.getContainingClass(): FirRegularClass? =
    this.containingClassLookupTag()?.let { lookupTag ->
        lookupTag.toRegularClassSymbol(moduleData.session)?.fir
    }

internal fun FirFunction.areNamedArgumentsForbiddenIgnoringOverridden(): Boolean =
    forbiddenNamedArgumentsTargetOrNullIgnoringOverridden() != null

private fun FirFunction.forbiddenNamedArgumentsTargetOrNullIgnoringOverridden(): ForbiddenNamedArgumentsTarget? =
    forbiddenNamedArgumentsTargetOrNull(originScope = null)

/**
 * Returns a non-null value when named arguments are forbidden for calls to this function.
 *
 * When [originScope] is provided, overrides of the function will be checked.
 * If one of the overridden functions allows named arguments, `null` will be returned.
 *
 * One example of this behavior is a Java function that overrides a Kotlin function.
 * In this case, `null` will be returned, if [originScope] is provided.
 * Otherwise, [ForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION] will be returned.
 *
 * To check if a function allows named arguments regardless of its overrides, it is recommended to use
 * [FirFunction.areNamedArgumentsForbiddenIgnoringOverridden].
 */
internal fun FirFunction.forbiddenNamedArgumentsTargetOrNull(originScope: FirTypeScope?): ForbiddenNamedArgumentsTarget? {
    if (hasStableParameterNames) return null

    return when (origin) {
        FirDeclarationOrigin.ImportedFromObjectOrStatic ->
            importedFromObjectOrStaticData?.original?.forbiddenNamedArgumentsTargetOrNullIgnoringOverridden()

        FirDeclarationOrigin.IntersectionOverride, is FirDeclarationOrigin.SubstitutionOverride, FirDeclarationOrigin.Delegated -> {
            val initial = unwrapFakeOverridesOrDelegated().forbiddenNamedArgumentsTargetOrNullIgnoringOverridden() ?: return null
            initial.takeUnless { symbol.hasOverrideThatAllowsNamedArguments(originScope) }
        }

        FirDeclarationOrigin.Enhancement -> {
            ForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION.takeUnless { symbol.hasOverrideThatAllowsNamedArguments(originScope) }
        }

        FirDeclarationOrigin.BuiltIns, FirDeclarationOrigin.BuiltInsFallback -> ForbiddenNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE
        is FirDeclarationOrigin.Plugin -> null // TODO: figure out what to do with plugin generated functions
        else -> ForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION
    }
}

private fun FirFunctionSymbol<*>.hasOverrideThatAllowsNamedArguments(originScope: FirTypeScope?): Boolean {
    var result = false
    if (this is FirNamedFunctionSymbol) {
        originScope?.processOverriddenFunctions(this) {
            // If an override allows named arguments, it overrides the initial result.
            if (!it.fir.areNamedArgumentsForbiddenIgnoringOverridden()) {
                result = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }
    }
    return result
}

@OptIn(ExperimentalContracts::class)
fun FirExpression?.isIntegerLiteralOrOperatorCall(): Boolean {
    contract {
        returns(true) implies (this@isIntegerLiteralOrOperatorCall != null)
    }
    return when (this) {
        is FirLiteralExpression -> kind == ConstantValueKind.Int
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

            (symbol as? FirConstructorSymbol)?.typeAliasForConstructor?.let {
                if (!session.visibilityChecker.isVisible(it.fir, candidate)) {
                    return ConeVisibilityError(it)
                }
            }

            val declaration = symbol.fir
            if (declaration is FirMemberDeclaration &&
                session.visibilityChecker.isVisible(declaration, candidate, skipCheckForContainingClassVisibility = true)
            ) {
                // We can have declarations that are visible by themselves, but some containing declaration is invisible.
                // We report the nearest invisible containing declaration, otherwise we'll get a confusing diagnostic like
                // Cannot access 'foo', it is public in 'Bar'.
                declaration
                    .parentDeclarationSequence(
                        session,
                        candidate.dispatchReceiver?.expression,
                        candidate.callInfo.containingDeclarations
                    )?.firstOrNull {
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

            ConeVisibilityError(symbol)
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
    get() = ConeTypeParameterTypeImpl(toLookupTag(), isMarkedNullable = false)

fun ConeClassLikeLookupTag.isRealOwnerOf(declarationSymbol: FirCallableSymbol<*>): Boolean =
    this == declarationSymbol.dispatchReceiverClassLookupTagOrNull()

val FirUserTypeRef.shortName: Name get() = qualifier.last().name

val FirThisReference.referencedMemberSymbol: FirBasedSymbol<*>?
    get() = when (val boundSymbol = boundSymbol) {
        is FirReceiverParameterSymbol -> boundSymbol.containingDeclarationSymbol
        is FirValueParameterSymbol -> boundSymbol.containingDeclarationSymbol
        is FirClassSymbol -> boundSymbol
        null -> null
        is FirTypeParameterSymbol, is FirTypeAliasSymbol -> errorWithAttachment(
            message = "Unexpected FirThisOwnerSymbol ${boundSymbol::class.simpleName}"
        ) {
            withFirEntry("FIR", fir = boundSymbol.fir)
        }
    }

internal fun FirBasedSymbol<*>.getExpectedReceiverType(): ConeKotlinType? {
    val callableSymbol = this as? FirCallableSymbol<*> ?: return null
    return callableSymbol.fir.let { it.receiverParameter?.typeRef?.coneType ?: (it as? FirConstructor)?.outerTypeIfTypeAlias }
}