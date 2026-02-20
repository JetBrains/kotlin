/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.defaultTrue
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringType


val actualWasmArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.wasmArguments) {
    compilerArgument {
        name = "Xwasm"
        description = "Use the WebAssembly compiler backend.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-target"
        description = "Set up the Wasm target (wasm-js or wasm-wasi).".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-debug-info"
        compilerName = "wasmDebug"
        description = "Add debug info to the compiled WebAssembly module.".asReleaseDependent()
        argumentType = BooleanType(
            isNullable = false.asReleaseDependent(),
            defaultValue = true.asReleaseDependent()
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-debug-friendly"
        compilerName = "forceDebugFriendlyCompilation"
        description = "Avoid optimizations that can break debugging.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-included-module-only"
        description = "Compile only a module passed using `-include` option.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xwasm-generate-closed-world-multimodule"
        description = "Compile modules in multi-module closed-world mode using module passed in `-include` argument as main module".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }

    compilerArgument {
        name = "Xwasm-generate-wat"
        description = "Generate a .wat file.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-kclass-fqn"
        compilerName = "wasmKClassFqn"
        description = "Enable support for 'KClass.qualifiedName'.".asReleaseDependent()
        argumentType = BooleanType.defaultTrue

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-enable-array-range-checks"
        description = "Turn on range checks for array access functions.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-enable-asserts"
        description = "Turn on asserts.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-use-traps-instead-of-exceptions"
        description = "Use traps instead of throwing exceptions.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-internal-local-variable-prefix"
        description = "Prefix to use for internally generated local variables.".asReleaseDependent()
        argumentType = StringType(
            isNullable = false.asReleaseDependent(),
            defaultValue = "~".asReleaseDependent()
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0
        )
    }

    compilerArgument {
        name = "Xwasm-use-new-exception-proposal"
        description = "Use an updated version of the exception proposal with try_table.".asReleaseDependent()
        argumentType = BooleanType(
            isNullable = ReleaseDependent(
                true,
                KotlinReleaseVersion.v2_1_20..KotlinReleaseVersion.v2_2_20 to false,
            ),
            defaultValue = ReleaseDependent(
                null,
                KotlinReleaseVersion.v2_1_20..KotlinReleaseVersion.v2_2_20 to false,
            )
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {

        name = "Xwasm-no-jstag"
        compilerName = "wasmNoJsTag"
        description = "Don't use WebAssembly.JSTag for throwing and catching exceptions".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20,
        )
    }

    compilerArgument {
        name = "Xwasm-debugger-custom-formatters"
        compilerName = "debuggerCustomFormatters"
        description = "Generates devtools custom formatters (https://firefox-source-docs.mozilla.org/devtools-user/custom_formatters) for Kotlin/Wasm values".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-source-map-include-mappings-from-unavailable-sources"
        compilerName = "includeUnavailableSourcesIntoSourceMap"
        description = "Insert source mappings from libraries even if their sources are unavailable on the end-user machine.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xwasm-generate-dwarf"
        compilerName = "generateDwarf"
        description = "Generate DWARF debug information.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xir-dce-dump-reachability-info-to-file"
        description = ("Dump reachability information collected about declarations while performing DCE to a file. " +
                "The format will be chosen automatically based on the file extension. " +
                "Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types.").asReleaseDependent()
        argumentType = StringType.defaultNull
        argumentTypeDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xir-dump-declaration-ir-sizes-to-file"
        compilerName = "irDceDumpDeclarationIrSizesToFile"
        description = ("Dump the IR size of each declaration into a file. " +
                "The format will be chosen automatically depending on the file extension. " +
                "Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types.").asReleaseDependent()
        argumentType = StringType.defaultNull
        argumentTypeDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }
}
