/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtElement
import kotlin.getValue

class KtLightPsiNameValuePair(
    override val kotlinOrigin: KtElement,
    private val name: String,
    lightParent: PsiElement,
    private val argument: (KtLightPsiNameValuePair) -> PsiAnnotationMemberValue?
) : KtLightElementBase(lightParent),
    PsiNameValuePair {

    override fun setValue(newValue: PsiAnnotationMemberValue): PsiAnnotationMemberValue = cannotModify()

    override fun getNameIdentifier(): PsiIdentifier? = LightIdentifier(kotlinOrigin.manager, name)

    override fun getName(): String? = name

    private val _value: PsiAnnotationMemberValue? by lazyPub { argument(this) }

    override fun getValue(): PsiAnnotationMemberValue? = _value

    override fun getLiteralValue(): String? = (value as? PsiLiteralExpression)?.value?.toString()

}