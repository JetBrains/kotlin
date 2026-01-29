/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.backend.konan

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

object KonanConfigKeys {
    // Bundle ID to be set in Info.plist of a produced framework.
    @JvmField
    val BUNDLE_ID = CompilerConfigurationKey.create<String>("BUNDLE_ID")

    // Check dependencies and download the missing ones.
    @JvmField
    val CHECK_DEPENDENCIES = CompilerConfigurationKey.create<Boolean>("CHECK_DEPENDENCIES")

    @JvmField
    val DEBUG = CompilerConfigurationKey.create<Boolean>("DEBUG")

    @JvmField
    val FAKE_OVERRIDE_VALIDATOR = CompilerConfigurationKey.create<Boolean>("FAKE_OVERRIDE_VALIDATOR")

    @JvmField
    val EMIT_LAZY_OBJC_HEADER_FILE = CompilerConfigurationKey.create<String>("EMIT_LAZY_OBJC_HEADER_FILE")

    @JvmField
    val ENABLE_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("ENABLE_ASSERTIONS")

    // Fully qualified main() name.
    @JvmField
    val ENTRY = CompilerConfigurationKey.create<String>("ENTRY")

    // Libraries included into produced framework API.
    @JvmField
    val EXPORTED_LIBRARIES = CompilerConfigurationKey.create<List<String>>("EXPORTED_LIBRARIES")

    // Prefix used when exporting Kotlin names to other languages.
    @JvmField
    val FULL_EXPORTED_NAME_PREFIX = CompilerConfigurationKey.create<String>("FULL_EXPORTED_NAME_PREFIX")

    @JvmField
    val LIBRARY_TO_ADD_TO_CACHE = CompilerConfigurationKey.create<String>("LIBRARY_TO_ADD_TO_CACHE")

    @JvmField
    val CACHE_DIRECTORIES = CompilerConfigurationKey.create<List<String>>("CACHE_DIRECTORIES")

    // Paths to the root directories from which dependencies are to be cached automatically.
    @JvmField
    val AUTO_CACHEABLE_FROM = CompilerConfigurationKey.create<List<String>>("AUTO_CACHEABLE_FROM")

    // Path to the directory where to put caches for auto-cacheable dependencies.
    @JvmField
    val AUTO_CACHE_DIR = CompilerConfigurationKey.create<String>("AUTO_CACHE_DIR")

    // Path to the directory where to put incremental build caches.
    @JvmField
    val INCREMENTAL_CACHE_DIR = CompilerConfigurationKey.create<String>("INCREMENTAL_CACHE_DIR")

    // Mapping from library paths to cache paths.
    @JvmField
    val CACHED_LIBRARIES = CompilerConfigurationKey.create<Map<String, String>>("CACHED_LIBRARIES")

    // Which files should be compiled to cache.
    @JvmField
    val FILES_TO_CACHE = CompilerConfigurationKey.create<List<String>>("FILES_TO_CACHE")

    @JvmField
    val MAKE_PER_FILE_CACHE = CompilerConfigurationKey.create<Boolean>("MAKE_PER_FILE_CACHE")

    @JvmField
    val FRAMEWORK_IMPORT_HEADERS = CompilerConfigurationKey.create<List<String>>("FRAMEWORK_IMPORT_HEADERS")

    @JvmField
    val FRIEND_MODULES = CompilerConfigurationKey.create<List<String>>("FRIEND_MODULES")

    @JvmField
    val REFINES_MODULES = CompilerConfigurationKey.create<List<String>>("REFINES_MODULES")

    @JvmField
    val GENERATE_TEST_RUNNER = CompilerConfigurationKey.create<TestRunnerKind>("GENERATE_TEST_RUNNER")

    @JvmField
    val INCLUDED_BINARY_FILES = CompilerConfigurationKey.create<List<String>>("INCLUDED_BINARY_FILES")

    @JvmField
    val KONAN_HOME = CompilerConfigurationKey.create<String>("KONAN_HOME")

    @JvmField
    val LIBRARY_FILES = CompilerConfigurationKey.create<List<String>>("LIBRARY_FILES")

    @JvmField
    val LIGHT_DEBUG = CompilerConfigurationKey.create<Boolean>("LIGHT_DEBUG")

    // Generates debug trampolines to make debugger breakpoint resolution more accurate.
    @JvmField
    val GENERATE_DEBUG_TRAMPOLINE = CompilerConfigurationKey.create<Boolean>("GENERATE_DEBUG_TRAMPOLINE")

    @JvmField
    val LINKER_ARGS = CompilerConfigurationKey.create<List<String>>("LINKER_ARGS")

    @JvmField
    val LIST_TARGETS = CompilerConfigurationKey.create<Boolean>("LIST_TARGETS")

    @JvmField
    val MANIFEST_FILE = CompilerConfigurationKey.create<String>("MANIFEST_FILE")

    // Path to file where header klib should be produced.
    @JvmField
    val HEADER_KLIB = CompilerConfigurationKey.create<String>("HEADER_KLIB")

    @JvmField
    val MODULE_NAME = CompilerConfigurationKey.create<String>("MODULE_NAME")

    @JvmField
    val NATIVE_LIBRARY_FILES = CompilerConfigurationKey.create<List<String>>("NATIVE_LIBRARY_FILES")

    // Don't link with the default libraries.
    @JvmField
    val NODEFAULTLIBS = CompilerConfigurationKey.create<Boolean>("NODEFAULTLIBS")

    // Don't link with the endorsed libraries.
    @JvmField
    val NOENDORSEDLIBS = CompilerConfigurationKey.create<Boolean>("NOENDORSEDLIBS")

    // Assume 'main' entry point to be provided by external libraries.
    @JvmField
    val NOMAIN = CompilerConfigurationKey.create<Boolean>("NOMAIN")

    // Don't link with stdlib.
    @JvmField
    val NOSTDLIB = CompilerConfigurationKey.create<Boolean>("NOSTDLIB")

    // Don't pack the library into a klib file.
    @JvmField
    val NOPACK = CompilerConfigurationKey.create<Boolean>("NOPACK")

    @JvmField
    val OPTIMIZATION = CompilerConfigurationKey.create<Boolean>("OPTIMIZATION")

    @JvmField
    val OUTPUT = CompilerConfigurationKey.create<String>("OUTPUT")

    @JvmField
    val OVERRIDE_CLANG_OPTIONS = CompilerConfigurationKey.create<List<String>>("OVERRIDE_CLANG_OPTIONS")

    @JvmField
    val ALLOCATION_MODE = CompilerConfigurationKey.create<AllocationMode>("ALLOCATION_MODE")

    @JvmField
    val PRINT_BITCODE = CompilerConfigurationKey.create<Boolean>("PRINT_BITCODE")

    @JvmField
    val PRINT_IR = CompilerConfigurationKey.create<Boolean>("PRINT_IR")

    @JvmField
    val PRINT_FILES = CompilerConfigurationKey.create<Boolean>("PRINT_FILES")

    @JvmField
    val PRODUCE = CompilerConfigurationKey.create<CompilerOutputKind>("PRODUCE")

    @JvmField
    val PURGE_USER_LIBS = CompilerConfigurationKey.create<Boolean>("PURGE_USER_LIBS")

    @JvmField
    val RUNTIME_FILE = CompilerConfigurationKey.create<String>("RUNTIME_FILE")

    // Klibs processed in the same manner as source files.
    @JvmField
    val INCLUDED_LIBRARIES = CompilerConfigurationKey.create<List<String>>("INCLUDED_LIBRARIES")

    // Short module name for IDE and export.
    @JvmField
    val SHORT_MODULE_NAME = CompilerConfigurationKey.create<String>("SHORT_MODULE_NAME")

    // Produce a static library for a framework.
    @JvmField
    val STATIC_FRAMEWORK = CompilerConfigurationKey.create<Boolean>("STATIC_FRAMEWORK")

    @JvmField
    val TARGET = CompilerConfigurationKey.create<String>("TARGET")

    @JvmField
    val TEMPORARY_FILES_DIR = CompilerConfigurationKey.create<String>("TEMPORARY_FILES_DIR")

    @JvmField
    val SAVE_LLVM_IR = CompilerConfigurationKey.create<List<String>>("SAVE_LLVM_IR")

    @JvmField
    val VERIFY_BITCODE = CompilerConfigurationKey.create<Boolean>("VERIFY_BITCODE")

    @JvmField
    val VERIFY_COMPILER = CompilerConfigurationKey.create<Boolean>("VERIFY_COMPILER")

    @JvmField
    val WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO = CompilerConfigurationKey.create<String>("WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    @JvmField
    val DEBUG_INFO_VERSION = CompilerConfigurationKey.create<Int>("DEBUG_INFO_VERSION")

    // Write objc header with generics support.
    @JvmField
    val OBJC_GENERICS = CompilerConfigurationKey.create<Boolean>("OBJC_GENERICS")

    // Remap file source paths in debug info.
    @JvmField
    val DEBUG_PREFIX_MAP = CompilerConfigurationKey.create<Map<String, String>>("DEBUG_PREFIX_MAP")

    // Perform compiler caches pre-link.
    @JvmField
    val PRE_LINK_CACHES = CompilerConfigurationKey.create<Boolean>("PRE_LINK_CACHES")

    // Override konan.properties values.
    @JvmField
    val OVERRIDE_KONAN_PROPERTIES = CompilerConfigurationKey.create<Map<String, String>>("OVERRIDE_KONAN_PROPERTIES")

    @JvmField
    val PROPERTY_LAZY_INITIALIZATION = CompilerConfigurationKey.create<Boolean>("PROPERTY_LAZY_INITIALIZATION")

    // Use external dependencies to enhance IR linker error messages.
    @JvmField
    val EXTERNAL_DEPENDENCIES = CompilerConfigurationKey.create<String>("EXTERNAL_DEPENDENCIES")

    @JvmField
    val LLVM_VARIANT = CompilerConfigurationKey.create<LlvmVariant>("LLVM_VARIANT")

    @JvmField
    val RUNTIME_LOGS = CompilerConfigurationKey.create<String>("RUNTIME_LOGS")

    // Path to a file to dump the list of all available tests.
    @JvmField
    val TEST_DUMP_OUTPUT_PATH = CompilerConfigurationKey.create<String>("TEST_DUMP_OUTPUT_PATH")

    // Do not generate binary in framework.
    @JvmField
    val OMIT_FRAMEWORK_BINARY = CompilerConfigurationKey.create<Boolean>("OMIT_FRAMEWORK_BINARY")

    // Path to bitcode file to compile.
    @JvmField
    val COMPILE_FROM_BITCODE = CompilerConfigurationKey.create<String>("COMPILE_FROM_BITCODE")

    // Path to serialized dependencies for native linking.
    @JvmField
    val SERIALIZED_DEPENDENCIES = CompilerConfigurationKey.create<String>("SERIALIZED_DEPENDENCIES")

    // Path to save serialized dependencies to.
    @JvmField
    val SAVE_DEPENDENCIES_PATH = CompilerConfigurationKey.create<String>("SAVE_DEPENDENCIES_PATH")

    // Directory to store LLVM IR from phases.
    @JvmField
    val SAVE_LLVM_IR_DIRECTORY = CompilerConfigurationKey.create<String>("SAVE_LLVM_IR_DIRECTORY")

    // Directory for storing konan dependencies, cache and prebuilds.
    @JvmField
    val KONAN_DATA_DIR = CompilerConfigurationKey.create<String>("KONAN_DATA_DIR")

    // Value of native_targets property to write in manifest.
    @JvmField
    val MANIFEST_NATIVE_TARGETS = CompilerConfigurationKey.create<List<KonanTarget>>("MANIFEST_NATIVE_TARGETS")

    // LLVM passes to run instead of module optimization pipeline.
    @JvmField
    val LLVM_MODULE_PASSES = CompilerConfigurationKey.create<String>("LLVM_MODULE_PASSES")

    // LLVM passes to run instead of LTO optimization pipeline.
    @JvmField
    val LLVM_LTO_PASSES = CompilerConfigurationKey.create<String>("LLVM_LTO_PASSES")

}

var CompilerConfiguration.bundleId: String?
    get() = get(KonanConfigKeys.BUNDLE_ID)
    set(value) { put(KonanConfigKeys.BUNDLE_ID, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.checkDependencies: Boolean
    get() = getBoolean(KonanConfigKeys.CHECK_DEPENDENCIES)
    set(value) { put(KonanConfigKeys.CHECK_DEPENDENCIES, value) }

var CompilerConfiguration.debug: Boolean
    get() = getBoolean(KonanConfigKeys.DEBUG)
    set(value) { put(KonanConfigKeys.DEBUG, value) }

var CompilerConfiguration.fakeOverrideValidator: Boolean
    get() = getBoolean(KonanConfigKeys.FAKE_OVERRIDE_VALIDATOR)
    set(value) { put(KonanConfigKeys.FAKE_OVERRIDE_VALIDATOR, value) }

var CompilerConfiguration.emitLazyObjcHeaderFile: String?
    get() = get(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE)
    set(value) { put(KonanConfigKeys.EMIT_LAZY_OBJC_HEADER_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.enableAssertions: Boolean
    get() = getBoolean(KonanConfigKeys.ENABLE_ASSERTIONS)
    set(value) { put(KonanConfigKeys.ENABLE_ASSERTIONS, value) }

var CompilerConfiguration.entry: String?
    get() = get(KonanConfigKeys.ENTRY)
    set(value) { put(KonanConfigKeys.ENTRY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.exportedLibraries: List<String>
    get() = getList(KonanConfigKeys.EXPORTED_LIBRARIES)
    set(value) { put(KonanConfigKeys.EXPORTED_LIBRARIES, value) }

var CompilerConfiguration.fullExportedNamePrefix: String?
    get() = get(KonanConfigKeys.FULL_EXPORTED_NAME_PREFIX)
    set(value) { put(KonanConfigKeys.FULL_EXPORTED_NAME_PREFIX, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.libraryToAddToCache: String?
    get() = get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE)
    set(value) { put(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.cacheDirectories: List<String>
    get() = getList(KonanConfigKeys.CACHE_DIRECTORIES)
    set(value) { put(KonanConfigKeys.CACHE_DIRECTORIES, value) }

var CompilerConfiguration.autoCacheableFrom: List<String>
    get() = getList(KonanConfigKeys.AUTO_CACHEABLE_FROM)
    set(value) { put(KonanConfigKeys.AUTO_CACHEABLE_FROM, value) }

var CompilerConfiguration.autoCacheDir: String?
    get() = get(KonanConfigKeys.AUTO_CACHE_DIR)
    set(value) { put(KonanConfigKeys.AUTO_CACHE_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.incrementalCacheDir: String?
    get() = get(KonanConfigKeys.INCREMENTAL_CACHE_DIR)
    set(value) { put(KonanConfigKeys.INCREMENTAL_CACHE_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.cachedLibraries: Map<String, String>
    get() = getMap(KonanConfigKeys.CACHED_LIBRARIES)
    set(value) { put(KonanConfigKeys.CACHED_LIBRARIES, value) }

var CompilerConfiguration.filesToCache: List<String>
    get() = getList(KonanConfigKeys.FILES_TO_CACHE)
    set(value) { put(KonanConfigKeys.FILES_TO_CACHE, value) }

var CompilerConfiguration.makePerFileCache: Boolean
    get() = getBoolean(KonanConfigKeys.MAKE_PER_FILE_CACHE)
    set(value) { put(KonanConfigKeys.MAKE_PER_FILE_CACHE, value) }

var CompilerConfiguration.frameworkImportHeaders: List<String>
    get() = getList(KonanConfigKeys.FRAMEWORK_IMPORT_HEADERS)
    set(value) { put(KonanConfigKeys.FRAMEWORK_IMPORT_HEADERS, value) }

var CompilerConfiguration.friendModules: List<String>
    get() = getList(KonanConfigKeys.FRIEND_MODULES)
    set(value) { put(KonanConfigKeys.FRIEND_MODULES, value) }

var CompilerConfiguration.refinesModules: List<String>
    get() = getList(KonanConfigKeys.REFINES_MODULES)
    set(value) { put(KonanConfigKeys.REFINES_MODULES, value) }

var CompilerConfiguration.generateTestRunner: TestRunnerKind?
    get() = get(KonanConfigKeys.GENERATE_TEST_RUNNER)
    set(value) { put(KonanConfigKeys.GENERATE_TEST_RUNNER, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.includedBinaryFiles: List<String>
    get() = getList(KonanConfigKeys.INCLUDED_BINARY_FILES)
    set(value) { put(KonanConfigKeys.INCLUDED_BINARY_FILES, value) }

var CompilerConfiguration.konanHome: String?
    get() = get(KonanConfigKeys.KONAN_HOME)
    set(value) { put(KonanConfigKeys.KONAN_HOME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.libraryFiles: List<String>
    get() = getList(KonanConfigKeys.LIBRARY_FILES)
    set(value) { put(KonanConfigKeys.LIBRARY_FILES, value) }

var CompilerConfiguration.lightDebug: Boolean
    get() = getBoolean(KonanConfigKeys.LIGHT_DEBUG)
    set(value) { put(KonanConfigKeys.LIGHT_DEBUG, value) }

var CompilerConfiguration.generateDebugTrampoline: Boolean
    get() = getBoolean(KonanConfigKeys.GENERATE_DEBUG_TRAMPOLINE)
    set(value) { put(KonanConfigKeys.GENERATE_DEBUG_TRAMPOLINE, value) }

var CompilerConfiguration.linkerArgs: List<String>
    get() = getList(KonanConfigKeys.LINKER_ARGS)
    set(value) { put(KonanConfigKeys.LINKER_ARGS, value) }

var CompilerConfiguration.listTargets: Boolean
    get() = getBoolean(KonanConfigKeys.LIST_TARGETS)
    set(value) { put(KonanConfigKeys.LIST_TARGETS, value) }

var CompilerConfiguration.manifestFile: String?
    get() = get(KonanConfigKeys.MANIFEST_FILE)
    set(value) { put(KonanConfigKeys.MANIFEST_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.headerKlib: String?
    get() = get(KonanConfigKeys.HEADER_KLIB)
    set(value) { put(KonanConfigKeys.HEADER_KLIB, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.moduleName: String?
    get() = get(KonanConfigKeys.MODULE_NAME)
    set(value) { put(KonanConfigKeys.MODULE_NAME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.nativeLibraryFiles: List<String>
    get() = getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)
    set(value) { put(KonanConfigKeys.NATIVE_LIBRARY_FILES, value) }

var CompilerConfiguration.nodefaultlibs: Boolean
    get() = getBoolean(KonanConfigKeys.NODEFAULTLIBS)
    set(value) { put(KonanConfigKeys.NODEFAULTLIBS, value) }

var CompilerConfiguration.noendorsedlibs: Boolean
    get() = getBoolean(KonanConfigKeys.NOENDORSEDLIBS)
    set(value) { put(KonanConfigKeys.NOENDORSEDLIBS, value) }

var CompilerConfiguration.nomain: Boolean
    get() = getBoolean(KonanConfigKeys.NOMAIN)
    set(value) { put(KonanConfigKeys.NOMAIN, value) }

var CompilerConfiguration.nostdlib: Boolean
    get() = getBoolean(KonanConfigKeys.NOSTDLIB)
    set(value) { put(KonanConfigKeys.NOSTDLIB, value) }

var CompilerConfiguration.nopack: Boolean
    get() = getBoolean(KonanConfigKeys.NOPACK)
    set(value) { put(KonanConfigKeys.NOPACK, value) }

var CompilerConfiguration.optimization: Boolean
    get() = getBoolean(KonanConfigKeys.OPTIMIZATION)
    set(value) { put(KonanConfigKeys.OPTIMIZATION, value) }

var CompilerConfiguration.output: String?
    get() = get(KonanConfigKeys.OUTPUT)
    set(value) { put(KonanConfigKeys.OUTPUT, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.overrideClangOptions: List<String>
    get() = getList(KonanConfigKeys.OVERRIDE_CLANG_OPTIONS)
    set(value) { put(KonanConfigKeys.OVERRIDE_CLANG_OPTIONS, value) }

var CompilerConfiguration.allocationMode: AllocationMode?
    get() = get(KonanConfigKeys.ALLOCATION_MODE)
    set(value) { put(KonanConfigKeys.ALLOCATION_MODE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.printBitcode: Boolean
    get() = getBoolean(KonanConfigKeys.PRINT_BITCODE)
    set(value) { put(KonanConfigKeys.PRINT_BITCODE, value) }

var CompilerConfiguration.printIr: Boolean
    get() = getBoolean(KonanConfigKeys.PRINT_IR)
    set(value) { put(KonanConfigKeys.PRINT_IR, value) }

var CompilerConfiguration.printFiles: Boolean
    get() = getBoolean(KonanConfigKeys.PRINT_FILES)
    set(value) { put(KonanConfigKeys.PRINT_FILES, value) }

var CompilerConfiguration.produce: CompilerOutputKind?
    get() = get(KonanConfigKeys.PRODUCE)
    set(value) { put(KonanConfigKeys.PRODUCE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.purgeUserLibs: Boolean
    get() = getBoolean(KonanConfigKeys.PURGE_USER_LIBS)
    set(value) { put(KonanConfigKeys.PURGE_USER_LIBS, value) }

var CompilerConfiguration.runtimeFile: String?
    get() = get(KonanConfigKeys.RUNTIME_FILE)
    set(value) { put(KonanConfigKeys.RUNTIME_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.includedLibraries: List<String>
    get() = getList(KonanConfigKeys.INCLUDED_LIBRARIES)
    set(value) { put(KonanConfigKeys.INCLUDED_LIBRARIES, value) }

var CompilerConfiguration.shortModuleName: String?
    get() = get(KonanConfigKeys.SHORT_MODULE_NAME)
    set(value) { put(KonanConfigKeys.SHORT_MODULE_NAME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.staticFramework: Boolean
    get() = getBoolean(KonanConfigKeys.STATIC_FRAMEWORK)
    set(value) { put(KonanConfigKeys.STATIC_FRAMEWORK, value) }

var CompilerConfiguration.target: String?
    get() = get(KonanConfigKeys.TARGET)
    set(value) { put(KonanConfigKeys.TARGET, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.temporaryFilesDir: String?
    get() = get(KonanConfigKeys.TEMPORARY_FILES_DIR)
    set(value) { put(KonanConfigKeys.TEMPORARY_FILES_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.saveLlvmIr: List<String>
    get() = getList(KonanConfigKeys.SAVE_LLVM_IR)
    set(value) { put(KonanConfigKeys.SAVE_LLVM_IR, value) }

var CompilerConfiguration.verifyBitcode: Boolean
    get() = getBoolean(KonanConfigKeys.VERIFY_BITCODE)
    set(value) { put(KonanConfigKeys.VERIFY_BITCODE, value) }

var CompilerConfiguration.verifyCompiler: Boolean
    get() = getBoolean(KonanConfigKeys.VERIFY_COMPILER)
    set(value) { put(KonanConfigKeys.VERIFY_COMPILER, value) }

var CompilerConfiguration.writeDependenciesOfProducedKlibTo: String?
    get() = get(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)
    set(value) { put(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.debugInfoVersion: Int?
    get() = get(KonanConfigKeys.DEBUG_INFO_VERSION)
    set(value) { put(KonanConfigKeys.DEBUG_INFO_VERSION, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.objcGenerics: Boolean
    get() = getBoolean(KonanConfigKeys.OBJC_GENERICS)
    set(value) { put(KonanConfigKeys.OBJC_GENERICS, value) }

var CompilerConfiguration.debugPrefixMap: Map<String, String>
    get() = getMap(KonanConfigKeys.DEBUG_PREFIX_MAP)
    set(value) { put(KonanConfigKeys.DEBUG_PREFIX_MAP, value) }

var CompilerConfiguration.preLinkCaches: Boolean
    get() = getBoolean(KonanConfigKeys.PRE_LINK_CACHES)
    set(value) { put(KonanConfigKeys.PRE_LINK_CACHES, value) }

var CompilerConfiguration.overrideKonanProperties: Map<String, String>
    get() = getMap(KonanConfigKeys.OVERRIDE_KONAN_PROPERTIES)
    set(value) { put(KonanConfigKeys.OVERRIDE_KONAN_PROPERTIES, value) }

var CompilerConfiguration.propertyLazyInitialization: Boolean
    get() = getBoolean(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION)
    set(value) { put(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION, value) }

var CompilerConfiguration.externalDependencies: String?
    get() = get(KonanConfigKeys.EXTERNAL_DEPENDENCIES)
    set(value) { put(KonanConfigKeys.EXTERNAL_DEPENDENCIES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.llvmVariant: LlvmVariant?
    get() = get(KonanConfigKeys.LLVM_VARIANT)
    set(value) { put(KonanConfigKeys.LLVM_VARIANT, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.runtimeLogs: String?
    get() = get(KonanConfigKeys.RUNTIME_LOGS)
    set(value) { put(KonanConfigKeys.RUNTIME_LOGS, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.testDumpOutputPath: String?
    get() = get(KonanConfigKeys.TEST_DUMP_OUTPUT_PATH)
    set(value) { put(KonanConfigKeys.TEST_DUMP_OUTPUT_PATH, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.omitFrameworkBinary: Boolean
    get() = getBoolean(KonanConfigKeys.OMIT_FRAMEWORK_BINARY)
    set(value) { put(KonanConfigKeys.OMIT_FRAMEWORK_BINARY, value) }

var CompilerConfiguration.compileFromBitcode: String?
    get() = get(KonanConfigKeys.COMPILE_FROM_BITCODE)
    set(value) { put(KonanConfigKeys.COMPILE_FROM_BITCODE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.serializedDependencies: String?
    get() = get(KonanConfigKeys.SERIALIZED_DEPENDENCIES)
    set(value) { put(KonanConfigKeys.SERIALIZED_DEPENDENCIES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.saveDependenciesPath: String?
    get() = get(KonanConfigKeys.SAVE_DEPENDENCIES_PATH)
    set(value) { put(KonanConfigKeys.SAVE_DEPENDENCIES_PATH, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.saveLlvmIrDirectory: String?
    get() = get(KonanConfigKeys.SAVE_LLVM_IR_DIRECTORY)
    set(value) { put(KonanConfigKeys.SAVE_LLVM_IR_DIRECTORY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanDataDir: String?
    get() = get(KonanConfigKeys.KONAN_DATA_DIR)
    set(value) { put(KonanConfigKeys.KONAN_DATA_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.manifestNativeTargets: List<KonanTarget>
    get() = getList(KonanConfigKeys.MANIFEST_NATIVE_TARGETS)
    set(value) { put(KonanConfigKeys.MANIFEST_NATIVE_TARGETS, value) }

var CompilerConfiguration.llvmModulePasses: String?
    get() = get(KonanConfigKeys.LLVM_MODULE_PASSES)
    set(value) { put(KonanConfigKeys.LLVM_MODULE_PASSES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.llvmLtoPasses: String?
    get() = get(KonanConfigKeys.LLVM_LTO_PASSES)
    set(value) { put(KonanConfigKeys.LLVM_LTO_PASSES, requireNotNull(value) { "nullable values are not allowed" }) }

