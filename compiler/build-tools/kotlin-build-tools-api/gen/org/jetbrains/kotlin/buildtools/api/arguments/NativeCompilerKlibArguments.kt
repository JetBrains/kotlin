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
public interface NativeCompilerKlibArguments : NativeCompilerArguments,
    CommonKlibBasedArgumentsKlibArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: NativeCompilerKlibArgument<V>): V

  /**
   * An option for configuring [NativeCompilerKlibArguments].
   *
   * @see get
   * @see set    
   */
  public class NativeCompilerKlibArgument<V>(
    public val id: String,
    public val availableSinceVersion: KotlinReleaseVersion,
  )

  /**
   * A builder for [NativeCompilerKlibArguments].
   */
  public interface Builder : NativeCompilerArguments.Builder,
      CommonKlibBasedArgumentsKlibArguments.Builder {
    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> `get`(key: NativeCompilerKlibArgument<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> `set`(key: NativeCompilerKlibArgument<V>, `value`: V)

    /**
     * Constructs a new immutable [NativeCompilerKlibArguments] instance with the options set in this builder.
     */
    override fun build(): NativeCompilerKlibArguments
  }

  public companion object {
    /**
     * Save a klib that only contains the public ABI to the given path.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_HEADER_KLIB_PATH: NativeCompilerKlibArgument<String?> =
        NativeCompilerKlibArgument("X_HEADER_KLIB_PATH", KotlinReleaseVersion(1, 9, 24))

    /**
     * Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MANIFEST_NATIVE_TARGETS: NativeCompilerKlibArgument<Array<String>?> =
        NativeCompilerKlibArgument("X_MANIFEST_NATIVE_TARGETS", KotlinReleaseVersion(2, 0, 20))

    /**
     * Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REFINES_PATHS: NativeCompilerKlibArgument<Array<String>?> =
        NativeCompilerKlibArgument("X_REFINES_PATHS", KotlinReleaseVersion(1, 8, 20))

    /**
     * A short name used to denote this library in the IDE and in a generated Objective-C header.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SHORT_MODULE_NAME: NativeCompilerKlibArgument<String?> =
        NativeCompilerKlibArgument("X_SHORT_MODULE_NAME", KotlinReleaseVersion(1, 5, 20))

    /**
     * Write file containing the paths of dependencies used during klib compilation to the provided path
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO: NativeCompilerKlibArgument<String?> =
        NativeCompilerKlibArgument("X_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO", KotlinReleaseVersion(2, 1, 20))

    /**
     * Pack the given external binary into the klib.
     */
    @JvmField
    public val INCLUDE_BINARY: NativeCompilerKlibArgument<Array<String>?> =
        NativeCompilerKlibArgument("INCLUDE_BINARY", KotlinReleaseVersion(1, 5, 20))

    /**
     * The library version.
     *
     * Deprecated in Kotlin version 2.0.20.
     *
     * Removed in Kotlin version 2.4.20.
     */
    @JvmField
    @RemovedCompilerArgument
    public val LIBRARY_VERSION: NativeCompilerKlibArgument<String?> =
        NativeCompilerKlibArgument("LIBRARY_VERSION", KotlinReleaseVersion(1, 5, 20))

    /**
     * Provide a manifest addend file.
     */
    @JvmField
    public val MANIFEST: NativeCompilerKlibArgument<String?> =
        NativeCompilerKlibArgument("MANIFEST", KotlinReleaseVersion(1, 5, 20))

    /**
     * Don't pack the library into a klib file.
     */
    @JvmField
    public val NOPACK: NativeCompilerKlibArgument<Boolean> =
        NativeCompilerKlibArgument("NOPACK", KotlinReleaseVersion(1, 5, 20))
  }
}
