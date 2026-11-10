// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.CompilerOutputKind as InternalArgumentsEnumsCompilerOutputKind
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.EnableDisableMode as InternalArgumentsEnumsEnableDisableMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.KonanTarget as InternalArgumentsEnumsKonanTarget
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NativeAllocationMode as InternalArgumentsEnumsNativeAllocationMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NativeGcMode as InternalArgumentsEnumsNativeGcMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NativeMemoryModel as InternalArgumentsEnumsNativeMemoryModel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.CompilerOutputKind as ApiArgumentsEnumsCompilerOutputKind
import org.jetbrains.kotlin.buildtools.api.arguments.enums.EnableDisableMode as ApiArgumentsEnumsEnableDisableMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget as ApiArgumentsEnumsKonanTarget
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NativeAllocationMode as ApiArgumentsEnumsNativeAllocationMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NativeGcMode as ApiArgumentsEnumsNativeGcMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NativeMemoryModel as ApiArgumentsEnumsNativeMemoryModel

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object NativeArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is InternalArgumentsEnumsEnableDisableMode -> value.toApiEnum<ApiArgumentsEnumsEnableDisableMode>()
    is InternalArgumentsEnumsNativeAllocationMode -> value.toApiEnum<ApiArgumentsEnumsNativeAllocationMode>()
    is InternalArgumentsEnumsNativeGcMode -> value.toApiEnum<ApiArgumentsEnumsNativeGcMode>()
    is InternalArgumentsEnumsKonanTarget -> value.toApiEnum<ApiArgumentsEnumsKonanTarget>()
    is InternalArgumentsEnumsNativeMemoryModel -> value.toApiEnum<ApiArgumentsEnumsNativeMemoryModel>()
    is InternalArgumentsEnumsCompilerOutputKind -> value.toApiEnum<ApiArgumentsEnumsCompilerOutputKind>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsEnableDisableMode -> value.toImplEnum<InternalArgumentsEnumsEnableDisableMode>()
    is ApiArgumentsEnumsNativeAllocationMode -> value.toImplEnum<InternalArgumentsEnumsNativeAllocationMode>()
    is ApiArgumentsEnumsNativeGcMode -> value.toImplEnum<InternalArgumentsEnumsNativeGcMode>()
    is ApiArgumentsEnumsKonanTarget -> value.toImplEnum<InternalArgumentsEnumsKonanTarget>()
    is ApiArgumentsEnumsNativeMemoryModel -> value.toImplEnum<InternalArgumentsEnumsNativeMemoryModel>()
    is ApiArgumentsEnumsCompilerOutputKind -> value.toImplEnum<InternalArgumentsEnumsCompilerOutputKind>()
    else -> value
  }
}
