/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.ResolutionMode

fun <F : FirClass<F>> F.runAllPhasesForLocalClass(
    components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionMode: ResolutionMode
): F {
    val localClassesNavigationInfo = collectLocalClassesNavigationInfo()
    runBodiesResolutionForLocalClass(components, resolutionMode, localClassesNavigationInfo)
    return this
}
