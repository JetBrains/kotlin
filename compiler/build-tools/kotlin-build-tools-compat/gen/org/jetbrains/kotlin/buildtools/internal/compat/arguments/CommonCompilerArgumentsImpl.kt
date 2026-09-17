// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

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
import kotlin.collections.emptyList
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments as ArgumentsCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal abstract class CommonCompilerArgumentsImpl() : CommonToolArgumentsImpl(),
    ArgumentsCommonCompilerArguments, ArgumentsCommonCompilerArguments.Builder {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("P")
  protected var P: Array<String>? = defaultArguments.pluginOptions

  @SerialName("XX_DEBUG_LEVEL_COMPILER_CHECKS")
  protected var `XXdebug-level-compiler-checks`: Boolean = defaultArguments.debugLevelCompilerChecks

  @SerialName("XX_EXPLICIT_RETURN_TYPES")
  protected var `XXexplicit-return-types`: ExplicitApiMode =
      defaultArguments.explicitReturnTypes.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") }

  @SerialName("XX_LENIENT_MODE")
  protected var `XXlenient-mode`: Boolean = defaultArguments.lenientMode

  @SerialName("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")
  protected var `Xallow-any-scripts-in-source-roots`: Boolean =
      defaultArguments.allowAnyScriptsInSourceRoots

  @SerialName("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS")
  protected var `Xallow-condition-implies-returns-contracts`: Boolean =
      defaultArguments.allowConditionImpliesReturnsContracts

  @SerialName("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS")
  protected var `Xallow-contracts-on-more-functions`: Boolean =
      defaultArguments.allowContractsOnMoreFunctions

  @SerialName("X_ALLOW_HOLDSIN_CONTRACT")
  protected var `Xallow-holdsin-contract`: Boolean = defaultArguments.allowHoldsinContract

  @SerialName("X_ALLOW_KOTLIN_PACKAGE")
  protected var `Xallow-kotlin-package`: Boolean = defaultArguments.allowKotlinPackage

  @SerialName("X_ALLOW_REIFIED_TYPE_IN_CATCH")
  protected var `Xallow-reified-type-in-catch`: Boolean = defaultArguments.allowReifiedTypeInCatch

  @SerialName("X_ANNOTATION_DEFAULT_TARGET")
  protected var `Xannotation-default-target`: AnnotationDefaultTargetMode? =
      defaultArguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") }

  @SerialName("X_ANNOTATION_TARGET_ALL")
  protected var `Xannotation-target-all`: Boolean = defaultArguments.annotationTargetAll

  @SerialName("X_CHECK_PHASE_CONDITIONS")
  protected var `Xcheck-phase-conditions`: Boolean = defaultArguments.checkPhaseConditions

  @SerialName("X_COMMON_SOURCES")
  protected var `Xcommon-sources`: Array<String>? = defaultArguments.commonSources

  @SerialName("X_COMPILER_PLUGIN")
  protected var `Xcompiler-plugin`: Array<String>? = defaultArguments.pluginConfigurations

  @SerialName("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY")
  protected var `Xconsistent-data-class-copy-visibility`: Boolean =
      defaultArguments.consistentDataClassCopyVisibility

  @SerialName("X_CONTEXT_PARAMETERS")
  protected var `Xcontext-parameters`: Boolean = defaultArguments.contextParameters

  @SerialName("X_CONTEXT_RECEIVERS")
  protected var `Xcontext-receivers`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("contextReceivers")

  @SerialName("X_CONTEXT_SENSITIVE_RESOLUTION")
  protected var `Xcontext-sensitive-resolution`: Boolean =
      defaultArguments.contextSensitiveResolution

  @SerialName("X_DATA_FLOW_BASED_EXHAUSTIVENESS")
  protected var `Xdata-flow-based-exhaustiveness`: Boolean =
      defaultArguments.dataFlowBasedExhaustiveness

  @SerialName("X_DIRECT_JAVA_ACTUALIZATION")
  protected var `Xdirect-java-actualization`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("directJavaActualization")

  @SerialName("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")
  protected var `Xdisable-default-scripting-plugin`: Boolean =
      defaultArguments.disableDefaultScriptingPlugin

  @SerialName("X_DISABLE_PHASES")
  protected var `Xdisable-phases`: List<String> = defaultArguments.disablePhases.toListOrEmpty()

  @SerialName("X_DONT_WARN_ON_ERROR_SUPPRESSION")
  protected var `Xdont-warn-on-error-suppression`: Boolean =
      defaultArguments.dontWarnOnErrorSuppression

  @SerialName("X_DUMP_DIRECTORY")
  protected var `Xdump-directory`: Path? =
      defaultArguments.dumpDirectory?.let { kotlin.io.path.Path(it) }

  @SerialName("X_DUMP_FQNAME")
  protected var `Xdump-fqname`: String? = defaultArguments.dumpOnlyFqName

  @SerialName("X_DUMP_PERF")
  protected var `Xdump-perf`: Path? = defaultArguments.dumpPerf?.let { kotlin.io.path.Path(it) }

  @SerialName("X_ENABLE_INCREMENTAL_COMPILATION")
  protected var `Xenable-incremental-compilation`: Boolean? =
      defaultArguments.incrementalCompilation

  @SerialName("X_EXPECT_ACTUAL_CLASSES")
  protected var `Xexpect-actual-classes`: Boolean = defaultArguments.expectActualClasses

  @SerialName("X_EXPLICIT_API")
  protected var `Xexplicit-api`: ExplicitApiMode =
      defaultArguments.explicitApi.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") }

  @SerialName("X_FRAGMENT_DEPENDENCY")
  protected var `Xfragment-dependency`: Array<String>? = defaultArguments.fragmentDependencies

  @SerialName("X_FRAGMENT_REFINES")
  protected var `Xfragment-refines`: Array<String>? = defaultArguments.fragmentRefines

  @SerialName("X_FRAGMENT_SOURCES")
  protected var `Xfragment-sources`: Array<String>? = defaultArguments.fragmentSources

  @SerialName("X_FRAGMENTS")
  protected var Xfragments: Array<String>? = defaultArguments.fragments

  @SerialName("X_IGNORE_CONST_OPTIMIZATION_ERRORS")
  protected var `Xignore-const-optimization-errors`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("ignoreConstOptimizationErrors")

  @SerialName("X_INLINE_CLASSES")
  protected var `Xinline-classes`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("inlineClasses")

  @SerialName("X_INTELLIJ_PLUGIN_ROOT")
  protected var `Xintellij-plugin-root`: String? =
      defaultArguments.getUsingReflection<String?>("intellijPluginRoot")

  @SerialName("X_LIST_PHASES")
  protected var `Xlist-phases`: Boolean = defaultArguments.listPhases

  @SerialName("X_METADATA_KLIB")
  protected var `Xmetadata-klib`: Boolean = defaultArguments.metadataKlib

  @SerialName("X_METADATA_VERSION")
  protected var `Xmetadata-version`: String? = defaultArguments.metadataVersion

  @SerialName("X_MULTI_DOLLAR_INTERPOLATION")
  protected var `Xmulti-dollar-interpolation`: Boolean = defaultArguments.multiDollarInterpolation

  @SerialName("X_MULTI_PLATFORM")
  protected var `Xmulti-platform`: Boolean = defaultArguments.multiPlatform

  @SerialName("X_NESTED_TYPE_ALIASES")
  protected var `Xnested-type-aliases`: Boolean = defaultArguments.nestedTypeAliases

  @SerialName("X_NEW_INFERENCE")
  protected var `Xnew-inference`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("newInference")

  @SerialName("X_NO_CHECK_ACTUAL")
  protected var `Xno-check-actual`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("noCheckActual")

  @SerialName("X_NO_INLINE")
  protected var `Xno-inline`: Boolean = defaultArguments.noInline

  @SerialName("X_NON_LOCAL_BREAK_CONTINUE")
  protected var `Xnon-local-break-continue`: Boolean = defaultArguments.nonLocalBreakContinue

  @SerialName("X_PHASES_TO_DUMP")
  protected var `Xphases-to-dump`: List<String> = defaultArguments.phasesToDump.toListOrEmpty()

  @SerialName("X_PHASES_TO_DUMP_AFTER")
  protected var `Xphases-to-dump-after`: List<String> =
      defaultArguments.phasesToDumpAfter.toListOrEmpty()

  @SerialName("X_PHASES_TO_DUMP_BEFORE")
  protected var `Xphases-to-dump-before`: List<String> =
      defaultArguments.phasesToDumpBefore.toListOrEmpty()

  @SerialName("X_PHASES_TO_VALIDATE")
  protected var `Xphases-to-validate`: List<String> =
      defaultArguments.phasesToValidate.toListOrEmpty()

  @SerialName("X_PHASES_TO_VALIDATE_AFTER")
  protected var `Xphases-to-validate-after`: List<String> =
      defaultArguments.phasesToValidateAfter.toListOrEmpty()

  @SerialName("X_PHASES_TO_VALIDATE_BEFORE")
  protected var `Xphases-to-validate-before`: List<String> =
      defaultArguments.phasesToValidateBefore.toListOrEmpty()

  @SerialName("X_PLUGIN")
  protected var Xplugin: Array<String>? = defaultArguments.pluginClasspaths

  @SerialName("X_PROFILE_PHASES")
  protected var `Xprofile-phases`: Boolean = defaultArguments.profilePhases

  @SerialName("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES")
  protected var `Xrender-internal-diagnostic-names`: Boolean =
      defaultArguments.renderInternalDiagnosticNames

  @SerialName("X_REPL")
  protected var Xrepl: Boolean = defaultArguments.repl

  @SerialName("X_REPORT_ALL_WARNINGS")
  protected var `Xreport-all-warnings`: Boolean = defaultArguments.reportAllWarnings

  @SerialName("X_REPORT_OUTPUT_FILES")
  protected var `Xreport-output-files`: Boolean = defaultArguments.reportOutputFiles

  @SerialName("X_REPORT_PERF")
  protected var `Xreport-perf`: Boolean = defaultArguments.reportPerf

  @SerialName("X_RETURN_VALUE_CHECKER")
  protected var `Xreturn-value-checker`: ReturnValueCheckerMode =
      defaultArguments.returnValueChecker.let { ReturnValueCheckerMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") }

  @SerialName("X_SEPARATE_KMP_COMPILATION")
  protected var `Xseparate-kmp-compilation`: Boolean = defaultArguments.separateKmpCompilationScheme

  @SerialName("X_SKIP_METADATA_VERSION_CHECK")
  protected var `Xskip-metadata-version-check`: Boolean = defaultArguments.skipMetadataVersionCheck

  @SerialName("X_SKIP_PRERELEASE_CHECK")
  protected var `Xskip-prerelease-check`: Boolean = defaultArguments.skipPrereleaseCheck

  @SerialName("X_STDLIB_COMPILATION")
  protected var `Xstdlib-compilation`: Boolean = defaultArguments.stdlibCompilation

  @SerialName("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR")
  protected var `Xsuppress-api-version-greater-than-language-version-error`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("suppressApiVersionGreaterThanLanguageVersionError")

  @SerialName("X_SUPPRESS_VERSION_WARNINGS")
  protected var `Xsuppress-version-warnings`: Boolean = defaultArguments.suppressVersionWarnings

  @SerialName("X_SUPPRESS_WARNING")
  protected var `Xsuppress-warning`: List<String> =
      defaultArguments.getUsingReflection<Array<String>>("suppressedDiagnostics").toListOrEmpty()

  @SerialName("X_UNRESTRICTED_BUILDER_INFERENCE")
  protected var `Xunrestricted-builder-inference`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("unrestrictedBuilderInference")

  @SerialName("X_USE_FIR_EXPERIMENTAL_CHECKERS")
  protected var `Xuse-fir-experimental-checkers`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("useFirExperimentalCheckers")

  @SerialName("X_USE_FIR_IC")
  protected var `Xuse-fir-ic`: Boolean = defaultArguments.useFirIC

  @SerialName("X_USE_FIR_LT")
  protected var `Xuse-fir-lt`: Boolean = defaultArguments.useFirLT

  @SerialName("X_USE_K2")
  protected var `Xuse-k2`: Boolean = defaultArguments.getUsingReflection<Boolean>("useK2")

  @SerialName("X_VERBOSE_PHASES")
  protected var `Xverbose-phases`: List<String> = defaultArguments.verbosePhases.toListOrEmpty()

  @SerialName("X_VERIFY_IR")
  protected var `Xverify-ir`: VerifyIrMode? =
      defaultArguments.verifyIr?.let { VerifyIrMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") }

  @SerialName("X_VERIFY_IR_VISIBILITY")
  protected var `Xverify-ir-visibility`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("verifyIrVisibility")

  @SerialName("X_WHEN_GUARDS")
  protected var `Xwhen-guards`: Boolean = defaultArguments.whenGuards

  @SerialName("API_VERSION")
  protected var `api-version`: KotlinVersion? =
      defaultArguments.apiVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") }

  @SerialName("KOTLIN_HOME")
  protected var `kotlin-home`: Path? = defaultArguments.kotlinHome?.let { kotlin.io.path.Path(it) }

  @SerialName("LANGUAGE_VERSION")
  protected var `language-version`: KotlinVersion? =
      defaultArguments.languageVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") }

  @SerialName("OPT_IN")
  protected var `opt-in`: List<String> = defaultArguments.optIn.toListOrEmpty()

  @SerialName("PROGRESSIVE")
  protected var progressive: Boolean = defaultArguments.progressiveMode

  @SerialName("SCRIPT")
  protected var script: Boolean = defaultArguments.script

  @SerialName("X_WARNING_LEVEL")
  protected var `Xwarning-level`: List<WarningLevel> =
      applyWarningLevels(emptyList<WarningLevel>(), defaultArguments)

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
  override operator fun <V> `get`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  override operator fun <V> `set`(key: ArgumentsCommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    val currentKotlinVersion = KotlinToolingVersion(KC_VERSION)
    if (key.availableSinceVersion > KotlinReleaseVersion(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)) {
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
    try { arguments.debugLevelCompilerChecks = `XXdebug-level-compiler-checks` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_DEBUG_LEVEL_COMPILER_CHECKS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    try { arguments.explicitReturnTypes = `XXexplicit-return-types`.stringValue } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_EXPLICIT_RETURN_TYPES. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    try { arguments.lenientMode = `XXlenient-mode` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: XX_LENIENT_MODE. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    arguments.allowAnyScriptsInSourceRoots = `Xallow-any-scripts-in-source-roots`
    try { arguments.allowConditionImpliesReturnsContracts = `Xallow-condition-implies-returns-contracts` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    try { arguments.allowContractsOnMoreFunctions = `Xallow-contracts-on-more-functions` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    try { arguments.allowHoldsinContract = `Xallow-holdsin-contract` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_HOLDSIN_CONTRACT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    arguments.allowKotlinPackage = `Xallow-kotlin-package`
    try { arguments.allowReifiedTypeInCatch = `Xallow-reified-type-in-catch` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ALLOW_REIFIED_TYPE_IN_CATCH. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    try { arguments.annotationDefaultTarget = `Xannotation-default-target`?.stringValue } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATION_DEFAULT_TARGET. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    try { arguments.annotationTargetAll = `Xannotation-target-all` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATION_TARGET_ALL. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    arguments.checkPhaseConditions = `Xcheck-phase-conditions`
    arguments.commonSources = `Xcommon-sources` ?: emptyArray()
    arguments.pluginConfigurations = `Xcompiler-plugin` ?: emptyArray()
    try { arguments.consistentDataClassCopyVisibility = `Xconsistent-data-class-copy-visibility` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    try { arguments.contextParameters = `Xcontext-parameters` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_PARAMETERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    try { arguments.setUsingReflection("contextReceivers", `Xcontext-receivers`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_RECEIVERS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.contextSensitiveResolution = `Xcontext-sensitive-resolution` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_CONTEXT_SENSITIVE_RESOLUTION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    try { arguments.dataFlowBasedExhaustiveness = `Xdata-flow-based-exhaustiveness` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DATA_FLOW_BASED_EXHAUSTIVENESS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    try { arguments.setUsingReflection("directJavaActualization", `Xdirect-java-actualization`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DIRECT_JAVA_ACTUALIZATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0 and removed in 2.5.0""").initCause(e) }
    arguments.disableDefaultScriptingPlugin = `Xdisable-default-scripting-plugin`
    arguments.disablePhases = `Xdisable-phases`.toTypedArray()
    try { arguments.dontWarnOnErrorSuppression = `Xdont-warn-on-error-suppression` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_DONT_WARN_ON_ERROR_SUPPRESSION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    arguments.dumpDirectory = `Xdump-directory`?.absolutePathStringOrThrow()
    arguments.dumpOnlyFqName = `Xdump-fqname`
    arguments.dumpPerf = `Xdump-perf`?.absolutePathStringOrThrow()
    arguments.incrementalCompilation = `Xenable-incremental-compilation`
    try { arguments.expectActualClasses = `Xexpect-actual-classes` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_EXPECT_ACTUAL_CLASSES. Current compiler version is: $KC_VERSION, but the argument was introduced in 1.9.20""").initCause(e) }
    arguments.explicitApi = `Xexplicit-api`.stringValue
    try { arguments.fragmentDependencies = `Xfragment-dependency` ?: emptyArray() } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_FRAGMENT_DEPENDENCY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    arguments.fragmentRefines = `Xfragment-refines` ?: emptyArray()
    arguments.fragmentSources = `Xfragment-sources` ?: emptyArray()
    arguments.fragments = Xfragments ?: emptyArray()
    try { arguments.setUsingReflection("ignoreConstOptimizationErrors", `Xignore-const-optimization-errors`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IGNORE_CONST_OPTIMIZATION_ERRORS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("inlineClasses", `Xinline-classes`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INLINE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("intellijPluginRoot", `Xintellij-plugin-root`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INTELLIJ_PLUGIN_ROOT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.listPhases = `Xlist-phases`
    try { arguments.metadataKlib = `Xmetadata-klib` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_METADATA_KLIB. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    arguments.metadataVersion = `Xmetadata-version`
    try { arguments.multiDollarInterpolation = `Xmulti-dollar-interpolation` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_MULTI_DOLLAR_INTERPOLATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    arguments.multiPlatform = `Xmulti-platform`
    try { arguments.nestedTypeAliases = `Xnested-type-aliases` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NESTED_TYPE_ALIASES. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    try { arguments.setUsingReflection("newInference", `Xnew-inference`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NEW_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("noCheckActual", `Xno-check-actual`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NO_CHECK_ACTUAL. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.noInline = `Xno-inline`
    try { arguments.nonLocalBreakContinue = `Xnon-local-break-continue` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_NON_LOCAL_BREAK_CONTINUE. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0""").initCause(e) }
    arguments.phasesToDump = `Xphases-to-dump`.toTypedArray()
    arguments.phasesToDumpAfter = `Xphases-to-dump-after`.toTypedArray()
    arguments.phasesToDumpBefore = `Xphases-to-dump-before`.toTypedArray()
    arguments.phasesToValidate = `Xphases-to-validate`.toTypedArray()
    arguments.phasesToValidateAfter = `Xphases-to-validate-after`.toTypedArray()
    arguments.phasesToValidateBefore = `Xphases-to-validate-before`.toTypedArray()
    arguments.pluginClasspaths = Xplugin ?: emptyArray()
    arguments.profilePhases = `Xprofile-phases`
    arguments.renderInternalDiagnosticNames = `Xrender-internal-diagnostic-names`
    try { arguments.repl = Xrepl } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_REPL. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    try { arguments.reportAllWarnings = `Xreport-all-warnings` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_REPORT_ALL_WARNINGS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    arguments.reportOutputFiles = `Xreport-output-files`
    arguments.reportPerf = `Xreport-perf`
    try { arguments.returnValueChecker = `Xreturn-value-checker`.stringValue } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_RETURN_VALUE_CHECKER. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    try { arguments.separateKmpCompilationScheme = `Xseparate-kmp-compilation` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SEPARATE_KMP_COMPILATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    arguments.skipMetadataVersionCheck = `Xskip-metadata-version-check`
    arguments.skipPrereleaseCheck = `Xskip-prerelease-check`
    try { arguments.stdlibCompilation = `Xstdlib-compilation` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_STDLIB_COMPILATION. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    try { arguments.setUsingReflection("suppressApiVersionGreaterThanLanguageVersionError", `Xsuppress-api-version-greater-than-language-version-error`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0 and removed in 2.5.0""").initCause(e) }
    arguments.suppressVersionWarnings = `Xsuppress-version-warnings`
    try { arguments.setUsingReflection("suppressedDiagnostics", `Xsuppress-warning`.toTypedArray()) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_WARNING. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0 and removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("unrestrictedBuilderInference", `Xunrestricted-builder-inference`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_UNRESTRICTED_BUILDER_INFERENCE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("useFirExperimentalCheckers", `Xuse-fir-experimental-checkers`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_FIR_EXPERIMENTAL_CHECKERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0 and removed in 2.5.0""").initCause(e) }
    arguments.useFirIC = `Xuse-fir-ic`
    arguments.useFirLT = `Xuse-fir-lt`
    try { arguments.setUsingReflection("useK2", `Xuse-k2`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2. Current compiler version is: $KC_VERSION, but the argument was removed in 2.2.0""").initCause(e) }
    arguments.verbosePhases = `Xverbose-phases`.toTypedArray()
    try { arguments.verifyIr = `Xverify-ir`?.stringValue } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    try { arguments.setUsingReflection("verifyIrVisibility", `Xverify-ir-visibility`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VERIFY_IR_VISIBILITY. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20 and removed in 2.4.20""").initCause(e) }
    try { arguments.whenGuards = `Xwhen-guards` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WHEN_GUARDS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.20""").initCause(e) }
    arguments.apiVersion = `api-version`?.stringValue
    arguments.kotlinHome = `kotlin-home`?.absolutePathStringOrThrow()
    arguments.languageVersion = `language-version`?.stringValue
    arguments.optIn = `opt-in`.toTypedArray()
    arguments.progressiveMode = progressive
    arguments.script = script
    try { arguments.applyWarningLevels(`Xwarning-level`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WARNING_LEVEL. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: CommonCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { P = arguments.pluginOptions } catch (_: NoSuchMethodError) {  }
    try { `XXdebug-level-compiler-checks` = arguments.debugLevelCompilerChecks } catch (_: NoSuchMethodError) {  }
    try { `XXexplicit-return-types` = arguments.explicitReturnTypes.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -XXexplicit-return-types value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `XXlenient-mode` = arguments.lenientMode } catch (_: NoSuchMethodError) {  }
    try { `Xallow-any-scripts-in-source-roots` = arguments.allowAnyScriptsInSourceRoots } catch (_: NoSuchMethodError) {  }
    try { `Xallow-condition-implies-returns-contracts` = arguments.allowConditionImpliesReturnsContracts } catch (_: NoSuchMethodError) {  }
    try { `Xallow-contracts-on-more-functions` = arguments.allowContractsOnMoreFunctions } catch (_: NoSuchMethodError) {  }
    try { `Xallow-holdsin-contract` = arguments.allowHoldsinContract } catch (_: NoSuchMethodError) {  }
    try { `Xallow-kotlin-package` = arguments.allowKotlinPackage } catch (_: NoSuchMethodError) {  }
    try { `Xallow-reified-type-in-catch` = arguments.allowReifiedTypeInCatch } catch (_: NoSuchMethodError) {  }
    try { `Xannotation-default-target` = arguments.annotationDefaultTarget?.let { AnnotationDefaultTargetMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xannotation-target-all` = arguments.annotationTargetAll } catch (_: NoSuchMethodError) {  }
    try { `Xcheck-phase-conditions` = arguments.checkPhaseConditions } catch (_: NoSuchMethodError) {  }
    try { `Xcommon-sources` = arguments.commonSources } catch (_: NoSuchMethodError) {  }
    try { `Xcompiler-plugin` = arguments.pluginConfigurations } catch (_: NoSuchMethodError) {  }
    try { `Xconsistent-data-class-copy-visibility` = arguments.consistentDataClassCopyVisibility } catch (_: NoSuchMethodError) {  }
    try { `Xcontext-parameters` = arguments.contextParameters } catch (_: NoSuchMethodError) {  }
    try { `Xcontext-receivers` = arguments.getUsingReflection<Boolean>("contextReceivers") } catch (_: NoSuchMethodError) {  }
    try { `Xcontext-sensitive-resolution` = arguments.contextSensitiveResolution } catch (_: NoSuchMethodError) {  }
    try { `Xdata-flow-based-exhaustiveness` = arguments.dataFlowBasedExhaustiveness } catch (_: NoSuchMethodError) {  }
    try { `Xdirect-java-actualization` = arguments.getUsingReflection<Boolean>("directJavaActualization") } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-default-scripting-plugin` = arguments.disableDefaultScriptingPlugin } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-phases` = arguments.disablePhases.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xdont-warn-on-error-suppression` = arguments.dontWarnOnErrorSuppression } catch (_: NoSuchMethodError) {  }
    try { `Xdump-directory` = arguments.dumpDirectory?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xdump-fqname` = arguments.dumpOnlyFqName } catch (_: NoSuchMethodError) {  }
    try { `Xdump-perf` = arguments.dumpPerf?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xenable-incremental-compilation` = arguments.incrementalCompilation } catch (_: NoSuchMethodError) {  }
    try { `Xexpect-actual-classes` = arguments.expectActualClasses } catch (_: NoSuchMethodError) {  }
    try { `Xexplicit-api` = arguments.explicitApi.let { ExplicitApiMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xexplicit-api value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xfragment-dependency` = arguments.fragmentDependencies } catch (_: NoSuchMethodError) {  }
    try { `Xfragment-refines` = arguments.fragmentRefines } catch (_: NoSuchMethodError) {  }
    try { `Xfragment-sources` = arguments.fragmentSources } catch (_: NoSuchMethodError) {  }
    try { Xfragments = arguments.fragments } catch (_: NoSuchMethodError) {  }
    try { `Xignore-const-optimization-errors` = arguments.getUsingReflection<Boolean>("ignoreConstOptimizationErrors") } catch (_: NoSuchMethodError) {  }
    try { `Xinline-classes` = arguments.getUsingReflection<Boolean>("inlineClasses") } catch (_: NoSuchMethodError) {  }
    try { `Xintellij-plugin-root` = arguments.getUsingReflection<String?>("intellijPluginRoot") } catch (_: NoSuchMethodError) {  }
    try { `Xlist-phases` = arguments.listPhases } catch (_: NoSuchMethodError) {  }
    try { `Xmetadata-klib` = arguments.metadataKlib } catch (_: NoSuchMethodError) {  }
    try { `Xmetadata-version` = arguments.metadataVersion } catch (_: NoSuchMethodError) {  }
    try { `Xmulti-dollar-interpolation` = arguments.multiDollarInterpolation } catch (_: NoSuchMethodError) {  }
    try { `Xmulti-platform` = arguments.multiPlatform } catch (_: NoSuchMethodError) {  }
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
    try { `Xprofile-phases` = arguments.profilePhases } catch (_: NoSuchMethodError) {  }
    try { `Xrender-internal-diagnostic-names` = arguments.renderInternalDiagnosticNames } catch (_: NoSuchMethodError) {  }
    try { Xrepl = arguments.repl } catch (_: NoSuchMethodError) {  }
    try { `Xreport-all-warnings` = arguments.reportAllWarnings } catch (_: NoSuchMethodError) {  }
    try { `Xreport-output-files` = arguments.reportOutputFiles } catch (_: NoSuchMethodError) {  }
    try { `Xreport-perf` = arguments.reportPerf } catch (_: NoSuchMethodError) {  }
    try { `Xreturn-value-checker` = arguments.returnValueChecker.let { ReturnValueCheckerMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xreturn-value-checker value: $it") } } catch (_: NoSuchMethodError) {  }
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
    try { `Xuse-k2` = arguments.getUsingReflection<Boolean>("useK2") } catch (_: NoSuchMethodError) {  }
    try { `Xverbose-phases` = arguments.verbosePhases.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xverify-ir` = arguments.verifyIr?.let { VerifyIrMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xverify-ir-visibility` = arguments.getUsingReflection<Boolean>("verifyIrVisibility") } catch (_: NoSuchMethodError) {  }
    try { `Xwhen-guards` = arguments.whenGuards } catch (_: NoSuchMethodError) {  }
    try { `api-version` = arguments.apiVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -api-version value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `kotlin-home` = arguments.kotlinHome?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `language-version` = arguments.languageVersion?.let { KotlinVersion.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -language-version value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `opt-in` = arguments.optIn.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { progressive = arguments.progressiveMode } catch (_: NoSuchMethodError) {  }
    try { script = arguments.script } catch (_: NoSuchMethodError) {  }
    try { `Xwarning-level` = applyWarningLevels(`Xwarning-level`, arguments) } catch (_: NoSuchMethodError) {  }
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

    public val X_DUMP_DIRECTORY: CommonCompilerArgument<Path?> =
        CommonCompilerArgument("X_DUMP_DIRECTORY")

    public val X_DUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_FQNAME")

    public val X_DUMP_PERF: CommonCompilerArgument<Path?> = CommonCompilerArgument("X_DUMP_PERF")

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

    public val KOTLIN_HOME: CommonCompilerArgument<Path?> = CommonCompilerArgument("KOTLIN_HOME")

    public val LANGUAGE_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("LANGUAGE_VERSION")

    public val OPT_IN: CommonCompilerArgument<List<String>> = CommonCompilerArgument("OPT_IN")

    public val PROGRESSIVE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("PROGRESSIVE")

    public val SCRIPT: CommonCompilerArgument<Boolean> = CommonCompilerArgument("SCRIPT")

    public val X_WARNING_LEVEL: CommonCompilerArgument<List<WarningLevel>> =
        CommonCompilerArgument("X_WARNING_LEVEL")
  }
}
