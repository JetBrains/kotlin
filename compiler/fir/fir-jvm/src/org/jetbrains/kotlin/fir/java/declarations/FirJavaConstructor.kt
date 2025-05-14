/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.java.enhancement.FirEmptyJavaAnnotationList
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaAnnotationList
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

class FirJavaConstructor @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val symbol: FirConstructorSymbol,
    override val origin: FirDeclarationOrigin.Java,
    override val isPrimary: Boolean,
    override var returnTypeRef: FirTypeRef,
    override val valueParameters: MutableList<FirValueParameter>,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    private val annotationList: FirJavaAnnotationList,
    private val originalStatus: FirResolvedDeclarationStatusImpl,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    private val containingClassSymbol: FirClassSymbol<*>,
) : FirConstructor() {
    override val receiverParameter: FirReceiverParameter? get() = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override val contractDescription: FirContractDescription? get() = null

    init {
        @OptIn(FirImplementationDetail::class)
        symbol.bind(this)

        @OptIn(ResolveStateAccess::class)
        this.resolveState = FirResolvePhase.ANALYZED_DEPENDENCIES.asResolveState()
    }

    private val typeParameterBoundsResolveLock = ReentrantLock()

    override val staticReceiverParameter: FirTypeRef?
        get() = null

    override val delegatedConstructor: FirDelegatedConstructorCall?
        get() = null

    override val body: FirBlock?
        get() = null

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null

    override val annotations: List<FirAnnotation> get() = annotationList

    // TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val status: FirDeclarationStatus by lazy {
        applyStatusTransformerExtensions(this, originalStatus) {
            transformStatus(it, this@FirJavaConstructor, containingClassSymbol, isLocal = false)
        }
    }

    override val contextParameters: List<FirValueParameter>
        get() = emptyList()

    internal fun withTypeParameterBoundsResolveLock(f: () -> Unit) {
        // TODO: KT-68587
        typeParameterBoundsResolveLock.withLock(f)
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirConstructor {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
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
        return this
    }

    override fun <D> transformStaticReceiverParameter(transformer: FirTransformer<D>, data: D): FirConstructor {
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirConstructor {
        return this
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirConstructor {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        shouldNotBeCalled(::replaceAnnotations, ::annotations)
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
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

    override fun replaceStaticReceiverParameter(newStaticReceiverParameter: FirTypeRef?) {
        error("Static receiver parameter cannot be replaced for FirJavaConstructor")
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters += newValueParameters
    }

    override fun replaceDelegatedConstructor(newDelegatedConstructor: FirDelegatedConstructorCall?) {
        error("Delegated constructor cannot be replaced for FirJavaConstructor")
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {}

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {
        error("Context receivers cannot be replaced for FirJavaConstructor")
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {}
    override fun replaceContractDescription(newContractDescription: FirContractDescription?) {
        error("Contract description cannot be replaced for FirJavaConstructor")
    }

    override fun replaceBody(newBody: FirBlock?) {
        error("Body cannot be replaced for FirJavaConstructor")
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        shouldNotBeCalled(::replaceStatus, ::status)
    }
}

@FirBuilderDsl
class FirJavaConstructorBuilder : FirConstructorBuilder() {
    var isInner: Boolean by Delegates.notNull()
    var isPrimary: Boolean by Delegates.notNull()
    var isFromSource: Boolean by Delegates.notNull()
    var annotationList: FirJavaAnnotationList = FirEmptyJavaAnnotationList
    lateinit var containingClassSymbol: FirClassSymbol<*>

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaConstructor {
        return FirJavaConstructor(
            source,
            moduleData,
            symbol,
            origin = javaOrigin(isFromSource),
            isPrimary,
            returnTypeRef,
            valueParameters,
            typeParameters,
            annotationList,
            status as FirResolvedDeclarationStatusImpl,
            dispatchReceiverType,
            containingClassSymbol,
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

    @Deprecated("Modification of 'receiverParameter' has no impact for FirJavaConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var receiverParameter: FirReceiverParameter?
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

