/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.diagnostics.*

fun RenderingContext.Companion.parameters(d: Diagnostic): List<Any> = when (d) {
    is SimpleDiagnostic<*> -> listOf()
    is DiagnosticWithParameters1<*, *> -> listOf(d.a)
    is DiagnosticWithParameters2<*, *, *> -> listOf(d.a, d.b)
    is DiagnosticWithParameters3<*, *, *, *> -> listOf(d.a, d.b, d.c)
    is DiagnosticWithParameters4<*, *, *, *, *> -> listOf(d.a, d.b, d.c, d.d)
    is ParametrizedDiagnostic<*> -> error("Unexpected diagnostic: ${d::class.java}")
    else -> listOf()
}

fun RenderingContext.Companion.fromDiagnostic(d: Diagnostic): RenderingContext = RenderingContext.Impl(parameters(d))
