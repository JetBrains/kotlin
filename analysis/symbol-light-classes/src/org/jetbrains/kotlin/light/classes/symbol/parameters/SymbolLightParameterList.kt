/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameterList

context(KtAnalysisSession)
internal class SymbolLightParameterList(
    private val parent: SymbolLightMethodBase,
    private val callableSymbol: KtCallableSymbol?,
    parameterPopulator: (LightParameterListBuilder) -> Unit,
) : KtLightElement<KtParameterList, PsiParameterList>,
    // With this, a parent chain is properly built: from SymbolLightParameter through SymbolLightParameterList to SymbolLightMethod
    KtLightElementBase(parent),
    // NB: we can't use delegation here, which will conflict getTextRange from KtLightElementBase
    PsiParameterList {

    override val kotlinOrigin: KtParameterList?
        get() = (parent.kotlinOrigin as? KtFunction)?.valueParameterList

    private val clsDelegate: PsiParameterList by lazyPub {
        val builder = LightParameterListBuilder(manager, language)

        callableSymbol?.let {
            SymbolLightParameterForReceiver.tryGet(it, parent)?.let { receiver ->
                builder.addParameter(receiver)
            }
        }

        parameterPopulator.invoke(builder)

        builder
    }

    override fun getParameters(): Array<PsiParameter> {
        return clsDelegate.parameters
    }

    override fun getParameterIndex(p: PsiParameter): Int {
        return clsDelegate.getParameterIndex(p)
    }

    override fun getParametersCount(): Int {
        return clsDelegate.parametersCount
    }
}
