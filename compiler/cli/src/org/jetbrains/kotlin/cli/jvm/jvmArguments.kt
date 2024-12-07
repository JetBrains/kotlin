/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

    val releaseTargetArg = arguments.jdkRelease
    val jvmTargetArg = arguments.jvmTarget
    if (releaseTargetArg != null) {
        val value =
            when (releaseTargetArg) {
                "1.6" -> 6
                "1.7" -> 7
                "1.8" -> 8
                else -> releaseTargetArg.toIntOrNull()
            }
        if (value == null || value < 6) {
            messageCollector.report(ERROR, "Unknown JDK release version: $releaseTargetArg")
        } else {
            //don't use release flag if it equals to compilation JDK version
            if (value != getJavaVersion() || arguments.jdkHome != null) {
                put(JVMConfigurationKeys.JDK_RELEASE, value)
            }
            if (jvmTargetArg != null && !isCompatibleJvmTargetAndRelease(jvmTargetArg, releaseTargetArg)) {
                val suggestion =
                    if (value < 8) "Please change the value of the 'jvm-target' option to 1.8"
                    else "Please remove the '-jvm-target' option"
                messageCollector.report(
                    ERROR,
                    "'-Xjdk-release=$releaseTargetArg' option conflicts with '-jvm-target $jvmTargetArg'. $suggestion"
                )
            }
        }
    }

    val jvmTargetValue = when (releaseTargetArg) {
        "6", "1.6", "7", "1.7" -> {
            if (jvmTargetArg == null) {
                messageCollector.report(
                    ERROR,
                    "'-Xjdk-release=$releaseTargetArg' option requires JVM target explicitly set to 1.8. " +
                            "Please specify the '-jvm-target' option"
                )
            }
            jvmTargetArg
        }
        "8" -> "1.8"
        null -> jvmTargetArg
        else -> releaseTargetArg
    }
    if (jvmTargetValue != null) {
        val jvmTarget = JvmTarget.fromString(jvmTargetValue)
        if (jvmTarget != null) {
            put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
            if (jvmTarget == JvmTarget.JVM_1_6) {
                messageCollector.report(
                    ERROR,
                    "JVM target 1.6 is no longer supported. Please migrate to JVM target 1.8 or above"
                )
            }
        } else {
            messageCollector.report(
                ERROR, "Unknown JVM target version: $jvmTargetValue\n" +
                        "Supported versions: ${JvmTarget.supportedValues().joinToString { it.description }}"
            )
        }
    }

    val jvmTarget = get(JVMConfigurationKeys.JVM_TARGET) ?: JvmTarget.DEFAULT

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
                        "Supported modes: ${JvmStringConcat.entries.joinToString { it.description }}"
            )
        }
    }

    handleClosureGenerationSchemeArgument("-Xsam-conversions", arguments.samConversions, JVMConfigurationKeys.SAM_CONVERSIONS)
    handleClosureGenerationSchemeArgument("-Xlambdas", arguments.lambdas, JVMConfigurationKeys.LAMBDAS)

    addAll(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, arguments.additionalJavaModules?.asList())
}

private fun isCompatibleJvmTargetAndRelease(jvmTarget: String, release: String): Boolean {
    if (jvmTarget == "1.8") {
        // This is needed to be able to compile stdlib with -jvm-target 1.8 and -Xjdk-release=1.6/1.7.
        return release in listOf("6", "1.6", "7", "1.7", "8", "1.8")
    }

    return jvmTarget == release
}

private fun CompilerConfiguration.handleClosureGenerationSchemeArgument(
    flag: String,
    value: String?,
    key: CompilerConfigurationKey<JvmClosureGenerationScheme>,
) {
    if (value == null) return

    val parsedValue = JvmClosureGenerationScheme.fromString(value)
    if (parsedValue != null) {
        put(key, parsedValue)
    } else {
        messageCollector.report(
            ERROR,
            "Unknown `$flag` argument: ${value}\n." +
                    "Supported arguments: ${JvmClosureGenerationScheme.entries.joinToString { it.description }}"
        )
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
    } else {
        configureJdkHomeFromSystemProperty()
    }

    return true
}

fun CompilerConfiguration.configureJdkHomeFromSystemProperty() {
    val javaHome = File(System.getProperty("java.home"))
    messageCollector.report(LOGGING, "Using JDK home inferred from java.home: $javaHome")
    put(JVMConfigurationKeys.JDK_HOME, javaHome)
}

fun CompilerConfiguration.configureJavaModulesContentRoots(arguments: K2JVMCompilerArguments) {
    for (modularRoot in arguments.javaModulePath?.split(File.pathSeparatorChar).orEmpty()) {
        add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(File(modularRoot)))
    }
}

fun CompilerConfiguration.configureContentRootsFromClassPath(arguments: K2JVMCompilerArguments) {
    for (path in arguments.classpath?.split(File.pathSeparatorChar).orEmpty()) {
        add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(path)))
    }
}

fun CompilerConfiguration.configureStandardLibs(paths: KotlinPaths?, arguments: K2JVMCompilerArguments) {
    configureStandardLibs(
        paths,
        KotlinPaths::stdlibPath,
        KotlinPaths::scriptRuntimePath,
        KotlinPaths::reflectPath,
        arguments
    )
}

fun <PathProvider : Any> CompilerConfiguration.configureStandardLibs(
    paths: PathProvider?,
    stdlibPath: (PathProvider) -> File,
    scriptRuntimePath: (PathProvider) -> File,
    reflectPath: (PathProvider) -> File,
    arguments: K2JVMCompilerArguments
) {
    val jdkRelease = get(JVMConfigurationKeys.JDK_RELEASE)
    val isModularJava = isModularJava() && (jdkRelease == null || jdkRelease >= 9)

    fun addRoot(moduleName: String, libraryName: String, getLibrary: (PathProvider) -> File, noLibraryArgument: String) {
        addModularRootIfNotNull(
            isModularJava, moduleName,
            getLibraryFromHome(paths, getLibrary, libraryName, messageCollector, noLibraryArgument)
        )
    }

    if (!arguments.noStdlib) {
        addRoot("kotlin.stdlib", PathUtil.KOTLIN_JAVA_STDLIB_JAR, stdlibPath, "'-no-stdlib'")
        addRoot("kotlin.script.runtime", PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR, scriptRuntimePath, "'-no-stdlib'")
    }
    // "-no-stdlib" implies "-no-reflect": otherwise we would be able to transitively read stdlib classes through kotlin-reflect,
    // which is likely not what user wants since s/he manually provided "-no-stdlib"
    if (!arguments.noReflect && !arguments.noStdlib) {
        addRoot("kotlin.reflect", PathUtil.KOTLIN_JAVA_REFLECT_JAR, reflectPath, "'-no-reflect' or '-no-stdlib'")
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

    val abiStability = JvmAbiStability.fromStringOrNull(arguments.abiStability)
    if (arguments.abiStability != null) {
        if (abiStability == null) {
            messageCollector.report(
                ERROR,
                "Unknown ABI stability mode: ${arguments.abiStability}, supported modes: ${JvmAbiStability.entries.map { it.description }}"
            )
        } else {
            put(JVMConfigurationKeys.ABI_STABILITY, abiStability)
        }
    }

    put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, arguments.doNotClearBindingContext)
    put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, arguments.noCallAssertions)
    put(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS, arguments.noReceiverAssertions)
    put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, arguments.noParamAssertions)
    put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, arguments.noOptimize)
    put(JVMConfigurationKeys.EMIT_JVM_TYPE_ANNOTATIONS, arguments.emitJvmTypeAnnotations)
    put(JVMConfigurationKeys.NO_RESET_JAR_TIMESTAMPS, arguments.noResetJarTimestamps)
    put(JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS, arguments.noUnifiedNullChecks)
    put(JVMConfigurationKeys.NO_SOURCE_DEBUG_EXTENSION, arguments.noSourceDebugExtension)

    put(JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.fromString(arguments.serializeIr))

    put(JVMConfigurationKeys.VALIDATE_BYTECODE, arguments.validateBytecode)

    put(JVMConfigurationKeys.LINK_VIA_SIGNATURES, arguments.linkViaSignatures)

    put(JVMConfigurationKeys.ENABLE_DEBUG_MODE, arguments.enableDebugMode)
    put(JVMConfigurationKeys.NO_NEW_JAVA_ANNOTATION_TARGETS, arguments.noNewJavaAnnotationTargets)
    put(JVMConfigurationKeys.OLD_INNER_CLASSES_LOGIC, arguments.oldInnerClassesLogic)
    put(JVMConfigurationKeys.ENABLE_IR_INLINER, arguments.enableIrInliner)
    put(JVMConfigurationKeys.USE_INLINE_SCOPES_NUMBERS, arguments.useInlineScopesNumbers)

    val assertionsMode =
        JVMAssertionsMode.fromStringOrNull(arguments.assertionsMode)
    if (assertionsMode == null) {
        messageCollector.report(
            ERROR,
            "Unknown assertions mode: ${arguments.assertionsMode}, supported modes: ${JVMAssertionsMode.entries.map { it.description }}"
        )
    }
    put(JVMConfigurationKeys.ASSERTIONS_MODE, assertionsMode ?: JVMAssertionsMode.DEFAULT)

    put(JVMConfigurationKeys.USE_TYPE_TABLE, arguments.useTypeTable)
    put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, arguments.useOldClassFilesReading)
    arguments.useFastJarFileSystem?.let { put(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM, it) }

    if (arguments.useOldClassFilesReading) {
        messageCollector.report(INFO, "Using the old java class files reading implementation")
    }

    put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
    put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)
    put(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME, arguments.useOldInlineClassesManglingScheme)
    put(JVMConfigurationKeys.ENABLE_JVM_PREVIEW, arguments.enableJvmPreview)

    if (arguments.enableJvmPreview) {
        messageCollector.report(INFO, "Using preview Java language features")
    }

    val nThreadsRaw = parseBackendThreads(arguments.backendThreads, messageCollector)
    val nThreads = if (nThreadsRaw == 0) Runtime.getRuntime().availableProcessors() else nThreadsRaw
    if (nThreads > 1) {
        messageCollector.report(LOGGING, "Running backend in parallel with $nThreads threads")
    }
    put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, nThreads)
}

private fun parseBackendThreads(stringValue: String, messageCollector: MessageCollector): Int {
    val value = stringValue.toIntOrNull()
    if (value == null) {
        messageCollector.report(ERROR, "Cannot parse -Xbackend-threads value: \"$stringValue\". Please use an integer number")
        return 1
    }
    if (value < 0) {
        messageCollector.report(ERROR, "-Xbackend-threads value cannot be negative")
        return 1
    }
    return value
}

fun CompilerConfiguration.configureKlibPaths(arguments: K2JVMCompilerArguments) {
    val libraries = arguments.klibLibraries ?: return
    put(JVMConfigurationKeys.KLIB_PATHS, libraries.split(File.pathSeparator.toRegex()).filterNot(String::isEmpty))
}

private fun getJavaVersion(): Int =
    System.getProperty("java.specification.version")?.substringAfter('.')?.toIntOrNull() ?: 6
