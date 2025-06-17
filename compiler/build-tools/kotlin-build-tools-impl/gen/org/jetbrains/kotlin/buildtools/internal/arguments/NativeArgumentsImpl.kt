// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.ENABLE_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.ENTRY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.G
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.GENERATE_NO_EXIT_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.GENERATE_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.GENERATE_WORKER_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.INCLUDE_BINARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.LIBRARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.LIBRARY_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.LINKER_OPTION
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.LINKER_OPTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.LIST_TARGETS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.MANIFEST
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.MEMORY_MODEL
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.NATIVE_LIBRARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.NOMAIN
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.NOSTDLIB
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.NO_DEFAULT_LIBS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.NO_ENDORSED_LIBS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.OPT
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.OUTPUT
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.PRODUCE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_ADD_CACHE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_ADD_LIGHT_DEBUG
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_ALLOCATOR
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_AUTO_CACHE_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_AUTO_CACHE_FROM
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_BACKEND_THREADS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_BINARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_BUNDLE_ID
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_CACHED_LIBRARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_CACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_CHECK_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_CHECK_STATE_AT_EXTERNAL_CALLS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_COMPILE_FROM_BITCODE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_DEBUG_INFO_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_DEBUG_PREFIX_MAP
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_DESTROY_RUNTIME_MODE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_DUMP_TESTS_TO
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_EMIT_LAZY_OBJC_HEADER
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_EXPORT_KDOC
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_EXPORT_LIBRARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_EXTERNAL_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_FAKE_OVERRIDE_VALIDATOR
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_FILE_TO_CACHE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_FRAMEWORK_IMPORT_HEADER
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_G0
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_GC
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_G_GENERATE_DEBUG_TRAMPOLINE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_HEADER_KLIB_PATH
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_IC_CACHE_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_INCLUDE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_IR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_KONAN_DATA_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_LAZY_IR_FOR_CACHES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_LLVM_LTO_PASSES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_LLVM_MODULE_PASSES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_LLVM_VARIANT
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_MAKE_PER_FILE_CACHE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_MANIFEST_NATIVE_TARGETS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_NO_OBJC_GENERICS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_OMIT_FRAMEWORK_BINARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_OVERRIDE_CLANG_OPTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_OVERRIDE_KONAN_PROPERTIES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_PRE_LINK_CACHES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_PRINT_BITCODE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_PRINT_FILES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_PRINT_IR
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_PURGE_USER_LIBS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_READ_DEPENDENCIES_FROM
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_REFINES_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_RUNTIME
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_RUNTIME_LOGS
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_SAVE_LLVM_IR_AFTER
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_SAVE_LLVM_IR_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_SHORT_MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_STATIC_FRAMEWORK
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_TEMPORARY_FILES_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_VERIFY_BITCODE
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_VERIFY_COMPILER
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_WORKER_EXCEPTION_HANDLING
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments.Companion.X_WRITE_DEPENDENCIES_TO
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments

public class NativeArgumentsImpl : CommonKlibBasedArgumentsImpl(), NativeArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: NativeArguments.NativeArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: NativeArguments.NativeArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: NativeArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: NativeArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: NativeArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2NativeCompilerArguments = K2NativeCompilerArguments()): K2NativeCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("ENABLE_ASSERTIONS" in optionsMap) { arguments.enableAssertions = get(ENABLE_ASSERTIONS) }
    if ("G" in optionsMap) { arguments.debug = get(G) }
    if ("GENERATE_TEST_RUNNER" in optionsMap) { arguments.generateTestRunner = get(GENERATE_TEST_RUNNER) }
    if ("GENERATE_WORKER_TEST_RUNNER" in optionsMap) { arguments.generateWorkerTestRunner = get(GENERATE_WORKER_TEST_RUNNER) }
    if ("GENERATE_NO_EXIT_TEST_RUNNER" in optionsMap) { arguments.generateNoExitTestRunner = get(GENERATE_NO_EXIT_TEST_RUNNER) }
    if ("INCLUDE_BINARY" in optionsMap) { arguments.includeBinaries = get(INCLUDE_BINARY) }
    if ("LIBRARY" in optionsMap) { arguments.libraries = get(LIBRARY) }
    if ("LIBRARY_VERSION" in optionsMap) { arguments.libraryVersion = get(LIBRARY_VERSION) }
    if ("LIST_TARGETS" in optionsMap) { arguments.listTargets = get(LIST_TARGETS) }
    if ("MANIFEST" in optionsMap) { arguments.manifestFile = get(MANIFEST) }
    if ("MEMORY_MODEL" in optionsMap) { arguments.memoryModel = get(MEMORY_MODEL) }
    if ("MODULE_NAME" in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if ("NATIVE_LIBRARY" in optionsMap) { arguments.nativeLibraries = get(NATIVE_LIBRARY) }
    if ("NO_DEFAULT_LIBS" in optionsMap) { arguments.nodefaultlibs = get(NO_DEFAULT_LIBS) }
    if ("NO_ENDORSED_LIBS" in optionsMap) { arguments.noendorsedlibs = get(NO_ENDORSED_LIBS) }
    if ("NOMAIN" in optionsMap) { arguments.nomain = get(NOMAIN) }
    if ("NOPACK" in optionsMap) { arguments.nopack = get(NOPACK) }
    if ("LINKER_OPTIONS" in optionsMap) { arguments.linkerArguments = get(LINKER_OPTIONS) }
    if ("LINKER_OPTION" in optionsMap) { arguments.singleLinkerArguments = get(LINKER_OPTION) }
    if ("NOSTDLIB" in optionsMap) { arguments.nostdlib = get(NOSTDLIB) }
    if ("OPT" in optionsMap) { arguments.optimization = get(OPT) }
    if ("OUTPUT" in optionsMap) { arguments.outputName = get(OUTPUT) }
    if ("ENTRY" in optionsMap) { arguments.mainPackage = get(ENTRY) }
    if ("PRODUCE" in optionsMap) { arguments.produce = get(PRODUCE) }
    if ("TARGET" in optionsMap) { arguments.target = get(TARGET) }
    if ("X_BUNDLE_ID" in optionsMap) { arguments.bundleId = get(X_BUNDLE_ID) }
    if ("X_CACHE_DIRECTORY" in optionsMap) { arguments.cacheDirectories = get(X_CACHE_DIRECTORY) }
    if ("X_CACHED_LIBRARY" in optionsMap) { arguments.cachedLibraries = get(X_CACHED_LIBRARY) }
    if ("X_AUTO_CACHE_FROM" in optionsMap) { arguments.autoCacheableFrom = get(X_AUTO_CACHE_FROM) }
    if ("X_AUTO_CACHE_DIR" in optionsMap) { arguments.autoCacheDir = get(X_AUTO_CACHE_DIR) }
    if ("X_IC_CACHE_DIR" in optionsMap) { arguments.incrementalCacheDir = get(X_IC_CACHE_DIR) }
    if ("X_CHECK_DEPENDENCIES" in optionsMap) { arguments.checkDependencies = get(X_CHECK_DEPENDENCIES) }
    if ("X_EMIT_LAZY_OBJC_HEADER" in optionsMap) { arguments.emitLazyObjCHeader = get(X_EMIT_LAZY_OBJC_HEADER) }
    if ("X_EXPORT_LIBRARY" in optionsMap) { arguments.exportedLibraries = get(X_EXPORT_LIBRARY) }
    if ("X_EXTERNAL_DEPENDENCIES" in optionsMap) { arguments.externalDependencies = get(X_EXTERNAL_DEPENDENCIES) }
    if ("X_FAKE_OVERRIDE_VALIDATOR" in optionsMap) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR) }
    if ("X_FRAMEWORK_IMPORT_HEADER" in optionsMap) { arguments.frameworkImportHeaders = get(X_FRAMEWORK_IMPORT_HEADER) }
    if ("X_ADD_LIGHT_DEBUG" in optionsMap) { arguments.lightDebugString = get(X_ADD_LIGHT_DEBUG) }
    if ("X_G0" in optionsMap) { arguments.lightDebugDeprecated = get(X_G0) }
    if ("X_G_GENERATE_DEBUG_TRAMPOLINE" in optionsMap) { arguments.generateDebugTrampolineString = get(X_G_GENERATE_DEBUG_TRAMPOLINE) }
    if ("X_ADD_CACHE" in optionsMap) { arguments.libraryToAddToCache = get(X_ADD_CACHE) }
    if ("X_FILE_TO_CACHE" in optionsMap) { arguments.filesToCache = get(X_FILE_TO_CACHE) }
    if ("X_MAKE_PER_FILE_CACHE" in optionsMap) { arguments.makePerFileCache = get(X_MAKE_PER_FILE_CACHE) }
    if ("X_BACKEND_THREADS" in optionsMap) { arguments.backendThreads = get(X_BACKEND_THREADS).toString() }
    if ("X_EXPORT_KDOC" in optionsMap) { arguments.exportKDoc = get(X_EXPORT_KDOC) }
    if ("X_PRINT_BITCODE" in optionsMap) { arguments.printBitCode = get(X_PRINT_BITCODE) }
    if ("X_CHECK_STATE_AT_EXTERNAL_CALLS" in optionsMap) { arguments.checkExternalCalls = get(X_CHECK_STATE_AT_EXTERNAL_CALLS) }
    if ("X_PRINT_IR" in optionsMap) { arguments.printIr = get(X_PRINT_IR) }
    if ("X_PRINT_FILES" in optionsMap) { arguments.printFiles = get(X_PRINT_FILES) }
    if ("X_PURGE_USER_LIBS" in optionsMap) { arguments.purgeUserLibs = get(X_PURGE_USER_LIBS) }
    if ("X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO" in optionsMap) { arguments.writeDependenciesOfProducedKlibTo = get(X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO) }
    if ("X_RUNTIME" in optionsMap) { arguments.runtimeFile = get(X_RUNTIME) }
    if ("X_INCLUDE" in optionsMap) { arguments.includes = get(X_INCLUDE) }
    if ("X_SHORT_MODULE_NAME" in optionsMap) { arguments.shortModuleName = get(X_SHORT_MODULE_NAME) }
    if ("X_STATIC_FRAMEWORK" in optionsMap) { arguments.staticFramework = get(X_STATIC_FRAMEWORK) }
    if ("X_TEMPORARY_FILES_DIR" in optionsMap) { arguments.temporaryFilesDir = get(X_TEMPORARY_FILES_DIR) }
    if ("X_SAVE_LLVM_IR_AFTER" in optionsMap) { arguments.saveLlvmIrAfter = get(X_SAVE_LLVM_IR_AFTER) }
    if ("X_VERIFY_BITCODE" in optionsMap) { arguments.verifyBitCode = get(X_VERIFY_BITCODE) }
    if ("X_VERIFY_COMPILER" in optionsMap) { arguments.verifyCompiler = get(X_VERIFY_COMPILER) }
    if ("FRIEND_MODULES" in optionsMap) { arguments.friendModules = get(FRIEND_MODULES) }
    if ("X_REFINES_PATHS" in optionsMap) { arguments.refinesPaths = get(X_REFINES_PATHS) }
    if ("X_DEBUG_INFO_VERSION" in optionsMap) { arguments.debugInfoFormatVersion = get(X_DEBUG_INFO_VERSION).toString() }
    if ("X_NO_OBJC_GENERICS" in optionsMap) { arguments.noObjcGenerics = get(X_NO_OBJC_GENERICS) }
    if ("X_OVERRIDE_CLANG_OPTIONS" in optionsMap) { arguments.clangOptions = get(X_OVERRIDE_CLANG_OPTIONS) }
    if ("X_ALLOCATOR" in optionsMap) { arguments.allocator = get(X_ALLOCATOR) }
    if ("X_HEADER_KLIB_PATH" in optionsMap) { arguments.headerKlibPath = get(X_HEADER_KLIB_PATH) }
    if ("X_DEBUG_PREFIX_MAP" in optionsMap) { arguments.debugPrefixMap = get(X_DEBUG_PREFIX_MAP) }
    if ("X_PRE_LINK_CACHES" in optionsMap) { arguments.preLinkCaches = get(X_PRE_LINK_CACHES) }
    if ("X_OVERRIDE_KONAN_PROPERTIES" in optionsMap) { arguments.overrideKonanProperties = get(X_OVERRIDE_KONAN_PROPERTIES) }
    if ("X_DESTROY_RUNTIME_MODE" in optionsMap) { arguments.destroyRuntimeMode = get(X_DESTROY_RUNTIME_MODE) }
    if ("X_GC" in optionsMap) { arguments.gc = get(X_GC) }
    if ("X_IR_PROPERTY_LAZY_INITIALIZATION" in optionsMap) { arguments.propertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION) }
    if ("X_WORKER_EXCEPTION_HANDLING" in optionsMap) { arguments.workerExceptionHandling = get(X_WORKER_EXCEPTION_HANDLING) }
    if ("X_LLVM_VARIANT" in optionsMap) { arguments.llvmVariant = get(X_LLVM_VARIANT) }
    if ("X_BINARY" in optionsMap) { arguments.binaryOptions = get(X_BINARY) }
    if ("X_RUNTIME_LOGS" in optionsMap) { arguments.runtimeLogs = get(X_RUNTIME_LOGS) }
    if ("X_DUMP_TESTS_TO" in optionsMap) { arguments.testDumpOutputPath = get(X_DUMP_TESTS_TO) }
    if ("X_LAZY_IR_FOR_CACHES" in optionsMap) { arguments.lazyIrForCaches = get(X_LAZY_IR_FOR_CACHES) }
    if ("X_OMIT_FRAMEWORK_BINARY" in optionsMap) { arguments.omitFrameworkBinary = get(X_OMIT_FRAMEWORK_BINARY) }
    if ("X_COMPILE_FROM_BITCODE" in optionsMap) { arguments.compileFromBitcode = get(X_COMPILE_FROM_BITCODE) }
    if ("X_READ_DEPENDENCIES_FROM" in optionsMap) { arguments.serializedDependencies = get(X_READ_DEPENDENCIES_FROM) }
    if ("X_WRITE_DEPENDENCIES_TO" in optionsMap) { arguments.saveDependenciesPath = get(X_WRITE_DEPENDENCIES_TO) }
    if ("X_SAVE_LLVM_IR_DIRECTORY" in optionsMap) { arguments.saveLlvmIrDirectory = get(X_SAVE_LLVM_IR_DIRECTORY) }
    if ("X_KONAN_DATA_DIR" in optionsMap) { arguments.konanDataDir = get(X_KONAN_DATA_DIR) }
    if ("X_LLVM_MODULE_PASSES" in optionsMap) { arguments.llvmModulePasses = get(X_LLVM_MODULE_PASSES) }
    if ("X_LLVM_LTO_PASSES" in optionsMap) { arguments.llvmLTOPasses = get(X_LLVM_LTO_PASSES) }
    if ("X_MANIFEST_NATIVE_TARGETS" in optionsMap) { arguments.manifestNativeTargets = get(X_MANIFEST_NATIVE_TARGETS) }
    return arguments
  }

  /**
   * Base class for [NativeArguments] options.
   *
   * @see get
   * @see set    
   */
  public class NativeArgument<V>(
    public val id: String,
  )

  public companion object {
    public val ENABLE_ASSERTIONS: NativeArgument<Boolean> = NativeArgument("ENABLE_ASSERTIONS")

    public val G: NativeArgument<Boolean> = NativeArgument("G")

    public val GENERATE_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_TEST_RUNNER")

    public val GENERATE_WORKER_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_WORKER_TEST_RUNNER")

    public val GENERATE_NO_EXIT_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_NO_EXIT_TEST_RUNNER")

    public val INCLUDE_BINARY: NativeArgument<Array<String>?> = NativeArgument("INCLUDE_BINARY")

    public val LIBRARY: NativeArgument<Array<String>?> = NativeArgument("LIBRARY")

    public val LIBRARY_VERSION: NativeArgument<String?> = NativeArgument("LIBRARY_VERSION")

    public val LIST_TARGETS: NativeArgument<Boolean> = NativeArgument("LIST_TARGETS")

    public val MANIFEST: NativeArgument<String?> = NativeArgument("MANIFEST")

    public val MEMORY_MODEL: NativeArgument<String?> = NativeArgument("MEMORY_MODEL")

    public val MODULE_NAME: NativeArgument<String?> = NativeArgument("MODULE_NAME")

    public val NATIVE_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("NATIVE_LIBRARY")

    public val NO_DEFAULT_LIBS: NativeArgument<Boolean> = NativeArgument("NO_DEFAULT_LIBS")

    public val NO_ENDORSED_LIBS: NativeArgument<Boolean> = NativeArgument("NO_ENDORSED_LIBS")

    public val NOMAIN: NativeArgument<Boolean> = NativeArgument("NOMAIN")

    public val NOPACK: NativeArgument<Boolean> = NativeArgument("NOPACK")

    public val LINKER_OPTIONS: NativeArgument<Array<String>?> = NativeArgument("LINKER_OPTIONS")

    public val LINKER_OPTION: NativeArgument<Array<String>?> = NativeArgument("LINKER_OPTION")

    public val NOSTDLIB: NativeArgument<Boolean> = NativeArgument("NOSTDLIB")

    public val OPT: NativeArgument<Boolean> = NativeArgument("OPT")

    public val OUTPUT: NativeArgument<String?> = NativeArgument("OUTPUT")

    public val ENTRY: NativeArgument<String?> = NativeArgument("ENTRY")

    public val PRODUCE: NativeArgument<String?> = NativeArgument("PRODUCE")

    public val TARGET: NativeArgument<String?> = NativeArgument("TARGET")

    public val X_BUNDLE_ID: NativeArgument<String?> = NativeArgument("X_BUNDLE_ID")

    public val X_CACHE_DIRECTORY: NativeArgument<Array<String>?> =
        NativeArgument("X_CACHE_DIRECTORY")

    public val X_CACHED_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("X_CACHED_LIBRARY")

    public val X_AUTO_CACHE_FROM: NativeArgument<Array<String>?> =
        NativeArgument("X_AUTO_CACHE_FROM")

    public val X_AUTO_CACHE_DIR: NativeArgument<String?> = NativeArgument("X_AUTO_CACHE_DIR")

    public val X_IC_CACHE_DIR: NativeArgument<String?> = NativeArgument("X_IC_CACHE_DIR")

    public val X_CHECK_DEPENDENCIES: NativeArgument<Boolean> =
        NativeArgument("X_CHECK_DEPENDENCIES")

    public val X_EMIT_LAZY_OBJC_HEADER: NativeArgument<String?> =
        NativeArgument("X_EMIT_LAZY_OBJC_HEADER")

    public val X_EXPORT_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("X_EXPORT_LIBRARY")

    public val X_EXTERNAL_DEPENDENCIES: NativeArgument<String?> =
        NativeArgument("X_EXTERNAL_DEPENDENCIES")

    public val X_FAKE_OVERRIDE_VALIDATOR: NativeArgument<Boolean> =
        NativeArgument("X_FAKE_OVERRIDE_VALIDATOR")

    public val X_FRAMEWORK_IMPORT_HEADER: NativeArgument<Array<String>?> =
        NativeArgument("X_FRAMEWORK_IMPORT_HEADER")

    public val X_ADD_LIGHT_DEBUG: NativeArgument<String?> = NativeArgument("X_ADD_LIGHT_DEBUG")

    public val X_G0: NativeArgument<Boolean> = NativeArgument("X_G0")

    public val X_G_GENERATE_DEBUG_TRAMPOLINE: NativeArgument<String?> =
        NativeArgument("X_G_GENERATE_DEBUG_TRAMPOLINE")

    public val X_ADD_CACHE: NativeArgument<String?> = NativeArgument("X_ADD_CACHE")

    public val X_FILE_TO_CACHE: NativeArgument<Array<String>?> = NativeArgument("X_FILE_TO_CACHE")

    public val X_MAKE_PER_FILE_CACHE: NativeArgument<Boolean> =
        NativeArgument("X_MAKE_PER_FILE_CACHE")

    public val X_BACKEND_THREADS: NativeArgument<Int> = NativeArgument("X_BACKEND_THREADS")

    public val X_EXPORT_KDOC: NativeArgument<Boolean> = NativeArgument("X_EXPORT_KDOC")

    public val X_PRINT_BITCODE: NativeArgument<Boolean> = NativeArgument("X_PRINT_BITCODE")

    public val X_CHECK_STATE_AT_EXTERNAL_CALLS: NativeArgument<Boolean> =
        NativeArgument("X_CHECK_STATE_AT_EXTERNAL_CALLS")

    public val X_PRINT_IR: NativeArgument<Boolean> = NativeArgument("X_PRINT_IR")

    public val X_PRINT_FILES: NativeArgument<Boolean> = NativeArgument("X_PRINT_FILES")

    public val X_PURGE_USER_LIBS: NativeArgument<Boolean> = NativeArgument("X_PURGE_USER_LIBS")

    public val X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO: NativeArgument<String?> =
        NativeArgument("X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    public val X_RUNTIME: NativeArgument<String?> = NativeArgument("X_RUNTIME")

    public val X_INCLUDE: NativeArgument<Array<String>?> = NativeArgument("X_INCLUDE")

    public val X_SHORT_MODULE_NAME: NativeArgument<String?> = NativeArgument("X_SHORT_MODULE_NAME")

    public val X_STATIC_FRAMEWORK: NativeArgument<Boolean> = NativeArgument("X_STATIC_FRAMEWORK")

    public val X_TEMPORARY_FILES_DIR: NativeArgument<String?> =
        NativeArgument("X_TEMPORARY_FILES_DIR")

    public val X_SAVE_LLVM_IR_AFTER: NativeArgument<Array<String>?> =
        NativeArgument("X_SAVE_LLVM_IR_AFTER")

    public val X_VERIFY_BITCODE: NativeArgument<Boolean> = NativeArgument("X_VERIFY_BITCODE")

    public val X_VERIFY_COMPILER: NativeArgument<String?> = NativeArgument("X_VERIFY_COMPILER")

    public val FRIEND_MODULES: NativeArgument<String?> = NativeArgument("FRIEND_MODULES")

    public val X_REFINES_PATHS: NativeArgument<Array<String>?> = NativeArgument("X_REFINES_PATHS")

    public val X_DEBUG_INFO_VERSION: NativeArgument<Int> = NativeArgument("X_DEBUG_INFO_VERSION")

    public val X_NO_OBJC_GENERICS: NativeArgument<Boolean> = NativeArgument("X_NO_OBJC_GENERICS")

    public val X_OVERRIDE_CLANG_OPTIONS: NativeArgument<Array<String>?> =
        NativeArgument("X_OVERRIDE_CLANG_OPTIONS")

    public val X_ALLOCATOR: NativeArgument<String?> = NativeArgument("X_ALLOCATOR")

    public val X_HEADER_KLIB_PATH: NativeArgument<String?> = NativeArgument("X_HEADER_KLIB_PATH")

    public val X_DEBUG_PREFIX_MAP: NativeArgument<Array<String>?> =
        NativeArgument("X_DEBUG_PREFIX_MAP")

    public val X_PRE_LINK_CACHES: NativeArgument<String?> = NativeArgument("X_PRE_LINK_CACHES")

    public val X_OVERRIDE_KONAN_PROPERTIES: NativeArgument<Array<String>?> =
        NativeArgument("X_OVERRIDE_KONAN_PROPERTIES")

    public val X_DESTROY_RUNTIME_MODE: NativeArgument<String?> =
        NativeArgument("X_DESTROY_RUNTIME_MODE")

    public val X_GC: NativeArgument<String?> = NativeArgument("X_GC")

    public val X_IR_PROPERTY_LAZY_INITIALIZATION: NativeArgument<String?> =
        NativeArgument("X_IR_PROPERTY_LAZY_INITIALIZATION")

    public val X_WORKER_EXCEPTION_HANDLING: NativeArgument<String?> =
        NativeArgument("X_WORKER_EXCEPTION_HANDLING")

    public val X_LLVM_VARIANT: NativeArgument<String?> = NativeArgument("X_LLVM_VARIANT")

    public val X_BINARY: NativeArgument<Array<String>?> = NativeArgument("X_BINARY")

    public val X_RUNTIME_LOGS: NativeArgument<String?> = NativeArgument("X_RUNTIME_LOGS")

    public val X_DUMP_TESTS_TO: NativeArgument<String?> = NativeArgument("X_DUMP_TESTS_TO")

    public val X_LAZY_IR_FOR_CACHES: NativeArgument<String?> =
        NativeArgument("X_LAZY_IR_FOR_CACHES")

    public val X_OMIT_FRAMEWORK_BINARY: NativeArgument<Boolean> =
        NativeArgument("X_OMIT_FRAMEWORK_BINARY")

    public val X_COMPILE_FROM_BITCODE: NativeArgument<String?> =
        NativeArgument("X_COMPILE_FROM_BITCODE")

    public val X_READ_DEPENDENCIES_FROM: NativeArgument<String?> =
        NativeArgument("X_READ_DEPENDENCIES_FROM")

    public val X_WRITE_DEPENDENCIES_TO: NativeArgument<String?> =
        NativeArgument("X_WRITE_DEPENDENCIES_TO")

    public val X_SAVE_LLVM_IR_DIRECTORY: NativeArgument<String?> =
        NativeArgument("X_SAVE_LLVM_IR_DIRECTORY")

    public val X_KONAN_DATA_DIR: NativeArgument<String?> = NativeArgument("X_KONAN_DATA_DIR")

    public val X_LLVM_MODULE_PASSES: NativeArgument<String?> =
        NativeArgument("X_LLVM_MODULE_PASSES")

    public val X_LLVM_LTO_PASSES: NativeArgument<String?> = NativeArgument("X_LLVM_LTO_PASSES")

    public val X_MANIFEST_NATIVE_TARGETS: NativeArgument<Array<String>?> =
        NativeArgument("X_MANIFEST_NATIVE_TARGETS")
  }
}
