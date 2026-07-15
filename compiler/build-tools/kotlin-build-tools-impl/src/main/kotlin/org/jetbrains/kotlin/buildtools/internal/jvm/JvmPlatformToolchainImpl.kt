/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.KaptCompilerPlugin
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.KotlinToolchainsImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.DiscoverScriptExtensionsOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmClasspathSnapshottingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import java.nio.file.Path

internal class JvmPlatformToolchainImpl(
    private val kotlinToolchains: KotlinToolchainsImpl,
) : JvmPlatformToolchain {
    @Deprecated(
        "Use jvmCompilationOperationBuilder instead",
        replaceWith = ReplaceWith("jvmCompilationOperationBuilder(sources, destinationDirectory)"),
        level = DeprecationLevel.HIDDEN
    )
    fun createJvmCompilationOperation(
        sources: List<Path>,
        destinationDirectory: Path,
    ): JvmCompilationOperation =
        JvmCompilationOperationImpl(sources, destinationDirectory, kotlinToolchains = kotlinToolchains)

    override fun jvmCompilationOperationBuilder(
        sources: List<Path>,
        destinationDirectory: Path,
    ): JvmCompilationOperation.Builder =
        JvmCompilationOperationImpl(sources, destinationDirectory, kotlinToolchains = kotlinToolchains)

    @Deprecated(
        "Use `classpathSnapshottingOperationBuilder` instead",
        replaceWith = ReplaceWith("classpathSnapshottingOperationBuilder(classpathEntry)"),
        level = DeprecationLevel.HIDDEN
    )
    fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
        return JvmClasspathSnapshottingOperationImpl(classpathEntry)
    }

    override fun classpathSnapshottingOperationBuilder(classpathEntry: Path): JvmClasspathSnapshottingOperation.Builder =
        JvmClasspathSnapshottingOperationImpl(classpathEntry)

    override fun discoverScriptExtensionsOperationBuilder(classpath: List<Path>): DiscoverScriptExtensionsOperationImpl {
        return DiscoverScriptExtensionsOperationImpl(classpath)
    }

    override fun kaptCompilerPluginBuilder(kaptClasspath: List<Path>): KaptCompilerPlugin.Builder {
        return KaptCompilerPluginImpl(kaptClasspath.toList())
    }
}
