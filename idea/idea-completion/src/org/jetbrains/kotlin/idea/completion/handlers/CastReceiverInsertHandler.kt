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

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.core.completion.DeclarationDescriptorLookupObject
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*

object CastReceiverInsertHandler : KotlinCallableInsertHandler() {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        val expression = PsiTreeUtil.findElementOfClassAtOffset(context.getFile(), context.getStartOffset(), javaClass<JetSimpleNameExpression>(), false)
        val qualifiedExpression = PsiTreeUtil.getParentOfType(expression, javaClass<JetQualifiedExpression>(), true)
        if (qualifiedExpression != null) {
            val receiver = qualifiedExpression.getReceiverExpression()

            val descriptor = (item.getObject() as? DeclarationDescriptorLookupObject)?.descriptor as CallableDescriptor
            val project = context.getProject()

            val thisObj = if (descriptor.getExtensionReceiverParameter() != null) descriptor.getExtensionReceiverParameter() else descriptor.getDispatchReceiverParameter()
            val fqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(thisObj.getType().getConstructor().getDeclarationDescriptor())

            val parentCast = JetPsiFactory(project).createExpression("(expr as $fqName)") as JetParenthesizedExpression
            val cast = parentCast.getExpression() as JetBinaryExpressionWithTypeRHS
            cast.getLeft().replace(receiver)

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.commitAllDocuments()
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.getDocument())

            val expr = receiver.replace(parentCast) as JetParenthesizedExpression

            ShortenReferences.DEFAULT.process((expr.getExpression() as JetBinaryExpressionWithTypeRHS).getRight())
        }
    }
}