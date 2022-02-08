/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.FirModuleResolveStateDepended
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.psi.KtElement

internal class TowerProviderForElementForState(private val state: LLFirModuleResolveState) : FirTowerContextProvider {
    override fun getClosestAvailableParentContext(ktElement: KtElement): FirTowerDataContext? {
        if (state is FirModuleResolveStateDepended) {
            state.towerProviderBuiltUponElement
                .getClosestAvailableParentContext(ktElement)
                ?.let { return it }

            return LowLevelFirApiFacadeForResolveOnAir
                .onAirGetTowerContextProvider(state.originalState, ktElement)
                .getClosestAvailableParentContext(ktElement)
        }
        return LowLevelFirApiFacadeForResolveOnAir
            .onAirGetTowerContextProvider(state, ktElement)
            .getClosestAvailableParentContext(ktElement)
    }
}