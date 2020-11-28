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
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
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
import kotlin.properties.Delegates

@OptIn(FirImplementationDetail::class)
class FirJavaMethod @FirImplementationDetail constructor(
    source: FirSourceElement?,
    session: FirSession,
    resolvePhase: FirResolvePhase,
    attributes: FirDeclarationAttributes,
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
    dispatchReceiverType: ConeKotlinType?,
) : FirSimpleFunctionImpl(
    source,
    session,
    resolvePhase,
    FirDeclarationOrigin.Java,
    attributes,
    returnTypeRef,
    receiverTypeRef,
    valueParameters,
    body,
    status,
    containerSource,
    dispatchReceiverType = dispatchReceiverType,
    contractDescription = FirEmptyContractDescription,
    name,
    symbol,
    annotations,
    typeParameters,
)

val ALL_JAVA_OPERATION_NAMES =
    UNARY_OPERATION_NAMES + BINARY_OPERATION_NAMES + ASSIGNMENT_OPERATIONS + DELEGATED_PROPERTY_OPERATORS +
            EQUALS + COMPARE_TO + CONTAINS + INVOKE + ITERATOR + GET + SET + NEXT + HAS_NEXT

@FirBuilderDsl
class FirJavaMethodBuilder : FirSimpleFunctionBuilder() {
    lateinit var visibility: Visibility
    var modality: Modality? = null
    var isStatic: Boolean by Delegates.notNull()
    override var resolvePhase: FirResolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

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
            session,
            resolvePhase,
            attributes,
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
            dispatchReceiverType,
        )
    }
}

inline fun buildJavaMethod(init: FirJavaMethodBuilder.() -> Unit): FirJavaMethod {
    return FirJavaMethodBuilder().apply(init).build()
}
