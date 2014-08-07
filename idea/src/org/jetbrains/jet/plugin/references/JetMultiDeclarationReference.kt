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

import org.jetbrains.jet.lang.psi.JetMultiDeclaration
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import com.intellij.openapi.util.TextRange
import java.util.Collections
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.source.PsiSourceElement
import org.jetbrains.jet.lang.resolve.source.getPsi
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor

class JetMultiDeclarationReference(element: JetMultiDeclaration) : JetMultiReference<JetMultiDeclaration>(element) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return expression.getEntries().map { entry ->
            context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)?.getCandidateDescriptor()
        }.filterNotNull()
    }

    override fun getRangeInElement(): TextRange? {
        val start = expression.getLPar()
        val end = expression.getRPar()
        if (start == null || end == null) return TextRange.EMPTY_RANGE
        return TextRange(start.getStartOffsetInParent(), end.getStartOffsetInParent())
    }

    override fun canRename(): Boolean {
        return resolveToDescriptors().all { it is CallableMemberDescriptor && it.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED}
    }

    override fun handleElementRename(newElementName: String?): PsiElement? {
        if (canRename()) return expression
        throw IncorrectOperationException()
    }
}
