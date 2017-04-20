/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents;
import org.jetbrains.kotlin.modules.Module;
import org.jetbrains.kotlin.script.KotlinScriptDefinition;

import java.io.File;
import java.util.List;

public class JVMConfigurationKeys {
    private JVMConfigurationKeys() {
    }

    // roots, including dependencies and own source
    public static final CompilerConfigurationKey<List<ContentRoot>> CONTENT_ROOTS =
            CompilerConfigurationKey.create("content roots");

    public static final CompilerConfigurationKey<File> OUTPUT_DIRECTORY =
            CompilerConfigurationKey.create("output directory");
    public static final CompilerConfigurationKey<File> OUTPUT_JAR =
            CompilerConfigurationKey.create("output .jar");
    public static final CompilerConfigurationKey<Boolean> INCLUDE_RUNTIME =
            CompilerConfigurationKey.create("include runtime to the resulting .jar");

    public static final CompilerConfigurationKey<List<KotlinScriptDefinition>> SCRIPT_DEFINITIONS =
            CompilerConfigurationKey.create("script definitions");

    public static final CompilerConfigurationKey<Boolean> RETAIN_OUTPUT_IN_MEMORY =
            CompilerConfigurationKey.create("retain compiled classes in memory for further use, e.g. when running scripts");

    public static final CompilerConfigurationKey<Boolean> DISABLE_CALL_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null call assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_PARAM_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null parameter assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_OPTIMIZATION =
            CompilerConfigurationKey.create("disable optimization");
    public static final CompilerConfigurationKey<Boolean> INHERIT_MULTIFILE_PARTS =
            CompilerConfigurationKey.create("compile multifile classes to a hierarchy of parts and facade");
    public static final CompilerConfigurationKey<Boolean> USE_TYPE_TABLE =
            CompilerConfigurationKey.create("use type table in serializer");

    public static final CompilerConfigurationKey<Boolean> USE_SINGLE_MODULE =
            CompilerConfigurationKey.create("combine modules for source files and binary dependencies into a single module");

    /**
     * Controls whether the module depends on an additional "built-ins" module, which contains binary metadata of built-in definitions.
     * By default, that metadata is loaded from kotlin-compiler.jar.
     * However, it can be also loaded directly from the module, see {@link CREATE_BUILT_INS_FROM_MODULE_DEPENDENCIES}
     */
    public static final CompilerConfigurationKey<Boolean> ADD_BUILT_INS_FROM_COMPILER_TO_DEPENDENCIES =
            CompilerConfigurationKey.create("add built-ins from the compiler jar to the dependencies of the module being resolved");

    /**
     * Controls whether an instance of KotlinBuiltIns which is passed to {@link ModuleDescriptorImpl}'s constructor and ends up being used
     * everywhere in the compiler front-end is loaded directly from the module (from its sources and/or its binaries), as opposed to
     * from kotlin-compiler.jar
     */
    public static final CompilerConfigurationKey<Boolean> CREATE_BUILT_INS_FROM_MODULE_DEPENDENCIES =
            CompilerConfigurationKey.create("create built-ins from resources found in the module dependencies");

    public static final CompilerConfigurationKey<Boolean> SKIP_RUNTIME_VERSION_CHECK =
            CompilerConfigurationKey.create("do not perform checks on runtime versions consistency");

    public static final CompilerConfigurationKey<JvmTarget> JVM_TARGET =
            CompilerConfigurationKey.create("JVM bytecode target version");

    public static final CompilerConfigurationKey<Boolean> PARAMETERS_METADATA =
            CompilerConfigurationKey.create("Parameters metadata for java 1.8 reflection");

    public static final CompilerConfigurationKey<Boolean> INTERFACE_COMPATIBILITY =
            CompilerConfigurationKey.create("Generate additional 'DefaultImpls' class files for jvm 8 target for compatibility with 6 target interfaces");

    public static final CompilerConfigurationKey<Boolean> JVM8_TARGET_WITH_DEFAULTS =
            CompilerConfigurationKey.create("Generate default methods in interfaces");

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

    public static final CompilerConfigurationKey<Boolean> USE_FAST_CLASS_FILES_READING =
            CompilerConfigurationKey.create("use fast class files reading implementation [experimental]");
}
