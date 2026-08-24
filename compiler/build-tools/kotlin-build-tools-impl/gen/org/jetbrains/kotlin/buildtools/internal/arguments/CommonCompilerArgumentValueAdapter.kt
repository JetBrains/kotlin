// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.AnnotationDefaultTargetMode as InternalArgumentsEnumsAnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ExplicitApiMode as InternalArgumentsEnumsExplicitApiMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.HeaderMode as InternalArgumentsEnumsHeaderMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.KotlinVersion as InternalArgumentsEnumsKotlinVersion
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.NameBasedDestructuringMode as InternalArgumentsEnumsNameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ReturnValueCheckerMode as InternalArgumentsEnumsReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.VerifyIrMode as InternalArgumentsEnumsVerifyIrMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AnnotationDefaultTargetMode as ApiArgumentsEnumsAnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode as ApiArgumentsEnumsExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.HeaderMode as ApiArgumentsEnumsHeaderMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion as ApiArgumentsEnumsKotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NameBasedDestructuringMode as ApiArgumentsEnumsNameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode as ApiArgumentsEnumsReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode as ApiArgumentsEnumsVerifyIrMode

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object CommonCompilerArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is InternalArgumentsEnumsExplicitApiMode -> value.toApiEnum<ApiArgumentsEnumsExplicitApiMode>()
    is InternalArgumentsEnumsAnnotationDefaultTargetMode -> value.toApiEnum<ApiArgumentsEnumsAnnotationDefaultTargetMode>()
    is InternalArgumentsEnumsHeaderMode -> value.toApiEnum<ApiArgumentsEnumsHeaderMode>()
    is InternalArgumentsEnumsNameBasedDestructuringMode -> value.toApiEnum<ApiArgumentsEnumsNameBasedDestructuringMode>()
    is InternalArgumentsEnumsReturnValueCheckerMode -> value.toApiEnum<ApiArgumentsEnumsReturnValueCheckerMode>()
    is InternalArgumentsEnumsVerifyIrMode -> value.toApiEnum<ApiArgumentsEnumsVerifyIrMode>()
    is InternalArgumentsEnumsKotlinVersion -> value.toApiEnum<ApiArgumentsEnumsKotlinVersion>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsExplicitApiMode -> value.toImplEnum<InternalArgumentsEnumsExplicitApiMode>()
    is ApiArgumentsEnumsAnnotationDefaultTargetMode -> value.toImplEnum<InternalArgumentsEnumsAnnotationDefaultTargetMode>()
    is ApiArgumentsEnumsHeaderMode -> value.toImplEnum<InternalArgumentsEnumsHeaderMode>()
    is ApiArgumentsEnumsNameBasedDestructuringMode -> value.toImplEnum<InternalArgumentsEnumsNameBasedDestructuringMode>()
    is ApiArgumentsEnumsReturnValueCheckerMode -> value.toImplEnum<InternalArgumentsEnumsReturnValueCheckerMode>()
    is ApiArgumentsEnumsVerifyIrMode -> value.toImplEnum<InternalArgumentsEnumsVerifyIrMode>()
    is ApiArgumentsEnumsKotlinVersion -> value.toImplEnum<InternalArgumentsEnumsKotlinVersion>()
    else -> value
  }
}
