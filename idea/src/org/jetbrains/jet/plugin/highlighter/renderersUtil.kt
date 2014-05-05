/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter.renderersUtil

import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.calls.util.*
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.highlighter.IdeRenderers
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor

fun <D : CallableDescriptor> renderResolvedCall(resolvedCall: ResolvedCall<D>): String {
    val htmlRenderer = DescriptorRenderer.HTML_FOR_UNINFERRED_TYPE_PARAMS
    val stringBuilder = StringBuilder("")
    val indent = "&nbsp;&nbsp;"

    fun append(any: Any): StringBuilder = stringBuilder.append(any)

    fun renderParameter(parameter: ValueParameterDescriptor): String {
        val varargElementType = parameter.getVarargElementType()
        val parameterType = varargElementType ?: parameter.getType()
        val renderedParameter =
                (if (varargElementType != null) "<b>vararg</b> " else "") +
                htmlRenderer.renderType(parameterType) +
                if (parameter.hasDefaultValue()) " = ..." else ""
        if (resolvedCall.hasTypeMismatchErrorOnParameter(parameter)) {
            return IdeRenderers.error(renderedParameter)
        }
        return renderedParameter
    }

    fun appendTypeParametersSubstitution() {
        val parametersToArgumentsMap = resolvedCall.getTypeArguments()
        fun TypeParameterDescriptor.isInferred(): Boolean {
            val typeArgument = parametersToArgumentsMap[this]
            if (typeArgument == null) return false
            return !ErrorUtils.isUninferredParameter(typeArgument)
        }

        val typeParameters = resolvedCall.getCandidateDescriptor().getTypeParameters()
        val (inferredTypeParameters, notInferredTypeParameters) = typeParameters.partition { parameter -> parameter.isInferred() }

        append("<br/>$indent<i>where</i> ")
        if (!notInferredTypeParameters.isEmpty()) {
            append(notInferredTypeParameters.map { typeParameter -> IdeRenderers.error(typeParameter.getName()) }.makeString())
            append("<i> cannot be inferred</i>")
            if (!inferredTypeParameters.isEmpty()) {
                append("; ")
            }
        }

        val typeParameterToTypeArgumentMap = resolvedCall.getTypeArguments()
        if (!inferredTypeParameters.isEmpty()) {
            append(inferredTypeParameters.map { typeParameter ->
                "${typeParameter.getName()} = ${htmlRenderer.renderType(typeParameterToTypeArgumentMap[typeParameter]!!)}"
            }.makeString())
        }
    }

    val resultingDescriptor = resolvedCall.getResultingDescriptor()
    val receiverParameter = resultingDescriptor.getReceiverParameter()
    if (receiverParameter != null) {
        append(htmlRenderer.renderType(receiverParameter.getType())).append(".")
    }
    append(resultingDescriptor.getName()).append("(")
    append(resultingDescriptor.getValueParameters().map { parameter -> renderParameter(parameter) }.makeString())
    append(if (resolvedCall.hasUnmappedArguments()) IdeRenderers.error(")") else ")")

    if (!resolvedCall.getCandidateDescriptor().getTypeParameters().isEmpty()) {
        appendTypeParametersSubstitution()
        append("<i> for </i><br/>$indent")
        append(htmlRenderer.render(resolvedCall.getCandidateDescriptor()))
    }
    else {
        append(" <i>defined in</i> ")
        val containingDeclaration = resultingDescriptor.getContainingDeclaration()
        val fqName = DescriptorUtils.getFqName(containingDeclaration)
        append(if (FqName.ROOT.equalsTo(fqName)) "root package" else fqName.asString())
    }
    return stringBuilder.toString()
}

