/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.MethodSignatureUtil
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor

/**
 * A reference pointing to a single argument in annotation's constructor call.
 * It is created only when the following conditions hold:
 *
 * - Annotation's constructor call has a **single** argument
 * - This argument is passed as is, without an explicit name
 *
 * Examples:
 * ```
 * @Foo("bar")        // reference is created
 * @Foo("bar", "baz") // no reference, there are two arguments
 * @Foo(name = "bar") // no reference, named argument is used
 * ```
 */
@OptIn(KtImplementationDetail::class)
class KtDefaultAnnotationArgumentReference(element: KtValueArgument) : AbstractKtReference<KtValueArgument>(element) {
    override val resolver: ResolveCache.PolyVariantResolver<KtReference>
        get() = Resolver

    override val resolvesByNames: Collection<Name>
        get() = emptyList()

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
            return when (annotationPsi) {
                is PsiClass -> {
                    val name = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME
                    val signature = MethodSignatureUtil.createMethodSignature(
                        name, PsiType.EMPTY_ARRAY, PsiTypeParameter.EMPTY_ARRAY, PsiSubstitutor.EMPTY
                    )
                    val method = MethodSignatureUtil.findMethodBySignature(annotationPsi, signature, false) ?: return emptyArray()
                    arrayOf(PsiElementResolveResult(method))
                }
                is KtPrimaryConstructor -> {
                    // parameters in primary constructor on Kotlin annotation can have any names,
                    // so we just take the first parameter
                    val property = annotationPsi.valueParameters.firstOrNull() ?: return emptyArray()
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

    class Provider : KotlinPsiReferenceProviderContributor<KtValueArgument> {
        override val elementClass: Class<KtValueArgument>
            get() = KtValueArgument::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtValueArgument>
            get() = { element ->
                val reference = when {
                    element.isNamed() -> null
                    element.getParentOfTypeAndBranch<KtAnnotationEntry> { valueArgumentList }?.valueArguments?.size != 1 -> null
                    else -> KtDefaultAnnotationArgumentReference(element)
                }

                listOfNotNull(reference)
            }
    }
}
