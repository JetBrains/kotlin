/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.impl.PsiVariableEx
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

interface KtLightElement<out T : KtElement, out D : PsiElement> : PsiElement {
    val kotlinOrigin: T?

    val clsDelegate: D
}

interface KtLightDeclaration<out T : KtDeclaration, out D : PsiElement> : KtLightElement<T, D>, PsiNamedElement

interface KtLightMember<out D : PsiMember> : PsiMember, KtLightDeclaration<KtDeclaration, D>, PsiNameIdentifierOwner, PsiDocCommentOwner {
    val lightMemberOrigin: LightMemberOrigin?

    override fun getContainingClass(): KtLightClass
}

interface KtLightField : PsiField, KtLightMember<PsiField>, PsiVariableEx

interface KtLightMethod : PsiAnnotationMethod, KtLightMember<PsiMethod> {
    val isDelegated: Boolean
        get() = lightMemberOrigin?.originKind == JvmDeclarationOriginKind.DELEGATION
                || lightMemberOrigin?.originKind == JvmDeclarationOriginKind.CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL
}