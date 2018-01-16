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
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

class KonanConfigKeys {
    companion object {
        // Keep the list lexically sorted.
        val ABI_VERSION: CompilerConfigurationKey<Int> 
                = CompilerConfigurationKey.create("current abi version")
        val CHECK_DEPENDENCIES: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("check dependencies and download the missing ones")
        val DEBUG: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("add debug information")
        val DISABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("disable backend phases")
        val ENABLE_ASSERTIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("enable runtime assertions in generated code")
        val ENABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("enable backend phases")
        val ENTRY: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("fully qualified main() name")
        val GENERATE_TEST_RUNNER: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("generate test runner") 
        val INCLUDED_BINARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("included binary file paths")
        val LIBRARY_FILES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("library file paths")
        val LINKER_ARGS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("additional linker arguments")
        val LIST_PHASES: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("list backend phases")
        val LIST_TARGETS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("list available targets")
        val MANIFEST_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("provide manifest addend file")
        val META_INFO: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate metadata")
        val MODULE_KIND: CompilerConfigurationKey<ModuleKind> 
                = CompilerConfigurationKey.create("module kind")
        val MODULE_NAME: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("module name")
        val NATIVE_LIBRARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("native library file paths")
        val NODEFAULTLIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("don't link with the default libraries")
        val NOMAIN: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("assume 'main' entry point to be provided by external libraries")
        val NOSTDLIB: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't link with stdlib")
        val NOPACK: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't the library into a klib file")
        val OPTIMIZATION: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("optimized compilation")
        val OUTPUT: CompilerConfigurationKey<String> 
                = CompilerConfigurationKey.create("program or library name")
        val PRINT_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print bitcode")
        val PRINT_DESCRIPTORS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print descriptors")
        val PRINT_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print ir")
        val PRINT_IR_WITH_DESCRIPTORS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print ir with descriptors")
        val PRINT_LOCATIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print locations")
        val PRODUCE: CompilerConfigurationKey<CompilerOutputKind>
                = CompilerConfigurationKey.create("compiler output kind")
        val PURGE_USER_LIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("purge user-specified libs too")
        val REPOSITORIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("library search path repositories")
        val RUNTIME_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("override default runtime file path")
        val SOURCE_MAP: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate source map")
        val TARGET: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("target we compile for")
        val TIME_PHASES: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("time backend phases")
        val VERIFY_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify bitcode")
        val VERIFY_DESCRIPTORS: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify descriptors")
        val VERIFY_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify ir")
        val VERBOSE_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("verbose backend phases")
    }
}

