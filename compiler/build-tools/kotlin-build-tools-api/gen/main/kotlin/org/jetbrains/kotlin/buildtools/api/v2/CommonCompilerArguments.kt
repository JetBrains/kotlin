package org.jetbrains.kotlin.buildtools.api.v2

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.v2.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.v2.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.v2.enums.ReturnValueCheckerMode

public open class CommonCompilerArguments : CommonToolArguments() {
  private val optionsMap: MutableMap<CommonCompilerArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V = optionsMap[key] as V

  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  public fun toCompilerArguments(): org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments {
    val arguments = org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments()
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

  public class CommonCompilerArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Provide source compatibility with the specified version of Kotlin.
     */
    @JvmField
    public val LANGUAGE_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("LANGUAGE_VERSION")

    /**
     * Allow using declarations from only the specified version of bundled libraries.
     */
    @JvmField
    public val API_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("API_VERSION")

    /**
     * Path to the Kotlin compiler home directory used for the discovery of runtime libraries.
     */
    @JvmField
    public val KOTLIN_HOME: CommonCompilerArgument<String?> = CommonCompilerArgument("KOTLIN_HOME")

    /**
     * Enable progressive compiler mode.
     * In this mode, deprecations and bug fixes for unstable code take effect immediately
     * instead of going through a graceful migration cycle.
     * Code written in progressive mode is backward compatible; however, code written without
     * progressive mode enabled may cause compilation errors in progressive mode.
     */
    @JvmField
    public val PROGRESSIVE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("PROGRESSIVE")

    /**
     * Evaluate the given Kotlin script (*.kts) file.
     */
    @JvmField
    public val SCRIPT: CommonCompilerArgument<Boolean> = CommonCompilerArgument("SCRIPT")

    /**
     * Run Kotlin REPL (deprecated)
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XREPL: CommonCompilerArgument<Boolean> = CommonCompilerArgument("XREPL")

    /**
     * Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.
     */
    @JvmField
    public val OPT_IN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("OPT_IN")

    /**
     * Disable method inlining.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_INLINE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("XNO_INLINE")

    /**
     * Allow loading classes with bad metadata versions and pre-release classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSKIP_METADATA_VERSION_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XSKIP_METADATA_VERSION_CHECK")

    /**
     * Allow loading pre-release classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSKIP_PRERELEASE_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XSKIP_PRERELEASE_CHECK")

    /**
     * Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XALLOW_KOTLIN_PACKAGE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XALLOW_KOTLIN_PACKAGE")

    /**
     * Enables special features which are relevant only for stdlib compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSTDLIB_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XSTDLIB_COMPILATION")

    /**
     * Report the source-to-output file mapping.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XREPORT_OUTPUT_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XREPORT_OUTPUT_FILES")

    /**
     * Load plugins from the given classpath.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPLUGIN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("XPLUGIN")

    /**
     * Pass an option to a plugin.
     */
    @JvmField
    public val P: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("P")

    /**
     * Register a compiler plugin.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCOMPILER_PLUGIN: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XCOMPILER_PLUGIN")

    /**
     * Enable language support for multiplatform projects.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMULTI_PLATFORM: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XMULTI_PLATFORM")

    /**
     * Do not check for the presence of the 'actual' modifier in multiplatform projects.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_CHECK_ACTUAL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XNO_CHECK_ACTUAL")

    /**
     * Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XINTELLIJ_PLUGIN_ROOT: CommonCompilerArgument<String?> =
        CommonCompilerArgument("XINTELLIJ_PLUGIN_ROOT")

    /**
     * Enable the new experimental generic type inference algorithm.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNEW_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XNEW_INFERENCE")

    /**
     * Enable experimental inline classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XINLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XINLINE_CLASSES")

    /**
     * Report detailed performance statistics.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XREPORT_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XREPORT_PERF")

    /**
     * Dump detailed performance statistics to the specified file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDUMP_PERF: CommonCompilerArgument<String?> = CommonCompilerArgument("XDUMP_PERF")

    /**
     * Change the metadata version of the generated binary files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMETADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("XMETADATA_VERSION")

    /**
     * Sources of the common module that need to be compiled together with this module in multiplatform mode.
     * They should be a subset of sources passed as free arguments.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCOMMON_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XCOMMON_SOURCES")

    /**
     * List backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLIST_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XLIST_PHASES")

    /**
     * Disable backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDISABLE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XDISABLE_PHASES")

    /**
     * Be verbose while performing the given backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XVERBOSE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XVERBOSE_PHASES")

    /**
     * Dump the backend's state before these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPHASES_TO_DUMP_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XPHASES_TO_DUMP_BEFORE")

    /**
     * Dump the backend's state after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPHASES_TO_DUMP_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XPHASES_TO_DUMP_AFTER")

    /**
     * Dump the backend's state both before and after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPHASES_TO_DUMP: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XPHASES_TO_DUMP")

    /**
     * Dump the backend state into this directory.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDUMP_DIRECTORY: CommonCompilerArgument<String?> =
        CommonCompilerArgument("XDUMP_DIRECTORY")

    /**
     * Dump the declaration with the given FqName.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("XDUMP_FQNAME")

    /**
     * Validate the backend's state before these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPHASES_TO_VALIDATE_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XPHASES_TO_VALIDATE_BEFORE")

    /**
     * Validate the backend's state after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPHASES_TO_VALIDATE_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XPHASES_TO_VALIDATE_AFTER")

    /**
     * Validate the backend's state both before and after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPHASES_TO_VALIDATE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XPHASES_TO_VALIDATE")

    /**
     * IR verification mode (no verification by default).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XVERIFY_IR: CommonCompilerArgument<String?> = CommonCompilerArgument("XVERIFY_IR")

    /**
     * Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XVERIFY_IR_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XVERIFY_IR_VISIBILITY")

    /**
     * Profile backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPROFILE_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XPROFILE_PHASES")

    /**
     * Check pre- and postconditions of IR lowering phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCHECK_PHASE_CONDITIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XCHECK_PHASE_CONDITIONS")

    /**
     * Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_K2: CommonCompilerArgument<Boolean> = CommonCompilerArgument("XUSE_K2")

    /**
     * Enable experimental frontend IR checkers that are not yet ready for production.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_FIR_EXPERIMENTAL_CHECKERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XUSE_FIR_EXPERIMENTAL_CHECKERS")

    /**
     * Compile using frontend IR internal incremental compilation.
     * Warning: This feature is not yet production-ready.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_FIR_IC: CommonCompilerArgument<Boolean> = CommonCompilerArgument("XUSE_FIR_IC")

    /**
     * Compile using the LightTree parser with the frontend IR.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_FIR_LT: CommonCompilerArgument<Boolean> = CommonCompilerArgument("XUSE_FIR_LT")

    /**
     * Produce a klib that only contains the metadata of declarations.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMETADATA_KLIB: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XMETADATA_KLIB")

    /**
     * Don't enable the scripting plugin by default.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XDISABLE_DEFAULT_SCRIPTING_PLUGIN")

    /**
     * Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
     * Use the 'warning' level to issue warnings instead of errors.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEXPLICIT_API: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("XEXPLICIT_API")

    /**
     * Force the compiler to report errors on all public API declarations without an explicit return type.
     * Use the 'warning' level to issue warnings instead of errors.
     * This flag partially enables functionality of `-Xexplicit-api` flag, so please don't use them altogether
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XXEXPLICIT_RETURN_TYPES: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("XXEXPLICIT_RETURN_TYPES")

    /**
     * Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XRETURN_VALUE_CHECKER: CommonCompilerArgument<ReturnValueCheckerMode> =
        CommonCompilerArgument("XRETURN_VALUE_CHECKER")

    /**
     * Suppress warnings about outdated, inconsistent, or experimental language or API versions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSUPPRESS_VERSION_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XSUPPRESS_VERSION_WARNINGS")

    /**
     * Suppress error about API version greater than language version.
     * Warning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR:
        CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XSUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR")

    /**
     * 'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
     * Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEXPECT_ACTUAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XEXPECT_ACTUAL_CLASSES")

    /**
     * The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCONSISTENT_DATA_CLASS_COPY_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XCONSISTENT_DATA_CLASS_COPY_VISIBILITY")

    /**
     * Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUNRESTRICTED_BUILDER_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XUNRESTRICTED_BUILDER_INFERENCE")

    /**
     * Enable experimental context receivers.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCONTEXT_RECEIVERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XCONTEXT_RECEIVERS")

    /**
     * Enable experimental context parameters.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCONTEXT_PARAMETERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XCONTEXT_PARAMETERS")

    /**
     * Enable experimental context-sensitive resolution.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCONTEXT_SENSITIVE_RESOLUTION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XCONTEXT_SENSITIVE_RESOLUTION")

    /**
     * Enable experimental non-local break and continue.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNON_LOCAL_BREAK_CONTINUE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XNON_LOCAL_BREAK_CONTINUE")

    /**
     * Enable experimental direct Java actualization support.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDIRECT_JAVA_ACTUALIZATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XDIRECT_JAVA_ACTUALIZATION")

    /**
     * Enable experimental multi-dollar interpolation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XMULTI_DOLLAR_INTERPOLATION")

    /**
     * Enable incremental compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XENABLE_INCREMENTAL_COMPILATION: CommonCompilerArgument<Boolean?> =
        CommonCompilerArgument("XENABLE_INCREMENTAL_COMPILATION")

    /**
     * Render the internal names of warnings and errors.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XRENDER_INTERNAL_DIAGNOSTIC_NAMES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XRENDER_INTERNAL_DIAGNOSTIC_NAMES")

    /**
     * Allow compiling scripts along with regular Kotlin sources.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    /**
     * Report all warnings even if errors are found.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XREPORT_ALL_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XREPORT_ALL_WARNINGS")

    /**
     * Declare all known fragments of a multiplatform compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRAGMENTS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XFRAGMENTS")

    /**
     * Add sources to a specific fragment of a multiplatform compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRAGMENT_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XFRAGMENT_SOURCES")

    /**
     * Declare that <fromModuleName> refines <onModuleName> with the dependsOn/refines relation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRAGMENT_REFINES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XFRAGMENT_REFINES")

    /**
     * Declare common klib dependencies for the specific fragment.
     * This argument is required for any HMPP module except the platform leaf module: it takes dependencies from -cp/-libraries.
     * The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRAGMENT_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XFRAGMENT_DEPENDENCY")

    /**
     * Enables the separated compilation scheme, in which common source sets are analyzed against their own dependencies
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSEPARATE_KMP_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XSEPARATE_KMP_COMPILATION")

    /**
     * Ignore all compilation exceptions while optimizing some constant expressions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIGNORE_CONST_OPTIMIZATION_ERRORS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XIGNORE_CONST_OPTIMIZATION_ERRORS")

    /**
     * Don't report warnings when errors are suppressed. This only affects K2.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XDONT_WARN_ON_ERROR_SUPPRESSION")

    /**
     * Enable experimental language support for when guards.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWHEN_GUARDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XWHEN_GUARDS")

    /**
     * Enable experimental language support for nested type aliases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNESTED_TYPE_ALIASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XNESTED_TYPE_ALIASES")

    /**
     * Suppress specified warning module-wide. This option is deprecated in favor of "-Xwarning-level" flag
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSUPPRESS_WARNING: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XSUPPRESS_WARNING")

    /**
     * Set the severity of the given warning.
     * - `error` level raises the severity of a warning to error level (similar to -Werror but more granular)
     * - `disabled` level suppresses reporting of a warning (similar to -nowarn but more granular)
     * - `warning` level overrides -nowarn and -Werror for this specific warning (the warning will be reported/won't be considered as an error)
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XWARNING_LEVEL: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("XWARNING_LEVEL")

    /**
     * Change the default annotation targets for constructor properties:
     * -Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:';
     * -Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;
     * -Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';
     * default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XANNOTATION_DEFAULT_TARGET: CommonCompilerArgument<String?> =
        CommonCompilerArgument("XANNOTATION_DEFAULT_TARGET")

    /**
     * Enable debug level compiler checks. ATTENTION: these checks can slow compiler down or even crash it.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XXDEBUG_LEVEL_COMPILER_CHECKS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XXDEBUG_LEVEL_COMPILER_CHECKS")

    /**
     * Enable experimental language support for @all: annotation use-site target.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XANNOTATION_TARGET_ALL")

    /**
     * Lenient compiler mode. When actuals are missing, placeholder declarations are generated.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XXLENIENT_MODE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XXLENIENT_MODE")
  }
}
