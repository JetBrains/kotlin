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
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

interface KtLightMember<out D : PsiMember> : PsiMember, KtLightDeclaration<KtDeclaration, D>, PsiNameIdentifierOwner, PsiDocCommentOwner {
    val lightMemberOrigin: LightMemberOrigin?

    override fun getContainingClass(): KtLightClass
}

interface KtLightField : PsiField, KtLightMember<PsiField>, PsiVariableEx

interface KtLightParameter : PsiParameter, KtLightDeclaration<KtParameter, PsiParameter> {
    val method: KtLightMethod
}

interface KtLightMethod : PsiAnnotationMethod, KtLightMember<PsiMethod> {
    val isDelegated: Boolean
        get() = lightMemberOrigin?.originKind == JvmDeclarationOriginKind.DELEGATION
                || lightMemberOrigin?.originKind == JvmDeclarationOriginKind.CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL

    val isMangled: Boolean
}
