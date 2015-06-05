/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.idea.highlighter.renderersUtil.renderResolvedCall
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.Renderer
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.types.JetType

public object IdeRenderers {
    public val HTML_AMBIGUOUS_CALLS: Renderer<Collection<ResolvedCall<*>>> = Renderer {
        calls: Collection<ResolvedCall<*>> ->
            calls
                .map { it.getResultingDescriptor() }
                .sortBy(MemberComparator.INSTANCE)
                .joinToString("") { "<li>" + DescriptorRenderer.HTML.render(it) + "</li>" }
    }

    public val HTML_RENDER_TYPE: Renderer<JetType> = Renderer {
        DescriptorRenderer.HTML.renderType(it)
    }

    public val HTML_NONE_APPLICABLE_CALLS: Renderer<Collection<ResolvedCall<*>>> = Renderer {
        calls: Collection<ResolvedCall<*>> ->
            // TODO: compareBy(comparator, selector) in stdlib
            val comparator = comparator<ResolvedCall<*>> { c1, c2 -> MemberComparator.INSTANCE.compare(c1.getResultingDescriptor(), c2.getResultingDescriptor()) }
            calls
                .sortBy(comparator)
                .joinToString("") { "<li>" + renderResolvedCall(it) + "</li>" }
    }

    public val HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER: Renderer<InferenceErrorData> = Renderer {
        Renderers.renderConflictingSubstitutionsInferenceError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    public val HTML_TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER: Renderer<InferenceErrorData> = Renderer {
        Renderers.renderTypeConstructorMismatchError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    public val HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER: Renderer<InferenceErrorData> = Renderer {
        Renderers.renderNoInformationForParameterError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    public val HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER: Renderer<InferenceErrorData> = Renderer {
        Renderers.renderUpperBoundViolatedInferenceError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    public val HTML_RENDER_RETURN_TYPE: Renderer<CallableMemberDescriptor> = Renderer {
        val returnType = it.getReturnType()!!
        DescriptorRenderer.HTML.renderType(returnType)
    }

    public val HTML_COMPACT_WITH_MODIFIERS: DescriptorRenderer = DescriptorRenderer.HTML.withOptions {
        withDefinedIn = false
    }

    public val HTML_CONFLICTING_JVM_DECLARATIONS_DATA: Renderer<ConflictingJvmDeclarationsData> = Renderer {
        data: ConflictingJvmDeclarationsData ->

        val conflicts = data.signatureOrigins
            .map { it.descriptor }
            .filterNotNull()
            .sortBy(MemberComparator.INSTANCE)
            .joinToString("") { "<li>" + HTML_COMPACT_WITH_MODIFIERS.render(it) + "</li>\n" }

        "The following declarations have the same JVM signature (<code>${data.signature.name}${data.signature.desc}</code>):<br/>\n<ul>\n$conflicts</ul>"
    }

    public val HTML_THROWABLE: Renderer<Throwable> = Renderer {
        Renderers.THROWABLE.render(it).replace("\n", "<br/>")
    }
}
