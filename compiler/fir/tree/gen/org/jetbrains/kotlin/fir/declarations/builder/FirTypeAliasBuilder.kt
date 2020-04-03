/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParametersOwnerBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeAliasImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirTypeAliasBuilder : FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    lateinit var session: FirSession
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    lateinit var status: FirDeclarationStatus
    lateinit var name: Name
    lateinit var symbol: FirTypeAliasSymbol
    lateinit var expandedTypeRef: FirTypeRef
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    override fun build(): FirTypeAlias {
        return FirTypeAliasImpl(
            source,
            session,
            resolvePhase,
            typeParameters,
            status,
            name,
            symbol,
            expandedTypeRef,
            annotations,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAlias(init: FirTypeAliasBuilder.() -> Unit): FirTypeAlias {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirTypeAliasBuilder().apply(init).build()
}
