// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.API_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.PROGRESSIVE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ALLOW_HOLDSIN_CONTRACT
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ALLOW_REIFIED_TYPE_IN_CATCH
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ANNOTATION_TARGET_ALL
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_CHECK_PHASE_CONDITIONS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_CONTEXT_PARAMETERS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_CONTEXT_RECEIVERS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_CONTEXT_SENSITIVE_RESOLUTION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DISABLE_DEFAULT_SCRIPTING_PLUGIN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DISABLE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DONT_WARN_ON_ERROR_SUPPRESSION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_FQNAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_PERF
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_EXPECT_ACTUAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_EXPLICIT_API
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_IGNORE_CONST_OPTIMIZATION_ERRORS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_INLINE_CLASSES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_LIST_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_METADATA_KLIB
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_METADATA_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_MULTI_DOLLAR_INTERPOLATION
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NAME_BASED_DESTRUCTURING
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NESTED_TYPE_ALIASES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NEW_INFERENCE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NON_LOCAL_BREAK_CONTINUE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NO_INLINE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP_AFTER
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP_BEFORE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE_AFTER
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE_BEFORE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PROFILE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_RENDER_INTERNAL_DIAGNOSTIC_NAMES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_REPORT_ALL_WARNINGS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_REPORT_OUTPUT_FILES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_REPORT_PERF
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_RETURN_VALUE_CHECKER
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SKIP_METADATA_VERSION_CHECK
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SKIP_PRERELEASE_CHECK
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SUPPRESS_VERSION_WARNINGS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_UNRESTRICTED_BUILDER_INFERENCE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_USE_FIR_EXPERIMENTAL_CHECKERS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_USE_FIR_IC
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_USE_FIR_LT
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERIFY_IR
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERIFY_IR_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_WHEN_GUARDS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion._XDATA_FLOW_BASED_EXHAUSTIVENESS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments

internal open class CommonCompilerArgumentsImpl : CommonToolArgumentsImpl(),
    ArgumentsCommonCompilerArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonCompilerArguments): CommonCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("LANGUAGE_VERSION" in optionsMap) { arguments.languageVersion = get(LANGUAGE_VERSION)?.stringValue }
    if ("API_VERSION" in optionsMap) { arguments.apiVersion = get(API_VERSION)?.stringValue }
    if ("KOTLIN_HOME" in optionsMap) { arguments.kotlinHome = get(KOTLIN_HOME) }
    if ("PROGRESSIVE" in optionsMap) { arguments.progressiveMode = get(PROGRESSIVE) }
    if ("OPT_IN" in optionsMap) { arguments.optIn = get(OPT_IN) }
    if ("X_NO_INLINE" in optionsMap) { arguments.noInline = get(X_NO_INLINE) }
    if ("X_SKIP_METADATA_VERSION_CHECK" in optionsMap) { arguments.skipMetadataVersionCheck = get(X_SKIP_METADATA_VERSION_CHECK) }
    if ("X_SKIP_PRERELEASE_CHECK" in optionsMap) { arguments.skipPrereleaseCheck = get(X_SKIP_PRERELEASE_CHECK) }
    if ("X_REPORT_OUTPUT_FILES" in optionsMap) { arguments.reportOutputFiles = get(X_REPORT_OUTPUT_FILES) }
    if ("X_NEW_INFERENCE" in optionsMap) { arguments.newInference = get(X_NEW_INFERENCE) }
    if ("X_INLINE_CLASSES" in optionsMap) { arguments.inlineClasses = get(X_INLINE_CLASSES) }
    if ("X_REPORT_PERF" in optionsMap) { arguments.reportPerf = get(X_REPORT_PERF) }
    if ("X_DUMP_PERF" in optionsMap) { arguments.dumpPerf = get(X_DUMP_PERF) }
    if ("X_METADATA_VERSION" in optionsMap) { arguments.metadataVersion = get(X_METADATA_VERSION) }
    if ("X_LIST_PHASES" in optionsMap) { arguments.listPhases = get(X_LIST_PHASES) }
    if ("X_DISABLE_PHASES" in optionsMap) { arguments.disablePhases = get(X_DISABLE_PHASES) }
    if ("X_VERBOSE_PHASES" in optionsMap) { arguments.verbosePhases = get(X_VERBOSE_PHASES) }
    if ("X_PHASES_TO_DUMP_BEFORE" in optionsMap) { arguments.phasesToDumpBefore = get(X_PHASES_TO_DUMP_BEFORE) }
    if ("X_PHASES_TO_DUMP_AFTER" in optionsMap) { arguments.phasesToDumpAfter = get(X_PHASES_TO_DUMP_AFTER) }
    if ("X_PHASES_TO_DUMP" in optionsMap) { arguments.phasesToDump = get(X_PHASES_TO_DUMP) }
    if ("X_DUMP_DIRECTORY" in optionsMap) { arguments.dumpDirectory = get(X_DUMP_DIRECTORY) }
    if ("X_DUMP_FQNAME" in optionsMap) { arguments.dumpOnlyFqName = get(X_DUMP_FQNAME) }
    if ("X_PHASES_TO_VALIDATE_BEFORE" in optionsMap) { arguments.phasesToValidateBefore = get(X_PHASES_TO_VALIDATE_BEFORE) }
    if ("X_PHASES_TO_VALIDATE_AFTER" in optionsMap) { arguments.phasesToValidateAfter = get(X_PHASES_TO_VALIDATE_AFTER) }
    if ("X_PHASES_TO_VALIDATE" in optionsMap) { arguments.phasesToValidate = get(X_PHASES_TO_VALIDATE) }
    if ("X_VERIFY_IR" in optionsMap) { arguments.verifyIr = get(X_VERIFY_IR) }
    if ("X_VERIFY_IR_VISIBILITY" in optionsMap) { arguments.verifyIrVisibility = get(X_VERIFY_IR_VISIBILITY) }
    if ("X_PROFILE_PHASES" in optionsMap) { arguments.profilePhases = get(X_PROFILE_PHASES) }
    if ("X_CHECK_PHASE_CONDITIONS" in optionsMap) { arguments.checkPhaseConditions = get(X_CHECK_PHASE_CONDITIONS) }
    if ("X_USE_FIR_EXPERIMENTAL_CHECKERS" in optionsMap) { arguments.useFirExperimentalCheckers = get(X_USE_FIR_EXPERIMENTAL_CHECKERS) }
    if ("X_USE_FIR_IC" in optionsMap) { arguments.useFirIC = get(X_USE_FIR_IC) }
    if ("X_USE_FIR_LT" in optionsMap) { arguments.useFirLT = get(X_USE_FIR_LT) }
    if ("X_METADATA_KLIB" in optionsMap) { arguments.metadataKlib = get(X_METADATA_KLIB) }
    if ("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" in optionsMap) { arguments.disableDefaultScriptingPlugin = get(X_DISABLE_DEFAULT_SCRIPTING_PLUGIN) }
    if ("X_EXPLICIT_API" in optionsMap) { arguments.explicitApi = get(X_EXPLICIT_API).stringValue }
    if ("X_RETURN_VALUE_CHECKER" in optionsMap) { arguments.returnValueChecker = get(X_RETURN_VALUE_CHECKER).stringValue }
    if ("X_SUPPRESS_VERSION_WARNINGS" in optionsMap) { arguments.suppressVersionWarnings = get(X_SUPPRESS_VERSION_WARNINGS) }
    if ("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" in optionsMap) { arguments.suppressApiVersionGreaterThanLanguageVersionError = get(X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR) }
    if ("X_EXPECT_ACTUAL_CLASSES" in optionsMap) { arguments.expectActualClasses = get(X_EXPECT_ACTUAL_CLASSES) }
    if ("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" in optionsMap) { arguments.consistentDataClassCopyVisibility = get(X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY) }
    if ("X_UNRESTRICTED_BUILDER_INFERENCE" in optionsMap) { arguments.unrestrictedBuilderInference = get(X_UNRESTRICTED_BUILDER_INFERENCE) }
    if ("X_CONTEXT_RECEIVERS" in optionsMap) { arguments.contextReceivers = get(X_CONTEXT_RECEIVERS) }
    if ("X_CONTEXT_PARAMETERS" in optionsMap) { arguments.contextParameters = get(X_CONTEXT_PARAMETERS) }
    if ("X_CONTEXT_SENSITIVE_RESOLUTION" in optionsMap) { arguments.contextSensitiveResolution = get(X_CONTEXT_SENSITIVE_RESOLUTION) }
    if ("X_NON_LOCAL_BREAK_CONTINUE" in optionsMap) { arguments.nonLocalBreakContinue = get(X_NON_LOCAL_BREAK_CONTINUE) }
    if ("_XDATA_FLOW_BASED_EXHAUSTIVENESS" in optionsMap) { arguments.xdataFlowBasedExhaustiveness = get(_XDATA_FLOW_BASED_EXHAUSTIVENESS) }
    if ("X_MULTI_DOLLAR_INTERPOLATION" in optionsMap) { arguments.multiDollarInterpolation = get(X_MULTI_DOLLAR_INTERPOLATION) }
    if ("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" in optionsMap) { arguments.renderInternalDiagnosticNames = get(X_RENDER_INTERNAL_DIAGNOSTIC_NAMES) }
    if ("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" in optionsMap) { arguments.allowAnyScriptsInSourceRoots = get(X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS) }
    if ("X_REPORT_ALL_WARNINGS" in optionsMap) { arguments.reportAllWarnings = get(X_REPORT_ALL_WARNINGS) }
    if ("X_IGNORE_CONST_OPTIMIZATION_ERRORS" in optionsMap) { arguments.ignoreConstOptimizationErrors = get(X_IGNORE_CONST_OPTIMIZATION_ERRORS) }
    if ("X_DONT_WARN_ON_ERROR_SUPPRESSION" in optionsMap) { arguments.dontWarnOnErrorSuppression = get(X_DONT_WARN_ON_ERROR_SUPPRESSION) }
    if ("X_WHEN_GUARDS" in optionsMap) { arguments.whenGuards = get(X_WHEN_GUARDS) }
    if ("X_NESTED_TYPE_ALIASES" in optionsMap) { arguments.nestedTypeAliases = get(X_NESTED_TYPE_ALIASES) }
    if ("X_SUPPRESS_WARNING" in optionsMap) { arguments.suppressedDiagnostics = get(X_SUPPRESS_WARNING) }
    if ("X_WARNING_LEVEL" in optionsMap) { arguments.warningLevels = get(X_WARNING_LEVEL) }
    if ("X_ANNOTATION_DEFAULT_TARGET" in optionsMap) { arguments.annotationDefaultTarget = get(X_ANNOTATION_DEFAULT_TARGET) }
    if ("X_ANNOTATION_TARGET_ALL" in optionsMap) { arguments.annotationTargetAll = get(X_ANNOTATION_TARGET_ALL) }
    if ("X_ALLOW_REIFIED_TYPE_IN_CATCH" in optionsMap) { arguments.allowReifiedTypeInCatch = get(X_ALLOW_REIFIED_TYPE_IN_CATCH) }
    if ("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" in optionsMap) { arguments.allowContractsOnMoreFunctions = get(X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS) }
    if ("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" in optionsMap) { arguments.allowConditionImpliesReturnsContracts = get(X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS) }
    if ("X_ALLOW_HOLDSIN_CONTRACT" in optionsMap) { arguments.allowHoldsinContract = get(X_ALLOW_HOLDSIN_CONTRACT) }
    if ("X_NAME_BASED_DESTRUCTURING" in optionsMap) { arguments.nameBasedDestructuring = get(X_NAME_BASED_DESTRUCTURING) }
    return arguments
  }

  @Suppress("DEPRECATION")
  override fun applyArgumentStrings(arguments: List<String>) {
    super.applyArgumentStrings(arguments)
    val compilerArgs: CommonCompilerArguments = parseCommandLineArguments(arguments)
    this[LANGUAGE_VERSION] = compilerArgs.languageVersion?.let { KotlinVersion.valueOf(it) }
    this[API_VERSION] = compilerArgs.apiVersion?.let { KotlinVersion.valueOf(it) }
    this[KOTLIN_HOME] = compilerArgs.kotlinHome
    this[PROGRESSIVE] = compilerArgs.progressiveMode
    this[OPT_IN] = compilerArgs.optIn
    this[X_NO_INLINE] = compilerArgs.noInline
    this[X_SKIP_METADATA_VERSION_CHECK] = compilerArgs.skipMetadataVersionCheck
    this[X_SKIP_PRERELEASE_CHECK] = compilerArgs.skipPrereleaseCheck
    this[X_REPORT_OUTPUT_FILES] = compilerArgs.reportOutputFiles
    this[X_NEW_INFERENCE] = compilerArgs.newInference
    this[X_INLINE_CLASSES] = compilerArgs.inlineClasses
    this[X_REPORT_PERF] = compilerArgs.reportPerf
    this[X_DUMP_PERF] = compilerArgs.dumpPerf
    this[X_METADATA_VERSION] = compilerArgs.metadataVersion
    this[X_LIST_PHASES] = compilerArgs.listPhases
    this[X_DISABLE_PHASES] = compilerArgs.disablePhases
    this[X_VERBOSE_PHASES] = compilerArgs.verbosePhases
    this[X_PHASES_TO_DUMP_BEFORE] = compilerArgs.phasesToDumpBefore
    this[X_PHASES_TO_DUMP_AFTER] = compilerArgs.phasesToDumpAfter
    this[X_PHASES_TO_DUMP] = compilerArgs.phasesToDump
    this[X_DUMP_DIRECTORY] = compilerArgs.dumpDirectory
    this[X_DUMP_FQNAME] = compilerArgs.dumpOnlyFqName
    this[X_PHASES_TO_VALIDATE_BEFORE] = compilerArgs.phasesToValidateBefore
    this[X_PHASES_TO_VALIDATE_AFTER] = compilerArgs.phasesToValidateAfter
    this[X_PHASES_TO_VALIDATE] = compilerArgs.phasesToValidate
    this[X_VERIFY_IR] = compilerArgs.verifyIr
    this[X_VERIFY_IR_VISIBILITY] = compilerArgs.verifyIrVisibility
    this[X_PROFILE_PHASES] = compilerArgs.profilePhases
    this[X_CHECK_PHASE_CONDITIONS] = compilerArgs.checkPhaseConditions
    this[X_USE_FIR_EXPERIMENTAL_CHECKERS] = compilerArgs.useFirExperimentalCheckers
    this[X_USE_FIR_IC] = compilerArgs.useFirIC
    this[X_USE_FIR_LT] = compilerArgs.useFirLT
    this[X_METADATA_KLIB] = compilerArgs.metadataKlib
    this[X_DISABLE_DEFAULT_SCRIPTING_PLUGIN] = compilerArgs.disableDefaultScriptingPlugin
    this[X_EXPLICIT_API] = compilerArgs.explicitApi.let { ExplicitApiMode.valueOf(it) }
    this[X_RETURN_VALUE_CHECKER] = compilerArgs.returnValueChecker.let { ReturnValueCheckerMode.valueOf(it) }
    this[X_SUPPRESS_VERSION_WARNINGS] = compilerArgs.suppressVersionWarnings
    this[X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR] = compilerArgs.suppressApiVersionGreaterThanLanguageVersionError
    this[X_EXPECT_ACTUAL_CLASSES] = compilerArgs.expectActualClasses
    this[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = compilerArgs.consistentDataClassCopyVisibility
    this[X_UNRESTRICTED_BUILDER_INFERENCE] = compilerArgs.unrestrictedBuilderInference
    this[X_CONTEXT_RECEIVERS] = compilerArgs.contextReceivers
    this[X_CONTEXT_PARAMETERS] = compilerArgs.contextParameters
    this[X_CONTEXT_SENSITIVE_RESOLUTION] = compilerArgs.contextSensitiveResolution
    this[X_NON_LOCAL_BREAK_CONTINUE] = compilerArgs.nonLocalBreakContinue
    this[_XDATA_FLOW_BASED_EXHAUSTIVENESS] = compilerArgs.xdataFlowBasedExhaustiveness
    this[X_MULTI_DOLLAR_INTERPOLATION] = compilerArgs.multiDollarInterpolation
    this[X_RENDER_INTERNAL_DIAGNOSTIC_NAMES] = compilerArgs.renderInternalDiagnosticNames
    this[X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS] = compilerArgs.allowAnyScriptsInSourceRoots
    this[X_REPORT_ALL_WARNINGS] = compilerArgs.reportAllWarnings
    this[X_IGNORE_CONST_OPTIMIZATION_ERRORS] = compilerArgs.ignoreConstOptimizationErrors
    this[X_DONT_WARN_ON_ERROR_SUPPRESSION] = compilerArgs.dontWarnOnErrorSuppression
    this[X_WHEN_GUARDS] = compilerArgs.whenGuards
    this[X_NESTED_TYPE_ALIASES] = compilerArgs.nestedTypeAliases
    this[X_SUPPRESS_WARNING] = compilerArgs.suppressedDiagnostics
    this[X_WARNING_LEVEL] = compilerArgs.warningLevels
    this[X_ANNOTATION_DEFAULT_TARGET] = compilerArgs.annotationDefaultTarget
    this[X_ANNOTATION_TARGET_ALL] = compilerArgs.annotationTargetAll
    this[X_ALLOW_REIFIED_TYPE_IN_CATCH] = compilerArgs.allowReifiedTypeInCatch
    this[X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS] = compilerArgs.allowContractsOnMoreFunctions
    this[X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS] = compilerArgs.allowConditionImpliesReturnsContracts
    this[X_ALLOW_HOLDSIN_CONTRACT] = compilerArgs.allowHoldsinContract
    this[X_NAME_BASED_DESTRUCTURING] = compilerArgs.nameBasedDestructuring
  }

  @Suppress("DEPRECATION")
  @OptIn(ExperimentalCompilerArgument::class)
  override fun toArgumentStrings(): List<String> {
    val arguments = mutableListOf<String>()
    arguments.addAll(super.toArgumentStrings())
    if ("LANGUAGE_VERSION" in optionsMap) { arguments.add("-language-version=" + get(LANGUAGE_VERSION)?.stringValue) }
    if ("API_VERSION" in optionsMap) { arguments.add("-api-version=" + get(API_VERSION)?.stringValue) }
    if ("KOTLIN_HOME" in optionsMap) { arguments.add("-kotlin-home=" + get(KOTLIN_HOME)) }
    if ("PROGRESSIVE" in optionsMap) { arguments.add("-progressive=" + get(PROGRESSIVE)) }
    if ("OPT_IN" in optionsMap) { arguments.add("-opt-in=" + get(OPT_IN)) }
    if ("X_NO_INLINE" in optionsMap) { arguments.add("-Xno-inline=" + get(X_NO_INLINE)) }
    if ("X_SKIP_METADATA_VERSION_CHECK" in optionsMap) { arguments.add("-Xskip-metadata-version-check=" + get(X_SKIP_METADATA_VERSION_CHECK)) }
    if ("X_SKIP_PRERELEASE_CHECK" in optionsMap) { arguments.add("-Xskip-prerelease-check=" + get(X_SKIP_PRERELEASE_CHECK)) }
    if ("X_REPORT_OUTPUT_FILES" in optionsMap) { arguments.add("-Xreport-output-files=" + get(X_REPORT_OUTPUT_FILES)) }
    if ("X_NEW_INFERENCE" in optionsMap) { arguments.add("-Xnew-inference=" + get(X_NEW_INFERENCE)) }
    if ("X_INLINE_CLASSES" in optionsMap) { arguments.add("-Xinline-classes=" + get(X_INLINE_CLASSES)) }
    if ("X_REPORT_PERF" in optionsMap) { arguments.add("-Xreport-perf=" + get(X_REPORT_PERF)) }
    if ("X_DUMP_PERF" in optionsMap) { arguments.add("-Xdump-perf=" + get(X_DUMP_PERF)) }
    if ("X_METADATA_VERSION" in optionsMap) { arguments.add("-Xmetadata-version=" + get(X_METADATA_VERSION)) }
    if ("X_LIST_PHASES" in optionsMap) { arguments.add("-Xlist-phases=" + get(X_LIST_PHASES)) }
    if ("X_DISABLE_PHASES" in optionsMap) { arguments.add("-Xdisable-phases=" + get(X_DISABLE_PHASES)) }
    if ("X_VERBOSE_PHASES" in optionsMap) { arguments.add("-Xverbose-phases=" + get(X_VERBOSE_PHASES)) }
    if ("X_PHASES_TO_DUMP_BEFORE" in optionsMap) { arguments.add("-Xphases-to-dump-before=" + get(X_PHASES_TO_DUMP_BEFORE)) }
    if ("X_PHASES_TO_DUMP_AFTER" in optionsMap) { arguments.add("-Xphases-to-dump-after=" + get(X_PHASES_TO_DUMP_AFTER)) }
    if ("X_PHASES_TO_DUMP" in optionsMap) { arguments.add("-Xphases-to-dump=" + get(X_PHASES_TO_DUMP)) }
    if ("X_DUMP_DIRECTORY" in optionsMap) { arguments.add("-Xdump-directory=" + get(X_DUMP_DIRECTORY)) }
    if ("X_DUMP_FQNAME" in optionsMap) { arguments.add("-Xdump-fqname=" + get(X_DUMP_FQNAME)) }
    if ("X_PHASES_TO_VALIDATE_BEFORE" in optionsMap) { arguments.add("-Xphases-to-validate-before=" + get(X_PHASES_TO_VALIDATE_BEFORE)) }
    if ("X_PHASES_TO_VALIDATE_AFTER" in optionsMap) { arguments.add("-Xphases-to-validate-after=" + get(X_PHASES_TO_VALIDATE_AFTER)) }
    if ("X_PHASES_TO_VALIDATE" in optionsMap) { arguments.add("-Xphases-to-validate=" + get(X_PHASES_TO_VALIDATE)) }
    if ("X_VERIFY_IR" in optionsMap) { arguments.add("-Xverify-ir=" + get(X_VERIFY_IR)) }
    if ("X_VERIFY_IR_VISIBILITY" in optionsMap) { arguments.add("-Xverify-ir-visibility=" + get(X_VERIFY_IR_VISIBILITY)) }
    if ("X_PROFILE_PHASES" in optionsMap) { arguments.add("-Xprofile-phases=" + get(X_PROFILE_PHASES)) }
    if ("X_CHECK_PHASE_CONDITIONS" in optionsMap) { arguments.add("-Xcheck-phase-conditions=" + get(X_CHECK_PHASE_CONDITIONS)) }
    if ("X_USE_FIR_EXPERIMENTAL_CHECKERS" in optionsMap) { arguments.add("-Xuse-fir-experimental-checkers=" + get(X_USE_FIR_EXPERIMENTAL_CHECKERS)) }
    if ("X_USE_FIR_IC" in optionsMap) { arguments.add("-Xuse-fir-ic=" + get(X_USE_FIR_IC)) }
    if ("X_USE_FIR_LT" in optionsMap) { arguments.add("-Xuse-fir-lt=" + get(X_USE_FIR_LT)) }
    if ("X_METADATA_KLIB" in optionsMap) { arguments.add("-Xmetadata-klib=" + get(X_METADATA_KLIB)) }
    if ("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" in optionsMap) { arguments.add("-Xdisable-default-scripting-plugin=" + get(X_DISABLE_DEFAULT_SCRIPTING_PLUGIN)) }
    if ("X_EXPLICIT_API" in optionsMap) { arguments.add("-Xexplicit-api=" + get(X_EXPLICIT_API).stringValue) }
    if ("X_RETURN_VALUE_CHECKER" in optionsMap) { arguments.add("-Xreturn-value-checker=" + get(X_RETURN_VALUE_CHECKER).stringValue) }
    if ("X_SUPPRESS_VERSION_WARNINGS" in optionsMap) { arguments.add("-Xsuppress-version-warnings=" + get(X_SUPPRESS_VERSION_WARNINGS)) }
    if ("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" in optionsMap) { arguments.add("-Xsuppress-api-version-greater-than-language-version-error=" + get(X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR)) }
    if ("X_EXPECT_ACTUAL_CLASSES" in optionsMap) { arguments.add("-Xexpect-actual-classes=" + get(X_EXPECT_ACTUAL_CLASSES)) }
    if ("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" in optionsMap) { arguments.add("-Xconsistent-data-class-copy-visibility=" + get(X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY)) }
    if ("X_UNRESTRICTED_BUILDER_INFERENCE" in optionsMap) { arguments.add("-Xunrestricted-builder-inference=" + get(X_UNRESTRICTED_BUILDER_INFERENCE)) }
    if ("X_CONTEXT_RECEIVERS" in optionsMap) { arguments.add("-Xcontext-receivers=" + get(X_CONTEXT_RECEIVERS)) }
    if ("X_CONTEXT_PARAMETERS" in optionsMap) { arguments.add("-Xcontext-parameters=" + get(X_CONTEXT_PARAMETERS)) }
    if ("X_CONTEXT_SENSITIVE_RESOLUTION" in optionsMap) { arguments.add("-Xcontext-sensitive-resolution=" + get(X_CONTEXT_SENSITIVE_RESOLUTION)) }
    if ("X_NON_LOCAL_BREAK_CONTINUE" in optionsMap) { arguments.add("-Xnon-local-break-continue=" + get(X_NON_LOCAL_BREAK_CONTINUE)) }
    if ("_XDATA_FLOW_BASED_EXHAUSTIVENESS" in optionsMap) { arguments.add("--Xdata-flow-based-exhaustiveness=" + get(_XDATA_FLOW_BASED_EXHAUSTIVENESS)) }
    if ("X_MULTI_DOLLAR_INTERPOLATION" in optionsMap) { arguments.add("-Xmulti-dollar-interpolation=" + get(X_MULTI_DOLLAR_INTERPOLATION)) }
    if ("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" in optionsMap) { arguments.add("-Xrender-internal-diagnostic-names=" + get(X_RENDER_INTERNAL_DIAGNOSTIC_NAMES)) }
    if ("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" in optionsMap) { arguments.add("-Xallow-any-scripts-in-source-roots=" + get(X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)) }
    if ("X_REPORT_ALL_WARNINGS" in optionsMap) { arguments.add("-Xreport-all-warnings=" + get(X_REPORT_ALL_WARNINGS)) }
    if ("X_IGNORE_CONST_OPTIMIZATION_ERRORS" in optionsMap) { arguments.add("-Xignore-const-optimization-errors=" + get(X_IGNORE_CONST_OPTIMIZATION_ERRORS)) }
    if ("X_DONT_WARN_ON_ERROR_SUPPRESSION" in optionsMap) { arguments.add("-Xdont-warn-on-error-suppression=" + get(X_DONT_WARN_ON_ERROR_SUPPRESSION)) }
    if ("X_WHEN_GUARDS" in optionsMap) { arguments.add("-Xwhen-guards=" + get(X_WHEN_GUARDS)) }
    if ("X_NESTED_TYPE_ALIASES" in optionsMap) { arguments.add("-Xnested-type-aliases=" + get(X_NESTED_TYPE_ALIASES)) }
    if ("X_SUPPRESS_WARNING" in optionsMap) { arguments.add("-Xsuppress-warning=" + get(X_SUPPRESS_WARNING)) }
    if ("X_WARNING_LEVEL" in optionsMap) { arguments.add("-Xwarning-level=" + get(X_WARNING_LEVEL)) }
    if ("X_ANNOTATION_DEFAULT_TARGET" in optionsMap) { arguments.add("-Xannotation-default-target=" + get(X_ANNOTATION_DEFAULT_TARGET)) }
    if ("X_ANNOTATION_TARGET_ALL" in optionsMap) { arguments.add("-Xannotation-target-all=" + get(X_ANNOTATION_TARGET_ALL)) }
    if ("X_ALLOW_REIFIED_TYPE_IN_CATCH" in optionsMap) { arguments.add("-Xallow-reified-type-in-catch=" + get(X_ALLOW_REIFIED_TYPE_IN_CATCH)) }
    if ("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" in optionsMap) { arguments.add("-Xallow-contracts-on-more-functions=" + get(X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS)) }
    if ("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" in optionsMap) { arguments.add("-Xallow-condition-implies-returns-contracts=" + get(X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS)) }
    if ("X_ALLOW_HOLDSIN_CONTRACT" in optionsMap) { arguments.add("-Xallow-holdsin-contract=" + get(X_ALLOW_HOLDSIN_CONTRACT)) }
    if ("X_NAME_BASED_DESTRUCTURING" in optionsMap) { arguments.add("-Xname-based-destructuring=" + get(X_NAME_BASED_DESTRUCTURING)) }
    return arguments
  }

  public class CommonCompilerArgument<V>(
    public val id: String,
  )

  public companion object {
    public val LANGUAGE_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("LANGUAGE_VERSION")

    public val API_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("API_VERSION")

    public val KOTLIN_HOME: CommonCompilerArgument<String?> = CommonCompilerArgument("KOTLIN_HOME")

    public val PROGRESSIVE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("PROGRESSIVE")

    public val OPT_IN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("OPT_IN")

    public val X_NO_INLINE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_NO_INLINE")

    public val X_SKIP_METADATA_VERSION_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_METADATA_VERSION_CHECK")

    public val X_SKIP_PRERELEASE_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_PRERELEASE_CHECK")

    public val X_REPORT_OUTPUT_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_OUTPUT_FILES")

    public val X_NEW_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NEW_INFERENCE")

    public val X_INLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_INLINE_CLASSES")

    public val X_REPORT_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_PERF")

    public val X_DUMP_PERF: CommonCompilerArgument<String?> = CommonCompilerArgument("X_DUMP_PERF")

    public val X_METADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_METADATA_VERSION")

    public val X_LIST_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_LIST_PHASES")

    public val X_DISABLE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_DISABLE_PHASES")

    public val X_VERBOSE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_VERBOSE_PHASES")

    public val X_PHASES_TO_DUMP_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_BEFORE")

    public val X_PHASES_TO_DUMP_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_AFTER")

    public val X_PHASES_TO_DUMP: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP")

    public val X_DUMP_DIRECTORY: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_DIRECTORY")

    public val X_DUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_FQNAME")

    public val X_PHASES_TO_VALIDATE_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_BEFORE")

    public val X_PHASES_TO_VALIDATE_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_AFTER")

    public val X_PHASES_TO_VALIDATE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE")

    public val X_VERIFY_IR: CommonCompilerArgument<String?> = CommonCompilerArgument("X_VERIFY_IR")

    public val X_VERIFY_IR_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_VERIFY_IR_VISIBILITY")

    public val X_PROFILE_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_PROFILE_PHASES")

    public val X_CHECK_PHASE_CONDITIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CHECK_PHASE_CONDITIONS")

    public val X_USE_FIR_EXPERIMENTAL_CHECKERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_EXPERIMENTAL_CHECKERS")

    public val X_USE_FIR_IC: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_IC")

    public val X_USE_FIR_LT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_LT")

    public val X_METADATA_KLIB: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_METADATA_KLIB")

    public val X_DISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")

    public val X_EXPLICIT_API: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("X_EXPLICIT_API")

    public val X_RETURN_VALUE_CHECKER: CommonCompilerArgument<ReturnValueCheckerMode> =
        CommonCompilerArgument("X_RETURN_VALUE_CHECKER")

    public val X_SUPPRESS_VERSION_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_VERSION_WARNINGS")

    public val X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR:
        CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR")

    public val X_EXPECT_ACTUAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPECT_ACTUAL_CLASSES")

    public val X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY")

    public val X_UNRESTRICTED_BUILDER_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_UNRESTRICTED_BUILDER_INFERENCE")

    public val X_CONTEXT_RECEIVERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_RECEIVERS")

    public val X_CONTEXT_PARAMETERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_PARAMETERS")

    public val X_CONTEXT_SENSITIVE_RESOLUTION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_SENSITIVE_RESOLUTION")

    public val X_NON_LOCAL_BREAK_CONTINUE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NON_LOCAL_BREAK_CONTINUE")

    public val _XDATA_FLOW_BASED_EXHAUSTIVENESS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("_XDATA_FLOW_BASED_EXHAUSTIVENESS")

    public val X_MULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_DOLLAR_INTERPOLATION")

    public val X_RENDER_INTERNAL_DIAGNOSTIC_NAMES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES")

    public val X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    public val X_REPORT_ALL_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_ALL_WARNINGS")

    public val X_IGNORE_CONST_OPTIMIZATION_ERRORS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_IGNORE_CONST_OPTIMIZATION_ERRORS")

    public val X_DONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_WARN_ON_ERROR_SUPPRESSION")

    public val X_WHEN_GUARDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_WHEN_GUARDS")

    public val X_NESTED_TYPE_ALIASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NESTED_TYPE_ALIASES")

    public val X_SUPPRESS_WARNING: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_SUPPRESS_WARNING")

    public val X_WARNING_LEVEL: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_WARNING_LEVEL")

    public val X_ANNOTATION_DEFAULT_TARGET: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_ANNOTATION_DEFAULT_TARGET")

    public val X_ANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ANNOTATION_TARGET_ALL")

    public val X_ALLOW_REIFIED_TYPE_IN_CATCH: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_REIFIED_TYPE_IN_CATCH")

    public val X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS")

    public val X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS")

    public val X_ALLOW_HOLDSIN_CONTRACT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_HOLDSIN_CONTRACT")

    public val X_NAME_BASED_DESTRUCTURING: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_NAME_BASED_DESTRUCTURING")
  }
}
