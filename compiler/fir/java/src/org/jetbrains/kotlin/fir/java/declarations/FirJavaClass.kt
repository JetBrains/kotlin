/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.convertAnnotationsToFir
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.Name
import kotlin.properties.Delegates

class FirJavaClass @FirImplementationDetail internal constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val name: Name,
    override val origin: FirDeclarationOrigin.Java,
    private val unEnhancedAnnotations: MutableOrEmptyList<JavaAnnotation>,
    override var status: FirDeclarationStatus,
    override val classKind: ClassKind,
    override val declarations: MutableList<FirDeclaration>,
    override val scopeProvider: FirScopeProvider,
    override val symbol: FirRegularClassSymbol,
    private val unenhnancedSuperTypes: List<FirTypeRef>,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    internal val javaPackage: JavaPackage?,
    val javaTypeParameterStack: JavaTypeParameterStack,
    internal val existingNestedClassifierNames: List<Name>
) : FirRegularClass() {
    override val hasLazyNestedClassifiers: Boolean get() = true
    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null

    override val contextReceivers: List<FirContextReceiver>
        get() = emptyList()

    init {
        symbol.bind(this)
    }

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    // TODO: the lazy superTypeRefs is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val superTypeRefs: List<FirTypeRef> by lazy {
        val enhancement = FirSignatureEnhancement(this@FirJavaClass, moduleData.session, overridden = { emptyList() })
        enhancement.enhanceSuperTypes(unenhnancedSuperTypes)
    }

    // TODO: the lazy annotations is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val annotations: List<FirAnnotation> by lazy {
        unEnhancedAnnotations.convertAnnotationsToFir(moduleData.session, javaTypeParameterStack)
    }

    // TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val deprecationsProvider: DeprecationsProvider by lazy {
        getDeprecationsProvider(moduleData.session)
    }

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        error("${::replaceSuperTypeRefs.name} should not be called for ${this::class.simpleName}, ${superTypeRefs::class.simpleName} is lazily calulated")
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        error("${::replaceDeprecationsProvider.name} should not be called for ${this::class.simpleName}, ${deprecationsProvider::class.simpleName} is lazily calculated")
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {}

    override val companionObjectSymbol: FirRegularClassSymbol?
        get() = null

    override fun replaceCompanionObjectSymbol(newCompanionObjectSymbol: FirRegularClassSymbol?) {}

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        superTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaClass {
        transformTypeParameters(transformer, data)
        transformDeclarations(transformer, data)
        status = status.transformSingle(transformer, data)
        transformSuperTypeRefs(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirRegularClass {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaClass {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        error("${::replaceAnnotations.name} should not be called for ${this::class.simpleName}, ${annotations::class.simpleName} is lazily calculated")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaClass {
        return this
    }

    override fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirJavaClass {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirRegularClass {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }
}

@FirBuilderDsl
class FirJavaClassBuilder : FirRegularClassBuilder(), FirAnnotationContainerBuilder {
    lateinit var visibility: Visibility
    var modality: Modality? = null
    var isFromSource: Boolean by Delegates.notNull()
    var isTopLevel: Boolean by Delegates.notNull()
    var isStatic: Boolean by Delegates.notNull()
    var isNotSam: Boolean by Delegates.notNull()
    var javaPackage: JavaPackage? = null
    lateinit var javaTypeParameterStack: JavaTypeParameterStack
    val existingNestedClassifierNames: MutableList<Name> = mutableListOf()

    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    var javaAnnotations: MutableList<JavaAnnotation> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    override val declarations: MutableList<FirDeclaration> = mutableListOf()

    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaClass {
        return FirJavaClass(
            source,
            moduleData,
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES,
            name,
            origin = javaOrigin(isFromSource),
            javaAnnotations.toMutableOrEmpty(),
            status,
            classKind,
            declarations,
            scopeProvider,
            symbol,
            superTypeRefs,
            typeParameters,
            javaPackage,
            javaTypeParameterStack,
            existingNestedClassifierNames
        )
    }

    @Deprecated("Modification of 'origin' has no impact for FirJavaClassBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirDeclarationOrigin
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }
}

inline fun buildJavaClass(init: FirJavaClassBuilder.() -> Unit): FirJavaClass {
    return FirJavaClassBuilder().apply(init).build()
}
