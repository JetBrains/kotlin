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

package org.jetbrains.kotlin.idea.highlighter

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.ContextDependentRenderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.asRenderer
import org.jetbrains.kotlin.idea.highlighter.renderersUtil.renderResolvedCall
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.types.KotlinType
import kotlin.comparisons.compareBy

object IdeRenderers {

    @JvmField val HTML_AMBIGUOUS_CALLS = Renderer {
        calls: Collection<ResolvedCall<*>> ->
            calls
                .map { it.resultingDescriptor }
                .sortedWith(MemberComparator.INSTANCE)
                .joinToString("") { "<li>${DescriptorRenderer.HTML.render(it)}</li>" }
    }

    @JvmField val HTML_RENDER_TYPE = Renderer<KotlinType> {
        DescriptorRenderer.HTML.renderType(it)
    }

    @JvmField val HTML_NONE_APPLICABLE_CALLS= Renderer {
        calls: Collection<ResolvedCall<*>> ->
            val comparator = compareBy(MemberComparator.INSTANCE) { c: ResolvedCall<*> -> c.resultingDescriptor }
            calls
                .sortedWith(comparator)
                .joinToString("") { "<li>${renderResolvedCall(it)}</li>" }
    }

    @JvmField val HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderConflictingSubstitutionsInferenceError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField val HTML_TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderParameterConstraintError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField val HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderNoInformationForParameterError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField val HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderUpperBoundViolatedInferenceError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField val HTML_RENDER_RETURN_TYPE = ContextDependentRenderer<CallableMemberDescriptor> {
        member, context ->
        HTML_RENDER_TYPE.render(member.returnType!!, context)
    }

    @JvmField val HTML_COMPACT_WITH_MODIFIERS = DescriptorRenderer.HTML.withOptions {
        withDefinedIn = false
    }.asRenderer()

    @JvmField val HTML_CONFLICTING_JVM_DECLARATIONS_DATA = ContextDependentRenderer {
        data: ConflictingJvmDeclarationsData, renderingContext ->

        val conflicts = data.signatureOrigins
                .mapNotNull { it.descriptor }
                .sortedWith(MemberComparator.INSTANCE)
                .joinToString("") { "<li>" + HTML_COMPACT_WITH_MODIFIERS.render(it, renderingContext) + "</li>\n" }

        "The following declarations have the same JVM signature (<code>${data.signature.name}${data.signature.desc}</code>):<br/>\n<ul>\n$conflicts</ul>"
    }

    @JvmField val HTML_THROWABLE = ContextDependentRenderer<Throwable> {
        throwable, context ->
        Renderers.THROWABLE.render(throwable, context).replace("\n", "<br/>")
    }

    @JvmField val HTML = DescriptorRenderer.HTML.asRenderer()
}
