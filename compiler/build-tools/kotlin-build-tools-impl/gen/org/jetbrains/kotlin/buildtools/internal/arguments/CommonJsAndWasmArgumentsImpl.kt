// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.io.File
import java.lang.IllegalStateException
import java.nio.`file`.Path
import kotlin.Any
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.text.split
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SourceMapNamesPolicy
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal abstract class CommonJsAndWasmArgumentsImpl() : CommonKlibBasedArgumentsImpl(),
    CommonJsAndWasmArguments, CommonJsAndWasmArguments.Builder,
    CommonJsAndWasmCompilerKlibArguments, CommonJsAndWasmCompilerKlibArguments.Builder,
    CommonJsAndWasmCompilerLinkingArguments, CommonJsAndWasmCompilerLinkingArguments.Builder {
  @Transient
  private val defaultArguments: CommonJsAndWasmCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_CACHE_DIRECTORY")
  internal var cacheDirectory: Path? =
      defaultArguments.cacheDirectory?.let { kotlin.io.path.Path(it) }

  @SerialName("X_ENABLE_ADVANCED_OPTIMIZATIONS")
  internal var enableAdvancedOptimizations: Boolean = defaultArguments.enableAdvancedOptimizations

  @SerialName("X_FRIEND_MODULES")
  internal var friendModules: List<Path>? =
      defaultArguments.friendModules?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("X_FRIEND_MODULES_DISABLED")
  internal var friendModulesDisabled: Boolean = defaultArguments.friendModulesDisabled

  @SerialName("X_GENERATE_DTS")
  internal var generateDts: Boolean = defaultArguments.generateDts

  @SerialName("X_INCLUDE")
  internal var includes: Path? = defaultArguments.includes?.let { kotlin.io.path.Path(it) }

  @SerialName("X_IR_DCE")
  internal var irDce: Boolean = defaultArguments.irDce

  @SerialName("X_IR_DCE_PRINT_REACHABILITY_INFO")
  internal var irDcePrintReachabilityInfo: Boolean = defaultArguments.irDcePrintReachabilityInfo

  @SerialName("X_IR_DCE_RUNTIME_DIAGNOSTIC")
  internal var irDceRuntimeDiagnostic: JsIrDiagnosticMode? =
      defaultArguments.irDceRuntimeDiagnostic?.let { JsIrDiagnosticMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::irDceRuntimeDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-dce-runtime-diagnostic value: $it") }

  @SerialName("X_IR_MODULE_NAME")
  internal var irModuleName: String? = defaultArguments.irModuleName

  @SerialName("X_IR_PER_MODULE_OUTPUT_NAME")
  internal var irPerModuleOutputName: String? = defaultArguments.irPerModuleOutputName

  @SerialName("X_IR_PRODUCE_JS")
  internal var irProduceJs: Boolean = defaultArguments.irProduceJs

  @SerialName("X_IR_PRODUCE_KLIB_DIR")
  @Suppress("DEPRECATION")
  internal var irProduceKlibDir: Boolean? = defaultArguments.irProduceKlibDir

  @SerialName("X_IR_PRODUCE_KLIB_FILE")
  @Suppress("DEPRECATION")
  internal var irProduceKlibFile: Boolean? = defaultArguments.irProduceKlibFile

  @SerialName("X_IR_PROPERTY_LAZY_INITIALIZATION")
  internal var irPropertyLazyInitialization: Boolean = defaultArguments.irPropertyLazyInitialization

  @SerialName("X_STRICT_IMPLICIT_EXPORT_TYPES")
  internal var strictImplicitExportType: Boolean = defaultArguments.strictImplicitExportType

  @SerialName("IR_OUTPUT_DIR")
  internal var outputDir: Path? = defaultArguments.outputDir?.let { kotlin.io.path.Path(it) }

  @SerialName("IR_OUTPUT_NAME")
  internal var moduleName: String? = defaultArguments.moduleName

  @SerialName("LIBRARIES")
  internal var libraries: List<Path>? =
      defaultArguments.libraries?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("MAIN")
  internal var main: JsMainCallMode? =
      defaultArguments.main?.let { JsMainCallMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::main, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -main value: $it") }

  @SerialName("NOPACK")
  internal var nopack: Boolean = defaultArguments.nopack

  @SerialName("SOURCE_MAP")
  internal var sourceMap: Boolean = defaultArguments.sourceMap

  @SerialName("SOURCE_MAP_BASE_DIRS")
  internal var sourceMapBaseDirs: List<Path>? =
      defaultArguments.sourceMapBaseDirs?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("SOURCE_MAP_EMBED_SOURCES")
  internal var sourceMapEmbedSources: SourceMapEmbedSources? =
      defaultArguments.sourceMapEmbedSources?.let { SourceMapEmbedSources.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::sourceMapEmbedSources, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-embed-sources value: $it") }

  @SerialName("SOURCE_MAP_NAMES_POLICY")
  internal var sourceMapNamesPolicy: SourceMapNamesPolicy? =
      defaultArguments.sourceMapNamesPolicy?.let { SourceMapNamesPolicy.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::sourceMapNamesPolicy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-names-policy value: $it") }

  @SerialName("SOURCE_MAP_PREFIX")
  internal var sourceMapPrefix: String? = defaultArguments.sourceMapPrefix

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  abstract override fun createDefaultArguments(): CommonJsAndWasmCompilerArguments

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>): V = CommonJsAndWasmArgumentValueAdapter.toApi(when (key.id) {
    "X_CACHE_DIRECTORY" -> cacheDirectory
    "X_ENABLE_ADVANCED_OPTIMIZATIONS" -> enableAdvancedOptimizations
    "X_FRIEND_MODULES" -> friendModules
    "X_FRIEND_MODULES_DISABLED" -> friendModulesDisabled
    "X_GENERATE_DTS" -> generateDts
    "X_INCLUDE" -> includes
    "X_IR_DCE" -> irDce
    "X_IR_DCE_PRINT_REACHABILITY_INFO" -> irDcePrintReachabilityInfo
    "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> irDceRuntimeDiagnostic
    "X_IR_MODULE_NAME" -> irModuleName
    "X_IR_PER_MODULE_OUTPUT_NAME" -> irPerModuleOutputName
    "X_IR_PRODUCE_JS" -> irProduceJs
    "X_IR_PRODUCE_KLIB_DIR" -> irProduceKlibDir
    "X_IR_PRODUCE_KLIB_FILE" -> irProduceKlibFile
    "X_IR_PROPERTY_LAZY_INITIALIZATION" -> irPropertyLazyInitialization
    "X_STRICT_IMPLICIT_EXPORT_TYPES" -> strictImplicitExportType
    "IR_OUTPUT_DIR" -> outputDir
    "IR_OUTPUT_NAME" -> moduleName
    "LIBRARIES" -> libraries
    "MAIN" -> main
    "NOPACK" -> nopack
    "SOURCE_MAP" -> sourceMap
    "SOURCE_MAP_BASE_DIRS" -> sourceMapBaseDirs
    "SOURCE_MAP_EMBED_SOURCES" -> sourceMapEmbedSources
    "SOURCE_MAP_NAMES_POLICY" -> sourceMapNamesPolicy
    "SOURCE_MAP_PREFIX" -> sourceMapPrefix
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_CACHE_DIRECTORY" -> cacheDirectory = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_ENABLE_ADVANCED_OPTIMIZATIONS" -> enableAdvancedOptimizations = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_FRIEND_MODULES" -> friendModules = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_FRIEND_MODULES_DISABLED" -> friendModulesDisabled = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_GENERATE_DTS" -> generateDts = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_INCLUDE" -> includes = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE" -> irDce = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE_PRINT_REACHABILITY_INFO" -> irDcePrintReachabilityInfo = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> irDceRuntimeDiagnostic = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_MODULE_NAME" -> irModuleName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PER_MODULE_OUTPUT_NAME" -> irPerModuleOutputName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_JS" -> irProduceJs = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_KLIB_DIR" -> irProduceKlibDir = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_KLIB_FILE" -> irProduceKlibFile = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PROPERTY_LAZY_INITIALIZATION" -> irPropertyLazyInitialization = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_STRICT_IMPLICIT_EXPORT_TYPES" -> strictImplicitExportType = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "IR_OUTPUT_DIR" -> outputDir = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "IR_OUTPUT_NAME" -> moduleName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "LIBRARIES" -> libraries = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "MAIN" -> main = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "NOPACK" -> nopack = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP" -> sourceMap = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_BASE_DIRS" -> sourceMapBaseDirs = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_EMBED_SOURCES" -> sourceMapEmbedSources = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_NAMES_POLICY" -> sourceMapNamesPolicy = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_PREFIX" -> sourceMapPrefix = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>): V = CommonJsAndWasmArgumentValueAdapter.toApi(when (key.id) {
    "X_CACHE_DIRECTORY" -> cacheDirectory
    "X_ENABLE_ADVANCED_OPTIMIZATIONS" -> enableAdvancedOptimizations
    "X_FRIEND_MODULES" -> friendModules
    "X_FRIEND_MODULES_DISABLED" -> friendModulesDisabled
    "X_GENERATE_DTS" -> generateDts
    "X_INCLUDE" -> includes
    "X_IR_DCE" -> irDce
    "X_IR_DCE_PRINT_REACHABILITY_INFO" -> irDcePrintReachabilityInfo
    "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> irDceRuntimeDiagnostic
    "X_IR_MODULE_NAME" -> irModuleName
    "X_IR_PER_MODULE_OUTPUT_NAME" -> irPerModuleOutputName
    "X_IR_PRODUCE_JS" -> irProduceJs
    "X_IR_PRODUCE_KLIB_DIR" -> irProduceKlibDir
    "X_IR_PRODUCE_KLIB_FILE" -> irProduceKlibFile
    "X_IR_PROPERTY_LAZY_INITIALIZATION" -> irPropertyLazyInitialization
    "X_STRICT_IMPLICIT_EXPORT_TYPES" -> strictImplicitExportType
    "IR_OUTPUT_DIR" -> outputDir
    "IR_OUTPUT_NAME" -> moduleName
    "LIBRARIES" -> libraries
    "MAIN" -> main
    "NOPACK" -> nopack
    "SOURCE_MAP" -> sourceMap
    "SOURCE_MAP_BASE_DIRS" -> sourceMapBaseDirs
    "SOURCE_MAP_EMBED_SOURCES" -> sourceMapEmbedSources
    "SOURCE_MAP_NAMES_POLICY" -> sourceMapNamesPolicy
    "SOURCE_MAP_PREFIX" -> sourceMapPrefix
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_CACHE_DIRECTORY" -> cacheDirectory = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_ENABLE_ADVANCED_OPTIMIZATIONS" -> enableAdvancedOptimizations = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_FRIEND_MODULES" -> friendModules = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_FRIEND_MODULES_DISABLED" -> friendModulesDisabled = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_GENERATE_DTS" -> generateDts = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_INCLUDE" -> includes = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE" -> irDce = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE_PRINT_REACHABILITY_INFO" -> irDcePrintReachabilityInfo = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> irDceRuntimeDiagnostic = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_MODULE_NAME" -> irModuleName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PER_MODULE_OUTPUT_NAME" -> irPerModuleOutputName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_JS" -> irProduceJs = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_KLIB_DIR" -> irProduceKlibDir = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_KLIB_FILE" -> irProduceKlibFile = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PROPERTY_LAZY_INITIALIZATION" -> irPropertyLazyInitialization = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_STRICT_IMPLICIT_EXPORT_TYPES" -> strictImplicitExportType = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "IR_OUTPUT_DIR" -> outputDir = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "IR_OUTPUT_NAME" -> moduleName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "LIBRARIES" -> libraries = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "MAIN" -> main = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "NOPACK" -> nopack = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP" -> sourceMap = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_BASE_DIRS" -> sourceMapBaseDirs = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_EMBED_SOURCES" -> sourceMapEmbedSources = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_NAMES_POLICY" -> sourceMapNamesPolicy = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_PREFIX" -> sourceMapPrefix = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>): V = CommonJsAndWasmArgumentValueAdapter.toApi(when (key.id) {
    "X_CACHE_DIRECTORY" -> cacheDirectory
    "X_ENABLE_ADVANCED_OPTIMIZATIONS" -> enableAdvancedOptimizations
    "X_FRIEND_MODULES" -> friendModules
    "X_FRIEND_MODULES_DISABLED" -> friendModulesDisabled
    "X_GENERATE_DTS" -> generateDts
    "X_INCLUDE" -> includes
    "X_IR_DCE" -> irDce
    "X_IR_DCE_PRINT_REACHABILITY_INFO" -> irDcePrintReachabilityInfo
    "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> irDceRuntimeDiagnostic
    "X_IR_MODULE_NAME" -> irModuleName
    "X_IR_PER_MODULE_OUTPUT_NAME" -> irPerModuleOutputName
    "X_IR_PRODUCE_JS" -> irProduceJs
    "X_IR_PRODUCE_KLIB_DIR" -> irProduceKlibDir
    "X_IR_PRODUCE_KLIB_FILE" -> irProduceKlibFile
    "X_IR_PROPERTY_LAZY_INITIALIZATION" -> irPropertyLazyInitialization
    "X_STRICT_IMPLICIT_EXPORT_TYPES" -> strictImplicitExportType
    "IR_OUTPUT_DIR" -> outputDir
    "IR_OUTPUT_NAME" -> moduleName
    "LIBRARIES" -> libraries
    "MAIN" -> main
    "NOPACK" -> nopack
    "SOURCE_MAP" -> sourceMap
    "SOURCE_MAP_BASE_DIRS" -> sourceMapBaseDirs
    "SOURCE_MAP_EMBED_SOURCES" -> sourceMapEmbedSources
    "SOURCE_MAP_NAMES_POLICY" -> sourceMapNamesPolicy
    "SOURCE_MAP_PREFIX" -> sourceMapPrefix
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_CACHE_DIRECTORY" -> cacheDirectory = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_ENABLE_ADVANCED_OPTIMIZATIONS" -> enableAdvancedOptimizations = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_FRIEND_MODULES" -> friendModules = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_FRIEND_MODULES_DISABLED" -> friendModulesDisabled = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_GENERATE_DTS" -> generateDts = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_INCLUDE" -> includes = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE" -> irDce = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE_PRINT_REACHABILITY_INFO" -> irDcePrintReachabilityInfo = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> irDceRuntimeDiagnostic = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_MODULE_NAME" -> irModuleName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PER_MODULE_OUTPUT_NAME" -> irPerModuleOutputName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_JS" -> irProduceJs = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_KLIB_DIR" -> irProduceKlibDir = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PRODUCE_KLIB_FILE" -> irProduceKlibFile = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_IR_PROPERTY_LAZY_INITIALIZATION" -> irPropertyLazyInitialization = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "X_STRICT_IMPLICIT_EXPORT_TYPES" -> strictImplicitExportType = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "IR_OUTPUT_DIR" -> outputDir = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "IR_OUTPUT_NAME" -> moduleName = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "LIBRARIES" -> libraries = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "MAIN" -> main = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "NOPACK" -> nopack = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP" -> sourceMap = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_BASE_DIRS" -> sourceMapBaseDirs = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_EMBED_SOURCES" -> sourceMapEmbedSources = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_NAMES_POLICY" -> sourceMapNamesPolicy = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      "SOURCE_MAP_PREFIX" -> sourceMapPrefix = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = CommonJsAndWasmArgumentValueAdapter.toImpl(value)
          }
    }
  }

  abstract override fun build(): CommonJsAndWasmArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.cacheDirectory = cacheDirectory?.absolutePathStringOrThrow()
    arguments.enableAdvancedOptimizations = enableAdvancedOptimizations
    arguments.friendModules = friendModules?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.friendModulesDisabled = friendModulesDisabled
    arguments.generateDts = generateDts
    arguments.includes = includes?.absolutePathStringOrThrow()
    arguments.irDce = irDce
    arguments.irDcePrintReachabilityInfo = irDcePrintReachabilityInfo
    arguments.irDceRuntimeDiagnostic = irDceRuntimeDiagnostic?.stringValue
    arguments.irModuleName = irModuleName
    arguments.irPerModuleOutputName = irPerModuleOutputName
    arguments.irProduceJs = irProduceJs
    arguments.irProduceKlibDir = irProduceKlibDir
    arguments.irProduceKlibFile = irProduceKlibFile
    arguments.irPropertyLazyInitialization = irPropertyLazyInitialization
    arguments.strictImplicitExportType = strictImplicitExportType
    arguments.outputDir = outputDir?.absolutePathStringOrThrow()
    arguments.moduleName = moduleName
    arguments.libraries = libraries?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.main = main?.stringValue
    arguments.nopack = nopack
    arguments.sourceMap = sourceMap
    arguments.sourceMapBaseDirs = sourceMapBaseDirs?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.sourceMapEmbedSources = sourceMapEmbedSources?.stringValue
    arguments.sourceMapNamesPolicy = sourceMapNamesPolicy?.stringValue
    arguments.sourceMapPrefix = sourceMapPrefix
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonJsAndWasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
    cacheDirectory = arguments.cacheDirectory?.let { kotlin.io.path.Path(it) }
    enableAdvancedOptimizations = arguments.enableAdvancedOptimizations
    friendModules = arguments.friendModules?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }
    friendModulesDisabled = arguments.friendModulesDisabled
    generateDts = arguments.generateDts
    includes = arguments.includes?.let { kotlin.io.path.Path(it) }
    irDce = arguments.irDce
    irDcePrintReachabilityInfo = arguments.irDcePrintReachabilityInfo
    try { irDceRuntimeDiagnostic = arguments.irDceRuntimeDiagnostic?.let { JsIrDiagnosticMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irDceRuntimeDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-dce-runtime-diagnostic value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    irModuleName = arguments.irModuleName
    irPerModuleOutputName = arguments.irPerModuleOutputName
    irProduceJs = arguments.irProduceJs
    irProduceKlibDir = arguments.irProduceKlibDir
    irProduceKlibFile = arguments.irProduceKlibFile
    irPropertyLazyInitialization = arguments.irPropertyLazyInitialization
    strictImplicitExportType = arguments.strictImplicitExportType
    outputDir = arguments.outputDir?.let { kotlin.io.path.Path(it) }
    moduleName = arguments.moduleName
    libraries = arguments.libraries?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }
    try { main = arguments.main?.let { JsMainCallMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::main, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -main value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    nopack = arguments.nopack
    sourceMap = arguments.sourceMap
    sourceMapBaseDirs = arguments.sourceMapBaseDirs?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }
    try { sourceMapEmbedSources = arguments.sourceMapEmbedSources?.let { SourceMapEmbedSources.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::sourceMapEmbedSources, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-embed-sources value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { sourceMapNamesPolicy = arguments.sourceMapNamesPolicy?.let { SourceMapNamesPolicy.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::sourceMapNamesPolicy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-names-policy value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    sourceMapPrefix = arguments.sourceMapPrefix
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.cacheDirectory = cacheDirectory?.absolutePathStringOrThrow()
    arguments.enableAdvancedOptimizations = enableAdvancedOptimizations
    arguments.friendModules = friendModules?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.friendModulesDisabled = friendModulesDisabled
    arguments.generateDts = generateDts
    arguments.includes = includes?.absolutePathStringOrThrow()
    arguments.irDce = irDce
    arguments.irDceRuntimeDiagnostic = irDceRuntimeDiagnostic?.stringValue
    arguments.irModuleName = irModuleName
    arguments.irPerModuleOutputName = irPerModuleOutputName
    arguments.irProduceJs = irProduceJs
    arguments.irProduceKlibDir = irProduceKlibDir
    arguments.irProduceKlibFile = irProduceKlibFile
    arguments.irPropertyLazyInitialization = irPropertyLazyInitialization
    arguments.strictImplicitExportType = strictImplicitExportType
    arguments.outputDir = outputDir?.absolutePathStringOrThrow()
    arguments.moduleName = moduleName
    arguments.libraries = libraries?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.main = main?.stringValue
    arguments.nopack = nopack
    arguments.sourceMap = sourceMap
    arguments.sourceMapBaseDirs = sourceMapBaseDirs?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.sourceMapEmbedSources = sourceMapEmbedSources?.stringValue
    arguments.sourceMapNamesPolicy = sourceMapNamesPolicy?.stringValue
    arguments.sourceMapPrefix = sourceMapPrefix
    return arguments
  }

  @Suppress("DEPRECATION")
  internal override fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    super.collectRestrictedArgViolations(compilerArgs, defaultArgs)
    val args = compilerArgs as CommonJsAndWasmCompilerArguments
    val castedDefaults = defaultArgs as CommonJsAndWasmCompilerArguments
    if (args.irProduceJs != castedDefaults.irProduceJs) _restrictedArgViolations.add(RestrictedArgViolation.Warning("Argument '-Xir-produce-js' is not supported in the Build Tools API. It is added automatically based on type of operation (klib vs linking). This warning will become an error starting from Kotlin 2.6.0."))
    if (args.outputDir != castedDefaults.outputDir) _restrictedArgViolations.add(RestrictedArgViolation.Warning("Argument '-ir-output-dir' is not supported in the Build Tools API. It is overwritten with the destination property of the build operation.  This warning will become an error starting from Kotlin 2.6.0."))
    if (args.includes != castedDefaults.includes) _restrictedArgViolations.add(RestrictedArgViolation.Warning("Argument '-Xinclude' is not supported in the Build Tools API. It is overwritten with the klib property of the linking operation. This warning will become an error starting from Kotlin 2.6.0."))
    if (args.irProduceKlibDir != castedDefaults.irProduceKlibDir) _restrictedArgViolations.add(RestrictedArgViolation.Warning("Argument '-Xir-produce-klib-dir' is not supported in the Build Tools API. Producing packed/unpacked klib is controlled using the `nopack` argument. This warning will become an error starting from Kotlin 2.6.0."))
    if (args.irProduceKlibFile != castedDefaults.irProduceKlibFile) _restrictedArgViolations.add(RestrictedArgViolation.Warning("Argument '-Xir-produce-klib-file' is not supported in the Build Tools API. Producing packed/unpacked klib is controlled using the `nopack` argument. This warning will become an error starting from Kotlin 2.6.0."))
  }

  public companion object {
    private val knownOptionIds: List<String> =
        listOf("X_CACHE_DIRECTORY","X_ENABLE_ADVANCED_OPTIMIZATIONS","X_FRIEND_MODULES","X_FRIEND_MODULES_DISABLED","X_GENERATE_DTS","X_INCLUDE","X_IR_DCE","X_IR_DCE_PRINT_REACHABILITY_INFO","X_IR_DCE_RUNTIME_DIAGNOSTIC","X_IR_MODULE_NAME","X_IR_PER_MODULE_OUTPUT_NAME","X_IR_PRODUCE_JS","X_IR_PRODUCE_KLIB_DIR","X_IR_PRODUCE_KLIB_FILE","X_IR_PROPERTY_LAZY_INITIALIZATION","X_STRICT_IMPLICIT_EXPORT_TYPES","IR_OUTPUT_DIR","IR_OUTPUT_NAME","LIBRARIES","MAIN","NOPACK","SOURCE_MAP","SOURCE_MAP_BASE_DIRS","SOURCE_MAP_EMBED_SOURCES","SOURCE_MAP_NAMES_POLICY","SOURCE_MAP_PREFIX",)
  }
}
