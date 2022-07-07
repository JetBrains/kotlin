/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.light.LightMethod
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.toFakeLightClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils
import javax.swing.Icon

class KtDescriptorBasedFakeLightClass(kotlinOrigin: KtClassOrObject) : KtFakeLightClass(kotlinOrigin) {
    override fun copy(): KtFakeLightClass = KtDescriptorBasedFakeLightClass(kotlinOrigin)

    private val _containingClass: KtFakeLightClass? by lazy {
        kotlinOrigin.containingClassOrObject?.let { KtDescriptorBasedFakeLightClass(it) }
    }

    override fun getContainingClass(): KtFakeLightClass? = _containingClass

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (manager.areElementsEquivalent(baseClass, this)) return false
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        val baseKtClass = (baseClass as? KtLightClass)?.kotlinOrigin ?: return false

        val generationSupport = LightClassGenerationSupport.getInstance(project)

        val baseDescriptor = generationSupport.resolveToDescriptor(baseKtClass) as? ClassDescriptor ?: return false
        val thisDescriptor = generationSupport.resolveToDescriptor(kotlinOrigin) as? ClassDescriptor ?: return false

        val thisFqName = DescriptorUtils.getFqName(thisDescriptor).asString()
        val baseFqName = DescriptorUtils.getFqName(baseDescriptor).asString()
        if (thisFqName == baseFqName) return false

        return if (checkDeep)
            DescriptorUtils.isSubclass(thisDescriptor, baseDescriptor)
        else
            DescriptorUtils.isDirectSubclass(thisDescriptor, baseDescriptor)
    }
}

class KtFakeLightMethod private constructor(
    val ktDeclaration: KtNamedDeclaration,
    ktClassOrObject: KtClassOrObject
) : LightMethod(
    ktDeclaration.manager,
    DummyJavaPsiFactory.createDummyVoidMethod(ktDeclaration.project),
    ktClassOrObject.toFakeLightClass(),
    KotlinLanguage.INSTANCE
), KtLightElement<KtNamedDeclaration, PsiMethod> {
    override val kotlinOrigin get() = ktDeclaration

    override fun getName() = ktDeclaration.name ?: ""

    override fun getNavigationElement() = ktDeclaration
    override fun getIcon(flags: Int): Icon? = ktDeclaration.getIcon(flags)
    override fun getUseScope() = ktDeclaration.useScope

    companion object {
        fun get(ktDeclaration: KtNamedDeclaration): KtFakeLightMethod? {
            val ktClassOrObject = ktDeclaration.containingClassOrObject ?: return null
            return KtFakeLightMethod(ktDeclaration, ktClassOrObject)
        }
    }
}
