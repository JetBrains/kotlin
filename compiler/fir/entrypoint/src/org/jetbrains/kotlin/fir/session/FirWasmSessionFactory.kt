/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerWasmCheckers
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportProviderHolder
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.wasm.resolve.WasmPlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmWasiPlatformAnalyzerServices

@OptIn(SessionConfiguration::class)
object FirWasmSessionFactory : AbstractFirWebSessionFactory<FirWasmSessionFactory.Context, FirWasmSessionFactory.Context>() {

    // ==================================== Library session ====================================

    override fun createLibraryContext(configuration: CompilerConfiguration): Context {
        return Context(configuration.wasmTarget)
    }

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        registerDefaultComponents()
        registerWasmComponents(c.wasmTarget)
    }

    // ==================================== Platform session ====================================

    override fun createSourceContext(configuration: CompilerConfiguration): Context {
        return Context(configuration.wasmTarget)
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Context) {
        registerWasmCheckers(c.wasmTarget)
    }

    override fun FirSessionConfigurator.registerExtraPlatformCheckers(c: Context) {}

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        registerDefaultComponents()
        registerWasmComponents(c.wasmTarget)
    }

    @OptIn(SessionConfiguration::class)
    fun FirSession.registerWasmComponents(wasmTarget: WasmTarget) {
        val analyzerServices = when (wasmTarget) {
            WasmTarget.JS -> WasmPlatformAnalyzerServices
            WasmTarget.WASI -> WasmWasiPlatformAnalyzerServices
        }
        register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(analyzerServices))
    }

    // ==================================== Utilities ====================================

    class Context(val wasmTarget: WasmTarget)
}
