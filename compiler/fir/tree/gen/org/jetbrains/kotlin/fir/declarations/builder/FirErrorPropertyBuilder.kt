/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorPropertyImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirErrorPropertyBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    lateinit var moduleData: FirModuleData
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    lateinit var diagnostic: ConeDiagnostic
    val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    lateinit var symbol: FirErrorPropertySymbol

    override fun build(): FirErrorProperty {
        return FirErrorPropertyImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            name,
            annotations,
            diagnostic,
            typeParameters,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorProperty(init: FirErrorPropertyBuilder.() -> Unit): FirErrorProperty {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirErrorPropertyBuilder().apply(init).build()
}
