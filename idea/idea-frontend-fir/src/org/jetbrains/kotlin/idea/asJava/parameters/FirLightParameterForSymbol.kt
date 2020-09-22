/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtParameterSymbol
import org.jetbrains.kotlin.psi.KtParameter

internal class FirLightParameterForSymbol(
    private val parameterSymbol: KtParameterSymbol,
    containingMethod: FirLightMethod
) : FirLightParameter(containingMethod) {
    private val _name: String = parameterSymbol.name.asString()
    override fun getName(): String = _name

    private val _isVarArgs: Boolean = parameterSymbol.isVararg
    override fun isVarArgs() = _isVarArgs
    override fun hasModifierProperty(name: String): Boolean =
        modifierList.hasModifierProperty(name)

    override val kotlinOrigin: KtParameter? = parameterSymbol.psi as? KtParameter

    private val _annotations: List<PsiAnnotation> by lazyPub {
        parameterSymbol.computeAnnotations(this, parameterSymbol.type.nullabilityType)
    }

    override fun getModifierList(): PsiModifierList = _modifierList
    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, emptySet(), _annotations)
    }

    private val _type by lazyPub {
        parameterSymbol.asPsiType(this, FirResolvePhase.TYPES)
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightParameterForSymbol &&
                 kotlinOrigin == other.kotlinOrigin &&
                 parameterSymbol == other.parameterSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}