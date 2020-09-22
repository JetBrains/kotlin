/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.psi.KtElement

object LowLevelFirApiFacade {
    fun getResolveStateFor(element: KtElement): FirModuleResolveState =
        element.firResolveState()

    fun getSessionFor(element: KtElement): FirSession =
        getResolveStateFor(element).getSessionFor(element.getModuleInfo())

    fun getOrBuildFirFor(element: KtElement, resolveState: FirModuleResolveState): FirElement =
        resolveState.getOrBuildFirFor(element)

    fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> {
        return resolveState.getDiagnostics(element)
    }
}
