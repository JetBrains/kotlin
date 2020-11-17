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

import org.jetbrains.kotlin.diagnostics.*
import java.text.MessageFormat


abstract class AbstractDiagnosticWithParametersRenderer<D : Diagnostic> protected constructor(message: String) : DiagnosticRenderer<D> {
    private val messageFormat = MessageFormat(message)

    override fun render(diagnostic: D): String {
        return messageFormat.format(renderParameters(diagnostic))
    }

    abstract fun renderParameters(diagnostic: D): Array<out Any>

}


class DiagnosticWithParameters1Renderer<A : Any>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters1<*, A>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters1<*, A>): Array<out Any> {
        val context = RenderingContext.of(diagnostic.a)
        return arrayOf(renderParameter(diagnostic.a, rendererForA, context))
    }
}

class DiagnosticWithParameters2Renderer<A : Any, B : Any>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters2<*, A, B>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters2<*, A, B>): Array<out Any> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context)
        )
    }
}

class DiagnosticWithParameters3Renderer<A : Any, B : Any, C : Any>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters3<*, A, B, C>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters3<*, A, B, C>): Array<out Any> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
            renderParameter(diagnostic.c, rendererForC, context)
        )
    }
}

class DiagnosticWithParametersMultiRenderer<A>(
    message: String,
    private val renderer: MultiRenderer<A>
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters1<*, A>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters1<*, A>): Array<out Any> {
        return renderer.render(diagnostic.a)
    }
}

interface MultiRenderer<in A> {
    fun render(a: A): Array<String>
}
