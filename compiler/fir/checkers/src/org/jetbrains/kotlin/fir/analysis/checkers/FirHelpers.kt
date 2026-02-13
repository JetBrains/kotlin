/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.isExpression
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FinallyBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.multipleDelegatesWithTheSameSignature
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens.VAL_VAR
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.AnnotationTargetList
import org.jetbrains.kotlin.resolve.AnnotationTargetListForDeprecation
import org.jetbrains.kotlin.resolve.AnnotationTargetLists
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.getChildren
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private val INLINE_ONLY_ANNOTATION_CLASS_ID: ClassId = ClassId.topLevel(FqName("kotlin.internal.InlineOnly"))

context(context: CheckerContext)
fun FirClass.unsubstitutedScope(): FirTypeScope =
    this.unsubstitutedScope(
        withForcedTypeCalculator = true,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )

context(context: CheckerContext)
fun FirClassSymbol<*>.unsubstitutedScope(): FirTypeScope = unsubstitutedScope(context)

fun FirClassSymbol<*>.unsubstitutedScope(context: CheckerContext): FirTypeScope =
    this.unsubstitutedScope(
        context.sessionHolder.session,
        context.sessionHolder.scopeSession,
        withForcedTypeCalculator = true,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )

context(context: CheckerContext)
fun FirClassSymbol<*>.declaredMemberScope(): FirContainingNamesAwareScope =
    this.declaredMemberScope(
        context.sessionHolder.session,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )

fun FirTypeRef.toClassLikeSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return coneType.toClassLikeSymbol(session)
}

/**
 * Returns true if this is a supertype of other.
 */
fun FirClassSymbol<*>.isSupertypeOf(other: FirClassSymbol<*>, session: FirSession): Boolean {
    /**
     * Hides additional parameters.
     */
    fun FirClassSymbol<*>.isSupertypeOf(other: FirClassSymbol<*>, exclude: MutableSet<FirClassSymbol<*>>): Boolean {
        for (it in other.resolvedSuperTypeRefs) {
            val candidate = it.toClassLikeSymbol(session)?.fullyExpandedClass(session) ?: continue

            if (candidate in exclude) {
                continue
            }

            exclude.add(candidate)

            if (candidate == this) {
                return true
            }

            if (this.isSupertypeOf(candidate, exclude)) {
                return true
            }
        }

        return false
    }

    return isSupertypeOf(other, mutableSetOf())
}

fun ConeKotlinType.isValueClass(session: FirSession): Boolean {
    // Value classes have `inline` or `value` modifier in FIR
    return toRegularClassSymbol(session)?.isInlineOrValue == true
}

fun ConeKotlinType.isSingleFieldValueClass(session: FirSession): Boolean = with(session.typeContext) {
    isRecursiveSingleFieldValueClassType(session) || typeConstructor().isInlineClass()
}

private fun ConeKotlinType.isRecursiveSingleFieldValueClassType(session: FirSession) =
    isRecursiveValueClassType(hashSetOf(), session, onlyInline = true)

fun ConeKotlinType.isRecursiveValueClassType(session: FirSession): Boolean =
    isRecursiveValueClassType(hashSetOf(), session, onlyInline = false)

private fun ConeKotlinType.isRecursiveValueClassType(visited: HashSet<ConeKotlinType>, session: FirSession, onlyInline: Boolean): Boolean {
    val asRegularClass = this.toRegularClassSymbol(session)?.takeIf { it.isInlineOrValueClass() } ?: return false
    val primaryConstructor = asRegularClass.primaryConstructorIfAny(session) ?: return false

    if (primaryConstructor.valueParameterSymbols.size > 1 && onlyInline) return false
    return !visited.add(this) || primaryConstructor.valueParameterSymbols.any {
        it.resolvedReturnType.isRecursiveValueClassType(visited, session, onlyInline)
    }.also { visited.remove(this) }
}

context(context: CheckerContext)
fun FirClassLikeSymbol<*>.outerClassSymbol(): FirClassLikeSymbol<*>? {
    if (this !is FirClassSymbol<*>) return null
    return getContainingDeclaration(context.session)
}

/**
 * Returns the closest to the end of context.containingDeclarations
 * item like FirRegularClass or FirAnonymousObject
 * or null if no such item could be found.
 */
fun CheckerContext.findClosestClassOrObject(): FirClassSymbol<*>? {
    for (it in containingDeclarations.asReversed()) {
        if (
            it is FirRegularClassSymbol ||
            it is FirAnonymousObjectSymbol
        ) {
            return it
        }
    }

    return null
}

context(context: CheckerContext)
fun FirNamedFunctionSymbol.overriddenFunctions(
    containingClass: FirClassSymbol<*>,
): Collection<FirFunctionSymbol<*>> {
    return overriddenFunctions(containingClass, context.session, context.scopeSession)
}

fun FirClass.collectSupertypesWithDelegates(): Map<FirTypeRef, FirFieldSymbol?> {
    val fieldsMap = delegateFieldsMap ?: emptyMap()
    return superTypeRefs.mapIndexed { index, it -> it to fieldsMap[index] }.toMap()
}

/**
 * Returns the modality of the class
 */
fun FirClass.modality(): Modality? {
    return when (this) {
        is FirRegularClass -> modality
        else -> Modality.FINAL
    }
}

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.modality(): Modality? {
    lazyResolveToPhase(FirResolvePhase.STATUS)
    return fir.modality()
}

/**
 * Returns a set of [Modality] modifiers which are redundant for the given [FirMemberDeclaration]. If a modality modifier is redundant, the
 * declaration's modality won't be changed by the modifier.
 */
context(context: CheckerContext)
fun FirMemberDeclaration.redundantModalities(defaultModality: Modality): Set<Modality> {
    if (this is FirRegularClass) {
        return when (classKind) {
            ClassKind.INTERFACE -> setOf(Modality.ABSTRACT, Modality.OPEN)
            else -> setOf(defaultModality)
        }
    }

    val containingClass = context.findClosestClassOrObject() ?: return setOf(defaultModality)

    return when {
        isOverride && !containingClass.isFinal -> setOf(Modality.OPEN)
        containingClass.isInterface -> when {
            hasBody() -> setOf(Modality.OPEN)
            else -> setOf(Modality.ABSTRACT, Modality.OPEN)
        }
        else -> setOf(defaultModality)
    }
}

private fun FirDeclaration.hasBody(): Boolean = when (this) {
    is FirNamedFunction -> this.body != null && this.body !is FirEmptyExpressionBlock
    is FirProperty -> this.setter?.body !is FirEmptyExpressionBlock? || this.getter?.body !is FirEmptyExpressionBlock?
    else -> false
}

/**
 * Finds any non-interface supertype and returns it
 * or null if couldn't find any.
 */
context(context: CheckerContext)
fun FirClass.findNonInterfaceSupertype(): FirTypeRef? {
    for (superTypeRef in superTypeRefs) {
        val lookupTag = (superTypeRef.coneType as? ConeClassLikeType)?.lookupTag ?: continue

        val symbol = lookupTag.toClassSymbol() ?: continue

        if (symbol.classKind != ClassKind.INTERFACE) {
            return superTypeRef
        }
    }

    return null
}

val FirFunctionCall.isIterator: Boolean
    get() = this.calleeReference.name == SpecialNames.ITERATOR

fun ConeKotlinType.isSubtypeOfThrowable(session: FirSession): Boolean =
    session.builtinTypes.throwableType.coneType.isSupertypeOf(session.typeContext, this.fullyExpandedType(session))

val FirValueParameter.hasValOrVar: Boolean
    get() {
        val source = this.source ?: return false
        return source.getChild(VAL_VAR) != null
    }

fun KotlinTypeMarker.isSupertypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
    type != null && AbstractTypeChecker.isSubtypeOf(context, type, this)

fun FirCallableDeclaration.isInlineOnly(session: FirSession): Boolean = symbol.isInlineOnly(session)

fun FirCallableSymbol<*>.isInlineOnly(session: FirSession): Boolean =
    isInline && hasAnnotation(INLINE_ONLY_ANNOTATION_CLASS_ID, session)

fun isSubtypeForTypeMismatch(context: ConeInferenceContext, subtype: ConeKotlinType, supertype: ConeKotlinType): Boolean {
    val subtypeFullyExpanded = subtype.fullyExpandedType(context.session)
    val supertypeFullyExpanded = supertype.fullyExpandedType(context.session)
    return AbstractTypeChecker.isSubtypeOf(
        context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = false,
            dnnTypesEqualToFlexible = false
        ),
        subtypeFullyExpanded,
        supertypeFullyExpanded
    )
}


/**
 * Get the [ImplementationStatus] for this member.
 * The containing symbol is resolved using the declaration-site session.
 *
 * @param parentClassSymbol the contextual class for this query.
 */
context(sessionHolder: SessionAndScopeSessionHolder) @OptIn(ScopeFunctionRequiresPrewarm::class)
fun FirCallableSymbol<*>.getImplementationStatus(
    parentClassSymbol: FirClassSymbol<*>
): ImplementationStatus {
    val containingClassSymbol = getContainingClassSymbol()
    val symbol = this

    if (this.multipleDelegatesWithTheSameSignature == true && containingClassSymbol == parentClassSymbol) {
        return ImplementationStatus.AMBIGUOUSLY_INHERITED
    }

    if (symbol is FirIntersectionCallableSymbol) {
        val dispatchReceiverScope = symbol.dispatchReceiverScope()
        val memberWithBaseScope = MemberWithBaseScope(symbol, dispatchReceiverScope)
        val nonSubsumed = memberWithBaseScope.getNonSubsumedOverriddenSymbols()

        if (containingClassSymbol === parentClassSymbol && !memberWithBaseScope.isTrivialIntersection() && nonSubsumed.subjectToManyNotImplemented()) {
            return ImplementationStatus.AMBIGUOUSLY_INHERITED
        }

        var hasAbstractFromClass = false
        var hasInterfaceDelegation = false
        var hasAbstractVar = false
        var hasImplementation = false
        var hasImplementationVar = false

        for (intersection in nonSubsumed) {
            val unwrapped = intersection.unwrapFakeOverrides()
            val isVar = unwrapped is FirPropertySymbol && unwrapped.isVar
            val isFromClass = unwrapped.getContainingClassSymbol()?.classKind == ClassKind.CLASS

            if (intersection.isAbstract) {
                if (isFromClass) {
                    hasAbstractFromClass = true
                }
                if (isVar) {
                    hasAbstractVar = true
                }
            } else {
                if (intersection.origin == FirDeclarationOrigin.Delegated) {
                    hasInterfaceDelegation = true
                }
                if (isFromClass) {
                    hasImplementation = true
                    if (isVar) {
                        hasImplementationVar = true
                    }
                }
            }
        }

        // In Java 8, non-abstract intersection overrides having abstract symbol from base class
        // still should be implemented in current class (even when they have default interface implementation)
        // Exception to the rule above: interface implementation via delegation
        if (hasAbstractFromClass && !hasInterfaceDelegation) {
            return ImplementationStatus.NOT_IMPLEMENTED
        }
        if (hasAbstractVar && hasImplementation && !hasImplementationVar) {
            return ImplementationStatus.VAR_IMPLEMENTED_BY_VAL
        }
    }

    when (symbol) {
        is FirNamedFunctionSymbol -> {
            if (
                parentClassSymbol is FirRegularClassSymbol &&
                parentClassSymbol.isData &&
                symbol.matchesDataClassSyntheticMemberSignatures
            ) {
                return ImplementationStatus.INHERITED_OR_SYNTHESIZED
            }
        }
        is FirFieldSymbol -> if (symbol.isJavaOrEnhancement) return ImplementationStatus.CANNOT_BE_IMPLEMENTED
    }

    return when {
        isFinal -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        containingClassSymbol === parentClassSymbol && (origin == FirDeclarationOrigin.Source || origin == FirDeclarationOrigin.Precompiled) ->
            ImplementationStatus.ALREADY_IMPLEMENTED
        isAbstract -> ImplementationStatus.NOT_IMPLEMENTED
        else -> ImplementationStatus.INHERITED_OR_SYNTHESIZED
    }
}

private fun List<FirCallableSymbol<*>>.subjectToManyNotImplemented(): Boolean {
    var nonAbstractCountInClass = 0
    var nonAbstractCountInInterface = 0
    var abstractCountInInterface = 0
    for (intersectionSymbol in this) {
        val containingClassSymbol = intersectionSymbol.getContainingClassSymbol() as? FirRegularClassSymbol
        val hasInterfaceContainer = containingClassSymbol?.classKind == ClassKind.INTERFACE
        if (intersectionSymbol.modality != Modality.ABSTRACT) {
            if (hasInterfaceContainer) {
                nonAbstractCountInInterface++
            } else {
                nonAbstractCountInClass++
            }
        } else if (hasInterfaceContainer) {
            abstractCountInInterface++
        }
        if (nonAbstractCountInClass + nonAbstractCountInInterface > 1) {
            return true
        }
        if (nonAbstractCountInInterface > 0 && abstractCountInInterface > 0) {
            return true
        }
    }
    return false
}

private val FirNamedFunctionSymbol.matchesDataClassSyntheticMemberSignatures: Boolean
    get() {
        val name = callableId.callableName
        return receiverParameterSymbol == null &&
                !hasContextParameters &&
                (name == OperatorNameConventions.EQUALS && matchesEqualsSignature) ||
                (name == HASHCODE_NAME && matchesHashCodeSignature) ||
                (name == OperatorNameConventions.TO_STRING && matchesToStringSignature)
    }

// NB: we intentionally do not check return types
private val FirNamedFunctionSymbol.matchesEqualsSignature: Boolean
    get() {
        val valueParameters = valueParameterSymbols
        return valueParameters.size == 1 && valueParameters[0].resolvedReturnType.isNullableAny
    }

private val FirNamedFunctionSymbol.matchesHashCodeSignature: Boolean
    get() = valueParameterSymbols.isEmpty()

private val FirNamedFunctionSymbol.matchesToStringSignature: Boolean
    get() = valueParameterSymbols.isEmpty()

val Name.isDelegated: Boolean get() = asString().startsWith("\$\$delegate_")

val ConeTypeProjection.isConflictingOrNotInvariant: Boolean get() = kind != ProjectionKind.INVARIANT || this is ConeKotlinTypeConflictingProjection

val CheckerContext.secondToLastContainer: FirElement?
    get() = nthLastContainer(2)

fun CheckerContext.nthLastContainer(n: Int): FirElement? = containingElements.let { it.getOrNull(it.size - n) }

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkTypeMismatch(
    lValueOriginalType: ConeKotlinType,
    assignment: FirVariableAssignment?,
    rValue: FirExpression,
    source: KtSourceElement,
    isInitializer: Boolean
) {
    var lValueType = lValueOriginalType
    var rValueType = rValue.resolvedType
    if (source.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
        if (!lValueType.isMarkedOrFlexiblyNullable && rValueType.isMarkedOrFlexiblyNullable) {
            val tempType = rValueType
            rValueType = lValueType
            lValueType = tempType
        }
    }

    val typeContext = context.session.typeContext

    // there is nothing to report if types are matching
    if (isSubtypeForTypeMismatch(typeContext, subtype = rValueType, supertype = lValueType)) return

    val resolvedSymbol = assignment?.calleeReference?.toResolvedCallableSymbol() as? FirPropertySymbol
    val receiverType = (assignment?.extensionReceiver ?: assignment?.dispatchReceiver)?.resolvedType

    when {
        resolvedSymbol != null &&
                receiverType != null &&
                lValueType is ConeCapturedType &&
                lValueType.constructor.projection.kind.let { it == ProjectionKind.STAR || it == ProjectionKind.OUT } -> {
            reporter.reportOn(
                assignment.source,
                FirErrors.SETTER_PROJECTED_OUT,
                receiverType,
                lValueType.projectionKindAsString(),
                resolvedSymbol
            )
        }
        rValue.isNullLiteral && !lValueType.isMarkedOrFlexiblyNullable -> {
            reporter.reportOn(rValue.source, FirErrors.NULL_FOR_NONNULL_TYPE, lValueType)
        }
        source.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement || assignment?.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement -> {
            if (!lValueType.isMarkedOrFlexiblyNullable && rValueType.isMarkedOrFlexiblyNullable) {
                val tempType = rValueType
                rValueType = lValueType
                lValueType = tempType
            }
            if (rValueType.isUnit) {
                reporter.reportOn(source, FirErrors.INC_DEC_SHOULD_NOT_RETURN_UNIT)
            } else {
                reporter.reportOn(source, FirErrors.RESULT_TYPE_MISMATCH, lValueType, rValueType)
            }
        }
        else -> {
            if (reportReturnTypeMismatchInLambda(
                    lValueType = lValueType.fullyExpandedType(),
                    rValue = rValue,
                    rValueType = rValueType.fullyExpandedType(),
                )
            ) return

            val factory = when {
                !isInitializer -> FirErrors.ASSIGNMENT_TYPE_MISMATCH
                source.elementType == KtNodeTypes.BACKING_FIELD -> FirErrors.FIELD_INITIALIZER_TYPE_MISMATCH
                else -> FirErrors.INITIALIZER_TYPE_MISMATCH
            }

            reporter.reportOn(
                assignment?.source ?: source,
                factory,
                lValueType,
                rValueType,
                context.session.typeContext.isTypeMismatchDueToNullability(rValueType, lValueType)
            )
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
/**
 * Instead of reporting type mismatch on the whole lambda, tries to report more granular type mismatch on the return expressions.
 */
private fun reportReturnTypeMismatchInLambda(
    lValueType: ConeKotlinType,
    rValue: FirExpression,
    rValueType: ConeKotlinType,
): Boolean {
    if (rValue !is FirAnonymousFunctionExpression) return false
    if (!lValueType.isSomeFunctionType(context.session) && lValueType.classId != StandardClassIds.Function) return false

    val expectedReturnType = lValueType.typeArguments.lastOrNull()?.type ?: return false

    val rValueTypeWithExpectedReturnType = rValueType.withArguments(
        rValueType.typeArguments.dropLast(1).plus(expectedReturnType).toTypedArray()
    )

    if (!isSubtypeForTypeMismatch(context.session.typeContext, rValueTypeWithExpectedReturnType, lValueType)) return false

    var reported = false

    for (expression in rValue.anonymousFunction.symbol.getReturnedExpressions()) {
        if (!isSubtypeForTypeMismatch(context.session.typeContext, expression.resolvedType, expectedReturnType)) {
            reported = true
            reporter.reportOn(
                expression.source,
                FirErrors.RETURN_TYPE_MISMATCH,
                expectedReturnType,
                expression.resolvedType,
                rValue.anonymousFunction,
                context.session.typeContext.isTypeMismatchDueToNullability(expression.resolvedType, expectedReturnType)
            )
        }
    }

    return reported
}

fun ConeCapturedType.projectionKindAsString(): String {
    return when (constructor.projection.kind) {
        ProjectionKind.OUT -> "out"
        ProjectionKind.IN -> "in"
        ProjectionKind.STAR -> "star"
        ProjectionKind.INVARIANT -> error("no projection")
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkCondition(condition: FirExpression) {
    val coneType = condition.resolvedType.fullyExpandedType().lowerBoundIfFlexible()
    if (coneType !is ConeErrorType &&
        !coneType.isSubtypeOf(context.session.typeContext, context.session.builtinTypes.booleanType.coneType)
    ) {
        if (condition is FirFunctionCall &&
            condition.origin == FirFunctionCallOrigin.Operator &&
            condition.calleeReference.name == OperatorNameConventions.HAS_NEXT
        ) {
            reporter.reportOn(condition.source, FirErrors.HAS_NEXT_FUNCTION_TYPE_MISMATCH, coneType)
        } else {
            reporter.reportOn(
                condition.source,
                FirErrors.CONDITION_TYPE_MISMATCH,
                coneType,
                coneType.isNullableBoolean
            )
        }
    }
}

fun extractArgumentsTypeRefAndSource(typeRef: FirTypeRef?): List<FirTypeRefSource>? {
    if (typeRef !is FirResolvedTypeRef) return null
    val result = mutableListOf<FirTypeRefSource>()
    when (val delegatedTypeRef = typeRef.delegatedTypeRef) {
        is FirUserTypeRef -> {
            val qualifier = delegatedTypeRef.qualifier

            for (i in qualifier.size - 1 downTo 0) {
                for (typeArgument in qualifier[i].typeArgumentList.typeArguments) {
                    result.add(FirTypeRefSource((typeArgument as? FirTypeProjectionWithVariance)?.typeRef, typeArgument.source))
                }
            }
        }
        is FirFunctionTypeRef -> {
            val parameters = delegatedTypeRef.parameters

            for (contextParameter in delegatedTypeRef.contextParameterTypeRefs) {
                result.add(FirTypeRefSource(contextParameter, contextParameter.source))
            }
            delegatedTypeRef.receiverTypeRef?.let { result.add(FirTypeRefSource(it, it.source)) }
            for (valueParameter in parameters) {
                val valueParamTypeRef = valueParameter.returnTypeRef
                result.add(FirTypeRefSource(valueParamTypeRef, valueParamTypeRef.source))
            }
            val returnTypeRef = delegatedTypeRef.returnTypeRef
            result.add(FirTypeRefSource(returnTypeRef, returnTypeRef.source))
        }
        else -> return null
    }

    return result
}

data class FirTypeRefSource(val typeRef: FirTypeRef?, val source: KtSourceElement?) {
    override fun toString(): String {
        return "FirTypeRefSource(typeRef=${typeRef?.render()}, source=${source?.kind?.javaClass?.simpleName})"
    }
}

val FirClassLikeSymbol<*>.classKind: ClassKind?
    get() = (this as? FirClassSymbol<*>)?.classKind

val FirBasedSymbol<*>.typeParameterSymbols: List<FirTypeParameterSymbol>?
    get() = when (this) {
        is FirCallableSymbol<*> -> typeParameterSymbols
        is FirClassLikeSymbol<*> -> typeParameterSymbols
        else -> null
    }

/*
 * This is phase-safe version of similar function from FirCallCompleter
 *
 * Expect type is only being added to calls in a position of cast argument: foo() as R
 * And that call should be resolved to something materialize()-like: it returns its single generic parameter and doesn't have value parameters
 * fun <T> materialize(): T
 */
fun FirFunctionSymbol<*>.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameterSymbol = typeParameterSymbols.singleOrNull() ?: return false

    val returnType = resolvedReturnType

    if ((returnType.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag != typeParameterSymbol.toLookupTag()) return false

    fun FirTypeRef.isBadType() =
        coneType.contains { (it.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag == typeParameterSymbol.toLookupTag() }

    return valueParameterSymbols.none { it.resolvedReturnTypeRef.isBadType() }
            && resolvedReceiverTypeRef?.isBadType() != true
            && contextParameterSymbols.none { it.resolvedReturnTypeRef.isBadType() }
}

private val FirCallableDeclaration.isMember get() = dispatchReceiverType != null

@OptIn(SymbolInternals::class)
context(sessionHolder: SessionHolder)
fun getActualTargetList(container: FirBasedSymbol<*>): AnnotationTargetList {
    return getActualTargetList(container.fir, sessionHolder.session)
}

context(sessionHolder: SessionHolder)
fun getActualTargetList(container: FirAnnotationContainer): AnnotationTargetList {
    return getActualTargetList(container, sessionHolder.session)
}

fun getActualTargetList(container: FirAnnotationContainer, session: FirSession): AnnotationTargetList {
    val annotated =
        if (container is FirBackingField) {
            when {
                !container.propertySymbol.hasBackingField -> container.propertyIfBackingField
                container.propertySymbol.getContainingClassSymbol()?.classKind == ClassKind.ANNOTATION_CLASS -> {
                    @OptIn(AnnotationTargetListForDeprecation::class)
                    return TargetLists.T_MEMBER_PROPERTY_IN_ANNOTATION
                }
                else -> container
            }
        } else container

    return when (annotated) {
        is FirRegularClass -> {
            AnnotationTargetList(
                KotlinTarget.classActualTargets(
                    annotated.classKind, annotated.isInner, annotated.isCompanion,
                    isLocalClass = annotated.isReplSnippetDeclaration != true && annotated.isLocal
                )
            )
        }
        is FirEnumEntry -> AnnotationTargetList(
            KotlinTarget.classActualTargets(ClassKind.ENUM_ENTRY, annotated.isInner, isCompanionObject = false, isLocalClass = false)
        )
        is FirProperty -> {
            when {
                annotated.symbol is FirLocalPropertySymbol ->
                    when {
                        annotated.name == SpecialNames.DESTRUCT -> if (session.languageVersionSettings.supportsFeature(LanguageFeature.LocalVariableTargetedAnnotationOnDestructuring)) {
                            TargetLists.T_DESTRUCTURING_DECLARATION_NEW
                        } else {
                            TargetLists.T_DESTRUCTURING_DECLARATION
                        }
                        annotated.isCatchParameter == true -> TargetLists.T_CATCH_PARAMETER
                        annotated.isForLoopParameter == true -> TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
                        else -> TargetLists.T_LOCAL_VARIABLE
                    }
                annotated.isMember ->
                    if (annotated.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                        TargetLists.T_VALUE_PARAMETER_WITH_VAL
                    } else {
                        TargetLists.T_MEMBER_PROPERTY(annotated.hasBackingField, annotated.delegate != null)
                    }
                else ->
                    TargetLists.T_TOP_LEVEL_PROPERTY(annotated.hasBackingField, annotated.delegate != null)
            }
        }
        is FirValueParameter -> {
            when {
                annotated.hasValOrVar -> TargetLists.T_VALUE_PARAMETER_WITH_VAL
                else -> TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
            }
        }
        is FirConstructor -> TargetLists.T_CONSTRUCTOR
        is FirAnonymousFunction -> {
            TargetLists.T_FUNCTION_EXPRESSION
        }
        is FirNamedFunction -> {
            when {
                annotated.status.visibility == Visibilities.Local -> TargetLists.T_LOCAL_FUNCTION
                annotated.isMember -> TargetLists.T_MEMBER_FUNCTION
                else -> TargetLists.T_TOP_LEVEL_FUNCTION
            }
        }
        is FirTypeAlias -> TargetLists.T_TYPEALIAS
        is FirPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
        is FirBackingField -> TargetLists.T_BACKING_FIELD
        is FirFile -> TargetLists.T_FILE
        is FirTypeParameter -> TargetLists.T_TYPE_PARAMETER
        is FirReceiverParameter -> TargetLists.T_TYPE_REFERENCE
        is FirAnonymousInitializer -> TargetLists.T_INITIALIZER
        is FirAnonymousObject ->
            if (annotated.source?.kind == KtFakeSourceElementKind.EnumInitializer) {
                AnnotationTargetList(
                    KotlinTarget.classActualTargets(
                        ClassKind.ENUM_ENTRY,
                        isInnerClass = false,
                        isCompanionObject = false,
                        isLocalClass = false
                    )
                )
            } else {
                TargetLists.T_OBJECT_LITERAL
            }
//            TODO, KT-59819: properly implement this case
//            is KtLambdaExpression -> TargetLists.T_FUNCTION_LITERAL
        else -> TargetLists.EMPTY
    }
}

private typealias TargetLists = AnnotationTargetLists

fun FirQualifiedAccessExpression.explicitReceiverIsNotSuperReference(): Boolean {
    return this.explicitReceiver !is FirSuperReceiverExpression
}


internal val KtSourceElement.defaultValueForParameter: KtSourceElement?
    get() = when (this) {
        is KtPsiSourceElement -> (psi as? KtParameter)?.defaultValue?.toKtPsiSourceElement()
        is KtLightSourceElement -> findDefaultValue(this)
    }

private fun findDefaultValue(source: KtLightSourceElement): KtLightSourceElement? {
    var defaultValue: LighterASTNode? = null
    var defaultValueOffset = source.startOffset

    val nodes = source.lighterASTNode.getChildren(source.treeStructure)
    for (node in nodes) {
        if (node.isExpression()) {
            defaultValue = node
            break
        } else {
            defaultValueOffset += node.endOffset - node.startOffset
        }
    }
    if (defaultValue == null) return null

    return defaultValue.toKtLightSourceElement(
        source.treeStructure,
        startOffset = defaultValueOffset,
        endOffset = defaultValueOffset + defaultValue.textLength,
    )
}

context(context: CheckerContext)
@OptIn(ScopeFunctionRequiresPrewarm::class)
fun FirCallableSymbol<*>.directOverriddenSymbolsSafe(): List<FirCallableSymbol<*>> {
    if (!this.isOverride) return emptyList()
    val scope = containingClassUnsubstitutedScope() ?: return emptyList()
    scope.processFunctionsByName(this.name) { }
    return scope.getDirectOverriddenMembers(this, true)
}

context(context: CheckerContext)
fun FirNamedFunctionSymbol.directOverriddenFunctionsSafe(): List<FirNamedFunctionSymbol> = directOverriddenFunctionsSafe(context)

fun FirNamedFunctionSymbol.directOverriddenFunctionsSafe(context: CheckerContext): List<FirNamedFunctionSymbol> {
    with(context) {
        @Suppress("UNCHECKED_CAST")
        return directOverriddenSymbolsSafe() as List<FirNamedFunctionSymbol>
    }
}

context(context: CheckerContext)
fun FirPropertySymbol.directOverriddenPropertiesSafe(): List<FirPropertySymbol> = directOverriddenPropertiesSafe(context)

fun FirPropertySymbol.directOverriddenPropertiesSafe(context: CheckerContext): List<FirPropertySymbol> {
    with(context) {
        @Suppress("UNCHECKED_CAST")
        return directOverriddenSymbolsSafe() as List<FirPropertySymbol>
    }
}

context(context: CheckerContext)
@OptIn(ScopeFunctionRequiresPrewarm::class)
inline fun FirNamedFunctionSymbol.processOverriddenFunctionsSafe(
    crossinline action: (FirNamedFunctionSymbol) -> Unit,
) {
    processOverriddenFunctionsWithActionSafe {
        action(it)
        ProcessorAction.NEXT
    }
}

context(context: CheckerContext)
@OptIn(ScopeFunctionRequiresPrewarm::class)
fun FirNamedFunctionSymbol.processOverriddenFunctionsWithActionSafe(
    action: (FirNamedFunctionSymbol) -> ProcessorAction,
) {
    val firTypeScope = containingClassUnsubstitutedScope() ?: return
    firTypeScope.processFunctionsByName(callableId.callableName) { }
    firTypeScope.processOverriddenFunctions(this, action)
}

context(context: CheckerContext)
@OptIn(ScopeFunctionRequiresPrewarm::class)
fun FirPropertySymbol.processOverriddenPropertiesWithActionSafe(
    action: (FirPropertySymbol) -> ProcessorAction,
) {
    val firTypeScope = containingClassUnsubstitutedScope() ?: return
    firTypeScope.processPropertiesByName(name) { }
    firTypeScope.processOverriddenProperties(this, action)
}

context(context: CheckerContext)
private fun FirCallableSymbol<*>.containingClassUnsubstitutedScope(): FirTypeScope? {
    val containingClass = getContainingClassSymbol() as? FirClassSymbol ?: return null
    return containingClass.unsubstitutedScope()
}

val CheckerContext.closestNonLocal: FirBasedSymbol<*>?
    get() {
        for (symbol in containingDeclarations) {
            if (symbol is FirCallableSymbol || symbol is FirAnonymousInitializerSymbol) {
                return symbol
            }
        }
        return containingDeclarations.lastOrNull()
    }

fun CheckerContext.closestNonLocalWith(declaration: FirDeclaration): FirBasedSymbol<*>? {
    for (symbol in containingDeclarations + declaration.symbol) {
        if (symbol is FirCallableSymbol || symbol is FirAnonymousInitializerSymbol) {
            return symbol
        }
    }
    return declaration.symbol
}

val CheckerContext.isTopLevel: Boolean get() = containingDeclarations.lastOrNull().let { it is FirFileSymbol || it is FirScriptSymbol }

/**
 * The containing symbol is resolved using the declaration-site session.
 */
fun FirBasedSymbol<*>.hasAnnotationOrInsideAnnotatedClass(classId: ClassId, session: FirSession): Boolean {
    if (hasAnnotation(classId, session)) return true
    val container = getContainingClassSymbol() ?: return false
    return container.hasAnnotationOrInsideAnnotatedClass(classId, session)
}

fun FirDeclaration.hasAnnotationOrInsideAnnotatedClass(classId: ClassId, session: FirSession): Boolean =
    symbol.hasAnnotationOrInsideAnnotatedClass(classId, session)

fun FirBasedSymbol<*>.getAnnotationFirstArgument(classId: ClassId, session: FirSession): FirExpression? {
    val annotation = getAnnotationWithResolvedArgumentsByClassId(classId, session)
    return annotation?.argumentMapping?.mapping?.values?.firstOrNull()
}

fun FirBasedSymbol<*>.getAnnotationStringParameter(classId: ClassId, session: FirSession): String? {
    val expression = getAnnotationFirstArgument(classId, session) as? FirLiteralExpression
    return expression?.value as? String
}

fun FirBasedSymbol<*>.getAnnotationBooleanParameter(classId: ClassId, session: FirSession): Boolean? {
    val expression = getAnnotationFirstArgument(classId, session) as? FirLiteralExpression
    return expression?.value as? Boolean
}

context(context: CheckerContext)
fun FirElement.isLhsOfAssignment(): Boolean {
    if (this !is FirQualifiedAccessExpression) return false
    val lastQualified = context.callsOrAssignments.lastOrNull { it != this } ?: return false
    return lastQualified is FirVariableAssignment && lastQualified.lValue == this
}

fun ConeKotlinType.leastUpperBound(session: FirSession): ConeKotlinType {
    val upperBounds = collectUpperBounds(session.typeContext).takeIf { it.isNotEmpty() }
        ?: return session.builtinTypes.nullableAnyType.coneType
    return ConeTypeIntersector.intersectTypes(session.typeContext, upperBounds)
}

fun ConeKotlinType.fullyExpandedClassId(session: FirSession): ClassId? {
    return fullyExpandedType(session).classId
}

@OptIn(ExperimentalContracts::class)
fun ConeKotlinType.hasDiagnosticKind(kind: DiagnosticKind): Boolean {
    contract { returns(true) implies (this@hasDiagnosticKind is ConeErrorType) }
    return this is ConeErrorType && (diagnostic as? ConeSimpleDiagnostic)?.kind == kind
}

context(context: CheckerContext)
fun ConeKotlinType.finalApproximationOrSelf(): ConeKotlinType {
    return context.session.typeApproximator.approximateToSuperType(
        this,
        TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
    ) ?: this
}

context(context: CheckerContext)
fun FirResolvedQualifier.isStandalone(
): Boolean {
    val lastQualifiedAccess = context.callsOrAssignments.lastOrNull() as? FirQualifiedAccessExpression
    // Note: qualifier isn't standalone when it's in receiver (SomeClass.foo) or getClass (SomeClass::class) position
    if (lastQualifiedAccess?.explicitReceiver === this || lastQualifiedAccess?.dispatchReceiver === this) return false
    val lastGetClass = context.getClassCalls.lastOrNull()
    if (lastGetClass?.argument === this) return false
    if (isExplicitParentOfResolvedQualifier()) return false

    return true
}

/**
 * @return `true` for qualifiers that are explicit parents (receivers) for other qualifiers, e.g., for `Outer` in `Outer.Nested`
 */
context(context: CheckerContext)
fun FirResolvedQualifier.isExplicitParentOfResolvedQualifier(): Boolean {
    return context.secondToLastContainer.let { it is FirResolvedQualifier && it.explicitParent == this }
}

fun isExplicitTypeArgumentSource(source: KtSourceElement?): Boolean =
    source != null && source.kind !is KtFakeSourceElementKind.ImplicitTypeArgument

val FirTypeProjection.isExplicit: Boolean get() = isExplicitTypeArgumentSource(source)

fun FirAnonymousFunctionSymbol.getReturnedExpressions(): List<FirExpression> {
    val exitNode = resolvedControlFlowGraphReference?.controlFlowGraph?.exitNode ?: return emptyList()

    fun extractReturnedExpression(it: CFGNode<*>): FirExpression? {
        return when (it) {
            is JumpNode -> (it.fir as? FirReturnExpression)?.result
            is BlockExitNode -> (it.fir.statements.lastOrNull() as? FirReturnExpression)?.result
            is FinallyBlockExitNode -> {
                val finallyBlockEnterNode = it.enterNode
                finallyBlockEnterNode.previousNodes.firstOrNull { x -> finallyBlockEnterNode.edgeFrom(x) == exitNode.edgeFrom(it) }
                    ?.let(::extractReturnedExpression)
            }
            else -> null
        }
    }

    return exitNode.previousNodes.mapNotNull(::extractReturnedExpression).distinct()
}

context(context: CheckerContext)
fun ConeKotlinType.isMalformedExpandedType(allowNullableNothing: Boolean): Boolean {
    val expandedType = fullyExpandedType()
    if (expandedType.classId == StandardClassIds.Array) {
        val singleArgumentType = expandedType.typeArguments.singleOrNull()?.type?.fullyExpandedType()
        if (singleArgumentType != null &&
            (singleArgumentType.isNothing || (singleArgumentType.isNullableNothing && !allowNullableNothing))
        ) {
            return true
        }
    }
    return expandedType.containsMalformedArgument(allowNullableNothing)
}

context(context: CheckerContext)
private fun ConeKotlinType.containsMalformedArgument(allowNullableNothing: Boolean) =
    typeArguments.any {
        it.type?.fullyExpandedType()?.isMalformedExpandedType(allowNullableNothing) == true
    }

context(context: CheckerContext, reporter: DiagnosticReporter)
fun KtSourceElement?.requireFeatureSupport(
    feature: LanguageFeature,
    positioningStrategy: SourceElementPositioningStrategy? = null,
) {
    if (!feature.isEnabled()) {
        reporter.reportOn(this, FirErrors.UNSUPPORTED_FEATURE, feature to context.languageVersionSettings, positioningStrategy)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun FirElement.requireFeatureSupport(
    feature: LanguageFeature,
    positioningStrategy: SourceElementPositioningStrategy? = null,
) {
    source.requireFeatureSupport(feature, positioningStrategy)
}

context(context: CheckerContext)
internal val ConeKotlinType.hasStableIdentityForAtomicOperations: Boolean
    get() = fullyExpandedType().unwrapToSimpleTypeUsingLowerBound().let {
        !it.isPrimitiveOrNullablePrimitive && !it.isValueClass(context.session)
    }

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkAtomicCallReceiverForStableIdentity(
    type: ConeKotlinType,
    source: KtSourceElement?,
    atomicReferenceClassId: ClassId,
    appropriateCandidatesForArgument: Map<ClassId, ClassId>,
) {
    val expanded = type.fullyExpandedType()
    val argument = expanded.typeArguments.firstOrNull()?.type ?: return

    if (!argument.hasStableIdentityForAtomicOperations) {
        val candidate = appropriateCandidatesForArgument[argument.classId]
        reporter.reportOn(source, FirErrors.ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY, atomicReferenceClassId, argument, candidate)
    }
}

@OptIn(ExperimentalContracts::class)
fun FirBasedSymbol<*>?.isPrimaryConstructor(): Boolean {
    contract {
        returns(true) implies (this@isPrimaryConstructor is FirConstructorSymbol)
    }
    if (this !is FirConstructorSymbol) return false
    return isPrimary
}

fun FirBasedSymbol<*>?.isExpect(): Boolean {
    return when (this) {
        is FirCallableSymbol<*> -> isExpect
        is FirClassLikeSymbol -> isExpect
        else -> false
    }
}

context(context: SessionHolder)
fun FirResolvedQualifier.resolvedSymbolOrCompanionSymbol(): FirClassLikeSymbol<*>? {
    return symbol?.applyIf(resolvedToCompanionObject) {
        fullyExpandedClass()?.resolvedCompanionObjectSymbol
    }
}

context(context: SessionHolder)
fun FirResolvedQualifier.resolvedCompanionSymbol(): FirClassLikeSymbol<*>? {
    return symbol.takeIf { resolvedToCompanionObject }?.fullyExpandedClass()?.resolvedCompanionObjectSymbol
}

context(context: CheckerContext)
fun FirExpression.isDispatchReceiver(): Boolean {
    val parentElement = context.containingElements.elementAtOrNull(context.containingElements.size - 2)
    return parentElement is FirQualifiedAccessExpression && parentElement.dispatchReceiver == this
}
