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
            // Unwrap so the pre-2.4.0 executeOperation can handle its own type check
            val realOperation = if (operation is BuildOperationWrapper) operation.baseOperation else operation
            return base.executeOperation(realOperation, executionPolicy, logger)
        }
    }

    private abstract class BuildOperationWrapper<R>(val baseOperation: BuildOperation<R>) : BuildOperation<R>

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(
            sources: List<Path>,
            destinationDirectory: Path,
        ): JvmCompilationOperation.Builder =
            JvmCompilationOperationBuilderWrapper(
                base.jvmCompilationOperationBuilder(sources, destinationDirectory),
            )
    }

    private class JvmCompilationOperationBuilderWrapper(
        private val base: JvmCompilationOperation.Builder,
    ) : JvmCompilationOperation.Builder by base {
        override val compilerArguments: JvmCompilerArguments.Builder = JvmCompilerArgumentsBuilderWrapper(base.compilerArguments)

        override fun build(): JvmCompilationOperation {
            return JvmCompilationOperationWrapper(
                base.build().also { it.compilerArguments.applyCompilerArguments(base.compilerArguments.toCompilerArguments()) })
        }
    }

    private class JvmCompilationOperationWrapper(
        private val base: JvmCompilationOperation,
    ) : JvmCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {

        override fun toBuilder(): JvmCompilationOperation.Builder =
            JvmCompilationOperationBuilderWrapper(base.toBuilder())
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

                else -> base[key] = value
            }
        }
    }
}

private typealias K2JVMCompilerArguments = Any

private fun JvmCompilerArguments.Builder.toCompilerArguments(): K2JVMCompilerArguments {
    var current: Any = this
    while (true) {
        val method = current::class.java.methods.firstOrNull { it.name == "toCompilerArguments" && it.parameterCount == 1 }
        if (method != null) {
            val arguments = method.parameterTypes[0].getDeclaredConstructor().newInstance()
            unwrapInvocationTargetException { method.invoke(current, arguments) }
            return arguments
        }
        current = current::class.java.getDeclaredField("base").also { it.isAccessible = true }.get(current)
    }
}

private fun JvmCompilerArguments.applyCompilerArguments(arguments: K2JVMCompilerArguments) {
    var current: Any = this
    val k2ArgsClass = arguments::class.java
    while (true) {
        val method = current::class.java.methods.firstOrNull {
            it.name == "applyCompilerArguments" && it.parameterCount == 1 && it.parameterTypes[0] == k2ArgsClass
        }
        if (method != null) {
            unwrapInvocationTargetException { method.invoke(current, arguments) }
            return
        }
        current = current::class.java.getDeclaredField("base").also { it.isAccessible = true }.get(current)
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
