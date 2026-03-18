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
    val OUTPUT_DIRECTORY by key<File>()
    val OUTPUT_JAR by key<File>()
    val INCLUDE_RUNTIME by key<Boolean>("Include runtime to the resulting .jar file.")
    val JDK_HOME by key<File>()
    val NO_JDK by key<Boolean>()
    val DISABLE_STANDARD_SCRIPT_DEFINITION by key<Boolean>()
    val DISABLE_CALL_ASSERTIONS by key<Boolean>()
    val DISABLE_RECEIVER_ASSERTIONS by key<Boolean>()
    val DISABLE_PARAM_ASSERTIONS by key<Boolean>()
    val ASSERTIONS_MODE by key<JVMAssertionsMode>()
    val DISABLE_OPTIMIZATION by key<Boolean>()
    val USE_TYPE_TABLE by key<Boolean>()
    val JVM_TARGET by key<JvmTarget>()
    val PARAMETERS_METADATA by key<Boolean>()
    val INCREMENTAL_COMPILATION_COMPONENTS by key<IncrementalCompilationComponents>(throwOnNull = false)
    val MODULE_XML_FILE by key<File>()
    val MODULES by key<List<Module>>()
    val FRIEND_PATHS by key<List<String>>()
    val USE_PSI_CLASS_FILES_READING by key<Boolean>("Use a slower, PSI-based, class files reading implementation.")
    val USE_FAST_JAR_FILE_SYSTEM by key<Boolean>()
    val USE_JAVAC by key<Boolean>()
    val COMPILE_JAVA by key<Boolean>()
    val ADDITIONAL_JAVA_MODULES by key<List<String>>()
    val EMIT_JVM_TYPE_ANNOTATIONS by key<Boolean>()
    val STRING_CONCAT by key<JvmStringConcat>()
    val JDK_RELEASE by key<Int>()
    val SAM_CONVERSIONS by key<JvmClosureGenerationScheme>()
    val LAMBDAS by key<JvmClosureGenerationScheme>()
    val KLIB_PATHS by key<List<String>>("Paths to .klib libraries.")
    val ABI_STABILITY by key<JvmAbiStability>("ABI stability of class files produced by JVM IR and/or FIR.")
    val DO_NOT_CLEAR_BINDING_CONTEXT by key<Boolean>("When using K1, do not clear BindingContext between psi2ir and lowerings.")
    val NO_RESET_JAR_TIMESTAMPS by key<Boolean>()
    val NO_UNIFIED_NULL_CHECKS by key<Boolean>("Use pre-1.4 exception types in null checks instead of java.lang.NPE.")
    val NO_SOURCE_DEBUG_EXTENSION by key<Boolean>("Do not generate @kotlin.jvm.internal.SourceDebugExtension annotation on a class with the copy of SMAP.")
    val USE_OLD_INLINE_CLASSES_MANGLING_SCHEME by key<Boolean>("Use old, 1.4 version of inline classes mangling scheme.")
    val ENABLE_JVM_PREVIEW by key<Boolean>()
    val NO_REFLECT by key<Boolean>("Don't automatically include kotlin-reflect.jar into the output if the output is a jar.")
    val VALIDATE_BYTECODE by key<Boolean>()
    val LINK_VIA_SIGNATURES by key<Boolean>("Link JVM IR symbols via signatures, instead of by descriptors on the K1 frontend.")
    val ENABLE_DEBUG_MODE by key<Boolean>()
    val ENHANCED_COROUTINES_DEBUGGING by key<Boolean>("Mark compiled generated code in coroutines.")
    val NO_NEW_JAVA_ANNOTATION_TARGETS by key<Boolean>("Do not generate Java 1.8+ targets for Kotlin annotation classes.")
    val USE_INLINE_SCOPES_NUMBERS by key<Boolean>("Use inline scopes numbers for inline marker variables.")
    val SKIP_BODIES by key<Boolean>("Enable internal mode which causes FIR2IR to skip function bodies, used in KAPT.")
    val EXPRESSION_TO_EVALUATE by key<String>("Expression to evaluate in script mode.", throwOnNull = false)
    val WHEN_GENERATION_SCHEME by key<JvmWhenGenerationScheme>("Specifies generation scheme for type-checking 'when' expressions.")
    val IGNORED_ANNOTATIONS_FOR_BRIDGES by key<List<String>>("Annotations fqNames that shall be skipped while copying the annotations from the target to the bridge functions.")
}
