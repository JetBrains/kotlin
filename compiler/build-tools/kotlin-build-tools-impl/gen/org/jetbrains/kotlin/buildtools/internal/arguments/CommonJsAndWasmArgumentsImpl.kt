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
import kotlin.collections.mutableSetOf
import kotlin.io.path.Path
import kotlin.text.split
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapNamesPolicy
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonJsAndWasmArgumentsImpl(
  protected override val compilerArguments: CommonJsAndWasmCompilerArguments,
  protected override val optionsMap: MutableMap<String, Any?>,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonKlibBasedArgumentsImpl(compilerArguments, optionsMap, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    CommonJsAndWasmArguments,
    CommonJsAndWasmArguments.Builder,
    CommonJsAndWasmCompilerKlibArguments,
    CommonJsAndWasmCompilerKlibArguments.Builder,
    CommonJsAndWasmCompilerLinkingArguments,
    CommonJsAndWasmCompilerLinkingArguments.Builder {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonJsAndWasmArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: CommonJsAndWasmArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: CommonJsAndWasmArgument<*>): Boolean = isArgumentKnown(key.id) 

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmArguments.CommonJsAndWasmArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerKlibArguments.CommonJsAndWasmCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: CommonJsAndWasmCompilerLinkingArguments.CommonJsAndWasmCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun getOption(keyId: String): Any? = when (keyId) {
    "X_CACHE_DIRECTORY" -> {
    this.compilerArguments.cacheDirectory?.let { Path(it) }
    }
    "X_FRIEND_MODULES" -> {
    this.compilerArguments.friendModules?.split(File.pathSeparator)?.map { Path(it) }
    }
    "X_FRIEND_MODULES_DISABLED" -> {
    this.compilerArguments.friendModulesDisabled
    }
    "X_GENERATE_DTS" -> {
    this.compilerArguments.generateDts
    }
    "X_INCLUDE" -> {
    this.compilerArguments.includes?.let { Path(it) }
    }
    "X_IR_DCE" -> {
    this.compilerArguments.irDce
    }
    "X_IR_DCE_PRINT_REACHABILITY_INFO" -> {
    this.compilerArguments.irDcePrintReachabilityInfo
    }
    "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> {
    this.compilerArguments.irDceRuntimeDiagnostic?.let { JsIrDiagnosticMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::irDceRuntimeDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-dce-runtime-diagnostic value: $it") }
    }
    "X_IR_MODULE_NAME" -> {
    this.compilerArguments.irModuleName
    }
    "X_IR_PER_MODULE_OUTPUT_NAME" -> {
    this.compilerArguments.irPerModuleOutputName
    }
    "X_IR_PRODUCE_JS" -> {
    this.compilerArguments.irProduceJs
    }
    "X_IR_PRODUCE_KLIB_DIR" -> {
    this.compilerArguments.irProduceKlibDir
    }
    "X_IR_PRODUCE_KLIB_FILE" -> {
    this.compilerArguments.irProduceKlibFile
    }
    "X_IR_PROPERTY_LAZY_INITIALIZATION" -> {
    this.compilerArguments.irPropertyLazyInitialization
    }
    "X_STRICT_IMPLICIT_EXPORT_TYPES" -> {
    this.compilerArguments.strictImplicitExportType
    }
    "IR_OUTPUT_DIR" -> {
    this.compilerArguments.outputDir?.let { Path(it) }
    }
    "IR_OUTPUT_NAME" -> {
    this.compilerArguments.moduleName
    }
    "LIBRARIES" -> {
    this.compilerArguments.libraries?.split(File.pathSeparator)?.map { Path(it) }
    }
    "MAIN" -> {
    this.compilerArguments.main?.let { JsMainCallMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::main, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -main value: $it") }
    }
    "NOPACK" -> {
    try {
    this.compilerArguments.nopack
    } catch (_: NoSuchMethodError) { null }
    }
    "SOURCE_MAP" -> {
    this.compilerArguments.sourceMap
    }
    "SOURCE_MAP_BASE_DIRS" -> {
    this.compilerArguments.sourceMapBaseDirs?.split(File.pathSeparator)?.map { Path(it) }
    }
    "SOURCE_MAP_EMBED_SOURCES" -> {
    this.compilerArguments.sourceMapEmbedSources?.let { SourceMapEmbedSources.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::sourceMapEmbedSources, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-embed-sources value: $it") }
    }
    "SOURCE_MAP_NAMES_POLICY" -> {
    this.compilerArguments.sourceMapNamesPolicy?.let { SourceMapNamesPolicy.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::sourceMapNamesPolicy, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -source-map-names-policy value: $it") }
    }
    "SOURCE_MAP_PREFIX" -> {
    this.compilerArguments.sourceMapPrefix
    }
    else -> {
      check(keyId in optionsMap) { "Argument ${keyId} is not set and has no default value" }
      optionsMap[keyId]
    }
  }

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun setOption(keyId: String, `value`: Any?) {
    when (keyId) {
      "X_CACHE_DIRECTORY" -> {
      this.compilerArguments.cacheDirectory = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "X_FRIEND_MODULES" -> {
      this.compilerArguments.friendModules = (value as List<java.nio.`file`.Path>?)?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
      }
      "X_FRIEND_MODULES_DISABLED" -> {
      this.compilerArguments.friendModulesDisabled = (value as Boolean)
      }
      "X_GENERATE_DTS" -> {
      this.compilerArguments.generateDts = (value as Boolean)
      }
      "X_INCLUDE" -> {
      this.compilerArguments.includes = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "X_IR_DCE" -> {
      this.compilerArguments.irDce = (value as Boolean)
      }
      "X_IR_DCE_PRINT_REACHABILITY_INFO" -> {
      this.compilerArguments.irDcePrintReachabilityInfo = (value as Boolean)
      }
      "X_IR_DCE_RUNTIME_DIAGNOSTIC" -> {
      this.compilerArguments.irDceRuntimeDiagnostic = (value as JsIrDiagnosticMode?)?.stringValue
      }
      "X_IR_MODULE_NAME" -> {
      this.compilerArguments.irModuleName = (value as String?)
      }
      "X_IR_PER_MODULE_OUTPUT_NAME" -> {
      this.compilerArguments.irPerModuleOutputName = (value as String?)
      }
      "X_IR_PRODUCE_JS" -> {
      this.compilerArguments.irProduceJs = (value as Boolean)
      }
      "X_IR_PRODUCE_KLIB_DIR" -> {
      this.compilerArguments.irProduceKlibDir = (value as Boolean?)
      }
      "X_IR_PRODUCE_KLIB_FILE" -> {
      this.compilerArguments.irProduceKlibFile = (value as Boolean?)
      }
      "X_IR_PROPERTY_LAZY_INITIALIZATION" -> {
      this.compilerArguments.irPropertyLazyInitialization = (value as Boolean)
      }
      "X_STRICT_IMPLICIT_EXPORT_TYPES" -> {
      this.compilerArguments.strictImplicitExportType = (value as Boolean)
      }
      "IR_OUTPUT_DIR" -> {
      this.compilerArguments.outputDir = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "IR_OUTPUT_NAME" -> {
      this.compilerArguments.moduleName = (value as String?)
      }
      "LIBRARIES" -> {
      this.compilerArguments.libraries = (value as List<java.nio.`file`.Path>?)?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
      }
      "MAIN" -> {
      this.compilerArguments.main = (value as JsMainCallMode?)?.stringValue
      }
      "NOPACK" -> {
      try {
      this.compilerArguments.nopack = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "SOURCE_MAP" -> {
      this.compilerArguments.sourceMap = (value as Boolean)
      }
      "SOURCE_MAP_BASE_DIRS" -> {
      this.compilerArguments.sourceMapBaseDirs = (value as List<java.nio.`file`.Path>?)?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
      }
      "SOURCE_MAP_EMBED_SOURCES" -> {
      this.compilerArguments.sourceMapEmbedSources = (value as SourceMapEmbedSources?)?.stringValue
      }
      "SOURCE_MAP_NAMES_POLICY" -> {
      this.compilerArguments.sourceMapNamesPolicy = (value as SourceMapNamesPolicy?)?.stringValue
      }
      "SOURCE_MAP_PREFIX" -> {
      this.compilerArguments.sourceMapPrefix = (value as String?)
      }
      else -> optionsMap[keyId] = value
    }
  }

  abstract override fun build(): CommonJsAndWasmArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArguments(arguments)
    return arguments
  }

  protected fun applyCompilerArguments(arguments: CommonJsAndWasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonJsAndWasmCompilerArguments): CommonJsAndWasmCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.cacheDirectory = this.compilerArguments.cacheDirectory
    arguments.friendModules = this.compilerArguments.friendModules
    arguments.friendModulesDisabled = this.compilerArguments.friendModulesDisabled
    arguments.generateDts = this.compilerArguments.generateDts
    arguments.includes = this.compilerArguments.includes
    arguments.irDce = this.compilerArguments.irDce
    arguments.irDceRuntimeDiagnostic = this.compilerArguments.irDceRuntimeDiagnostic
    arguments.irModuleName = this.compilerArguments.irModuleName
    arguments.irPerModuleOutputName = this.compilerArguments.irPerModuleOutputName
    arguments.irProduceJs = this.compilerArguments.irProduceJs
    arguments.irProduceKlibDir = this.compilerArguments.irProduceKlibDir
    arguments.irProduceKlibFile = this.compilerArguments.irProduceKlibFile
    arguments.irPropertyLazyInitialization = this.compilerArguments.irPropertyLazyInitialization
    arguments.strictImplicitExportType = this.compilerArguments.strictImplicitExportType
    arguments.outputDir = this.compilerArguments.outputDir
    arguments.moduleName = this.compilerArguments.moduleName
    arguments.libraries = this.compilerArguments.libraries
    arguments.main = this.compilerArguments.main
    arguments.nopack = this.compilerArguments.nopack
    arguments.sourceMap = this.compilerArguments.sourceMap
    arguments.sourceMapBaseDirs = this.compilerArguments.sourceMapBaseDirs
    arguments.sourceMapEmbedSources = this.compilerArguments.sourceMapEmbedSources
    arguments.sourceMapNamesPolicy = this.compilerArguments.sourceMapNamesPolicy
    arguments.sourceMapPrefix = this.compilerArguments.sourceMapPrefix
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

    public val IR_OUTPUT_DIR: CommonJsAndWasmArgument<java.nio.`file`.Path?> =
        CommonJsAndWasmArgument("IR_OUTPUT_DIR")

    public val IR_OUTPUT_NAME: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("IR_OUTPUT_NAME")

    public val LIBRARIES: CommonJsAndWasmArgument<List<java.nio.`file`.Path>?> =
        CommonJsAndWasmArgument("LIBRARIES")

    public val MAIN: CommonJsAndWasmArgument<JsMainCallMode?> = CommonJsAndWasmArgument("MAIN")

    public val NOPACK: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("NOPACK")

    public val SOURCE_MAP: CommonJsAndWasmArgument<Boolean> = CommonJsAndWasmArgument("SOURCE_MAP")

    public val SOURCE_MAP_BASE_DIRS: CommonJsAndWasmArgument<List<java.nio.`file`.Path>?> =
        CommonJsAndWasmArgument("SOURCE_MAP_BASE_DIRS")

    public val SOURCE_MAP_EMBED_SOURCES: CommonJsAndWasmArgument<SourceMapEmbedSources?> =
        CommonJsAndWasmArgument("SOURCE_MAP_EMBED_SOURCES")

    public val SOURCE_MAP_NAMES_POLICY: CommonJsAndWasmArgument<SourceMapNamesPolicy?> =
        CommonJsAndWasmArgument("SOURCE_MAP_NAMES_POLICY")

    public val SOURCE_MAP_PREFIX: CommonJsAndWasmArgument<String?> =
        CommonJsAndWasmArgument("SOURCE_MAP_PREFIX")
  }
}
