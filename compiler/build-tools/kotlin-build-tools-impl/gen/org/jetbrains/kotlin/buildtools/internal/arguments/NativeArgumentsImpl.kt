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
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.ENABLE_ASSERTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.ENTRY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.G
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.GENERATE_NO_EXIT_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.GENERATE_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.GENERATE_WORKER_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.INCLUDE_BINARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.LIBRARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.LIBRARY_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.LINKER_OPTION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.LINKER_OPTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.LIST_TARGETS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.MANIFEST
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.MEMORY_MODEL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.NATIVE_LIBRARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.NOMAIN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.NOSTDLIB
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.NO_DEFAULT_LIBS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.NO_ENDORSED_LIBS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.OPT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.OUTPUT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.PRODUCE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.TARGET
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_ADD_CACHE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_ADD_LIGHT_DEBUG
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_ALLOCATOR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_AUTO_CACHE_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_AUTO_CACHE_FROM
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_BACKEND_THREADS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_BINARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_BUNDLE_ID
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_CACHED_LIBRARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_CACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_CHECK_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_CHECK_STATE_AT_EXTERNAL_CALLS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_COMPILE_FROM_BITCODE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_DEBUG_INFO_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_DEBUG_PREFIX_MAP
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_DESTROY_RUNTIME_MODE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_DUMP_TESTS_TO
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EMIT_LAZY_OBJC_HEADER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EXPORT_KDOC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EXPORT_LIBRARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EXTERNAL_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_FAKE_OVERRIDE_VALIDATOR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_FILE_TO_CACHE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_FRAMEWORK_IMPORT_HEADER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_G0
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_GC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_G_GENERATE_DEBUG_TRAMPOLINE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_HEADER_KLIB_PATH
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_IC_CACHE_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_INCLUDE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_IR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_KONAN_DATA_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_LAZY_IR_FOR_CACHES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_LLVM_LTO_PASSES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_LLVM_MODULE_PASSES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_LLVM_VARIANT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_MAKE_PER_FILE_CACHE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_MANIFEST_NATIVE_TARGETS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_NO_OBJC_GENERICS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_OMIT_FRAMEWORK_BINARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_OVERRIDE_CLANG_OPTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_OVERRIDE_KONAN_PROPERTIES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_PRE_LINK_CACHES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_PRINT_BITCODE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_PRINT_FILES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_PRINT_IR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_PURGE_USER_LIBS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_READ_DEPENDENCIES_FROM
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_REFINES_PATHS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_RUNTIME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_RUNTIME_LOGS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_SAVE_LLVM_IR_AFTER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_SAVE_LLVM_IR_DIRECTORY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_SHORT_MODULE_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_STATIC_FRAMEWORK
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_TEMPORARY_FILES_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_VERIFY_BITCODE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_VERIFY_COMPILER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_WORKER_EXCEPTION_HANDLING
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_WRITE_DEPENDENCIES_TO
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.NativeArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings

internal class NativeArgumentsImpl : CommonKlibBasedArgumentsImpl(), NativeArguments {
  private val internalArguments: MutableSet<String> = mutableSetOf()

  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: NativeArguments.NativeArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: NativeArguments.NativeArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: NativeArguments.NativeArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: NativeArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: NativeArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: NativeArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2NativeCompilerArguments = K2NativeCompilerArguments()): K2NativeCompilerArguments {
    super.toCompilerArguments(arguments)
    try { if ("ENABLE_ASSERTIONS" in optionsMap) { arguments.enableAssertions = get(ENABLE_ASSERTIONS) } } catch (_: NoSuchMethodError) {}
    try { if ("G" in optionsMap) { arguments.debug = get(G) } } catch (_: NoSuchMethodError) {}
    try { if ("GENERATE_TEST_RUNNER" in optionsMap) { arguments.generateTestRunner = get(GENERATE_TEST_RUNNER) } } catch (_: NoSuchMethodError) {}
    try { if ("GENERATE_WORKER_TEST_RUNNER" in optionsMap) { arguments.generateWorkerTestRunner = get(GENERATE_WORKER_TEST_RUNNER) } } catch (_: NoSuchMethodError) {}
    try { if ("GENERATE_NO_EXIT_TEST_RUNNER" in optionsMap) { arguments.generateNoExitTestRunner = get(GENERATE_NO_EXIT_TEST_RUNNER) } } catch (_: NoSuchMethodError) {}
    try { if ("INCLUDE_BINARY" in optionsMap) { arguments.includeBinaries = get(INCLUDE_BINARY) } } catch (_: NoSuchMethodError) {}
    try { if ("LIBRARY" in optionsMap) { arguments.libraries = get(LIBRARY) } } catch (_: NoSuchMethodError) {}
    try { if ("LIBRARY_VERSION" in optionsMap) { arguments.libraryVersion = get(LIBRARY_VERSION) } } catch (_: NoSuchMethodError) {}
    try { if ("LIST_TARGETS" in optionsMap) { arguments.listTargets = get(LIST_TARGETS) } } catch (_: NoSuchMethodError) {}
    try { if ("MANIFEST" in optionsMap) { arguments.manifestFile = get(MANIFEST) } } catch (_: NoSuchMethodError) {}
    try { if ("MEMORY_MODEL" in optionsMap) { arguments.memoryModel = get(MEMORY_MODEL) } } catch (_: NoSuchMethodError) {}
    try { if ("MODULE_NAME" in optionsMap) { arguments.moduleName = get(MODULE_NAME) } } catch (_: NoSuchMethodError) {}
    try { if ("NATIVE_LIBRARY" in optionsMap) { arguments.nativeLibraries = get(NATIVE_LIBRARY) } } catch (_: NoSuchMethodError) {}
    try { if ("NO_DEFAULT_LIBS" in optionsMap) { arguments.nodefaultlibs = get(NO_DEFAULT_LIBS) } } catch (_: NoSuchMethodError) {}
    try { if ("NO_ENDORSED_LIBS" in optionsMap) { arguments.noendorsedlibs = get(NO_ENDORSED_LIBS) } } catch (_: NoSuchMethodError) {}
    try { if ("NOMAIN" in optionsMap) { arguments.nomain = get(NOMAIN) } } catch (_: NoSuchMethodError) {}
    try { if ("NOPACK" in optionsMap) { arguments.nopack = get(NOPACK) } } catch (_: NoSuchMethodError) {}
    try { if ("LINKER_OPTIONS" in optionsMap) { arguments.linkerArguments = get(LINKER_OPTIONS) } } catch (_: NoSuchMethodError) {}
    try { if ("LINKER_OPTION" in optionsMap) { arguments.singleLinkerArguments = get(LINKER_OPTION) } } catch (_: NoSuchMethodError) {}
    try { if ("NOSTDLIB" in optionsMap) { arguments.nostdlib = get(NOSTDLIB) } } catch (_: NoSuchMethodError) {}
    try { if ("OPT" in optionsMap) { arguments.optimization = get(OPT) } } catch (_: NoSuchMethodError) {}
    try { if ("OUTPUT" in optionsMap) { arguments.outputName = get(OUTPUT) } } catch (_: NoSuchMethodError) {}
    try { if ("ENTRY" in optionsMap) { arguments.mainPackage = get(ENTRY) } } catch (_: NoSuchMethodError) {}
    try { if ("PRODUCE" in optionsMap) { arguments.produce = get(PRODUCE) } } catch (_: NoSuchMethodError) {}
    try { if ("TARGET" in optionsMap) { arguments.target = get(TARGET) } } catch (_: NoSuchMethodError) {}
    try { if ("X_BUNDLE_ID" in optionsMap) { arguments.bundleId = get(X_BUNDLE_ID) } } catch (_: NoSuchMethodError) {}
    try { if ("X_CACHE_DIRECTORY" in optionsMap) { arguments.cacheDirectories = get(X_CACHE_DIRECTORY) } } catch (_: NoSuchMethodError) {}
    try { if ("X_CACHED_LIBRARY" in optionsMap) { arguments.cachedLibraries = get(X_CACHED_LIBRARY) } } catch (_: NoSuchMethodError) {}
    try { if ("X_AUTO_CACHE_FROM" in optionsMap) { arguments.autoCacheableFrom = get(X_AUTO_CACHE_FROM) } } catch (_: NoSuchMethodError) {}
    try { if ("X_AUTO_CACHE_DIR" in optionsMap) { arguments.autoCacheDir = get(X_AUTO_CACHE_DIR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IC_CACHE_DIR" in optionsMap) { arguments.incrementalCacheDir = get(X_IC_CACHE_DIR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_CHECK_DEPENDENCIES" in optionsMap) { arguments.checkDependencies = get(X_CHECK_DEPENDENCIES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_EMIT_LAZY_OBJC_HEADER" in optionsMap) { arguments.emitLazyObjCHeader = get(X_EMIT_LAZY_OBJC_HEADER) } } catch (_: NoSuchMethodError) {}
    try { if ("X_EXPORT_LIBRARY" in optionsMap) { arguments.exportedLibraries = get(X_EXPORT_LIBRARY) } } catch (_: NoSuchMethodError) {}
    try { if ("X_EXTERNAL_DEPENDENCIES" in optionsMap) { arguments.externalDependencies = get(X_EXTERNAL_DEPENDENCIES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_FAKE_OVERRIDE_VALIDATOR" in optionsMap) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_FRAMEWORK_IMPORT_HEADER" in optionsMap) { arguments.frameworkImportHeaders = get(X_FRAMEWORK_IMPORT_HEADER) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ADD_LIGHT_DEBUG" in optionsMap) { arguments.lightDebugString = get(X_ADD_LIGHT_DEBUG) } } catch (_: NoSuchMethodError) {}
    try { if ("X_G0" in optionsMap) { arguments.lightDebugDeprecated = get(X_G0) } } catch (_: NoSuchMethodError) {}
    try { if ("X_G_GENERATE_DEBUG_TRAMPOLINE" in optionsMap) { arguments.generateDebugTrampolineString = get(X_G_GENERATE_DEBUG_TRAMPOLINE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ADD_CACHE" in optionsMap) { arguments.libraryToAddToCache = get(X_ADD_CACHE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_FILE_TO_CACHE" in optionsMap) { arguments.filesToCache = get(X_FILE_TO_CACHE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_MAKE_PER_FILE_CACHE" in optionsMap) { arguments.makePerFileCache = get(X_MAKE_PER_FILE_CACHE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_BACKEND_THREADS" in optionsMap) { arguments.backendThreads = get(X_BACKEND_THREADS).toString() } } catch (_: NoSuchMethodError) {}
    try { if ("X_EXPORT_KDOC" in optionsMap) { arguments.exportKDoc = get(X_EXPORT_KDOC) } } catch (_: NoSuchMethodError) {}
    try { if ("X_PRINT_BITCODE" in optionsMap) { arguments.printBitCode = get(X_PRINT_BITCODE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_CHECK_STATE_AT_EXTERNAL_CALLS" in optionsMap) { arguments.checkExternalCalls = get(X_CHECK_STATE_AT_EXTERNAL_CALLS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_PRINT_IR" in optionsMap) { arguments.printIr = get(X_PRINT_IR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_PRINT_FILES" in optionsMap) { arguments.printFiles = get(X_PRINT_FILES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_PURGE_USER_LIBS" in optionsMap) { arguments.purgeUserLibs = get(X_PURGE_USER_LIBS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO" in optionsMap) { arguments.writeDependenciesOfProducedKlibTo = get(X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO) } } catch (_: NoSuchMethodError) {}
    try { if ("X_RUNTIME" in optionsMap) { arguments.runtimeFile = get(X_RUNTIME) } } catch (_: NoSuchMethodError) {}
    try { if ("X_INCLUDE" in optionsMap) { arguments.includes = get(X_INCLUDE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_SHORT_MODULE_NAME" in optionsMap) { arguments.shortModuleName = get(X_SHORT_MODULE_NAME) } } catch (_: NoSuchMethodError) {}
    try { if ("X_STATIC_FRAMEWORK" in optionsMap) { arguments.staticFramework = get(X_STATIC_FRAMEWORK) } } catch (_: NoSuchMethodError) {}
    try { if ("X_TEMPORARY_FILES_DIR" in optionsMap) { arguments.temporaryFilesDir = get(X_TEMPORARY_FILES_DIR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_SAVE_LLVM_IR_AFTER" in optionsMap) { arguments.saveLlvmIrAfter = get(X_SAVE_LLVM_IR_AFTER) } } catch (_: NoSuchMethodError) {}
    try { if ("X_VERIFY_BITCODE" in optionsMap) { arguments.verifyBitCode = get(X_VERIFY_BITCODE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_VERIFY_COMPILER" in optionsMap) { arguments.verifyCompiler = get(X_VERIFY_COMPILER) } } catch (_: NoSuchMethodError) {}
    try { if ("FRIEND_MODULES" in optionsMap) { arguments.friendModules = get(FRIEND_MODULES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_REFINES_PATHS" in optionsMap) { arguments.refinesPaths = get(X_REFINES_PATHS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_DEBUG_INFO_VERSION" in optionsMap) { arguments.debugInfoFormatVersion = get(X_DEBUG_INFO_VERSION).toString() } } catch (_: NoSuchMethodError) {}
    try { if ("X_NO_OBJC_GENERICS" in optionsMap) { arguments.noObjcGenerics = get(X_NO_OBJC_GENERICS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_OVERRIDE_CLANG_OPTIONS" in optionsMap) { arguments.clangOptions = get(X_OVERRIDE_CLANG_OPTIONS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ALLOCATOR" in optionsMap) { arguments.allocator = get(X_ALLOCATOR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_HEADER_KLIB_PATH" in optionsMap) { arguments.headerKlibPath = get(X_HEADER_KLIB_PATH) } } catch (_: NoSuchMethodError) {}
    try { if ("X_DEBUG_PREFIX_MAP" in optionsMap) { arguments.debugPrefixMap = get(X_DEBUG_PREFIX_MAP) } } catch (_: NoSuchMethodError) {}
    try { if ("X_PRE_LINK_CACHES" in optionsMap) { arguments.preLinkCaches = get(X_PRE_LINK_CACHES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_OVERRIDE_KONAN_PROPERTIES" in optionsMap) { arguments.overrideKonanProperties = get(X_OVERRIDE_KONAN_PROPERTIES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_DESTROY_RUNTIME_MODE" in optionsMap) { arguments.destroyRuntimeMode = get(X_DESTROY_RUNTIME_MODE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_GC" in optionsMap) { arguments.gc = get(X_GC) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PROPERTY_LAZY_INITIALIZATION" in optionsMap) { arguments.propertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION) } } catch (_: NoSuchMethodError) {}
    try { if ("X_WORKER_EXCEPTION_HANDLING" in optionsMap) { arguments.workerExceptionHandling = get(X_WORKER_EXCEPTION_HANDLING) } } catch (_: NoSuchMethodError) {}
    try { if ("X_LLVM_VARIANT" in optionsMap) { arguments.llvmVariant = get(X_LLVM_VARIANT) } } catch (_: NoSuchMethodError) {}
    try { if ("X_BINARY" in optionsMap) { arguments.binaryOptions = get(X_BINARY) } } catch (_: NoSuchMethodError) {}
    try { if ("X_RUNTIME_LOGS" in optionsMap) { arguments.runtimeLogs = get(X_RUNTIME_LOGS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_DUMP_TESTS_TO" in optionsMap) { arguments.testDumpOutputPath = get(X_DUMP_TESTS_TO) } } catch (_: NoSuchMethodError) {}
    try { if ("X_LAZY_IR_FOR_CACHES" in optionsMap) { arguments.lazyIrForCaches = get(X_LAZY_IR_FOR_CACHES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_OMIT_FRAMEWORK_BINARY" in optionsMap) { arguments.omitFrameworkBinary = get(X_OMIT_FRAMEWORK_BINARY) } } catch (_: NoSuchMethodError) {}
    try { if ("X_COMPILE_FROM_BITCODE" in optionsMap) { arguments.compileFromBitcode = get(X_COMPILE_FROM_BITCODE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_READ_DEPENDENCIES_FROM" in optionsMap) { arguments.serializedDependencies = get(X_READ_DEPENDENCIES_FROM) } } catch (_: NoSuchMethodError) {}
    try { if ("X_WRITE_DEPENDENCIES_TO" in optionsMap) { arguments.saveDependenciesPath = get(X_WRITE_DEPENDENCIES_TO) } } catch (_: NoSuchMethodError) {}
    try { if ("X_SAVE_LLVM_IR_DIRECTORY" in optionsMap) { arguments.saveLlvmIrDirectory = get(X_SAVE_LLVM_IR_DIRECTORY) } } catch (_: NoSuchMethodError) {}
    try { if ("X_KONAN_DATA_DIR" in optionsMap) { arguments.konanDataDir = get(X_KONAN_DATA_DIR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_LLVM_MODULE_PASSES" in optionsMap) { arguments.llvmModulePasses = get(X_LLVM_MODULE_PASSES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_LLVM_LTO_PASSES" in optionsMap) { arguments.llvmLTOPasses = get(X_LLVM_LTO_PASSES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_MANIFEST_NATIVE_TARGETS" in optionsMap) { arguments.manifestNativeTargets = get(X_MANIFEST_NATIVE_TARGETS) } } catch (_: NoSuchMethodError) {}
    arguments.internalArguments = parseCommandLineArguments<K2NativeCompilerArguments>(internalArguments.toList()).internalArguments
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2NativeCompilerArguments = parseCommandLineArguments(arguments)
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings()
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: K2NativeCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[ENABLE_ASSERTIONS] = arguments.enableAssertions } catch (_: NoSuchMethodError) {}
    try { this[G] = arguments.debug } catch (_: NoSuchMethodError) {}
    try { this[GENERATE_TEST_RUNNER] = arguments.generateTestRunner } catch (_: NoSuchMethodError) {}
    try { this[GENERATE_WORKER_TEST_RUNNER] = arguments.generateWorkerTestRunner } catch (_: NoSuchMethodError) {}
    try { this[GENERATE_NO_EXIT_TEST_RUNNER] = arguments.generateNoExitTestRunner } catch (_: NoSuchMethodError) {}
    try { this[INCLUDE_BINARY] = arguments.includeBinaries } catch (_: NoSuchMethodError) {}
    try { this[LIBRARY] = arguments.libraries } catch (_: NoSuchMethodError) {}
    try { this[LIBRARY_VERSION] = arguments.libraryVersion } catch (_: NoSuchMethodError) {}
    try { this[LIST_TARGETS] = arguments.listTargets } catch (_: NoSuchMethodError) {}
    try { this[MANIFEST] = arguments.manifestFile } catch (_: NoSuchMethodError) {}
    try { this[MEMORY_MODEL] = arguments.memoryModel } catch (_: NoSuchMethodError) {}
    try { this[MODULE_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {}
    try { this[NATIVE_LIBRARY] = arguments.nativeLibraries } catch (_: NoSuchMethodError) {}
    try { this[NO_DEFAULT_LIBS] = arguments.nodefaultlibs } catch (_: NoSuchMethodError) {}
    try { this[NO_ENDORSED_LIBS] = arguments.noendorsedlibs } catch (_: NoSuchMethodError) {}
    try { this[NOMAIN] = arguments.nomain } catch (_: NoSuchMethodError) {}
    try { this[NOPACK] = arguments.nopack } catch (_: NoSuchMethodError) {}
    try { this[LINKER_OPTIONS] = arguments.linkerArguments } catch (_: NoSuchMethodError) {}
    try { this[LINKER_OPTION] = arguments.singleLinkerArguments } catch (_: NoSuchMethodError) {}
    try { this[NOSTDLIB] = arguments.nostdlib } catch (_: NoSuchMethodError) {}
    try { this[OPT] = arguments.optimization } catch (_: NoSuchMethodError) {}
    try { this[OUTPUT] = arguments.outputName } catch (_: NoSuchMethodError) {}
    try { this[ENTRY] = arguments.mainPackage } catch (_: NoSuchMethodError) {}
    try { this[PRODUCE] = arguments.produce } catch (_: NoSuchMethodError) {}
    try { this[TARGET] = arguments.target } catch (_: NoSuchMethodError) {}
    try { this[X_BUNDLE_ID] = arguments.bundleId } catch (_: NoSuchMethodError) {}
    try { this[X_CACHE_DIRECTORY] = arguments.cacheDirectories } catch (_: NoSuchMethodError) {}
    try { this[X_CACHED_LIBRARY] = arguments.cachedLibraries } catch (_: NoSuchMethodError) {}
    try { this[X_AUTO_CACHE_FROM] = arguments.autoCacheableFrom } catch (_: NoSuchMethodError) {}
    try { this[X_AUTO_CACHE_DIR] = arguments.autoCacheDir } catch (_: NoSuchMethodError) {}
    try { this[X_IC_CACHE_DIR] = arguments.incrementalCacheDir } catch (_: NoSuchMethodError) {}
    try { this[X_CHECK_DEPENDENCIES] = arguments.checkDependencies } catch (_: NoSuchMethodError) {}
    try { this[X_EMIT_LAZY_OBJC_HEADER] = arguments.emitLazyObjCHeader } catch (_: NoSuchMethodError) {}
    try { this[X_EXPORT_LIBRARY] = arguments.exportedLibraries } catch (_: NoSuchMethodError) {}
    try { this[X_EXTERNAL_DEPENDENCIES] = arguments.externalDependencies } catch (_: NoSuchMethodError) {}
    try { this[X_FAKE_OVERRIDE_VALIDATOR] = arguments.fakeOverrideValidator } catch (_: NoSuchMethodError) {}
    try { this[X_FRAMEWORK_IMPORT_HEADER] = arguments.frameworkImportHeaders } catch (_: NoSuchMethodError) {}
    try { this[X_ADD_LIGHT_DEBUG] = arguments.lightDebugString } catch (_: NoSuchMethodError) {}
    try { this[X_G0] = arguments.lightDebugDeprecated } catch (_: NoSuchMethodError) {}
    try { this[X_G_GENERATE_DEBUG_TRAMPOLINE] = arguments.generateDebugTrampolineString } catch (_: NoSuchMethodError) {}
    try { this[X_ADD_CACHE] = arguments.libraryToAddToCache } catch (_: NoSuchMethodError) {}
    try { this[X_FILE_TO_CACHE] = arguments.filesToCache } catch (_: NoSuchMethodError) {}
    try { this[X_MAKE_PER_FILE_CACHE] = arguments.makePerFileCache } catch (_: NoSuchMethodError) {}
    try { this[X_BACKEND_THREADS] = arguments.backendThreads.let { it.toInt() } } catch (_: NoSuchMethodError) {}
    try { this[X_EXPORT_KDOC] = arguments.exportKDoc } catch (_: NoSuchMethodError) {}
    try { this[X_PRINT_BITCODE] = arguments.printBitCode } catch (_: NoSuchMethodError) {}
    try { this[X_CHECK_STATE_AT_EXTERNAL_CALLS] = arguments.checkExternalCalls } catch (_: NoSuchMethodError) {}
    try { this[X_PRINT_IR] = arguments.printIr } catch (_: NoSuchMethodError) {}
    try { this[X_PRINT_FILES] = arguments.printFiles } catch (_: NoSuchMethodError) {}
    try { this[X_PURGE_USER_LIBS] = arguments.purgeUserLibs } catch (_: NoSuchMethodError) {}
    try { this[X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO] = arguments.writeDependenciesOfProducedKlibTo } catch (_: NoSuchMethodError) {}
    try { this[X_RUNTIME] = arguments.runtimeFile } catch (_: NoSuchMethodError) {}
    try { this[X_INCLUDE] = arguments.includes } catch (_: NoSuchMethodError) {}
    try { this[X_SHORT_MODULE_NAME] = arguments.shortModuleName } catch (_: NoSuchMethodError) {}
    try { this[X_STATIC_FRAMEWORK] = arguments.staticFramework } catch (_: NoSuchMethodError) {}
    try { this[X_TEMPORARY_FILES_DIR] = arguments.temporaryFilesDir } catch (_: NoSuchMethodError) {}
    try { this[X_SAVE_LLVM_IR_AFTER] = arguments.saveLlvmIrAfter } catch (_: NoSuchMethodError) {}
    try { this[X_VERIFY_BITCODE] = arguments.verifyBitCode } catch (_: NoSuchMethodError) {}
    try { this[X_VERIFY_COMPILER] = arguments.verifyCompiler } catch (_: NoSuchMethodError) {}
    try { this[FRIEND_MODULES] = arguments.friendModules } catch (_: NoSuchMethodError) {}
    try { this[X_REFINES_PATHS] = arguments.refinesPaths } catch (_: NoSuchMethodError) {}
    try { this[X_DEBUG_INFO_VERSION] = arguments.debugInfoFormatVersion.let { it.toInt() } } catch (_: NoSuchMethodError) {}
    try { this[X_NO_OBJC_GENERICS] = arguments.noObjcGenerics } catch (_: NoSuchMethodError) {}
    try { this[X_OVERRIDE_CLANG_OPTIONS] = arguments.clangOptions } catch (_: NoSuchMethodError) {}
    try { this[X_ALLOCATOR] = arguments.allocator } catch (_: NoSuchMethodError) {}
    try { this[X_HEADER_KLIB_PATH] = arguments.headerKlibPath } catch (_: NoSuchMethodError) {}
    try { this[X_DEBUG_PREFIX_MAP] = arguments.debugPrefixMap } catch (_: NoSuchMethodError) {}
    try { this[X_PRE_LINK_CACHES] = arguments.preLinkCaches } catch (_: NoSuchMethodError) {}
    try { this[X_OVERRIDE_KONAN_PROPERTIES] = arguments.overrideKonanProperties } catch (_: NoSuchMethodError) {}
    try { this[X_DESTROY_RUNTIME_MODE] = arguments.destroyRuntimeMode } catch (_: NoSuchMethodError) {}
    try { this[X_GC] = arguments.gc } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PROPERTY_LAZY_INITIALIZATION] = arguments.propertyLazyInitialization } catch (_: NoSuchMethodError) {}
    try { this[X_WORKER_EXCEPTION_HANDLING] = arguments.workerExceptionHandling } catch (_: NoSuchMethodError) {}
    try { this[X_LLVM_VARIANT] = arguments.llvmVariant } catch (_: NoSuchMethodError) {}
    try { this[X_BINARY] = arguments.binaryOptions } catch (_: NoSuchMethodError) {}
    try { this[X_RUNTIME_LOGS] = arguments.runtimeLogs } catch (_: NoSuchMethodError) {}
    try { this[X_DUMP_TESTS_TO] = arguments.testDumpOutputPath } catch (_: NoSuchMethodError) {}
    try { this[X_LAZY_IR_FOR_CACHES] = arguments.lazyIrForCaches } catch (_: NoSuchMethodError) {}
    try { this[X_OMIT_FRAMEWORK_BINARY] = arguments.omitFrameworkBinary } catch (_: NoSuchMethodError) {}
    try { this[X_COMPILE_FROM_BITCODE] = arguments.compileFromBitcode } catch (_: NoSuchMethodError) {}
    try { this[X_READ_DEPENDENCIES_FROM] = arguments.serializedDependencies } catch (_: NoSuchMethodError) {}
    try { this[X_WRITE_DEPENDENCIES_TO] = arguments.saveDependenciesPath } catch (_: NoSuchMethodError) {}
    try { this[X_SAVE_LLVM_IR_DIRECTORY] = arguments.saveLlvmIrDirectory } catch (_: NoSuchMethodError) {}
    try { this[X_KONAN_DATA_DIR] = arguments.konanDataDir } catch (_: NoSuchMethodError) {}
    try { this[X_LLVM_MODULE_PASSES] = arguments.llvmModulePasses } catch (_: NoSuchMethodError) {}
    try { this[X_LLVM_LTO_PASSES] = arguments.llvmLTOPasses } catch (_: NoSuchMethodError) {}
    try { this[X_MANIFEST_NATIVE_TARGETS] = arguments.manifestNativeTargets } catch (_: NoSuchMethodError) {}
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

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
