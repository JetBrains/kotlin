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
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.buildMap
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.mutableMapOf
import kotlin.reflect.KMutableProperty1
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
import org.jetbrains.kotlin.buildtools.`internal`.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.`internal`.serializability.setPropertyByName
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
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerArguments.JsCompilerArgument<V>): V = try { JsArgumentValueAdapter.toApi(getPropertyByName(propertyAccessMap, key.id)) } catch (_: IllegalStateException) {
      check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
      optionsMap[key.id]                    
  } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerArguments.JsCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { setPropertyByName(propertyAccessMap, key.id, JsArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>): V = try { JsArgumentValueAdapter.toApi(getPropertyByName(propertyAccessMap, key.id)) } catch (_: IllegalStateException) {
      check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
      optionsMap[key.id]                    
  } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { setPropertyByName(propertyAccessMap, key.id, JsArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>): V = try { JsArgumentValueAdapter.toApi(getPropertyByName(propertyAccessMap, key.id)) } catch (_: IllegalStateException) {
      check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
      optionsMap[key.id]                    
  } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { setPropertyByName(propertyAccessMap, key.id, JsArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
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
    useUnknownInsteadAny = arguments.useUnknownInsteadAny
    extensionFunctionsInExternals = arguments.extensionFunctionsInExternals
    allowImplementableInterfacesExporting = arguments.allowImplementableInterfacesExporting
    allowExportingSuspendFunctions = arguments.allowExportingSuspendFunctions
    useEsArrowFunctions = arguments.useEsArrowFunctions
    useEsClasses = arguments.useEsClasses
    useEsGenerators = arguments.useEsGenerators
    compileLongAsBigInt = arguments.compileLongAsBigInt
    exportKDoc = arguments.exportKDoc
    generatePolyfills = arguments.generatePolyfills
    integerDivisionCheck = arguments.integerDivisionCheck
    irBuildCache = arguments.irBuildCache
    irGenerateInlineAnonymousFunctions = arguments.irGenerateInlineAnonymousFunctions
    irKeep = arguments.irKeep
    irMinimizedMemberNames = arguments.irMinimizedMemberNames
    irPerFile = arguments.irPerFile
    irPerModule = arguments.irPerModule
    irSafeExternalBoolean = arguments.irSafeExternalBoolean
    try { irSafeExternalBooleanDiagnostic = arguments.irSafeExternalBooleanDiagnostic?.let { JsIrDiagnosticMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::irSafeExternalBooleanDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-safe-external-boolean-diagnostic value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    optimizeGeneratedJs = arguments.optimizeGeneratedJs
    platformArgumentsProviderJsExpression = arguments.platformArgumentsProviderJsExpression
    allowExportingSuspendLambdas = arguments.allowExportingSuspendLambdas
    try { moduleKind = arguments.moduleKind?.let { JsModuleKind.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::moduleKind, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { target = arguments.target?.let { JsEcmaVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
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

  public companion object {
    private val propertyAccessMap: Map<String, KMutableProperty1<*, *>> = buildMap {
      put("X_DTS_USE_UNKNOWN_INSTEAD_ANY", JsArgumentsImpl::useUnknownInsteadAny)
      put("X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS", JsArgumentsImpl::extensionFunctionsInExternals)
      put("X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT", JsArgumentsImpl::allowImplementableInterfacesExporting)
      put("X_ENABLE_SUSPEND_FUNCTION_EXPORTING", JsArgumentsImpl::allowExportingSuspendFunctions)
      put("X_ES_ARROW_FUNCTIONS", JsArgumentsImpl::useEsArrowFunctions)
      put("X_ES_CLASSES", JsArgumentsImpl::useEsClasses)
      put("X_ES_GENERATORS", JsArgumentsImpl::useEsGenerators)
      put("X_ES_LONG_AS_BIGINT", JsArgumentsImpl::compileLongAsBigInt)
      put("X_EXPORT_KDOC", JsArgumentsImpl::exportKDoc)
      put("X_GENERATE_POLYFILLS", JsArgumentsImpl::generatePolyfills)
      put("X_INTEGER_DIVISION_CHECK", JsArgumentsImpl::integerDivisionCheck)
      put("X_IR_BUILD_CACHE", JsArgumentsImpl::irBuildCache)
      put("X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS", JsArgumentsImpl::irGenerateInlineAnonymousFunctions)
      put("X_IR_KEEP", JsArgumentsImpl::irKeep)
      put("X_IR_MINIMIZED_MEMBER_NAMES", JsArgumentsImpl::irMinimizedMemberNames)
      put("X_IR_PER_FILE", JsArgumentsImpl::irPerFile)
      put("X_IR_PER_MODULE", JsArgumentsImpl::irPerModule)
      put("X_IR_SAFE_EXTERNAL_BOOLEAN", JsArgumentsImpl::irSafeExternalBoolean)
      put("X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC", JsArgumentsImpl::irSafeExternalBooleanDiagnostic)
      put("X_OPTIMIZE_GENERATED_JS", JsArgumentsImpl::optimizeGeneratedJs)
      put("X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION", JsArgumentsImpl::platformArgumentsProviderJsExpression)
      put("X_SUSPEND_LAMBDA_EXPORTING", JsArgumentsImpl::allowExportingSuspendLambdas)
      put("MODULE_KIND", JsArgumentsImpl::moduleKind)
      put("TARGET", JsArgumentsImpl::target)
    }
  }
}
