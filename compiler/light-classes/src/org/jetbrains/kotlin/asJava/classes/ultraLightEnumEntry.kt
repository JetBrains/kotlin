/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class KtUltraLightEnumEntry(
    declaration: KtEnumEntry,
    name: String,
    containingClass: KtUltraLightClass,
    support: KtUltraLightSupport,
    modifiers: Set<String>
) : KtUltraLightField(declaration, name, containingClass, support, modifiers), PsiEnumConstant {

    private val enumEntry get() = declaration as KtEnumEntry

    private val _initializingClass by lazyPub {
        enumEntry.body?.let { KtUltraLightClassForEnumEntry(enumEntry, containingClass.support, this) }
    }

    override fun getInitializingClass(): PsiEnumConstantInitializer? = _initializingClass
    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        _initializingClass ?: error("cannot create initializing class in light enum constant")

    override fun getArgumentList(): PsiExpressionList? = null
    override fun resolveMethod(): PsiMethod? = null
    override fun resolveConstructor(): PsiMethod? = null

    override fun resolveMethodGenerics(): JavaResolveResult = JavaResolveResult.EMPTY

    override fun hasInitializer() = true
    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?) = this
}