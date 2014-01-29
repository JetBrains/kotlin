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

package org.jetbrains.jet.plugin.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetPropertyDelegate
import org.jetbrains.jet.lang.resolve.BindingContext
import java.util.Collections

public class JetPropertyDelegationMethodsReference(element: JetPropertyDelegate) : JetMultiReference<JetPropertyDelegate>(element) {

    override fun getRangeInElement(): TextRange {
        val byKeywordNode = expression.getByKeywordNode()
        val offset = byKeywordNode.getPsi()!!.getStartOffsetInParent()
        return TextRange(offset, offset + byKeywordNode.getTextLength())
    }

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val property = PsiTreeUtil.getParentOfType(expression, javaClass<JetProperty>())
        if (property == null) {
            return Collections.emptyList()
        }
        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property)
        if (descriptor !is PropertyDescriptor) {
            return Collections.emptyList()
        }
        return descriptor.getAccessors().map {
            accessor ->
            val candidateDescriptor = context.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor)?.getCandidateDescriptor()
            //TODO: should not getOriginal here, because candidate descriptor should not have substituted type parameters
            // remove after problem is solved
            candidateDescriptor?.getOriginal()
        }.filterNotNull()
    }
}
