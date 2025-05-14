/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirFieldBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.enhancement.FirEmptyJavaAnnotationList
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaAnnotationList
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates

class FirJavaField @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin.Java,
    override val symbol: FirFieldSymbol,
    override val name: Name,
    override var returnTypeRef: FirTypeRef,
    private val originalStatus: FirResolvedDeclarationStatusImpl,
    override val isVar: Boolean,
    private val annotationList: FirJavaAnnotationList,
    lazyInitializer: Lazy<FirExpression?>,
    lazyHasConstantInitializer: Lazy<Boolean>,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override val attributes: FirDeclarationAttributes,
    private val containingClassSymbol: FirClassSymbol<*>,
) : FirField() {
    internal var lazyInitializer: Lazy<FirExpression?> = lazyInitializer
        private set

    internal var lazyHasConstantInitializer: Lazy<Boolean> = lazyHasConstantInitializer
        private set

    init {
        @OptIn(FirImplementationDetail::class)
        symbol.bind(this)

        @OptIn(ResolveStateAccess::class)
        this.resolveState = FirResolvePhase.ANALYZED_DEPENDENCIES.asResolveState()
    }

    override val receiverParameter: FirReceiverParameter? get() = null
    override val isVal: Boolean get() = !isVar
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null
    override val backingField: FirBackingField? = null
    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null

    override val annotations: List<FirAnnotation> get() = annotationList

    override val initializer: FirExpression?
        get() = lazyInitializer.value

    override val hasConstantInitializer: Boolean
        get() = lazyHasConstantInitializer.value

    override val deprecationsProvider: DeprecationsProvider by lazy {
        annotations.getDeprecationsProviderFromAnnotations(moduleData.session, fromJava = true)
    }

    // TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val status: FirDeclarationStatus by lazy {
        applyStatusTransformerExtensions(this, originalStatus) {
            transformStatus(it, this@FirJavaField, containingClassSymbol, isLocal = false)
        }
    }

    override val typeParameters: List<FirTypeParameterRef>
        get() = emptyList()

    override val contextParameters: List<FirValueParameter>
        get() = emptyList()

    override val staticReceiverParameter: FirTypeRef?
        get() = null

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirField {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirField {
        transformInitializer(transformer, data)
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaField {
        transformReturnTypeRef(transformer, data)
        transformTypeParameters(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaField {
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun <D> transformStaticReceiverParameter(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        shouldNotBeCalled(::replaceAnnotations, ::annotations)
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaField {
        return this
    }

    override fun replaceInitializer(newInitializer: FirExpression?) {
        lazyInitializer = lazyOf(newInitializer)
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun replaceDelegate(newDelegate: FirExpression?) {}
    override val delegate: FirExpression?
        get() = null

    override var containerSource: DeserializedContainerSource? = null

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirField {
        replaceInitializer(initializer?.transformSingle(transformer, data))
        return this
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {}
    override fun replaceStaticReceiverParameter(newStaticReceiverParameter: FirTypeRef?) {}
    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {}

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {}

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {}

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {}

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {
        error("Body cannot be replaced for FirJavaField")
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        shouldNotBeCalled(::replaceStatus, ::status)
    }
}

@FirBuilderDsl
internal class FirJavaFieldBuilder : FirFieldBuilder() {
    var isFromSource: Boolean by Delegates.notNull()
    var annotationList: FirJavaAnnotationList = FirEmptyJavaAnnotationList
    var lazyInitializer: Lazy<FirExpression?>? = null
    lateinit var lazyHasConstantInitializer: Lazy<Boolean>
    lateinit var containingClassSymbol: FirClassSymbol<*>

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaField {
        return FirJavaField(
            source,
            moduleData,
            origin = javaOrigin(isFromSource),
            symbol,
            name,
            returnTypeRef,
            status as FirResolvedDeclarationStatusImpl,
            isVar,
            annotationList,
            lazyInitializer ?: lazyOf(initializer),
            lazyHasConstantInitializer,
            dispatchReceiverType,
            attributes,
            containingClassSymbol,
        )
    }

    @Deprecated("Modification of 'origin' has no impact for FirJavaFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirDeclarationOrigin
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
internal inline fun buildJavaField(init: FirJavaFieldBuilder.() -> Unit): FirJavaField {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirJavaFieldBuilder().apply(init).build()
}
