/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType

fun FirCallableSymbol<*>.dispatchReceiverClassOrNull(): ConeClassLikeLookupTag? =
    (fir as? FirCallableMemberDeclaration<*>)?.dispatchReceiverClassOrNull()

fun FirCallableDeclaration<*>.dispatchReceiverClassOrNull(): ConeClassLikeLookupTag? {
    if (this !is FirCallableMemberDeclaration<*>) return null
    if (dispatchReceiverType is ConeIntersectionType && isIntersectionOverride) return symbol.baseForIntersectionOverride!!.fir.dispatchReceiverClassOrNull()

    return (dispatchReceiverType as? ConeClassLikeType)?.lookupTag
}

fun FirCallableSymbol<*>.containingClass(): ConeClassLikeLookupTag? = fir.containingClass()
fun FirCallableDeclaration<*>.containingClass(): ConeClassLikeLookupTag? {
    return (containingClassAttr ?: dispatchReceiverClassOrNull())
}

private object ContainingClassKey : FirDeclarationDataKey()
var FirCallableDeclaration<*>.containingClassAttr: ConeClassLikeLookupTag? by FirDeclarationDataRegistry.data(ContainingClassKey)

val FirCallableDeclaration<*>.isIntersectionOverride get() = origin == FirDeclarationOrigin.IntersectionOverride
val FirCallableDeclaration<*>.isSubstitutionOverride get() = origin == FirDeclarationOrigin.SubstitutionOverride
val FirCallableDeclaration<*>.isSubstitutionOrIntersectionOverride get() = isSubstitutionOverride || isIntersectionOverride

inline val <reified D : FirCallableDeclaration<*>> D.originalForSubstitutionOverride: D?
    get() = if (isSubstitutionOverride) originalForSubstitutionOverrideAttr else null

inline val <reified S : FirCallableSymbol<*>> S.originalForSubstitutionOverride: S?
    get() = fir.originalForSubstitutionOverride?.symbol as S?

inline val <reified D : FirCallableDeclaration<*>> D.baseForIntersectionOverride: D?
    get() = if (isIntersectionOverride) originalForIntersectionOverrideAttr else null

inline val <reified S : FirCallableSymbol<*>> S.baseForIntersectionOverride: S?
    get() = fir.baseForIntersectionOverride?.symbol as S?

val FirSimpleFunction.isJavaDefault: Boolean
    get() {
        if (isIntersectionOverride) return baseForIntersectionOverride!!.isJavaDefault
        return origin == FirDeclarationOrigin.Enhancement && modality == Modality.OPEN
    }

fun FirCallableSymbol<*>.shouldHaveComputedBaseSymbolsForClass(classLookupTag: ConeClassLikeLookupTag): Boolean =
    fir.origin.fromSupertypes && dispatchReceiverClassOrNull() == classLookupTag

inline fun <reified S : FirCallableSymbol<*>> computeBaseSymbols(
    symbol: S,
    directOverridden: FirTypeScope.(S) -> List<S>,
    scope: FirTypeScope,
    containingClass: ConeClassLikeLookupTag,
): List<S> {
    if (symbol.fir.origin == FirDeclarationOrigin.SubstitutionOverride) {
        return listOf(symbol.originalForSubstitutionOverride!!)
    }

    return scope.directOverridden(symbol).map {
        // Unwrapping should happen only for fake overrides members from the same class, not from supertypes
        if (it.dispatchReceiverClassOrNull() != containingClass) return@map it
        when {
            it.fir.isSubstitutionOverride ->
                it.originalForSubstitutionOverride!!
            it.fir.origin == FirDeclarationOrigin.Delegated ->
                it.fir.delegatedWrapperData?.wrapped?.symbol!! as S
            else -> it
        }
    }
}

inline fun <reified D : FirCallableDeclaration<*>> D.originalIfFakeOverride(): D? =
    originalForSubstitutionOverride ?: baseForIntersectionOverride

inline fun <reified S : FirCallableSymbol<*>> S.originalIfFakeOverride(): S? =
    fir.originalIfFakeOverride()?.symbol as S?

inline fun <reified D : FirCallableDeclaration<*>> D.unwrapFakeOverrides(): D {
    var current = this

    do {
        val next = current.originalIfFakeOverride() ?: return current
        current = next
    } while (true)
}

inline fun <reified S : FirCallableSymbol<*>> S.unwrapFakeOverrides(): S = fir.unwrapFakeOverrides().symbol as S

private object SubstitutedOverrideOriginalKey : FirDeclarationDataKey()
var <D : FirCallableDeclaration<*>>
        D.originalForSubstitutionOverrideAttr: D? by FirDeclarationDataRegistry.data(SubstitutedOverrideOriginalKey)

private object IntersectionOverrideOriginalKey : FirDeclarationDataKey()
var <D : FirCallableDeclaration<*>>
        D.originalForIntersectionOverrideAttr: D? by FirDeclarationDataRegistry.data(IntersectionOverrideOriginalKey)

private object InitialSignatureKey : FirDeclarationDataKey()
var FirCallableDeclaration<*>.initialSignatureAttr: FirCallableDeclaration<*>? by FirDeclarationDataRegistry.data(InitialSignatureKey)

private object DelegatedWrapperDataKey : FirDeclarationDataKey()
class DelegatedWrapperData<D : FirCallableDeclaration<*>>(
    val wrapped: D,
    val containingClass: ConeClassLikeLookupTag,
    val delegateField: FirField,
)
var <D : FirCallableDeclaration<*>>
        D.delegatedWrapperData: DelegatedWrapperData<D>? by FirDeclarationDataRegistry.data(DelegatedWrapperDataKey)
