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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.codeInsight.highlighting.JavaReadWriteAccessDetector
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.references.ReferenceAccess
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class KotlinReadWriteAccessDetector : ReadWriteAccessDetector() {
    companion object {
        val INSTANCE = KotlinReadWriteAccessDetector()
    }

    override fun isReadWriteAccessible(element: PsiElement) = element is KtVariableDeclaration || element is KtParameter

    override fun isDeclarationWriteAccess(element: PsiElement) = isReadWriteAccessible(element)

    override fun getReferenceAccess(referencedElement: PsiElement, reference: PsiReference): Access {
        if (!isReadWriteAccessible(referencedElement)) {
            return Access.Read
        }

        val refTarget = reference.resolve()
        if (refTarget is KtLightMethod) {
            val origin = refTarget.kotlinOrigin
            val declaration: KtNamedDeclaration = when (origin) {
                is KtPropertyAccessor -> origin.getNonStrictParentOfType<KtProperty>()
                is KtProperty, is KtParameter -> origin as KtNamedDeclaration
                else -> null
            } ?: return Access.ReadWrite

            return when (refTarget.name) {
                JvmAbi.getterName(declaration.name!!) -> return Access.Read
                JvmAbi.setterName(declaration.name!!) -> return Access.Write
                else -> Access.ReadWrite
            }
        }

        return getExpressionAccess(reference.element)
    }

    override fun getExpressionAccess(expression: PsiElement): Access {
        if (expression !is KtExpression) { //TODO: there should be a more correct scheme of access type detection for cross-language references
            return JavaReadWriteAccessDetector().getExpressionAccess(expression)
        }

        return when (expression.readWriteAccess(useResolveForReadWrite = true)) {
            ReferenceAccess.READ -> Access.Read
            ReferenceAccess.WRITE -> Access.Write
            ReferenceAccess.READ_WRITE -> Access.ReadWrite
        }
    }
}