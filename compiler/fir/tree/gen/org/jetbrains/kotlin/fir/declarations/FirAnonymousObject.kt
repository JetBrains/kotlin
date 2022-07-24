/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
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

abstract class FirAnonymousObject : FirClass(), FirControlFlowGraphOwner {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val deprecation: DeprecationsPerUseSite?
    abstract override val classKind: ClassKind
    abstract override val superTypeRefs: List<FirTypeRef>
    abstract override val declarations: List<FirDeclaration>
    abstract override val annotations: List<FirAnnotation>
    abstract override val scopeProvider: FirScopeProvider
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val symbol: FirAnonymousObjectSymbol


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)

    abstract override fun replaceDeclarations(newDeclarations: List<FirDeclaration>)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)
}

inline fun <D> FirAnonymousObject.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirAnonymousObject 
     = apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirAnonymousObject.transformStatus(transformer: FirTransformer<D>, data: D): FirAnonymousObject 
     = apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirAnonymousObject.transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirAnonymousObject 
     = apply { replaceSuperTypeRefs(superTypeRefs.transform(transformer, data)) }

inline fun <D> FirAnonymousObject.transformDeclarations(transformer: FirTransformer<D>, data: D): FirAnonymousObject 
     = apply { replaceDeclarations(declarations.transform(transformer, data)) }

inline fun <D> FirAnonymousObject.transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousObject 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirAnonymousObject.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirAnonymousObject 
     = apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }
