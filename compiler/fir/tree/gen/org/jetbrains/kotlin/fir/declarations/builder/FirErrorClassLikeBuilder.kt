/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirErrorClassLike
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorClassLikeImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorClassLikeSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirErrorClassLikeBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var moduleData: FirModuleData
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    lateinit var status: FirDeclarationStatus
    var deprecation: DeprecationsPerUseSite? = null
    lateinit var diagnostic: ConeDiagnostic
    lateinit var symbol: FirErrorClassLikeSymbol

    override fun build(): FirErrorClassLike {
        return FirErrorClassLikeImpl(
            source,
            annotations,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            typeParameters,
            status,
            deprecation,
            diagnostic,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorClassLike(init: FirErrorClassLikeBuilder.() -> Unit): FirErrorClassLike {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirErrorClassLikeBuilder().apply(init).build()
}
