/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousInitializerImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol

@FirBuilderDsl
class FirAnonymousInitializerBuilder : FirDeclarationBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    var body: FirBlock? = null
    var symbol: FirAnonymousInitializerSymbol = FirAnonymousInitializerSymbol()
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>

    override fun build(): FirAnonymousInitializer {
        return FirAnonymousInitializerImpl(
            source,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            moduleData,
            origin,
            attributes,
            body,
            symbol,
            containingDeclarationSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousInitializer(init: FirAnonymousInitializerBuilder.() -> Unit): FirAnonymousInitializer {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousInitializerBuilder().apply(init).build()
}
