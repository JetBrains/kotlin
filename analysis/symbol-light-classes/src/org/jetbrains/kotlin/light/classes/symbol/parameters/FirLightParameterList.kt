/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameterList

internal class FirLightParameterList(
    private val parent: FirLightMethod,
    private val callableSymbol: KtCallableSymbol,
    parameterPopulator: (LightParameterListBuilder) -> Unit,
) : KtLightElement<KtParameterList, PsiParameterList>,
    // With this, a parent chain is properly built: from FirLightParameter through FirLightParameterList to FirLightMethod
    KtLightElementBase(parent),
    // NB: we can't use delegation here, which will conflict getTextRange from KtLightElementBase
    PsiParameterList {

    override val kotlinOrigin: KtParameterList?
        get() = (parent.kotlinOrigin as? KtFunction)?.valueParameterList

    override val clsDelegate: PsiParameterList by lazyPub {
        val builder = LightParameterListBuilder(manager, language)

        FirLightParameterForReceiver.tryGet(callableSymbol, parent)?.let {
            builder.addParameter(it)
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
