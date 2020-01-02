/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

private const val RED_TEMPLATE = "<font color=red><b>%s</b></font>"
private const val STRONG_TEMPLATE = "<b>%s</b>"

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
        val renderedParameter = (if (varargElementType != null) "<b>vararg</b> " else "") +
                typeRenderer.render(parameterType, context) +
                if (parameter.hasDefaultValue()) " = ..." else ""
        return if (resolvedCall.hasTypeMismatchErrorOnParameter(parameter))
            renderError(renderedParameter)
        else
            renderedParameter
    }

    fun appendTypeParametersSubstitution() {
        val parametersToArgumentsMap = resolvedCall.typeArguments
        fun TypeParameterDescriptor.isInferred(): Boolean {
            val typeArgument = parametersToArgumentsMap[this] ?: return false
            return !ErrorUtils.isUninferredParameter(typeArgument)
        }

        val typeParameters = resolvedCall.candidateDescriptor.typeParameters
        val (inferredTypeParameters, notInferredTypeParameters) = typeParameters.partition(TypeParameterDescriptor::isInferred)

        append("<br/>$indent<i>where</i> ")
        if (notInferredTypeParameters.isNotEmpty()) {
            append(notInferredTypeParameters.joinToString { typeParameter -> renderError(typeParameter.name) })
            append("<i> cannot be inferred</i>")
            if (inferredTypeParameters.isNotEmpty()) {
                append("; ")
            }
        }

        val typeParameterToTypeArgumentMap = resolvedCall.typeArguments
        if (inferredTypeParameters.isNotEmpty()) {
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

    if (resolvedCall.candidateDescriptor.typeParameters.isNotEmpty()) {
        appendTypeParametersSubstitution()
        append("<i> for </i><br/>$indent")
        // candidate descriptor is not in context of the rest of the message
        append(descriptorRenderer.render(resolvedCall.candidateDescriptor, RenderingContext.of(resolvedCall.candidateDescriptor)))
    } else {
        append(" <i>defined in</i> ")
        val containingDeclaration = resultingDescriptor.containingDeclaration
        val fqName = DescriptorUtils.getFqName(containingDeclaration)
        append(if (fqName.isRoot) "root package" else fqName.asString())
    }
    return stringBuilder.toString()
}

