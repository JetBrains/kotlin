/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import java.util.IdentityHashMap

class ParameterWithTail(val parameter: String, val tail: List<String>?)

interface DiagnosticParameterRenderer<in O> {
    fun render(obj: O, renderingContext: RenderingContext): String

    fun renderWithTail(obj: O, renderingContext: RenderingContext): ParameterWithTail? = null
}

interface ContextIndependentParameterRenderer<in O> : DiagnosticParameterRenderer<O> {
    override fun render(obj: O, renderingContext: RenderingContext): String = render(obj)

    override fun renderWithTail(obj: O, renderingContext: RenderingContext): ParameterWithTail? = renderWithTail(obj)

    fun render(obj: O): String

    fun renderWithTail(obj: O): ParameterWithTail? = null
}

fun <O> Renderer(block: (O) -> String) = object : ContextIndependentParameterRenderer<O> {
    override fun render(obj: O): String = block(obj)
}

fun <O> ContextDependentRenderer(block: (O, RenderingContext) -> String) = object : DiagnosticParameterRenderer<O> {
    override fun render(obj: O, renderingContext: RenderingContext): String = block(obj, renderingContext)
}

fun <P> renderParameter(parameter: P, renderer: DiagnosticParameterRenderer<P>?, context: RenderingContext): Any? =
    renderer?.renderWithTail(parameter, context)
        ?: renderer?.render(parameter, context)
        ?: parameter

fun renderTailsJoined(vararg tails: List<String>?): String {
    return buildString {
        var index = 1
        // This logic with IdentityHashMap is necessary here, as AdaptiveTypeRenderer uses the same storage for all rendered types
        // and without this identity filtering we will render
        val map = IdentityHashMap<List<String>, Boolean>()
        for (tail in tails) {
            if (tail.isNullOrEmpty() || tail in map) continue
            map[tail] = true
            if (index == 1) {
                append(" Where:")
            }
            tail.forEachIndexed { i, it ->
                appendLine()
                append("    #$index = $it")
                index++
            }
        }
    }
}
