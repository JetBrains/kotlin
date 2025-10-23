/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerWasmJsCheckers
import org.jetbrains.kotlin.fir.checkers.registerWasmWasiCheckers
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportsProviderHolder
import org.jetbrains.kotlin.fir.scopes.impl.FirEnumEntriesSupport
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.resolve.DefaultImportsProvider
import org.jetbrains.kotlin.wasm.resolve.WasmJsDefaultImportsProvider
import org.jetbrains.kotlin.wasm.resolve.WasmWasiDefaultImportsProvider

@OptIn(SessionConfiguration::class)
sealed class FirWasmSessionFactory : AbstractFirKlibSessionFactory<Nothing?, Nothing?>() {
    object WasmJs : FirWasmSessionFactory() {
        override val defaultImportsProvider: DefaultImportsProvider
            get() = WasmJsDefaultImportsProvider

        override fun FirSessionConfigurator.registerPlatformCheckers() {
            registerWasmJsCheckers()
        }
    }

    object WasmWasi : FirWasmSessionFactory() {
        override val defaultImportsProvider: DefaultImportsProvider
            get() = WasmWasiDefaultImportsProvider

        override fun FirSessionConfigurator.registerPlatformCheckers() {
            registerWasmWasiCheckers()
        }
    }

    companion object {
        fun of(wasmTarget: WasmTarget): FirWasmSessionFactory {
            return when (wasmTarget) {
                WasmTarget.JS -> WasmJs
                WasmTarget.WASI -> WasmWasi
            }
        }
    }

    protected abstract val defaultImportsProvider: DefaultImportsProvider

    // ==================================== Library session ====================================

    override fun createLibraryContext(configuration: CompilerConfiguration): Nothing? = null

    override fun FirSession.registerLibrarySessionComponents(c: Nothing?) {
        registerWasmComponents()
    }

    // ==================================== Platform session ====================================

    override fun createSourceContext(configuration: CompilerConfiguration): Nothing? = null

    abstract override fun FirSessionConfigurator.registerPlatformCheckers()

    override fun FirSessionConfigurator.registerExtraPlatformCheckers() {}

    override fun FirSession.registerSourceSessionComponents(c: Nothing?) {
        registerWasmComponents()
    }

    @OptIn(SessionConfiguration::class)
    fun FirSession.registerWasmComponents() {
        register(FirEnumEntriesSupport(this))
        register(FirDefaultImportsProviderHolder.of(defaultImportsProvider))
    }

    // ==================================== Utilities ====================================
}
