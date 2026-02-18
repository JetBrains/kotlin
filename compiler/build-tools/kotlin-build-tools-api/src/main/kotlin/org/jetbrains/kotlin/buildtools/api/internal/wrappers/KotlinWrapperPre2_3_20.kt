/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * A wrapper class for `KotlinToolchains` to accommodate functionality
 * changes and compatibility adjustments for versions pre Kotlin 2.3.20.
 *
 * Delegates the majority of functionality to the `base` implementation,
 * while selectively overriding methods to either introduce new behavior
 * or adapt existing operations.
 *
 * @param base The base implementation of `KotlinToolchains` to wrap.
 */
@Suppress("DEPRECATION", "ClassName")
internal class KotlinWrapperPre2_3_20(
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

    override fun daemonExecutionPolicyBuilder(): ExecutionPolicy.WithDaemon.Builder = WithDaemonWrapper(base.createDaemonExecutionPolicy())

    private inner class WithDaemonWrapper(val baseExecutionPolicy: ExecutionPolicy.WithDaemon) :
        ExecutionPolicy.WithDaemon by baseExecutionPolicy, ExecutionPolicy.WithDaemon.Builder {
        override fun build(): ExecutionPolicy.WithDaemon = deepCopy()

        override fun toBuilder(): ExecutionPolicy.WithDaemon.Builder = deepCopy()

        private fun deepCopy() = WithDaemonWrapper(createDaemonExecutionPolicy()).also { newPolicy ->
            ExecutionPolicy.WithDaemon::class.java.declaredFields.filter {
                it.type.isAssignableFrom(
                    ExecutionPolicy.WithDaemon.Option::class.java
                )
            }.forEach { field ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    newPolicy[field.get(ExecutionPolicy.WithDaemon.Companion) as ExecutionPolicy.WithDaemon.Option<Any?>] =
                        this[field.get(ExecutionPolicy.WithDaemon.Companion) as ExecutionPolicy.WithDaemon.Option<*>]
                } catch (_: IllegalStateException) {
                    // this field was not set and has no default
                }
            }
        }
    }

    class BuildSessionWrapper(override val kotlinToolchains: KotlinWrapperPre2_3_20, private val base: KotlinToolchains.BuildSession) :
        KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            // we need to unwrap due to an `operation is BuildOperationImpl` check inside `executeOperation`
            val realOperation = if (operation is BuildOperationWrapper) operation.baseOperation else operation
            val realExecutionPolicy = if (executionPolicy is WithDaemonWrapper) executionPolicy.baseExecutionPolicy else executionPolicy
            return base.executeOperation(realOperation, realExecutionPolicy, logger)
        }
    }

    private abstract class BuildOperationWrapper<R>(val baseOperation: BuildOperation<R>) : BuildOperation<R>

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperationWrapper {
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
            return jvmCompilationOperationBuilder(sources, destinationDirectory)
        }

        override fun classpathSnapshottingOperationBuilder(classpathEntry: Path): JvmClasspathSnapshottingOperationWrapper {
            return JvmClasspathSnapshottingOperationWrapper(base.createClasspathSnapshottingOperation(classpathEntry), this, classpathEntry)
        }

        @Deprecated(
            "Use classpathSnapshottingOperationBuilder instead",
            replaceWith = ReplaceWith("classpathSnapshottingOperationBuilder(classpathEntry)")
        )
        override fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
            return classpathSnapshottingOperationBuilder(classpathEntry)
        }


        private inner class JvmClasspathSnapshottingOperationWrapper(
            private val base: JvmClasspathSnapshottingOperation,
            private val toolchain: JvmPlatformToolchainWrapper,
            override val classpathEntry: Path,
        ) : BuildOperationWrapper<ClasspathEntrySnapshot>(base), JvmClasspathSnapshottingOperation by base,
            JvmClasspathSnapshottingOperation.Builder {
            override fun toBuilder(): JvmClasspathSnapshottingOperation.Builder {
                return copy()
            }

            override fun build(): JvmClasspathSnapshottingOperation {
                return copy()
            }

            fun copy() = toolchain.classpathSnapshottingOperationBuilder(classpathEntry)
                .also { newOperation: JvmClasspathSnapshottingOperationWrapper ->
                    newOperation.copyBuildOperationOptions(this)
                    JvmClasspathSnapshottingOperation::class.java.declaredFields.filter {
                        it.type.isAssignableFrom(
                            JvmClasspathSnapshottingOperation.Option::class.java
                        )
                    }.forEach { field ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            newOperation[field.get(JvmClasspathSnapshottingOperation.Companion) as JvmClasspathSnapshottingOperation.Option<Any?>] =
                                this[field.get(JvmClasspathSnapshottingOperation.Companion) as JvmClasspathSnapshottingOperation.Option<*>]
                        } catch (_: IllegalStateException) {
                            // this field was not set and has no default
                        }
                    }
                }
        }

        private inner class JvmCompilationOperationWrapper(
            private val base: JvmCompilationOperation,
            private val toolchain: JvmPlatformToolchainWrapper,
            override val sources: List<Path>,
            override val destinationDirectory: Path,
            override val compilerArguments: JvmCompilerArgumentsWrapper = JvmCompilerArgumentsWrapper(
                base.compilerArguments,
                argumentsFactory = {
                    toolchain.jvmCompilationOperationBuilder(
                        emptyList(),
                        Path("")
                    ).compilerArguments
                }),
        ) : BuildOperationWrapper<CompilationResult>(base), JvmCompilationOperation by base, JvmCompilationOperation.Builder {
            override fun toBuilder(): JvmCompilationOperation.Builder {
                return copy()
            }

            override fun build(): JvmCompilationOperation {
                return copy()
            }

            override fun snapshotBasedIcConfigurationBuilder(
                workingDirectory: Path,
                sourcesChanges: SourcesChanges,
                dependenciesSnapshotFiles: List<Path>,
            ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
                val options = createSnapshotBasedIcOptions()
                return JvmSnapshotBasedIncrementalCompilationConfigurationWrapper(
                    workingDirectory,
                    sourcesChanges,
                    dependenciesSnapshotFiles,
                    options
                )
            }

            @Deprecated(
                "The shrunkClasspathSnapshot parameter is no longer required",
                replaceWith = ReplaceWith("snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles)"),
                level = DeprecationLevel.WARNING
            )
            override fun snapshotBasedIcConfigurationBuilder(
                workingDirectory: Path,
                sourcesChanges: SourcesChanges,
                dependenciesSnapshotFiles: List<Path>,
                shrunkClasspathSnapshot: Path,
            ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
                val options = createSnapshotBasedIcOptions()
                return JvmSnapshotBasedIncrementalCompilationConfigurationWrapper(
                    workingDirectory,
                    sourcesChanges,
                    dependenciesSnapshotFiles,
                    shrunkClasspathSnapshot,
                    options
                )
            }

            inner class JvmSnapshotBasedIncrementalCompilationConfigurationWrapper(
                workingDirectory: Path,
                sourcesChanges: SourcesChanges,
                dependenciesSnapshotFiles: List<Path>,
                shrunkClasspathSnapshot: Path,
                options: JvmSnapshotBasedIncrementalCompilationOptions,
            ) : JvmSnapshotBasedIncrementalCompilationConfiguration(
                workingDirectory,
                sourcesChanges,
                dependenciesSnapshotFiles,
                shrunkClasspathSnapshot,
                options
            ), JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {

                constructor(
                    workingDirectory: Path,
                    sourcesChanges: SourcesChanges,
                    dependenciesSnapshotFiles: List<Path>,
                    options: JvmSnapshotBasedIncrementalCompilationOptions,
                ) : this(
                    workingDirectory,
                    sourcesChanges,
                    dependenciesSnapshotFiles,
                    workingDirectory.resolve("shrunk-classpath-snapshot.bin"),
                    options
                )

                override fun toBuilder(): Builder = deepCopy()

                override fun <V> get(key: Option<V>): V {
                    return options[key]
                }

                override fun <V> set(key: Option<V>, value: V) {
                    options[key] = value
                }

                override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration = deepCopy()

                private fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationWrapper {
                    return JvmSnapshotBasedIncrementalCompilationConfigurationWrapper(
                        workingDirectory, sourcesChanges, dependenciesSnapshotFiles, shrunkClasspathSnapshot,
                        createSnapshotBasedIcOptions().also { newOptions ->
                            JvmSnapshotBasedIncrementalCompilationConfiguration::class.java.declaredFields.filter {
                                it.type.isAssignableFrom(
                                    Option::class.java
                                )
                            }.forEach { field ->
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    newOptions[field.get(Companion) as Option<Any?>] =
                                        this[field.get(Companion) as Option<*>]
                                } catch (_: IllegalStateException) {
                                    // this field was not set and has no default
                                }
                            }
                        }
                    )
                }
            }


            private fun copy(): JvmCompilationOperationWrapper {
                return toolchain.jvmCompilationOperationBuilder(sources, destinationDirectory)
                    .also { newOperation: JvmCompilationOperationWrapper ->
                        newOperation.copyBuildOperationOptions(this)
                        JvmCompilationOperation::class.java.declaredFields.filter { it.type.isAssignableFrom(JvmCompilationOperation.Option::class.java) }
                            .forEach { field ->
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    newOperation[field.get(JvmCompilationOperation.Companion) as JvmCompilationOperation.Option<Any?>] =
                                        this[field.get(JvmCompilationOperation.Companion) as JvmCompilationOperation.Option<*>]
                                } catch (_: IllegalStateException) {
                                    // this field was not set and has no default
                                }
                            }
                        newOperation.compilerArguments.applyArgumentStrings(this.compilerArguments.toArgumentStrings())
                    }
            }
        }

        private fun BuildOperationWrapper<*>.copyBuildOperationOptions(from: BuildOperation<*>) {
            BuildOperation::class.java.declaredFields.filter { it.type.isAssignableFrom(BuildOperation.Option::class.java) }
                .forEach { field ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        this[field.get(BuildOperation.Companion) as BuildOperation.Option<Any?>] =
                            from[field.get(BuildOperation.Companion) as BuildOperation.Option<*>]
                    } catch (_: IllegalStateException) {
                        // this field was not set and has no default
                    }
                }
        }
    }

    private class JvmCompilerArgumentsWrapper(
        private val baseCompilerArguments: JvmCompilerArguments,
        private val argumentsFactory: () -> JvmCompilerArguments,
    ) :
        JvmCompilerArguments by baseCompilerArguments, JvmCompilerArguments.Builder {
        override fun build(): JvmCompilerArguments {
            return JvmCompilerArgumentsWrapper(
                argumentsFactory().also { newArguments -> newArguments.applyArgumentStrings(toArgumentStrings()) },
                argumentsFactory
            )
        }
    }
}