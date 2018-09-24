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

package org.jetbrains.kotlin.idea.highlighter.renderersUtil

import com.google.common.html.HtmlEscapers
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.diagnostics.rendering.SmartTypeRenderer
import org.jetbrains.kotlin.diagnostics.rendering.asRenderer
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.hasTypeMismatchErrorOnParameter
import org.jetbrains.kotlin.resolve.calls.callUtil.hasUnmappedArguments
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.ErrorUtils

private val RED_TEMPLATE = "<font color=red><b>%s</b></font>"
private val STRONG_TEMPLATE = "<b>%s</b>"

fun renderStrong(o: Any): String = STRONG_TEMPLATE.format(o)

fun renderError(o: Any): String = RED_TEMPLATE.format(o)

fun renderStrong(o: Any, error: Boolean): String = (if (error) RED_TEMPLATE else STRONG_TEMPLATE).format(o)

private val HTML_FOR_UNINFERRED_TYPE_PARAMS: DescriptorRenderer = DescriptorRenderer.withOptions {
    uninferredTypeParameterAsName = true
    modifiers = emptySet()
    classifierNamePolicy = ClassifierNamePolicy.SHORT
    textFormat = RenderingFormat.HTML
}

fun renderResolvedCall(resolvedCall: ResolvedCall<*>, context: RenderingContext): String {
    val typeRenderer = SmartTypeRenderer(HTML_FOR_UNINFERRED_TYPE_PARAMS)
    val descriptorRenderer = HTML_FOR_UNINFERRED_TYPE_PARAMS.asRenderer()
    val stringBuilder = StringBuilder("")
    val indent = "&nbsp;&nbsp;"

    fun append(any: Any): StringBuilder = stringBuilder.append(any)

    fun renderParameter(parameter: ValueParameterDescriptor): String {
        val varargElementType = parameter.varargElementType
        val parameterType = varargElementType ?: parameter.type
        val renderedParameter =
                (if (varargElementType != null) "<b>vararg</b> " else "") +
                typeRenderer.render(parameterType, context) +
                if (parameter.hasDefaultValue()) " = ..." else ""
        if (resolvedCall.hasTypeMismatchErrorOnParameter(parameter)) {
            return renderError(renderedParameter)
        }
        return renderedParameter
    }

    fun appendTypeParametersSubstitution() {
        val parametersToArgumentsMap = resolvedCall.typeArguments
        fun TypeParameterDescriptor.isInferred(): Boolean {
            val typeArgument = parametersToArgumentsMap[this]
            if (typeArgument == null) return false
            return !ErrorUtils.isUninferredParameter(typeArgument)
        }

        val typeParameters = resolvedCall.candidateDescriptor.typeParameters
        val (inferredTypeParameters, notInferredTypeParameters) = typeParameters.partition(TypeParameterDescriptor::isInferred)

        append("<br/>$indent<i>where</i> ")
        if (!notInferredTypeParameters.isEmpty()) {
            append(notInferredTypeParameters.joinToString { typeParameter -> renderError(typeParameter.name) })
            append("<i> cannot be inferred</i>")
            if (!inferredTypeParameters.isEmpty()) {
                append("; ")
            }
        }

        val typeParameterToTypeArgumentMap = resolvedCall.typeArguments
        if (!inferredTypeParameters.isEmpty()) {
            append(inferredTypeParameters.joinToString { typeParameter ->
                "${typeParameter.name} = ${typeRenderer.render(typeParameterToTypeArgumentMap[typeParameter]!!, context)}"
            })
        }
    }

    val resultingDescriptor = resolvedCall.resultingDescriptor
    val receiverParameter = resultingDescriptor.extensionReceiverParameter
    if (receiverParameter != null) {
        append(typeRenderer.render(receiverParameter.type, context)).append(".")
    }
    append(HtmlEscapers.htmlEscaper().escape(resultingDescriptor.name.asString())).append("(")
    append(resultingDescriptor.valueParameters.joinToString(transform = ::renderParameter))
    append(if (resolvedCall.hasUnmappedArguments()) renderError(")") else ")")

    if (!resolvedCall.candidateDescriptor.typeParameters.isEmpty()) {
        appendTypeParametersSubstitution()
        append("<i> for </i><br/>$indent")
        // candidate descriptor is not in context of the rest of the message
        append(descriptorRenderer.render(resolvedCall.candidateDescriptor, RenderingContext.of(resolvedCall.candidateDescriptor)))
    }
    else {
        append(" <i>defined in</i> ")
        val containingDeclaration = resultingDescriptor.containingDeclaration
        val fqName = DescriptorUtils.getFqName(containingDeclaration)
        append(if (fqName.isRoot) "root package" else fqName.asString())
    }
    return stringBuilder.toString()
}

