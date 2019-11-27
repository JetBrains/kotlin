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

package org.jetbrains.kotlin.idea.editor.fixers

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.editor.KotlinSmartEnterHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class KotlinClassBodyFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is KtClassOrObject) return
        val declarationKeyword = psiElement.getDeclarationKeyword() ?: return

        val body = psiElement.body
        if (!body?.text.isNullOrBlank()) return

        var endOffset = psiElement.range.end

        if (body != null) {
            body.getPrevSiblingIgnoringWhitespaceAndComments()?.let {
                endOffset = it.endOffset
            }
        }

        val task = {
            val notInitializedSuperType = psiElement.superTypeListEntries.firstNotNullResult {
                if (it is KtSuperTypeCallEntry) return@firstNotNullResult null
                val ktClass = it.typeAsUserType?.referenceExpression?.mainReference?.resolve() as? KtClass ?: return@firstNotNullResult null
                if (ktClass.isInterface()) return@firstNotNullResult null
                val descriptor = ktClass.descriptor as? ClassDescriptor ?: return@firstNotNullResult null
                it to descriptor
            }
            val (superType, superTypeDescriptor) = notInitializedSuperType ?: (null to null)
            val isExistsSuperTypeCtorParams = superTypeDescriptor?.constructors?.any { it.valueParameters.isNotEmpty() } == true
            editor.document.insertString(endOffset, "{\n}")
            if (superType != null) {
                val superTypeEndOffset = superType.endOffset
                editor.document.insertString(superTypeEndOffset, "()")
                if (isExistsSuperTypeCtorParams) {
                    editor.caretModel.moveToOffset(superTypeEndOffset + 1)
                } else {
                    editor.caretModel.moveToOffset(declarationKeyword.startOffset)
                }
            } else {
                editor.caretModel.moveToOffset(declarationKeyword.startOffset)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressSynchronously(task, "Adding class body ...", true, psiElement.project)
    }
}