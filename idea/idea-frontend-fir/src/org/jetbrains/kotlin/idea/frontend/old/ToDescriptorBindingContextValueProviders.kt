/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.old

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal
class ToDescriptorBindingContextValueProviders(val context: KtSymbolBasedContext) : FirBindingContextValueProviders {
    override fun <K, V> getIfPossible(slice: ReadOnlySlice<K, V>?, key: K): V? {

        when (slice) {
            BindingContext.DECLARATION_TO_DESCRIPTOR, BindingContext.FUNCTION -> return getFunction(key as PsiElement) as V?
        }

        return null
    }

    private fun getFunction(key: PsiElement): SimpleFunctionDescriptor? {
        val ktFunctionSymbol = with(context.ktAnalysisSession) {
            key.safeAs<KtDeclaration>()?.getSymbol().safeAs<KtFunctionSymbol>() ?: return null
        }

        return KtSymbolBasedFunctionDescriptor(ktFunctionSymbol, context)
    }
}