/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirConstructorBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.properties.Delegates

@OptIn(FirImplementationDetail::class)
class FirJavaConstructor @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override val symbol: FirConstructorSymbol,
    override val isPrimary: Boolean,
    override var returnTypeRef: FirTypeRef,
    override val valueParameters: MutableList<FirValueParameter>,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    override val annotations: MutableList<FirAnnotationCall>,
    override var status: FirDeclarationStatus,
    override var resolvePhase: FirResolvePhase,
    override val dispatchReceiverType: ConeKotlinType?,
) : FirConstructor() {
    override val receiverTypeRef: FirTypeRef? get() = null

    init {
        symbol.bind(this)
    }

    override val delegatedConstructor: FirDelegatedConstructorCall?
        get() = null

    override val body: FirBlock?
        get() = null

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Java

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirConstructor {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        transformReturnTypeRef(transformer, data)
        transformTypeParameters(transformer, data)
        transformValueParameters(transformer, data)
        status = status.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDelegatedConstructor(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        return this
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirConstructor {
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override var containerSource: DeserializedContainerSource? = null

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters += newValueParameters
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {}

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {}

    override fun replaceBody(newBody: FirBlock?) {
        error("Body cannot be replaced for FirJavaConstructor")
    }
}

@FirBuilderDsl
class FirJavaConstructorBuilder : FirConstructorBuilder() {
    lateinit var visibility: Visibility
    var isInner: Boolean by Delegates.notNull()
    var isPrimary: Boolean by Delegates.notNull()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaConstructor {
        return FirJavaConstructor(
            source,
            session,
            symbol,
            isPrimary,
            returnTypeRef,
            valueParameters,
            typeParameters,
            annotations,
            status,
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES,
            dispatchReceiverType
        )
    }

    @Deprecated("Modification of 'body' has no impact for FirJavaConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var body: FirBlock?
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'delegatedConstructor' has no impact for FirJavaConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var delegatedConstructor: FirDelegatedConstructorCall?
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'resolvePhase' has no impact for FirJavaConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var resolvePhase: FirResolvePhase
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'receiverTypeRef' has no impact for FirJavaConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var receiverTypeRef: FirTypeRef?
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'origin' has no impact for FirJavaConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirDeclarationOrigin
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }
}

inline fun buildJavaConstructor(init: FirJavaConstructorBuilder.() -> Unit): FirJavaConstructor {
    return FirJavaConstructorBuilder().apply(init).build()
}

