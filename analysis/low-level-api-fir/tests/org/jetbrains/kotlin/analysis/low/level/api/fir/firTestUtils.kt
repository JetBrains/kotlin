/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirNotUnderContentRootResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceResolveSession
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.psi.KtElement


internal fun FirElement.renderWithClassName(): String =
    "${this::class.simpleName} `${render()}`"


internal fun FirBasedSymbol<*>.name(): String = when (this) {
    is FirCallableSymbol<*> -> callableId.callableName.asString()
    is FirClassLikeSymbol<*> -> classId.shortClassName.asString()
    is FirAnonymousInitializerSymbol -> "<init>"
    is FirFileSymbol -> "<FILE>"
    else -> error("unknown symbol ${this::class.simpleName}")
}

internal fun FirDeclaration.name(): String = symbol.name()

internal inline fun <R> resolveWithClearCaches(
    context: KtElement,
    noinline configureSession: LLFirSession.() -> Unit = {},
    action: (LLFirResolveSession) -> R,
): R {
    val project = context.project
    val firResolveSession = createFirResolveSessionForNoCaching(context.getKtModule(project), project, configureSession)
    return action(firResolveSession)
}

internal val LLFirResolveSession.isSourceSession: Boolean
    get() {
        return when (this) {
            is LLFirSourceResolveSession, is LLFirNotUnderContentRootResolveSession -> true
            else -> false
        }
    }