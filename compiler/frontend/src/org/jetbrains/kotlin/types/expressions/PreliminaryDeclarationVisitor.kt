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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class PreliminaryDeclarationVisitor(val declaration: KtDeclaration): AssignedVariablesSearcher() {

    override fun writers(variableDescriptor: VariableDescriptor): MutableSet<KtDeclaration?> {
        lazyTrigger
        return super.writers(variableDescriptor)
    }

    private val lazyTrigger by lazy {
        declaration.accept(this)
    }

    companion object {

        fun createForExpression(expression: KtExpression, trace: BindingTrace) {
            expression.getStrictParentOfType<KtDeclaration>()?.let { createForDeclaration(it, trace) }
        }

        private fun topMostNonClassDeclaration(declaration: KtDeclaration) =
                declaration.parentsWithSelf.filterIsInstance<KtDeclaration>().findLast { it !is KtClassOrObject } ?: declaration

        fun createForDeclaration(declaration: KtDeclaration, trace: BindingTrace) {
            val visitorOwner = topMostNonClassDeclaration(declaration)
            if (trace.get(BindingContext.PRELIMINARY_VISITOR, visitorOwner) != null) return
            trace.record(BindingContext.PRELIMINARY_VISITOR, visitorOwner, PreliminaryDeclarationVisitor(visitorOwner))
        }

        fun getVisitorByVariable(variableDescriptor: VariableDescriptor, bindingContext: BindingContext): PreliminaryDeclarationVisitor? {
            // Search for preliminary visitor of parent descriptor
            val containingDescriptor = variableDescriptor.containingDeclaration
            var currentDeclaration: KtDeclaration? =
                    DescriptorToSourceUtils.descriptorToDeclaration(containingDescriptor) as? KtDeclaration ?: return null
            var preliminaryVisitor = bindingContext.get(BindingContext.PRELIMINARY_VISITOR, currentDeclaration)
            while (preliminaryVisitor == null && currentDeclaration != null) {
                currentDeclaration = currentDeclaration.getStrictParentOfType()
                preliminaryVisitor = bindingContext.get(BindingContext.PRELIMINARY_VISITOR, currentDeclaration)
            }
            return preliminaryVisitor
        }
    }
}