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
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.MODULE_KIND
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.OUTPUT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.TARGET
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ENABLE_SUSPEND_FUNCTION_EXPORTING
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_ARROW_FUNCTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_CLASSES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_GENERATORS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_ES_LONG_AS_BIGINT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_GENERATE_POLYFILLS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_BUILD_CACHE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_KEEP
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_MINIMIZED_MEMBER_NAMES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PER_FILE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_PER_MODULE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_SAFE_EXTERNAL_BOOLEAN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_OPTIMIZE_GENERATED_JS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JsArgumentsImpl.Companion.X_TYPED_ARRAYS
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class JsArgumentsImpl(
  private val adapter: JsArgumentValueAdapter? = null,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
) : CommonJsAndWasmArgumentsImpl(adapter, argumentValidationErrors, restrictedArgViolations),
    JsArguments,
    JsArguments.Builder,
    DeepCopyable<JsArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()
  init {
    applyCompilerArguments(K2JSCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsArguments.JsArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return adapter?.mapFrom(optionsMap[key.id], key) ?: optionsMap[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsArguments.JsArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 4, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = adapter?.mapTo(`value`, key) ?: `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JsArgument<V>): V = optionsMap[key.id] as V

  private operator fun <V> `set`(key: JsArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: JsArgument<*>): Boolean = key.id in optionsMap

  override fun deepCopy(): JsArgumentsImpl = JsArgumentsImpl(adapter, argumentValidationErrors.toSet(), restrictedArgViolations.toList()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): JsArguments = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2JSCompilerArguments {
    val arguments = K2JSCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS in this) { arguments.extensionFunctionsInExternals = get(X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS)}
    if (X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT in this) { arguments.allowImplementableInterfacesExporting = get(X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT)}
    if (X_ENABLE_SUSPEND_FUNCTION_EXPORTING in this) { arguments.allowExportingSuspendFunctions = get(X_ENABLE_SUSPEND_FUNCTION_EXPORTING)}
    if (X_ES_ARROW_FUNCTIONS in this) { arguments.useEsArrowFunctions = get(X_ES_ARROW_FUNCTIONS)}
    if (X_ES_CLASSES in this) { arguments.useEsClasses = get(X_ES_CLASSES)}
    if (X_ES_GENERATORS in this) { arguments.useEsGenerators = get(X_ES_GENERATORS)}
    if (X_ES_LONG_AS_BIGINT in this) { arguments.compileLongAsBigInt = get(X_ES_LONG_AS_BIGINT)}
    if (X_GENERATE_POLYFILLS in this) { arguments.generatePolyfills = get(X_GENERATE_POLYFILLS)}
    if (X_IR_BUILD_CACHE in this) { arguments.irBuildCache = get(X_IR_BUILD_CACHE)}
    if (X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS in this) { arguments.irGenerateInlineAnonymousFunctions = get(X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS)}
    if (X_IR_KEEP in this) { arguments.irKeep = get(X_IR_KEEP)}
    if (X_IR_MINIMIZED_MEMBER_NAMES in this) { arguments.irMinimizedMemberNames = get(X_IR_MINIMIZED_MEMBER_NAMES)}
    if (X_IR_PER_FILE in this) { arguments.irPerFile = get(X_IR_PER_FILE)}
    if (X_IR_PER_MODULE in this) { arguments.irPerModule = get(X_IR_PER_MODULE)}
    if (X_IR_SAFE_EXTERNAL_BOOLEAN in this) { arguments.irSafeExternalBoolean = get(X_IR_SAFE_EXTERNAL_BOOLEAN)}
    if (X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC in this) { arguments.irSafeExternalBooleanDiagnostic = get(X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC)?.stringValue}
    if (X_OPTIMIZE_GENERATED_JS in this) { arguments.optimizeGeneratedJs = get(X_OPTIMIZE_GENERATED_JS)}
    if (X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION in this) { arguments.platformArgumentsProviderJsExpression = get(X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION)}
    try { if (X_TYPED_ARRAYS in this) { arguments.setUsingReflection("typedArrays", get(X_TYPED_ARRAYS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    if (MODULE_KIND in this) { arguments.moduleKind = get(MODULE_KIND)?.stringValue}
    try { if (OUTPUT in this) { arguments.setUsingReflection("outputFile", get(OUTPUT))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: OUTPUT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.2.0""").initCause(e) }
    if (TARGET in this) { arguments.target = get(TARGET)?.stringValue}
    arguments.internalArguments = parseCommandLineArguments<K2JSCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2JSCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS] = arguments.extensionFunctionsInExternals } catch (_: NoSuchMethodError) {  }
    try { this[X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT] = arguments.allowImplementableInterfacesExporting } catch (_: NoSuchMethodError) {  }
    try { this[X_ENABLE_SUSPEND_FUNCTION_EXPORTING] = arguments.allowExportingSuspendFunctions } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_ARROW_FUNCTIONS] = arguments.useEsArrowFunctions } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_CLASSES] = arguments.useEsClasses } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_GENERATORS] = arguments.useEsGenerators } catch (_: NoSuchMethodError) {  }
    try { this[X_ES_LONG_AS_BIGINT] = arguments.compileLongAsBigInt } catch (_: NoSuchMethodError) {  }
    try { this[X_GENERATE_POLYFILLS] = arguments.generatePolyfills } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_BUILD_CACHE] = arguments.irBuildCache } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS] = arguments.irGenerateInlineAnonymousFunctions } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_KEEP] = arguments.irKeep } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_MINIMIZED_MEMBER_NAMES] = arguments.irMinimizedMemberNames } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_FILE] = arguments.irPerFile } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_PER_MODULE] = arguments.irPerModule } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_SAFE_EXTERNAL_BOOLEAN] = arguments.irSafeExternalBoolean } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC] = arguments.irSafeExternalBooleanDiagnostic?.let { JsIrDiagnosticMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irSafeExternalBooleanDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-safe-external-boolean-diagnostic value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[X_OPTIMIZE_GENERATED_JS] = arguments.optimizeGeneratedJs } catch (_: NoSuchMethodError) {  }
    try { this[X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION] = arguments.platformArgumentsProviderJsExpression } catch (_: NoSuchMethodError) {  }
    try { this[X_TYPED_ARRAYS] = arguments.getUsingReflection("typedArrays") } catch (_: NoSuchMethodError) {  }
    try { this[MODULE_KIND] = arguments.moduleKind?.let { JsModuleKind.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::moduleKind, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[OUTPUT] = arguments.getUsingReflection("outputFile") } catch (_: NoSuchMethodError) {  }
    try { this[TARGET] = arguments.target?.let { JsEcmaVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") } } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2JSCompilerArguments = K2JSCompilerArguments()): K2JSCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    if (X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS in this) { arguments.extensionFunctionsInExternals = get(X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS)}
    if (X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT in this) { arguments.allowImplementableInterfacesExporting = get(X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT)}
    if (X_ENABLE_SUSPEND_FUNCTION_EXPORTING in this) { arguments.allowExportingSuspendFunctions = get(X_ENABLE_SUSPEND_FUNCTION_EXPORTING)}
    if (X_ES_ARROW_FUNCTIONS in this) { arguments.useEsArrowFunctions = get(X_ES_ARROW_FUNCTIONS)}
    if (X_ES_CLASSES in this) { arguments.useEsClasses = get(X_ES_CLASSES)}
    if (X_ES_GENERATORS in this) { arguments.useEsGenerators = get(X_ES_GENERATORS)}
    if (X_ES_LONG_AS_BIGINT in this) { arguments.compileLongAsBigInt = get(X_ES_LONG_AS_BIGINT)}
    if (X_GENERATE_POLYFILLS in this) { arguments.generatePolyfills = get(X_GENERATE_POLYFILLS)}
    if (X_IR_BUILD_CACHE in this) { arguments.irBuildCache = get(X_IR_BUILD_CACHE)}
    if (X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS in this) { arguments.irGenerateInlineAnonymousFunctions = get(X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS)}
    if (X_IR_KEEP in this) { arguments.irKeep = get(X_IR_KEEP)}
    if (X_IR_MINIMIZED_MEMBER_NAMES in this) { arguments.irMinimizedMemberNames = get(X_IR_MINIMIZED_MEMBER_NAMES)}
    if (X_IR_PER_FILE in this) { arguments.irPerFile = get(X_IR_PER_FILE)}
    if (X_IR_PER_MODULE in this) { arguments.irPerModule = get(X_IR_PER_MODULE)}
    if (X_IR_SAFE_EXTERNAL_BOOLEAN in this) { arguments.irSafeExternalBoolean = get(X_IR_SAFE_EXTERNAL_BOOLEAN)}
    if (X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC in this) { arguments.irSafeExternalBooleanDiagnostic = get(X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC)?.stringValue}
    if (X_OPTIMIZE_GENERATED_JS in this) { arguments.optimizeGeneratedJs = get(X_OPTIMIZE_GENERATED_JS)}
    if (X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION in this) { arguments.platformArgumentsProviderJsExpression = get(X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION)}
    try { if (X_TYPED_ARRAYS in this) { arguments.setUsingReflection("typedArrays", get(X_TYPED_ARRAYS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    if (MODULE_KIND in this) { arguments.moduleKind = get(MODULE_KIND)?.stringValue}
    try { if (OUTPUT in this) { arguments.setUsingReflection("outputFile", get(OUTPUT))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: OUTPUT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.2.0""").initCause(e) }
    if (TARGET in this) { arguments.target = get(TARGET)?.stringValue}
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    try {
      val compilerArgs: K2JSCompilerArguments = parseCommandLineArguments(arguments)
      collectRestrictedArgViolations(compilerArgs, K2JSCompilerArguments())
      validateArguments(compilerArgs.errors)?.let { throw CompilerArgumentsParseException(it) }
      applyCompilerArguments(compilerArgs)
    } catch (e: org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException) {
      _argumentValidationErrors.add(e.message ?: "Error parsing compiler arguments")
    }
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

    public val X_GENERATE_POLYFILLS: JsArgument<Boolean> = JsArgument("X_GENERATE_POLYFILLS")

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

    public val X_TYPED_ARRAYS: JsArgument<Boolean> = JsArgument("X_TYPED_ARRAYS")

    public val MODULE_KIND: JsArgument<JsModuleKind?> = JsArgument("MODULE_KIND")

    public val OUTPUT: JsArgument<String?> = JsArgument("OUTPUT")

    public val TARGET: JsArgument<JsEcmaVersion?> = JsArgument("TARGET")
  }
}
