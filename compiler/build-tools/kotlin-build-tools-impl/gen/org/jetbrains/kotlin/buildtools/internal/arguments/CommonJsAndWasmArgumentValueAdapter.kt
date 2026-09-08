// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Enum
import kotlin.OptIn
import kotlin.collections.List
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsIrDiagnosticMode as InternalArgumentsEnumsJsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JsMainCallMode as InternalArgumentsEnumsJsMainCallMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SourceMapEmbedSources as InternalArgumentsEnumsSourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SourceMapNamesPolicy as InternalArgumentsEnumsSourceMapNamesPolicy
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode as ApiArgumentsEnumsJsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsMainCallMode as ApiArgumentsEnumsJsMainCallMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapEmbedSources as ApiArgumentsEnumsSourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapNamesPolicy as ApiArgumentsEnumsSourceMapNamesPolicy

/**
 * Converts argument values between the representation used by the API and the one used by `org.jetbrains.kotlin.buildtools.internal.arguments`.
 *
 * A value needs converting whenever the two sides declare its type separately, as they do for the mirrored argument enums.
 */
internal object CommonJsAndWasmArgumentValueAdapter {
  public fun toApi(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toApi(it) }
    is InternalArgumentsEnumsJsIrDiagnosticMode -> value.toApiEnum<ApiArgumentsEnumsJsIrDiagnosticMode>()
    is InternalArgumentsEnumsJsMainCallMode -> value.toApiEnum<ApiArgumentsEnumsJsMainCallMode>()
    is InternalArgumentsEnumsSourceMapEmbedSources -> value.toApiEnum<ApiArgumentsEnumsSourceMapEmbedSources>()
    is InternalArgumentsEnumsSourceMapNamesPolicy -> value.toApiEnum<ApiArgumentsEnumsSourceMapNamesPolicy>()
    else -> value
  }

  public fun toImpl(`value`: Any?): Any? = when (value) {
    is List<*> if value.firstOrNull() is Enum<*> -> value.map { toImpl(it) }
    is ApiArgumentsEnumsJsIrDiagnosticMode -> value.toImplEnum<InternalArgumentsEnumsJsIrDiagnosticMode>()
    is ApiArgumentsEnumsJsMainCallMode -> value.toImplEnum<InternalArgumentsEnumsJsMainCallMode>()
    is ApiArgumentsEnumsSourceMapEmbedSources -> value.toImplEnum<InternalArgumentsEnumsSourceMapEmbedSources>()
    is ApiArgumentsEnumsSourceMapNamesPolicy -> value.toImplEnum<InternalArgumentsEnumsSourceMapNamesPolicy>()
    else -> value
  }
}
