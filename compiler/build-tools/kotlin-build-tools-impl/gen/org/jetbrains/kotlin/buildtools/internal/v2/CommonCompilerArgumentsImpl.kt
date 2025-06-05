package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.API_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.P
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.PROGRESSIVE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.SCRIPT
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XANNOTATION_TARGET_ALL
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XCHECK_PHASE_CONDITIONS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XCOMMON_SOURCES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XCOMPILER_PLUGIN
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XCONSISTENT_DATA_CLASS_COPY_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XCONTEXT_PARAMETERS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XCONTEXT_RECEIVERS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XCONTEXT_SENSITIVE_RESOLUTION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XDIRECT_JAVA_ACTUALIZATION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XDISABLE_DEFAULT_SCRIPTING_PLUGIN
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XDISABLE_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XDONT_WARN_ON_ERROR_SUPPRESSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XDUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XDUMP_FQNAME
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XDUMP_PERF
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XENABLE_INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XEXPECT_ACTUAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XEXPLICIT_API
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XFRAGMENTS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XFRAGMENT_DEPENDENCY
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XFRAGMENT_REFINES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XFRAGMENT_SOURCES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XIGNORE_CONST_OPTIMIZATION_ERRORS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XINLINE_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XINTELLIJ_PLUGIN_ROOT
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XLIST_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XMETADATA_KLIB
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XMETADATA_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XMULTI_DOLLAR_INTERPOLATION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XMULTI_PLATFORM
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XNESTED_TYPE_ALIASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XNEW_INFERENCE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XNON_LOCAL_BREAK_CONTINUE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XNO_CHECK_ACTUAL
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XNO_INLINE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPHASES_TO_DUMP
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPHASES_TO_DUMP_AFTER
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPHASES_TO_DUMP_BEFORE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPHASES_TO_VALIDATE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPHASES_TO_VALIDATE_AFTER
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPHASES_TO_VALIDATE_BEFORE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPLUGIN
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XPROFILE_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XRENDER_INTERNAL_DIAGNOSTIC_NAMES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XREPL
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XREPORT_ALL_WARNINGS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XREPORT_OUTPUT_FILES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XREPORT_PERF
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XRETURN_VALUE_CHECKER
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XSEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XSKIP_METADATA_VERSION_CHECK
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XSKIP_PRERELEASE_CHECK
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XSTDLIB_COMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XSUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XSUPPRESS_VERSION_WARNINGS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XSUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XUNRESTRICTED_BUILDER_INFERENCE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XUSE_FIR_EXPERIMENTAL_CHECKERS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XUSE_FIR_IC
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XUSE_FIR_LT
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XUSE_K2
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XVERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XVERIFY_IR
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XVERIFY_IR_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XWARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XWHEN_GUARDS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XXDEBUG_LEVEL_COMPILER_CHECKS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XXEXPLICIT_RETURN_TYPES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XXLENIENT_MODE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments as V2CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments as CommonCompilerArguments

public open class CommonCompilerArgumentsImpl : CommonToolArgumentsImpl(),
    V2CommonCompilerArguments {
  private val optionsMap: MutableMap<V2CommonCompilerArguments.CommonCompilerArgument<*>, Any?> =
      mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: V2CommonCompilerArguments.CommonCompilerArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: V2CommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: CommonCompilerArguments): CommonCompilerArguments {
    if (LANGUAGE_VERSION in optionsMap) { arguments.languageVersion = get(LANGUAGE_VERSION)?.value }
    if (API_VERSION in optionsMap) { arguments.apiVersion = get(API_VERSION)?.value }
    if (KOTLIN_HOME in optionsMap) { arguments.kotlinHome = get(KOTLIN_HOME) }
    if (PROGRESSIVE in optionsMap) { arguments.progressiveMode = get(PROGRESSIVE) }
    if (SCRIPT in optionsMap) { arguments.script = get(SCRIPT) }
    if (XREPL in optionsMap) { arguments.repl = get(XREPL) }
    if (OPT_IN in optionsMap) { arguments.optIn = get(OPT_IN) }
    if (XNO_INLINE in optionsMap) { arguments.noInline = get(XNO_INLINE) }
    if (XSKIP_METADATA_VERSION_CHECK in optionsMap) { arguments.skipMetadataVersionCheck = get(XSKIP_METADATA_VERSION_CHECK) }
    if (XSKIP_PRERELEASE_CHECK in optionsMap) { arguments.skipPrereleaseCheck = get(XSKIP_PRERELEASE_CHECK) }
    if (XALLOW_KOTLIN_PACKAGE in optionsMap) { arguments.allowKotlinPackage = get(XALLOW_KOTLIN_PACKAGE) }
    if (XSTDLIB_COMPILATION in optionsMap) { arguments.stdlibCompilation = get(XSTDLIB_COMPILATION) }
    if (XREPORT_OUTPUT_FILES in optionsMap) { arguments.reportOutputFiles = get(XREPORT_OUTPUT_FILES) }
    if (XPLUGIN in optionsMap) { arguments.pluginClasspaths = get(XPLUGIN) }
    if (P in optionsMap) { arguments.pluginOptions = get(P) }
    if (XCOMPILER_PLUGIN in optionsMap) { arguments.pluginConfigurations = get(XCOMPILER_PLUGIN) }
    if (XMULTI_PLATFORM in optionsMap) { arguments.multiPlatform = get(XMULTI_PLATFORM) }
    if (XNO_CHECK_ACTUAL in optionsMap) { arguments.noCheckActual = get(XNO_CHECK_ACTUAL) }
    if (XINTELLIJ_PLUGIN_ROOT in optionsMap) { arguments.intellijPluginRoot = get(XINTELLIJ_PLUGIN_ROOT) }
    if (XNEW_INFERENCE in optionsMap) { arguments.newInference = get(XNEW_INFERENCE) }
    if (XINLINE_CLASSES in optionsMap) { arguments.inlineClasses = get(XINLINE_CLASSES) }
    if (XREPORT_PERF in optionsMap) { arguments.reportPerf = get(XREPORT_PERF) }
    if (XDUMP_PERF in optionsMap) { arguments.dumpPerf = get(XDUMP_PERF) }
    if (XMETADATA_VERSION in optionsMap) { arguments.metadataVersion = get(XMETADATA_VERSION) }
    if (XCOMMON_SOURCES in optionsMap) { arguments.commonSources = get(XCOMMON_SOURCES) }
    if (XLIST_PHASES in optionsMap) { arguments.listPhases = get(XLIST_PHASES) }
    if (XDISABLE_PHASES in optionsMap) { arguments.disablePhases = get(XDISABLE_PHASES) }
    if (XVERBOSE_PHASES in optionsMap) { arguments.verbosePhases = get(XVERBOSE_PHASES) }
    if (XPHASES_TO_DUMP_BEFORE in optionsMap) { arguments.phasesToDumpBefore = get(XPHASES_TO_DUMP_BEFORE) }
    if (XPHASES_TO_DUMP_AFTER in optionsMap) { arguments.phasesToDumpAfter = get(XPHASES_TO_DUMP_AFTER) }
    if (XPHASES_TO_DUMP in optionsMap) { arguments.phasesToDump = get(XPHASES_TO_DUMP) }
    if (XDUMP_DIRECTORY in optionsMap) { arguments.dumpDirectory = get(XDUMP_DIRECTORY) }
    if (XDUMP_FQNAME in optionsMap) { arguments.dumpOnlyFqName = get(XDUMP_FQNAME) }
    if (XPHASES_TO_VALIDATE_BEFORE in optionsMap) { arguments.phasesToValidateBefore = get(XPHASES_TO_VALIDATE_BEFORE) }
    if (XPHASES_TO_VALIDATE_AFTER in optionsMap) { arguments.phasesToValidateAfter = get(XPHASES_TO_VALIDATE_AFTER) }
    if (XPHASES_TO_VALIDATE in optionsMap) { arguments.phasesToValidate = get(XPHASES_TO_VALIDATE) }
    if (XVERIFY_IR in optionsMap) { arguments.verifyIr = get(XVERIFY_IR) }
    if (XVERIFY_IR_VISIBILITY in optionsMap) { arguments.verifyIrVisibility = get(XVERIFY_IR_VISIBILITY) }
    if (XPROFILE_PHASES in optionsMap) { arguments.profilePhases = get(XPROFILE_PHASES) }
    if (XCHECK_PHASE_CONDITIONS in optionsMap) { arguments.checkPhaseConditions = get(XCHECK_PHASE_CONDITIONS) }
    if (XUSE_K2 in optionsMap) { arguments.useK2 = get(XUSE_K2) }
    if (XUSE_FIR_EXPERIMENTAL_CHECKERS in optionsMap) { arguments.useFirExperimentalCheckers = get(XUSE_FIR_EXPERIMENTAL_CHECKERS) }
    if (XUSE_FIR_IC in optionsMap) { arguments.useFirIC = get(XUSE_FIR_IC) }
    if (XUSE_FIR_LT in optionsMap) { arguments.useFirLT = get(XUSE_FIR_LT) }
    if (XMETADATA_KLIB in optionsMap) { arguments.metadataKlib = get(XMETADATA_KLIB) }
    if (XDISABLE_DEFAULT_SCRIPTING_PLUGIN in optionsMap) { arguments.disableDefaultScriptingPlugin = get(XDISABLE_DEFAULT_SCRIPTING_PLUGIN) }
    if (XEXPLICIT_API in optionsMap) { arguments.explicitApi = get(XEXPLICIT_API).value }
    if (XXEXPLICIT_RETURN_TYPES in optionsMap) { arguments.explicitReturnTypes = get(XXEXPLICIT_RETURN_TYPES).value }
    if (XRETURN_VALUE_CHECKER in optionsMap) { arguments.returnValueChecker = get(XRETURN_VALUE_CHECKER).value }
    if (XSUPPRESS_VERSION_WARNINGS in optionsMap) { arguments.suppressVersionWarnings = get(XSUPPRESS_VERSION_WARNINGS) }
    if (XSUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR in optionsMap) { arguments.suppressApiVersionGreaterThanLanguageVersionError = get(XSUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR) }
    if (XEXPECT_ACTUAL_CLASSES in optionsMap) { arguments.expectActualClasses = get(XEXPECT_ACTUAL_CLASSES) }
    if (XCONSISTENT_DATA_CLASS_COPY_VISIBILITY in optionsMap) { arguments.consistentDataClassCopyVisibility = get(XCONSISTENT_DATA_CLASS_COPY_VISIBILITY) }
    if (XUNRESTRICTED_BUILDER_INFERENCE in optionsMap) { arguments.unrestrictedBuilderInference = get(XUNRESTRICTED_BUILDER_INFERENCE) }
    if (XCONTEXT_RECEIVERS in optionsMap) { arguments.contextReceivers = get(XCONTEXT_RECEIVERS) }
    if (XCONTEXT_PARAMETERS in optionsMap) { arguments.contextParameters = get(XCONTEXT_PARAMETERS) }
    if (XCONTEXT_SENSITIVE_RESOLUTION in optionsMap) { arguments.contextSensitiveResolution = get(XCONTEXT_SENSITIVE_RESOLUTION) }
    if (XNON_LOCAL_BREAK_CONTINUE in optionsMap) { arguments.nonLocalBreakContinue = get(XNON_LOCAL_BREAK_CONTINUE) }
    if (XDIRECT_JAVA_ACTUALIZATION in optionsMap) { arguments.directJavaActualization = get(XDIRECT_JAVA_ACTUALIZATION) }
    if (XMULTI_DOLLAR_INTERPOLATION in optionsMap) { arguments.multiDollarInterpolation = get(XMULTI_DOLLAR_INTERPOLATION) }
    if (XENABLE_INCREMENTAL_COMPILATION in optionsMap) { arguments.incrementalCompilation = get(XENABLE_INCREMENTAL_COMPILATION) }
    if (XRENDER_INTERNAL_DIAGNOSTIC_NAMES in optionsMap) { arguments.renderInternalDiagnosticNames = get(XRENDER_INTERNAL_DIAGNOSTIC_NAMES) }
    if (XALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS in optionsMap) { arguments.allowAnyScriptsInSourceRoots = get(XALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS) }
    if (XREPORT_ALL_WARNINGS in optionsMap) { arguments.reportAllWarnings = get(XREPORT_ALL_WARNINGS) }
    if (XFRAGMENTS in optionsMap) { arguments.fragments = get(XFRAGMENTS) }
    if (XFRAGMENT_SOURCES in optionsMap) { arguments.fragmentSources = get(XFRAGMENT_SOURCES) }
    if (XFRAGMENT_REFINES in optionsMap) { arguments.fragmentRefines = get(XFRAGMENT_REFINES) }
    if (XFRAGMENT_DEPENDENCY in optionsMap) { arguments.fragmentDependencies = get(XFRAGMENT_DEPENDENCY) }
    if (XSEPARATE_KMP_COMPILATION in optionsMap) { arguments.separateKmpCompilationScheme = get(XSEPARATE_KMP_COMPILATION) }
    if (XIGNORE_CONST_OPTIMIZATION_ERRORS in optionsMap) { arguments.ignoreConstOptimizationErrors = get(XIGNORE_CONST_OPTIMIZATION_ERRORS) }
    if (XDONT_WARN_ON_ERROR_SUPPRESSION in optionsMap) { arguments.dontWarnOnErrorSuppression = get(XDONT_WARN_ON_ERROR_SUPPRESSION) }
    if (XWHEN_GUARDS in optionsMap) { arguments.whenGuards = get(XWHEN_GUARDS) }
    if (XNESTED_TYPE_ALIASES in optionsMap) { arguments.nestedTypeAliases = get(XNESTED_TYPE_ALIASES) }
    if (XSUPPRESS_WARNING in optionsMap) { arguments.suppressedDiagnostics = get(XSUPPRESS_WARNING) }
    if (XWARNING_LEVEL in optionsMap) { arguments.warningLevels = get(XWARNING_LEVEL) }
    if (XANNOTATION_DEFAULT_TARGET in optionsMap) { arguments.annotationDefaultTarget = get(XANNOTATION_DEFAULT_TARGET) }
    if (XXDEBUG_LEVEL_COMPILER_CHECKS in optionsMap) { arguments.debugLevelCompilerChecks = get(XXDEBUG_LEVEL_COMPILER_CHECKS) }
    if (XANNOTATION_TARGET_ALL in optionsMap) { arguments.annotationTargetAll = get(XANNOTATION_TARGET_ALL) }
    if (XXLENIENT_MODE in optionsMap) { arguments.lenientMode = get(XXLENIENT_MODE) }
    return arguments
  }
}
