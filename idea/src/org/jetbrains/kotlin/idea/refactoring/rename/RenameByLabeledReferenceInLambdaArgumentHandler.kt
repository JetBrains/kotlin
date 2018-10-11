/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.project
import org.jetbrains.kotlin.idea.references.getCalleeByLambdaArgument
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getLabeledParent
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RenameByLabeledReferenceInLambdaArgumentHandler :
        AbstractReferenceSubstitutionRenameHandler(KotlinVariableInplaceRenameHandler()) {
    override fun getElementToRename(dataContext: DataContext): PsiElement? {
        val refExpr = getReferenceExpression(dataContext) as? KtLabelReferenceExpression ?: return null
        val context = refExpr.analyze(BodyResolveMode.PARTIAL)
        val lambda = context[BindingContext.LABEL_TARGET, refExpr] as? KtFunction ?: return null
        val labeledParent = lambda.getLabeledParent(refExpr.getReferencedName())
        return if (labeledParent != null) {
            labeledParent
        }
        else {
            val calleeExpression = lambda.getCalleeByLambdaArgument() ?: return null
            val descriptor = context[BindingContext.REFERENCE_TARGET, calleeExpression] as? FunctionDescriptor ?: return null
            DescriptorToSourceUtilsIde.getAnyDeclaration(dataContext.project, descriptor)
        }
    }
}
