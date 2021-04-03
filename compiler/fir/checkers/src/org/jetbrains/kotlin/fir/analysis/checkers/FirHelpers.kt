/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.modalityModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.overrideModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.visibilityModifier
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val INLINE_ONLY_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("kotlin.internal.InlineOnly"))

internal fun FirClass<*>.unsubstitutedScope(context: CheckerContext) =
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

/**
 * Returns KtModifierToken by Modality
 */
fun Modality.toToken(): KtModifierKeywordToken = when (this) {
    Modality.FINAL -> KtTokens.FINAL_KEYWORD
    Modality.SEALED -> KtTokens.SEALED_KEYWORD
    Modality.OPEN -> KtTokens.OPEN_KEYWORD
    Modality.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
}

val FirFunctionCall.isIterator
    get() = this.calleeReference.name.asString() == "<iterator>"

internal fun throwableClassLikeType(session: FirSession) = session.builtinTypes.throwableType.type

fun ConeKotlinType.isSubtypeOfThrowable(session: FirSession) =
    throwableClassLikeType(session).isSupertypeOf(session.typeContext, this.fullyExpandedType(session))

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

val FirExpression.isComponentCall
    get() = this is FirComponentCall

fun isSubtypeForTypeMismatch(context: ConeInferenceContext, subtype: ConeKotlinType, supertype: ConeKotlinType): Boolean {
    return AbstractTypeChecker.isSubtypeOf(context, subtype, supertype)
            || isSubtypeOfForFunctionalTypeReturningUnit(context.session.typeContext, subtype, supertype)
}

fun isSubtypeOfForFunctionalTypeReturningUnit(context: ConeInferenceContext, subtype: ConeKotlinType, supertype: ConeKotlinType): Boolean {
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
