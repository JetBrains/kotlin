/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("DEPRECATION")
internal class PreKotlin230Wrapper(
    private val base: KotlinToolchains,
) : KotlinToolchains by base {

    @Suppress("UNCHECKED_CAST")
    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T = when (type) {
        JvmPlatformToolchain::class.java -> JvmPlatformToolchainWrapper(base.getToolchain(type))
        else -> error("Unsupported platform toolchain type: $type. Only JVM compilation is supported for now.")
    } as T

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionWrapper(base.createBuildSession())
    }

    class BuildSessionWrapper(private val base: KotlinToolchains.BuildSession) : KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            val realOperation = if (operation is BuildOperationWrapper) operation.baseOperation else operation
            return base.executeOperation(realOperation, executionPolicy, logger)
        }
    }

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperation.Builder {
            return JvmCompilationOperationWrapper(
                base.createJvmCompilationOperation(sources, destinationDirectory),
                this,
                sources,
                destinationDirectory
            )
        }

        @Deprecated(
            "Use newJvmCompilationOperation instead",
            replaceWith = ReplaceWith("newJvmCompilationOperation(sources, destinationDirectory)")
        )
        override fun createJvmCompilationOperation(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperation {
            return jvmCompilationOperationBuilder(sources, destinationDirectory).build()
        }

        override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
            return createJvmCompilationOperation(listOf(), Paths.get("")).createSnapshotBasedIcOptions()
        }
    }

    private abstract class BuildOperationWrapper<R>(val baseOperation: BuildOperation<R>) : BuildOperation<R>

    private class JvmCompilationOperationWrapper(
        private val base: JvmCompilationOperation,
        val toolchain: JvmPlatformToolchainWrapper,
        private val sources: List<Path>,
        private val destinationDirectory: Path,
    ) : BuildOperationWrapper<CompilationResult>(base), JvmCompilationOperation by base, JvmCompilationOperation.Builder {
        override fun toBuilder(): JvmCompilationOperation.Builder {
            return copy()
        }

        override fun build(): JvmCompilationOperation {
            return copy() as JvmCompilationOperation
        }

        fun copy(): JvmCompilationOperation.Builder {
            return toolchain.jvmCompilationOperationBuilder(sources, destinationDirectory).apply {
                compilerArguments.applyArgumentStrings(base.compilerArguments.toArgumentStrings())
                JvmCompilationOperation::class.java.declaredFields.filter { it.type.isAssignableFrom(JvmCompilationOperation.Option::class.java) }
                    .forEach { field ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            this[field.get(JvmCompilationOperation.Companion) as JvmCompilationOperation.Option<Any?>] =
                                base[field.get(JvmCompilationOperation.Companion) as JvmCompilationOperation.Option<Any?>]
                        } catch (_: IllegalStateException) {
                            // this field was not set and has no default
                        }
                    }
                BuildOperation::class.java.declaredFields.filter { it.type.isAssignableFrom(BuildOperation.Option::class.java) }
                    .forEach { field ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            this[field.get(BuildOperation.Companion) as BuildOperation.Option<Any?>] =
                                base[field.get(BuildOperation.Companion) as BuildOperation.Option<Any?>]
                        } catch (_: IllegalStateException) {
                            // this field was not set and has no default
                        }
                    }
            }
        }
    }
}


