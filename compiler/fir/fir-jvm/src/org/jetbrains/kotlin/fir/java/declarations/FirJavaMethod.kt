/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParametersOwnerBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.java.enhancement.FirEmptyJavaAnnotationList
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaAnnotationList
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates

class FirJavaMethod @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin.Java,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override val typeParameters: MutableList<FirTypeParameter>,
    override val valueParameters: MutableList<FirValueParameter>,
    override val name: Name,
    private val originalStatus: FirResolvedDeclarationStatusImpl,
    override val symbol: FirNamedFunctionSymbol,
    val annotationList: FirJavaAnnotationList,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    val containingClassSymbol: FirClassSymbol<*>,
) : FirSimpleFunction() {
    init {
        @OptIn(FirImplementationDetail::class)
        symbol.bind(this)

        @OptIn(ResolveStateAccess::class)
        this.resolveState = FirResolvePhase.ANALYZED_DEPENDENCIES.asResolveState()
    }

    private val typeParameterBoundsResolveLock = ReentrantLock()

    override val receiverParameter: FirReceiverParameter?
        get() = null

    override val staticReceiverParameter: FirTypeRef?
        get() = null

    override val body: FirBlock?
        get() = null

    override val containerSource: DeserializedContainerSource?
        get() = null

    override val contractDescription: FirContractDescription?
        get() = null

    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    override val annotations: List<FirAnnotation> get() = annotationList

    // TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val status: FirDeclarationStatus by lazy {
        applyStatusTransformerExtensions(this, originalStatus) {
            transformStatus(it, this@FirJavaMethod, containingClassSymbol, isLocal = false)
        }
    }

    override val contextParameters: List<FirValueParameter>
        get() = emptyList()

    //not used actually, because get 'enhanced' into regular FirSimpleFunction
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider

    internal fun withTypeParameterBoundsResolveLock(f: () -> Unit) {
        // TODO: KT-68587
        typeParameterBoundsResolveLock.withLock(f)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverParameter?.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        transformReturnTypeRef(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transformSingle(transformer, data)
        transformValueParameters(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformStaticReceiverParameter(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        shouldNotBeCalled(::replaceAnnotations, ::annotations)
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceStaticReceiverParameter(newStaticReceiverParameter: FirTypeRef?) {
        error("Static receiver parameter cannot be replaced for FirJavaMethod")
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {}

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: FirBlock?) {
    }

    override fun replaceContractDescription(newContractDescription: FirContractDescription?) {
        error("Contract description cannot be replaced for FirJavaMethod")
    }

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {
        error("Body cannot be replaced for FirJavaMethod")
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        shouldNotBeCalled(::replaceStatus, ::status)
    }
}

@FirBuilderDsl
class FirJavaMethodBuilder : FirFunctionBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override lateinit var moduleData: FirModuleData
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var returnTypeRef: FirTypeRef
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    override lateinit var status: FirDeclarationStatus
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    lateinit var name: Name
    lateinit var symbol: FirNamedFunctionSymbol
    override val annotations: MutableList<FirAnnotation> get() = shouldNotBeCalled()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    var isStatic: Boolean by Delegates.notNull()
    override var resolvePhase: FirResolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
    var isFromSource: Boolean by Delegates.notNull()
    var annotationList: FirJavaAnnotationList = FirEmptyJavaAnnotationList
    lateinit var containingClassSymbol: FirClassSymbol<*>

    @Deprecated("Modification of 'deprecation' has no impact for FirJavaFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var deprecationsProvider: DeprecationsProvider
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'containerSource' has no impact for FirJavaFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var containerSource: DeserializedContainerSource?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'origin' has no impact for FirJavaFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirDeclarationOrigin
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'contextParameters' has no impact for FirJavaFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override val contextParameters: MutableList<FirValueParameter>
        get() = throw IllegalStateException()

    @Deprecated("Modification of 'staticReceiverParameter' has no impact for FirJavaFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var staticReceiverParameter: FirTypeRef?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaMethod {
        return FirJavaMethod(
            source,
            moduleData,
            origin = javaOrigin(isFromSource),
            attributes,
            returnTypeRef,
            typeParameters,
            valueParameters,
            name,
            status as FirResolvedDeclarationStatusImpl,
            symbol,
            annotationList,
            dispatchReceiverType,
            containingClassSymbol,
        )
    }
}

inline fun buildJavaMethod(init: FirJavaMethodBuilder.() -> Unit): FirJavaMethod {
    return FirJavaMethodBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJavaMethodCopy(original: FirJavaMethod, init: FirJavaMethodBuilder.() -> Unit): FirJavaMethod {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirJavaMethodBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.status = original.status
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.name = original.name
    copyBuilder.symbol = original.symbol
    copyBuilder.isFromSource = original.origin.fromSource
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.annotationList = original.annotationList
    copyBuilder.containingClassSymbol = original.containingClassSymbol
    return copyBuilder.apply(init).build()
}
