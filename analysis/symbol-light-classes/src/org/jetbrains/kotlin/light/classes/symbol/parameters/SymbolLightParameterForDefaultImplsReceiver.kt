/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.annotations.ComputeAllAtOnceAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForInterface
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.psi.KtParameter

internal class SymbolLightParameterForDefaultImplsReceiver(containingDeclaration: SymbolLightMethodBase) :
    SymbolLightParameterBase(containingDeclaration) {
    private val _type by lazyPub {
        (method.containingClass.containingClass as SymbolLightClassForInterface).withClassOrObjectSymbol {
            it.buildSelfClassType().asPsiType(containingDeclaration, allowErrorTypes = true) ?: nonExistentType()
        }
    }

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getName(): String = "\$this"

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean =
        other === this || other is SymbolLightParameterForDefaultImplsReceiver && other.parent == parent

    override fun hashCode(): Int = parent.hashCode()

    override fun isVarArgs(): Boolean = false

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            this,
            annotationsBox = ComputeAllAtOnceAnnotationsBox { modifierList ->
                listOf(SymbolLightSimpleAnnotation(NotNull::class.java.name, modifierList))
            })
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun hasModifierProperty(name: String): Boolean = false

    override val kotlinOrigin: KtParameter?
        get() = null
}