// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

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
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.WasmTarget
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

internal class WasmArgumentsImpl(
  defaultArguments: KotlinWasmCompilerArguments = KotlinWasmCompilerArguments(),
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonJsAndWasmArgumentsImpl(defaultArguments, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    WasmCompilerArguments,
    WasmCompilerArguments.Builder,
    WasmCompilerKlibArguments,
    WasmCompilerKlibArguments.Builder,
    WasmCompilerLinkingArguments,
    WasmCompilerLinkingArguments.Builder,
    DeepCopyable<WasmArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")
  protected var `Xir-dce-dump-reachability-info-to-file`: Path? =
      defaultArguments.irDceDumpReachabilityInfoToFile?.let { kotlin.io.path.Path(it) }

  @SerialName("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE")
  protected var `Xir-dump-declaration-ir-sizes-to-file`: Path? =
      defaultArguments.irDceDumpDeclarationIrSizesToFile?.let { kotlin.io.path.Path(it) }

  @SerialName("X_WASM")
  protected var Xwasm: Boolean = defaultArguments.wasm

  @SerialName("X_WASM_IC_GENERATE_UNCHANGED_MODULES")
  protected var `Xwasm-IC-generate-unchanged-modules`: Boolean =
      defaultArguments.regenerateUnchangedModules

  @SerialName("X_WASM_DEBUG_FRIENDLY")
  protected var `Xwasm-debug-friendly`: Boolean = defaultArguments.forceDebugFriendlyCompilation

  @SerialName("X_WASM_DEBUG_INFO")
  protected var `Xwasm-debug-info`: Boolean = defaultArguments.wasmDebug

  @SerialName("X_WASM_DEBUGGER_CUSTOM_FORMATTERS")
  protected var `Xwasm-debugger-custom-formatters`: Boolean =
      defaultArguments.debuggerCustomFormatters

  @SerialName("X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION")
  protected var `Xwasm-disable-array-range-checks-safe-elimination`: Boolean =
      defaultArguments.wasmDisableArrayRangeChecksSafeElimination

  @SerialName("X_WASM_ENABLE_ARRAY_RANGE_CHECKS")
  protected var `Xwasm-enable-array-range-checks`: Boolean =
      defaultArguments.wasmEnableArrayRangeChecks

  @SerialName("X_WASM_ENABLE_ASSERTS")
  protected var `Xwasm-enable-asserts`: Boolean = defaultArguments.wasmEnableAsserts

  @SerialName("X_WASM_ENABLE_TAIL_CALLS")
  protected var `Xwasm-enable-tail-calls`: Boolean = defaultArguments.wasmEnableTailCalls

  @SerialName("X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE")
  protected var `Xwasm-generate-closed-world-multimodule`: Boolean =
      defaultArguments.wasmGenerateClosedWorldMultimodule

  @SerialName("X_WASM_GENERATE_DWARF")
  protected var `Xwasm-generate-dwarf`: Boolean = defaultArguments.generateDwarf

  @SerialName("X_WASM_GENERATE_WAT")
  protected var `Xwasm-generate-wat`: Boolean = defaultArguments.wasmGenerateWat

  @SerialName("X_WASM_INCLUDED_MODULE_ONLY")
  protected var `Xwasm-included-module-only`: Boolean = defaultArguments.wasmIncludedModuleOnly

  @SerialName("X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX")
  protected var `Xwasm-internal-local-variable-prefix`: String =
      defaultArguments.wasmInternalLocalVariablePrefix

  @SerialName("X_WASM_KCLASS_FQN")
  protected var `Xwasm-kclass-fqn`: Boolean = defaultArguments.wasmKClassFqn

  @SerialName("X_WASM_NO_JSTAG")
  protected var `Xwasm-no-jstag`: Boolean = defaultArguments.wasmNoJsTag

  @SerialName("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES")
  protected var `Xwasm-source-map-include-mappings-from-unavailable-sources`: Boolean =
      defaultArguments.includeUnavailableSourcesIntoSourceMap

  @SerialName("X_WASM_TARGET")
  protected var `Xwasm-target`: WasmTarget? =
      defaultArguments.wasmTarget?.let { WasmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::wasmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwasm-target value: $it") }

  @SerialName("X_WASM_USE_NEW_EXCEPTION_PROPOSAL")
  protected var `Xwasm-use-new-exception-proposal`: Boolean? =
      defaultArguments.wasmUseNewExceptionProposal

  @SerialName("X_WASM_USE_STACK_SWITCHING_PROPOSAL")
  protected var `Xwasm-use-stack-switching-proposal`: Boolean =
      defaultArguments.wasmUseStackSwitchingProposal

  @SerialName("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")
  protected var `Xwasm-use-traps-instead-of-exceptions`: Boolean =
      defaultArguments.wasmUseTrapsInsteadOfExceptions
  init {
    applyCompilerArguments(KotlinWasmCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: WasmArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: WasmArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: WasmArgument<*>): Boolean = key.id in optionsMap

  private operator fun `get`(key: String): Any? = WasmArgumentValueAdapter.toApi(optionsMap[key])

  private operator fun `set`(key: String, `value`: Any?) {
    optionsMap[key] = WasmArgumentValueAdapter.toImpl(`value`)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerArguments.WasmCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerArguments.WasmCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerKlibArguments.WasmCompilerKlibArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerKlibArguments.WasmCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: WasmCompilerLinkingArguments.WasmCompilerLinkingArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: WasmCompilerLinkingArguments.WasmCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  override fun deepCopy(): WasmArgumentsImpl = WasmArgumentsImpl(argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): WasmArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): KotlinWasmCompilerArguments {
    val arguments = KotlinWasmCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.irDceDumpReachabilityInfoToFile = `Xir-dce-dump-reachability-info-to-file`?.absolutePathStringOrThrow()
    arguments.irDceDumpDeclarationIrSizesToFile = `Xir-dump-declaration-ir-sizes-to-file`?.absolutePathStringOrThrow()
    arguments.wasm = Xwasm
    arguments.regenerateUnchangedModules = `Xwasm-IC-generate-unchanged-modules`
    arguments.forceDebugFriendlyCompilation = `Xwasm-debug-friendly`
    arguments.wasmDebug = `Xwasm-debug-info`
    arguments.debuggerCustomFormatters = `Xwasm-debugger-custom-formatters`
    arguments.wasmDisableArrayRangeChecksSafeElimination = `Xwasm-disable-array-range-checks-safe-elimination`
    arguments.wasmEnableArrayRangeChecks = `Xwasm-enable-array-range-checks`
    arguments.wasmEnableAsserts = `Xwasm-enable-asserts`
    arguments.wasmEnableTailCalls = `Xwasm-enable-tail-calls`
    arguments.wasmGenerateClosedWorldMultimodule = `Xwasm-generate-closed-world-multimodule`
    arguments.generateDwarf = `Xwasm-generate-dwarf`
    arguments.wasmGenerateWat = `Xwasm-generate-wat`
    arguments.wasmIncludedModuleOnly = `Xwasm-included-module-only`
    arguments.wasmInternalLocalVariablePrefix = `Xwasm-internal-local-variable-prefix`
    arguments.wasmKClassFqn = `Xwasm-kclass-fqn`
    arguments.wasmNoJsTag = `Xwasm-no-jstag`
    arguments.includeUnavailableSourcesIntoSourceMap = `Xwasm-source-map-include-mappings-from-unavailable-sources`
    arguments.wasmTarget = `Xwasm-target`?.stringValue
    arguments.wasmUseNewExceptionProposal = `Xwasm-use-new-exception-proposal`
    arguments.wasmUseStackSwitchingProposal = `Xwasm-use-stack-switching-proposal`
    arguments.wasmUseTrapsInsteadOfExceptions = `Xwasm-use-traps-instead-of-exceptions`
    arguments.internalArguments = parseCommandLineArguments<KotlinWasmCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: KotlinWasmCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { `Xir-dce-dump-reachability-info-to-file` = arguments.irDceDumpReachabilityInfoToFile?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xir-dump-declaration-ir-sizes-to-file` = arguments.irDceDumpDeclarationIrSizesToFile?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { Xwasm = arguments.wasm } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-IC-generate-unchanged-modules` = arguments.regenerateUnchangedModules } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-debug-friendly` = arguments.forceDebugFriendlyCompilation } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-debug-info` = arguments.wasmDebug } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-debugger-custom-formatters` = arguments.debuggerCustomFormatters } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-disable-array-range-checks-safe-elimination` = arguments.wasmDisableArrayRangeChecksSafeElimination } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-enable-array-range-checks` = arguments.wasmEnableArrayRangeChecks } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-enable-asserts` = arguments.wasmEnableAsserts } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-enable-tail-calls` = arguments.wasmEnableTailCalls } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-generate-closed-world-multimodule` = arguments.wasmGenerateClosedWorldMultimodule } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-generate-dwarf` = arguments.generateDwarf } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-generate-wat` = arguments.wasmGenerateWat } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-included-module-only` = arguments.wasmIncludedModuleOnly } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-internal-local-variable-prefix` = arguments.wasmInternalLocalVariablePrefix } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-kclass-fqn` = arguments.wasmKClassFqn } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-no-jstag` = arguments.wasmNoJsTag } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-source-map-include-mappings-from-unavailable-sources` = arguments.includeUnavailableSourcesIntoSourceMap } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-target` = arguments.wasmTarget?.let { WasmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::wasmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwasm-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-use-new-exception-proposal` = arguments.wasmUseNewExceptionProposal } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-use-stack-switching-proposal` = arguments.wasmUseStackSwitchingProposal } catch (_: NoSuchMethodError) {  }
    try { `Xwasm-use-traps-instead-of-exceptions` = arguments.wasmUseTrapsInsteadOfExceptions } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: KotlinWasmCompilerArguments = KotlinWasmCompilerArguments()): KotlinWasmCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.irDceDumpReachabilityInfoToFile = `Xir-dce-dump-reachability-info-to-file`?.absolutePathStringOrThrow()
    arguments.irDceDumpDeclarationIrSizesToFile = `Xir-dump-declaration-ir-sizes-to-file`?.absolutePathStringOrThrow()
    arguments.wasm = Xwasm
    arguments.regenerateUnchangedModules = `Xwasm-IC-generate-unchanged-modules`
    arguments.forceDebugFriendlyCompilation = `Xwasm-debug-friendly`
    arguments.wasmDebug = `Xwasm-debug-info`
    arguments.debuggerCustomFormatters = `Xwasm-debugger-custom-formatters`
    arguments.wasmDisableArrayRangeChecksSafeElimination = `Xwasm-disable-array-range-checks-safe-elimination`
    arguments.wasmEnableArrayRangeChecks = `Xwasm-enable-array-range-checks`
    arguments.wasmEnableAsserts = `Xwasm-enable-asserts`
    arguments.wasmEnableTailCalls = `Xwasm-enable-tail-calls`
    arguments.wasmGenerateClosedWorldMultimodule = `Xwasm-generate-closed-world-multimodule`
    arguments.generateDwarf = `Xwasm-generate-dwarf`
    arguments.wasmGenerateWat = `Xwasm-generate-wat`
    arguments.wasmIncludedModuleOnly = `Xwasm-included-module-only`
    arguments.wasmInternalLocalVariablePrefix = `Xwasm-internal-local-variable-prefix`
    arguments.wasmKClassFqn = `Xwasm-kclass-fqn`
    arguments.wasmNoJsTag = `Xwasm-no-jstag`
    arguments.includeUnavailableSourcesIntoSourceMap = `Xwasm-source-map-include-mappings-from-unavailable-sources`
    arguments.wasmTarget = `Xwasm-target`?.stringValue
    arguments.wasmUseNewExceptionProposal = `Xwasm-use-new-exception-proposal`
    arguments.wasmUseStackSwitchingProposal = `Xwasm-use-stack-switching-proposal`
    arguments.wasmUseTrapsInsteadOfExceptions = `Xwasm-use-traps-instead-of-exceptions`
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

  public class WasmArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE: WasmArgument<Path?> =
        WasmArgument("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE")

    public val X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE: WasmArgument<Path?> =
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
