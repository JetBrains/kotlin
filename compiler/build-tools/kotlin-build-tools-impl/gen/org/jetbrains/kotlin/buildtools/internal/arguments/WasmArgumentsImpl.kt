// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import java.nio.`file`.Path
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.listOf
import kotlin.collections.mutableMapOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal class WasmArgumentsImpl() : CommonJsAndWasmArgumentsImpl(), WasmCompilerArguments,
    WasmCompilerArguments.Builder, WasmCompilerKlibArguments, WasmCompilerKlibArguments.Builder,
    WasmCompilerLinkingArguments, WasmCompilerLinkingArguments.Builder,
    DeepCopyable<WasmArgumentsImpl> {
  @Transient
  private val defaultArguments: KotlinWasmCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")
  internal var irDceDumpReachabilityInfoToFile: Path? =
      defaultArguments.irDceDumpReachabilityInfoToFile?.let { kotlin.io.path.Path(it) }

  @SerialName("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE")
  internal var irDceDumpDeclarationIrSizesToFile: Path? =
      defaultArguments.irDceDumpDeclarationIrSizesToFile?.let { kotlin.io.path.Path(it) }

  @SerialName("X_WASM")
  @Suppress("DEPRECATION")
  internal var wasm: Boolean = defaultArguments.wasm

  @SerialName("X_WASM_IC_GENERATE_UNCHANGED_MODULES")
  internal var regenerateUnchangedModules: Boolean = defaultArguments.regenerateUnchangedModules

  @SerialName("X_WASM_DEBUG_FRIENDLY")
  internal var forceDebugFriendlyCompilation: Boolean =
      defaultArguments.forceDebugFriendlyCompilation

  @SerialName("X_WASM_DEBUG_INFO")
  internal var wasmDebug: Boolean = defaultArguments.wasmDebug

  @SerialName("X_WASM_DEBUGGER_CUSTOM_FORMATTERS")
  internal var debuggerCustomFormatters: Boolean = defaultArguments.debuggerCustomFormatters

  @SerialName("X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION")
  internal var wasmDisableArrayRangeChecksSafeElimination: Boolean =
      defaultArguments.wasmDisableArrayRangeChecksSafeElimination

  @SerialName("X_WASM_ENABLE_ARRAY_RANGE_CHECKS")
  internal var wasmEnableArrayRangeChecks: Boolean = defaultArguments.wasmEnableArrayRangeChecks

  @SerialName("X_WASM_ENABLE_ASSERTS")
  internal var wasmEnableAsserts: Boolean = defaultArguments.wasmEnableAsserts

  @SerialName("X_WASM_ENABLE_TAIL_CALLS")
  internal var wasmEnableTailCalls: Boolean = defaultArguments.wasmEnableTailCalls

  @SerialName("X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE")
  internal var wasmGenerateClosedWorldMultimodule: Boolean =
      defaultArguments.wasmGenerateClosedWorldMultimodule

  @SerialName("X_WASM_GENERATE_DWARF")
  internal var generateDwarf: Boolean = defaultArguments.generateDwarf

  @SerialName("X_WASM_GENERATE_WAT")
  internal var wasmGenerateWat: Boolean = defaultArguments.wasmGenerateWat

  @SerialName("X_WASM_INCLUDED_MODULE_ONLY")
  internal var wasmIncludedModuleOnly: Boolean = defaultArguments.wasmIncludedModuleOnly

  @SerialName("X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX")
  internal var wasmInternalLocalVariablePrefix: String =
      defaultArguments.wasmInternalLocalVariablePrefix

  @SerialName("X_WASM_KCLASS_FQN")
  internal var wasmKClassFqn: Boolean = defaultArguments.wasmKClassFqn

  @SerialName("X_WASM_NO_JSTAG")
  internal var wasmNoJsTag: Boolean = defaultArguments.wasmNoJsTag

  @SerialName("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES")
  internal var includeUnavailableSourcesIntoSourceMap: Boolean =
      defaultArguments.includeUnavailableSourcesIntoSourceMap

  @SerialName("X_WASM_TARGET")
  internal var wasmTarget: WasmTarget? =
      defaultArguments.wasmTarget?.let { WasmTarget.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::wasmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwasm-target value: $it") }

  @SerialName("X_WASM_USE_NEW_EXCEPTION_PROPOSAL")
  internal var wasmUseNewExceptionProposal: Boolean? = defaultArguments.wasmUseNewExceptionProposal

  @SerialName("X_WASM_USE_STACK_SWITCHING_PROPOSAL")
  internal var wasmUseStackSwitchingProposal: Boolean =
      defaultArguments.wasmUseStackSwitchingProposal

  @SerialName("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")
  internal var wasmUseTrapsInsteadOfExceptions: Boolean =
      defaultArguments.wasmUseTrapsInsteadOfExceptions

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  override fun createDefaultArguments(): KotlinWasmCompilerArguments = KotlinWasmCompilerArguments()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerArguments.WasmCompilerArgument<V>): V = WasmArgumentValueAdapter.toApi(when (key.id) {
    "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> irDceDumpReachabilityInfoToFile
    "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> irDceDumpDeclarationIrSizesToFile
    "X_WASM" -> wasm
    "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> regenerateUnchangedModules
    "X_WASM_DEBUG_FRIENDLY" -> forceDebugFriendlyCompilation
    "X_WASM_DEBUG_INFO" -> wasmDebug
    "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> debuggerCustomFormatters
    "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> wasmDisableArrayRangeChecksSafeElimination
    "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> wasmEnableArrayRangeChecks
    "X_WASM_ENABLE_ASSERTS" -> wasmEnableAsserts
    "X_WASM_ENABLE_TAIL_CALLS" -> wasmEnableTailCalls
    "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> wasmGenerateClosedWorldMultimodule
    "X_WASM_GENERATE_DWARF" -> generateDwarf
    "X_WASM_GENERATE_WAT" -> wasmGenerateWat
    "X_WASM_INCLUDED_MODULE_ONLY" -> wasmIncludedModuleOnly
    "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> wasmInternalLocalVariablePrefix
    "X_WASM_KCLASS_FQN" -> wasmKClassFqn
    "X_WASM_NO_JSTAG" -> wasmNoJsTag
    "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> includeUnavailableSourcesIntoSourceMap
    "X_WASM_TARGET" -> wasmTarget
    "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> wasmUseNewExceptionProposal
    "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> wasmUseStackSwitchingProposal
    "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> wasmUseTrapsInsteadOfExceptions
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerArguments.WasmCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> irDceDumpReachabilityInfoToFile = WasmArgumentValueAdapter.toImpl(value)
      "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> irDceDumpDeclarationIrSizesToFile = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM" -> wasm = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> regenerateUnchangedModules = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUG_FRIENDLY" -> forceDebugFriendlyCompilation = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUG_INFO" -> wasmDebug = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> debuggerCustomFormatters = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> wasmDisableArrayRangeChecksSafeElimination = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> wasmEnableArrayRangeChecks = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_ASSERTS" -> wasmEnableAsserts = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_TAIL_CALLS" -> wasmEnableTailCalls = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> wasmGenerateClosedWorldMultimodule = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_DWARF" -> generateDwarf = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_WAT" -> wasmGenerateWat = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_INCLUDED_MODULE_ONLY" -> wasmIncludedModuleOnly = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> wasmInternalLocalVariablePrefix = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_KCLASS_FQN" -> wasmKClassFqn = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_NO_JSTAG" -> wasmNoJsTag = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> includeUnavailableSourcesIntoSourceMap = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_TARGET" -> wasmTarget = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> wasmUseNewExceptionProposal = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> wasmUseStackSwitchingProposal = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> wasmUseTrapsInsteadOfExceptions = WasmArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = WasmArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerKlibArguments.WasmCompilerKlibArgument<V>): V = WasmArgumentValueAdapter.toApi(when (key.id) {
    "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> irDceDumpReachabilityInfoToFile
    "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> irDceDumpDeclarationIrSizesToFile
    "X_WASM" -> wasm
    "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> regenerateUnchangedModules
    "X_WASM_DEBUG_FRIENDLY" -> forceDebugFriendlyCompilation
    "X_WASM_DEBUG_INFO" -> wasmDebug
    "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> debuggerCustomFormatters
    "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> wasmDisableArrayRangeChecksSafeElimination
    "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> wasmEnableArrayRangeChecks
    "X_WASM_ENABLE_ASSERTS" -> wasmEnableAsserts
    "X_WASM_ENABLE_TAIL_CALLS" -> wasmEnableTailCalls
    "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> wasmGenerateClosedWorldMultimodule
    "X_WASM_GENERATE_DWARF" -> generateDwarf
    "X_WASM_GENERATE_WAT" -> wasmGenerateWat
    "X_WASM_INCLUDED_MODULE_ONLY" -> wasmIncludedModuleOnly
    "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> wasmInternalLocalVariablePrefix
    "X_WASM_KCLASS_FQN" -> wasmKClassFqn
    "X_WASM_NO_JSTAG" -> wasmNoJsTag
    "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> includeUnavailableSourcesIntoSourceMap
    "X_WASM_TARGET" -> wasmTarget
    "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> wasmUseNewExceptionProposal
    "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> wasmUseStackSwitchingProposal
    "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> wasmUseTrapsInsteadOfExceptions
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerKlibArguments.WasmCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> irDceDumpReachabilityInfoToFile = WasmArgumentValueAdapter.toImpl(value)
      "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> irDceDumpDeclarationIrSizesToFile = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM" -> wasm = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> regenerateUnchangedModules = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUG_FRIENDLY" -> forceDebugFriendlyCompilation = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUG_INFO" -> wasmDebug = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> debuggerCustomFormatters = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> wasmDisableArrayRangeChecksSafeElimination = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> wasmEnableArrayRangeChecks = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_ASSERTS" -> wasmEnableAsserts = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_TAIL_CALLS" -> wasmEnableTailCalls = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> wasmGenerateClosedWorldMultimodule = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_DWARF" -> generateDwarf = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_WAT" -> wasmGenerateWat = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_INCLUDED_MODULE_ONLY" -> wasmIncludedModuleOnly = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> wasmInternalLocalVariablePrefix = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_KCLASS_FQN" -> wasmKClassFqn = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_NO_JSTAG" -> wasmNoJsTag = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> includeUnavailableSourcesIntoSourceMap = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_TARGET" -> wasmTarget = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> wasmUseNewExceptionProposal = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> wasmUseStackSwitchingProposal = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> wasmUseTrapsInsteadOfExceptions = WasmArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = WasmArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerLinkingArguments.WasmCompilerLinkingArgument<V>): V = WasmArgumentValueAdapter.toApi(when (key.id) {
    "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> irDceDumpReachabilityInfoToFile
    "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> irDceDumpDeclarationIrSizesToFile
    "X_WASM" -> wasm
    "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> regenerateUnchangedModules
    "X_WASM_DEBUG_FRIENDLY" -> forceDebugFriendlyCompilation
    "X_WASM_DEBUG_INFO" -> wasmDebug
    "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> debuggerCustomFormatters
    "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> wasmDisableArrayRangeChecksSafeElimination
    "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> wasmEnableArrayRangeChecks
    "X_WASM_ENABLE_ASSERTS" -> wasmEnableAsserts
    "X_WASM_ENABLE_TAIL_CALLS" -> wasmEnableTailCalls
    "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> wasmGenerateClosedWorldMultimodule
    "X_WASM_GENERATE_DWARF" -> generateDwarf
    "X_WASM_GENERATE_WAT" -> wasmGenerateWat
    "X_WASM_INCLUDED_MODULE_ONLY" -> wasmIncludedModuleOnly
    "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> wasmInternalLocalVariablePrefix
    "X_WASM_KCLASS_FQN" -> wasmKClassFqn
    "X_WASM_NO_JSTAG" -> wasmNoJsTag
    "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> includeUnavailableSourcesIntoSourceMap
    "X_WASM_TARGET" -> wasmTarget
    "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> wasmUseNewExceptionProposal
    "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> wasmUseStackSwitchingProposal
    "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> wasmUseTrapsInsteadOfExceptions
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerLinkingArguments.WasmCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE" -> irDceDumpReachabilityInfoToFile = WasmArgumentValueAdapter.toImpl(value)
      "X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE" -> irDceDumpDeclarationIrSizesToFile = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM" -> wasm = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_IC_GENERATE_UNCHANGED_MODULES" -> regenerateUnchangedModules = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUG_FRIENDLY" -> forceDebugFriendlyCompilation = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUG_INFO" -> wasmDebug = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DEBUGGER_CUSTOM_FORMATTERS" -> debuggerCustomFormatters = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION" -> wasmDisableArrayRangeChecksSafeElimination = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_ARRAY_RANGE_CHECKS" -> wasmEnableArrayRangeChecks = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_ASSERTS" -> wasmEnableAsserts = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_ENABLE_TAIL_CALLS" -> wasmEnableTailCalls = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE" -> wasmGenerateClosedWorldMultimodule = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_DWARF" -> generateDwarf = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_GENERATE_WAT" -> wasmGenerateWat = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_INCLUDED_MODULE_ONLY" -> wasmIncludedModuleOnly = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX" -> wasmInternalLocalVariablePrefix = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_KCLASS_FQN" -> wasmKClassFqn = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_NO_JSTAG" -> wasmNoJsTag = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES" -> includeUnavailableSourcesIntoSourceMap = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_TARGET" -> wasmTarget = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_NEW_EXCEPTION_PROPOSAL" -> wasmUseNewExceptionProposal = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_STACK_SWITCHING_PROPOSAL" -> wasmUseStackSwitchingProposal = WasmArgumentValueAdapter.toImpl(value)
      "X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS" -> wasmUseTrapsInsteadOfExceptions = WasmArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = WasmArgumentValueAdapter.toImpl(value)
          }
    }
  }

  override fun deepCopy(): WasmArgumentsImpl = WasmArgumentsImpl(argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): WasmArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): KotlinWasmCompilerArguments {
    val arguments = KotlinWasmCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.irDceDumpReachabilityInfoToFile = irDceDumpReachabilityInfoToFile?.absolutePathStringOrThrow()
    arguments.irDceDumpDeclarationIrSizesToFile = irDceDumpDeclarationIrSizesToFile?.absolutePathStringOrThrow()
    arguments.wasm = wasm
    arguments.regenerateUnchangedModules = regenerateUnchangedModules
    arguments.forceDebugFriendlyCompilation = forceDebugFriendlyCompilation
    arguments.wasmDebug = wasmDebug
    arguments.debuggerCustomFormatters = debuggerCustomFormatters
    arguments.wasmDisableArrayRangeChecksSafeElimination = wasmDisableArrayRangeChecksSafeElimination
    arguments.wasmEnableArrayRangeChecks = wasmEnableArrayRangeChecks
    arguments.wasmEnableAsserts = wasmEnableAsserts
    arguments.wasmEnableTailCalls = wasmEnableTailCalls
    arguments.wasmGenerateClosedWorldMultimodule = wasmGenerateClosedWorldMultimodule
    arguments.generateDwarf = generateDwarf
    arguments.wasmGenerateWat = wasmGenerateWat
    arguments.wasmIncludedModuleOnly = wasmIncludedModuleOnly
    arguments.wasmInternalLocalVariablePrefix = wasmInternalLocalVariablePrefix
    arguments.wasmKClassFqn = wasmKClassFqn
    arguments.wasmNoJsTag = wasmNoJsTag
    arguments.includeUnavailableSourcesIntoSourceMap = includeUnavailableSourcesIntoSourceMap
    arguments.wasmTarget = wasmTarget?.stringValue
    arguments.wasmUseNewExceptionProposal = wasmUseNewExceptionProposal
    arguments.wasmUseStackSwitchingProposal = wasmUseStackSwitchingProposal
    arguments.wasmUseTrapsInsteadOfExceptions = wasmUseTrapsInsteadOfExceptions
    arguments.internalArguments = parseCommandLineArguments<KotlinWasmCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: KotlinWasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
    irDceDumpReachabilityInfoToFile = arguments.irDceDumpReachabilityInfoToFile?.let { kotlin.io.path.Path(it) }
    irDceDumpDeclarationIrSizesToFile = arguments.irDceDumpDeclarationIrSizesToFile?.let { kotlin.io.path.Path(it) }
    wasm = arguments.wasm
    regenerateUnchangedModules = arguments.regenerateUnchangedModules
    forceDebugFriendlyCompilation = arguments.forceDebugFriendlyCompilation
    wasmDebug = arguments.wasmDebug
    debuggerCustomFormatters = arguments.debuggerCustomFormatters
    wasmDisableArrayRangeChecksSafeElimination = arguments.wasmDisableArrayRangeChecksSafeElimination
    wasmEnableArrayRangeChecks = arguments.wasmEnableArrayRangeChecks
    wasmEnableAsserts = arguments.wasmEnableAsserts
    wasmEnableTailCalls = arguments.wasmEnableTailCalls
    wasmGenerateClosedWorldMultimodule = arguments.wasmGenerateClosedWorldMultimodule
    generateDwarf = arguments.generateDwarf
    wasmGenerateWat = arguments.wasmGenerateWat
    wasmIncludedModuleOnly = arguments.wasmIncludedModuleOnly
    wasmInternalLocalVariablePrefix = arguments.wasmInternalLocalVariablePrefix
    wasmKClassFqn = arguments.wasmKClassFqn
    wasmNoJsTag = arguments.wasmNoJsTag
    includeUnavailableSourcesIntoSourceMap = arguments.includeUnavailableSourcesIntoSourceMap
    try { wasmTarget = arguments.wasmTarget?.let { WasmTarget.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::wasmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwasm-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    wasmUseNewExceptionProposal = arguments.wasmUseNewExceptionProposal
    wasmUseStackSwitchingProposal = arguments.wasmUseStackSwitchingProposal
    wasmUseTrapsInsteadOfExceptions = arguments.wasmUseTrapsInsteadOfExceptions
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: KotlinWasmCompilerArguments = KotlinWasmCompilerArguments()): KotlinWasmCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.irDceDumpReachabilityInfoToFile = irDceDumpReachabilityInfoToFile?.absolutePathStringOrThrow()
    arguments.irDceDumpDeclarationIrSizesToFile = irDceDumpDeclarationIrSizesToFile?.absolutePathStringOrThrow()
    arguments.wasm = wasm
    arguments.regenerateUnchangedModules = regenerateUnchangedModules
    arguments.forceDebugFriendlyCompilation = forceDebugFriendlyCompilation
    arguments.wasmDebug = wasmDebug
    arguments.debuggerCustomFormatters = debuggerCustomFormatters
    arguments.wasmDisableArrayRangeChecksSafeElimination = wasmDisableArrayRangeChecksSafeElimination
    arguments.wasmEnableArrayRangeChecks = wasmEnableArrayRangeChecks
    arguments.wasmEnableAsserts = wasmEnableAsserts
    arguments.wasmEnableTailCalls = wasmEnableTailCalls
    arguments.wasmGenerateClosedWorldMultimodule = wasmGenerateClosedWorldMultimodule
    arguments.generateDwarf = generateDwarf
    arguments.wasmGenerateWat = wasmGenerateWat
    arguments.wasmIncludedModuleOnly = wasmIncludedModuleOnly
    arguments.wasmInternalLocalVariablePrefix = wasmInternalLocalVariablePrefix
    arguments.wasmKClassFqn = wasmKClassFqn
    arguments.wasmNoJsTag = wasmNoJsTag
    arguments.includeUnavailableSourcesIntoSourceMap = includeUnavailableSourcesIntoSourceMap
    arguments.wasmTarget = wasmTarget?.stringValue
    arguments.wasmUseNewExceptionProposal = wasmUseNewExceptionProposal
    arguments.wasmUseStackSwitchingProposal = wasmUseStackSwitchingProposal
    arguments.wasmUseTrapsInsteadOfExceptions = wasmUseTrapsInsteadOfExceptions
    return arguments
  }

  @Deprecated(
    message = "This method is deprecated. Use applyCommandLineArguments instead.",
    level = DeprecationLevel.WARNING,
  )
  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: KotlinWasmCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, KotlinWasmCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  @DelicateBuildToolsApi
  override fun applyCommandLineArguments(arguments: List<String>) {
    val compilerArgs = toCompilerArguments()
    parseCommandLineArguments(arguments, compilerArgs, false)
    handleCustomPluginArguments(this, compilerArgs)
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

  public companion object {
    private val knownOptionIds: List<String> =
        listOf("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE","X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE","X_WASM","X_WASM_IC_GENERATE_UNCHANGED_MODULES","X_WASM_DEBUG_FRIENDLY","X_WASM_DEBUG_INFO","X_WASM_DEBUGGER_CUSTOM_FORMATTERS","X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION","X_WASM_ENABLE_ARRAY_RANGE_CHECKS","X_WASM_ENABLE_ASSERTS","X_WASM_ENABLE_TAIL_CALLS","X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE","X_WASM_GENERATE_DWARF","X_WASM_GENERATE_WAT","X_WASM_INCLUDED_MODULE_ONLY","X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX","X_WASM_KCLASS_FQN","X_WASM_NO_JSTAG","X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES","X_WASM_TARGET","X_WASM_USE_NEW_EXCEPTION_PROPOSAL","X_WASM_USE_STACK_SWITCHING_PROPOSAL","X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS",)
  }
}
