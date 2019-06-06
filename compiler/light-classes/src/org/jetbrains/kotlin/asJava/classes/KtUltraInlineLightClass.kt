/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClassOrObject

class KtUltraLightInlineClass(
    classOrObject: KtClassOrObject,
    support: KtUltraLightSupport
) : KtUltraLightClass(classOrObject, support) {

    override fun getDelegate(): PsiClass =
        throw IllegalStateException("Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: $qualifiedName")

    override fun getScope(): PsiElement? = parent

    private val _ownMethods: List<KtLightMethod> by lazyPub {

        val inlineClassParameter = classOrObject
            .primaryConstructor
            ?.valueParameters
            ?.firstOrNull()
            ?: return@lazyPub emptyList()


        UltraLightMembersCreator(
            containingClass = this,
            containingClassIsNamedObject = false,
            containingClassIsSealed = false,
            mangleInternalFunctions = false,
            support = support
        ).run {
            propertyAccessors(
                inlineClassParameter,
                mutable = false,
                forceStatic = false,
                onlyJvmStatic = false
            )
        }
    }

    override fun getOwnFields(): List<KtLightField> = emptyList()

    override fun getOwnMethods() = _ownMethods

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun copy(): KtUltraLightInlineClass =
        KtUltraLightInlineClass(classOrObject, support)
}