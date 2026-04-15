/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.io.File
import java.nio.file.Path

/**
 * A wrapper class for `KotlinToolchains` to accommodate functionality
 * changes and compatibility adjustments for versions pre Kotlin 2.4.20.
 *
 * Delegates the majority of functionality to the `base` implementation,
 * while selectively overriding methods to either introduce new behavior
 * or adapt existing operations.
 *
 * @param base The base implementation of `KotlinToolchains` to wrap.
 */
@Suppress("ClassName")
internal class KotlinWrapperPre2_4_20(
    private val base: KotlinToolchains,
) : KotlinToolchains by base {

    @Suppress("UNCHECKED_CAST")
    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T = when (type) {
        JvmPlatformToolchain::class.java -> JvmPlatformToolchainWrapper(base.getToolchain(type))
        else -> base.getToolchain(type)
    } as T

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionWrapper(this, base.createBuildSession())
    }

    class BuildSessionWrapper(override val kotlinToolchains: KotlinWrapperPre2_4_20, private val base: KotlinToolchains.BuildSession) :
        KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            return base.executeOperation(operation, executionPolicy, logger)
        }
    }

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(
            sources: List<Path>,
            destinationDirectory: Path,
        ): JvmCompilationOperation.Builder =
            JvmCompilationOperationBuilderWrapper(
                base.jvmCompilationOperationBuilder(sources, destinationDirectory)
            )
    }

    private class JvmCompilationOperationBuilderWrapper(
        private val base: JvmCompilationOperation.Builder,
    ) : JvmCompilationOperation.Builder by base {
        override val compilerArguments: JvmCompilerArguments.Builder = JvmCompilerArgumentsBuilderWrapper(base.compilerArguments)
    }

    internal class JvmCompilerArgumentsBuilderWrapper(
        private val base: JvmCompilerArguments.Builder,
    ) : JvmCompilerArguments.Builder by base {

        @OptIn(ExperimentalCompilerArgument::class)
        override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) {
            when (key) {
                JvmCompilerArguments.CLASSPATH,
                JvmCompilerArguments.X_KLIB,
                JvmCompilerArguments.X_MODULE_PATH,
                    -> {
                    @Suppress("UNCHECKED_CAST")
                    (value as List<Path>?)?.checkNoneContains(File.pathSeparator)
                    base[key] = value
                }

                JvmCompilerArguments.X_FRIEND_PATHS,
                JvmCompilerArguments.X_JAVA_SOURCE_ROOTS,
                    -> {
                    @Suppress("UNCHECKED_CAST")
                    (value as List<Path>).checkNoneContains(",")
                    base[key] = value
                }

                else -> base[key] = value
            }
        }
    }
}

private fun List<Path>.checkNoneContains(other: CharSequence) {
    val invalidItem = firstOrNull { it.toFile().absolutePath.contains(other) }
    if (invalidItem != null) {
        throw CompilerArgumentsParseException(
            "Invalid character '${other}' found in argument '${invalidItem.toFile().absolutePath}'. " +
                    "This character is currently not supported in this context. " +
                    "If you need its support, please let us know: https://youtrack.jetbrains.com/issue/KT-85553"
        )
    }
}
