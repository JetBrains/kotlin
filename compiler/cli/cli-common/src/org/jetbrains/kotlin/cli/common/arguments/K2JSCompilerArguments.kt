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

    @GradleOption(
        value = DefaultValue.STRING_NULL_DEFAULT,
        gradleInputType = GradleInputTypes.INTERNAL, // handled by task 'outputFileProperty'
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @GradleDeprecatedOption(
        message = "Only for legacy backend. For IR backend please use task.destinationDirectory and moduleName",
        level = DeprecationLevel.WARNING,
        removeAfter = "1.9.0"
    )
    @Argument(value = "-output", valueDescription = "<filepath>", description = "Destination *.js file for the compilation result")
    var outputFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-ir-output-dir", valueDescription = "<directory>", description = "Destination for generated files")
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
    @Argument(value = "-ir-output-name", description = "Base name of generated files")
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
    @Argument(value = "-no-stdlib", description = "Don't automatically include the default Kotlin/JS stdlib into compilation dependencies")
    var noStdlib = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
            value = "-libraries",
            valueDescription = "<path>",
            description = "Paths to Kotlin libraries with .meta.js and .kjsm files, separated by system path separator"
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
        description = "How to map generated names to original names (IR backend only)"
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
    @Argument(value = "-meta-info", description = "Generate .meta.js and .kjsm files with metadata. Use to create a library")
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
    @Argument(value = "-target", valueDescription = "{ v5 }", description = "Generate JS files for specific ECMA version")
    var target: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-keep",
        description = "Comma-separated list of fully-qualified names to not be eliminated by DCE (if it can be reached), " +
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
            description = "Kind of the JS module generated by the compiler"
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
        description = "Define whether the `main` function should be called upon execution"
    )
    var main: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
            value = "-output-prefix",
            valueDescription = "<path>",
            description = "Add the content of the specified file to the beginning of output file"
    )
    var outputPrefix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
            value = "-output-postfix",
            valueDescription = "<path>",
            description = "Add the content of the specified file to the end of output file"
    )
    var outputPostfix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    // Advanced options

    @Argument(
        value = "-Xir-produce-klib-dir",
        description = "Generate unpacked KLIB into parent directory of output JS file.\n" +
                "In combination with -meta-info generates both IR and pre-IR versions of library."
    )
    var irProduceKlibDir = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-produce-klib-file",
        description = "Generate packed klib into file specified by -output. Disables pre-IR backend"
    )
    var irProduceKlibFile = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-produce-js", description = "Generates JS file using IR backend. Also disables pre-IR backend")
    var irProduceJs = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-dce", description = "Perform experimental dead code elimination")
    var irDce = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-dce-runtime-diagnostic",
        valueDescription = "{$RUNTIME_DIAGNOSTIC_LOG|$RUNTIME_DIAGNOSTIC_EXCEPTION}",
        description = "Enable runtime diagnostics when performing DCE instead of removing declarations"
    )
    var irDceRuntimeDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xir-dce-print-reachability-info",
        description = "Print declarations' reachability info to stdout during performing DCE"
    )
    var irDcePrintReachabilityInfo = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-property-lazy-initialization", description = "Perform lazy initialization for properties")
    var irPropertyLazyInitialization = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-minimized-member-names", description = "Perform minimization for names of members")
    var irMinimizedMemberNames = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-only", description = "Disables pre-IR backend")
    var irOnly = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-module-name",
        valueDescription = "<name>",
        description = "Specify a compilation module name for IR backend"
    )
    var irModuleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-base-class-in-metadata", description = "Write base class into metadata")
    var irBaseClassInMetadata = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-safe-external-boolean",
        description = "Safe access via Boolean() to Boolean properties in externals to safely cast falsy values."
    )
    var irSafeExternalBoolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-safe-external-boolean-diagnostic",
        valueDescription = "{$RUNTIME_DIAGNOSTIC_LOG|$RUNTIME_DIAGNOSTIC_EXCEPTION}",
        description = "Enable runtime diagnostics when access safely to boolean in external declarations"
    )
    var irSafeExternalBooleanDiagnostic: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-per-module", description = "Splits generated .js per-module")
    var irPerModule = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-per-module-output-name", description = "Adds a custom output name to the splitted js files")
    var irPerModuleOutputName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xir-per-file", description = "Splits generated .js per-file")
    var irPerFile = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xir-new-ir2js", description = "New fragment-based ir2js")
    var irNewIr2Js = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-generate-inline-anonymous-functions",
        description = "Lambda expressions that capture values are translated into in-line anonymous JavaScript functions"
    )
    var irGenerateInlineAnonymousFunctions = false
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

    @Argument(
        value = "-Xgenerate-dts",
        description = "Generate TypeScript declarations .d.ts file alongside JS file. Available in IR backend only."
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
        description = "Generate strict types for implicitly exported entities inside d.ts files. Available in IR backend only."
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
        description = "Generated JavaScript will use ES2015 classes."
    )
    var useEsClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_TRUE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-Xtyped-arrays", description = "Translate primitive arrays to JS typed arrays")
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

    @Argument(
        value = "-Xenable-extension-functions-in-externals",
        description = "Enable extensions functions members in external interfaces"
    )
    var extensionFunctionsInExternals = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xmetadata-only", description = "Generate *.meta.js and *.kjsm files only")
    var metadataOnly = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xenable-js-scripting", description = "Enable experimental support of .kts files using K/JS (with -Xir only)")
    var enableJsScripting = false
        set(value) {
            checkFrozen()
            field = value
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

    @Argument(value = "-Xpartial-linkage-loglevel", valueDescription = "{info|warning|error}", description = "Partial linkage compile-time log level")
    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xwasm", description = "Use experimental WebAssembly compiler backend")
    var wasm = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-debug-info", description = "Add debug info to WebAssembly compiled module")
    var wasmDebug = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-kclass-fqn", description = "Enable support for FQ names in KClass")
    var wasmKClassFqn = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-enable-array-range-checks", description = "Turn on range checks for the array access functions")
    var wasmEnableArrayRangeChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-enable-asserts", description = "Turn on asserts")
    var wasmEnableAsserts = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xwasm-generate-wat", description = "Generate wat file")
    var wasmGenerateWat = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xforce-deprecated-legacy-compiler-usage",
        description = "The flag is used only for our inner infrastructure. It will be removed soon, so it's unsafe to use it nowadays."
    )
    var forceDeprecatedLegacyCompilerUsage = false
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

fun K2JSCompilerArguments.isPreIrBackendDisabled(): Boolean =
    irOnly || irProduceJs || irProduceKlibFile || irBuildCache || useK2

fun K2JSCompilerArguments.isIrBackendEnabled(): Boolean =
    irProduceKlibDir || irProduceJs || irProduceKlibFile || wasm || irBuildCache || useK2
