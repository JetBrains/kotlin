// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_REFINES_PATHS
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments

internal class MetadataArgumentsImpl : CommonCompilerArgumentsImpl(), MetadataArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: MetadataArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: MetadataArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: MetadataArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("CLASSPATH" in optionsMap) { arguments.classpath = get(CLASSPATH) }
    if ("MODULE_NAME" in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if ("X_FRIEND_PATHS" in optionsMap) { arguments.friendPaths = get(X_FRIEND_PATHS) }
    if ("X_REFINES_PATHS" in optionsMap) { arguments.refinesPaths = get(X_REFINES_PATHS) }
    return arguments
  }

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
    public val CLASSPATH: MetadataArgument<String?> = MetadataArgument("CLASSPATH")

    public val MODULE_NAME: MetadataArgument<String?> = MetadataArgument("MODULE_NAME")

    public val X_FRIEND_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("X_FRIEND_PATHS")

    public val X_REFINES_PATHS: MetadataArgument<Array<String>?> =
        MetadataArgument("X_REFINES_PATHS")
  }
}
