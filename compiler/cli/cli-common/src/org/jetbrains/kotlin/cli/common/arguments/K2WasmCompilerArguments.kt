/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

abstract class K2WasmCompilerArguments : CommonKlibBasedCompilerArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @Argument(value = "-Xwasm", description = "Use the WebAssembly compiler backend.")
    var wasm = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-target", description = "Set up the Wasm target (wasm-js or wasm-wasi).")
    var wasmTarget: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xwasm-debug-info", description = "Add debug info to the compiled WebAssembly module.")
    var wasmDebug = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-generate-wat", description = "Generate a .wat file.")
    var wasmGenerateWat = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-kclass-fqn", description = "Enable support for 'KClass.qualifiedName'.")
    var wasmKClassFqn = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-enable-array-range-checks", description = "Turn on range checks for array access functions.")
    var wasmEnableArrayRangeChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-enable-asserts", description = "Turn on asserts.")
    var wasmEnableAsserts = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xwasm-use-traps-instead-of-exceptions",
        description = "Use traps instead of throwing exceptions."
    )
    var wasmUseTrapsInsteadOfExceptions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xwasm-use-new-exception-proposal",
        description = "Use an updated version of the exception proposal with try_table."
    )
    var wasmUseNewExceptionProposal = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xwasm-attach-js-exception",
        description = "Attach a thrown by JS-value to the JsException class"
    )
    var wasmUseJsTag: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xwasm-debugger-custom-formatters",
        description = "Generates devtools custom formatters (https://firefox-source-docs.mozilla.org/devtools-user/custom_formatters) for Kotlin/Wasm values"
    )
    var debuggerCustomFormatters = false
        set(value) {
            checkFrozen()
            field = value
        }


    @Argument(
        value = "-Xwasm-source-map-include-mappings-from-unavailable-sources",
        description = "Insert source mappings from libraries even if their sources are unavailable on the end-user machine."
    )
    var includeUnavailableSourcesIntoSourceMap = false
        set(value) {
            checkFrozen()
            field = value
        }


    @Argument(value = "-Xwasm-preserve-ic-order", description = "Preserve wasm file structure between IC runs.")
    var preserveIcOrder = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-ic-cache-readonly", description = "Do not commit IC cache updates.")
    var icCacheReadonly = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-generate-dwarf", description = "Generate DWARF debug information.")
    var generateDwarf = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-dce-dump-reachability-info-to-file",
        valueDescription = "<path>",
        description = "Dump reachability information collected about declarations while performing DCE to a file. " +
                "The format will be chosen automatically based on the file extension. " +
                "Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types."
    )
    var irDceDumpReachabilityInfoToFile: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-dump-declaration-ir-sizes-to-file",
        valueDescription = "<path>",
        description = "Dump the IR size of each declaration into a file. " +
                "The format will be chosen automatically depending on the file extension. " +
                "Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types."
    )
    var irDceDumpDeclarationIrSizesToFile: String? = null
        set(value) {
            checkFrozen()
            field = value
        }
}
