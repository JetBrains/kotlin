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

fun <D : CallableDescriptor> renderResolvedCall(resolvedCall: ResolvedCall<D>): String {
    val stringBuilder = StringBuilder("")
    val funDescriptor = resolvedCall.getResultingDescriptor()

    val htmlRenderer = DescriptorRenderer.HTML
    val receiverParameter = funDescriptor.getReceiverParameter()
    if (receiverParameter != null) {
        stringBuilder.append(htmlRenderer.renderType(receiverParameter.getType())).append(".")
    }
    stringBuilder.append(funDescriptor.getName()).append("(")
    var first = true
    for (parameter in funDescriptor.getValueParameters()) {
        if (!first) {
            stringBuilder.append(", ")
        }
        var parameterType = parameter.getType()
        val varargElementType = parameter.getVarargElementType()
        if (varargElementType != null) {
            parameterType = varargElementType
        }
        var paramString: String = (if (varargElementType != null) "<b>vararg</b> " else "") + htmlRenderer.renderType(parameterType)
        if (parameter.hasDefaultValue()) {
            paramString += " = ..."
        }
        if (resolvedCall.hasErrorOnParameter(parameter)) {
            paramString = IdeRenderers.error(paramString)
        }
        stringBuilder.append(paramString)

        first = false
    }
    stringBuilder.append(if (resolvedCall.hasUnmappedArguments()) IdeRenderers.error(")") else ")")
    stringBuilder.append(" <i>defined in</i> ")
    val containingDeclaration = funDescriptor.getContainingDeclaration()
    val fqName = DescriptorUtils.getFqName(containingDeclaration)
    stringBuilder.append(if (FqName.ROOT.equalsTo(fqName)) "root package" else fqName.asString())
    return stringBuilder.toString()
}