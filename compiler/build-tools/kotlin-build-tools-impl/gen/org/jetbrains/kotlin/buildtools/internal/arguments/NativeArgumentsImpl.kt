// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
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
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_DUMP_BUILT_CACHES_TO
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_DUMP_TESTS_TO
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EMIT_LAZY_OBJC_HEADER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EXPORT_KDOC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EXPORT_LIBRARY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.NativeArgumentsImpl.Companion.X_EXTERNAL_DEPENDENCIES
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
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerLinkingArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class NativeArgumentsImpl(
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonKlibBasedArgumentsImpl(argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    NativeCompilerArguments,
    NativeCompilerArguments.Builder,
    NativeCompilerKlibArguments,
    NativeCompilerKlibArguments.Builder,
    NativeCompilerLinkingArguments,
    NativeCompilerLinkingArguments.Builder,
    DeepCopyable<NativeArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()
  init {
    applyCompilerArguments(K2NativeCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: NativeArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: NativeArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: NativeArgument<*>): Boolean = key.id in optionsMap

  private operator fun `get`(key: String): Any? = NativeArgumentValueAdapter.toApi(optionsMap[key])

  private operator fun `set`(key: String, `value`: Any?) {
    optionsMap[key] = NativeArgumentValueAdapter.toImpl(`value`)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: NativeCompilerArguments.NativeCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: NativeCompilerArguments.NativeCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: NativeCompilerKlibArguments.NativeCompilerKlibArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: NativeCompilerKlibArguments.NativeCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: NativeCompilerLinkingArguments.NativeCompilerLinkingArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: NativeCompilerLinkingArguments.NativeCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  override fun deepCopy(): NativeArgumentsImpl = NativeArgumentsImpl(argumentValidationErrors.toSet(), restrictedArgViolations.toList(), argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): NativeArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2NativeCompilerArguments {
    val arguments = K2NativeCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_ADD_CACHE in this) { arguments.libraryToAddToCache = get(X_ADD_CACHE)}
    if (X_ADD_LIGHT_DEBUG in this) { arguments.lightDebugString = get(X_ADD_LIGHT_DEBUG)}
    if (X_ALLOCATOR in this) { arguments.allocator = get(X_ALLOCATOR)}
    if (X_AUTO_CACHE_DIR in this) { arguments.autoCacheDir = get(X_AUTO_CACHE_DIR)}
    if (X_AUTO_CACHE_FROM in this) { arguments.autoCacheableFrom = get(X_AUTO_CACHE_FROM) ?: emptyArray()}
    if (X_BACKEND_THREADS in this) { arguments.backendThreads = get(X_BACKEND_THREADS).toString()}
    if (X_BINARY in this) { arguments.binaryOptions = get(X_BINARY) ?: emptyArray()}
    try { if (X_BUNDLE_ID in this) { arguments.setUsingReflection("bundleId", get(X_BUNDLE_ID))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_BUNDLE_ID. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_CACHE_DIRECTORY in this) { arguments.cacheDirectories = get(X_CACHE_DIRECTORY) ?: emptyArray()}
    if (X_CACHED_LIBRARY in this) { arguments.cachedLibraries = get(X_CACHED_LIBRARY) ?: emptyArray()}
    if (X_CHECK_DEPENDENCIES in this) { arguments.checkDependencies = get(X_CHECK_DEPENDENCIES)}
    if (X_CHECK_STATE_AT_EXTERNAL_CALLS in this) { arguments.checkExternalCalls = get(X_CHECK_STATE_AT_EXTERNAL_CALLS)}
    if (X_COMPILE_FROM_BITCODE in this) { arguments.compileFromBitcode = get(X_COMPILE_FROM_BITCODE)}
    if (X_DEBUG_INFO_VERSION in this) { arguments.debugInfoFormatVersion = get(X_DEBUG_INFO_VERSION).toString()}
    if (X_DEBUG_PREFIX_MAP in this) { arguments.debugPrefixMap = get(X_DEBUG_PREFIX_MAP) ?: emptyArray()}
    try { if (X_DESTROY_RUNTIME_MODE in this) { arguments.setUsingReflection("destroyRuntimeMode", get(X_DESTROY_RUNTIME_MODE))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DESTROY_RUNTIME_MODE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_DUMP_BUILT_CACHES_TO in this) { arguments.dumpBuiltCachesTo = get(X_DUMP_BUILT_CACHES_TO)}
    if (X_DUMP_TESTS_TO in this) { arguments.testDumpOutputPath = get(X_DUMP_TESTS_TO)}
    if (X_EMIT_LAZY_OBJC_HEADER in this) { arguments.emitLazyObjCHeader = get(X_EMIT_LAZY_OBJC_HEADER)}
    if (X_EXPORT_KDOC in this) { arguments.exportKDoc = get(X_EXPORT_KDOC)}
    if (X_EXPORT_LIBRARY in this) { arguments.exportedLibraries = get(X_EXPORT_LIBRARY) ?: emptyArray()}
    if (X_EXTERNAL_DEPENDENCIES in this) { arguments.externalDependencies = get(X_EXTERNAL_DEPENDENCIES)}
    if (X_FILE_TO_CACHE in this) { arguments.filesToCache = get(X_FILE_TO_CACHE) ?: emptyArray()}
    if (X_FRAMEWORK_IMPORT_HEADER in this) { arguments.frameworkImportHeaders = get(X_FRAMEWORK_IMPORT_HEADER) ?: emptyArray()}
    if (X_G_GENERATE_DEBUG_TRAMPOLINE in this) { arguments.generateDebugTrampolineString = get(X_G_GENERATE_DEBUG_TRAMPOLINE)}
    try { if (X_G0 in this) { arguments.setUsingReflection("lightDebugDeprecated", get(X_G0))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_G0. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    if (X_GC in this) { arguments.gc = get(X_GC)}
    if (X_HEADER_KLIB_PATH in this) { arguments.headerKlibPath = get(X_HEADER_KLIB_PATH)}
    if (X_IC_CACHE_DIR in this) { arguments.incrementalCacheDir = get(X_IC_CACHE_DIR)}
    if (X_INCLUDE in this) { arguments.includes = get(X_INCLUDE) ?: emptyArray()}
    if (X_IR_PROPERTY_LAZY_INITIALIZATION in this) { arguments.propertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION)}
    if (X_KONAN_DATA_DIR in this) { arguments.konanDataDir = get(X_KONAN_DATA_DIR)}
    if (X_LLVM_LTO_PASSES in this) { arguments.llvmLTOPasses = get(X_LLVM_LTO_PASSES)}
    if (X_LLVM_MODULE_PASSES in this) { arguments.llvmModulePasses = get(X_LLVM_MODULE_PASSES)}
    if (X_LLVM_VARIANT in this) { arguments.llvmVariant = get(X_LLVM_VARIANT)}
    if (X_MAKE_PER_FILE_CACHE in this) { arguments.makePerFileCache = get(X_MAKE_PER_FILE_CACHE)}
    if (X_MANIFEST_NATIVE_TARGETS in this) { arguments.manifestNativeTargets = get(X_MANIFEST_NATIVE_TARGETS) ?: emptyArray()}
    if (X_NO_OBJC_GENERICS in this) { arguments.noObjcGenerics = get(X_NO_OBJC_GENERICS)}
    if (X_OMIT_FRAMEWORK_BINARY in this) { arguments.omitFrameworkBinary = get(X_OMIT_FRAMEWORK_BINARY)}
    if (X_OVERRIDE_CLANG_OPTIONS in this) { arguments.clangOptions = get(X_OVERRIDE_CLANG_OPTIONS) ?: emptyArray()}
    if (X_OVERRIDE_KONAN_PROPERTIES in this) { arguments.overrideKonanProperties = get(X_OVERRIDE_KONAN_PROPERTIES) ?: emptyArray()}
    if (X_PRE_LINK_CACHES in this) { arguments.preLinkCaches = get(X_PRE_LINK_CACHES)}
    if (X_PRINT_BITCODE in this) { arguments.printBitCode = get(X_PRINT_BITCODE)}
    if (X_PRINT_FILES in this) { arguments.printFiles = get(X_PRINT_FILES)}
    if (X_PRINT_IR in this) { arguments.printIr = get(X_PRINT_IR)}
    try { if (X_PURGE_USER_LIBS in this) { arguments.setUsingReflection("purgeUserLibs", get(X_PURGE_USER_LIBS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_PURGE_USER_LIBS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_READ_DEPENDENCIES_FROM in this) { arguments.serializedDependencies = get(X_READ_DEPENDENCIES_FROM)}
    if (X_REFINES_PATHS in this) { arguments.refinesPaths = get(X_REFINES_PATHS) ?: emptyArray()}
    if (X_RUNTIME in this) { arguments.runtimeFile = get(X_RUNTIME)}
    if (X_RUNTIME_LOGS in this) { arguments.runtimeLogs = get(X_RUNTIME_LOGS)}
    if (X_SAVE_LLVM_IR_AFTER in this) { arguments.saveLlvmIrAfter = get(X_SAVE_LLVM_IR_AFTER) ?: emptyArray()}
    if (X_SAVE_LLVM_IR_DIRECTORY in this) { arguments.saveLlvmIrDirectory = get(X_SAVE_LLVM_IR_DIRECTORY)}
    if (X_SHORT_MODULE_NAME in this) { arguments.shortModuleName = get(X_SHORT_MODULE_NAME)}
    if (X_STATIC_FRAMEWORK in this) { arguments.staticFramework = get(X_STATIC_FRAMEWORK)}
    if (X_TEMPORARY_FILES_DIR in this) { arguments.temporaryFilesDir = get(X_TEMPORARY_FILES_DIR)}
    if (X_VERIFY_BITCODE in this) { arguments.verifyBitCode = get(X_VERIFY_BITCODE)}
    if (X_VERIFY_COMPILER in this) { arguments.verifyCompiler = get(X_VERIFY_COMPILER)}
    try { if (X_WORKER_EXCEPTION_HANDLING in this) { arguments.setUsingReflection("workerExceptionHandling", get(X_WORKER_EXCEPTION_HANDLING))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WORKER_EXCEPTION_HANDLING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO in this) { arguments.writeDependenciesOfProducedKlibTo = get(X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)}
    if (X_WRITE_DEPENDENCIES_TO in this) { arguments.saveDependenciesPath = get(X_WRITE_DEPENDENCIES_TO)}
    if (ENABLE_ASSERTIONS in this) { arguments.enableAssertions = get(ENABLE_ASSERTIONS)}
    if (ENTRY in this) { arguments.mainPackage = get(ENTRY)}
    if (FRIEND_MODULES in this) { arguments.friendModules = get(FRIEND_MODULES)}
    if (G in this) { arguments.debug = get(G)}
    if (GENERATE_NO_EXIT_TEST_RUNNER in this) { arguments.generateNoExitTestRunner = get(GENERATE_NO_EXIT_TEST_RUNNER)}
    if (GENERATE_TEST_RUNNER in this) { arguments.generateTestRunner = get(GENERATE_TEST_RUNNER)}
    if (GENERATE_WORKER_TEST_RUNNER in this) { arguments.generateWorkerTestRunner = get(GENERATE_WORKER_TEST_RUNNER)}
    if (INCLUDE_BINARY in this) { arguments.includeBinaries = get(INCLUDE_BINARY) ?: emptyArray()}
    if (LIBRARY in this) { arguments.libraries = get(LIBRARY) ?: emptyArray()}
    try { if (LIBRARY_VERSION in this) { arguments.setUsingReflection("libraryVersion", get(LIBRARY_VERSION))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: LIBRARY_VERSION. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    if (LINKER_OPTION in this) { arguments.singleLinkerArguments = get(LINKER_OPTION) ?: emptyArray()}
    if (LINKER_OPTIONS in this) { arguments.linkerArguments = get(LINKER_OPTIONS) ?: emptyArray()}
    if (LIST_TARGETS in this) { arguments.listTargets = get(LIST_TARGETS)}
    if (MANIFEST in this) { arguments.manifestFile = get(MANIFEST)}
    if (MEMORY_MODEL in this) { arguments.memoryModel = get(MEMORY_MODEL)}
    if (MODULE_NAME in this) { arguments.moduleName = get(MODULE_NAME)}
    if (NATIVE_LIBRARY in this) { arguments.nativeLibraries = get(NATIVE_LIBRARY) ?: emptyArray()}
    if (NO_DEFAULT_LIBS in this) { arguments.nodefaultlibs = get(NO_DEFAULT_LIBS)}
    try { if (NO_ENDORSED_LIBS in this) { arguments.setUsingReflection("noendorsedlibs", get(NO_ENDORSED_LIBS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: NO_ENDORSED_LIBS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    if (NOMAIN in this) { arguments.nomain = get(NOMAIN)}
    if (NOPACK in this) { arguments.nopack = get(NOPACK)}
    if (NOSTDLIB in this) { arguments.nostdlib = get(NOSTDLIB)}
    if (OPT in this) { arguments.optimization = get(OPT)}
    if (OUTPUT in this) { arguments.outputName = get(OUTPUT)}
    if (PRODUCE in this) { arguments.produce = get(PRODUCE)}
    if (TARGET in this) { arguments.target = get(TARGET)}
    arguments.internalArguments = parseCommandLineArguments<K2NativeCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2NativeCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_ADD_CACHE] = arguments.libraryToAddToCache } catch (_: NoSuchMethodError) {  }
    try { this[X_ADD_LIGHT_DEBUG] = arguments.lightDebugString } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOCATOR] = arguments.allocator } catch (_: NoSuchMethodError) {  }
    try { this[X_AUTO_CACHE_DIR] = arguments.autoCacheDir } catch (_: NoSuchMethodError) {  }
    try { this[X_AUTO_CACHE_FROM] = arguments.autoCacheableFrom } catch (_: NoSuchMethodError) {  }
    try { this[X_BACKEND_THREADS] = arguments.backendThreads.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { this[X_BINARY] = arguments.binaryOptions } catch (_: NoSuchMethodError) {  }
    try { this[X_BUNDLE_ID] = arguments.getUsingReflection<String?>("bundleId") } catch (_: NoSuchMethodError) {  }
    try { this[X_CACHE_DIRECTORY] = arguments.cacheDirectories } catch (_: NoSuchMethodError) {  }
    try { this[X_CACHED_LIBRARY] = arguments.cachedLibraries } catch (_: NoSuchMethodError) {  }
    try { this[X_CHECK_DEPENDENCIES] = arguments.checkDependencies } catch (_: NoSuchMethodError) {  }
    try { this[X_CHECK_STATE_AT_EXTERNAL_CALLS] = arguments.checkExternalCalls } catch (_: NoSuchMethodError) {  }
    try { this[X_COMPILE_FROM_BITCODE] = arguments.compileFromBitcode } catch (_: NoSuchMethodError) {  }
    try { this[X_DEBUG_INFO_VERSION] = arguments.debugInfoFormatVersion.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { this[X_DEBUG_PREFIX_MAP] = arguments.debugPrefixMap } catch (_: NoSuchMethodError) {  }
    try { this[X_DESTROY_RUNTIME_MODE] = arguments.getUsingReflection<String?>("destroyRuntimeMode") } catch (_: NoSuchMethodError) {  }
    try { this[X_DUMP_BUILT_CACHES_TO] = arguments.dumpBuiltCachesTo } catch (_: NoSuchMethodError) {  }
    try { this[X_DUMP_TESTS_TO] = arguments.testDumpOutputPath } catch (_: NoSuchMethodError) {  }
    try { this[X_EMIT_LAZY_OBJC_HEADER] = arguments.emitLazyObjCHeader } catch (_: NoSuchMethodError) {  }
    try { this[X_EXPORT_KDOC] = arguments.exportKDoc } catch (_: NoSuchMethodError) {  }
    try { this[X_EXPORT_LIBRARY] = arguments.exportedLibraries } catch (_: NoSuchMethodError) {  }
    try { this[X_EXTERNAL_DEPENDENCIES] = arguments.externalDependencies } catch (_: NoSuchMethodError) {  }
    try { this[X_FILE_TO_CACHE] = arguments.filesToCache } catch (_: NoSuchMethodError) {  }
    try { this[X_FRAMEWORK_IMPORT_HEADER] = arguments.frameworkImportHeaders } catch (_: NoSuchMethodError) {  }
    try { this[X_G_GENERATE_DEBUG_TRAMPOLINE] = arguments.generateDebugTrampolineString } catch (_: NoSuchMethodError) {  }
    try { this[X_G0] = arguments.getUsingReflection<Boolean>("lightDebugDeprecated") } catch (_: NoSuchMethodError) {  }
    try { this[X_GC] = arguments.gc } catch (_: NoSuchMethodError) {  }
    try { this[X_HEADER_KLIB_PATH] = arguments.headerKlibPath } catch (_: NoSuchMethodError) {  }
    try { this[X_IC_CACHE_DIR] = arguments.incrementalCacheDir } catch (_: NoSuchMethodError) {  }
    try { this[X_INCLUDE] = arguments.includes } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PROPERTY_LAZY_INITIALIZATION] = arguments.propertyLazyInitialization } catch (_: NoSuchMethodError) {  }
    try { this[X_KONAN_DATA_DIR] = arguments.konanDataDir } catch (_: NoSuchMethodError) {  }
    try { this[X_LLVM_LTO_PASSES] = arguments.llvmLTOPasses } catch (_: NoSuchMethodError) {  }
    try { this[X_LLVM_MODULE_PASSES] = arguments.llvmModulePasses } catch (_: NoSuchMethodError) {  }
    try { this[X_LLVM_VARIANT] = arguments.llvmVariant } catch (_: NoSuchMethodError) {  }
    try { this[X_MAKE_PER_FILE_CACHE] = arguments.makePerFileCache } catch (_: NoSuchMethodError) {  }
    try { this[X_MANIFEST_NATIVE_TARGETS] = arguments.manifestNativeTargets } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_OBJC_GENERICS] = arguments.noObjcGenerics } catch (_: NoSuchMethodError) {  }
    try { this[X_OMIT_FRAMEWORK_BINARY] = arguments.omitFrameworkBinary } catch (_: NoSuchMethodError) {  }
    try { this[X_OVERRIDE_CLANG_OPTIONS] = arguments.clangOptions } catch (_: NoSuchMethodError) {  }
    try { this[X_OVERRIDE_KONAN_PROPERTIES] = arguments.overrideKonanProperties } catch (_: NoSuchMethodError) {  }
    try { this[X_PRE_LINK_CACHES] = arguments.preLinkCaches } catch (_: NoSuchMethodError) {  }
    try { this[X_PRINT_BITCODE] = arguments.printBitCode } catch (_: NoSuchMethodError) {  }
    try { this[X_PRINT_FILES] = arguments.printFiles } catch (_: NoSuchMethodError) {  }
    try { this[X_PRINT_IR] = arguments.printIr } catch (_: NoSuchMethodError) {  }
    try { this[X_PURGE_USER_LIBS] = arguments.getUsingReflection<Boolean>("purgeUserLibs") } catch (_: NoSuchMethodError) {  }
    try { this[X_READ_DEPENDENCIES_FROM] = arguments.serializedDependencies } catch (_: NoSuchMethodError) {  }
    try { this[X_REFINES_PATHS] = arguments.refinesPaths } catch (_: NoSuchMethodError) {  }
    try { this[X_RUNTIME] = arguments.runtimeFile } catch (_: NoSuchMethodError) {  }
    try { this[X_RUNTIME_LOGS] = arguments.runtimeLogs } catch (_: NoSuchMethodError) {  }
    try { this[X_SAVE_LLVM_IR_AFTER] = arguments.saveLlvmIrAfter } catch (_: NoSuchMethodError) {  }
    try { this[X_SAVE_LLVM_IR_DIRECTORY] = arguments.saveLlvmIrDirectory } catch (_: NoSuchMethodError) {  }
    try { this[X_SHORT_MODULE_NAME] = arguments.shortModuleName } catch (_: NoSuchMethodError) {  }
    try { this[X_STATIC_FRAMEWORK] = arguments.staticFramework } catch (_: NoSuchMethodError) {  }
    try { this[X_TEMPORARY_FILES_DIR] = arguments.temporaryFilesDir } catch (_: NoSuchMethodError) {  }
    try { this[X_VERIFY_BITCODE] = arguments.verifyBitCode } catch (_: NoSuchMethodError) {  }
    try { this[X_VERIFY_COMPILER] = arguments.verifyCompiler } catch (_: NoSuchMethodError) {  }
    try { this[X_WORKER_EXCEPTION_HANDLING] = arguments.getUsingReflection<String?>("workerExceptionHandling") } catch (_: NoSuchMethodError) {  }
    try { this[X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO] = arguments.writeDependenciesOfProducedKlibTo } catch (_: NoSuchMethodError) {  }
    try { this[X_WRITE_DEPENDENCIES_TO] = arguments.saveDependenciesPath } catch (_: NoSuchMethodError) {  }
    try { this[ENABLE_ASSERTIONS] = arguments.enableAssertions } catch (_: NoSuchMethodError) {  }
    try { this[ENTRY] = arguments.mainPackage } catch (_: NoSuchMethodError) {  }
    try { this[FRIEND_MODULES] = arguments.friendModules } catch (_: NoSuchMethodError) {  }
    try { this[G] = arguments.debug } catch (_: NoSuchMethodError) {  }
    try { this[GENERATE_NO_EXIT_TEST_RUNNER] = arguments.generateNoExitTestRunner } catch (_: NoSuchMethodError) {  }
    try { this[GENERATE_TEST_RUNNER] = arguments.generateTestRunner } catch (_: NoSuchMethodError) {  }
    try { this[GENERATE_WORKER_TEST_RUNNER] = arguments.generateWorkerTestRunner } catch (_: NoSuchMethodError) {  }
    try { this[INCLUDE_BINARY] = arguments.includeBinaries } catch (_: NoSuchMethodError) {  }
    try { this[LIBRARY] = arguments.libraries } catch (_: NoSuchMethodError) {  }
    try { this[LIBRARY_VERSION] = arguments.getUsingReflection<String?>("libraryVersion") } catch (_: NoSuchMethodError) {  }
    try { this[LINKER_OPTION] = arguments.singleLinkerArguments } catch (_: NoSuchMethodError) {  }
    try { this[LINKER_OPTIONS] = arguments.linkerArguments } catch (_: NoSuchMethodError) {  }
    try { this[LIST_TARGETS] = arguments.listTargets } catch (_: NoSuchMethodError) {  }
    try { this[MANIFEST] = arguments.manifestFile } catch (_: NoSuchMethodError) {  }
    try { this[MEMORY_MODEL] = arguments.memoryModel } catch (_: NoSuchMethodError) {  }
    try { this[MODULE_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { this[NATIVE_LIBRARY] = arguments.nativeLibraries } catch (_: NoSuchMethodError) {  }
    try { this[NO_DEFAULT_LIBS] = arguments.nodefaultlibs } catch (_: NoSuchMethodError) {  }
    try { this[NO_ENDORSED_LIBS] = arguments.getUsingReflection<Boolean>("noendorsedlibs") } catch (_: NoSuchMethodError) {  }
    try { this[NOMAIN] = arguments.nomain } catch (_: NoSuchMethodError) {  }
    try { this[NOPACK] = arguments.nopack } catch (_: NoSuchMethodError) {  }
    try { this[NOSTDLIB] = arguments.nostdlib } catch (_: NoSuchMethodError) {  }
    try { this[OPT] = arguments.optimization } catch (_: NoSuchMethodError) {  }
    try { this[OUTPUT] = arguments.outputName } catch (_: NoSuchMethodError) {  }
    try { this[PRODUCE] = arguments.produce } catch (_: NoSuchMethodError) {  }
    try { this[TARGET] = arguments.target } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2NativeCompilerArguments = K2NativeCompilerArguments()): K2NativeCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    if (X_ADD_CACHE in this) { arguments.libraryToAddToCache = get(X_ADD_CACHE)}
    if (X_ADD_LIGHT_DEBUG in this) { arguments.lightDebugString = get(X_ADD_LIGHT_DEBUG)}
    if (X_ALLOCATOR in this) { arguments.allocator = get(X_ALLOCATOR)}
    if (X_AUTO_CACHE_DIR in this) { arguments.autoCacheDir = get(X_AUTO_CACHE_DIR)}
    if (X_AUTO_CACHE_FROM in this) { arguments.autoCacheableFrom = get(X_AUTO_CACHE_FROM) ?: emptyArray()}
    if (X_BINARY in this) { arguments.binaryOptions = get(X_BINARY) ?: emptyArray()}
    try { if (X_BUNDLE_ID in this) { arguments.setUsingReflection("bundleId", get(X_BUNDLE_ID))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_BUNDLE_ID. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_CACHE_DIRECTORY in this) { arguments.cacheDirectories = get(X_CACHE_DIRECTORY) ?: emptyArray()}
    if (X_CACHED_LIBRARY in this) { arguments.cachedLibraries = get(X_CACHED_LIBRARY) ?: emptyArray()}
    if (X_CHECK_DEPENDENCIES in this) { arguments.checkDependencies = get(X_CHECK_DEPENDENCIES)}
    if (X_CHECK_STATE_AT_EXTERNAL_CALLS in this) { arguments.checkExternalCalls = get(X_CHECK_STATE_AT_EXTERNAL_CALLS)}
    if (X_COMPILE_FROM_BITCODE in this) { arguments.compileFromBitcode = get(X_COMPILE_FROM_BITCODE)}
    if (X_DEBUG_INFO_VERSION in this) { arguments.debugInfoFormatVersion = get(X_DEBUG_INFO_VERSION).toString()}
    if (X_DEBUG_PREFIX_MAP in this) { arguments.debugPrefixMap = get(X_DEBUG_PREFIX_MAP) ?: emptyArray()}
    try { if (X_DESTROY_RUNTIME_MODE in this) { arguments.setUsingReflection("destroyRuntimeMode", get(X_DESTROY_RUNTIME_MODE))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DESTROY_RUNTIME_MODE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_DUMP_BUILT_CACHES_TO in this) { arguments.dumpBuiltCachesTo = get(X_DUMP_BUILT_CACHES_TO)}
    if (X_DUMP_TESTS_TO in this) { arguments.testDumpOutputPath = get(X_DUMP_TESTS_TO)}
    if (X_EMIT_LAZY_OBJC_HEADER in this) { arguments.emitLazyObjCHeader = get(X_EMIT_LAZY_OBJC_HEADER)}
    if (X_EXPORT_KDOC in this) { arguments.exportKDoc = get(X_EXPORT_KDOC)}
    if (X_EXPORT_LIBRARY in this) { arguments.exportedLibraries = get(X_EXPORT_LIBRARY) ?: emptyArray()}
    if (X_EXTERNAL_DEPENDENCIES in this) { arguments.externalDependencies = get(X_EXTERNAL_DEPENDENCIES)}
    if (X_FILE_TO_CACHE in this) { arguments.filesToCache = get(X_FILE_TO_CACHE) ?: emptyArray()}
    if (X_FRAMEWORK_IMPORT_HEADER in this) { arguments.frameworkImportHeaders = get(X_FRAMEWORK_IMPORT_HEADER) ?: emptyArray()}
    if (X_G_GENERATE_DEBUG_TRAMPOLINE in this) { arguments.generateDebugTrampolineString = get(X_G_GENERATE_DEBUG_TRAMPOLINE)}
    try { if (X_G0 in this) { arguments.setUsingReflection("lightDebugDeprecated", get(X_G0))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_G0. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    if (X_GC in this) { arguments.gc = get(X_GC)}
    if (X_HEADER_KLIB_PATH in this) { arguments.headerKlibPath = get(X_HEADER_KLIB_PATH)}
    if (X_IC_CACHE_DIR in this) { arguments.incrementalCacheDir = get(X_IC_CACHE_DIR)}
    if (X_INCLUDE in this) { arguments.includes = get(X_INCLUDE) ?: emptyArray()}
    if (X_IR_PROPERTY_LAZY_INITIALIZATION in this) { arguments.propertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION)}
    if (X_KONAN_DATA_DIR in this) { arguments.konanDataDir = get(X_KONAN_DATA_DIR)}
    if (X_LLVM_LTO_PASSES in this) { arguments.llvmLTOPasses = get(X_LLVM_LTO_PASSES)}
    if (X_LLVM_MODULE_PASSES in this) { arguments.llvmModulePasses = get(X_LLVM_MODULE_PASSES)}
    if (X_LLVM_VARIANT in this) { arguments.llvmVariant = get(X_LLVM_VARIANT)}
    if (X_MAKE_PER_FILE_CACHE in this) { arguments.makePerFileCache = get(X_MAKE_PER_FILE_CACHE)}
    if (X_MANIFEST_NATIVE_TARGETS in this) { arguments.manifestNativeTargets = get(X_MANIFEST_NATIVE_TARGETS) ?: emptyArray()}
    if (X_NO_OBJC_GENERICS in this) { arguments.noObjcGenerics = get(X_NO_OBJC_GENERICS)}
    if (X_OMIT_FRAMEWORK_BINARY in this) { arguments.omitFrameworkBinary = get(X_OMIT_FRAMEWORK_BINARY)}
    if (X_OVERRIDE_CLANG_OPTIONS in this) { arguments.clangOptions = get(X_OVERRIDE_CLANG_OPTIONS) ?: emptyArray()}
    if (X_OVERRIDE_KONAN_PROPERTIES in this) { arguments.overrideKonanProperties = get(X_OVERRIDE_KONAN_PROPERTIES) ?: emptyArray()}
    if (X_PRE_LINK_CACHES in this) { arguments.preLinkCaches = get(X_PRE_LINK_CACHES)}
    try { if (X_PURGE_USER_LIBS in this) { arguments.setUsingReflection("purgeUserLibs", get(X_PURGE_USER_LIBS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_PURGE_USER_LIBS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_READ_DEPENDENCIES_FROM in this) { arguments.serializedDependencies = get(X_READ_DEPENDENCIES_FROM)}
    if (X_REFINES_PATHS in this) { arguments.refinesPaths = get(X_REFINES_PATHS) ?: emptyArray()}
    if (X_RUNTIME in this) { arguments.runtimeFile = get(X_RUNTIME)}
    if (X_RUNTIME_LOGS in this) { arguments.runtimeLogs = get(X_RUNTIME_LOGS)}
    if (X_SAVE_LLVM_IR_AFTER in this) { arguments.saveLlvmIrAfter = get(X_SAVE_LLVM_IR_AFTER) ?: emptyArray()}
    if (X_SAVE_LLVM_IR_DIRECTORY in this) { arguments.saveLlvmIrDirectory = get(X_SAVE_LLVM_IR_DIRECTORY)}
    if (X_SHORT_MODULE_NAME in this) { arguments.shortModuleName = get(X_SHORT_MODULE_NAME)}
    if (X_STATIC_FRAMEWORK in this) { arguments.staticFramework = get(X_STATIC_FRAMEWORK)}
    if (X_VERIFY_BITCODE in this) { arguments.verifyBitCode = get(X_VERIFY_BITCODE)}
    if (X_VERIFY_COMPILER in this) { arguments.verifyCompiler = get(X_VERIFY_COMPILER)}
    try { if (X_WORKER_EXCEPTION_HANDLING in this) { arguments.setUsingReflection("workerExceptionHandling", get(X_WORKER_EXCEPTION_HANDLING))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WORKER_EXCEPTION_HANDLING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    if (X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO in this) { arguments.writeDependenciesOfProducedKlibTo = get(X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)}
    if (X_WRITE_DEPENDENCIES_TO in this) { arguments.saveDependenciesPath = get(X_WRITE_DEPENDENCIES_TO)}
    if (ENABLE_ASSERTIONS in this) { arguments.enableAssertions = get(ENABLE_ASSERTIONS)}
    if (ENTRY in this) { arguments.mainPackage = get(ENTRY)}
    if (FRIEND_MODULES in this) { arguments.friendModules = get(FRIEND_MODULES)}
    if (G in this) { arguments.debug = get(G)}
    if (GENERATE_NO_EXIT_TEST_RUNNER in this) { arguments.generateNoExitTestRunner = get(GENERATE_NO_EXIT_TEST_RUNNER)}
    if (GENERATE_TEST_RUNNER in this) { arguments.generateTestRunner = get(GENERATE_TEST_RUNNER)}
    if (GENERATE_WORKER_TEST_RUNNER in this) { arguments.generateWorkerTestRunner = get(GENERATE_WORKER_TEST_RUNNER)}
    if (INCLUDE_BINARY in this) { arguments.includeBinaries = get(INCLUDE_BINARY) ?: emptyArray()}
    if (LIBRARY in this) { arguments.libraries = get(LIBRARY) ?: emptyArray()}
    try { if (LIBRARY_VERSION in this) { arguments.setUsingReflection("libraryVersion", get(LIBRARY_VERSION))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: LIBRARY_VERSION. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    if (LINKER_OPTION in this) { arguments.singleLinkerArguments = get(LINKER_OPTION) ?: emptyArray()}
    if (LINKER_OPTIONS in this) { arguments.linkerArguments = get(LINKER_OPTIONS) ?: emptyArray()}
    if (MANIFEST in this) { arguments.manifestFile = get(MANIFEST)}
    if (MEMORY_MODEL in this) { arguments.memoryModel = get(MEMORY_MODEL)}
    if (MODULE_NAME in this) { arguments.moduleName = get(MODULE_NAME)}
    if (NATIVE_LIBRARY in this) { arguments.nativeLibraries = get(NATIVE_LIBRARY) ?: emptyArray()}
    if (NO_DEFAULT_LIBS in this) { arguments.nodefaultlibs = get(NO_DEFAULT_LIBS)}
    try { if (NO_ENDORSED_LIBS in this) { arguments.setUsingReflection("noendorsedlibs", get(NO_ENDORSED_LIBS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: NO_ENDORSED_LIBS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    if (NOMAIN in this) { arguments.nomain = get(NOMAIN)}
    if (NOPACK in this) { arguments.nopack = get(NOPACK)}
    if (NOSTDLIB in this) { arguments.nostdlib = get(NOSTDLIB)}
    if (OPT in this) { arguments.optimization = get(OPT)}
    if (OUTPUT in this) { arguments.outputName = get(OUTPUT)}
    if (PRODUCE in this) { arguments.produce = get(PRODUCE)}
    if (TARGET in this) { arguments.target = get(TARGET)}
    return arguments
  }

  @Deprecated(
    message = "This method is deprecated. Use applyCommandLineArguments instead.",
    level = DeprecationLevel.WARNING,
  )
  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2NativeCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, K2NativeCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  @DelicateBuildToolsApi
  override fun applyCommandLineArguments(arguments: List<String>) {
    val compilerArgs = toCompilerArguments()
    parseCommandLineArguments(arguments, compilerArgs, false)
    handleCustomPluginArguments(this, compilerArgs)
    collectRestrictedArgViolations(compilerArgs, K2NativeCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)
    return arguments
  }

  /**
   * Returns a sorted list of compiler argument strings representing only the arguments
   * that affect the compilation outcome (i.e. those with [affectsCompilationOutcome][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument.affectsCompilationOutcome] set to true).
   * Arguments with default values are omitted from the output, because [toCompilerArgumentsAffectingOutcome]
   * only sets arguments that have been explicitly assigned, and [compilerToArgumentStrings][org.jetbrains.kotlin.compilerRunner.toArgumentStrings]
   * skips properties whose value matches the default.
   */
  public fun toCompilationInputs(): List<String> = toCompilerArgumentsAffectingOutcome().compilerToArgumentStrings(allowArgFileInValues = false).sorted()

  public class NativeArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_ADD_CACHE: NativeArgument<String?> = NativeArgument("X_ADD_CACHE")

    public val X_ADD_LIGHT_DEBUG: NativeArgument<String?> = NativeArgument("X_ADD_LIGHT_DEBUG")

    public val X_ALLOCATOR: NativeArgument<String?> = NativeArgument("X_ALLOCATOR")

    public val X_AUTO_CACHE_DIR: NativeArgument<String?> = NativeArgument("X_AUTO_CACHE_DIR")

    public val X_AUTO_CACHE_FROM: NativeArgument<Array<String>?> =
        NativeArgument("X_AUTO_CACHE_FROM")

    public val X_BACKEND_THREADS: NativeArgument<Int> = NativeArgument("X_BACKEND_THREADS")

    public val X_BINARY: NativeArgument<Array<String>?> = NativeArgument("X_BINARY")

    public val X_BUNDLE_ID: NativeArgument<String?> = NativeArgument("X_BUNDLE_ID")

    public val X_CACHE_DIRECTORY: NativeArgument<Array<String>?> =
        NativeArgument("X_CACHE_DIRECTORY")

    public val X_CACHED_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("X_CACHED_LIBRARY")

    public val X_CHECK_DEPENDENCIES: NativeArgument<Boolean> =
        NativeArgument("X_CHECK_DEPENDENCIES")

    public val X_CHECK_STATE_AT_EXTERNAL_CALLS: NativeArgument<Boolean> =
        NativeArgument("X_CHECK_STATE_AT_EXTERNAL_CALLS")

    public val X_COMPILE_FROM_BITCODE: NativeArgument<String?> =
        NativeArgument("X_COMPILE_FROM_BITCODE")

    public val X_DEBUG_INFO_VERSION: NativeArgument<Int> = NativeArgument("X_DEBUG_INFO_VERSION")

    public val X_DEBUG_PREFIX_MAP: NativeArgument<Array<String>?> =
        NativeArgument("X_DEBUG_PREFIX_MAP")

    public val X_DESTROY_RUNTIME_MODE: NativeArgument<String?> =
        NativeArgument("X_DESTROY_RUNTIME_MODE")

    public val X_DUMP_BUILT_CACHES_TO: NativeArgument<String?> =
        NativeArgument("X_DUMP_BUILT_CACHES_TO")

    public val X_DUMP_TESTS_TO: NativeArgument<String?> = NativeArgument("X_DUMP_TESTS_TO")

    public val X_EMIT_LAZY_OBJC_HEADER: NativeArgument<String?> =
        NativeArgument("X_EMIT_LAZY_OBJC_HEADER")

    public val X_EXPORT_KDOC: NativeArgument<Boolean> = NativeArgument("X_EXPORT_KDOC")

    public val X_EXPORT_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("X_EXPORT_LIBRARY")

    public val X_EXTERNAL_DEPENDENCIES: NativeArgument<String?> =
        NativeArgument("X_EXTERNAL_DEPENDENCIES")

    public val X_FILE_TO_CACHE: NativeArgument<Array<String>?> = NativeArgument("X_FILE_TO_CACHE")

    public val X_FRAMEWORK_IMPORT_HEADER: NativeArgument<Array<String>?> =
        NativeArgument("X_FRAMEWORK_IMPORT_HEADER")

    public val X_G_GENERATE_DEBUG_TRAMPOLINE: NativeArgument<String?> =
        NativeArgument("X_G_GENERATE_DEBUG_TRAMPOLINE")

    public val X_G0: NativeArgument<Boolean> = NativeArgument("X_G0")

    public val X_GC: NativeArgument<String?> = NativeArgument("X_GC")

    public val X_HEADER_KLIB_PATH: NativeArgument<String?> = NativeArgument("X_HEADER_KLIB_PATH")

    public val X_IC_CACHE_DIR: NativeArgument<String?> = NativeArgument("X_IC_CACHE_DIR")

    public val X_INCLUDE: NativeArgument<Array<String>?> = NativeArgument("X_INCLUDE")

    public val X_IR_PROPERTY_LAZY_INITIALIZATION: NativeArgument<String?> =
        NativeArgument("X_IR_PROPERTY_LAZY_INITIALIZATION")

    public val X_KONAN_DATA_DIR: NativeArgument<String?> = NativeArgument("X_KONAN_DATA_DIR")

    public val X_LLVM_LTO_PASSES: NativeArgument<String?> = NativeArgument("X_LLVM_LTO_PASSES")

    public val X_LLVM_MODULE_PASSES: NativeArgument<String?> =
        NativeArgument("X_LLVM_MODULE_PASSES")

    public val X_LLVM_VARIANT: NativeArgument<String?> = NativeArgument("X_LLVM_VARIANT")

    public val X_MAKE_PER_FILE_CACHE: NativeArgument<Boolean> =
        NativeArgument("X_MAKE_PER_FILE_CACHE")

    public val X_MANIFEST_NATIVE_TARGETS: NativeArgument<Array<String>?> =
        NativeArgument("X_MANIFEST_NATIVE_TARGETS")

    public val X_NO_OBJC_GENERICS: NativeArgument<Boolean> = NativeArgument("X_NO_OBJC_GENERICS")

    public val X_OMIT_FRAMEWORK_BINARY: NativeArgument<Boolean> =
        NativeArgument("X_OMIT_FRAMEWORK_BINARY")

    public val X_OVERRIDE_CLANG_OPTIONS: NativeArgument<Array<String>?> =
        NativeArgument("X_OVERRIDE_CLANG_OPTIONS")

    public val X_OVERRIDE_KONAN_PROPERTIES: NativeArgument<Array<String>?> =
        NativeArgument("X_OVERRIDE_KONAN_PROPERTIES")

    public val X_PRE_LINK_CACHES: NativeArgument<String?> = NativeArgument("X_PRE_LINK_CACHES")

    public val X_PRINT_BITCODE: NativeArgument<Boolean> = NativeArgument("X_PRINT_BITCODE")

    public val X_PRINT_FILES: NativeArgument<Boolean> = NativeArgument("X_PRINT_FILES")

    public val X_PRINT_IR: NativeArgument<Boolean> = NativeArgument("X_PRINT_IR")

    public val X_PURGE_USER_LIBS: NativeArgument<Boolean> = NativeArgument("X_PURGE_USER_LIBS")

    public val X_READ_DEPENDENCIES_FROM: NativeArgument<String?> =
        NativeArgument("X_READ_DEPENDENCIES_FROM")

    public val X_REFINES_PATHS: NativeArgument<Array<String>?> = NativeArgument("X_REFINES_PATHS")

    public val X_RUNTIME: NativeArgument<String?> = NativeArgument("X_RUNTIME")

    public val X_RUNTIME_LOGS: NativeArgument<String?> = NativeArgument("X_RUNTIME_LOGS")

    public val X_SAVE_LLVM_IR_AFTER: NativeArgument<Array<String>?> =
        NativeArgument("X_SAVE_LLVM_IR_AFTER")

    public val X_SAVE_LLVM_IR_DIRECTORY: NativeArgument<String?> =
        NativeArgument("X_SAVE_LLVM_IR_DIRECTORY")

    public val X_SHORT_MODULE_NAME: NativeArgument<String?> = NativeArgument("X_SHORT_MODULE_NAME")

    public val X_STATIC_FRAMEWORK: NativeArgument<Boolean> = NativeArgument("X_STATIC_FRAMEWORK")

    public val X_TEMPORARY_FILES_DIR: NativeArgument<String?> =
        NativeArgument("X_TEMPORARY_FILES_DIR")

    public val X_VERIFY_BITCODE: NativeArgument<Boolean> = NativeArgument("X_VERIFY_BITCODE")

    public val X_VERIFY_COMPILER: NativeArgument<String?> = NativeArgument("X_VERIFY_COMPILER")

    public val X_WORKER_EXCEPTION_HANDLING: NativeArgument<String?> =
        NativeArgument("X_WORKER_EXCEPTION_HANDLING")

    public val X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO: NativeArgument<String?> =
        NativeArgument("X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    public val X_WRITE_DEPENDENCIES_TO: NativeArgument<String?> =
        NativeArgument("X_WRITE_DEPENDENCIES_TO")

    public val ENABLE_ASSERTIONS: NativeArgument<Boolean> = NativeArgument("ENABLE_ASSERTIONS")

    public val ENTRY: NativeArgument<String?> = NativeArgument("ENTRY")

    public val FRIEND_MODULES: NativeArgument<String?> = NativeArgument("FRIEND_MODULES")

    public val G: NativeArgument<Boolean> = NativeArgument("G")

    public val GENERATE_NO_EXIT_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_NO_EXIT_TEST_RUNNER")

    public val GENERATE_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_TEST_RUNNER")

    public val GENERATE_WORKER_TEST_RUNNER: NativeArgument<Boolean> =
        NativeArgument("GENERATE_WORKER_TEST_RUNNER")

    public val INCLUDE_BINARY: NativeArgument<Array<String>?> = NativeArgument("INCLUDE_BINARY")

    public val LIBRARY: NativeArgument<Array<String>?> = NativeArgument("LIBRARY")

    public val LIBRARY_VERSION: NativeArgument<String?> = NativeArgument("LIBRARY_VERSION")

    public val LINKER_OPTION: NativeArgument<Array<String>?> = NativeArgument("LINKER_OPTION")

    public val LINKER_OPTIONS: NativeArgument<Array<String>?> = NativeArgument("LINKER_OPTIONS")

    public val LIST_TARGETS: NativeArgument<Boolean> = NativeArgument("LIST_TARGETS")

    public val MANIFEST: NativeArgument<String?> = NativeArgument("MANIFEST")

    public val MEMORY_MODEL: NativeArgument<String?> = NativeArgument("MEMORY_MODEL")

    public val MODULE_NAME: NativeArgument<String?> = NativeArgument("MODULE_NAME")

    public val NATIVE_LIBRARY: NativeArgument<Array<String>?> = NativeArgument("NATIVE_LIBRARY")

    public val NO_DEFAULT_LIBS: NativeArgument<Boolean> = NativeArgument("NO_DEFAULT_LIBS")

    public val NO_ENDORSED_LIBS: NativeArgument<Boolean> = NativeArgument("NO_ENDORSED_LIBS")

    public val NOMAIN: NativeArgument<Boolean> = NativeArgument("NOMAIN")

    public val NOPACK: NativeArgument<Boolean> = NativeArgument("NOPACK")

    public val NOSTDLIB: NativeArgument<Boolean> = NativeArgument("NOSTDLIB")

    public val OPT: NativeArgument<Boolean> = NativeArgument("OPT")

    public val OUTPUT: NativeArgument<String?> = NativeArgument("OUTPUT")

    public val PRODUCE: NativeArgument<String?> = NativeArgument("PRODUCE")

    public val TARGET: NativeArgument<String?> = NativeArgument("TARGET")
  }
}
