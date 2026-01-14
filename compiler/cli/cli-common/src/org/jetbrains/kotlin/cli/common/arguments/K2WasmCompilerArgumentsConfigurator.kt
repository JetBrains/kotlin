/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion

class K2WasmCompilerArgumentsConfigurator : CommonKlibBasedCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is K2WasmCompilerArguments)

        super.configureAnalysisFlags(arguments, collector, languageVersion).also {
            it[allowFullyQualifiedNameInKClass] = wasmKClassFqn //Only enabled WASM BE supports this flag
        }
    }

    override fun configureLanguageFeatures(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
    ): MutableMap<LanguageFeature, LanguageFeature.State> = with(arguments) {
        require(this is K2WasmCompilerArguments)
        val result = super.configureLanguageFeatures(arguments, collector)
//        result.configureJsLanguageFeatures(this)
//         TODO: Should be removed (see KT-80182)
//        result[LanguageFeature.AllowAnyAsAnActualTypeForExpectInterface] = LanguageFeature.State.ENABLED
        if (arguments is K2WasmCompilerArguments) {
            result[LanguageFeature.JsAllowImplementingFunctionInterface] = LanguageFeature.State.ENABLED
        }
        return result
    }
}
