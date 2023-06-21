/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.createCurrentScopeList
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.runCompilerRequiredAnnotationsResolvePhaseForLocalClass
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.runCompanionGenerationPhaseForLocalClass
import org.jetbrains.kotlin.fir.resolve.transformers.runStatusResolveForLocalClass
import org.jetbrains.kotlin.fir.resolve.transformers.runSupertypeResolvePhaseForLocalClass
import org.jetbrains.kotlin.fir.resolve.transformers.runTypeResolvePhaseForLocalClass

fun <F : FirClassLikeDeclaration> F.runAllPhasesForLocalClass(
    transformer: FirAbstractBodyResolveTransformer,
    components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionMode: ResolutionMode,
    firResolveContextCollector: FirResolveContextCollector?
): F {
    if (status is FirResolvedDeclarationStatus) return this
    if (this is FirRegularClass) {
        components.context.storeClassIfNotNested(this, components.session)
    }
    this.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
    val localClassesNavigationInfo = collectLocalClassesNavigationInfo()

    for ((nested, outer) in localClassesNavigationInfo.parentForClass) {
        if (outer == null) continue
        components.context.outerLocalClassForNested[nested.symbol] = outer.symbol
    }

    runCompilerRequiredAnnotationsResolvePhaseForLocalClass(
        components.session,
        components.scopeSession,
        localClassesNavigationInfo,
        components.file,
        components.containingDeclarations,
    )
    runCompanionGenerationPhaseForLocalClass(components.session)
    runSupertypeResolvePhaseForLocalClass(
        components.session,
        components.scopeSession,
        components.createCurrentScopeList(),
        localClassesNavigationInfo,
        components.file,
        components.containingDeclarations,
    )
    runTypeResolvePhaseForLocalClass(
        components.session,
        components.scopeSession,
        components.createCurrentScopeList(),
        components.file,
        components.containingDeclarations
    )
    runStatusResolveForLocalClass(
        components.session,
        components.scopeSession,
        components.createCurrentScopeList(),
        localClassesNavigationInfo
    )
    runContractAndBodiesResolutionForLocalClass(
        components,
        resolutionMode,
        localClassesNavigationInfo,
        firResolveContextCollector
    )
    return this
}
