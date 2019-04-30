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

package org.jetbrains.kotlin.native.interop.tool

import org.jetbrains.kliopt.*

const val HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX = "headerFilterAdditionalSearchPrefix"
const val NODEFAULTLIBS = "nodefaultlibs"
const val PURGE_USER_LIBS = "Xpurge-user-libs"
const val TEMP_DIR = "Xtemporary-files-dir"

// TODO: unify camel and snake cases.
// Possible solution is to accept both cases
fun getCommonInteropArguments() = listOf(
        OptionDescriptor(ArgType.Boolean(), "verbose", description = "Enable verbose logging output", defaultValue = "false"),
        OptionDescriptor(ArgType.Choice(listOf("jvm", "native", "wasm")),
                "flavor", description = "Interop target", defaultValue = "jvm"),
        OptionDescriptor(ArgType.String(), "pkg", description = "place generated bindings to the package"),
        OptionDescriptor(ArgType.String(), "output", "o", "specifies the resulting library file", defaultValue = "nativelib"),
        OptionDescriptor(ArgType.String(), "libraryPath", description = "add a library search path",
                isMultiple = true, delimiter = ","),
        OptionDescriptor(ArgType.String(), "staticLibrary", description = "embed static library to the result",
                isMultiple = true, delimiter = ","),
        OptionDescriptor(ArgType.String(), "generated", description = "place generated bindings to the directory",
                defaultValue = System.getProperty("user.dir")),
        OptionDescriptor(ArgType.String(), "natives", description = "where to put the built native files",
                defaultValue = System.getProperty("user.dir")),
        OptionDescriptor(ArgType.String(), "library", "l", "library to use for building", isMultiple = true),
        OptionDescriptor(ArgType.String(), "repo", "r",
                "repository to resolve dependencies", isMultiple = true),
        OptionDescriptor(ArgType.Boolean(), NODEFAULTLIBS, description = "don't link the libraries from dist/klib automatically",
                defaultValue = "false"),
        OptionDescriptor(ArgType.Boolean(), PURGE_USER_LIBS, description = "don't link unused libraries even explicitly specified",
                defaultValue = "false"),
        OptionDescriptor(ArgType.String(), TEMP_DIR, description = "save temporary files to the given directory")
    )

fun getCInteropArguments(): List<OptionDescriptor> {
    val options = listOf(
            OptionDescriptor(ArgType.String(), "target", description = "native target to compile to", defaultValue = "host"),
            OptionDescriptor(ArgType.String(), "def", description = "the library definition file"),
            OptionDescriptor(ArgType.String(), "header", description = "header file to produce kotlin bindings for",
                    isMultiple = true, delimiter = ","),
            OptionDescriptor(ArgType.String(), "h", description = "header file to produce kotlin bindings for",
                    isMultiple = true, delimiter = ",", deprecatedWarning = "Option -h is deprecated. Please use -header."),
            OptionDescriptor(ArgType.String(), HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX, "hfasp",
                    "header file to produce kotlin bindings for", isMultiple = true, delimiter = ","),
            OptionDescriptor(ArgType.String(), "compilerOpts",
                    description = "additional compiler options (allows to add several options separated by spaces)",
                    isMultiple = true, delimiter = " "),
            OptionDescriptor(ArgType.String(), "compiler-options",
                    description = "additional compiler options (allows to add several options separated by spaces)",
                    isMultiple = true, delimiter = " "),
            OptionDescriptor(ArgType.String(), "linkerOpts",
                    description = "additional linker options (allows to add several options separated by spaces)",
                    isMultiple = true, delimiter = " "),
            OptionDescriptor(ArgType.String(), "linker-options",
                    description = "additional linker options (allows to add several options separated by spaces)",
                    isMultiple = true, delimiter = " "),
            OptionDescriptor(ArgType.String(), "compiler-option",
                    description = "additional compiler option", isMultiple = true),
            OptionDescriptor(ArgType.String(), "linker-option",
                    description = "additional linker option", isMultiple = true),
            OptionDescriptor(ArgType.String(), "copt", description = "additional compiler options (allows to add several options separated by spaces)",
                    isMultiple = true, delimiter = " ", deprecatedWarning = "Option -copt is deprecated. Please use -compiler-options."),
            OptionDescriptor(ArgType.String(), "lopt", description = "additional linker options (allows to add several options separated by spaces)",
                    isMultiple = true, delimiter = " ", deprecatedWarning = "Option -lopt is deprecated. Please use -linker-options."),
            OptionDescriptor(ArgType.String(), "linker", description = "use specified linker")
    )
    return (options + getCommonInteropArguments())
}

fun getJSInteropArguments(): List<OptionDescriptor> {
    val options = listOf(
            OptionDescriptor(ArgType.Choice(listOf("wasm32")), "target", description = "wasm target to compile to", defaultValue = "wasm32")
    )
    return (options + getCommonInteropArguments())
}

internal fun warn(msg: String) {
    println("warning: $msg")
}

fun ArgParser.getValuesAsArray(propertyName: String) =
        (getAll<String>(propertyName) ?: listOf<String>()).toTypedArray()
