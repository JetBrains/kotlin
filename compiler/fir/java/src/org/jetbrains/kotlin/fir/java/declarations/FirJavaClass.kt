/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.Name
import kotlin.properties.Delegates
import org.jetbrains.kotlin.fir.visitors.FirElementKind

class FirJavaClass @FirImplementationDetail internal constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val name: Name,
    override val origin: FirDeclarationOrigin.Java,
    override val annotations: MutableList<FirAnnotation>,
    override var status: FirDeclarationStatus,
    override val classKind: ClassKind,
    override val declarations: MutableList<FirDeclaration>,
    override val scopeProvider: FirScopeProvider,
    override val symbol: FirRegularClassSymbol,
    override val superTypeRefs: MutableList<FirTypeRef>,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    internal val javaPackage: JavaPackage?,
    val javaTypeParameterStack: JavaTypeParameterStack,
    internal val existingNestedClassifierNames: List<Name>
) : FirRegularClass() {
    override val hasLazyNestedClassifiers: Boolean get() = true
    override val controlFlowGraphReference: FirControlFlowGraphReference? get() = null
    override var deprecation: DeprecationsPerUseSite? = null

    override val contextReceivers: List<FirContextReceiver>
        get() = emptyList()

    init {
        symbol.bind(this)
    }

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        error("cannot be replaced for FirJavaClass")
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>) {
        error("cannot be replaced for FirJavaClass")
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        error("cannot be replaced for FirJavaClass")
    }

    override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?) {
        deprecation = newDeprecation
    }

    override fun replaceDeclarations(newDeclarations: List<FirDeclaration>) {
        error("cannot be replaced for FirJavaClass")
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        error("cannot be replaced for FirJavaClass")
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {}

    override val companionObjectSymbol: FirRegularClassSymbol?
        get() = null

    override fun replaceCompanionObjectSymbol(newCompanionObjectSymbol: FirRegularClassSymbol?) {}
    override val elementKind: FirElementKind
        get() = FirElementKind.RegularClass

}

@FirBuilderDsl
internal class FirJavaClassBuilder : FirRegularClassBuilder(), FirAnnotationContainerBuilder {
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
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
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
            annotations,
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

internal inline fun buildJavaClass(init: FirJavaClassBuilder.() -> Unit): FirJavaClass {
    return FirJavaClassBuilder().apply(init).build()
}
