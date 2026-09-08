// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.AbiStabilityMode as InternalArgumentsEnumsAbiStabilityMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.AssertionsMode as InternalArgumentsEnumsAssertionsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.CompatqualAnnotationsMode as InternalArgumentsEnumsCompatqualAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JdkRelease as InternalArgumentsEnumsJdkRelease
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JspecifyAnnotationsMode as InternalArgumentsEnumsJspecifyAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JvmDefaultMode as InternalArgumentsEnumsJvmDefaultMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JvmTarget as InternalArgumentsEnumsJvmTarget
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.LambdasMode as InternalArgumentsEnumsLambdasMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SamConversionsMode as InternalArgumentsEnumsSamConversionsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.StringConcatMode as InternalArgumentsEnumsStringConcatMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ValhallaSupportMode as InternalArgumentsEnumsValhallaSupportMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.WhenExpressionsMode as InternalArgumentsEnumsWhenExpressionsMode
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
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ValhallaSupportMode as ApiArgumentsEnumsValhallaSupportMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WhenExpressionsMode as ApiArgumentsEnumsWhenExpressionsMode

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object JvmCompilerArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is InternalArgumentsEnumsAbiStabilityMode -> value.toApiEnum<ApiArgumentsEnumsAbiStabilityMode>()
    is InternalArgumentsEnumsAssertionsMode -> value.toApiEnum<ApiArgumentsEnumsAssertionsMode>()
    is InternalArgumentsEnumsJdkRelease -> value.toApiEnum<ApiArgumentsEnumsJdkRelease>()
    is InternalArgumentsEnumsJspecifyAnnotationsMode -> value.toApiEnum<ApiArgumentsEnumsJspecifyAnnotationsMode>()
    is InternalArgumentsEnumsLambdasMode -> value.toApiEnum<ApiArgumentsEnumsLambdasMode>()
    is InternalArgumentsEnumsSamConversionsMode -> value.toApiEnum<ApiArgumentsEnumsSamConversionsMode>()
    is InternalArgumentsEnumsStringConcatMode -> value.toApiEnum<ApiArgumentsEnumsStringConcatMode>()
    is InternalArgumentsEnumsCompatqualAnnotationsMode -> value.toApiEnum<ApiArgumentsEnumsCompatqualAnnotationsMode>()
    is InternalArgumentsEnumsValhallaSupportMode -> value.toApiEnum<ApiArgumentsEnumsValhallaSupportMode>()
    is InternalArgumentsEnumsWhenExpressionsMode -> value.toApiEnum<ApiArgumentsEnumsWhenExpressionsMode>()
    is InternalArgumentsEnumsJvmDefaultMode -> value.toApiEnum<ApiArgumentsEnumsJvmDefaultMode>()
    is InternalArgumentsEnumsJvmTarget -> value.toApiEnum<ApiArgumentsEnumsJvmTarget>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsAbiStabilityMode -> value.toImplEnum<InternalArgumentsEnumsAbiStabilityMode>()
    is ApiArgumentsEnumsAssertionsMode -> value.toImplEnum<InternalArgumentsEnumsAssertionsMode>()
    is ApiArgumentsEnumsJdkRelease -> value.toImplEnum<InternalArgumentsEnumsJdkRelease>()
    is ApiArgumentsEnumsJspecifyAnnotationsMode -> value.toImplEnum<InternalArgumentsEnumsJspecifyAnnotationsMode>()
    is ApiArgumentsEnumsLambdasMode -> value.toImplEnum<InternalArgumentsEnumsLambdasMode>()
    is ApiArgumentsEnumsSamConversionsMode -> value.toImplEnum<InternalArgumentsEnumsSamConversionsMode>()
    is ApiArgumentsEnumsStringConcatMode -> value.toImplEnum<InternalArgumentsEnumsStringConcatMode>()
    is ApiArgumentsEnumsCompatqualAnnotationsMode -> value.toImplEnum<InternalArgumentsEnumsCompatqualAnnotationsMode>()
    is ApiArgumentsEnumsValhallaSupportMode -> value.toImplEnum<InternalArgumentsEnumsValhallaSupportMode>()
    is ApiArgumentsEnumsWhenExpressionsMode -> value.toImplEnum<InternalArgumentsEnumsWhenExpressionsMode>()
    is ApiArgumentsEnumsJvmDefaultMode -> value.toImplEnum<InternalArgumentsEnumsJvmDefaultMode>()
    is ApiArgumentsEnumsJvmTarget -> value.toImplEnum<InternalArgumentsEnumsJvmTarget>()
    else -> value
  }
}
