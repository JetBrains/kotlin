// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.DuplicatedUniqueNameStrategy as InternalArgumentsEnumsDuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.KlibIrInlinerMode as InternalArgumentsEnumsKlibIrInlinerMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.PartialLinkageLogLevel as InternalArgumentsEnumsPartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.PartialLinkageMode as InternalArgumentsEnumsPartialLinkageMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.DuplicatedUniqueNameStrategy as ApiArgumentsEnumsDuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KlibIrInlinerMode as ApiArgumentsEnumsKlibIrInlinerMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel as ApiArgumentsEnumsPartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageMode as ApiArgumentsEnumsPartialLinkageMode

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object CommonKlibBasedArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is InternalArgumentsEnumsDuplicatedUniqueNameStrategy -> value.toApiEnum<ApiArgumentsEnumsDuplicatedUniqueNameStrategy>()
    is InternalArgumentsEnumsKlibIrInlinerMode -> value.toApiEnum<ApiArgumentsEnumsKlibIrInlinerMode>()
    is InternalArgumentsEnumsPartialLinkageMode -> value.toApiEnum<ApiArgumentsEnumsPartialLinkageMode>()
    is InternalArgumentsEnumsPartialLinkageLogLevel -> value.toApiEnum<ApiArgumentsEnumsPartialLinkageLogLevel>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsDuplicatedUniqueNameStrategy -> value.toImplEnum<InternalArgumentsEnumsDuplicatedUniqueNameStrategy>()
    is ApiArgumentsEnumsKlibIrInlinerMode -> value.toImplEnum<InternalArgumentsEnumsKlibIrInlinerMode>()
    is ApiArgumentsEnumsPartialLinkageMode -> value.toImplEnum<InternalArgumentsEnumsPartialLinkageMode>()
    is ApiArgumentsEnumsPartialLinkageLogLevel -> value.toImplEnum<InternalArgumentsEnumsPartialLinkageLogLevel>()
    else -> value
  }
}
