/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.enhancement.FirEmptyJavaAnnotationList
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaAnnotationList
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates

/** Java enum constant with lazy annotations to avoid re-entrant resolution (KT-74097). */
class FirJavaEnumEntry @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin.Java,
    override val symbol: FirEnumEntrySymbol,
    override val name: Name,
    override var returnTypeRef: FirTypeRef,
    override var status: FirDeclarationStatus,
    private val annotationList: FirJavaAnnotationList,
    override val attributes: FirDeclarationAttributes,
) : FirEnumEntry() {
    init {
        @OptIn(FirImplementationDetail::class)
        symbol.bind(this)

        @OptIn(ResolveStateAccess::class)
        this.resolveState = FirResolvePhase.ANALYZED_DEPENDENCIES.asResolveState()
    }

    override val isLocal: Boolean get() = false
    override val typeParameters: List<FirTypeParameterRef> get() = emptyList()
    override val contextParameters: List<FirValueParameter> get() = emptyList()
    override val receiverParameter: FirReceiverParameter? get() = null
    override val containerSource: DeserializedContainerSource? get() = null
    override val dispatchReceiverType: ConeSimpleKotlinType? get() = null
    override val delegate: FirExpression? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null
    override val backingField: FirBackingField? get() = null
    override var initializer: FirExpression? = null

    override val annotations: List<FirAnnotation> get() = annotationList
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    // TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val deprecationsProvider: DeprecationsProvider by lazy {
        annotations.getDeprecationsProviderFromAnnotations(moduleData.session, fromJava = true)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        initializer?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry {
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformInitializer(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry {
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry = this

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirJavaEnumEntry {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {}

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {}

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {}

    override fun replaceInitializer(newInitializer: FirExpression?) {
        initializer = newInitializer
    }

    override fun replaceDelegate(newDelegate: FirExpression?) {}

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {}

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {}

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        shouldNotBeCalled(::replaceAnnotations, ::annotations)
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }
}

@FirBuilderDsl
class FirJavaEnumEntryBuilder {
    var source: KtSourceElement? = null
    lateinit var moduleData: FirModuleData
    var isFromSource: Boolean by Delegates.notNull()
    lateinit var symbol: FirEnumEntrySymbol
    lateinit var name: Name
    lateinit var returnTypeRef: FirTypeRef
    lateinit var status: FirDeclarationStatus
    var annotationList: FirJavaAnnotationList = FirEmptyJavaAnnotationList
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    @OptIn(FirImplementationDetail::class)
    fun build(): FirJavaEnumEntry {
        return FirJavaEnumEntry(
            source,
            moduleData,
            origin = javaOrigin(isFromSource),
            symbol,
            name,
            returnTypeRef,
            status,
            annotationList,
            attributes,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildJavaEnumEntry(init: FirJavaEnumEntryBuilder.() -> Unit): FirJavaEnumEntry {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirJavaEnumEntryBuilder().apply(init).build()
}
