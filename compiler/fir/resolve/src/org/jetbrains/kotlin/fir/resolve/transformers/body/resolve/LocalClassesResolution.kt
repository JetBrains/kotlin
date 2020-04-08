/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.transformers.runStatusResolveForLocalClass
import org.jetbrains.kotlin.fir.resolve.transformers.runSupertypeResolvePhaseForLocalClass
import org.jetbrains.kotlin.fir.resolve.transformers.runTypeResolvePhaseForLocalClass
import org.jetbrains.kotlin.fir.scopes.impl.createCurrentScopeList

fun <F : FirClass<F>> F.runAllPhasesForLocalClass(
    transformer: FirAbstractBodyResolveTransformer,
    components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionMode: ResolutionMode
): F {
    if (this.resolvePhase > FirResolvePhase.RAW_FIR) return this
    this.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
    val localClassesNavigationInfo = collectLocalClassesNavigationInfo()
    runSupertypeResolvePhaseForLocalClass(
        components.session,
        components.scopeSession,
        components.createCurrentScopeList(),
        localClassesNavigationInfo
    )
    runTypeResolvePhaseForLocalClass(
        components.session,
        components.scopeSession,
        components.createCurrentScopeList()
    )
    runStatusResolveForLocalClass(components.session)
    runBodiesResolutionForLocalClass(components, resolutionMode, localClassesNavigationInfo)
    return this
}
