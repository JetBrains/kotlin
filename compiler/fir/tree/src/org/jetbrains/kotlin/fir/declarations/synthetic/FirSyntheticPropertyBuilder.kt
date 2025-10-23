/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.synthetic

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.name.Name

class FirSyntheticPropertyBuilder {
    lateinit var moduleData: FirModuleData
    lateinit var name: Name
    lateinit var symbol: FirSyntheticPropertySymbol

    lateinit var getterSymbol: FirSyntheticPropertyAccessorSymbol
    lateinit var delegateGetter: FirNamedFunction

    lateinit var deprecationsProvider: DeprecationsProvider

    /**
     * The values should be `null` for cases where [delegateGetter] values should be reused.
     */
    var customStatus: FirDeclarationStatus? = null

    var setterSymbol: FirSyntheticPropertyAccessorSymbol? = null
    var delegateSetter: FirNamedFunction? = null

    var dispatchReceiverType: ConeSimpleKotlinType? = null


    @OptIn(FirImplementationDetail::class)
    fun build(): FirSyntheticProperty = FirSyntheticProperty(
        moduleData,
        name,
        isVar = delegateSetter != null,
        symbol = symbol,
        customStatus = customStatus,
        getter = FirSyntheticPropertyAccessor(getterSymbol, delegateGetter, isGetter = true, symbol),
        setter = setterSymbol?.let { setterSymbol ->
            // TODO (marco): We need to check that the `setterSymbol != null` when `delegateSetter != null`.
            //  Also: Can we find a more elegant way that either allows both or none of these to be null? For example, a data class which
            //  encapsulates the options.
            val delegateSetter = delegateSetter ?: error("Setter delegate should be provided when the setter symbol is provided.")
            FirSyntheticPropertyAccessor(setterSymbol, delegateSetter, isGetter = false, symbol)
        },
        dispatchReceiverType = dispatchReceiverType ?: delegateGetter.dispatchReceiverType,
        deprecationsProvider = deprecationsProvider
    )
}

fun buildSyntheticProperty(f: FirSyntheticPropertyBuilder.() -> Unit): FirSyntheticProperty =
    FirSyntheticPropertyBuilder().apply { f() }.build()
