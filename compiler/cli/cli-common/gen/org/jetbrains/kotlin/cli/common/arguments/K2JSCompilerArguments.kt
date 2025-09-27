/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.kotlin.config.LanguageFeature

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/JsCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

class K2JSCompilerArguments : K2WasmCompilerArguments() {
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
        value = "-Xenable-extension-functions-in-externals",
        description = "Enable extension function members in external interfaces.",
    )
    @Enables(LanguageFeature.JsEnableExtensionFunctionInExternals)
    var extensionFunctionsInExternals: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xes-arrow-functions",
        description = "Use ES2015 arrow functions in the JavaScript code generated for Kotlin lambdas. Enabled by default in case of ES2015 target usage",
    )
    var useEsArrowFunctions: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xes-classes",
        description = "Let generated JavaScript code use ES2015 classes. Enabled by default in case of ES2015 target usage",
    )
    var useEsClasses: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xes-generators",
        description = "Enable ES2015 generator functions usage inside the compiled code. Enabled by default in case of ES2015 target usage",
    )
    var useEsGenerators: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xes-long-as-bigint",
        description = "Compile Long values as ES2020 bigint instead of object.",
    )
    var compileLongAsBigInt: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
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
        value = "-Xgenerate-polyfills",
        description = "Generate polyfills for features from the ES6+ standards.",
    )
    var generatePolyfills: Boolean = true
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
        value = "-Xir-build-cache",
        description = "Use the compiler to build the cache.",
    )
    var irBuildCache: Boolean = false
        set(value) {
            checkFrozen()
            field = value
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
        value = "-Xir-generate-inline-anonymous-functions",
        description = "Lambda expressions that capture values are translated into in-line anonymous JavaScript functions.",
    )
    var irGenerateInlineAnonymousFunctions: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-keep",
        description = "Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), and for which to keep non-minified names.",
    )
    var irKeep: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-minimized-member-names",
        description = "Minimize the names of members.",
    )
    var irMinimizedMemberNames: Boolean = false
        set(value) {
            checkFrozen()
            field = value
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
        value = "-Xir-safe-external-boolean",
        description = "Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.",
    )
    var irSafeExternalBoolean: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-safe-external-boolean-diagnostic",
        valueDescription = "{log|exception}",
        description = "Enable runtime diagnostics when accessing external 'Boolean' properties.",
    )
    var irSafeExternalBooleanDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xoptimize-generated-js",
        description = "Perform additional optimizations on the generated JS code.",
    )
    var optimizeGeneratedJs: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xplatform-arguments-in-main-function",
        description = "JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function",
    )
    var platformArgumentsProviderJsExpression: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
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
        value = "-module-kind",
        valueDescription = "{plain|amd|commonjs|umd|es}",
        description = "The kind of JS module generated by the compiler. ES modules are enabled by default in case of ES2015 target usage",
    )
    var moduleKind: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Deprecated("It is senseless to use with IR compiler. Only for compatibility.")
    @Argument(
        value = "-output",
        valueDescription = "<filepath>",
        description = "",
        isObsolete = true,
    )
    var outputFile: String? = null
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

    @Argument(
        value = "-target",
        valueDescription = "{ es5, es2015 }",
        description = "Generate JS files for the specified ECMA version.",
    )
    var target: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @get:Transient
    @field:kotlin.jvm.Transient
    override val configurator: CommonCompilerArgumentsConfigurator = K2JSCompilerArgumentsConfigurator()

    override fun copyOf(): Freezable = copyK2JSCompilerArguments(this, K2JSCompilerArguments())
}
