/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/CommonJsWasmCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

abstract class CommonJsWasmCompilerArguments : CommonKlibBasedCompilerArguments() {
    @Argument(
        value = "-Xcache-directory",
        valueDescription = "<path>",
        description = "Path to the cache directory.",
    )
    var cacheDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xfake-override-validator",
        description = "Enable the IR fake override validator.",
    )
    var fakeOverrideValidator: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfriend-modules",
        valueDescription = "<path>",
        description = "Paths to friend modules.",
    )
    var friendModules: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xfriend-modules-disabled",
        description = "Disable internal declaration export.",
    )
    var friendModulesDisabled: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xgenerate-dts",
        description = "Generate a TypeScript declaration .d.ts file alongside the JS file.",
    )
    var generateDts: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xinclude",
        valueDescription = "<path>",
        description = "Path to an intermediate library that should be processed in the same manner as source files.",
    )
    var includes: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-dce",
        description = "Perform experimental dead code elimination.",
    )
    var irDce: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-dce-print-reachability-info",
        description = "Print reachability information about declarations to 'stdout' while performing DCE.",
    )
    var irDcePrintReachabilityInfo: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-dce-runtime-diagnostic",
        valueDescription = "{log|exception}",
        description = "Enable runtime diagnostics instead of removing declarations when performing DCE.",
    )
    var irDceRuntimeDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-module-name",
        valueDescription = "<name>",
        description = "Specify the name of the compilation module for the IR backend.",
    )
    var irModuleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-per-file",
        description = "Generate one .js file per source file.",
    )
    var irPerFile: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-per-module",
        description = "Generate one .js file per module.",
    )
    var irPerModule: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-per-module-output-name",
        description = "Add a custom output name to the split .js files.",
    )
    var irPerModuleOutputName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-produce-js",
        description = "Generate a JS file using the IR backend.",
    )
    var irProduceJs: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-produce-klib-dir",
        description = "Generate an unpacked klib into the parent directory of the output JS file.",
    )
    var irProduceKlibDir: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-produce-klib-file",
        description = "Generate a packed klib into the directory specified by '-ir-output-dir'.",
    )
    var irProduceKlibFile: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-property-lazy-initialization",
        description = "Perform lazy initialization for properties.",
    )
    var irPropertyLazyInitialization: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xstrict-implicit-export-types",
        description = "Generate strict types for implicitly exported entities inside d.ts files.",
    )
    var strictImplicitExportType: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-ir-output-dir",
        valueDescription = "<directory>",
        description = "Destination for generated files.",
    )
    var outputDir: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-ir-output-name",
        description = "Base name of generated files.",
    )
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-libraries",
        valueDescription = "<path>",
        description = "Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.",
    )
    var libraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-main",
        valueDescription = "{call|noCall}",
        description = "Specify whether the 'main' function should be called upon execution.",
    )
    var main: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-source-map",
        description = "Generate a source map.",
    )
    var sourceMap: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-source-map-base-dirs",
        deprecatedName = "-source-map-source-roots",
        valueDescription = "<path>",
        description = "Base directories for calculating relative paths to source files in the source map.",
    )
    var sourceMapBaseDirs: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-source-map-embed-sources",
        valueDescription = "{always|never|inlining}",
        description = "Embed source files into the source map.",
    )
    var sourceMapEmbedSources: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-source-map-names-policy",
        valueDescription = "{no|simple-names|fully-qualified-names}",
        description = "Mode for mapping generated names to original names.",
    )
    var sourceMapNamesPolicy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-source-map-prefix",
        description = "Add the specified prefix to the paths in the source map.",
    )
    var sourceMapPrefix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

}
