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

package org.jetbrains.kotlin.config;

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents;
import org.jetbrains.kotlin.modules.Module;

import java.io.File;
import java.util.List;

public class JVMConfigurationKeys {
    private JVMConfigurationKeys() {
    }

    public static final CompilerConfigurationKey<File> OUTPUT_DIRECTORY =
            CompilerConfigurationKey.create("output directory");
    public static final CompilerConfigurationKey<File> OUTPUT_JAR =
            CompilerConfigurationKey.create("output .jar");
    public static final CompilerConfigurationKey<Boolean> INCLUDE_RUNTIME =
            CompilerConfigurationKey.create("include runtime to the resulting .jar");

    public static final CompilerConfigurationKey<File> JDK_HOME =
            CompilerConfigurationKey.create("jdk home");

    public static final CompilerConfigurationKey<Boolean> NO_JDK =
            CompilerConfigurationKey.create("no jdk");

    public static final CompilerConfigurationKey<Boolean> DISABLE_STANDARD_SCRIPT_DEFINITION =
            CompilerConfigurationKey.create("Disable standard kotlin script support");

    public static final CompilerConfigurationKey<Boolean> RETAIN_OUTPUT_IN_MEMORY =
            CompilerConfigurationKey.create("retain compiled classes in memory for further use, e.g. when running scripts");

    public static final CompilerConfigurationKey<Boolean> DISABLE_CALL_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null call assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_RECEIVER_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null call receiver assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_PARAM_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null parameter assertions");
    public static final CompilerConfigurationKey<JVMAssertionsMode> ASSERTIONS_MODE =
            CompilerConfigurationKey.create("assertions mode");
    public static final CompilerConfigurationKey<Boolean> DISABLE_OPTIMIZATION =
            CompilerConfigurationKey.create("disable optimization");
    public static final CompilerConfigurationKey<Boolean> USE_TYPE_TABLE =
            CompilerConfigurationKey.create("use type table in serializer");

    public static final CompilerConfigurationKey<Boolean> USE_SINGLE_MODULE =
            CompilerConfigurationKey.create("combine modules for source files and binary dependencies into a single module");

    public static final CompilerConfigurationKey<JvmTarget> JVM_TARGET =
            CompilerConfigurationKey.create("JVM bytecode target version");

    public static final CompilerConfigurationKey<Boolean> PARAMETERS_METADATA =
            CompilerConfigurationKey.create("Parameters metadata for java 1.8 reflection");
    
    public static final CompilerConfigurationKey<IncrementalCompilationComponents> INCREMENTAL_COMPILATION_COMPONENTS =
            CompilerConfigurationKey.create("incremental cache provider");

    public static final CompilerConfigurationKey<File> MODULE_XML_FILE =
            CompilerConfigurationKey.create("path to module.xml");

    public static final CompilerConfigurationKey<String> DECLARATIONS_JSON_PATH =
            CompilerConfigurationKey.create("path to declarations output");

    public static final CompilerConfigurationKey<List<Module>> MODULES =
            CompilerConfigurationKey.create("module data");

    public static final CompilerConfigurationKey<List<String>> FRIEND_PATHS =
            CompilerConfigurationKey.create("friend module paths");

    public static final CompilerConfigurationKey<Boolean> IR =
            CompilerConfigurationKey.create("IR");

    public static final CompilerConfigurationKey<Boolean> USE_PSI_CLASS_FILES_READING =
            CompilerConfigurationKey.create("use a slower (PSI-based) class files reading implementation");

    public static final CompilerConfigurationKey<Boolean> USE_FAST_JAR_FILE_SYSTEM =
            CompilerConfigurationKey.create("use a faster JAR filesystem implementation");

    public static final CompilerConfigurationKey<Boolean> USE_JAVAC =
            CompilerConfigurationKey.create("use javac [experimental]");

    public static final CompilerConfigurationKey<Boolean> COMPILE_JAVA =
            CompilerConfigurationKey.create("compile java files [experimental]");

    public static final CompilerConfigurationKey<List<String>> ADDITIONAL_JAVA_MODULES =
            CompilerConfigurationKey.create("additional Java modules");

    public static final CompilerConfigurationKey<Boolean> EMIT_JVM_TYPE_ANNOTATIONS =
            CompilerConfigurationKey.create("Emit JVM type annotations in bytecode");

    public static final CompilerConfigurationKey<JvmStringConcat> STRING_CONCAT =
            CompilerConfigurationKey.create("Specifies string concatenation scheme");

    public static final CompilerConfigurationKey<Integer> JDK_RELEASE =
            CompilerConfigurationKey.create("Specifies JDK API version");

    public static final CompilerConfigurationKey<JvmClosureGenerationScheme> SAM_CONVERSIONS =
            CompilerConfigurationKey.create("SAM conversions code generation scheme");

    public static final CompilerConfigurationKey<JvmClosureGenerationScheme> LAMBDAS =
            CompilerConfigurationKey.create("Lambdas code generation scheme");

    public static final CompilerConfigurationKey<List<String>> KLIB_PATHS =
            CompilerConfigurationKey.create("Paths to .klib libraries");

    public static final CompilerConfigurationKey<JvmAbiStability> ABI_STABILITY =
            CompilerConfigurationKey.create("ABI stability of class files produced by JVM IR and/or FIR");

    public static final CompilerConfigurationKey<Boolean> DO_NOT_CLEAR_BINDING_CONTEXT =
            CompilerConfigurationKey.create("When using the IR backend, do not clear BindingContext between psi2ir and lowerings");

    public static final CompilerConfigurationKey<Boolean> NO_OPTIMIZED_CALLABLE_REFERENCES =
            CompilerConfigurationKey.create("Do not use optimized callable reference superclasses available from 1.4");

    public static final CompilerConfigurationKey<Boolean> NO_KOTLIN_NOTHING_VALUE_EXCEPTION =
            CompilerConfigurationKey.create("Do not use KotlinNothingValueException available since 1.4");

    public static final CompilerConfigurationKey<Boolean> NO_RESET_JAR_TIMESTAMPS =
            CompilerConfigurationKey.create("Do not reset timestamps in jar entries");

    public static final CompilerConfigurationKey<Boolean> NO_UNIFIED_NULL_CHECKS =
            CompilerConfigurationKey.create("Use pre-1.4 exception types in null checks instead of java.lang.NPE");

    public static final CompilerConfigurationKey<Boolean> USE_OLD_INLINE_CLASSES_MANGLING_SCHEME =
            CompilerConfigurationKey.create("Use old, 1.4 version of inline classes mangling scheme");

    public static final CompilerConfigurationKey<Boolean> ENABLE_JVM_PREVIEW =
            CompilerConfigurationKey.create("Enable Java language preview features");

    public static final CompilerConfigurationKey<Boolean> NO_REFLECT =
            CompilerConfigurationKey.create("Don't automatically include kotlin-reflect.jar into the output if the output is a jar");

    public static final CompilerConfigurationKey<JvmSerializeIrMode> SERIALIZE_IR =
            CompilerConfigurationKey.create("What functions to serialize as IR to class metadata");

    public static final CompilerConfigurationKey<Boolean> VALIDATE_IR =
            CompilerConfigurationKey.create("Validate IR");

    public static final CompilerConfigurationKey<Boolean> VALIDATE_BYTECODE =
            CompilerConfigurationKey.create("Validate generated JVM bytecode");

    public static final CompilerConfigurationKey<Boolean> LINK_VIA_SIGNATURES =
            CompilerConfigurationKey.create("Link JVM IR symbols via signatures, instead of by descriptors");
}
