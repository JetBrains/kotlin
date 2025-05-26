/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel.ERROR
import kotlin.String
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.BOOLEAN_FALSE_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.BOOLEAN_NULL_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.JS_ECMA_VERSIONS
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.JS_MAIN
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.JS_MODULE_KINDS
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.JS_SOURCE_MAP_CONTENT_MODES
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.JS_SOURCE_MAP_NAMES_POLICY
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.STRING_NULL_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes.INPUT
import org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_2_2
import com.intellij.util.xmlb.annotations.Transient as AnnotationsTransient
import kotlin.jvm.Transient as JvmTransient

/**
 * This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
 * Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/JsCompilerArguments.kt
 * DO NOT MODIFY IT MANUALLY.
 */
public class K2JSCompilerArguments : K2WasmCompilerArguments() {
  @Deprecated("It is senseless to use with IR compiler. Only for compatibility.")
  @Argument(
    value = "-output",
    valueDescription = "<filepath>",
    isObsolete = true,
    description = "",
  )
  public var outputFile: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-ir-output-dir",
    valueDescription = "<directory>",
    description = "Destination for generated files.",
  )
  public var outputDir: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = STRING_NULL_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-ir-output-name",
    description = "Base name of generated files.",
  )
  public var moduleName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-libraries",
    valueDescription = "<path>",
    description = "Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.",
  )
  public var libraries: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-source-map",
    description = "Generate a source map.",
  )
  public var sourceMap: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = STRING_NULL_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-source-map-prefix",
    description = "Add the specified prefix to the paths in the source map.",
  )
  public var sourceMapPrefix: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-source-map-base-dirs",
    deprecatedName = "-source-map-source-roots",
    valueDescription = "<path>",
    description = "Base directories for calculating relative paths to source files in the source map.",
  )
  public var sourceMapBaseDirs: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = JS_SOURCE_MAP_CONTENT_MODES,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-source-map-embed-sources",
    valueDescription = "{always|never|inlining}",
    description = "Embed source files into the source map.",
  )
  public var sourceMapEmbedSources: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = JS_SOURCE_MAP_NAMES_POLICY,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-source-map-names-policy",
    valueDescription = "{no|simple-names|fully-qualified-names}",
    description = "Mode for mapping generated names to original names.",
  )
  public var sourceMapNamesPolicy: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = JS_ECMA_VERSIONS,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-target",
    valueDescription = "{ es5, es2015 }",
    description = "Generate JS files for the specified ECMA version.",
  )
  public var target: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-keep",
    description = "Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), and for which to keep non-minified names.",
  )
  public var irKeep: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = JS_MODULE_KINDS,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-module-kind",
    valueDescription = "{plain|amd|commonjs|umd|es}",
    description = "The kind of JS module generated by the compiler. ES modules are enabled by default in case of ES2015 target usage",
  )
  public var moduleKind: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = JS_MAIN,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-main",
    valueDescription = "{call|noCall}",
    description = "Specify whether the 'main' function should be called upon execution.",
  )
  public var main: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-produce-klib-dir",
    description = "Generate an unpacked klib into the parent directory of the output JS file.",
  )
  public var irProduceKlibDir: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-produce-klib-file",
    description = "Generate a packed klib into the directory specified by '-ir-output-dir'.",
  )
  public var irProduceKlibFile: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-produce-js",
    description = "Generate a JS file using the IR backend.",
  )
  public var irProduceJs: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-dce",
    description = "Perform experimental dead code elimination.",
  )
  public var irDce: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-dce-runtime-diagnostic",
    valueDescription = "{log|exception}",
    description = "Enable runtime diagnostics instead of removing declarations when performing DCE.",
  )
  public var irDceRuntimeDiagnostic: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-dce-print-reachability-info",
    description = "Print reachability information about declarations to 'stdout' while performing DCE.",
  )
  public var irDcePrintReachabilityInfo: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-property-lazy-initialization",
    description = "Perform lazy initialization for properties.",
  )
  public var irPropertyLazyInitialization: Boolean = true
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-minimized-member-names",
    description = "Minimize the names of members.",
  )
  public var irMinimizedMemberNames: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-module-name",
    valueDescription = "<name>",
    description = "Specify the name of the compilation module for the IR backend.",
  )
  public var irModuleName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-safe-external-boolean",
    description = "Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.",
  )
  public var irSafeExternalBoolean: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-safe-external-boolean-diagnostic",
    valueDescription = "{log|exception}",
    description = "Enable runtime diagnostics when accessing external 'Boolean' properties.",
  )
  public var irSafeExternalBooleanDiagnostic: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-per-module",
    description = "Generate one .js file per module.",
  )
  public var irPerModule: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-per-module-output-name",
    description = "Add a custom output name to the split .js files.",
  )
  public var irPerModuleOutputName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-per-file",
    description = "Generate one .js file per source file.",
  )
  public var irPerFile: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-generate-inline-anonymous-functions",
    description = "Lambda expressions that capture values are translated into in-line anonymous JavaScript functions.",
  )
  public var irGenerateInlineAnonymousFunctions: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xinclude",
    valueDescription = "<path>",
    description = "Path to an intermediate library that should be processed in the same manner as source files.",
  )
  public var includes: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xcache-directory",
    valueDescription = "<path>",
    description = "Path to the cache directory.",
  )
  public var cacheDirectory: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-build-cache",
    description = "Use the compiler to build the cache.",
  )
  public var irBuildCache: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xgenerate-dts",
    description = "Generate a TypeScript declaration .d.ts file alongside the JS file.",
  )
  public var generateDts: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xgenerate-polyfills",
    description = "Generate polyfills for features from the ES6+ standards.",
  )
  public var generatePolyfills: Boolean = true
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xstrict-implicit-export-types",
    description = "Generate strict types for implicitly exported entities inside d.ts files.",
  )
  public var strictImplicitExportType: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = BOOLEAN_NULL_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-Xes-classes",
    description = "Let generated JavaScript code use ES2015 classes. Enabled by default in case of ES2015 target usage",
  )
  public var useEsClasses: Boolean? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xplatform-arguments-in-main-function",
    description = "JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function",
  )
  public var platformArgumentsProviderJsExpression: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xes-generators",
    description = "Enable ES2015 generator functions usage inside the compiled code. Enabled by default in case of ES2015 target usage",
  )
  public var useEsGenerators: Boolean? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xes-arrow-functions",
    description = "Use ES2015 arrow functions in the JavaScript code generated for Kotlin lambdas. Enabled by default in case of ES2015 target usage",
  )
  public var useEsArrowFunctions: Boolean? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Deprecated("It is senseless to use with IR compiler. Only for compatibility.")
  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @GradleDeprecatedOption(
    message = "Only for legacy backend.",
    removeAfter = KOTLIN_2_2,
    level = ERROR,
  )
  @Argument(
    value = "-Xtyped-arrays",
    description = "This option does nothing and is left for compatibility with the legacy backend.\nIt is deprecated and will be removed in a future release.",
  )
  public var typedArrays: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-Xfriend-modules-disabled",
    description = "Disable internal declaration export.",
  )
  public var friendModulesDisabled: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xfriend-modules",
    valueDescription = "<path>",
    description = "Paths to friend modules.",
  )
  public var friendModules: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xenable-extension-functions-in-externals",
    description = "Enable extension function members in external interfaces.",
  )
  public var extensionFunctionsInExternals: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xfake-override-validator",
    description = "Enable the IR fake override validator.",
  )
  public var fakeOverrideValidator: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xoptimize-generated-js",
    description = "Perform additional optimizations on the generated JS code.",
  )
  public var optimizeGeneratedJs: Boolean = true
    set(`value`) {
      checkFrozen()
      field = value
    }

  @get:AnnotationsTransient
  @field:JvmTransient
  override val configurator: CommonCompilerArgumentsConfigurator =
      K2JSCompilerArgumentsConfigurator()

  override fun copyOf(): Freezable = copyK2JSCompilerArguments(this, K2JSCompilerArguments())
}
