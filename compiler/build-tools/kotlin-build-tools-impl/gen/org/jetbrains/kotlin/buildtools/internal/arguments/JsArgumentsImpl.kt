// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.IR_OUTPUT_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.MAIN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.MODULE_KIND
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.SOURCE_MAP
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.SOURCE_MAP_BASE_DIRS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.SOURCE_MAP_NAMES_POLICY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.SOURCE_MAP_PREFIX
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.TARGET
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_CACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_ARROW_FUNCTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_CLASSES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_GENERATORS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_LONG_AS_BIGINT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_FAKE_OVERRIDE_VALIDATOR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_FRIEND_MODULES_DISABLED
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_GENERATE_DTS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_GENERATE_POLYFILLS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_INCLUDE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_BUILD_CACHE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_DCE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_DCE_PRINT_REACHABILITY_INFO
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_DCE_RUNTIME_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_KEEP
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_MINIMIZED_MEMBER_NAMES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_MODULE_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PER_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PER_MODULE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PRODUCE_JS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PRODUCE_KLIB_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PRODUCE_KLIB_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_SAFE_EXTERNAL_BOOLEAN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_OPTIMIZE_GENERATED_JS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_STRICT_IMPLICIT_EXPORT_TYPES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_TYPED_ARRAYS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings

internal class JsArgumentsImpl : WasmArgumentsImpl(), JsArguments {
  private val internalArguments: MutableSet<String> = mutableSetOf()

  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsArguments.JsArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsArguments.JsArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: JsArguments.JsArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JsArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: JsArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: JsArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2JSCompilerArguments = K2JSCompilerArguments()): K2JSCompilerArguments {
    super.toCompilerArguments(arguments)
    try { if ("IR_OUTPUT_DIR" in optionsMap) { arguments.outputDir = get(IR_OUTPUT_DIR) } } catch (_: NoSuchMethodError) {}
    try { if ("IR_OUTPUT_NAME" in optionsMap) { arguments.moduleName = get(IR_OUTPUT_NAME) } } catch (_: NoSuchMethodError) {}
    try { if ("LIBRARIES" in optionsMap) { arguments.libraries = get(LIBRARIES) } } catch (_: NoSuchMethodError) {}
    try { if ("SOURCE_MAP" in optionsMap) { arguments.sourceMap = get(SOURCE_MAP) } } catch (_: NoSuchMethodError) {}
    try { if ("SOURCE_MAP_PREFIX" in optionsMap) { arguments.sourceMapPrefix = get(SOURCE_MAP_PREFIX) } } catch (_: NoSuchMethodError) {}
    try { if ("SOURCE_MAP_BASE_DIRS" in optionsMap) { arguments.sourceMapBaseDirs = get(SOURCE_MAP_BASE_DIRS) } } catch (_: NoSuchMethodError) {}
    try { if ("SOURCE_MAP_EMBED_SOURCES" in optionsMap) { arguments.sourceMapEmbedSources = get(SOURCE_MAP_EMBED_SOURCES) } } catch (_: NoSuchMethodError) {}
    try { if ("SOURCE_MAP_NAMES_POLICY" in optionsMap) { arguments.sourceMapNamesPolicy = get(SOURCE_MAP_NAMES_POLICY) } } catch (_: NoSuchMethodError) {}
    try { if ("TARGET" in optionsMap) { arguments.target = get(TARGET) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_KEEP" in optionsMap) { arguments.irKeep = get(X_IR_KEEP) } } catch (_: NoSuchMethodError) {}
    try { if ("MODULE_KIND" in optionsMap) { arguments.moduleKind = get(MODULE_KIND) } } catch (_: NoSuchMethodError) {}
    try { if ("MAIN" in optionsMap) { arguments.main = get(MAIN) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PRODUCE_KLIB_DIR" in optionsMap) { arguments.irProduceKlibDir = get(X_IR_PRODUCE_KLIB_DIR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PRODUCE_KLIB_FILE" in optionsMap) { arguments.irProduceKlibFile = get(X_IR_PRODUCE_KLIB_FILE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PRODUCE_JS" in optionsMap) { arguments.irProduceJs = get(X_IR_PRODUCE_JS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_DCE" in optionsMap) { arguments.irDce = get(X_IR_DCE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_DCE_RUNTIME_DIAGNOSTIC" in optionsMap) { arguments.irDceRuntimeDiagnostic = get(X_IR_DCE_RUNTIME_DIAGNOSTIC) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_DCE_PRINT_REACHABILITY_INFO" in optionsMap) { arguments.irDcePrintReachabilityInfo = get(X_IR_DCE_PRINT_REACHABILITY_INFO) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PROPERTY_LAZY_INITIALIZATION" in optionsMap) { arguments.irPropertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_MINIMIZED_MEMBER_NAMES" in optionsMap) { arguments.irMinimizedMemberNames = get(X_IR_MINIMIZED_MEMBER_NAMES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_MODULE_NAME" in optionsMap) { arguments.irModuleName = get(X_IR_MODULE_NAME) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_SAFE_EXTERNAL_BOOLEAN" in optionsMap) { arguments.irSafeExternalBoolean = get(X_IR_SAFE_EXTERNAL_BOOLEAN) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC" in optionsMap) { arguments.irSafeExternalBooleanDiagnostic = get(X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PER_MODULE" in optionsMap) { arguments.irPerModule = get(X_IR_PER_MODULE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PER_MODULE_OUTPUT_NAME" in optionsMap) { arguments.irPerModuleOutputName = get(X_IR_PER_MODULE_OUTPUT_NAME) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_PER_FILE" in optionsMap) { arguments.irPerFile = get(X_IR_PER_FILE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS" in optionsMap) { arguments.irGenerateInlineAnonymousFunctions = get(X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_INCLUDE" in optionsMap) { arguments.includes = get(X_INCLUDE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_CACHE_DIRECTORY" in optionsMap) { arguments.cacheDirectory = get(X_CACHE_DIRECTORY) } } catch (_: NoSuchMethodError) {}
    try { if ("X_IR_BUILD_CACHE" in optionsMap) { arguments.irBuildCache = get(X_IR_BUILD_CACHE) } } catch (_: NoSuchMethodError) {}
    try { if ("X_GENERATE_DTS" in optionsMap) { arguments.generateDts = get(X_GENERATE_DTS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_GENERATE_POLYFILLS" in optionsMap) { arguments.generatePolyfills = get(X_GENERATE_POLYFILLS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_STRICT_IMPLICIT_EXPORT_TYPES" in optionsMap) { arguments.strictImplicitExportType = get(X_STRICT_IMPLICIT_EXPORT_TYPES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ES_CLASSES" in optionsMap) { arguments.useEsClasses = get(X_ES_CLASSES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION" in optionsMap) { arguments.platformArgumentsProviderJsExpression = get(X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ES_GENERATORS" in optionsMap) { arguments.useEsGenerators = get(X_ES_GENERATORS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ES_ARROW_FUNCTIONS" in optionsMap) { arguments.useEsArrowFunctions = get(X_ES_ARROW_FUNCTIONS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ES_LONG_AS_BIGINT" in optionsMap) { arguments.compileLongAsBigInt = get(X_ES_LONG_AS_BIGINT) } } catch (_: NoSuchMethodError) {}
    try { if ("X_TYPED_ARRAYS" in optionsMap) { arguments.typedArrays = get(X_TYPED_ARRAYS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_FRIEND_MODULES_DISABLED" in optionsMap) { arguments.friendModulesDisabled = get(X_FRIEND_MODULES_DISABLED) } } catch (_: NoSuchMethodError) {}
    try { if ("X_FRIEND_MODULES" in optionsMap) { arguments.friendModules = get(X_FRIEND_MODULES) } } catch (_: NoSuchMethodError) {}
    try { if ("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS" in optionsMap) { arguments.extensionFunctionsInExternals = get(X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS) } } catch (_: NoSuchMethodError) {}
    try { if ("X_FAKE_OVERRIDE_VALIDATOR" in optionsMap) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR) } } catch (_: NoSuchMethodError) {}
    try { if ("X_OPTIMIZE_GENERATED_JS" in optionsMap) { arguments.optimizeGeneratedJs = get(X_OPTIMIZE_GENERATED_JS) } } catch (_: NoSuchMethodError) {}
    arguments.internalArguments = parseCommandLineArguments<K2JSCompilerArguments>(internalArguments.toList()).internalArguments
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2JSCompilerArguments = parseCommandLineArguments(arguments)
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings()
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: K2JSCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[IR_OUTPUT_DIR] = arguments.outputDir } catch (_: NoSuchMethodError) {}
    try { this[IR_OUTPUT_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {}
    try { this[LIBRARIES] = arguments.libraries } catch (_: NoSuchMethodError) {}
    try { this[SOURCE_MAP] = arguments.sourceMap } catch (_: NoSuchMethodError) {}
    try { this[SOURCE_MAP_PREFIX] = arguments.sourceMapPrefix } catch (_: NoSuchMethodError) {}
    try { this[SOURCE_MAP_BASE_DIRS] = arguments.sourceMapBaseDirs } catch (_: NoSuchMethodError) {}
    try { this[SOURCE_MAP_EMBED_SOURCES] = arguments.sourceMapEmbedSources } catch (_: NoSuchMethodError) {}
    try { this[SOURCE_MAP_NAMES_POLICY] = arguments.sourceMapNamesPolicy } catch (_: NoSuchMethodError) {}
    try { this[TARGET] = arguments.target } catch (_: NoSuchMethodError) {}
    try { this[X_IR_KEEP] = arguments.irKeep } catch (_: NoSuchMethodError) {}
    try { this[MODULE_KIND] = arguments.moduleKind } catch (_: NoSuchMethodError) {}
    try { this[MAIN] = arguments.main } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PRODUCE_KLIB_DIR] = arguments.irProduceKlibDir } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PRODUCE_KLIB_FILE] = arguments.irProduceKlibFile } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PRODUCE_JS] = arguments.irProduceJs } catch (_: NoSuchMethodError) {}
    try { this[X_IR_DCE] = arguments.irDce } catch (_: NoSuchMethodError) {}
    try { this[X_IR_DCE_RUNTIME_DIAGNOSTIC] = arguments.irDceRuntimeDiagnostic } catch (_: NoSuchMethodError) {}
    try { this[X_IR_DCE_PRINT_REACHABILITY_INFO] = arguments.irDcePrintReachabilityInfo } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PROPERTY_LAZY_INITIALIZATION] = arguments.irPropertyLazyInitialization } catch (_: NoSuchMethodError) {}
    try { this[X_IR_MINIMIZED_MEMBER_NAMES] = arguments.irMinimizedMemberNames } catch (_: NoSuchMethodError) {}
    try { this[X_IR_MODULE_NAME] = arguments.irModuleName } catch (_: NoSuchMethodError) {}
    try { this[X_IR_SAFE_EXTERNAL_BOOLEAN] = arguments.irSafeExternalBoolean } catch (_: NoSuchMethodError) {}
    try { this[X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC] = arguments.irSafeExternalBooleanDiagnostic } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PER_MODULE] = arguments.irPerModule } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PER_MODULE_OUTPUT_NAME] = arguments.irPerModuleOutputName } catch (_: NoSuchMethodError) {}
    try { this[X_IR_PER_FILE] = arguments.irPerFile } catch (_: NoSuchMethodError) {}
    try { this[X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS] = arguments.irGenerateInlineAnonymousFunctions } catch (_: NoSuchMethodError) {}
    try { this[X_INCLUDE] = arguments.includes } catch (_: NoSuchMethodError) {}
    try { this[X_CACHE_DIRECTORY] = arguments.cacheDirectory } catch (_: NoSuchMethodError) {}
    try { this[X_IR_BUILD_CACHE] = arguments.irBuildCache } catch (_: NoSuchMethodError) {}
    try { this[X_GENERATE_DTS] = arguments.generateDts } catch (_: NoSuchMethodError) {}
    try { this[X_GENERATE_POLYFILLS] = arguments.generatePolyfills } catch (_: NoSuchMethodError) {}
    try { this[X_STRICT_IMPLICIT_EXPORT_TYPES] = arguments.strictImplicitExportType } catch (_: NoSuchMethodError) {}
    try { this[X_ES_CLASSES] = arguments.useEsClasses } catch (_: NoSuchMethodError) {}
    try { this[X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION] = arguments.platformArgumentsProviderJsExpression } catch (_: NoSuchMethodError) {}
    try { this[X_ES_GENERATORS] = arguments.useEsGenerators } catch (_: NoSuchMethodError) {}
    try { this[X_ES_ARROW_FUNCTIONS] = arguments.useEsArrowFunctions } catch (_: NoSuchMethodError) {}
    try { this[X_ES_LONG_AS_BIGINT] = arguments.compileLongAsBigInt } catch (_: NoSuchMethodError) {}
    try { this[X_TYPED_ARRAYS] = arguments.typedArrays } catch (_: NoSuchMethodError) {}
    try { this[X_FRIEND_MODULES_DISABLED] = arguments.friendModulesDisabled } catch (_: NoSuchMethodError) {}
    try { this[X_FRIEND_MODULES] = arguments.friendModules } catch (_: NoSuchMethodError) {}
    try { this[X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS] = arguments.extensionFunctionsInExternals } catch (_: NoSuchMethodError) {}
    try { this[X_FAKE_OVERRIDE_VALIDATOR] = arguments.fakeOverrideValidator } catch (_: NoSuchMethodError) {}
    try { this[X_OPTIMIZE_GENERATED_JS] = arguments.optimizeGeneratedJs } catch (_: NoSuchMethodError) {}
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class JsArgument<V>(
    public val id: String,
  )

  public companion object {
    public val IR_OUTPUT_DIR: JsArgument<String?> = JsArgument("IR_OUTPUT_DIR")

    public val IR_OUTPUT_NAME: JsArgument<String?> = JsArgument("IR_OUTPUT_NAME")

    public val LIBRARIES: JsArgument<String?> = JsArgument("LIBRARIES")

    public val SOURCE_MAP: JsArgument<Boolean> = JsArgument("SOURCE_MAP")

    public val SOURCE_MAP_PREFIX: JsArgument<String?> = JsArgument("SOURCE_MAP_PREFIX")

    public val SOURCE_MAP_BASE_DIRS: JsArgument<String?> = JsArgument("SOURCE_MAP_BASE_DIRS")

    public val SOURCE_MAP_EMBED_SOURCES: JsArgument<String?> =
        JsArgument("SOURCE_MAP_EMBED_SOURCES")

    public val SOURCE_MAP_NAMES_POLICY: JsArgument<String?> = JsArgument("SOURCE_MAP_NAMES_POLICY")

    public val TARGET: JsArgument<String?> = JsArgument("TARGET")

    public val X_IR_KEEP: JsArgument<String?> = JsArgument("X_IR_KEEP")

    public val MODULE_KIND: JsArgument<String?> = JsArgument("MODULE_KIND")

    public val MAIN: JsArgument<String?> = JsArgument("MAIN")

    public val X_IR_PRODUCE_KLIB_DIR: JsArgument<Boolean> = JsArgument("X_IR_PRODUCE_KLIB_DIR")

    public val X_IR_PRODUCE_KLIB_FILE: JsArgument<Boolean> = JsArgument("X_IR_PRODUCE_KLIB_FILE")

    public val X_IR_PRODUCE_JS: JsArgument<Boolean> = JsArgument("X_IR_PRODUCE_JS")

    public val X_IR_DCE: JsArgument<Boolean> = JsArgument("X_IR_DCE")

    public val X_IR_DCE_RUNTIME_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("X_IR_DCE_RUNTIME_DIAGNOSTIC")

    public val X_IR_DCE_PRINT_REACHABILITY_INFO: JsArgument<Boolean> =
        JsArgument("X_IR_DCE_PRINT_REACHABILITY_INFO")

    public val X_IR_PROPERTY_LAZY_INITIALIZATION: JsArgument<Boolean> =
        JsArgument("X_IR_PROPERTY_LAZY_INITIALIZATION")

    public val X_IR_MINIMIZED_MEMBER_NAMES: JsArgument<Boolean> =
        JsArgument("X_IR_MINIMIZED_MEMBER_NAMES")

    public val X_IR_MODULE_NAME: JsArgument<String?> = JsArgument("X_IR_MODULE_NAME")

    public val X_IR_SAFE_EXTERNAL_BOOLEAN: JsArgument<Boolean> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN")

    public val X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC")

    public val X_IR_PER_MODULE: JsArgument<Boolean> = JsArgument("X_IR_PER_MODULE")

    public val X_IR_PER_MODULE_OUTPUT_NAME: JsArgument<String?> =
        JsArgument("X_IR_PER_MODULE_OUTPUT_NAME")

    public val X_IR_PER_FILE: JsArgument<Boolean> = JsArgument("X_IR_PER_FILE")

    public val X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS: JsArgument<Boolean> =
        JsArgument("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS")

    public val X_INCLUDE: JsArgument<String?> = JsArgument("X_INCLUDE")

    public val X_CACHE_DIRECTORY: JsArgument<String?> = JsArgument("X_CACHE_DIRECTORY")

    public val X_IR_BUILD_CACHE: JsArgument<Boolean> = JsArgument("X_IR_BUILD_CACHE")

    public val X_GENERATE_DTS: JsArgument<Boolean> = JsArgument("X_GENERATE_DTS")

    public val X_GENERATE_POLYFILLS: JsArgument<Boolean> = JsArgument("X_GENERATE_POLYFILLS")

    public val X_STRICT_IMPLICIT_EXPORT_TYPES: JsArgument<Boolean> =
        JsArgument("X_STRICT_IMPLICIT_EXPORT_TYPES")

    public val X_ES_CLASSES: JsArgument<Boolean?> = JsArgument("X_ES_CLASSES")

    public val X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION: JsArgument<String?> =
        JsArgument("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION")

    public val X_ES_GENERATORS: JsArgument<Boolean?> = JsArgument("X_ES_GENERATORS")

    public val X_ES_ARROW_FUNCTIONS: JsArgument<Boolean?> = JsArgument("X_ES_ARROW_FUNCTIONS")

    public val X_ES_LONG_AS_BIGINT: JsArgument<Boolean?> = JsArgument("X_ES_LONG_AS_BIGINT")

    public val X_TYPED_ARRAYS: JsArgument<Boolean> = JsArgument("X_TYPED_ARRAYS")

    public val X_FRIEND_MODULES_DISABLED: JsArgument<Boolean> =
        JsArgument("X_FRIEND_MODULES_DISABLED")

    public val X_FRIEND_MODULES: JsArgument<String?> = JsArgument("X_FRIEND_MODULES")

    public val X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS: JsArgument<Boolean> =
        JsArgument("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS")

    public val X_FAKE_OVERRIDE_VALIDATOR: JsArgument<Boolean> =
        JsArgument("X_FAKE_OVERRIDE_VALIDATOR")

    public val X_OPTIMIZE_GENERATED_JS: JsArgument<Boolean> = JsArgument("X_OPTIMIZE_GENERATED_JS")
  }
}
