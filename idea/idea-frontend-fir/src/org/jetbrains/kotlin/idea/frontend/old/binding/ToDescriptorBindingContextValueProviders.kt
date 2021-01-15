/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.old.binding

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.old.KtSymbolBasedClassDescriptor
import org.jetbrains.kotlin.idea.frontend.old.KtSymbolBasedContext
import org.jetbrains.kotlin.idea.frontend.old.KtSymbolBasedFunctionDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class ToDescriptorBindingContextValueProviders(bindingContext: KtSymbolBasedBindingContext) {
    private val context = bindingContext.context
    private val declarationToDescriptorGetters = mutableListOf<(PsiElement) -> DeclarationDescriptor?>()

    private inline fun <reified K : PsiElement, V : DeclarationDescriptor> KtSymbolBasedBindingContext.registerDeclarationToDescriptorByKey(
        slice: ReadOnlySlice<K, V>,
        crossinline getter: (K) -> V?
    ) {
        declarationToDescriptorGetters.add {
            if (it is K) getter(it) else null
        }
        registerGetterByKey(slice, { getter(it) })
    }

    init {
        bindingContext.registerDeclarationToDescriptorByKey(BindingContext.CLASS, this::getClass)
        bindingContext.registerDeclarationToDescriptorByKey(BindingContext.FUNCTION, this::getFunction)


        bindingContext.registerGetterByKey(BindingContext.DECLARATION_TO_DESCRIPTOR, this::getDeclarationToDescriptor)
    }

    private fun getClass(key: PsiElement): ClassDescriptor? {
        val ktClassSymbol = with(context.ktAnalysisSession) {
            key.safeAs<KtDeclaration>()?.getSymbol().safeAs<KtClassOrObjectSymbol>() ?: return null
        }

        return KtSymbolBasedClassDescriptor(ktClassSymbol, context)
    }

    private fun getFunction(key: PsiElement): SimpleFunctionDescriptor? {
        val ktFunctionSymbol = with(context.ktAnalysisSession) {
            key.safeAs<KtDeclaration>()?.getSymbol().safeAs<KtFunctionSymbol>() ?: return null
        }

        return KtSymbolBasedFunctionDescriptor(ktFunctionSymbol, context)
    }

    private fun getDeclarationToDescriptor(key: PsiElement): DeclarationDescriptor? {
        for (getter in declarationToDescriptorGetters) {
            getter(key)?.let { return it }
        }
        return null
    }
}