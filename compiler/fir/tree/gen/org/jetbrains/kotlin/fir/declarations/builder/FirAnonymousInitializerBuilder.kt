/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.FirDeclarationBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousInitializerImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAnonymousInitializerBuilder : FirDeclarationBuilder {
    override var source: FirSourceElement? = null
    override lateinit var moduleData: FirModuleData
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    var body: FirBlock? = null
    var symbol: FirAnonymousInitializerSymbol = FirAnonymousInitializerSymbol()

    override fun build(): FirAnonymousInitializer {
        return FirAnonymousInitializerImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            body,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousInitializer(init: FirAnonymousInitializerBuilder.() -> Unit): FirAnonymousInitializer {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousInitializerBuilder().apply(init).build()
}
