/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.name.Name

/**
 * A symbol pointer for Java accessor methods (getFoo/setFoo) that implement Kotlin properties.
 * These accessors are exposed through [KaSyntheticJavaPropertySymbol.javaGetterSymbol]
 * and [KaSyntheticJavaPropertySymbol.javaSetterSymbol].
 *
 * Unlike regular member functions, these accessor functions are not directly found in the class scope.
 * Instead, they're accessed through the synthetic property's getter/setter accessors.
 */
internal class KaFirJavaSyntheticPropertyAccessorFunctionSymbolPointer(
    ownerPointer: KaSymbolPointer<KaDeclarationContainerSymbol>,
    private val propertyName: Name,
    private val isGetter: Boolean,
    originalSymbol: KaNamedFunctionSymbol?,
) : KaFirMemberSymbolPointer<KaNamedFunctionSymbol>(ownerPointer, originalSymbol = originalSymbol) {
    override fun KaFirSession.chooseCandidateAndCreateSymbol(candidates: FirScope, firSession: FirSession): KaNamedFunctionSymbol? {
        val syntheticProperty = candidates.getProperties(propertyName)
            .mapNotNull { it.fir as? FirSyntheticProperty }
            .singleOrNull()
            ?: return null

        val functionSymbol = if (isGetter) {
            syntheticProperty.getter.delegate.symbol
        } else {
            syntheticProperty.setter?.delegate?.symbol ?: return null
        }

        return firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(functionSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean {
        return this === other ||
                other is KaFirJavaSyntheticPropertyAccessorFunctionSymbolPointer &&
                other.propertyName == propertyName &&
                other.isGetter == isGetter &&
                hasTheSameOwner(other)
    }
}
