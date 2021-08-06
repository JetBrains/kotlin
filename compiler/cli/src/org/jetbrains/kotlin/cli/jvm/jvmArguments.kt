/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.getLibraryFromHome
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

fun CompilerConfiguration.setupJvmSpecificArguments(arguments: K2JVMCompilerArguments) {
    put(JVMConfigurationKeys.INCLUDE_RUNTIME, arguments.includeRuntime)
    put(JVMConfigurationKeys.NO_REFLECT, arguments.noReflect)

    putIfNotNull(JVMConfigurationKeys.FRIEND_PATHS, arguments.friendPaths?.asList())

    if (arguments.jvmTarget != null) {
        val jvmTarget = JvmTarget.fromString(arguments.jvmTarget!!)
        if (jvmTarget != null) {
            put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
            if (jvmTarget == JvmTarget.JVM_1_6 && !arguments.suppressDeprecatedJvmTargetWarning) {
                messageCollector.report(
                    STRONG_WARNING,
                    "JVM target 1.6 is deprecated and will be removed in a future release. Please migrate to JVM target 1.8 or above"
                )
            }
        } else {
            messageCollector.report(
                ERROR, "Unknown JVM target version: ${arguments.jvmTarget}\n" +
                        "Supported versions: ${JvmTarget.values().joinToString { it.description }}"
            )
        }
    }

    val jvmTarget = get(JVMConfigurationKeys.JVM_TARGET) ?: JvmTarget.DEFAULT
    if (jvmTarget.majorVersion < JvmTarget.JVM_1_8.majorVersion) {
        val jvmDefaultMode = languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)
        if (jvmDefaultMode.forAllMethodsWithBody) {
            messageCollector.report(
                ERROR,
                "'-Xjvm-default=${jvmDefaultMode.description}' is only supported since JVM target 1.8. Recompile with '-jvm-target 1.8'"
            )
        }
    }

    val stringConcat = arguments.stringConcat
    if (stringConcat != null) {
        val runtimeStringConcat = JvmStringConcat.fromString(stringConcat)
        if (runtimeStringConcat != null) {
            put(JVMConfigurationKeys.STRING_CONCAT, runtimeStringConcat)
            if (jvmTarget.majorVersion < JvmTarget.JVM_9.majorVersion && runtimeStringConcat != JvmStringConcat.INLINE) {
                messageCollector.report(
                    WARNING,
                    "`-Xstring-concat=$stringConcat` does nothing with JVM target `${jvmTarget.description}`."
                )
            }
        } else {
            messageCollector.report(
                ERROR, "Unknown `-Xstring-concat` mode: $stringConcat\n" +
                        "Supported modes: ${JvmStringConcat.values().joinToString { it.description }}"
            )
        }
    }

    handleClosureGenerationSchemeArgument("-Xsam-conversions", arguments.samConversions, JVMConfigurationKeys.SAM_CONVERSIONS, jvmTarget)
    handleClosureGenerationSchemeArgument("-Xlambdas", arguments.lambdas, JVMConfigurationKeys.LAMBDAS, jvmTarget)

    addAll(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, arguments.additionalJavaModules?.asList())
}

private fun CompilerConfiguration.handleClosureGenerationSchemeArgument(
    flag: String,
    value: String?,
    key: CompilerConfigurationKey<JvmClosureGenerationScheme>,
    jvmTarget: JvmTarget
) {
    if (value != null) {
        val parsedValue = JvmClosureGenerationScheme.fromString(value)
        if (parsedValue != null) {
            put(key, parsedValue)
            if (jvmTarget < parsedValue.minJvmTarget) {
                messageCollector.report(
                    WARNING,
                    "`$flag=$value` requires JVM target at least " +
                            "${parsedValue.minJvmTarget.description} and is ignored."
                )
            }
        } else {
            messageCollector.report(
                ERROR,
                "Unknown `$flag` argument: ${value}\n." +
                        "Supported arguments: ${JvmClosureGenerationScheme.values().joinToString { it.description }}"
            )
        }
    }
}

fun CompilerConfiguration.configureJdkHome(arguments: K2JVMCompilerArguments): Boolean {
    if (arguments.noJdk) {
        put(JVMConfigurationKeys.NO_JDK, true)

        if (arguments.jdkHome != null) {
            messageCollector.report(STRONG_WARNING, "The '-jdk-home' option is ignored because '-no-jdk' is specified")
        }
        return true
    }

    if (arguments.jdkHome != null) {
        val jdkHome = File(arguments.jdkHome!!)
        if (!jdkHome.exists()) {
            messageCollector.report(ERROR, "JDK home directory does not exist: $jdkHome")
            return false
        }

        messageCollector.report(LOGGING, "Using JDK home directory $jdkHome")

        put(JVMConfigurationKeys.JDK_HOME, jdkHome)
    }

    return true
}

fun CompilerConfiguration.configureExplicitContentRoots(arguments: K2JVMCompilerArguments) {
    for (modularRoot in arguments.javaModulePath?.split(File.pathSeparatorChar).orEmpty()) {
        add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(File(modularRoot)))
    }

    if (arguments.buildFile != null) {
        // In the .xml compilation mode, all content roots except module path will be loaded from the .xml build file.
        return
    }

    for (path in arguments.classpath?.split(File.pathSeparatorChar).orEmpty()) {
        add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(path)))
    }
}

fun CompilerConfiguration.configureStandardLibs(paths: KotlinPaths?, arguments: K2JVMCompilerArguments) {
    val isModularJava = isModularJava()

    fun addRoot(moduleName: String, libraryName: String, getLibrary: (KotlinPaths) -> File, noLibraryArgument: String) {
        addModularRootIfNotNull(
            isModularJava, moduleName,
            getLibraryFromHome(paths, getLibrary, libraryName, messageCollector, noLibraryArgument)
        )
    }

    if (!arguments.noStdlib) {
        addRoot("kotlin.stdlib", PathUtil.KOTLIN_JAVA_STDLIB_JAR, KotlinPaths::stdlibPath, "'-no-stdlib'")
        addRoot("kotlin.script.runtime", PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR, KotlinPaths::scriptRuntimePath, "'-no-stdlib'")
    }
    // "-no-stdlib" implies "-no-reflect": otherwise we would be able to transitively read stdlib classes through kotlin-reflect,
    // which is likely not what user wants since s/he manually provided "-no-stdlib"
    if (!arguments.noReflect && !arguments.noStdlib) {
        addRoot("kotlin.reflect", PathUtil.KOTLIN_JAVA_REFLECT_JAR, { it.reflectPath }, "'-no-reflect' or '-no-stdlib'")
    }
}

fun CompilerConfiguration.isModularJava(): Boolean {
    return get(JVMConfigurationKeys.JDK_HOME)?.let {
        CoreJrtFileSystem.isModularJdk(it)
    } ?: false
}

fun CompilerConfiguration.addModularRootIfNotNull(isModularJava: Boolean, moduleName: String, file: File?) {
    when {
        file == null -> {
        }
        isModularJava -> {
            add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
            add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, moduleName)
        }
        else -> add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(file))
    }
}

fun KotlinCoreEnvironment.registerJavacIfNeeded(
    arguments: K2JVMCompilerArguments
): Boolean {
    if (arguments.useJavac) {
        configuration.put(JVMConfigurationKeys.USE_JAVAC, true)
        if (arguments.compileJava) {
            configuration.put(JVMConfigurationKeys.COMPILE_JAVA, true)
        }
        return registerJavac(arguments = arguments.javacArguments)
    }

    return true
}

fun CompilerConfiguration.configureAdvancedJvmOptions(arguments: K2JVMCompilerArguments) {

    put(JVMConfigurationKeys.PARAMETERS_METADATA, arguments.javaParameters)

    // TODO: ignore previous configuration value when we do not need old backend in scripting by default
    val useOldBackend = arguments.useOldBackend || (!arguments.useIR && get(JVMConfigurationKeys.IR) == false)
    val useIR = arguments.useFir ||
            if (languageVersionSettings.supportsFeature(LanguageFeature.JvmIrEnabledByDefault)) {
                !useOldBackend
            } else {
                arguments.useIR && !useOldBackend
            }

    if (arguments.useOldBackend) {
        messageCollector.report(
            STRONG_WARNING,
            "-Xuse-old-backend is deprecated and will be removed in a future release"
        )
        if (arguments.useIR) {
            messageCollector.report(
                STRONG_WARNING,
                "Both -Xuse-ir and -Xuse-old-backend are passed. This is an inconsistent configuration. " +
                        "The compiler will use the ${if (useIR) "JVM IR" else "old JVM"} backend"
            )
        }
    }

    messageCollector.report(LOGGING, "Using ${if (useIR) "JVM IR" else "old JVM"} backend")

    put(JVMConfigurationKeys.IR, useIR)

    val abiStability = JvmAbiStability.fromStringOrNull(arguments.abiStability)
    if (arguments.abiStability != null) {
        if (abiStability == null) {
            messageCollector.report(
                ERROR,
                "Unknown ABI stability mode: ${arguments.abiStability}, supported modes: ${JvmAbiStability.values().map { it.description }}"
            )
        } else if (!useIR && abiStability == JvmAbiStability.UNSTABLE) {
            messageCollector.report(ERROR, "-Xabi-stability=unstable is not supported in the old JVM backend")
        } else {
            put(JVMConfigurationKeys.ABI_STABILITY, abiStability)
        }
    }

    put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, arguments.doNotClearBindingContext)
    put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, arguments.noCallAssertions)
    put(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS, arguments.noReceiverAssertions)
    put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, arguments.noParamAssertions)
    put(
        JVMConfigurationKeys.NO_EXCEPTION_ON_EXPLICIT_EQUALS_FOR_BOXED_NULL,
        arguments.noExceptionOnExplicitEqualsForBoxedNull
    )
    put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, arguments.noOptimize)
    put(JVMConfigurationKeys.EMIT_JVM_TYPE_ANNOTATIONS, arguments.emitJvmTypeAnnotations)
    put(JVMConfigurationKeys.NO_OPTIMIZED_CALLABLE_REFERENCES, arguments.noOptimizedCallableReferences)
    put(JVMConfigurationKeys.NO_KOTLIN_NOTHING_VALUE_EXCEPTION, arguments.noKotlinNothingValueException)
    put(JVMConfigurationKeys.NO_RESET_JAR_TIMESTAMPS, arguments.noResetJarTimestamps)
    put(JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS, arguments.noUnifiedNullChecks)

    put(JVMConfigurationKeys.SERIALIZE_IR, arguments.serializeIr)

    put(JVMConfigurationKeys.VALIDATE_IR, arguments.validateIr)
    put(JVMConfigurationKeys.VALIDATE_BYTECODE, arguments.validateBytecode)

    if (!JVMConstructorCallNormalizationMode.isSupportedValue(arguments.constructorCallNormalizationMode)) {
        messageCollector.report(
            ERROR,
            "Unknown constructor call normalization mode: ${arguments.constructorCallNormalizationMode}, " +
                    "supported modes: ${JVMConstructorCallNormalizationMode.values().map { it.description }}"
        )
    }

    val constructorCallNormalizationMode = JVMConstructorCallNormalizationMode.fromStringOrNull(arguments.constructorCallNormalizationMode)
    if (constructorCallNormalizationMode != null) {
        put(JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE, constructorCallNormalizationMode)
    }

    val assertionsMode =
        JVMAssertionsMode.fromStringOrNull(arguments.assertionsMode)
    if (assertionsMode == null) {
        messageCollector.report(
            ERROR,
            "Unknown assertions mode: ${arguments.assertionsMode}, supported modes: ${JVMAssertionsMode.values().map { it.description }}"
        )
    }
    put(JVMConfigurationKeys.ASSERTIONS_MODE, assertionsMode ?: JVMAssertionsMode.DEFAULT)

    put(JVMConfigurationKeys.USE_TYPE_TABLE, arguments.useTypeTable)
    put(JVMConfigurationKeys.SKIP_RUNTIME_VERSION_CHECK, arguments.skipRuntimeVersionCheck)
    put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, arguments.useOldClassFilesReading)
    put(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM, arguments.useFastJarFileSystem)

    if (arguments.useOldClassFilesReading) {
        messageCollector.report(INFO, "Using the old java class files reading implementation")
    }

    if (arguments.useFastJarFileSystem) {
        messageCollector.report(INFO, "Using fast Jar FS implementation")
    }

    put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
    put(JVMConfigurationKeys.USE_SINGLE_MODULE, arguments.singleModule)
    put(JVMConfigurationKeys.USE_OLD_SPILLED_VAR_TYPE_ANALYSIS, arguments.useOldSpilledVarTypeAnalysis)
    put(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME, arguments.useOldInlineClassesManglingScheme)
    put(JVMConfigurationKeys.ENABLE_JVM_PREVIEW, arguments.enableJvmPreview)

    if (arguments.enableJvmPreview) {
        messageCollector.report(INFO, "Using preview Java language features")
    }

    arguments.declarationsOutputPath?.let { put(JVMConfigurationKeys.DECLARATIONS_JSON_PATH, it) }

    val nThreadsRaw = arguments.parallelBackendThreads.toIntOrNull() ?: 1
    val nThreads = if (nThreadsRaw == 0) Runtime.getRuntime().availableProcessors() else nThreadsRaw
    put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, nThreads)
}

fun CompilerConfiguration.configureKlibPaths(arguments: K2JVMCompilerArguments) {
    val libraries = arguments.klibLibraries ?: return
    assert(arguments.useIR && !arguments.useOldBackend) { "Klib libraries can only be used with IR backend" }
    put(JVMConfigurationKeys.KLIB_PATHS, libraries.split(File.pathSeparator.toRegex()).filterNot(String::isEmpty))
}

private val CompilerConfiguration.messageCollector: MessageCollector
    get() = getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
