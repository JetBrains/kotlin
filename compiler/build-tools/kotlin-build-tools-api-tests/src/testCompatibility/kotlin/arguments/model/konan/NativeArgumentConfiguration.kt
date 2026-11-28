/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.konan

import org.jetbrains.kotlin.arguments.description.actualNativeArguments
import org.jetbrains.kotlin.arguments.description.removed.removedNativeArguments
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain.Companion.native
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentConfiguration
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion
import java.nio.file.Paths

internal class NativeArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    private val descriptor: NativeArgumentTestDescriptor<T>,
) : ArgumentConfiguration<T>(kotlinToolchain, descriptor, listOf(actualNativeArguments, removedNativeArguments)) {
    val argumentValues: List<T> = descriptor.argumentValues
    val argumentRawValues: List<String> = descriptor.argumentRawValues

    val invalidArgumentValues: List<T> = descriptor.invalidArgumentValues
    val invalidRawValues: List<String> = descriptor.invalidRawValues

    /**
     * Converting the arguments doesn't run the Kotlin/Native compiler, so unlike the compilation tests,
     * it only needs a Build Tools API implementation that knows about Kotlin/Native.
     */
    fun isPlatformSupported(): Boolean =
        KotlinToolingVersion(kotlinToolchain.getCompilerVersion()).toKotlinVersion() >= KotlinVersion(2, 5, 20)

    /**
     * Builds the arguments of a klib compilation operation, the only Kotlin/Native operation available so far.
     */
    fun buildArguments(configure: CommonToolArguments.Builder.() -> Unit = {}): CommonToolArguments =
        kotlinToolchain.native.nativeKlibCompilationOperationBuilder(emptyList(), Paths.get("out.klib")).apply {
            compilerArguments.configure()
        }.build().compilerArguments

    fun setArgument(arguments: CommonToolArguments.Builder, value: T) {
        descriptor.setArgument(arguments, value)
    }

    fun getArgument(arguments: CommonToolArguments): T = descriptor.getArgument(arguments)
}
