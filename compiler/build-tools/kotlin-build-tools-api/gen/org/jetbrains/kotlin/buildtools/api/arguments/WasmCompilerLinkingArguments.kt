// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import java.nio.`file`.Path
import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion

/**
 * @since 2.4.20
 */
public interface WasmCompilerLinkingArguments : WasmCompilerArguments,
    CommonJsAndWasmCompilerLinkingArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: WasmCompilerLinkingArgument<V>): V

  /**
   * An option for configuring [WasmCompilerLinkingArguments].
   *
   * @see get
   * @see set    
   */
  public class WasmCompilerLinkingArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [WasmCompilerLinkingArguments].
   */
  public interface Builder : WasmCompilerArguments.Builder,
      CommonJsAndWasmCompilerLinkingArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: WasmCompilerLinkingArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: WasmCompilerLinkingArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [WasmCompilerLinkingArguments] instance with the options set in this builder.
     */
    override fun build(): WasmCompilerLinkingArguments
  }

  public companion object {
    /**
     * Dump reachability information collected about declarations while performing DCE to a file. The format will be chosen automatically based on the file extension. Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, and plain text for all other file types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE: WasmCompilerLinkingArgument<Path?> =
        WasmCompilerLinkingArgument("X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE", KotlinReleaseVersion(2, 1, 20))

    /**
     * Dump the IR size of each declaration into a file. The format will be chosen automatically depending on the file extension. Supported output formats include JSON for .json, a JS const initialized with a plain object containing information for .js, and plain text for all other file types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE: WasmCompilerLinkingArgument<Path?> =
        WasmCompilerLinkingArgument("X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE", KotlinReleaseVersion(2, 1, 20))

    /**
     * Regenerate unchanged modules in multimodule IC.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_IC_GENERATE_UNCHANGED_MODULES: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_IC_GENERATE_UNCHANGED_MODULES", KotlinReleaseVersion(2, 4, 20))

    /**
     * Avoid optimizations that can break debugging.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_DEBUG_FRIENDLY: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_DEBUG_FRIENDLY", KotlinReleaseVersion(2, 1, 20))

    /**
     * Add debug info to the compiled WebAssembly module.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_DEBUG_INFO: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_DEBUG_INFO", KotlinReleaseVersion(2, 1, 20))

    /**
     * Generates devtools custom formatters (https://firefox-source-docs.mozilla.org/devtools-user/custom_formatters) for Kotlin/Wasm values
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_DEBUGGER_CUSTOM_FORMATTERS: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_DEBUGGER_CUSTOM_FORMATTERS", KotlinReleaseVersion(2, 1, 20))

    /**
     * Disable bounds check elimination for provably-safe array accesses in for-loops. Only effective when -Xwasm-enable-array-range-checks is also enabled.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION:
        WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION", KotlinReleaseVersion(2, 4, 0))

    /**
     * Turn on range checks for array access functions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_ENABLE_ARRAY_RANGE_CHECKS: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_ENABLE_ARRAY_RANGE_CHECKS", KotlinReleaseVersion(2, 1, 20))

    /**
     * Turn on asserts.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_ENABLE_ASSERTS: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_ENABLE_ASSERTS", KotlinReleaseVersion(2, 1, 20))

    /**
     * Compile modules in multi-module closed-world mode using module passed in `-include` argument as main module
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_GENERATE_CLOSED_WORLD_MULTIMODULE", KotlinReleaseVersion(2, 4, 0))

    /**
     * Generate DWARF debug information.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_GENERATE_DWARF: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_GENERATE_DWARF", KotlinReleaseVersion(2, 1, 20))

    /**
     * Generate a .wat file.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_GENERATE_WAT: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_GENERATE_WAT", KotlinReleaseVersion(2, 1, 20))

    /**
     * Compile only a module passed using `-include` option.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_INCLUDED_MODULE_ONLY: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_INCLUDED_MODULE_ONLY", KotlinReleaseVersion(2, 3, 0))

    /**
     * Prefix to use for internally generated local variables.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX: WasmCompilerLinkingArgument<String> =
        WasmCompilerLinkingArgument("X_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX", KotlinReleaseVersion(2, 4, 0))

    /**
     * Don't use WebAssembly.JSTag for throwing and catching exceptions
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_NO_JSTAG: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_NO_JSTAG", KotlinReleaseVersion(2, 2, 20))

    /**
     * Insert source mappings from libraries even if their sources are unavailable on the end-user machine.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES:
        WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_SOURCES", KotlinReleaseVersion(2, 1, 20))

    /**
     * Use an updated version of the exception proposal with try_table.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_USE_NEW_EXCEPTION_PROPOSAL: WasmCompilerLinkingArgument<Boolean?> =
        WasmCompilerLinkingArgument("X_WASM_USE_NEW_EXCEPTION_PROPOSAL", KotlinReleaseVersion(2, 1, 20))

    /**
     * Compile Kotlin Coroutines with WebAssembly Stack Switching Proposal
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_USE_STACK_SWITCHING_PROPOSAL: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_USE_STACK_SWITCHING_PROPOSAL", KotlinReleaseVersion(2, 4, 20))

    /**
     * Use traps instead of throwing exceptions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS: WasmCompilerLinkingArgument<Boolean> =
        WasmCompilerLinkingArgument("X_WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS", KotlinReleaseVersion(2, 1, 20))
  }
}
