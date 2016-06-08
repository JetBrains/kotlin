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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.KtLightClass
import org.jetbrains.kotlin.asJava.KtLightClassForExplicitDeclaration
import org.jetbrains.kotlin.asJava.KtLightClassForFacade
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RenameKotlinClassProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtClassOrObject || element is KtLightClass || element is KtConstructor<*>
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?) = getClassOrObject(element)

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        super.prepareRenaming(element, newName, allRenames)

        val classOrObject = getClassOrObject(element) as? KtClassOrObject ?: return

        val file = classOrObject.getContainingKtFile()

        val virtualFile = file.virtualFile
        if (virtualFile != null) {
            val nameWithoutExtensions = virtualFile.nameWithoutExtension
            if (nameWithoutExtensions == classOrObject.name) {
                allRenames.put(file, newName + "." + virtualFile.extension)
            }
        }
    }

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        if (element is KtObjectDeclaration && element.isCompanion()) {
            return super.findReferences(element).filter { !it.isCompanionObjectClassReference() }
        }
        return super.findReferences(element)
    }

    private fun PsiReference.isCompanionObjectClassReference(): Boolean {
        if (this !is KtSimpleNameReference) {
            return false
        }
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        return bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element] != null
    }

    private fun getClassOrObject(element: PsiElement?): PsiElement? = when (element) {
        is KtLightClass ->
            when (element) {
                is KtLightClassForExplicitDeclaration -> element.kotlinOrigin
                is KtLightClassForFacade -> element
                else -> throw AssertionError("Should not be suggested to rename element of type " + element.javaClass + " " + element)
            }

        is KtConstructor<*> ->
            element.getContainingClassOrObject()

        else ->
            element as? KtClassOrObject
    }
}
