/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10Reference
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.references.KtPropertyDelegationMethodsReference
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PropertyDelegationMethodsReference(
    element: KtPropertyDelegate
) : KtPropertyDelegationMethodsReference(element), KtFe10Reference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        require(this is KtFe10AnalysisSession)

        val property = expression.parent as? KtProperty
        if (property == null || property.delegate !== element) {
            return emptyList()
        }

        val bindingContext = analyze(property)
        val propertyDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, property]

        if (propertyDescriptor is PropertyDescriptor) {
            return listOfNotNull(propertyDescriptor.getter, propertyDescriptor.setter)
                .mapNotNull { accessor ->
                    val descriptor = bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor]?.resultingDescriptor
                    descriptor?.toKtCallableSymbol(this)
                }
        }

        return emptyList()
    }
}