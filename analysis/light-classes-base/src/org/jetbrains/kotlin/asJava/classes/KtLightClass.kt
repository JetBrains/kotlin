/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImplementationDetail

interface KtLightClass : PsiClass, KtLightDeclaration<KtClassOrObject, PsiClass>, KtLightClassMarker

interface KtExtensibleLightClass : KtLightClass, PsiExtensibleClass {
    /**
     * Returns the [own methods][getOwnMethods] of this class whose [KtLightElement.kotlinOrigin] is [kotlinOrigin],
     * in the order of [getOwnMethods].
     *
     * The default implementation scans all own methods and computes the [KtLightElement.kotlinOrigin] of each one, which is
     * expensive: for light classes of decompiled declarations, `kotlinOrigin` is a search in the compiled file.
     * Symbol light classes override it with a lookup cached together with their members.
     */
    @KtImplementationDetail
    fun findOwnMethodsByKotlinOrigin(kotlinOrigin: KtElement): List<PsiMethod> =
        ownMethods.filter { (it as? KtLightElement<*, *>)?.kotlinOrigin == kotlinOrigin }

    /**
     * Returns the [own fields][getOwnFields] of this class whose [KtLightElement.kotlinOrigin] is [kotlinOrigin],
     * in the order of [getOwnFields].
     *
     * The default implementation scans all own fields, which is as expensive as in [findOwnMethodsByKotlinOrigin].
     * Symbol light classes override it with a lookup cached together with their members.
     *
     * @see findOwnMethodsByKotlinOrigin
     */
    @KtImplementationDetail
    fun findOwnFieldsByKotlinOrigin(kotlinOrigin: KtElement): List<PsiField> =
        ownFields.filter { (it as? KtLightElement<*, *>)?.kotlinOrigin == kotlinOrigin }
}