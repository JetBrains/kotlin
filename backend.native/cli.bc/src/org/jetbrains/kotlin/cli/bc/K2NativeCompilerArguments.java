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

package org.jetbrains.kotlin.cli.bc;

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.Argument;

public class K2NativeCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "-output", shortName = "-o", valueDescription = "<path>", description = "Output file path")
    public String outputFile;

    @Argument(value = "-runtime", valueDescription = "<path>", description = "Override standard 'runtime.bc' location")
    public String runtimeFile;

    @Argument(value = "-properties", valueDescription = "<path>", description = "Override standard 'konan.properties' location")
    public String propertyFile;

    @Argument(value = "-library", shortName = "-l", valueDescription = "<path>", description = "Link with the library")
    public String[] libraries;

    @Argument(value = "-nativelibrary", shortName = "-nl", valueDescription = "<path>", description = "Include the native library")
    public String[] nativeLibraries;

    @Argument(value = "-nolink", description = "Don't link, just produce a bitcode file")
    public boolean nolink;

    @Argument(value = "-nopack", description = "Don't pack the library into a klib file")
    public boolean nopack;

    @Argument(value = "-nomain", description = "Assume 'main' entry point to be provided by external libraries")
    public boolean nomain;

    @Argument(value = "-linkerArgs", valueDescription = "<arg>", description = "Pass arguments to linker", delimiter = " ")
    public String[] linkerArguments;

    @Argument(value = "-nostdlib", description = "Don't link with stdlib")
    public boolean nostdlib;

    @Argument(value = "-opt", description = "Enable optimizations during compilation")
    public boolean optimization;

    @Argument(value = "-target", valueDescription = "<target>", description = "Set hardware target")
    public String target;

    @Argument(value = "-g", description = "Enable emitting debug information")
    public boolean debug;

    @Argument(value = "-enable_assertions", shortName = "-ea", description = "Enable runtime assertions in generated code")
    public boolean enableAssertions;

    // The rest of the options are only interesting for developers.
    // Make sure to prepend them with double dash.

    @Argument(value = "-list_targets", description = "List available hardware targets")
    public boolean listTargets;

    @Argument(value = "--print_ir", description = "Print IR")
    public boolean printIr;

    @Argument(value = "--print_ir_with_descriptors", description = "Print IR with descriptors")
    public boolean printIrWithDescriptors;

    @Argument(value = "--print_descriptors", description = "Print descriptor tree")
    public boolean printDescriptors;

    @Argument(value = "--print_locations", description = "Print locations")
    public boolean printLocations;

    @Argument(value = "--print_bitcode", description = "Print llvm bitcode")
    public boolean printBitCode;

    @Argument(value = "--verify_ir", description = "Verify IR")
    public boolean verifyIr;

    @Argument(value = "--verify_descriptors", description = "Verify descriptor tree")
    public boolean verifyDescriptors;

    @Argument(value = "--verify_bitcode", description = "Verify llvm bitcode after each method")
    public boolean verifyBitCode;

    @Argument(value = "--enable", valueDescription = "<Phase>", description = "Enable backend phase")
    public String[] enablePhases;

    @Argument(value = "--disable", valueDescription = "<Phase>", description = "Disable backend phase")
    public String[] disablePhases;

    @Argument(value = "--verbose", valueDescription = "<Phase>", description = "Trace phase execution")
    public String[] verbosePhases;

    @Argument(value = "--list_phases", description = "List all backend phases")
    public boolean listPhases;

    @Argument(value = "--time", description = "Report execution time for compiler phases")
    public boolean timePhases;
}
