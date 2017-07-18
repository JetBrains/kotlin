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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker.Compatibility.Incompatible

object PlatformIncompatibilityDiagnosticRenderer : DiagnosticParameterRenderer<Map<Incompatible, Collection<MemberDescriptor>>> {
    override fun render(
            obj: Map<Incompatible, Collection<MemberDescriptor>>,
            renderingContext: RenderingContext
    ): String {
        if (obj.isEmpty()) return ""

        return buildString {
            appendln()
            renderIncompatibilityInformation(obj, "", renderingContext)
        }
    }
}

object IncompatibleHeaderImplClassScopesRenderer :
        DiagnosticParameterRenderer<List<Pair<CallableMemberDescriptor, Map<Incompatible, Collection<CallableMemberDescriptor>>>>> {
    override fun render(
            obj: List<Pair<CallableMemberDescriptor, Map<Incompatible, Collection<CallableMemberDescriptor>>>>,
            renderingContext: RenderingContext): String {
        if (obj.isEmpty()) return ""

        return buildString {
            appendln()
            renderIncompatibleClassScopes(obj, "", renderingContext)
        }
    }
}

private fun StringBuilder.renderIncompatibilityInformation(
        map: Map<Incompatible, Collection<MemberDescriptor>>,
        indent: String,
        context: RenderingContext
) {
    for ((incompatibility, descriptors) in map) {
        append(indent)
        append("The following declaration")
        if (descriptors.size == 1) append(" is") else append("s are")
        append(" incompatible")
        incompatibility.reason?.let { appendln(" because $it:") }

        for (descriptor in descriptors) {
            append(indent + "    ")
            appendln(descriptor.render(context))
        }

        if (incompatibility is Incompatible.ClassScopes) {
            append(indent)
            appendln("No implementations are found for members listed below:")
            renderIncompatibleClassScopes(incompatibility.unimplemented, indent, context)
        }
    }
}

private fun StringBuilder.renderIncompatibleClassScopes(
        unimplemented: List<Pair<CallableMemberDescriptor, Map<Incompatible, Collection<CallableMemberDescriptor>>>>,
        indent: String,
        context: RenderingContext
) {
    for ((descriptor, mapping) in unimplemented) {
        appendln()
        append(indent + "    ")
        appendln(descriptor.render(context))
        if (mapping.isNotEmpty()) {
            appendln()
        }
        renderIncompatibilityInformation(mapping, indent + INDENTATION_UNIT, context)
    }
}

private const val INDENTATION_UNIT = "    "

private fun DeclarationDescriptor.render(context: RenderingContext): String {
    return Renderers.COMPACT_WITH_MODIFIERS.render(this, context)
}
