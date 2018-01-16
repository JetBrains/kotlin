/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.bc

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.Argument

class K2NativeCompilerArguments : CommonCompilerArguments() {
    // First go the options interesting to the general public.
    // Prepend them with a single dash.
    // Keep the list lexically sorted.

    @Argument(value = "-enable_assertions", shortName = "-ea", description = "Enable runtime assertions in generated code")
    var enableAssertions: Boolean = false

    @Argument(value = "-g", description = "Enable emitting debug information")
    var debug: Boolean = false

    @Argument(value = "-generate_test_runner", shortName = "-tr", description = "Produce a runner for unit tests")
    var generateTestRunner: Boolean = false

    @Argument(value = "-includeBinary", shortName = "-ib", valueDescription = "<path>", description = "Pack external binary within the klib")
    var includeBinaries: Array<String>? = null

    @Argument(value = "-library", shortName = "-l", valueDescription = "<path>", description = "Link with the library")
    var libraries: Array<String>? = null

    @Argument(value = "-list_targets", description = "List available hardware targets")
    var listTargets: Boolean = false

    @Argument(value = "-manifest", valueDescription = "<path>", description = "Provide a maniferst addend file")
    var manifestFile: String? = null

    @Argument(value = "-module_name", valueDescription = "<name>", description = "Spicify a name for the compilation module")
    var moduleName: String? = null

    @Argument(value = "-nativelibrary", shortName = "-nl", valueDescription = "<path>", description = "Include the native bitcode library")
    var nativeLibraries: Array<String>? = null

    @Argument(value = "-nodefaultlibs", description = "Don't link the libraries from dist/klib automatically")
    var nodefaultlibs: Boolean = false

    @Argument(value = "-nomain", description = "Assume 'main' entry point to be provided by external libraries")
    var nomain: Boolean = false

    @Argument(value = "-nopack", description = "Don't pack the library into a klib file")
    var nopack: Boolean = false

    @Argument(value = "-linkerOpts", valueDescription = "<arg>", description = "Pass arguments to linker", delimiter = " ")
    var linkerArguments: Array<String>? = null

    @Argument(value = "-nostdlib", description = "Don't link with stdlib")
    var nostdlib: Boolean = false

    @Argument(value = "-opt", description = "Enable optimizations during compilation")
    var optimization: Boolean = false

    @Argument(value = "-output", shortName = "-o", valueDescription = "<name>", description = "Output name")
    var outputName: String? = null

    @Argument(value = "-entry", shortName = "-e", valueDescription = "<name>", description = "Qualified entry point name")
    var mainPackage: String? = null

    @Argument(value = "-produce", shortName = "-p", valueDescription = "{program|dynamic|framework|library|bitcode}", description = "Specify output file kind")
    var produce: String? = null

    @Argument(value = "-repo", shortName = "-r", valueDescription = "<path>", description = "Library search path")
    var repositories: Array<String>? = null

    @Argument(value = "-target", valueDescription = "<target>", description = "Set hardware target")
    var target: String? = null

    // The rest of the options are only interesting to the developers.
    // Make sure to prepend them with a double dash.
    // Keep the list lexically sorted.

    @Argument(value = "--check_dependencies", description = "Check dependencies and download the missing ones")
    var checkDependencies: Boolean = false

    @Argument(value = "--disable", valueDescription = "<Phase>", description = "Disable backend phase")
    var disablePhases: Array<String>? = null

    @Argument(value = "--enable", valueDescription = "<Phase>", description = "Enable backend phase")
    var enablePhases: Array<String>? = null

    @Argument(value = "--list_phases", description = "List all backend phases")
    var listPhases: Boolean = false

    @Argument(value = "--print_bitcode", description = "Print llvm bitcode")
    var printBitCode: Boolean = false

    @Argument(value = "--print_descriptors", description = "Print descriptor tree")
    var printDescriptors: Boolean = false

    @Argument(value = "--print_ir", description = "Print IR")
    var printIr: Boolean = false

    @Argument(value = "--print_ir_with_descriptors", description = "Print IR with descriptors")
    var printIrWithDescriptors: Boolean = false

    @Argument(value = "--print_locations", description = "Print locations")
    var printLocations: Boolean = false

    @Argument(value = "--purge_user_libs", description = "Don't link unused libraries even explicitly specified")
    var purgeUserLibs: Boolean = false

    @Argument(value = "--runtime", valueDescription = "<path>", description = "Override standard 'runtime.bc' location")

    var runtimeFile: String? = null
    @Argument(value = "--time", description = "Report execution time for compiler phases")
    var timePhases: Boolean = false

    @Argument(value = "--verbose", valueDescription = "<Phase>", description = "Trace phase execution")
    var verbosePhases: Array<String>? = null

    @Argument(value = "--verify_bitcode", description = "Verify llvm bitcode after each method")
    var verifyBitCode: Boolean = false

    @Argument(value = "--verify_descriptors", description = "Verify descriptor tree")
    var verifyDescriptors: Boolean = false

    @Argument(value = "--verify_ir", description = "Verify IR")
    var verifyIr: Boolean = false

}

