// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.AnnotationDefaultTargetMode as CompatArgumentsEnumsAnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.ExplicitApiMode as CompatArgumentsEnumsExplicitApiMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.KotlinVersion as CompatArgumentsEnumsKotlinVersion
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.ReturnValueCheckerMode as CompatArgumentsEnumsReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.VerifyIrMode as CompatArgumentsEnumsVerifyIrMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AnnotationDefaultTargetMode as ApiArgumentsEnumsAnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ExplicitApiMode as ApiArgumentsEnumsExplicitApiMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion as ApiArgumentsEnumsKotlinVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode as ApiArgumentsEnumsReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode as ApiArgumentsEnumsVerifyIrMode

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.compat.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object CommonCompilerArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is CompatArgumentsEnumsExplicitApiMode -> value.toApiEnum<ApiArgumentsEnumsExplicitApiMode>()
    is CompatArgumentsEnumsAnnotationDefaultTargetMode -> value.toApiEnum<ApiArgumentsEnumsAnnotationDefaultTargetMode>()
    is CompatArgumentsEnumsReturnValueCheckerMode -> value.toApiEnum<ApiArgumentsEnumsReturnValueCheckerMode>()
    is CompatArgumentsEnumsVerifyIrMode -> value.toApiEnum<ApiArgumentsEnumsVerifyIrMode>()
    is CompatArgumentsEnumsKotlinVersion -> value.toApiEnum<ApiArgumentsEnumsKotlinVersion>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsExplicitApiMode -> value.toImplEnum<CompatArgumentsEnumsExplicitApiMode>()
    is ApiArgumentsEnumsAnnotationDefaultTargetMode -> value.toImplEnum<CompatArgumentsEnumsAnnotationDefaultTargetMode>()
    is ApiArgumentsEnumsReturnValueCheckerMode -> value.toImplEnum<CompatArgumentsEnumsReturnValueCheckerMode>()
    is ApiArgumentsEnumsVerifyIrMode -> value.toImplEnum<CompatArgumentsEnumsVerifyIrMode>()
    is ApiArgumentsEnumsKotlinVersion -> value.toImplEnum<CompatArgumentsEnumsKotlinVersion>()
    else -> value
  }
}
