package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.ENABLE_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.ENTRY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.G
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.GENERATE_NO_EXIT_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.GENERATE_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.GENERATE_WORKER_TEST_RUNNER
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.INCLUDE_BINARY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.LIBRARY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.LIBRARY_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.LINKER_OPTION
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.LINKER_OPTIONS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.LIST_TARGETS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.MANIFEST
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.MEMORY_MODEL
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.NATIVE_LIBRARY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.NOMAIN
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.NOSTDLIB
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.NO_DEFAULT_LIBS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.NO_ENDORSED_LIBS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.OPT
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.OUTPUT
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.PRODUCE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XADD_CACHE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XADD_LIGHT_DEBUG
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XALLOCATOR
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XAUTO_CACHE_DIR
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XAUTO_CACHE_FROM
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XBACKEND_THREADS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XBINARY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XBUNDLE_ID
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XCACHED_LIBRARY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XCACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XCHECK_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XCHECK_STATE_AT_EXTERNAL_CALLS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XCOMPILE_FROM_BITCODE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XDEBUG_INFO_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XDEBUG_PREFIX_MAP
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XDESTROY_RUNTIME_MODE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XDUMP_SYNTHETIC_ACCESSORS_TO
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XDUMP_TESTS_TO
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XEMIT_LAZY_OBJC_HEADER
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XEXPORT_KDOC
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XEXPORT_LIBRARY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XEXTERNAL_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XFAKE_OVERRIDE_VALIDATOR
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XFILE_TO_CACHE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XFRAMEWORK_IMPORT_HEADER
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XG0
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XGC
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XG_GENERATE_DEBUG_TRAMPOLINE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XHEADER_KLIB_PATH
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XIC_CACHE_DIR
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XINCLUDE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XIR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XKONAN_DATA_DIR
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XLAZY_IR_FOR_CACHES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XLLVM_LTO_PASSES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XLLVM_MODULE_PASSES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XLLVM_VARIANT
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XMAKE_PER_FILE_CACHE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XMANIFEST_NATIVE_TARGETS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XNO_OBJC_GENERICS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XOMIT_FRAMEWORK_BINARY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XOVERRIDE_CLANG_OPTIONS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XOVERRIDE_KONAN_PROPERTIES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XPRE_LINK_CACHES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XPRINT_BITCODE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XPRINT_FILES
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XPRINT_IR
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XPURGE_USER_LIBS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XREAD_DEPENDENCIES_FROM
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XREFINES_PATHS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XRUNTIME
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XRUNTIME_LOGS
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XSAVE_LLVM_IR_AFTER
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XSAVE_LLVM_IR_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XSHORT_MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XSTATIC_FRAMEWORK
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XTEMPORARY_FILES_DIR
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XVERIFY_BITCODE
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XVERIFY_COMPILER
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XWORKER_EXCEPTION_HANDLING
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO
import org.jetbrains.kotlin.buildtools.api.v2.NativeArguments.Companion.XWRITE_DEPENDENCIES_TO
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments

public class NativeArgumentsImpl : CommonKlibBasedArgumentsImpl(), NativeArguments {
  private val optionsMap: MutableMap<NativeArguments.NativeArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: NativeArguments.NativeArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: NativeArguments.NativeArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2NativeCompilerArguments = K2NativeCompilerArguments()): K2NativeCompilerArguments {
    if (ENABLE_ASSERTIONS in optionsMap) { arguments.enableAssertions = get(ENABLE_ASSERTIONS) }
    if (G in optionsMap) { arguments.debug = get(G) }
    if (GENERATE_TEST_RUNNER in optionsMap) { arguments.generateTestRunner = get(GENERATE_TEST_RUNNER) }
    if (GENERATE_WORKER_TEST_RUNNER in optionsMap) { arguments.generateWorkerTestRunner = get(GENERATE_WORKER_TEST_RUNNER) }
    if (GENERATE_NO_EXIT_TEST_RUNNER in optionsMap) { arguments.generateNoExitTestRunner = get(GENERATE_NO_EXIT_TEST_RUNNER) }
    if (INCLUDE_BINARY in optionsMap) { arguments.includeBinaries = get(INCLUDE_BINARY) }
    if (LIBRARY in optionsMap) { arguments.libraries = get(LIBRARY) }
    if (LIBRARY_VERSION in optionsMap) { arguments.libraryVersion = get(LIBRARY_VERSION) }
    if (LIST_TARGETS in optionsMap) { arguments.listTargets = get(LIST_TARGETS) }
    if (MANIFEST in optionsMap) { arguments.manifestFile = get(MANIFEST) }
    if (MEMORY_MODEL in optionsMap) { arguments.memoryModel = get(MEMORY_MODEL) }
    if (MODULE_NAME in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if (NATIVE_LIBRARY in optionsMap) { arguments.nativeLibraries = get(NATIVE_LIBRARY) }
    if (NO_DEFAULT_LIBS in optionsMap) { arguments.nodefaultlibs = get(NO_DEFAULT_LIBS) }
    if (NO_ENDORSED_LIBS in optionsMap) { arguments.noendorsedlibs = get(NO_ENDORSED_LIBS) }
    if (NOMAIN in optionsMap) { arguments.nomain = get(NOMAIN) }
    if (NOPACK in optionsMap) { arguments.nopack = get(NOPACK) }
    if (LINKER_OPTIONS in optionsMap) { arguments.linkerArguments = get(LINKER_OPTIONS) }
    if (LINKER_OPTION in optionsMap) { arguments.singleLinkerArguments = get(LINKER_OPTION) }
    if (NOSTDLIB in optionsMap) { arguments.nostdlib = get(NOSTDLIB) }
    if (OPT in optionsMap) { arguments.optimization = get(OPT) }
    if (OUTPUT in optionsMap) { arguments.outputName = get(OUTPUT) }
    if (ENTRY in optionsMap) { arguments.mainPackage = get(ENTRY) }
    if (PRODUCE in optionsMap) { arguments.produce = get(PRODUCE) }
    if (TARGET in optionsMap) { arguments.target = get(TARGET) }
    if (XBUNDLE_ID in optionsMap) { arguments.bundleId = get(XBUNDLE_ID) }
    if (XCACHE_DIRECTORY in optionsMap) { arguments.cacheDirectories = get(XCACHE_DIRECTORY) }
    if (XCACHED_LIBRARY in optionsMap) { arguments.cachedLibraries = get(XCACHED_LIBRARY) }
    if (XAUTO_CACHE_FROM in optionsMap) { arguments.autoCacheableFrom = get(XAUTO_CACHE_FROM) }
    if (XAUTO_CACHE_DIR in optionsMap) { arguments.autoCacheDir = get(XAUTO_CACHE_DIR) }
    if (XIC_CACHE_DIR in optionsMap) { arguments.incrementalCacheDir = get(XIC_CACHE_DIR) }
    if (XCHECK_DEPENDENCIES in optionsMap) { arguments.checkDependencies = get(XCHECK_DEPENDENCIES) }
    if (XEMIT_LAZY_OBJC_HEADER in optionsMap) { arguments.emitLazyObjCHeader = get(XEMIT_LAZY_OBJC_HEADER) }
    if (XEXPORT_LIBRARY in optionsMap) { arguments.exportedLibraries = get(XEXPORT_LIBRARY) }
    if (XEXTERNAL_DEPENDENCIES in optionsMap) { arguments.externalDependencies = get(XEXTERNAL_DEPENDENCIES) }
    if (XFAKE_OVERRIDE_VALIDATOR in optionsMap) { arguments.fakeOverrideValidator = get(XFAKE_OVERRIDE_VALIDATOR) }
    if (XFRAMEWORK_IMPORT_HEADER in optionsMap) { arguments.frameworkImportHeaders = get(XFRAMEWORK_IMPORT_HEADER) }
    if (XADD_LIGHT_DEBUG in optionsMap) { arguments.lightDebugString = get(XADD_LIGHT_DEBUG) }
    if (XG0 in optionsMap) { arguments.lightDebugDeprecated = get(XG0) }
    if (XG_GENERATE_DEBUG_TRAMPOLINE in optionsMap) { arguments.generateDebugTrampolineString = get(XG_GENERATE_DEBUG_TRAMPOLINE) }
    if (XADD_CACHE in optionsMap) { arguments.libraryToAddToCache = get(XADD_CACHE) }
    if (XFILE_TO_CACHE in optionsMap) { arguments.filesToCache = get(XFILE_TO_CACHE) }
    if (XMAKE_PER_FILE_CACHE in optionsMap) { arguments.makePerFileCache = get(XMAKE_PER_FILE_CACHE) }
    if (XBACKEND_THREADS in optionsMap) { arguments.backendThreads = get(XBACKEND_THREADS).toString() }
    if (XEXPORT_KDOC in optionsMap) { arguments.exportKDoc = get(XEXPORT_KDOC) }
    if (XPRINT_BITCODE in optionsMap) { arguments.printBitCode = get(XPRINT_BITCODE) }
    if (XCHECK_STATE_AT_EXTERNAL_CALLS in optionsMap) { arguments.checkExternalCalls = get(XCHECK_STATE_AT_EXTERNAL_CALLS) }
    if (XPRINT_IR in optionsMap) { arguments.printIr = get(XPRINT_IR) }
    if (XPRINT_FILES in optionsMap) { arguments.printFiles = get(XPRINT_FILES) }
    if (XPURGE_USER_LIBS in optionsMap) { arguments.purgeUserLibs = get(XPURGE_USER_LIBS) }
    if (XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO in optionsMap) { arguments.writeDependenciesOfProducedKlibTo = get(XWRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO) }
    if (XRUNTIME in optionsMap) { arguments.runtimeFile = get(XRUNTIME) }
    if (XINCLUDE in optionsMap) { arguments.includes = get(XINCLUDE) }
    if (XSHORT_MODULE_NAME in optionsMap) { arguments.shortModuleName = get(XSHORT_MODULE_NAME) }
    if (XSTATIC_FRAMEWORK in optionsMap) { arguments.staticFramework = get(XSTATIC_FRAMEWORK) }
    if (XTEMPORARY_FILES_DIR in optionsMap) { arguments.temporaryFilesDir = get(XTEMPORARY_FILES_DIR) }
    if (XSAVE_LLVM_IR_AFTER in optionsMap) { arguments.saveLlvmIrAfter = get(XSAVE_LLVM_IR_AFTER) }
    if (XVERIFY_BITCODE in optionsMap) { arguments.verifyBitCode = get(XVERIFY_BITCODE) }
    if (XVERIFY_COMPILER in optionsMap) { arguments.verifyCompiler = get(XVERIFY_COMPILER) }
    if (FRIEND_MODULES in optionsMap) { arguments.friendModules = get(FRIEND_MODULES) }
    if (XREFINES_PATHS in optionsMap) { arguments.refinesPaths = get(XREFINES_PATHS) }
    if (XDEBUG_INFO_VERSION in optionsMap) { arguments.debugInfoFormatVersion = get(XDEBUG_INFO_VERSION).toString() }
    if (XNO_OBJC_GENERICS in optionsMap) { arguments.noObjcGenerics = get(XNO_OBJC_GENERICS) }
    if (XOVERRIDE_CLANG_OPTIONS in optionsMap) { arguments.clangOptions = get(XOVERRIDE_CLANG_OPTIONS) }
    if (XALLOCATOR in optionsMap) { arguments.allocator = get(XALLOCATOR) }
    if (XHEADER_KLIB_PATH in optionsMap) { arguments.headerKlibPath = get(XHEADER_KLIB_PATH) }
    if (XDEBUG_PREFIX_MAP in optionsMap) { arguments.debugPrefixMap = get(XDEBUG_PREFIX_MAP) }
    if (XPRE_LINK_CACHES in optionsMap) { arguments.preLinkCaches = get(XPRE_LINK_CACHES) }
    if (XOVERRIDE_KONAN_PROPERTIES in optionsMap) { arguments.overrideKonanProperties = get(XOVERRIDE_KONAN_PROPERTIES) }
    if (XDESTROY_RUNTIME_MODE in optionsMap) { arguments.destroyRuntimeMode = get(XDESTROY_RUNTIME_MODE) }
    if (XGC in optionsMap) { arguments.gc = get(XGC) }
    if (XIR_PROPERTY_LAZY_INITIALIZATION in optionsMap) { arguments.propertyLazyInitialization = get(XIR_PROPERTY_LAZY_INITIALIZATION) }
    if (XWORKER_EXCEPTION_HANDLING in optionsMap) { arguments.workerExceptionHandling = get(XWORKER_EXCEPTION_HANDLING) }
    if (XLLVM_VARIANT in optionsMap) { arguments.llvmVariant = get(XLLVM_VARIANT) }
    if (XBINARY in optionsMap) { arguments.binaryOptions = get(XBINARY) }
    if (XRUNTIME_LOGS in optionsMap) { arguments.runtimeLogs = get(XRUNTIME_LOGS) }
    if (XDUMP_TESTS_TO in optionsMap) { arguments.testDumpOutputPath = get(XDUMP_TESTS_TO) }
    if (XLAZY_IR_FOR_CACHES in optionsMap) { arguments.lazyIrForCaches = get(XLAZY_IR_FOR_CACHES) }
    if (XOMIT_FRAMEWORK_BINARY in optionsMap) { arguments.omitFrameworkBinary = get(XOMIT_FRAMEWORK_BINARY) }
    if (XCOMPILE_FROM_BITCODE in optionsMap) { arguments.compileFromBitcode = get(XCOMPILE_FROM_BITCODE) }
    if (XREAD_DEPENDENCIES_FROM in optionsMap) { arguments.serializedDependencies = get(XREAD_DEPENDENCIES_FROM) }
    if (XWRITE_DEPENDENCIES_TO in optionsMap) { arguments.saveDependenciesPath = get(XWRITE_DEPENDENCIES_TO) }
    if (XSAVE_LLVM_IR_DIRECTORY in optionsMap) { arguments.saveLlvmIrDirectory = get(XSAVE_LLVM_IR_DIRECTORY) }
    if (XKONAN_DATA_DIR in optionsMap) { arguments.konanDataDir = get(XKONAN_DATA_DIR) }
    if (XLLVM_MODULE_PASSES in optionsMap) { arguments.llvmModulePasses = get(XLLVM_MODULE_PASSES) }
    if (XLLVM_LTO_PASSES in optionsMap) { arguments.llvmLTOPasses = get(XLLVM_LTO_PASSES) }
    if (XMANIFEST_NATIVE_TARGETS in optionsMap) { arguments.manifestNativeTargets = get(XMANIFEST_NATIVE_TARGETS) }
    if (XDUMP_SYNTHETIC_ACCESSORS_TO in optionsMap) { arguments.dumpSyntheticAccessorsTo = get(XDUMP_SYNTHETIC_ACCESSORS_TO) }
    return arguments
  }
}
