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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddTypeAnnotationToValueParameterFix(element: KtParameter) : KotlinQuickFixAction<KtParameter>(element) {
    private val typeNameShort : String?
    val typeName: String?

    init {
        val defaultValue = element.defaultValue
        val type = defaultValue?.getType(defaultValue.analyze(BodyResolveMode.PARTIAL))

        typeNameShort = type?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) }
        typeName = type?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return element.typeReference == null && typeNameShort != null
    }

    override fun getFamilyName() = "Add type annotation"
    override fun getText() = element?.let { "Add type '$typeNameShort' to parameter '${it.name}'" } ?: ""

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        if (typeName != null) {
            element.typeReference = KtPsiFactory(element).createType(typeName)
            ShortenReferences.DEFAULT.process(element)
        }
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): AddTypeAnnotationToValueParameterFix? {
            val element = Errors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.cast(diagnostic).psiElement
            if (element.defaultValue == null) return null
            return AddTypeAnnotationToValueParameterFix(element)
        }
    }
}