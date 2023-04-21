/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderer

object KtDiagnosticRenderers {
    val NULLABLE_STRING = Renderer<String?> { it ?: "null" }

    val TO_STRING = Renderer { element: Any? ->
        element.toString()
    }

    val OPTIONAL_COLON_TO_STRING = Renderer { element: Any? ->
        val string = element.toString()
        if (string.isNotEmpty()) ": $string" else ""
    }

    val EMPTY = Renderer { _: Any? -> "" }

    val VISIBILITY = Renderer { visibility: Visibility ->
        visibility.externalDisplayName
    }

    val NOT_RENDERED = Renderer<Any?> {
        ""
    }

    val FUNCTION_PARAMETERS = Renderer { hasValueParameters: Boolean -> if (hasValueParameters) "..." else "" }

    @Suppress("FunctionName")
    fun <T> COLLECTION(renderer: ContextIndependentParameterRenderer<T>): ContextIndependentParameterRenderer<Collection<T>> {
        return Renderer { list ->
            list.joinToString(prefix = "[", postfix = "]", separator = ", ", limit = 3, truncated = "...") {
                renderer.render(it)
            }
        }
    }
}
