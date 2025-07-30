// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
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

  public operator fun contains(key: CommonCompilerArgument<*>): Boolean

  /**
   * Base class for [CommonCompilerArguments] options.
   *
   * @see get
   * @see set    
   */
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
     * Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.
     */
    @JvmField
    public val OPT_IN: CommonCompilerArgument<Array<String>?> = CommonCompilerArgument("OPT_IN")

    /**
     * Disable method inlining.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_INLINE: CommonCompilerArgument<Boolean> = CommonCompilerArgument("X_NO_INLINE")

    /**
     * Allow loading classes with bad metadata versions and pre-release classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SKIP_METADATA_VERSION_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_METADATA_VERSION_CHECK")

    /**
     * Allow loading pre-release classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SKIP_PRERELEASE_CHECK: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SKIP_PRERELEASE_CHECK")

    /**
     * Report the source-to-output file mapping.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REPORT_OUTPUT_FILES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_OUTPUT_FILES")

    /**
     * Enable the new experimental generic type inference algorithm.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NEW_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NEW_INFERENCE")

    /**
     * Enable experimental inline classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_INLINE_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_INLINE_CLASSES")

    /**
     * Report detailed performance statistics.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REPORT_PERF: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_PERF")

    /**
     * Dump detailed performance statistics to the specified file in plain text, JSON or markdown format (it's detected by the file's extension).
     * Also, it supports the placeholder `*` and directory for generating file names based on the module being compiled and the current time stamp.
     * Example: `path/to/dir/​*.log` creates logs like `path/to/dir/my-module_2025-06-20-12-22-32.log` in plain text format, `path/to/dir/` creates logs like `path/to/dir/my-log_2025-06-20-12-22-32.json`.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_PERF: CommonCompilerArgument<String?> = CommonCompilerArgument("X_DUMP_PERF")

    /**
     * Change the metadata version of the generated binary files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_METADATA_VERSION: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_METADATA_VERSION")

    /**
     * List backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LIST_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_LIST_PHASES")

    /**
     * Disable backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DISABLE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_DISABLE_PHASES")

    /**
     * Be verbose while performing the given backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERBOSE_PHASES: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_VERBOSE_PHASES")

    /**
     * Dump the backend's state before these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_DUMP_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_BEFORE")

    /**
     * Dump the backend's state after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_DUMP_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP_AFTER")

    /**
     * Dump the backend's state both before and after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_DUMP: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_DUMP")

    /**
     * Dump the backend state into this directory.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_DIRECTORY: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_DIRECTORY")

    /**
     * Dump the declaration with the given FqName.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DUMP_FQNAME: CommonCompilerArgument<String?> =
        CommonCompilerArgument("X_DUMP_FQNAME")

    /**
     * Validate the backend's state before these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_VALIDATE_BEFORE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_BEFORE")

    /**
     * Validate the backend's state after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_VALIDATE_AFTER: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE_AFTER")

    /**
     * Validate the backend's state both before and after these phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PHASES_TO_VALIDATE: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_PHASES_TO_VALIDATE")

    /**
     * IR verification mode (no verification by default).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_IR: CommonCompilerArgument<String?> = CommonCompilerArgument("X_VERIFY_IR")

    /**
     * Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VERIFY_IR_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_VERIFY_IR_VISIBILITY")

    /**
     * Profile backend phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PROFILE_PHASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_PROFILE_PHASES")

    /**
     * Check pre- and postconditions of IR lowering phases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CHECK_PHASE_CONDITIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CHECK_PHASE_CONDITIONS")

    /**
     * Enable experimental frontend IR checkers that are not yet ready for production.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_FIR_EXPERIMENTAL_CHECKERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_EXPERIMENTAL_CHECKERS")

    /**
     * Compile using frontend IR internal incremental compilation.
     * Warning: This feature is not yet production-ready.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_FIR_IC: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_IC")

    /**
     * Compile using the LightTree parser with the frontend IR.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_FIR_LT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_USE_FIR_LT")

    /**
     * Produce a klib that only contains the metadata of declarations.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_METADATA_KLIB: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_METADATA_KLIB")

    /**
     * Don't enable the scripting plugin by default.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DISABLE_DEFAULT_SCRIPTING_PLUGIN: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN")

    /**
     * Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
     * Use the 'warning' level to issue warnings instead of errors.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPLICIT_API: CommonCompilerArgument<ExplicitApiMode> =
        CommonCompilerArgument("X_EXPLICIT_API")

    /**
     * Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RETURN_VALUE_CHECKER: CommonCompilerArgument<ReturnValueCheckerMode> =
        CommonCompilerArgument("X_RETURN_VALUE_CHECKER")

    /**
     * Suppress warnings about outdated, inconsistent, or experimental language or API versions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPRESS_VERSION_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_SUPPRESS_VERSION_WARNINGS")

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
        CommonCompilerArgument("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR")

    /**
     * 'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
     * Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPECT_ACTUAL_CLASSES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_EXPECT_ACTUAL_CLASSES")

    /**
     * The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY")

    /**
     * Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_UNRESTRICTED_BUILDER_INFERENCE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_UNRESTRICTED_BUILDER_INFERENCE")

    /**
     * Enable experimental context receivers.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONTEXT_RECEIVERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_RECEIVERS")

    /**
     * Enable experimental context parameters.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONTEXT_PARAMETERS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_PARAMETERS")

    /**
     * Enable experimental context-sensitive resolution.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CONTEXT_SENSITIVE_RESOLUTION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_CONTEXT_SENSITIVE_RESOLUTION")

    /**
     * Enable experimental non-local break and continue.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NON_LOCAL_BREAK_CONTINUE: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NON_LOCAL_BREAK_CONTINUE")

    /**
     * Enable `when` exhaustiveness improvements that rely on data-flow analysis.
     */
    @JvmField
    public val _XDATA_FLOW_BASED_EXHAUSTIVENESS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("_XDATA_FLOW_BASED_EXHAUSTIVENESS")

    /**
     * Enable experimental multi-dollar interpolation.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MULTI_DOLLAR_INTERPOLATION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_MULTI_DOLLAR_INTERPOLATION")

    /**
     * Render the internal names of warnings and errors.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_RENDER_INTERNAL_DIAGNOSTIC_NAMES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES")

    /**
     * Allow compiling scripts along with regular Kotlin sources.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    /**
     * Report all warnings even if errors are found.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REPORT_ALL_WARNINGS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_REPORT_ALL_WARNINGS")

    /**
     * Ignore all compilation exceptions while optimizing some constant expressions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IGNORE_CONST_OPTIMIZATION_ERRORS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_IGNORE_CONST_OPTIMIZATION_ERRORS")

    /**
     * Don't report warnings when errors are suppressed. This only affects K2.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DONT_WARN_ON_ERROR_SUPPRESSION: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_DONT_WARN_ON_ERROR_SUPPRESSION")

    /**
     * Enable experimental language support for when guards.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WHEN_GUARDS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_WHEN_GUARDS")

    /**
     * Enable experimental language support for nested type aliases.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NESTED_TYPE_ALIASES: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_NESTED_TYPE_ALIASES")

    /**
     * Suppress specified warning module-wide. This option is deprecated in favor of "-Xwarning-level" flag
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPRESS_WARNING: CommonCompilerArgument<Array<String>?> =
        CommonCompilerArgument("X_SUPPRESS_WARNING")

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
        CommonCompilerArgument("X_WARNING_LEVEL")

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
        CommonCompilerArgument("X_ANNOTATION_DEFAULT_TARGET")

    /**
     * Enable experimental language support for @all: annotation use-site target.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ANNOTATION_TARGET_ALL: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ANNOTATION_TARGET_ALL")

    /**
     * Allow 'catch' parameters to have reified types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_REIFIED_TYPE_IN_CATCH: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_REIFIED_TYPE_IN_CATCH")

    /**
     * Allow contracts on some operators and accessors, and allow checks for erased types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS")

    /**
     * Allow contracts that specify a limited conditional returns postcondition.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS")

    /**
     * Allow contracts that specify a condition that holds true inside a lambda argument.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_HOLDSIN_CONTRACT: CommonCompilerArgument<Boolean> =
        CommonCompilerArgument("X_ALLOW_HOLDSIN_CONTRACT")

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
        CommonCompilerArgument("X_NAME_BASED_DESTRUCTURING")
  }
}
