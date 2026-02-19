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
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * A wrapper class for `KotlinToolchains` to accommodate functionality
 * changes and compatibility adjustments for versions pre-Kotlin 2.4.0.
 *
 * Delegates the majority of functionality to the `base` implementation,
 * while selectively overriding methods to either introduce new behavior
 * or adapt existing operations.
 *
 * @param base The base implementation of `KotlinToolchains` to wrap.
 */
internal class KotlinBelow240Wrapper(
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

    class BuildSessionWrapper(override val kotlinToolchains: KotlinBelow240Wrapper, private val base: KotlinToolchains.BuildSession) :
        KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            // we need to unwrap due to an `operation is BuildOperationImpl` check inside `executeOperation`
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
                base.jvmCompilationOperationBuilder(sources, destinationDirectory)
            )

        @Deprecated(
            "Use jvmCompilationOperationBuilder instead",
            replaceWith = ReplaceWith("jvmCompilationOperationBuilder(sources, destinationDirectory)")
        )
        override fun createJvmCompilationOperation(
            sources: List<Path>,
            destinationDirectory: Path,
        ): JvmCompilationOperation =
            JvmCompilationOperationWrapper(
                base.createJvmCompilationOperation(sources, destinationDirectory)
            )
    }

    private class JvmCompilationOperationWrapper(
        private val base: JvmCompilationOperation,
    ) : JvmCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override val compilerArguments: JvmCompilerArguments = JvmCompilerArgumentsWrapper(base.compilerArguments)
    }

    private class JvmCompilationOperationBuilderWrapper(
        private val base: JvmCompilationOperation.Builder,
    ) : JvmCompilationOperation.Builder by base {
        override val compilerArguments: JvmCompilerArguments.Builder = JvmCompilerArgumentsBuilderWrapper(base.compilerArguments)
    }

    internal class JvmCompilerArgumentsWrapper(
        private val base: JvmCompilerArguments,
    ) : JvmCompilerArguments by base {

        private val interceptor = JvmArgumentInterceptor(object : JvmArgumentAccessor {
            override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>) = base[key]
            override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) {
                base[key] = value
            }
        })

        override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = interceptor[key]

        @Deprecated("Compiler argument classes will become immutable in an upcoming release. Use a Builder instance to create and modify compiler arguments.")
        override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) = interceptor.set(key, value)
    }

    internal class JvmCompilerArgumentsBuilderWrapper(
        private val base: JvmCompilerArguments.Builder,
    ) : JvmCompilerArguments.Builder by base {

        private val interceptor = JvmArgumentInterceptor(object : JvmArgumentAccessor {
            override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>) = base[key]
            override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) {
                base[key] = value
            }
        })

        override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = interceptor[key]
        override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) = interceptor.set(key, value)
    }

    private interface JvmArgumentAccessor {
        operator fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V
        operator fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V)
    }

    @OptIn(ExperimentalCompilerArgument::class)
    private class JvmArgumentInterceptor(
        private val delegate: JvmArgumentAccessor,
    ) : JvmArgumentAccessor by delegate {

        @Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
        override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V {
            return when (key) {
                JvmCompilerArguments.X_PROFILE -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    val parts = stringValue.split(File.pathSeparator)
                    require(parts.size == 3) { "Invalid async profiler settings format: $this" }

                    ProfileCompilerCommand(Path(parts[0]), parts[1], Path(parts[2])) as V
                }

                JvmCompilerArguments.JDK_HOME -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    Path(stringValue) as V
                }

                else -> delegate[key]
            }
        }

        override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) {
            when (key) {
                JvmCompilerArguments.X_PROFILE -> {
                    val profileCompilerCommand = value as ProfileCompilerCommand?
                    val stringValue =
                        profileCompilerCommand?.let { "${it.profilerPath.toFile().absolutePath}${File.pathSeparator}${it.command}${File.pathSeparator}${it.outputDir.toFile().absolutePath}" }
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                    return
                }

                JvmCompilerArguments.JDK_HOME -> {
                    val pathValue = value as Path?
                    val stringValue = pathValue?.toFile()?.absolutePath
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                else -> delegate[key] = value
            }
        }
    }
}
