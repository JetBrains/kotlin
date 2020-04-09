/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.AbstractFirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
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
    override val session: FirSession,
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
    override val controlFlowGraphReference: FirControlFlowGraphReference get() = FirEmptyControlFlowGraphReference

    init {
        symbol.bind(this)
    }


    override val companionObject: FirRegularClass?
        get() = null

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        superTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaClass {
        typeParameters.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        status = status.transformSingle(transformer, data)
        superTypeRefs.transformInplace(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaClass {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirRegularClass {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaClass {
        annotations.transformInplace(transformer, data)
        return this
    }
}

@FirBuilderDsl
internal class FirJavaClassBuilder : AbstractFirRegularClassBuilder, FirAnnotationContainerBuilder {
    lateinit var visibility: Visibility
    var modality: Modality? = null
    var isTopLevel: Boolean by Delegates.notNull()
    var isStatic: Boolean by Delegates.notNull()
    var isNotSam: Boolean by Delegates.notNull()
    lateinit var javaTypeParameterStack: JavaTypeParameterStack
    val existingNestedClassifierNames: MutableList<Name> = mutableListOf()

    override var source: FirSourceElement? = null
    override lateinit var session: FirSession
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var name: Name
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    override lateinit var status: FirDeclarationStatus
    override lateinit var classKind: ClassKind
    override val declarations: MutableList<FirDeclaration> = mutableListOf()
    override lateinit var scopeProvider: FirScopeProvider
    override lateinit var symbol: FirRegularClassSymbol

    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirJavaClass {
        val status = FirDeclarationStatusImpl(visibility, modality).apply {
            isInner = !isTopLevel && !isStatic
            isCompanion = false
            isData = false
            isInline = false
        }

        return FirJavaClass(
            source,
            session,
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

    @Deprecated("Modification of 'hasLazyNestedClassifiers' has no impact for FirClassImplBuilder", level = DeprecationLevel.HIDDEN)
    override var companionObject: FirRegularClass?
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'hasLazyNestedClassifiers' has no impact for FirClassImplBuilder", level = DeprecationLevel.HIDDEN)
    override var hasLazyNestedClassifiers: Boolean
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'controlFlowGraphReference' has no impact for FirClassImplBuilder", level = DeprecationLevel.HIDDEN)
    override var controlFlowGraphReference: FirControlFlowGraphReference
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

internal inline fun buildJavaClass(init: FirJavaClassBuilder.() -> Unit): FirJavaClass {
    return FirJavaClassBuilder().apply(init).build()
}