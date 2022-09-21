/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode

public abstract class KtPsiTypeProvider : KtAnalysisSessionComponent() {
    public abstract fun asPsiType(
        type: KtType,
        useSitePosition: PsiElement,
        mode: KtTypeMappingMode,
        isAnnotationMethod: Boolean,
        allowErrorTypes: Boolean
    ): PsiType?
}

public interface KtPsiTypeProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Converts the given [KtType] to [PsiType] under [useSitePosition] context.
     *
     * [useSitePosition] is used as the parent of the resulting [PsiType],
     * which is in turn used to resolve [PsiType].
     *
     * [useSitePosition] is also used to determine if the given [KtType] needs to be approximated.
     * For example, if the given type is local yet available in the same scope of use site, we can
     * still use such local type. Otherwise, e.g., exposed to public as a return type, the resulting
     * type will be approximated accordingly.
     *
     * If [allowErrorTypes] set to false then method returns `null` if the conversion encounters any
     * erroneous cases, e.g., errors in type arguments.
     * A client can handle such case in its own way. For instance,
     *   * UAST will return `UastErrorType` as a default error type.
     *
     * If [allowErrorTypes] set to true then erroneous types will be replaced with `error.NonExistentClass` type
     */
    public fun KtType.asPsiType(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KtTypeMappingMode = KtTypeMappingMode.DEFAULT,
        isAnnotationMethod: Boolean = false
    ): PsiType? = withValidityAssertion {
        analysisSession.psiTypeProvider.asPsiType(this, useSitePosition, mode, isAnnotationMethod, allowErrorTypes)
    }
}
