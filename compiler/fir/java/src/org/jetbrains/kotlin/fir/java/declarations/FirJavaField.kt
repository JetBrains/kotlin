/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirFieldBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates

@OptIn(FirImplementationDetail::class)
class FirJavaField @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override val symbol: FirFieldSymbol,
    override val name: Name,
    override var resolvePhase: FirResolvePhase,
    override var returnTypeRef: FirTypeRef,
    override var status: FirDeclarationStatus,
    override val isVar: Boolean,
    annotationBuilder: () -> List<FirAnnotationCall>,
    override val typeParameters: MutableList<FirTypeParameter>,
    override var initializer: FirExpression?,
    override val dispatchReceiverType: ConeKotlinType?,
    override val attributes: FirDeclarationAttributes,
) : FirField() {
    init {
        symbol.bind(this)
    }

    override val receiverTypeRef: FirTypeRef? get() = null
    override val isVal: Boolean get() = !isVar
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Java

    override val annotations: List<FirAnnotationCall> by lazy { annotationBuilder() }


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

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirField {
        transformAnnotations(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
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
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaField {
        return this
    }

    override fun replaceInitializer(newInitializer: FirExpression?) {
        initializer = newInitializer
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirField {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override val delegate: FirExpression?
        get() = null

    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirField>?
        get() = null

    override var containerSource: DeserializedContainerSource? = null

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {}

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirField {
        return this
    }
}

@FirBuilderDsl
internal class FirJavaFieldBuilder : FirFieldBuilder() {
    var modality: Modality? = null
    lateinit var visibility: Visibility
    var isStatic: Boolean by Delegates.notNull()
    lateinit var annotationBuilder: () -> List<FirAnnotationCall>

    override var resolvePhase: FirResolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaField {
        return FirJavaField(
            source,
            moduleData,
            symbol as FirFieldSymbol,
            name,
            resolvePhase,
            returnTypeRef,
            status,
            isVar,
            annotationBuilder,
            typeParameters,
            initializer,
            dispatchReceiverType,
            attributes,
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
