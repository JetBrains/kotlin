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
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
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
import org.jetbrains.kotlin.buildtools.`internal`.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.`internal`.serializability.setPropertyWithSerialNameValue
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
  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V = CommonCompilerArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)

  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V) {
    CommonCompilerArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
  }

  public operator fun contains(key: CommonCompilerArgument<*>): Boolean = true

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V = try { CommonCompilerArgumentValueAdapter.toApi(CommonCompilerArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { CommonCompilerArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, CommonCompilerArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<*>): Boolean = key.id in optionsMap

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
    try { pluginOptions = arguments.pluginOptions } catch (_: NoSuchMethodError) {  }
    try { manuallyConfiguredFeatures = arguments.manuallyConfiguredFeatures } catch (_: NoSuchMethodError) {  }
    try { debugLevelCompilerChecks = arguments.debugLevelCompilerChecks } catch (_: NoSuchMethodError) {  }
    try { dumpArgumentsDir = arguments.dumpArgumentsDir } catch (_: NoSuchMethodError) {  }
    try { explicitReturnTypes = arguments.explicitReturnTypes.let { ExplicitApiMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::explicitReturnTypes, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { lenientMode = arguments.lenientMode } catch (_: NoSuchMethodError) {  }
    try { allowAnyScriptsInSourceRoots = arguments.allowAnyScriptsInSourceRoots } catch (_: NoSuchMethodError) {  }
    try { allowConditionImpliesReturnsContracts = arguments.allowConditionImpliesReturnsContracts } catch (_: NoSuchMethodError) {  }
    try { allowContractsOnMoreFunctions = arguments.allowContractsOnMoreFunctions } catch (_: NoSuchMethodError) {  }
    try { allowHoldsinContract = arguments.allowHoldsinContract } catch (_: NoSuchMethodError) {  }
    try { allowKotlinPackage = arguments.allowKotlinPackage } catch (_: NoSuchMethodError) {  }
    try { allowPre17RuntimeJdk = arguments.allowPre17RuntimeJdk } catch (_: NoSuchMethodError) {  }
    try { allowReifiedTypeInCatch = arguments.allowReifiedTypeInCatch } catch (_: NoSuchMethodError) {  }
    try { allowReturnsResultOf = arguments.allowReturnsResultOf } catch (_: NoSuchMethodError) {  }
    try { annotationDefaultTarget = arguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::annotationDefaultTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { annotationTargetAll = arguments.annotationTargetAll } catch (_: NoSuchMethodError) {  }
    try { callableReferencesToContextual = arguments.callableReferencesToContextual } catch (_: NoSuchMethodError) {  }
    try { checkPhaseConditions = arguments.checkPhaseConditions } catch (_: NoSuchMethodError) {  }
    try { collectionLiterals = arguments.collectionLiterals } catch (_: NoSuchMethodError) {  }
    try { commonSources = arguments.commonSources } catch (_: NoSuchMethodError) {  }
    try { companionBlocks = arguments.companionBlocks } catch (_: NoSuchMethodError) {  }
    try { companionBlocksAndExtensions = arguments.companionBlocksAndExtensions } catch (_: NoSuchMethodError) {  }
    try { pluginConfigurations = arguments.pluginConfigurations } catch (_: NoSuchMethodError) {  }
    try { pluginOrderConstraints = arguments.pluginOrderConstraints } catch (_: NoSuchMethodError) {  }
    try { consistentDataClassCopyVisibility = arguments.consistentDataClassCopyVisibility } catch (_: NoSuchMethodError) {  }
    try { contextParameters = arguments.contextParameters } catch (_: NoSuchMethodError) {  }
    try { contextSensitiveResolution = arguments.contextSensitiveResolution } catch (_: NoSuchMethodError) {  }
    try { dataFlowBasedExhaustiveness = arguments.dataFlowBasedExhaustiveness } catch (_: NoSuchMethodError) {  }
    try { detailedPerf = arguments.detailedPerf } catch (_: NoSuchMethodError) {  }
    try { disableDefaultScriptingPlugin = arguments.disableDefaultScriptingPlugin } catch (_: NoSuchMethodError) {  }
    try { disableIrCheckers = arguments.disableIrCheckers } catch (_: NoSuchMethodError) {  }
    try { disablePhases = arguments.disablePhases.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { doNotNormalizeNanValuesInConstContext = arguments.doNotNormalizeNanValuesInConstContext } catch (_: NoSuchMethodError) {  }
    try { dontSortSourceFiles = arguments.dontSortSourceFiles } catch (_: NoSuchMethodError) {  }
    try { dontWarnOnErrorSuppression = arguments.dontWarnOnErrorSuppression } catch (_: NoSuchMethodError) {  }
    try { dumpDirectory = arguments.dumpDirectory?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { dumpOnlyFqName = arguments.dumpOnlyFqName } catch (_: NoSuchMethodError) {  }
    try { dumpPerf = arguments.dumpPerf?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { eagerLambdaAnalysis = arguments.eagerLambdaAnalysis } catch (_: NoSuchMethodError) {  }
    try { enableAdditionalIrCheckers = arguments.enableAdditionalIrCheckers } catch (_: NoSuchMethodError) {  }
    try { incrementalCompilation = arguments.incrementalCompilation } catch (_: NoSuchMethodError) {  }
    try { escapingFunctions = arguments.escapingFunctions.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { expectActualClasses = arguments.expectActualClasses } catch (_: NoSuchMethodError) {  }
    try { explicitApi = arguments.explicitApi.let { ExplicitApiMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::explicitApi, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { explicitBackingFields = arguments.explicitBackingFields } catch (_: NoSuchMethodError) {  }
    try { explicitContextArguments = arguments.explicitContextArguments } catch (_: NoSuchMethodError) {  }
    try { firAggressivePruning = arguments.firAggressivePruning } catch (_: NoSuchMethodError) {  }
    try { fragmentDependencies = arguments.fragmentDependencies } catch (_: NoSuchMethodError) {  }
    try { fragmentFriendDependencies = arguments.fragmentFriendDependencies } catch (_: NoSuchMethodError) {  }
    try { fragmentRefines = arguments.fragmentRefines } catch (_: NoSuchMethodError) {  }
    try { fragmentSources = arguments.fragmentSources } catch (_: NoSuchMethodError) {  }
    try { fragments = arguments.fragments } catch (_: NoSuchMethodError) {  }
    try { fullValueClasses = arguments.fullValueClasses } catch (_: NoSuchMethodError) {  }
    try { headerMode = arguments.headerMode } catch (_: NoSuchMethodError) {  }
    try { headerModeType = arguments.headerModeType.let { HeaderMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::headerModeType, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xheader-mode-type value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { intrinsicConstEvaluation = arguments.intrinsicConstEvaluation } catch (_: NoSuchMethodError) {  }
    try { listPhases = arguments.listPhases } catch (_: NoSuchMethodError) {  }
    try { localTypeAliases = arguments.localTypeAliases } catch (_: NoSuchMethodError) {  }
    try { metadataKlib = arguments.metadataKlib } catch (_: NoSuchMethodError) {  }
    try { metadataVersion = arguments.metadataVersion } catch (_: NoSuchMethodError) {  }
    try { multiDollarInterpolation = arguments.multiDollarInterpolation } catch (_: NoSuchMethodError) {  }
    try { multiPlatform = arguments.multiPlatform } catch (_: NoSuchMethodError) {  }
    try { nameBasedDestructuring = arguments.nameBasedDestructuring?.let { NameBasedDestructuringMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::nameBasedDestructuring, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xname-based-destructuring value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { nestedTypeAliases = arguments.nestedTypeAliases } catch (_: NoSuchMethodError) {  }
    try { noInline = arguments.noInline } catch (_: NoSuchMethodError) {  }
    try { nonLocalBreakContinue = arguments.nonLocalBreakContinue } catch (_: NoSuchMethodError) {  }
    try { phasesToDump = arguments.phasesToDump.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { phasesToDumpAfter = arguments.phasesToDumpAfter.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { phasesToDumpBefore = arguments.phasesToDumpBefore.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { phasesToValidate = arguments.phasesToValidate.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { phasesToValidateAfter = arguments.phasesToValidateAfter.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { phasesToValidateBefore = arguments.phasesToValidateBefore.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { pluginClasspaths = arguments.pluginClasspaths } catch (_: NoSuchMethodError) {  }
    try { printConfiguration = arguments.printConfiguration } catch (_: NoSuchMethodError) {  }
    try { profilePhases = arguments.profilePhases } catch (_: NoSuchMethodError) {  }
    try { renderInternalDiagnosticNames = arguments.renderInternalDiagnosticNames } catch (_: NoSuchMethodError) {  }
    try { reportAllWarnings = arguments.reportAllWarnings } catch (_: NoSuchMethodError) {  }
    try { reportOutputFiles = arguments.reportOutputFiles } catch (_: NoSuchMethodError) {  }
    try { reportPerf = arguments.reportPerf } catch (_: NoSuchMethodError) {  }
    try { returnValueChecker = arguments.returnValueChecker.let { ReturnValueCheckerMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::returnValueChecker, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { separateKmpCompilationScheme = arguments.separateKmpCompilationScheme } catch (_: NoSuchMethodError) {  }
    try { skipMetadataVersionCheck = arguments.skipMetadataVersionCheck } catch (_: NoSuchMethodError) {  }
    try { skipPrereleaseCheck = arguments.skipPrereleaseCheck } catch (_: NoSuchMethodError) {  }
    try { stdlibCompilation = arguments.stdlibCompilation } catch (_: NoSuchMethodError) {  }
    try { strictEqualityForStructuralClasses = arguments.strictEqualityForStructuralClasses } catch (_: NoSuchMethodError) {  }
    try { suppressVersionWarnings = arguments.suppressVersionWarnings } catch (_: NoSuchMethodError) {  }
    try { useFirIC = arguments.useFirIC } catch (_: NoSuchMethodError) {  }
    try { useFirLT = arguments.useFirLT } catch (_: NoSuchMethodError) {  }
    try { verbosePhases = arguments.verbosePhases.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { verifyIr = arguments.verifyIr?.let { VerifyIrMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::verifyIr, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { whenGuards = arguments.whenGuards } catch (_: NoSuchMethodError) {  }
    try { apiVersion = arguments.apiVersion?.let { KotlinVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::apiVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { kotlinHome = arguments.kotlinHome?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { languageVersion = arguments.languageVersion?.let { KotlinVersion.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::languageVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { optIn = arguments.optIn.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { progressiveMode = arguments.progressiveMode } catch (_: NoSuchMethodError) {  }
    try { script = arguments.script } catch (_: NoSuchMethodError) {  }
    try { compilerPlugins = applyCompilerPlugins(compilerPlugins, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { warningLevel = applyWarningLevels(warningLevel, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
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

    public val X_ALLOW_PRE_17_RUNTIME_JDK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_PRE_17_RUNTIME_JDK")

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

    public val X_CONTEXT_SENSITIVE_RESOLUTION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_SENSITIVE_RESOLUTION")

    public val X_DATA_FLOW_BASED_EXHAUSTIVENESS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DATA_FLOW_BASED_EXHAUSTIVENESS")

    public val X_DETAILED_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DETAILED_PERF")

    public val X_DISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")

    public val X_DISABLE_IR_CHECKERS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_DISABLE_IR_CHECKERS")

    public val X_DISABLE_PHASES: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_DISABLE_PHASES")

    public val X_DO_NOT_NORMALIZE_NAN_VALUES_IN_CONST_CONTEXT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DO_NOT_NORMALIZE_NAN_VALUES_IN_CONST_CONTEXT")

    public val X_DONT_SORT_SOURCE_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_SORT_SOURCE_FILES")

    public val X_DONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_WARN_ON_ERROR_SUPPRESSION")

    public val X_DUMP_DIRECTORY: CommonCompilerArgument<Path?> =
        CommonCompilerArgument("X_DUMP_DIRECTORY")

    public val X_DUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_FQNAME")

    public val X_DUMP_PERF: CommonCompilerArgument<Path?> = CommonCompilerArgument("X_DUMP_PERF")

    public val X_EAGER_LAMBDA_ANALYSIS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EAGER_LAMBDA_ANALYSIS")

    public val X_ENABLE_ADDITIONAL_IR_CHECKERS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_ENABLE_ADDITIONAL_IR_CHECKERS")

    public val X_ENABLE_INCREMENTAL_COMPILATION: CommonCompilerArgument<Boolean?> =
        CommonCompilerArgument("X_ENABLE_INCREMENTAL_COMPILATION")

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

    public val X_FULL_VALUE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_FULL_VALUE_CLASSES")

    public val X_HEADER_MODE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_HEADER_MODE")

    public val X_HEADER_MODE_TYPE: CommonCompilerArgument<HeaderMode> =
        CommonCompilerArgument("X_HEADER_MODE_TYPE")

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

    public val X_STRICT_EQUALITY_FOR_STRUCTURAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_STRICT_EQUALITY_FOR_STRUCTURAL_CLASSES")

    public val X_SUPPRESS_VERSION_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_VERSION_WARNINGS")

    public val X_USE_FIR_IC: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_IC")

    public val X_USE_FIR_LT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_LT")

    public val X_VERBOSE_PHASES: CommonCompilerArgument<List<String>> =
        CommonCompilerArgument("X_VERBOSE_PHASES")

    public val X_VERIFY_IR: CommonCompilerArgument<VerifyIrMode?> =
        CommonCompilerArgument("X_VERIFY_IR")

    public val X_WHEN_GUARDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_WHEN_GUARDS")

    public val API_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("API_VERSION")

    public val KOTLIN_HOME: CommonCompilerArgument<Path?> = CommonCompilerArgument("KOTLIN_HOME")

    public val LANGUAGE_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("LANGUAGE_VERSION")

    public val OPT_IN: CommonCompilerArgument<List<String>> = CommonCompilerArgument("OPT_IN")

    public val PROGRESSIVE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("PROGRESSIVE")

    public val SCRIPT: CommonCompilerArgument<Boolean> = CommonCompilerArgument("SCRIPT")

    public val COMPILER_PLUGINS: CommonCompilerArgument<List<CompilerPluginImpl>> =
        CommonCompilerArgument("COMPILER_PLUGINS")

    public val X_WARNING_LEVEL: CommonCompilerArgument<List<WarningLevelImpl>> =
        CommonCompilerArgument("X_WARNING_LEVEL")
  }
}
