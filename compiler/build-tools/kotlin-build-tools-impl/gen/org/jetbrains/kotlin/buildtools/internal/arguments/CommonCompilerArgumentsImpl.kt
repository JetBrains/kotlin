// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import java.lang.NoSuchMethodError
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
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.io.path.Path
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.HeaderMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonCompilerArgumentsImpl(
  protected override val compilerArguments: CommonCompilerArguments,
  protected override val optionsMap: MutableMap<String, Any?>,
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonToolArgumentsImpl(compilerArguments, optionsMap, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    ArgumentsCommonCompilerArguments,
    ArgumentsCommonCompilerArguments.Builder {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: CommonCompilerArgument<*>): Boolean = isArgumentKnown(key.id) 

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V = getOption(key.id) as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
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
    "XX_LANGUAGE" -> {
    try {
    this.compilerArguments.manuallyConfiguredFeatures
    } catch (_: NoSuchMethodError) { null }
    }
    "XX_DEBUG_LEVEL_COMPILER_CHECKS" -> {
    this.compilerArguments.debugLevelCompilerChecks
    }
    "XX_DUMP_MODEL" -> {
    try {
    this.compilerArguments.dumpArgumentsDir
    } catch (_: NoSuchMethodError) { null }
    }
    "XX_EXPLICIT_RETURN_TYPES" -> {
    this.compilerArguments.explicitReturnTypes.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::explicitReturnTypes, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") }
    }
    "XX_LENIENT_MODE" -> {
    this.compilerArguments.lenientMode
    }
    "X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" -> {
    this.compilerArguments.allowAnyScriptsInSourceRoots
    }
    "X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" -> {
    try {
    this.compilerArguments.allowConditionImpliesReturnsContracts
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" -> {
    try {
    this.compilerArguments.allowContractsOnMoreFunctions
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_HOLDSIN_CONTRACT" -> {
    try {
    this.compilerArguments.allowHoldsinContract
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_KOTLIN_PACKAGE" -> {
    this.compilerArguments.allowKotlinPackage
    }
    "X_ALLOW_REIFIED_TYPE_IN_CATCH" -> {
    try {
    this.compilerArguments.allowReifiedTypeInCatch
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ALLOW_RETURNS_RESULT_OF" -> {
    try {
    this.compilerArguments.allowReturnsResultOf
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ANNOTATION_DEFAULT_TARGET" -> {
    this.compilerArguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::annotationDefaultTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") }
    }
    "X_ANNOTATION_TARGET_ALL" -> {
    this.compilerArguments.annotationTargetAll
    }
    "X_CALLABLE_REFERENCES_TO_CONTEXTUAL" -> {
    try {
    this.compilerArguments.callableReferencesToContextual
    } catch (_: NoSuchMethodError) { null }
    }
    "X_CHECK_PHASE_CONDITIONS" -> {
    this.compilerArguments.checkPhaseConditions
    }
    "X_COLLECTION_LITERALS" -> {
    try {
    this.compilerArguments.collectionLiterals
    } catch (_: NoSuchMethodError) { null }
    }
    "X_COMMON_SOURCES" -> {
    this.compilerArguments.commonSources
    }
    "X_COMPANION_BLOCKS" -> {
    try {
    this.compilerArguments.companionBlocks
    } catch (_: NoSuchMethodError) { null }
    }
    "X_COMPANION_BLOCKS_AND_EXTENSIONS" -> {
    try {
    this.compilerArguments.companionBlocksAndExtensions
    } catch (_: NoSuchMethodError) { null }
    }
    "X_COMPILER_PLUGIN" -> {
    this.compilerArguments.pluginConfigurations
    }
    "X_COMPILER_PLUGIN_ORDER" -> {
    try {
    this.compilerArguments.pluginOrderConstraints
    } catch (_: NoSuchMethodError) { null }
    }
    "X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" -> {
    this.compilerArguments.consistentDataClassCopyVisibility
    }
    "X_CONTEXT_PARAMETERS" -> {
    this.compilerArguments.contextParameters
    }
    "X_CONTEXT_RECEIVERS" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("contextReceivers") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_CONTEXT_SENSITIVE_RESOLUTION" -> {
    this.compilerArguments.contextSensitiveResolution
    }
    "X_DATA_FLOW_BASED_EXHAUSTIVENESS" -> {
    try {
    this.compilerArguments.dataFlowBasedExhaustiveness
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DETAILED_PERF" -> {
    try {
    this.compilerArguments.detailedPerf
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DIRECT_JAVA_ACTUALIZATION" -> {
    this.compilerArguments.directJavaActualization
    }
    "X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" -> {
    this.compilerArguments.disableDefaultScriptingPlugin
    }
    "X_DISABLE_IR_CHECKERS" -> {
    try {
    this.compilerArguments.disableIrCheckers
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DISABLE_PHASES" -> {
    this.compilerArguments.disablePhases.toListOrEmpty()
    }
    "X_DONT_SORT_SOURCE_FILES" -> {
    try {
    this.compilerArguments.dontSortSourceFiles
    } catch (_: NoSuchMethodError) { null }
    }
    "X_DONT_WARN_ON_ERROR_SUPPRESSION" -> {
    this.compilerArguments.dontWarnOnErrorSuppression
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
    "X_EAGER_LAMBDA_ANALYSIS" -> {
    try {
    this.compilerArguments.eagerLambdaAnalysis
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ENABLE_ADDITIONAL_IR_CHECKERS" -> {
    try {
    this.compilerArguments.enableAdditionalIrCheckers
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ENABLE_INCREMENTAL_COMPILATION" -> {
    this.compilerArguments.incrementalCompilation
    }
    "X_EQUALITY_BOUNDS" -> {
    try {
    this.compilerArguments.equalityBounds
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ESCAPING_FUNCTIONS" -> {
    try {
    this.compilerArguments.escapingFunctions.toListOrEmpty()
    } catch (_: NoSuchMethodError) { null }
    }
    "X_EXPECT_ACTUAL_CLASSES" -> {
    this.compilerArguments.expectActualClasses
    }
    "X_EXPLICIT_API" -> {
    this.compilerArguments.explicitApi.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::explicitApi, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") }
    }
    "X_EXPLICIT_BACKING_FIELDS" -> {
    try {
    this.compilerArguments.explicitBackingFields
    } catch (_: NoSuchMethodError) { null }
    }
    "X_EXPLICIT_CONTEXT_ARGUMENTS" -> {
    try {
    this.compilerArguments.explicitContextArguments
    } catch (_: NoSuchMethodError) { null }
    }
    "X_FIR_AGGRESSIVE_PRUNING" -> {
    try {
    this.compilerArguments.firAggressivePruning
    } catch (_: NoSuchMethodError) { null }
    }
    "X_FRAGMENT_DEPENDENCY" -> {
    try {
    this.compilerArguments.fragmentDependencies
    } catch (_: NoSuchMethodError) { null }
    }
    "X_FRAGMENT_FRIEND_DEPENDENCY" -> {
    try {
    this.compilerArguments.fragmentFriendDependencies
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
    "X_HEADER_MODE" -> {
    try {
    this.compilerArguments.headerMode
    } catch (_: NoSuchMethodError) { null }
    }
    "X_HEADER_MODE_TYPE" -> {
    try {
    this.compilerArguments.headerModeType.let { HeaderMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::headerModeType, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xheader-mode-type value: $it") }
    } catch (_: NoSuchMethodError) { null }
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
    "X_INTRINSIC_CONST_EVALUATION" -> {
    try {
    this.compilerArguments.intrinsicConstEvaluation
    } catch (_: NoSuchMethodError) { null }
    }
    "X_LIST_PHASES" -> {
    this.compilerArguments.listPhases
    }
    "X_LOCAL_TYPE_ALIASES" -> {
    try {
    this.compilerArguments.localTypeAliases
    } catch (_: NoSuchMethodError) { null }
    }
    "X_METADATA_KLIB" -> {
    this.compilerArguments.metadataKlib
    }
    "X_METADATA_VERSION" -> {
    this.compilerArguments.metadataVersion
    }
    "X_MULTI_DOLLAR_INTERPOLATION" -> {
    this.compilerArguments.multiDollarInterpolation
    }
    "X_MULTI_PLATFORM" -> {
    this.compilerArguments.multiPlatform
    }
    "X_NAME_BASED_DESTRUCTURING" -> {
    try {
    this.compilerArguments.nameBasedDestructuring?.let { NameBasedDestructuringMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::nameBasedDestructuring, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xname-based-destructuring value: $it") }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_NESTED_TYPE_ALIASES" -> {
    this.compilerArguments.nestedTypeAliases
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
    this.compilerArguments.nonLocalBreakContinue
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
    "X_PRINT_CONFIGURATION" -> {
    try {
    this.compilerArguments.printConfiguration
    } catch (_: NoSuchMethodError) { null }
    }
    "X_PROFILE_PHASES" -> {
    this.compilerArguments.profilePhases
    }
    "X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" -> {
    this.compilerArguments.renderInternalDiagnosticNames
    }
    "X_REPL" -> {
    this.compilerArguments.repl
    }
    "X_REPORT_ALL_WARNINGS" -> {
    this.compilerArguments.reportAllWarnings
    }
    "X_REPORT_OUTPUT_FILES" -> {
    this.compilerArguments.reportOutputFiles
    }
    "X_REPORT_PERF" -> {
    this.compilerArguments.reportPerf
    }
    "X_RETURN_VALUE_CHECKER" -> {
    this.compilerArguments.returnValueChecker.let { ReturnValueCheckerMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::returnValueChecker, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") }
    }
    "X_SEPARATE_KMP_COMPILATION" -> {
    try {
    this.compilerArguments.separateKmpCompilationScheme
    } catch (_: NoSuchMethodError) { null }
    }
    "X_SKIP_METADATA_VERSION_CHECK" -> {
    this.compilerArguments.skipMetadataVersionCheck
    }
    "X_SKIP_PRERELEASE_CHECK" -> {
    this.compilerArguments.skipPrereleaseCheck
    }
    "X_STDLIB_COMPILATION" -> {
    this.compilerArguments.stdlibCompilation
    }
    "X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("suppressApiVersionGreaterThanLanguageVersionError") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_SUPPRESS_VERSION_WARNINGS" -> {
    this.compilerArguments.suppressVersionWarnings
    }
    "X_SUPPRESS_WARNING" -> {
    this.compilerArguments.suppressedDiagnostics.toListOrEmpty()
    }
    "X_UNRESTRICTED_BUILDER_INFERENCE" -> {
    this.compilerArguments.unrestrictedBuilderInference
    }
    "X_USE_FIR_EXPERIMENTAL_CHECKERS" -> {
    this.compilerArguments.useFirExperimentalCheckers
    }
    "X_USE_FIR_IC" -> {
    this.compilerArguments.useFirIC
    }
    "X_USE_FIR_LT" -> {
    this.compilerArguments.useFirLT
    }
    "X_VERBOSE_PHASES" -> {
    this.compilerArguments.verbosePhases.toListOrEmpty()
    }
    "X_VERIFY_IR" -> {
    this.compilerArguments.verifyIr?.let { VerifyIrMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::verifyIr, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") }
    }
    "X_VERIFY_IR_NESTED_OFFSETS" -> {
    try {
    try { this.compilerArguments.getUsingReflection<Boolean>("verifyIrNestedOffsets") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_NESTED_OFFSETS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.3.20 and removed in 2.4.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_VERIFY_IR_VISIBILITY" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("verifyIrVisibility") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    }
    "X_WHEN_GUARDS" -> {
    this.compilerArguments.whenGuards
    }
    "API_VERSION" -> {
    this.compilerArguments.apiVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::apiVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") }
    }
    "KOTLIN_HOME" -> {
    this.compilerArguments.kotlinHome?.let { Path(it) }
    }
    "LANGUAGE_VERSION" -> {
    this.compilerArguments.languageVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, this.compilerArguments::languageVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") }
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
    "COMPILER_PLUGINS" -> {
        if ("COMPILER_PLUGINS" in optionsMap) optionsMap["COMPILER_PLUGINS"] else emptyList<CompilerPlugin>()
    }
    "X_WARNING_LEVEL" -> {
    applyWarningLevels(null, compilerArguments)
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
      "XX_LANGUAGE" -> {
      try {
      this.compilerArguments.manuallyConfiguredFeatures = (value as Array<String>?) ?: emptyArray()
      } catch (_: NoSuchMethodError) { }
      }
      "XX_DEBUG_LEVEL_COMPILER_CHECKS" -> {
      this.compilerArguments.debugLevelCompilerChecks = (value as Boolean)
      }
      "XX_DUMP_MODEL" -> {
      try {
      this.compilerArguments.dumpArgumentsDir = (value as String?)
      } catch (_: NoSuchMethodError) { }
      }
      "XX_EXPLICIT_RETURN_TYPES" -> {
      this.compilerArguments.explicitReturnTypes = (value as ExplicitApiMode).stringValue
      }
      "XX_LENIENT_MODE" -> {
      this.compilerArguments.lenientMode = (value as Boolean)
      }
      "X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" -> {
      this.compilerArguments.allowAnyScriptsInSourceRoots = (value as Boolean)
      }
      "X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" -> {
      try {
      this.compilerArguments.allowConditionImpliesReturnsContracts = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" -> {
      try {
      this.compilerArguments.allowContractsOnMoreFunctions = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_HOLDSIN_CONTRACT" -> {
      try {
      this.compilerArguments.allowHoldsinContract = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_KOTLIN_PACKAGE" -> {
      this.compilerArguments.allowKotlinPackage = (value as Boolean)
      }
      "X_ALLOW_REIFIED_TYPE_IN_CATCH" -> {
      try {
      this.compilerArguments.allowReifiedTypeInCatch = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ALLOW_RETURNS_RESULT_OF" -> {
      try {
      this.compilerArguments.allowReturnsResultOf = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ANNOTATION_DEFAULT_TARGET" -> {
      this.compilerArguments.annotationDefaultTarget = (value as AnnotationDefaultTargetMode?)?.stringValue
      }
      "X_ANNOTATION_TARGET_ALL" -> {
      this.compilerArguments.annotationTargetAll = (value as Boolean)
      }
      "X_CALLABLE_REFERENCES_TO_CONTEXTUAL" -> {
      try {
      this.compilerArguments.callableReferencesToContextual = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_CHECK_PHASE_CONDITIONS" -> {
      this.compilerArguments.checkPhaseConditions = (value as Boolean)
      }
      "X_COLLECTION_LITERALS" -> {
      try {
      this.compilerArguments.collectionLiterals = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_COMMON_SOURCES" -> {
      this.compilerArguments.commonSources = (value as Array<String>?) ?: emptyArray()
      }
      "X_COMPANION_BLOCKS" -> {
      try {
      this.compilerArguments.companionBlocks = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_COMPANION_BLOCKS_AND_EXTENSIONS" -> {
      try {
      this.compilerArguments.companionBlocksAndExtensions = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_COMPILER_PLUGIN" -> {
      this.compilerArguments.pluginConfigurations = (value as Array<String>?) ?: emptyArray()
      }
      "X_COMPILER_PLUGIN_ORDER" -> {
      try {
      this.compilerArguments.pluginOrderConstraints = (value as Array<String>?) ?: emptyArray()
      } catch (_: NoSuchMethodError) { }
      }
      "X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" -> {
      this.compilerArguments.consistentDataClassCopyVisibility = (value as Boolean)
      }
      "X_CONTEXT_PARAMETERS" -> {
      this.compilerArguments.contextParameters = (value as Boolean)
      }
      "X_CONTEXT_RECEIVERS" -> {
      try { this.compilerArguments.setUsingReflection("contextReceivers", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_CONTEXT_SENSITIVE_RESOLUTION" -> {
      this.compilerArguments.contextSensitiveResolution = (value as Boolean)
      }
      "X_DATA_FLOW_BASED_EXHAUSTIVENESS" -> {
      try {
      this.compilerArguments.dataFlowBasedExhaustiveness = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_DETAILED_PERF" -> {
      try {
      this.compilerArguments.detailedPerf = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_DIRECT_JAVA_ACTUALIZATION" -> {
      this.compilerArguments.directJavaActualization = (value as Boolean)
      }
      "X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" -> {
      this.compilerArguments.disableDefaultScriptingPlugin = (value as Boolean)
      }
      "X_DISABLE_IR_CHECKERS" -> {
      try {
      this.compilerArguments.disableIrCheckers = (value as Array<String>?) ?: emptyArray()
      } catch (_: NoSuchMethodError) { }
      }
      "X_DISABLE_PHASES" -> {
      this.compilerArguments.disablePhases = (value as List<String>).toTypedArray()
      }
      "X_DONT_SORT_SOURCE_FILES" -> {
      try {
      this.compilerArguments.dontSortSourceFiles = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_DONT_WARN_ON_ERROR_SUPPRESSION" -> {
      this.compilerArguments.dontWarnOnErrorSuppression = (value as Boolean)
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
      "X_EAGER_LAMBDA_ANALYSIS" -> {
      try {
      this.compilerArguments.eagerLambdaAnalysis = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ENABLE_ADDITIONAL_IR_CHECKERS" -> {
      try {
      this.compilerArguments.enableAdditionalIrCheckers = (value as Array<String>?) ?: emptyArray()
      } catch (_: NoSuchMethodError) { }
      }
      "X_ENABLE_INCREMENTAL_COMPILATION" -> {
      this.compilerArguments.incrementalCompilation = (value as Boolean?)
      }
      "X_EQUALITY_BOUNDS" -> {
      try {
      this.compilerArguments.equalityBounds = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_ESCAPING_FUNCTIONS" -> {
      try {
      this.compilerArguments.escapingFunctions = (value as List<String>).toTypedArray()
      } catch (_: NoSuchMethodError) { }
      }
      "X_EXPECT_ACTUAL_CLASSES" -> {
      this.compilerArguments.expectActualClasses = (value as Boolean)
      }
      "X_EXPLICIT_API" -> {
      this.compilerArguments.explicitApi = (value as ExplicitApiMode).stringValue
      }
      "X_EXPLICIT_BACKING_FIELDS" -> {
      try {
      this.compilerArguments.explicitBackingFields = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_EXPLICIT_CONTEXT_ARGUMENTS" -> {
      try {
      this.compilerArguments.explicitContextArguments = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_FIR_AGGRESSIVE_PRUNING" -> {
      try {
      this.compilerArguments.firAggressivePruning = (value as Boolean?)
      } catch (_: NoSuchMethodError) { }
      }
      "X_FRAGMENT_DEPENDENCY" -> {
      try {
      this.compilerArguments.fragmentDependencies = (value as Array<String>?) ?: emptyArray()
      } catch (_: NoSuchMethodError) { }
      }
      "X_FRAGMENT_FRIEND_DEPENDENCY" -> {
      try {
      this.compilerArguments.fragmentFriendDependencies = (value as Array<String>?) ?: emptyArray()
      } catch (_: NoSuchMethodError) { }
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
      "X_HEADER_MODE" -> {
      try {
      this.compilerArguments.headerMode = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_HEADER_MODE_TYPE" -> {
      try {
      this.compilerArguments.headerModeType = (value as HeaderMode).stringValue
      } catch (_: NoSuchMethodError) { }
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
      "X_INTRINSIC_CONST_EVALUATION" -> {
      try {
      this.compilerArguments.intrinsicConstEvaluation = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_LIST_PHASES" -> {
      this.compilerArguments.listPhases = (value as Boolean)
      }
      "X_LOCAL_TYPE_ALIASES" -> {
      try {
      this.compilerArguments.localTypeAliases = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_METADATA_KLIB" -> {
      this.compilerArguments.metadataKlib = (value as Boolean)
      }
      "X_METADATA_VERSION" -> {
      this.compilerArguments.metadataVersion = (value as String?)
      }
      "X_MULTI_DOLLAR_INTERPOLATION" -> {
      this.compilerArguments.multiDollarInterpolation = (value as Boolean)
      }
      "X_MULTI_PLATFORM" -> {
      this.compilerArguments.multiPlatform = (value as Boolean)
      }
      "X_NAME_BASED_DESTRUCTURING" -> {
      try {
      this.compilerArguments.nameBasedDestructuring = (value as NameBasedDestructuringMode?)?.stringValue
      } catch (_: NoSuchMethodError) { }
      }
      "X_NESTED_TYPE_ALIASES" -> {
      this.compilerArguments.nestedTypeAliases = (value as Boolean)
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
      this.compilerArguments.nonLocalBreakContinue = (value as Boolean)
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
      "X_PRINT_CONFIGURATION" -> {
      try {
      this.compilerArguments.printConfiguration = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_PROFILE_PHASES" -> {
      this.compilerArguments.profilePhases = (value as Boolean)
      }
      "X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" -> {
      this.compilerArguments.renderInternalDiagnosticNames = (value as Boolean)
      }
      "X_REPL" -> {
      this.compilerArguments.repl = (value as Boolean)
      }
      "X_REPORT_ALL_WARNINGS" -> {
      this.compilerArguments.reportAllWarnings = (value as Boolean)
      }
      "X_REPORT_OUTPUT_FILES" -> {
      this.compilerArguments.reportOutputFiles = (value as Boolean)
      }
      "X_REPORT_PERF" -> {
      this.compilerArguments.reportPerf = (value as Boolean)
      }
      "X_RETURN_VALUE_CHECKER" -> {
      this.compilerArguments.returnValueChecker = (value as ReturnValueCheckerMode).stringValue
      }
      "X_SEPARATE_KMP_COMPILATION" -> {
      try {
      this.compilerArguments.separateKmpCompilationScheme = (value as Boolean)
      } catch (_: NoSuchMethodError) { }
      }
      "X_SKIP_METADATA_VERSION_CHECK" -> {
      this.compilerArguments.skipMetadataVersionCheck = (value as Boolean)
      }
      "X_SKIP_PRERELEASE_CHECK" -> {
      this.compilerArguments.skipPrereleaseCheck = (value as Boolean)
      }
      "X_STDLIB_COMPILATION" -> {
      this.compilerArguments.stdlibCompilation = (value as Boolean)
      }
      "X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" -> {
      try { this.compilerArguments.setUsingReflection("suppressApiVersionGreaterThanLanguageVersionError", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_SUPPRESS_VERSION_WARNINGS" -> {
      this.compilerArguments.suppressVersionWarnings = (value as Boolean)
      }
      "X_SUPPRESS_WARNING" -> {
      this.compilerArguments.suppressedDiagnostics = (value as List<String>).toTypedArray()
      }
      "X_UNRESTRICTED_BUILDER_INFERENCE" -> {
      this.compilerArguments.unrestrictedBuilderInference = (value as Boolean)
      }
      "X_USE_FIR_EXPERIMENTAL_CHECKERS" -> {
      this.compilerArguments.useFirExperimentalCheckers = (value as Boolean)
      }
      "X_USE_FIR_IC" -> {
      this.compilerArguments.useFirIC = (value as Boolean)
      }
      "X_USE_FIR_LT" -> {
      this.compilerArguments.useFirLT = (value as Boolean)
      }
      "X_VERBOSE_PHASES" -> {
      this.compilerArguments.verbosePhases = (value as List<String>).toTypedArray()
      }
      "X_VERIFY_IR" -> {
      this.compilerArguments.verifyIr = (value as VerifyIrMode?)?.stringValue
      }
      "X_VERIFY_IR_NESTED_OFFSETS" -> {
      try {
      try { this.compilerArguments.setUsingReflection("verifyIrNestedOffsets", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_NESTED_OFFSETS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.3.20 and removed in 2.4.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_VERIFY_IR_VISIBILITY" -> {
      try { this.compilerArguments.setUsingReflection("verifyIrVisibility", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }}
      "X_WHEN_GUARDS" -> {
      this.compilerArguments.whenGuards = (value as Boolean)
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
      compilerArguments.applyWarningLevels(value as List<WarningLevel>)}
      else -> optionsMap[keyId] = value
    }
  }

  abstract override fun build(): CommonCompilerArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonCompilerArguments): CommonCompilerArguments {
    super.toCompilerArguments(arguments)
    if (COMPILER_PLUGINS in this) { arguments.applyCompilerPlugins(get(COMPILER_PLUGINS))}
    return arguments
  }

  protected fun applyCompilerArguments(arguments: CommonCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[COMPILER_PLUGINS] = applyCompilerPlugins(if(COMPILER_PLUGINS in this) this[COMPILER_PLUGINS] else emptyList<CompilerPlugin>(), arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonCompilerArguments): CommonCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.pluginOptions = this.compilerArguments.pluginOptions
    arguments.manuallyConfiguredFeatures = this.compilerArguments.manuallyConfiguredFeatures
    arguments.debugLevelCompilerChecks = this.compilerArguments.debugLevelCompilerChecks
    arguments.explicitReturnTypes = this.compilerArguments.explicitReturnTypes
    arguments.lenientMode = this.compilerArguments.lenientMode
    arguments.allowAnyScriptsInSourceRoots = this.compilerArguments.allowAnyScriptsInSourceRoots
    arguments.allowConditionImpliesReturnsContracts = this.compilerArguments.allowConditionImpliesReturnsContracts
    arguments.allowContractsOnMoreFunctions = this.compilerArguments.allowContractsOnMoreFunctions
    arguments.allowHoldsinContract = this.compilerArguments.allowHoldsinContract
    arguments.allowKotlinPackage = this.compilerArguments.allowKotlinPackage
    arguments.allowReifiedTypeInCatch = this.compilerArguments.allowReifiedTypeInCatch
    arguments.allowReturnsResultOf = this.compilerArguments.allowReturnsResultOf
    arguments.annotationDefaultTarget = this.compilerArguments.annotationDefaultTarget
    arguments.annotationTargetAll = this.compilerArguments.annotationTargetAll
    arguments.callableReferencesToContextual = this.compilerArguments.callableReferencesToContextual
    arguments.checkPhaseConditions = this.compilerArguments.checkPhaseConditions
    arguments.collectionLiterals = this.compilerArguments.collectionLiterals
    arguments.commonSources = this.compilerArguments.commonSources
    arguments.companionBlocks = this.compilerArguments.companionBlocks
    arguments.companionBlocksAndExtensions = this.compilerArguments.companionBlocksAndExtensions
    arguments.pluginConfigurations = this.compilerArguments.pluginConfigurations
    arguments.pluginOrderConstraints = this.compilerArguments.pluginOrderConstraints
    arguments.consistentDataClassCopyVisibility = this.compilerArguments.consistentDataClassCopyVisibility
    arguments.contextParameters = this.compilerArguments.contextParameters
    try { arguments.setUsingReflection("contextReceivers", this.compilerArguments.getUsingReflection<Boolean>("contextReceivers")) } catch (_: NoSuchMethodError) { }
    arguments.contextSensitiveResolution = this.compilerArguments.contextSensitiveResolution
    arguments.dataFlowBasedExhaustiveness = this.compilerArguments.dataFlowBasedExhaustiveness
    arguments.directJavaActualization = this.compilerArguments.directJavaActualization
    arguments.disableDefaultScriptingPlugin = this.compilerArguments.disableDefaultScriptingPlugin
    arguments.disableIrCheckers = this.compilerArguments.disableIrCheckers
    arguments.disablePhases = this.compilerArguments.disablePhases
    arguments.dontSortSourceFiles = this.compilerArguments.dontSortSourceFiles
    arguments.dontWarnOnErrorSuppression = this.compilerArguments.dontWarnOnErrorSuppression
    arguments.eagerLambdaAnalysis = this.compilerArguments.eagerLambdaAnalysis
    arguments.enableAdditionalIrCheckers = this.compilerArguments.enableAdditionalIrCheckers
    arguments.incrementalCompilation = this.compilerArguments.incrementalCompilation
    arguments.equalityBounds = this.compilerArguments.equalityBounds
    arguments.escapingFunctions = this.compilerArguments.escapingFunctions
    arguments.expectActualClasses = this.compilerArguments.expectActualClasses
    arguments.explicitApi = this.compilerArguments.explicitApi
    arguments.explicitBackingFields = this.compilerArguments.explicitBackingFields
    arguments.explicitContextArguments = this.compilerArguments.explicitContextArguments
    arguments.firAggressivePruning = this.compilerArguments.firAggressivePruning
    arguments.fragmentDependencies = this.compilerArguments.fragmentDependencies
    arguments.fragmentFriendDependencies = this.compilerArguments.fragmentFriendDependencies
    arguments.fragmentRefines = this.compilerArguments.fragmentRefines
    arguments.fragmentSources = this.compilerArguments.fragmentSources
    arguments.fragments = this.compilerArguments.fragments
    arguments.headerMode = this.compilerArguments.headerMode
    arguments.headerModeType = this.compilerArguments.headerModeType
    arguments.ignoreConstOptimizationErrors = this.compilerArguments.ignoreConstOptimizationErrors
    arguments.inlineClasses = this.compilerArguments.inlineClasses
    arguments.intellijPluginRoot = this.compilerArguments.intellijPluginRoot
    arguments.intrinsicConstEvaluation = this.compilerArguments.intrinsicConstEvaluation
    arguments.localTypeAliases = this.compilerArguments.localTypeAliases
    arguments.metadataKlib = this.compilerArguments.metadataKlib
    arguments.metadataVersion = this.compilerArguments.metadataVersion
    arguments.multiDollarInterpolation = this.compilerArguments.multiDollarInterpolation
    arguments.multiPlatform = this.compilerArguments.multiPlatform
    arguments.nameBasedDestructuring = this.compilerArguments.nameBasedDestructuring
    arguments.nestedTypeAliases = this.compilerArguments.nestedTypeAliases
    arguments.newInference = this.compilerArguments.newInference
    arguments.noCheckActual = this.compilerArguments.noCheckActual
    arguments.noInline = this.compilerArguments.noInline
    arguments.nonLocalBreakContinue = this.compilerArguments.nonLocalBreakContinue
    arguments.phasesToValidate = this.compilerArguments.phasesToValidate
    arguments.phasesToValidateAfter = this.compilerArguments.phasesToValidateAfter
    arguments.phasesToValidateBefore = this.compilerArguments.phasesToValidateBefore
    arguments.pluginClasspaths = this.compilerArguments.pluginClasspaths
    arguments.renderInternalDiagnosticNames = this.compilerArguments.renderInternalDiagnosticNames
    arguments.repl = this.compilerArguments.repl
    arguments.returnValueChecker = this.compilerArguments.returnValueChecker
    arguments.separateKmpCompilationScheme = this.compilerArguments.separateKmpCompilationScheme
    arguments.skipMetadataVersionCheck = this.compilerArguments.skipMetadataVersionCheck
    arguments.skipPrereleaseCheck = this.compilerArguments.skipPrereleaseCheck
    arguments.stdlibCompilation = this.compilerArguments.stdlibCompilation
    try { arguments.setUsingReflection("suppressApiVersionGreaterThanLanguageVersionError", this.compilerArguments.getUsingReflection<Boolean>("suppressApiVersionGreaterThanLanguageVersionError")) } catch (_: NoSuchMethodError) { }
    arguments.suppressVersionWarnings = this.compilerArguments.suppressVersionWarnings
    arguments.suppressedDiagnostics = this.compilerArguments.suppressedDiagnostics
    arguments.unrestrictedBuilderInference = this.compilerArguments.unrestrictedBuilderInference
    arguments.useFirExperimentalCheckers = this.compilerArguments.useFirExperimentalCheckers
    arguments.useFirIC = this.compilerArguments.useFirIC
    arguments.useFirLT = this.compilerArguments.useFirLT
    arguments.verifyIr = this.compilerArguments.verifyIr
    try { arguments.setUsingReflection("verifyIrNestedOffsets", this.compilerArguments.getUsingReflection<Boolean>("verifyIrNestedOffsets")) } catch (_: NoSuchMethodError) { }
    try { arguments.setUsingReflection("verifyIrVisibility", this.compilerArguments.getUsingReflection<Boolean>("verifyIrVisibility")) } catch (_: NoSuchMethodError) { }
    arguments.whenGuards = this.compilerArguments.whenGuards
    arguments.apiVersion = this.compilerArguments.apiVersion
    arguments.kotlinHome = this.compilerArguments.kotlinHome
    arguments.languageVersion = this.compilerArguments.languageVersion
    arguments.optIn = this.compilerArguments.optIn
    arguments.progressiveMode = this.compilerArguments.progressiveMode
    arguments.script = this.compilerArguments.script
    if (COMPILER_PLUGINS in this) { arguments.applyCompilerPlugins(get(COMPILER_PLUGINS))}
    arguments.warningLevels = this.compilerArguments.warningLevels
    return arguments
  }

  @Suppress("DEPRECATION")
  internal override fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    super.collectRestrictedArgViolations(compilerArgs, defaultArgs)
    val args = compilerArgs as CommonCompilerArguments
    val castedDefaults = defaultArgs as CommonCompilerArguments
    if (args.repl != castedDefaults.repl) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-Xrepl' is not supported in the Build Tools API."))
    if (args.incrementalCompilation != castedDefaults.incrementalCompilation) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-Xenable-incremental-compilation' is not supported in the Build Tools API. Configure it via the JvmCompilationOperation.INCREMENTAL_COMPILATION option instead."))
  }

  public class CommonCompilerArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val P: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("P")

    public val XX_LANGUAGE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XX_LANGUAGE")

    public val XX_DEBUG_LEVEL_COMPILER_CHECKS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XX_DEBUG_LEVEL_COMPILER_CHECKS")

    public val XX_DUMP_MODEL: CommonCompilerArgument<String?> =
        CommonCompilerArgument("XX_DUMP_MODEL")

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

    public val X_ALLOW_RETURNS_RESULT_OF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_RETURNS_RESULT_OF")

    public val X_ANNOTATION_DEFAULT_TARGET: CommonCompilerArgument<AnnotationDefaultTargetMode?> =
        CommonCompilerArgument("X_ANNOTATION_DEFAULT_TARGET")

    public val X_ANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ANNOTATION_TARGET_ALL")

    public val X_CALLABLE_REFERENCES_TO_CONTEXTUAL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CALLABLE_REFERENCES_TO_CONTEXTUAL")

    public val X_CHECK_PHASE_CONDITIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CHECK_PHASE_CONDITIONS")

    public val X_COLLECTION_LITERALS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_COLLECTION_LITERALS")

    public val X_COMMON_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMMON_SOURCES")

    public val X_COMPANION_BLOCKS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_COMPANION_BLOCKS")

    public val X_COMPANION_BLOCKS_AND_EXTENSIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_COMPANION_BLOCKS_AND_EXTENSIONS")

    public val X_COMPILER_PLUGIN: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMPILER_PLUGIN")

    public val X_COMPILER_PLUGIN_ORDER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMPILER_PLUGIN_ORDER")

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

    public val X_DETAILED_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DETAILED_PERF")

    public val X_DIRECT_JAVA_ACTUALIZATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DIRECT_JAVA_ACTUALIZATION")

    public val X_DISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")

    public val X_DISABLE_IR_CHECKERS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_DISABLE_IR_CHECKERS")

    public val X_DISABLE_PHASES: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_DISABLE_PHASES")

    public val X_DONT_SORT_SOURCE_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_SORT_SOURCE_FILES")

    public val X_DONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_WARN_ON_ERROR_SUPPRESSION")

    public val X_DUMP_DIRECTORY: CommonCompilerArgument<java.nio.`file`.Path?> =
        CommonCompilerArgument("X_DUMP_DIRECTORY")

    public val X_DUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_FQNAME")

    public val X_DUMP_PERF: CommonCompilerArgument<java.nio.`file`.Path?> =
        CommonCompilerArgument("X_DUMP_PERF")

    public val X_EAGER_LAMBDA_ANALYSIS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EAGER_LAMBDA_ANALYSIS")

    public val X_ENABLE_ADDITIONAL_IR_CHECKERS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_ENABLE_ADDITIONAL_IR_CHECKERS")

    public val X_ENABLE_INCREMENTAL_COMPILATION: CommonCompilerArgument<Boolean?> =
        CommonCompilerArgument("X_ENABLE_INCREMENTAL_COMPILATION")

    public val X_EQUALITY_BOUNDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EQUALITY_BOUNDS")

    public val X_ESCAPING_FUNCTIONS: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_ESCAPING_FUNCTIONS")

    public val X_EXPECT_ACTUAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPECT_ACTUAL_CLASSES")

    public val X_EXPLICIT_API: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("X_EXPLICIT_API")

    public val X_EXPLICIT_BACKING_FIELDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPLICIT_BACKING_FIELDS")

    public val X_EXPLICIT_CONTEXT_ARGUMENTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPLICIT_CONTEXT_ARGUMENTS")

    public val X_FIR_AGGRESSIVE_PRUNING: CommonCompilerArgument<Boolean?> =
        CommonCompilerArgument("X_FIR_AGGRESSIVE_PRUNING")

    public val X_FRAGMENT_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_DEPENDENCY")

    public val X_FRAGMENT_FRIEND_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_FRIEND_DEPENDENCY")

    public val X_FRAGMENT_REFINES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_REFINES")

    public val X_FRAGMENT_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_SOURCES")

    public val X_FRAGMENTS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENTS")

    public val X_HEADER_MODE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_HEADER_MODE")

    public val X_HEADER_MODE_TYPE: CommonCompilerArgument<HeaderMode> =
        CommonCompilerArgument("X_HEADER_MODE_TYPE")

    public val X_IGNORE_CONST_OPTIMIZATION_ERRORS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_IGNORE_CONST_OPTIMIZATION_ERRORS")

    public val X_INLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_INLINE_CLASSES")

    public val X_INTELLIJ_PLUGIN_ROOT: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_INTELLIJ_PLUGIN_ROOT")

    public val X_INTRINSIC_CONST_EVALUATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_INTRINSIC_CONST_EVALUATION")

    public val X_LIST_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_LIST_PHASES")

    public val X_LOCAL_TYPE_ALIASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_LOCAL_TYPE_ALIASES")

    public val X_METADATA_KLIB: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_METADATA_KLIB")

    public val X_METADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_METADATA_VERSION")

    public val X_MULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_DOLLAR_INTERPOLATION")

    public val X_MULTI_PLATFORM: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_PLATFORM")

    public val X_NAME_BASED_DESTRUCTURING: CommonCompilerArgument<NameBasedDestructuringMode?> =
        CommonCompilerArgument("X_NAME_BASED_DESTRUCTURING")

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

    public val X_PRINT_CONFIGURATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_PRINT_CONFIGURATION")

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

    public val X_VERBOSE_PHASES: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_VERBOSE_PHASES")

    public val X_VERIFY_IR: CommonCompilerArgument<VerifyIrMode?> =
        CommonCompilerArgument("X_VERIFY_IR")

    public val X_VERIFY_IR_NESTED_OFFSETS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_VERIFY_IR_NESTED_OFFSETS")

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

    public val COMPILER_PLUGINS: CommonCompilerArgument<List<CompilerPlugin>> =
        CommonCompilerArgument("COMPILER_PLUGINS")

    public val X_WARNING_LEVEL: CommonCompilerArgument<List<WarningLevel>> =
        CommonCompilerArgument("X_WARNING_LEVEL")
  }
}
