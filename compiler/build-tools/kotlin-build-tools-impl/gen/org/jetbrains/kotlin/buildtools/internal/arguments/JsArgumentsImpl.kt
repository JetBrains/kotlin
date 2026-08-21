// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import java.lang.NoSuchMethodError
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
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyK2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class JsArgumentsImpl(
  protected override val compilerArguments: K2JSCompilerArguments = K2JSCompilerArguments(),
  protected override val optionsMap: MutableMap<String, Any?> = mutableMapOf(),
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonJsAndWasmArgumentsImpl(compilerArguments, optionsMap, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    JsCompilerArguments,
    JsCompilerArguments.Builder,
    JsCompilerKlibArguments,
    JsCompilerKlibArguments.Builder,
    JsCompilerLinkingArguments,
    JsCompilerLinkingArguments.Builder,
    DeepCopyable<JsArgumentsImpl> {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JsArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: JsArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: JsArgument<*>): Boolean = isArgumentKnown(key.id) 

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerArguments.JsCompilerArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerArguments.JsCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerKlibArguments.JsCompilerKlibArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JsCompilerLinkingArguments.JsCompilerLinkingArgument<V>, `value`: V) {
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
    "X_DTS_USE_UNKNOWN_INSTEAD_ANY" -> {
    try {
    this.compilerArguments.useUnknownInsteadAny
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS" -> {
    this.compilerArguments.extensionFunctionsInExternals
    }
    "X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT" -> {
    try {
    this.compilerArguments.allowImplementableInterfacesExporting
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ENABLE_SUSPEND_FUNCTION_EXPORTING" -> {
    try {
    this.compilerArguments.allowExportingSuspendFunctions
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ES_ARROW_FUNCTIONS" -> {
    this.compilerArguments.useEsArrowFunctions
    }
    "X_ES_CLASSES" -> {
    this.compilerArguments.useEsClasses
    }
    "X_ES_GENERATORS" -> {
    this.compilerArguments.useEsGenerators
    }
    "X_ES_LONG_AS_BIGINT" -> {
    try {
    this.compilerArguments.compileLongAsBigInt
    } catch (_: NoSuchMethodError) { null }
    }
    "X_GENERATE_POLYFILLS" -> {
    this.compilerArguments.generatePolyfills
    }
    "X_INTEGER_DIVISION_CHECK" -> {
    try {
    this.compilerArguments.integerDivisionCheck
    } catch (_: NoSuchMethodError) { null }
    }
    "X_IR_BUILD_CACHE" -> {
    this.compilerArguments.irBuildCache
    }
    "X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS" -> {
    this.compilerArguments.irGenerateInlineAnonymousFunctions
    }
    "X_IR_KEEP" -> {
    this.compilerArguments.irKeep
    }
    "X_IR_MINIMIZED_MEMBER_NAMES" -> {
    this.compilerArguments.irMinimizedMemberNames
    }
    "X_IR_PER_FILE" -> {
    this.compilerArguments.irPerFile
    }
    "X_IR_PER_MODULE" -> {
    this.compilerArguments.irPerModule
    }
    "X_IR_SAFE_EXTERNAL_BOOLEAN" -> {
    this.compilerArguments.irSafeExternalBoolean
    }
    "X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC" -> {
    this.compilerArguments.irSafeExternalBooleanDiagnostic?.let { JsIrDiagnosticMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::irSafeExternalBooleanDiagnostic, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xir-safe-external-boolean-diagnostic value: $it") }
    }
    "X_OPTIMIZE_GENERATED_JS" -> {
    this.compilerArguments.optimizeGeneratedJs
    }
    "X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION" -> {
    this.compilerArguments.platformArgumentsProviderJsExpression
    }
    "X_SUSPEND_LAMBDA_EXPORTING" -> {
    try {
    this.compilerArguments.allowExportingSuspendLambdas
    } catch (_: NoSuchMethodError) { null }
    }
    "X_TYPED_ARRAYS" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("typedArrays") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    }
    "MODULE_KIND" -> {
    this.compilerArguments.moduleKind?.let { JsModuleKind.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::moduleKind, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it") }
    }
    "TARGET" -> {
    this.compilerArguments.target?.let { JsEcmaVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::target, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -target value: $it") }
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
      "X_DTS_USE_UNKNOWN_INSTEAD_ANY" -> {
      try {
      this.compilerArguments.useUnknownInsteadAny = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ENABLE_EXTENSION_FUNCTIONS_IN_EXTERNALS" -> {
      this.compilerArguments.extensionFunctionsInExternals = (value as Boolean)
      }
      "X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT" -> {
      try {
      this.compilerArguments.allowImplementableInterfacesExporting = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ENABLE_SUSPEND_FUNCTION_EXPORTING" -> {
      try {
      this.compilerArguments.allowExportingSuspendFunctions = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ES_ARROW_FUNCTIONS" -> {
      this.compilerArguments.useEsArrowFunctions = (value as Boolean?)
      }
      "X_ES_CLASSES" -> {
      this.compilerArguments.useEsClasses = (value as Boolean?)
      }
      "X_ES_GENERATORS" -> {
      this.compilerArguments.useEsGenerators = (value as Boolean?)
      }
      "X_ES_LONG_AS_BIGINT" -> {
      try {
      this.compilerArguments.compileLongAsBigInt = (value as Boolean?)
      } catch (_: NoSuchMethodError) { }
      }
      "X_GENERATE_POLYFILLS" -> {
      this.compilerArguments.generatePolyfills = (value as Boolean)
      }
      "X_INTEGER_DIVISION_CHECK" -> {
      try {
      this.compilerArguments.integerDivisionCheck = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_IR_BUILD_CACHE" -> {
      this.compilerArguments.irBuildCache = (value as Boolean)
      }
      "X_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS" -> {
      this.compilerArguments.irGenerateInlineAnonymousFunctions = (value as Boolean)
      }
      "X_IR_KEEP" -> {
      this.compilerArguments.irKeep = (value as String?)
      }
      "X_IR_MINIMIZED_MEMBER_NAMES" -> {
      this.compilerArguments.irMinimizedMemberNames = (value as Boolean)
      }
      "X_IR_PER_FILE" -> {
      this.compilerArguments.irPerFile = (value as Boolean)
      }
      "X_IR_PER_MODULE" -> {
      this.compilerArguments.irPerModule = (value as Boolean)
      }
      "X_IR_SAFE_EXTERNAL_BOOLEAN" -> {
      this.compilerArguments.irSafeExternalBoolean = (value as Boolean)
      }
      "X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC" -> {
      this.compilerArguments.irSafeExternalBooleanDiagnostic = (value as JsIrDiagnosticMode?)?.stringValue
      }
      "X_OPTIMIZE_GENERATED_JS" -> {
      this.compilerArguments.optimizeGeneratedJs = (value as Boolean)
      }
      "X_PLATFORM_ARGUMENTS_IN_MAIN_FUNCTION" -> {
      this.compilerArguments.platformArgumentsProviderJsExpression = (value as String?)
      }
      "X_SUSPEND_LAMBDA_EXPORTING" -> {
      try {
      this.compilerArguments.allowExportingSuspendLambdas = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_TYPED_ARRAYS" -> {
      try { this.compilerArguments.setUsingReflection("typedArrays", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPED_ARRAYS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }}
      "MODULE_KIND" -> {
      this.compilerArguments.moduleKind = (value as JsModuleKind?)?.stringValue
      }
      "TARGET" -> {
      this.compilerArguments.target = (value as JsEcmaVersion?)?.stringValue
      }
      else -> optionsMap[keyId] = value
    }
  }

  override fun deepCopy(): JsArgumentsImpl = JsArgumentsImpl(org.jetbrains.kotlin.cli.common.arguments.copyK2JSCompilerArguments(this.compilerArguments, org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments()).also { newArgs -> newArgs.errors = this.compilerArguments.errors } , optionsMap.toMutableMap(), _argumentValidationErrors.toMutableSet(), restrictedArgViolations.toList(),  argumentParseDiagnostics.copy())

  override fun build(): JsArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2JSCompilerArguments {
    val arguments = copyK2JSCompilerArguments(compilerArguments, K2JSCompilerArguments()).also { newArgs -> newArgs.errors = compilerArguments.errors } 
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filterNot { isArgumentKnown(it) }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    populateExplicitArguments(arguments)
    return arguments
  }

  protected fun applyCompilerArguments(arguments: K2JSCompilerArguments) {
    copyK2JSCompilerArguments(arguments, this.compilerArguments).also { newArgs -> newArgs.errors = arguments.errors } 
    super.applyCompilerArguments(arguments)
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2JSCompilerArguments = K2JSCompilerArguments()): K2JSCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.useUnknownInsteadAny = this.compilerArguments.useUnknownInsteadAny
    arguments.extensionFunctionsInExternals = this.compilerArguments.extensionFunctionsInExternals
    arguments.allowImplementableInterfacesExporting = this.compilerArguments.allowImplementableInterfacesExporting
    arguments.allowExportingSuspendFunctions = this.compilerArguments.allowExportingSuspendFunctions
    arguments.useEsArrowFunctions = this.compilerArguments.useEsArrowFunctions
    arguments.useEsClasses = this.compilerArguments.useEsClasses
    arguments.useEsGenerators = this.compilerArguments.useEsGenerators
    arguments.compileLongAsBigInt = this.compilerArguments.compileLongAsBigInt
    arguments.generatePolyfills = this.compilerArguments.generatePolyfills
    arguments.integerDivisionCheck = this.compilerArguments.integerDivisionCheck
    arguments.irBuildCache = this.compilerArguments.irBuildCache
    arguments.irGenerateInlineAnonymousFunctions = this.compilerArguments.irGenerateInlineAnonymousFunctions
    arguments.irKeep = this.compilerArguments.irKeep
    arguments.irMinimizedMemberNames = this.compilerArguments.irMinimizedMemberNames
    arguments.irPerFile = this.compilerArguments.irPerFile
    arguments.irPerModule = this.compilerArguments.irPerModule
    arguments.irSafeExternalBoolean = this.compilerArguments.irSafeExternalBoolean
    arguments.irSafeExternalBooleanDiagnostic = this.compilerArguments.irSafeExternalBooleanDiagnostic
    arguments.optimizeGeneratedJs = this.compilerArguments.optimizeGeneratedJs
    arguments.platformArgumentsProviderJsExpression = this.compilerArguments.platformArgumentsProviderJsExpression
    arguments.allowExportingSuspendLambdas = this.compilerArguments.allowExportingSuspendLambdas
    try { arguments.setUsingReflection("typedArrays", this.compilerArguments.getUsingReflection<Boolean>("typedArrays")) } catch (_: NoSuchMethodError) { }
    arguments.moduleKind = this.compilerArguments.moduleKind
    arguments.target = this.compilerArguments.target
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2JSCompilerArguments = parseCommandLineArguments(arguments)
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
