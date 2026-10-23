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
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.buildMap
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.reflect.KMutableProperty1
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
import org.jetbrains.kotlin.buildtools.`internal`.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.`internal`.serializability.setPropertyByName
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
  override operator fun <V> `get`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>): V = try { CommonJsAndWasmArgumentValueAdapter.toApi(getPropertyByName(propertyAccessMap, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { setPropertyByName(propertyAccessMap, key.id, CommonJsAndWasmArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>): V = try { CommonJsAndWasmArgumentValueAdapter.toApi(getPropertyByName(propertyAccessMap, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { setPropertyByName(propertyAccessMap, key.id, CommonJsAndWasmArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>): V = try { CommonJsAndWasmArgumentValueAdapter.toApi(getPropertyByName(propertyAccessMap, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { setPropertyByName(propertyAccessMap, key.id, CommonJsAndWasmArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
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
    private val propertyAccessMap: Map<String, KMutableProperty1<*, *>> = buildMap {
      put("X_CACHE_DIRECTORY", CommonJsAndWasmArgumentsImpl::cacheDirectory)
      put("X_ENABLE_ADVANCED_OPTIMIZATIONS", CommonJsAndWasmArgumentsImpl::enableAdvancedOptimizations)
      put("X_FRIEND_MODULES", CommonJsAndWasmArgumentsImpl::friendModules)
      put("X_FRIEND_MODULES_DISABLED", CommonJsAndWasmArgumentsImpl::friendModulesDisabled)
      put("X_GENERATE_DTS", CommonJsAndWasmArgumentsImpl::generateDts)
      put("X_INCLUDE", CommonJsAndWasmArgumentsImpl::includes)
      put("X_IR_DCE", CommonJsAndWasmArgumentsImpl::irDce)
      put("X_IR_DCE_PRINT_REACHABILITY_INFO", CommonJsAndWasmArgumentsImpl::irDcePrintReachabilityInfo)
      put("X_IR_DCE_RUNTIME_DIAGNOSTIC", CommonJsAndWasmArgumentsImpl::irDceRuntimeDiagnostic)
      put("X_IR_MODULE_NAME", CommonJsAndWasmArgumentsImpl::irModuleName)
      put("X_IR_PER_MODULE_OUTPUT_NAME", CommonJsAndWasmArgumentsImpl::irPerModuleOutputName)
      put("X_IR_PRODUCE_JS", CommonJsAndWasmArgumentsImpl::irProduceJs)
      put("X_IR_PRODUCE_KLIB_DIR", CommonJsAndWasmArgumentsImpl::irProduceKlibDir)
      put("X_IR_PRODUCE_KLIB_FILE", CommonJsAndWasmArgumentsImpl::irProduceKlibFile)
      put("X_IR_PROPERTY_LAZY_INITIALIZATION", CommonJsAndWasmArgumentsImpl::irPropertyLazyInitialization)
      put("X_STRICT_IMPLICIT_EXPORT_TYPES", CommonJsAndWasmArgumentsImpl::strictImplicitExportType)
      put("IR_OUTPUT_DIR", CommonJsAndWasmArgumentsImpl::outputDir)
      put("IR_OUTPUT_NAME", CommonJsAndWasmArgumentsImpl::moduleName)
      put("LIBRARIES", CommonJsAndWasmArgumentsImpl::libraries)
      put("MAIN", CommonJsAndWasmArgumentsImpl::main)
      put("NOPACK", CommonJsAndWasmArgumentsImpl::nopack)
      put("SOURCE_MAP", CommonJsAndWasmArgumentsImpl::sourceMap)
      put("SOURCE_MAP_BASE_DIRS", CommonJsAndWasmArgumentsImpl::sourceMapBaseDirs)
      put("SOURCE_MAP_EMBED_SOURCES", CommonJsAndWasmArgumentsImpl::sourceMapEmbedSources)
      put("SOURCE_MAP_NAMES_POLICY", CommonJsAndWasmArgumentsImpl::sourceMapNamesPolicy)
      put("SOURCE_MAP_PREFIX", CommonJsAndWasmArgumentsImpl::sourceMapPrefix)
    }
  }
}
