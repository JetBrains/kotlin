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

import org.jetbrains.kotlin.cli.common.arguments.*

open class CommonInteropArguments : CommonToolArguments() {
    @Argument(value = "-flavor", valueDescription = "<flavor>", description = "One of: jvm, native or wasm")
    var flavor: String? = null

    @Argument(value = "-pkg", valueDescription = "<fully qualified name>", description = "place generated bindings to the package")
    var pkg: String? = null

    @Argument(value = "-generated", valueDescription = "<dir>", description = "place generated bindings to the directory")
    var generated: String? = null

    @Argument(value = "-natives", valueDescription = "<directory>", description = "where to put the built native files") 
    var natives: String? = null

    @Argument(value = "-manifest", valueDescription = "<file>", description = "library manifest addend") 
    var manifest: String? = null

    @Argument(value = "-staticLibrary", valueDescription = "<file>", description = "embed static library to the result") 
    var staticLibrary: Array<String> = arrayOf()

    @Argument(value = "-libraryPath", valueDescription = "<dir>", description = "add a library search path") 
    var libraryPath: Array<String> = arrayOf()
}

class CInteropArguments : CommonInteropArguments() {
    @Argument(value = "-import", valueDescription = "<imports>", description = "a semicolon separated list of headers, prepended with the package name") 
    var import: Array<String> = arrayOf()

    @Argument(value = "-target", valueDescription = "<target>", description = "native target to compile to") 
    var target: String? = null

    @Argument(value = "-def", valueDescription = "<file>", description = "the library definition file") 
    var def: String? = null

    // TODO: the short -h for -header conflicts with -h for -help.
    // The -header currently wins, but need to make it a little more sound.
    @Argument(value = "-header", shortName = "-h",  valueDescription = "<file>", description = "header file to produce kotlin bindings for") 
    var header: Array<String> = arrayOf()

    @Argument(value = HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX, shortName = "-hfasp",  valueDescription = "<file>", description = "header file to produce kotlin bindings for") 
    var headerFilterPrefix: Array<String> = arrayOf()

    @Argument(value = "-compilerOpts", shortName = "-copt", valueDescription = "<arg>", description = "additional compiler options", delimiter = " ")
    var compilerOpts: Array<String> = arrayOf()

    @Argument(value = "-linkerOpts", shortName = "-lopt", valueDescription = "<arg>", description = "additional linker options", delimiter = " ")
    var linkerOpts: Array<String> = arrayOf()

    @Argument(value = "-shims", description = "wrap bindings by a tracing layer") 
    var shims: Boolean = false

    @Argument(value = "-linker", valueDescription = "<file>", description = "use specified linker") 

    var linker: String? = null
    @Argument(value = "-cstubsname", valueDescription = "<name>", description = "provide a name for the generated c stubs file") 
    var cstubsname: String? = null

    @Argument(value = "-keepcstubs", description = "preserve the generated c stubs for inspection") 
    var keepcstubs: Boolean = false
}

const val HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX = "-headerFilterAdditionalSearchPrefix"

fun <T: CommonToolArguments> parseCommandLine(args: Array<String>, arguments: T): T {
    parseCommandLineArguments(args.asList(), arguments)
    reportArgumentParseProblems(arguments.errors)
    return arguments
}

// Integrate with CLITool from the big Kotlin and get rid of the mess below.

internal fun warn(msg: String) {
    println("warning: $msg")
}

// This is a copy of CLITool.kt's function adapted to work without a collector.
private fun reportArgumentParseProblems(errors: ArgumentParseErrors) {
    for (flag in errors.unknownExtraFlags) {
        warn("Flag is not supported by this version of the compiler: $flag")
    }
    for (argument in errors.extraArgumentsPassedInObsoleteForm) {
        warn("Advanced option value is passed in an obsolete form. Please use the '=' character " +
                "to specify the value: $argument=...")
    }
    for ((key, value) in errors.duplicateArguments) {
        warn("Argument $key is passed multiple times. Only the last value will be used: $value")
    }
    for ((deprecatedName, newName) in errors.deprecatedArguments) {
        warn("Argument $deprecatedName is deprecated. Please use $newName instead")
    }
}
