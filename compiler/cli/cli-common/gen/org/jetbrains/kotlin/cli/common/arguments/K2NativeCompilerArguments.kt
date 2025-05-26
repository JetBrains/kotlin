/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.STRING_NULL_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes.INPUT
import com.intellij.util.xmlb.annotations.Transient as AnnotationsTransient
import kotlin.jvm.Transient as JvmTransient

/**
 * This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
 * Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/NativeCompilerArguments.kt
 * DO NOT MODIFY IT MANUALLY.
 */
public class K2NativeCompilerArguments : CommonKlibBasedCompilerArguments() {
  @Argument(
    value = "-enable-assertions",
    shortName = "-ea",
    deprecatedName = "-enable_assertions",
    description = "Enable runtime assertions in generated code.",
  )
  public var enableAssertions: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-g",
    description = "Enable the emission of debug information.",
  )
  public var debug: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-generate-test-runner",
    shortName = "-tr",
    deprecatedName = "-generate_test_runner",
    description = "Produce a runner for unit tests.",
  )
  public var generateTestRunner: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-generate-worker-test-runner",
    shortName = "-trw",
    description = "Produce a worker runner for unit tests.",
  )
  public var generateWorkerTestRunner: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-generate-no-exit-test-runner",
    shortName = "-trn",
    description = "Produce a runner for unit tests that doesn't force an exit.",
  )
  public var generateNoExitTestRunner: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-include-binary",
    shortName = "-ib",
    deprecatedName = "-includeBinary",
    valueDescription = "<path>",
    description = "Pack the given external binary into the klib.",
  )
  public var includeBinaries: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-library",
    shortName = "-l",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Link with the given library.",
  )
  public var libraries: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-library-version",
    shortName = "-lv",
    valueDescription = "<version>",
    description = "The library version.\nNote: This option is deprecated and will be removed in one of the future releases.",
  )
  public var libraryVersion: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-list-targets",
    deprecatedName = "-list_targets",
    description = "List available hardware targets.",
  )
  public var listTargets: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-manifest",
    valueDescription = "<path>",
    description = "Provide a manifest addend file.",
  )
  public var manifestFile: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-memory-model",
    valueDescription = "<model>",
    description = "Choose the memory model to be used – 'strict' and 'experimental' are currently supported.",
  )
  public var memoryModel: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = STRING_NULL_DEFAULT,
    gradleInputType = INPUT,
  )
  @Argument(
    value = "-module-name",
    deprecatedName = "-module_name",
    valueDescription = "<name>",
    description = "Specify a name for the compilation module.",
  )
  public var moduleName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-native-library",
    shortName = "-nl",
    deprecatedName = "-nativelibrary",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Include the given native bitcode library.",
  )
  public var nativeLibraries: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-no-default-libs",
    deprecatedName = "-nodefaultlibs",
    description = "Don't link the libraries from dist/klib automatically.",
  )
  public var nodefaultlibs: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-no-endorsed-libs",
    description = "Don't link endorsed libraries from the dist automatically. This option has been deprecated, as the dist no longer has any endorsed libraries.",
  )
  public var noendorsedlibs: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-nomain",
    description = "Assume the 'main' entry point will be provided by external libraries.",
  )
  public var nomain: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-nopack",
    description = "Don't pack the library into a klib file.",
  )
  public var nopack: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-linker-options",
    deprecatedName = "-linkerOpts",
    valueDescription = "<arg>",
    delimiter = Argument.Delimiters.space,
    description = "Pass arguments to the linker.",
  )
  public var linkerArguments: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-linker-option",
    valueDescription = "<arg>",
    delimiter = Argument.Delimiters.none,
    description = "Pass the given argument to the linker.",
  )
  public var singleLinkerArguments: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-nostdlib",
    description = "Don't link with the stdlib.",
  )
  public var nostdlib: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-opt",
    description = "Enable optimizations during compilation.",
  )
  public var optimization: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-output",
    shortName = "-o",
    valueDescription = "<name>",
    description = "Output name.",
  )
  public var outputName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-entry",
    shortName = "-e",
    valueDescription = "<name>",
    description = "Qualified entry point name.",
  )
  public var mainPackage: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-produce",
    shortName = "-p",
    valueDescription = "{program|static|dynamic|framework|library|bitcode}",
    description = "Specify the output file kind.",
  )
  public var produce: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-target",
    valueDescription = "<target>",
    description = "Set the hardware target.",
  )
  public var target: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xbundle-id",
    valueDescription = "<id>",
    description = "Bundle ID to be set in the Info.plist file of the produced framework. This option is deprecated. Please use '-Xbinary=bundleId=<id>'.",
  )
  public var bundleId: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xcache-directory",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Path to the directory containing caches.",
  )
  public var cacheDirectories: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcached-library",
    valueDescription = "<library path>,<cache path>",
    delimiter = Argument.Delimiters.none,
    description = "Paths to a library and its cache, separated by a comma.",
  )
  public var cachedLibraries: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xauto-cache-from",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Path to the root directory from which dependencies are to be cached automatically.\nBy default caches will be placed into the kotlin-native system cache directory.",
  )
  public var autoCacheableFrom: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xauto-cache-dir",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Path to the directory where caches for auto-cacheable dependencies should be put.",
  )
  public var autoCacheDir: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xic-cache-dir",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Path to the directory where incremental build caches should be put.",
  )
  public var incrementalCacheDir: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xcheck-dependencies",
    deprecatedName = "--check_dependencies",
    description = "Check dependencies and download the missing ones.",
  )
  public var checkDependencies: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xemit-lazy-objc-header",
    description = "",
  )
  public var emitLazyObjCHeader: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xexport-library",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "A library to be included in the produced framework API.\nThis library must be one of the ones passed with '-library'.",
  )
  public var exportedLibraries: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xexternal-dependencies",
    valueDescription = "<path>",
    description = "Path to the file containing external dependencies.\nExternal dependencies are required for verbose output in the event of IR linker errors,\nbut they do not affect compilation at all.",
  )
  public var externalDependencies: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xfake-override-validator",
    description = "Enable the IR fake override validator.",
  )
  public var fakeOverrideValidator: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xframework-import-header",
    valueDescription = "<header>",
    description = "Add an additional header import to the framework header.",
  )
  public var frameworkImportHeaders: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xadd-light-debug",
    valueDescription = "{disable|enable}",
    description = "Add light debug information for optimized builds. This option is skipped in debug builds.\nIt's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.\nCurrently this option is disabled by default on other platforms.",
  )
  public var lightDebugString: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xg0",
    description = "Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead.",
  )
  public var lightDebugDeprecated: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xg-generate-debug-trampoline",
    valueDescription = "{disable|enable}",
    description = "Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.).",
  )
  public var generateDebugTrampolineString: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xadd-cache",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Path to a library to be added to the cache.",
  )
  public var libraryToAddToCache: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xfile-to-cache",
    valueDescription = "<path>",
    delimiter = Argument.Delimiters.none,
    description = "Path to the file to cache.",
  )
  public var filesToCache: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xmake-per-file-cache",
    description = "Force the compiler to produce per-file caches.",
  )
  public var makePerFileCache: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xbackend-threads",
    valueDescription = "<N>",
    description = "Run codegen by file in N parallel threads.\n0 means use one thread per processor core.\nThe default value is 1.",
  )
  public var backendThreads: String = "1"
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xexport-kdoc",
    description = "Export KDoc entries in the framework header.",
  )
  public var exportKDoc: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xprint-bitcode",
    deprecatedName = "--print_bitcode",
    description = "Print LLVM bitcode.",
  )
  public var printBitCode: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcheck-state-at-external-calls",
    description = "Ensure that all calls of possibly long external functions are done in the native thread state.",
  )
  public var checkExternalCalls: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xprint-ir",
    deprecatedName = "--print_ir",
    description = "Print IR.",
  )
  public var printIr: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xprint-files",
    description = "Print files.",
  )
  public var printFiles: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xpurge-user-libs",
    deprecatedName = "--purge_user_libs",
    description = "Don't link unused libraries even if explicitly specified.",
  )
  public var purgeUserLibs: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xwrite-dependencies-of-produced-klib-to",
    valueDescription = "<path>",
    description = "Write file containing the paths of dependencies used during klib compilation to the provided path",
  )
  public var writeDependenciesOfProducedKlibTo: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xruntime",
    deprecatedName = "--runtime",
    valueDescription = "<path>",
    description = "Override the standard 'runtime.bc' location.",
  )
  public var runtimeFile: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xinclude",
    valueDescription = "<path>",
    description = "A path to an intermediate library that should be processed in the same manner as source files.",
  )
  public var includes: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xshort-module-name",
    valueDescription = "<name>",
    description = "A short name used to denote this library in the IDE and in a generated Objective-C header.",
  )
  public var shortModuleName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xstatic-framework",
    description = "Create a framework with a static library instead of a dynamic one.",
  )
  public var staticFramework: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xtemporary-files-dir",
    deprecatedName = "--temporary_files_dir",
    valueDescription = "<path>",
    description = "Save temporary files to the given directory.",
  )
  public var temporaryFilesDir: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xsave-llvm-ir-after",
    description = "Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.",
  )
  public var saveLlvmIrAfter: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xverify-bitcode",
    deprecatedName = "--verify_bitcode",
    description = "Verify LLVM bitcode after each method.",
  )
  public var verifyBitCode: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xverify-compiler",
    description = "Verify the compiler.",
  )
  public var verifyCompiler: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-friend-modules",
    valueDescription = "<path>",
    description = "Paths to friend modules.",
  )
  public var friendModules: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xrefines-paths",
    valueDescription = "<path>",
    description = "Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize).",
  )
  public var refinesPaths: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdebug-info-version",
    description = "Generate debug info of the given version (1, 2).",
  )
  public var debugInfoFormatVersion: String = "1"
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-objc-generics",
    description = "Disable generics support for framework header.",
  )
  public var noObjcGenerics: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xoverride-clang-options",
    valueDescription = "<arg1,arg2,...>",
    description = "Explicit list of Clang options.",
  )
  public var clangOptions: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xallocator",
    valueDescription = "std | mimalloc | custom",
    description = "Allocator used at runtime.",
  )
  public var allocator: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xheader-klib-path",
    description = "Save a klib that only contains the public ABI to the given path.",
  )
  public var headerKlibPath: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xdebug-prefix-map",
    valueDescription = "<old1=new1,old2=new2,...>",
    description = "Remap file source directory paths in debug info.",
  )
  public var debugPrefixMap: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xpre-link-caches",
    valueDescription = "{disable|enable}",
    description = "Perform caches pre-linking.",
  )
  public var preLinkCaches: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xoverride-konan-properties",
    valueDescription = "key1=value1;key2=value2;...",
    delimiter = Argument.Delimiters.semicolon,
    description = "Override values from 'konan.properties' with the given ones.",
  )
  public var overrideKonanProperties: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdestroy-runtime-mode",
    valueDescription = "<mode>",
    description = "When to destroy the runtime – 'legacy' and 'on-shutdown' are currently supported. Note that 'legacy' mode is deprecated and will be removed.",
  )
  public var destroyRuntimeMode: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xgc",
    valueDescription = "<gc>",
    description = "GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.",
  )
  public var gc: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-property-lazy-initialization",
    valueDescription = "{disable|enable}",
    description = "Initialize top level properties lazily per file.",
  )
  public var propertyLazyInitialization: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xworker-exception-handling",
    valueDescription = "<mode>",
    description = "Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.",
  )
  public var workerExceptionHandling: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xllvm-variant",
    valueDescription = "{dev|user|absolute path to llvm}",
    description = "Choose the LLVM distribution that will be used during compilation.",
  )
  public var llvmVariant: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xbinary",
    valueDescription = "<option=value>",
    description = "Specify a binary option.",
  )
  public var binaryOptions: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xruntime-logs",
    valueDescription = "<tag1=level1,tag2=level2,...>",
    description = "Enable logging of Native runtime internals.",
  )
  public var runtimeLogs: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xdump-tests-to",
    valueDescription = "<path>",
    description = "Path to a file for dumping the list of all available tests.",
  )
  public var testDumpOutputPath: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xlazy-ir-for-caches",
    valueDescription = "{disable|enable}",
    description = "Use lazy IR for cached libraries.",
  )
  public var lazyIrForCaches: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xomit-framework-binary",
    description = "Omit binary when compiling the framework.",
  )
  public var omitFrameworkBinary: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcompile-from-bitcode",
    valueDescription = "<path>",
    description = "Continue compilation from the given bitcode file.",
  )
  public var compileFromBitcode: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xread-dependencies-from",
    valueDescription = "<path>",
    description = "Serialized dependencies to use for linking.",
  )
  public var serializedDependencies: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xwrite-dependencies-to",
    description = "Path for writing backend dependencies.",
  )
  public var saveDependenciesPath: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xsave-llvm-ir-directory",
    description = "Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.",
  )
  public var saveLlvmIrDirectory: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xkonan-data-dir",
    description = "Custom path to the location of konan distributions.",
  )
  public var konanDataDir: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xllvm-module-passes",
    description = "Custom set of LLVM passes to run as the ModuleOptimizationPipeline.",
  )
  public var llvmModulePasses: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xllvm-lto-passes",
    description = "Custom set of LLVM passes to run as the LTOOptimizationPipeline.",
  )
  public var llvmLTOPasses: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xmanifest-native-targets",
    description = "Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.",
  )
  public var manifestNativeTargets: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdump-synthetic-accessors-to",
    description = "Path to a directory to dump synthetic accessors and their use sites.",
  )
  public var dumpSyntheticAccessorsTo: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @get:AnnotationsTransient
  @field:JvmTransient
  override val configurator: CommonCompilerArgumentsConfigurator =
      K2NativeCompilerArgumentsConfigurator()

  override fun copyOf(): Freezable = copyK2NativeCompilerArguments(this, K2NativeCompilerArguments())
}
