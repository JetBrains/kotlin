/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode

public abstract class KtPsiTypeProvider : KtAnalysisSessionComponent() {
    public abstract fun asPsiTypeElement(
        type: KtType,
        useSitePosition: PsiElement,
        mode: KtTypeMappingMode,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
        allowErrorTypes: Boolean,
    ): PsiTypeElement?

    public abstract fun asKtType(
        psiType: PsiType,
        useSitePosition: PsiElement
    ): KtType?
}

public interface KtPsiTypeProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Converts the given [KtType] to [PsiTypeElement] under [useSitePosition] context.
     *
     * [useSitePosition] is used as the parent of the resulting [PsiTypeElement],
     * which is in turn used to resolve [PsiTypeElement].
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
     * If [allowErrorTypes] set to true then erroneous types will be replaced with `error.NonExistentClass` type.
     *
     * [suppressWildcards] indicates whether wild cards in type arguments need to be suppressed or not,
     * e.g., according to the annotation on the containing declarations.
     *   `true` means they should be suppressed;
     *   `false` means they should appear;
     *   `null` is no-op by default, i.e., their suppression/appearance is determined by type annotations.
     *
     * Note: [PsiTypeElement] is JVM conception, so this method will return `null` for non-JVM platforms.
     */
    public fun KtType.asPsiTypeElement(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KtTypeMappingMode = KtTypeMappingMode.DEFAULT,
        isAnnotationMethod: Boolean = false,
        suppressWildcards: Boolean? = null,
    ): PsiTypeElement? = withValidityAssertion {
        analysisSession.psiTypeProvider.asPsiTypeElement(
            this,
            useSitePosition,
            mode,
            isAnnotationMethod,
            suppressWildcards,
            allowErrorTypes,
        )
    }

    /**
     * Converts the given [KtType] to [PsiType] under [useSitePosition] context.
     *
     * This simply unwraps [PsiTypeElement] returned from [asPsiTypeElement].
     * Use this version if type annotation is not required. Otherwise, use [asPsiTypeElement] to get [PsiTypeElement] as an owner of
     * annotations on [PsiType] and annotate the resulting [PsiType] with proper [PsiAnnotation][com.intellij.psi.PsiAnnotation].
     *
     * Note: [PsiType] is JVM conception, so this method will return `null` for non-JVM platforms.
     */
    public fun KtType.asPsiType(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KtTypeMappingMode = KtTypeMappingMode.DEFAULT,
        isAnnotationMethod: Boolean = false,
        suppressWildcards: Boolean? = null,
    ): PsiType? =
        asPsiTypeElement(
            useSitePosition,
            allowErrorTypes,
            mode,
            isAnnotationMethod,
            suppressWildcards,
        )?.type

    /**
     * Converts given [PsiType] to [KtType].
     *
     * [useSitePosition] may be used to clarify how to resolve some parts of [PsiType].
     * For instance, it can be used to collect type parameters and use them during the conversion.
     *
     * @receiver [PsiType] to be converted.
     * @return The converted [KtType], or null if conversion is not possible e.g., [PsiType] is not resolved
     */
    public fun PsiType.asKtType(useSitePosition: PsiElement): KtType? = withValidityAssertion {
        analysisSession.psiTypeProvider.asKtType(this, useSitePosition)
    }
}
