// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.io.path.Path
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonCompilerArgumentsImpl(
  protected override val compilerArguments: CommonCompilerArguments,
  protected override val optionsMap: MutableMap<String, Any?>,
) : CommonToolArgumentsImpl(compilerArguments, optionsMap),
    ArgumentsCommonCompilerArguments,
    ArgumentsCommonCompilerArguments.Builder {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: CommonCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V = getOption(key.id) as V

  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    val currentKotlinVersion = KotlinToolingVersion(KC_VERSION)
    if (key.availableSinceVersion > KotlinReleaseVersion(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun getOption(keyId: String): Any? = when (keyId) {
    "P" -> {
    this.compilerArguments.pluginOptions
    }
    "XX_DEBUG_LEVEL_COMPILER_CHECKS" -> {
    try {
    try { this.compilerArguments.debugLevelCompilerChecks } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_DEBUG_LEVEL_COMPILER_CHECKS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "XX_EXPLICIT_RETURN_TYPES" -> {
    try {
    try { this.compilerArguments.explicitReturnTypes.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") } } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_EXPLICIT_RETURN_TYPES. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "XX_LENIENT_MODE" -> {
    try {
    try { this.compilerArguments.lenientMode } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_LENIENT_MODE. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" -> {
    this.compilerArguments.allowAnyScriptsInSourceRoots
    }
    "X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" -> {
    try {
    try { this.compilerArguments.allowConditionImpliesReturnsContracts } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" -> {
    try {
    try { this.compilerArguments.allowContractsOnMoreFunctions } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_HOLDSIN_CONTRACT" -> {
    try {
    try { this.compilerArguments.allowHoldsinContract } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_HOLDSIN_CONTRACT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_KOTLIN_PACKAGE" -> {
    this.compilerArguments.allowKotlinPackage
    }
    "X_ALLOW_REIFIED_TYPE_IN_CATCH" -> {
    try {
    try { this.compilerArguments.allowReifiedTypeInCatch } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_REIFIED_TYPE_IN_CATCH. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ANNOTATION_DEFAULT_TARGET" -> {
    try {
    try { this.compilerArguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") } } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATION_DEFAULT_TARGET. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ANNOTATION_TARGET_ALL" -> {
    try {
    try { this.compilerArguments.annotationTargetAll } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATION_TARGET_ALL. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_CHECK_PHASE_CONDITIONS" -> {
    this.compilerArguments.checkPhaseConditions
    }
    "X_COMMON_SOURCES" -> {
    this.compilerArguments.commonSources
    }
    "X_COMPILER_PLUGIN" -> {
    this.compilerArguments.pluginConfigurations
    }
    "X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" -> {
    try {
    try { this.compilerArguments.consistentDataClassCopyVisibility } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_CONTEXT_PARAMETERS" -> {
    try {
    try { this.compilerArguments.contextParameters } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_PARAMETERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_CONTEXT_RECEIVERS" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("contextReceivers") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_CONTEXT_SENSITIVE_RESOLUTION" -> {
    try {
    try { this.compilerArguments.contextSensitiveResolution } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_SENSITIVE_RESOLUTION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DATA_FLOW_BASED_EXHAUSTIVENESS" -> {
    try {
    try { this.compilerArguments.dataFlowBasedExhaustiveness } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DATA_FLOW_BASED_EXHAUSTIVENESS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DIRECT_JAVA_ACTUALIZATION" -> {
    try {
    try { this.compilerArguments.directJavaActualization } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DIRECT_JAVA_ACTUALIZATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" -> {
    this.compilerArguments.disableDefaultScriptingPlugin
    }
    "X_DISABLE_PHASES" -> {
    this.compilerArguments.disablePhases.toListOrEmpty()
    }
    "X_DONT_WARN_ON_ERROR_SUPPRESSION" -> {
    try {
    try { this.compilerArguments.dontWarnOnErrorSuppression } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DONT_WARN_ON_ERROR_SUPPRESSION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DUMP_DIRECTORY" -> {
    this.compilerArguments.dumpDirectory?.let { Path(it) }
    }
    "X_DUMP_FQNAME" -> {
    this.compilerArguments.dumpOnlyFqName
    }
    "X_DUMP_PERF" -> {
    this.compilerArguments.dumpPerf?.let { Path(it) }
    }
    "X_ENABLE_INCREMENTAL_COMPILATION" -> {
    this.compilerArguments.incrementalCompilation
    }
    "X_EXPECT_ACTUAL_CLASSES" -> {
    try {
    try { this.compilerArguments.expectActualClasses } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_EXPECT_ACTUAL_CLASSES. Current compiler version is: $KC_VERSION, but the argument was introduced in 1.9.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_EXPLICIT_API" -> {
    this.compilerArguments.explicitApi.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") }
    }
    "X_FRAGMENT_DEPENDENCY" -> {
    try {
    try { this.compilerArguments.fragmentDependencies } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_FRAGMENT_DEPENDENCY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_FRAGMENT_REFINES" -> {
    this.compilerArguments.fragmentRefines
    }
    "X_FRAGMENT_SOURCES" -> {
    this.compilerArguments.fragmentSources
    }
    "X_FRAGMENTS" -> {
    this.compilerArguments.fragments
    }
    "X_IGNORE_CONST_OPTIMIZATION_ERRORS" -> {
    this.compilerArguments.ignoreConstOptimizationErrors
    }
    "X_INLINE_CLASSES" -> {
    this.compilerArguments.inlineClasses
    }
    "X_INTELLIJ_PLUGIN_ROOT" -> {
    this.compilerArguments.intellijPluginRoot
    }
    "X_LIST_PHASES" -> {
    this.compilerArguments.listPhases
    }
    "X_METADATA_KLIB" -> {
    try {
    try { this.compilerArguments.metadataKlib } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_METADATA_KLIB. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_METADATA_VERSION" -> {
    this.compilerArguments.metadataVersion
    }
    "X_MULTI_DOLLAR_INTERPOLATION" -> {
    try {
    try { this.compilerArguments.multiDollarInterpolation } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_MULTI_DOLLAR_INTERPOLATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_MULTI_PLATFORM" -> {
    this.compilerArguments.multiPlatform
    }
    "X_NESTED_TYPE_ALIASES" -> {
    try {
    try { this.compilerArguments.nestedTypeAliases } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NESTED_TYPE_ALIASES. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_NEW_INFERENCE" -> {
    this.compilerArguments.newInference
    }
    "X_NO_CHECK_ACTUAL" -> {
    this.compilerArguments.noCheckActual
    }
    "X_NO_INLINE" -> {
    this.compilerArguments.noInline
    }
    "X_NON_LOCAL_BREAK_CONTINUE" -> {
    try {
    try { this.compilerArguments.nonLocalBreakContinue } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NON_LOCAL_BREAK_CONTINUE. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_PHASES_TO_DUMP" -> {
    this.compilerArguments.phasesToDump.toListOrEmpty()
    }
    "X_PHASES_TO_DUMP_AFTER" -> {
    this.compilerArguments.phasesToDumpAfter.toListOrEmpty()
    }
    "X_PHASES_TO_DUMP_BEFORE" -> {
    this.compilerArguments.phasesToDumpBefore.toListOrEmpty()
    }
    "X_PHASES_TO_VALIDATE" -> {
    this.compilerArguments.phasesToValidate.toListOrEmpty()
    }
    "X_PHASES_TO_VALIDATE_AFTER" -> {
    this.compilerArguments.phasesToValidateAfter.toListOrEmpty()
    }
    "X_PHASES_TO_VALIDATE_BEFORE" -> {
    this.compilerArguments.phasesToValidateBefore.toListOrEmpty()
    }
    "X_PLUGIN" -> {
    this.compilerArguments.pluginClasspaths
    }
    "X_PROFILE_PHASES" -> {
    this.compilerArguments.profilePhases
    }
    "X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" -> {
    this.compilerArguments.renderInternalDiagnosticNames
    }
    "X_REPL" -> {
    try {
    try { this.compilerArguments.repl } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_REPL. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_REPORT_ALL_WARNINGS" -> {
    try {
    try { this.compilerArguments.reportAllWarnings } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_REPORT_ALL_WARNINGS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_REPORT_OUTPUT_FILES" -> {
    this.compilerArguments.reportOutputFiles
    }
    "X_REPORT_PERF" -> {
    this.compilerArguments.reportPerf
    }
    "X_RETURN_VALUE_CHECKER" -> {
    try {
    try { this.compilerArguments.returnValueChecker.let { ReturnValueCheckerMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") } } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_RETURN_VALUE_CHECKER. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_SEPARATE_KMP_COMPILATION" -> {
    try {
    try { this.compilerArguments.separateKmpCompilationScheme } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SEPARATE_KMP_COMPILATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_SKIP_METADATA_VERSION_CHECK" -> {
    this.compilerArguments.skipMetadataVersionCheck
    }
    "X_SKIP_PRERELEASE_CHECK" -> {
    this.compilerArguments.skipPrereleaseCheck
    }
    "X_STDLIB_COMPILATION" -> {
    try {
    try { this.compilerArguments.stdlibCompilation } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_STDLIB_COMPILATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" -> {
    try {
    try { this.compilerArguments.getUsingReflection<Boolean>("suppressApiVersionGreaterThanLanguageVersionError") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0 and removed in 2.5.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_SUPPRESS_VERSION_WARNINGS" -> {
    this.compilerArguments.suppressVersionWarnings
    }
    "X_SUPPRESS_WARNING" -> {
    try {
    try { this.compilerArguments.suppressedDiagnostics.toListOrEmpty() } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_WARNING. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_UNRESTRICTED_BUILDER_INFERENCE" -> {
    this.compilerArguments.unrestrictedBuilderInference
    }
    "X_USE_FIR_EXPERIMENTAL_CHECKERS" -> {
    try {
    try { this.compilerArguments.useFirExperimentalCheckers } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_FIR_EXPERIMENTAL_CHECKERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_USE_FIR_IC" -> {
    this.compilerArguments.useFirIC
    }
    "X_USE_FIR_LT" -> {
    this.compilerArguments.useFirLT
    }
    "X_USE_K2" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("useK2") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2. Current compiler version is: $KC_VERSION, but the argument was removed in 2.2.0""").initCause(e) }
    }
    "X_VERBOSE_PHASES" -> {
    this.compilerArguments.verbosePhases.toListOrEmpty()
    }
    "X_VERIFY_IR" -> {
    try {
    try { this.compilerArguments.verifyIr?.let { VerifyIrMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") } } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_VERIFY_IR_VISIBILITY" -> {
    try {
    try { this.compilerArguments.getUsingReflection<Boolean>("verifyIrVisibility") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20 and removed in 2.4.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_WHEN_GUARDS" -> {
    try {
    try { this.compilerArguments.whenGuards } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WHEN_GUARDS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "API_VERSION" -> {
    this.compilerArguments.apiVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") }
    }
    "KOTLIN_HOME" -> {
    this.compilerArguments.kotlinHome?.let { Path(it) }
    }
    "LANGUAGE_VERSION" -> {
    this.compilerArguments.languageVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") }
    }
    "OPT_IN" -> {
    this.compilerArguments.optIn.toListOrEmpty()
    }
    "PROGRESSIVE" -> {
    this.compilerArguments.progressiveMode
    }
    "SCRIPT" -> {
    this.compilerArguments.script
    }
    "X_WARNING_LEVEL" -> {
    try {
        applyWarningLevels(null, compilerArguments)
    } catch (_: NoSuchMethodError) { null }
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
      "P" -> {
      this.compilerArguments.pluginOptions = (value as Array<String>?) ?: emptyArray()
      }
      "XX_DEBUG_LEVEL_COMPILER_CHECKS" -> {
      try {
      try { this.compilerArguments.debugLevelCompilerChecks = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_DEBUG_LEVEL_COMPILER_CHECKS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "XX_EXPLICIT_RETURN_TYPES" -> {
      try {
      try { this.compilerArguments.explicitReturnTypes = (value as ExplicitApiMode).stringValue
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_EXPLICIT_RETURN_TYPES. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "XX_LENIENT_MODE" -> {
      try {
      try { this.compilerArguments.lenientMode = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_LENIENT_MODE. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" -> {
      this.compilerArguments.allowAnyScriptsInSourceRoots = (value as Boolean)
      }
      "X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" -> {
      try {
      try { this.compilerArguments.allowConditionImpliesReturnsContracts = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" -> {
      try {
      try { this.compilerArguments.allowContractsOnMoreFunctions = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_HOLDSIN_CONTRACT" -> {
      try {
      try { this.compilerArguments.allowHoldsinContract = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_HOLDSIN_CONTRACT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_KOTLIN_PACKAGE" -> {
      this.compilerArguments.allowKotlinPackage = (value as Boolean)
      }
      "X_ALLOW_REIFIED_TYPE_IN_CATCH" -> {
      try {
      try { this.compilerArguments.allowReifiedTypeInCatch = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_REIFIED_TYPE_IN_CATCH. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_ANNOTATION_DEFAULT_TARGET" -> {
      try {
      try { this.compilerArguments.annotationDefaultTarget = (value as AnnotationDefaultTargetMode?)?.stringValue
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATION_DEFAULT_TARGET. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_ANNOTATION_TARGET_ALL" -> {
      try {
      try { this.compilerArguments.annotationTargetAll = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATION_TARGET_ALL. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_CHECK_PHASE_CONDITIONS" -> {
      this.compilerArguments.checkPhaseConditions = (value as Boolean)
      }
      "X_COMMON_SOURCES" -> {
      this.compilerArguments.commonSources = (value as Array<String>?) ?: emptyArray()
      }
      "X_COMPILER_PLUGIN" -> {
      this.compilerArguments.pluginConfigurations = (value as Array<String>?) ?: emptyArray()
      }
      "X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" -> {
      try {
      try { this.compilerArguments.consistentDataClassCopyVisibility = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_CONTEXT_PARAMETERS" -> {
      try {
      try { this.compilerArguments.contextParameters = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_PARAMETERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_CONTEXT_RECEIVERS" -> {
      try { this.compilerArguments.setUsingReflection("contextReceivers", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_CONTEXT_SENSITIVE_RESOLUTION" -> {
      try {
      try { this.compilerArguments.contextSensitiveResolution = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_SENSITIVE_RESOLUTION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_DATA_FLOW_BASED_EXHAUSTIVENESS" -> {
      try {
      try { this.compilerArguments.dataFlowBasedExhaustiveness = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DATA_FLOW_BASED_EXHAUSTIVENESS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_DIRECT_JAVA_ACTUALIZATION" -> {
      try {
      try { this.compilerArguments.directJavaActualization = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DIRECT_JAVA_ACTUALIZATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" -> {
      this.compilerArguments.disableDefaultScriptingPlugin = (value as Boolean)
      }
      "X_DISABLE_PHASES" -> {
      this.compilerArguments.disablePhases = (value as List<String>).toTypedArray()
      }
      "X_DONT_WARN_ON_ERROR_SUPPRESSION" -> {
      try {
      try { this.compilerArguments.dontWarnOnErrorSuppression = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DONT_WARN_ON_ERROR_SUPPRESSION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_DUMP_DIRECTORY" -> {
      this.compilerArguments.dumpDirectory = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "X_DUMP_FQNAME" -> {
      this.compilerArguments.dumpOnlyFqName = (value as String?)
      }
      "X_DUMP_PERF" -> {
      this.compilerArguments.dumpPerf = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "X_ENABLE_INCREMENTAL_COMPILATION" -> {
      this.compilerArguments.incrementalCompilation = (value as Boolean?)
      }
      "X_EXPECT_ACTUAL_CLASSES" -> {
      try {
      try { this.compilerArguments.expectActualClasses = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_EXPECT_ACTUAL_CLASSES. Current compiler version is: $KC_VERSION, but the argument was introduced in 1.9.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_EXPLICIT_API" -> {
      this.compilerArguments.explicitApi = (value as ExplicitApiMode).stringValue
      }
      "X_FRAGMENT_DEPENDENCY" -> {
      try {
      try { this.compilerArguments.fragmentDependencies = (value as Array<String>?) ?: emptyArray()
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_FRAGMENT_DEPENDENCY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_FRAGMENT_REFINES" -> {
      this.compilerArguments.fragmentRefines = (value as Array<String>?) ?: emptyArray()
      }
      "X_FRAGMENT_SOURCES" -> {
      this.compilerArguments.fragmentSources = (value as Array<String>?) ?: emptyArray()
      }
      "X_FRAGMENTS" -> {
      this.compilerArguments.fragments = (value as Array<String>?) ?: emptyArray()
      }
      "X_IGNORE_CONST_OPTIMIZATION_ERRORS" -> {
      this.compilerArguments.ignoreConstOptimizationErrors = (value as Boolean)
      }
      "X_INLINE_CLASSES" -> {
      this.compilerArguments.inlineClasses = (value as Boolean)
      }
      "X_INTELLIJ_PLUGIN_ROOT" -> {
      this.compilerArguments.intellijPluginRoot = (value as String?)
      }
      "X_LIST_PHASES" -> {
      this.compilerArguments.listPhases = (value as Boolean)
      }
      "X_METADATA_KLIB" -> {
      try {
      try { this.compilerArguments.metadataKlib = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_METADATA_KLIB. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_METADATA_VERSION" -> {
      this.compilerArguments.metadataVersion = (value as String?)
      }
      "X_MULTI_DOLLAR_INTERPOLATION" -> {
      try {
      try { this.compilerArguments.multiDollarInterpolation = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_MULTI_DOLLAR_INTERPOLATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_MULTI_PLATFORM" -> {
      this.compilerArguments.multiPlatform = (value as Boolean)
      }
      "X_NESTED_TYPE_ALIASES" -> {
      try {
      try { this.compilerArguments.nestedTypeAliases = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NESTED_TYPE_ALIASES. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_NEW_INFERENCE" -> {
      this.compilerArguments.newInference = (value as Boolean)
      }
      "X_NO_CHECK_ACTUAL" -> {
      this.compilerArguments.noCheckActual = (value as Boolean)
      }
      "X_NO_INLINE" -> {
      this.compilerArguments.noInline = (value as Boolean)
      }
      "X_NON_LOCAL_BREAK_CONTINUE" -> {
      try {
      try { this.compilerArguments.nonLocalBreakContinue = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NON_LOCAL_BREAK_CONTINUE. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_PHASES_TO_DUMP" -> {
      this.compilerArguments.phasesToDump = (value as List<String>).toTypedArray()
      }
      "X_PHASES_TO_DUMP_AFTER" -> {
      this.compilerArguments.phasesToDumpAfter = (value as List<String>).toTypedArray()
      }
      "X_PHASES_TO_DUMP_BEFORE" -> {
      this.compilerArguments.phasesToDumpBefore = (value as List<String>).toTypedArray()
      }
      "X_PHASES_TO_VALIDATE" -> {
      this.compilerArguments.phasesToValidate = (value as List<String>).toTypedArray()
      }
      "X_PHASES_TO_VALIDATE_AFTER" -> {
      this.compilerArguments.phasesToValidateAfter = (value as List<String>).toTypedArray()
      }
      "X_PHASES_TO_VALIDATE_BEFORE" -> {
      this.compilerArguments.phasesToValidateBefore = (value as List<String>).toTypedArray()
      }
      "X_PLUGIN" -> {
      this.compilerArguments.pluginClasspaths = (value as Array<String>?) ?: emptyArray()
      }
      "X_PROFILE_PHASES" -> {
      this.compilerArguments.profilePhases = (value as Boolean)
      }
      "X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" -> {
      this.compilerArguments.renderInternalDiagnosticNames = (value as Boolean)
      }
      "X_REPL" -> {
      try {
      try { this.compilerArguments.repl = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_REPL. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_REPORT_ALL_WARNINGS" -> {
      try {
      try { this.compilerArguments.reportAllWarnings = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_REPORT_ALL_WARNINGS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_REPORT_OUTPUT_FILES" -> {
      this.compilerArguments.reportOutputFiles = (value as Boolean)
      }
      "X_REPORT_PERF" -> {
      this.compilerArguments.reportPerf = (value as Boolean)
      }
      "X_RETURN_VALUE_CHECKER" -> {
      try {
      try { this.compilerArguments.returnValueChecker = (value as ReturnValueCheckerMode).stringValue
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_RETURN_VALUE_CHECKER. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_SEPARATE_KMP_COMPILATION" -> {
      try {
      try { this.compilerArguments.separateKmpCompilationScheme = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SEPARATE_KMP_COMPILATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_SKIP_METADATA_VERSION_CHECK" -> {
      this.compilerArguments.skipMetadataVersionCheck = (value as Boolean)
      }
      "X_SKIP_PRERELEASE_CHECK" -> {
      this.compilerArguments.skipPrereleaseCheck = (value as Boolean)
      }
      "X_STDLIB_COMPILATION" -> {
      try {
      try { this.compilerArguments.stdlibCompilation = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_STDLIB_COMPILATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" -> {
      try {
      try { this.compilerArguments.setUsingReflection("suppressApiVersionGreaterThanLanguageVersionError", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0 and removed in 2.5.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_SUPPRESS_VERSION_WARNINGS" -> {
      this.compilerArguments.suppressVersionWarnings = (value as Boolean)
      }
      "X_SUPPRESS_WARNING" -> {
      try {
      try { this.compilerArguments.suppressedDiagnostics = (value as List<String>).toTypedArray()
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_WARNING. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_UNRESTRICTED_BUILDER_INFERENCE" -> {
      this.compilerArguments.unrestrictedBuilderInference = (value as Boolean)
      }
      "X_USE_FIR_EXPERIMENTAL_CHECKERS" -> {
      try {
      try { this.compilerArguments.useFirExperimentalCheckers = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_FIR_EXPERIMENTAL_CHECKERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_USE_FIR_IC" -> {
      this.compilerArguments.useFirIC = (value as Boolean)
      }
      "X_USE_FIR_LT" -> {
      this.compilerArguments.useFirLT = (value as Boolean)
      }
      "X_USE_K2" -> {
      try { this.compilerArguments.setUsingReflection("useK2", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2. Current compiler version is: $KC_VERSION, but the argument was removed in 2.2.0""").initCause(e) }}
      "X_VERBOSE_PHASES" -> {
      this.compilerArguments.verbosePhases = (value as List<String>).toTypedArray()
      }
      "X_VERIFY_IR" -> {
      try {
      try { this.compilerArguments.verifyIr = (value as VerifyIrMode?)?.stringValue
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_VERIFY_IR_VISIBILITY" -> {
      try {
      try { this.compilerArguments.setUsingReflection("verifyIrVisibility", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20 and removed in 2.4.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_WHEN_GUARDS" -> {
      try {
      try { this.compilerArguments.whenGuards = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WHEN_GUARDS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "API_VERSION" -> {
      this.compilerArguments.apiVersion = (value as KotlinVersion?)?.stringValue
      }
      "KOTLIN_HOME" -> {
      this.compilerArguments.kotlinHome = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "LANGUAGE_VERSION" -> {
      this.compilerArguments.languageVersion = (value as KotlinVersion?)?.stringValue
      }
      "OPT_IN" -> {
      this.compilerArguments.optIn = (value as List<String>).toTypedArray()
      }
      "PROGRESSIVE" -> {
      this.compilerArguments.progressiveMode = (value as Boolean)
      }
      "SCRIPT" -> {
      this.compilerArguments.script = (value as Boolean)
      }
      "X_WARNING_LEVEL" -> {
      try {
          compilerArguments.applyWarningLevels(value as List<WarningLevel>)} catch (_: NoSuchMethodError) { }
      }
      else -> optionsMap[keyId] = value
    }
  }

  abstract override fun build(): CommonCompilerArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonCompilerArguments): CommonCompilerArguments {
    super.toCompilerArguments(arguments)
    return arguments
  }

  protected fun applyCompilerArguments(arguments: CommonCompilerArguments) {
    super.applyCompilerArguments(arguments)
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  public class CommonCompilerArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val P: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("P")

    public val XX_DEBUG_LEVEL_COMPILER_CHECKS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XX_DEBUG_LEVEL_COMPILER_CHECKS")

    public val XX_EXPLICIT_RETURN_TYPES: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("XX_EXPLICIT_RETURN_TYPES")

    public val XX_LENIENT_MODE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XX_LENIENT_MODE")

    public val X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    public val X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS")

    public val X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS")

    public val X_ALLOW_HOLDSIN_CONTRACT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_HOLDSIN_CONTRACT")

    public val X_ALLOW_KOTLIN_PACKAGE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_KOTLIN_PACKAGE")

    public val X_ALLOW_REIFIED_TYPE_IN_CATCH: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_REIFIED_TYPE_IN_CATCH")

    public val X_ANNOTATION_DEFAULT_TARGET: CommonCompilerArgument<AnnotationDefaultTargetMode?> =
        CommonCompilerArgument("X_ANNOTATION_DEFAULT_TARGET")

    public val X_ANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ANNOTATION_TARGET_ALL")

    public val X_CHECK_PHASE_CONDITIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CHECK_PHASE_CONDITIONS")

    public val X_COMMON_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMMON_SOURCES")

    public val X_COMPILER_PLUGIN: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMPILER_PLUGIN")

    public val X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY")

    public val X_CONTEXT_PARAMETERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_PARAMETERS")

    public val X_CONTEXT_RECEIVERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_RECEIVERS")

    public val X_CONTEXT_SENSITIVE_RESOLUTION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_SENSITIVE_RESOLUTION")

    public val X_DATA_FLOW_BASED_EXHAUSTIVENESS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DATA_FLOW_BASED_EXHAUSTIVENESS")

    public val X_DIRECT_JAVA_ACTUALIZATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DIRECT_JAVA_ACTUALIZATION")

    public val X_DISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")

    public val X_DISABLE_PHASES: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_DISABLE_PHASES")

    public val X_DONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_WARN_ON_ERROR_SUPPRESSION")

    public val X_DUMP_DIRECTORY: CommonCompilerArgument<java.nio.`file`.Path?> =
        CommonCompilerArgument("X_DUMP_DIRECTORY")

    public val X_DUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_FQNAME")

    public val X_DUMP_PERF: CommonCompilerArgument<java.nio.`file`.Path?> =
        CommonCompilerArgument("X_DUMP_PERF")

    public val X_ENABLE_INCREMENTAL_COMPILATION: CommonCompilerArgument<Boolean?> =
        CommonCompilerArgument("X_ENABLE_INCREMENTAL_COMPILATION")

    public val X_EXPECT_ACTUAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPECT_ACTUAL_CLASSES")

    public val X_EXPLICIT_API: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("X_EXPLICIT_API")

    public val X_FRAGMENT_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_DEPENDENCY")

    public val X_FRAGMENT_REFINES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_REFINES")

    public val X_FRAGMENT_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_SOURCES")

    public val X_FRAGMENTS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENTS")

    public val X_IGNORE_CONST_OPTIMIZATION_ERRORS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_IGNORE_CONST_OPTIMIZATION_ERRORS")

    public val X_INLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_INLINE_CLASSES")

    public val X_INTELLIJ_PLUGIN_ROOT: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_INTELLIJ_PLUGIN_ROOT")

    public val X_LIST_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_LIST_PHASES")

    public val X_METADATA_KLIB: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_METADATA_KLIB")

    public val X_METADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_METADATA_VERSION")

    public val X_MULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_DOLLAR_INTERPOLATION")

    public val X_MULTI_PLATFORM: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_PLATFORM")

    public val X_NESTED_TYPE_ALIASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NESTED_TYPE_ALIASES")

    public val X_NEW_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NEW_INFERENCE")

    public val X_NO_CHECK_ACTUAL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NO_CHECK_ACTUAL")

    public val X_NO_INLINE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_NO_INLINE")

    public val X_NON_LOCAL_BREAK_CONTINUE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NON_LOCAL_BREAK_CONTINUE")

    public val X_PHASES_TO_DUMP: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_PHASES_TO_DUMP")

    public val X_PHASES_TO_DUMP_AFTER: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_AFTER")

    public val X_PHASES_TO_DUMP_BEFORE: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_BEFORE")

    public val X_PHASES_TO_VALIDATE: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE")

    public val X_PHASES_TO_VALIDATE_AFTER: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_AFTER")

    public val X_PHASES_TO_VALIDATE_BEFORE: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_BEFORE")

    public val X_PLUGIN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("X_PLUGIN")

    public val X_PROFILE_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_PROFILE_PHASES")

    public val X_RENDER_INTERNAL_DIAGNOSTIC_NAMES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES")

    public val X_REPL: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_REPL")

    public val X_REPORT_ALL_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_ALL_WARNINGS")

    public val X_REPORT_OUTPUT_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_OUTPUT_FILES")

    public val X_REPORT_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_PERF")

    public val X_RETURN_VALUE_CHECKER: CommonCompilerArgument<ReturnValueCheckerMode> =
        CommonCompilerArgument("X_RETURN_VALUE_CHECKER")

    public val X_SEPARATE_KMP_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SEPARATE_KMP_COMPILATION")

    public val X_SKIP_METADATA_VERSION_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_METADATA_VERSION_CHECK")

    public val X_SKIP_PRERELEASE_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_PRERELEASE_CHECK")

    public val X_STDLIB_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_STDLIB_COMPILATION")

    public val X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR:
        CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR")

    public val X_SUPPRESS_VERSION_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_VERSION_WARNINGS")

    public val X_SUPPRESS_WARNING: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_SUPPRESS_WARNING")

    public val X_UNRESTRICTED_BUILDER_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_UNRESTRICTED_BUILDER_INFERENCE")

    public val X_USE_FIR_EXPERIMENTAL_CHECKERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_EXPERIMENTAL_CHECKERS")

    public val X_USE_FIR_IC: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_IC")

    public val X_USE_FIR_LT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_LT")

    public val X_USE_K2: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_USE_K2")

    public val X_VERBOSE_PHASES: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_VERBOSE_PHASES")

    public val X_VERIFY_IR: CommonCompilerArgument<VerifyIrMode?> =
        CommonCompilerArgument("X_VERIFY_IR")

    public val X_VERIFY_IR_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_VERIFY_IR_VISIBILITY")

    public val X_WHEN_GUARDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_WHEN_GUARDS")

    public val API_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("API_VERSION")

    public val KOTLIN_HOME: CommonCompilerArgument<java.nio.`file`.Path?> =
        CommonCompilerArgument("KOTLIN_HOME")

    public val LANGUAGE_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("LANGUAGE_VERSION")

    public val OPT_IN: CommonCompilerArgument<List<String>> = CommonCompilerArgument("OPT_IN")

    public val PROGRESSIVE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("PROGRESSIVE")

    public val SCRIPT: CommonCompilerArgument<Boolean> = CommonCompilerArgument("SCRIPT")

    public val X_WARNING_LEVEL: CommonCompilerArgument<List<WarningLevel>> =
        CommonCompilerArgument("X_WARNING_LEVEL")
  }
}
