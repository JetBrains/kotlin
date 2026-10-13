// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument

/**
 * @since 2.4.20
 */
public interface NativeCompilerArguments : CommonKlibBasedArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: NativeCompilerArgument<V>): V

  /**
   * An option for configuring [NativeCompilerArguments].
   *
   * @see get
   * @see set    
   */
  public class NativeCompilerArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [NativeCompilerArguments].
   */
  public interface Builder : CommonKlibBasedArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: NativeCompilerArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: NativeCompilerArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [NativeCompilerArguments] instance with the options set in this builder.
     *
     * @since 2.4.20
     */
    override fun build(): NativeCompilerArguments
  }

  public companion object {
    /**
     * Check dependencies and download the missing ones.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_CHECK_DEPENDENCIES: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("X_CHECK_DEPENDENCIES", KotlinReleaseVersion(1, 5, 20))

    /**
     * Export KDoc entries in the framework header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EXPORT_KDOC: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("X_EXPORT_KDOC", KotlinReleaseVersion(1, 5, 20))

    /**
     * Custom path to the location of konan distributions.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KONAN_DATA_DIR: NativeCompilerArgument<String?> =
        NativeCompilerArgument("X_KONAN_DATA_DIR", KotlinReleaseVersion(1, 9, 20))

    /**
     * Print files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRINT_FILES: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("X_PRINT_FILES", KotlinReleaseVersion(1, 5, 20))

    /**
     * Print IR.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PRINT_IR: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("X_PRINT_IR", KotlinReleaseVersion(1, 5, 20))

    /**
     * Paths to friend modules.
     */
    @JvmField
    public val FRIEND_MODULES: NativeCompilerArgument<String?> =
        NativeCompilerArgument("FRIEND_MODULES", KotlinReleaseVersion(1, 5, 20))

    /**
     * Link with the given library.
     */
    @JvmField
    public val LIBRARY: NativeCompilerArgument<Array<String>?> =
        NativeCompilerArgument("LIBRARY", KotlinReleaseVersion(1, 5, 20))

    /**
     * List available hardware targets.
     */
    @JvmField
    public val LIST_TARGETS: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("LIST_TARGETS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Specify a name for the compilation module.
     */
    @JvmField
    public val MODULE_NAME: NativeCompilerArgument<String?> =
        NativeCompilerArgument("MODULE_NAME", KotlinReleaseVersion(1, 5, 20))

    /**
     * Include the given native bitcode library.
     */
    @JvmField
    public val NATIVE_LIBRARY: NativeCompilerArgument<Array<String>?> =
        NativeCompilerArgument("NATIVE_LIBRARY", KotlinReleaseVersion(1, 5, 20))

    /**
     * Don't link the libraries from dist/klib automatically.
     */
    @JvmField
    public val NO_DEFAULT_LIBS: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("NO_DEFAULT_LIBS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Don't link endorsed libraries from the dist automatically.
     *
     * Deprecated in Kotlin version 1.9.20.
     *
     * Removed in Kotlin version 2.4.20.
     */
    @JvmField
    @RemovedCompilerArgument
    public val NO_ENDORSED_LIBS: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("NO_ENDORSED_LIBS", KotlinReleaseVersion(1, 5, 20))

    /**
     * Don't link with the stdlib.
     */
    @JvmField
    public val NOSTDLIB: NativeCompilerArgument<Boolean> =
        NativeCompilerArgument("NOSTDLIB", KotlinReleaseVersion(1, 5, 20))

    /**
     * Output name.
     */
    @JvmField
    public val OUTPUT: NativeCompilerArgument<String?> =
        NativeCompilerArgument("OUTPUT", KotlinReleaseVersion(1, 5, 20))

    /**
     * Specify the output file kind.
     */
    @JvmField
    public val PRODUCE: NativeCompilerArgument<String?> =
        NativeCompilerArgument("PRODUCE", KotlinReleaseVersion(1, 5, 20))

    /**
     * Set the hardware target.
     */
    @JvmField
    public val TARGET: NativeCompilerArgument<String?> =
        NativeCompilerArgument("TARGET", KotlinReleaseVersion(1, 5, 20))
  }
}
