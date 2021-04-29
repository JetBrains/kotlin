/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import kotlin.properties.Delegates

@OptIn(FirImplementationDetail::class)
class FirJavaClass @FirImplementationDetail internal constructor(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override var resolvePhase: FirResolvePhase,
    override val name: Name,
    override val annotations: MutableList<FirAnnotationCall>,
    override var status: FirDeclarationStatus,
    override val classKind: ClassKind,
    override val declarations: MutableList<FirDeclaration>,
    override val scopeProvider: FirScopeProvider,
    override val symbol: FirRegularClassSymbol,
    override val superTypeRefs: MutableList<FirTypeRef>,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    internal val javaTypeParameterStack: JavaTypeParameterStack,
    internal val existingNestedClassifierNames: List<Name>
) : FirRegularClass() {
    override val hasLazyNestedClassifiers: Boolean get() = true
    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null

    init {
        symbol.bind(this)
    }

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Java

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    override val companionObject: FirRegularClass?
        get() = null

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {}

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
        superTypeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaClass {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaClass {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirJavaClass {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCompanionObject(transformer: FirTransformer<D>, data: D): FirJavaClass {
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirRegularClass {
        typeParameters.transformInplace(transformer, data)
        return this
    }
}

@FirBuilderDsl
internal class FirJavaClassBuilder : FirRegularClassBuilder(), FirAnnotationContainerBuilder {
    lateinit var visibility: Visibility
    var modality: Modality? = null
    var isTopLevel: Boolean by Delegates.notNull()
    var isStatic: Boolean by Delegates.notNull()
    var isNotSam: Boolean by Delegates.notNull()
    lateinit var javaTypeParameterStack: JavaTypeParameterStack
    val existingNestedClassifierNames: MutableList<Name> = mutableListOf()

    override var source: FirSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
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
            annotations,
            status,
            classKind,
            declarations,
            scopeProvider,
            symbol,
            superTypeRefs,
            typeParameters,
            javaTypeParameterStack,
            existingNestedClassifierNames
        )
    }

    @Deprecated("Modification of 'hasLazyNestedClassifiers' has no impact for FirRegularClassImplBuilder", level = DeprecationLevel.HIDDEN)
    override var companionObject: FirRegularClass?
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'origin' has no impact for FirJavaClassBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirDeclarationOrigin
        get() = throw IllegalStateException()
        set(@Suppress("UNUSED_PARAMETER") value) {
            throw IllegalStateException()
        }
}

internal inline fun buildJavaClass(init: FirJavaClassBuilder.() -> Unit): FirJavaClass {
    return FirJavaClassBuilder().apply(init).build()
}
