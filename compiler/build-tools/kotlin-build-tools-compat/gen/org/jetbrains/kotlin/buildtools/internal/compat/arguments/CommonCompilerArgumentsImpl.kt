// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.API_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.P
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.PROGRESSIVE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.SCRIPT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.XX_DEBUG_LEVEL_COMPILER_CHECKS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.XX_EXPLICIT_RETURN_TYPES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.XX_LENIENT_MODE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_HOLDSIN_CONTRACT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_REIFIED_TYPE_IN_CATCH
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ANNOTATION_TARGET_ALL
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_CHECK_PHASE_CONDITIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_COMMON_SOURCES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_COMPILER_PLUGIN
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_CONTEXT_PARAMETERS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_CONTEXT_RECEIVERS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_CONTEXT_SENSITIVE_RESOLUTION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_DIRECT_JAVA_ACTUALIZATION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_DISABLE_DEFAULT_SCRIPTING_PLUGIN
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_DISABLE_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_DONT_WARN_ON_ERROR_SUPPRESSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_DUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_DUMP_FQNAME
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_DUMP_PERF
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_ENABLE_INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_EXPECT_ACTUAL_CLASSES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_EXPLICIT_API
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENTS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENT_DEPENDENCY
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENT_REFINES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENT_SOURCES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_IGNORE_CONST_OPTIMIZATION_ERRORS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_INLINE_CLASSES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_INTELLIJ_PLUGIN_ROOT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_LIST_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_METADATA_KLIB
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_METADATA_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_MULTI_DOLLAR_INTERPOLATION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_MULTI_PLATFORM
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_NESTED_TYPE_ALIASES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_NEW_INFERENCE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_NON_LOCAL_BREAK_CONTINUE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_NO_CHECK_ACTUAL
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_NO_INLINE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_DUMP
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_DUMP_AFTER
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_DUMP_BEFORE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_VALIDATE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_VALIDATE_AFTER
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_VALIDATE_BEFORE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PLUGIN
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_PROFILE_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_RENDER_INTERNAL_DIAGNOSTIC_NAMES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_REPL
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_REPORT_ALL_WARNINGS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_REPORT_OUTPUT_FILES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_REPORT_PERF
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_RETURN_VALUE_CHECKER
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_SEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_SKIP_METADATA_VERSION_CHECK
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_SKIP_PRERELEASE_CHECK
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_STDLIB_COMPILATION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_SUPPRESS_VERSION_WARNINGS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_SUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_UNRESTRICTED_BUILDER_INFERENCE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_EXPERIMENTAL_CHECKERS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_IC
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_LT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_K2
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_VERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_VERIFY_IR
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_VERIFY_IR_VISIBILITY
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion.X_WHEN_GUARDS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.CommonCompilerArgumentsImpl.Companion._XDATA_FLOW_BASED_EXHAUSTIVENESS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings

internal abstract class CommonCompilerArgumentsImpl : CommonToolArgumentsImpl(),
    ArgumentsCommonCompilerArguments {
  private val internalArguments: MutableSet<String> = mutableSetOf()

  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V = optionsMap[key.id] as V

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
    try { if ("LANGUAGE_VERSION" in optionsMap) { arguments.languageVersion = get(LANGUAGE_VERSION)?.stringValue} } catch (_: NoSuchMethodError) {}
    try { if ("API_VERSION" in optionsMap) { arguments.apiVersion = get(API_VERSION)?.stringValue} } catch (_: NoSuchMethodError) {}
    try { if ("KOTLIN_HOME" in optionsMap) { arguments.kotlinHome = get(KOTLIN_HOME)} } catch (_: NoSuchMethodError) {}
    try { if ("PROGRESSIVE" in optionsMap) { arguments.progressiveMode = get(PROGRESSIVE)} } catch (_: NoSuchMethodError) {}
    try { if ("SCRIPT" in optionsMap) { arguments.script = get(SCRIPT)} } catch (_: NoSuchMethodError) {}
    try { if ("X_REPL" in optionsMap) { arguments.repl = get(X_REPL)} } catch (_: NoSuchMethodError) {}
    try { if ("OPT_IN" in optionsMap) { arguments.optIn = get(OPT_IN)} } catch (_: NoSuchMethodError) {}
    try { if ("X_NO_INLINE" in optionsMap) { arguments.noInline = get(X_NO_INLINE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_SKIP_METADATA_VERSION_CHECK" in optionsMap) { arguments.skipMetadataVersionCheck = get(X_SKIP_METADATA_VERSION_CHECK)} } catch (_: NoSuchMethodError) {}
    try { if ("X_SKIP_PRERELEASE_CHECK" in optionsMap) { arguments.skipPrereleaseCheck = get(X_SKIP_PRERELEASE_CHECK)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ALLOW_KOTLIN_PACKAGE" in optionsMap) { arguments.allowKotlinPackage = get(X_ALLOW_KOTLIN_PACKAGE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_STDLIB_COMPILATION" in optionsMap) { arguments.stdlibCompilation = get(X_STDLIB_COMPILATION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_REPORT_OUTPUT_FILES" in optionsMap) { arguments.reportOutputFiles = get(X_REPORT_OUTPUT_FILES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PLUGIN" in optionsMap) { arguments.pluginClasspaths = get(X_PLUGIN)} } catch (_: NoSuchMethodError) {}
    try { if ("P" in optionsMap) { arguments.pluginOptions = get(P)} } catch (_: NoSuchMethodError) {}
    try { if ("X_COMPILER_PLUGIN" in optionsMap) { arguments.pluginConfigurations = get(X_COMPILER_PLUGIN)} } catch (_: NoSuchMethodError) {}
    try { if ("X_MULTI_PLATFORM" in optionsMap) { arguments.multiPlatform = get(X_MULTI_PLATFORM)} } catch (_: NoSuchMethodError) {}
    try { if ("X_NO_CHECK_ACTUAL" in optionsMap) { arguments.noCheckActual = get(X_NO_CHECK_ACTUAL)} } catch (_: NoSuchMethodError) {}
    try { if ("X_INTELLIJ_PLUGIN_ROOT" in optionsMap) { arguments.intellijPluginRoot = get(X_INTELLIJ_PLUGIN_ROOT)} } catch (_: NoSuchMethodError) {}
    try { if ("X_NEW_INFERENCE" in optionsMap) { arguments.newInference = get(X_NEW_INFERENCE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_INLINE_CLASSES" in optionsMap) { arguments.inlineClasses = get(X_INLINE_CLASSES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_REPORT_PERF" in optionsMap) { arguments.reportPerf = get(X_REPORT_PERF)} } catch (_: NoSuchMethodError) {}
    try { if ("X_DUMP_PERF" in optionsMap) { arguments.dumpPerf = get(X_DUMP_PERF)} } catch (_: NoSuchMethodError) {}
    try { if ("X_METADATA_VERSION" in optionsMap) { arguments.metadataVersion = get(X_METADATA_VERSION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_COMMON_SOURCES" in optionsMap) { arguments.commonSources = get(X_COMMON_SOURCES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_LIST_PHASES" in optionsMap) { arguments.listPhases = get(X_LIST_PHASES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_DISABLE_PHASES" in optionsMap) { arguments.disablePhases = get(X_DISABLE_PHASES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_VERBOSE_PHASES" in optionsMap) { arguments.verbosePhases = get(X_VERBOSE_PHASES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PHASES_TO_DUMP_BEFORE" in optionsMap) { arguments.phasesToDumpBefore = get(X_PHASES_TO_DUMP_BEFORE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PHASES_TO_DUMP_AFTER" in optionsMap) { arguments.phasesToDumpAfter = get(X_PHASES_TO_DUMP_AFTER)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PHASES_TO_DUMP" in optionsMap) { arguments.phasesToDump = get(X_PHASES_TO_DUMP)} } catch (_: NoSuchMethodError) {}
    try { if ("X_DUMP_DIRECTORY" in optionsMap) { arguments.dumpDirectory = get(X_DUMP_DIRECTORY)} } catch (_: NoSuchMethodError) {}
    try { if ("X_DUMP_FQNAME" in optionsMap) { arguments.dumpOnlyFqName = get(X_DUMP_FQNAME)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PHASES_TO_VALIDATE_BEFORE" in optionsMap) { arguments.phasesToValidateBefore = get(X_PHASES_TO_VALIDATE_BEFORE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PHASES_TO_VALIDATE_AFTER" in optionsMap) { arguments.phasesToValidateAfter = get(X_PHASES_TO_VALIDATE_AFTER)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PHASES_TO_VALIDATE" in optionsMap) { arguments.phasesToValidate = get(X_PHASES_TO_VALIDATE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_VERIFY_IR" in optionsMap) { arguments.verifyIr = get(X_VERIFY_IR)} } catch (_: NoSuchMethodError) {}
    try { if ("X_VERIFY_IR_VISIBILITY" in optionsMap) { arguments.verifyIrVisibility = get(X_VERIFY_IR_VISIBILITY)} } catch (_: NoSuchMethodError) {}
    try { if ("X_PROFILE_PHASES" in optionsMap) { arguments.profilePhases = get(X_PROFILE_PHASES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_CHECK_PHASE_CONDITIONS" in optionsMap) { arguments.checkPhaseConditions = get(X_CHECK_PHASE_CONDITIONS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_USE_K2" in optionsMap) { arguments.setUsingReflection("useK2", get(X_USE_K2))} } catch (_: NoSuchMethodError) {}
    try { if ("X_USE_FIR_EXPERIMENTAL_CHECKERS" in optionsMap) { arguments.useFirExperimentalCheckers = get(X_USE_FIR_EXPERIMENTAL_CHECKERS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_USE_FIR_IC" in optionsMap) { arguments.useFirIC = get(X_USE_FIR_IC)} } catch (_: NoSuchMethodError) {}
    try { if ("X_USE_FIR_LT" in optionsMap) { arguments.useFirLT = get(X_USE_FIR_LT)} } catch (_: NoSuchMethodError) {}
    try { if ("X_METADATA_KLIB" in optionsMap) { arguments.metadataKlib = get(X_METADATA_KLIB)} } catch (_: NoSuchMethodError) {}
    try { if ("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" in optionsMap) { arguments.disableDefaultScriptingPlugin = get(X_DISABLE_DEFAULT_SCRIPTING_PLUGIN)} } catch (_: NoSuchMethodError) {}
    try { if ("X_EXPLICIT_API" in optionsMap) { arguments.explicitApi = get(X_EXPLICIT_API).stringValue} } catch (_: NoSuchMethodError) {}
    try { if ("XX_EXPLICIT_RETURN_TYPES" in optionsMap) { arguments.explicitReturnTypes = get(XX_EXPLICIT_RETURN_TYPES).stringValue} } catch (_: NoSuchMethodError) {}
    try { if ("X_RETURN_VALUE_CHECKER" in optionsMap) { arguments.returnValueChecker = get(X_RETURN_VALUE_CHECKER).stringValue} } catch (_: NoSuchMethodError) {}
    try { if ("X_SUPPRESS_VERSION_WARNINGS" in optionsMap) { arguments.suppressVersionWarnings = get(X_SUPPRESS_VERSION_WARNINGS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" in optionsMap) { arguments.suppressApiVersionGreaterThanLanguageVersionError = get(X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR)} } catch (_: NoSuchMethodError) {}
    try { if ("X_EXPECT_ACTUAL_CLASSES" in optionsMap) { arguments.expectActualClasses = get(X_EXPECT_ACTUAL_CLASSES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" in optionsMap) { arguments.consistentDataClassCopyVisibility = get(X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY)} } catch (_: NoSuchMethodError) {}
    try { if ("X_UNRESTRICTED_BUILDER_INFERENCE" in optionsMap) { arguments.unrestrictedBuilderInference = get(X_UNRESTRICTED_BUILDER_INFERENCE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_CONTEXT_RECEIVERS" in optionsMap) { arguments.contextReceivers = get(X_CONTEXT_RECEIVERS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_CONTEXT_PARAMETERS" in optionsMap) { arguments.contextParameters = get(X_CONTEXT_PARAMETERS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_CONTEXT_SENSITIVE_RESOLUTION" in optionsMap) { arguments.contextSensitiveResolution = get(X_CONTEXT_SENSITIVE_RESOLUTION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_NON_LOCAL_BREAK_CONTINUE" in optionsMap) { arguments.nonLocalBreakContinue = get(X_NON_LOCAL_BREAK_CONTINUE)} } catch (_: NoSuchMethodError) {}
    try { if ("_XDATA_FLOW_BASED_EXHAUSTIVENESS" in optionsMap) { arguments.xdataFlowBasedExhaustiveness = get(_XDATA_FLOW_BASED_EXHAUSTIVENESS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_DIRECT_JAVA_ACTUALIZATION" in optionsMap) { arguments.directJavaActualization = get(X_DIRECT_JAVA_ACTUALIZATION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_MULTI_DOLLAR_INTERPOLATION" in optionsMap) { arguments.multiDollarInterpolation = get(X_MULTI_DOLLAR_INTERPOLATION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ENABLE_INCREMENTAL_COMPILATION" in optionsMap) { arguments.incrementalCompilation = get(X_ENABLE_INCREMENTAL_COMPILATION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" in optionsMap) { arguments.renderInternalDiagnosticNames = get(X_RENDER_INTERNAL_DIAGNOSTIC_NAMES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" in optionsMap) { arguments.allowAnyScriptsInSourceRoots = get(X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_REPORT_ALL_WARNINGS" in optionsMap) { arguments.reportAllWarnings = get(X_REPORT_ALL_WARNINGS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_FRAGMENTS" in optionsMap) { arguments.fragments = get(X_FRAGMENTS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_FRAGMENT_SOURCES" in optionsMap) { arguments.fragmentSources = get(X_FRAGMENT_SOURCES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_FRAGMENT_REFINES" in optionsMap) { arguments.fragmentRefines = get(X_FRAGMENT_REFINES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_FRAGMENT_DEPENDENCY" in optionsMap) { arguments.fragmentDependencies = get(X_FRAGMENT_DEPENDENCY)} } catch (_: NoSuchMethodError) {}
    try { if ("X_SEPARATE_KMP_COMPILATION" in optionsMap) { arguments.separateKmpCompilationScheme = get(X_SEPARATE_KMP_COMPILATION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_IGNORE_CONST_OPTIMIZATION_ERRORS" in optionsMap) { arguments.ignoreConstOptimizationErrors = get(X_IGNORE_CONST_OPTIMIZATION_ERRORS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_DONT_WARN_ON_ERROR_SUPPRESSION" in optionsMap) { arguments.dontWarnOnErrorSuppression = get(X_DONT_WARN_ON_ERROR_SUPPRESSION)} } catch (_: NoSuchMethodError) {}
    try { if ("X_WHEN_GUARDS" in optionsMap) { arguments.whenGuards = get(X_WHEN_GUARDS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_NESTED_TYPE_ALIASES" in optionsMap) { arguments.nestedTypeAliases = get(X_NESTED_TYPE_ALIASES)} } catch (_: NoSuchMethodError) {}
    try { if ("X_SUPPRESS_WARNING" in optionsMap) { arguments.suppressedDiagnostics = get(X_SUPPRESS_WARNING)} } catch (_: NoSuchMethodError) {}
    try { if ("X_WARNING_LEVEL" in optionsMap) { arguments.warningLevels = get(X_WARNING_LEVEL)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ANNOTATION_DEFAULT_TARGET" in optionsMap) { arguments.annotationDefaultTarget = get(X_ANNOTATION_DEFAULT_TARGET)} } catch (_: NoSuchMethodError) {}
    try { if ("XX_DEBUG_LEVEL_COMPILER_CHECKS" in optionsMap) { arguments.debugLevelCompilerChecks = get(XX_DEBUG_LEVEL_COMPILER_CHECKS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ANNOTATION_TARGET_ALL" in optionsMap) { arguments.annotationTargetAll = get(X_ANNOTATION_TARGET_ALL)} } catch (_: NoSuchMethodError) {}
    try { if ("XX_LENIENT_MODE" in optionsMap) { arguments.lenientMode = get(XX_LENIENT_MODE)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ALLOW_REIFIED_TYPE_IN_CATCH" in optionsMap) { arguments.allowReifiedTypeInCatch = get(X_ALLOW_REIFIED_TYPE_IN_CATCH)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" in optionsMap) { arguments.allowContractsOnMoreFunctions = get(X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" in optionsMap) { arguments.allowConditionImpliesReturnsContracts = get(X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_ALLOW_HOLDSIN_CONTRACT" in optionsMap) { arguments.allowHoldsinContract = get(X_ALLOW_HOLDSIN_CONTRACT)} } catch (_: NoSuchMethodError) {}
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: CommonCompilerArguments = parseCommandLineArguments(arguments)
    applyCompilerArguments(compilerArgs)
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: CommonCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[LANGUAGE_VERSION] = arguments.languageVersion?.let { KotlinVersion.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {}
    try { this[API_VERSION] = arguments.apiVersion?.let { KotlinVersion.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {}
    try { this[KOTLIN_HOME] = arguments.kotlinHome } catch (_: NoSuchMethodError) {}
    try { this[PROGRESSIVE] = arguments.progressiveMode } catch (_: NoSuchMethodError) {}
    try { this[SCRIPT] = arguments.script } catch (_: NoSuchMethodError) {}
    try { this[X_REPL] = arguments.repl } catch (_: NoSuchMethodError) {}
    try { this[OPT_IN] = arguments.optIn } catch (_: NoSuchMethodError) {}
    try { this[X_NO_INLINE] = arguments.noInline } catch (_: NoSuchMethodError) {}
    try { this[X_SKIP_METADATA_VERSION_CHECK] = arguments.skipMetadataVersionCheck } catch (_: NoSuchMethodError) {}
    try { this[X_SKIP_PRERELEASE_CHECK] = arguments.skipPrereleaseCheck } catch (_: NoSuchMethodError) {}
    try { this[X_ALLOW_KOTLIN_PACKAGE] = arguments.allowKotlinPackage } catch (_: NoSuchMethodError) {}
    try { this[X_STDLIB_COMPILATION] = arguments.stdlibCompilation } catch (_: NoSuchMethodError) {}
    try { this[X_REPORT_OUTPUT_FILES] = arguments.reportOutputFiles } catch (_: NoSuchMethodError) {}
    try { this[X_PLUGIN] = arguments.pluginClasspaths } catch (_: NoSuchMethodError) {}
    try { this[P] = arguments.pluginOptions } catch (_: NoSuchMethodError) {}
    try { this[X_COMPILER_PLUGIN] = arguments.pluginConfigurations } catch (_: NoSuchMethodError) {}
    try { this[X_MULTI_PLATFORM] = arguments.multiPlatform } catch (_: NoSuchMethodError) {}
    try { this[X_NO_CHECK_ACTUAL] = arguments.noCheckActual } catch (_: NoSuchMethodError) {}
    try { this[X_INTELLIJ_PLUGIN_ROOT] = arguments.intellijPluginRoot } catch (_: NoSuchMethodError) {}
    try { this[X_NEW_INFERENCE] = arguments.newInference } catch (_: NoSuchMethodError) {}
    try { this[X_INLINE_CLASSES] = arguments.inlineClasses } catch (_: NoSuchMethodError) {}
    try { this[X_REPORT_PERF] = arguments.reportPerf } catch (_: NoSuchMethodError) {}
    try { this[X_DUMP_PERF] = arguments.dumpPerf } catch (_: NoSuchMethodError) {}
    try { this[X_METADATA_VERSION] = arguments.metadataVersion } catch (_: NoSuchMethodError) {}
    try { this[X_COMMON_SOURCES] = arguments.commonSources } catch (_: NoSuchMethodError) {}
    try { this[X_LIST_PHASES] = arguments.listPhases } catch (_: NoSuchMethodError) {}
    try { this[X_DISABLE_PHASES] = arguments.disablePhases } catch (_: NoSuchMethodError) {}
    try { this[X_VERBOSE_PHASES] = arguments.verbosePhases } catch (_: NoSuchMethodError) {}
    try { this[X_PHASES_TO_DUMP_BEFORE] = arguments.phasesToDumpBefore } catch (_: NoSuchMethodError) {}
    try { this[X_PHASES_TO_DUMP_AFTER] = arguments.phasesToDumpAfter } catch (_: NoSuchMethodError) {}
    try { this[X_PHASES_TO_DUMP] = arguments.phasesToDump } catch (_: NoSuchMethodError) {}
    try { this[X_DUMP_DIRECTORY] = arguments.dumpDirectory } catch (_: NoSuchMethodError) {}
    try { this[X_DUMP_FQNAME] = arguments.dumpOnlyFqName } catch (_: NoSuchMethodError) {}
    try { this[X_PHASES_TO_VALIDATE_BEFORE] = arguments.phasesToValidateBefore } catch (_: NoSuchMethodError) {}
    try { this[X_PHASES_TO_VALIDATE_AFTER] = arguments.phasesToValidateAfter } catch (_: NoSuchMethodError) {}
    try { this[X_PHASES_TO_VALIDATE] = arguments.phasesToValidate } catch (_: NoSuchMethodError) {}
    try { this[X_VERIFY_IR] = arguments.verifyIr } catch (_: NoSuchMethodError) {}
    try { this[X_VERIFY_IR_VISIBILITY] = arguments.verifyIrVisibility } catch (_: NoSuchMethodError) {}
    try { this[X_PROFILE_PHASES] = arguments.profilePhases } catch (_: NoSuchMethodError) {}
    try { this[X_CHECK_PHASE_CONDITIONS] = arguments.checkPhaseConditions } catch (_: NoSuchMethodError) {}
    try { this[X_USE_K2] = arguments.getUsingReflection("useK2") } catch (_: NoSuchMethodError) {}
    try { this[X_USE_FIR_EXPERIMENTAL_CHECKERS] = arguments.useFirExperimentalCheckers } catch (_: NoSuchMethodError) {}
    try { this[X_USE_FIR_IC] = arguments.useFirIC } catch (_: NoSuchMethodError) {}
    try { this[X_USE_FIR_LT] = arguments.useFirLT } catch (_: NoSuchMethodError) {}
    try { this[X_METADATA_KLIB] = arguments.metadataKlib } catch (_: NoSuchMethodError) {}
    try { this[X_DISABLE_DEFAULT_SCRIPTING_PLUGIN] = arguments.disableDefaultScriptingPlugin } catch (_: NoSuchMethodError) {}
    try { this[X_EXPLICIT_API] = arguments.explicitApi.let { ExplicitApiMode.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {}
    try { this[XX_EXPLICIT_RETURN_TYPES] = arguments.explicitReturnTypes.let { ExplicitApiMode.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {}
    try { this[X_RETURN_VALUE_CHECKER] = arguments.returnValueChecker.let { ReturnValueCheckerMode.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {}
    try { this[X_SUPPRESS_VERSION_WARNINGS] = arguments.suppressVersionWarnings } catch (_: NoSuchMethodError) {}
    try { this[X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR] = arguments.suppressApiVersionGreaterThanLanguageVersionError } catch (_: NoSuchMethodError) {}
    try { this[X_EXPECT_ACTUAL_CLASSES] = arguments.expectActualClasses } catch (_: NoSuchMethodError) {}
    try { this[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = arguments.consistentDataClassCopyVisibility } catch (_: NoSuchMethodError) {}
    try { this[X_UNRESTRICTED_BUILDER_INFERENCE] = arguments.unrestrictedBuilderInference } catch (_: NoSuchMethodError) {}
    try { this[X_CONTEXT_RECEIVERS] = arguments.contextReceivers } catch (_: NoSuchMethodError) {}
    try { this[X_CONTEXT_PARAMETERS] = arguments.contextParameters } catch (_: NoSuchMethodError) {}
    try { this[X_CONTEXT_SENSITIVE_RESOLUTION] = arguments.contextSensitiveResolution } catch (_: NoSuchMethodError) {}
    try { this[X_NON_LOCAL_BREAK_CONTINUE] = arguments.nonLocalBreakContinue } catch (_: NoSuchMethodError) {}
    try { this[_XDATA_FLOW_BASED_EXHAUSTIVENESS] = arguments.xdataFlowBasedExhaustiveness } catch (_: NoSuchMethodError) {}
    try { this[X_DIRECT_JAVA_ACTUALIZATION] = arguments.directJavaActualization } catch (_: NoSuchMethodError) {}
    try { this[X_MULTI_DOLLAR_INTERPOLATION] = arguments.multiDollarInterpolation } catch (_: NoSuchMethodError) {}
    try { this[X_ENABLE_INCREMENTAL_COMPILATION] = arguments.incrementalCompilation } catch (_: NoSuchMethodError) {}
    try { this[X_RENDER_INTERNAL_DIAGNOSTIC_NAMES] = arguments.renderInternalDiagnosticNames } catch (_: NoSuchMethodError) {}
    try { this[X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS] = arguments.allowAnyScriptsInSourceRoots } catch (_: NoSuchMethodError) {}
    try { this[X_REPORT_ALL_WARNINGS] = arguments.reportAllWarnings } catch (_: NoSuchMethodError) {}
    try { this[X_FRAGMENTS] = arguments.fragments } catch (_: NoSuchMethodError) {}
    try { this[X_FRAGMENT_SOURCES] = arguments.fragmentSources } catch (_: NoSuchMethodError) {}
    try { this[X_FRAGMENT_REFINES] = arguments.fragmentRefines } catch (_: NoSuchMethodError) {}
    try { this[X_FRAGMENT_DEPENDENCY] = arguments.fragmentDependencies } catch (_: NoSuchMethodError) {}
    try { this[X_SEPARATE_KMP_COMPILATION] = arguments.separateKmpCompilationScheme } catch (_: NoSuchMethodError) {}
    try { this[X_IGNORE_CONST_OPTIMIZATION_ERRORS] = arguments.ignoreConstOptimizationErrors } catch (_: NoSuchMethodError) {}
    try { this[X_DONT_WARN_ON_ERROR_SUPPRESSION] = arguments.dontWarnOnErrorSuppression } catch (_: NoSuchMethodError) {}
    try { this[X_WHEN_GUARDS] = arguments.whenGuards } catch (_: NoSuchMethodError) {}
    try { this[X_NESTED_TYPE_ALIASES] = arguments.nestedTypeAliases } catch (_: NoSuchMethodError) {}
    try { this[X_SUPPRESS_WARNING] = arguments.suppressedDiagnostics } catch (_: NoSuchMethodError) {}
    try { this[X_WARNING_LEVEL] = arguments.warningLevels } catch (_: NoSuchMethodError) {}
    try { this[X_ANNOTATION_DEFAULT_TARGET] = arguments.annotationDefaultTarget } catch (_: NoSuchMethodError) {}
    try { this[XX_DEBUG_LEVEL_COMPILER_CHECKS] = arguments.debugLevelCompilerChecks } catch (_: NoSuchMethodError) {}
    try { this[X_ANNOTATION_TARGET_ALL] = arguments.annotationTargetAll } catch (_: NoSuchMethodError) {}
    try { this[XX_LENIENT_MODE] = arguments.lenientMode } catch (_: NoSuchMethodError) {}
    try { this[X_ALLOW_REIFIED_TYPE_IN_CATCH] = arguments.allowReifiedTypeInCatch } catch (_: NoSuchMethodError) {}
    try { this[X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS] = arguments.allowContractsOnMoreFunctions } catch (_: NoSuchMethodError) {}
    try { this[X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS] = arguments.allowConditionImpliesReturnsContracts } catch (_: NoSuchMethodError) {}
    try { this[X_ALLOW_HOLDSIN_CONTRACT] = arguments.allowHoldsinContract } catch (_: NoSuchMethodError) {}
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
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

    public val SCRIPT: CommonCompilerArgument<Boolean> = CommonCompilerArgument("SCRIPT")

    public val X_REPL: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_REPL")

    public val OPT_IN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("OPT_IN")

    public val X_NO_INLINE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_NO_INLINE")

    public val X_SKIP_METADATA_VERSION_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_METADATA_VERSION_CHECK")

    public val X_SKIP_PRERELEASE_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_PRERELEASE_CHECK")

    public val X_ALLOW_KOTLIN_PACKAGE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_KOTLIN_PACKAGE")

    public val X_STDLIB_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_STDLIB_COMPILATION")

    public val X_REPORT_OUTPUT_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_OUTPUT_FILES")

    public val X_PLUGIN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("X_PLUGIN")

    public val P: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("P")

    public val X_COMPILER_PLUGIN: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMPILER_PLUGIN")

    public val X_MULTI_PLATFORM: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_PLATFORM")

    public val X_NO_CHECK_ACTUAL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NO_CHECK_ACTUAL")

    public val X_INTELLIJ_PLUGIN_ROOT: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_INTELLIJ_PLUGIN_ROOT")

    public val X_NEW_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NEW_INFERENCE")

    public val X_INLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_INLINE_CLASSES")

    public val X_REPORT_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_PERF")

    public val X_DUMP_PERF: CommonCompilerArgument<String?> = CommonCompilerArgument("X_DUMP_PERF")

    public val X_METADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_METADATA_VERSION")

    public val X_COMMON_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMMON_SOURCES")

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

    public val X_USE_K2: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_USE_K2")

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

    public val XX_EXPLICIT_RETURN_TYPES: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("XX_EXPLICIT_RETURN_TYPES")

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

    public val X_DIRECT_JAVA_ACTUALIZATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DIRECT_JAVA_ACTUALIZATION")

    public val X_MULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_DOLLAR_INTERPOLATION")

    public val X_ENABLE_INCREMENTAL_COMPILATION: CommonCompilerArgument<Boolean?> =
        CommonCompilerArgument("X_ENABLE_INCREMENTAL_COMPILATION")

    public val X_RENDER_INTERNAL_DIAGNOSTIC_NAMES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES")

    public val X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    public val X_REPORT_ALL_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_ALL_WARNINGS")

    public val X_FRAGMENTS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENTS")

    public val X_FRAGMENT_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_SOURCES")

    public val X_FRAGMENT_REFINES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_REFINES")

    public val X_FRAGMENT_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_DEPENDENCY")

    public val X_SEPARATE_KMP_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SEPARATE_KMP_COMPILATION")

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

    public val XX_DEBUG_LEVEL_COMPILER_CHECKS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XX_DEBUG_LEVEL_COMPILER_CHECKS")

    public val X_ANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ANNOTATION_TARGET_ALL")

    public val XX_LENIENT_MODE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XX_LENIENT_MODE")

    public val X_ALLOW_REIFIED_TYPE_IN_CATCH: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_REIFIED_TYPE_IN_CATCH")

    public val X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS")

    public val X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS")

    public val X_ALLOW_HOLDSIN_CONTRACT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_HOLDSIN_CONTRACT")
  }
}
