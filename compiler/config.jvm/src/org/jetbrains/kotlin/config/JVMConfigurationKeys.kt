/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import java.io.File

object JVMConfigurationKeys {
    @JvmField
    val OUTPUT_DIRECTORY: CompilerConfigurationKey<File> = CompilerConfigurationKey.create<File>("output directory")

    @JvmField
    val OUTPUT_JAR: CompilerConfigurationKey<File> = CompilerConfigurationKey.create<File>("output .jar")

    @JvmField
    val INCLUDE_RUNTIME: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("include runtime to the resulting .jar")

    @JvmField
    val JDK_HOME: CompilerConfigurationKey<File> = CompilerConfigurationKey.create<File>("jdk home")

    @JvmField
    val NO_JDK: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("no jdk")

    @JvmField
    val DISABLE_STANDARD_SCRIPT_DEFINITION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Disable standard kotlin script support")

    @JvmField
    val RETAIN_OUTPUT_IN_MEMORY: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("retain compiled classes in memory for further use, e.g. when running scripts")

    @JvmField
    val DISABLE_CALL_ASSERTIONS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("disable not-null call assertions")

    @JvmField
    val DISABLE_RECEIVER_ASSERTIONS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("disable not-null call receiver assertions")

    @JvmField
    val DISABLE_PARAM_ASSERTIONS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("disable not-null parameter assertions")

    @JvmField
    val ASSERTIONS_MODE: CompilerConfigurationKey<JVMAssertionsMode> =
        CompilerConfigurationKey.create<JVMAssertionsMode>("assertions mode")

    @JvmField
    val DISABLE_OPTIMIZATION: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("disable optimization")

    @JvmField
    val USE_TYPE_TABLE: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("use type table in serializer")

    @JvmField
    val JVM_TARGET: CompilerConfigurationKey<JvmTarget> = CompilerConfigurationKey.create<JvmTarget>("JVM bytecode target version")

    @JvmField
    val PARAMETERS_METADATA: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Parameters metadata for java 1.8 reflection")

    @JvmField
    val INCREMENTAL_COMPILATION_COMPONENTS: CompilerConfigurationKey<IncrementalCompilationComponents> =
        CompilerConfigurationKey.create<IncrementalCompilationComponents>("incremental cache provider")

    @JvmField
    val MODULE_XML_FILE: CompilerConfigurationKey<File> = CompilerConfigurationKey.create<File>("path to module.xml")

    @JvmField
    val MODULES: CompilerConfigurationKey<List<Module>> = CompilerConfigurationKey.create<List<Module>>("module data")

    @JvmField
    val FRIEND_PATHS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create<List<String>>("friend module paths")

    @JvmField
    val USE_PSI_CLASS_FILES_READING: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("use a slower (PSI-based) class files reading implementation")

    @JvmField
    val USE_FAST_JAR_FILE_SYSTEM: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("use a faster JAR filesystem implementation")

    @JvmField
    val USE_JAVAC: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("use javac [experimental]")

    @JvmField
    val COMPILE_JAVA: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("compile java files [experimental]")

    @JvmField
    val ADDITIONAL_JAVA_MODULES: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create<List<String>>("additional Java modules")

    @JvmField
    val EMIT_JVM_TYPE_ANNOTATIONS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Emit JVM type annotations in bytecode")

    @JvmField
    val STRING_CONCAT: CompilerConfigurationKey<JvmStringConcat> =
        CompilerConfigurationKey.create<JvmStringConcat>("Specifies string concatenation scheme")

    @JvmField
    val JDK_RELEASE: CompilerConfigurationKey<Int> = CompilerConfigurationKey.create<Int>("Specifies JDK API version")

    @JvmField
    val SAM_CONVERSIONS: CompilerConfigurationKey<JvmClosureGenerationScheme> =
        CompilerConfigurationKey.create<JvmClosureGenerationScheme>("SAM conversions code generation scheme")

    @JvmField
    val LAMBDAS: CompilerConfigurationKey<JvmClosureGenerationScheme> =
        CompilerConfigurationKey.create<JvmClosureGenerationScheme>("Lambdas code generation scheme")

    @JvmField
    val KLIB_PATHS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create<List<String>>("Paths to .klib libraries")

    @JvmField
    val ABI_STABILITY: CompilerConfigurationKey<JvmAbiStability> =
        CompilerConfigurationKey.create<JvmAbiStability>("ABI stability of class files produced by JVM IR and/or FIR")

    @JvmField
    val DO_NOT_CLEAR_BINDING_CONTEXT: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("When using the IR backend, do not clear BindingContext between psi2ir and lowerings")

    @JvmField
    val NO_RESET_JAR_TIMESTAMPS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Do not reset timestamps in jar entries")

    @JvmField
    val NO_UNIFIED_NULL_CHECKS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Use pre-1.4 exception types in null checks instead of java.lang.NPE")

    @JvmField
    val NO_SOURCE_DEBUG_EXTENSION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Do not generate @kotlin.jvm.internal.SourceDebugExtension annotation on a class with the copy of SMAP")

    @JvmField
    val USE_OLD_INLINE_CLASSES_MANGLING_SCHEME: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Use old, 1.4 version of inline classes mangling scheme")

    @JvmField
    val ENABLE_JVM_PREVIEW: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Enable Java language preview features")

    @JvmField
    val NO_REFLECT: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Don't automatically include kotlin-reflect.jar into the output if the output is a jar")

    @JvmField
    val SERIALIZE_IR: CompilerConfigurationKey<JvmSerializeIrMode> =
        CompilerConfigurationKey.create<JvmSerializeIrMode>("What functions to serialize as IR to class metadata")

    @JvmField
    val VALIDATE_BYTECODE: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("Validate generated JVM bytecode")

    @JvmField
    val LINK_VIA_SIGNATURES: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Link JVM IR symbols via signatures, instead of by descriptors on the K1 frontend")

    @JvmField
    val ENABLE_DEBUG_MODE: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("Enable debug mode")

    @JvmField
    val NO_NEW_JAVA_ANNOTATION_TARGETS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Do not generate Java 1.8+ targets for Kotlin annotation classes")

    @JvmField
    val OLD_INNER_CLASSES_LOGIC: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Use old logic for generation of InnerClasses attributes")

    @JvmField
    val ENABLE_IR_INLINER: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Enable inlining on IR, instead of inlining on bytecode")

    @JvmField
    val USE_INLINE_SCOPES_NUMBERS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Use inline scopes numbers for inline marker variables")

    @JvmField
    val SKIP_BODIES: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("Enable internal mode which causes FIR2IR to skip function bodies, used in KAPT")
}
