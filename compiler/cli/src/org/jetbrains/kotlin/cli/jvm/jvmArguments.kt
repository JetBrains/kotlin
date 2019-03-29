/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.getLibraryFromHome
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

fun CompilerConfiguration.setupJvmSpecificArguments(arguments: K2JVMCompilerArguments) {

    val messageCollector = getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    put(JVMConfigurationKeys.INCLUDE_RUNTIME, arguments.includeRuntime)

    putIfNotNull(JVMConfigurationKeys.FRIEND_PATHS, arguments.friendPaths?.asList())

    if (arguments.jvmTarget != null) {
        val jvmTarget = JvmTarget.fromString(arguments.jvmTarget!!)
        if (jvmTarget != null) {
            put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
        } else {
            messageCollector.report(
                ERROR, "Unknown JVM target version: ${arguments.jvmTarget}\n" +
                        "Supported versions: ${JvmTarget.values().joinToString { it.description }}"
            )
        }
    }

    addAll(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, arguments.additionalJavaModules?.asList())
}

fun CompilerConfiguration.configureJdkHome(arguments: K2JVMCompilerArguments): Boolean {

    val messageCollector = getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    if (arguments.noJdk) {
        put(JVMConfigurationKeys.NO_JDK, true)

        if (arguments.jdkHome != null) {
            messageCollector.report(STRONG_WARNING, "The '-jdk-home' option is ignored because '-no-jdk' is specified")
        }
        return true
    }

    if (arguments.jdkHome != null) {
        val jdkHome = File(arguments.jdkHome)
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
    val messageCollector = getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
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

    put(JVMConfigurationKeys.IR, arguments.useIR)
    put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, arguments.noCallAssertions)
    put(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS, arguments.noReceiverAssertions)
    put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, arguments.noParamAssertions)
    put(
        JVMConfigurationKeys.NO_EXCEPTION_ON_EXPLICIT_EQUALS_FOR_BOXED_NULL,
        arguments.noExceptionOnExplicitEqualsForBoxedNull
    )
    put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, arguments.noOptimize)

    if (!JVMConstructorCallNormalizationMode.isSupportedValue(arguments.constructorCallNormalizationMode)) {
        getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
            ERROR,
            "Unknown constructor call normalization mode: ${arguments.constructorCallNormalizationMode}, " +
                    "supported modes: ${JVMConstructorCallNormalizationMode.values().map { it.description }}"
        )
    }

    val constructorCallNormalizationMode =
        JVMConstructorCallNormalizationMode.fromStringOrNull(arguments.constructorCallNormalizationMode)
    if (constructorCallNormalizationMode != null) {
        put(
            JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE,
            constructorCallNormalizationMode
        )
    }

    val assertionsMode =
        JVMAssertionsMode.fromStringOrNull(arguments.assertionsMode)
    if (assertionsMode == null) {
        getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
            ERROR,
            "Unknown assertions mode: ${arguments.assertionsMode}, " +
                    "supported modes: ${JVMAssertionsMode.values().map { it.description }}"
        )
    }
    put(
        JVMConfigurationKeys.ASSERTIONS_MODE,
        assertionsMode ?: JVMAssertionsMode.DEFAULT
    )

    put(JVMConfigurationKeys.USE_TYPE_TABLE, arguments.useTypeTable)
    put(JVMConfigurationKeys.SKIP_RUNTIME_VERSION_CHECK, arguments.skipRuntimeVersionCheck)
    put(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING, !arguments.useOldClassFilesReading)

    if (arguments.useOldClassFilesReading) {
        getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            .report(INFO, "Using the old java class files reading implementation")
    }

    put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
    put(JVMConfigurationKeys.USE_SINGLE_MODULE, arguments.singleModule)

    arguments.declarationsOutputPath?.let { put(JVMConfigurationKeys.DECLARATIONS_JSON_PATH, it) }
}
