// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
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
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.OUTPUT
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
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class JsArgumentsImpl : WasmArgumentsImpl(), JsArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsArguments.JsArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsArguments.JsArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 3, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
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
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_CACHE_DIRECTORY in this) { arguments.cacheDirectory = get(X_CACHE_DIRECTORY)}
    if (X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS in this) { arguments.extensionFunctionsInExternals = get(X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS)}
    if (X_ES_ARROW_FUNCTIONS in this) { arguments.useEsArrowFunctions = get(X_ES_ARROW_FUNCTIONS)}
    if (X_ES_CLASSES in this) { arguments.useEsClasses = get(X_ES_CLASSES)}
    if (X_ES_GENERATORS in this) { arguments.useEsGenerators = get(X_ES_GENERATORS)}
    if (X_ES_LONG_AS_BIGINT in this) { arguments.compileLongAsBigInt = get(X_ES_LONG_AS_BIGINT)}
    if (X_FAKE_OVERRIDE_VALIDATOR in this) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR)}
    if (X_FRIEND_MODULES in this) { arguments.friendModules = get(X_FRIEND_MODULES)}
    if (X_FRIEND_MODULES_DISABLED in this) { arguments.friendModulesDisabled = get(X_FRIEND_MODULES_DISABLED)}
    if (X_GENERATE_DTS in this) { arguments.generateDts = get(X_GENERATE_DTS)}
    if (X_GENERATE_POLYFILLS in this) { arguments.generatePolyfills = get(X_GENERATE_POLYFILLS)}
    if (X_INCLUDE in this) { arguments.includes = get(X_INCLUDE)}
    if (X_IR_BUILD_CACHE in this) { arguments.irBuildCache = get(X_IR_BUILD_CACHE)}
    if (X_IR_DCE in this) { arguments.irDce = get(X_IR_DCE)}
    if (X_IR_DCE_PRINT_REACHABILITY_INFO in this) { arguments.irDcePrintReachabilityInfo = get(X_IR_DCE_PRINT_REACHABILITY_INFO)}
    if (X_IR_DCE_RUNTIME_DIAGNOSTIC in this) { arguments.irDceRuntimeDiagnostic = get(X_IR_DCE_RUNTIME_DIAGNOSTIC)}
    if (X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS in this) { arguments.irGenerateInlineAnonymousFunctions = get(X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS)}
    if (X_IR_KEEP in this) { arguments.irKeep = get(X_IR_KEEP)}
    if (X_IR_MINIMIZED_MEMBER_NAMES in this) { arguments.irMinimizedMemberNames = get(X_IR_MINIMIZED_MEMBER_NAMES)}
    if (X_IR_MODULE_NAME in this) { arguments.irModuleName = get(X_IR_MODULE_NAME)}
    if (X_IR_PER_FILE in this) { arguments.irPerFile = get(X_IR_PER_FILE)}
    if (X_IR_PER_MODULE in this) { arguments.irPerModule = get(X_IR_PER_MODULE)}
    if (X_IR_PER_MODULE_OUTPUT_NAME in this) { arguments.irPerModuleOutputName = get(X_IR_PER_MODULE_OUTPUT_NAME)}
    if (X_IR_PRODUCE_JS in this) { arguments.irProduceJs = get(X_IR_PRODUCE_JS)}
    if (X_IR_PRODUCE_KLIB_DIR in this) { arguments.irProduceKlibDir = get(X_IR_PRODUCE_KLIB_DIR)}
    if (X_IR_PRODUCE_KLIB_FILE in this) { arguments.irProduceKlibFile = get(X_IR_PRODUCE_KLIB_FILE)}
    if (X_IR_PROPERTY_LAZY_INITIALIZATION in this) { arguments.irPropertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION)}
    if (X_IR_SAFE_EXTERNAL_BOOLEAN in this) { arguments.irSafeExternalBoolean = get(X_IR_SAFE_EXTERNAL_BOOLEAN)}
    if (X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC in this) { arguments.irSafeExternalBooleanDiagnostic = get(X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC)}
    if (X_OPTIMIZE_GENERATED_JS in this) { arguments.optimizeGeneratedJs = get(X_OPTIMIZE_GENERATED_JS)}
    if (X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION in this) { arguments.platformArgumentsProviderJsExpression = get(X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION)}
    if (X_STRICT_IMPLICIT_EXPORT_TYPES in this) { arguments.strictImplicitExportType = get(X_STRICT_IMPLICIT_EXPORT_TYPES)}
    try { if (X_TYPED_ARRAYS in this) { arguments.setUsingReflection("typedArrays", get(X_TYPED_ARRAYS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    if (IR_OUTPUT_DIR in this) { arguments.outputDir = get(IR_OUTPUT_DIR)}
    if (IR_OUTPUT_NAME in this) { arguments.moduleName = get(IR_OUTPUT_NAME)}
    if (LIBRARIES in this) { arguments.libraries = get(LIBRARIES)}
    if (MAIN in this) { arguments.main = get(MAIN)}
    if (MODULE_KIND in this) { arguments.moduleKind = get(MODULE_KIND)}
    try { if (OUTPUT in this) { arguments.setUsingReflection("outputFile", get(OUTPUT))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: OUTPUT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.2.0""").initCause(e) }
    if (SOURCE_MAP in this) { arguments.sourceMap = get(SOURCE_MAP)}
    if (SOURCE_MAP_BASE_DIRS in this) { arguments.sourceMapBaseDirs = get(SOURCE_MAP_BASE_DIRS)}
    if (SOURCE_MAP_EMBED_SOURCES in this) { arguments.sourceMapEmbedSources = get(SOURCE_MAP_EMBED_SOURCES)}
    if (SOURCE_MAP_NAMES_POLICY in this) { arguments.sourceMapNamesPolicy = get(SOURCE_MAP_NAMES_POLICY)}
    if (SOURCE_MAP_PREFIX in this) { arguments.sourceMapPrefix = get(SOURCE_MAP_PREFIX)}
    if (TARGET in this) { arguments.target = get(TARGET)}
    arguments.internalArguments = parseCommandLineArguments<K2JSCompilerArguments>(internalArguments.toList()).internalArguments
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: K2JSCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_CACHE_DIRECTORY] = arguments.cacheDirectory } catch (_: NoSuchMethodError) {  }
    try { this[X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS] = arguments.extensionFunctionsInExternals } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_ARROW_FUNCTIONS] = arguments.useEsArrowFunctions } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_CLASSES] = arguments.useEsClasses } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_GENERATORS] = arguments.useEsGenerators } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_LONG_AS_BIGINT] = arguments.compileLongAsBigInt } catch (_: NoSuchMethodError) {  }
    try { this[X_FAKE_OVERRIDE_VALIDATOR] = arguments.fakeOverrideValidator } catch (_: NoSuchMethodError) {  }
    try { this[X_FRIEND_MODULES] = arguments.friendModules } catch (_: NoSuchMethodError) {  }
    try { this[X_FRIEND_MODULES_DISABLED] = arguments.friendModulesDisabled } catch (_: NoSuchMethodError) {  }
    try { this[X_GENERATE_DTS] = arguments.generateDts } catch (_: NoSuchMethodError) {  }
    try { this[X_GENERATE_POLYFILLS] = arguments.generatePolyfills } catch (_: NoSuchMethodError) {  }
    try { this[X_INCLUDE] = arguments.includes } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_BUILD_CACHE] = arguments.irBuildCache } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE] = arguments.irDce } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE_PRINT_REACHABILITY_INFO] = arguments.irDcePrintReachabilityInfo } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE_RUNTIME_DIAGNOSTIC] = arguments.irDceRuntimeDiagnostic } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS] = arguments.irGenerateInlineAnonymousFunctions } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_KEEP] = arguments.irKeep } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_MINIMIZED_MEMBER_NAMES] = arguments.irMinimizedMemberNames } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_MODULE_NAME] = arguments.irModuleName } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_FILE] = arguments.irPerFile } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_MODULE] = arguments.irPerModule } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_MODULE_OUTPUT_NAME] = arguments.irPerModuleOutputName } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_JS] = arguments.irProduceJs } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_KLIB_DIR] = arguments.irProduceKlibDir } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_KLIB_FILE] = arguments.irProduceKlibFile } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PROPERTY_LAZY_INITIALIZATION] = arguments.irPropertyLazyInitialization } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_SAFE_EXTERNAL_BOOLEAN] = arguments.irSafeExternalBoolean } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC] = arguments.irSafeExternalBooleanDiagnostic } catch (_: NoSuchMethodError) {  }
    try { this[X_OPTIMIZE_GENERATED_JS] = arguments.optimizeGeneratedJs } catch (_: NoSuchMethodError) {  }
    try { this[X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION] = arguments.platformArgumentsProviderJsExpression } catch (_: NoSuchMethodError) {  }
    try { this[X_STRICT_IMPLICIT_EXPORT_TYPES] = arguments.strictImplicitExportType } catch (_: NoSuchMethodError) {  }
    try { this[X_TYPED_ARRAYS] = arguments.getUsingReflection("typedArrays") } catch (_: NoSuchMethodError) {  }
    try { this[IR_OUTPUT_DIR] = arguments.outputDir } catch (_: NoSuchMethodError) {  }
    try { this[IR_OUTPUT_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { this[LIBRARIES] = arguments.libraries } catch (_: NoSuchMethodError) {  }
    try { this[MAIN] = arguments.main } catch (_: NoSuchMethodError) {  }
    try { this[MODULE_KIND] = arguments.moduleKind } catch (_: NoSuchMethodError) {  }
    try { this[OUTPUT] = arguments.getUsingReflection("outputFile") } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP] = arguments.sourceMap } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_BASE_DIRS] = arguments.sourceMapBaseDirs } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_EMBED_SOURCES] = arguments.sourceMapEmbedSources } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_NAMES_POLICY] = arguments.sourceMapNamesPolicy } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_PREFIX] = arguments.sourceMapPrefix } catch (_: NoSuchMethodError) {  }
    try { this[TARGET] = arguments.target } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2JSCompilerArguments = parseCommandLineArguments(arguments)
    validateArguments(compilerArgs.errors)?.let { throw CompilerArgumentsParseException(it) }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings()
    return arguments
  }

  public class JsArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_CACHE_DIRECTORY: JsArgument<String?> = JsArgument("X_CACHE_DIRECTORY")

    public val X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS: JsArgument<Boolean> =
        JsArgument("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS")

    public val X_ES_ARROW_FUNCTIONS: JsArgument<Boolean?> = JsArgument("X_ES_ARROW_FUNCTIONS")

    public val X_ES_CLASSES: JsArgument<Boolean?> = JsArgument("X_ES_CLASSES")

    public val X_ES_GENERATORS: JsArgument<Boolean?> = JsArgument("X_ES_GENERATORS")

    public val X_ES_LONG_AS_BIGINT: JsArgument<Boolean?> = JsArgument("X_ES_LONG_AS_BIGINT")

    public val X_FAKE_OVERRIDE_VALIDATOR: JsArgument<Boolean> =
        JsArgument("X_FAKE_OVERRIDE_VALIDATOR")

    public val X_FRIEND_MODULES: JsArgument<String?> = JsArgument("X_FRIEND_MODULES")

    public val X_FRIEND_MODULES_DISABLED: JsArgument<Boolean> =
        JsArgument("X_FRIEND_MODULES_DISABLED")

    public val X_GENERATE_DTS: JsArgument<Boolean> = JsArgument("X_GENERATE_DTS")

    public val X_GENERATE_POLYFILLS: JsArgument<Boolean> = JsArgument("X_GENERATE_POLYFILLS")

    public val X_INCLUDE: JsArgument<String?> = JsArgument("X_INCLUDE")

    public val X_IR_BUILD_CACHE: JsArgument<Boolean> = JsArgument("X_IR_BUILD_CACHE")

    public val X_IR_DCE: JsArgument<Boolean> = JsArgument("X_IR_DCE")

    public val X_IR_DCE_PRINT_REACHABILITY_INFO: JsArgument<Boolean> =
        JsArgument("X_IR_DCE_PRINT_REACHABILITY_INFO")

    public val X_IR_DCE_RUNTIME_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("X_IR_DCE_RUNTIME_DIAGNOSTIC")

    public val X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS: JsArgument<Boolean> =
        JsArgument("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS")

    public val X_IR_KEEP: JsArgument<String?> = JsArgument("X_IR_KEEP")

    public val X_IR_MINIMIZED_MEMBER_NAMES: JsArgument<Boolean> =
        JsArgument("X_IR_MINIMIZED_MEMBER_NAMES")

    public val X_IR_MODULE_NAME: JsArgument<String?> = JsArgument("X_IR_MODULE_NAME")

    public val X_IR_PER_FILE: JsArgument<Boolean> = JsArgument("X_IR_PER_FILE")

    public val X_IR_PER_MODULE: JsArgument<Boolean> = JsArgument("X_IR_PER_MODULE")

    public val X_IR_PER_MODULE_OUTPUT_NAME: JsArgument<String?> =
        JsArgument("X_IR_PER_MODULE_OUTPUT_NAME")

    public val X_IR_PRODUCE_JS: JsArgument<Boolean> = JsArgument("X_IR_PRODUCE_JS")

    public val X_IR_PRODUCE_KLIB_DIR: JsArgument<Boolean> = JsArgument("X_IR_PRODUCE_KLIB_DIR")

    public val X_IR_PRODUCE_KLIB_FILE: JsArgument<Boolean> = JsArgument("X_IR_PRODUCE_KLIB_FILE")

    public val X_IR_PROPERTY_LAZY_INITIALIZATION: JsArgument<Boolean> =
        JsArgument("X_IR_PROPERTY_LAZY_INITIALIZATION")

    public val X_IR_SAFE_EXTERNAL_BOOLEAN: JsArgument<Boolean> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN")

    public val X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: JsArgument<String?> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC")

    public val X_OPTIMIZE_GENERATED_JS: JsArgument<Boolean> = JsArgument("X_OPTIMIZE_GENERATED_JS")

    public val X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION: JsArgument<String?> =
        JsArgument("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION")

    public val X_STRICT_IMPLICIT_EXPORT_TYPES: JsArgument<Boolean> =
        JsArgument("X_STRICT_IMPLICIT_EXPORT_TYPES")

    public val X_TYPED_ARRAYS: JsArgument<Boolean> = JsArgument("X_TYPED_ARRAYS")

    public val IR_OUTPUT_DIR: JsArgument<String?> = JsArgument("IR_OUTPUT_DIR")

    public val IR_OUTPUT_NAME: JsArgument<String?> = JsArgument("IR_OUTPUT_NAME")

    public val LIBRARIES: JsArgument<String?> = JsArgument("LIBRARIES")

    public val MAIN: JsArgument<String?> = JsArgument("MAIN")

    public val MODULE_KIND: JsArgument<String?> = JsArgument("MODULE_KIND")

    public val OUTPUT: JsArgument<String?> = JsArgument("OUTPUT")

    public val SOURCE_MAP: JsArgument<Boolean> = JsArgument("SOURCE_MAP")

    public val SOURCE_MAP_BASE_DIRS: JsArgument<String?> = JsArgument("SOURCE_MAP_BASE_DIRS")

    public val SOURCE_MAP_EMBED_SOURCES: JsArgument<String?> =
        JsArgument("SOURCE_MAP_EMBED_SOURCES")

    public val SOURCE_MAP_NAMES_POLICY: JsArgument<String?> = JsArgument("SOURCE_MAP_NAMES_POLICY")

    public val SOURCE_MAP_PREFIX: JsArgument<String?> = JsArgument("SOURCE_MAP_PREFIX")

    public val TARGET: JsArgument<String?> = JsArgument("TARGET")
  }
}
