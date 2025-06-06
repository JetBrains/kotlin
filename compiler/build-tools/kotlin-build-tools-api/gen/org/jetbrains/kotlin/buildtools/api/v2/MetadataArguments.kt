package org.jetbrains.kotlin.buildtools.api.v2

import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
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
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may` be changed in the future")
    public val XFRIEND_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("XFRIEND_PATHS")

    /**
     * Paths to output directories for refined modules (modules whose expects this module can actualize).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may` be changed in the future")
    public val XREFINES_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("XREFINES_PATHS")

    /**
     * Produce a legacy metadata jar instead of metadata klib. Suitable only for K2 compilation
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may` be changed in the future")
    public val XLEGACY_METADATA_JAR_K2: MetadataArgument<Boolean> =
        MetadataArgument("XLEGACY_METADATA_JAR_K2")
  }
}
