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
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.io.path.Path
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WasmTarget
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyKotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class WasmArgumentsImpl(
  protected override val compilerArguments:
      KotlinWasmCompilerArguments = KotlinWasmCompilerArguments(),
  protected override val optionsMap: MutableMap<String, Any?> = mutableMapOf(),
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonJsAndWasmArgumentsImpl(compilerArguments, optionsMap, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    WasmCompilerArguments,
    WasmCompilerArguments.Builder,
    WasmCompilerKlibArguments,
    WasmCompilerKlibArguments.Builder,
    WasmCompilerLinkingArguments,
    WasmCompilerLinkingArguments.Builder,
    DeepCopyable<WasmArgumentsImpl> {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: WasmArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: WasmArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: WasmArgument<*>): Boolean = isArgumentKnown(key.id) 

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerArguments.WasmCompilerArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerArguments.WasmCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerKlibArguments.WasmCompilerKlibArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerKlibArguments.WasmCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerLinkingArguments.WasmCompilerLinkingArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerLinkingArguments.WasmCompilerLinkingArgument<V>, `value`: V) {
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
    "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> {
    this.compilerArguments.irDceDumpReachabilityInfoToFile?.let { Path(it) }
    }
    "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> {
    this.compilerArguments.irDceDumpDeclarationIrSizesToFile?.let { Path(it) }
    }
    "X_WASM" -> {
    this.compilerArguments.wasm
    }
    "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> {
    try {
    this.compilerArguments.regenerateUnchangedModules
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_DEBUG_FRIENDLY" -> {
    this.compilerArguments.forceDebugFriendlyCompilation
    }
    "X_WASM_DEBUG_INFO" -> {
    this.compilerArguments.wasmDebug
    }
    "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> {
    this.compilerArguments.debuggerCustomFormatters
    }
    "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> {
    try {
    this.compilerArguments.wasmDisableArrayRangeChecksSafeElimination
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> {
    this.compilerArguments.wasmEnableArrayRangeChecks
    }
    "X_WASM_ENABLE_ASSERTS" -> {
    this.compilerArguments.wasmEnableAsserts
    }
    "X_WASM_ENABLE_TAIL_CALLS" -> {
    try {
    this.compilerArguments.wasmEnableTailCalls
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> {
    try {
    this.compilerArguments.wasmGenerateClosedWorldMultimodule
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_GENERATE_DWARF" -> {
    this.compilerArguments.generateDwarf
    }
    "X_WASM_GENERATE_WAT" -> {
    this.compilerArguments.wasmGenerateWat
    }
    "X_WASM_INCLUDED_MODULE_ONLY" -> {
    try {
    this.compilerArguments.wasmIncludedModuleOnly
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> {
    try {
    this.compilerArguments.wasmInternalLocalVariablePrefix
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_KCLASS_FQN" -> {
    this.compilerArguments.wasmKClassFqn
    }
    "X_WASM_NO_JSTAG" -> {
    try {
    this.compilerArguments.wasmNoJsTag
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> {
    this.compilerArguments.includeUnavailableSourcesIntoSourceMap
    }
    "X_WASM_TARGET" -> {
    this.compilerArguments.wasmTarget?.let { WasmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::wasmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwasm-target value: $it") }
    }
    "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> {
    this.compilerArguments.wasmUseNewExceptionProposal
    }
    "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> {
    try {
    this.compilerArguments.wasmUseStackSwitchingProposal
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> {
    this.compilerArguments.wasmUseTrapsInsteadOfExceptions
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
      "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> {
      this.compilerArguments.irDceDumpReachabilityInfoToFile = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> {
      this.compilerArguments.irDceDumpDeclarationIrSizesToFile = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "X_WASM" -> {
      this.compilerArguments.wasm = (value as Boolean)
      }
      "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> {
      try {
      this.compilerArguments.regenerateUnchangedModules = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_DEBUG_FRIENDLY" -> {
      this.compilerArguments.forceDebugFriendlyCompilation = (value as Boolean)
      }
      "X_WASM_DEBUG_INFO" -> {
      this.compilerArguments.wasmDebug = (value as Boolean)
      }
      "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> {
      this.compilerArguments.debuggerCustomFormatters = (value as Boolean)
      }
      "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> {
      try {
      this.compilerArguments.wasmDisableArrayRangeChecksSafeElimination = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> {
      this.compilerArguments.wasmEnableArrayRangeChecks = (value as Boolean)
      }
      "X_WASM_ENABLE_ASSERTS" -> {
      this.compilerArguments.wasmEnableAsserts = (value as Boolean)
      }
      "X_WASM_ENABLE_TAIL_CALLS" -> {
      try {
      this.compilerArguments.wasmEnableTailCalls = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> {
      try {
      this.compilerArguments.wasmGenerateClosedWorldMultimodule = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_GENERATE_DWARF" -> {
      this.compilerArguments.generateDwarf = (value as Boolean)
      }
      "X_WASM_GENERATE_WAT" -> {
      this.compilerArguments.wasmGenerateWat = (value as Boolean)
      }
      "X_WASM_INCLUDED_MODULE_ONLY" -> {
      try {
      this.compilerArguments.wasmIncludedModuleOnly = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> {
      try {
      this.compilerArguments.wasmInternalLocalVariablePrefix = (value as String)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_KCLASS_FQN" -> {
      this.compilerArguments.wasmKClassFqn = (value as Boolean)
      }
      "X_WASM_NO_JSTAG" -> {
      try {
      this.compilerArguments.wasmNoJsTag = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> {
      this.compilerArguments.includeUnavailableSourcesIntoSourceMap = (value as Boolean)
      }
      "X_WASM_TARGET" -> {
      this.compilerArguments.wasmTarget = (value as WasmTarget?)?.stringValue
      }
      "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> {
      this.compilerArguments.wasmUseNewExceptionProposal = (value as Boolean?)
      }
      "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> {
      try {
      this.compilerArguments.wasmUseStackSwitchingProposal = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> {
      this.compilerArguments.wasmUseTrapsInsteadOfExceptions = (value as Boolean)
      }
      else -> optionsMap[keyId] = value
    }
  }

  override fun deepCopy(): WasmArgumentsImpl = WasmArgumentsImpl(org.jetbrains.kotlin.cli.common.arguments.copyKotlinWasmCompilerArguments(this.compilerArguments, org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments()).also { newArgs -> newArgs.errors = this.compilerArguments.errors } , optionsMap.toMutableMap(), _argumentValidationErrors.toMutableSet(), restrictedArgViolations.toList(),  argumentParseDiagnostics.copy())

  override fun build(): WasmArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): KotlinWasmCompilerArguments {
    val arguments = copyKotlinWasmCompilerArguments(compilerArguments, KotlinWasmCompilerArguments()).also { newArgs -> newArgs.errors = compilerArguments.errors } 
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filterNot { isArgumentKnown(it) }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    populateExplicitArguments(arguments)
    return arguments
  }

  protected fun applyCompilerArguments(arguments: KotlinWasmCompilerArguments) {
    copyKotlinWasmCompilerArguments(arguments, this.compilerArguments).also { newArgs -> newArgs.errors = arguments.errors } 
    super.applyCompilerArguments(arguments)
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: KotlinWasmCompilerArguments = KotlinWasmCompilerArguments()): KotlinWasmCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.irDceDumpReachabilityInfoToFile = this.compilerArguments.irDceDumpReachabilityInfoToFile
    arguments.irDceDumpDeclarationIrSizesToFile = this.compilerArguments.irDceDumpDeclarationIrSizesToFile
    arguments.wasm = this.compilerArguments.wasm
    arguments.regenerateUnchangedModules = this.compilerArguments.regenerateUnchangedModules
    arguments.forceDebugFriendlyCompilation = this.compilerArguments.forceDebugFriendlyCompilation
    arguments.wasmDebug = this.compilerArguments.wasmDebug
    arguments.debuggerCustomFormatters = this.compilerArguments.debuggerCustomFormatters
    arguments.wasmDisableArrayRangeChecksSafeElimination = this.compilerArguments.wasmDisableArrayRangeChecksSafeElimination
    arguments.wasmEnableArrayRangeChecks = this.compilerArguments.wasmEnableArrayRangeChecks
    arguments.wasmEnableAsserts = this.compilerArguments.wasmEnableAsserts
    arguments.wasmEnableTailCalls = this.compilerArguments.wasmEnableTailCalls
    arguments.wasmGenerateClosedWorldMultimodule = this.compilerArguments.wasmGenerateClosedWorldMultimodule
    arguments.generateDwarf = this.compilerArguments.generateDwarf
    arguments.wasmGenerateWat = this.compilerArguments.wasmGenerateWat
    arguments.wasmIncludedModuleOnly = this.compilerArguments.wasmIncludedModuleOnly
    arguments.wasmInternalLocalVariablePrefix = this.compilerArguments.wasmInternalLocalVariablePrefix
    arguments.wasmKClassFqn = this.compilerArguments.wasmKClassFqn
    arguments.wasmNoJsTag = this.compilerArguments.wasmNoJsTag
    arguments.includeUnavailableSourcesIntoSourceMap = this.compilerArguments.includeUnavailableSourcesIntoSourceMap
    arguments.wasmTarget = this.compilerArguments.wasmTarget
    arguments.wasmUseNewExceptionProposal = this.compilerArguments.wasmUseNewExceptionProposal
    arguments.wasmUseStackSwitchingProposal = this.compilerArguments.wasmUseStackSwitchingProposal
    arguments.wasmUseTrapsInsteadOfExceptions = this.compilerArguments.wasmUseTrapsInsteadOfExceptions
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: KotlinWasmCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, KotlinWasmCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)
    return arguments
  }

  /**
   * Returns a sorted list of compiler argument strings representing only the arguments
   * that affect the compilation outcome (i.e. those with [affectsCompilationOutcome][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument.affectsCompilationOutcome] set to true).
   * Arguments with default values are omitted from the output, because [toCompilerArgumentsAffectingOutcome]
   * only sets arguments that have been explicitly assigned, and [compilerToArgumentStrings][org.jetbrains.kotlin.compilerRunner.toArgumentStrings]
   * skips properties whose value matches the default.
   */
  public fun toCompilationInputs(): List<String> = toCompilerArgumentsAffectingOutcome().compilerToArgumentStrings(allowArgFileInValues = false).sorted()

  public class WasmArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE: WasmArgument<java.nio.`file`.Path?> =
        WasmArgument("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")

    public val X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE: WasmArgument<java.nio.`file`.Path?> =
        WasmArgument("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE")

    public val X_WASM: WasmArgument<Boolean> = WasmArgument("X_WASM")

    public val X_WASM_IC_GENERATE_UNCHANGED_MODULES: WasmArgument<Boolean> =
        WasmArgument("X_WASM_IC_GENERATE_UNCHANGED_MODULES")

    public val X_WASM_DEBUG_FRIENDLY: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_FRIENDLY")

    public val X_WASM_DEBUG_INFO: WasmArgument<Boolean> = WasmArgument("X_WASM_DEBUG_INFO")

    public val X_WASM_DEBUGGER_CUSTOM_FORMATTERS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_DEBUGGER_CUSTOM_FORMATTERS")

    public val X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION: WasmArgument<Boolean> =
        WasmArgument("X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION")

    public val X_WASM_ENABLE_ARRAY_RANGE_CHECKS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_ENABLE_ARRAY_RANGE_CHECKS")

    public val X_WASM_ENABLE_ASSERTS: WasmArgument<Boolean> = WasmArgument("X_WASM_ENABLE_ASSERTS")

    public val X_WASM_ENABLE_TAIL_CALLS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_ENABLE_TAIL_CALLS")

    public val X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE: WasmArgument<Boolean> =
        WasmArgument("X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE")

    public val X_WASM_GENERATE_DWARF: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_DWARF")

    public val X_WASM_GENERATE_WAT: WasmArgument<Boolean> = WasmArgument("X_WASM_GENERATE_WAT")

    public val X_WASM_INCLUDED_MODULE_ONLY: WasmArgument<Boolean> =
        WasmArgument("X_WASM_INCLUDED_MODULE_ONLY")

    public val X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX: WasmArgument<String> =
        WasmArgument("X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX")

    public val X_WASM_KCLASS_FQN: WasmArgument<Boolean> = WasmArgument("X_WASM_KCLASS_FQN")

    public val X_WASM_NO_JSTAG: WasmArgument<Boolean> = WasmArgument("X_WASM_NO_JSTAG")

    public val X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES: WasmArgument<Boolean> =
        WasmArgument("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES")

    public val X_WASM_TARGET: WasmArgument<WasmTarget?> = WasmArgument("X_WASM_TARGET")

    public val X_WASM_USE_NEW_EXCEPTION_PROPOSAL: WasmArgument<Boolean?> =
        WasmArgument("X_WASM_USE_NEW_EXCEPTION_PROPOSAL")

    public val X_WASM_USE_STACK_SWITCHING_PROPOSAL: WasmArgument<Boolean> =
        WasmArgument("X_WASM_USE_STACK_SWITCHING_PROPOSAL")

    public val X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS: WasmArgument<Boolean> =
        WasmArgument("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")
  }
}
