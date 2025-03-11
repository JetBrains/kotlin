/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import java.io.File

@Suppress("unused")
object JvmConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.config", "JVMConfigurationKeys") {
    val OUTPUT_DIRECTORY by key<File>("output directory");
    val OUTPUT_JAR by key<File>("output .jar")
    val INCLUDE_RUNTIME by key<Boolean>("include runtime to the resulting .jar")

    val JDK_HOME by key<File>("jdk home")
    val NO_JDK by key<Boolean>("no jdk")

    val DISABLE_STANDARD_SCRIPT_DEFINITION by key<Boolean>("Disable standard kotlin script support")
    val RETAIN_OUTPUT_IN_MEMORY by key<Boolean>("retain compiled classes in memory for further use, e.g. when running scripts")

    val DISABLE_CALL_ASSERTIONS by key<Boolean>("disable not-null call assertions")
    val DISABLE_RECEIVER_ASSERTIONS by key<Boolean>("disable not-null call receiver assertions")
    val DISABLE_PARAM_ASSERTIONS by key<Boolean>("disable not-null parameter assertions")
    val ASSERTIONS_MODE by key<JVMAssertionsMode>("assertions mode")
    val DISABLE_OPTIMIZATION by key<Boolean>("disable optimization")
    val USE_TYPE_TABLE by key<Boolean>("use type table in serializer")

    val JVM_TARGET by key<JvmTarget>("JVM bytecode target version")

    val PARAMETERS_METADATA by key<Boolean>("Parameters metadata for java 1.8 reflection")

    val INCREMENTAL_COMPILATION_COMPONENTS by key<IncrementalCompilationComponents>("incremental cache provider", throwOnNull = false)

    val MODULE_XML_FILE by key<File>("path to module.xml")

    val MODULES by key<List<Module>>("module data")

    val FRIEND_PATHS by key<List<String>>("friend module paths")

    val USE_PSI_CLASS_FILES_READING by key<Boolean>("use a slower (PSI-based) class files reading implementation")

    val USE_FAST_JAR_FILE_SYSTEM by key<Boolean>("use a faster JAR filesystem implementation")

    val USE_JAVAC by key<Boolean>("use javac [experimental]")

    val COMPILE_JAVA by key<Boolean>("compile java files [experimental]")

    val ADDITIONAL_JAVA_MODULES by key<List<String>>("additional Java modules")

    val EMIT_JVM_TYPE_ANNOTATIONS by key<Boolean>("Emit JVM type annotations in bytecode")

    val STRING_CONCAT by key<JvmStringConcat>("Specifies string concatenation scheme")

    val JDK_RELEASE by key<Integer>("Specifies JDK API version")

    val SAM_CONVERSIONS by key<JvmClosureGenerationScheme>("SAM conversions code generation scheme")

    val LAMBDAS by key<JvmClosureGenerationScheme>("Lambdas code generation scheme")

    val INDY_ALLOW_ANNOTATED_LAMBDAS by key<Boolean>("Allow using indy for lambdas with annotations")

    val KLIB_PATHS by key<List<String>>("Paths to .klib libraries")

    val ABI_STABILITY by key<JvmAbiStability>("ABI stability of class files produced by JVM IR and/or FIR")

    val DO_NOT_CLEAR_BINDING_CONTEXT by key<Boolean>("When using the IR backend, do not clear BindingContext between psi2ir and lowerings")

    val NO_RESET_JAR_TIMESTAMPS by key<Boolean>("Do not reset timestamps in jar entries")

    val NO_UNIFIED_NULL_CHECKS by key<Boolean>("Use pre-1.4 exception types in null checks instead of java.lang.NPE")

    val NO_SOURCE_DEBUG_EXTENSION by key<Boolean>("Do not generate @kotlin.jvm.internal.SourceDebugExtension annotation on a class with the copy of SMAP")

    val USE_OLD_INLINE_CLASSES_MANGLING_SCHEME by key<Boolean>("Use old, 1.4 version of inline classes mangling scheme")

    val ENABLE_JVM_PREVIEW by key<Boolean>("Enable Java language preview features")

    val NO_REFLECT by key<Boolean>("Don't automatically include kotlin-reflect.jar into the output if the output is a jar")

    val SERIALIZE_IR by key<JvmSerializeIrMode>("What functions to serialize as IR to class metadata")

    val VALIDATE_BYTECODE by key<Boolean>("Validate generated JVM bytecode")

    val LINK_VIA_SIGNATURES by key<Boolean>("Link JVM IR symbols via signatures, instead of by descriptors on the K1 frontend")

    val ENABLE_DEBUG_MODE by key<Boolean>("Enable debug mode")

    val NO_NEW_JAVA_ANNOTATION_TARGETS by key<Boolean>("Do not generate Java 1.8+ targets for Kotlin annotation classes")

    val ENABLE_IR_INLINER by key<Boolean>("Enable inlining on IR, instead of inlining on bytecode")

    val USE_INLINE_SCOPES_NUMBERS by key<Boolean>("Use inline scopes numbers for inline marker variables")

    val SKIP_BODIES by key<Boolean>("Enable internal mode which causes FIR2IR to skip function bodies, used in KAPT")

    val EXPRESSION_TO_EVALUATE by key<String>("Expression to evaluate in script mode", throwOnNull = false)

    val USE_CLASS_BUILDER_FACTORY_FOR_TEST by key<Boolean>(
        "Use ClassBuilderFactory.Test for GenerationState",
        comment = "For test purposes only. Cannot be set via CLI arguments"
    )
}
