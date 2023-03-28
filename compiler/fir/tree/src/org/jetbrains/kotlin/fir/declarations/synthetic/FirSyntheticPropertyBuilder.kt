/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.synthetic

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.name.Name

class FirSyntheticPropertyBuilder {
    lateinit var moduleData: FirModuleData
    lateinit var name: Name
    lateinit var symbol: FirSyntheticPropertySymbol
    lateinit var delegateGetter: FirSimpleFunction
    lateinit var deprecationsProvider: DeprecationsProvider

    var status: FirDeclarationStatus? = null
    var delegateSetter: FirSimpleFunction? = null


    fun build(): FirSyntheticProperty = FirSyntheticProperty(
        moduleData, name, isVar = delegateSetter != null, symbol = symbol,
        status = status ?: delegateGetter.status,
        resolvePhase = delegateGetter.resolvePhase,
        getter = FirSyntheticPropertyAccessor(delegateGetter, isGetter = true, symbol),
        setter = delegateSetter?.let { FirSyntheticPropertyAccessor(it, isGetter = false, symbol) },
        deprecationsProvider = deprecationsProvider
    )
}

fun buildSyntheticProperty(f: FirSyntheticPropertyBuilder.() -> Unit): FirSyntheticProperty =
    FirSyntheticPropertyBuilder().apply { f() }.build()
