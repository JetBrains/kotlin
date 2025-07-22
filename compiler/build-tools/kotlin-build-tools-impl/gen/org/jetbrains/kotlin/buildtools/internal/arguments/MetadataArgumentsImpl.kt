// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.D
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_REFINES_PATHS
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments

public class MetadataArgumentsImpl : CommonCompilerArgumentsImpl(), MetadataArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V = optionsMap[key.id] as V

  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("D" in optionsMap) { arguments.destination = get(D) }
    if ("CLASSPATH" in optionsMap) { arguments.classpath = get(CLASSPATH) }
    if ("MODULE_NAME" in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if ("X_FRIEND_PATHS" in optionsMap) { arguments.friendPaths = get(X_FRIEND_PATHS) }
    if ("X_REFINES_PATHS" in optionsMap) { arguments.refinesPaths = get(X_REFINES_PATHS) }
    return arguments
  }
}
