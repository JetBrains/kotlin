// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.lang.IllegalStateException
import java.nio.`file`.Path
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
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.listOf
import kotlin.collections.mutableMapOf
import kotlin.collections.toTypedArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.HeaderMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal abstract class CommonCompilerArgumentsImpl() : CommonToolArgumentsImpl(),
    ArgumentsCommonCompilerArguments, ArgumentsCommonCompilerArguments.Builder {
  @Transient
  private val defaultArguments: CommonCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("P")
  internal var pluginOptions: Array<String>? = defaultArguments.pluginOptions

  @SerialName("XX_LANGUAGE")
  internal var manuallyConfiguredFeatures: Array<String>? =
      defaultArguments.manuallyConfiguredFeatures

  @SerialName("XX_DEBUG_LEVEL_COMPILER_CHECKS")
  internal var debugLevelCompilerChecks: Boolean = defaultArguments.debugLevelCompilerChecks

  @SerialName("XX_DUMP_MODEL")
  internal var dumpArgumentsDir: String? = defaultArguments.dumpArgumentsDir

  @SerialName("XX_EXPLICIT_RETURN_TYPES")
  internal var explicitReturnTypes: ExplicitApiMode =
      defaultArguments.explicitReturnTypes.let { ExplicitApiMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::explicitReturnTypes, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") }

  @SerialName("XX_LENIENT_MODE")
  internal var lenientMode: Boolean = defaultArguments.lenientMode

  @SerialName("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")
  internal var allowAnyScriptsInSourceRoots: Boolean = defaultArguments.allowAnyScriptsInSourceRoots

  @SerialName("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS")
  internal var allowConditionImpliesReturnsContracts: Boolean =
      defaultArguments.allowConditionImpliesReturnsContracts

  @SerialName("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS")
  internal var allowContractsOnMoreFunctions: Boolean =
      defaultArguments.allowContractsOnMoreFunctions

  @SerialName("X_ALLOW_HOLDSIN_CONTRACT")
  internal var allowHoldsinContract: Boolean = defaultArguments.allowHoldsinContract

  @SerialName("X_ALLOW_KOTLIN_PACKAGE")
  internal var allowKotlinPackage: Boolean = defaultArguments.allowKotlinPackage

  @SerialName("X_ALLOW_PRE_17_RUNTIME_JDK")
  internal var allowPre17RuntimeJdk: Boolean = defaultArguments.allowPre17RuntimeJdk

  @SerialName("X_ALLOW_REIFIED_TYPE_IN_CATCH")
  internal var allowReifiedTypeInCatch: Boolean = defaultArguments.allowReifiedTypeInCatch

  @SerialName("X_ALLOW_RETURNS_RESULT_OF")
  internal var allowReturnsResultOf: Boolean = defaultArguments.allowReturnsResultOf

  @SerialName("X_ANNOTATION_DEFAULT_TARGET")
  internal var annotationDefaultTarget: AnnotationDefaultTargetMode? =
      defaultArguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::annotationDefaultTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") }

  @SerialName("X_ANNOTATION_TARGET_ALL")
  internal var annotationTargetAll: Boolean = defaultArguments.annotationTargetAll

  @SerialName("X_CALLABLE_REFERENCES_TO_CONTEXTUAL")
  internal var callableReferencesToContextual: Boolean =
      defaultArguments.callableReferencesToContextual

  @SerialName("X_CHECK_PHASE_CONDITIONS")
  internal var checkPhaseConditions: Boolean = defaultArguments.checkPhaseConditions

  @SerialName("X_COLLECTION_LITERALS")
  internal var collectionLiterals: Boolean = defaultArguments.collectionLiterals

  @SerialName("X_COMMON_SOURCES")
  internal var commonSources: Array<String>? = defaultArguments.commonSources

  @SerialName("X_COMPANION_BLOCKS")
  internal var companionBlocks: Boolean = defaultArguments.companionBlocks

  @SerialName("X_COMPANION_BLOCKS_AND_EXTENSIONS")
  internal var companionBlocksAndExtensions: Boolean = defaultArguments.companionBlocksAndExtensions

  @SerialName("X_COMPILER_PLUGIN")
  internal var pluginConfigurations: Array<String>? = defaultArguments.pluginConfigurations

  @SerialName("X_COMPILER_PLUGIN_ORDER")
  internal var pluginOrderConstraints: Array<String>? = defaultArguments.pluginOrderConstraints

  @SerialName("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY")
  internal var consistentDataClassCopyVisibility: Boolean =
      defaultArguments.consistentDataClassCopyVisibility

  @SerialName("X_CONTEXT_PARAMETERS")
  internal var contextParameters: Boolean = defaultArguments.contextParameters

  @SerialName("X_CONTEXT_SENSITIVE_RESOLUTION")
  internal var contextSensitiveResolution: Boolean = defaultArguments.contextSensitiveResolution

  @SerialName("X_DATA_FLOW_BASED_EXHAUSTIVENESS")
  internal var dataFlowBasedExhaustiveness: Boolean = defaultArguments.dataFlowBasedExhaustiveness

  @SerialName("X_DETAILED_PERF")
  internal var detailedPerf: Boolean = defaultArguments.detailedPerf

  @SerialName("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")
  internal var disableDefaultScriptingPlugin: Boolean =
      defaultArguments.disableDefaultScriptingPlugin

  @SerialName("X_DISABLE_IR_CHECKERS")
  internal var disableIrCheckers: Array<String>? = defaultArguments.disableIrCheckers

  @SerialName("X_DISABLE_PHASES")
  internal var disablePhases: List<String> = defaultArguments.disablePhases.toListOrEmpty()

  @SerialName("X_DO_NOT_NORMALIZE_NAN_VALUES_IN_CONST_CONTEXT")
  internal var doNotNormalizeNanValuesInConstContext: Boolean =
      defaultArguments.doNotNormalizeNanValuesInConstContext

  @SerialName("X_DONT_SORT_SOURCE_FILES")
  internal var dontSortSourceFiles: Boolean = defaultArguments.dontSortSourceFiles

  @SerialName("X_DONT_WARN_ON_ERROR_SUPPRESSION")
  internal var dontWarnOnErrorSuppression: Boolean = defaultArguments.dontWarnOnErrorSuppression

  @SerialName("X_DUMP_DIRECTORY")
  internal var dumpDirectory: Path? =
      defaultArguments.dumpDirectory?.let { kotlin.io.path.Path(it) }

  @SerialName("X_DUMP_FQNAME")
  internal var dumpOnlyFqName: String? = defaultArguments.dumpOnlyFqName

  @SerialName("X_DUMP_PERF")
  internal var dumpPerf: Path? = defaultArguments.dumpPerf?.let { kotlin.io.path.Path(it) }

  @SerialName("X_EAGER_LAMBDA_ANALYSIS")
  internal var eagerLambdaAnalysis: Boolean = defaultArguments.eagerLambdaAnalysis

  @SerialName("X_ENABLE_ADDITIONAL_IR_CHECKERS")
  internal var enableAdditionalIrCheckers: Array<String>? =
      defaultArguments.enableAdditionalIrCheckers

  @SerialName("X_ENABLE_INCREMENTAL_COMPILATION")
  internal var incrementalCompilation: Boolean? = defaultArguments.incrementalCompilation

  @SerialName("X_ESCAPING_FUNCTIONS")
  internal var escapingFunctions: List<String> = defaultArguments.escapingFunctions.toListOrEmpty()

  @SerialName("X_EXPECT_ACTUAL_CLASSES")
  internal var expectActualClasses: Boolean = defaultArguments.expectActualClasses

  @SerialName("X_EXPLICIT_API")
  internal var explicitApi: ExplicitApiMode =
      defaultArguments.explicitApi.let { ExplicitApiMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::explicitApi, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") }

  @SerialName("X_EXPLICIT_BACKING_FIELDS")
  internal var explicitBackingFields: Boolean = defaultArguments.explicitBackingFields

  @SerialName("X_EXPLICIT_CONTEXT_ARGUMENTS")
  internal var explicitContextArguments: Boolean = defaultArguments.explicitContextArguments

  @SerialName("X_FIR_AGGRESSIVE_PRUNING")
  internal var firAggressivePruning: Boolean? = defaultArguments.firAggressivePruning

  @SerialName("X_FRAGMENT_DEPENDENCY")
  internal var fragmentDependencies: Array<String>? = defaultArguments.fragmentDependencies

  @SerialName("X_FRAGMENT_FRIEND_DEPENDENCY")
  internal var fragmentFriendDependencies: Array<String>? =
      defaultArguments.fragmentFriendDependencies

  @SerialName("X_FRAGMENT_REFINES")
  internal var fragmentRefines: Array<String>? = defaultArguments.fragmentRefines

  @SerialName("X_FRAGMENT_SOURCES")
  internal var fragmentSources: Array<String>? = defaultArguments.fragmentSources

  @SerialName("X_FRAGMENTS")
  internal var fragments: Array<String>? = defaultArguments.fragments

  @SerialName("X_FULL_VALUE_CLASSES")
  internal var fullValueClasses: Boolean = defaultArguments.fullValueClasses

  @SerialName("X_HEADER_MODE")
  internal var headerMode: Boolean = defaultArguments.headerMode

  @SerialName("X_HEADER_MODE_TYPE")
  internal var headerModeType: HeaderMode =
      defaultArguments.headerModeType.let { HeaderMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::headerModeType, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xheader-mode-type value: $it") }

  @SerialName("X_INTRINSIC_CONST_EVALUATION")
  internal var intrinsicConstEvaluation: Boolean = defaultArguments.intrinsicConstEvaluation

  @SerialName("X_LIST_PHASES")
  internal var listPhases: Boolean = defaultArguments.listPhases

  @SerialName("X_LOCAL_TYPE_ALIASES")
  internal var localTypeAliases: Boolean = defaultArguments.localTypeAliases

  @SerialName("X_METADATA_KLIB")
  internal var metadataKlib: Boolean = defaultArguments.metadataKlib

  @SerialName("X_METADATA_VERSION")
  internal var metadataVersion: String? = defaultArguments.metadataVersion

  @SerialName("X_MULTI_DOLLAR_INTERPOLATION")
  @Suppress("DEPRECATION")
  internal var multiDollarInterpolation: Boolean = defaultArguments.multiDollarInterpolation

  @SerialName("X_MULTI_PLATFORM")
  internal var multiPlatform: Boolean = defaultArguments.multiPlatform

  @SerialName("X_NAME_BASED_DESTRUCTURING")
  internal var nameBasedDestructuring: NameBasedDestructuringMode? =
      defaultArguments.nameBasedDestructuring?.let { NameBasedDestructuringMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::nameBasedDestructuring, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xname-based-destructuring value: $it") }

  @SerialName("X_NESTED_TYPE_ALIASES")
  internal var nestedTypeAliases: Boolean = defaultArguments.nestedTypeAliases

  @SerialName("X_NO_INLINE")
  internal var noInline: Boolean = defaultArguments.noInline

  @SerialName("X_NON_LOCAL_BREAK_CONTINUE")
  @Suppress("DEPRECATION")
  internal var nonLocalBreakContinue: Boolean = defaultArguments.nonLocalBreakContinue

  @SerialName("X_PHASES_TO_DUMP")
  internal var phasesToDump: List<String> = defaultArguments.phasesToDump.toListOrEmpty()

  @SerialName("X_PHASES_TO_DUMP_AFTER")
  internal var phasesToDumpAfter: List<String> = defaultArguments.phasesToDumpAfter.toListOrEmpty()

  @SerialName("X_PHASES_TO_DUMP_BEFORE")
  internal var phasesToDumpBefore: List<String> =
      defaultArguments.phasesToDumpBefore.toListOrEmpty()

  @SerialName("X_PHASES_TO_VALIDATE")
  internal var phasesToValidate: List<String> = defaultArguments.phasesToValidate.toListOrEmpty()

  @SerialName("X_PHASES_TO_VALIDATE_AFTER")
  internal var phasesToValidateAfter: List<String> =
      defaultArguments.phasesToValidateAfter.toListOrEmpty()

  @SerialName("X_PHASES_TO_VALIDATE_BEFORE")
  internal var phasesToValidateBefore: List<String> =
      defaultArguments.phasesToValidateBefore.toListOrEmpty()

  @SerialName("X_PLUGIN")
  internal var pluginClasspaths: Array<String>? = defaultArguments.pluginClasspaths

  @SerialName("X_PRINT_CONFIGURATION")
  internal var printConfiguration: Boolean = defaultArguments.printConfiguration

  @SerialName("X_PROFILE_PHASES")
  internal var profilePhases: Boolean = defaultArguments.profilePhases

  @SerialName("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES")
  internal var renderInternalDiagnosticNames: Boolean =
      defaultArguments.renderInternalDiagnosticNames

  @SerialName("X_REPORT_ALL_WARNINGS")
  internal var reportAllWarnings: Boolean = defaultArguments.reportAllWarnings

  @SerialName("X_REPORT_OUTPUT_FILES")
  internal var reportOutputFiles: Boolean = defaultArguments.reportOutputFiles

  @SerialName("X_REPORT_PERF")
  internal var reportPerf: Boolean = defaultArguments.reportPerf

  @SerialName("X_RETURN_VALUE_CHECKER")
  internal var returnValueChecker: ReturnValueCheckerMode =
      defaultArguments.returnValueChecker.let { ReturnValueCheckerMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::returnValueChecker, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") }

  @SerialName("X_RICH_ERRORS")
  internal var richErrors: Boolean = defaultArguments.richErrors

  @SerialName("X_SEPARATE_KMP_COMPILATION")
  internal var separateKmpCompilationScheme: Boolean = defaultArguments.separateKmpCompilationScheme

  @SerialName("X_SKIP_METADATA_VERSION_CHECK")
  internal var skipMetadataVersionCheck: Boolean = defaultArguments.skipMetadataVersionCheck

  @SerialName("X_SKIP_PRERELEASE_CHECK")
  internal var skipPrereleaseCheck: Boolean = defaultArguments.skipPrereleaseCheck

  @SerialName("X_STDLIB_COMPILATION")
  internal var stdlibCompilation: Boolean = defaultArguments.stdlibCompilation

  @SerialName("X_STRICT_EQUALITY_FOR_STRUCTURAL_CLASSES")
  internal var strictEqualityForStructuralClasses: Boolean =
      defaultArguments.strictEqualityForStructuralClasses

  @SerialName("X_SUPPRESS_VERSION_WARNINGS")
  internal var suppressVersionWarnings: Boolean = defaultArguments.suppressVersionWarnings

  @SerialName("X_USE_FIR_IC")
  @Suppress("DEPRECATION")
  internal var useFirIC: Boolean = defaultArguments.useFirIC

  @SerialName("X_USE_FIR_LT")
  @Suppress("DEPRECATION")
  internal var useFirLT: Boolean = defaultArguments.useFirLT

  @SerialName("X_VERBOSE_PHASES")
  internal var verbosePhases: List<String> = defaultArguments.verbosePhases.toListOrEmpty()

  @SerialName("X_VERIFY_IR")
  internal var verifyIr: VerifyIrMode? =
      defaultArguments.verifyIr?.let { VerifyIrMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::verifyIr, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") }

  @SerialName("X_WHEN_GUARDS")
  @Suppress("DEPRECATION")
  internal var whenGuards: Boolean = defaultArguments.whenGuards

  @SerialName("API_VERSION")
  internal var apiVersion: KotlinVersion? =
      defaultArguments.apiVersion?.let { KotlinVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::apiVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") }

  @SerialName("KOTLIN_HOME")
  internal var kotlinHome: Path? = defaultArguments.kotlinHome?.let { kotlin.io.path.Path(it) }

  @SerialName("LANGUAGE_VERSION")
  internal var languageVersion: KotlinVersion? =
      defaultArguments.languageVersion?.let { KotlinVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::languageVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") }

  @SerialName("OPT_IN")
  internal var optIn: List<String> = defaultArguments.optIn.toListOrEmpty()

  @SerialName("PROGRESSIVE")
  internal var progressiveMode: Boolean = defaultArguments.progressiveMode

  @SerialName("SCRIPT")
  internal var script: Boolean = defaultArguments.script

  @SerialName("COMPILER_PLUGINS")
  internal var compilerPlugins: List<CompilerPluginImpl> =
      @Suppress("UNCHECKED_CAST") applyCompilerPlugins(CommonCompilerArgumentValueAdapter.toImpl(emptyList<CompilerPluginImpl>()) as List<CompilerPluginImpl>, defaultArguments)

  @SerialName("X_WARNING_LEVEL")
  internal var warningLevel: List<WarningLevelImpl> =
      @Suppress("UNCHECKED_CAST") applyWarningLevels(CommonCompilerArgumentValueAdapter.toImpl(emptyList<WarningLevelImpl>()) as List<WarningLevelImpl>, defaultArguments)

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  abstract override fun createDefaultArguments(): CommonCompilerArguments

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V = CommonCompilerArgumentValueAdapter.toApi(when (key.id) {
    "P" -> pluginOptions
    "XX_LANGUAGE" -> manuallyConfiguredFeatures
    "XX_DEBUG_LEVEL_COMPILER_CHECKS" -> debugLevelCompilerChecks
    "XX_DUMP_MODEL" -> dumpArgumentsDir
    "XX_EXPLICIT_RETURN_TYPES" -> explicitReturnTypes
    "XX_LENIENT_MODE" -> lenientMode
    "X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" -> allowAnyScriptsInSourceRoots
    "X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" -> allowConditionImpliesReturnsContracts
    "X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" -> allowContractsOnMoreFunctions
    "X_ALLOW_HOLDSIN_CONTRACT" -> allowHoldsinContract
    "X_ALLOW_KOTLIN_PACKAGE" -> allowKotlinPackage
    "X_ALLOW_PRE_17_RUNTIME_JDK" -> allowPre17RuntimeJdk
    "X_ALLOW_REIFIED_TYPE_IN_CATCH" -> allowReifiedTypeInCatch
    "X_ALLOW_RETURNS_RESULT_OF" -> allowReturnsResultOf
    "X_ANNOTATION_DEFAULT_TARGET" -> annotationDefaultTarget
    "X_ANNOTATION_TARGET_ALL" -> annotationTargetAll
    "X_CALLABLE_REFERENCES_TO_CONTEXTUAL" -> callableReferencesToContextual
    "X_CHECK_PHASE_CONDITIONS" -> checkPhaseConditions
    "X_COLLECTION_LITERALS" -> collectionLiterals
    "X_COMMON_SOURCES" -> commonSources
    "X_COMPANION_BLOCKS" -> companionBlocks
    "X_COMPANION_BLOCKS_AND_EXTENSIONS" -> companionBlocksAndExtensions
    "X_COMPILER_PLUGIN" -> pluginConfigurations
    "X_COMPILER_PLUGIN_ORDER" -> pluginOrderConstraints
    "X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" -> consistentDataClassCopyVisibility
    "X_CONTEXT_PARAMETERS" -> contextParameters
    "X_CONTEXT_SENSITIVE_RESOLUTION" -> contextSensitiveResolution
    "X_DATA_FLOW_BASED_EXHAUSTIVENESS" -> dataFlowBasedExhaustiveness
    "X_DETAILED_PERF" -> detailedPerf
    "X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" -> disableDefaultScriptingPlugin
    "X_DISABLE_IR_CHECKERS" -> disableIrCheckers
    "X_DISABLE_PHASES" -> disablePhases
    "X_DO_NOT_NORMALIZE_NAN_VALUES_IN_CONST_CONTEXT" -> doNotNormalizeNanValuesInConstContext
    "X_DONT_SORT_SOURCE_FILES" -> dontSortSourceFiles
    "X_DONT_WARN_ON_ERROR_SUPPRESSION" -> dontWarnOnErrorSuppression
    "X_DUMP_DIRECTORY" -> dumpDirectory
    "X_DUMP_FQNAME" -> dumpOnlyFqName
    "X_DUMP_PERF" -> dumpPerf
    "X_EAGER_LAMBDA_ANALYSIS" -> eagerLambdaAnalysis
    "X_ENABLE_ADDITIONAL_IR_CHECKERS" -> enableAdditionalIrCheckers
    "X_ENABLE_INCREMENTAL_COMPILATION" -> incrementalCompilation
    "X_ESCAPING_FUNCTIONS" -> escapingFunctions
    "X_EXPECT_ACTUAL_CLASSES" -> expectActualClasses
    "X_EXPLICIT_API" -> explicitApi
    "X_EXPLICIT_BACKING_FIELDS" -> explicitBackingFields
    "X_EXPLICIT_CONTEXT_ARGUMENTS" -> explicitContextArguments
    "X_FIR_AGGRESSIVE_PRUNING" -> firAggressivePruning
    "X_FRAGMENT_DEPENDENCY" -> fragmentDependencies
    "X_FRAGMENT_FRIEND_DEPENDENCY" -> fragmentFriendDependencies
    "X_FRAGMENT_REFINES" -> fragmentRefines
    "X_FRAGMENT_SOURCES" -> fragmentSources
    "X_FRAGMENTS" -> fragments
    "X_FULL_VALUE_CLASSES" -> fullValueClasses
    "X_HEADER_MODE" -> headerMode
    "X_HEADER_MODE_TYPE" -> headerModeType
    "X_INTRINSIC_CONST_EVALUATION" -> intrinsicConstEvaluation
    "X_LIST_PHASES" -> listPhases
    "X_LOCAL_TYPE_ALIASES" -> localTypeAliases
    "X_METADATA_KLIB" -> metadataKlib
    "X_METADATA_VERSION" -> metadataVersion
    "X_MULTI_DOLLAR_INTERPOLATION" -> multiDollarInterpolation
    "X_MULTI_PLATFORM" -> multiPlatform
    "X_NAME_BASED_DESTRUCTURING" -> nameBasedDestructuring
    "X_NESTED_TYPE_ALIASES" -> nestedTypeAliases
    "X_NO_INLINE" -> noInline
    "X_NON_LOCAL_BREAK_CONTINUE" -> nonLocalBreakContinue
    "X_PHASES_TO_DUMP" -> phasesToDump
    "X_PHASES_TO_DUMP_AFTER" -> phasesToDumpAfter
    "X_PHASES_TO_DUMP_BEFORE" -> phasesToDumpBefore
    "X_PHASES_TO_VALIDATE" -> phasesToValidate
    "X_PHASES_TO_VALIDATE_AFTER" -> phasesToValidateAfter
    "X_PHASES_TO_VALIDATE_BEFORE" -> phasesToValidateBefore
    "X_PLUGIN" -> pluginClasspaths
    "X_PRINT_CONFIGURATION" -> printConfiguration
    "X_PROFILE_PHASES" -> profilePhases
    "X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" -> renderInternalDiagnosticNames
    "X_REPORT_ALL_WARNINGS" -> reportAllWarnings
    "X_REPORT_OUTPUT_FILES" -> reportOutputFiles
    "X_REPORT_PERF" -> reportPerf
    "X_RETURN_VALUE_CHECKER" -> returnValueChecker
    "X_RICH_ERRORS" -> richErrors
    "X_SEPARATE_KMP_COMPILATION" -> separateKmpCompilationScheme
    "X_SKIP_METADATA_VERSION_CHECK" -> skipMetadataVersionCheck
    "X_SKIP_PRERELEASE_CHECK" -> skipPrereleaseCheck
    "X_STDLIB_COMPILATION" -> stdlibCompilation
    "X_STRICT_EQUALITY_FOR_STRUCTURAL_CLASSES" -> strictEqualityForStructuralClasses
    "X_SUPPRESS_VERSION_WARNINGS" -> suppressVersionWarnings
    "X_USE_FIR_IC" -> useFirIC
    "X_USE_FIR_LT" -> useFirLT
    "X_VERBOSE_PHASES" -> verbosePhases
    "X_VERIFY_IR" -> verifyIr
    "X_WHEN_GUARDS" -> whenGuards
    "API_VERSION" -> apiVersion
    "KOTLIN_HOME" -> kotlinHome
    "LANGUAGE_VERSION" -> languageVersion
    "OPT_IN" -> optIn
    "PROGRESSIVE" -> progressiveMode
    "SCRIPT" -> script
    "COMPILER_PLUGINS" -> compilerPlugins
    "X_WARNING_LEVEL" -> warningLevel
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "P" -> pluginOptions = CommonCompilerArgumentValueAdapter.toImpl(value)
      "XX_LANGUAGE" -> manuallyConfiguredFeatures = CommonCompilerArgumentValueAdapter.toImpl(value)
      "XX_DEBUG_LEVEL_COMPILER_CHECKS" -> debugLevelCompilerChecks = CommonCompilerArgumentValueAdapter.toImpl(value)
      "XX_DUMP_MODEL" -> dumpArgumentsDir = CommonCompilerArgumentValueAdapter.toImpl(value)
      "XX_EXPLICIT_RETURN_TYPES" -> explicitReturnTypes = CommonCompilerArgumentValueAdapter.toImpl(value)
      "XX_LENIENT_MODE" -> lenientMode = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" -> allowAnyScriptsInSourceRoots = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS" -> allowConditionImpliesReturnsContracts = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS" -> allowContractsOnMoreFunctions = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_HOLDSIN_CONTRACT" -> allowHoldsinContract = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_KOTLIN_PACKAGE" -> allowKotlinPackage = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_PRE_17_RUNTIME_JDK" -> allowPre17RuntimeJdk = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_REIFIED_TYPE_IN_CATCH" -> allowReifiedTypeInCatch = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_RETURNS_RESULT_OF" -> allowReturnsResultOf = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ANNOTATION_DEFAULT_TARGET" -> annotationDefaultTarget = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ANNOTATION_TARGET_ALL" -> annotationTargetAll = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_CALLABLE_REFERENCES_TO_CONTEXTUAL" -> callableReferencesToContextual = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_CHECK_PHASE_CONDITIONS" -> checkPhaseConditions = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_COLLECTION_LITERALS" -> collectionLiterals = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_COMMON_SOURCES" -> commonSources = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_COMPANION_BLOCKS" -> companionBlocks = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_COMPANION_BLOCKS_AND_EXTENSIONS" -> companionBlocksAndExtensions = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_COMPILER_PLUGIN" -> pluginConfigurations = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_COMPILER_PLUGIN_ORDER" -> pluginOrderConstraints = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" -> consistentDataClassCopyVisibility = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_CONTEXT_PARAMETERS" -> contextParameters = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_CONTEXT_SENSITIVE_RESOLUTION" -> contextSensitiveResolution = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DATA_FLOW_BASED_EXHAUSTIVENESS" -> dataFlowBasedExhaustiveness = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DETAILED_PERF" -> detailedPerf = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" -> disableDefaultScriptingPlugin = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DISABLE_IR_CHECKERS" -> disableIrCheckers = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DISABLE_PHASES" -> disablePhases = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DO_NOT_NORMALIZE_NAN_VALUES_IN_CONST_CONTEXT" -> doNotNormalizeNanValuesInConstContext = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DONT_SORT_SOURCE_FILES" -> dontSortSourceFiles = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DONT_WARN_ON_ERROR_SUPPRESSION" -> dontWarnOnErrorSuppression = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DUMP_DIRECTORY" -> dumpDirectory = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DUMP_FQNAME" -> dumpOnlyFqName = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_DUMP_PERF" -> dumpPerf = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_EAGER_LAMBDA_ANALYSIS" -> eagerLambdaAnalysis = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ENABLE_ADDITIONAL_IR_CHECKERS" -> enableAdditionalIrCheckers = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ENABLE_INCREMENTAL_COMPILATION" -> incrementalCompilation = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_ESCAPING_FUNCTIONS" -> escapingFunctions = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_EXPECT_ACTUAL_CLASSES" -> expectActualClasses = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_EXPLICIT_API" -> explicitApi = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_EXPLICIT_BACKING_FIELDS" -> explicitBackingFields = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_EXPLICIT_CONTEXT_ARGUMENTS" -> explicitContextArguments = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_FIR_AGGRESSIVE_PRUNING" -> firAggressivePruning = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_FRAGMENT_DEPENDENCY" -> fragmentDependencies = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_FRAGMENT_FRIEND_DEPENDENCY" -> fragmentFriendDependencies = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_FRAGMENT_REFINES" -> fragmentRefines = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_FRAGMENT_SOURCES" -> fragmentSources = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_FRAGMENTS" -> fragments = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_FULL_VALUE_CLASSES" -> fullValueClasses = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_HEADER_MODE" -> headerMode = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_HEADER_MODE_TYPE" -> headerModeType = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_INTRINSIC_CONST_EVALUATION" -> intrinsicConstEvaluation = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_LIST_PHASES" -> listPhases = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_LOCAL_TYPE_ALIASES" -> localTypeAliases = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_METADATA_KLIB" -> metadataKlib = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_METADATA_VERSION" -> metadataVersion = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_MULTI_DOLLAR_INTERPOLATION" -> multiDollarInterpolation = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_MULTI_PLATFORM" -> multiPlatform = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_NAME_BASED_DESTRUCTURING" -> nameBasedDestructuring = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_NESTED_TYPE_ALIASES" -> nestedTypeAliases = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_INLINE" -> noInline = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_NON_LOCAL_BREAK_CONTINUE" -> nonLocalBreakContinue = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PHASES_TO_DUMP" -> phasesToDump = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PHASES_TO_DUMP_AFTER" -> phasesToDumpAfter = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PHASES_TO_DUMP_BEFORE" -> phasesToDumpBefore = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PHASES_TO_VALIDATE" -> phasesToValidate = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PHASES_TO_VALIDATE_AFTER" -> phasesToValidateAfter = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PHASES_TO_VALIDATE_BEFORE" -> phasesToValidateBefore = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PLUGIN" -> pluginClasspaths = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PRINT_CONFIGURATION" -> printConfiguration = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_PROFILE_PHASES" -> profilePhases = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" -> renderInternalDiagnosticNames = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_REPORT_ALL_WARNINGS" -> reportAllWarnings = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_REPORT_OUTPUT_FILES" -> reportOutputFiles = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_REPORT_PERF" -> reportPerf = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_RETURN_VALUE_CHECKER" -> returnValueChecker = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_RICH_ERRORS" -> richErrors = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_SEPARATE_KMP_COMPILATION" -> separateKmpCompilationScheme = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_SKIP_METADATA_VERSION_CHECK" -> skipMetadataVersionCheck = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_SKIP_PRERELEASE_CHECK" -> skipPrereleaseCheck = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_STDLIB_COMPILATION" -> stdlibCompilation = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_STRICT_EQUALITY_FOR_STRUCTURAL_CLASSES" -> strictEqualityForStructuralClasses = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_SUPPRESS_VERSION_WARNINGS" -> suppressVersionWarnings = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_FIR_IC" -> useFirIC = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_FIR_LT" -> useFirLT = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_VERBOSE_PHASES" -> verbosePhases = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_VERIFY_IR" -> verifyIr = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_WHEN_GUARDS" -> whenGuards = CommonCompilerArgumentValueAdapter.toImpl(value)
      "API_VERSION" -> apiVersion = CommonCompilerArgumentValueAdapter.toImpl(value)
      "KOTLIN_HOME" -> kotlinHome = CommonCompilerArgumentValueAdapter.toImpl(value)
      "LANGUAGE_VERSION" -> languageVersion = CommonCompilerArgumentValueAdapter.toImpl(value)
      "OPT_IN" -> optIn = CommonCompilerArgumentValueAdapter.toImpl(value)
      "PROGRESSIVE" -> progressiveMode = CommonCompilerArgumentValueAdapter.toImpl(value)
      "SCRIPT" -> script = CommonCompilerArgumentValueAdapter.toImpl(value)
      "COMPILER_PLUGINS" -> compilerPlugins = CommonCompilerArgumentValueAdapter.toImpl(value)
      "X_WARNING_LEVEL" -> warningLevel = CommonCompilerArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = CommonCompilerArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<*>): Boolean = key.id in knownOptionIds

  abstract override fun build(): CommonCompilerArgumentsImpl

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonCompilerArguments): CommonCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter {
      when (it) {
        "X_CONTEXT_RECEIVERS" -> throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_IGNORE_CONST_OPTIMIZATION_ERRORS" -> throw IllegalStateException("""Compiler parameter not recognized: X_IGNORE_CONST_OPTIMIZATION_ERRORS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_INLINE_CLASSES" -> throw IllegalStateException("""Compiler parameter not recognized: X_INLINE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_NEW_INFERENCE" -> throw IllegalStateException("""Compiler parameter not recognized: X_NEW_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" -> throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_SUPPRESS_WARNING" -> throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_WARNING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_UNRESTRICTED_BUILDER_INFERENCE" -> throw IllegalStateException("""Compiler parameter not recognized: X_UNRESTRICTED_BUILDER_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_USE_FIR_EXPERIMENTAL_CHECKERS" -> throw IllegalStateException("""Compiler parameter not recognized: X_USE_FIR_EXPERIMENTAL_CHECKERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_VERIFY_IR_NESTED_OFFSETS" -> throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_NESTED_OFFSETS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.3.20 and removed in 2.4.20""")
        "X_VERIFY_IR_VISIBILITY" -> throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""")
        else -> true
      }
    }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.pluginOptions = pluginOptions ?: emptyArray()
    arguments.manuallyConfiguredFeatures = manuallyConfiguredFeatures ?: emptyArray()
    arguments.debugLevelCompilerChecks = debugLevelCompilerChecks
    arguments.dumpArgumentsDir = dumpArgumentsDir
    arguments.explicitReturnTypes = explicitReturnTypes.stringValue
    arguments.lenientMode = lenientMode
    arguments.allowAnyScriptsInSourceRoots = allowAnyScriptsInSourceRoots
    arguments.allowConditionImpliesReturnsContracts = allowConditionImpliesReturnsContracts
    arguments.allowContractsOnMoreFunctions = allowContractsOnMoreFunctions
    arguments.allowHoldsinContract = allowHoldsinContract
    arguments.allowKotlinPackage = allowKotlinPackage
    arguments.allowPre17RuntimeJdk = allowPre17RuntimeJdk
    arguments.allowReifiedTypeInCatch = allowReifiedTypeInCatch
    arguments.allowReturnsResultOf = allowReturnsResultOf
    arguments.annotationDefaultTarget = annotationDefaultTarget?.stringValue
    arguments.annotationTargetAll = annotationTargetAll
    arguments.callableReferencesToContextual = callableReferencesToContextual
    arguments.checkPhaseConditions = checkPhaseConditions
    arguments.collectionLiterals = collectionLiterals
    arguments.commonSources = commonSources ?: emptyArray()
    arguments.companionBlocks = companionBlocks
    arguments.companionBlocksAndExtensions = companionBlocksAndExtensions
    arguments.pluginConfigurations = pluginConfigurations ?: emptyArray()
    arguments.pluginOrderConstraints = pluginOrderConstraints ?: emptyArray()
    arguments.consistentDataClassCopyVisibility = consistentDataClassCopyVisibility
    arguments.contextParameters = contextParameters
    arguments.contextSensitiveResolution = contextSensitiveResolution
    arguments.dataFlowBasedExhaustiveness = dataFlowBasedExhaustiveness
    arguments.detailedPerf = detailedPerf
    arguments.disableDefaultScriptingPlugin = disableDefaultScriptingPlugin
    arguments.disableIrCheckers = disableIrCheckers ?: emptyArray()
    arguments.disablePhases = disablePhases.toTypedArray()
    arguments.doNotNormalizeNanValuesInConstContext = doNotNormalizeNanValuesInConstContext
    arguments.dontSortSourceFiles = dontSortSourceFiles
    arguments.dontWarnOnErrorSuppression = dontWarnOnErrorSuppression
    arguments.dumpDirectory = dumpDirectory?.absolutePathStringOrThrow()
    arguments.dumpOnlyFqName = dumpOnlyFqName
    arguments.dumpPerf = dumpPerf?.absolutePathStringOrThrow()
    arguments.eagerLambdaAnalysis = eagerLambdaAnalysis
    arguments.enableAdditionalIrCheckers = enableAdditionalIrCheckers ?: emptyArray()
    arguments.incrementalCompilation = incrementalCompilation
    arguments.escapingFunctions = escapingFunctions.toTypedArray()
    arguments.expectActualClasses = expectActualClasses
    arguments.explicitApi = explicitApi.stringValue
    arguments.explicitBackingFields = explicitBackingFields
    arguments.explicitContextArguments = explicitContextArguments
    arguments.firAggressivePruning = firAggressivePruning
    arguments.fragmentDependencies = fragmentDependencies ?: emptyArray()
    arguments.fragmentFriendDependencies = fragmentFriendDependencies ?: emptyArray()
    arguments.fragmentRefines = fragmentRefines ?: emptyArray()
    arguments.fragmentSources = fragmentSources ?: emptyArray()
    arguments.fragments = fragments ?: emptyArray()
    arguments.fullValueClasses = fullValueClasses
    arguments.headerMode = headerMode
    arguments.headerModeType = headerModeType.stringValue
    arguments.intrinsicConstEvaluation = intrinsicConstEvaluation
    arguments.listPhases = listPhases
    arguments.localTypeAliases = localTypeAliases
    arguments.metadataKlib = metadataKlib
    arguments.metadataVersion = metadataVersion
    arguments.multiDollarInterpolation = multiDollarInterpolation
    arguments.multiPlatform = multiPlatform
    arguments.nameBasedDestructuring = nameBasedDestructuring?.stringValue
    arguments.nestedTypeAliases = nestedTypeAliases
    arguments.noInline = noInline
    arguments.nonLocalBreakContinue = nonLocalBreakContinue
    arguments.phasesToDump = phasesToDump.toTypedArray()
    arguments.phasesToDumpAfter = phasesToDumpAfter.toTypedArray()
    arguments.phasesToDumpBefore = phasesToDumpBefore.toTypedArray()
    arguments.phasesToValidate = phasesToValidate.toTypedArray()
    arguments.phasesToValidateAfter = phasesToValidateAfter.toTypedArray()
    arguments.phasesToValidateBefore = phasesToValidateBefore.toTypedArray()
    arguments.pluginClasspaths = pluginClasspaths ?: emptyArray()
    arguments.printConfiguration = printConfiguration
    arguments.profilePhases = profilePhases
    arguments.renderInternalDiagnosticNames = renderInternalDiagnosticNames
    arguments.reportAllWarnings = reportAllWarnings
    arguments.reportOutputFiles = reportOutputFiles
    arguments.reportPerf = reportPerf
    arguments.returnValueChecker = returnValueChecker.stringValue
    arguments.richErrors = richErrors
    arguments.separateKmpCompilationScheme = separateKmpCompilationScheme
    arguments.skipMetadataVersionCheck = skipMetadataVersionCheck
    arguments.skipPrereleaseCheck = skipPrereleaseCheck
    arguments.stdlibCompilation = stdlibCompilation
    arguments.strictEqualityForStructuralClasses = strictEqualityForStructuralClasses
    arguments.suppressVersionWarnings = suppressVersionWarnings
    arguments.useFirIC = useFirIC
    arguments.useFirLT = useFirLT
    arguments.verbosePhases = verbosePhases.toTypedArray()
    arguments.verifyIr = verifyIr?.stringValue
    arguments.whenGuards = whenGuards
    arguments.apiVersion = apiVersion?.stringValue
    arguments.kotlinHome = kotlinHome?.absolutePathStringOrThrow()
    arguments.languageVersion = languageVersion?.stringValue
    arguments.optIn = optIn.toTypedArray()
    arguments.progressiveMode = progressiveMode
    arguments.script = script
    arguments.applyCompilerPlugins(compilerPlugins)
    arguments.applyWarningLevels(warningLevel)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonCompilerArguments) {
    super.applyCompilerArguments(arguments)
    pluginOptions = arguments.pluginOptions
    manuallyConfiguredFeatures = arguments.manuallyConfiguredFeatures
    debugLevelCompilerChecks = arguments.debugLevelCompilerChecks
    dumpArgumentsDir = arguments.dumpArgumentsDir
    try { explicitReturnTypes = arguments.explicitReturnTypes.let { ExplicitApiMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::explicitReturnTypes, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    lenientMode = arguments.lenientMode
    allowAnyScriptsInSourceRoots = arguments.allowAnyScriptsInSourceRoots
    allowConditionImpliesReturnsContracts = arguments.allowConditionImpliesReturnsContracts
    allowContractsOnMoreFunctions = arguments.allowContractsOnMoreFunctions
    allowHoldsinContract = arguments.allowHoldsinContract
    allowKotlinPackage = arguments.allowKotlinPackage
    allowPre17RuntimeJdk = arguments.allowPre17RuntimeJdk
    allowReifiedTypeInCatch = arguments.allowReifiedTypeInCatch
    allowReturnsResultOf = arguments.allowReturnsResultOf
    try { annotationDefaultTarget = arguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::annotationDefaultTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    annotationTargetAll = arguments.annotationTargetAll
    callableReferencesToContextual = arguments.callableReferencesToContextual
    checkPhaseConditions = arguments.checkPhaseConditions
    collectionLiterals = arguments.collectionLiterals
    commonSources = arguments.commonSources
    companionBlocks = arguments.companionBlocks
    companionBlocksAndExtensions = arguments.companionBlocksAndExtensions
    pluginConfigurations = arguments.pluginConfigurations
    pluginOrderConstraints = arguments.pluginOrderConstraints
    consistentDataClassCopyVisibility = arguments.consistentDataClassCopyVisibility
    contextParameters = arguments.contextParameters
    contextSensitiveResolution = arguments.contextSensitiveResolution
    dataFlowBasedExhaustiveness = arguments.dataFlowBasedExhaustiveness
    detailedPerf = arguments.detailedPerf
    disableDefaultScriptingPlugin = arguments.disableDefaultScriptingPlugin
    disableIrCheckers = arguments.disableIrCheckers
    disablePhases = arguments.disablePhases.toListOrEmpty()
    doNotNormalizeNanValuesInConstContext = arguments.doNotNormalizeNanValuesInConstContext
    dontSortSourceFiles = arguments.dontSortSourceFiles
    dontWarnOnErrorSuppression = arguments.dontWarnOnErrorSuppression
    dumpDirectory = arguments.dumpDirectory?.let { kotlin.io.path.Path(it) }
    dumpOnlyFqName = arguments.dumpOnlyFqName
    dumpPerf = arguments.dumpPerf?.let { kotlin.io.path.Path(it) }
    eagerLambdaAnalysis = arguments.eagerLambdaAnalysis
    enableAdditionalIrCheckers = arguments.enableAdditionalIrCheckers
    incrementalCompilation = arguments.incrementalCompilation
    escapingFunctions = arguments.escapingFunctions.toListOrEmpty()
    expectActualClasses = arguments.expectActualClasses
    try { explicitApi = arguments.explicitApi.let { ExplicitApiMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::explicitApi, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    explicitBackingFields = arguments.explicitBackingFields
    explicitContextArguments = arguments.explicitContextArguments
    firAggressivePruning = arguments.firAggressivePruning
    fragmentDependencies = arguments.fragmentDependencies
    fragmentFriendDependencies = arguments.fragmentFriendDependencies
    fragmentRefines = arguments.fragmentRefines
    fragmentSources = arguments.fragmentSources
    fragments = arguments.fragments
    fullValueClasses = arguments.fullValueClasses
    headerMode = arguments.headerMode
    try { headerModeType = arguments.headerModeType.let { HeaderMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::headerModeType, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xheader-mode-type value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    intrinsicConstEvaluation = arguments.intrinsicConstEvaluation
    listPhases = arguments.listPhases
    localTypeAliases = arguments.localTypeAliases
    metadataKlib = arguments.metadataKlib
    metadataVersion = arguments.metadataVersion
    multiDollarInterpolation = arguments.multiDollarInterpolation
    multiPlatform = arguments.multiPlatform
    try { nameBasedDestructuring = arguments.nameBasedDestructuring?.let { NameBasedDestructuringMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::nameBasedDestructuring, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xname-based-destructuring value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    nestedTypeAliases = arguments.nestedTypeAliases
    noInline = arguments.noInline
    nonLocalBreakContinue = arguments.nonLocalBreakContinue
    phasesToDump = arguments.phasesToDump.toListOrEmpty()
    phasesToDumpAfter = arguments.phasesToDumpAfter.toListOrEmpty()
    phasesToDumpBefore = arguments.phasesToDumpBefore.toListOrEmpty()
    phasesToValidate = arguments.phasesToValidate.toListOrEmpty()
    phasesToValidateAfter = arguments.phasesToValidateAfter.toListOrEmpty()
    phasesToValidateBefore = arguments.phasesToValidateBefore.toListOrEmpty()
    pluginClasspaths = arguments.pluginClasspaths
    printConfiguration = arguments.printConfiguration
    profilePhases = arguments.profilePhases
    renderInternalDiagnosticNames = arguments.renderInternalDiagnosticNames
    reportAllWarnings = arguments.reportAllWarnings
    reportOutputFiles = arguments.reportOutputFiles
    reportPerf = arguments.reportPerf
    try { returnValueChecker = arguments.returnValueChecker.let { ReturnValueCheckerMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::returnValueChecker, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    richErrors = arguments.richErrors
    separateKmpCompilationScheme = arguments.separateKmpCompilationScheme
    skipMetadataVersionCheck = arguments.skipMetadataVersionCheck
    skipPrereleaseCheck = arguments.skipPrereleaseCheck
    stdlibCompilation = arguments.stdlibCompilation
    strictEqualityForStructuralClasses = arguments.strictEqualityForStructuralClasses
    suppressVersionWarnings = arguments.suppressVersionWarnings
    useFirIC = arguments.useFirIC
    useFirLT = arguments.useFirLT
    verbosePhases = arguments.verbosePhases.toListOrEmpty()
    try { verifyIr = arguments.verifyIr?.let { VerifyIrMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::verifyIr, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    whenGuards = arguments.whenGuards
    try { apiVersion = arguments.apiVersion?.let { KotlinVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::apiVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    kotlinHome = arguments.kotlinHome?.let { kotlin.io.path.Path(it) }
    try { languageVersion = arguments.languageVersion?.let { KotlinVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::languageVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    optIn = arguments.optIn.toListOrEmpty()
    progressiveMode = arguments.progressiveMode
    script = arguments.script
    try { compilerPlugins = applyCompilerPlugins(compilerPlugins, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { warningLevel = applyWarningLevels(warningLevel, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonCompilerArguments): CommonCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.pluginOptions = pluginOptions ?: emptyArray()
    arguments.manuallyConfiguredFeatures = manuallyConfiguredFeatures ?: emptyArray()
    arguments.debugLevelCompilerChecks = debugLevelCompilerChecks
    arguments.explicitReturnTypes = explicitReturnTypes.stringValue
    arguments.lenientMode = lenientMode
    arguments.allowAnyScriptsInSourceRoots = allowAnyScriptsInSourceRoots
    arguments.allowConditionImpliesReturnsContracts = allowConditionImpliesReturnsContracts
    arguments.allowContractsOnMoreFunctions = allowContractsOnMoreFunctions
    arguments.allowHoldsinContract = allowHoldsinContract
    arguments.allowKotlinPackage = allowKotlinPackage
    arguments.allowPre17RuntimeJdk = allowPre17RuntimeJdk
    arguments.allowReifiedTypeInCatch = allowReifiedTypeInCatch
    arguments.allowReturnsResultOf = allowReturnsResultOf
    arguments.annotationDefaultTarget = annotationDefaultTarget?.stringValue
    arguments.annotationTargetAll = annotationTargetAll
    arguments.callableReferencesToContextual = callableReferencesToContextual
    arguments.checkPhaseConditions = checkPhaseConditions
    arguments.collectionLiterals = collectionLiterals
    arguments.commonSources = commonSources ?: emptyArray()
    arguments.companionBlocks = companionBlocks
    arguments.companionBlocksAndExtensions = companionBlocksAndExtensions
    arguments.pluginConfigurations = pluginConfigurations ?: emptyArray()
    arguments.pluginOrderConstraints = pluginOrderConstraints ?: emptyArray()
    arguments.consistentDataClassCopyVisibility = consistentDataClassCopyVisibility
    arguments.contextParameters = contextParameters
    arguments.contextSensitiveResolution = contextSensitiveResolution
    arguments.dataFlowBasedExhaustiveness = dataFlowBasedExhaustiveness
    arguments.disableDefaultScriptingPlugin = disableDefaultScriptingPlugin
    arguments.disableIrCheckers = disableIrCheckers ?: emptyArray()
    arguments.disablePhases = disablePhases.toTypedArray()
    arguments.doNotNormalizeNanValuesInConstContext = doNotNormalizeNanValuesInConstContext
    arguments.dontSortSourceFiles = dontSortSourceFiles
    arguments.dontWarnOnErrorSuppression = dontWarnOnErrorSuppression
    arguments.eagerLambdaAnalysis = eagerLambdaAnalysis
    arguments.enableAdditionalIrCheckers = enableAdditionalIrCheckers ?: emptyArray()
    arguments.incrementalCompilation = incrementalCompilation
    arguments.escapingFunctions = escapingFunctions.toTypedArray()
    arguments.expectActualClasses = expectActualClasses
    arguments.explicitApi = explicitApi.stringValue
    arguments.explicitBackingFields = explicitBackingFields
    arguments.explicitContextArguments = explicitContextArguments
    arguments.firAggressivePruning = firAggressivePruning
    arguments.fragmentDependencies = fragmentDependencies ?: emptyArray()
    arguments.fragmentFriendDependencies = fragmentFriendDependencies ?: emptyArray()
    arguments.fragmentRefines = fragmentRefines ?: emptyArray()
    arguments.fragmentSources = fragmentSources ?: emptyArray()
    arguments.fragments = fragments ?: emptyArray()
    arguments.fullValueClasses = fullValueClasses
    arguments.headerMode = headerMode
    arguments.headerModeType = headerModeType.stringValue
    arguments.intrinsicConstEvaluation = intrinsicConstEvaluation
    arguments.localTypeAliases = localTypeAliases
    arguments.metadataKlib = metadataKlib
    arguments.metadataVersion = metadataVersion
    arguments.multiDollarInterpolation = multiDollarInterpolation
    arguments.multiPlatform = multiPlatform
    arguments.nameBasedDestructuring = nameBasedDestructuring?.stringValue
    arguments.nestedTypeAliases = nestedTypeAliases
    arguments.noInline = noInline
    arguments.nonLocalBreakContinue = nonLocalBreakContinue
    arguments.phasesToValidate = phasesToValidate.toTypedArray()
    arguments.phasesToValidateAfter = phasesToValidateAfter.toTypedArray()
    arguments.phasesToValidateBefore = phasesToValidateBefore.toTypedArray()
    arguments.pluginClasspaths = pluginClasspaths ?: emptyArray()
    arguments.renderInternalDiagnosticNames = renderInternalDiagnosticNames
    arguments.returnValueChecker = returnValueChecker.stringValue
    arguments.richErrors = richErrors
    arguments.separateKmpCompilationScheme = separateKmpCompilationScheme
    arguments.skipMetadataVersionCheck = skipMetadataVersionCheck
    arguments.skipPrereleaseCheck = skipPrereleaseCheck
    arguments.stdlibCompilation = stdlibCompilation
    arguments.strictEqualityForStructuralClasses = strictEqualityForStructuralClasses
    arguments.suppressVersionWarnings = suppressVersionWarnings
    arguments.useFirIC = useFirIC
    arguments.useFirLT = useFirLT
    arguments.verifyIr = verifyIr?.stringValue
    arguments.whenGuards = whenGuards
    arguments.apiVersion = apiVersion?.stringValue
    arguments.kotlinHome = kotlinHome?.absolutePathStringOrThrow()
    arguments.languageVersion = languageVersion?.stringValue
    arguments.optIn = optIn.toTypedArray()
    arguments.progressiveMode = progressiveMode
    arguments.script = script
    arguments.applyCompilerPlugins(compilerPlugins)
    arguments.applyWarningLevels(warningLevel)
    return arguments
  }

  @Suppress("DEPRECATION")
  internal override fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    super.collectRestrictedArgViolations(compilerArgs, defaultArgs)
    val args = compilerArgs as CommonCompilerArguments
    val castedDefaults = defaultArgs as CommonCompilerArguments
    if (args.incrementalCompilation != castedDefaults.incrementalCompilation) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-Xenable-incremental-compilation' is not supported in the Build Tools API. Configure it via the JvmCompilationOperation.INCREMENTAL_COMPILATION option instead."))
  }

  public companion object {
    private val knownOptionIds: List<String> =
        listOf("P","XX_LANGUAGE","XX_DEBUG_LEVEL_COMPILER_CHECKS","XX_DUMP_MODEL","XX_EXPLICIT_RETURN_TYPES","XX_LENIENT_MODE","X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS","X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS","X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS","X_ALLOW_HOLDSIN_CONTRACT","X_ALLOW_KOTLIN_PACKAGE","X_ALLOW_PRE_17_RUNTIME_JDK","X_ALLOW_REIFIED_TYPE_IN_CATCH","X_ALLOW_RETURNS_RESULT_OF","X_ANNOTATION_DEFAULT_TARGET","X_ANNOTATION_TARGET_ALL","X_CALLABLE_REFERENCES_TO_CONTEXTUAL","X_CHECK_PHASE_CONDITIONS","X_COLLECTION_LITERALS","X_COMMON_SOURCES","X_COMPANION_BLOCKS","X_COMPANION_BLOCKS_AND_EXTENSIONS","X_COMPILER_PLUGIN","X_COMPILER_PLUGIN_ORDER","X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY","X_CONTEXT_PARAMETERS","X_CONTEXT_SENSITIVE_RESOLUTION","X_DATA_FLOW_BASED_EXHAUSTIVENESS","X_DETAILED_PERF","X_DISABLE_DEFAULT_SCRIPTING_PLUGIN","X_DISABLE_IR_CHECKERS","X_DISABLE_PHASES","X_DO_NOT_NORMALIZE_NAN_VALUES_IN_CONST_CONTEXT","X_DONT_SORT_SOURCE_FILES","X_DONT_WARN_ON_ERROR_SUPPRESSION","X_DUMP_DIRECTORY","X_DUMP_FQNAME","X_DUMP_PERF","X_EAGER_LAMBDA_ANALYSIS","X_ENABLE_ADDITIONAL_IR_CHECKERS","X_ENABLE_INCREMENTAL_COMPILATION","X_ESCAPING_FUNCTIONS","X_EXPECT_ACTUAL_CLASSES","X_EXPLICIT_API","X_EXPLICIT_BACKING_FIELDS","X_EXPLICIT_CONTEXT_ARGUMENTS","X_FIR_AGGRESSIVE_PRUNING","X_FRAGMENT_DEPENDENCY","X_FRAGMENT_FRIEND_DEPENDENCY","X_FRAGMENT_REFINES","X_FRAGMENT_SOURCES","X_FRAGMENTS","X_FULL_VALUE_CLASSES","X_HEADER_MODE","X_HEADER_MODE_TYPE","X_INTRINSIC_CONST_EVALUATION","X_LIST_PHASES","X_LOCAL_TYPE_ALIASES","X_METADATA_KLIB","X_METADATA_VERSION","X_MULTI_DOLLAR_INTERPOLATION","X_MULTI_PLATFORM","X_NAME_BASED_DESTRUCTURING","X_NESTED_TYPE_ALIASES","X_NO_INLINE","X_NON_LOCAL_BREAK_CONTINUE","X_PHASES_TO_DUMP","X_PHASES_TO_DUMP_AFTER","X_PHASES_TO_DUMP_BEFORE","X_PHASES_TO_VALIDATE","X_PHASES_TO_VALIDATE_AFTER","X_PHASES_TO_VALIDATE_BEFORE","X_PLUGIN","X_PRINT_CONFIGURATION","X_PROFILE_PHASES","X_RENDER_INTERNAL_DIAGNOSTIC_NAMES","X_REPORT_ALL_WARNINGS","X_REPORT_OUTPUT_FILES","X_REPORT_PERF","X_RETURN_VALUE_CHECKER","X_RICH_ERRORS","X_SEPARATE_KMP_COMPILATION","X_SKIP_METADATA_VERSION_CHECK","X_SKIP_PRERELEASE_CHECK","X_STDLIB_COMPILATION","X_STRICT_EQUALITY_FOR_STRUCTURAL_CLASSES","X_SUPPRESS_VERSION_WARNINGS","X_USE_FIR_IC","X_USE_FIR_LT","X_VERBOSE_PHASES","X_VERIFY_IR","X_WHEN_GUARDS","API_VERSION","KOTLIN_HOME","LANGUAGE_VERSION","OPT_IN","PROGRESSIVE","SCRIPT","COMPILER_PLUGINS","X_WARNING_LEVEL",)
  }
}
