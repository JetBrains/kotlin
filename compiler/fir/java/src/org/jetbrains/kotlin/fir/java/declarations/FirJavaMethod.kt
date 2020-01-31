/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.properties.Delegates

@UseExperimental(FirImplementationDetail::class)
class FirJavaMethod @FirImplementationDetail constructor(
    source: FirSourceElement?,
    session: FirSession,
    resolvePhase: FirResolvePhase,
    returnTypeRef: FirTypeRef,
    receiverTypeRef: FirTypeRef?,
    typeParameters: MutableList<FirTypeParameter>,
    valueParameters: MutableList<FirValueParameter>,
    body: FirBlock?,
    name: Name,
    status: FirDeclarationStatus,
    containerSource: DeserializedContainerSource?,
    symbol: FirFunctionSymbol<FirSimpleFunction>,
    annotations: MutableList<FirAnnotationCall>,
) : FirSimpleFunctionImpl(
    source,
    session,
    resolvePhase,
    returnTypeRef,
    receiverTypeRef,
    typeParameters,
    valueParameters,
    body,
    status,
    containerSource,
    name,
    symbol,
    annotations,
)

@FirBuilderDsl
class FirJavaMethodBuilder : FirSimpleFunctionBuilder() {
    lateinit var visibility: Visibility
    var modality: Modality? = null
    var isStatic: Boolean by Delegates.notNull()
    override var resolvePhase: FirResolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

    @UseExperimental(FirImplementationDetail::class)
    override fun build(): FirJavaMethod {
        val status = FirDeclarationStatusImpl(visibility, modality).apply {
            isStatic = this@FirJavaMethodBuilder.isStatic
            isExpect = false
            isActual = false
            isOverride = false
            isOperator = true // All Java methods with name that allows to use it in operator form are considered operators
            isInfix = false
            isInline = false
            isTailRec = false
            isExternal = false
            isSuspend = false
        }

        return FirJavaMethod(
            source,
            session,
            resolvePhase,
            returnTypeRef as FirJavaTypeRef,
            receiverTypeRef = null,
            typeParameters,
            valueParameters,
            body,
            name,
            status,
            containerSource,
            symbol,
            annotations,
        )
    }
}

inline fun buildJavaMethod(init: FirJavaMethodBuilder.() -> Unit): FirJavaMethod {
    return FirJavaMethodBuilder().apply(init).build()
}