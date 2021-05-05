/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.old.binding

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.old.*
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtTypeParameter
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
        bindingContext.registerDeclarationToDescriptorByKey(BindingContext.TYPE_PARAMETER, this::getTypeParameter)
        bindingContext.registerDeclarationToDescriptorByKey(BindingContext.FUNCTION, this::getFunction)
        bindingContext.registerDeclarationToDescriptorByKey(BindingContext.CONSTRUCTOR, this::getConstructor)


        bindingContext.registerGetterByKey(BindingContext.DECLARATION_TO_DESCRIPTOR, this::getDeclarationToDescriptor)
    }

    private inline fun <reified T : Any> PsiElement.getKtSymbolOfTypeOrNull(): T? =
        this@ToDescriptorBindingContextValueProviders.context.withAnalysisSession {
            this@getKtSymbolOfTypeOrNull.safeAs<KtDeclaration>()?.getSymbol().safeAs<T>()
        }

    private fun getClass(key: PsiElement): ClassDescriptor? {
        val ktClassSymbol = key.getKtSymbolOfTypeOrNull<KtNamedClassOrObjectSymbol>() ?: return null

        return KtSymbolBasedClassDescriptor(ktClassSymbol, context)
    }

    private fun getTypeParameter(key: KtTypeParameter): TypeParameterDescriptor {
        val ktTypeParameterSymbol = context.withAnalysisSession { key.getTypeParameterSymbol() }
        return KtSymbolBasedTypeParameterDescriptor(ktTypeParameterSymbol, context)
    }

    private fun getFunction(key: PsiElement): SimpleFunctionDescriptor? {
        val ktFunctionLikeSymbol = key.getKtSymbolOfTypeOrNull<KtFunctionLikeSymbol>() ?: return null
        return ktFunctionLikeSymbol.toDeclarationDescriptor(context).safeAs()
    }

    private fun getConstructor(key: PsiElement): ConstructorDescriptor? {
        val ktConstructorSymbol = key.getKtSymbolOfTypeOrNull<KtConstructorSymbol>() ?: return null
        val containerClass = context.withAnalysisSession { ktConstructorSymbol.getContainingSymbol() }
        check(containerClass is KtNamedClassOrObjectSymbol) {
            "Unexpected contained for Constructor symbol: $containerClass, ktConstructorSymbol = $ktConstructorSymbol"
        }

        return KtSymbolBasedConstructorDescriptor(ktConstructorSymbol, KtSymbolBasedClassDescriptor(containerClass, context))
    }

    private fun getDeclarationToDescriptor(key: PsiElement): DeclarationDescriptor? {
        for (getter in declarationToDescriptorGetters) {
            getter(key)?.let { return it }
        }
        return null
    }
}