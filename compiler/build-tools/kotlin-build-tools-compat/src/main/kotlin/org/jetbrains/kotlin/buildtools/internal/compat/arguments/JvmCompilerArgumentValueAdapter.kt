// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.AbiStabilityMode as CompatArgumentsEnumsAbiStabilityMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.AssertionsMode as CompatArgumentsEnumsAssertionsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.CompatqualAnnotationsMode as CompatArgumentsEnumsCompatqualAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JdkRelease as CompatArgumentsEnumsJdkRelease
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JspecifyAnnotationsMode as CompatArgumentsEnumsJspecifyAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JvmDefaultMode as CompatArgumentsEnumsJvmDefaultMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JvmTarget as CompatArgumentsEnumsJvmTarget
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.LambdasMode as CompatArgumentsEnumsLambdasMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.SamConversionsMode as CompatArgumentsEnumsSamConversionsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.StringConcatMode as CompatArgumentsEnumsStringConcatMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.WhenExpressionsMode as CompatArgumentsEnumsWhenExpressionsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AbiStabilityMode as ApiArgumentsEnumsAbiStabilityMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AssertionsMode as ApiArgumentsEnumsAssertionsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.CompatqualAnnotationsMode as ApiArgumentsEnumsCompatqualAnnotationsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JdkRelease as ApiArgumentsEnumsJdkRelease
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JspecifyAnnotationsMode as ApiArgumentsEnumsJspecifyAnnotationsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmDefaultMode as ApiArgumentsEnumsJvmDefaultMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget as ApiArgumentsEnumsJvmTarget
import org.jetbrains.kotlin.buildtools.api.arguments.enums.LambdasMode as ApiArgumentsEnumsLambdasMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SamConversionsMode as ApiArgumentsEnumsSamConversionsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.StringConcatMode as ApiArgumentsEnumsStringConcatMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WhenExpressionsMode as ApiArgumentsEnumsWhenExpressionsMode

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.compat.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object JvmCompilerArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is CompatArgumentsEnumsAbiStabilityMode -> value.toApiEnum<ApiArgumentsEnumsAbiStabilityMode>()
    is CompatArgumentsEnumsAssertionsMode -> value.toApiEnum<ApiArgumentsEnumsAssertionsMode>()
    is CompatArgumentsEnumsJdkRelease -> value.toApiEnum<ApiArgumentsEnumsJdkRelease>()
    is CompatArgumentsEnumsJspecifyAnnotationsMode -> value.toApiEnum<ApiArgumentsEnumsJspecifyAnnotationsMode>()
    is CompatArgumentsEnumsLambdasMode -> value.toApiEnum<ApiArgumentsEnumsLambdasMode>()
    is CompatArgumentsEnumsSamConversionsMode -> value.toApiEnum<ApiArgumentsEnumsSamConversionsMode>()
    is CompatArgumentsEnumsStringConcatMode -> value.toApiEnum<ApiArgumentsEnumsStringConcatMode>()
    is CompatArgumentsEnumsCompatqualAnnotationsMode -> value.toApiEnum<ApiArgumentsEnumsCompatqualAnnotationsMode>()
    is CompatArgumentsEnumsWhenExpressionsMode -> value.toApiEnum<ApiArgumentsEnumsWhenExpressionsMode>()
    is CompatArgumentsEnumsJvmDefaultMode -> value.toApiEnum<ApiArgumentsEnumsJvmDefaultMode>()
    is CompatArgumentsEnumsJvmTarget -> value.toApiEnum<ApiArgumentsEnumsJvmTarget>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsAbiStabilityMode -> value.toImplEnum<CompatArgumentsEnumsAbiStabilityMode>()
    is ApiArgumentsEnumsAssertionsMode -> value.toImplEnum<CompatArgumentsEnumsAssertionsMode>()
    is ApiArgumentsEnumsJdkRelease -> value.toImplEnum<CompatArgumentsEnumsJdkRelease>()
    is ApiArgumentsEnumsJspecifyAnnotationsMode -> value.toImplEnum<CompatArgumentsEnumsJspecifyAnnotationsMode>()
    is ApiArgumentsEnumsLambdasMode -> value.toImplEnum<CompatArgumentsEnumsLambdasMode>()
    is ApiArgumentsEnumsSamConversionsMode -> value.toImplEnum<CompatArgumentsEnumsSamConversionsMode>()
    is ApiArgumentsEnumsStringConcatMode -> value.toImplEnum<CompatArgumentsEnumsStringConcatMode>()
    is ApiArgumentsEnumsCompatqualAnnotationsMode -> value.toImplEnum<CompatArgumentsEnumsCompatqualAnnotationsMode>()
    is ApiArgumentsEnumsWhenExpressionsMode -> value.toImplEnum<CompatArgumentsEnumsWhenExpressionsMode>()
    is ApiArgumentsEnumsJvmDefaultMode -> value.toImplEnum<CompatArgumentsEnumsJvmDefaultMode>()
    is ApiArgumentsEnumsJvmTarget -> value.toImplEnum<CompatArgumentsEnumsJvmTarget>()
    else -> value
  }
}
