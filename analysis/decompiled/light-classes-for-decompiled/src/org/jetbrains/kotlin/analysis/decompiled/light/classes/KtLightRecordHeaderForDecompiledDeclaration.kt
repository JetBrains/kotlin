/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiRecordComponent
import com.intellij.psi.PsiRecordHeader
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

internal class KtLightRecordHeaderForDecompiledDeclaration(
    private val clsDelegate: PsiRecordHeader,
    private val containingClass: KtLightClassForDecompiledDeclarationBase,
    override val kotlinOrigin: KtPrimaryConstructor?,
) : KtLightElementBase(parent = containingClass), PsiRecordHeader, KtLightElement<KtPrimaryConstructor, PsiRecordHeader> {
    override fun getRecordComponents(): Array<PsiRecordComponent> =
        cachedValueWithLibraryTracker { createRecordComponents() }.toArrayIfNotEmptyOrDefault(PsiRecordComponent.EMPTY_ARRAY)

    private fun createRecordComponents(): List<PsiRecordComponent> {
        val originParameters = kotlinOrigin?.valueParameters.orEmpty()
        return clsDelegate.recordComponents.mapIndexed { index, recordComponent ->
            KtLightRecordComponentForDecompiledDeclaration(
                clsDelegate = recordComponent,
                recordHeader = this,
                containingClass = containingClass,
                kotlinOrigin = originParameters.getOrNull(index),
            )
        }
    }

    override fun getContainingClass(): PsiClass = containingClass
    override fun copy(): PsiElement = this
    override fun clone(): Any = this
    override fun getOriginalElement(): PsiElement = clsDelegate

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return this == another ||
                another is KtLightRecordHeaderForDecompiledDeclaration && clsDelegate.isEquivalentTo(another.clsDelegate) ||
                clsDelegate.isEquivalentTo(another)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitRecordHeader(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is KtLightRecordHeaderForDecompiledDeclaration &&
                containingClass == other.containingClass &&
                clsDelegate == other.clsDelegate
    }

    override fun hashCode(): Int = 31 * containingClass.hashCode() + clsDelegate.hashCode()
}
