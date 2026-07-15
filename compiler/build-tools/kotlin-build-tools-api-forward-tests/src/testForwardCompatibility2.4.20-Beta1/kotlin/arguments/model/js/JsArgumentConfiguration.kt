/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class, ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.js

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.ArgumentConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.supportsJs
import java.nio.file.Paths

internal class JsArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    private val descriptor: JsArgumentTestDescriptor<T>,
) : ArgumentConfiguration<T>(kotlinToolchain, descriptor) {
    val availableSinceVersion: KotlinReleaseVersion = descriptor.availableSinceVersion
    val argumentValues: List<T> = descriptor.argumentValues
    val argumentRawValues: List<String> = descriptor.argumentRawValues
    val invalidRawValues: List<String> = descriptor.invalidRawValues

    fun isPlatformSupported(): Boolean = kotlinToolchain.supportsJs()

    fun buildArguments(configure: CommonToolArguments.Builder.() -> Unit = {}): CommonToolArguments {
        return kotlinToolchain.js.jsLinkingOperationBuilder(Paths.get("input.klib"), Paths.get(".")).apply {
            compilerArguments.configure()
        }.build().compilerArguments
    }

    fun setArgument(arguments: CommonToolArguments.Builder, value: T) {
        descriptor.setArgument(arguments, value)
    }

    fun getArgument(arguments: CommonToolArguments): T = descriptor.getArgument(arguments)
}
