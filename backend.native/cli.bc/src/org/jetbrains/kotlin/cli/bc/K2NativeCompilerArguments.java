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
import org.jetbrains.kotlin.cli.common.arguments.ValueDescription;
import org.jetbrains.kotlin.cli.common.parser.com.sampullara.cli.Argument;

public class K2NativeCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "output", alias = "o", description = "Output file path")
    @ValueDescription("<path>")
    public String outputFile;

    @Argument(value = "runtime", description = "Override standard \'runtime.bc\' location")
    @ValueDescription("<path>")
    public String runtimeFile;

    @Argument(value = "properties", description = "Override standard \'konan.properties\' location")
    @ValueDescription("<path>")
    public String propertyFile;

    @Argument(value = "library", alias = "l", description = "Link with the library")
    @ValueDescription("<path>")
    public String[] libraries;

    @Argument(value = "nativelibrary", alias = "nl", description = "Include the native library")
    @ValueDescription("<path>")
    public String[] nativeLibraries;

    @Argument(value = "nolink", description = "Don't link, just produce a bitcode file")
    public boolean nolink;

    @Argument(value = "nomain", description = "Assume 'main' entry point to be provided by external libraries")
    public boolean nomain;

    @Argument(value = "linkerArgs", description = "Pass arguments to linker", delimiter = " ")
    @ValueDescription("<arg>")
    public String[] linkerArguments;

    @Argument(value = "nostdlib", description = "Don't link with stdlib")
    public boolean nostdlib;

    @Argument(value = "opt", description = "Enable optimizations during compilation")
    public boolean optimization;

    @Argument(value = "target", description = "Set hardware target")
    @ValueDescription("<target>")
    public String target;

    @Argument(value = "enable_assertions", alias = "ea", description = "Enable runtime assertions in generated code")
    public boolean enableAssertions;

    // The rest of the options are only interesting for developers.
    // Make sure to prepend them with double dash.

    @Argument(value = "list_targets", description = "List available hardware targets")
    public boolean listTargets;

    @Argument(value = "print_ir", prefix = "--", description = "Print IR")
    public boolean printIr;

    @Argument(value = "print_ir_with_descriptors", prefix = "--", description = "Print IR with descriptors")
    public boolean printIrWithDescriptors;

    @Argument(value = "print_descriptors", prefix = "--", description = "Print descriptor tree")
    public boolean printDescriptors;

    @Argument(value = "print_locations", prefix = "--", description = "Print locations")
    public boolean printLocations;

    @Argument(value = "print_bitcode", prefix = "--", description = "Print llvm bitcode")
    public boolean printBitCode;

    @Argument(value = "verify_ir", prefix = "--", description = "Verify IR")
    public boolean verifyIr;

    @Argument(value = "verify_descriptors", prefix = "--", description = "Verify descriptor tree")
    public boolean verifyDescriptors;

    @Argument(value = "verify_bitcode", prefix = "--", description = "Verify llvm bitcode after each method")
    public boolean verifyBitCode;

    @Argument(value = "enable", prefix = "--", description = "Enable backend phase")
    @ValueDescription("<Phase>")
    public String[] enablePhases;

    @Argument(value = "disable", prefix = "--", description = "Disable backend phase")
    @ValueDescription("<Phase>")
    public String[] disablePhases;

    @Argument(value = "verbose", prefix = "--", description = "Trace phase execution")
    @ValueDescription("<Phase>")
    public String[] verbosePhases;

    @Argument(value = "list_phases", prefix = "--", description = "List all backend phases")
    public boolean listPhases;

    @Argument(value = "time", prefix = "--", description = "Report execution time for compiler phases")
    public boolean timePhases;
}

