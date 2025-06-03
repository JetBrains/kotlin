package org.jetbrains.kotlin.build.tools.api

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField

public class MetadataArguments : CommonCompilerArguments() {
  private val optionsMap: MutableMap<MetadataArgument<*>, Any?> = mutableMapOf()

  public operator fun <V> `get`(key: MetadataArgument<V>): V? = optionsMap[key] as V?

  public operator fun <V> `set`(key: MetadataArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

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
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val FRIEND_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("FRIEND_PATHS")

    /**
     * Paths to output directories for refined modules (modules whose expects this module can actualize).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val REFINES_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("REFINES_PATHS")

    /**
     * Produce a legacy metadata jar instead of metadata klib. Suitable only for K2 compilation
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val LEGACY_METADATA_JAR_K2: MetadataArgument<Boolean> =
        MetadataArgument("LEGACY_METADATA_JAR_K2")
  }
}
