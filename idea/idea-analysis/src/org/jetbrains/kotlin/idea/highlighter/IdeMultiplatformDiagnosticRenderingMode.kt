/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.MultiplatformDiagnosticRenderingMode
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext

object IdeMultiplatformDiagnosticRenderingMode : MultiplatformDiagnosticRenderingMode() {
    override fun newLine(sb: StringBuilder) {
        sb.append("<br/>")
    }

    override fun renderList(sb: StringBuilder, elements: List<() -> Unit>) {
        sb.append("<ul>")
        for (element in elements) {
            sb.append("<li>")
            element()
            sb.append("</li>")
        }
        sb.append("</ul>")
    }

    override fun renderDescriptor(sb: StringBuilder, descriptor: DeclarationDescriptor, context: RenderingContext, indent: String) {
        sb.append(IdeRenderers.HTML.render(descriptor, context))
    }
}
