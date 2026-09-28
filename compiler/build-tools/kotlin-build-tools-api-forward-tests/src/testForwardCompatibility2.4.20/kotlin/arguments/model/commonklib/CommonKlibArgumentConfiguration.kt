/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(DeprecatedCompilerArgument::class, ExperimentalBuildToolsApi::class, ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.commonklib

import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain.Companion.wasm
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.ArgumentConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.JS_KLIB
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.JS_LINKING
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.WASM_KLIB
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.WASM_LINKING
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.supportsJs
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.supportsWasm
import java.nio.file.Paths

internal class CommonKlibArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    private val descriptor: CommonKlibArgumentTestDescriptor<T>,
    val operationKind: CommonKlibArgumentOperationKind,
) : ArgumentConfiguration<T>(kotlinToolchain, descriptor) {
    val availableSinceVersion: KotlinReleaseVersion = descriptor.availableSinceVersion
    val argumentValues: List<T> = descriptor.argumentValues
    val argumentRawValues: List<String> = descriptor.argumentRawValues
    val invalidArgumentValues: List<T> = descriptor.invalidArgumentValues
    val invalidRawValues: List<String> = descriptor.invalidRawValues

    fun isPlatformSupported(): Boolean = when (operationKind) {
        JS_KLIB, JS_LINKING -> kotlinToolchain.supportsJs()
        WASM_KLIB, WASM_LINKING -> kotlinToolchain.supportsWasm()
    }

    fun buildArguments(configure: CommonToolArguments.Builder.() -> Unit = {}): CommonToolArguments {
        return when (operationKind) {
            JS_KLIB -> kotlinToolchain.js.jsKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments.configure()
            }.build().compilerArguments
            JS_LINKING -> kotlinToolchain.js.jsLinkingOperationBuilder(Paths.get("input.klib"), Paths.get(".")).apply {
                compilerArguments.configure()
            }.build().compilerArguments
            WASM_KLIB -> kotlinToolchain.wasm.wasmKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments.configure()
            }.build().compilerArguments
            WASM_LINKING -> kotlinToolchain.wasm.wasmLinkingOperationBuilder(Paths.get("input.klib"), Paths.get(".")).apply {
                compilerArguments.configure()
            }.build().compilerArguments
        }
    }

    fun setArgument(arguments: CommonToolArguments.Builder, value: T) {
        descriptor.setArgument(arguments, value)
    }

    fun getArgument(arguments: CommonToolArguments): T = descriptor.getArgument(arguments)
}
