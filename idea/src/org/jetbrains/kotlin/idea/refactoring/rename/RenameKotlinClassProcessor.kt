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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.asJava.KotlinLightClassForExplicitDeclaration
import org.jetbrains.kotlin.asJava.KotlinLightClassForPackage
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetConstructor
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class RenameKotlinClassProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is JetClassOrObject || element is KotlinLightClass || element is JetConstructor<*>
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
        return getJetClassOrObject(element, true, editor)
    }

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        val classOrObject = getJetClassOrObject(element, false, null) ?: return

        val file = classOrObject.getContainingJetFile()

        val virtualFile = file.virtualFile
        if (virtualFile != null) {
            val nameWithoutExtensions = virtualFile.nameWithoutExtension
            if (nameWithoutExtensions == classOrObject.name) {
                allRenames.put(file, newName + "." + virtualFile.extension)
            }
        }
    }

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        if (element is JetObjectDeclaration && element.isCompanion()) {
            return super.findReferences(element).filter { !it.isCompanionObjectClassReference() }
        }
        return super.findReferences(element)
    }

    private fun PsiReference.isCompanionObjectClassReference(): Boolean {
        if (this !is JetSimpleNameReference) {
            return false
        }
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        return bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element] != null
    }

    private fun getJetClassOrObject(element: PsiElement?, showErrors: Boolean, editor: Editor?): JetClassOrObject? = when (element) {
        is KotlinLightClass ->
            if (element is KotlinLightClassForExplicitDeclaration) {
                element.getOrigin()
            }
            else if (element is KotlinLightClassForPackage) {
                if (showErrors) {
                    CommonRefactoringUtil.showErrorHint(
                            element.project, editor,
                            JetBundle.message("rename.kotlin.package.class.error"),
                            RefactoringBundle.message("rename.title"),
                            null)
                }

                // Cancel rename
                null
            }
            else {
                assert(false) { "Should not be suggested to rename element of type " + element.javaClass + " " + element }
                null
            }

        is JetConstructor<*> ->
            element.getContainingClassOrObject()

        else ->
            element as? JetClassOrObject
    }
}
