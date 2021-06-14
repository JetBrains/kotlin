/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.modalityModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.overrideModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.visibilityModifier
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val INLINE_ONLY_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("kotlin.internal.InlineOnly"))

fun FirClass<*>.unsubstitutedScope(context: CheckerContext) =
    this.unsubstitutedScope(context.sessionHolder.session, context.sessionHolder.scopeSession, withForcedTypeCalculator = false)

/**
 * Returns true if this is a supertype of other.
 */
fun FirClass<*>.isSupertypeOf(other: FirClass<*>, session: FirSession): Boolean {
    /**
     * Hides additional parameters.
     */
    fun FirClass<*>.isSupertypeOf(other: FirClass<*>, exclude: MutableSet<FirClass<*>>): Boolean {
        for (it in other.superTypeRefs) {
            val candidate = it.firClassLike(session)
                ?.followAllAlias(session)
                ?.safeAs<FirClass<*>>()
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
 * Returns the FirClass associated with this
 * or null of something goes wrong.
 */
fun ConeClassLikeType.toClass(session: FirSession): FirClass<*>? {
    return lookupTag.toSymbol(session).safeAs<FirClassSymbol<*>>()?.fir
}

/**
 * Returns the FirRegularClass associated with this
 * or null of something goes wrong.
 */
fun ConeClassLikeType.toRegularClass(session: FirSession): FirRegularClass? {
    return lookupTag.toSymbol(session).safeAs<FirRegularClassSymbol>()?.fir
}

/**
 * Returns the FirRegularClass associated with this
 * or null of something goes wrong.
 */
fun ConeKotlinType.toRegularClass(session: FirSession): FirRegularClass? {
    return safeAs<ConeClassLikeType>()?.fullyExpandedType(session)?.toRegularClass(session)
}

fun ConeKotlinType.isInline(session: FirSession): Boolean = toRegularClass(session)?.isInline == true

/**
 * Returns the FirRegularClass associated with this
 * or null of something goes wrong.
 */
fun FirTypeRef.toRegularClass(session: FirSession): FirRegularClass? {
    return coneType.toRegularClass(session)
}

/**
 * Returns FirSimpleFunction based on the given FirFunctionCall
 */
inline fun <reified T : Any> FirQualifiedAccessExpression.getDeclaration(): T? {
    return this.calleeReference.safeAs<FirResolvedNamedReference>()
        ?.resolvedSymbol
        ?.fir.safeAs()
}

/**
 * Returns the ClassLikeDeclaration where the Fir object has been defined
 * or null if no proper declaration has been found.
 */
fun FirSymbolOwner<*>.getContainingClass(context: CheckerContext): FirClassLikeDeclaration<*>? =
    this.safeAs<FirCallableMemberDeclaration<*>>()?.containingClass()?.toSymbol(context.session)?.fir

fun FirClassLikeSymbol<*>.outerClass(context: CheckerContext): FirClassLikeSymbol<*>? {
    if (this !is FirClassSymbol<*>) return null
    val outerClassId = classId.outerClassId ?: return null
    return context.session.symbolProvider.getClassLikeSymbolByFqName(outerClassId)
}

fun FirClass<*>.outerClass(context: CheckerContext): FirClass<*>? {
    return symbol.outerClass(context)?.fir as? FirClass<*>
}

/**
 * Returns the FirClassLikeDeclaration that the
 * sequence of FirTypeAlias'es points to starting
 * with `this`. Or null if something goes wrong.
 */
fun FirClassLikeDeclaration<*>.followAllAlias(session: FirSession): FirClassLikeDeclaration<*>? {
    var it: FirClassLikeDeclaration<*>? = this

    while (it is FirTypeAlias) {
        it = it.expandedTypeRef.firClassLike(session)
    }

    return it
}

/**
 * Returns the closest to the end of context.containingDeclarations
 * item like FirRegularClass or FirAnonymousObject
 * or null if no such item could be found.
 */
fun CheckerContext.findClosestClassOrObject(): FirClass<*>? {
    for (it in containingDeclarations.asReversed()) {
        if (
            it is FirRegularClass ||
            it is FirAnonymousObject
        ) {
            return it as FirClass<*>
        }
    }

    return null
}

/**
 * Returns the list of functions that overridden by given
 */
fun FirSimpleFunction.overriddenFunctions(
    containingClass: FirClass<*>,
    context: CheckerContext
): List<FirFunctionSymbol<*>> {
    val firTypeScope = containingClass.unsubstitutedScope(
        context.sessionHolder.session,
        context.sessionHolder.scopeSession,
        withForcedTypeCalculator = true
    )

    val overriddenFunctions = mutableListOf<FirFunctionSymbol<*>>()
    firTypeScope.processFunctionsByName(symbol.fir.name) { }
    firTypeScope.processOverriddenFunctions(symbol) {
        overriddenFunctions.add(it)
        ProcessorAction.NEXT
    }

    return overriddenFunctions
}

/**
 * Returns the visibility by given KtModifierList
 */
fun KtModifierList?.getVisibility() = this?.visibilityModifierType()?.toVisibilityOrNull()

/**
 * Returns Visibility by token or null
 */
fun KtModifierKeywordToken.toVisibilityOrNull(): Visibility? {
    return when (this) {
        KtTokens.PUBLIC_KEYWORD -> Visibilities.Public
        KtTokens.PRIVATE_KEYWORD -> Visibilities.Private
        KtTokens.PROTECTED_KEYWORD -> Visibilities.Protected
        KtTokens.INTERNAL_KEYWORD -> Visibilities.Internal
        else -> null
    }
}

/**
 * Returns the modality of the class
 */
fun FirClass<*>.modality(): Modality? {
    return when (this) {
        is FirRegularClass -> modality
        else -> Modality.FINAL
    }
}

/**
 * returns implicit modality by FirMemberDeclaration
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
fun FirClass<*>.findNonInterfaceSupertype(context: CheckerContext): FirTypeRef? {
    for (superTypeRef in superTypeRefs) {
        val lookupTag = superTypeRef.coneType.safeAs<ConeClassLikeType>()?.lookupTag ?: continue

        val fir = lookupTag.toSymbol(context.session)
            ?.fir.safeAs<FirClass<*>>()
            ?: continue

        if (fir.classKind != ClassKind.INTERFACE) {
            return superTypeRef
        }
    }

    return null
}

val FirFunctionCall.isIterator
    get() = this.calleeReference.name.asString() == "<iterator>"

fun ConeKotlinType.isSubtypeOfThrowable(session: FirSession) =
    session.builtinTypes.throwableType.type.isSupertypeOf(session.typeContext, this.fullyExpandedType(session))

val FirValueParameter.hasValOrVar: Boolean
    get() {
        val source = this.source ?: return false
        return source.getChild(VAL_VAR_TOKEN_SET) != null
    }

fun KotlinTypeMarker.isSupertypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?) =
    type != null && AbstractTypeChecker.isSubtypeOf(context, type, this)

fun KotlinTypeMarker.isSubtypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?) =
    type != null && AbstractTypeChecker.isSubtypeOf(context, this, type)

fun ConeKotlinType.canHaveSubtypes(session: FirSession): Boolean {
    if (this.isMarkedNullable) {
        return true
    }
    val clazz = toRegularClass(session) ?: return true
    if (clazz.isEnumClass || clazz.isExpect || clazz.modality != Modality.FINAL) {
        return true
    }

    clazz.typeParameters.forEachIndexed { idx, typeParameterRef ->
        val typeParameter = typeParameterRef.symbol.fir
        val typeProjection = typeArguments[idx]

        if (typeProjection.isStarProjection) {
            return true
        }

        val argument = typeProjection.type!! //safe because it is not a star

        when (typeParameter.variance) {
            Variance.INVARIANT ->
                when (typeProjection.kind) {
                    ProjectionKind.INVARIANT ->
                        if (lowerThanBound(session.typeContext, argument, typeParameter) || argument.canHaveSubtypes(session)) {
                            return true
                        }
                    ProjectionKind.IN ->
                        if (lowerThanBound(session.typeContext, argument, typeParameter)) {
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
                    if (lowerThanBound(session.typeContext, argument, typeParameter)) {
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
                    if (lowerThanBound(session.typeContext, argument, typeParameter)) {
                        return true
                    }
                }
        }
    }

    return false
}

private fun lowerThanBound(context: ConeInferenceContext, argument: ConeKotlinType, typeParameter: FirTypeParameter): Boolean {
    typeParameter.bounds.forEach { boundTypeRef ->
        if (argument != boundTypeRef.coneType && argument.isSubtypeOf(context, boundTypeRef.coneType)) {
            return true
        }
    }
    return false
}

fun FirMemberDeclaration.isInlineOnly(): Boolean = isInline && hasAnnotation(INLINE_ONLY_ANNOTATION_CLASS_ID)

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

fun FirCallableMemberDeclaration<*>.isVisibleInClass(parentClass: FirClass<*>): Boolean {
    val classPackage = parentClass.symbol.classId.packageFqName
    if (visibility == Visibilities.Private ||
        !visibility.visibleFromPackage(classPackage, symbol.callableId.packageName)
    ) return false
    if (
        visibility == Visibilities.Internal &&
        (moduleData != parentClass.moduleData || parentClass.moduleData in moduleData.friendDependencies)
    ) return false
    return true
}

/**
 * Get the [ImplementationStatus] for this member.
 *
 * @param parentClass the contextual class for this query.
 */
fun FirCallableMemberDeclaration<*>.getImplementationStatus(sessionHolder: SessionHolder, parentClass: FirClass<*>): ImplementationStatus {
    val containingClass = getContainingClass(sessionHolder)
    val symbol = this.symbol
    if (symbol is FirIntersectionCallableSymbol) {
        if (containingClass === parentClass && symbol.subjectToManyNotImplemented(sessionHolder)) {
            return ImplementationStatus.AMBIGUOUSLY_INHERITED
        }
        // In Java 8, non-abstract intersection overrides having abstract symbol from base class
        // still should be implemented in current class (even when they have default interface implementation)
        if (symbol.intersections.any {
                val fir = (it.fir as FirCallableMemberDeclaration).unwrapFakeOverrides()
                fir.isAbstract && (fir.getContainingClass(sessionHolder) as? FirRegularClass)?.classKind == ClassKind.CLASS
            }
        ) {
            // Exception from the rule above: interface implementation via delegation
            if (symbol.intersections.none {
                    val fir = (it.fir as FirCallableMemberDeclaration)
                    fir.origin == FirDeclarationOrigin.Delegated && !fir.isAbstract
                }
            ) {
                return ImplementationStatus.NOT_IMPLEMENTED
            }
        }
    }
    if (this is FirSimpleFunction) {
        if (parentClass is FirRegularClass && parentClass.isData && matchesDataClassSyntheticMemberSignatures) {
            return ImplementationStatus.INHERITED_OR_SYNTHESIZED
        }
        // TODO: suspend function overridden by a Java class in the middle is not properly regarded as an override
        if (isSuspend) {
            return ImplementationStatus.INHERITED_OR_SYNTHESIZED
        }
    }
    return when {
        isFinal -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        containingClass === parentClass && origin == FirDeclarationOrigin.Source -> ImplementationStatus.ALREADY_IMPLEMENTED
        containingClass is FirRegularClass && containingClass.isExpect -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        isAbstract -> ImplementationStatus.NOT_IMPLEMENTED
        else -> ImplementationStatus.INHERITED_OR_SYNTHESIZED
    }
}


private fun FirIntersectionCallableSymbol.subjectToManyNotImplemented(sessionHolder: SessionHolder): Boolean {
    var nonAbstractCountInClass = 0
    var nonAbstractCountInInterface = 0
    var abstractCountInInterface = 0
    for (intersectionSymbol in intersections) {
        val intersection = intersectionSymbol.fir as FirCallableMemberDeclaration
        val containingClass = intersection.getContainingClass(sessionHolder) as? FirRegularClass
        val hasInterfaceContainer = containingClass?.classKind == ClassKind.INTERFACE
        if (intersection.modality != Modality.ABSTRACT) {
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

private val FirSimpleFunction.matchesDataClassSyntheticMemberSignatures: Boolean
    get() = (this.name == OperatorNameConventions.EQUALS && matchesEqualsSignature) ||
            (this.name == HASHCODE_NAME && matchesHashCodeSignature) ||
            (this.name == OperatorNameConventions.TO_STRING && matchesToStringSignature)

private fun FirSymbolOwner<*>.getContainingClass(sessionHolder: SessionHolder): FirClassLikeDeclaration<*>? =
    this.safeAs<FirCallableMemberDeclaration<*>>()?.containingClass()?.toSymbol(sessionHolder.session)?.fir

// NB: we intentionally do not check return types
private val FirSimpleFunction.matchesEqualsSignature: Boolean
    get() = valueParameters.size == 1 && valueParameters[0].returnTypeRef.coneType.isNullableAny

private val FirSimpleFunction.matchesHashCodeSignature: Boolean
    get() = valueParameters.isEmpty()

private val FirSimpleFunction.matchesToStringSignature: Boolean
    get() = valueParameters.isEmpty()

fun checkTypeMismatch(
    lValueOriginalType: ConeKotlinType,
    rValue: FirExpression,
    context: CheckerContext,
    source: FirSourceElement,
    reporter: DiagnosticReporter,
    isInitializer: Boolean
) {
    var lValueType = lValueOriginalType
    var rValueType = rValue.typeRef.coneType
    val typeContext = context.session.typeContext

    val diagnosticFactory = when {
        isInitializer -> {
            FirErrors.INITIALIZER_TYPE_MISMATCH
        }
        source.kind is FirFakeSourceElementKind.DesugaredIncrementOrDecrement -> {
            if (!lValueType.isNullable && rValueType.isNullable) {
                val tempType = rValueType
                rValueType = lValueType
                lValueType = tempType
            }
            FirErrors.RESULT_TYPE_MISMATCH
        }
        else -> {
            FirErrors.ASSIGNMENT_TYPE_MISMATCH
        }
    }

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
        if (rValue.isNullLiteral && lValueType.nullability == ConeNullability.NOT_NULL) {
            reporter.reportOn(rValue.source, FirErrors.NULL_FOR_NONNULL_TYPE, context)
        } else {
            reporter.reportOn(source, diagnosticFactory, lValueType, rValueType, context)
        }
    }
}

internal fun checkCondition(condition: FirExpression, context: CheckerContext, reporter: DiagnosticReporter) {
    val coneType = condition.typeRef.coneType.lowerBoundIfFlexible()
    if (coneType !is ConeKotlinErrorType &&
        !coneType.isSubtypeOf(context.session.typeContext, context.session.builtinTypes.booleanType.type)
    ) {
        reporter.reportOn(condition.source, FirErrors.CONDITION_TYPE_MISMATCH, coneType, context)
    }
}

fun extractArgumentTypeRefAndSource(typeRef: FirTypeRef?, index: Int): FirTypeRefSource? {
    if (typeRef is FirResolvedTypeRef) {
        val delegatedTypeRef = typeRef.delegatedTypeRef
        if (delegatedTypeRef is FirUserTypeRef) {
            var currentTypeArguments: List<FirTypeProjection>? = null
            var currentIndex = index
            val qualifier = delegatedTypeRef.qualifier

            for (i in qualifier.size - 1 downTo 0) {
                val typeArguments = qualifier[i].typeArgumentList.typeArguments
                if (currentIndex < typeArguments.size) {
                    currentTypeArguments = typeArguments
                    break
                } else {
                    currentIndex -= typeArguments.size
                }
            }

            val typeArgument = currentTypeArguments?.elementAtOrNull(currentIndex)
            if (typeArgument is FirTypeProjectionWithVariance) {
                return FirTypeRefSource(typeArgument.typeRef, typeArgument.source)
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