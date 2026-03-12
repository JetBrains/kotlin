/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.*
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
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
    ) : JvmCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base), CancellableBuildOperation<CompilationResult> {
        override val compilerArguments: JvmCompilerArguments = JvmCompilerArgumentsWrapper(base.compilerArguments)
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
            return base.snapshotBasedIcConfigurationBuilder(
                workingDirectory,
                sourcesChanges,
                dependenciesSnapshotFiles,
                workingDirectory.resolve("shrunk-classpath-snapshot.bin"),
            )
        }

        override fun build(): JvmCompilationOperation {
            return JvmCompilationOperationWrapper(base.build())
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

        @Deprecated("Compiler argument classes will become immutable in an upcoming release. Use a Builder instance to create and modify compiler arguments.")
        override fun <V> set(key: JvmCompilerArguments.JvmCompilerArgument<V>, value: V) = interceptor.set(key, value)

        override fun <V> get(key: CommonCompilerArguments.CommonCompilerArgument<V>): V = interceptor[key]

        @Deprecated("Compiler argument classes will become immutable in an upcoming release. Use a Builder instance to create and modify compiler arguments.")
        override fun <V> set(key: CommonCompilerArguments.CommonCompilerArgument<V>, value: V) = interceptor.set(key, value)
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
                CommonCompilerArguments.KOTLIN_HOME -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    Path(stringValue) as V
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_PHASES_TO_VALIDATE -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_DISABLE_PHASES -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_VERBOSE_PHASES -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.X_SUPPRESS_WARNING -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                CommonCompilerArguments.OPT_IN -> {
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

                else -> delegate[key]
            }
        }

        override fun <V> set(
            key: CommonCompilerArguments.CommonCompilerArgument<V>,
            value: V,
        ) {
            when (key) {
                CommonCompilerArguments.KOTLIN_HOME -> {
                    val pathValue = value as Path?
                    val stringValue = pathValue?.toFile()?.absolutePath
                    val stringKey = CommonCompilerArguments.CommonCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_PHASES_TO_VALIDATE -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_DISABLE_PHASES -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_VERBOSE_PHASES -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.X_SUPPRESS_WARNING -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = CommonCompilerArguments.CommonCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                CommonCompilerArguments.OPT_IN -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
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
                    require(parts.size == 3) { "Invalid async profiler settings format: $this" }

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

                JvmCompilerArguments.X_ADD_MODULES -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
                }

                JvmCompilerArguments.CLASSPATH -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    stringValue.split(File.pathSeparator).map { Path(it) } as V
                }

                JvmCompilerArguments.X_KLIB -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    stringValue.split(File.pathSeparator).map { Path(it) } as V
                }

                JvmCompilerArguments.X_MODULE_PATH -> {
                    if (delegate[key] == null) return null as V

                    val stringValue = delegate[key] as String
                    stringValue.split(File.pathSeparator).map { Path(it) } as V
                }

                JvmCompilerArguments.X_FRIEND_PATHS -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.map { Path(it) } as V
                }

                JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.map { Path(it) } as V
                }

                JvmCompilerArguments.SCRIPT_TEMPLATES -> {
                    @Suppress("SENSELESS_COMPARISON")
                    if (delegate[key] == null) return emptyList<String>() as V

                    val arrayValue = delegate[key] as Array<String>
                    arrayValue.toList() as V
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

                JvmCompilerArguments.X_ADD_MODULES -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                JvmCompilerArguments.CLASSPATH -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<Path>? = (value as? List<*>)?.takeIf { it.all { item -> item is Path } } as List<Path>?
                    val stringValue = listValue?.joinToString(File.pathSeparator) { it.toFile().absolutePath }
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_KLIB -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<Path>? = (value as? List<*>)?.takeIf { it.all { item -> item is Path } } as List<Path>?
                    val stringValue = listValue?.joinToString(File.pathSeparator) { it.toFile().absolutePath }
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_MODULE_PATH -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<Path>? = (value as? List<*>)?.takeIf { it.all { item -> item is Path } } as List<Path>?
                    val stringValue = listValue?.joinToString(File.pathSeparator) { it.toFile().absolutePath }
                    val stringKey = JvmCompilerArguments.JvmCompilerArgument<String?>(key.id, key.availableSinceVersion)

                    delegate[stringKey] = stringValue
                }

                JvmCompilerArguments.X_FRIEND_PATHS -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<Path>? = (value as? List<*>)?.takeIf { it.all { item -> item is Path } } as List<Path>?
                    val arrayValue = listValue?.map { it.toFile().absolutePath }?.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<Path>? = (value as? List<*>)?.takeIf { it.all { item -> item is Path } } as List<Path>?
                    val arrayValue = listValue?.map { it.toFile().absolutePath }?.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                JvmCompilerArguments.SCRIPT_TEMPLATES -> {
                    @Suppress("UNCHECKED_CAST")
                    val listValue: List<String>? = (value as? List<*>)?.takeIf { it.all { item -> item is String } } as List<String>?
                    val arrayValue = listValue?.toTypedArray()
                    val arrayKey = JvmCompilerArguments.JvmCompilerArgument<Array<String>?>(key.id, key.availableSinceVersion)

                    delegate[arrayKey] = arrayValue
                }

                else -> delegate[key] = value
            }
        }
    }
}
