/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiParameterList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.SymbolLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSuspendContinuationParameter
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

context(KtAnalysisSession)
internal abstract class SymbolLightMethod(
    private val functionSymbol: KtFunctionLikeSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    argumentsSkipMask: BitSet? = null
) : SymbolLightMethodBase(
    lightMemberOrigin,
    containingClass,
    methodIndex
) {
    private var _isVarArgs: Boolean = functionSymbol.valueParameters.any { it.isVararg }

    override fun isVarArgs(): Boolean = _isVarArgs

    private val _parametersList by lazyPub {
        SymbolLightParameterList(this, functionSymbol) { builder ->
            functionSymbol.valueParameters.mapIndexed { index, parameter ->
                val needToSkip = argumentsSkipMask?.get(index) == true
                if (!needToSkip) {
                    builder.addParameter(
                        SymbolLightParameter(
                            parameterSymbol = parameter,
                            containingMethod = this@SymbolLightMethod
                        )
                    )
                }
            }
            if ((functionSymbol as? KtFunctionSymbol)?.isSuspend == true) {
                builder.addParameter(
                    SymbolLightSuspendContinuationParameter(
                        functionSymbol = functionSymbol,
                        containingMethod = this@SymbolLightMethod
                    )
                )
            }
        }
    }

    private val _identifier: PsiIdentifier by lazyPub {
        SymbolLightIdentifier(this, functionSymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun getParameterList(): PsiParameterList = _parametersList

    override val kotlinOrigin: KtDeclaration? =
        lightMemberOrigin?.originalElement ?: functionSymbol.psi as? KtDeclaration

    override fun isValid(): Boolean = super.isValid() && functionSymbol.isValid()

    override fun isOverride(): Boolean = functionSymbol.getDirectlyOverriddenSymbols().isNotEmpty()
}
