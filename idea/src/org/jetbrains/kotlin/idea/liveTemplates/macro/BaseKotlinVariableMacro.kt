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

package org.jetbrains.kotlin.idea.liveTemplates.macro

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Macro
import com.intellij.codeInsight.template.Result
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.IterableTypesDetection
import org.jetbrains.kotlin.idea.core.IterableTypesDetector
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallableWithImplicitReceiver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import java.util.*

abstract class BaseKotlinVariableMacro : Macro() {
    private fun getVariables(params: Array<Expression>, context: ExpressionContext): Collection<KtNamedDeclaration> {
        if (params.size != 0) return emptyList()

        val project = context.project
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(context.editor!!.document) as? KtFile ?: return emptyList()

        val contextExpression = findContextExpression(psiFile, context.startOffset) ?: return emptyList()

        val resolutionFacade = contextExpression.getResolutionFacade()

        val bindingContext = resolutionFacade.analyze(contextExpression, BodyResolveMode.FULL)
        val scope = contextExpression.getResolutionScope(bindingContext, resolutionFacade)

        val detector = resolutionFacade.getIdeService(IterableTypesDetection::class.java).createDetector(scope)

        val dataFlowInfo = bindingContext.getDataFlowInfo(contextExpression)

        val filteredDescriptors = ArrayList<VariableDescriptor>()
        for (declarationDescriptor in getAllVariables(scope)) {
            if (declarationDescriptor is VariableDescriptor) {

                if (declarationDescriptor.extensionReceiverParameter != null && declarationDescriptor.substituteExtensionIfCallableWithImplicitReceiver(scope, bindingContext, dataFlowInfo).isEmpty()) {
                    continue
                }

                if (isSuitable(declarationDescriptor, project, detector)) {
                    filteredDescriptors.add(declarationDescriptor)
                }
            }
        }


        val declarations = ArrayList<KtNamedDeclaration>()
        for (declarationDescriptor in filteredDescriptors) {
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor)
            assert(declaration == null || declaration is PsiNamedElement)

            if (declaration is KtProperty) {
                declarations.add(declaration)
            }
            else if (declaration is KtParameter) {
                declarations.add(declaration)
            }
        }

        return declarations
    }

    private fun getAllVariables(scope: LexicalScope): Collection<DeclarationDescriptor> {
        val result = ContainerUtil.newArrayList<DeclarationDescriptor>()
        result.addAll(scope.collectDescriptorsFiltered(DescriptorKindFilter.VARIABLES, MemberScope.ALL_NAME_FILTER))
        for (implicitReceiver in scope.getImplicitReceiversHierarchy()) {
            result.addAll(DescriptorUtils.getAllDescriptors(implicitReceiver.type.memberScope))
        }
        return result
    }

    protected abstract fun isSuitable(
            variableDescriptor: VariableDescriptor,
            project: Project,
            iterableTypesDetector: IterableTypesDetector): Boolean

    private fun findContextExpression(psiFile: PsiFile, startOffset: Int): KtExpression? {
        var e = psiFile.findElementAt(startOffset)
        while (e != null) {
            if (e is KtExpression) {
                return e
            }
            e = e.parent
        }
        return null
    }

    override fun calculateResult(params: Array<Expression>, context: ExpressionContext): Result? {
        val vars = getVariables(params, context)
        if (vars.isEmpty()) return null
        return KotlinPsiElementResult(vars.first())
    }

    override fun calculateLookupItems(params: Array<Expression>, context: ExpressionContext): Array<LookupElement>? {
        val vars = getVariables(params, context)
        if (vars.size < 2) return null
        return vars.map { LookupElementBuilder.create(it) }.toTypedArray()
    }
}
