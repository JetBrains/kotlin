/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolution.KtResolvable

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
@OptIn(KtExperimentalApi::class)
@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KtDefaultAnnotationArgumentReference(
    element: KtValueArgument,
) : AbstractKtReference<KtValueArgument>(element), KtResolvable {
    override val resolvesByNames: Collection<Name>
        get() = emptyList()

    override fun getRangeInElement(): TextRange = TextRange.EMPTY_RANGE

    override fun getCanonicalText(): String = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME

    override fun isReferenceTo(candidateTarget: PsiElement): Boolean {
        val unwrapped = candidateTarget.unwrapped
        return (unwrapped is PsiMethod || unwrapped is KtParameter) && unwrapped == resolve()
    }

    override fun canRename(): Boolean = true

    @KtImplementationDetail
    companion object {
        @KtImplementationDetail
        fun KtValueArgument.shouldProduceReference(): Boolean = !isNamed() &&
                getParentOfTypeAndBranch<KtAnnotationEntry> { valueArgumentList }
                    ?.valueArguments
                    ?.size == 1
    }
}
