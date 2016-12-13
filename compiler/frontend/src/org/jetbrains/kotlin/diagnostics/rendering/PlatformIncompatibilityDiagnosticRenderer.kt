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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker

object PlatformIncompatibilityDiagnosticRenderer :
        DiagnosticParameterRenderer<Map<HeaderImplDeclarationChecker.Compatibility.Incompatible, Collection<MemberDescriptor>>> {
    private val INDENTATION_UNIT = "    "

    override fun render(
            obj: Map<HeaderImplDeclarationChecker.Compatibility.Incompatible, Collection<MemberDescriptor>>,
            renderingContext: RenderingContext
    ): String {
        if (obj.isEmpty()) return ""

        val renderDescriptor: (DeclarationDescriptor) -> String = { Renderers.COMPACT_WITH_MODIFIERS.render(it, renderingContext) }

        return buildString {
            appendln()
            render(obj, "", renderDescriptor)
        }
    }

    private fun StringBuilder.render(
            map: Map<HeaderImplDeclarationChecker.Compatibility.Incompatible, Collection<MemberDescriptor>>,
            indent: String,
            renderDescriptor: (DeclarationDescriptor) -> String
    ) {
        for ((incompatibility, descriptors) in map) {
            append(indent)
            append("The following declaration")
            if (descriptors.size == 1) append(" is") else append("s are")
            append(" incompatible")
            incompatibility.reason?.let { append(" because $it") }

            incompatibility.unimplemented?.let { unimplemented ->
                appendln(".")
                append(indent)
                appendln("No implementations are found for members listed below:")
                for ((descriptor, mapping) in unimplemented) {
                    appendln()
                    append(indent + "    ")
                    appendln(renderDescriptor(descriptor))
                    if (mapping.isNotEmpty()) {
                        appendln()
                    }
                    render(mapping, indent + INDENTATION_UNIT, renderDescriptor)
                }
            } ?: run {
                appendln(":")
                for (descriptor in descriptors) {
                    append(indent + "    ")
                    appendln(renderDescriptor(descriptor))
                }
            }
        }
    }
}
