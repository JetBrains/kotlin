/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirValueParameterBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

@OptIn(FirImplementationDetail::class)
class FirJavaValueParameter @FirImplementationDetail constructor(
    source: FirSourceElement?,
    session: FirSession,
    resolvePhase: FirResolvePhase,
    returnTypeRef: FirTypeRef,
    name: Name,
    symbol: FirVariableSymbol<FirValueParameter>,
    annotations: MutableList<FirAnnotationCall>,
    defaultValue: FirExpression?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isVararg: Boolean,
) : FirValueParameterImpl(
    source,
    session,
    resolvePhase,
    FirDeclarationOrigin.Java,
    returnTypeRef,
    name,
    symbol,
    annotations,
    defaultValue,
    isCrossinline,
    isNoinline,
    isVararg,
)

@FirBuilderDsl
class FirJavaValueParameterBuilder : FirValueParameterBuilder() {
    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaValueParameter {
        return FirJavaValueParameter(
            source,
            session,
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES,
            returnTypeRef,
            name,
            symbol = FirVariableSymbol(name),
            annotations,
            defaultValue,
            isCrossinline = false,
            isNoinline = false,
            isVararg,
        )
    }

    @Deprecated("Modification of 'resolvePhase' has no impact for FirJavaValueParameterBuilder", level = DeprecationLevel.HIDDEN)
    override var resolvePhase: FirResolvePhase
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of '' has no impact for FirJavaValueParameterBuilder", level = DeprecationLevel.HIDDEN)
    override var symbol: FirVariableSymbol<FirValueParameter>
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'isCrossinline' has no impact for FirJavaValueParameterBuilder", level = DeprecationLevel.HIDDEN)
    override var isCrossinline: Boolean
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'isNoinline' has no impact for FirJavaValueParameterBuilder", level = DeprecationLevel.HIDDEN)
    override var isNoinline: Boolean
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'origin' has no impact for FirJavaValueParameterBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirDeclarationOrigin
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }
}

inline fun buildJavaValueParameter(init: FirJavaValueParameterBuilder.() -> Unit): FirJavaValueParameter {
    return FirJavaValueParameterBuilder().apply(init).build()
}
