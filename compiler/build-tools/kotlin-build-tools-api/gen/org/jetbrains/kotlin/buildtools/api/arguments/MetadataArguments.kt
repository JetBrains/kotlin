package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.jvm.JvmField

public interface MetadataArguments : CommonCompilerArguments {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: MetadataArgument<V>): V

  public operator fun <V> `set`(key: MetadataArgument<V>, `value`: V)

  public class MetadataArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Destination for generated .kotlin_metadata files.
     */
    @JvmField
    public val D: MetadataArgument<String?> = MetadataArgument("D")

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
