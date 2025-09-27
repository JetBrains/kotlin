// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.API_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.HEADER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.P
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.PROGRESSIVE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.SCRIPT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.XX_DEBUG_LEVEL_COMPILER_CHECKS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.XX_DUMP_MODEL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.XX_EXPLICIT_RETURN_TYPES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.XX_LANGUAGE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.XX_LENIENT_MODE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_HOLDSIN_CONTRACT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ALLOW_REIFIED_TYPE_IN_CATCH
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ANNOTATION_TARGET_ALL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_CHECK_PHASE_CONDITIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_COMMON_SOURCES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_COMPILER_PLUGIN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_COMPILER_PLUGIN_ORDER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_CONTEXT_PARAMETERS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_CONTEXT_RECEIVERS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_CONTEXT_SENSITIVE_RESOLUTION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DATA_FLOW_BASED_EXHAUSTIVENESS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DETAILED_PERF
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DIRECT_JAVA_ACTUALIZATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DISABLE_DEFAULT_SCRIPTING_PLUGIN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DISABLE_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DONT_WARN_ON_ERROR_SUPPRESSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DUMP_FQNAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_DUMP_PERF
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_ENABLE_INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_EXPECT_ACTUAL_CLASSES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_EXPLICIT_API
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_EXPLICIT_BACKING_FIELDS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENTS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENT_DEPENDENCY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENT_FRIEND_DEPENDENCY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENT_REFINES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_FRAGMENT_SOURCES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_IGNORE_CONST_OPTIMIZATION_ERRORS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_INLINE_CLASSES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_INTELLIJ_PLUGIN_ROOT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_LIST_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_METADATA_KLIB
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_METADATA_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_MULTI_DOLLAR_INTERPOLATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_MULTI_PLATFORM
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_NAME_BASED_DESTRUCTURING
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_NESTED_TYPE_ALIASES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_NEW_INFERENCE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_NON_LOCAL_BREAK_CONTINUE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_NO_CHECK_ACTUAL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_NO_INLINE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_DUMP
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_DUMP_AFTER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_DUMP_BEFORE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_VALIDATE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_VALIDATE_AFTER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PHASES_TO_VALIDATE_BEFORE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PLUGIN
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_PROFILE_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_RENDER_INTERNAL_DIAGNOSTIC_NAMES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_REPL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_REPORT_ALL_WARNINGS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_REPORT_OUTPUT_FILES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_REPORT_PERF
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_RETURN_VALUE_CHECKER
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_SEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_SKIP_METADATA_VERSION_CHECK
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_SKIP_PRERELEASE_CHECK
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_STDLIB_COMPILATION
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_SUPPRESS_VERSION_WARNINGS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_SUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_UNRESTRICTED_BUILDER_INFERENCE
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_EXPERIMENTAL_CHECKERS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_IC
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_FIR_LT
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_USE_K2
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_VERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_VERIFY_IR
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_VERIFY_IR_VISIBILITY
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_WHEN_GUARDS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode
import kotlin.KotlinVersion as KotlinKotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion as EnumsKotlinVersion
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonCompilerArgumentsImpl : CommonToolArgumentsImpl(),
    ArgumentsCommonCompilerArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinKotlinVersion(2, 3, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
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
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (LANGUAGE_VERSION in this) { arguments.languageVersion = get(LANGUAGE_VERSION)?.stringValue}
    if (API_VERSION in this) { arguments.apiVersion = get(API_VERSION)?.stringValue}
    if (KOTLIN_HOME in this) { arguments.kotlinHome = get(KOTLIN_HOME)}
    if (HEADER in this) { arguments.headerMode = get(HEADER)}
    if (PROGRESSIVE in this) { arguments.progressiveMode = get(PROGRESSIVE)}
    if (SCRIPT in this) { arguments.script = get(SCRIPT)}
    if (X_REPL in this) { arguments.repl = get(X_REPL)}
    if (OPT_IN in this) { arguments.optIn = get(OPT_IN)}
    if (X_NO_INLINE in this) { arguments.noInline = get(X_NO_INLINE)}
    if (X_SKIP_METADATA_VERSION_CHECK in this) { arguments.skipMetadataVersionCheck = get(X_SKIP_METADATA_VERSION_CHECK)}
    if (X_SKIP_PRERELEASE_CHECK in this) { arguments.skipPrereleaseCheck = get(X_SKIP_PRERELEASE_CHECK)}
    if (X_ALLOW_KOTLIN_PACKAGE in this) { arguments.allowKotlinPackage = get(X_ALLOW_KOTLIN_PACKAGE)}
    if (X_STDLIB_COMPILATION in this) { arguments.stdlibCompilation = get(X_STDLIB_COMPILATION)}
    if (X_REPORT_OUTPUT_FILES in this) { arguments.reportOutputFiles = get(X_REPORT_OUTPUT_FILES)}
    if (X_PLUGIN in this) { arguments.pluginClasspaths = get(X_PLUGIN)}
    if (P in this) { arguments.pluginOptions = get(P)}
    if (X_COMPILER_PLUGIN in this) { arguments.pluginConfigurations = get(X_COMPILER_PLUGIN)}
    if (X_COMPILER_PLUGIN_ORDER in this) { arguments.pluginOrderConstraints = get(X_COMPILER_PLUGIN_ORDER)}
    if (X_MULTI_PLATFORM in this) { arguments.multiPlatform = get(X_MULTI_PLATFORM)}
    if (X_NO_CHECK_ACTUAL in this) { arguments.noCheckActual = get(X_NO_CHECK_ACTUAL)}
    if (X_INTELLIJ_PLUGIN_ROOT in this) { arguments.intellijPluginRoot = get(X_INTELLIJ_PLUGIN_ROOT)}
    if (X_NEW_INFERENCE in this) { arguments.newInference = get(X_NEW_INFERENCE)}
    if (X_INLINE_CLASSES in this) { arguments.inlineClasses = get(X_INLINE_CLASSES)}
    if (X_REPORT_PERF in this) { arguments.reportPerf = get(X_REPORT_PERF)}
    if (X_DETAILED_PERF in this) { arguments.detailedPerf = get(X_DETAILED_PERF)}
    if (X_DUMP_PERF in this) { arguments.dumpPerf = get(X_DUMP_PERF)}
    if (XX_DUMP_MODEL in this) { arguments.dumpArgumentsDir = get(XX_DUMP_MODEL)}
    if (X_METADATA_VERSION in this) { arguments.metadataVersion = get(X_METADATA_VERSION)}
    if (X_COMMON_SOURCES in this) { arguments.commonSources = get(X_COMMON_SOURCES)}
    if (X_LIST_PHASES in this) { arguments.listPhases = get(X_LIST_PHASES)}
    if (X_DISABLE_PHASES in this) { arguments.disablePhases = get(X_DISABLE_PHASES)}
    if (X_VERBOSE_PHASES in this) { arguments.verbosePhases = get(X_VERBOSE_PHASES)}
    if (X_PHASES_TO_DUMP_BEFORE in this) { arguments.phasesToDumpBefore = get(X_PHASES_TO_DUMP_BEFORE)}
    if (X_PHASES_TO_DUMP_AFTER in this) { arguments.phasesToDumpAfter = get(X_PHASES_TO_DUMP_AFTER)}
    if (X_PHASES_TO_DUMP in this) { arguments.phasesToDump = get(X_PHASES_TO_DUMP)}
    if (X_DUMP_DIRECTORY in this) { arguments.dumpDirectory = get(X_DUMP_DIRECTORY)}
    if (X_DUMP_FQNAME in this) { arguments.dumpOnlyFqName = get(X_DUMP_FQNAME)}
    if (X_PHASES_TO_VALIDATE_BEFORE in this) { arguments.phasesToValidateBefore = get(X_PHASES_TO_VALIDATE_BEFORE)}
    if (X_PHASES_TO_VALIDATE_AFTER in this) { arguments.phasesToValidateAfter = get(X_PHASES_TO_VALIDATE_AFTER)}
    if (X_PHASES_TO_VALIDATE in this) { arguments.phasesToValidate = get(X_PHASES_TO_VALIDATE)}
    if (X_VERIFY_IR in this) { arguments.verifyIr = get(X_VERIFY_IR)}
    if (X_VERIFY_IR_VISIBILITY in this) { arguments.verifyIrVisibility = get(X_VERIFY_IR_VISIBILITY)}
    if (X_PROFILE_PHASES in this) { arguments.profilePhases = get(X_PROFILE_PHASES)}
    if (X_CHECK_PHASE_CONDITIONS in this) { arguments.checkPhaseConditions = get(X_CHECK_PHASE_CONDITIONS)}
    try { if (X_USE_K2 in this) { arguments.setUsingReflection("useK2", get(X_USE_K2))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2. Current compiler version is: $KC_VERSION}, but the argument was removed in 2.2.0""").initCause(e) }
    if (X_USE_FIR_EXPERIMENTAL_CHECKERS in this) { arguments.useFirExperimentalCheckers = get(X_USE_FIR_EXPERIMENTAL_CHECKERS)}
    if (X_USE_FIR_IC in this) { arguments.useFirIC = get(X_USE_FIR_IC)}
    if (X_USE_FIR_LT in this) { arguments.useFirLT = get(X_USE_FIR_LT)}
    if (X_METADATA_KLIB in this) { arguments.metadataKlib = get(X_METADATA_KLIB)}
    if (X_DISABLE_DEFAULT_SCRIPTING_PLUGIN in this) { arguments.disableDefaultScriptingPlugin = get(X_DISABLE_DEFAULT_SCRIPTING_PLUGIN)}
    if (X_EXPLICIT_API in this) { arguments.explicitApi = get(X_EXPLICIT_API).stringValue}
    if (XX_EXPLICIT_RETURN_TYPES in this) { arguments.explicitReturnTypes = get(XX_EXPLICIT_RETURN_TYPES).stringValue}
    if (X_RETURN_VALUE_CHECKER in this) { arguments.returnValueChecker = get(X_RETURN_VALUE_CHECKER).stringValue}
    if (X_SUPPRESS_VERSION_WARNINGS in this) { arguments.suppressVersionWarnings = get(X_SUPPRESS_VERSION_WARNINGS)}
    if (X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR in this) { arguments.suppressApiVersionGreaterThanLanguageVersionError = get(X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR)}
    if (X_EXPECT_ACTUAL_CLASSES in this) { arguments.expectActualClasses = get(X_EXPECT_ACTUAL_CLASSES)}
    if (X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY in this) { arguments.consistentDataClassCopyVisibility = get(X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY)}
    if (X_UNRESTRICTED_BUILDER_INFERENCE in this) { arguments.unrestrictedBuilderInference = get(X_UNRESTRICTED_BUILDER_INFERENCE)}
    if (X_CONTEXT_RECEIVERS in this) { arguments.contextReceivers = get(X_CONTEXT_RECEIVERS)}
    if (X_CONTEXT_PARAMETERS in this) { arguments.contextParameters = get(X_CONTEXT_PARAMETERS)}
    if (X_CONTEXT_SENSITIVE_RESOLUTION in this) { arguments.contextSensitiveResolution = get(X_CONTEXT_SENSITIVE_RESOLUTION)}
    if (X_NON_LOCAL_BREAK_CONTINUE in this) { arguments.nonLocalBreakContinue = get(X_NON_LOCAL_BREAK_CONTINUE)}
    if (X_DATA_FLOW_BASED_EXHAUSTIVENESS in this) { arguments.dataFlowBasedExhaustiveness = get(X_DATA_FLOW_BASED_EXHAUSTIVENESS)}
    if (X_EXPLICIT_BACKING_FIELDS in this) { arguments.explicitBackingFields = get(X_EXPLICIT_BACKING_FIELDS)}
    if (X_DIRECT_JAVA_ACTUALIZATION in this) { arguments.directJavaActualization = get(X_DIRECT_JAVA_ACTUALIZATION)}
    if (X_MULTI_DOLLAR_INTERPOLATION in this) { arguments.multiDollarInterpolation = get(X_MULTI_DOLLAR_INTERPOLATION)}
    if (X_ENABLE_INCREMENTAL_COMPILATION in this) { arguments.incrementalCompilation = get(X_ENABLE_INCREMENTAL_COMPILATION)}
    if (X_RENDER_INTERNAL_DIAGNOSTIC_NAMES in this) { arguments.renderInternalDiagnosticNames = get(X_RENDER_INTERNAL_DIAGNOSTIC_NAMES)}
    if (X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS in this) { arguments.allowAnyScriptsInSourceRoots = get(X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)}
    if (X_REPORT_ALL_WARNINGS in this) { arguments.reportAllWarnings = get(X_REPORT_ALL_WARNINGS)}
    if (X_FRAGMENTS in this) { arguments.fragments = get(X_FRAGMENTS)}
    if (X_FRAGMENT_SOURCES in this) { arguments.fragmentSources = get(X_FRAGMENT_SOURCES)}
    if (X_FRAGMENT_REFINES in this) { arguments.fragmentRefines = get(X_FRAGMENT_REFINES)}
    if (X_FRAGMENT_DEPENDENCY in this) { arguments.fragmentDependencies = get(X_FRAGMENT_DEPENDENCY)}
    if (X_FRAGMENT_FRIEND_DEPENDENCY in this) { arguments.fragmentFriendDependencies = get(X_FRAGMENT_FRIEND_DEPENDENCY)}
    if (X_SEPARATE_KMP_COMPILATION in this) { arguments.separateKmpCompilationScheme = get(X_SEPARATE_KMP_COMPILATION)}
    if (X_IGNORE_CONST_OPTIMIZATION_ERRORS in this) { arguments.ignoreConstOptimizationErrors = get(X_IGNORE_CONST_OPTIMIZATION_ERRORS)}
    if (X_DONT_WARN_ON_ERROR_SUPPRESSION in this) { arguments.dontWarnOnErrorSuppression = get(X_DONT_WARN_ON_ERROR_SUPPRESSION)}
    if (X_WHEN_GUARDS in this) { arguments.whenGuards = get(X_WHEN_GUARDS)}
    if (X_NESTED_TYPE_ALIASES in this) { arguments.nestedTypeAliases = get(X_NESTED_TYPE_ALIASES)}
    if (X_SUPPRESS_WARNING in this) { arguments.suppressedDiagnostics = get(X_SUPPRESS_WARNING)}
    if (X_WARNING_LEVEL in this) { arguments.warningLevels = get(X_WARNING_LEVEL)}
    if (X_ANNOTATION_DEFAULT_TARGET in this) { arguments.annotationDefaultTarget = get(X_ANNOTATION_DEFAULT_TARGET)}
    if (XX_DEBUG_LEVEL_COMPILER_CHECKS in this) { arguments.debugLevelCompilerChecks = get(XX_DEBUG_LEVEL_COMPILER_CHECKS)}
    if (X_ANNOTATION_TARGET_ALL in this) { arguments.annotationTargetAll = get(X_ANNOTATION_TARGET_ALL)}
    if (XX_LENIENT_MODE in this) { arguments.lenientMode = get(XX_LENIENT_MODE)}
    if (X_ALLOW_REIFIED_TYPE_IN_CATCH in this) { arguments.allowReifiedTypeInCatch = get(X_ALLOW_REIFIED_TYPE_IN_CATCH)}
    if (X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS in this) { arguments.allowContractsOnMoreFunctions = get(X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS)}
    if (X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS in this) { arguments.allowConditionImpliesReturnsContracts = get(X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS)}
    if (X_ALLOW_HOLDSIN_CONTRACT in this) { arguments.allowHoldsinContract = get(X_ALLOW_HOLDSIN_CONTRACT)}
    if (X_NAME_BASED_DESTRUCTURING in this) { arguments.nameBasedDestructuring = get(X_NAME_BASED_DESTRUCTURING)}
    if (XX_LANGUAGE in this) { arguments.manuallyConfiguredFeatures = get(XX_LANGUAGE)}
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: CommonCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[LANGUAGE_VERSION] = arguments.languageVersion?.let { EnumsKotlinVersion.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {  }
    try { this[API_VERSION] = arguments.apiVersion?.let { EnumsKotlinVersion.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {  }
    try { this[KOTLIN_HOME] = arguments.kotlinHome } catch (_: NoSuchMethodError) {  }
    try { this[HEADER] = arguments.headerMode } catch (_: NoSuchMethodError) {  }
    try { this[PROGRESSIVE] = arguments.progressiveMode } catch (_: NoSuchMethodError) {  }
    try { this[SCRIPT] = arguments.script } catch (_: NoSuchMethodError) {  }
    try { this[X_REPL] = arguments.repl } catch (_: NoSuchMethodError) {  }
    try { this[OPT_IN] = arguments.optIn } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_INLINE] = arguments.noInline } catch (_: NoSuchMethodError) {  }
    try { this[X_SKIP_METADATA_VERSION_CHECK] = arguments.skipMetadataVersionCheck } catch (_: NoSuchMethodError) {  }
    try { this[X_SKIP_PRERELEASE_CHECK] = arguments.skipPrereleaseCheck } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_KOTLIN_PACKAGE] = arguments.allowKotlinPackage } catch (_: NoSuchMethodError) {  }
    try { this[X_STDLIB_COMPILATION] = arguments.stdlibCompilation } catch (_: NoSuchMethodError) {  }
    try { this[X_REPORT_OUTPUT_FILES] = arguments.reportOutputFiles } catch (_: NoSuchMethodError) {  }
    try { this[X_PLUGIN] = arguments.pluginClasspaths } catch (_: NoSuchMethodError) {  }
    try { this[P] = arguments.pluginOptions } catch (_: NoSuchMethodError) {  }
    try { this[X_COMPILER_PLUGIN] = arguments.pluginConfigurations } catch (_: NoSuchMethodError) {  }
    try { this[X_COMPILER_PLUGIN_ORDER] = arguments.pluginOrderConstraints } catch (_: NoSuchMethodError) {  }
    try { this[X_MULTI_PLATFORM] = arguments.multiPlatform } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_CHECK_ACTUAL] = arguments.noCheckActual } catch (_: NoSuchMethodError) {  }
    try { this[X_INTELLIJ_PLUGIN_ROOT] = arguments.intellijPluginRoot } catch (_: NoSuchMethodError) {  }
    try { this[X_NEW_INFERENCE] = arguments.newInference } catch (_: NoSuchMethodError) {  }
    try { this[X_INLINE_CLASSES] = arguments.inlineClasses } catch (_: NoSuchMethodError) {  }
    try { this[X_REPORT_PERF] = arguments.reportPerf } catch (_: NoSuchMethodError) {  }
    try { this[X_DETAILED_PERF] = arguments.detailedPerf } catch (_: NoSuchMethodError) {  }
    try { this[X_DUMP_PERF] = arguments.dumpPerf } catch (_: NoSuchMethodError) {  }
    try { this[XX_DUMP_MODEL] = arguments.dumpArgumentsDir } catch (_: NoSuchMethodError) {  }
    try { this[X_METADATA_VERSION] = arguments.metadataVersion } catch (_: NoSuchMethodError) {  }
    try { this[X_COMMON_SOURCES] = arguments.commonSources } catch (_: NoSuchMethodError) {  }
    try { this[X_LIST_PHASES] = arguments.listPhases } catch (_: NoSuchMethodError) {  }
    try { this[X_DISABLE_PHASES] = arguments.disablePhases } catch (_: NoSuchMethodError) {  }
    try { this[X_VERBOSE_PHASES] = arguments.verbosePhases } catch (_: NoSuchMethodError) {  }
    try { this[X_PHASES_TO_DUMP_BEFORE] = arguments.phasesToDumpBefore } catch (_: NoSuchMethodError) {  }
    try { this[X_PHASES_TO_DUMP_AFTER] = arguments.phasesToDumpAfter } catch (_: NoSuchMethodError) {  }
    try { this[X_PHASES_TO_DUMP] = arguments.phasesToDump } catch (_: NoSuchMethodError) {  }
    try { this[X_DUMP_DIRECTORY] = arguments.dumpDirectory } catch (_: NoSuchMethodError) {  }
    try { this[X_DUMP_FQNAME] = arguments.dumpOnlyFqName } catch (_: NoSuchMethodError) {  }
    try { this[X_PHASES_TO_VALIDATE_BEFORE] = arguments.phasesToValidateBefore } catch (_: NoSuchMethodError) {  }
    try { this[X_PHASES_TO_VALIDATE_AFTER] = arguments.phasesToValidateAfter } catch (_: NoSuchMethodError) {  }
    try { this[X_PHASES_TO_VALIDATE] = arguments.phasesToValidate } catch (_: NoSuchMethodError) {  }
    try { this[X_VERIFY_IR] = arguments.verifyIr } catch (_: NoSuchMethodError) {  }
    try { this[X_VERIFY_IR_VISIBILITY] = arguments.verifyIrVisibility } catch (_: NoSuchMethodError) {  }
    try { this[X_PROFILE_PHASES] = arguments.profilePhases } catch (_: NoSuchMethodError) {  }
    try { this[X_CHECK_PHASE_CONDITIONS] = arguments.checkPhaseConditions } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_K2] = arguments.getUsingReflection("useK2") } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_FIR_EXPERIMENTAL_CHECKERS] = arguments.useFirExperimentalCheckers } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_FIR_IC] = arguments.useFirIC } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_FIR_LT] = arguments.useFirLT } catch (_: NoSuchMethodError) {  }
    try { this[X_METADATA_KLIB] = arguments.metadataKlib } catch (_: NoSuchMethodError) {  }
    try { this[X_DISABLE_DEFAULT_SCRIPTING_PLUGIN] = arguments.disableDefaultScriptingPlugin } catch (_: NoSuchMethodError) {  }
    try { this[X_EXPLICIT_API] = arguments.explicitApi.let { ExplicitApiMode.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {  }
    try { this[XX_EXPLICIT_RETURN_TYPES] = arguments.explicitReturnTypes.let { ExplicitApiMode.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {  }
    try { this[X_RETURN_VALUE_CHECKER] = arguments.returnValueChecker.let { ReturnValueCheckerMode.entries.first { entry -> entry.stringValue == it } } } catch (_: NoSuchMethodError) {  }
    try { this[X_SUPPRESS_VERSION_WARNINGS] = arguments.suppressVersionWarnings } catch (_: NoSuchMethodError) {  }
    try { this[X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR] = arguments.suppressApiVersionGreaterThanLanguageVersionError } catch (_: NoSuchMethodError) {  }
    try { this[X_EXPECT_ACTUAL_CLASSES] = arguments.expectActualClasses } catch (_: NoSuchMethodError) {  }
    try { this[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = arguments.consistentDataClassCopyVisibility } catch (_: NoSuchMethodError) {  }
    try { this[X_UNRESTRICTED_BUILDER_INFERENCE] = arguments.unrestrictedBuilderInference } catch (_: NoSuchMethodError) {  }
    try { this[X_CONTEXT_RECEIVERS] = arguments.contextReceivers } catch (_: NoSuchMethodError) {  }
    try { this[X_CONTEXT_PARAMETERS] = arguments.contextParameters } catch (_: NoSuchMethodError) {  }
    try { this[X_CONTEXT_SENSITIVE_RESOLUTION] = arguments.contextSensitiveResolution } catch (_: NoSuchMethodError) {  }
    try { this[X_NON_LOCAL_BREAK_CONTINUE] = arguments.nonLocalBreakContinue } catch (_: NoSuchMethodError) {  }
    try { this[X_DATA_FLOW_BASED_EXHAUSTIVENESS] = arguments.dataFlowBasedExhaustiveness } catch (_: NoSuchMethodError) {  }
    try { this[X_EXPLICIT_BACKING_FIELDS] = arguments.explicitBackingFields } catch (_: NoSuchMethodError) {  }
    try { this[X_DIRECT_JAVA_ACTUALIZATION] = arguments.directJavaActualization } catch (_: NoSuchMethodError) {  }
    try { this[X_MULTI_DOLLAR_INTERPOLATION] = arguments.multiDollarInterpolation } catch (_: NoSuchMethodError) {  }
    try { this[X_ENABLE_INCREMENTAL_COMPILATION] = arguments.incrementalCompilation } catch (_: NoSuchMethodError) {  }
    try { this[X_RENDER_INTERNAL_DIAGNOSTIC_NAMES] = arguments.renderInternalDiagnosticNames } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS] = arguments.allowAnyScriptsInSourceRoots } catch (_: NoSuchMethodError) {  }
    try { this[X_REPORT_ALL_WARNINGS] = arguments.reportAllWarnings } catch (_: NoSuchMethodError) {  }
    try { this[X_FRAGMENTS] = arguments.fragments } catch (_: NoSuchMethodError) {  }
    try { this[X_FRAGMENT_SOURCES] = arguments.fragmentSources } catch (_: NoSuchMethodError) {  }
    try { this[X_FRAGMENT_REFINES] = arguments.fragmentRefines } catch (_: NoSuchMethodError) {  }
    try { this[X_FRAGMENT_DEPENDENCY] = arguments.fragmentDependencies } catch (_: NoSuchMethodError) {  }
    try { this[X_FRAGMENT_FRIEND_DEPENDENCY] = arguments.fragmentFriendDependencies } catch (_: NoSuchMethodError) {  }
    try { this[X_SEPARATE_KMP_COMPILATION] = arguments.separateKmpCompilationScheme } catch (_: NoSuchMethodError) {  }
    try { this[X_IGNORE_CONST_OPTIMIZATION_ERRORS] = arguments.ignoreConstOptimizationErrors } catch (_: NoSuchMethodError) {  }
    try { this[X_DONT_WARN_ON_ERROR_SUPPRESSION] = arguments.dontWarnOnErrorSuppression } catch (_: NoSuchMethodError) {  }
    try { this[X_WHEN_GUARDS] = arguments.whenGuards } catch (_: NoSuchMethodError) {  }
    try { this[X_NESTED_TYPE_ALIASES] = arguments.nestedTypeAliases } catch (_: NoSuchMethodError) {  }
    try { this[X_SUPPRESS_WARNING] = arguments.suppressedDiagnostics } catch (_: NoSuchMethodError) {  }
    try { this[X_WARNING_LEVEL] = arguments.warningLevels } catch (_: NoSuchMethodError) {  }
    try { this[X_ANNOTATION_DEFAULT_TARGET] = arguments.annotationDefaultTarget } catch (_: NoSuchMethodError) {  }
    try { this[XX_DEBUG_LEVEL_COMPILER_CHECKS] = arguments.debugLevelCompilerChecks } catch (_: NoSuchMethodError) {  }
    try { this[X_ANNOTATION_TARGET_ALL] = arguments.annotationTargetAll } catch (_: NoSuchMethodError) {  }
    try { this[XX_LENIENT_MODE] = arguments.lenientMode } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_REIFIED_TYPE_IN_CATCH] = arguments.allowReifiedTypeInCatch } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS] = arguments.allowContractsOnMoreFunctions } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS] = arguments.allowConditionImpliesReturnsContracts } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_HOLDSIN_CONTRACT] = arguments.allowHoldsinContract } catch (_: NoSuchMethodError) {  }
    try { this[X_NAME_BASED_DESTRUCTURING] = arguments.nameBasedDestructuring } catch (_: NoSuchMethodError) {  }
    try { this[XX_LANGUAGE] = arguments.manuallyConfiguredFeatures } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class CommonCompilerArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val LANGUAGE_VERSION: CommonCompilerArgument<EnumsKotlinVersion?> =
        CommonCompilerArgument("LANGUAGE_VERSION")

    public val API_VERSION: CommonCompilerArgument<EnumsKotlinVersion?> =
        CommonCompilerArgument("API_VERSION")

    public val KOTLIN_HOME: CommonCompilerArgument<String?> = CommonCompilerArgument("KOTLIN_HOME")

    public val HEADER: CommonCompilerArgument<Boolean> = CommonCompilerArgument("HEADER")

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

    public val X_COMPILER_PLUGIN_ORDER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMPILER_PLUGIN_ORDER")

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

    public val X_DETAILED_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DETAILED_PERF")

    public val X_DUMP_PERF: CommonCompilerArgument<String?> = CommonCompilerArgument("X_DUMP_PERF")

    public val XX_DUMP_MODEL: CommonCompilerArgument<String?> =
        CommonCompilerArgument("XX_DUMP_MODEL")

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

    public val X_DATA_FLOW_BASED_EXHAUSTIVENESS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DATA_FLOW_BASED_EXHAUSTIVENESS")

    public val X_EXPLICIT_BACKING_FIELDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPLICIT_BACKING_FIELDS")

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

    public val X_FRAGMENT_FRIEND_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_FRIEND_DEPENDENCY")

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

    public val X_NAME_BASED_DESTRUCTURING: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_NAME_BASED_DESTRUCTURING")

    public val XX_LANGUAGE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XX_LANGUAGE")
  }
}
