/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiClass
import com.intellij.psi.impl.InheritanceImplUtil
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.LightClassInheritanceHelper
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

//TODO Make fake class symbol based
class KtFirBasedFakeLightClass(kotlinOrigin: KtClassOrObject) : KtFakeLightClass(kotlinOrigin) {
    override fun copy(): KtFakeLightClass = KtFirBasedFakeLightClass(kotlinOrigin)

    private val _containingClass: KtFakeLightClass? by lazy {
        kotlinOrigin.containingClassOrObject?.let { KtFirBasedFakeLightClass(it) }
    }

    override fun getContainingClass(): KtFakeLightClass? = _containingClass

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        //TODO Implement correct inheritance check with symbols
        if (manager.areElementsEquivalent(baseClass, this)) return false
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        if ((baseClass as? KtLightClass)?.kotlinOrigin == null) return false
        return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)
    }
}