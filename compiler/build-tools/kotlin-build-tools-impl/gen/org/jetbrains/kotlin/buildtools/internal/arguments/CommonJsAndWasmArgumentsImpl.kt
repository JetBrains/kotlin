// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

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
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.text.split
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SourceMapNamesPolicy
import org.jetbrains.kotlin.buildtools.`internal`.serializability.findPropertyWithSerialName
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

internal abstract class CommonJsAndWasmArgumentsImpl(
  defaultArguments: CommonJsAndWasmCompilerArguments,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonKlibBasedArgumentsImpl(defaultArguments, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    CommonJsAndWasmArguments,
    CommonJsAndWasmArguments.Builder,
    CommonJsAndWasmCompilerKlibArguments,
    CommonJsAndWasmCompilerKlibArguments.Builder,
    CommonJsAndWasmCompilerLinkingArguments,
    CommonJsAndWasmCompilerLinkingArguments.Builder {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_CACHE_DIRECTORY")
  internal var `Xcache-directory`: Path? =
      defaultArguments.cacheDirectory?.let { kotlin.io.path.Path(it) }

  @SerialName("X_ENABLE_ADVANCED_OPTIMIZATIONS")
  internal var `Xenable-advanced-optimizations`: Boolean =
      defaultArguments.enableAdvancedOptimizations

  @SerialName("X_FRIEND_MODULES")
  internal var `Xfriend-modules`: List<Path>? =
      defaultArguments.friendModules?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("X_FRIEND_MODULES_DISABLED")
  internal var `Xfriend-modules-disabled`: Boolean = defaultArguments.friendModulesDisabled

  @SerialName("X_GENERATE_DTS")
  internal var `Xgenerate-dts`: Boolean = defaultArguments.generateDts

  @SerialName("X_INCLUDE")
  internal var Xinclude: Path? = defaultArguments.includes?.let { kotlin.io.path.Path(it) }

  @SerialName("X_IR_DCE")
  internal var `Xir-dce`: Boolean = defaultArguments.irDce

  @SerialName("X_IR_DCE_PRINT_REACHABILITY_INFO")
  internal var `Xir-dce-print-reachability-info`: Boolean =
      defaultArguments.irDcePrintReachabilityInfo

  @SerialName("X_IR_DCE_RUNTIME_DIAGNOSTIC")
  internal var `Xir-dce-runtime-diagnostic`: JsIrDiagnosticMode? =
      defaultArguments.irDceRuntimeDiagnostic?.let { JsIrDiagnosticMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::irDceRuntimeDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-dce-runtime-diagnostic value: $it") }

  @SerialName("X_IR_MODULE_NAME")
  internal var `Xir-module-name`: String? = defaultArguments.irModuleName

  @SerialName("X_IR_PER_MODULE_OUTPUT_NAME")
  internal var `Xir-per-module-output-name`: String? = defaultArguments.irPerModuleOutputName

  @SerialName("X_IR_PRODUCE_JS")
  internal var `Xir-produce-js`: Boolean = defaultArguments.irProduceJs

  @SerialName("X_IR_PRODUCE_KLIB_DIR")
  @Suppress("DEPRECATION")
  internal var `Xir-produce-klib-dir`: Boolean? = defaultArguments.irProduceKlibDir

  @SerialName("X_IR_PRODUCE_KLIB_FILE")
  @Suppress("DEPRECATION")
  internal var `Xir-produce-klib-file`: Boolean? = defaultArguments.irProduceKlibFile

  @SerialName("X_IR_PROPERTY_LAZY_INITIALIZATION")
  internal var `Xir-property-lazy-initialization`: Boolean =
      defaultArguments.irPropertyLazyInitialization

  @SerialName("X_STRICT_IMPLICIT_EXPORT_TYPES")
  internal var `Xstrict-implicit-export-types`: Boolean = defaultArguments.strictImplicitExportType

  @SerialName("IR_OUTPUT_DIR")
  internal var `ir-output-dir`: Path? = defaultArguments.outputDir?.let { kotlin.io.path.Path(it) }

  @SerialName("IR_OUTPUT_NAME")
  internal var `ir-output-name`: String? = defaultArguments.moduleName

  @SerialName("LIBRARIES")
  internal var libraries: List<Path>? =
      defaultArguments.libraries?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("MAIN")
  internal var main: JsMainCallMode? =
      defaultArguments.main?.let { JsMainCallMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::main, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -main value: $it") }

  @SerialName("NOPACK")
  internal var nopack: Boolean = defaultArguments.nopack

  @SerialName("SOURCE_MAP")
  internal var `source-map`: Boolean = defaultArguments.sourceMap

  @SerialName("SOURCE_MAP_BASE_DIRS")
  internal var `source-map-base-dirs`: List<Path>? =
      defaultArguments.sourceMapBaseDirs?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("SOURCE_MAP_EMBED_SOURCES")
  internal var `source-map-embed-sources`: SourceMapEmbedSources? =
      defaultArguments.sourceMapEmbedSources?.let { SourceMapEmbedSources.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::sourceMapEmbedSources, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-embed-sources value: $it") }

  @SerialName("SOURCE_MAP_NAMES_POLICY")
  internal var `source-map-names-policy`: SourceMapNamesPolicy? =
      defaultArguments.sourceMapNamesPolicy?.let { SourceMapNamesPolicy.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::sourceMapNamesPolicy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-names-policy value: $it") }

  @SerialName("SOURCE_MAP_PREFIX")
  internal var `source-map-prefix`: String? = defaultArguments.sourceMapPrefix

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonJsAndWasmArgument<V>): V = CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).getter.call(this) as V

  public operator fun <V> `set`(key: CommonJsAndWasmArgument<V>, `value`: V) {
    CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).setter.call(this, `value`)
  }

  public operator fun contains(key: CommonJsAndWasmArgument<*>): Boolean = true

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>): V = CommonJsAndWasmArgumentValueAdapter.toApi(CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).getter.call(this)) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).setter.call(this, CommonJsAndWasmArgumentValueAdapter.toImpl(value))
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>): V = CommonJsAndWasmArgumentValueAdapter.toApi(CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).getter.call(this)) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).setter.call(this, CommonJsAndWasmArgumentValueAdapter.toImpl(value))
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>): V = CommonJsAndWasmArgumentValueAdapter.toApi(CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).getter.call(this)) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    CommonJsAndWasmArgumentsImpl::class.findPropertyWithSerialName(key.id).setter.call(this, CommonJsAndWasmArgumentValueAdapter.toImpl(value))
  }

  abstract override fun build(): CommonJsAndWasmArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.cacheDirectory = `Xcache-directory`?.absolutePathStringOrThrow()
    arguments.enableAdvancedOptimizations = `Xenable-advanced-optimizations`
    arguments.friendModules = `Xfriend-modules`?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.friendModulesDisabled = `Xfriend-modules-disabled`
    arguments.generateDts = `Xgenerate-dts`
    arguments.includes = Xinclude?.absolutePathStringOrThrow()
    arguments.irDce = `Xir-dce`
    arguments.irDcePrintReachabilityInfo = `Xir-dce-print-reachability-info`
    arguments.irDceRuntimeDiagnostic = `Xir-dce-runtime-diagnostic`?.stringValue
    arguments.irModuleName = `Xir-module-name`
    arguments.irPerModuleOutputName = `Xir-per-module-output-name`
    arguments.irProduceJs = `Xir-produce-js`
    arguments.irProduceKlibDir = `Xir-produce-klib-dir`
    arguments.irProduceKlibFile = `Xir-produce-klib-file`
    arguments.irPropertyLazyInitialization = `Xir-property-lazy-initialization`
    arguments.strictImplicitExportType = `Xstrict-implicit-export-types`
    arguments.outputDir = `ir-output-dir`?.absolutePathStringOrThrow()
    arguments.moduleName = `ir-output-name`
    arguments.libraries = libraries?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.main = main?.stringValue
    arguments.nopack = nopack
    arguments.sourceMap = `source-map`
    arguments.sourceMapBaseDirs = `source-map-base-dirs`?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.sourceMapEmbedSources = `source-map-embed-sources`?.stringValue
    arguments.sourceMapNamesPolicy = `source-map-names-policy`?.stringValue
    arguments.sourceMapPrefix = `source-map-prefix`
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonJsAndWasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { `Xcache-directory` = arguments.cacheDirectory?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xenable-advanced-optimizations` = arguments.enableAdvancedOptimizations } catch (_: NoSuchMethodError) {  }
    try { `Xfriend-modules` = arguments.friendModules?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xfriend-modules-disabled` = arguments.friendModulesDisabled } catch (_: NoSuchMethodError) {  }
    try { `Xgenerate-dts` = arguments.generateDts } catch (_: NoSuchMethodError) {  }
    try { Xinclude = arguments.includes?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xir-dce` = arguments.irDce } catch (_: NoSuchMethodError) {  }
    try { `Xir-dce-print-reachability-info` = arguments.irDcePrintReachabilityInfo } catch (_: NoSuchMethodError) {  }
    try { `Xir-dce-runtime-diagnostic` = arguments.irDceRuntimeDiagnostic?.let { JsIrDiagnosticMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irDceRuntimeDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-dce-runtime-diagnostic value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xir-module-name` = arguments.irModuleName } catch (_: NoSuchMethodError) {  }
    try { `Xir-per-module-output-name` = arguments.irPerModuleOutputName } catch (_: NoSuchMethodError) {  }
    try { `Xir-produce-js` = arguments.irProduceJs } catch (_: NoSuchMethodError) {  }
    try { `Xir-produce-klib-dir` = arguments.irProduceKlibDir } catch (_: NoSuchMethodError) {  }
    try { `Xir-produce-klib-file` = arguments.irProduceKlibFile } catch (_: NoSuchMethodError) {  }
    try { `Xir-property-lazy-initialization` = arguments.irPropertyLazyInitialization } catch (_: NoSuchMethodError) {  }
    try { `Xstrict-implicit-export-types` = arguments.strictImplicitExportType } catch (_: NoSuchMethodError) {  }
    try { `ir-output-dir` = arguments.outputDir?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `ir-output-name` = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { libraries = arguments.libraries?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { main = arguments.main?.let { JsMainCallMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::main, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -main value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { nopack = arguments.nopack } catch (_: NoSuchMethodError) {  }
    try { `source-map` = arguments.sourceMap } catch (_: NoSuchMethodError) {  }
    try { `source-map-base-dirs` = arguments.sourceMapBaseDirs?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `source-map-embed-sources` = arguments.sourceMapEmbedSources?.let { SourceMapEmbedSources.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::sourceMapEmbedSources, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-embed-sources value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `source-map-names-policy` = arguments.sourceMapNamesPolicy?.let { SourceMapNamesPolicy.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::sourceMapNamesPolicy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-names-policy value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `source-map-prefix` = arguments.sourceMapPrefix } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.cacheDirectory = `Xcache-directory`?.absolutePathStringOrThrow()
    arguments.enableAdvancedOptimizations = `Xenable-advanced-optimizations`
    arguments.friendModules = `Xfriend-modules`?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.friendModulesDisabled = `Xfriend-modules-disabled`
    arguments.generateDts = `Xgenerate-dts`
    arguments.includes = Xinclude?.absolutePathStringOrThrow()
    arguments.irDce = `Xir-dce`
    arguments.irDceRuntimeDiagnostic = `Xir-dce-runtime-diagnostic`?.stringValue
    arguments.irModuleName = `Xir-module-name`
    arguments.irPerModuleOutputName = `Xir-per-module-output-name`
    arguments.irProduceJs = `Xir-produce-js`
    arguments.irProduceKlibDir = `Xir-produce-klib-dir`
    arguments.irProduceKlibFile = `Xir-produce-klib-file`
    arguments.irPropertyLazyInitialization = `Xir-property-lazy-initialization`
    arguments.strictImplicitExportType = `Xstrict-implicit-export-types`
    arguments.outputDir = `ir-output-dir`?.absolutePathStringOrThrow()
    arguments.moduleName = `ir-output-name`
    arguments.libraries = libraries?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.main = main?.stringValue
    arguments.nopack = nopack
    arguments.sourceMap = `source-map`
    arguments.sourceMapBaseDirs = `source-map-base-dirs`?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.sourceMapEmbedSources = `source-map-embed-sources`?.stringValue
    arguments.sourceMapNamesPolicy = `source-map-names-policy`?.stringValue
    arguments.sourceMapPrefix = `source-map-prefix`
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

  public class CommonJsAndWasmArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_CACHE_DIRECTORY: CommonJsAndWasmArgument<Path?> =
        CommonJsAndWasmArgument("X_CACHE_DIRECTORY")

    public val X_ENABLE_ADVANCED_OPTIMIZATIONS: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_ENABLE_ADVANCED_OPTIMIZATIONS")

    public val X_FRIEND_MODULES: CommonJsAndWasmArgument<List<Path>?> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES")

    public val X_FRIEND_MODULES_DISABLED: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_FRIEND_MODULES_DISABLED")

    public val X_GENERATE_DTS: CommonJsAndWasmArgument<Boolean> =
        CommonJsAndWasmArgument("X_GENERATE_DTS")

    public val X_INCLUDE: CommonJsAndWasmArgument<Path?> = CommonJsAndWasmArgument("X_INCLUDE")

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

    public val IR_OUTPUT_DIR: CommonJsAndWasmArgument<Path?> =
        CommonJsAndWasmArgument("IR_OUTPUT_DIR")

    public val IR_OUTPUT_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("IR_OUTPUT_NAME")

    public val LIBRARIES: CommonJsAndWasmArgument<List<Path>?> =
        CommonJsAndWasmArgument("LIBRARIES")

    public val MAIN: CommonJsAndWasmArgument<JsMainCallMode?> = CommonJsAndWasmArgument("MAIN")

    public val NOPACK: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("NOPACK")

    public val SOURCE_MAP: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("SOURCE_MAP")

    public val SOURCE_MAP_BASE_DIRS: CommonJsAndWasmArgument<List<Path>?> =
        CommonJsAndWasmArgument("SOURCE_MAP_BASE_DIRS")

    public val SOURCE_MAP_EMBED_SOURCES: CommonJsAndWasmArgument<SourceMapEmbedSources?> =
        CommonJsAndWasmArgument("SOURCE_MAP_EMBED_SOURCES")

    public val SOURCE_MAP_NAMES_POLICY: CommonJsAndWasmArgument<SourceMapNamesPolicy?> =
        CommonJsAndWasmArgument("SOURCE_MAP_NAMES_POLICY")

    public val SOURCE_MAP_PREFIX: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_PREFIX")
  }
}
