/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.MethodSignatureUtil
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KtDefaultAnnotationArgumentReference(element: KtValueArgument) : AbstractKtReference<KtValueArgument>(element) {
    override val resolver: ResolveCache.PolyVariantResolver<KtReference>
        get() = Resolver

    override val resolvesByNames: Collection<Name>
        get() = listOf(Name.identifier(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME))

    override fun getRangeInElement() = TextRange.EMPTY_RANGE

    override fun getCanonicalText() = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME

    override fun isReferenceTo(candidateTarget: PsiElement): Boolean {
        val unwrapped = candidateTarget.unwrapped
        return (unwrapped is PsiMethod || unwrapped is KtParameter) && unwrapped == resolve()
    }

    override fun canRename(): Boolean = true

    private object Resolver : ResolveCache.PolyVariantResolver<KtReference> {
        override fun resolve(t: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
            require(t is KtDefaultAnnotationArgumentReference)
            val annotationPsi = t.resolveAnnotationCallee() ?: return emptyArray()
            val name = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME
            return when (annotationPsi) {
                is PsiClass -> {
                    val signature = MethodSignatureUtil.createMethodSignature(
                        name, PsiType.EMPTY_ARRAY, PsiTypeParameter.EMPTY_ARRAY, PsiSubstitutor.EMPTY
                    )
                    val method = MethodSignatureUtil.findMethodBySignature(annotationPsi, signature, false) ?: return emptyArray()
                    arrayOf(PsiElementResolveResult(method))
                }
                is KtPrimaryConstructor -> {
                    val property = annotationPsi.containingClassOrObject?.findPropertyByName(name) as? KtParameter ?: return emptyArray()
                    arrayOf(PsiElementResolveResult(property))
                }
                else -> emptyArray()
            }
        }

        private fun KtDefaultAnnotationArgumentReference.resolveAnnotationCallee(): PsiElement? =
            element.getStrictParentOfType<KtAnnotationEntry>()
                ?.calleeExpression
                ?.constructorReferenceExpression
                ?.mainReference
                ?.resolve()

    }
}
