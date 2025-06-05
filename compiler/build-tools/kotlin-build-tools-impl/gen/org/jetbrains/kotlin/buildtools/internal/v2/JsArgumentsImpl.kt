package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.IR_OUTPUT_DIR
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.MAIN
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.MODULE_KIND
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.OUTPUT
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.SOURCE_MAP
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.SOURCE_MAP_BASE_DIRS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.SOURCE_MAP_NAMES_POLICY
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.SOURCE_MAP_PREFIX
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XCACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XES_ARROW_FUNCTIONS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XES_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XES_GENERATORS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XFAKE_OVERRIDE_VALIDATOR
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XFRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XFRIEND_MODULES_DISABLED
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XGENERATE_DTS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XGENERATE_POLYFILLS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XINCLUDE
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_BUILD_CACHE
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_DCE
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_DCE_PRINT_REACHABILITY_INFO
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_DCE_RUNTIME_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_KEEP
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_MINIMIZED_MEMBER_NAMES
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_PER_FILE
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_PER_MODULE
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_PRODUCE_JS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_PRODUCE_KLIB_DIR
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_PRODUCE_KLIB_FILE
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_SAFE_EXTERNAL_BOOLEAN
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XIR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XOPTIMIZE_GENERATED_JS
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XPLATFORM_ARGUMENTS_IN_MAIN_FUNCTION
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XSTRICT_IMPLICIT_EXPORT_TYPES
import org.jetbrains.kotlin.buildtools.api.v2.JsArguments.Companion.XTYPED_ARRAYS
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments

public class JsArgumentsImpl : WasmArgumentsImpl(), JsArguments {
  private val optionsMap: MutableMap<JsArguments.JsArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: JsArguments.JsArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: JsArguments.JsArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2JSCompilerArguments = K2JSCompilerArguments()): K2JSCompilerArguments {
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
}
