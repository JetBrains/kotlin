/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.LightClassInheritanceHelper
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

//TODO Make fake class symbol based

class SymbolBasedFakeLightClass(kotlinOrigin: KtClassOrObject) :
    KtFakeLightClass(kotlinOrigin) {

    override fun copy(): KtFakeLightClass = SymbolBasedFakeLightClass(kotlinOrigin)

    private val _containingClass: KtFakeLightClass? by lazy {
        kotlinOrigin.containingClassOrObject?.let { SymbolBasedFakeLightClass(it) }
    }

    override fun getContainingClass(): KtFakeLightClass? = _containingClass

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (manager.areElementsEquivalent(baseClass, this)) return false
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        val baseClassOrigin = (baseClass as? KtLightClass)?.kotlinOrigin ?: return false
        return analyzeForLightClasses(baseClassOrigin) {
            kotlinOrigin.checkIsInheritor(baseClassOrigin, checkDeep)
        }
    }
}