/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType

fun FirCallableSymbol<*>.dispatchReceiverTypeOrNull(): ConeKotlinType? =
    fir.dispatchReceiverType

fun FirCallableSymbol<*>.dispatchReceiverClassOrNull(): ConeClassLikeLookupTag? =
    fir.dispatchReceiverClassOrNull()

fun FirCallableDeclaration.dispatchReceiverClassOrNull(): ConeClassLikeLookupTag? {
    if (dispatchReceiverType is ConeIntersectionType && isIntersectionOverride) return symbol.baseForIntersectionOverride!!.fir.dispatchReceiverClassOrNull()

    return (dispatchReceiverType as? ConeClassLikeType)?.lookupTag
}

fun FirCallableSymbol<*>.containingClass(): ConeClassLikeLookupTag? = fir.containingClass()
fun FirCallableDeclaration.containingClass(): ConeClassLikeLookupTag? {
    return (containingClassForStaticMemberAttr ?: dispatchReceiverClassOrNull())
}

fun FirRegularClass.containingClassForLocal(): ConeClassLikeLookupTag? =
    if (isLocal) containingClassForLocalAttr else null

private object ContainingClassKey : FirDeclarationDataKey()
var FirCallableDeclaration.containingClassForStaticMemberAttr: ConeClassLikeLookupTag? by FirDeclarationDataRegistry.data(ContainingClassKey)
var FirRegularClass.containingClassForLocalAttr: ConeClassLikeLookupTag? by FirDeclarationDataRegistry.data(ContainingClassKey)

private object IsNewPlaceForBodyGeneration : FirDeclarationDataKey()
var FirRegularClass.isNewPlaceForBodyGeneration: Boolean? by FirDeclarationDataRegistry.data(IsNewPlaceForBodyGeneration)

val FirCallableDeclaration.isIntersectionOverride: Boolean get() = origin == FirDeclarationOrigin.IntersectionOverride
val FirCallableDeclaration.isSubstitutionOverride: Boolean get() = origin == FirDeclarationOrigin.SubstitutionOverride
val FirCallableDeclaration.isSubstitutionOrIntersectionOverride: Boolean get() = isSubstitutionOverride || isIntersectionOverride

val FirCallableSymbol<*>.isIntersectionOverride: Boolean get() = origin == FirDeclarationOrigin.IntersectionOverride
val FirCallableSymbol<*>.isSubstitutionOverride: Boolean get() = origin == FirDeclarationOrigin.SubstitutionOverride
val FirCallableSymbol<*>.isSubstitutionOrIntersectionOverride: Boolean get() = isSubstitutionOverride || isIntersectionOverride

inline val <reified D : FirCallableDeclaration> D.originalForSubstitutionOverride: D?
    get() = if (isSubstitutionOverride) originalForSubstitutionOverrideAttr else null

inline val <reified S : FirCallableSymbol<*>> S.originalForSubstitutionOverride: S?
    get() = fir.originalForSubstitutionOverride?.symbol as S?

inline val <reified D : FirCallableDeclaration> D.baseForIntersectionOverride: D?
    get() = if (isIntersectionOverride) originalForIntersectionOverrideAttr else null

inline val <reified S : FirCallableSymbol<*>> S.baseForIntersectionOverride: S?
    get() = fir.baseForIntersectionOverride?.symbol as S?

inline fun <reified D : FirCallableDeclaration> D.originalIfFakeOverride(): D? =
    originalForSubstitutionOverride ?: baseForIntersectionOverride

inline fun <reified S : FirCallableSymbol<*>> S.originalIfFakeOverride(): S? =
    fir.originalIfFakeOverride()?.symbol as S?

inline fun <reified D : FirCallableDeclaration> D.originalOrSelf(): D {
    var result = this
    while (result.isSubstitutionOrIntersectionOverride) {
        result = result.originalIfFakeOverride() ?: break
    }
    return result
}

inline fun <reified S : FirCallableSymbol<*>> S.originalOrSelf(): S = fir.originalOrSelf().symbol as S

inline fun <reified D : FirCallableDeclaration> D.unwrapFakeOverrides(): D {
    var current = this

    do {
        val next = current.originalIfFakeOverride() ?: return current
        current = next
    } while (true)
}

inline fun <reified S : FirCallableSymbol<*>> S.unwrapFakeOverrides(): S = fir.unwrapFakeOverrides().symbol as S

private object SubstitutedOverrideOriginalKey : FirDeclarationDataKey()

var <D : FirCallableDeclaration>
        D.originalForSubstitutionOverrideAttr: D? by FirDeclarationDataRegistry.data(SubstitutedOverrideOriginalKey)

private object IntersectionOverrideOriginalKey : FirDeclarationDataKey()

var <D : FirCallableDeclaration>
        D.originalForIntersectionOverrideAttr: D? by FirDeclarationDataRegistry.data(IntersectionOverrideOriginalKey)

private object InitialSignatureKey : FirDeclarationDataKey()

var FirCallableDeclaration.initialSignatureAttr: FirCallableDeclaration? by FirDeclarationDataRegistry.data(InitialSignatureKey)

private object MatchingParameterFunctionTypeKey : FirDeclarationDataKey()

/**
 * Consider the following
 * ```
 * fun <T> run(block: @Foo T.() -> Unit) {...}
 *
 * fun test() {
 *   run<String> {
 *     <caret>
 *   }
 * }
 * ```
 * The original function type `@Foo T.() -> Unit` can be accessed with this property on the FirAnonymousFunction at caret.
 */
var <D : FirAnonymousFunction>
        D.matchingParameterFunctionType: ConeKotlinType? by FirDeclarationDataRegistry.data(MatchingParameterFunctionTypeKey)

private object CorrespondingProperty : FirDeclarationDataKey()

/**
 * The corresponding [FirProperty] if the current value parameter is a `val` or `var` declared inside the primary constructor.
 */
var FirValueParameter.correspondingProperty: FirProperty? by FirDeclarationDataRegistry.data(CorrespondingProperty)
