/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(FirImplementationDetail::class)
class FirJavaValueParameter @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override val name: Name,
    override val symbol: FirValueParameterSymbol,
    annotationBuilder: () -> List<FirAnnotation>,
    override var defaultValue: FirExpression?,
    override val isVararg: Boolean,
) : FirValueParameter() {
    init {
        symbol.bind(this)
    }

    override val isCrossinline: Boolean
        get() = false

    override val isNoinline: Boolean
        get() = false

    override val isVal: Boolean
        get() = true

    override val isVar: Boolean
        get() = false

    override val annotations: List<FirAnnotation> by lazy { annotationBuilder() }

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Java

    override val receiverTypeRef: FirTypeRef?
        get() = null

    override val deprecation: DeprecationsPerUseSite
        get() = EmptyDeprecationsPerUseSite

    override val initializer: FirExpression?
        get() = null

    override val delegate: FirExpression?
        get() = null

    override val getter: FirPropertyAccessor?
        get() = null

    override val setter: FirPropertyAccessor?
        get() = null

    override val backingField: FirBackingField?
        get() = null

    override val controlFlowGraphReference: FirControlFlowGraphReference?
        get() = null

    override val typeParameters: List<FirTypeParameterRef>
        get() = emptyList()

    override val status: FirDeclarationStatus
        get() = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS

    override val containerSource: DeserializedContainerSource?
        get() = null

    override val dispatchReceiverType: ConeKotlinType?
        get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirValueParameter {
        transformReturnTypeRef(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameter {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirValueParameter {
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirValueParameter {
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {
    }

    override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?) {

    }

    override fun replaceInitializer(newInitializer: FirExpression?) {
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
    }

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {
    }

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {
    }
}

@FirBuilderDsl
class FirJavaValueParameterBuilder {
    var source: KtSourceElement? = null
    lateinit var moduleData: FirModuleData
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var returnTypeRef: FirTypeRef
    lateinit var name: Name
    lateinit var annotationBuilder: () -> List<FirAnnotation>
    var defaultValue: FirExpression? = null
    var isVararg: Boolean by kotlin.properties.Delegates.notNull()

    @OptIn(FirImplementationDetail::class)
    fun build(): FirJavaValueParameter {
        return FirJavaValueParameter(
            source,
            moduleData,
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES,
            attributes,
            returnTypeRef,
            name,
            symbol = FirValueParameterSymbol(name),
            annotationBuilder,
            defaultValue,
            isVararg,
        )
    }
}

inline fun buildJavaValueParameter(init: FirJavaValueParameterBuilder.() -> Unit): FirJavaValueParameter {
    return FirJavaValueParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJavaValueParameterCopy(original: FirValueParameter, init: FirJavaValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirJavaValueParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.name = original.name
    val annotations = original.annotations
    copyBuilder.annotationBuilder = { annotations }
    copyBuilder.defaultValue = original.defaultValue
    copyBuilder.isVararg = original.isVararg
    return copyBuilder.apply(init).build()
}
