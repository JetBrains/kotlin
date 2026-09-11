// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

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
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.CommonCompilerArgumentsImpl.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.HeaderMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonCompilerArgumentsImpl(
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonToolArgumentsImpl(argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    ArgumentsCommonCompilerArguments,
    ArgumentsCommonCompilerArguments.Builder {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("P")
  protected var P: Array<String>?

  @SerialName("XX_LANGUAGE")
  protected var XXLanguage: Array<String>?

  @SerialName("XX_DEBUG_LEVEL_COMPILER_CHECKS")
  protected var `XXdebug-level-compiler-checks`: Boolean

  @SerialName("XX_DUMP_MODEL")
  protected var `XXdump-model`: String?

  @SerialName("XX_EXPLICIT_RETURN_TYPES")
  protected var `XXexplicit-return-types`: ExplicitApiMode

  @SerialName("XX_LENIENT_MODE")
  protected var `XXlenient-mode`: Boolean

  @SerialName("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")
  protected var `Xallow-any-scripts-in-source-roots`: Boolean

  @SerialName("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS")
  protected var `Xallow-condition-implies-returns-contracts`: Boolean

  @SerialName("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS")
  protected var `Xallow-contracts-on-more-functions`: Boolean

  @SerialName("X_ALLOW_HOLDSIN_CONTRACT")
  protected var `Xallow-holdsin-contract`: Boolean

  @SerialName("X_ALLOW_KOTLIN_PACKAGE")
  protected var `Xallow-kotlin-package`: Boolean

  @SerialName("X_ALLOW_PRE_17_RUNTIME_JDK")
  protected var `Xallow-pre-17-runtime-jdk`: Boolean

  @SerialName("X_ALLOW_REIFIED_TYPE_IN_CATCH")
  protected var `Xallow-reified-type-in-catch`: Boolean

  @SerialName("X_ALLOW_RETURNS_RESULT_OF")
  protected var `Xallow-returns-result-of`: Boolean

  @SerialName("X_ANNOTATION_DEFAULT_TARGET")
  protected var `Xannotation-default-target`: AnnotationDefaultTargetMode?

  @SerialName("X_ANNOTATION_TARGET_ALL")
  protected var `Xannotation-target-all`: Boolean

  @SerialName("X_CALLABLE_REFERENCES_TO_CONTEXTUAL")
  protected var `Xcallable-references-to-contextual`: Boolean

  @SerialName("X_CHECK_PHASE_CONDITIONS")
  protected var `Xcheck-phase-conditions`: Boolean

  @SerialName("X_COLLECTION_LITERALS")
  protected var `Xcollection-literals`: Boolean

  @SerialName("X_COMMON_SOURCES")
  protected var `Xcommon-sources`: Array<String>?

  @SerialName("X_COMPANION_BLOCKS")
  protected var `Xcompanion-blocks`: Boolean

  @SerialName("X_COMPANION_BLOCKS_AND_EXTENSIONS")
  protected var `Xcompanion-blocks-and-extensions`: Boolean

  @SerialName("X_COMPILER_PLUGIN")
  protected var `Xcompiler-plugin`: Array<String>?

  @SerialName("X_COMPILER_PLUGIN_ORDER")
  protected var `Xcompiler-plugin-order`: Array<String>?

  @SerialName("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY")
  protected var `Xconsistent-data-class-copy-visibility`: Boolean

  @SerialName("X_CONTEXT_PARAMETERS")
  protected var `Xcontext-parameters`: Boolean

  @SerialName("X_CONTEXT_RECEIVERS")
  protected var `Xcontext-receivers`: Boolean

  @SerialName("X_CONTEXT_SENSITIVE_RESOLUTION")
  protected var `Xcontext-sensitive-resolution`: Boolean

  @SerialName("X_DATA_FLOW_BASED_EXHAUSTIVENESS")
  protected var `Xdata-flow-based-exhaustiveness`: Boolean

  @SerialName("X_DETAILED_PERF")
  protected var `Xdetailed-perf`: Boolean

  @SerialName("X_DIRECT_JAVA_ACTUALIZATION")
  protected var `Xdirect-java-actualization`: Boolean

  @SerialName("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")
  protected var `Xdisable-default-scripting-plugin`: Boolean

  @SerialName("X_DISABLE_IR_CHECKERS")
  protected var `Xdisable-ir-checkers`: Array<String>?

  @SerialName("X_DISABLE_PHASES")
  protected var `Xdisable-phases`: List<String>

  @SerialName("X_DONT_SORT_SOURCE_FILES")
  protected var `Xdont-sort-source-files`: Boolean

  @SerialName("X_DONT_WARN_ON_ERROR_SUPPRESSION")
  protected var `Xdont-warn-on-error-suppression`: Boolean

  @SerialName("X_DUMP_DIRECTORY")
  protected var `Xdump-directory`: Path?

  @SerialName("X_DUMP_FQNAME")
  protected var `Xdump-fqname`: String?

  @SerialName("X_DUMP_PERF")
  protected var `Xdump-perf`: Path?

  @SerialName("X_EAGER_LAMBDA_ANALYSIS")
  protected var `Xeager-lambda-analysis`: Boolean

  @SerialName("X_ENABLE_ADDITIONAL_IR_CHECKERS")
  protected var `Xenable-additional-ir-checkers`: Array<String>?

  @SerialName("X_ENABLE_INCREMENTAL_COMPILATION")
  protected var `Xenable-incremental-compilation`: Boolean?

  @SerialName("X_ESCAPING_FUNCTIONS")
  protected var `Xescaping-functions`: List<String>

  @SerialName("X_EXPECT_ACTUAL_CLASSES")
  protected var `Xexpect-actual-classes`: Boolean

  @SerialName("X_EXPLICIT_API")
  protected var `Xexplicit-api`: ExplicitApiMode

  @SerialName("X_EXPLICIT_BACKING_FIELDS")
  protected var `Xexplicit-backing-fields`: Boolean

  @SerialName("X_EXPLICIT_CONTEXT_ARGUMENTS")
  protected var `Xexplicit-context-arguments`: Boolean

  @SerialName("X_FIR_AGGRESSIVE_PRUNING")
  protected var `Xfir-aggressive-pruning`: Boolean?

  @SerialName("X_FRAGMENT_DEPENDENCY")
  protected var `Xfragment-dependency`: Array<String>?

  @SerialName("X_FRAGMENT_FRIEND_DEPENDENCY")
  protected var `Xfragment-friend-dependency`: Array<String>?

  @SerialName("X_FRAGMENT_REFINES")
  protected var `Xfragment-refines`: Array<String>?

  @SerialName("X_FRAGMENT_SOURCES")
  protected var `Xfragment-sources`: Array<String>?

  @SerialName("X_FRAGMENTS")
  protected var Xfragments: Array<String>?

  @SerialName("X_HEADER_MODE")
  protected var `Xheader-mode`: Boolean

  @SerialName("X_HEADER_MODE_TYPE")
  protected var `Xheader-mode-type`: HeaderMode

  @SerialName("X_IGNORE_CONST_OPTIMIZATION_ERRORS")
  protected var `Xignore-const-optimization-errors`: Boolean

  @SerialName("X_INLINE_CLASSES")
  protected var `Xinline-classes`: Boolean

  @SerialName("X_INTELLIJ_PLUGIN_ROOT")
  protected var `Xintellij-plugin-root`: String?

  @SerialName("X_INTRINSIC_CONST_EVALUATION")
  protected var `Xintrinsic-const-evaluation`: Boolean

  @SerialName("X_LIST_PHASES")
  protected var `Xlist-phases`: Boolean

  @SerialName("X_LOCAL_TYPE_ALIASES")
  protected var `Xlocal-type-aliases`: Boolean

  @SerialName("X_METADATA_KLIB")
  protected var `Xmetadata-klib`: Boolean

  @SerialName("X_METADATA_VERSION")
  protected var `Xmetadata-version`: String?

  @SerialName("X_MULTI_DOLLAR_INTERPOLATION")
  protected var `Xmulti-dollar-interpolation`: Boolean

  @SerialName("X_MULTI_PLATFORM")
  protected var `Xmulti-platform`: Boolean

  @SerialName("X_NAME_BASED_DESTRUCTURING")
  protected var `Xname-based-destructuring`: NameBasedDestructuringMode?

  @SerialName("X_NESTED_TYPE_ALIASES")
  protected var `Xnested-type-aliases`: Boolean

  @SerialName("X_NEW_INFERENCE")
  protected var `Xnew-inference`: Boolean

  @SerialName("X_NO_CHECK_ACTUAL")
  protected var `Xno-check-actual`: Boolean

  @SerialName("X_NO_INLINE")
  protected var `Xno-inline`: Boolean

  @SerialName("X_NON_LOCAL_BREAK_CONTINUE")
  protected var `Xnon-local-break-continue`: Boolean

  @SerialName("X_PHASES_TO_DUMP")
  protected var `Xphases-to-dump`: List<String>

  @SerialName("X_PHASES_TO_DUMP_AFTER")
  protected var `Xphases-to-dump-after`: List<String>

  @SerialName("X_PHASES_TO_DUMP_BEFORE")
  protected var `Xphases-to-dump-before`: List<String>

  @SerialName("X_PHASES_TO_VALIDATE")
  protected var `Xphases-to-validate`: List<String>

  @SerialName("X_PHASES_TO_VALIDATE_AFTER")
  protected var `Xphases-to-validate-after`: List<String>

  @SerialName("X_PHASES_TO_VALIDATE_BEFORE")
  protected var `Xphases-to-validate-before`: List<String>

  @SerialName("X_PLUGIN")
  protected var Xplugin: Array<String>?

  @SerialName("X_PRINT_CONFIGURATION")
  protected var `Xprint-configuration`: Boolean

  @SerialName("X_PROFILE_PHASES")
  protected var `Xprofile-phases`: Boolean

  @SerialName("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES")
  protected var `Xrender-internal-diagnostic-names`: Boolean

  @SerialName("X_REPL")
  protected var Xrepl: Boolean

  @SerialName("X_REPORT_ALL_WARNINGS")
  protected var `Xreport-all-warnings`: Boolean

  @SerialName("X_REPORT_OUTPUT_FILES")
  protected var `Xreport-output-files`: Boolean

  @SerialName("X_REPORT_PERF")
  protected var `Xreport-perf`: Boolean

  @SerialName("X_RETURN_VALUE_CHECKER")
  protected var `Xreturn-value-checker`: ReturnValueCheckerMode

  @SerialName("X_SEPARATE_KMP_COMPILATION")
  protected var `Xseparate-kmp-compilation`: Boolean

  @SerialName("X_SKIP_METADATA_VERSION_CHECK")
  protected var `Xskip-metadata-version-check`: Boolean

  @SerialName("X_SKIP_PRERELEASE_CHECK")
  protected var `Xskip-prerelease-check`: Boolean

  @SerialName("X_STDLIB_COMPILATION")
  protected var `Xstdlib-compilation`: Boolean

  @SerialName("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR")
  protected var `Xsuppress-api-version-greater-than-language-version-error`: Boolean

  @SerialName("X_SUPPRESS_VERSION_WARNINGS")
  protected var `Xsuppress-version-warnings`: Boolean

  @SerialName("X_SUPPRESS_WARNING")
  protected var `Xsuppress-warning`: List<String>

  @SerialName("X_UNRESTRICTED_BUILDER_INFERENCE")
  protected var `Xunrestricted-builder-inference`: Boolean

  @SerialName("X_USE_FIR_EXPERIMENTAL_CHECKERS")
  protected var `Xuse-fir-experimental-checkers`: Boolean

  @SerialName("X_USE_FIR_IC")
  protected var `Xuse-fir-ic`: Boolean

  @SerialName("X_USE_FIR_LT")
  protected var `Xuse-fir-lt`: Boolean

  @SerialName("X_VERBOSE_PHASES")
  protected var `Xverbose-phases`: List<String>

  @SerialName("X_VERIFY_IR")
  protected var `Xverify-ir`: VerifyIrMode?

  @SerialName("X_VERIFY_IR_NESTED_OFFSETS")
  protected var `Xverify-ir-nested-offsets`: Boolean

  @SerialName("X_VERIFY_IR_VISIBILITY")
  protected var `Xverify-ir-visibility`: Boolean

  @SerialName("X_WHEN_GUARDS")
  protected var `Xwhen-guards`: Boolean

  @SerialName("API_VERSION")
  protected var `api-version`: KotlinVersion?

  @SerialName("KOTLIN_HOME")
  protected var `kotlin-home`: Path?

  @SerialName("LANGUAGE_VERSION")
  protected var `language-version`: KotlinVersion?

  @SerialName("OPT_IN")
  protected var `opt-in`: List<String>

  @SerialName("PROGRESSIVE")
  protected var progressive: Boolean

  @SerialName("SCRIPT")
  protected var script: Boolean

  @SerialName("COMPILER_PLUGINS")
  protected var `compiler-plugins`: List<CompilerPlugin>

  @SerialName("X_WARNING_LEVEL")
  protected var `Xwarning-level`: List<WarningLevel>

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: CommonCompilerArgument<*>): Boolean = key.id in optionsMap

  private operator fun `get`(key: String): Any? = CommonCompilerArgumentValueAdapter.toApi(optionsMap[key])

  private operator fun `set`(key: String, `value`: Any?) {
    optionsMap[key] = CommonCompilerArgumentValueAdapter.toImpl(`value`)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
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
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.pluginOptions = P ?: emptyArray()
    arguments.manuallyConfiguredFeatures = XXLanguage ?: emptyArray()
    arguments.debugLevelCompilerChecks = `XXdebug-level-compiler-checks`
    arguments.dumpArgumentsDir = `XXdump-model`
    arguments.explicitReturnTypes = `XXexplicit-return-types`.stringValue
    arguments.lenientMode = `XXlenient-mode`
    arguments.allowAnyScriptsInSourceRoots = `Xallow-any-scripts-in-source-roots`
    arguments.allowConditionImpliesReturnsContracts = `Xallow-condition-implies-returns-contracts`
    arguments.allowContractsOnMoreFunctions = `Xallow-contracts-on-more-functions`
    arguments.allowHoldsinContract = `Xallow-holdsin-contract`
    arguments.allowKotlinPackage = `Xallow-kotlin-package`
    arguments.allowPre17RuntimeJdk = `Xallow-pre-17-runtime-jdk`
    arguments.allowReifiedTypeInCatch = `Xallow-reified-type-in-catch`
    arguments.allowReturnsResultOf = `Xallow-returns-result-of`
    arguments.annotationDefaultTarget = `Xannotation-default-target`?.stringValue
    arguments.annotationTargetAll = `Xannotation-target-all`
    arguments.callableReferencesToContextual = `Xcallable-references-to-contextual`
    arguments.checkPhaseConditions = `Xcheck-phase-conditions`
    arguments.collectionLiterals = `Xcollection-literals`
    arguments.commonSources = `Xcommon-sources` ?: emptyArray()
    arguments.companionBlocks = `Xcompanion-blocks`
    arguments.companionBlocksAndExtensions = `Xcompanion-blocks-and-extensions`
    arguments.pluginConfigurations = `Xcompiler-plugin` ?: emptyArray()
    arguments.pluginOrderConstraints = `Xcompiler-plugin-order` ?: emptyArray()
    arguments.consistentDataClassCopyVisibility = `Xconsistent-data-class-copy-visibility`
    arguments.contextParameters = `Xcontext-parameters`
    try { arguments.setUsingReflection("contextReceivers", `Xcontext-receivers`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.contextSensitiveResolution = `Xcontext-sensitive-resolution`
    arguments.dataFlowBasedExhaustiveness = `Xdata-flow-based-exhaustiveness`
    arguments.detailedPerf = `Xdetailed-perf`
    try { arguments.setUsingReflection("directJavaActualization", `Xdirect-java-actualization`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DIRECT_JAVA_ACTUALIZATION. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.disableDefaultScriptingPlugin = `Xdisable-default-scripting-plugin`
    arguments.disableIrCheckers = `Xdisable-ir-checkers` ?: emptyArray()
    arguments.disablePhases = `Xdisable-phases`.toTypedArray()
    arguments.dontSortSourceFiles = `Xdont-sort-source-files`
    arguments.dontWarnOnErrorSuppression = `Xdont-warn-on-error-suppression`
    arguments.dumpDirectory = `Xdump-directory`?.absolutePathStringOrThrow()
    arguments.dumpOnlyFqName = `Xdump-fqname`
    arguments.dumpPerf = `Xdump-perf`?.absolutePathStringOrThrow()
    arguments.eagerLambdaAnalysis = `Xeager-lambda-analysis`
    arguments.enableAdditionalIrCheckers = `Xenable-additional-ir-checkers` ?: emptyArray()
    arguments.incrementalCompilation = `Xenable-incremental-compilation`
    arguments.escapingFunctions = `Xescaping-functions`.toTypedArray()
    arguments.expectActualClasses = `Xexpect-actual-classes`
    arguments.explicitApi = `Xexplicit-api`.stringValue
    arguments.explicitBackingFields = `Xexplicit-backing-fields`
    arguments.explicitContextArguments = `Xexplicit-context-arguments`
    arguments.firAggressivePruning = `Xfir-aggressive-pruning`
    arguments.fragmentDependencies = `Xfragment-dependency` ?: emptyArray()
    arguments.fragmentFriendDependencies = `Xfragment-friend-dependency` ?: emptyArray()
    arguments.fragmentRefines = `Xfragment-refines` ?: emptyArray()
    arguments.fragmentSources = `Xfragment-sources` ?: emptyArray()
    arguments.fragments = Xfragments ?: emptyArray()
    arguments.headerMode = `Xheader-mode`
    arguments.headerModeType = `Xheader-mode-type`.stringValue
    try { arguments.setUsingReflection("ignoreConstOptimizationErrors", `Xignore-const-optimization-errors`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IGNORE_CONST_OPTIMIZATION_ERRORS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("inlineClasses", `Xinline-classes`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INLINE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("intellijPluginRoot", `Xintellij-plugin-root`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INTELLIJ_PLUGIN_ROOT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.intrinsicConstEvaluation = `Xintrinsic-const-evaluation`
    arguments.listPhases = `Xlist-phases`
    arguments.localTypeAliases = `Xlocal-type-aliases`
    arguments.metadataKlib = `Xmetadata-klib`
    arguments.metadataVersion = `Xmetadata-version`
    arguments.multiDollarInterpolation = `Xmulti-dollar-interpolation`
    arguments.multiPlatform = `Xmulti-platform`
    arguments.nameBasedDestructuring = `Xname-based-destructuring`?.stringValue
    arguments.nestedTypeAliases = `Xnested-type-aliases`
    try { arguments.setUsingReflection("newInference", `Xnew-inference`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NEW_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("noCheckActual", `Xno-check-actual`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NO_CHECK_ACTUAL. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.noInline = `Xno-inline`
    arguments.nonLocalBreakContinue = `Xnon-local-break-continue`
    arguments.phasesToDump = `Xphases-to-dump`.toTypedArray()
    arguments.phasesToDumpAfter = `Xphases-to-dump-after`.toTypedArray()
    arguments.phasesToDumpBefore = `Xphases-to-dump-before`.toTypedArray()
    arguments.phasesToValidate = `Xphases-to-validate`.toTypedArray()
    arguments.phasesToValidateAfter = `Xphases-to-validate-after`.toTypedArray()
    arguments.phasesToValidateBefore = `Xphases-to-validate-before`.toTypedArray()
    arguments.pluginClasspaths = Xplugin ?: emptyArray()
    arguments.printConfiguration = `Xprint-configuration`
    arguments.profilePhases = `Xprofile-phases`
    arguments.renderInternalDiagnosticNames = `Xrender-internal-diagnostic-names`
    arguments.repl = Xrepl
    arguments.reportAllWarnings = `Xreport-all-warnings`
    arguments.reportOutputFiles = `Xreport-output-files`
    arguments.reportPerf = `Xreport-perf`
    arguments.returnValueChecker = `Xreturn-value-checker`.stringValue
    arguments.separateKmpCompilationScheme = `Xseparate-kmp-compilation`
    arguments.skipMetadataVersionCheck = `Xskip-metadata-version-check`
    arguments.skipPrereleaseCheck = `Xskip-prerelease-check`
    arguments.stdlibCompilation = `Xstdlib-compilation`
    try { arguments.setUsingReflection("suppressApiVersionGreaterThanLanguageVersionError", `Xsuppress-api-version-greater-than-language-version-error`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.suppressVersionWarnings = `Xsuppress-version-warnings`
    try { arguments.setUsingReflection("suppressedDiagnostics", `Xsuppress-warning`.toTypedArray()) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_WARNING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("unrestrictedBuilderInference", `Xunrestricted-builder-inference`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_UNRESTRICTED_BUILDER_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("useFirExperimentalCheckers", `Xuse-fir-experimental-checkers`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_FIR_EXPERIMENTAL_CHECKERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.useFirIC = `Xuse-fir-ic`
    arguments.useFirLT = `Xuse-fir-lt`
    arguments.verbosePhases = `Xverbose-phases`.toTypedArray()
    arguments.verifyIr = `Xverify-ir`?.stringValue
    try { arguments.setUsingReflection("verifyIrNestedOffsets", `Xverify-ir-nested-offsets`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_NESTED_OFFSETS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.3.20 and removed in 2.4.20""").initCause(e) }
    try { arguments.setUsingReflection("verifyIrVisibility", `Xverify-ir-visibility`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    arguments.whenGuards = `Xwhen-guards`
    arguments.apiVersion = `api-version`?.stringValue
    arguments.kotlinHome = `kotlin-home`?.absolutePathStringOrThrow()
    arguments.languageVersion = `language-version`?.stringValue
    arguments.optIn = `opt-in`.toTypedArray()
    arguments.progressiveMode = progressive
    arguments.script = script
    if (COMPILER_PLUGINS in this) { arguments.applyCompilerPlugins(get(COMPILER_PLUGINS))}
    if (X_WARNING_LEVEL in this) { arguments.applyWarningLevels(get(X_WARNING_LEVEL))}
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { P = arguments.pluginOptions } catch (_: NoSuchMethodError) {  }
    try { XXLanguage = arguments.manuallyConfiguredFeatures } catch (_: NoSuchMethodError) {  }
    try { `XXdebug-level-compiler-checks` = arguments.debugLevelCompilerChecks } catch (_: NoSuchMethodError) {  }
    try { `XXdump-model` = arguments.dumpArgumentsDir } catch (_: NoSuchMethodError) {  }
    try { `XXexplicit-return-types` = arguments.explicitReturnTypes.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::explicitReturnTypes, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `XXlenient-mode` = arguments.lenientMode } catch (_: NoSuchMethodError) {  }
    try { `Xallow-any-scripts-in-source-roots` = arguments.allowAnyScriptsInSourceRoots } catch (_: NoSuchMethodError) {  }
    try { `Xallow-condition-implies-returns-contracts` = arguments.allowConditionImpliesReturnsContracts } catch (_: NoSuchMethodError) {  }
    try { `Xallow-contracts-on-more-functions` = arguments.allowContractsOnMoreFunctions } catch (_: NoSuchMethodError) {  }
    try { `Xallow-holdsin-contract` = arguments.allowHoldsinContract } catch (_: NoSuchMethodError) {  }
    try { `Xallow-kotlin-package` = arguments.allowKotlinPackage } catch (_: NoSuchMethodError) {  }
    try { `Xallow-pre-17-runtime-jdk` = arguments.allowPre17RuntimeJdk } catch (_: NoSuchMethodError) {  }
    try { `Xallow-reified-type-in-catch` = arguments.allowReifiedTypeInCatch } catch (_: NoSuchMethodError) {  }
    try { `Xallow-returns-result-of` = arguments.allowReturnsResultOf } catch (_: NoSuchMethodError) {  }
    try { `Xannotation-default-target` = arguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::annotationDefaultTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xannotation-target-all` = arguments.annotationTargetAll } catch (_: NoSuchMethodError) {  }
    try { `Xcallable-references-to-contextual` = arguments.callableReferencesToContextual } catch (_: NoSuchMethodError) {  }
    try { `Xcheck-phase-conditions` = arguments.checkPhaseConditions } catch (_: NoSuchMethodError) {  }
    try { `Xcollection-literals` = arguments.collectionLiterals } catch (_: NoSuchMethodError) {  }
    try { `Xcommon-sources` = arguments.commonSources } catch (_: NoSuchMethodError) {  }
    try { `Xcompanion-blocks` = arguments.companionBlocks } catch (_: NoSuchMethodError) {  }
    try { `Xcompanion-blocks-and-extensions` = arguments.companionBlocksAndExtensions } catch (_: NoSuchMethodError) {  }
    try { `Xcompiler-plugin` = arguments.pluginConfigurations } catch (_: NoSuchMethodError) {  }
    try { `Xcompiler-plugin-order` = arguments.pluginOrderConstraints } catch (_: NoSuchMethodError) {  }
    try { `Xconsistent-data-class-copy-visibility` = arguments.consistentDataClassCopyVisibility } catch (_: NoSuchMethodError) {  }
    try { `Xcontext-parameters` = arguments.contextParameters } catch (_: NoSuchMethodError) {  }
    try { `Xcontext-receivers` = arguments.getUsingReflection<Boolean>("contextReceivers") } catch (_: NoSuchMethodError) {  }
    try { `Xcontext-sensitive-resolution` = arguments.contextSensitiveResolution } catch (_: NoSuchMethodError) {  }
    try { `Xdata-flow-based-exhaustiveness` = arguments.dataFlowBasedExhaustiveness } catch (_: NoSuchMethodError) {  }
    try { `Xdetailed-perf` = arguments.detailedPerf } catch (_: NoSuchMethodError) {  }
    try { `Xdirect-java-actualization` = arguments.getUsingReflection<Boolean>("directJavaActualization") } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-default-scripting-plugin` = arguments.disableDefaultScriptingPlugin } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-ir-checkers` = arguments.disableIrCheckers } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-phases` = arguments.disablePhases.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xdont-sort-source-files` = arguments.dontSortSourceFiles } catch (_: NoSuchMethodError) {  }
    try { `Xdont-warn-on-error-suppression` = arguments.dontWarnOnErrorSuppression } catch (_: NoSuchMethodError) {  }
    try { `Xdump-directory` = arguments.dumpDirectory?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xdump-fqname` = arguments.dumpOnlyFqName } catch (_: NoSuchMethodError) {  }
    try { `Xdump-perf` = arguments.dumpPerf?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xeager-lambda-analysis` = arguments.eagerLambdaAnalysis } catch (_: NoSuchMethodError) {  }
    try { `Xenable-additional-ir-checkers` = arguments.enableAdditionalIrCheckers } catch (_: NoSuchMethodError) {  }
    try { `Xenable-incremental-compilation` = arguments.incrementalCompilation } catch (_: NoSuchMethodError) {  }
    try { `Xescaping-functions` = arguments.escapingFunctions.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xexpect-actual-classes` = arguments.expectActualClasses } catch (_: NoSuchMethodError) {  }
    try { `Xexplicit-api` = arguments.explicitApi.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::explicitApi, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xexplicit-backing-fields` = arguments.explicitBackingFields } catch (_: NoSuchMethodError) {  }
    try { `Xexplicit-context-arguments` = arguments.explicitContextArguments } catch (_: NoSuchMethodError) {  }
    try { `Xfir-aggressive-pruning` = arguments.firAggressivePruning } catch (_: NoSuchMethodError) {  }
    try { `Xfragment-dependency` = arguments.fragmentDependencies } catch (_: NoSuchMethodError) {  }
    try { `Xfragment-friend-dependency` = arguments.fragmentFriendDependencies } catch (_: NoSuchMethodError) {  }
    try { `Xfragment-refines` = arguments.fragmentRefines } catch (_: NoSuchMethodError) {  }
    try { `Xfragment-sources` = arguments.fragmentSources } catch (_: NoSuchMethodError) {  }
    try { Xfragments = arguments.fragments } catch (_: NoSuchMethodError) {  }
    try { `Xheader-mode` = arguments.headerMode } catch (_: NoSuchMethodError) {  }
    try { `Xheader-mode-type` = arguments.headerModeType.let { HeaderMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::headerModeType, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xheader-mode-type value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xignore-const-optimization-errors` = arguments.getUsingReflection<Boolean>("ignoreConstOptimizationErrors") } catch (_: NoSuchMethodError) {  }
    try { `Xinline-classes` = arguments.getUsingReflection<Boolean>("inlineClasses") } catch (_: NoSuchMethodError) {  }
    try { `Xintellij-plugin-root` = arguments.getUsingReflection<String?>("intellijPluginRoot") } catch (_: NoSuchMethodError) {  }
    try { `Xintrinsic-const-evaluation` = arguments.intrinsicConstEvaluation } catch (_: NoSuchMethodError) {  }
    try { `Xlist-phases` = arguments.listPhases } catch (_: NoSuchMethodError) {  }
    try { `Xlocal-type-aliases` = arguments.localTypeAliases } catch (_: NoSuchMethodError) {  }
    try { `Xmetadata-klib` = arguments.metadataKlib } catch (_: NoSuchMethodError) {  }
    try { `Xmetadata-version` = arguments.metadataVersion } catch (_: NoSuchMethodError) {  }
    try { `Xmulti-dollar-interpolation` = arguments.multiDollarInterpolation } catch (_: NoSuchMethodError) {  }
    try { `Xmulti-platform` = arguments.multiPlatform } catch (_: NoSuchMethodError) {  }
    try { `Xname-based-destructuring` = arguments.nameBasedDestructuring?.let { NameBasedDestructuringMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::nameBasedDestructuring, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xname-based-destructuring value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xnested-type-aliases` = arguments.nestedTypeAliases } catch (_: NoSuchMethodError) {  }
    try { `Xnew-inference` = arguments.getUsingReflection<Boolean>("newInference") } catch (_: NoSuchMethodError) {  }
    try { `Xno-check-actual` = arguments.getUsingReflection<Boolean>("noCheckActual") } catch (_: NoSuchMethodError) {  }
    try { `Xno-inline` = arguments.noInline } catch (_: NoSuchMethodError) {  }
    try { `Xnon-local-break-continue` = arguments.nonLocalBreakContinue } catch (_: NoSuchMethodError) {  }
    try { `Xphases-to-dump` = arguments.phasesToDump.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xphases-to-dump-after` = arguments.phasesToDumpAfter.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xphases-to-dump-before` = arguments.phasesToDumpBefore.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xphases-to-validate` = arguments.phasesToValidate.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xphases-to-validate-after` = arguments.phasesToValidateAfter.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xphases-to-validate-before` = arguments.phasesToValidateBefore.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { Xplugin = arguments.pluginClasspaths } catch (_: NoSuchMethodError) {  }
    try { `Xprint-configuration` = arguments.printConfiguration } catch (_: NoSuchMethodError) {  }
    try { `Xprofile-phases` = arguments.profilePhases } catch (_: NoSuchMethodError) {  }
    try { `Xrender-internal-diagnostic-names` = arguments.renderInternalDiagnosticNames } catch (_: NoSuchMethodError) {  }
    try { Xrepl = arguments.repl } catch (_: NoSuchMethodError) {  }
    try { `Xreport-all-warnings` = arguments.reportAllWarnings } catch (_: NoSuchMethodError) {  }
    try { `Xreport-output-files` = arguments.reportOutputFiles } catch (_: NoSuchMethodError) {  }
    try { `Xreport-perf` = arguments.reportPerf } catch (_: NoSuchMethodError) {  }
    try { `Xreturn-value-checker` = arguments.returnValueChecker.let { ReturnValueCheckerMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::returnValueChecker, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xseparate-kmp-compilation` = arguments.separateKmpCompilationScheme } catch (_: NoSuchMethodError) {  }
    try { `Xskip-metadata-version-check` = arguments.skipMetadataVersionCheck } catch (_: NoSuchMethodError) {  }
    try { `Xskip-prerelease-check` = arguments.skipPrereleaseCheck } catch (_: NoSuchMethodError) {  }
    try { `Xstdlib-compilation` = arguments.stdlibCompilation } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-api-version-greater-than-language-version-error` = arguments.getUsingReflection<Boolean>("suppressApiVersionGreaterThanLanguageVersionError") } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-version-warnings` = arguments.suppressVersionWarnings } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-warning` = arguments.getUsingReflection<Array<String>>("suppressedDiagnostics").toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xunrestricted-builder-inference` = arguments.getUsingReflection<Boolean>("unrestrictedBuilderInference") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-fir-experimental-checkers` = arguments.getUsingReflection<Boolean>("useFirExperimentalCheckers") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-fir-ic` = arguments.useFirIC } catch (_: NoSuchMethodError) {  }
    try { `Xuse-fir-lt` = arguments.useFirLT } catch (_: NoSuchMethodError) {  }
    try { `Xverbose-phases` = arguments.verbosePhases.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xverify-ir` = arguments.verifyIr?.let { VerifyIrMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::verifyIr, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xverify-ir-nested-offsets` = arguments.getUsingReflection<Boolean>("verifyIrNestedOffsets") } catch (_: NoSuchMethodError) {  }
    try { `Xverify-ir-visibility` = arguments.getUsingReflection<Boolean>("verifyIrVisibility") } catch (_: NoSuchMethodError) {  }
    try { `Xwhen-guards` = arguments.whenGuards } catch (_: NoSuchMethodError) {  }
    try { `api-version` = arguments.apiVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::apiVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `kotlin-home` = arguments.kotlinHome?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `language-version` = arguments.languageVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::languageVersion, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `opt-in` = arguments.optIn.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { progressive = arguments.progressiveMode } catch (_: NoSuchMethodError) {  }
    try { script = arguments.script } catch (_: NoSuchMethodError) {  }
    try { this[COMPILER_PLUGINS] = applyCompilerPlugins(if(COMPILER_PLUGINS in this) this[COMPILER_PLUGINS] else emptyList<CompilerPlugin>(), arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { this[X_WARNING_LEVEL] = applyWarningLevels(if(X_WARNING_LEVEL in this) this[X_WARNING_LEVEL] else emptyList<WarningLevel>(), arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: CommonCompilerArguments): CommonCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.pluginOptions = P ?: emptyArray()
    arguments.manuallyConfiguredFeatures = XXLanguage ?: emptyArray()
    arguments.debugLevelCompilerChecks = `XXdebug-level-compiler-checks`
    arguments.explicitReturnTypes = `XXexplicit-return-types`.stringValue
    arguments.lenientMode = `XXlenient-mode`
    arguments.allowAnyScriptsInSourceRoots = `Xallow-any-scripts-in-source-roots`
    arguments.allowConditionImpliesReturnsContracts = `Xallow-condition-implies-returns-contracts`
    arguments.allowContractsOnMoreFunctions = `Xallow-contracts-on-more-functions`
    arguments.allowHoldsinContract = `Xallow-holdsin-contract`
    arguments.allowKotlinPackage = `Xallow-kotlin-package`
    arguments.allowPre17RuntimeJdk = `Xallow-pre-17-runtime-jdk`
    arguments.allowReifiedTypeInCatch = `Xallow-reified-type-in-catch`
    arguments.allowReturnsResultOf = `Xallow-returns-result-of`
    arguments.annotationDefaultTarget = `Xannotation-default-target`?.stringValue
    arguments.annotationTargetAll = `Xannotation-target-all`
    arguments.callableReferencesToContextual = `Xcallable-references-to-contextual`
    arguments.checkPhaseConditions = `Xcheck-phase-conditions`
    arguments.collectionLiterals = `Xcollection-literals`
    arguments.commonSources = `Xcommon-sources` ?: emptyArray()
    arguments.companionBlocks = `Xcompanion-blocks`
    arguments.companionBlocksAndExtensions = `Xcompanion-blocks-and-extensions`
    arguments.pluginConfigurations = `Xcompiler-plugin` ?: emptyArray()
    arguments.pluginOrderConstraints = `Xcompiler-plugin-order` ?: emptyArray()
    arguments.consistentDataClassCopyVisibility = `Xconsistent-data-class-copy-visibility`
    arguments.contextParameters = `Xcontext-parameters`
    try { arguments.setUsingReflection("contextReceivers", `Xcontext-receivers`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.contextSensitiveResolution = `Xcontext-sensitive-resolution`
    arguments.dataFlowBasedExhaustiveness = `Xdata-flow-based-exhaustiveness`
    try { arguments.setUsingReflection("directJavaActualization", `Xdirect-java-actualization`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DIRECT_JAVA_ACTUALIZATION. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.disableDefaultScriptingPlugin = `Xdisable-default-scripting-plugin`
    arguments.disableIrCheckers = `Xdisable-ir-checkers` ?: emptyArray()
    arguments.disablePhases = `Xdisable-phases`.toTypedArray()
    arguments.dontSortSourceFiles = `Xdont-sort-source-files`
    arguments.dontWarnOnErrorSuppression = `Xdont-warn-on-error-suppression`
    arguments.eagerLambdaAnalysis = `Xeager-lambda-analysis`
    arguments.enableAdditionalIrCheckers = `Xenable-additional-ir-checkers` ?: emptyArray()
    arguments.incrementalCompilation = `Xenable-incremental-compilation`
    arguments.escapingFunctions = `Xescaping-functions`.toTypedArray()
    arguments.expectActualClasses = `Xexpect-actual-classes`
    arguments.explicitApi = `Xexplicit-api`.stringValue
    arguments.explicitBackingFields = `Xexplicit-backing-fields`
    arguments.explicitContextArguments = `Xexplicit-context-arguments`
    arguments.firAggressivePruning = `Xfir-aggressive-pruning`
    arguments.fragmentDependencies = `Xfragment-dependency` ?: emptyArray()
    arguments.fragmentFriendDependencies = `Xfragment-friend-dependency` ?: emptyArray()
    arguments.fragmentRefines = `Xfragment-refines` ?: emptyArray()
    arguments.fragmentSources = `Xfragment-sources` ?: emptyArray()
    arguments.fragments = Xfragments ?: emptyArray()
    arguments.headerMode = `Xheader-mode`
    arguments.headerModeType = `Xheader-mode-type`.stringValue
    try { arguments.setUsingReflection("ignoreConstOptimizationErrors", `Xignore-const-optimization-errors`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IGNORE_CONST_OPTIMIZATION_ERRORS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("inlineClasses", `Xinline-classes`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INLINE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("intellijPluginRoot", `Xintellij-plugin-root`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INTELLIJ_PLUGIN_ROOT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.intrinsicConstEvaluation = `Xintrinsic-const-evaluation`
    arguments.localTypeAliases = `Xlocal-type-aliases`
    arguments.metadataKlib = `Xmetadata-klib`
    arguments.metadataVersion = `Xmetadata-version`
    arguments.multiDollarInterpolation = `Xmulti-dollar-interpolation`
    arguments.multiPlatform = `Xmulti-platform`
    arguments.nameBasedDestructuring = `Xname-based-destructuring`?.stringValue
    arguments.nestedTypeAliases = `Xnested-type-aliases`
    try { arguments.setUsingReflection("newInference", `Xnew-inference`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NEW_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("noCheckActual", `Xno-check-actual`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NO_CHECK_ACTUAL. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.noInline = `Xno-inline`
    arguments.nonLocalBreakContinue = `Xnon-local-break-continue`
    arguments.phasesToValidate = `Xphases-to-validate`.toTypedArray()
    arguments.phasesToValidateAfter = `Xphases-to-validate-after`.toTypedArray()
    arguments.phasesToValidateBefore = `Xphases-to-validate-before`.toTypedArray()
    arguments.pluginClasspaths = Xplugin ?: emptyArray()
    arguments.renderInternalDiagnosticNames = `Xrender-internal-diagnostic-names`
    arguments.repl = Xrepl
    arguments.returnValueChecker = `Xreturn-value-checker`.stringValue
    arguments.separateKmpCompilationScheme = `Xseparate-kmp-compilation`
    arguments.skipMetadataVersionCheck = `Xskip-metadata-version-check`
    arguments.skipPrereleaseCheck = `Xskip-prerelease-check`
    arguments.stdlibCompilation = `Xstdlib-compilation`
    try { arguments.setUsingReflection("suppressApiVersionGreaterThanLanguageVersionError", `Xsuppress-api-version-greater-than-language-version-error`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.suppressVersionWarnings = `Xsuppress-version-warnings`
    try { arguments.setUsingReflection("suppressedDiagnostics", `Xsuppress-warning`.toTypedArray()) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_WARNING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("unrestrictedBuilderInference", `Xunrestricted-builder-inference`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_UNRESTRICTED_BUILDER_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("useFirExperimentalCheckers", `Xuse-fir-experimental-checkers`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_FIR_EXPERIMENTAL_CHECKERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.useFirIC = `Xuse-fir-ic`
    arguments.useFirLT = `Xuse-fir-lt`
    arguments.verifyIr = `Xverify-ir`?.stringValue
    try { arguments.setUsingReflection("verifyIrNestedOffsets", `Xverify-ir-nested-offsets`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_NESTED_OFFSETS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.3.20 and removed in 2.4.20""").initCause(e) }
    try { arguments.setUsingReflection("verifyIrVisibility", `Xverify-ir-visibility`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    arguments.whenGuards = `Xwhen-guards`
    arguments.apiVersion = `api-version`?.stringValue
    arguments.kotlinHome = `kotlin-home`?.absolutePathStringOrThrow()
    arguments.languageVersion = `language-version`?.stringValue
    arguments.optIn = `opt-in`.toTypedArray()
    arguments.progressiveMode = progressive
    arguments.script = script
    if (COMPILER_PLUGINS in this) { arguments.applyCompilerPlugins(get(COMPILER_PLUGINS))}
    if (X_WARNING_LEVEL in this) { arguments.applyWarningLevels(get(X_WARNING_LEVEL))}
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

    public val KOTLIN_HOME: CommonCompilerArgument<Path?> = CommonCompilerArgument("KOTLIN_HOME")

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
