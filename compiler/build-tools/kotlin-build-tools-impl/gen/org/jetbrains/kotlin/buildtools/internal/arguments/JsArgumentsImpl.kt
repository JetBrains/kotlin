@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.IR_OUTPUT_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.MAIN
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.MODULE_KIND
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.SOURCE_MAP
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.SOURCE_MAP_BASE_DIRS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.SOURCE_MAP_NAMES_POLICY
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.SOURCE_MAP_PREFIX
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_CACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_ES_ARROW_FUNCTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_ES_CLASSES
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_ES_GENERATORS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_ES_LONG_AS_BIGINT
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_FAKE_OVERRIDE_VALIDATOR
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_FRIEND_MODULES_DISABLED
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_GENERATE_DTS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_GENERATE_POLYFILLS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_INCLUDE
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_BUILD_CACHE
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_DCE
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_DCE_PRINT_REACHABILITY_INFO
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_DCE_RUNTIME_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_KEEP
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_MINIMIZED_MEMBER_NAMES
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_PER_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_PER_MODULE
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_PRODUCE_JS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_PRODUCE_KLIB_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_PRODUCE_KLIB_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_SAFE_EXTERNAL_BOOLEAN
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_OPTIMIZE_GENERATED_JS
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_STRICT_IMPLICIT_EXPORT_TYPES
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments.Companion.X_TYPED_ARRAYS
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments

public class JsArgumentsImpl : WasmArgumentsImpl(), JsArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: JsArguments.JsArgument<V>): V = optionsMap[key.id] as V

  override operator fun <V> `set`(key: JsArguments.JsArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2JSCompilerArguments = K2JSCompilerArguments()): K2JSCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("IR_OUTPUT_DIR" in optionsMap) { arguments.outputDir = get(IR_OUTPUT_DIR) }
    if ("IR_OUTPUT_NAME" in optionsMap) { arguments.moduleName = get(IR_OUTPUT_NAME) }
    if ("LIBRARIES" in optionsMap) { arguments.libraries = get(LIBRARIES) }
    if ("SOURCE_MAP" in optionsMap) { arguments.sourceMap = get(SOURCE_MAP) }
    if ("SOURCE_MAP_PREFIX" in optionsMap) { arguments.sourceMapPrefix = get(SOURCE_MAP_PREFIX) }
    if ("SOURCE_MAP_BASE_DIRS" in optionsMap) { arguments.sourceMapBaseDirs = get(SOURCE_MAP_BASE_DIRS) }
    if ("SOURCE_MAP_EMBED_SOURCES" in optionsMap) { arguments.sourceMapEmbedSources = get(SOURCE_MAP_EMBED_SOURCES) }
    if ("SOURCE_MAP_NAMES_POLICY" in optionsMap) { arguments.sourceMapNamesPolicy = get(SOURCE_MAP_NAMES_POLICY) }
    if ("TARGET" in optionsMap) { arguments.target = get(TARGET) }
    if ("X_IR_KEEP" in optionsMap) { arguments.irKeep = get(X_IR_KEEP) }
    if ("MODULE_KIND" in optionsMap) { arguments.moduleKind = get(MODULE_KIND) }
    if ("MAIN" in optionsMap) { arguments.main = get(MAIN) }
    if ("X_IR_PRODUCE_KLIB_DIR" in optionsMap) { arguments.irProduceKlibDir = get(X_IR_PRODUCE_KLIB_DIR) }
    if ("X_IR_PRODUCE_KLIB_FILE" in optionsMap) { arguments.irProduceKlibFile = get(X_IR_PRODUCE_KLIB_FILE) }
    if ("X_IR_PRODUCE_JS" in optionsMap) { arguments.irProduceJs = get(X_IR_PRODUCE_JS) }
    if ("X_IR_DCE" in optionsMap) { arguments.irDce = get(X_IR_DCE) }
    if ("X_IR_DCE_RUNTIME_DIAGNOSTIC" in optionsMap) { arguments.irDceRuntimeDiagnostic = get(X_IR_DCE_RUNTIME_DIAGNOSTIC) }
    if ("X_IR_DCE_PRINT_REACHABILITY_INFO" in optionsMap) { arguments.irDcePrintReachabilityInfo = get(X_IR_DCE_PRINT_REACHABILITY_INFO) }
    if ("X_IR_PROPERTY_LAZY_INITIALIZATION" in optionsMap) { arguments.irPropertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION) }
    if ("X_IR_MINIMIZED_MEMBER_NAMES" in optionsMap) { arguments.irMinimizedMemberNames = get(X_IR_MINIMIZED_MEMBER_NAMES) }
    if ("X_IR_MODULE_NAME" in optionsMap) { arguments.irModuleName = get(X_IR_MODULE_NAME) }
    if ("X_IR_SAFE_EXTERNAL_BOOLEAN" in optionsMap) { arguments.irSafeExternalBoolean = get(X_IR_SAFE_EXTERNAL_BOOLEAN) }
    if ("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC" in optionsMap) { arguments.irSafeExternalBooleanDiagnostic = get(X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC) }
    if ("X_IR_PER_MODULE" in optionsMap) { arguments.irPerModule = get(X_IR_PER_MODULE) }
    if ("X_IR_PER_MODULE_OUTPUT_NAME" in optionsMap) { arguments.irPerModuleOutputName = get(X_IR_PER_MODULE_OUTPUT_NAME) }
    if ("X_IR_PER_FILE" in optionsMap) { arguments.irPerFile = get(X_IR_PER_FILE) }
    if ("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS" in optionsMap) { arguments.irGenerateInlineAnonymousFunctions = get(X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS) }
    if ("X_INCLUDE" in optionsMap) { arguments.includes = get(X_INCLUDE) }
    if ("X_CACHE_DIRECTORY" in optionsMap) { arguments.cacheDirectory = get(X_CACHE_DIRECTORY) }
    if ("X_IR_BUILD_CACHE" in optionsMap) { arguments.irBuildCache = get(X_IR_BUILD_CACHE) }
    if ("X_GENERATE_DTS" in optionsMap) { arguments.generateDts = get(X_GENERATE_DTS) }
    if ("X_GENERATE_POLYFILLS" in optionsMap) { arguments.generatePolyfills = get(X_GENERATE_POLYFILLS) }
    if ("X_STRICT_IMPLICIT_EXPORT_TYPES" in optionsMap) { arguments.strictImplicitExportType = get(X_STRICT_IMPLICIT_EXPORT_TYPES) }
    if ("X_ES_CLASSES" in optionsMap) { arguments.useEsClasses = get(X_ES_CLASSES) }
    if ("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION" in optionsMap) { arguments.platformArgumentsProviderJsExpression = get(X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION) }
    if ("X_ES_GENERATORS" in optionsMap) { arguments.useEsGenerators = get(X_ES_GENERATORS) }
    if ("X_ES_ARROW_FUNCTIONS" in optionsMap) { arguments.useEsArrowFunctions = get(X_ES_ARROW_FUNCTIONS) }
    if ("X_ES_LONG_AS_BIGINT" in optionsMap) { arguments.compileLongAsBigInt = get(X_ES_LONG_AS_BIGINT) }
    if ("X_TYPED_ARRAYS" in optionsMap) { arguments.typedArrays = get(X_TYPED_ARRAYS) }
    if ("X_FRIEND_MODULES_DISABLED" in optionsMap) { arguments.friendModulesDisabled = get(X_FRIEND_MODULES_DISABLED) }
    if ("X_FRIEND_MODULES" in optionsMap) { arguments.friendModules = get(X_FRIEND_MODULES) }
    if ("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS" in optionsMap) { arguments.extensionFunctionsInExternals = get(X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS) }
    if ("X_FAKE_OVERRIDE_VALIDATOR" in optionsMap) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR) }
    if ("X_OPTIMIZE_GENERATED_JS" in optionsMap) { arguments.optimizeGeneratedJs = get(X_OPTIMIZE_GENERATED_JS) }
    return arguments
  }
}
