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
    val OUTPUT_DIRECTORY = CompilerConfigurationKey.create<File>("output directory")

    @JvmField
    val OUTPUT_JAR = CompilerConfigurationKey.create<File>("output .jar")

    @JvmField
    val INCLUDE_RUNTIME = CompilerConfigurationKey.create<Boolean>("include runtime to the resulting .jar")

    @JvmField
    val JDK_HOME = CompilerConfigurationKey.create<File>("jdk home")

    @JvmField
    val NO_JDK = CompilerConfigurationKey.create<Boolean>("no jdk")

    @JvmField
    val DISABLE_STANDARD_SCRIPT_DEFINITION = CompilerConfigurationKey.create<Boolean>("Disable standard kotlin script support")

    @JvmField
    val RETAIN_OUTPUT_IN_MEMORY = CompilerConfigurationKey.create<Boolean>("retain compiled classes in memory for further use, e.g. when running scripts")

    @JvmField
    val DISABLE_CALL_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("disable not-null call assertions")

    @JvmField
    val DISABLE_RECEIVER_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("disable not-null call receiver assertions")

    @JvmField
    val DISABLE_PARAM_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("disable not-null parameter assertions")

    @JvmField
    val ASSERTIONS_MODE = CompilerConfigurationKey.create<JVMAssertionsMode>("assertions mode")

    @JvmField
    val DISABLE_OPTIMIZATION = CompilerConfigurationKey.create<Boolean>("disable optimization")

    @JvmField
    val USE_TYPE_TABLE = CompilerConfigurationKey.create<Boolean>("use type table in serializer")

    @JvmField
    val JVM_TARGET = CompilerConfigurationKey.create<JvmTarget>("JVM bytecode target version")

    @JvmField
    val PARAMETERS_METADATA = CompilerConfigurationKey.create<Boolean>("Parameters metadata for java 1.8 reflection")

    @JvmField
    val INCREMENTAL_COMPILATION_COMPONENTS = CompilerConfigurationKey.create<IncrementalCompilationComponents>("incremental cache provider")

    @JvmField
    val MODULE_XML_FILE = CompilerConfigurationKey.create<File>("path to module.xml")

    @JvmField
    val MODULES = CompilerConfigurationKey.create<List<Module>>("module data")

    @JvmField
    val FRIEND_PATHS = CompilerConfigurationKey.create<List<String>>("friend module paths")

    @JvmField
    val USE_PSI_CLASS_FILES_READING = CompilerConfigurationKey.create<Boolean>("use a slower (PSI-based) class files reading implementation")

    @JvmField
    val USE_FAST_JAR_FILE_SYSTEM = CompilerConfigurationKey.create<Boolean>("use a faster JAR filesystem implementation")

    @JvmField
    val USE_JAVAC = CompilerConfigurationKey.create<Boolean>("use javac [experimental]")

    @JvmField
    val COMPILE_JAVA = CompilerConfigurationKey.create<Boolean>("compile java files [experimental]")

    @JvmField
    val ADDITIONAL_JAVA_MODULES = CompilerConfigurationKey.create<List<String>>("additional Java modules")

    @JvmField
    val EMIT_JVM_TYPE_ANNOTATIONS = CompilerConfigurationKey.create<Boolean>("Emit JVM type annotations in bytecode")

    @JvmField
    val STRING_CONCAT = CompilerConfigurationKey.create<JvmStringConcat>("Specifies string concatenation scheme")

    @JvmField
    val JDK_RELEASE = CompilerConfigurationKey.create<Int>("Specifies JDK API version")

    @JvmField
    val SAM_CONVERSIONS = CompilerConfigurationKey.create<JvmClosureGenerationScheme>("SAM conversions code generation scheme")

    @JvmField
    val LAMBDAS = CompilerConfigurationKey.create<JvmClosureGenerationScheme>("Lambdas code generation scheme")

    @JvmField
    val INDY_ALLOW_ANNOTATED_LAMBDAS = CompilerConfigurationKey.create<Boolean>("Allow using indy for lambdas with annotations")

    @JvmField
    val KLIB_PATHS = CompilerConfigurationKey.create<List<String>>("Paths to .klib libraries")

    @JvmField
    val ABI_STABILITY = CompilerConfigurationKey.create<JvmAbiStability>("ABI stability of class files produced by JVM IR and/or FIR")

    @JvmField
    val DO_NOT_CLEAR_BINDING_CONTEXT = CompilerConfigurationKey.create<Boolean>("When using the IR backend, do not clear BindingContext between psi2ir and lowerings")

    @JvmField
    val NO_RESET_JAR_TIMESTAMPS = CompilerConfigurationKey.create<Boolean>("Do not reset timestamps in jar entries")

    @JvmField
    val NO_UNIFIED_NULL_CHECKS = CompilerConfigurationKey.create<Boolean>("Use pre-1.4 exception types in null checks instead of java.lang.NPE")

    @JvmField
    val NO_SOURCE_DEBUG_EXTENSION = CompilerConfigurationKey.create<Boolean>("Do not generate @kotlin.jvm.internal.SourceDebugExtension annotation on a class with the copy of SMAP")

    @JvmField
    val USE_OLD_INLINE_CLASSES_MANGLING_SCHEME = CompilerConfigurationKey.create<Boolean>("Use old, 1.4 version of inline classes mangling scheme")

    @JvmField
    val ENABLE_JVM_PREVIEW = CompilerConfigurationKey.create<Boolean>("Enable Java language preview features")

    @JvmField
    val NO_REFLECT = CompilerConfigurationKey.create<Boolean>("Don't automatically include kotlin-reflect.jar into the output if the output is a jar")

    @JvmField
    val SERIALIZE_IR = CompilerConfigurationKey.create<JvmSerializeIrMode>("What functions to serialize as IR to class metadata")

    @JvmField
    val VALIDATE_BYTECODE = CompilerConfigurationKey.create<Boolean>("Validate generated JVM bytecode")

    @JvmField
    val LINK_VIA_SIGNATURES = CompilerConfigurationKey.create<Boolean>("Link JVM IR symbols via signatures, instead of by descriptors on the K1 frontend")

    @JvmField
    val ENABLE_DEBUG_MODE = CompilerConfigurationKey.create<Boolean>("Enable debug mode")

    @JvmField
    val NO_NEW_JAVA_ANNOTATION_TARGETS = CompilerConfigurationKey.create<Boolean>("Do not generate Java 1.8+ targets for Kotlin annotation classes")

    @JvmField
    val ENABLE_IR_INLINER = CompilerConfigurationKey.create<Boolean>("Enable inlining on IR, instead of inlining on bytecode")

    @JvmField
    val USE_INLINE_SCOPES_NUMBERS = CompilerConfigurationKey.create<Boolean>("Use inline scopes numbers for inline marker variables")

    @JvmField
    val SKIP_BODIES = CompilerConfigurationKey.create<Boolean>("Enable internal mode which causes FIR2IR to skip function bodies, used in KAPT")

    @JvmField
    val EXPRESSION_TO_EVALUATE = CompilerConfigurationKey.create<String>("Expression to evaluate in script mode")

    // For test purposes only. Cannot be set via CLI arguments
    @JvmField
    val USE_CLASS_BUILDER_FACTORY_FOR_TEST = CompilerConfigurationKey.create<Boolean>("Use ClassBuilderFactory.Test for GenerationState")

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

var CompilerConfiguration.retainOutputInMemory: Boolean
    get() = getBoolean(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY)
    set(value) { put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, value) }

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

var CompilerConfiguration.indyAllowAnnotatedLambdas: Boolean
    get() = getBoolean(JVMConfigurationKeys.INDY_ALLOW_ANNOTATED_LAMBDAS)
    set(value) { put(JVMConfigurationKeys.INDY_ALLOW_ANNOTATED_LAMBDAS, value) }

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

var CompilerConfiguration.serializeIr: JvmSerializeIrMode?
    get() = get(JVMConfigurationKeys.SERIALIZE_IR)
    set(value) { put(JVMConfigurationKeys.SERIALIZE_IR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.validateBytecode: Boolean
    get() = getBoolean(JVMConfigurationKeys.VALIDATE_BYTECODE)
    set(value) { put(JVMConfigurationKeys.VALIDATE_BYTECODE, value) }

var CompilerConfiguration.linkViaSignatures: Boolean
    get() = getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES)
    set(value) { put(JVMConfigurationKeys.LINK_VIA_SIGNATURES, value) }

var CompilerConfiguration.enableDebugMode: Boolean
    get() = getBoolean(JVMConfigurationKeys.ENABLE_DEBUG_MODE)
    set(value) { put(JVMConfigurationKeys.ENABLE_DEBUG_MODE, value) }

var CompilerConfiguration.noNewJavaAnnotationTargets: Boolean
    get() = getBoolean(JVMConfigurationKeys.NO_NEW_JAVA_ANNOTATION_TARGETS)
    set(value) { put(JVMConfigurationKeys.NO_NEW_JAVA_ANNOTATION_TARGETS, value) }

var CompilerConfiguration.enableIrInliner: Boolean
    get() = getBoolean(JVMConfigurationKeys.ENABLE_IR_INLINER)
    set(value) { put(JVMConfigurationKeys.ENABLE_IR_INLINER, value) }

var CompilerConfiguration.useInlineScopesNumbers: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_INLINE_SCOPES_NUMBERS)
    set(value) { put(JVMConfigurationKeys.USE_INLINE_SCOPES_NUMBERS, value) }

var CompilerConfiguration.skipBodies: Boolean
    get() = getBoolean(JVMConfigurationKeys.SKIP_BODIES)
    set(value) { put(JVMConfigurationKeys.SKIP_BODIES, value) }

var CompilerConfiguration.expressionToEvaluate: String?
    get() = get(JVMConfigurationKeys.EXPRESSION_TO_EVALUATE)
    set(value) { putIfNotNull(JVMConfigurationKeys.EXPRESSION_TO_EVALUATE, value) }

var CompilerConfiguration.useClassBuilderFactoryForTest: Boolean
    get() = getBoolean(JVMConfigurationKeys.USE_CLASS_BUILDER_FACTORY_FOR_TEST)
    set(value) { put(JVMConfigurationKeys.USE_CLASS_BUILDER_FACTORY_FOR_TEST, value) }

