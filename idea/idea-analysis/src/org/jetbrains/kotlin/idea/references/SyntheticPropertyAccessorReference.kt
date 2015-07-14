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

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetNameReferenceExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.synthetic.SyntheticJavaBeansPropertyDescriptor
import org.jetbrains.kotlin.utils.addIfNotNull

sealed class SyntheticPropertyAccessorReference(expression: JetNameReferenceExpression, private val getter: Boolean) : JetSimpleReference<JetNameReferenceExpression>(expression) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val descriptors = super.getTargetDescriptors(context)
        if (descriptors.none { it is SyntheticJavaBeansPropertyDescriptor }) return emptyList()

        val result = SmartList<FunctionDescriptor>()
        for (descriptor in descriptors) {
            if (descriptor is SyntheticJavaBeansPropertyDescriptor) {
                if (getter) {
                    result.add(descriptor.getMethod)
                }
                else {
                    result.addIfNotNull(descriptor.setMethod)
                }
            }
        }
        return result
    }

    override fun getRangeInElement() = TextRange(0, expression.getTextLength())

    override fun canRename() = true

    override fun handleElementRename(newElementName: String?): PsiElement? {
        if (!Name.isValidIdentifier(newElementName!!)) return expression

        val newNameAsName = Name.identifier(newElementName)
        val newName = if (getter)
            SyntheticJavaBeansPropertyDescriptor.propertyNameByGetMethodName(newNameAsName)
        else
            SyntheticJavaBeansPropertyDescriptor.propertyNameBySetMethodName(newNameAsName)
        if (newName == null) return expression //TODO: handle the case when get/set becomes ordinary method

        val nameIdentifier = JetPsiFactory(expression).createNameIdentifier(newName.getIdentifier())
        expression.getReferencedNameElement().replace(nameIdentifier)
        return expression
    }

    public class Getter(expression: JetNameReferenceExpression) : SyntheticPropertyAccessorReference(expression, true)
    public class Setter(expression: JetNameReferenceExpression) : SyntheticPropertyAccessorReference(expression, false)
}
