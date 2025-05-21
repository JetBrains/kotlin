/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.defaultOne
import org.jetbrains.kotlin.arguments.dsl.stubLifecycle
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes
import org.jetbrains.kotlin.cli.common.arguments.GradleOption

val actualNativeArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.nativeArguments) {
    compilerArgument {
        name = "enable-assertions"
        deprecatedName = "enable_assertions"
        shortName = "ea"
        description = "Enable runtime assertions in generated code.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "g"
        compilerName = "debug"
        description = "Enable the emission of debug information.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "generate-test-runner"
        deprecatedName = "generate_test_runner"
        shortName = "tr"
        description = "Produce a runner for unit tests.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "generate-worker-test-runner"
        shortName = "trw"
        description = "Produce a worker runner for unit tests.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "generate-no-exit-test-runner"
        shortName = "trn"
        description = "Produce a runner for unit tests that doesn't force an exit.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "include-binary"
        compilerName = "includeBinaries"
        deprecatedName = "includeBinary"
        shortName = "ib"
        description = "Pack the given external binary into the klib.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "library"
        compilerName = "libraries"
        shortName = "l"
        description = "Link with the given library.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "library-version"
        shortName = "lv"
        description = "The library version.\nNote: This option is deprecated and will be removed in one of the future releases.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<version>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "list-targets"
        deprecatedName = "list_targets"
        description = "List available hardware targets.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "manifest"
        compilerName = "manifestFile"
        description = "Provide a manifest addend file.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "memory-model"
        description = "Choose the memory model to be used – 'strict' and 'experimental' are currently supported.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<model>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "module-name"
        deprecatedName = "module_name"
        description = "Specify a name for the compilation module.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.STRING_NULL_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT
            )
        )

        stubLifecycle()
    }

    compilerArgument {
        name = "native-library"
        compilerName = "nativeLibraries"
        deprecatedName = "nativelibrary"
        shortName = "nl"
        delimiter = KotlinCompilerArgument.Delimiter.None
        description = "Include the given native bitcode library.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "no-default-libs"
        compilerName = "nodefaultlibs"
        deprecatedName = "nodefaultlibs"
        description = "Don't link the libraries from dist/klib automatically.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "no-endorsed-libs"
        compilerName = "noendorsedlibs"
        description = "Don't link endorsed libraries from the dist automatically. This option has been deprecated, as the dist no longer has any endorsed libraries.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "nomain"
        description = "Assume the 'main' entry point will be provided by external libraries.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "nopack"
        description = "Don't pack the library into a klib file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "linker-options"
        compilerName = "linkerArguments"
        deprecatedName = "linkerOpts"
        description = "Pass arguments to the linker.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<arg>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.Space

        stubLifecycle()
    }

    compilerArgument {
        name = "linker-option"
        compilerName = "singleLinkerArguments"
        description = "Pass the given argument to the linker.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<arg>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "nostdlib"
        description = "Don't link with the stdlib.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "opt"
        compilerName = "optimization"
        description = "Enable optimizations during compilation.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "output"
        compilerName = "outputName"
        shortName = "o"
        description = "Output name.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "entry"
        compilerName = "mainPackage"
        shortName = "e"
        description = "Qualified entry point name.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "produce"
        shortName = "p"
        description = "Specify the output file kind.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{program|static|dynamic|framework|library|bitcode}".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "target"
        description = "Set the hardware target.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<target>".asReleaseDependent()

        stubLifecycle()
    }

    // Advanced options with -X prefix

    compilerArgument {
        name = "Xbundle-id"
        description = "Bundle ID to be set in the Info.plist file of the produced framework. This option is deprecated. Please use '-Xbinary=bundleId=<id>'.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<id>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xcache-directory"
        compilerName = "cacheDirectories"
        description = "Path to the directory containing caches.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xcached-library"
        compilerName = "cachedLibraries"
        description = "Paths to a library and its cache, separated by a comma.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<library path>,<cache path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xauto-cache-from"
        compilerName = "autoCacheableFrom"
        description = """Path to the root directory from which dependencies are to be cached automatically.
By default caches will be placed into the kotlin-native system cache directory.""".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xauto-cache-dir"
        description = "Path to the directory where caches for auto-cacheable dependencies should be put.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xic-cache-dir"
        compilerName = "incrementalCacheDir"
        description = "Path to the directory where incremental build caches should be put.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xcheck-dependencies"
        deprecatedName = "-check_dependencies"
        description = "Check dependencies and download the missing ones.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xemit-lazy-objc-header"
        compilerName = "emitLazyObjCHeader"
        description = "".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xexport-library"
        compilerName = "exportedLibraries"
        description = """A library to be included in the produced framework API.
This library must be one of the ones passed with '-library'.""".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xexternal-dependencies"
        description = """Path to the file containing external dependencies.
External dependencies are required for verbose output in the event of IR linker errors,
but they do not affect compilation at all.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xfake-override-validator"
        description = "Enable the IR fake override validator.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xframework-import-header"
        compilerName = "frameworkImportHeaders"
        description = "Add an additional header import to the framework header.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<header>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xadd-light-debug"
        compilerName = "lightDebugString"
        description = """Add light debug information for optimized builds. This option is skipped in debug builds.
It's enabled by default on Darwin platforms where collected debug information is stored in a .dSYM file.
Currently this option is disabled by default on other platforms.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{disable|enable}".asReleaseDependent()

        stubLifecycle()
    }

    // TODO: remove after 1.4 release.
    compilerArgument {
        name = "Xg0"
        compilerName = "lightDebugDeprecated"
        description = "Add light debug information. This option has been deprecated. Please use '-Xadd-light-debug=enable' instead.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xg-generate-debug-trampoline"
        compilerName = "generateDebugTrampolineString"
        description = "Generate trampolines to make debugger breakpoint resolution more accurate (inlines, 'when', etc.).".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{disable|enable}".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xadd-cache"
        compilerName = "libraryToAddToCache"
        description = "Path to a library to be added to the cache.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xfile-to-cache"
        compilerName = "filesToCache"
        description = "Path to the file to cache.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        stubLifecycle()
    }

    compilerArgument {
        name = "Xmake-per-file-cache"
        description = "Force the compiler to produce per-file caches.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xbackend-threads"
        description = """Run codegen by file in N parallel threads.
0 means use one thread per processor core.
The default value is 1.""".asReleaseDependent()
        valueType = IntType.defaultOne
        valueDescription = "<N>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xexport-kdoc"
        compilerName = "exportKDoc"
        description = "Export KDoc entries in the framework header.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xprint-bitcode"
        compilerName = "printBitCode"
        deprecatedName = "-print_bitcode"
        description = "Print LLVM bitcode.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xcheck-state-at-external-calls"
        compilerName = "checkExternalCalls"
        description = "Ensure that all calls of possibly long external functions are done in the native thread state.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xprint-ir"
        deprecatedName = "-print_ir"
        description = "Print IR.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xprint-files"
        description = "Print files.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xpurge-user-libs"
        deprecatedName = "-purge_user_libs"
        description = "Don't link unused libraries even if explicitly specified.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwrite-dependencies-of-produced-klib-to"
        description = "Write file containing the paths of dependencies used during klib compilation to the provided path".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xruntime"
        compilerName = "runtimeFile"
        deprecatedName = "-runtime"
        description = "Override the standard 'runtime.bc' location.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xinclude"
        compilerName = "includes"
        description = "A path to an intermediate library that should be processed in the same manner as source files.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xshort-module-name"
        description = "A short name used to denote this library in the IDE and in a generated Objective-C header.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xstatic-framework"
        description = "Create a framework with a static library instead of a dynamic one.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xtemporary-files-dir"
        deprecatedName = "-temporary_files_dir"
        description = "Save temporary files to the given directory.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xsave-llvm-ir-after"
        description = "Save the result of the Kotlin IR to LLVM IR translation to '-Xsave-llvm-ir-directory'.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xverify-bitcode"
        compilerName = "verifyBitCode"
        deprecatedName = "-verify_bitcode"
        description = "Verify LLVM bitcode after each method.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xverify-compiler"
        description = "Verify the compiler.".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "friend-modules"
        description = "Paths to friend modules.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xrefines-paths"
        description = "Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize).".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xdebug-info-version"
        compilerName = "debugInfoFormatVersion"
        description = "Generate debug info of the given version (1, 2).".asReleaseDependent()
        valueType = IntType.defaultOne

        stubLifecycle()
    }

    compilerArgument {
        name = "Xno-objc-generics"
        description = "Disable generics support for framework header.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xoverride-clang-options"
        compilerName = "clangOptions"
        description = "Explicit list of Clang options.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<arg1,arg2,...>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xallocator"
        description = "Allocator used at runtime.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "std | mimalloc | custom".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xheader-klib-path"
        description = "Save a klib that only contains the public ABI to the given path.".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xdebug-prefix-map"
        description = "Remap file source directory paths in debug info.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<old1=new1,old2=new2,...>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xpre-link-caches"
        description = "Perform caches pre-linking.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{disable|enable}".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xoverride-konan-properties"
        description = "Override values from 'konan.properties' with the given ones.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "key1=value1;key2=value2;...".asReleaseDependent()
        // We use `;` as delimiter because properties may contain comma-separated values.
        // For example, target cpu features.
        delimiter = KotlinCompilerArgument.Delimiter.Semicolon

        stubLifecycle()
    }

    compilerArgument {
        name = "Xdestroy-runtime-mode"
        description = "When to destroy the runtime – 'legacy' and 'on-shutdown' are currently supported. Note that 'legacy' mode is deprecated and will be removed.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<mode>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xgc"
        description = "GC to use – 'noop', 'stms', and 'cms' are currently supported. This works only with '-memory-model experimental'.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<gc>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xir-property-lazy-initialization"
        compilerName = "propertyLazyInitialization"
        description = "Initialize top level properties lazily per file.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{disable|enable}".asReleaseDependent()

        stubLifecycle()
    }

    // TODO: Remove when legacy MM is gone.
    compilerArgument {
        name = "Xworker-exception-handling"
        description = "Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<mode>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xllvm-variant"
        description = "Choose the LLVM distribution that will be used during compilation.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{dev|user|absolute path to llvm}".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xbinary"
        compilerName = "binaryOptions"
        description = "Specify a binary option.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<option=value>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xruntime-logs"
        description = "Enable logging of Native runtime internals.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<tag1=level1,tag2=level2,...>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xdump-tests-to"
        compilerName = "testDumpOutputPath"
        description = "Path to a file for dumping the list of all available tests.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xlazy-ir-for-caches"
        description = "Use lazy IR for cached libraries.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{disable|enable}".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xomit-framework-binary"
        description = "Omit binary when compiling the framework.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xcompile-from-bitcode"
        description = "Continue compilation from the given bitcode file.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xread-dependencies-from"
        compilerName = "serializedDependencies"
        description = "Serialized dependencies to use for linking.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwrite-dependencies-to"
        compilerName = "saveDependenciesPath"
        description = "Path for writing backend dependencies.".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xsave-llvm-ir-directory"
        description = "Directory that should contain the results of '-Xsave-llvm-ir-after=<phase>'.".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xkonan-data-dir"
        description = "Custom path to the location of konan distributions.".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xllvm-module-passes"
        description = "Custom set of LLVM passes to run as the ModuleOptimizationPipeline.".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xllvm-lto-passes"
        compilerName = "llvmLTOPasses"
        description = "Custom set of LLVM passes to run as the LTOOptimizationPipeline.".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xmanifest-native-targets"
        description = "Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        stubLifecycle()
    }
}
