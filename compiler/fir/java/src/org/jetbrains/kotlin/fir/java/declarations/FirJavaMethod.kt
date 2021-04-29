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
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParametersOwnerBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.util.OperatorNameConventions.ASSIGNMENT_OPERATIONS
import org.jetbrains.kotlin.util.OperatorNameConventions.BINARY_OPERATION_NAMES
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPARE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.CONTAINS
import org.jetbrains.kotlin.util.OperatorNameConventions.DELEGATED_PROPERTY_OPERATORS
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.GET
import org.jetbrains.kotlin.util.OperatorNameConventions.HAS_NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.INVOKE
import org.jetbrains.kotlin.util.OperatorNameConventions.ITERATOR
import org.jetbrains.kotlin.util.OperatorNameConventions.NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.SET
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_OPERATION_NAMES
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates

@OptIn(FirImplementationDetail::class)
class FirJavaMethod @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override var resolvePhase: FirResolvePhase,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override val typeParameters: MutableList<FirTypeParameter>,
    override val valueParameters: MutableList<FirValueParameter>,
    override val name: Name,
    override var status: FirDeclarationStatus,
    override val symbol: FirNamedFunctionSymbol,
    annotationBuilder: () -> List<FirAnnotationCall>,
    override val dispatchReceiverType: ConeKotlinType?,
) : FirSimpleFunction() {
    init {
        symbol.bind(this)
    }

    override val receiverTypeRef: FirTypeRef?
        get() = null

    override val body: FirBlock?
        get() = null

    override val containerSource: DeserializedContainerSource?
        get() = null

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Java

    override val contractDescription: FirContractDescription
        get() = FirEmptyContractDescription

    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    override val annotations: List<FirAnnotationCall> by lazy { annotationBuilder() }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        status.accept(visitor, data)
        contractDescription.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transformSingle(transformer, data)
        transformValueParameters(transformer, data)
        transformBody(transformer, data)
        transformStatus(transformer, data)
        transformContractDescription(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
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
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunction {
        typeParameters.transformInplace(transformer, data)
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

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: FirBlock?) {
    }

    override fun replaceContractDescription(newContractDescription: FirContractDescription) {
    }
}

val ALL_JAVA_OPERATION_NAMES =
    UNARY_OPERATION_NAMES + BINARY_OPERATION_NAMES + ASSIGNMENT_OPERATIONS + DELEGATED_PROPERTY_OPERATORS +
            EQUALS + COMPARE_TO + CONTAINS + INVOKE + ITERATOR + GET + SET + NEXT + HAS_NEXT

@FirBuilderDsl
class FirJavaMethodBuilder : FirFunctionBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override lateinit var moduleData: FirModuleData
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var returnTypeRef: FirTypeRef
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    lateinit var status: FirDeclarationStatus
    var dispatchReceiverType: ConeKotlinType? = null
    lateinit var name: Name
    lateinit var symbol: FirNamedFunctionSymbol
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    lateinit var visibility: Visibility
    var modality: Modality? = null
    var isStatic: Boolean by Delegates.notNull()
    override var resolvePhase: FirResolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
    lateinit var annotationBuilder: () -> List<FirAnnotationCall>

    @Deprecated("Modification of 'origin' has no impact for FirJavaFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirDeclarationOrigin
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaMethod {
        return FirJavaMethod(
            source,
            moduleData,
            resolvePhase,
            attributes,
            returnTypeRef as FirJavaTypeRef,
            typeParameters,
            valueParameters,
            name,
            status,
            symbol,
            annotationBuilder,
            dispatchReceiverType,
        )
    }
}

inline fun buildJavaMethod(init: FirJavaMethodBuilder.() -> Unit): FirJavaMethod {
    return FirJavaMethodBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJavaMethodCopy(original: FirSimpleFunction, init: FirJavaMethodBuilder.() -> Unit): FirJavaMethod {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirJavaMethodBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.status = original.status
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.name = original.name
    copyBuilder.symbol = original.symbol
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.typeParameters.addAll(original.typeParameters)
    val annotations = original.annotations
    copyBuilder.annotationBuilder = { annotations }
    return copyBuilder
        .apply(init)
        .build()
}
