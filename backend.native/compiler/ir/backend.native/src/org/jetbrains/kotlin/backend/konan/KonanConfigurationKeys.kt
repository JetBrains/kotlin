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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.serialization.js.ModuleKind

class KonanConfigKeys {
    companion object {
        val LIBRARY_FILES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("library file paths")
        val NATIVE_LIBRARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("native library file paths")
        val BITCODE_FILE: CompilerConfigurationKey<String> 
                = CompilerConfigurationKey.create("emitted bitcode file path")
        val EXECUTABLE_FILE: CompilerConfigurationKey<String> 
                = CompilerConfigurationKey.create("final executable file path")
        val RUNTIME_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("override default runtime file path")
        val PROPERTY_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("override default property file path")
        val ABI_VERSION: CompilerConfigurationKey<Int> 
                = CompilerConfigurationKey.create("current abi version")
        val OPTIMIZATION: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("optimized compilation")
        val DEBUG: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("compilation with debug information")
        val NOSTDLIB: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't link with stdlib")
        val NOLINK: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't link, only produce a bitcode file ")
        val NOMAIN: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("assume 'main' entry point to be provided by external libraries")
        val LINKER_ARGS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("additional linker arguments")
        val TARGET: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("target we compile for")
        val LIST_TARGETS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("list available targets")

        val SOURCE_MAP: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate source map")
        val META_INFO: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate metadata")
        val MODULE_KIND: CompilerConfigurationKey<ModuleKind> 
                = CompilerConfigurationKey.create("module kind")

        val VERIFY_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify ir")
        val VERIFY_DESCRIPTORS: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify descriptors")
        val VERIFY_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify bitcode")

        val PRINT_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print ir")
        val PRINT_IR_WITH_DESCRIPTORS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print ir with descriptors")
        val PRINT_DESCRIPTORS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print descriptors")
        val PRINT_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print bitcode")
        val PRINT_LOCATIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print locations")

        val ENABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("enable backend phases")
        val DISABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("disable backend phases")
        val VERBOSE_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("verbose backend phases")
        val LIST_PHASES: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("list backend phases")
        val TIME_PHASES: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("time backend phases")

        val ENABLE_ASSERTIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("enable runtime assertions in generated code")
    }
}

