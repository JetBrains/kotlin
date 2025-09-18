// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode

/**
 * @since 2.3.0
 */
public interface CommonCompilerArguments : CommonToolArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V

  /**
   * Set the [value] for option specified by [key], overriding any previous value for that option.
   */
  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V)

  /**
   * Check if an option specified by [key] has a value set.
   *
   * Note: trying to read an option (by using [get]) that has not been set will result in an exception.
   *
   * @return true if the option has a value set, false otherwise
   */
  public operator fun contains(key: CommonCompilerArgument<*>): Boolean

  /**
   * Base class for [CommonCompilerArguments] options.
   *
   * @see get
   * @see set    
   */
  public class CommonCompilerArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  public companion object {
    /**
     * Provide source compatibility with the specified version of Kotlin.
     */
    @JvmField
    public val LANGUAGE_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("LANGUAGE_VERSION", KotlinReleaseVersion(1, 0, 3))

    /**
     * Allow using declarations from only the specified version of bundled libraries.
     */
    @JvmField
    public val API_VERSION: CommonCompilerArgument<KotlinVersion?> =
        CommonCompilerArgument("API_VERSION", KotlinReleaseVersion(1, 0, 5))

    /**
     * Path to the Kotlin compiler home directory used for the discovery of runtime libraries.
     */
    @JvmField
    public val KOTLIN_HOME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("KOTLIN_HOME", KotlinReleaseVersion(1, 1, 50))

    /**
     * Enable progressive compiler mode.
     * In this mode, deprecations and bug fixes for unstable code take effect immediately
     * instead of going through a graceful migration cycle.
     * Code written in progressive mode is backward compatible; however, code written without
     * progressive mode enabled may cause compilation errors in progressive mode.
     */
    @JvmField
    public val PROGRESSIVE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("PROGRESSIVE", KotlinReleaseVersion(1, 2, 50))

    /**
     * Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.
     */
    @JvmField
    public val OPT_IN: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("OPT_IN", KotlinReleaseVersion(1, 4, 0))

    /**
     * Disable method inlining.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_INLINE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NO_INLINE", KotlinReleaseVersion(1, 0, 0))

    /**
     * Allow loading classes with bad metadata versions and pre-release classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SKIP_METADATA_VERSION_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_METADATA_VERSION_CHECK", KotlinReleaseVersion(1, 1, 2))

    /**
     * Allow loading pre-release classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SKIP_PRERELEASE_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_PRERELEASE_CHECK", KotlinReleaseVersion(1, 4, 0))

    /**
     * Report the source-to-output file mapping.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REPORT_OUTPUT_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_OUTPUT_FILES", KotlinReleaseVersion(1, 1, 3))

    /**
     * Specify an execution order constraint for compiler plugins.
     * Order constraint can be specified using the 'pluginId' of compiler plugins.
     * The first specified plugin will be executed before the second plugin.
     * Multiple constraints can be specified by repeating this option. Cycles in constraints will cause an error.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_COMPILER_PLUGIN_ORDER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_COMPILER_PLUGIN_ORDER", KotlinReleaseVersion(2, 3, 0))

    /**
     * Enable the new experimental generic type inference algorithm.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NEW_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NEW_INFERENCE", KotlinReleaseVersion(1, 2, 20))

    /**
     * Enable experimental inline classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_INLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_INLINE_CLASSES", KotlinReleaseVersion(1, 3, 50))

    /**
     * Report detailed performance statistics.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REPORT_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_PERF", KotlinReleaseVersion(1, 2, 50))

    /**
     * Enable more detailed performance statistics (Experimental).
     * For Native, the performance report includes execution time and lines processed per second for every individual lowering.
     * For WASM and JS, the performance report includes execution time and lines per second for each lowering of the first stage of compilation.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DETAILED_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DETAILED_PERF", KotlinReleaseVersion(2, 3, 0))

    /**
     * Dump detailed performance statistics to the specified file in plain text, JSON or markdown format (it's detected by the file's extension).
     * Also, it supports the placeholder `*` and directory for generating file names based on the module being compiled and the current time stamp.
     * Example: `path/to/dir/​*.log` creates logs like `path/to/dir/my-module_2025-06-20-12-22-32.log` in plain text format, `path/to/dir/` creates logs like `path/to/dir/my-log_2025-06-20-12-22-32.json`.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_PERF: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_PERF", KotlinReleaseVersion(1, 2, 50))

    /**
     * Change the metadata version of the generated binary files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_METADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_METADATA_VERSION", KotlinReleaseVersion(1, 2, 70))

    /**
     * List backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LIST_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_LIST_PHASES", KotlinReleaseVersion(1, 3, 20))

    /**
     * Disable backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DISABLE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_DISABLE_PHASES", KotlinReleaseVersion(1, 3, 20))

    /**
     * Be verbose while performing the given backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERBOSE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_VERBOSE_PHASES", KotlinReleaseVersion(1, 3, 20))

    /**
     * Dump the backend's state before these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_DUMP_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_BEFORE", KotlinReleaseVersion(1, 3, 20))

    /**
     * Dump the backend's state after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_DUMP_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_AFTER", KotlinReleaseVersion(1, 3, 20))

    /**
     * Dump the backend's state both before and after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_DUMP: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP", KotlinReleaseVersion(1, 3, 20))

    /**
     * Dump the backend state into this directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_DIRECTORY: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_DIRECTORY", KotlinReleaseVersion(1, 3, 50))

    /**
     * Dump the declaration with the given FqName.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_FQNAME", KotlinReleaseVersion(1, 3, 50))

    /**
     * Validate the backend's state before these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_VALIDATE_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_BEFORE", KotlinReleaseVersion(1, 3, 40))

    /**
     * Validate the backend's state after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_VALIDATE_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_AFTER", KotlinReleaseVersion(1, 3, 40))

    /**
     * Validate the backend's state both before and after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_VALIDATE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE", KotlinReleaseVersion(1, 3, 40))

    /**
     * IR verification mode (no verification by default).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_IR: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_VERIFY_IR", KotlinReleaseVersion(2, 0, 20))

    /**
     * Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_IR_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_VERIFY_IR_VISIBILITY", KotlinReleaseVersion(2, 0, 20))

    /**
     * Profile backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PROFILE_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_PROFILE_PHASES", KotlinReleaseVersion(1, 3, 20))

    /**
     * Check pre- and postconditions of IR lowering phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CHECK_PHASE_CONDITIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CHECK_PHASE_CONDITIONS", KotlinReleaseVersion(1, 3, 40))

    /**
     * Enable experimental frontend IR checkers that are not yet ready for production.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.2.20.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @DeprecatedCompilerArgument
    public val X_USE_FIR_EXPERIMENTAL_CHECKERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_EXPERIMENTAL_CHECKERS", KotlinReleaseVersion(2, 1, 0))

    /**
     * Compile using frontend IR internal incremental compilation.
     * Warning: This feature is not yet production-ready.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_FIR_IC: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_IC", KotlinReleaseVersion(1, 7, 0))

    /**
     * Compile using the LightTree parser with the frontend IR.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_FIR_LT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_LT", KotlinReleaseVersion(1, 7, 0))

    /**
     * Produce a klib that only contains the metadata of declarations.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_METADATA_KLIB: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_METADATA_KLIB", KotlinReleaseVersion(2, 0, 0))

    /**
     * Don't enable the scripting plugin by default.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN", KotlinReleaseVersion(1, 3, 70))

    /**
     * Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
     * Use the 'warning' level to issue warnings instead of errors.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPLICIT_API: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("X_EXPLICIT_API", KotlinReleaseVersion(1, 3, 70))

    /**
     * Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RETURN_VALUE_CHECKER: CommonCompilerArgument<ReturnValueCheckerMode> =
        CommonCompilerArgument("X_RETURN_VALUE_CHECKER", KotlinReleaseVersion(2, 2, 0))

    /**
     * Suppress warnings about outdated, inconsistent, or experimental language or API versions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPRESS_VERSION_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_VERSION_WARNINGS", KotlinReleaseVersion(1, 5, 0))

    /**
     * Suppress error about API version greater than language version.
     * Warning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR:
        CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR", KotlinReleaseVersion(2, 0, 0))

    /**
     * 'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
     * Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPECT_ACTUAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPECT_ACTUAL_CLASSES", KotlinReleaseVersion(1, 9, 20))

    /**
     * The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY", KotlinReleaseVersion(2, 0, 20))

    /**
     * Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_UNRESTRICTED_BUILDER_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_UNRESTRICTED_BUILDER_INFERENCE", KotlinReleaseVersion(1, 5, 30))

    /**
     * Enable experimental context receivers.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONTEXT_RECEIVERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_RECEIVERS", KotlinReleaseVersion(1, 6, 20))

    /**
     * Enable experimental context parameters.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONTEXT_PARAMETERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_PARAMETERS", KotlinReleaseVersion(2, 1, 20))

    /**
     * Enable experimental context-sensitive resolution.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONTEXT_SENSITIVE_RESOLUTION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_SENSITIVE_RESOLUTION", KotlinReleaseVersion(2, 2, 0))

    /**
     * Enable experimental non-local break and continue.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NON_LOCAL_BREAK_CONTINUE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NON_LOCAL_BREAK_CONTINUE", KotlinReleaseVersion(2, 1, 0))

    /**
     * Enable `when` exhaustiveness improvements that rely on data-flow analysis.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DATA_FLOW_BASED_EXHAUSTIVENESS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DATA_FLOW_BASED_EXHAUSTIVENESS", KotlinReleaseVersion(2, 2, 20))

    /**
     * Enable experimental language support for explicit backing fields.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPLICIT_BACKING_FIELDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPLICIT_BACKING_FIELDS", KotlinReleaseVersion(2, 3, 0))

    /**
     * Enable experimental multi-dollar interpolation.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_DOLLAR_INTERPOLATION", KotlinReleaseVersion(2, 0, 20))

    /**
     * Render the internal names of warnings and errors.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RENDER_INTERNAL_DIAGNOSTIC_NAMES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES", KotlinReleaseVersion(1, 7, 0))

    /**
     * Allow compiling scripts along with regular Kotlin sources.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS", KotlinReleaseVersion(1, 7, 20))

    /**
     * Report all warnings even if errors are found.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REPORT_ALL_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_ALL_WARNINGS", KotlinReleaseVersion(2, 0, 0))

    /**
     * Declare common klib friend dependencies for the specific fragment.
     * This argument can be specified for any HMPP module except the platform leaf module: it takes dependencies from the platform specific friend module arguments.
     * The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
     *
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRAGMENT_FRIEND_DEPENDENCY: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_FRAGMENT_FRIEND_DEPENDENCY", KotlinReleaseVersion(2, 3, 0))

    /**
     * Ignore all compilation exceptions while optimizing some constant expressions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IGNORE_CONST_OPTIMIZATION_ERRORS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_IGNORE_CONST_OPTIMIZATION_ERRORS", KotlinReleaseVersion(1, 9, 0))

    /**
     * Don't report warnings when errors are suppressed. This only affects K2.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_WARN_ON_ERROR_SUPPRESSION", KotlinReleaseVersion(2, 0, 0))

    /**
     * Enable experimental language support for when guards.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WHEN_GUARDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_WHEN_GUARDS", KotlinReleaseVersion(2, 0, 20))

    /**
     * Enable experimental language support for nested type aliases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NESTED_TYPE_ALIASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NESTED_TYPE_ALIASES", KotlinReleaseVersion(2, 1, 20))

    /**
     * Suppress specified warning module-wide. This option is deprecated in favor of "-Xwarning-level" flag
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPRESS_WARNING: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_SUPPRESS_WARNING", KotlinReleaseVersion(2, 1, 0))

    /**
     * Set the severity of the given warning.
     * - `error` level raises the severity of a warning to error level (similar to -Werror but more granular)
     * - `disabled` level suppresses reporting of a warning (similar to -nowarn but more granular)
     * - `warning` level overrides -nowarn and -Werror for this specific warning (the warning will be reported/won't be considered as an error)
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WARNING_LEVEL: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_WARNING_LEVEL", KotlinReleaseVersion(2, 2, 0))

    /**
     * Change the default annotation targets for constructor properties:
     * -Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:';
     * -Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;
     * -Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';
     * default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ANNOTATION_DEFAULT_TARGET: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_ANNOTATION_DEFAULT_TARGET", KotlinReleaseVersion(2, 1, 20))

    /**
     * Enable experimental language support for @all: annotation use-site target.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ANNOTATION_TARGET_ALL", KotlinReleaseVersion(2, 1, 20))

    /**
     * Allow 'catch' parameters to have reified types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_REIFIED_TYPE_IN_CATCH: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_REIFIED_TYPE_IN_CATCH", KotlinReleaseVersion(2, 2, 20))

    /**
     * Allow contracts on some operators and accessors, and allow checks for erased types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS", KotlinReleaseVersion(2, 2, 20))

    /**
     * Allow contracts that specify a limited conditional returns postcondition.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS", KotlinReleaseVersion(2, 2, 20))

    /**
     * Allow contracts that specify a condition that holds true inside a lambda argument.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_HOLDSIN_CONTRACT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_HOLDSIN_CONTRACT", KotlinReleaseVersion(2, 2, 20))

    /**
     * Enables the following destructuring features:
     * -Xname-based-destructuring=only-syntax:   Enables syntax for positional destructuring with square brackets and the full form of name-based destructuring with parentheses;
     * -Xname-based-destructuring=name-mismatch: Reports warnings when short form positional destructuring of data classes uses names that don't match the property names;
     * -Xname-based-destructuring=complete:      Enables short-form name-based destructuring with parentheses;
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NAME_BASED_DESTRUCTURING: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_NAME_BASED_DESTRUCTURING", KotlinReleaseVersion(2, 3, 0))
  }
}
