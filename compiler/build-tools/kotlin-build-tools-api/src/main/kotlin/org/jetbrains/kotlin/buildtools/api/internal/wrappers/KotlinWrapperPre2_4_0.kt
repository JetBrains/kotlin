/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.*
import org.jetbrains.kotlin.buildtools.api.arguments.enums.*
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * A wrapper class for `KotlinToolchains` to accommodate functionality
 * changes and compatibility adjustments for versions pre Kotlin 2.4.0.
 *
 * Delegates the majority of functionality to the `base` implementation,
 * while selectively overriding methods to either introduce new behavior
 * or adapt existing operations.
 *
 * @param base The base implementation of `KotlinToolchains` to wrap.
 */
@Suppress("ClassName")
internal class KotlinWrapperPre2_4_0(
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

    class BuildSessionWrapper(override val kotlinToolchains: KotlinWrapperPre2_4_0, private val base: KotlinToolchains.BuildSession) :
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

        override fun discoverScriptExtensionsOperationBuilder(classpath: List<Path>): DiscoverScriptExtensionsOperation.Builder {
            throw UnsupportedOperationException("DiscoverScriptExtensionsOperation is available from Kotlin compiler version 2.4.0")
        }
    }

    private class JvmCompilationOperationWrapper(
        private val base: JvmCompilationOperation,
    ) : JvmCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base), CancellableBuildOperation<CompilationResult> {
        override val compilerArguments: JvmCompilerArguments = JvmCompilerArgumentsWrapper(base.compilerArguments)

        override fun <V> get(key: BaseCompilationOperation.Option<V>): V {
            val jvmOption = JvmCompilationOperation.Option<V>(key.id, key.availableSinceVersion)
            return base[jvmOption]
        }
    }

    private class JvmCompilationOperationBuilderWrapper(
        private val base: JvmCompilationOperation.Builder,
    ) : JvmCompilationOperation.Builder by base {
        override val compilerArguments: JvmCompilerArguments.Builder = JvmCompilerArgumentsBuilderWrapper(base.compilerArguments)

        override fun snapshotBasedIcConfigurationBuilder(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
        ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
            return snapshotBasedIcConfigurationBuilder(
                workingDirectory,
                sourcesChanges,
                dependenciesSnapshotFiles,
                workingDirectory.resolve("shrunk-classpath-snapshot.bin"),
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
            return JvmSnapshotBasedIncrementalCompilationConfigurationBuilderWrapper(
                base.snapshotBasedIcConfigurationBuilder(
                    workingDirectory,
                    sourcesChanges,
                    dependenciesSnapshotFiles,
                    shrunkClasspathSnapshot,
                )
            )
        }

        override fun <V> set(key: BaseCompilationOperation.Option<V>, value: V) {
            val jvmOption = JvmCompilationOperation.Option<V>(key.id, key.availableSinceVersion)
            base[jvmOption] = value
        }

        override fun <V> get(key: BaseCompilationOperation.Option<V>): V {
            val jvmOption = JvmCompilationOperation.Option<V>(key.id, key.availableSinceVersion)
            return base[jvmOption]
        }

        override fun build(): JvmCompilationOperation {
            return JvmCompilationOperationWrapper(base.build())
        }

        @Suppress("DEPRECATION_ERROR")
        private class JvmSnapshotBasedIncrementalCompilationConfigurationWrapper(
            val base: JvmSnapshotBasedIncrementalCompilationConfiguration,
        ) : JvmSnapshotBasedIncrementalCompilationConfiguration(
            base.workingDirectory,
            base.sourcesChanges,
            base.dependenciesSnapshotFiles,
            base.shrunkClasspathSnapshot,
            base.options
        ) {

            override fun toBuilder(): Builder = JvmSnapshotBasedIncrementalCompilationConfigurationBuilderWrapper(base.toBuilder())

            override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
                val oldOption = Option<V>(key.id, key.availableSinceVersion)
                return base[oldOption]
            }

            override fun <V> get(key: Option<V>): V {
                return base[key]
            }
        }

        private class JvmSnapshotBasedIncrementalCompilationConfigurationBuilderWrapper(
            val base: JvmSnapshotBasedIncrementalCompilationConfiguration.Builder,
        ) : JvmSnapshotBasedIncrementalCompilationConfiguration.Builder by base {
            override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
                val oldOption = JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>(key.id, key.availableSinceVersion)
                return base[oldOption]
            }

            override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
                val oldOption = JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>(key.id, key.availableSinceVersion)
                base[oldOption] = value
            }

            override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration {
                return JvmSnapshotBasedIncrementalCompilationConfigurationWrapper(base.build())
            }
        }
    }

    private interface CommonArgumentAccessor {
        operator fun <V> get(key: CommonCompilerArguments.CommonCompilerArgument<V>): V
        operator fun <V> set(key: CommonCompilerArguments.CommonCompilerArgument<V>, value: V)
    }

    private interface JvmArgumentAccessor : CommonArgumentAccessor {
        operator fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V
        operator fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V)
    }

    private class JvmCompilerArgumentsWrapper(
        private val base: JvmCompilerArguments,
    ) : JvmCompilerArguments by base {

        private val interceptor = JvmArgumentInterceptor(object : JvmArgumentAccessor {
            override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>) = base[key]
            override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) {
                base[key] = value
            }

            override fun <V> get(key: CommonCompilerArguments.CommonCompilerArgument<V>): V = base[key]
            override fun <V> set(key: CommonCompilerArguments.CommonCompilerArgument<V>, value: V) {
                base[key] = value
            }
        })

        override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = interceptor[key]

        override fun <V> get(key: CommonCompilerArguments.CommonCompilerArgument<V>): V = interceptor[key]
    }

    internal class JvmCompilerArgumentsBuilderWrapper(
        private val base: JvmCompilerArguments.Builder,
    ) : JvmCompilerArguments.Builder by base {

        private val interceptor = JvmArgumentInterceptor(object : JvmArgumentAccessor {
            override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>) = base[key]
            override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) {
                base[key] = value
            }

            override fun <V> get(key: CommonCompilerArguments.CommonCompilerArgument<V>): V = base[key]
            override fun <V> set(key: CommonCompilerArguments.CommonCompilerArgument<V>, value: V) {
                base[key] = value
            }
        })

        override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = interceptor[key]
        override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) = interceptor.set(key, value)

        override fun <V> get(key: CommonCompilerArguments.CommonCompilerArgument<V>): V = interceptor[key]
        override fun <V> set(key: CommonCompilerArguments.CommonCompilerArgument<V>, value: V) = interceptor.set(key, value)
    }

    @OptIn(ExperimentalCompilerArgument::class)
    private class JvmArgumentInterceptor(
        private val delegate: JvmArgumentAccessor,
    ) : JvmArgumentAccessor by delegate {

        @Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")
        override fun <V> get(key: CommonCompilerArguments.CommonCompilerArgument<V>): V {
            return when (key) {
                CommonCompilerArguments.KOTLIN_HOME,
                CommonCompilerArguments.X_DUMP_DIRECTORY,
                CommonCompilerArguments.X_DUMP_PERF,
                    -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    Path(stringValue) as V
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP,
                CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE,
                CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER,
                CommonCompilerArguments.X_PHASES_TO_VALIDATE,
                CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE,
                CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER,
                CommonCompilerArguments.X_DISABLE_PHASES,
                CommonCompilerArguments.X_VERBOSE_PHASES,
                CommonCompilerArguments.X_SUPPRESS_WARNING,
                CommonCompilerArguments.OPT_IN,
                    -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_ANNOTATION_DEFAULT_TARGET -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    AnnotationDefaultTargetMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $stringValue")
                }

                CommonCompilerArguments.X_NAME_BASED_DESTRUCTURING -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    NameBasedDestructuringMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xname-based-destructuring value: $stringValue")
                }

                CommonCompilerArguments.X_VERIFY_IR -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    VerifyIrMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $stringValue")
                }

                CommonCompilerArguments.X_WARNING_LEVEL -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<WarningLevel>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.map {
                        val parts = it.split(":", limit = 2)
                        if (parts.size != 2) {
                            throw CompilerArgumentsParseException("Invalid -Xwarning-level format: $it")
                        }
                        val severity = WarningLevel.Severity.values().firstOrNull { entry -> entry.stringValue == parts[1] }
                            ?: throw CompilerArgumentsParseException("Unknown -Xwarning-level level: $it")
                        WarningLevel(parts[0], severity)
                    } as V
                }

                else -> delegate[key]
            }
        }

        override fun <V> set(
            key: CommonCompilerArguments.CommonCompilerArgument<V>,
            value: V,
        ) {
            when (key) {
                CommonCompilerArguments.KOTLIN_HOME,
                CommonCompilerArguments.X_DUMP_DIRECTORY,
                CommonCompilerArguments.X_DUMP_PERF,
                    -> {
                    val pathValue = value as Path?
                    val stringValue = pathValue?.toFile()?.absolutePath
                    val stringKey = CommonCompilerArguments.CommonCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP,
                CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE,
                CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER,
                CommonCompilerArguments.X_PHASES_TO_VALIDATE,
                CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE,
                CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER,
                CommonCompilerArguments.X_DISABLE_PHASES,
                CommonCompilerArguments.X_VERBOSE_PHASES,
                CommonCompilerArguments.X_SUPPRESS_WARNING,
                CommonCompilerArguments.OPT_IN,
                    -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String> = value as List<String>
                    val arrayValue = listValue.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_ANNOTATION_DEFAULT_TARGET -> {
                    val mode = value as AnnotationDefaultTargetMode?
                    val stringValue = mode?.stringValue
                    val stringKey = CommonCompilerArguments.CommonCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                CommonCompilerArguments.X_NAME_BASED_DESTRUCTURING -> {
                    val mode = value as NameBasedDestructuringMode?
                    val stringValue = mode?.stringValue
                    val stringKey = CommonCompilerArguments.CommonCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                CommonCompilerArguments.X_VERIFY_IR -> {
                    val mode = value as VerifyIrMode?
                    val stringValue = mode?.stringValue
                    val stringKey = CommonCompilerArguments.CommonCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                CommonCompilerArguments.X_WARNING_LEVEL -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue = value as List<WarningLevel>
                    val arrayValue = listValue.map { item -> "${item.warningName}:${item.severity.stringValue}" }.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)
                    delegate[arrayKey] = arrayValue
                }

                else -> {
                    delegate[key] = value
                }
            }
        }

        @Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
        override fun <V> get(key: JvmCompilerArguments.JvmCompilerArgument<V>): V {
            return when (key) {
                JvmCompilerArguments.X_PROFILE -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    val parts = stringValue.split(File.pathSeparator)
                    if (parts.size != 3) {
                        throw CompilerArgumentsParseException("Invalid -Xprofile format: $stringValue")
                    }

                    ProfileCompilerCommand(Path(parts[0]), parts[1], Path(parts[2])) as V
                }

                JvmCompilerArguments.JDK_HOME -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    Path(stringValue) as V
                }

                JvmCompilerArguments.JVM_DEFAULT -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    JvmDefaultMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $stringValue")
                }

                JvmCompilerArguments.X_ABI_STABILITY -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    AbiStabilityMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $stringValue")
                }

                JvmCompilerArguments.X_ASSERTIONS -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    AssertionsMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $stringValue")
                }

                JvmCompilerArguments.X_JSPECIFY_ANNOTATIONS -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    JspecifyAnnotationsMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $stringValue")
                }

                JvmCompilerArguments.X_LAMBDAS -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    LambdasMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $stringValue")
                }

                JvmCompilerArguments.X_SAM_CONVERSIONS -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    SamConversionsMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $stringValue")
                }

                JvmCompilerArguments.X_STRING_CONCAT -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    StringConcatMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $stringValue")
                }

                JvmCompilerArguments.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    CompatqualAnnotationsMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $stringValue")
                }

                JvmCompilerArguments.X_WHEN_EXPRESSIONS -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    WhenExpressionsMode.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $stringValue")
                }

                JvmCompilerArguments.X_JDK_RELEASE -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    JdkRelease.values().firstOrNull { it.stringValue == stringValue } as V
                        ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $stringValue")
                }

                JvmCompilerArguments.CLASSPATH,
                JvmCompilerArguments.X_KLIB,
                JvmCompilerArguments.X_MODULE_PATH,
                    -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    stringValue.split(File.pathSeparator).map { Path(it) } as V
                }

                JvmCompilerArguments.X_FRIEND_PATHS,
                JvmCompilerArguments.X_JAVA_SOURCE_ROOTS,
                    -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<Path>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.map { Path(it) } as V
                }

                JvmCompilerArguments.X_ADD_MODULES,
                JvmCompilerArguments.SCRIPT_TEMPLATES,
                JvmCompilerArguments.X_IGNORED_ANNOTATIONS_FOR_BRIDGES,
                JvmCompilerArguments.X_SCRIPT_RESOLVER_ENVIRONMENT,
                    -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                JvmCompilerArguments.X_NULLABILITY_ANNOTATIONS -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<NullabilityAnnotation>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.map {
                        val parts = it.split(":")
                        if (parts.size != 2) {
                            throw CompilerArgumentsParseException("Invalid -Xnullability-annotations format: $this")
                        }

                        val nullabilityAnnotationMode =
                            NullabilityAnnotation.Mode.values().firstOrNull { entry -> entry.stringValue == parts[1] }
                                ?: throw CompilerArgumentsParseException("Unknown -Xnullability-annotations mode: $it")
                        NullabilityAnnotation(parts[0].removePrefix("@"), nullabilityAnnotationMode)
                    } as V
                }

                JvmCompilerArguments.X_JSR305 -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<Jsr305>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.map { fullEntry ->
                        fun jsr305mode(mode: String) = Jsr305.Mode.values().firstOrNull { entry -> entry.stringValue == mode }
                            ?: throw CompilerArgumentsParseException("Unknown -Xjsr305 mode: $fullEntry")

                        val parts = fullEntry.split(":")
                        when (parts.size) {
                            1 -> Jsr305.Global(jsr305mode(parts[0]))
                            2 -> {
                                if (parts[0] == "under-migration") {
                                    Jsr305.UnderMigration(jsr305mode(parts[1]))
                                } else {
                                    Jsr305.SpecificAnnotation(parts[0].removePrefix("@"), jsr305mode(parts[1]))
                                }
                            }
                            else -> throw CompilerArgumentsParseException("Invalid -Xjsr305 format: $fullEntry")
                        }
                    } as V
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

                JvmCompilerArguments.JVM_DEFAULT -> {
                    val mode = value as JvmDefaultMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_ABI_STABILITY -> {
                    val mode = value as AbiStabilityMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_ASSERTIONS -> {
                    val mode = value as AssertionsMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_JSPECIFY_ANNOTATIONS -> {
                    val mode = value as JspecifyAnnotationsMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_LAMBDAS -> {
                    val mode = value as LambdasMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_SAM_CONVERSIONS -> {
                    val mode = value as SamConversionsMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_STRING_CONCAT -> {
                    val mode = value as StringConcatMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS -> {
                    val mode = value as CompatqualAnnotationsMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_WHEN_EXPRESSIONS -> {
                    val mode = value as WhenExpressionsMode?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_JDK_RELEASE -> {
                    val mode = value as JdkRelease?
                    val stringValue = mode?.stringValue
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.CLASSPATH,
                JvmCompilerArguments.X_KLIB,
                JvmCompilerArguments.X_MODULE_PATH,
                    -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue = value as List<Path>?
                    val stringValue =
                        listValue?.map { it.toFile().absolutePath }
                            ?.joinToString(File.pathSeparator)
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_FRIEND_PATHS,
                JvmCompilerArguments.X_JAVA_SOURCE_ROOTS,
                    -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue = value as List<Path>
                    val arrayValue = listValue.map { it.toFile().absolutePath }.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                JvmCompilerArguments.X_ADD_MODULES,
                JvmCompilerArguments.SCRIPT_TEMPLATES,
                JvmCompilerArguments.X_IGNORED_ANNOTATIONS_FOR_BRIDGES,
                JvmCompilerArguments.X_SCRIPT_RESOLVER_ENVIRONMENT,
                    -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue = value as List<String>
                    val arrayValue = listValue.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                JvmCompilerArguments.X_NULLABILITY_ANNOTATIONS -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<NullabilityAnnotation>? =
                        (value as? List<*>)?.takeIf { it.all { item -> item is NullabilityAnnotation } } as List<NullabilityAnnotation>?
                    val arrayValue = listValue?.map { item -> "${item.annotationFqName}:${item.mode.stringValue}" }?.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                JvmCompilerArguments.X_JSR305 -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<Jsr305>? =
                        (value as? List<*>)?.takeIf { it.all { item -> item is Jsr305 } } as List<Jsr305>?
                    val arrayValue = listValue?.map { item ->
                        when (item) {
                            is Jsr305.Global -> item.mode.stringValue
                            is Jsr305.UnderMigration -> "under-migration:${item.mode.stringValue}"
                            is Jsr305.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
                        }
                    }?.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                else -> delegate[key] = value
            }
        }
    }
}
