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
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirRegularClass : FirClass(), FirControlFlowGraphOwner {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val deprecation: DeprecationsPerUseSite?
    abstract override val classKind: ClassKind
    abstract override val declarations: List<FirDeclaration>
    abstract override val annotations: List<FirAnnotation>
    abstract override val scopeProvider: FirScopeProvider
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract val name: Name
    abstract override val symbol: FirRegularClassSymbol
    abstract val hasLazyNestedClassifiers: Boolean
    abstract val companionObjectSymbol: FirRegularClassSymbol?
    abstract override val superTypeRefs: List<FirTypeRef>
    abstract val contextReceivers: List<FirContextReceiver>


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceDeclarations(newDeclarations: List<FirDeclaration>)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract fun replaceCompanionObjectSymbol(newCompanionObjectSymbol: FirRegularClassSymbol?)

    abstract override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)

    abstract fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)
}

inline fun <D> FirRegularClass.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirRegularClass 
     = apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirRegularClass.transformStatus(transformer: FirTransformer<D>, data: D): FirRegularClass 
     = apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirRegularClass.transformDeclarations(transformer: FirTransformer<D>, data: D): FirRegularClass 
     = apply { replaceDeclarations(declarations.transform(transformer, data)) }

inline fun <D> FirRegularClass.transformAnnotations(transformer: FirTransformer<D>, data: D): FirRegularClass 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirRegularClass.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirRegularClass 
     = apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirRegularClass.transformSuperTypeRefs(transformer: FirTransformer<D>, data: D): FirRegularClass 
     = apply { replaceSuperTypeRefs(superTypeRefs.transform(transformer, data)) }

inline fun <D> FirRegularClass.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirRegularClass 
     = apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }
