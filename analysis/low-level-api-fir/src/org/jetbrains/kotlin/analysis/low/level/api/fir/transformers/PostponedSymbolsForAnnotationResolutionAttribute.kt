/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isLocalForLazyResolutionPurposes
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

private object PostponedSymbolsForAnnotationResolutionKey : FirDeclarationDataKey()

/**
 * During [implicit type][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] phase we can
 * meet [FirAnnotationCall][org.jetbrains.kotlin.fir.expressions.FirAnnotationCall]'s which do not belong to us
 * (their [containingDeclarationSymbol][org.jetbrains.kotlin.fir.expressions.FirAnnotationCall.containingDeclarationSymbol] is not in our context).
 * Such annotations can't be resolved in-place due to:
 * * Contract violation
 * ([implicit type][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] phase is less than [annotation arguments][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.ANNOTATION_ARGUMENTS] phase)
 *
 * * Concurrent modification issues.
 * The same instance of a foreign annotation is shared at least between two declarations – the original declaration and this call site,
 * so simultaneous modification of the annotation can lead to undefined behavior.
 *
 * * Wrong context on the call site.
 * It is possible that the annotation can use arguments which are not visible from the call site.
 *
 * @return The collection of [FirBasedSymbol]s which have to be resolved on
 * [annotation arguments][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.ANNOTATION_ARGUMENTS] phase before [this] declaration.
 *
 * @see LLFirImplicitBodyTargetResolver
 * @see LLFirAnnotationArgumentsTargetResolver
 */
internal var FirCallableDeclaration.postponedSymbolsForAnnotationResolution: Collection<FirBasedSymbol<*>>?
        by FirDeclarationDataRegistry.data(PostponedSymbolsForAnnotationResolutionKey)

/**
 * Some symbols shouldn't be processed as a regular annotation owner and should be just skipped.
 * Example:
 * ```kotlin
 * fun foo() {
 *   class Local {
 *     fun localMemberWithoutType() = localMember()
 *     fun localMember(): @Anno Int = 0
 *   }
 * }
 * ```
 * Here `localMember` is the owner of `Anno`, but we shouldn't process it as a usual non-local declaration, because
 * this annotation cannot be leaked out of the body in not fully resolved state.
 *
 * @return true if this symbol shouldn't be processed as the owner of an annotation call
 */
internal fun FirBasedSymbol<*>.cannotResolveAnnotationsOnDemand(): Boolean {
    return this is FirCallableSymbol<*> && isLocalForLazyResolutionPurposes
}

/**
 * Invoke [action] on each callable declaration that can have postponed symbols
 *
 * @see postponedSymbolsForAnnotationResolution
 */
internal fun FirDeclaration.forEachDeclarationWhichCanHavePostponedSymbols(action: (FirCallableDeclaration) -> Unit) {
    when (this) {
        is FirCallableDeclaration -> action(this)
        else -> {}
    }
}

/**
 * @return a symbol which should be used as a member of [postponedSymbolsForAnnotationResolution] collection
 *
 * @see postponedSymbolsForAnnotationResolution
 */
internal fun FirBasedSymbol<*>.unwrapSymbolToPostpone(): FirBasedSymbol<*> = when (this) {
    is FirValueParameterSymbol -> containingFunctionSymbol
    else -> this
}

/**
 * @return an [unwrapped][unwrapSymbolToPostpone] symbol which [can][cannotResolveAnnotationsOnDemand] be resolved on demand
 *
 * @see unwrapSymbolToPostpone
 * @see cannotResolveAnnotationsOnDemand
 */
internal fun FirBasedSymbol<*>.symbolToPostponeIfCanBeResolvedOnDemand(): FirBasedSymbol<*>? {
    return unwrapSymbolToPostpone().takeUnless { it.cannotResolveAnnotationsOnDemand() }
}
