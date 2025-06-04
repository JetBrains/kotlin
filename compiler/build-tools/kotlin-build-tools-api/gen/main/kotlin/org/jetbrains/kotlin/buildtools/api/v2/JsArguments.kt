package org.jetbrains.kotlin.buildtools.api.v2

import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments

public class JsArguments : WasmArguments() {
  private val optionsMap: MutableMap<JsArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JsArgument<V>): V = optionsMap[key] as V

  public operator fun <V> `set`(key: JsArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  public fun toCompilerArguments(): K2JSCompilerArguments {
    val arguments = K2JSCompilerArguments()
    if (OUTPUT in optionsMap) { arguments.outputFile = get(OUTPUT) }
    if (IR_OUTPUT_DIR in optionsMap) { arguments.outputDir = get(IR_OUTPUT_DIR) }
    if (IR_OUTPUT_NAME in optionsMap) { arguments.moduleName = get(IR_OUTPUT_NAME) }
    if (LIBRARIES in optionsMap) { arguments.libraries = get(LIBRARIES) }
    if (SOURCE_MAP in optionsMap) { arguments.sourceMap = get(SOURCE_MAP) }
    if (SOURCE_MAP_PREFIX in optionsMap) { arguments.sourceMapPrefix = get(SOURCE_MAP_PREFIX) }
    if (SOURCE_MAP_BASE_DIRS in optionsMap) { arguments.sourceMapBaseDirs = get(SOURCE_MAP_BASE_DIRS) }
    if (SOURCE_MAP_EMBED_SOURCES in optionsMap) { arguments.sourceMapEmbedSources = get(SOURCE_MAP_EMBED_SOURCES) }
    if (SOURCE_MAP_NAMES_POLICY in optionsMap) { arguments.sourceMapNamesPolicy = get(SOURCE_MAP_NAMES_POLICY) }
    if (TARGET in optionsMap) { arguments.target = get(TARGET) }
    if (XIR_KEEP in optionsMap) { arguments.irKeep = get(XIR_KEEP) }
    if (MODULE_KIND in optionsMap) { arguments.moduleKind = get(MODULE_KIND) }
    if (MAIN in optionsMap) { arguments.main = get(MAIN) }
    if (XIR_PRODUCE_KLIB_DIR in optionsMap) { arguments.irProduceKlibDir = get(XIR_PRODUCE_KLIB_DIR) }
    if (XIR_PRODUCE_KLIB_FILE in optionsMap) { arguments.irProduceKlibFile = get(XIR_PRODUCE_KLIB_FILE) }
    if (XIR_PRODUCE_JS in optionsMap) { arguments.irProduceJs = get(XIR_PRODUCE_JS) }
    if (XIR_DCE in optionsMap) { arguments.irDce = get(XIR_DCE) }
    if (XIR_DCE_RUNTIME_DIAGNOSTIC in optionsMap) { arguments.irDceRuntimeDiagnostic = get(XIR_DCE_RUNTIME_DIAGNOSTIC) }
    if (XIR_DCE_PRINT_REACHABILITY_INFO in optionsMap) { arguments.irDcePrintReachabilityInfo = get(XIR_DCE_PRINT_REACHABILITY_INFO) }
    if (XIR_PROPERTY_LAZY_INITIALIZATION in optionsMap) { arguments.irPropertyLazyInitialization = get(XIR_PROPERTY_LAZY_INITIALIZATION) }
    if (XIR_MINIMIZED_MEMBER_NAMES in optionsMap) { arguments.irMinimizedMemberNames = get(XIR_MINIMIZED_MEMBER_NAMES) }
    if (XIR_MODULE_NAME in optionsMap) { arguments.irModuleName = get(XIR_MODULE_NAME) }
    if (XIR_SAFE_EXTERNAL_BOOLEAN in optionsMap) { arguments.irSafeExternalBoolean = get(XIR_SAFE_EXTERNAL_BOOLEAN) }
    if (XIR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC in optionsMap) { arguments.irSafeExternalBooleanDiagnostic = get(XIR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC) }
    if (XIR_PER_MODULE in optionsMap) { arguments.irPerModule = get(XIR_PER_MODULE) }
    if (XIR_PER_MODULE_OUTPUT_NAME in optionsMap) { arguments.irPerModuleOutputName = get(XIR_PER_MODULE_OUTPUT_NAME) }
    if (XIR_PER_FILE in optionsMap) { arguments.irPerFile = get(XIR_PER_FILE) }
    if (XIR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS in optionsMap) { arguments.irGenerateInlineAnonymousFunctions = get(XIR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS) }
    if (XINCLUDE in optionsMap) { arguments.includes = get(XINCLUDE) }
    if (XCACHE_DIRECTORY in optionsMap) { arguments.cacheDirectory = get(XCACHE_DIRECTORY) }
    if (XIR_BUILD_CACHE in optionsMap) { arguments.irBuildCache = get(XIR_BUILD_CACHE) }
    if (XGENERATE_DTS in optionsMap) { arguments.generateDts = get(XGENERATE_DTS) }
    if (XGENERATE_POLYFILLS in optionsMap) { arguments.generatePolyfills = get(XGENERATE_POLYFILLS) }
    if (XSTRICT_IMPLICIT_EXPORT_TYPES in optionsMap) { arguments.strictImplicitExportType = get(XSTRICT_IMPLICIT_EXPORT_TYPES) }
    if (XES_CLASSES in optionsMap) { arguments.useEsClasses = get(XES_CLASSES) }
    if (XPLATFORM_ARGUMENTS_IN_MAIN_FUNCTION in optionsMap) { arguments.platformArgumentsProviderJsExpression = get(XPLATFORM_ARGUMENTS_IN_MAIN_FUNCTION) }
    if (XES_GENERATORS in optionsMap) { arguments.useEsGenerators = get(XES_GENERATORS) }
    if (XES_ARROW_FUNCTIONS in optionsMap) { arguments.useEsArrowFunctions = get(XES_ARROW_FUNCTIONS) }
    if (XTYPED_ARRAYS in optionsMap) { arguments.typedArrays = get(XTYPED_ARRAYS) }
    if (XFRIEND_MODULES_DISABLED in optionsMap) { arguments.friendModulesDisabled = get(XFRIEND_MODULES_DISABLED) }
    if (XFRIEND_MODULES in optionsMap) { arguments.friendModules = get(XFRIEND_MODULES) }
    if (XENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS in optionsMap) { arguments.extensionFunctionsInExternals = get(XENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS) }
    if (XFAKE_OVERRIDE_VALIDATOR in optionsMap) { arguments.fakeOverrideValidator = get(XFAKE_OVERRIDE_VALIDATOR) }
    if (XOPTIMIZE_GENERATED_JS in optionsMap) { arguments.optimizeGeneratedJs = get(XOPTIMIZE_GENERATED_JS) }
    return arguments
  }

  public class JsArgument<V>(
    public val id: String,
  )

  public companion object {
    @JvmField
    public val OUTPUT: JsArgument<String?> = JsArgument("OUTPUT")

    /**
     * Destination for generated files.
     */
    @JvmField
    public val IR_OUTPUT_DIR: JsArgument<String?> = JsArgument("IR_OUTPUT_DIR")

    /**
     * Base name of generated files.
     */
    @JvmField
    public val IR_OUTPUT_NAME: JsArgument<String?> = JsArgument("IR_OUTPUT_NAME")

    /**
     * Paths to Kotlin libraries with .meta.js and .kjsm files, separated by the system path separator.
     */
    @JvmField
    public val LIBRARIES: JsArgument<String?> = JsArgument("LIBRARIES")

    /**
     * Generate a source map.
     */
    @JvmField
    public val SOURCE_MAP: JsArgument<Boolean> = JsArgument("SOURCE_MAP")

    /**
     * Add the specified prefix to the paths in the source map.
     */
    @JvmField
    public val SOURCE_MAP_PREFIX: JsArgument<String?> = JsArgument("SOURCE_MAP_PREFIX")

    /**
     * Base directories for calculating relative paths to source files in the source map.
     */
    @JvmField
    public val SOURCE_MAP_BASE_DIRS: JsArgument<String?> = JsArgument("SOURCE_MAP_BASE_DIRS")

    /**
     * Embed source files into the source map.
     */
    @JvmField
    public val SOURCE_MAP_EMBED_SOURCES: JsArgument<String?> =
        JsArgument("SOURCE_MAP_EMBED_SOURCES")

    /**
     * Mode for mapping generated names to original names.
     */
    @JvmField
    public val SOURCE_MAP_NAMES_POLICY: JsArgument<String?> = JsArgument("SOURCE_MAP_NAMES_POLICY")

    /**
     * Generate JS files for the specified ECMA version.
     */
    @JvmField
    public val TARGET: JsArgument<String?> = JsArgument("TARGET")

    /**
     * Comma-separated list of fully qualified names not to be eliminated by DCE (if it can be reached), and for which to keep non-minified names.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_KEEP: JsArgument<String?> = JsArgument("XIR_KEEP")

    /**
     * The kind of JS module generated by the compiler. ES modules are enabled by default in case of ES2015 target usage
     */
    @JvmField
    public val MODULE_KIND: JsArgument<String?> = JsArgument("MODULE_KIND")

    /**
     * Specify whether the 'main' function should be called upon execution.
     */
    @JvmField
    public val MAIN: JsArgument<String?> = JsArgument("MAIN")

    /**
     * Generate an unpacked klib into the parent directory of the output JS file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PRODUCE_KLIB_DIR: JsArgument<Boolean> = JsArgument("XIR_PRODUCE_KLIB_DIR")

    /**
     * Generate a packed klib into the directory specified by '-ir-output-dir'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PRODUCE_KLIB_FILE: JsArgument<Boolean> = JsArgument("XIR_PRODUCE_KLIB_FILE")

    /**
     * Generate a JS file using the IR backend.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PRODUCE_JS: JsArgument<Boolean> = JsArgument("XIR_PRODUCE_JS")

    /**
     * Perform experimental dead code elimination.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_DCE: JsArgument<Boolean> = JsArgument("XIR_DCE")

    /**
     * Enable runtime diagnostics instead of removing declarations when performing DCE.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_DCE_RUNTIME_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("XIR_DCE_RUNTIME_DIAGNOSTIC")

    /**
     * Print reachability information about declarations to 'stdout' while performing DCE.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_DCE_PRINT_REACHABILITY_INFO: JsArgument<Boolean> =
        JsArgument("XIR_DCE_PRINT_REACHABILITY_INFO")

    /**
     * Perform lazy initialization for properties.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PROPERTY_LAZY_INITIALIZATION: JsArgument<Boolean> =
        JsArgument("XIR_PROPERTY_LAZY_INITIALIZATION")

    /**
     * Minimize the names of members.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_MINIMIZED_MEMBER_NAMES: JsArgument<Boolean> =
        JsArgument("XIR_MINIMIZED_MEMBER_NAMES")

    /**
     * Specify the name of the compilation module for the IR backend.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_MODULE_NAME: JsArgument<String?> = JsArgument("XIR_MODULE_NAME")

    /**
     * Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_SAFE_EXTERNAL_BOOLEAN: JsArgument<Boolean> =
        JsArgument("XIR_SAFE_EXTERNAL_BOOLEAN")

    /**
     * Enable runtime diagnostics when accessing external 'Boolean' properties.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("XIR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC")

    /**
     * Generate one .js file per module.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PER_MODULE: JsArgument<Boolean> = JsArgument("XIR_PER_MODULE")

    /**
     * Add a custom output name to the split .js files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PER_MODULE_OUTPUT_NAME: JsArgument<String?> =
        JsArgument("XIR_PER_MODULE_OUTPUT_NAME")

    /**
     * Generate one .js file per source file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_PER_FILE: JsArgument<Boolean> = JsArgument("XIR_PER_FILE")

    /**
     * Lambda expressions that capture values are translated into in-line anonymous JavaScript functions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS: JsArgument<Boolean> =
        JsArgument("XIR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS")

    /**
     * Path to an intermediate library that should be processed in the same manner as source files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XINCLUDE: JsArgument<String?> = JsArgument("XINCLUDE")

    /**
     * Path to the cache directory.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCACHE_DIRECTORY: JsArgument<String?> = JsArgument("XCACHE_DIRECTORY")

    /**
     * Use the compiler to build the cache.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_BUILD_CACHE: JsArgument<Boolean> = JsArgument("XIR_BUILD_CACHE")

    /**
     * Generate a TypeScript declaration .d.ts file alongside the JS file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XGENERATE_DTS: JsArgument<Boolean> = JsArgument("XGENERATE_DTS")

    /**
     * Generate polyfills for features from the ES6+ standards.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XGENERATE_POLYFILLS: JsArgument<Boolean> = JsArgument("XGENERATE_POLYFILLS")

    /**
     * Generate strict types for implicitly exported entities inside d.ts files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSTRICT_IMPLICIT_EXPORT_TYPES: JsArgument<Boolean> =
        JsArgument("XSTRICT_IMPLICIT_EXPORT_TYPES")

    /**
     * Let generated JavaScript code use ES2015 classes. Enabled by default in case of ES2015 target usage
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XES_CLASSES: JsArgument<Boolean?> = JsArgument("XES_CLASSES")

    /**
     * JS expression that will be executed in runtime and be put as an Array<String> parameter of the main function
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPLATFORM_ARGUMENTS_IN_MAIN_FUNCTION: JsArgument<String?> =
        JsArgument("XPLATFORM_ARGUMENTS_IN_MAIN_FUNCTION")

    /**
     * Enable ES2015 generator functions usage inside the compiled code. Enabled by default in case of ES2015 target usage
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XES_GENERATORS: JsArgument<Boolean?> = JsArgument("XES_GENERATORS")

    /**
     * Use ES2015 arrow functions in the JavaScript code generated for Kotlin lambdas. Enabled by default in case of ES2015 target usage
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XES_ARROW_FUNCTIONS: JsArgument<Boolean?> = JsArgument("XES_ARROW_FUNCTIONS")

    /**
     * This option does nothing and is left for compatibility with the legacy backend.
     * It is deprecated and will be removed in a future release.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XTYPED_ARRAYS: JsArgument<Boolean> = JsArgument("XTYPED_ARRAYS")

    /**
     * Disable internal declaration export.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRIEND_MODULES_DISABLED: JsArgument<Boolean> =
        JsArgument("XFRIEND_MODULES_DISABLED")

    /**
     * Paths to friend modules.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRIEND_MODULES: JsArgument<String?> = JsArgument("XFRIEND_MODULES")

    /**
     * Enable extension function members in external interfaces.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS: JsArgument<Boolean> =
        JsArgument("XENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS")

    /**
     * Enable the IR fake override validator.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFAKE_OVERRIDE_VALIDATOR: JsArgument<Boolean> =
        JsArgument("XFAKE_OVERRIDE_VALIDATOR")

    /**
     * Perform additional optimizations on the generated JS code.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XOPTIMIZE_GENERATED_JS: JsArgument<Boolean> = JsArgument("XOPTIMIZE_GENERATED_JS")
  }
}
