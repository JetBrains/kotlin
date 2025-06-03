package org.jetbrains.kotlin.build.tools.api

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.build.tools.api.enums.ExplicitApiMode
import org.jetbrains.kotlin.build.tools.api.enums.KotlinVersion
import org.jetbrains.kotlin.build.tools.api.enums.ReturnValueCheckerMode

public open class CommonCompilerArguments : CommonToolArguments() {
  private val optionsMap: MutableMap<CommonCompilerArgument<*>, Any?> = mutableMapOf()

  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V? = optionsMap[key] as V?

  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V) {
    optionsMap[key] = `value`
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
    public val REPL: CommonCompilerArgument<Boolean> = CommonCompilerArgument("REPL")

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
    public val NO_INLINE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("NO_INLINE")

    /**
     * Allow loading classes with bad metadata versions and pre-release classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SKIP_METADATA_VERSION_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("SKIP_METADATA_VERSION_CHECK")

    /**
     * Allow loading pre-release classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SKIP_PRERELEASE_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("SKIP_PRERELEASE_CHECK")

    /**
     * Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ALLOW_KOTLIN_PACKAGE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("ALLOW_KOTLIN_PACKAGE")

    /**
     * Enables special features which are relevant only for stdlib compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val STDLIB_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("STDLIB_COMPILATION")

    /**
     * Report the source-to-output file mapping.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val REPORT_OUTPUT_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("REPORT_OUTPUT_FILES")

    /**
     * Load plugins from the given classpath.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PLUGIN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("PLUGIN")

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
    public val COMPILER_PLUGIN: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("COMPILER_PLUGIN")

    /**
     * Enable language support for multiplatform projects.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val MULTI_PLATFORM: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("MULTI_PLATFORM")

    /**
     * Do not check for the presence of the 'actual' modifier in multiplatform projects.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val NO_CHECK_ACTUAL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("NO_CHECK_ACTUAL")

    /**
     * Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val INTELLIJ_PLUGIN_ROOT: CommonCompilerArgument<String?> =
        CommonCompilerArgument("INTELLIJ_PLUGIN_ROOT")

    /**
     * Enable the new experimental generic type inference algorithm.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val NEW_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("NEW_INFERENCE")

    /**
     * Enable experimental inline classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val INLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("INLINE_CLASSES")

    /**
     * Report detailed performance statistics.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val REPORT_PERF: CommonCompilerArgument<Boolean> = CommonCompilerArgument("REPORT_PERF")

    /**
     * Dump detailed performance statistics to the specified file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DUMP_PERF: CommonCompilerArgument<String?> = CommonCompilerArgument("DUMP_PERF")

    /**
     * Change the metadata version of the generated binary files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val METADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("METADATA_VERSION")

    /**
     * Sources of the common module that need to be compiled together with this module in multiplatform mode.
     * They should be a subset of sources passed as free arguments.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val COMMON_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("COMMON_SOURCES")

    /**
     * List backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val LIST_PHASES: CommonCompilerArgument<Boolean> = CommonCompilerArgument("LIST_PHASES")

    /**
     * Disable backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DISABLE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("DISABLE_PHASES")

    /**
     * Be verbose while performing the given backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val VERBOSE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("VERBOSE_PHASES")

    /**
     * Dump the backend's state before these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PHASES_TO_DUMP_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("PHASES_TO_DUMP_BEFORE")

    /**
     * Dump the backend's state after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PHASES_TO_DUMP_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("PHASES_TO_DUMP_AFTER")

    /**
     * Dump the backend's state both before and after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PHASES_TO_DUMP: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("PHASES_TO_DUMP")

    /**
     * Dump the backend state into this directory.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DUMP_DIRECTORY: CommonCompilerArgument<String?> =
        CommonCompilerArgument("DUMP_DIRECTORY")

    /**
     * Dump the declaration with the given FqName.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DUMP_FQNAME: CommonCompilerArgument<String?> = CommonCompilerArgument("DUMP_FQNAME")

    /**
     * Validate the backend's state before these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PHASES_TO_VALIDATE_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("PHASES_TO_VALIDATE_BEFORE")

    /**
     * Validate the backend's state after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PHASES_TO_VALIDATE_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("PHASES_TO_VALIDATE_AFTER")

    /**
     * Validate the backend's state both before and after these phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PHASES_TO_VALIDATE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("PHASES_TO_VALIDATE")

    /**
     * IR verification mode (no verification by default).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val VERIFY_IR: CommonCompilerArgument<String?> = CommonCompilerArgument("VERIFY_IR")

    /**
     * Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val VERIFY_IR_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("VERIFY_IR_VISIBILITY")

    /**
     * Profile backend phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PROFILE_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("PROFILE_PHASES")

    /**
     * Check pre- and postconditions of IR lowering phases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CHECK_PHASE_CONDITIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("CHECK_PHASE_CONDITIONS")

    /**
     * Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val USE_K2: CommonCompilerArgument<Boolean> = CommonCompilerArgument("USE_K2")

    /**
     * Enable experimental frontend IR checkers that are not yet ready for production.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val USE_FIR_EXPERIMENTAL_CHECKERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("USE_FIR_EXPERIMENTAL_CHECKERS")

    /**
     * Compile using frontend IR internal incremental compilation.
     * Warning: This feature is not yet production-ready.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val USE_FIR_IC: CommonCompilerArgument<Boolean> = CommonCompilerArgument("USE_FIR_IC")

    /**
     * Compile using the LightTree parser with the frontend IR.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val USE_FIR_LT: CommonCompilerArgument<Boolean> = CommonCompilerArgument("USE_FIR_LT")

    /**
     * Produce a klib that only contains the metadata of declarations.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val METADATA_KLIB: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("METADATA_KLIB")

    /**
     * Don't enable the scripting plugin by default.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("DISABLE_DEFAULT_SCRIPTING_PLUGIN")

    /**
     * Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
     * Use the 'warning' level to issue warnings instead of errors.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val EXPLICIT_API: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("EXPLICIT_API")

    /**
     * Force the compiler to report errors on all public API declarations without an explicit return type.
     * Use the 'warning' level to issue warnings instead of errors.
     * This flag partially enables functionality of `-Xexplicit-api` flag, so please don't use them altogether
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEXPLICIT_RETURN_TYPES: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("XEXPLICIT_RETURN_TYPES")

    /**
     * Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val RETURN_VALUE_CHECKER: CommonCompilerArgument<ReturnValueCheckerMode> =
        CommonCompilerArgument("RETURN_VALUE_CHECKER")

    /**
     * Suppress warnings about outdated, inconsistent, or experimental language or API versions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SUPPRESS_VERSION_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("SUPPRESS_VERSION_WARNINGS")

    /**
     * Suppress error about API version greater than language version.
     * Warning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR:
        CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR")

    /**
     * 'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
     * Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val EXPECT_ACTUAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("EXPECT_ACTUAL_CLASSES")

    /**
     * The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CONSISTENT_DATA_CLASS_COPY_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("CONSISTENT_DATA_CLASS_COPY_VISIBILITY")

    /**
     * Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val UNRESTRICTED_BUILDER_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("UNRESTRICTED_BUILDER_INFERENCE")

    /**
     * Enable experimental context receivers.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CONTEXT_RECEIVERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("CONTEXT_RECEIVERS")

    /**
     * Enable experimental context parameters.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CONTEXT_PARAMETERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("CONTEXT_PARAMETERS")

    /**
     * Enable experimental context-sensitive resolution.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val CONTEXT_SENSITIVE_RESOLUTION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("CONTEXT_SENSITIVE_RESOLUTION")

    /**
     * Enable experimental non-local break and continue.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val NON_LOCAL_BREAK_CONTINUE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("NON_LOCAL_BREAK_CONTINUE")

    /**
     * Enable experimental direct Java actualization support.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DIRECT_JAVA_ACTUALIZATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("DIRECT_JAVA_ACTUALIZATION")

    /**
     * Enable experimental multi-dollar interpolation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val MULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("MULTI_DOLLAR_INTERPOLATION")

    /**
     * Enable incremental compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ENABLE_INCREMENTAL_COMPILATION: CommonCompilerArgument<Boolean?> =
        CommonCompilerArgument("ENABLE_INCREMENTAL_COMPILATION")

    /**
     * Render the internal names of warnings and errors.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val RENDER_INTERNAL_DIAGNOSTIC_NAMES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("RENDER_INTERNAL_DIAGNOSTIC_NAMES")

    /**
     * Allow compiling scripts along with regular Kotlin sources.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    /**
     * Report all warnings even if errors are found.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val REPORT_ALL_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("REPORT_ALL_WARNINGS")

    /**
     * Declare all known fragments of a multiplatform compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FRAGMENTS: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("FRAGMENTS")

    /**
     * Add sources to a specific fragment of a multiplatform compilation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FRAGMENT_SOURCES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("FRAGMENT_SOURCES")

    /**
     * Declare that <fromModuleName> refines <onModuleName> with the dependsOn/refines relation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FRAGMENT_REFINES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("FRAGMENT_REFINES")

    /**
     * Declare common klib dependencies for the specific fragment.
     * This argument is required for any HMPP module except the platform leaf module: it takes dependencies from -cp/-libraries.
     * The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FRAGMENT_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("FRAGMENT_DEPENDENCY")

    /**
     * Enables the separated compilation scheme, in which common source sets are analyzed against their own dependencies
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SEPARATE_KMP_COMPILATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("SEPARATE_KMP_COMPILATION")

    /**
     * Ignore all compilation exceptions while optimizing some constant expressions.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val IGNORE_CONST_OPTIMIZATION_ERRORS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("IGNORE_CONST_OPTIMIZATION_ERRORS")

    /**
     * Don't report warnings when errors are suppressed. This only affects K2.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val DONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("DONT_WARN_ON_ERROR_SUPPRESSION")

    /**
     * Enable experimental language support for when guards.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val WHEN_GUARDS: CommonCompilerArgument<Boolean> = CommonCompilerArgument("WHEN_GUARDS")

    /**
     * Enable experimental language support for nested type aliases.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val NESTED_TYPE_ALIASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("NESTED_TYPE_ALIASES")

    /**
     * Suppress specified warning module-wide. This option is deprecated in favor of "-Xwarning-level" flag
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val SUPPRESS_WARNING: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("SUPPRESS_WARNING")

    /**
     * Set the severity of the given warning.
     * - `error` level raises the severity of a warning to error level (similar to -Werror but more granular)
     * - `disabled` level suppresses reporting of a warning (similar to -nowarn but more granular)
     * - `warning` level overrides -nowarn and -Werror for this specific warning (the warning will be reported/won't be considered as an error)
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val WARNING_LEVEL: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("WARNING_LEVEL")

    /**
     * Change the default annotation targets for constructor properties:
     * -Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:';
     * -Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;
     * -Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';
     * default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ANNOTATION_DEFAULT_TARGET: CommonCompilerArgument<String?> =
        CommonCompilerArgument("ANNOTATION_DEFAULT_TARGET")

    /**
     * Enable debug level compiler checks. ATTENTION: these checks can slow compiler down or even crash it.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDEBUG_LEVEL_COMPILER_CHECKS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XDEBUG_LEVEL_COMPILER_CHECKS")

    /**
     * Enable experimental language support for @all: annotation use-site target.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val ANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("ANNOTATION_TARGET_ALL")

    /**
     * Lenient compiler mode. When actuals are missing, placeholder declarations are generated.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLENIENT_MODE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("XLENIENT_MODE")
  }
}
