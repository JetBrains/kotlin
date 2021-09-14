/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode

public abstract class KtPsiTypeProvider : KtAnalysisSessionComponent() {
    public abstract fun asPsiType(type: KtType, context: PsiElement, mode: TypeMappingMode): PsiType?
}

public interface KtPsiTypeProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Converts the given [KtType] to [PsiType].
     *
     * Returns `null` if the conversion encounters any erroneous cases, e.g., errors in type arguments.
     * A client can handle such case in its own way. For instance,
     *   * UAST will return `UastErrorType` as a default error type.
     *   * LC will return `NonExistentClass` from the [context].
     */
    public fun KtType.asPsiType(
        context: PsiElement,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
    ): PsiType? =
        analysisSession.psiTypeProvider.asPsiType(this, context, mode)
}
