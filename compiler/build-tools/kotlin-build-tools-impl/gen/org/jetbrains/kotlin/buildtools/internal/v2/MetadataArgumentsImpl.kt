package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.MetadataArguments
import org.jetbrains.kotlin.buildtools.api.v2.MetadataArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.v2.MetadataArguments.Companion.D
import org.jetbrains.kotlin.buildtools.api.v2.MetadataArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.v2.MetadataArguments.Companion.XFRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.v2.MetadataArguments.Companion.XLEGACY_METADATA_JAR_K2
import org.jetbrains.kotlin.buildtools.api.v2.MetadataArguments.Companion.XREFINES_PATHS
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments

public class MetadataArgumentsImpl : CommonCompilerArgumentsImpl(), MetadataArguments {
  private val optionsMap: MutableMap<MetadataArguments.MetadataArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    if (D in optionsMap) { arguments.destination = get(D) }
    if (CLASSPATH in optionsMap) { arguments.classpath = get(CLASSPATH) }
    if (MODULE_NAME in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if (XFRIEND_PATHS in optionsMap) { arguments.friendPaths = get(XFRIEND_PATHS)?.map{ it.toString() }?.toTypedArray() }
    if (XREFINES_PATHS in optionsMap) { arguments.refinesPaths = get(XREFINES_PATHS)?.map{ it.toString() }?.toTypedArray() }
    if (XLEGACY_METADATA_JAR_K2 in optionsMap) { arguments.legacyMetadataJar = get(XLEGACY_METADATA_JAR_K2) }
    return arguments
  }
}
