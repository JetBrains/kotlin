/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeAlias

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.placeDeclarationInContainer
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.types.KotlinType

class TypeAliasInfo(
        val name: String,
        val targetParent: PsiElement,
        val typeParameterNames: List<String>,
        val expectedType: KotlinType?
)

class CreateTypeAliasFromUsageFix<E : KtElement>(
        element: E,
        private val aliasInfo: TypeAliasInfo
) : CreateFromUsageFixBase<E>(element), LowPriorityAction {
    override fun getText() = "Create type alias '${aliasInfo.name}'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        if (editor == null) return

        val typeAliasProto = KtPsiFactory(project).createTypeAlias(aliasInfo.name, aliasInfo.typeParameterNames, "Dummy")
        val typeAlias = placeDeclarationInContainer(typeAliasProto, aliasInfo.targetParent, element)

        if (!ApplicationManager.getApplication().isUnitTestMode) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

            val aliasBody = typeAlias.getTypeReference()!!

            with(TemplateBuilderImpl(typeAlias)) {
                for ((typeParameter, typeParameterName) in (typeAlias.typeParameters zip aliasInfo.typeParameterNames)) {
                    replaceElement(typeParameter, ConstantNode(typeParameterName))
                }
                val defaultBodyText = aliasInfo.expectedType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) } ?: "Any"
                replaceElement(aliasBody, ConstantNode(defaultBodyText))

                run(editor, true)
            }
        }
    }
}
