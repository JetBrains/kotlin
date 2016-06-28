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

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinDefaultAnnotationMethodImplicitReferenceContributor : AbstractKotlinReferenceContributor() {
    class ReferenceImpl(private val argument: KtValueArgument) : PsiReference {
        private fun resolveAnnotationCallee(): PsiElement? = argument.getStrictParentOfType<KtAnnotationEntry>()
                ?.calleeExpression
                ?.constructorReferenceExpression
                ?.mainReference
                ?.resolve()

        override fun getElement() = argument

        override fun getRangeInElement() = TextRange.EMPTY_RANGE

        override fun resolve(): PsiElement? {
            val annotationPsi = resolveAnnotationCallee() ?: return null
            val name = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME
            return when (annotationPsi) {
                is PsiClass -> {
                    val signature = MethodSignatureUtil.createMethodSignature(
                            name, PsiType.EMPTY_ARRAY, PsiTypeParameter.EMPTY_ARRAY, PsiSubstitutor.EMPTY
                    )
                    MethodSignatureUtil.findMethodBySignature(annotationPsi, signature, false)
                }
                is KtPrimaryConstructor -> annotationPsi.containingClassOrObject?.findPropertyByName(name) as? KtParameter
                else -> null
            }
        }

        override fun getCanonicalText() = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME

        override fun handleElementRename(newElementName: String): PsiElement {
            val psiFactory = KtPsiFactory(argument)
            val newArgument = psiFactory.createArgument(
                    argument.getArgumentExpression(),
                    Name.identifier(newElementName.quoteIfNeeded()),
                    argument.getSpreadElement() != null
            )
            return argument.replaced(newArgument)
        }

        override fun bindToElement(element: PsiElement) = throw IncorrectOperationException("Not implemented")

        override fun isReferenceTo(element: PsiElement): Boolean {
            val unwrapped = element.unwrapped
            return (unwrapped is PsiMethod || unwrapped is KtParameter) && unwrapped == resolve()
        }

        override fun getVariants() = ArrayUtil.EMPTY_OBJECT_ARRAY

        override fun isSoft() = false
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerProvider<KtValueArgument> {
            if (it.isNamed()) return@registerProvider null
            val annotationEntry = it.getParentOfTypeAndBranch<KtAnnotationEntry> { valueArgumentList }
                                  ?: return@registerProvider null
            if (annotationEntry.valueArguments.size != 1) return@registerProvider null

            ReferenceImpl(it)
        }
    }
}
