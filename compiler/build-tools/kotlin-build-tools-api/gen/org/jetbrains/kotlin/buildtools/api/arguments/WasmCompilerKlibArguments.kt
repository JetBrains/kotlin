// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WasmTarget

/**
 * @since 2.4.20
 */
public interface WasmCompilerKlibArguments : WasmCompilerArguments,
    CommonJsAndWasmCompilerKlibArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: WasmCompilerKlibArgument<V>): V

  /**
   * An option for configuring [WasmCompilerKlibArguments].
   *
   * @see get
   * @see set    
   */
  public class WasmCompilerKlibArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [WasmCompilerKlibArguments].
   */
  public interface Builder : WasmCompilerArguments.Builder,
      CommonJsAndWasmCompilerKlibArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: WasmCompilerKlibArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: WasmCompilerKlibArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [WasmCompilerKlibArguments] instance with the options set in this builder.
     */
    public fun build(): WasmCompilerKlibArguments
  }

  public companion object {
    /**
     * Use the WebAssembly compiler backend.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     *
     * Deprecated in Kotlin version 2.4.0.
     */
    @JvmField
    @ExperimentalCompilerArgument
    @DeprecatedCompilerArgument
    public val X_WASM: WasmCompilerKlibArgument<Boolean> =
        WasmCompilerKlibArgument("X_WASM", KotlinReleaseVersion(2, 1, 20))

    /**
     * Set up the Wasm target (wasm-js or wasm-wasi).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_TARGET: WasmCompilerKlibArgument<WasmTarget?> =
        WasmCompilerKlibArgument("X_WASM_TARGET", KotlinReleaseVersion(2, 1, 20))

    /**
     * Enable support for 'KClass.qualifiedName'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WASM_KCLASS_FQN: WasmCompilerKlibArgument<Boolean> =
        WasmCompilerKlibArgument("X_WASM_KCLASS_FQN", KotlinReleaseVersion(2, 1, 20))
  }
}
