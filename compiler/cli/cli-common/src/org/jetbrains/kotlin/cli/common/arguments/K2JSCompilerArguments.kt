/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass

class K2JSCompilerArguments : CommonCompilerArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @Deprecated("It is senseless to use with IR compiler. Only for compatibility.")
    @Argument(value = "-output", valueDescription = "<filepath>", description = "Destination *.js file for the compilation result.")
    var outputFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-ir-output-dir", valueDescription = "<directory>", description = "Destination for generated files.")
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
    @Argument(value = "-ir-output-name", description = "Base name of generated files.")
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_TRUE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @GradleDeprecatedOption(
        message = "Only for legacy backend.",
        level = DeprecationLevel.WARNING,
        removeAfter = "2.0.0"
    )
    @Argument(value = "-no-stdlib", description = "Don't automatically include the default Kotlin/JS stdlib in compilation dependencies.")
    var noStdlib = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
            value = "-libraries",
            valueDescription = "<path>",
            description = "Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator."
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
    @Argument(value = "-source-map", description = "Generate a source map.")
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
    @Argument(value = "-source-map-prefix", description = "Add the specified prefix to the paths in the source map.")
    var sourceMapPrefix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
            value = "-source-map-base-dirs",
            deprecatedName = "-source-map-source-roots",
            valueDescription = "<path>",
            description = "Base directories for calculating relative paths to source files in the source map."
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
            description = "Embed source files into the source map."
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
        description = "Mode for mapping generated names to original names (IR backend only)."
    )
    var sourceMapNamesPolicy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_TRUE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @GradleDeprecatedOption(
        message = "Only for legacy backend.",
        level = DeprecationLevel.WARNING,
        removeAfter = "2.0.0"
    )
    @Deprecated("It is senseless to use with IR compiler. Only for compatibility.")
    @Argument(value = "-meta-info", description = "Generate .meta.js and .kjsm files with metadata. Use this to create a library.")
    var metaInfo = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.JS_ECMA_VERSIONS,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-target", valueDescription = "{ v5 }", description = "Generate JS files for the specified ECMA version.")
    var target: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-keep",
        description = "Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), " +
                "and for which to keep non-minified names."
    )
    var irKeep: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.JS_MODULE_KINDS,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
            value = "-module-kind",
            valueDescription = "{plain|amd|commonjs|umd|es}",
            description = "The kind of JS module generated by the compiler."
    )
    var moduleKind: String? = MODULE_PLAIN
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) MODULE_PLAIN else value
        }

    @GradleOption(
        value = DefaultValue.JS_MAIN,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-main",
        valueDescription = "{$CALL|$NO_CALL}",
        description = "Specify whether the 'main' function should be called upon execution."
    )
    var main: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    // Advanced options

    @Argument(
        value = "-Xir-produce-klib-dir",
        description = """Generate an unpacked klib into the parent directory of the output JS file.
In combination with '-meta-info', this generates both IR and pre-IR versions of the library."""
    )
    var irProduceKlibDir = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-produce-klib-file",
        description = "Generate a packed klib into the file specified by '-output'. This disables the pre-IR backend."
    )
    var irProduceKlibFile = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-produce-js", description = "Generate a JS file using the IR backend. This option also disables the pre-IR backend.")
    var irProduceJs = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-dce", description = "Perform experimental dead code elimination.")
    var irDce = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-dce-runtime-diagnostic",
        valueDescription = "{$RUNTIME_DIAGNOSTIC_LOG|$RUNTIME_DIAGNOSTIC_EXCEPTION}",
        description = "Enable runtime diagnostics instead of removing declarations when performing DCE."
    )
    var irDceRuntimeDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-dce-print-reachability-info",
        description = "Print reachability information about declarations to 'stdout' while performing DCE."
    )
    var irDcePrintReachabilityInfo = false
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

    @Argument(value = "-Xir-property-lazy-initialization", description = "Perform lazy initialization for properties.")
    var irPropertyLazyInitialization = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-minimized-member-names", description = "Minimize the names of members.")
    var irMinimizedMemberNames = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-only", description = "Disable the pre-IR backend.")
    var irOnly = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-module-name",
        valueDescription = "<name>",
        description = "Specify the name of the compilation module for the IR backend."
    )
    var irModuleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-base-class-in-metadata", description = "Write base classes into metadata.")
    var irBaseClassInMetadata = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-safe-external-boolean",
        description = "Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'."
    )
    var irSafeExternalBoolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-safe-external-boolean-diagnostic",
        valueDescription = "{$RUNTIME_DIAGNOSTIC_LOG|$RUNTIME_DIAGNOSTIC_EXCEPTION}",
        description = "Enable runtime diagnostics when accessing external 'Boolean' properties."
    )
    var irSafeExternalBooleanDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-per-module", description = "Generate one .js file per module.")
    var irPerModule = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-per-module-output-name", description = "Add a custom output name to the split .js files.")
    var irPerModuleOutputName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-per-file", description = "Generate one .js file per source file.")
    var irPerFile = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-new-ir2js", description = "New fragment-based 'ir2js'.")
    var irNewIr2Js = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-generate-inline-anonymous-functions",
        description = "Lambda expressions that capture values are translated into in-line anonymous JavaScript functions."
    )
    var irGenerateInlineAnonymousFunctions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xinclude",
        valueDescription = "<path>",
        description = "Path to an intermediate library that should be processed in the same manner as source files."
    )
    var includes: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xcache-directory",
        valueDescription = "<path>",
        description = "Path to the cache directory."
    )
    var cacheDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-build-cache", description = "Use the compiler to build the cache.")
    var irBuildCache = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xgenerate-dts",
        description = "Generate a TypeScript declaration .d.ts file alongside the JS file. This is available only in the IR backend."
    )
    var generateDts = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xgenerate-polyfills",
        description = "Generate polyfills for features from the ES6+ standards."
    )
    var generatePolyfills = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xstrict-implicit-export-types",
        description = "Generate strict types for implicitly exported entities inside d.ts files. This is available in the IR backend only."
    )
    var strictImplicitExportType = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-Xes-classes",
        description = "Let generated JavaScript code use ES2015 classes."
    )
    var useEsClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xplatform-arguments-in-main-function",
        description = "JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function"
    )
    var platformArgumentsProviderJsExpression: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xes-generators",
        description = "Enable ES2015 generator functions usage inside the compiled code"
    )
    var useEsGenerators = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_TRUE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-Xtyped-arrays", description = "Translate primitive arrays into JS typed arrays.")
    var typedArrays = true
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-Xfriend-modules-disabled", description = "Disable internal declaration export.")
    var friendModulesDisabled = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
            value = "-Xfriend-modules",
            valueDescription = "<path>",
            description = "Paths to friend modules."
    )
    var friendModules: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xenable-extension-functions-in-externals",
        description = "Enable extension function members in external interfaces."
    )
    var extensionFunctionsInExternals = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xenable-js-scripting", description = "Enable experimental support for .kts files using K/JS (with '-Xir' only).")
    var enableJsScripting = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xfake-override-validator", description = "Enable the IR fake override validator.")
    var fakeOverrideValidator = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xerror-tolerance-policy", description = "Set up an error tolerance policy (NONE, SEMANTIC, SYNTAX, ALL).")
    var errorTolerancePolicy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xpartial-linkage", valueDescription = "{enable|disable}", description = "Use partial linkage mode.")
    var partialLinkageMode: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xpartial-linkage-loglevel", valueDescription = "{info|warning|error}", description = "Define the compile-time log level for partial linkage.")
    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xwasm", description = "Use the experimental WebAssembly compiler backend.")
    var wasm = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-debug-info", description = "Add debug info to the compiled WebAssembly module.")
    var wasmDebug = true
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

    @Argument(value = "-Xwasm-generate-wat", description = "Generate a .wat file.")
    var wasmGenerateWat = false
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
        value = "-Xoptimize-generated-js",
        description = "Perform additional optimizations on the generated JS code."
    )
    var optimizeGeneratedJs = true
        set(value) {
            checkFrozen()
            field = value
        }

    private fun MessageCollector.deprecationWarn(value: Boolean, defaultValue: Boolean, name: String) {
        if (value != defaultValue) {
            report(CompilerMessageSeverity.WARNING, "'$name' is deprecated and ignored, it will be removed in a future release")
        }
    }

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
        // TODO: 'enableJsScripting' is used in intellij tests
        //   Drop it after removing the usage from the intellij repository:
        //   https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/gradle/gradle-java/tests/test/org/jetbrains/kotlin/gradle/CompilerArgumentsCachingTest.kt#L329
        collector.deprecationWarn(enableJsScripting, false, "-Xenable-js-scripting")
        collector.deprecationWarn(irBaseClassInMetadata, false, "-Xir-base-class-in-metadata")
        collector.deprecationWarn(irNewIr2Js, true, "-Xir-new-ir2js")

        if (irPerFile && moduleKind != MODULE_ES) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Per-file compilation can't be used with any `moduleKind` except `es` (ECMAScript Modules)"
            )
        }

        return super.configureAnalysisFlags(collector, languageVersion).also {
            it[allowFullyQualifiedNameInKClass] = wasm && wasmKClassFqn //Only enabled WASM BE supports this flag
        }
    }

    override fun checkIrSupport(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        if (!isIrBackendEnabled()) return

        if (languageVersionSettings.languageVersion < LanguageVersion.KOTLIN_1_4
            || languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_4
        ) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "IR backend cannot be used with language or API version below 1.4"
            )
        }
    }

    override fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> {
        return super.configureLanguageFeatures(collector).apply {
            if (extensionFunctionsInExternals) {
                this[LanguageFeature.JsEnableExtensionFunctionInExternals] = LanguageFeature.State.ENABLED
            }
            if (!isIrBackendEnabled()) {
                this[LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping] = LanguageFeature.State.DISABLED
            }
            if (isIrBackendEnabled()) {
                this[LanguageFeature.JsAllowValueClassesInExternals] = LanguageFeature.State.ENABLED
            }
            if (wasm) {
                this[LanguageFeature.JsAllowImplementingFunctionInterface] = LanguageFeature.State.ENABLED
            }
        }
    }

    override fun copyOf(): Freezable = copyK2JSCompilerArguments(this, K2JSCompilerArguments())
}

fun K2JSCompilerArguments.isIrBackendEnabled(): Boolean =
    irProduceKlibDir || irProduceJs || irProduceKlibFile || wasm || irBuildCache || useK2
