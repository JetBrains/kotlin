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

    @field:Argument(value = "-g", description = "Enable emitting debug information")
    @JvmField var debug: Boolean = false

    @field:Argument(value = "-enable_assertions", shortName = "-ea", description = "Enable runtime assertions in generated code")
    @JvmField var enableAssertions: Boolean = false

    @field:Argument(value = "-library", shortName = "-l", valueDescription = "<path>", description = "Link with the library")
    @JvmField var libraries: Array<String>? = null

    @field:Argument(value = "-list_targets", description = "List available hardware targets")
    @JvmField var listTargets: Boolean = false

    @field:Argument(value = "-manifest", valueDescription = "<path>", description = "Provide a maniferst addend file")
    @JvmField var manifestFile: String? = null

    @field:Argument(value = "-nativelibrary", shortName = "-nl", valueDescription = "<path>", description = "Include the native library")
    @JvmField var nativeLibraries: Array<String>? = null

    @field:Argument(value = "-nomain", description = "Assume 'main' entry point to be provided by external libraries")
    @JvmField var nomain: Boolean = false

    @field:Argument(value = "-nopack", description = "Don't pack the library into a klib file")
    @JvmField var nopack: Boolean = false

    @field:Argument(value = "-linkerOpts", valueDescription = "<arg>", description = "Pass arguments to linker", delimiter = " ")
    @JvmField var linkerArguments: Array<String>? = null

    @field:Argument(value = "-nostdlib", description = "Don't link with stdlib")
    @JvmField var nostdlib: Boolean = false

    @field:Argument(value = "-opt", description = "Enable optimizations during compilation")
    @JvmField var optimization: Boolean = false

    @field:Argument(value = "-output", shortName = "-o", valueDescription = "<name>", description = "Output name")
    @JvmField var outputName: String? = null

    @field:Argument(value = "-entry", shortName = "-e", valueDescription = "<name>", description = "Qualified entry point name")
    @JvmField var mainPackage: String? = null

    @field:Argument(value = "-produce", shortName = "-p", valueDescription = "{program|library|bitcode}", description = "Specify output file kind")
    @JvmField var produce: String? = null

    @field:Argument(value = "-properties", valueDescription = "<path>", description = "Override standard 'konan.properties' location")
    @JvmField var propertyFile: String? = null

    @field:Argument(value = "-repo", shortName = "-r", valueDescription = "<path>", description = "Library search path")
    @JvmField var repositories: Array<String>? = null

    @field:Argument(value = "-runtime", valueDescription = "<path>", description = "Override standard 'runtime.bc' location")
    @JvmField var runtimeFile: String? = null

    @field:Argument(value = "-target", valueDescription = "<target>", description = "Set hardware target")
    @JvmField var target: String? = null

    // The rest of the options are only interesting to the developers.
    // Make sure to prepend them with a double dash.
    // Keep the list lexically sorted.

    @field:Argument(value = "--enable", valueDescription = "<Phase>", description = "Enable backend phase")
    @JvmField var enablePhases: Array<String>? = null

    @field:Argument(value = "--disable", valueDescription = "<Phase>", description = "Disable backend phase")
    @JvmField var disablePhases: Array<String>? = null

    @field:Argument(value = "--list_phases", description = "List all backend phases")
    @JvmField var listPhases: Boolean = false

    @field:Argument(value = "--print_bitcode", description = "Print llvm bitcode")
    @JvmField var printBitCode: Boolean = false

    @field:Argument(value = "--print_descriptors", description = "Print descriptor tree")
    @JvmField var printDescriptors: Boolean = false

    @field:Argument(value = "--print_ir", description = "Print IR")
    @JvmField var printIr: Boolean = false

    @field:Argument(value = "--print_ir_with_descriptors", description = "Print IR with descriptors")
    @JvmField var printIrWithDescriptors: Boolean = false

    @field:Argument(value = "--print_locations", description = "Print locations")
    @JvmField var printLocations: Boolean = false

    @field:Argument(value = "--time", description = "Report execution time for compiler phases")
    @JvmField var timePhases: Boolean = false

    @field:Argument(value = "--verbose", valueDescription = "<Phase>", description = "Trace phase execution")
    @JvmField var verbosePhases: Array<String>? = null

    @field:Argument(value = "--verify_bitcode", description = "Verify llvm bitcode after each method")
    @JvmField var verifyBitCode: Boolean = false

    @field:Argument(value = "--verify_descriptors", description = "Verify descriptor tree")
    @JvmField var verifyDescriptors: Boolean = false

    @field:Argument(value = "--verify_ir", description = "Verify IR")
    @JvmField var verifyIr: Boolean = false

}

