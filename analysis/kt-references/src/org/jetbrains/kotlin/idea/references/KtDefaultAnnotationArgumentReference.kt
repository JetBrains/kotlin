/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch

/**
 * A reference pointing to a single argument in the annotation's constructor call.
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
 *
 * **Note**: the reference might be resolved only when the annotation parameter has [PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME] name.
 */
@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KtDefaultAnnotationArgumentReference(
    element: KtValueArgument,
) : AbstractKtReference<KtValueArgument>(element) {
    override val resolvesByNames: Collection<Name>
        get() = listOf(Name.identifier(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME))

    override fun getRangeInElement(): TextRange = TextRange.EMPTY_RANGE

    override fun getCanonicalText(): String = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME

    protected val PsiElement.isDefaultAnnotationMethod: Boolean
        get() = this is PsiMethod &&
                PsiUtil.isAnnotationMethod(this) &&
                name == PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME
                && parameterList.parametersCount == 0

    override fun isReferenceTo(candidateTarget: PsiElement): Boolean {
        return candidateTarget.isDefaultAnnotationMethod && candidateTarget == resolve()
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
