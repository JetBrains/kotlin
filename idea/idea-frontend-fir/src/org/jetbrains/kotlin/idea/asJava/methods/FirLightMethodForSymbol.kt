/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.FirLightIdentifier
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

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
        val builder = LightParameterListBuilder(manager, language)

        FirLightParameterForReceiver.tryGet(functionSymbol, this)?.let {
            builder.addParameter(it)
        }

        functionSymbol.valueParameters.mapIndexed { index, parameter ->
            val needToSkip = argumentsSkipMask?.get(index) == true
            if (!needToSkip) {
                builder.addParameter(
                    FirLightParameterForSymbol(
                        parameterSymbol = parameter,
                        containingMethod = this@FirLightMethodForSymbol
                    )
                )
            }
        }

        builder
    }

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, functionSymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun getParameterList(): PsiParameterList = _parametersList

    override val kotlinOrigin: KtDeclaration? = functionSymbol.psi as? KtDeclaration

    override fun isValid(): Boolean = super.isValid() && functionSymbol.isValid()
}