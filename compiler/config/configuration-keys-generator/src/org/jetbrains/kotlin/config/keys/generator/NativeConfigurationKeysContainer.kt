/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.backend.konan.AllocationMode
import org.jetbrains.kotlin.backend.konan.CachePolicy.*
import org.jetbrains.kotlin.backend.konan.LlvmVariant
import org.jetbrains.kotlin.backend.konan.TestRunnerKind
import org.jetbrains.kotlin.konan.library.SerializedKlibDAG
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

@Suppress("unused")
object NativeConfigurationKeysContainer : NativeKeysContainer("org.jetbrains.kotlin.konan.config", "NativeConfigurationKeys") {
    val CHECK_DEPENDENCIES by key<Boolean>("Check dependencies and download the missing ones.", cachePolicy = NOT_PROPAGATED)
    val DEBUG by key<Boolean>(cachePolicy = AFFECTS_CODEGEN)
    val EMIT_LAZY_OBJC_HEADER_FILE by key<String>(cachePolicy = NOT_PROPAGATED)
    val ENABLE_ASSERTIONS by key<Boolean>(cachePolicy = AFFECTS_CODEGEN)
    val KONAN_ENTRY_POINT by key<String>("Fully qualified main() name.", cachePolicy = NOT_PROPAGATED)
    val EXPORTED_LIBRARIES by key<List<String>>("Libraries included into produced framework API.", cachePolicy = NOT_PROPAGATED)
    val FULL_EXPORTED_NAME_PREFIX by key<String>("Prefix used when exporting Kotlin names to other languages.", cachePolicy = NOT_PROPAGATED)
    val KONAN_LIBRARY_TO_ADD_TO_CACHE by key<String>(cachePolicy = NOT_PROPAGATED)
    val CACHE_DIRECTORIES by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val AUTO_CACHEABLE_FROM by key<List<String>>("Paths to the root directories from which dependencies are to be cached automatically.", cachePolicy = NOT_PROPAGATED)
    val AUTO_CACHE_DIR by key<String>("Path to the directory where to put caches for auto-cacheable dependencies.", cachePolicy = NOT_PROPAGATED)
    val INCREMENTAL_CACHE_DIR by key<String>("Path to the directory where to put incremental build caches.", cachePolicy = NOT_PROPAGATED)
    val DUMP_BUILT_CACHES_TO by key<String>("Path to a file where the list of all cache archives produced by this build should be written.", cachePolicy = NOT_PROPAGATED)
    val CACHED_LIBRARIES by key<Map<String, String>>("Mapping from library paths to cache paths.", cachePolicy = NOT_PROPAGATED)
    val FILES_TO_CACHE by key<List<String>>("Which files should be compiled to cache.", cachePolicy = NOT_PROPAGATED)
    val MAKE_PER_FILE_CACHE by key<Boolean>(cachePolicy = NOT_PROPAGATED)
    val CACHED_LIBRARY_DEPENDENCIES_FINGERPRINT by key<String>("Combined fingerprint of external cached dependencies of the library being cached.", cachePolicy = NOT_PROPAGATED)
    val FRAMEWORK_IMPORT_HEADERS by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val KONAN_FRIEND_LIBRARIES by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val KONAN_REFINES_MODULES by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val GENERATE_TEST_RUNNER by key<TestRunnerKind>(cachePolicy = NOT_PROPAGATED)
    val KONAN_INCLUDED_BINARIES by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val KONAN_HOME by key<String>(cachePolicy = NOT_PROPAGATED)
    val KONAN_LIBRARIES by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val LIGHT_DEBUG by key<Boolean>(cachePolicy = AFFECTS_CODEGEN)
    val GENERATE_DEBUG_TRAMPOLINE by key<Boolean>("Generates debug trampolines to make debugger breakpoint resolution more accurate.", cachePolicy = AFFECTS_CODEGEN)
    val LINKER_ARGS by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val LIST_TARGETS by key<Boolean>(cachePolicy = NOT_PROPAGATED)
    val KONAN_MANIFEST_ADDEND by key<String>(cachePolicy = NOT_PROPAGATED)
    val KONAN_GENERATED_HEADER_KLIB_PATH by key<String>("Path to file where header klib should be produced.", cachePolicy = NOT_PROPAGATED)
    // The native libraries are linked into the LLVM module being compiled,
    // so passing them to every cache build would link them into every cache.
    val KONAN_NATIVE_LIBRARIES by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val KONAN_NO_DEFAULT_LIBS by key<Boolean>("Don't link with the default libraries.", cachePolicy = NOT_PROPAGATED)
    val NOMAIN by key<Boolean>("Assume 'main' entry point to be provided by external libraries.", cachePolicy = NOT_PROPAGATED)
    val KONAN_NO_STDLIB by key<Boolean>("Don't link with stdlib.", cachePolicy = NOT_PROPAGATED)
    val KONAN_DONT_COMPRESS_KLIB by key<Boolean>("Don't pack the library into a klib file.", cachePolicy = NOT_PROPAGATED)
    val OPTIMIZATION by key<Boolean>(cachePolicy = AFFECTS_CODEGEN)
    val KONAN_OUTPUT_PATH by key<String>(cachePolicy = NOT_PROPAGATED)
    val OVERRIDE_CLANG_OPTIONS by key<List<String>>(cachePolicy = AFFECTS_CODEGEN)
    val ALLOCATION_MODE by key<AllocationMode>(cachePolicy = AFFECTS_RUNTIME)
    val KONAN_PRINT_BITCODE by key<Boolean>(cachePolicy = NOT_PROPAGATED)
    val KONAN_PRINT_IR by key<Boolean>(cachePolicy = NOT_PROPAGATED)
    val KONAN_PRINT_FILES by key<Boolean>(cachePolicy = NOT_PROPAGATED)
    val KONAN_PRODUCED_ARTIFACT_KIND by key<CompilerOutputKind>(cachePolicy = NOT_PROPAGATED)
    val RUNTIME_FILE by key<String>(cachePolicy = AFFECTS_RUNTIME)
    val KONAN_INCLUDED_LIBRARIES by key<List<String>>("Klibs processed in the same manner as source files.", cachePolicy = NOT_PROPAGATED)
    val KONAN_SHORT_MODULE_NAME by key<String>("Short module name for IDE and export.", cachePolicy = NOT_PROPAGATED)
    val STATIC_FRAMEWORK by key<Boolean>("Produce a static library for a framework.", cachePolicy = NOT_PROPAGATED)
    val KONAN_TARGET by key<String>(cachePolicy = AFFECTS_CODEGEN)
    val TEMPORARY_FILES_DIR by key<String>(cachePolicy = NOT_PROPAGATED)
    val SAVE_LLVM_IR by key<List<String>>(cachePolicy = NOT_PROPAGATED)
    val VERIFY_BITCODE by key<Boolean>(cachePolicy = PROPAGATED)
    val VERIFY_COMPILER by key<Boolean>(cachePolicy = PROPAGATED)
    val KONAN_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO by key<String>(cachePolicy = NOT_PROPAGATED)
    val DEBUG_INFO_VERSION by key<Int>(cachePolicy = AFFECTS_CODEGEN)
    val OBJC_GENERICS by key<Boolean>("Write objc header with generics support.", cachePolicy = NOT_PROPAGATED)
    // TODO: The prefix map affects the debug info in the caches, but its paths are specific to the build,
    //  so making it a part of the cache flavor would prevent sharing the caches between the projects.
    val DEBUG_PREFIX_MAP by key<Map<String, String>>("Remap file source paths in debug info.", cachePolicy = NOT_PROPAGATED)
    val PRE_LINK_CACHES by key<Boolean>("Perform compiler caches pre-link.", cachePolicy = NOT_PROPAGATED)
    // May change the toolchain (and so, the generated code), but is commonly used to tweak the environment
    // (e.g., to use a provisioned Xcode), so isn't a part of the cache flavor.
    val OVERRIDE_KONAN_PROPERTIES by key<Map<String, String>>("Override konan.properties values.", cachePolicy = PROPAGATED)
    val PROPERTY_LAZY_INITIALIZATION by key<Boolean>(cachePolicy = AFFECTS_CODEGEN)
    val EXTERNAL_DEPENDENCIES by key<String>("The file with the graph of dependencies (libraries) collected by the build system.", cachePolicy = PROPAGATED)
    val LLVM_VARIANT by key<LlvmVariant>(cachePolicy = PROPAGATED)
    val RUNTIME_LOGS by key<String>(cachePolicy = AFFECTS_RUNTIME)
    val TEST_DUMP_OUTPUT_PATH by key<String>("Path to a file to dump the list of all available tests.", cachePolicy = NOT_PROPAGATED)
    val OMIT_FRAMEWORK_BINARY by key<Boolean>("Do not generate binary in framework.", cachePolicy = NOT_PROPAGATED)
    val COMPILE_FROM_BITCODE by key<String>("Path to bitcode file to compile.", cachePolicy = NOT_PROPAGATED)
    val SERIALIZED_DEPENDENCIES by key<String>("Path to serialized dependencies for native linking.", cachePolicy = NOT_PROPAGATED)
    val SAVE_DEPENDENCIES_PATH by key<String>("Path to save serialized dependencies to.", cachePolicy = NOT_PROPAGATED)
    val SAVE_LLVM_IR_DIRECTORY by key<String>("Directory to store LLVM IR from phases.", cachePolicy = NOT_PROPAGATED)
    val KONAN_DATA_DIR by key<String>("Directory for storing konan dependencies, cache and prebuilds.", cachePolicy = PROPAGATED)
    val KONAN_MANIFEST_NATIVE_TARGETS by key<List<KonanTarget>>("Value of native_targets property to write in manifest.", cachePolicy = NOT_PROPAGATED)
    val LLVM_MODULE_PASSES by key<String>("LLVM passes to run instead of module optimization pipeline.", cachePolicy = AFFECTS_CODEGEN)
    val LLVM_LTO_PASSES by key<String>("LLVM passes to run instead of LTO optimization pipeline.", cachePolicy = AFFECTS_CODEGEN)
    val SERIALIZED_KLIB_DAG by key<SerializedKlibDAG>(
        "A wrapper over the DAG of libraries that the compiler needs to build static caches for." +
                " It's saved in the compiler configuration for spawned static cache compilation to avoid recomputing the DAG.",
        cachePolicy = NOT_PROPAGATED,
    )
}
