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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinTypeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

class ChangeParameterTypeFix(element: KtParameter, type: KotlinType) : KotlinQuickFixAction<KtParameter>(element) {
    private val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
    private val typeInfo = KotlinTypeInfo(isCovariant = false, text = IdeDescriptorRenderers.SOURCE_CODE_NOT_NULL_TYPE_APPROXIMATION.renderType(type))

    private val containingDeclarationName: String?
    private val isPrimaryConstructorParameter: Boolean

    init {
        val declaration = PsiTreeUtil.getParentOfType(element, KtNamedDeclaration::class.java)
        this.containingDeclarationName = declaration?.fqName?.asString() ?: declaration?.name
        this.isPrimaryConstructorParameter = declaration is KtPrimaryConstructor
    }

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file) && containingDeclarationName != null
    }

    override fun getText(): String {
        val element = element ?: return ""
        return if (isPrimaryConstructorParameter)
            "Change parameter '${element.name}' type of primary constructor of class '$containingDeclarationName' to '$typePresentation'"
        else
            "Change parameter '${element.name}' type of function '$containingDeclarationName' to '$typePresentation'"
    }

    override fun getFamilyName() = KotlinBundle.message("change.type.family")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val function = element.getStrictParentOfType<KtFunction>() ?: return
        val parameterIndex = function.valueParameters.indexOf(element)
        val context = function.analyze()
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, function] as? FunctionDescriptor ?: return
        val configuration = object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor) = originalDescriptor.apply {
                parameters[if (receiver != null) parameterIndex + 1 else parameterIndex].currentTypeInfo = typeInfo
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>) = true
        }
        runChangeSignature(element.project, descriptor, configuration, element, text)
    }
}
