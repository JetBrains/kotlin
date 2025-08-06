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
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.D
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_LEGACY_METADATA_JAR_K2
import org.jetbrains.kotlin.buildtools.`internal`.arguments.MetadataArgumentsImpl.Companion.X_REFINES_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings

internal class MetadataArgumentsImpl : CommonCompilerArgumentsImpl(), MetadataArguments {
  private val internalArguments: MutableSet<String> = mutableSetOf()

  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: MetadataArguments.MetadataArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: MetadataArguments.MetadataArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: MetadataArguments.MetadataArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: MetadataArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: MetadataArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: MetadataArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2MetadataCompilerArguments = K2MetadataCompilerArguments()): K2MetadataCompilerArguments {
    super.toCompilerArguments(arguments)
    try { if ("D" in optionsMap) { arguments.destination = get(D)} } catch (_: NoSuchMethodError) {}
    try { if ("CLASSPATH" in optionsMap) { arguments.classpath = get(CLASSPATH)} } catch (_: NoSuchMethodError) {}
    try { if ("MODULE_NAME" in optionsMap) { arguments.moduleName = get(MODULE_NAME)} } catch (_: NoSuchMethodError) {}
    try { if ("X_FRIEND_PATHS" in optionsMap) { arguments.friendPaths = get(X_FRIEND_PATHS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_REFINES_PATHS" in optionsMap) { arguments.refinesPaths = get(X_REFINES_PATHS)} } catch (_: NoSuchMethodError) {}
    try { if ("X_LEGACY_METADATA_JAR_K2" in optionsMap) { arguments.legacyMetadataJar = get(X_LEGACY_METADATA_JAR_K2)} } catch (_: NoSuchMethodError) {}
    arguments.internalArguments = parseCommandLineArguments<K2MetadataCompilerArguments>(internalArguments.toList()).internalArguments
    return arguments
  }

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2MetadataCompilerArguments = parseCommandLineArguments(arguments)
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings()
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: K2MetadataCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[D] = arguments.destination } catch (_: NoSuchMethodError) {}
    try { this[CLASSPATH] = arguments.classpath } catch (_: NoSuchMethodError) {}
    try { this[MODULE_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {}
    try { this[X_FRIEND_PATHS] = arguments.friendPaths } catch (_: NoSuchMethodError) {}
    try { this[X_REFINES_PATHS] = arguments.refinesPaths } catch (_: NoSuchMethodError) {}
    try { this[X_LEGACY_METADATA_JAR_K2] = arguments.legacyMetadataJar } catch (_: NoSuchMethodError) {}
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  public class MetadataArgument<V>(
    public val id: String,
  )

  public companion object {
    public val D: MetadataArgument<String?> = MetadataArgument("D")

    public val CLASSPATH: MetadataArgument<String?> = MetadataArgument("CLASSPATH")

    public val MODULE_NAME: MetadataArgument<String?> = MetadataArgument("MODULE_NAME")

    public val X_FRIEND_PATHS: MetadataArgument<Array<String>?> = MetadataArgument("X_FRIEND_PATHS")

    public val X_REFINES_PATHS: MetadataArgument<Array<String>?> =
        MetadataArgument("X_REFINES_PATHS")

    public val X_LEGACY_METADATA_JAR_K2: MetadataArgument<Boolean> =
        MetadataArgument("X_LEGACY_METADATA_JAR_K2")
  }
}
