/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.records

import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiRecordComponent
import com.intellij.psi.PsiRecordHeader
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

internal class SymbolLightRecordHeader(
    override val kotlinOrigin: KtPrimaryConstructor?,
    private val containingClass: SymbolLightClassForClassOrObject,
) : KtLightElementBase(parent = containingClass), PsiRecordHeader, KtLightElement<KtPrimaryConstructor, PsiRecordHeader> {
    override fun getRecordComponents(): Array<PsiRecordComponent> =
        cachedValue { createRecordComponents() }.toArrayIfNotEmptyOrDefault(PsiRecordComponent.EMPTY_ARRAY)

    override fun getContainingClass(): PsiClass = containingClass

    private fun createRecordComponents(): List<PsiRecordComponent> {
        return containingClass.withClassSymbol { classSymbol ->
            val primaryConstructorSymbol = classSymbol.declaredMemberScope.constructors.singleOrNull { it.isPrimary }
                ?: return@withClassSymbol emptyList()

            val components = primaryConstructorSymbol.valueParameters.mapNotNull { parameterSymbol ->
                val backingFieldSymbol = parameterSymbol.generatedPrimaryConstructorProperty?.backingFieldSymbol
                    ?: return@mapNotNull null
                SymbolLightRecordComponent(
                    parameterSymbol = parameterSymbol,
                    backingFieldSymbol = backingFieldSymbol,
                    parent = this@SymbolLightRecordHeader,
                    containingClass = containingClass,
                )
            }

            components
        }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitRecordHeader(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightRecordHeader) return false

        val otherKotlinOrigin = other.kotlinOrigin
        if (kotlinOrigin != null || otherKotlinOrigin != null) {
            return kotlinOrigin == otherKotlinOrigin
        }

        return other.containingClass == containingClass
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: containingClass.hashCode()
}
