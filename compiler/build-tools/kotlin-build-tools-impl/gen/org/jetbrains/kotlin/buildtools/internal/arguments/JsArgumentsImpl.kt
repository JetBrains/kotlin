// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

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
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsModuleKind
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

internal class JsArgumentsImpl(
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonJsAndWasmArgumentsImpl(argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    JsCompilerArguments,
    JsCompilerArguments.Builder,
    JsCompilerKlibArguments,
    JsCompilerKlibArguments.Builder,
    JsCompilerLinkingArguments,
    JsCompilerLinkingArguments.Builder,
    DeepCopyable<JsArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_DTS_USE_UNKNOWN_INSTEAD_ANY")
  protected var `Xdts-use-unknown-instead-any`: Boolean

  @SerialName("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS")
  protected var `Xenable-extension-functions-in-externals`: Boolean

  @SerialName("X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT")
  protected var `Xenable-implementing-interfaces-from-typescript`: Boolean

  @SerialName("X_ENABLE_SUSPEND_FUNCTION_EXPORTING")
  protected var `Xenable-suspend-function-exporting`: Boolean

  @SerialName("X_ES_ARROW_FUNCTIONS")
  protected var `Xes-arrow-functions`: Boolean?

  @SerialName("X_ES_CLASSES")
  protected var `Xes-classes`: Boolean?

  @SerialName("X_ES_GENERATORS")
  protected var `Xes-generators`: Boolean?

  @SerialName("X_ES_LONG_AS_BIGINT")
  protected var `Xes-long-as-bigint`: Boolean?

  @SerialName("X_EXPORT_KDOC")
  protected var `Xexport-kdoc`: Boolean

  @SerialName("X_GENERATE_POLYFILLS")
  protected var `Xgenerate-polyfills`: Boolean

  @SerialName("X_INTEGER_DIVISION_CHECK")
  protected var `Xinteger-division-check`: Boolean

  @SerialName("X_IR_BUILD_CACHE")
  protected var `Xir-build-cache`: Boolean

  @SerialName("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS")
  protected var `Xir-generate-inline-anonymous-functions`: Boolean

  @SerialName("X_IR_KEEP")
  protected var `Xir-keep`: String?

  @SerialName("X_IR_MINIMIZED_MEMBER_NAMES")
  protected var `Xir-minimized-member-names`: Boolean

  @SerialName("X_IR_PER_FILE")
  protected var `Xir-per-file`: Boolean

  @SerialName("X_IR_PER_MODULE")
  protected var `Xir-per-module`: Boolean

  @SerialName("X_IR_SAFE_EXTERNAL_BOOLEAN")
  protected var `Xir-safe-external-boolean`: Boolean

  @SerialName("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC")
  protected var `Xir-safe-external-boolean-diagnostic`: JsIrDiagnosticMode?

  @SerialName("X_OPTIMIZE_GENERATED_JS")
  protected var `Xoptimize-generated-js`: Boolean

  @SerialName("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION")
  protected var `Xplatform-arguments-in-main-function`: String?

  @SerialName("X_SUSPEND_LAMBDA_EXPORTING")
  protected var `Xsuspend-lambda-exporting`: Boolean

  @SerialName("X_TYPED_ARRAYS")
  protected var `Xtyped-arrays`: Boolean

  @SerialName("MODULE_KIND")
  protected var `module-kind`: JsModuleKind?

  @SerialName("TARGET")
  protected var target: JsEcmaVersion?
  init {
    applyCompilerArguments(K2JSCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JsArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: JsArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: JsArgument<*>): Boolean = key.id in optionsMap

  private operator fun `get`(key: String): Any? = JsArgumentValueAdapter.toApi(optionsMap[key])

  private operator fun `set`(key: String, `value`: Any?) {
    optionsMap[key] = JsArgumentValueAdapter.toImpl(`value`)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerArguments.JsCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerArguments.JsCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  override fun deepCopy(): JsArgumentsImpl = JsArgumentsImpl(argumentValidationErrors.toSet(), restrictedArgViolations.toList(), argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): JsArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2JSCompilerArguments {
    val arguments = K2JSCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.useUnknownInsteadAny = `Xdts-use-unknown-instead-any`
    arguments.extensionFunctionsInExternals = `Xenable-extension-functions-in-externals`
    arguments.allowImplementableInterfacesExporting = `Xenable-implementing-interfaces-from-typescript`
    arguments.allowExportingSuspendFunctions = `Xenable-suspend-function-exporting`
    arguments.useEsArrowFunctions = `Xes-arrow-functions`
    arguments.useEsClasses = `Xes-classes`
    arguments.useEsGenerators = `Xes-generators`
    arguments.compileLongAsBigInt = `Xes-long-as-bigint`
    arguments.exportKDoc = `Xexport-kdoc`
    arguments.generatePolyfills = `Xgenerate-polyfills`
    arguments.integerDivisionCheck = `Xinteger-division-check`
    arguments.irBuildCache = `Xir-build-cache`
    arguments.irGenerateInlineAnonymousFunctions = `Xir-generate-inline-anonymous-functions`
    arguments.irKeep = `Xir-keep`
    arguments.irMinimizedMemberNames = `Xir-minimized-member-names`
    arguments.irPerFile = `Xir-per-file`
    arguments.irPerModule = `Xir-per-module`
    arguments.irSafeExternalBoolean = `Xir-safe-external-boolean`
    arguments.irSafeExternalBooleanDiagnostic = `Xir-safe-external-boolean-diagnostic`?.stringValue
    arguments.optimizeGeneratedJs = `Xoptimize-generated-js`
    arguments.platformArgumentsProviderJsExpression = `Xplatform-arguments-in-main-function`
    arguments.allowExportingSuspendLambdas = `Xsuspend-lambda-exporting`
    try { arguments.setUsingReflection("typedArrays", `Xtyped-arrays`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    arguments.moduleKind = `module-kind`?.stringValue
    arguments.target = target?.stringValue
    arguments.internalArguments = parseCommandLineArguments<K2JSCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2JSCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { `Xdts-use-unknown-instead-any` = arguments.useUnknownInsteadAny } catch (_: NoSuchMethodError) {  }
    try { `Xenable-extension-functions-in-externals` = arguments.extensionFunctionsInExternals } catch (_: NoSuchMethodError) {  }
    try { `Xenable-implementing-interfaces-from-typescript` = arguments.allowImplementableInterfacesExporting } catch (_: NoSuchMethodError) {  }
    try { `Xenable-suspend-function-exporting` = arguments.allowExportingSuspendFunctions } catch (_: NoSuchMethodError) {  }
    try { `Xes-arrow-functions` = arguments.useEsArrowFunctions } catch (_: NoSuchMethodError) {  }
    try { `Xes-classes` = arguments.useEsClasses } catch (_: NoSuchMethodError) {  }
    try { `Xes-generators` = arguments.useEsGenerators } catch (_: NoSuchMethodError) {  }
    try { `Xes-long-as-bigint` = arguments.compileLongAsBigInt } catch (_: NoSuchMethodError) {  }
    try { `Xexport-kdoc` = arguments.exportKDoc } catch (_: NoSuchMethodError) {  }
    try { `Xgenerate-polyfills` = arguments.generatePolyfills } catch (_: NoSuchMethodError) {  }
    try { `Xinteger-division-check` = arguments.integerDivisionCheck } catch (_: NoSuchMethodError) {  }
    try { `Xir-build-cache` = arguments.irBuildCache } catch (_: NoSuchMethodError) {  }
    try { `Xir-generate-inline-anonymous-functions` = arguments.irGenerateInlineAnonymousFunctions } catch (_: NoSuchMethodError) {  }
    try { `Xir-keep` = arguments.irKeep } catch (_: NoSuchMethodError) {  }
    try { `Xir-minimized-member-names` = arguments.irMinimizedMemberNames } catch (_: NoSuchMethodError) {  }
    try { `Xir-per-file` = arguments.irPerFile } catch (_: NoSuchMethodError) {  }
    try { `Xir-per-module` = arguments.irPerModule } catch (_: NoSuchMethodError) {  }
    try { `Xir-safe-external-boolean` = arguments.irSafeExternalBoolean } catch (_: NoSuchMethodError) {  }
    try { `Xir-safe-external-boolean-diagnostic` = arguments.irSafeExternalBooleanDiagnostic?.let { JsIrDiagnosticMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irSafeExternalBooleanDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-safe-external-boolean-diagnostic value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xoptimize-generated-js` = arguments.optimizeGeneratedJs } catch (_: NoSuchMethodError) {  }
    try { `Xplatform-arguments-in-main-function` = arguments.platformArgumentsProviderJsExpression } catch (_: NoSuchMethodError) {  }
    try { `Xsuspend-lambda-exporting` = arguments.allowExportingSuspendLambdas } catch (_: NoSuchMethodError) {  }
    try { `Xtyped-arrays` = arguments.getUsingReflection<Boolean>("typedArrays") } catch (_: NoSuchMethodError) {  }
    try { `module-kind` = arguments.moduleKind?.let { JsModuleKind.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::moduleKind, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { target = arguments.target?.let { JsEcmaVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2JSCompilerArguments = K2JSCompilerArguments()): K2JSCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.useUnknownInsteadAny = `Xdts-use-unknown-instead-any`
    arguments.extensionFunctionsInExternals = `Xenable-extension-functions-in-externals`
    arguments.allowImplementableInterfacesExporting = `Xenable-implementing-interfaces-from-typescript`
    arguments.allowExportingSuspendFunctions = `Xenable-suspend-function-exporting`
    arguments.useEsArrowFunctions = `Xes-arrow-functions`
    arguments.useEsClasses = `Xes-classes`
    arguments.useEsGenerators = `Xes-generators`
    arguments.compileLongAsBigInt = `Xes-long-as-bigint`
    arguments.exportKDoc = `Xexport-kdoc`
    arguments.generatePolyfills = `Xgenerate-polyfills`
    arguments.integerDivisionCheck = `Xinteger-division-check`
    arguments.irBuildCache = `Xir-build-cache`
    arguments.irGenerateInlineAnonymousFunctions = `Xir-generate-inline-anonymous-functions`
    arguments.irKeep = `Xir-keep`
    arguments.irMinimizedMemberNames = `Xir-minimized-member-names`
    arguments.irPerFile = `Xir-per-file`
    arguments.irPerModule = `Xir-per-module`
    arguments.irSafeExternalBoolean = `Xir-safe-external-boolean`
    arguments.irSafeExternalBooleanDiagnostic = `Xir-safe-external-boolean-diagnostic`?.stringValue
    arguments.optimizeGeneratedJs = `Xoptimize-generated-js`
    arguments.platformArgumentsProviderJsExpression = `Xplatform-arguments-in-main-function`
    arguments.allowExportingSuspendLambdas = `Xsuspend-lambda-exporting`
    try { arguments.setUsingReflection("typedArrays", `Xtyped-arrays`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    arguments.moduleKind = `module-kind`?.stringValue
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

    public val X_TYPED_ARRAYS: JsArgument<Boolean> = JsArgument("X_TYPED_ARRAYS")

    public val MODULE_KIND: JsArgument<JsModuleKind?> = JsArgument("MODULE_KIND")

    public val TARGET: JsArgument<JsEcmaVersion?> = JsArgument("TARGET")
  }
}
