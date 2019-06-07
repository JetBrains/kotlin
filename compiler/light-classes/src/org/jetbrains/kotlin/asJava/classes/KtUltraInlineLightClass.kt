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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class KtUltraLightInlineClass(
    classOrObject: KtClassOrObject,
    support: KtUltraLightSupport
) : KtUltraLightClass(classOrObject, support) {

    override fun getDelegate(): PsiClass =
        throw IllegalStateException("Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: $qualifiedName")

    override fun getScope(): PsiElement? = parent

    private val membersBuilder: UltraLightMembersCreator by lazyPub {
        UltraLightMembersCreator(
            containingClass = this,
            containingClassIsNamedObject = false,
            containingClassIsSealed = false,
            mangleInternalFunctions = false,
            support = support
        )
    }

    private val _ownMethods: List<KtLightMethod> by lazyPub {

        val result = arrayListOf<KtLightMethod>()

        val applicableDeclarations = this.classOrObject.declarations
            .filter { it.hasModifier(KtTokens.OVERRIDE_KEYWORD) }
            .filterNot { it.isHiddenByDeprecation(support) }

        for (declaration in applicableDeclarations) {
            when (declaration) {
                is KtNamedFunction -> result.addAll(membersBuilder.createMethods(declaration, forceStatic = false))
                is KtProperty -> result.addAll(
                    membersBuilder.propertyAccessors(declaration, declaration.isVar, forceStatic = false, onlyJvmStatic = false)
                )
            }
        }

        val inlineClassParameter = classOrObject
            .primaryConstructor
            ?.valueParameters
            ?.firstOrNull()

        if (inlineClassParameter !== null) {
            membersBuilder.propertyAccessors(
                inlineClassParameter,
                mutable = false,
                forceStatic = false,
                onlyJvmStatic = false
            ).let {
                result.addAll(it)
            }
        }

        result;
    }

    override fun getOwnFields(): List<KtLightField> = emptyList()

    override fun getOwnMethods() = _ownMethods

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun copy(): KtUltraLightInlineClass =
        KtUltraLightInlineClass(classOrObject, support)
}