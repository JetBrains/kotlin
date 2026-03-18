/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.config

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import java.io.File
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module

object JVMConfigurationKeys {
    @JvmField
    val OUTPUT_DIRECTORY = CompilerConfigurationKey.create<File>("OUTPUT_DIRECTORY")

    @JvmField
    val OUTPUT_JAR = CompilerConfigurationKey.create<File>("OUTPUT_JAR")

    // Include runtime to the resulting .jar file.
    @JvmField
    val INCLUDE_RUNTIME = CompilerConfigurationKey.create<Boolean>("INCLUDE_RUNTIME")

    @JvmField
    val JDK_HOME = CompilerConfigurationKey.create<File>("JDK_HOME")

    @JvmField
    val NO_JDK = CompilerConfigurationKey.create<Boolean>("NO_JDK")

    @JvmField
    val DISABLE_STANDARD_SCRIPT_DEFINITION = CompilerConfigurationKey.create<Boolean>("DISABLE_STANDARD_SCRIPT_DEFINITION")

    @JvmField
    val DISABLE_CALL_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("DISABLE_CALL_ASSERTIONS")

    @JvmField
    val DISABLE_RECEIVER_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("DISABLE_RECEIVER_ASSERTIONS")

    @JvmField
    val DISABLE_PARAM_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("DISABLE_PARAM_ASSERTIONS")

    @JvmField
    val ASSERTIONS_MODE = CompilerConfigurationKey.create<JVMAssertionsMode>("ASSERTIONS_MODE")

    @JvmField
    val DISABLE_OPTIMIZATION = CompilerConfigurationKey.create<Boolean>("DISABLE_OPTIMIZATION")

    @JvmField
    val USE_TYPE_TABLE = CompilerConfigurationKey.create<Boolean>("USE_TYPE_TABLE")

    @JvmField
    val JVM_TARGET = CompilerConfigurationKey.create<JvmTarget>("JVM_TARGET")

    @JvmField
    val PARAMETERS_METADATA = CompilerConfigurationKey.create<Boolean>("PARAMETERS_METADATA")

    @JvmField
    val INCREMENTAL_COMPILATION_COMPONENTS = CompilerConfigurationKey.create<IncrementalCompilationComponents>("INCREMENTAL_COMPILATION_COMPONENTS")

    @JvmField
    val MODULE_XML_FILE = CompilerConfigurationKey.create<File>("MODULE_XML_FILE")

    @JvmField
    val MODULES = CompilerConfigurationKey.create<List<Module>>("MODULES")

    @JvmField
    val FRIEND_PATHS = CompilerConfigurationKey.create<List<String>>("FRIEND_PATHS")

    // Use a slower, PSI-based, class files reading implementation.
    @JvmField
    val USE_PSI_CLASS_FILES_READING = CompilerConfigurationKey.create<Boolean>("USE_PSI_CLASS_FILES_READING")

    @JvmField
    val USE_FAST_JAR_FILE_SYSTEM = CompilerConfigurationKey.create<Boolean>("USE_FAST_JAR_FILE_SYSTEM")

    @JvmField
    val USE_JAVAC = CompilerConfigurationKey.create<Boolean>("USE_JAVAC")

    @JvmField
    val COMPILE_JAVA = CompilerConfigurationKey.create<Boolean>("COMPILE_JAVA")

    @JvmField
    val ADDITIONAL_JAVA_MODULES = CompilerConfigurationKey.create<List<String>>("ADDITIONAL_JAVA_MODULES")

    @JvmField
    val EMIT_JVM_TYPE_ANNOTATIONS = CompilerConfigurationKey.create<Boolean>("EMIT_JVM_TYPE_ANNOTATIONS")

    @JvmField
    val STRING_CONCAT = CompilerConfigurationKey.create<JvmStringConcat>("STRING_CONCAT")

    @JvmField
    val JDK_RELEASE = CompilerConfigurationKey.create<Int>("JDK_RELEASE")

    @JvmField
    val SAM_CONVERSIONS = CompilerConfigurationKey.create<JvmClosureGenerationScheme>("SAM_CONVERSIONS")

    @JvmField
    val LAMBDAS = CompilerConfigurationKey.create<JvmClosureGenerationScheme>("LAMBDAS")

    // Paths to .klib libraries.
    @JvmField
    val KLIB_PATHS = CompilerConfigurationKey.create<List<String>>("KLIB_PATHS")

    // ABI stability of class files produced by JVM IR and/or FIR.
    @JvmField
    val ABI_STABILITY = CompilerConfigurationKey.create<JvmAbiStability>("ABI_STABILITY")

    // When using K1, do not clear BindingContext between psi2ir and lowerings.
    @JvmField
    val DO_NOT_CLEAR_BINDING_CONTEXT = CompilerConfigurationKey.create<Boolean>("DO_NOT_CLEAR_BINDING_CONTEXT")

    @JvmField
    val NO_RESET_JAR_TIMESTAMPS = CompilerConfigurationKey.create<Boolean>("NO_RESET_JAR_TIMESTAMPS")

    // Use pre-1.4 exception types in null checks instead of java.lang.NPE.
    @JvmField
    val NO_UNIFIED_NULL_CHECKS = CompilerConfigurationKey.create<Boolean>("NO_UNIFIED_NULL_CHECKS")

    // Do not generate @kotlin.jvm.internal.SourceDebugExtension annotation on a class with the copy of SMAP.
    @JvmField
    val NO_SOURCE_DEBUG_EXTENSION = CompilerConfigurationKey.create<Boolean>("NO_SOURCE_DEBUG_EXTENSION")

    // Use old, 1.4 version of inline classes mangling scheme.
    @JvmField
    val USE_OLD_INLINE_CLASSES_MANGLING_SCHEME = CompilerConfigurationKey.create<Boolean>("USE_OLD_INLINE_CLASSES_MANGLING_SCHEME")

    @JvmField
    val ENABLE_JVM_PREVIEW = CompilerConfigurationKey.create<Boolean>("ENABLE_JVM_PREVIEW")

    // Don't automatically include kotlin-reflect.jar into the output if the output is a jar.
    @JvmField
    val NO_REFLECT = CompilerConfigurationKey.create<Boolean>("NO_REFLECT")

    @JvmField
    val VALIDATE_BYTECODE = CompilerConfigurationKey.create<Boolean>("VALIDATE_BYTECODE")

    // Link JVM IR symbols via signatures, instead of by descriptors on the K1 frontend.
    @JvmField
    val LINK_VIA_SIGNATURES = CompilerConfigurationKey.create<Boolean>("LINK_VIA_SIGNATURES")

    @JvmField
    val ENABLE_DEBUG_MODE = CompilerConfigurationKey.create<Boolean>("ENABLE_DEBUG_MODE")

    // Mark compiled generated code in coroutines.
    @JvmField
    val ENHANCED_COROUTINES_DEBUGGING = CompilerConfigurationKey.create<Boolean>("ENHANCED_COROUTINES_DEBUGGING")

    // Do not generate Java 1.8+ targets for Kotlin annotation classes.
    @JvmField
    val NO_NEW_JAVA_ANNOTATION_TARGETS = CompilerConfigurationKey.create<Boolean>("NO_NEW_JAVA_ANNOTATION_TARGETS")

    // Use inline scopes numbers for inline marker variables.
    @JvmField
    val USE_INLINE_SCOPES_NUMBERS = CompilerConfigurationKey.create<Boolean>("USE_INLINE_SCOPES_NUMBERS")

    // Enable internal mode which causes FIR2IR to skip function bodies, used in KAPT.
    @JvmField
    val SKIP_BODIES = CompilerConfigurationKey.create<Boolean>("SKIP_BODIES")

    // Expression to evaluate in script mode.
    @JvmField
    val EXPRESSION_TO_EVALUATE = CompilerConfigurationKey.create<String>("EXPRESSION_TO_EVALUATE")

    // Specifies generation scheme for type-checking 'when' expressions.
    @JvmField
    val WHEN_GENERATION_SCHEME = CompilerConfigurationKey.create<JvmWhenGenerationScheme>("WHEN_GENERATION_SCHEME")

    // Annotations fqNames that shall be skipped while copying the annotations from the target to the bridge functions.
    @JvmField
    val IGNORED_ANNOTATIONS_FOR_BRIDGES = CompilerConfigurationKey.create<List<String>>("IGNORED_ANNOTATIONS_FOR_BRIDGES")

}

var CompilerConfiguration.outputDirectory: File?
    get() = get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
    set(value) { put(JVMConfigurationKeys.OUTPUT_DIRECTORY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.outputJar: File?
    get() = get(JVMConfigurationKeys.OUTPUT_JAR)
    set(value) { put(JVMConfigurationKeys.OUTPUT_JAR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.includeRuntime: Boolean
    get() = getBoolean(JVMConfigurationKeys.INCLUDE_RUNTIME)
    set(value) { put(JVMConfigurationKeys.INCLUDE_RUNTIME, value) }

var CompilerConfiguration.jdkHome: File?
    get() = get(JVMConfigurationKeys.JDK_HOME)
    set(value) { put(JVMConfigurationKeys.JDK_HOME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.noJdk: Boolean
    get() = getBoolean(JVMConfigurationKeys.NO_JDK)
    set(value) { put(JVMConfigurationKeys.NO_JDK, value) }

var CompilerConfiguration.disableStandardScriptDefinition: Boolean
    get() = getBoolean(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION)
    set(value) { put(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION, value) }

var CompilerConfiguration.disableCallAssertions: Boolean
    get() = getBoolean(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS)
    set(value) { put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, value) }

var CompilerConfiguration.disableReceiverAssertions: Boolean
    get() = getBoolean(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS)
    set(value) { put(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS, value) }

var CompilerConfiguration.disableParamAssertions: Boolean
    get() = getBoolean(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS)
    set(value) { put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, value) }

var CompilerConfiguration.assertionsMode: JVMAssertionsMode?
    get() = get(JVMConfigurationKeys.ASSERTIONS_MODE)
    set(value) { put(JVMConfigurationKeys.ASSERTIONS_MODE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.disableOptimization: Boolean
    get() = getBoolean(JVMConfigurationKeys.DISABLE_OPTIMIZATION)
    set(value) { put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, value) }

var CompilerConfiguration.useTypeTable: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_TYPE_TABLE)
    set(value) { put(JVMConfigurationKeys.USE_TYPE_TABLE, value) }

var CompilerConfiguration.jvmTarget: JvmTarget?
    get() = get(JVMConfigurationKeys.JVM_TARGET)
    set(value) { put(JVMConfigurationKeys.JVM_TARGET, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.parametersMetadata: Boolean
    get() = getBoolean(JVMConfigurationKeys.PARAMETERS_METADATA)
    set(value) { put(JVMConfigurationKeys.PARAMETERS_METADATA, value) }

var CompilerConfiguration.incrementalCompilationComponents: IncrementalCompilationComponents?
    get() = get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
    set(value) { putIfNotNull(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS, value) }

var CompilerConfiguration.moduleXmlFile: File?
    get() = get(JVMConfigurationKeys.MODULE_XML_FILE)
    set(value) { put(JVMConfigurationKeys.MODULE_XML_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.modules: List<Module>
    get() = getList(JVMConfigurationKeys.MODULES)
    set(value) { put(JVMConfigurationKeys.MODULES, value) }

var CompilerConfiguration.friendPaths: List<String>
    get() = getList(JVMConfigurationKeys.FRIEND_PATHS)
    set(value) { put(JVMConfigurationKeys.FRIEND_PATHS, value) }

var CompilerConfiguration.usePsiClassFilesReading: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING)
    set(value) { put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, value) }

var CompilerConfiguration.useFastJarFileSystem: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM)
    set(value) { put(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM, value) }

var CompilerConfiguration.useJavac: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_JAVAC)
    set(value) { put(JVMConfigurationKeys.USE_JAVAC, value) }

var CompilerConfiguration.compileJava: Boolean
    get() = getBoolean(JVMConfigurationKeys.COMPILE_JAVA)
    set(value) { put(JVMConfigurationKeys.COMPILE_JAVA, value) }

var CompilerConfiguration.additionalJavaModules: List<String>
    get() = getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES)
    set(value) { put(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, value) }

var CompilerConfiguration.emitJvmTypeAnnotations: Boolean
    get() = getBoolean(JVMConfigurationKeys.EMIT_JVM_TYPE_ANNOTATIONS)
    set(value) { put(JVMConfigurationKeys.EMIT_JVM_TYPE_ANNOTATIONS, value) }

var CompilerConfiguration.stringConcat: JvmStringConcat?
    get() = get(JVMConfigurationKeys.STRING_CONCAT)
    set(value) { put(JVMConfigurationKeys.STRING_CONCAT, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.jdkRelease: Int?
    get() = get(JVMConfigurationKeys.JDK_RELEASE)
    set(value) { put(JVMConfigurationKeys.JDK_RELEASE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.samConversions: JvmClosureGenerationScheme?
    get() = get(JVMConfigurationKeys.SAM_CONVERSIONS)
    set(value) { put(JVMConfigurationKeys.SAM_CONVERSIONS, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.lambdas: JvmClosureGenerationScheme?
    get() = get(JVMConfigurationKeys.LAMBDAS)
    set(value) { put(JVMConfigurationKeys.LAMBDAS, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.klibPaths: List<String>
    get() = getList(JVMConfigurationKeys.KLIB_PATHS)
    set(value) { put(JVMConfigurationKeys.KLIB_PATHS, value) }

var CompilerConfiguration.abiStability: JvmAbiStability?
    get() = get(JVMConfigurationKeys.ABI_STABILITY)
    set(value) { put(JVMConfigurationKeys.ABI_STABILITY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.doNotClearBindingContext: Boolean
    get() = getBoolean(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT)
    set(value) { put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, value) }

var CompilerConfiguration.noResetJarTimestamps: Boolean
    get() = getBoolean(JVMConfigurationKeys.NO_RESET_JAR_TIMESTAMPS)
    set(value) { put(JVMConfigurationKeys.NO_RESET_JAR_TIMESTAMPS, value) }

var CompilerConfiguration.noUnifiedNullChecks: Boolean
    get() = getBoolean(JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS)
    set(value) { put(JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS, value) }

var CompilerConfiguration.noSourceDebugExtension: Boolean
    get() = getBoolean(JVMConfigurationKeys.NO_SOURCE_DEBUG_EXTENSION)
    set(value) { put(JVMConfigurationKeys.NO_SOURCE_DEBUG_EXTENSION, value) }

var CompilerConfiguration.useOldInlineClassesManglingScheme: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME)
    set(value) { put(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME, value) }

var CompilerConfiguration.enableJvmPreview: Boolean
    get() = getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)
    set(value) { put(JVMConfigurationKeys.ENABLE_JVM_PREVIEW, value) }

var CompilerConfiguration.noReflect: Boolean
    get() = getBoolean(JVMConfigurationKeys.NO_REFLECT)
    set(value) { put(JVMConfigurationKeys.NO_REFLECT, value) }

var CompilerConfiguration.validateBytecode: Boolean
    get() = getBoolean(JVMConfigurationKeys.VALIDATE_BYTECODE)
    set(value) { put(JVMConfigurationKeys.VALIDATE_BYTECODE, value) }

var CompilerConfiguration.linkViaSignatures: Boolean
    get() = getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES)
    set(value) { put(JVMConfigurationKeys.LINK_VIA_SIGNATURES, value) }

var CompilerConfiguration.enableDebugMode: Boolean
    get() = getBoolean(JVMConfigurationKeys.ENABLE_DEBUG_MODE)
    set(value) { put(JVMConfigurationKeys.ENABLE_DEBUG_MODE, value) }

var CompilerConfiguration.enhancedCoroutinesDebugging: Boolean
    get() = getBoolean(JVMConfigurationKeys.ENHANCED_COROUTINES_DEBUGGING)
    set(value) { put(JVMConfigurationKeys.ENHANCED_COROUTINES_DEBUGGING, value) }

var CompilerConfiguration.noNewJavaAnnotationTargets: Boolean
    get() = getBoolean(JVMConfigurationKeys.NO_NEW_JAVA_ANNOTATION_TARGETS)
    set(value) { put(JVMConfigurationKeys.NO_NEW_JAVA_ANNOTATION_TARGETS, value) }

var CompilerConfiguration.useInlineScopesNumbers: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_INLINE_SCOPES_NUMBERS)
    set(value) { put(JVMConfigurationKeys.USE_INLINE_SCOPES_NUMBERS, value) }

var CompilerConfiguration.skipBodies: Boolean
    get() = getBoolean(JVMConfigurationKeys.SKIP_BODIES)
    set(value) { put(JVMConfigurationKeys.SKIP_BODIES, value) }

var CompilerConfiguration.expressionToEvaluate: String?
    get() = get(JVMConfigurationKeys.EXPRESSION_TO_EVALUATE)
    set(value) { putIfNotNull(JVMConfigurationKeys.EXPRESSION_TO_EVALUATE, value) }

var CompilerConfiguration.whenGenerationScheme: JvmWhenGenerationScheme?
    get() = get(JVMConfigurationKeys.WHEN_GENERATION_SCHEME)
    set(value) { put(JVMConfigurationKeys.WHEN_GENERATION_SCHEME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.ignoredAnnotationsForBridges: List<String>
    get() = getList(JVMConfigurationKeys.IGNORED_ANNOTATIONS_FOR_BRIDGES)
    set(value) { put(JVMConfigurationKeys.IGNORED_ANNOTATIONS_FOR_BRIDGES, value) }

