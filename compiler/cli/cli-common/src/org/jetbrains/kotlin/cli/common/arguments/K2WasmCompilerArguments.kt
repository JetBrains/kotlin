/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass

class K2WasmCompilerArguments : CommonCompilerArguments() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }

    @Argument(value = "-output-dir", valueDescription = "<directory>", description = "Destination for generated files")
    var outputDir: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.STRING_NULL_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-output-name", description = "Base name of generated files")
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-libraries",
        valueDescription = "<path>",
        description = "Paths to klib files, separated by system path separator"
    )
    var libraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-source-map", description = "Generate source map")
    var sourceMap = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.STRING_NULL_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-source-map-prefix", description = "Add the specified prefix to paths in the source map")
    var sourceMapPrefix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-source-map-base-dirs",
        deprecatedName = "-source-map-source-roots",
        valueDescription = "<path>",
        description = "Base directories for calculating relative paths to source files in source map"
    )
    var sourceMapBaseDirs: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    /**
     * SourceMapEmbedSources should be null by default, since it has effect only when source maps are enabled.
     * When sourceMapEmbedSources are not null and source maps is disabled warning is reported.
     */
    @GradleOption(
        value = DefaultValue.JS_SOURCE_MAP_CONTENT_MODES,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-source-map-embed-sources",
        valueDescription = "{always|never|inlining}",
        description = "Embed source files into source map"
    )
    var sourceMapEmbedSources: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.JS_SOURCE_MAP_NAMES_POLICY,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-source-map-names-policy",
        valueDescription = "{no|simple-names|fully-qualified-names}",
        description = "How to map generated names to original names"
    )
    var sourceMapNamesPolicy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    // Advanced options

    @Argument(
        value = "-Xproduce-klib-dir",
        description = "Generate unpacked KLIB into parent directory of output Wasm file."
    )
    var produceKlibDir = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xproduce-klib-file",
        description = "Generate packed klib into file specified by -output"
    )
    var produceKlibFile = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xproduce-wasm", description = "Generates Wasm and JS files")
    var produceWasm = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xdce", description = "Perform experimental dead code elimination")
    var dce = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdce-runtime-diagnostic",
        valueDescription = "{$RUNTIME_DIAGNOSTIC_LOG|$RUNTIME_DIAGNOSTIC_EXCEPTION}",
        description = "Enable runtime diagnostics when performing DCE instead of removing declarations"
    )
    var dceRuntimeDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xdce-print-reachability-info",
        description = "Print declarations' reachability info to stdout during performing DCE"
    )
    var dcePrintReachabilityInfo = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdce-dump-reachability-info-to-file",
        valueDescription = "<path>",
        description = "Dump declarations' reachability info collected during performing DCE to a file. " +
                "The format will be chosen automatically based on the file extension. " +
                "Supported output formats include JSON for .json, JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types."
    )
    var dceDumpReachabilityInfoToFile: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdump-declaration-ir-sizes-to-file",
        valueDescription = "<path>",
        description = "Dump the IR size of each declaration to a file. " +
                "The format will be chosen automatically depending on the file extension. " +
                "Supported output formats include JSON for .json, JS const initialized with a plain object containing information for .js, " +
                "and plain text for all other file types."
    )
    var dumpDeclarationIrSizesToFile: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xproperty-lazy-initialization", description = "Perform lazy initialization for properties")
    var propertyLazyInitialization = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xinclude",
        valueDescription = "<path>",
        description = "A path to an intermediate library that should be processed in the same manner as source files."
    )
    var includes: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xcache-directory",
        valueDescription = "<path>",
        description = "A path to cache directory"
    )
    var cacheDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-build-cache", description = "Use compiler to build cache")
    var irBuildCache = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-Xfriend-modules-disabled", description = "Disable internal declaration export")
    var friendModulesDisabled = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfriend-modules",
        valueDescription = "<path>",
        description = "Paths to friend modules"
    )
    var friendModules: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xfake-override-validator", description = "Enable IR fake override validator")
    var fakeOverrideValidator = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xerror-tolerance-policy", description = "Set up error tolerance policy (NONE, SEMANTIC, SYNTAX, ALL)")
    var errorTolerancePolicy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xpartial-linkage", valueDescription = "{enable|disable}", description = "Use partial linkage mode")
    var partialLinkageMode: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xpartial-linkage-loglevel",
        valueDescription = "{info|warning|error}",
        description = "Partial linkage compile-time log level"
    )
    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-debug-info", description = "Generated debug info")
    var debug = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xkclass-fqn", description = "Enable support for FQ names in KClass")
    var kClassFqn = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xenable-array-range-checks", description = "Turn on range checks for the array access functions")
    var enableArrayRangeChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xenable-asserts", description = "Turn on asserts")
    var enableAsserts = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xgenerate-wat", description = "Generate wat file")
    var generateWat = false
        set(value) {
            checkFrozen()
            field = value
        }

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
        return super.configureAnalysisFlags(collector, languageVersion).also {
            it[allowFullyQualifiedNameInKClass] = kClassFqn
        }
    }

    override fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> {
        return super.configureLanguageFeatures(collector).apply {
            this[LanguageFeature.JsAllowImplementingFunctionInterface] = LanguageFeature.State.ENABLED
        }
    }

    override fun copyOf(): Freezable = copyK2WasmCompilerArguments(this, K2WasmCompilerArguments())
}