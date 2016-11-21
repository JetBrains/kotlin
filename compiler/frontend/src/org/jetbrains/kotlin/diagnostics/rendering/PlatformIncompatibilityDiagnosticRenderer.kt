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

import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.resolve.checkers.PlatformImplDeclarationChecker

object PlatformIncompatibilityDiagnosticRenderer :
        DiagnosticParameterRenderer<Map<PlatformImplDeclarationChecker.Compatibility.Incompatible, Collection<MemberDescriptor>>> {
    override fun render(
            obj: Map<PlatformImplDeclarationChecker.Compatibility.Incompatible, Collection<MemberDescriptor>>,
            renderingContext: RenderingContext
    ): String {
        if (obj.isEmpty()) return ""

        return buildString {
            appendln()
            for ((incompatibility, descriptors) in obj) {
                append("The following declaration")
                if (descriptors.size == 1) append(" is") else append("s are")
                append(" incompatible")
                incompatibility.reason?.let { append(" because $it") }
                appendln(":")
                for (descriptor in descriptors) {
                    append("    ")
                    appendln(Renderers.COMPACT_WITH_MODIFIERS.render(descriptor, renderingContext))
                }
            }
        }
    }
}
