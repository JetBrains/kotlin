/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirFieldBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
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
import org.jetbrains.kotlin.fir.visitors.FirElementKind

class FirJavaField @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin.Java,
    override val symbol: FirFieldSymbol,
    override val name: Name,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override var returnTypeRef: FirTypeRef,
    override var status: FirDeclarationStatus,
    override val isVar: Boolean,
    annotationBuilder: () -> List<FirAnnotation>,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    override var initializer: FirExpression?,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override val attributes: FirDeclarationAttributes,
) : FirField() {
    init {
        symbol.bind(this)
    }

    override val receiverTypeRef: FirTypeRef? get() = null
    override val isVal: Boolean get() = !isVar
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null
    override val backingField: FirBackingField? = null
    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null

    override val annotations: List<FirAnnotation> by lazy { annotationBuilder() }

    override val deprecation: DeprecationsPerUseSite by lazy {
        annotations.getDeprecationInfosFromAnnotations(moduleData.session.languageVersionSettings.apiVersion, fromJava = true)
    }

    override val contextReceivers: List<FirContextReceiver>
        get() = emptyList()

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>) {
        error("cannot be replaced for FirJavaField")
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        error("cannot be replaced for FirJavaField")
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceInitializer(newInitializer: FirExpression?) {
        initializer = newInitializer
    }

    override fun replaceDelegate(newDelegate: FirExpression?) {
        error("cannot be replaced for FirJavaField")
    }

    override val delegate: FirExpression?
        get() = null

    override var containerSource: DeserializedContainerSource? = null

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {}

    override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?) {}

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {}

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {}
    override fun replaceBackingField(newBackingField: FirBackingField?) {
        error("cannot be replaced for FirJavaField")
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        error("cannot be replaced for FirJavaField")
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {}

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        error("Body cannot be replaced for FirJavaField")
    }

    override val elementKind: FirElementKind
        get() = TODO("not implemented")
}

@FirBuilderDsl
internal class FirJavaFieldBuilder : FirFieldBuilder() {
    var modality: Modality? = null
    lateinit var visibility: Visibility
    var isStatic: Boolean by Delegates.notNull()
    var isFromSource: Boolean by Delegates.notNull()
    lateinit var annotationBuilder: () -> List<FirAnnotation>

    override var resolvePhase: FirResolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaField {
        return FirJavaField(
            source,
            moduleData,
            origin = javaOrigin(isFromSource),
            symbol,
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
