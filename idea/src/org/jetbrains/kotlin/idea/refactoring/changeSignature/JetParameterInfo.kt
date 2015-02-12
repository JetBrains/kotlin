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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.lang.ASTNode
import com.intellij.refactoring.changeSignature.ParameterInfo
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JetFunctionDefinitionUsage
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetModifierList

public class JetParameterInfo(
        val originalIndex: Int = -1,
        private var name: String,
        type: JetType? = null,
        var defaultValueForParameter: JetExpression? = null,
        var defaultValueForCall: String = "",
        valOrVarNode: ASTNode? = null,
        val modifierList: JetModifierList? = null
): ParameterInfo {
    val originalType: JetType? = type
    var currentTypeText: String = getOldTypeText()
    var valOrVar: JetValVar = valOrVarNode.toValVar()

    private fun getOldTypeText() = originalType?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) } ?: ""

    override fun getOldIndex(): Int = originalIndex

    public val isNewParameter: Boolean
        get() = originalIndex == -1

    override fun getDefaultValue(): String? = null

    override fun getName(): String = name

    override fun setName(name: String?) {
        this.name = name ?: ""
    }

    override fun getTypeText(): String = currentTypeText

    public val isTypeChanged: Boolean get() = getOldTypeText() != currentTypeText

    override fun isUseAnySingleVariable(): Boolean = false

    override fun setUseAnySingleVariable(b: Boolean) {
        throw UnsupportedOperationException()
    }

    public fun renderType(parameterIndex: Int, inheritedFunction: JetFunctionDefinitionUsage<*>): String {
        val typeSubstitutor = inheritedFunction.getOrCreateTypeSubstitutor() ?: return currentTypeText
        val currentBaseFunction = inheritedFunction.getBaseFunction().getCurrentFunctionDescriptor() ?: return currentTypeText
        val parameterType = currentBaseFunction.getValueParameters().get(parameterIndex).getType()
        return parameterType.renderTypeWithSubstitution(typeSubstitutor, currentTypeText, true)
    }

    public fun getInheritedName(inheritedFunction: JetFunctionDefinitionUsage<PsiElement>): String {
        if (!inheritedFunction.isInherited()) return name

        val baseFunction = inheritedFunction.getBaseFunction()
        val baseFunctionDescriptor = baseFunction.getOriginalFunctionDescriptor()

        val inheritedFunctionDescriptor = inheritedFunction.getOriginalFunctionDescriptor()
        val inheritedParameterDescriptors = inheritedFunctionDescriptor.getValueParameters()
        if (originalIndex < 0
            || originalIndex >= baseFunctionDescriptor.getValueParameters().size()
            || originalIndex >= inheritedParameterDescriptors.size()) return name

        val inheritedParamName = inheritedParameterDescriptors.get(originalIndex).getName().asString()
        val oldParamName = baseFunctionDescriptor.getValueParameters().get(originalIndex).getName().asString()

        return when {
            oldParamName == inheritedParamName && inheritedFunctionDescriptor !is AnonymousFunctionDescriptor -> name
            else -> inheritedParamName
        }
    }

    public fun requiresExplicitType(inheritedFunction: JetFunctionDefinitionUsage<PsiElement>): Boolean {
        val inheritedFunctionDescriptor = inheritedFunction.getOriginalFunctionDescriptor()
        if (inheritedFunctionDescriptor !is AnonymousFunctionDescriptor) return true

        if (originalIndex < 0) return !inheritedFunction.hasExpectedType()

        val inheritedParameterDescriptor = inheritedFunctionDescriptor.getValueParameters().get(originalIndex)
        val parameter = DescriptorToSourceUtils.descriptorToDeclaration(inheritedParameterDescriptor) as? JetParameter ?: return false
        return parameter.getTypeReference() != null
    }

    public fun getDeclarationSignature(parameterIndex: Int, inheritedFunction: JetFunctionDefinitionUsage<PsiElement>): String {
        val buffer = StringBuilder()

        if (modifierList != null) {
            buffer.append(modifierList.getText()).append(' ')
        }

        if (valOrVar != JetValVar.None) {
            buffer.append(valOrVar).append(' ')
        }

        buffer.append(getInheritedName(inheritedFunction))

        if (requiresExplicitType(inheritedFunction)) {
            buffer.append(": ").append(renderType(parameterIndex, inheritedFunction))
        }

        if (!inheritedFunction.isInherited()) {
            defaultValueForParameter?.let { buffer.append(" = ").append(it.getText()) }
        }

        return buffer.toString()
    }
}
