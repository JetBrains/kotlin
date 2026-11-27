// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.io.File
import java.lang.IllegalStateException
import java.nio.`file`.Path
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
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.toTypedArray
import kotlin.text.split
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.CompilerOutputKind
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.EnableDisableMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.KonanTarget
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NativeAllocationMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NativeGcMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NativeMemoryModel
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerKlibArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.appendFatalErrors
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal class NativeArgumentsImpl() : CommonKlibBasedArgumentsImpl(), NativeCompilerArguments,
    NativeCompilerArguments.Builder, NativeCompilerKlibArguments,
    NativeCompilerKlibArguments.Builder, DeepCopyable<NativeArgumentsImpl> {
  @Transient
  private val defaultArguments: K2NativeCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_ADD_CACHE")
  internal var libraryToAddToCache: Path? =
      defaultArguments.libraryToAddToCache?.let { kotlin.io.path.Path(it) }

  @SerialName("X_ADD_LIGHT_DEBUG")
  internal var lightDebugString: EnableDisableMode? =
      defaultArguments.lightDebugString?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::lightDebugString, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xadd-light-debug value: $it") }

  @SerialName("X_ALLOCATOR")
  internal var allocator: NativeAllocationMode? =
      defaultArguments.allocator?.let { NativeAllocationMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::allocator, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xallocator value: $it") }

  @SerialName("X_AUTO_CACHE_DIR")
  internal var autoCacheDir: Path? = defaultArguments.autoCacheDir?.let { kotlin.io.path.Path(it) }

  @SerialName("X_AUTO_CACHE_FROM")
  internal var autoCacheableFrom: List<Path> =
      defaultArguments.autoCacheableFrom.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_BACKEND_THREADS")
  internal var backendThreads: Int = defaultArguments.backendThreads.let { it.toInt() }

  @SerialName("X_BINARY")
  internal var binaryOptions: Array<String>? = defaultArguments.binaryOptions

  @SerialName("X_CACHE_DIRECTORY")
  internal var cacheDirectories: List<Path> =
      defaultArguments.cacheDirectories.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_CACHED_LIBRARY")
  internal var cachedLibraries: Array<String>? = defaultArguments.cachedLibraries

  @SerialName("X_CHECK_DEPENDENCIES")
  internal var checkDependencies: Boolean = defaultArguments.checkDependencies

  @SerialName("X_CHECK_STATE_AT_EXTERNAL_CALLS")
  internal var checkExternalCalls: Boolean = defaultArguments.checkExternalCalls

  @SerialName("X_COMPILE_FROM_BITCODE")
  internal var compileFromBitcode: Path? =
      defaultArguments.compileFromBitcode?.let { kotlin.io.path.Path(it) }

  @SerialName("X_DEBUG_INFO_VERSION")
  internal var debugInfoFormatVersion: Int =
      defaultArguments.debugInfoFormatVersion.let { it.toInt() }

  @SerialName("X_DEBUG_PREFIX_MAP")
  internal var debugPrefixMap: Array<String>? = defaultArguments.debugPrefixMap

  @SerialName("X_DUMP_BUILT_CACHES_TO")
  internal var dumpBuiltCachesTo: Path? =
      defaultArguments.dumpBuiltCachesTo?.let { kotlin.io.path.Path(it) }

  @SerialName("X_DUMP_TESTS_TO")
  internal var testDumpOutputPath: Path? =
      defaultArguments.testDumpOutputPath?.let { kotlin.io.path.Path(it) }

  @SerialName("X_EMIT_LAZY_OBJC_HEADER")
  internal var emitLazyObjCHeader: Path? =
      defaultArguments.emitLazyObjCHeader?.let { kotlin.io.path.Path(it) }

  @SerialName("X_EXPORT_KDOC")
  internal var exportKDoc: Boolean = defaultArguments.exportKDoc

  @SerialName("X_EXPORT_LIBRARY")
  internal var exportedLibraries: List<Path> =
      defaultArguments.exportedLibraries.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_EXTERNAL_DEPENDENCIES")
  internal var externalDependencies: Path? =
      defaultArguments.externalDependencies?.let { kotlin.io.path.Path(it) }

  @SerialName("X_FILE_TO_CACHE")
  internal var filesToCache: Array<String>? = defaultArguments.filesToCache

  @SerialName("X_FRAMEWORK_IMPORT_HEADER")
  internal var frameworkImportHeaders: Array<String>? = defaultArguments.frameworkImportHeaders

  @SerialName("X_G_GENERATE_DEBUG_TRAMPOLINE")
  internal var generateDebugTrampolineString: EnableDisableMode? =
      defaultArguments.generateDebugTrampolineString?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::generateDebugTrampolineString, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xg-generate-debug-trampoline value: $it") }

  @SerialName("X_GC")
  @Suppress("DEPRECATION")
  internal var gc: NativeGcMode? =
      defaultArguments.gc?.let { NativeGcMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::gc, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xgc value: $it") }

  @SerialName("X_HEADER_KLIB_PATH")
  internal var headerKlibPath: Path? =
      defaultArguments.headerKlibPath?.let { kotlin.io.path.Path(it) }

  @SerialName("X_IC_CACHE_DIR")
  internal var incrementalCacheDir: Path? =
      defaultArguments.incrementalCacheDir?.let { kotlin.io.path.Path(it) }

  @SerialName("X_INCLUDE")
  internal var includes: List<Path> =
      defaultArguments.includes.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_IR_PROPERTY_LAZY_INITIALIZATION")
  internal var propertyLazyInitialization: EnableDisableMode? =
      defaultArguments.propertyLazyInitialization?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::propertyLazyInitialization, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-property-lazy-initialization value: $it") }

  @SerialName("X_KONAN_DATA_DIR")
  internal var konanDataDir: Path? = defaultArguments.konanDataDir?.let { kotlin.io.path.Path(it) }

  @SerialName("X_LLVM_LTO_PASSES")
  internal var llvmLTOPasses: String? = defaultArguments.llvmLTOPasses

  @SerialName("X_LLVM_MODULE_PASSES")
  internal var llvmModulePasses: String? = defaultArguments.llvmModulePasses

  @SerialName("X_LLVM_VARIANT")
  internal var llvmVariant: String? = defaultArguments.llvmVariant

  @SerialName("X_MAKE_PER_FILE_CACHE")
  internal var makePerFileCache: Boolean = defaultArguments.makePerFileCache

  @SerialName("X_MANIFEST_NATIVE_TARGETS")
  internal var manifestNativeTargets: List<KonanTarget> =
      defaultArguments.manifestNativeTargets.map { KonanTarget.values().firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -Xmanifest-native-targets value: $it") }

  @SerialName("X_NO_OBJC_GENERICS")
  internal var noObjcGenerics: Boolean = defaultArguments.noObjcGenerics

  @SerialName("X_OMIT_FRAMEWORK_BINARY")
  internal var omitFrameworkBinary: Boolean = defaultArguments.omitFrameworkBinary

  @SerialName("X_OVERRIDE_CLANG_OPTIONS")
  internal var clangOptions: Array<String>? = defaultArguments.clangOptions

  @SerialName("X_OVERRIDE_KONAN_PROPERTIES")
  internal var overrideKonanProperties: Array<String>? = defaultArguments.overrideKonanProperties

  @SerialName("X_PRE_LINK_CACHES")
  internal var preLinkCaches: EnableDisableMode? =
      defaultArguments.preLinkCaches?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::preLinkCaches, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpre-link-caches value: $it") }

  @SerialName("X_PRINT_BITCODE")
  internal var printBitCode: Boolean = defaultArguments.printBitCode

  @SerialName("X_PRINT_FILES")
  internal var printFiles: Boolean = defaultArguments.printFiles

  @SerialName("X_PRINT_IR")
  internal var printIr: Boolean = defaultArguments.printIr

  @SerialName("X_READ_DEPENDENCIES_FROM")
  internal var serializedDependencies: Path? =
      defaultArguments.serializedDependencies?.let { kotlin.io.path.Path(it) }

  @SerialName("X_REFINES_PATHS")
  internal var refinesPaths: List<Path> =
      defaultArguments.refinesPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_RUNTIME")
  internal var runtimeFile: Path? = defaultArguments.runtimeFile?.let { kotlin.io.path.Path(it) }

  @SerialName("X_RUNTIME_LOGS")
  internal var runtimeLogs: String? = defaultArguments.runtimeLogs

  @SerialName("X_SAVE_LLVM_IR_AFTER")
  internal var saveLlvmIrAfter: Array<String>? = defaultArguments.saveLlvmIrAfter

  @SerialName("X_SAVE_LLVM_IR_DIRECTORY")
  internal var saveLlvmIrDirectory: Path? =
      defaultArguments.saveLlvmIrDirectory?.let { kotlin.io.path.Path(it) }

  @SerialName("X_SHORT_MODULE_NAME")
  internal var shortModuleName: String? = defaultArguments.shortModuleName

  @SerialName("X_STATIC_FRAMEWORK")
  internal var staticFramework: Boolean = defaultArguments.staticFramework

  @SerialName("X_TEMPORARY_FILES_DIR")
  internal var temporaryFilesDir: Path? =
      defaultArguments.temporaryFilesDir?.let { kotlin.io.path.Path(it) }

  @SerialName("X_VERIFY_BITCODE")
  internal var verifyBitCode: Boolean = defaultArguments.verifyBitCode

  @SerialName("X_VERIFY_COMPILER")
  internal var verifyCompiler: String? = defaultArguments.verifyCompiler

  @SerialName("X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")
  internal var writeDependenciesOfProducedKlibTo: Path? =
      defaultArguments.writeDependenciesOfProducedKlibTo?.let { kotlin.io.path.Path(it) }

  @SerialName("X_WRITE_DEPENDENCIES_TO")
  internal var saveDependenciesPath: Path? =
      defaultArguments.saveDependenciesPath?.let { kotlin.io.path.Path(it) }

  @SerialName("ENABLE_ASSERTIONS")
  internal var enableAssertions: Boolean = defaultArguments.enableAssertions

  @SerialName("ENTRY")
  internal var mainPackage: String? = defaultArguments.mainPackage

  @SerialName("FRIEND_MODULES")
  internal var friendModules: List<Path>? =
      defaultArguments.friendModules?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("G")
  internal var debug: Boolean = defaultArguments.debug

  @SerialName("GENERATE_NO_EXIT_TEST_RUNNER")
  internal var generateNoExitTestRunner: Boolean = defaultArguments.generateNoExitTestRunner

  @SerialName("GENERATE_TEST_RUNNER")
  internal var generateTestRunner: Boolean = defaultArguments.generateTestRunner

  @SerialName("GENERATE_WORKER_TEST_RUNNER")
  @Suppress("DEPRECATION")
  internal var generateWorkerTestRunner: Boolean = defaultArguments.generateWorkerTestRunner

  @SerialName("INCLUDE_BINARY")
  internal var includeBinaries: List<Path> =
      defaultArguments.includeBinaries.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("LIBRARY")
  internal var libraries: List<Path> =
      defaultArguments.libraries.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("LINKER_OPTION")
  internal var singleLinkerArguments: Array<String>? = defaultArguments.singleLinkerArguments

  @SerialName("LINKER_OPTIONS")
  internal var linkerArguments: Array<String>? = defaultArguments.linkerArguments

  @SerialName("LIST_TARGETS")
  internal var listTargets: Boolean = defaultArguments.listTargets

  @SerialName("MANIFEST")
  internal var manifestFile: Path? = defaultArguments.manifestFile?.let { kotlin.io.path.Path(it) }

  @SerialName("MEMORY_MODEL")
  @Suppress("DEPRECATION")
  internal var memoryModel: NativeMemoryModel? =
      defaultArguments.memoryModel?.let { NativeMemoryModel.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::memoryModel, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -memory-model value: $it") }

  @SerialName("MODULE_NAME")
  internal var moduleName: String? = defaultArguments.moduleName

  @SerialName("NATIVE_LIBRARY")
  internal var nativeLibraries: List<Path> =
      defaultArguments.nativeLibraries.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("NO_DEFAULT_LIBS")
  internal var nodefaultlibs: Boolean = defaultArguments.nodefaultlibs

  @SerialName("NOMAIN")
  internal var nomain: Boolean = defaultArguments.nomain

  @SerialName("NOPACK")
  internal var nopack: Boolean = defaultArguments.nopack

  @SerialName("NOSTDLIB")
  internal var nostdlib: Boolean = defaultArguments.nostdlib

  @SerialName("OPT")
  internal var optimization: Boolean = defaultArguments.optimization

  @SerialName("OUTPUT")
  internal var outputName: Path? = defaultArguments.outputName?.let { kotlin.io.path.Path(it) }

  @SerialName("PRODUCE")
  internal var produce: CompilerOutputKind? =
      defaultArguments.produce?.let { CompilerOutputKind.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::produce, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -produce value: $it") }

  @SerialName("TARGET")
  internal var target: KonanTarget? =
      defaultArguments.target?.let { KonanTarget.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") }

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  override fun createDefaultArguments(): K2NativeCompilerArguments = K2NativeCompilerArguments()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: NativeCompilerArguments.NativeCompilerArgument<V>): V = NativeArgumentValueAdapter.toApi(when (key.id) {
    "X_ADD_CACHE" -> libraryToAddToCache
    "X_ADD_LIGHT_DEBUG" -> lightDebugString
    "X_ALLOCATOR" -> allocator
    "X_AUTO_CACHE_DIR" -> autoCacheDir
    "X_AUTO_CACHE_FROM" -> autoCacheableFrom
    "X_BACKEND_THREADS" -> backendThreads
    "X_BINARY" -> binaryOptions
    "X_CACHE_DIRECTORY" -> cacheDirectories
    "X_CACHED_LIBRARY" -> cachedLibraries
    "X_CHECK_DEPENDENCIES" -> checkDependencies
    "X_CHECK_STATE_AT_EXTERNAL_CALLS" -> checkExternalCalls
    "X_COMPILE_FROM_BITCODE" -> compileFromBitcode
    "X_DEBUG_INFO_VERSION" -> debugInfoFormatVersion
    "X_DEBUG_PREFIX_MAP" -> debugPrefixMap
    "X_DUMP_BUILT_CACHES_TO" -> dumpBuiltCachesTo
    "X_DUMP_TESTS_TO" -> testDumpOutputPath
    "X_EMIT_LAZY_OBJC_HEADER" -> emitLazyObjCHeader
    "X_EXPORT_KDOC" -> exportKDoc
    "X_EXPORT_LIBRARY" -> exportedLibraries
    "X_EXTERNAL_DEPENDENCIES" -> externalDependencies
    "X_FILE_TO_CACHE" -> filesToCache
    "X_FRAMEWORK_IMPORT_HEADER" -> frameworkImportHeaders
    "X_G_GENERATE_DEBUG_TRAMPOLINE" -> generateDebugTrampolineString
    "X_GC" -> gc
    "X_HEADER_KLIB_PATH" -> headerKlibPath
    "X_IC_CACHE_DIR" -> incrementalCacheDir
    "X_INCLUDE" -> includes
    "X_IR_PROPERTY_LAZY_INITIALIZATION" -> propertyLazyInitialization
    "X_KONAN_DATA_DIR" -> konanDataDir
    "X_LLVM_LTO_PASSES" -> llvmLTOPasses
    "X_LLVM_MODULE_PASSES" -> llvmModulePasses
    "X_LLVM_VARIANT" -> llvmVariant
    "X_MAKE_PER_FILE_CACHE" -> makePerFileCache
    "X_MANIFEST_NATIVE_TARGETS" -> manifestNativeTargets
    "X_NO_OBJC_GENERICS" -> noObjcGenerics
    "X_OMIT_FRAMEWORK_BINARY" -> omitFrameworkBinary
    "X_OVERRIDE_CLANG_OPTIONS" -> clangOptions
    "X_OVERRIDE_KONAN_PROPERTIES" -> overrideKonanProperties
    "X_PRE_LINK_CACHES" -> preLinkCaches
    "X_PRINT_BITCODE" -> printBitCode
    "X_PRINT_FILES" -> printFiles
    "X_PRINT_IR" -> printIr
    "X_READ_DEPENDENCIES_FROM" -> serializedDependencies
    "X_REFINES_PATHS" -> refinesPaths
    "X_RUNTIME" -> runtimeFile
    "X_RUNTIME_LOGS" -> runtimeLogs
    "X_SAVE_LLVM_IR_AFTER" -> saveLlvmIrAfter
    "X_SAVE_LLVM_IR_DIRECTORY" -> saveLlvmIrDirectory
    "X_SHORT_MODULE_NAME" -> shortModuleName
    "X_STATIC_FRAMEWORK" -> staticFramework
    "X_TEMPORARY_FILES_DIR" -> temporaryFilesDir
    "X_VERIFY_BITCODE" -> verifyBitCode
    "X_VERIFY_COMPILER" -> verifyCompiler
    "X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO" -> writeDependenciesOfProducedKlibTo
    "X_WRITE_DEPENDENCIES_TO" -> saveDependenciesPath
    "ENABLE_ASSERTIONS" -> enableAssertions
    "ENTRY" -> mainPackage
    "FRIEND_MODULES" -> friendModules
    "G" -> debug
    "GENERATE_NO_EXIT_TEST_RUNNER" -> generateNoExitTestRunner
    "GENERATE_TEST_RUNNER" -> generateTestRunner
    "GENERATE_WORKER_TEST_RUNNER" -> generateWorkerTestRunner
    "INCLUDE_BINARY" -> includeBinaries
    "LIBRARY" -> libraries
    "LINKER_OPTION" -> singleLinkerArguments
    "LINKER_OPTIONS" -> linkerArguments
    "LIST_TARGETS" -> listTargets
    "MANIFEST" -> manifestFile
    "MEMORY_MODEL" -> memoryModel
    "MODULE_NAME" -> moduleName
    "NATIVE_LIBRARY" -> nativeLibraries
    "NO_DEFAULT_LIBS" -> nodefaultlibs
    "NOMAIN" -> nomain
    "NOPACK" -> nopack
    "NOSTDLIB" -> nostdlib
    "OPT" -> optimization
    "OUTPUT" -> outputName
    "PRODUCE" -> produce
    "TARGET" -> target
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: NativeCompilerArguments.NativeCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_ADD_CACHE" -> libraryToAddToCache = NativeArgumentValueAdapter.toImpl(value)
      "X_ADD_LIGHT_DEBUG" -> lightDebugString = NativeArgumentValueAdapter.toImpl(value)
      "X_ALLOCATOR" -> allocator = NativeArgumentValueAdapter.toImpl(value)
      "X_AUTO_CACHE_DIR" -> autoCacheDir = NativeArgumentValueAdapter.toImpl(value)
      "X_AUTO_CACHE_FROM" -> autoCacheableFrom = NativeArgumentValueAdapter.toImpl(value)
      "X_BACKEND_THREADS" -> backendThreads = NativeArgumentValueAdapter.toImpl(value)
      "X_BINARY" -> binaryOptions = NativeArgumentValueAdapter.toImpl(value)
      "X_CACHE_DIRECTORY" -> cacheDirectories = NativeArgumentValueAdapter.toImpl(value)
      "X_CACHED_LIBRARY" -> cachedLibraries = NativeArgumentValueAdapter.toImpl(value)
      "X_CHECK_DEPENDENCIES" -> checkDependencies = NativeArgumentValueAdapter.toImpl(value)
      "X_CHECK_STATE_AT_EXTERNAL_CALLS" -> checkExternalCalls = NativeArgumentValueAdapter.toImpl(value)
      "X_COMPILE_FROM_BITCODE" -> compileFromBitcode = NativeArgumentValueAdapter.toImpl(value)
      "X_DEBUG_INFO_VERSION" -> debugInfoFormatVersion = NativeArgumentValueAdapter.toImpl(value)
      "X_DEBUG_PREFIX_MAP" -> debugPrefixMap = NativeArgumentValueAdapter.toImpl(value)
      "X_DUMP_BUILT_CACHES_TO" -> dumpBuiltCachesTo = NativeArgumentValueAdapter.toImpl(value)
      "X_DUMP_TESTS_TO" -> testDumpOutputPath = NativeArgumentValueAdapter.toImpl(value)
      "X_EMIT_LAZY_OBJC_HEADER" -> emitLazyObjCHeader = NativeArgumentValueAdapter.toImpl(value)
      "X_EXPORT_KDOC" -> exportKDoc = NativeArgumentValueAdapter.toImpl(value)
      "X_EXPORT_LIBRARY" -> exportedLibraries = NativeArgumentValueAdapter.toImpl(value)
      "X_EXTERNAL_DEPENDENCIES" -> externalDependencies = NativeArgumentValueAdapter.toImpl(value)
      "X_FILE_TO_CACHE" -> filesToCache = NativeArgumentValueAdapter.toImpl(value)
      "X_FRAMEWORK_IMPORT_HEADER" -> frameworkImportHeaders = NativeArgumentValueAdapter.toImpl(value)
      "X_G_GENERATE_DEBUG_TRAMPOLINE" -> generateDebugTrampolineString = NativeArgumentValueAdapter.toImpl(value)
      "X_GC" -> gc = NativeArgumentValueAdapter.toImpl(value)
      "X_HEADER_KLIB_PATH" -> headerKlibPath = NativeArgumentValueAdapter.toImpl(value)
      "X_IC_CACHE_DIR" -> incrementalCacheDir = NativeArgumentValueAdapter.toImpl(value)
      "X_INCLUDE" -> includes = NativeArgumentValueAdapter.toImpl(value)
      "X_IR_PROPERTY_LAZY_INITIALIZATION" -> propertyLazyInitialization = NativeArgumentValueAdapter.toImpl(value)
      "X_KONAN_DATA_DIR" -> konanDataDir = NativeArgumentValueAdapter.toImpl(value)
      "X_LLVM_LTO_PASSES" -> llvmLTOPasses = NativeArgumentValueAdapter.toImpl(value)
      "X_LLVM_MODULE_PASSES" -> llvmModulePasses = NativeArgumentValueAdapter.toImpl(value)
      "X_LLVM_VARIANT" -> llvmVariant = NativeArgumentValueAdapter.toImpl(value)
      "X_MAKE_PER_FILE_CACHE" -> makePerFileCache = NativeArgumentValueAdapter.toImpl(value)
      "X_MANIFEST_NATIVE_TARGETS" -> manifestNativeTargets = NativeArgumentValueAdapter.toImpl(value)
      "X_NO_OBJC_GENERICS" -> noObjcGenerics = NativeArgumentValueAdapter.toImpl(value)
      "X_OMIT_FRAMEWORK_BINARY" -> omitFrameworkBinary = NativeArgumentValueAdapter.toImpl(value)
      "X_OVERRIDE_CLANG_OPTIONS" -> clangOptions = NativeArgumentValueAdapter.toImpl(value)
      "X_OVERRIDE_KONAN_PROPERTIES" -> overrideKonanProperties = NativeArgumentValueAdapter.toImpl(value)
      "X_PRE_LINK_CACHES" -> preLinkCaches = NativeArgumentValueAdapter.toImpl(value)
      "X_PRINT_BITCODE" -> printBitCode = NativeArgumentValueAdapter.toImpl(value)
      "X_PRINT_FILES" -> printFiles = NativeArgumentValueAdapter.toImpl(value)
      "X_PRINT_IR" -> printIr = NativeArgumentValueAdapter.toImpl(value)
      "X_READ_DEPENDENCIES_FROM" -> serializedDependencies = NativeArgumentValueAdapter.toImpl(value)
      "X_REFINES_PATHS" -> refinesPaths = NativeArgumentValueAdapter.toImpl(value)
      "X_RUNTIME" -> runtimeFile = NativeArgumentValueAdapter.toImpl(value)
      "X_RUNTIME_LOGS" -> runtimeLogs = NativeArgumentValueAdapter.toImpl(value)
      "X_SAVE_LLVM_IR_AFTER" -> saveLlvmIrAfter = NativeArgumentValueAdapter.toImpl(value)
      "X_SAVE_LLVM_IR_DIRECTORY" -> saveLlvmIrDirectory = NativeArgumentValueAdapter.toImpl(value)
      "X_SHORT_MODULE_NAME" -> shortModuleName = NativeArgumentValueAdapter.toImpl(value)
      "X_STATIC_FRAMEWORK" -> staticFramework = NativeArgumentValueAdapter.toImpl(value)
      "X_TEMPORARY_FILES_DIR" -> temporaryFilesDir = NativeArgumentValueAdapter.toImpl(value)
      "X_VERIFY_BITCODE" -> verifyBitCode = NativeArgumentValueAdapter.toImpl(value)
      "X_VERIFY_COMPILER" -> verifyCompiler = NativeArgumentValueAdapter.toImpl(value)
      "X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO" -> writeDependenciesOfProducedKlibTo = NativeArgumentValueAdapter.toImpl(value)
      "X_WRITE_DEPENDENCIES_TO" -> saveDependenciesPath = NativeArgumentValueAdapter.toImpl(value)
      "ENABLE_ASSERTIONS" -> enableAssertions = NativeArgumentValueAdapter.toImpl(value)
      "ENTRY" -> mainPackage = NativeArgumentValueAdapter.toImpl(value)
      "FRIEND_MODULES" -> friendModules = NativeArgumentValueAdapter.toImpl(value)
      "G" -> debug = NativeArgumentValueAdapter.toImpl(value)
      "GENERATE_NO_EXIT_TEST_RUNNER" -> generateNoExitTestRunner = NativeArgumentValueAdapter.toImpl(value)
      "GENERATE_TEST_RUNNER" -> generateTestRunner = NativeArgumentValueAdapter.toImpl(value)
      "GENERATE_WORKER_TEST_RUNNER" -> generateWorkerTestRunner = NativeArgumentValueAdapter.toImpl(value)
      "INCLUDE_BINARY" -> includeBinaries = NativeArgumentValueAdapter.toImpl(value)
      "LIBRARY" -> libraries = NativeArgumentValueAdapter.toImpl(value)
      "LINKER_OPTION" -> singleLinkerArguments = NativeArgumentValueAdapter.toImpl(value)
      "LINKER_OPTIONS" -> linkerArguments = NativeArgumentValueAdapter.toImpl(value)
      "LIST_TARGETS" -> listTargets = NativeArgumentValueAdapter.toImpl(value)
      "MANIFEST" -> manifestFile = NativeArgumentValueAdapter.toImpl(value)
      "MEMORY_MODEL" -> memoryModel = NativeArgumentValueAdapter.toImpl(value)
      "MODULE_NAME" -> moduleName = NativeArgumentValueAdapter.toImpl(value)
      "NATIVE_LIBRARY" -> nativeLibraries = NativeArgumentValueAdapter.toImpl(value)
      "NO_DEFAULT_LIBS" -> nodefaultlibs = NativeArgumentValueAdapter.toImpl(value)
      "NOMAIN" -> nomain = NativeArgumentValueAdapter.toImpl(value)
      "NOPACK" -> nopack = NativeArgumentValueAdapter.toImpl(value)
      "NOSTDLIB" -> nostdlib = NativeArgumentValueAdapter.toImpl(value)
      "OPT" -> optimization = NativeArgumentValueAdapter.toImpl(value)
      "OUTPUT" -> outputName = NativeArgumentValueAdapter.toImpl(value)
      "PRODUCE" -> produce = NativeArgumentValueAdapter.toImpl(value)
      "TARGET" -> target = NativeArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = NativeArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: NativeCompilerKlibArguments.NativeCompilerKlibArgument<V>): V = NativeArgumentValueAdapter.toApi(when (key.id) {
    "X_ADD_CACHE" -> libraryToAddToCache
    "X_ADD_LIGHT_DEBUG" -> lightDebugString
    "X_ALLOCATOR" -> allocator
    "X_AUTO_CACHE_DIR" -> autoCacheDir
    "X_AUTO_CACHE_FROM" -> autoCacheableFrom
    "X_BACKEND_THREADS" -> backendThreads
    "X_BINARY" -> binaryOptions
    "X_CACHE_DIRECTORY" -> cacheDirectories
    "X_CACHED_LIBRARY" -> cachedLibraries
    "X_CHECK_DEPENDENCIES" -> checkDependencies
    "X_CHECK_STATE_AT_EXTERNAL_CALLS" -> checkExternalCalls
    "X_COMPILE_FROM_BITCODE" -> compileFromBitcode
    "X_DEBUG_INFO_VERSION" -> debugInfoFormatVersion
    "X_DEBUG_PREFIX_MAP" -> debugPrefixMap
    "X_DUMP_BUILT_CACHES_TO" -> dumpBuiltCachesTo
    "X_DUMP_TESTS_TO" -> testDumpOutputPath
    "X_EMIT_LAZY_OBJC_HEADER" -> emitLazyObjCHeader
    "X_EXPORT_KDOC" -> exportKDoc
    "X_EXPORT_LIBRARY" -> exportedLibraries
    "X_EXTERNAL_DEPENDENCIES" -> externalDependencies
    "X_FILE_TO_CACHE" -> filesToCache
    "X_FRAMEWORK_IMPORT_HEADER" -> frameworkImportHeaders
    "X_G_GENERATE_DEBUG_TRAMPOLINE" -> generateDebugTrampolineString
    "X_GC" -> gc
    "X_HEADER_KLIB_PATH" -> headerKlibPath
    "X_IC_CACHE_DIR" -> incrementalCacheDir
    "X_INCLUDE" -> includes
    "X_IR_PROPERTY_LAZY_INITIALIZATION" -> propertyLazyInitialization
    "X_KONAN_DATA_DIR" -> konanDataDir
    "X_LLVM_LTO_PASSES" -> llvmLTOPasses
    "X_LLVM_MODULE_PASSES" -> llvmModulePasses
    "X_LLVM_VARIANT" -> llvmVariant
    "X_MAKE_PER_FILE_CACHE" -> makePerFileCache
    "X_MANIFEST_NATIVE_TARGETS" -> manifestNativeTargets
    "X_NO_OBJC_GENERICS" -> noObjcGenerics
    "X_OMIT_FRAMEWORK_BINARY" -> omitFrameworkBinary
    "X_OVERRIDE_CLANG_OPTIONS" -> clangOptions
    "X_OVERRIDE_KONAN_PROPERTIES" -> overrideKonanProperties
    "X_PRE_LINK_CACHES" -> preLinkCaches
    "X_PRINT_BITCODE" -> printBitCode
    "X_PRINT_FILES" -> printFiles
    "X_PRINT_IR" -> printIr
    "X_READ_DEPENDENCIES_FROM" -> serializedDependencies
    "X_REFINES_PATHS" -> refinesPaths
    "X_RUNTIME" -> runtimeFile
    "X_RUNTIME_LOGS" -> runtimeLogs
    "X_SAVE_LLVM_IR_AFTER" -> saveLlvmIrAfter
    "X_SAVE_LLVM_IR_DIRECTORY" -> saveLlvmIrDirectory
    "X_SHORT_MODULE_NAME" -> shortModuleName
    "X_STATIC_FRAMEWORK" -> staticFramework
    "X_TEMPORARY_FILES_DIR" -> temporaryFilesDir
    "X_VERIFY_BITCODE" -> verifyBitCode
    "X_VERIFY_COMPILER" -> verifyCompiler
    "X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO" -> writeDependenciesOfProducedKlibTo
    "X_WRITE_DEPENDENCIES_TO" -> saveDependenciesPath
    "ENABLE_ASSERTIONS" -> enableAssertions
    "ENTRY" -> mainPackage
    "FRIEND_MODULES" -> friendModules
    "G" -> debug
    "GENERATE_NO_EXIT_TEST_RUNNER" -> generateNoExitTestRunner
    "GENERATE_TEST_RUNNER" -> generateTestRunner
    "GENERATE_WORKER_TEST_RUNNER" -> generateWorkerTestRunner
    "INCLUDE_BINARY" -> includeBinaries
    "LIBRARY" -> libraries
    "LINKER_OPTION" -> singleLinkerArguments
    "LINKER_OPTIONS" -> linkerArguments
    "LIST_TARGETS" -> listTargets
    "MANIFEST" -> manifestFile
    "MEMORY_MODEL" -> memoryModel
    "MODULE_NAME" -> moduleName
    "NATIVE_LIBRARY" -> nativeLibraries
    "NO_DEFAULT_LIBS" -> nodefaultlibs
    "NOMAIN" -> nomain
    "NOPACK" -> nopack
    "NOSTDLIB" -> nostdlib
    "OPT" -> optimization
    "OUTPUT" -> outputName
    "PRODUCE" -> produce
    "TARGET" -> target
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: NativeCompilerKlibArguments.NativeCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_ADD_CACHE" -> libraryToAddToCache = NativeArgumentValueAdapter.toImpl(value)
      "X_ADD_LIGHT_DEBUG" -> lightDebugString = NativeArgumentValueAdapter.toImpl(value)
      "X_ALLOCATOR" -> allocator = NativeArgumentValueAdapter.toImpl(value)
      "X_AUTO_CACHE_DIR" -> autoCacheDir = NativeArgumentValueAdapter.toImpl(value)
      "X_AUTO_CACHE_FROM" -> autoCacheableFrom = NativeArgumentValueAdapter.toImpl(value)
      "X_BACKEND_THREADS" -> backendThreads = NativeArgumentValueAdapter.toImpl(value)
      "X_BINARY" -> binaryOptions = NativeArgumentValueAdapter.toImpl(value)
      "X_CACHE_DIRECTORY" -> cacheDirectories = NativeArgumentValueAdapter.toImpl(value)
      "X_CACHED_LIBRARY" -> cachedLibraries = NativeArgumentValueAdapter.toImpl(value)
      "X_CHECK_DEPENDENCIES" -> checkDependencies = NativeArgumentValueAdapter.toImpl(value)
      "X_CHECK_STATE_AT_EXTERNAL_CALLS" -> checkExternalCalls = NativeArgumentValueAdapter.toImpl(value)
      "X_COMPILE_FROM_BITCODE" -> compileFromBitcode = NativeArgumentValueAdapter.toImpl(value)
      "X_DEBUG_INFO_VERSION" -> debugInfoFormatVersion = NativeArgumentValueAdapter.toImpl(value)
      "X_DEBUG_PREFIX_MAP" -> debugPrefixMap = NativeArgumentValueAdapter.toImpl(value)
      "X_DUMP_BUILT_CACHES_TO" -> dumpBuiltCachesTo = NativeArgumentValueAdapter.toImpl(value)
      "X_DUMP_TESTS_TO" -> testDumpOutputPath = NativeArgumentValueAdapter.toImpl(value)
      "X_EMIT_LAZY_OBJC_HEADER" -> emitLazyObjCHeader = NativeArgumentValueAdapter.toImpl(value)
      "X_EXPORT_KDOC" -> exportKDoc = NativeArgumentValueAdapter.toImpl(value)
      "X_EXPORT_LIBRARY" -> exportedLibraries = NativeArgumentValueAdapter.toImpl(value)
      "X_EXTERNAL_DEPENDENCIES" -> externalDependencies = NativeArgumentValueAdapter.toImpl(value)
      "X_FILE_TO_CACHE" -> filesToCache = NativeArgumentValueAdapter.toImpl(value)
      "X_FRAMEWORK_IMPORT_HEADER" -> frameworkImportHeaders = NativeArgumentValueAdapter.toImpl(value)
      "X_G_GENERATE_DEBUG_TRAMPOLINE" -> generateDebugTrampolineString = NativeArgumentValueAdapter.toImpl(value)
      "X_GC" -> gc = NativeArgumentValueAdapter.toImpl(value)
      "X_HEADER_KLIB_PATH" -> headerKlibPath = NativeArgumentValueAdapter.toImpl(value)
      "X_IC_CACHE_DIR" -> incrementalCacheDir = NativeArgumentValueAdapter.toImpl(value)
      "X_INCLUDE" -> includes = NativeArgumentValueAdapter.toImpl(value)
      "X_IR_PROPERTY_LAZY_INITIALIZATION" -> propertyLazyInitialization = NativeArgumentValueAdapter.toImpl(value)
      "X_KONAN_DATA_DIR" -> konanDataDir = NativeArgumentValueAdapter.toImpl(value)
      "X_LLVM_LTO_PASSES" -> llvmLTOPasses = NativeArgumentValueAdapter.toImpl(value)
      "X_LLVM_MODULE_PASSES" -> llvmModulePasses = NativeArgumentValueAdapter.toImpl(value)
      "X_LLVM_VARIANT" -> llvmVariant = NativeArgumentValueAdapter.toImpl(value)
      "X_MAKE_PER_FILE_CACHE" -> makePerFileCache = NativeArgumentValueAdapter.toImpl(value)
      "X_MANIFEST_NATIVE_TARGETS" -> manifestNativeTargets = NativeArgumentValueAdapter.toImpl(value)
      "X_NO_OBJC_GENERICS" -> noObjcGenerics = NativeArgumentValueAdapter.toImpl(value)
      "X_OMIT_FRAMEWORK_BINARY" -> omitFrameworkBinary = NativeArgumentValueAdapter.toImpl(value)
      "X_OVERRIDE_CLANG_OPTIONS" -> clangOptions = NativeArgumentValueAdapter.toImpl(value)
      "X_OVERRIDE_KONAN_PROPERTIES" -> overrideKonanProperties = NativeArgumentValueAdapter.toImpl(value)
      "X_PRE_LINK_CACHES" -> preLinkCaches = NativeArgumentValueAdapter.toImpl(value)
      "X_PRINT_BITCODE" -> printBitCode = NativeArgumentValueAdapter.toImpl(value)
      "X_PRINT_FILES" -> printFiles = NativeArgumentValueAdapter.toImpl(value)
      "X_PRINT_IR" -> printIr = NativeArgumentValueAdapter.toImpl(value)
      "X_READ_DEPENDENCIES_FROM" -> serializedDependencies = NativeArgumentValueAdapter.toImpl(value)
      "X_REFINES_PATHS" -> refinesPaths = NativeArgumentValueAdapter.toImpl(value)
      "X_RUNTIME" -> runtimeFile = NativeArgumentValueAdapter.toImpl(value)
      "X_RUNTIME_LOGS" -> runtimeLogs = NativeArgumentValueAdapter.toImpl(value)
      "X_SAVE_LLVM_IR_AFTER" -> saveLlvmIrAfter = NativeArgumentValueAdapter.toImpl(value)
      "X_SAVE_LLVM_IR_DIRECTORY" -> saveLlvmIrDirectory = NativeArgumentValueAdapter.toImpl(value)
      "X_SHORT_MODULE_NAME" -> shortModuleName = NativeArgumentValueAdapter.toImpl(value)
      "X_STATIC_FRAMEWORK" -> staticFramework = NativeArgumentValueAdapter.toImpl(value)
      "X_TEMPORARY_FILES_DIR" -> temporaryFilesDir = NativeArgumentValueAdapter.toImpl(value)
      "X_VERIFY_BITCODE" -> verifyBitCode = NativeArgumentValueAdapter.toImpl(value)
      "X_VERIFY_COMPILER" -> verifyCompiler = NativeArgumentValueAdapter.toImpl(value)
      "X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO" -> writeDependenciesOfProducedKlibTo = NativeArgumentValueAdapter.toImpl(value)
      "X_WRITE_DEPENDENCIES_TO" -> saveDependenciesPath = NativeArgumentValueAdapter.toImpl(value)
      "ENABLE_ASSERTIONS" -> enableAssertions = NativeArgumentValueAdapter.toImpl(value)
      "ENTRY" -> mainPackage = NativeArgumentValueAdapter.toImpl(value)
      "FRIEND_MODULES" -> friendModules = NativeArgumentValueAdapter.toImpl(value)
      "G" -> debug = NativeArgumentValueAdapter.toImpl(value)
      "GENERATE_NO_EXIT_TEST_RUNNER" -> generateNoExitTestRunner = NativeArgumentValueAdapter.toImpl(value)
      "GENERATE_TEST_RUNNER" -> generateTestRunner = NativeArgumentValueAdapter.toImpl(value)
      "GENERATE_WORKER_TEST_RUNNER" -> generateWorkerTestRunner = NativeArgumentValueAdapter.toImpl(value)
      "INCLUDE_BINARY" -> includeBinaries = NativeArgumentValueAdapter.toImpl(value)
      "LIBRARY" -> libraries = NativeArgumentValueAdapter.toImpl(value)
      "LINKER_OPTION" -> singleLinkerArguments = NativeArgumentValueAdapter.toImpl(value)
      "LINKER_OPTIONS" -> linkerArguments = NativeArgumentValueAdapter.toImpl(value)
      "LIST_TARGETS" -> listTargets = NativeArgumentValueAdapter.toImpl(value)
      "MANIFEST" -> manifestFile = NativeArgumentValueAdapter.toImpl(value)
      "MEMORY_MODEL" -> memoryModel = NativeArgumentValueAdapter.toImpl(value)
      "MODULE_NAME" -> moduleName = NativeArgumentValueAdapter.toImpl(value)
      "NATIVE_LIBRARY" -> nativeLibraries = NativeArgumentValueAdapter.toImpl(value)
      "NO_DEFAULT_LIBS" -> nodefaultlibs = NativeArgumentValueAdapter.toImpl(value)
      "NOMAIN" -> nomain = NativeArgumentValueAdapter.toImpl(value)
      "NOPACK" -> nopack = NativeArgumentValueAdapter.toImpl(value)
      "NOSTDLIB" -> nostdlib = NativeArgumentValueAdapter.toImpl(value)
      "OPT" -> optimization = NativeArgumentValueAdapter.toImpl(value)
      "OUTPUT" -> outputName = NativeArgumentValueAdapter.toImpl(value)
      "PRODUCE" -> produce = NativeArgumentValueAdapter.toImpl(value)
      "TARGET" -> target = NativeArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = NativeArgumentValueAdapter.toImpl(value)
          }
    }
  }

  override fun deepCopy(): NativeArgumentsImpl = NativeArgumentsImpl(argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): NativeArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2NativeCompilerArguments {
    val arguments = K2NativeCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.libraryToAddToCache = libraryToAddToCache?.absolutePathStringOrThrow()
    arguments.lightDebugString = lightDebugString?.stringValue
    arguments.allocator = allocator?.stringValue
    arguments.autoCacheDir = autoCacheDir?.absolutePathStringOrThrow()
    arguments.autoCacheableFrom = autoCacheableFrom.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.backendThreads = backendThreads.toString()
    arguments.binaryOptions = binaryOptions ?: emptyArray()
    arguments.cacheDirectories = cacheDirectories.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.cachedLibraries = cachedLibraries ?: emptyArray()
    arguments.checkDependencies = checkDependencies
    arguments.checkExternalCalls = checkExternalCalls
    arguments.compileFromBitcode = compileFromBitcode?.absolutePathStringOrThrow()
    arguments.debugInfoFormatVersion = debugInfoFormatVersion.toString()
    arguments.debugPrefixMap = debugPrefixMap ?: emptyArray()
    arguments.dumpBuiltCachesTo = dumpBuiltCachesTo?.absolutePathStringOrThrow()
    arguments.testDumpOutputPath = testDumpOutputPath?.absolutePathStringOrThrow()
    arguments.emitLazyObjCHeader = emitLazyObjCHeader?.absolutePathStringOrThrow()
    arguments.exportKDoc = exportKDoc
    arguments.exportedLibraries = exportedLibraries.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.externalDependencies = externalDependencies?.absolutePathStringOrThrow()
    arguments.filesToCache = filesToCache ?: emptyArray()
    arguments.frameworkImportHeaders = frameworkImportHeaders ?: emptyArray()
    arguments.generateDebugTrampolineString = generateDebugTrampolineString?.stringValue
    arguments.gc = gc?.stringValue
    arguments.headerKlibPath = headerKlibPath?.absolutePathStringOrThrow()
    arguments.incrementalCacheDir = incrementalCacheDir?.absolutePathStringOrThrow()
    arguments.includes = includes.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.propertyLazyInitialization = propertyLazyInitialization?.stringValue
    arguments.konanDataDir = konanDataDir?.absolutePathStringOrThrow()
    arguments.llvmLTOPasses = llvmLTOPasses
    arguments.llvmModulePasses = llvmModulePasses
    arguments.llvmVariant = llvmVariant
    arguments.makePerFileCache = makePerFileCache
    arguments.manifestNativeTargets = manifestNativeTargets.map { it.stringValue }.toTypedArray()
    arguments.noObjcGenerics = noObjcGenerics
    arguments.omitFrameworkBinary = omitFrameworkBinary
    arguments.clangOptions = clangOptions ?: emptyArray()
    arguments.overrideKonanProperties = overrideKonanProperties ?: emptyArray()
    arguments.preLinkCaches = preLinkCaches?.stringValue
    arguments.printBitCode = printBitCode
    arguments.printFiles = printFiles
    arguments.printIr = printIr
    arguments.serializedDependencies = serializedDependencies?.absolutePathStringOrThrow()
    arguments.refinesPaths = refinesPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.runtimeFile = runtimeFile?.absolutePathStringOrThrow()
    arguments.runtimeLogs = runtimeLogs
    arguments.saveLlvmIrAfter = saveLlvmIrAfter ?: emptyArray()
    arguments.saveLlvmIrDirectory = saveLlvmIrDirectory?.absolutePathStringOrThrow()
    arguments.shortModuleName = shortModuleName
    arguments.staticFramework = staticFramework
    arguments.temporaryFilesDir = temporaryFilesDir?.absolutePathStringOrThrow()
    arguments.verifyBitCode = verifyBitCode
    arguments.verifyCompiler = verifyCompiler
    arguments.writeDependenciesOfProducedKlibTo = writeDependenciesOfProducedKlibTo?.absolutePathStringOrThrow()
    arguments.saveDependenciesPath = saveDependenciesPath?.absolutePathStringOrThrow()
    arguments.enableAssertions = enableAssertions
    arguments.mainPackage = mainPackage
    arguments.friendModules = friendModules?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.debug = debug
    arguments.generateNoExitTestRunner = generateNoExitTestRunner
    arguments.generateTestRunner = generateTestRunner
    arguments.generateWorkerTestRunner = generateWorkerTestRunner
    arguments.includeBinaries = includeBinaries.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.libraries = libraries.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.singleLinkerArguments = singleLinkerArguments ?: emptyArray()
    arguments.linkerArguments = linkerArguments ?: emptyArray()
    arguments.listTargets = listTargets
    arguments.manifestFile = manifestFile?.absolutePathStringOrThrow()
    arguments.memoryModel = memoryModel?.stringValue
    arguments.moduleName = moduleName
    arguments.nativeLibraries = nativeLibraries.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.nodefaultlibs = nodefaultlibs
    arguments.nomain = nomain
    arguments.nopack = nopack
    arguments.nostdlib = nostdlib
    arguments.optimization = optimization
    arguments.outputName = outputName?.absolutePathStringOrThrow()
    arguments.produce = produce?.stringValue
    arguments.target = target?.stringValue
    arguments.internalArguments = parseCommandLineArguments<K2NativeCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2NativeCompilerArguments) {
    super.applyCompilerArguments(arguments)
    libraryToAddToCache = arguments.libraryToAddToCache?.let { kotlin.io.path.Path(it) }
    try { lightDebugString = arguments.lightDebugString?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::lightDebugString, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xadd-light-debug value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { allocator = arguments.allocator?.let { NativeAllocationMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::allocator, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xallocator value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    autoCacheDir = arguments.autoCacheDir?.let { kotlin.io.path.Path(it) }
    autoCacheableFrom = arguments.autoCacheableFrom.mapOrEmpty { kotlin.io.path.Path(it) }
    backendThreads = arguments.backendThreads.let { it.toInt() }
    binaryOptions = arguments.binaryOptions
    cacheDirectories = arguments.cacheDirectories.mapOrEmpty { kotlin.io.path.Path(it) }
    cachedLibraries = arguments.cachedLibraries
    checkDependencies = arguments.checkDependencies
    checkExternalCalls = arguments.checkExternalCalls
    compileFromBitcode = arguments.compileFromBitcode?.let { kotlin.io.path.Path(it) }
    debugInfoFormatVersion = arguments.debugInfoFormatVersion.let { it.toInt() }
    debugPrefixMap = arguments.debugPrefixMap
    dumpBuiltCachesTo = arguments.dumpBuiltCachesTo?.let { kotlin.io.path.Path(it) }
    testDumpOutputPath = arguments.testDumpOutputPath?.let { kotlin.io.path.Path(it) }
    emitLazyObjCHeader = arguments.emitLazyObjCHeader?.let { kotlin.io.path.Path(it) }
    exportKDoc = arguments.exportKDoc
    exportedLibraries = arguments.exportedLibraries.mapOrEmpty { kotlin.io.path.Path(it) }
    externalDependencies = arguments.externalDependencies?.let { kotlin.io.path.Path(it) }
    filesToCache = arguments.filesToCache
    frameworkImportHeaders = arguments.frameworkImportHeaders
    try { generateDebugTrampolineString = arguments.generateDebugTrampolineString?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::generateDebugTrampolineString, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xg-generate-debug-trampoline value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { gc = arguments.gc?.let { NativeGcMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::gc, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xgc value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    headerKlibPath = arguments.headerKlibPath?.let { kotlin.io.path.Path(it) }
    incrementalCacheDir = arguments.incrementalCacheDir?.let { kotlin.io.path.Path(it) }
    includes = arguments.includes.mapOrEmpty { kotlin.io.path.Path(it) }
    try { propertyLazyInitialization = arguments.propertyLazyInitialization?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::propertyLazyInitialization, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-property-lazy-initialization value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    konanDataDir = arguments.konanDataDir?.let { kotlin.io.path.Path(it) }
    llvmLTOPasses = arguments.llvmLTOPasses
    llvmModulePasses = arguments.llvmModulePasses
    llvmVariant = arguments.llvmVariant
    makePerFileCache = arguments.makePerFileCache
    try { manifestNativeTargets = arguments.manifestNativeTargets.map { KonanTarget.values().firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -Xmanifest-native-targets value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    noObjcGenerics = arguments.noObjcGenerics
    omitFrameworkBinary = arguments.omitFrameworkBinary
    clangOptions = arguments.clangOptions
    overrideKonanProperties = arguments.overrideKonanProperties
    try { preLinkCaches = arguments.preLinkCaches?.let { EnableDisableMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::preLinkCaches, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xpre-link-caches value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    printBitCode = arguments.printBitCode
    printFiles = arguments.printFiles
    printIr = arguments.printIr
    serializedDependencies = arguments.serializedDependencies?.let { kotlin.io.path.Path(it) }
    refinesPaths = arguments.refinesPaths.mapOrEmpty { kotlin.io.path.Path(it) }
    runtimeFile = arguments.runtimeFile?.let { kotlin.io.path.Path(it) }
    runtimeLogs = arguments.runtimeLogs
    saveLlvmIrAfter = arguments.saveLlvmIrAfter
    saveLlvmIrDirectory = arguments.saveLlvmIrDirectory?.let { kotlin.io.path.Path(it) }
    shortModuleName = arguments.shortModuleName
    staticFramework = arguments.staticFramework
    temporaryFilesDir = arguments.temporaryFilesDir?.let { kotlin.io.path.Path(it) }
    verifyBitCode = arguments.verifyBitCode
    verifyCompiler = arguments.verifyCompiler
    writeDependenciesOfProducedKlibTo = arguments.writeDependenciesOfProducedKlibTo?.let { kotlin.io.path.Path(it) }
    saveDependenciesPath = arguments.saveDependenciesPath?.let { kotlin.io.path.Path(it) }
    enableAssertions = arguments.enableAssertions
    mainPackage = arguments.mainPackage
    friendModules = arguments.friendModules?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }
    debug = arguments.debug
    generateNoExitTestRunner = arguments.generateNoExitTestRunner
    generateTestRunner = arguments.generateTestRunner
    generateWorkerTestRunner = arguments.generateWorkerTestRunner
    includeBinaries = arguments.includeBinaries.mapOrEmpty { kotlin.io.path.Path(it) }
    libraries = arguments.libraries.mapOrEmpty { kotlin.io.path.Path(it) }
    singleLinkerArguments = arguments.singleLinkerArguments
    linkerArguments = arguments.linkerArguments
    listTargets = arguments.listTargets
    manifestFile = arguments.manifestFile?.let { kotlin.io.path.Path(it) }
    try { memoryModel = arguments.memoryModel?.let { NativeMemoryModel.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::memoryModel, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -memory-model value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    moduleName = arguments.moduleName
    nativeLibraries = arguments.nativeLibraries.mapOrEmpty { kotlin.io.path.Path(it) }
    nodefaultlibs = arguments.nodefaultlibs
    nomain = arguments.nomain
    nopack = arguments.nopack
    nostdlib = arguments.nostdlib
    optimization = arguments.optimization
    outputName = arguments.outputName?.let { kotlin.io.path.Path(it) }
    try { produce = arguments.produce?.let { CompilerOutputKind.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::produce, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -produce value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { target = arguments.target?.let { KonanTarget.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2NativeCompilerArguments = K2NativeCompilerArguments()): K2NativeCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.libraryToAddToCache = libraryToAddToCache?.absolutePathStringOrThrow()
    arguments.lightDebugString = lightDebugString?.stringValue
    arguments.allocator = allocator?.stringValue
    arguments.autoCacheDir = autoCacheDir?.absolutePathStringOrThrow()
    arguments.autoCacheableFrom = autoCacheableFrom.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.binaryOptions = binaryOptions ?: emptyArray()
    arguments.cacheDirectories = cacheDirectories.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.cachedLibraries = cachedLibraries ?: emptyArray()
    arguments.checkDependencies = checkDependencies
    arguments.checkExternalCalls = checkExternalCalls
    arguments.compileFromBitcode = compileFromBitcode?.absolutePathStringOrThrow()
    arguments.debugInfoFormatVersion = debugInfoFormatVersion.toString()
    arguments.debugPrefixMap = debugPrefixMap ?: emptyArray()
    arguments.dumpBuiltCachesTo = dumpBuiltCachesTo?.absolutePathStringOrThrow()
    arguments.testDumpOutputPath = testDumpOutputPath?.absolutePathStringOrThrow()
    arguments.emitLazyObjCHeader = emitLazyObjCHeader?.absolutePathStringOrThrow()
    arguments.exportKDoc = exportKDoc
    arguments.exportedLibraries = exportedLibraries.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.externalDependencies = externalDependencies?.absolutePathStringOrThrow()
    arguments.filesToCache = filesToCache ?: emptyArray()
    arguments.frameworkImportHeaders = frameworkImportHeaders ?: emptyArray()
    arguments.generateDebugTrampolineString = generateDebugTrampolineString?.stringValue
    arguments.gc = gc?.stringValue
    arguments.headerKlibPath = headerKlibPath?.absolutePathStringOrThrow()
    arguments.incrementalCacheDir = incrementalCacheDir?.absolutePathStringOrThrow()
    arguments.includes = includes.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.propertyLazyInitialization = propertyLazyInitialization?.stringValue
    arguments.konanDataDir = konanDataDir?.absolutePathStringOrThrow()
    arguments.llvmLTOPasses = llvmLTOPasses
    arguments.llvmModulePasses = llvmModulePasses
    arguments.llvmVariant = llvmVariant
    arguments.makePerFileCache = makePerFileCache
    arguments.manifestNativeTargets = manifestNativeTargets.map { it.stringValue }.toTypedArray()
    arguments.noObjcGenerics = noObjcGenerics
    arguments.omitFrameworkBinary = omitFrameworkBinary
    arguments.clangOptions = clangOptions ?: emptyArray()
    arguments.overrideKonanProperties = overrideKonanProperties ?: emptyArray()
    arguments.preLinkCaches = preLinkCaches?.stringValue
    arguments.serializedDependencies = serializedDependencies?.absolutePathStringOrThrow()
    arguments.refinesPaths = refinesPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.runtimeFile = runtimeFile?.absolutePathStringOrThrow()
    arguments.runtimeLogs = runtimeLogs
    arguments.saveLlvmIrAfter = saveLlvmIrAfter ?: emptyArray()
    arguments.saveLlvmIrDirectory = saveLlvmIrDirectory?.absolutePathStringOrThrow()
    arguments.shortModuleName = shortModuleName
    arguments.staticFramework = staticFramework
    arguments.verifyBitCode = verifyBitCode
    arguments.verifyCompiler = verifyCompiler
    arguments.writeDependenciesOfProducedKlibTo = writeDependenciesOfProducedKlibTo?.absolutePathStringOrThrow()
    arguments.saveDependenciesPath = saveDependenciesPath?.absolutePathStringOrThrow()
    arguments.enableAssertions = enableAssertions
    arguments.mainPackage = mainPackage
    arguments.friendModules = friendModules?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.debug = debug
    arguments.generateNoExitTestRunner = generateNoExitTestRunner
    arguments.generateTestRunner = generateTestRunner
    arguments.generateWorkerTestRunner = generateWorkerTestRunner
    arguments.includeBinaries = includeBinaries.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.libraries = libraries.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.singleLinkerArguments = singleLinkerArguments ?: emptyArray()
    arguments.linkerArguments = linkerArguments ?: emptyArray()
    arguments.manifestFile = manifestFile?.absolutePathStringOrThrow()
    arguments.memoryModel = memoryModel?.stringValue
    arguments.moduleName = moduleName
    arguments.nativeLibraries = nativeLibraries.map { it.absolutePathStringOrThrow() }.toTypedArray()
    arguments.nodefaultlibs = nodefaultlibs
    arguments.nomain = nomain
    arguments.nopack = nopack
    arguments.nostdlib = nostdlib
    arguments.optimization = optimization
    arguments.outputName = outputName?.absolutePathStringOrThrow()
    arguments.produce = produce?.stringValue
    arguments.target = target?.stringValue
    return arguments
  }

  @Deprecated(
    message = "This method is deprecated. Use applyCommandLineArguments instead.",
    level = DeprecationLevel.WARNING,
  )
  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2NativeCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, K2NativeCompilerArguments())
    _argumentValidationErrors.appendFatalErrors(compilerArgs)
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  @DelicateBuildToolsApi
  override fun applyCommandLineArguments(arguments: List<String>) {
    val compilerArgs = toCompilerArguments()
    parseCommandLineArguments(arguments, compilerArgs, false)
    handleCustomPluginArguments(this, compilerArgs)
    collectRestrictedArgViolations(compilerArgs, K2NativeCompilerArguments())
    _argumentValidationErrors.appendFatalErrors(compilerArgs)
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)
    return arguments
  }

  @Suppress("DEPRECATION")
  internal override fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    super.collectRestrictedArgViolations(compilerArgs, defaultArgs)
    val args = compilerArgs as K2NativeCompilerArguments
    val castedDefaults = defaultArgs as K2NativeCompilerArguments
    if (args.outputName != castedDefaults.outputName) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-output'/'-o' is not supported in the Build Tools API. The destination is configured via the destination parameter of nativeKlibCompilationOperationBuilder."))
    if (args.produce != castedDefaults.produce) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-produce'/'-p' is not supported in the Build Tools API. Klib compilation always produces a library.", CompilerPhase.KLIB_COMPILATION))
  }

  /**
   * Returns a sorted list of compiler argument strings representing only the arguments
   * that affect the compilation outcome (i.e. those with [affectsCompilationOutcome][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument.affectsCompilationOutcome] set to true).
   * Arguments with default values are omitted from the output, because [toCompilerArgumentsAffectingOutcome]
   * only sets arguments that have been explicitly assigned, and [compilerToArgumentStrings][org.jetbrains.kotlin.compilerRunner.toArgumentStrings]
   * skips properties whose value matches the default.
   */
  public fun toCompilationInputs(): List<String> = toCompilerArgumentsAffectingOutcome().compilerToArgumentStrings(allowArgFileInValues = false).sorted()

  public companion object {
    private val knownOptionIds: List<String> =
        listOf("X_ADD_CACHE","X_ADD_LIGHT_DEBUG","X_ALLOCATOR","X_AUTO_CACHE_DIR","X_AUTO_CACHE_FROM","X_BACKEND_THREADS","X_BINARY","X_CACHE_DIRECTORY","X_CACHED_LIBRARY","X_CHECK_DEPENDENCIES","X_CHECK_STATE_AT_EXTERNAL_CALLS","X_COMPILE_FROM_BITCODE","X_DEBUG_INFO_VERSION","X_DEBUG_PREFIX_MAP","X_DUMP_BUILT_CACHES_TO","X_DUMP_TESTS_TO","X_EMIT_LAZY_OBJC_HEADER","X_EXPORT_KDOC","X_EXPORT_LIBRARY","X_EXTERNAL_DEPENDENCIES","X_FILE_TO_CACHE","X_FRAMEWORK_IMPORT_HEADER","X_G_GENERATE_DEBUG_TRAMPOLINE","X_GC","X_HEADER_KLIB_PATH","X_IC_CACHE_DIR","X_INCLUDE","X_IR_PROPERTY_LAZY_INITIALIZATION","X_KONAN_DATA_DIR","X_LLVM_LTO_PASSES","X_LLVM_MODULE_PASSES","X_LLVM_VARIANT","X_MAKE_PER_FILE_CACHE","X_MANIFEST_NATIVE_TARGETS","X_NO_OBJC_GENERICS","X_OMIT_FRAMEWORK_BINARY","X_OVERRIDE_CLANG_OPTIONS","X_OVERRIDE_KONAN_PROPERTIES","X_PRE_LINK_CACHES","X_PRINT_BITCODE","X_PRINT_FILES","X_PRINT_IR","X_READ_DEPENDENCIES_FROM","X_REFINES_PATHS","X_RUNTIME","X_RUNTIME_LOGS","X_SAVE_LLVM_IR_AFTER","X_SAVE_LLVM_IR_DIRECTORY","X_SHORT_MODULE_NAME","X_STATIC_FRAMEWORK","X_TEMPORARY_FILES_DIR","X_VERIFY_BITCODE","X_VERIFY_COMPILER","X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO","X_WRITE_DEPENDENCIES_TO","ENABLE_ASSERTIONS","ENTRY","FRIEND_MODULES","G","GENERATE_NO_EXIT_TEST_RUNNER","GENERATE_TEST_RUNNER","GENERATE_WORKER_TEST_RUNNER","INCLUDE_BINARY","LIBRARY","LINKER_OPTION","LINKER_OPTIONS","LIST_TARGETS","MANIFEST","MEMORY_MODEL","MODULE_NAME","NATIVE_LIBRARY","NO_DEFAULT_LIBS","NOMAIN","NOPACK","NOSTDLIB","OPT","OUTPUT","PRODUCE","TARGET",)
  }
}
