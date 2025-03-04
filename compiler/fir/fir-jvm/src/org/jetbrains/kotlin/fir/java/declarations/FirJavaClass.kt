/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.enhancement.*
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

class FirJavaClass @FirImplementationDetail internal constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val name: Name,
    override val origin: FirDeclarationOrigin.Java,
    private val annotationList: FirJavaAnnotationList,
    internal val originalStatus: FirResolvedDeclarationStatusImpl,
    override val classKind: ClassKind,
    private val declarationList: FirJavaDeclarationList,
    override val scopeProvider: FirScopeProvider,
    override val symbol: FirRegularClassSymbol,
    private val nonEnhancedSuperTypes: List<FirTypeRef>,
    val nonEnhancedTypeParameters: List<FirTypeParameterRef>,
    internal val javaPackage: JavaPackage?,

    /**
     * Contains mapping for type parameters from classes.
     *
     * @see javaTypeParameterStack
     */
    val classJavaTypeParameterStack: MutableJavaTypeParameterStack,
    internal val existingNestedClassifierNames: List<Name>,
    internal val containingClassSymbol: FirClassSymbol<*>?,
) : FirRegularClass() {
    /**
     * Unlike [classJavaTypeParameterStack] contains mapping not only for classes,
     * but also for all member functions.
     *
     * @see classJavaTypeParameterStack
     */
    val javaTypeParameterStack: JavaTypeParameterStack by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val copy = classJavaTypeParameterStack.copy()
        for (declaration in declarations) {
            if (declaration !is FirTypeParameterRefsOwner) continue
            for (typeParameter in declaration.typeParameters) {
                if (typeParameter !is FirJavaTypeParameter) continue
                copy.addParameter(typeParameter.javaTypeParameter, typeParameter.symbol)
            }
        }

        copy
    }

    override val hasLazyNestedClassifiers: Boolean get() = true
    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null

    override val contextParameters: List<FirValueParameter>
        get() = emptyList()

    init {
        @OptIn(FirImplementationDetail::class)
        symbol.bind(this)

        @OptIn(ResolveStateAccess::class)
        this.resolveState = FirResolvePhase.ANALYZED_DEPENDENCIES.asResolveState()
    }

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    // TODO: the lazy superTypeRefs is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val superTypeRefs: List<FirTypeRef> by lazy {
        val enhancement = FirSignatureEnhancement(
            this,
            moduleData.session,
            enhanceClassHeaderOnly = true,
            overridden = { emptyList() },
        )

        enhancement.enhanceSuperTypes(nonEnhancedSuperTypes)
    }

    // TODO: the lazy annotations is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val annotations: List<FirAnnotation> get() = annotationList

    // TODO: the lazy declarations is a workaround for KT-55387, some non-lazy solution should probably be used instead
    @DirectDeclarationsAccess
    override val declarations: List<FirDeclaration> get() = declarationList.declarations

    // TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val deprecationsProvider: DeprecationsProvider by lazy {
        getDeprecationsProvider(moduleData.session)
    }

    // TODO: KT-68587
    private val typeParameterBoundsResolveLock = ReentrantLock()

    /**
     * It is crucial to have [LazyThreadSafetyMode.PUBLICATION] here as [typeParameters] can be
     * accessible recursively via [FirSignatureEnhancement.enhanceTypeParameterBounds] from different threads,
     * so we may avoid deadlock.
     *
     * TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
     */
    override val typeParameters: List<FirTypeParameterRef> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val enhancement = FirSignatureEnhancement(
            this,
            moduleData.session,
            enhanceClassHeaderOnly = true,
            overridden = { emptyList() },
        )

        enhancement.enhanceTypeParameterBounds(this, nonEnhancedTypeParameters, typeParameterBoundsResolveLock::withLock)
        nonEnhancedTypeParameters
    }

    // TODO: the lazy deprecationsProvider is a workaround for KT-55387, some non-lazy solution should probably be used instead
    override val status: FirDeclarationStatus by lazy {
        applyStatusTransformerExtensions(this, originalStatus) {
            transformStatus(it, this@FirJavaClass, containingClassSymbol, isLocal = false)
        }
    }

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        shouldNotBeCalled(::replaceSuperTypeRefs, ::superTypeRefs)
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        shouldNotBeCalled(::replaceDeprecationsProvider, ::deprecationsProvider)
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
        return this
    }

    override fun <D> transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirRegularClass {
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirRegularClass {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaClass {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        shouldNotBeCalled(::replaceAnnotations, ::annotations)
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaClass {
        return this
    }

    override fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirJavaClass {
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirRegularClass {
        return this
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        shouldNotBeCalled(::replaceStatus, ::status)
    }
}

@FirBuilderDsl
class FirJavaClassBuilder : FirRegularClassBuilder(), FirAnnotationContainerBuilder {
    lateinit var visibility: Visibility
    var modality: Modality? = null
    var isFromSource: Boolean by Delegates.notNull()
    var isTopLevel: Boolean by Delegates.notNull()
    var isStatic: Boolean by Delegates.notNull()
    var javaPackage: JavaPackage? = null
    lateinit var javaTypeParameterStack: MutableJavaTypeParameterStack
    val existingNestedClassifierNames: MutableList<Name> = mutableListOf()

    override var source: KtSourceElement? = null
    var annotationList: FirJavaAnnotationList = FirEmptyJavaAnnotationList
    override val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    override val declarations: MutableList<FirDeclaration> get() = shouldNotBeCalled()

    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()
    var containingClassSymbol: FirClassSymbol<*>? = null
    var declarationList: FirJavaDeclarationList = FirEmptyJavaDeclarationList

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaClass {
        return FirJavaClass(
            source,
            moduleData,
            name,
            origin = javaOrigin(isFromSource),
            annotationList,
            status as FirResolvedDeclarationStatusImpl,
            classKind,
            declarationList,
            scopeProvider,
            symbol,
            superTypeRefs,
            typeParameters,
            javaPackage,
            javaTypeParameterStack.copy(),
            existingNestedClassifierNames,
            containingClassSymbol,
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
