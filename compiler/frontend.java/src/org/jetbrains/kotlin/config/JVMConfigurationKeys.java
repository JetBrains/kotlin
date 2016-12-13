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

import java.util.List;

public class JVMConfigurationKeys {
    private JVMConfigurationKeys() {
    }

    // roots, including dependencies and own source
    public static final CompilerConfigurationKey<List<ContentRoot>> CONTENT_ROOTS =
            CompilerConfigurationKey.create("content roots");

    public static final CompilerConfigurationKey<List<KotlinScriptDefinition>> SCRIPT_DEFINITIONS =
            CompilerConfigurationKey.create("script definitions");

    public static final CompilerConfigurationKey<Boolean> DISABLE_CALL_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null call assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_PARAM_ASSERTIONS =
            CompilerConfigurationKey.create("disable not-null parameter assertions");
    public static final CompilerConfigurationKey<Boolean> DISABLE_INLINE =
            CompilerConfigurationKey.create("disable inline");
    public static final CompilerConfigurationKey<Boolean> DISABLE_OPTIMIZATION =
            CompilerConfigurationKey.create("disable optimization");
    public static final CompilerConfigurationKey<Boolean> INHERIT_MULTIFILE_PARTS =
            CompilerConfigurationKey.create("compile multifile classes to a hierarchy of parts and facade");
    public static final CompilerConfigurationKey<Boolean> SKIP_RUNTIME_VERSION_CHECK =
            CompilerConfigurationKey.create("do not perform checks on runtime versions consistency");

    public static final CompilerConfigurationKey<JvmTarget> JVM_TARGET =
            CompilerConfigurationKey.create("JVM bytecode target version");

    public static final CompilerConfigurationKey<IncrementalCompilationComponents> INCREMENTAL_COMPILATION_COMPONENTS =
            CompilerConfigurationKey.create("incremental cache provider");

    public static final CompilerConfigurationKey<String> MODULE_XML_FILE_PATH =
            CompilerConfigurationKey.create("path to module.xml");

    public static final CompilerConfigurationKey<String> DECLARATIONS_JSON_PATH =
            CompilerConfigurationKey.create("path to declarations output");

    public static final CompilerConfigurationKey<List<Module>> MODULES =
            CompilerConfigurationKey.create("module data");

    public static final CompilerConfigurationKey<String> MODULE_NAME =
            CompilerConfigurationKey.create("module name");
}
