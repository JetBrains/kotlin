/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class KtUltraLightClassForFacade(
    manager: PsiManager,
    facadeClassFqName: FqName,
    lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade>,
    private val file: KtFile,
    private val support: KtUltraLightSupport
) : KtLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, listOf(file)) {

    private val methodsBuilder by lazyPub { UltraLightMembersCreator(this, false, true, support) }

    private inline fun <T> forTooComplex(getter: () -> T): T {
        check(tooComplex) {
            "Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: $qualifiedName"
        }
        return getter()
    }

    override val lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade>
        get() = forTooComplex { super.lightClassDataCache }

    override val clsDelegate: PsiClass
        get() = forTooComplex { super.clsDelegate }

    private val tooComplex: Boolean by lazyPub { file.declarations.any { support.isTooComplexForUltraLightGeneration(it) } }

    private val ownMethodsForNotTooComplex: List<KtLightMethod> by lazyPub {

        val result = arrayListOf<KtLightMethod>()

        for (declaration in file.declarations.filterNot { it.isHiddenByDeprecation(support) }) {
            if (declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) continue
            when (declaration) {
                is KtNamedFunction -> result.addAll(methodsBuilder.createMethods(declaration, true))
                is KtProperty -> result.addAll(methodsBuilder.propertyAccessors(declaration, declaration.isVar, true, false))
            }
        }

        result
    }

    private val ownFieldsForNotTooComplex: List<KtLightField> by lazyPub {
        hashSetOf<String>().run {
            file.declarations.filterIsInstance<KtProperty>().mapNotNull {
                methodsBuilder.createPropertyField(it, this, forceStatic = true)
            }
        }
    }

    override fun getOwnFields() = if (!tooComplex) ownFieldsForNotTooComplex else super.getOwnFields()

    override fun getOwnMethods() = if (!tooComplex) ownMethodsForNotTooComplex else super.getOwnMethods()

    override fun hashCode(): Int = file.hashCode()

    override fun toString(): String = "UltraLight class for file facade"

    override fun equals(other: Any?): Boolean = this === other

    override fun copy(): KtLightClassForFacade = KtUltraLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, file, support)

    override fun setName(name: String): PsiElement? = this
}