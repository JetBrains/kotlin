/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.MODULE_ES
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion

class K2JSCompilerArgumentsConfigurator : CommonKlibBasedCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is K2JSCompilerArguments)
        if (irPerFile && (moduleKind != MODULE_ES && target != ES_2015)) {
            reporter.reportError(
                "Per-file compilation can't be used with any `moduleKind` except `es` (ECMAScript Modules)"
            )
        }

        super.configureAnalysisFlags(arguments, reporter, languageVersion).apply {
            putAnalysisFlag(allowFullyQualifiedNameInKClass, wasm && wasmKClassFqn) //Only enabled WASM BE supports this flag
        }
    }

    override fun configureLanguageFeatures(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
    ): MutableMap<LanguageFeature, LanguageFeature.State> = with(arguments) {
        require(this is K2JSCompilerArguments)
        val result = super.configureLanguageFeatures(arguments, reporter)
        result.configureJsLanguageFeatures(this)
        // TODO: Should be removed (see KT-80182)
        result[LanguageFeature.AllowAnyAsAnActualTypeForExpectInterface] = LanguageFeature.State.ENABLED
        if (wasm) {
            result[LanguageFeature.JsAllowImplementingFunctionInterface] = LanguageFeature.State.ENABLED
        }
        return result
    }
}
