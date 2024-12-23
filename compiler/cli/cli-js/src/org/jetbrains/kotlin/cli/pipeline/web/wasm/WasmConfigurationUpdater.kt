/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.getWasmLowerings
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.list
import org.jetbrains.kotlin.cli.js.K2WasmCompilerImpl
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.js.config.propertyLazyInitialization
import org.jetbrains.kotlin.js.config.wasmCompilation
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

object WasmConfigurationUpdater : ConfigurationUpdater<K2JSCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JSCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        if (!configuration.wasmCompilation) return
        val arguments = input.arguments
        fillConfiguration(configuration, arguments)

        // setup phase config for the second compilation stage (Wasm codegen)
        if (arguments.includes != null) {
            configuration.phaseConfig = createPhaseConfig(arguments).also {
                it.list(getWasmLowerings(configuration, isIncremental = false))
            }
        }
    }

    /**
     * This part of the configuration update is shared between phased K2 CLI and
     * K1 implementation of [K2WasmCompilerImpl.tryInitializeCompiler].
     */
    internal fun fillConfiguration(configuration: CompilerConfiguration, arguments: K2JSCompilerArguments) {
        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, arguments.wasmEnableArrayRangeChecks)
        configuration.put(WasmConfigurationKeys.WASM_DEBUG, arguments.wasmDebug)
        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, arguments.wasmEnableAsserts)
        configuration.put(WasmConfigurationKeys.WASM_GENERATE_WAT, arguments.wasmGenerateWat)
        configuration.put(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS, arguments.wasmUseTrapsInsteadOfExceptions)
        configuration.put(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, arguments.wasmUseNewExceptionProposal)
        configuration.put(WasmConfigurationKeys.WASM_USE_JS_TAG, arguments.wasmUseJsTag ?: arguments.wasmUseNewExceptionProposal)
        configuration.put(WasmConfigurationKeys.WASM_GENERATE_DWARF, arguments.generateDwarf)
        configuration.put(WasmConfigurationKeys.WASM_FORCE_DEBUG_FRIENDLY_COMPILATION, arguments.forceDebugFriendlyCompilation)
        configuration.putIfNotNull(WasmConfigurationKeys.WASM_TARGET, arguments.wasmTarget?.let(WasmTarget::fromName))
        configuration.putIfNotNull(WasmConfigurationKeys.DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE, arguments.irDceDumpDeclarationIrSizesToFile)
        configuration.propertyLazyInitialization = arguments.irPropertyLazyInitialization
    }
}
