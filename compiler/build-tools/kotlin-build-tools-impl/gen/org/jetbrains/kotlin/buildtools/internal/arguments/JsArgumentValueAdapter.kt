// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsEcmaVersion as InternalArgumentsEnumsJsEcmaVersion
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsIrDiagnosticMode as InternalArgumentsEnumsJsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsModuleKind as InternalArgumentsEnumsJsModuleKind
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion as ApiArgumentsEnumsJsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode as ApiArgumentsEnumsJsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind as ApiArgumentsEnumsJsModuleKind

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object JsArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is InternalArgumentsEnumsJsIrDiagnosticMode -> value.toApiEnum<ApiArgumentsEnumsJsIrDiagnosticMode>()
    is InternalArgumentsEnumsJsModuleKind -> value.toApiEnum<ApiArgumentsEnumsJsModuleKind>()
    is InternalArgumentsEnumsJsEcmaVersion -> value.toApiEnum<ApiArgumentsEnumsJsEcmaVersion>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsJsIrDiagnosticMode -> value.toImplEnum<InternalArgumentsEnumsJsIrDiagnosticMode>()
    is ApiArgumentsEnumsJsModuleKind -> value.toImplEnum<InternalArgumentsEnumsJsModuleKind>()
    is ApiArgumentsEnumsJsEcmaVersion -> value.toImplEnum<InternalArgumentsEnumsJsEcmaVersion>()
    else -> value
  }
}
