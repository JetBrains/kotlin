// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.OptIn
import kotlin.Suppress
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.MetadataTargetPlatform as InternalArgumentsEnumsMetadataTargetPlatform
import org.jetbrains.kotlin.buildtools.api.arguments.enums.MetadataTargetPlatform as ApiArgumentsEnumsMetadataTargetPlatform

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object MetadataArgumentValueAdapter {
  @Suppress("UNCHECKED_CAST")
  public fun <T> toApi(`value`: Any?): T = when (value) {
    is List<*> -> value.map { toApi<T>(it) }
    is InternalArgumentsEnumsMetadataTargetPlatform -> value.toApiEnum<ApiArgumentsEnumsMetadataTargetPlatform>()
    else -> value
  }
  as T

  @Suppress("UNCHECKED_CAST")
  public fun <T> toImpl(`value`: Any?): T = when (value) {
    is List<*> -> value.map { toImpl<T>(it) }
    is ApiArgumentsEnumsMetadataTargetPlatform -> value.toImplEnum<InternalArgumentsEnumsMetadataTargetPlatform>()
    else -> value
  }
  as T
}
