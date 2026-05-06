/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)
@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.lang.reflect.InvocationTargetException
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
@Suppress("ClassName")
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
        override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
            baseExecutionPolicy.set(key, value)
        }

        override fun build(): ExecutionPolicy.WithDaemon = deepCopy()

        override fun toBuilder(): ExecutionPolicy.WithDaemon.Builder = deepCopy()

        private fun deepCopy(): WithDaemonWrapper = WithDaemonWrapper(base.createDaemonExecutionPolicy()).also { newPolicy ->
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
    private abstract class BaseCompilationOperationWrapper(val baseCompilationOperation: BaseCompilationOperation) :
        BuildOperationWrapper<CompilationResult>(baseCompilationOperation), BaseCompilationOperation

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperationWrapper {
            return JvmCompilationOperationWrapper(
                base.createJvmCompilationOperation(sources, destinationDirectory),
                this,
                sources,
                destinationDirectory
            )
        }

        override fun classpathSnapshottingOperationBuilder(classpathEntry: Path): JvmClasspathSnapshottingOperationWrapper {
            return JvmClasspathSnapshottingOperationWrapper(base.createClasspathSnapshottingOperation(classpathEntry), this, classpathEntry)
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

            override fun <V> set(
                key: JvmClasspathSnapshottingOperation.Option<V>,
                value: V,
            ) {
                base.set(key, value)
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

            override fun <V> set(key: BuildOperation.Option<V>, value: V) {
                base.set(key, value)
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
        ) : BaseCompilationOperationWrapper(base), JvmCompilationOperation by base, JvmCompilationOperation.Builder {
            override fun toBuilder(): JvmCompilationOperation.Builder {
                return copy()
            }

            override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
                base.set(key, value)
            }

            override fun build(): JvmCompilationOperation {
                return copy()
            }

            override fun snapshotBasedIcConfigurationBuilder(
                workingDirectory: Path,
                sourcesChanges: SourcesChanges,
                dependenciesSnapshotFiles: List<Path>,
            ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
                val options = base.createSnapshotBasedIcOptions()
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
                val options = base.createSnapshotBasedIcOptions()
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
                    val legacyOption = JvmSnapshotBasedIncrementalCompilationOptions.Option<V>(key.id)
                    return options[legacyOption]
                }

                override fun <V> set(key: Option<V>, value: V) {
                    val legacyOption = JvmSnapshotBasedIncrementalCompilationOptions.Option<V>(key.id)
                    options[legacyOption] = value
                }

                override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration = deepCopy()

                private fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationWrapper {
                    return JvmSnapshotBasedIncrementalCompilationConfigurationWrapper(
                        workingDirectory, sourcesChanges, dependenciesSnapshotFiles, shrunkClasspathSnapshot,
                        base.createSnapshotBasedIcOptions().also { newOptions ->
                            JvmSnapshotBasedIncrementalCompilationOptions::class.java.declaredFields.filter {
                                it.type.isAssignableFrom(
                                    JvmSnapshotBasedIncrementalCompilationOptions.Option::class.java
                                )
                            }.forEach { field ->
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    newOptions[field.get(JvmSnapshotBasedIncrementalCompilationOptions.Companion) as JvmSnapshotBasedIncrementalCompilationOptions.Option<Any?>] =
                                        options[field.get(JvmSnapshotBasedIncrementalCompilationOptions.Companion) as JvmSnapshotBasedIncrementalCompilationOptions.Option<*>]
                                } catch (_: IllegalStateException) {
                                    // this field was not set and has no default
                                }
                            }
                        }
                    )
                }

                override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
                    val oldOption = JvmSnapshotBasedIncrementalCompilationOptions.Option<V>(key.id)
                    options[oldOption] = value
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

            override fun <V> set(key: BaseCompilationOperation.Option<V>, value: V) {
                val oldOption = JvmCompilationOperation.Option<V>(key.id, key.availableSinceVersion)
                this[oldOption] = value
            }

            override fun <V> set(key: BuildOperation.Option<V>, value: V) {
                base[key] = value
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
        private val base: JvmCompilerArguments,
        private val argumentsFactory: () -> JvmCompilerArguments,
    ) :
        JvmCompilerArguments by base, JvmCompilerArguments.Builder {
        override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) {
            base.set(key, value)
        }

        override fun build(): JvmCompilerArguments {
            return JvmCompilerArgumentsWrapper(
                argumentsFactory().also { newArguments -> newArguments.applyArgumentStrings(toArgumentStrings()) },
                argumentsFactory
            )
        }

        override fun <V> set(
            key: CommonCompilerArguments.CommonCompilerArgument<V>,
            value: V,
        ) {
            base.set(key, value)
        }

        override fun <V> set(key: CommonToolArguments.CommonToolArgument<V>, value: V) {
            base.set(key, value)
        }

        override fun applyArgumentStrings(arguments: List<String>) {
            base.applyArgumentStrings(arguments)
        }
    }
}

private fun JvmCompilerArguments.applyArgumentStrings(toArgumentStrings: List<String>) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("applyArgumentStrings", List::class.java)
            .invoke(this, toArgumentStrings)
    }
}

private fun JvmPlatformToolchain.createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation =
    unwrapInvocationTargetException {
        this::class.java.getMethod("createClasspathSnapshottingOperation", Path::class.java)
            .invoke(this, classpathEntry) as JvmClasspathSnapshottingOperation
    }

private fun JvmPlatformToolchain.createJvmCompilationOperation(
    sources: List<Path>,
    destinationDirectory: Path,
): JvmCompilationOperation = unwrapInvocationTargetException {
    this::class.java.getMethod("createJvmCompilationOperation", List::class.java, Path::class.java).invoke(
        this, sources, destinationDirectory
    ) as JvmCompilationOperation
}

private fun KotlinToolchains.createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon = unwrapInvocationTargetException {
    this::class.java.getMethod("createDaemonExecutionPolicy").invoke(this) as ExecutionPolicy.WithDaemon
}

private fun <V> ExecutionPolicy.WithDaemon.set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("set", key::class.java, Any::class.java).invoke(this, key, value)
    }
}

private fun <V> JvmClasspathSnapshottingOperation.set(
    key: JvmClasspathSnapshottingOperation.Option<V>,
    value: V,
) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("set", key::class.java, Any::class.java).invoke(this, key, value)
    }
}


private operator fun <V> BuildOperation<*>.set(
    key: BuildOperation.Option<V>,
    value: V,
) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("set", key::class.java, Any::class.java).invoke(this, key, value)
    }
}

private fun <V> JvmCompilationOperation.set(
    key: JvmCompilationOperation.Option<V>,
    value: V,
) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("set", key::class.java, Any::class.java).invoke(this, key, value)
    }
}

internal operator fun <V> JvmCompilerArguments.set(
    key: JvmCompilerArguments.JvmCompilerArgument<V>,
    value: V,
) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("set", key::class.java, Any::class.java).invoke(this, key, value)
    }
}


internal operator fun <V> CommonCompilerArguments.set(
    key: CommonCompilerArguments.CommonCompilerArgument<V>,
    value: V,
) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("set", key::class.java, Any::class.java).invoke(this, key, value)
    }
}


private fun <V> CommonToolArguments.set(
    key: CommonToolArguments.CommonToolArgument<V>,
    value: V,
) {
    unwrapInvocationTargetException {
        this::class.java.getMethod("set", key::class.java, Any::class.java).invoke(this, key, value)
    }
}

private fun JvmCompilationOperation.createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions =
    unwrapInvocationTargetException {
        this::class.java.getMethod("createSnapshotBasedIcOptions").invoke(this) as JvmSnapshotBasedIncrementalCompilationOptions
    }

internal inline fun <T> unwrapInvocationTargetException(action: () -> T): T {
    return try {
        action()
    } catch (e: InvocationTargetException) {
        throw e.cause ?: e
    }
}

