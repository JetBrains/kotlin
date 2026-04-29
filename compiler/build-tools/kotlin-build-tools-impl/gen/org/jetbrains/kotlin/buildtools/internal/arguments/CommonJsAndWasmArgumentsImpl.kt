// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.io.File
import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.io.path.Path
import kotlin.text.split
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.IR_OUTPUT_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.MAIN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.SOURCE_MAP
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.SOURCE_MAP_BASE_DIRS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.SOURCE_MAP_NAMES_POLICY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.SOURCE_MAP_PREFIX
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_CACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_FAKE_OVERRIDE_VALIDATOR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_FRIEND_MODULES_DISABLED
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_GENERATE_DTS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_INCLUDE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_DCE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_DCE_PRINT_REACHABILITY_INFO
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_DCE_RUNTIME_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_MODULE_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PRODUCE_JS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PRODUCE_KLIB_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PRODUCE_KLIB_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_STRICT_IMPLICIT_EXPORT_TYPES
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapNamesPolicy
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonJsAndWasmArgumentsImpl(
  private val adapter: CommonJsAndWasmArgumentValueAdapter? = null,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
) : CommonKlibBasedArgumentsImpl(adapter, argumentValidationErrors, restrictedArgViolations),
    CommonJsAndWasmArguments,
    CommonJsAndWasmArguments.Builder {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return adapter?.mapFrom(optionsMap[key.id], key) ?: optionsMap[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 4, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = adapter?.mapTo(`value`, key) ?: `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonJsAndWasmArgument<V>): V = optionsMap[key.id] as V

  private operator fun <V> `set`(key: CommonJsAndWasmArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonJsAndWasmArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_CACHE_DIRECTORY in this) { arguments.cacheDirectory = get(X_CACHE_DIRECTORY)?.absolutePathStringOrThrow()}
    if (X_FAKE_OVERRIDE_VALIDATOR in this) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR)}
    if (X_FRIEND_MODULES in this) { arguments.friendModules = get(X_FRIEND_MODULES)?.map { it.absolutePathStringOrThrow() }?.joinToString(File.pathSeparator)}
    if (X_FRIEND_MODULES_DISABLED in this) { arguments.friendModulesDisabled = get(X_FRIEND_MODULES_DISABLED)}
    if (X_GENERATE_DTS in this) { arguments.generateDts = get(X_GENERATE_DTS)}
    if (X_INCLUDE in this) { arguments.includes = get(X_INCLUDE)?.absolutePathStringOrThrow()}
    if (X_IR_DCE in this) { arguments.irDce = get(X_IR_DCE)}
    if (X_IR_DCE_PRINT_REACHABILITY_INFO in this) { arguments.irDcePrintReachabilityInfo = get(X_IR_DCE_PRINT_REACHABILITY_INFO)}
    if (X_IR_DCE_RUNTIME_DIAGNOSTIC in this) { arguments.irDceRuntimeDiagnostic = get(X_IR_DCE_RUNTIME_DIAGNOSTIC)?.stringValue}
    if (X_IR_MODULE_NAME in this) { arguments.irModuleName = get(X_IR_MODULE_NAME)}
    if (X_IR_PER_MODULE_OUTPUT_NAME in this) { arguments.irPerModuleOutputName = get(X_IR_PER_MODULE_OUTPUT_NAME)}
    if (X_IR_PRODUCE_JS in this) { arguments.irProduceJs = get(X_IR_PRODUCE_JS)}
    if (X_IR_PRODUCE_KLIB_DIR in this) { arguments.irProduceKlibDir = get(X_IR_PRODUCE_KLIB_DIR)}
    if (X_IR_PRODUCE_KLIB_FILE in this) { arguments.irProduceKlibFile = get(X_IR_PRODUCE_KLIB_FILE)}
    if (X_IR_PROPERTY_LAZY_INITIALIZATION in this) { arguments.irPropertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION)}
    if (X_STRICT_IMPLICIT_EXPORT_TYPES in this) { arguments.strictImplicitExportType = get(X_STRICT_IMPLICIT_EXPORT_TYPES)}
    if (IR_OUTPUT_DIR in this) { arguments.outputDir = get(IR_OUTPUT_DIR)}
    if (IR_OUTPUT_NAME in this) { arguments.moduleName = get(IR_OUTPUT_NAME)}
    if (LIBRARIES in this) { arguments.libraries = get(LIBRARIES)?.map { it.absolutePathStringOrThrow() }?.joinToString(File.pathSeparator)}
    if (MAIN in this) { arguments.main = get(MAIN)?.stringValue}
    if (NOPACK in this) { arguments.nopack = get(NOPACK)}
    if (SOURCE_MAP in this) { arguments.sourceMap = get(SOURCE_MAP)}
    if (SOURCE_MAP_BASE_DIRS in this) { arguments.sourceMapBaseDirs = get(SOURCE_MAP_BASE_DIRS)}
    if (SOURCE_MAP_EMBED_SOURCES in this) { arguments.sourceMapEmbedSources = get(SOURCE_MAP_EMBED_SOURCES)?.stringValue}
    if (SOURCE_MAP_NAMES_POLICY in this) { arguments.sourceMapNamesPolicy = get(SOURCE_MAP_NAMES_POLICY)?.stringValue}
    if (SOURCE_MAP_PREFIX in this) { arguments.sourceMapPrefix = get(SOURCE_MAP_PREFIX)}
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonJsAndWasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_CACHE_DIRECTORY] = arguments.cacheDirectory?.let { Path(it) } } catch (_: NoSuchMethodError) {  }
    try { this[X_FAKE_OVERRIDE_VALIDATOR] = arguments.fakeOverrideValidator } catch (_: NoSuchMethodError) {  }
    try { this[X_FRIEND_MODULES] = arguments.friendModules?.split(File.pathSeparator)?.map { Path(it) } } catch (_: NoSuchMethodError) {  }
    try { this[X_FRIEND_MODULES_DISABLED] = arguments.friendModulesDisabled } catch (_: NoSuchMethodError) {  }
    try { this[X_GENERATE_DTS] = arguments.generateDts } catch (_: NoSuchMethodError) {  }
    try { this[X_INCLUDE] = arguments.includes?.let { Path(it) } } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE] = arguments.irDce } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE_PRINT_REACHABILITY_INFO] = arguments.irDcePrintReachabilityInfo } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE_RUNTIME_DIAGNOSTIC] = arguments.irDceRuntimeDiagnostic?.let { JsIrDiagnosticMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irDceRuntimeDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-dce-runtime-diagnostic value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_MODULE_NAME] = arguments.irModuleName } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_MODULE_OUTPUT_NAME] = arguments.irPerModuleOutputName } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_JS] = arguments.irProduceJs } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_KLIB_DIR] = arguments.irProduceKlibDir } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_KLIB_FILE] = arguments.irProduceKlibFile } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PROPERTY_LAZY_INITIALIZATION] = arguments.irPropertyLazyInitialization } catch (_: NoSuchMethodError) {  }
    try { this[X_STRICT_IMPLICIT_EXPORT_TYPES] = arguments.strictImplicitExportType } catch (_: NoSuchMethodError) {  }
    try { this[IR_OUTPUT_DIR] = arguments.outputDir } catch (_: NoSuchMethodError) {  }
    try { this[IR_OUTPUT_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { this[LIBRARIES] = arguments.libraries?.split(File.pathSeparator)?.map { Path(it) } } catch (_: NoSuchMethodError) {  }
    try { this[MAIN] = arguments.main?.let { JsMainCallMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::main, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -main value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[NOPACK] = arguments.nopack } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP] = arguments.sourceMap } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_BASE_DIRS] = arguments.sourceMapBaseDirs } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_EMBED_SOURCES] = arguments.sourceMapEmbedSources?.let { SourceMapEmbedSources.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::sourceMapEmbedSources, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-embed-sources value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_NAMES_POLICY] = arguments.sourceMapNamesPolicy?.let { SourceMapNamesPolicy.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::sourceMapNamesPolicy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-names-policy value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_PREFIX] = arguments.sourceMapPrefix } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    if (X_CACHE_DIRECTORY in this) { arguments.cacheDirectory = get(X_CACHE_DIRECTORY)?.absolutePathStringOrThrow()}
    if (X_FAKE_OVERRIDE_VALIDATOR in this) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR)}
    if (X_FRIEND_MODULES in this) { arguments.friendModules = get(X_FRIEND_MODULES)?.map { it.absolutePathStringOrThrow() }?.joinToString(File.pathSeparator)}
    if (X_FRIEND_MODULES_DISABLED in this) { arguments.friendModulesDisabled = get(X_FRIEND_MODULES_DISABLED)}
    if (X_GENERATE_DTS in this) { arguments.generateDts = get(X_GENERATE_DTS)}
    if (X_INCLUDE in this) { arguments.includes = get(X_INCLUDE)?.absolutePathStringOrThrow()}
    if (X_IR_DCE in this) { arguments.irDce = get(X_IR_DCE)}
    if (X_IR_DCE_RUNTIME_DIAGNOSTIC in this) { arguments.irDceRuntimeDiagnostic = get(X_IR_DCE_RUNTIME_DIAGNOSTIC)?.stringValue}
    if (X_IR_MODULE_NAME in this) { arguments.irModuleName = get(X_IR_MODULE_NAME)}
    if (X_IR_PER_MODULE_OUTPUT_NAME in this) { arguments.irPerModuleOutputName = get(X_IR_PER_MODULE_OUTPUT_NAME)}
    if (X_IR_PRODUCE_JS in this) { arguments.irProduceJs = get(X_IR_PRODUCE_JS)}
    if (X_IR_PRODUCE_KLIB_DIR in this) { arguments.irProduceKlibDir = get(X_IR_PRODUCE_KLIB_DIR)}
    if (X_IR_PRODUCE_KLIB_FILE in this) { arguments.irProduceKlibFile = get(X_IR_PRODUCE_KLIB_FILE)}
    if (X_IR_PROPERTY_LAZY_INITIALIZATION in this) { arguments.irPropertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION)}
    if (X_STRICT_IMPLICIT_EXPORT_TYPES in this) { arguments.strictImplicitExportType = get(X_STRICT_IMPLICIT_EXPORT_TYPES)}
    if (IR_OUTPUT_DIR in this) { arguments.outputDir = get(IR_OUTPUT_DIR)}
    if (IR_OUTPUT_NAME in this) { arguments.moduleName = get(IR_OUTPUT_NAME)}
    if (LIBRARIES in this) { arguments.libraries = get(LIBRARIES)?.map { it.absolutePathStringOrThrow() }?.joinToString(File.pathSeparator)}
    if (MAIN in this) { arguments.main = get(MAIN)?.stringValue}
    if (NOPACK in this) { arguments.nopack = get(NOPACK)}
    if (SOURCE_MAP in this) { arguments.sourceMap = get(SOURCE_MAP)}
    if (SOURCE_MAP_BASE_DIRS in this) { arguments.sourceMapBaseDirs = get(SOURCE_MAP_BASE_DIRS)}
    if (SOURCE_MAP_EMBED_SOURCES in this) { arguments.sourceMapEmbedSources = get(SOURCE_MAP_EMBED_SOURCES)?.stringValue}
    if (SOURCE_MAP_NAMES_POLICY in this) { arguments.sourceMapNamesPolicy = get(SOURCE_MAP_NAMES_POLICY)?.stringValue}
    if (SOURCE_MAP_PREFIX in this) { arguments.sourceMapPrefix = get(SOURCE_MAP_PREFIX)}
    return arguments
  }

  public class CommonJsAndWasmArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_CACHE_DIRECTORY: CommonJsAndWasmArgument<java.nio.`file`.Path?> =
        CommonJsAndWasmArgument("X_CACHE_DIRECTORY")

    public val X_FAKE_OVERRIDE_VALIDATOR: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_FAKE_OVERRIDE_VALIDATOR")

    public val X_FRIEND_MODULES: CommonJsAndWasmArgument<List<java.nio.`file`.Path>?> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES")

    public val X_FRIEND_MODULES_DISABLED: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES_DISABLED")

    public val X_GENERATE_DTS: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_GENERATE_DTS")

    public val X_INCLUDE: CommonJsAndWasmArgument<java.nio.`file`.Path?> =
        CommonJsAndWasmArgument("X_INCLUDE")

    public val X_IR_DCE: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("X_IR_DCE")

    public val X_IR_DCE_PRINT_REACHABILITY_INFO: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_DCE_PRINT_REACHABILITY_INFO")

    public val X_IR_DCE_RUNTIME_DIAGNOSTIC: CommonJsAndWasmArgument<JsIrDiagnosticMode?> =
        CommonJsAndWasmArgument("X_IR_DCE_RUNTIME_DIAGNOSTIC")

    public val X_IR_MODULE_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_IR_MODULE_NAME")

    public val X_IR_PER_MODULE_OUTPUT_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_IR_PER_MODULE_OUTPUT_NAME")

    public val X_IR_PRODUCE_JS: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PRODUCE_JS")

    public val X_IR_PRODUCE_KLIB_DIR: CommonJsAndWasmArgument<Boolean?> =
        CommonJsAndWasmArgument("X_IR_PRODUCE_KLIB_DIR")

    public val X_IR_PRODUCE_KLIB_FILE: CommonJsAndWasmArgument<Boolean?> =
        CommonJsAndWasmArgument("X_IR_PRODUCE_KLIB_FILE")

    public val X_IR_PROPERTY_LAZY_INITIALIZATION: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PROPERTY_LAZY_INITIALIZATION")

    public val X_STRICT_IMPLICIT_EXPORT_TYPES: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_STRICT_IMPLICIT_EXPORT_TYPES")

    public val IR_OUTPUT_DIR: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("IR_OUTPUT_DIR")

    public val IR_OUTPUT_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("IR_OUTPUT_NAME")

    public val LIBRARIES: CommonJsAndWasmArgument<List<java.nio.`file`.Path>?> =
        CommonJsAndWasmArgument("LIBRARIES")

    public val MAIN: CommonJsAndWasmArgument<JsMainCallMode?> = CommonJsAndWasmArgument("MAIN")

    public val NOPACK: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("NOPACK")

    public val SOURCE_MAP: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("SOURCE_MAP")

    public val SOURCE_MAP_BASE_DIRS: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_BASE_DIRS")

    public val SOURCE_MAP_EMBED_SOURCES: CommonJsAndWasmArgument<SourceMapEmbedSources?> =
        CommonJsAndWasmArgument("SOURCE_MAP_EMBED_SOURCES")

    public val SOURCE_MAP_NAMES_POLICY: CommonJsAndWasmArgument<SourceMapNamesPolicy?> =
        CommonJsAndWasmArgument("SOURCE_MAP_NAMES_POLICY")

    public val SOURCE_MAP_PREFIX: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_PREFIX")
  }
}
