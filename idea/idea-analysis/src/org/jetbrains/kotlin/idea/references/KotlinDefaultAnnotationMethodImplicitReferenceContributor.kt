/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

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

internal val KotlinDefaultAnnotationMethodImplicitReferenceProvider = provider@{ element: KtValueArgument ->
    if (element.isNamed()) return@provider null
    val annotationEntry = element.getParentOfTypeAndBranch<KtAnnotationEntry> { valueArgumentList } ?: return@provider null
    if (annotationEntry.valueArguments.size != 1) return@provider null

    ReferenceImpl(element)
}