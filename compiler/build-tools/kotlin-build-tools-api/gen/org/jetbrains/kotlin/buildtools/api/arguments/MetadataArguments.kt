// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.jvm.JvmField

/**
 * @since 2.3.0
 */
public interface MetadataArguments : CommonCompilerArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: MetadataArgument<V>): V

  /**
   * Set the [value] for option specified by [key], overriding any previous value for that option.
   */
  public operator fun <V> `set`(key: MetadataArgument<V>, `value`: V)

  public operator fun contains(key: MetadataArgument<*>): Boolean

  /**
   * Base class for [MetadataArguments] options.
   *
   * @see get
   * @see set    
   */
  public class MetadataArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * List of directories and JAR/ZIP archives to search for user .kotlin_metadata files.
     */
    @JvmField
    public val CLASSPATH: MetadataArgument<String?> = MetadataArgument("CLASSPATH")

    /**
     * Name of the generated .kotlin_module file.
     */
    @JvmField
    public val MODULE_NAME: MetadataArgument<String?> = MetadataArgument("MODULE_NAME")

    /**
     * Paths to output directories for friend modules (modules whose internals should be visible).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("X_FRIEND_PATHS")

    /**
     * Paths to output directories for refined modules (modules whose expects this module can actualize).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_REFINES_PATHS: MetadataArgument<Array<String>?> =
        MetadataArgument("X_REFINES_PATHS")
  }
}
