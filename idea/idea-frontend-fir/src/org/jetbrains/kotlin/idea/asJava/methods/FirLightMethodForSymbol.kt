/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.FirLightIdentifier
import org.jetbrains.kotlin.idea.asJava.parameters.FirLightParameterList
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.BitSet

internal abstract class FirLightMethodForSymbol(
    private val functionSymbol: KtFunctionLikeSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    methodIndex: Int,
    argumentsSkipMask: BitSet? = null
) : FirLightMethod(
    lightMemberOrigin,
    containingClass,
    methodIndex
) {
    private var _isVarArgs: Boolean = functionSymbol.valueParameters.any { it.isVararg }

    override fun isVarArgs(): Boolean = _isVarArgs

    private val _parametersList by lazyPub {
        FirLightParameterList(this, functionSymbol, argumentsSkipMask)
    }

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, functionSymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun getParameterList(): PsiParameterList = _parametersList

    override val kotlinOrigin: KtDeclaration? = functionSymbol.psi as? KtDeclaration

    override fun isValid(): Boolean = super.isValid() && functionSymbol.isValid()
}
