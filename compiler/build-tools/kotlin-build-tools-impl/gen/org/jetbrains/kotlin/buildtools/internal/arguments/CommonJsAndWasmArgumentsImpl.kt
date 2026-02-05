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
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.IR_OUTPUT_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.MAIN
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
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PER_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PER_MODULE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PRODUCE_JS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PRODUCE_KLIB_DIR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PRODUCE_KLIB_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_IR_PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonJsAndWasmArgumentsImpl.Companion.X_STRICT_IMPLICIT_EXPORT_TYPES
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonJsAndWasmArgumentsImpl : CommonKlibBasedArgumentsImpl(),
    CommonJsAndWasmArguments, CommonJsAndWasmArguments.Builder {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return optionsMap[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 4, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<*>): Boolean = key.id in optionsMap

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
    if (X_CACHE_DIRECTORY in this) { arguments.cacheDirectory = get(X_CACHE_DIRECTORY)}
    if (X_FAKE_OVERRIDE_VALIDATOR in this) { arguments.fakeOverrideValidator = get(X_FAKE_OVERRIDE_VALIDATOR)}
    if (X_FRIEND_MODULES in this) { arguments.friendModules = get(X_FRIEND_MODULES)}
    if (X_FRIEND_MODULES_DISABLED in this) { arguments.friendModulesDisabled = get(X_FRIEND_MODULES_DISABLED)}
    if (X_GENERATE_DTS in this) { arguments.generateDts = get(X_GENERATE_DTS)}
    if (X_INCLUDE in this) { arguments.includes = get(X_INCLUDE)}
    if (X_IR_DCE in this) { arguments.irDce = get(X_IR_DCE)}
    if (X_IR_DCE_PRINT_REACHABILITY_INFO in this) { arguments.irDcePrintReachabilityInfo = get(X_IR_DCE_PRINT_REACHABILITY_INFO)}
    if (X_IR_DCE_RUNTIME_DIAGNOSTIC in this) { arguments.irDceRuntimeDiagnostic = get(X_IR_DCE_RUNTIME_DIAGNOSTIC)}
    if (X_IR_MODULE_NAME in this) { arguments.irModuleName = get(X_IR_MODULE_NAME)}
    if (X_IR_PER_FILE in this) { arguments.irPerFile = get(X_IR_PER_FILE)}
    if (X_IR_PER_MODULE in this) { arguments.irPerModule = get(X_IR_PER_MODULE)}
    if (X_IR_PER_MODULE_OUTPUT_NAME in this) { arguments.irPerModuleOutputName = get(X_IR_PER_MODULE_OUTPUT_NAME)}
    if (X_IR_PRODUCE_JS in this) { arguments.irProduceJs = get(X_IR_PRODUCE_JS)}
    if (X_IR_PRODUCE_KLIB_DIR in this) { arguments.irProduceKlibDir = get(X_IR_PRODUCE_KLIB_DIR)}
    if (X_IR_PRODUCE_KLIB_FILE in this) { arguments.irProduceKlibFile = get(X_IR_PRODUCE_KLIB_FILE)}
    if (X_IR_PROPERTY_LAZY_INITIALIZATION in this) { arguments.irPropertyLazyInitialization = get(X_IR_PROPERTY_LAZY_INITIALIZATION)}
    if (X_STRICT_IMPLICIT_EXPORT_TYPES in this) { arguments.strictImplicitExportType = get(X_STRICT_IMPLICIT_EXPORT_TYPES)}
    if (IR_OUTPUT_DIR in this) { arguments.outputDir = get(IR_OUTPUT_DIR)}
    if (IR_OUTPUT_NAME in this) { arguments.moduleName = get(IR_OUTPUT_NAME)}
    if (LIBRARIES in this) { arguments.libraries = get(LIBRARIES)}
    if (MAIN in this) { arguments.main = get(MAIN)}
    if (SOURCE_MAP in this) { arguments.sourceMap = get(SOURCE_MAP)}
    if (SOURCE_MAP_BASE_DIRS in this) { arguments.sourceMapBaseDirs = get(SOURCE_MAP_BASE_DIRS)}
    if (SOURCE_MAP_EMBED_SOURCES in this) { arguments.sourceMapEmbedSources = get(SOURCE_MAP_EMBED_SOURCES)}
    if (SOURCE_MAP_NAMES_POLICY in this) { arguments.sourceMapNamesPolicy = get(SOURCE_MAP_NAMES_POLICY)}
    if (SOURCE_MAP_PREFIX in this) { arguments.sourceMapPrefix = get(SOURCE_MAP_PREFIX)}
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: CommonJsAndWasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_CACHE_DIRECTORY] = arguments.cacheDirectory } catch (_: NoSuchMethodError) {  }
    try { this[X_FAKE_OVERRIDE_VALIDATOR] = arguments.fakeOverrideValidator } catch (_: NoSuchMethodError) {  }
    try { this[X_FRIEND_MODULES] = arguments.friendModules } catch (_: NoSuchMethodError) {  }
    try { this[X_FRIEND_MODULES_DISABLED] = arguments.friendModulesDisabled } catch (_: NoSuchMethodError) {  }
    try { this[X_GENERATE_DTS] = arguments.generateDts } catch (_: NoSuchMethodError) {  }
    try { this[X_INCLUDE] = arguments.includes } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE] = arguments.irDce } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE_PRINT_REACHABILITY_INFO] = arguments.irDcePrintReachabilityInfo } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DCE_RUNTIME_DIAGNOSTIC] = arguments.irDceRuntimeDiagnostic } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_MODULE_NAME] = arguments.irModuleName } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_FILE] = arguments.irPerFile } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_MODULE] = arguments.irPerModule } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_MODULE_OUTPUT_NAME] = arguments.irPerModuleOutputName } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_JS] = arguments.irProduceJs } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_KLIB_DIR] = arguments.irProduceKlibDir } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PRODUCE_KLIB_FILE] = arguments.irProduceKlibFile } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PROPERTY_LAZY_INITIALIZATION] = arguments.irPropertyLazyInitialization } catch (_: NoSuchMethodError) {  }
    try { this[X_STRICT_IMPLICIT_EXPORT_TYPES] = arguments.strictImplicitExportType } catch (_: NoSuchMethodError) {  }
    try { this[IR_OUTPUT_DIR] = arguments.outputDir } catch (_: NoSuchMethodError) {  }
    try { this[IR_OUTPUT_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { this[LIBRARIES] = arguments.libraries } catch (_: NoSuchMethodError) {  }
    try { this[MAIN] = arguments.main } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP] = arguments.sourceMap } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_BASE_DIRS] = arguments.sourceMapBaseDirs } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_EMBED_SOURCES] = arguments.sourceMapEmbedSources } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_NAMES_POLICY] = arguments.sourceMapNamesPolicy } catch (_: NoSuchMethodError) {  }
    try { this[SOURCE_MAP_PREFIX] = arguments.sourceMapPrefix } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class CommonJsAndWasmArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_CACHE_DIRECTORY: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_CACHE_DIRECTORY")

    public val X_FAKE_OVERRIDE_VALIDATOR: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_FAKE_OVERRIDE_VALIDATOR")

    public val X_FRIEND_MODULES: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES")

    public val X_FRIEND_MODULES_DISABLED: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES_DISABLED")

    public val X_GENERATE_DTS: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_GENERATE_DTS")

    public val X_INCLUDE: CommonJsAndWasmArgument<String?> = CommonJsAndWasmArgument("X_INCLUDE")

    public val X_IR_DCE: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("X_IR_DCE")

    public val X_IR_DCE_PRINT_REACHABILITY_INFO: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_DCE_PRINT_REACHABILITY_INFO")

    public val X_IR_DCE_RUNTIME_DIAGNOSTIC: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_IR_DCE_RUNTIME_DIAGNOSTIC")

    public val X_IR_MODULE_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_IR_MODULE_NAME")

    public val X_IR_PER_FILE: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PER_FILE")

    public val X_IR_PER_MODULE: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PER_MODULE")

    public val X_IR_PER_MODULE_OUTPUT_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("X_IR_PER_MODULE_OUTPUT_NAME")

    public val X_IR_PRODUCE_JS: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PRODUCE_JS")

    public val X_IR_PRODUCE_KLIB_DIR: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PRODUCE_KLIB_DIR")

    public val X_IR_PRODUCE_KLIB_FILE: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PRODUCE_KLIB_FILE")

    public val X_IR_PROPERTY_LAZY_INITIALIZATION: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_IR_PROPERTY_LAZY_INITIALIZATION")

    public val X_STRICT_IMPLICIT_EXPORT_TYPES: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_STRICT_IMPLICIT_EXPORT_TYPES")

    public val IR_OUTPUT_DIR: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("IR_OUTPUT_DIR")

    public val IR_OUTPUT_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("IR_OUTPUT_NAME")

    public val LIBRARIES: CommonJsAndWasmArgument<String?> = CommonJsAndWasmArgument("LIBRARIES")

    public val MAIN: CommonJsAndWasmArgument<String?> = CommonJsAndWasmArgument("MAIN")

    public val SOURCE_MAP: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("SOURCE_MAP")

    public val SOURCE_MAP_BASE_DIRS: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_BASE_DIRS")

    public val SOURCE_MAP_EMBED_SOURCES: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_EMBED_SOURCES")

    public val SOURCE_MAP_NAMES_POLICY: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_NAMES_POLICY")

    public val SOURCE_MAP_PREFIX: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_PREFIX")
  }
}
