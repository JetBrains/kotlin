/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAnonymousObjectImpl(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    override var status: FirDeclarationStatus,
    override var deprecation: DeprecationsPerUseSite?,
    override val classKind: ClassKind,
    override val superTypeRefs: MutableList<FirTypeRef>,
    override val declarations: MutableList<FirDeclaration>,
    override val annotations: MutableList<FirAnnotation>,
    override val scopeProvider: FirScopeProvider,
    override val symbol: FirAnonymousObjectSymbol,
) : FirAnonymousObject() {
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
    }

    override val elementKind get() = FirElementKind.AnonymousObject

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>) {
        typeParameters.clear()
        typeParameters.addAll(newTypeParameters)
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?) {
        deprecation = newDeprecation
    }

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }

    override fun replaceDeclarations(newDeclarations: List<FirDeclaration>) {
        declarations.clear()
        declarations.addAll(newDeclarations)
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }
}
