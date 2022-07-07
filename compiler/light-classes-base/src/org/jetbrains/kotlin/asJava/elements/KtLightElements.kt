/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.impl.PsiVariableEx
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

interface KtLightElement<out T : KtElement, out D : PsiElement> : PsiElement {
    val kotlinOrigin: T?

    /**
     * KtLightModifierList by default retrieves annotation from the relevant KtElement or from clsDelegate
     * But we have none of them for KtUltraLightAnnotationForDescriptor built upon descriptor
     * For that case, KtLightModifierList in the beginning checks `givenAnnotations` and uses them if it's not null
     * Probably, it's a bit dirty solution. But, for now it's not clear how to make it better
     */
    val givenAnnotations: List<KtLightAbstractAnnotation>? get() = null
}

interface KtLightDeclaration<out T : KtDeclaration, out D : PsiElement> : KtLightElement<T, D>, PsiNamedElement

interface KtLightMember<out D : PsiMember> : PsiMember, KtLightDeclaration<KtDeclaration, D>, PsiNameIdentifierOwner, PsiDocCommentOwner {
    val lightMemberOrigin: LightMemberOrigin?

    override fun getContainingClass(): KtLightClass
}

interface KtLightField : PsiField, KtLightMember<PsiField>, PsiVariableEx

interface KtLightParameter : PsiParameter, KtLightDeclaration<KtParameter, PsiParameter> {
    val method: KtLightMethod
}

interface KtLightFieldForSourceDeclarationSupport : PsiField {
    val kotlinOrigin: KtDeclaration?
}

interface KtLightMethod : PsiAnnotationMethod, KtLightMember<PsiMethod> {
    val isDelegated: Boolean
        get() = lightMemberOrigin?.originKind == JvmDeclarationOriginKind.DELEGATION
                || lightMemberOrigin?.originKind == JvmDeclarationOriginKind.CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL

    val isMangled: Boolean
}
