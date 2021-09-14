/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.multipleDelegatesWithTheSameSignature
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.AnnotationTargetList
import org.jetbrains.kotlin.resolve.AnnotationTargetLists
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val INLINE_ONLY_ANNOTATION_CLASS_ID: ClassId = ClassId.topLevel(FqName("kotlin.internal.InlineOnly"))

fun FirClass.unsubstitutedScope(context: CheckerContext): FirTypeScope =
    this.unsubstitutedScope(context.sessionHolder.session, context.sessionHolder.scopeSession, withForcedTypeCalculator = false)

fun FirClassSymbol<*>.unsubstitutedScope(context: CheckerContext): FirTypeScope =
    this.unsubstitutedScope(context.sessionHolder.session, context.sessionHolder.scopeSession, withForcedTypeCalculator = false)

fun FirTypeRef.toClassLikeSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return coneTypeSafe<ConeClassLikeType>()?.toSymbol(session) as? FirClassLikeSymbol<*>
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
            val candidate = it.toClassLikeSymbol(session)
                ?.fullyExpandedClass(session) as? FirClassSymbol<*>
                ?: continue

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

/**
 * Returns the FirRegularClassSymbol associated with this
 * or null of something goes wrong.
 */
fun ConeClassLikeType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol
}

fun ConeKotlinType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return (this as? ConeClassLikeType)?.toRegularClassSymbol(session)
}

fun ConeKotlinType.isInlineClass(session: FirSession): Boolean = toRegularClassSymbol(session)?.isInline == true

/**
 * Returns the FirRegularClass associated with this
 * or null of something goes wrong.
 */
fun FirTypeRef.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return coneType.toRegularClassSymbol(session)
}

/**
 * Returns the ClassLikeDeclaration where the Fir object has been defined
 * or null if no proper declaration has been found.
 */
fun FirDeclaration.getContainingClassSymbol(session: FirSession): FirClassLikeSymbol<*>? =
    this.safeAs<FirCallableDeclaration>()?.containingClass()?.toSymbol(session)

@OptIn(SymbolInternals::class)
fun FirBasedSymbol<*>.getContainingClassSymbol(session: FirSession): FirClassLikeSymbol<*>? = fir.getContainingClassSymbol(session)

fun FirClassLikeSymbol<*>.outerClassSymbol(context: CheckerContext): FirClassLikeSymbol<*>? {
    if (this !is FirClassSymbol<*>) return null
    val outerClassId = classId.outerClassId ?: return null
    return context.session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
}

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.getContainingDeclarationSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    if (isLocal) {
        return (this as FirRegularClassSymbol).fir.containingClassForLocalAttr?.toFirRegularClassSymbol(session)
    } else {
        val parentId = classId.relativeClassName.parent()
        if (!parentId.isRoot) {
            val containingDeclarationId = ClassId(classId.packageFqName, parentId, false)
            return session.symbolProvider.getClassLikeSymbolByClassId(containingDeclarationId)
        }
    }

    return null
}

/**
 * Returns the FirClassLikeDeclaration that the
 * sequence of FirTypeAlias'es points to starting
 * with `this`. Or null if something goes wrong.
 */
fun FirClassLikeSymbol<*>.fullyExpandedClass(useSiteSession: FirSession): FirRegularClassSymbol? {
    return when (this) {
        is FirRegularClassSymbol -> this
        is FirTypeAliasSymbol -> (resolvedExpandedTypeRef.coneTypeSafe<ConeClassLikeType>()
            ?.toSymbol(useSiteSession) as? FirClassLikeSymbol<*>)?.fullyExpandedClass(useSiteSession)
        else -> null
    }
}

/**
 * Returns the closest to the end of context.containingDeclarations
 * item like FirRegularClass or FirAnonymousObject
 * or null if no such item could be found.
 */
fun CheckerContext.findClosestClassOrObject(): FirClass? {
    for (it in containingDeclarations.asReversed()) {
        if (
            it is FirRegularClass ||
            it is FirAnonymousObject
        ) {
            return it as FirClass
        }
    }

    return null
}

/**
 * Returns the list of functions that overridden by given
 */
fun FirSimpleFunction.overriddenFunctions(
    containingClass: FirClassSymbol<*>,
    context: CheckerContext
): List<FirFunctionSymbol<*>> {
    return symbol.overriddenFunctions(containingClass, context)
}

fun FirNamedFunctionSymbol.overriddenFunctions(
    containingClass: FirClassSymbol<*>,
    context: CheckerContext
): List<FirFunctionSymbol<*>> {
    val firTypeScope = containingClass.unsubstitutedScope(
        context.sessionHolder.session,
        context.sessionHolder.scopeSession,
        withForcedTypeCalculator = true
    )

    val overriddenFunctions = mutableListOf<FirFunctionSymbol<*>>()
    firTypeScope.processFunctionsByName(callableId.callableName) { }
    firTypeScope.processOverriddenFunctions(this) {
        overriddenFunctions.add(it)
        ProcessorAction.NEXT
    }

    return overriddenFunctions
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

/**
 * returns implicit modality by FirMemberDeclaration<*>
 */
fun FirMemberDeclaration.implicitModality(context: CheckerContext): Modality {
    if (this is FirRegularClass && (this.classKind == ClassKind.CLASS || this.classKind == ClassKind.OBJECT)) {
        if (this.classKind == ClassKind.INTERFACE) return Modality.ABSTRACT
        return Modality.FINAL
    }

    val klass = context.findClosestClassOrObject() ?: return Modality.FINAL
    val source = source ?: return Modality.FINAL
    val tree = source.treeStructure
    if (tree.overrideModifier(source.lighterASTNode) != null) {
        val klassModalityTokenType = klass.source?.let { tree.modalityModifier(it.lighterASTNode)?.tokenType }
        if (klassModalityTokenType == KtTokens.ABSTRACT_KEYWORD ||
            klassModalityTokenType == KtTokens.OPEN_KEYWORD ||
            klassModalityTokenType == KtTokens.SEALED_KEYWORD
        ) {
            return Modality.OPEN
        }
    }

    if (klass is FirRegularClass
        && klass.classKind == ClassKind.INTERFACE
        && tree.visibilityModifier(source.lighterASTNode)?.tokenType != KtTokens.PRIVATE_KEYWORD
    ) {
        require(this is FirDeclaration)
        return if (this.hasBody()) Modality.OPEN else Modality.ABSTRACT
    }

    return Modality.FINAL
}

private fun FirDeclaration.hasBody(): Boolean = when (this) {
    is FirSimpleFunction -> this.body != null && this.body !is FirEmptyExpressionBlock
    is FirProperty -> this.setter?.body !is FirEmptyExpressionBlock? || this.getter?.body !is FirEmptyExpressionBlock?
    else -> false
}

/**
 * Finds any non-interface supertype and returns it
 * or null if couldn't find any.
 */
fun FirClass.findNonInterfaceSupertype(context: CheckerContext): FirTypeRef? {
    for (superTypeRef in superTypeRefs) {
        val lookupTag = superTypeRef.coneType.safeAs<ConeClassLikeType>()?.lookupTag ?: continue

        val symbol = lookupTag.toSymbol(context.session) as? FirClassSymbol<*> ?: continue

        if (symbol.classKind != ClassKind.INTERFACE) {
            return superTypeRef
        }
    }

    return null
}

val FirFunctionCall.isIterator: Boolean
    get() = this.calleeReference.name.asString() == "<iterator>"

fun ConeKotlinType.isSubtypeOfThrowable(session: FirSession): Boolean =
    session.builtinTypes.throwableType.type.isSupertypeOf(session.typeContext, this.fullyExpandedType(session))

val FirValueParameter.hasValOrVar: Boolean
    get() {
        val source = this.source ?: return false
        return source.getChild(VAL_VAR_TOKEN_SET) != null
    }

fun KotlinTypeMarker.isSupertypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
    type != null && AbstractTypeChecker.isSubtypeOf(context, type, this)

fun KotlinTypeMarker.isSubtypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
    type != null && AbstractTypeChecker.isSubtypeOf(context, this, type)

fun ConeKotlinType.canHaveSubtypes(session: FirSession): Boolean {
    if (this.isMarkedNullable) {
        return true
    }
    val classSymbol = toRegularClassSymbol(session) ?: return true
    if (classSymbol.isEnumClass || classSymbol.isExpect || classSymbol.modality != Modality.FINAL) {
        return true
    }

    classSymbol.typeParameterSymbols.forEachIndexed { idx, typeParameterSymbol ->
        val typeProjection = typeArguments[idx]

        if (typeProjection.isStarProjection) {
            return true
        }

        val argument = typeProjection.type!! //safe because it is not a star

        when (typeParameterSymbol.variance) {
            Variance.INVARIANT ->
                when (typeProjection.kind) {
                    ProjectionKind.INVARIANT ->
                        if (lowerThanBound(session.typeContext, argument, typeParameterSymbol) || argument.canHaveSubtypes(session)) {
                            return true
                        }
                    ProjectionKind.IN ->
                        if (lowerThanBound(session.typeContext, argument, typeParameterSymbol)) {
                            return true
                        }
                    ProjectionKind.OUT ->
                        if (argument.canHaveSubtypes(session)) {
                            return true
                        }
                    ProjectionKind.STAR ->
                        return true
                }
            Variance.IN_VARIANCE ->
                if (typeProjection.kind != ProjectionKind.OUT) {
                    if (lowerThanBound(session.typeContext, argument, typeParameterSymbol)) {
                        return true
                    }
                } else {
                    if (argument.canHaveSubtypes(session)) {
                        return true
                    }
                }
            Variance.OUT_VARIANCE ->
                if (typeProjection.kind != ProjectionKind.IN) {
                    if (argument.canHaveSubtypes(session)) {
                        return true
                    }
                } else {
                    if (lowerThanBound(session.typeContext, argument, typeParameterSymbol)) {
                        return true
                    }
                }
        }
    }

    return false
}

private fun lowerThanBound(context: ConeInferenceContext, argument: ConeKotlinType, typeParameterSymbol: FirTypeParameterSymbol): Boolean {
    typeParameterSymbol.resolvedBounds.forEach { boundTypeRef ->
        if (argument != boundTypeRef.coneType && argument.isSubtypeOf(context, boundTypeRef.coneType)) {
            return true
        }
    }
    return false
}

fun FirMemberDeclaration.isInlineOnly(): Boolean =
    isInline && (this as FirAnnotatedDeclaration).hasAnnotation(INLINE_ONLY_ANNOTATION_CLASS_ID)

fun isSubtypeForTypeMismatch(context: ConeInferenceContext, subtype: ConeKotlinType, supertype: ConeKotlinType): Boolean {
    val subtypeFullyExpanded = subtype.fullyExpandedType(context.session)
    val supertypeFullyExpanded = supertype.fullyExpandedType(context.session)
    return AbstractTypeChecker.isSubtypeOf(context, subtypeFullyExpanded, supertypeFullyExpanded)
            || isSubtypeOfForFunctionalTypeReturningUnit(context.session.typeContext, subtypeFullyExpanded, supertypeFullyExpanded)
}

private fun isSubtypeOfForFunctionalTypeReturningUnit(
    context: ConeInferenceContext,
    subtype: ConeKotlinType,
    supertype: ConeKotlinType
): Boolean {
    if (!supertype.isBuiltinFunctionalType(context.session)) return false
    val functionalTypeReturnType = supertype.typeArguments.lastOrNull()
    if ((functionalTypeReturnType as? ConeClassLikeType)?.isUnit == true) {
        // We don't try to match return type for this case
        // Dropping the return type (getting only the lambda args)
        val superTypeArgs = supertype.typeArguments.dropLast(1)
        val subTypeArgs = subtype.typeArguments.dropLast(1)
        if (superTypeArgs.size != subTypeArgs.size) return false

        for (i in superTypeArgs.indices) {
            val subTypeArg = subTypeArgs[i].type ?: return false
            val superTypeArg = superTypeArgs[i].type ?: return false

            if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, subTypeArg, superTypeArg)) {
                return false
            }
        }

        return true
    }
    return false
}

fun FirCallableDeclaration.isVisibleInClass(parentClass: FirClass): Boolean {
    return symbol.isVisibleInClass(parentClass.symbol)
}

fun FirCallableSymbol<*>.isVisibleInClass(parentClassSymbol: FirClassSymbol<*>): Boolean {
    val classPackage = parentClassSymbol.classId.packageFqName
    if (visibility == Visibilities.Private ||
        !visibility.visibleFromPackage(classPackage, callableId.packageName)
    ) return false
    if (
        visibility == Visibilities.Internal &&
        (moduleData != parentClassSymbol.moduleData || parentClassSymbol.moduleData in moduleData.friendDependencies)
    ) return false
    return true
}

/**
 * Get the [ImplementationStatus] for this member.
 *
 * @param parentClassSymbol the contextual class for this query.
 */
fun FirCallableSymbol<*>.getImplementationStatus(
    sessionHolder: SessionHolder,
    parentClassSymbol: FirClassSymbol<*>
): ImplementationStatus {
    val containingClassSymbol = getContainingClassSymbol(sessionHolder.session)
    val symbol = this

    if (this.multipleDelegatesWithTheSameSignature == true && containingClassSymbol == parentClassSymbol) {
        return ImplementationStatus.AMBIGUOUSLY_INHERITED
    }

    if (symbol is FirIntersectionCallableSymbol) {
        if (containingClassSymbol === parentClassSymbol && symbol.subjectToManyNotImplemented(sessionHolder)) {
            return ImplementationStatus.AMBIGUOUSLY_INHERITED
        }
        // In Java 8, non-abstract intersection overrides having abstract symbol from base class
        // still should be implemented in current class (even when they have default interface implementation)
        if (symbol.intersections.any {
                @OptIn(SymbolInternals::class)
                val fir = it.fir.unwrapFakeOverrides()
                fir.isAbstract && (fir.getContainingClassSymbol(sessionHolder.session) as? FirRegularClassSymbol)?.classKind == ClassKind.CLASS
            }
        ) {
            // Exception from the rule above: interface implementation via delegation
            if (symbol.intersections.none {
                    @OptIn(SymbolInternals::class)
                    val fir = it.fir
                    fir.origin == FirDeclarationOrigin.Delegated && !fir.isAbstract
                }
            ) {
                return ImplementationStatus.NOT_IMPLEMENTED
            }
        }
    }
    if (this is FirNamedFunctionSymbol) {
        if (parentClassSymbol is FirRegularClassSymbol && parentClassSymbol.isData && matchesDataClassSyntheticMemberSignatures) {
            return ImplementationStatus.INHERITED_OR_SYNTHESIZED
        }
        // TODO: suspend function overridden by a Java class in the middle is not properly regarded as an override
        if (isSuspend) {
            return ImplementationStatus.INHERITED_OR_SYNTHESIZED
        }
    }
    return when {
        isFinal -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        containingClassSymbol === parentClassSymbol && origin == FirDeclarationOrigin.Source -> ImplementationStatus.ALREADY_IMPLEMENTED
        containingClassSymbol is FirRegularClassSymbol && containingClassSymbol.isExpect -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        isAbstract -> ImplementationStatus.NOT_IMPLEMENTED
        else -> ImplementationStatus.INHERITED_OR_SYNTHESIZED
    }
}


private fun FirIntersectionCallableSymbol.subjectToManyNotImplemented(sessionHolder: SessionHolder): Boolean {
    var nonAbstractCountInClass = 0
    var nonAbstractCountInInterface = 0
    var abstractCountInInterface = 0
    for (intersectionSymbol in intersections) {
        val containingClassSymbol = intersectionSymbol.getContainingClassSymbol(sessionHolder.session) as? FirRegularClassSymbol
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
        return (name == OperatorNameConventions.EQUALS && matchesEqualsSignature) ||
                (name == HASHCODE_NAME && matchesHashCodeSignature) ||
                (name == OperatorNameConventions.TO_STRING && matchesToStringSignature)
    }

// NB: we intentionally do not check return types
private val FirNamedFunctionSymbol.matchesEqualsSignature: Boolean
    get() {
        val valueParameters = valueParameterSymbols
        return valueParameters.size == 1 && valueParameters[0].resolvedReturnTypeRef.coneType.isNullableAny
    }

private val FirNamedFunctionSymbol.matchesHashCodeSignature: Boolean
    get() = valueParameterSymbols.isEmpty()

private val FirNamedFunctionSymbol.matchesToStringSignature: Boolean
    get() = valueParameterSymbols.isEmpty()

val Name.isDelegated: Boolean get() = asString().startsWith("<\$\$delegate_")

val ConeTypeProjection.isConflictingOrNotInvariant: Boolean get() = kind != ProjectionKind.INVARIANT || this is ConeKotlinTypeConflictingProjection

fun checkTypeMismatch(
    lValueOriginalType: ConeKotlinType,
    assignment: FirVariableAssignment?,
    rValue: FirExpression,
    context: CheckerContext,
    source: FirSourceElement,
    reporter: DiagnosticReporter,
    isInitializer: Boolean
) {
    var lValueType = lValueOriginalType
    var rValueType = rValue.typeRef.coneType
    if (source.kind is FirFakeSourceElementKind.DesugaredIncrementOrDecrement) {
        if (!lValueType.isNullable && rValueType.isNullable) {
            val tempType = rValueType
            rValueType = lValueType
            lValueType = tempType
        }
    }

    val typeContext = context.session.typeContext

    if (!isSubtypeForTypeMismatch(typeContext, subtype = rValueType, supertype = lValueType)) {
        if (rValueType is ConeClassLikeType &&
            rValueType.lookupTag.classId == StandardClassIds.Int &&
            lValueType.fullyExpandedType(context.session).isIntegerTypeOrNullableIntegerTypeOfAnySize &&
            rValueType.nullability == ConeNullability.NOT_NULL
        ) {
            // val p: Byte = 42 or similar situation
            // TODO: remove after fix of KT-46047
            return
        }
        if (lValueType.isExtensionFunctionType || rValueType.isExtensionFunctionType) {
            // TODO: remove after fix of KT-45989
            return
        }
        val resolvedSymbol = assignment?.calleeReference?.toResolvedCallableSymbol() as? FirPropertySymbol
        when {
            resolvedSymbol != null && lValueType is ConeCapturedType && lValueType.constructor.projection.kind.let {
                it == ProjectionKind.STAR || it == ProjectionKind.OUT
            } -> {
                reporter.reportOn(assignment.source, FirErrors.SETTER_PROJECTED_OUT, resolvedSymbol, context)
            }
            rValue.isNullLiteral && lValueType.nullability == ConeNullability.NOT_NULL -> {
                reporter.reportOn(rValue.source, FirErrors.NULL_FOR_NONNULL_TYPE, context)
            }
            isInitializer -> {
                reporter.reportOn(source, FirErrors.INITIALIZER_TYPE_MISMATCH, lValueType, rValueType, context)
            }
            source.kind is FirFakeSourceElementKind.DesugaredIncrementOrDecrement -> {
                if (!lValueType.isNullable && rValueType.isNullable) {
                    val tempType = rValueType
                    rValueType = lValueType
                    lValueType = tempType
                }
                if (rValueType.isUnit) {
                    reporter.reportOn(source, FirErrors.INC_DEC_SHOULD_NOT_RETURN_UNIT, context)
                } else {
                    reporter.reportOn(source, FirErrors.RESULT_TYPE_MISMATCH, lValueType, rValueType, context)
                }
            }
            else -> {
                reporter.reportOn(source, FirErrors.ASSIGNMENT_TYPE_MISMATCH, lValueType, rValueType, context)
            }
        }
    }
}

internal fun checkCondition(condition: FirExpression, context: CheckerContext, reporter: DiagnosticReporter) {
    val coneType = condition.typeRef.coneTypeSafe<ConeKotlinType>()?.lowerBoundIfFlexible()
    if (coneType != null &&
        coneType !is ConeKotlinErrorType &&
        !coneType.isSubtypeOf(context.session.typeContext, context.session.builtinTypes.booleanType.type)
    ) {
        reporter.reportOn(condition.source, FirErrors.CONDITION_TYPE_MISMATCH, coneType, context)
    }
}

fun extractArgumentTypeRefAndSource(typeRef: FirTypeRef?, index: Int): FirTypeRefSource? {
    if (typeRef is FirResolvedTypeRef) {
        val delegatedTypeRef = typeRef.delegatedTypeRef
        if (delegatedTypeRef is FirUserTypeRef) {
            var currentIndex = index
            val qualifier = delegatedTypeRef.qualifier

            for (i in qualifier.size - 1 downTo 0) {
                val typeArguments = qualifier[i].typeArgumentList.typeArguments
                if (currentIndex < typeArguments.size) {
                    val typeArgument = typeArguments.elementAtOrNull(currentIndex)
                    return if (typeArgument is FirTypeProjection)
                        FirTypeRefSource((typeArgument as? FirTypeProjectionWithVariance)?.typeRef, typeArgument.source)
                    else null
                } else {
                    currentIndex -= typeArguments.size
                }
            }
        } else if (delegatedTypeRef is FirFunctionTypeRef) {
            val valueParameters = delegatedTypeRef.valueParameters
            if (index < valueParameters.size) {
                val valueParamTypeRef = valueParameters.elementAt(index).returnTypeRef
                return FirTypeRefSource(valueParamTypeRef, valueParamTypeRef.source)
            }
            if (index == valueParameters.size) {
                val returnTypeRef = delegatedTypeRef.returnTypeRef
                return FirTypeRefSource(returnTypeRef, returnTypeRef.source)
            }
        }
    }

    return null
}

data class FirTypeRefSource(val typeRef: FirTypeRef?, val source: FirSourceElement?)

fun FirRegularClassSymbol.collectEnumEntries(): Collection<FirEnumEntrySymbol> {
    assert(classKind == ClassKind.ENUM_CLASS)
    return declarationSymbols.filterIsInstance<FirEnumEntrySymbol>()
}

val FirClassLikeSymbol<*>.classKind: ClassKind?
    get() = (this as? FirClassSymbol<*>)?.classKind

val FirBasedSymbol<*>.typeParameterSymbols: List<FirTypeParameterSymbol>?
    get() = when (this) {
        is FirCallableSymbol<*> -> typeParameterSymbols
        is FirClassSymbol<*> -> typeParameterSymbols
        is FirTypeAliasSymbol -> typeParameterSymbols
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

    val returnType = resolvedReturnTypeRef.coneType

    if ((returnType.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag != typeParameterSymbol.toLookupTag()) return false

    fun FirTypeRef.isBadType() =
        coneTypeSafe<ConeKotlinType>()
            ?.contains { (it.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag == typeParameterSymbol.toLookupTag() } != false

    if (valueParameterSymbols.any { it.resolvedReturnTypeRef.isBadType() } || resolvedReceiverTypeRef?.isBadType() == true) return false

    return true
}

fun getActualTargetList(annotated: FirDeclaration): AnnotationTargetList {
    fun CallableId.isMember(): Boolean {
        return classId != null || isLocal // TODO: Replace with .containingClass (after fixing)
    }

    return when (annotated) {
        is FirRegularClass -> {
            AnnotationTargetList(
                KotlinTarget.classActualTargets(annotated.classKind, annotated.isInner, annotated.isCompanion, annotated.isLocal)
            )
        }
        is FirEnumEntry -> AnnotationTargetList(
            KotlinTarget.classActualTargets(ClassKind.ENUM_ENTRY, annotated.isInner, isCompanionObject = false, isLocalClass = false)
        )
        is FirProperty -> {
            when {
                annotated.isLocal ->
                    if (annotated.source?.kind == FirFakeSourceElementKind.DesugaredComponentFunctionCall) {
                        TargetLists.T_DESTRUCTURING_DECLARATION
                    } else {
                        TargetLists.T_LOCAL_VARIABLE
                    }
                annotated.symbol.callableId.isMember() ->
                    if (annotated.source?.kind == FirFakeSourceElementKind.PropertyFromParameter) {
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
        is FirSimpleFunction -> {
            when {
                annotated.isLocal -> TargetLists.T_LOCAL_FUNCTION
                annotated.symbol.callableId.isMember() -> TargetLists.T_MEMBER_FUNCTION
                else -> TargetLists.T_TOP_LEVEL_FUNCTION
            }
        }
        is FirTypeAlias -> TargetLists.T_TYPEALIAS
        is FirPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
        is FirBackingField -> TargetLists.T_BACKING_FIELD
        is FirFile -> TargetLists.T_FILE
        is FirTypeParameter -> TargetLists.T_TYPE_PARAMETER
        is FirAnonymousInitializer -> TargetLists.T_INITIALIZER
        is FirAnonymousObject ->
            if (annotated.source?.kind == FirFakeSourceElementKind.EnumInitializer) {
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
//            TODO: properly implement those cases
//            is KtDestructuringDeclarationEntry -> TargetLists.T_LOCAL_VARIABLE
//            is KtDestructuringDeclaration -> TargetLists.T_DESTRUCTURING_DECLARATION
//            is KtLambdaExpression -> TargetLists.T_FUNCTION_LITERAL
        else -> TargetLists.EMPTY
    }
}

private typealias TargetLists = AnnotationTargetLists
