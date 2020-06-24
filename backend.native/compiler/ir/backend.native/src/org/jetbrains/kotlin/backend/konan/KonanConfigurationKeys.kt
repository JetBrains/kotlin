/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

class KonanConfigKeys {
    companion object {
        // Keep the list lexically sorted.
        val CHECK_DEPENDENCIES: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("check dependencies and download the missing ones")
        val COMPATIBLE_COMPILER_VERSIONS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("compatible compiler versions")
        val DEBUG: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("add debug information")
        val DISABLE_FAKE_OVERRIDE_VALIDATOR: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("disable fake override validator")
        val DISABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("disable backend phases")
        val BITCODE_EMBEDDING_MODE: CompilerConfigurationKey<BitcodeEmbedding.Mode>
                = CompilerConfigurationKey.create("bitcode embedding mode")
        val EMIT_LAZY_OBJC_HEADER_FILE: CompilerConfigurationKey<String?> =
                CompilerConfigurationKey.create("output file to emit lazy Obj-C header")
        val ENABLE_ASSERTIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("enable runtime assertions in generated code")
        val ENABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("enable backend phases")
        val ENTRY: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("fully qualified main() name")
        val EXPORTED_LIBRARIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("libraries included into produced framework API")
        val LIBRARIES_TO_CACHE: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("paths to libraries that to be compiled to cache")
        val LIBRARY_TO_ADD_TO_CACHE: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create<String?>("path to library that to be added to cache")
        val CACHE_DIRECTORIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("paths to directories containing caches")
        val CACHED_LIBRARIES: CompilerConfigurationKey<Map<String, String>>
                = CompilerConfigurationKey.create<Map<String, String>>("mapping from library paths to cache paths")
        val FRAMEWORK_IMPORT_HEADERS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("headers imported to framework header")
        val FRIEND_MODULES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("friend module paths")
        val GENERATE_TEST_RUNNER: CompilerConfigurationKey<TestRunnerKind>
                = CompilerConfigurationKey.create("generate test runner") 
        val INCLUDED_BINARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("included binary file paths")
        val KONAN_HOME: CompilerConfigurationKey<String>
                = CompilerConfigurationKey.create("overridden compiler distribution path")
        val LIBRARY_FILES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("library file paths")
        val LIBRARY_VERSION: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("library version")
        val LIGHT_DEBUG: CompilerConfigurationKey<Boolean?>
                = CompilerConfigurationKey.create("add light debug information")
        val LINKER_ARGS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("additional linker arguments")
        val LIST_PHASES: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("list backend phases")
        val LIST_TARGETS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("list available targets")
        val MANIFEST_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("provide manifest addend file")
        val MEMORY_MODEL: CompilerConfigurationKey<MemoryModel>
                = CompilerConfigurationKey.create("memory model")
        val META_INFO: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate metadata")
        val METADATA_KLIB: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("metadata klib")
        val MODULE_KIND: CompilerConfigurationKey<ModuleKind> 
                = CompilerConfigurationKey.create("module kind")
        val MODULE_NAME: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("module name")
        val NATIVE_LIBRARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("native library file paths")
        val NODEFAULTLIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("don't link with the default libraries")
        val NOENDORSEDLIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("don't link with the endorsed libraries")
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
        val OVERRIDE_CLANG_OPTIONS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("arguments for clang")
        val ALLOCATION_MODE: CompilerConfigurationKey<String>
                = CompilerConfigurationKey.create("allocation mode")
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
        val INCLUDED_LIBRARIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey("klibs processed in the same manner as source files")
        val SOURCE_MAP: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate source map")
        val SHORT_MODULE_NAME: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey("short module name for IDE and export")
        val STATIC_FRAMEWORK: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("produce a static library for a framework")
        val TARGET: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("target we compile for")
        val TEMPORARY_FILES_DIR: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("directory for temporary files")
        val VERIFY_BITCODE: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("verify bitcode")
        val VERIFY_COMPILER: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("verify compiler")
        val DEBUG_INFO_VERSION: CompilerConfigurationKey<Int>
                = CompilerConfigurationKey.create("debug info format version")
        val COVERAGE: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("emit coverage info for sources")
        val LIBRARIES_TO_COVER: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("libraries that should be covered")
        val PROFRAW_PATH: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("path to *.profraw coverage output")
        val OBJC_GENERICS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("write objc header with generics support")
        val DEBUG_PREFIX_MAP: CompilerConfigurationKey<Map<String, String>>
                = CompilerConfigurationKey.create("remap file source paths in debug info")
    }
}

