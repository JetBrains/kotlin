/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedCallableDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility

public sealed class CallableDeclarationBuildingContext<T : FirCallableDeclaration>(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?
) : DeclarationBuildingContext<T>(session, key, owner){
    private val statusConfigs: MutableList<FirResolvedCallableDeclarationStatusImpl.() -> Unit> = mutableListOf()

    /**
     * Allows to configure flags in status of declaration
     * For full list of possible flags refer to [org.jetbrains.kotlin.fir.declarations.FirCallableDeclarationStatus] class
     * Note that not all flags are meaningful for each declaration
     *   E.g. there is no point to mark function as inner
     */
    public fun status(statusConfig: FirResolvedCallableDeclarationStatusImpl.() -> Unit) {
        statusConfigs += statusConfig
    }

    protected fun generateStatus(): FirResolvedCallableDeclarationStatusImpl {
        return FirResolvedCallableDeclarationStatusImpl(
            visibility,
            modality,
            visibility.toEffectiveVisibility(owner, forClass = true)
        ).also {
            for (statusConfig in statusConfigs) {
                it.apply(statusConfig)
            }
        }
    }

}
