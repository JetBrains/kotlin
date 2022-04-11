/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirKtModuleBasedModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.firModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

val KtSymbol.firSymbol: FirBasedSymbol<*>
    get() {
        require(this is KtFirSymbol<*>)
        return this.firSymbol
    }


fun FirBasedSymbol<*>.getContainingKtModule(resolveState: LLFirModuleResolveState): KtModule {
    val target = when (this) {
        is FirCallableSymbol -> {
            // callable fake overrides have use-site FirModuleData
            dispatchReceiverClassOrNull()?.toFirRegularClassSymbol(resolveState.rootModuleSession) ?: this
        }
        else -> this
    }
    return when (val moduleData = target.firModuleData) {
        is LLFirKtModuleBasedModuleData -> moduleData.ktModule
        is LLFirBuiltinsModuleData -> resolveState.project.getService(ProjectStructureProvider::class.java).getStdlibWithBuiltinsModule(moduleData.useSiteKtModule)
            ?: error("Builtins not found for the ${moduleData.useSiteKtModule.moduleDescription}")
    }
}

fun KtSymbol.getContainingKtModule(resolveState: LLFirModuleResolveState): KtModule =
    firSymbol.getContainingKtModule(resolveState)