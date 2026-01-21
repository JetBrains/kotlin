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

class KotlinWasmCompilerArgumentsConfigurator : CommonKlibBasedCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is KotlinWasmCompilerArguments)

        super.configureAnalysisFlags(arguments, collector, languageVersion).also {
            it[allowFullyQualifiedNameInKClass] = wasm && wasmKClassFqn //Only enabled WASM BE supports this flag
        }
    }

    override fun configureLanguageFeatures(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
    ): MutableMap<LanguageFeature, LanguageFeature.State> = with(arguments) {
        require(this is KotlinWasmCompilerArguments)
        val result = super.configureLanguageFeatures(arguments, collector)
        result.configureWasmLanguageFeatures(this)
//        // TODO: Should be removed (see KT-80182)
        result[LanguageFeature.AllowAnyAsAnActualTypeForExpectInterface] = LanguageFeature.State.ENABLED
        result[LanguageFeature.JsAllowImplementingFunctionInterface] = LanguageFeature.State.ENABLED
        return result
    }
}
