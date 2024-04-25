/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun initializeActualDeclarationExtractorIfStdlib(platformComponents: Fir2IrComponents): IrExtraActualDeclarationExtractor? {
    val session = platformComponents.session
    return runIf(session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
        val dependencyProviders = (session.dependenciesSymbolProvider as FirCachingCompositeSymbolProvider).providers
        val builtinsSymbolProvider = dependencyProviders.filterIsInstance<FirBuiltinSymbolProvider>().single()
        FirJvmBuiltinProviderActualDeclarationExtractor(
            builtinsSymbolProvider, platformComponents.classifierStorage, platformComponents.declarationStorage
        )
    }
}