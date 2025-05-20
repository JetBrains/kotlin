/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.stubLifecycle
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.StringType


val actualWasmArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.wasmArguments) {
    compilerArgument {
        name = "Xwasm"
        description = "Use the WebAssembly compiler backend.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-target"
        description = "Set up the Wasm target (wasm-js or wasm-wasi).".asReleaseDependent()
        valueType = StringType.defaultNull

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-debug-info"
        compilerName = "wasmDebug"
        description = "Add debug info to the compiled WebAssembly module.".asReleaseDependent()
        valueType = BooleanType(
            isNullable = false.asReleaseDependent(),
            defaultValue = true.asReleaseDependent()
        )

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-debug-friendly"
        compilerName = "forceDebugFriendlyCompilation"
        description = "Avoid optimizations that can break debugging.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-generate-wat"
        description = "Generate a .wat file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-kclass-fqn"
        compilerName = "wasmKClassFqn"
        description = "Enable support for 'KClass.qualifiedName'.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-enable-array-range-checks"
        description = "Turn on range checks for array access functions.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-enable-asserts"
        description = "Turn on asserts.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-use-traps-instead-of-exceptions"
        description = "Use traps instead of throwing exceptions.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-use-new-exception-proposal"
        description = "Use an updated version of the exception proposal with try_table.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-attach-js-exception"
        compilerName = "wasmUseJsTag"
        description = "Attach a thrown by JS-value to the JsException class".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-debugger-custom-formatters"
        compilerName = "debuggerCustomFormatters"
        description = "Generates devtools custom formatters (https://firefox-source-docs.mozilla.org/devtools-user/custom_formatters) for Kotlin/Wasm values".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-source-map-include-mappings-from-unavailable-sources"
        compilerName = "includeUnavailableSourcesIntoSourceMap"
        description = "Insert source mappings from libraries even if their sources are unavailable on the end-user machine.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-preserve-ic-order"
        compilerName = "preserveIcOrder"
        description = "Preserve wasm file structure between IC runs.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-ic-cache-readonly"
        compilerName = "icCacheReadonly"
        description = "Do not commit IC cache updates.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xwasm-generate-dwarf"
        compilerName = "generateDwarf"
        description = "Generate DWARF debug information.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        stubLifecycle()
    }

    compilerArgument {
        name = "Xir-dce-dump-reachability-info-to-file"
        description = ("Dump reachability information collected about declarations while performing DCE to a file. " +
                "The format will be chosen automatically based on the file extension. " +
                "Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types.").asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }

    compilerArgument {
        name = "Xir-dump-declaration-ir-sizes-to-file"
        compilerName = "irDceDumpDeclarationIrSizesToFile"
        description = ("Dump the IR size of each declaration into a file. " +
                "The format will be chosen automatically depending on the file extension. " +
                "Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types.").asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        stubLifecycle()
    }
}
