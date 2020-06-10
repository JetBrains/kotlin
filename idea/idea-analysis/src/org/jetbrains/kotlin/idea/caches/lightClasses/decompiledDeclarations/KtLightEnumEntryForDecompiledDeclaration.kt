/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.lightClasses.decompiledDeclarations

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.caches.lightClasses.LightMemberOriginForCompiledField
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile

internal class KtLightEnumEntryForDecompiledDeclaration(
    private val fldDelegate: PsiEnumConstant,
    fldParent: KtLightClassForDecompiledDeclaration,
    lightMemberOrigin: LightMemberOriginForCompiledField,
    file: KtClsFile,
) : KtLightFieldForDecompiledDeclaration(
    fldDelegate,
    fldParent,
    lightMemberOrigin
), PsiEnumConstant {

    private val _initializingClass: PsiEnumConstantInitializer? by lazyPub {
        fldDelegate.initializingClass?.let {
            KtLightEnumClassForDecompiledDeclaration(
                psiConstantInitializer = it,
                enumConstant = this,
                clsParent = fldParent,
                file = file,
                kotlinOrigin = null
            )
        }
    }

    override fun getArgumentList(): PsiExpressionList? = fldDelegate.argumentList
    override fun resolveConstructor(): PsiMethod? = fldDelegate.resolveConstructor()
    override fun resolveMethod(): PsiMethod? = fldDelegate.resolveMethod()
    override fun resolveMethodGenerics(): JavaResolveResult = fldDelegate.resolveMethodGenerics()
    override fun getInitializingClass(): PsiEnumConstantInitializer? = _initializingClass
    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        _initializingClass ?: error("cannot create initializing class in light enum constant")

    override fun equals(other: Any?): Boolean = other is KtLightEnumEntryForDecompiledDeclaration && super.equals(other)
    override fun hashCode(): Int = super.hashCode()
}