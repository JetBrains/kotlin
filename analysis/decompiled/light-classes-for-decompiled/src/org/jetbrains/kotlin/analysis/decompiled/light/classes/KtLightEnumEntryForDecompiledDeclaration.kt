// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.LightMemberOriginForCompiledField
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile

internal class KtLightEnumEntryForDecompiledDeclaration(
    private val fldDelegate: PsiEnumConstant,
    fldParent: KtLightClassForDecompiledDeclaration,
    lightMemberOrigin: LightMemberOriginForCompiledField,
    private val file: KtClsFile,
) : KtLightFieldForDecompiledDeclaration(
    fldDelegate,
    fldParent,
    lightMemberOrigin,
), PsiEnumConstant {
    override fun getArgumentList(): PsiExpressionList? = fldDelegate.argumentList
    override fun resolveConstructor(): PsiMethod? = fldDelegate.resolveConstructor()
    override fun resolveMethod(): PsiMethod? = fldDelegate.resolveMethod()
    override fun resolveMethodGenerics(): JavaResolveResult = fldDelegate.resolveMethodGenerics()

    override fun getInitializingClass(): PsiEnumConstantInitializer? = cachedValueWithLibraryTracker {
        fldDelegate.initializingClass?.let {
            KtLightEnumClassForDecompiledDeclaration(
                psiConstantInitializer = it,
                enumConstant = this,
                clsParent = containingClass,
                file = file,
                kotlinOrigin = null,
            )
        }
    }

    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        initializingClass ?: error("cannot create initializing class in light enum constant")

    override fun equals(other: Any?): Boolean = other === this ||
            other is KtLightEnumEntryForDecompiledDeclaration &&
            containingClass == other.containingClass &&
            fldDelegate == other.fldDelegate

    override fun hashCode(): Int = super.hashCode()

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitEnumConstant(this)
        } else {
            visitor.visitElement(this)
        }
    }
}
