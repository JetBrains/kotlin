// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.`internal`.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.`internal`.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal class JsArgumentsImpl() : CommonJsAndWasmArgumentsImpl(), JsCompilerArguments,
    JsCompilerArguments.Builder, JsCompilerKlibArguments, JsCompilerKlibArguments.Builder,
    JsCompilerLinkingArguments, JsCompilerLinkingArguments.Builder, DeepCopyable<JsArgumentsImpl> {
  @Transient
  private val defaultArguments: K2JSCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_DTS_USE_UNKNOWN_INSTEAD_ANY")
  internal var useUnknownInsteadAny: Boolean = defaultArguments.useUnknownInsteadAny

  @SerialName("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS")
  internal var extensionFunctionsInExternals: Boolean =
      defaultArguments.extensionFunctionsInExternals

  @SerialName("X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT")
  internal var allowImplementableInterfacesExporting: Boolean =
      defaultArguments.allowImplementableInterfacesExporting

  @SerialName("X_ENABLE_SUSPEND_FUNCTION_EXPORTING")
  internal var allowExportingSuspendFunctions: Boolean =
      defaultArguments.allowExportingSuspendFunctions

  @SerialName("X_ES_ARROW_FUNCTIONS")
  internal var useEsArrowFunctions: Boolean? = defaultArguments.useEsArrowFunctions

  @SerialName("X_ES_CLASSES")
  internal var useEsClasses: Boolean? = defaultArguments.useEsClasses

  @SerialName("X_ES_GENERATORS")
  internal var useEsGenerators: Boolean? = defaultArguments.useEsGenerators

  @SerialName("X_ES_LONG_AS_BIGINT")
  internal var compileLongAsBigInt: Boolean? = defaultArguments.compileLongAsBigInt

  @SerialName("X_EXPORT_KDOC")
  internal var exportKDoc: Boolean = defaultArguments.exportKDoc

  @SerialName("X_GENERATE_POLYFILLS")
  internal var generatePolyfills: Boolean = defaultArguments.generatePolyfills

  @SerialName("X_INTEGER_DIVISION_CHECK")
  internal var integerDivisionCheck: Boolean = defaultArguments.integerDivisionCheck

  @SerialName("X_IR_BUILD_CACHE")
  internal var irBuildCache: Boolean = defaultArguments.irBuildCache

  @SerialName("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS")
  internal var irGenerateInlineAnonymousFunctions: Boolean =
      defaultArguments.irGenerateInlineAnonymousFunctions

  @SerialName("X_IR_KEEP")
  internal var irKeep: String? = defaultArguments.irKeep

  @SerialName("X_IR_MINIMIZED_MEMBER_NAMES")
  internal var irMinimizedMemberNames: Boolean = defaultArguments.irMinimizedMemberNames

  @SerialName("X_IR_PER_FILE")
  internal var irPerFile: Boolean = defaultArguments.irPerFile

  @SerialName("X_IR_PER_MODULE")
  internal var irPerModule: Boolean = defaultArguments.irPerModule

  @SerialName("X_IR_SAFE_EXTERNAL_BOOLEAN")
  internal var irSafeExternalBoolean: Boolean = defaultArguments.irSafeExternalBoolean

  @SerialName("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC")
  internal var irSafeExternalBooleanDiagnostic: JsIrDiagnosticMode? =
      defaultArguments.irSafeExternalBooleanDiagnostic?.let { JsIrDiagnosticMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::irSafeExternalBooleanDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-safe-external-boolean-diagnostic value: $it") }

  @SerialName("X_OPTIMIZE_GENERATED_JS")
  internal var optimizeGeneratedJs: Boolean = defaultArguments.optimizeGeneratedJs

  @SerialName("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION")
  internal var platformArgumentsProviderJsExpression: String? =
      defaultArguments.platformArgumentsProviderJsExpression

  @SerialName("X_SUSPEND_LAMBDA_EXPORTING")
  internal var allowExportingSuspendLambdas: Boolean = defaultArguments.allowExportingSuspendLambdas

  @SerialName("MODULE_KIND")
  internal var moduleKind: JsModuleKind? =
      defaultArguments.moduleKind?.let { JsModuleKind.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::moduleKind, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it") }

  @SerialName("TARGET")
  internal var target: JsEcmaVersion? =
      defaultArguments.target?.let { JsEcmaVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") }

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  override fun createDefaultArguments(): K2JSCompilerArguments = K2JSCompilerArguments()

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JsArgument<V>): V = JsArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)

  public operator fun <V> `set`(key: JsArgument<V>, `value`: V) {
    JsArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
  }

  public operator fun contains(key: JsArgument<*>): Boolean = true

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerArguments.JsCompilerArgument<V>): V = try { JsArgumentValueAdapter.toApi(JsArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerArguments.JsCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { JsArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, JsArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>): V = try { JsArgumentValueAdapter.toApi(JsArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { JsArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, JsArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>): V = try { JsArgumentValueAdapter.toApi(JsArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { JsArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, JsArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  override fun deepCopy(): JsArgumentsImpl = JsArgumentsImpl(argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): JsArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2JSCompilerArguments {
    val arguments = K2JSCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter {
      when (it) {
        "X_TYPED_ARRAYS" -> throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""")
        "OUTPUT" -> throw IllegalStateException("""Compiler parameter not recognized: OUTPUT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.2.0""")
        else -> true
      }
    }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.useUnknownInsteadAny = useUnknownInsteadAny
    arguments.extensionFunctionsInExternals = extensionFunctionsInExternals
    arguments.allowImplementableInterfacesExporting = allowImplementableInterfacesExporting
    arguments.allowExportingSuspendFunctions = allowExportingSuspendFunctions
    arguments.useEsArrowFunctions = useEsArrowFunctions
    arguments.useEsClasses = useEsClasses
    arguments.useEsGenerators = useEsGenerators
    arguments.compileLongAsBigInt = compileLongAsBigInt
    arguments.exportKDoc = exportKDoc
    arguments.generatePolyfills = generatePolyfills
    arguments.integerDivisionCheck = integerDivisionCheck
    arguments.irBuildCache = irBuildCache
    arguments.irGenerateInlineAnonymousFunctions = irGenerateInlineAnonymousFunctions
    arguments.irKeep = irKeep
    arguments.irMinimizedMemberNames = irMinimizedMemberNames
    arguments.irPerFile = irPerFile
    arguments.irPerModule = irPerModule
    arguments.irSafeExternalBoolean = irSafeExternalBoolean
    arguments.irSafeExternalBooleanDiagnostic = irSafeExternalBooleanDiagnostic?.stringValue
    arguments.optimizeGeneratedJs = optimizeGeneratedJs
    arguments.platformArgumentsProviderJsExpression = platformArgumentsProviderJsExpression
    arguments.allowExportingSuspendLambdas = allowExportingSuspendLambdas
    arguments.moduleKind = moduleKind?.stringValue
    arguments.target = target?.stringValue
    arguments.internalArguments = parseCommandLineArguments<K2JSCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2JSCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { useUnknownInsteadAny = arguments.useUnknownInsteadAny } catch (_: NoSuchMethodError) {  }
    try { extensionFunctionsInExternals = arguments.extensionFunctionsInExternals } catch (_: NoSuchMethodError) {  }
    try { allowImplementableInterfacesExporting = arguments.allowImplementableInterfacesExporting } catch (_: NoSuchMethodError) {  }
    try { allowExportingSuspendFunctions = arguments.allowExportingSuspendFunctions } catch (_: NoSuchMethodError) {  }
    try { useEsArrowFunctions = arguments.useEsArrowFunctions } catch (_: NoSuchMethodError) {  }
    try { useEsClasses = arguments.useEsClasses } catch (_: NoSuchMethodError) {  }
    try { useEsGenerators = arguments.useEsGenerators } catch (_: NoSuchMethodError) {  }
    try { compileLongAsBigInt = arguments.compileLongAsBigInt } catch (_: NoSuchMethodError) {  }
    try { exportKDoc = arguments.exportKDoc } catch (_: NoSuchMethodError) {  }
    try { generatePolyfills = arguments.generatePolyfills } catch (_: NoSuchMethodError) {  }
    try { integerDivisionCheck = arguments.integerDivisionCheck } catch (_: NoSuchMethodError) {  }
    try { irBuildCache = arguments.irBuildCache } catch (_: NoSuchMethodError) {  }
    try { irGenerateInlineAnonymousFunctions = arguments.irGenerateInlineAnonymousFunctions } catch (_: NoSuchMethodError) {  }
    try { irKeep = arguments.irKeep } catch (_: NoSuchMethodError) {  }
    try { irMinimizedMemberNames = arguments.irMinimizedMemberNames } catch (_: NoSuchMethodError) {  }
    try { irPerFile = arguments.irPerFile } catch (_: NoSuchMethodError) {  }
    try { irPerModule = arguments.irPerModule } catch (_: NoSuchMethodError) {  }
    try { irSafeExternalBoolean = arguments.irSafeExternalBoolean } catch (_: NoSuchMethodError) {  }
    try { irSafeExternalBooleanDiagnostic = arguments.irSafeExternalBooleanDiagnostic?.let { JsIrDiagnosticMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irSafeExternalBooleanDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-safe-external-boolean-diagnostic value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { optimizeGeneratedJs = arguments.optimizeGeneratedJs } catch (_: NoSuchMethodError) {  }
    try { platformArgumentsProviderJsExpression = arguments.platformArgumentsProviderJsExpression } catch (_: NoSuchMethodError) {  }
    try { allowExportingSuspendLambdas = arguments.allowExportingSuspendLambdas } catch (_: NoSuchMethodError) {  }
    try { moduleKind = arguments.moduleKind?.let { JsModuleKind.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::moduleKind, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { target = arguments.target?.let { JsEcmaVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2JSCompilerArguments = K2JSCompilerArguments()): K2JSCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.useUnknownInsteadAny = useUnknownInsteadAny
    arguments.extensionFunctionsInExternals = extensionFunctionsInExternals
    arguments.allowImplementableInterfacesExporting = allowImplementableInterfacesExporting
    arguments.allowExportingSuspendFunctions = allowExportingSuspendFunctions
    arguments.useEsArrowFunctions = useEsArrowFunctions
    arguments.useEsClasses = useEsClasses
    arguments.useEsGenerators = useEsGenerators
    arguments.compileLongAsBigInt = compileLongAsBigInt
    arguments.exportKDoc = exportKDoc
    arguments.generatePolyfills = generatePolyfills
    arguments.integerDivisionCheck = integerDivisionCheck
    arguments.irBuildCache = irBuildCache
    arguments.irGenerateInlineAnonymousFunctions = irGenerateInlineAnonymousFunctions
    arguments.irKeep = irKeep
    arguments.irMinimizedMemberNames = irMinimizedMemberNames
    arguments.irPerFile = irPerFile
    arguments.irPerModule = irPerModule
    arguments.irSafeExternalBoolean = irSafeExternalBoolean
    arguments.irSafeExternalBooleanDiagnostic = irSafeExternalBooleanDiagnostic?.stringValue
    arguments.optimizeGeneratedJs = optimizeGeneratedJs
    arguments.platformArgumentsProviderJsExpression = platformArgumentsProviderJsExpression
    arguments.allowExportingSuspendLambdas = allowExportingSuspendLambdas
    arguments.moduleKind = moduleKind?.stringValue
    arguments.target = target?.stringValue
    return arguments
  }

  @Deprecated(
    message = "This method is deprecated. Use applyCommandLineArguments instead.",
    level = DeprecationLevel.WARNING,
  )
  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2JSCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, K2JSCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  @DelicateBuildToolsApi
  override fun applyCommandLineArguments(arguments: List<String>) {
    val compilerArgs = toCompilerArguments()
    parseCommandLineArguments(arguments, compilerArgs, false)
    handleCustomPluginArguments(this, compilerArgs)
    collectRestrictedArgViolations(compilerArgs, K2JSCompilerArguments())
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

  public class JsArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_DTS_USE_UNKNOWN_INSTEAD_ANY: JsArgument<Boolean> =
        JsArgument("X_DTS_USE_UNKNOWN_INSTEAD_ANY")

    public val X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS: JsArgument<Boolean> =
        JsArgument("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS")

    public val X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT: JsArgument<Boolean> =
        JsArgument("X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT")

    public val X_ENABLE_SUSPEND_FUNCTION_EXPORTING: JsArgument<Boolean> =
        JsArgument("X_ENABLE_SUSPEND_FUNCTION_EXPORTING")

    public val X_ES_ARROW_FUNCTIONS: JsArgument<Boolean?> = JsArgument("X_ES_ARROW_FUNCTIONS")

    public val X_ES_CLASSES: JsArgument<Boolean?> = JsArgument("X_ES_CLASSES")

    public val X_ES_GENERATORS: JsArgument<Boolean?> = JsArgument("X_ES_GENERATORS")

    public val X_ES_LONG_AS_BIGINT: JsArgument<Boolean?> = JsArgument("X_ES_LONG_AS_BIGINT")

    public val X_EXPORT_KDOC: JsArgument<Boolean> = JsArgument("X_EXPORT_KDOC")

    public val X_GENERATE_POLYFILLS: JsArgument<Boolean> = JsArgument("X_GENERATE_POLYFILLS")

    public val X_INTEGER_DIVISION_CHECK: JsArgument<Boolean> =
        JsArgument("X_INTEGER_DIVISION_CHECK")

    public val X_IR_BUILD_CACHE: JsArgument<Boolean> = JsArgument("X_IR_BUILD_CACHE")

    public val X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS: JsArgument<Boolean> =
        JsArgument("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS")

    public val X_IR_KEEP: JsArgument<String?> = JsArgument("X_IR_KEEP")

    public val X_IR_MINIMIZED_MEMBER_NAMES: JsArgument<Boolean> =
        JsArgument("X_IR_MINIMIZED_MEMBER_NAMES")

    public val X_IR_PER_FILE: JsArgument<Boolean> = JsArgument("X_IR_PER_FILE")

    public val X_IR_PER_MODULE: JsArgument<Boolean> = JsArgument("X_IR_PER_MODULE")

    public val X_IR_SAFE_EXTERNAL_BOOLEAN: JsArgument<Boolean> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN")

    public val X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: JsArgument<JsIrDiagnosticMode?> =
        JsArgument("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC")

    public val X_OPTIMIZE_GENERATED_JS: JsArgument<Boolean> = JsArgument("X_OPTIMIZE_GENERATED_JS")

    public val X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION: JsArgument<String?> =
        JsArgument("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION")

    public val X_SUSPEND_LAMBDA_EXPORTING: JsArgument<Boolean> =
        JsArgument("X_SUSPEND_LAMBDA_EXPORTING")

    public val MODULE_KIND: JsArgument<JsModuleKind?> = JsArgument("MODULE_KIND")

    public val TARGET: JsArgument<JsEcmaVersion?> = JsArgument("TARGET")
  }
}
