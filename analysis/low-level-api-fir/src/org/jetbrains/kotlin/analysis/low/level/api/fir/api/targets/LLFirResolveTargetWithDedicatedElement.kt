/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript

/**
 * [LLFirResolveTarget] representing a target with a dedicated main element.
 * Such resolved is supposed to resolve [target] element and/or its declaration subgraph.
 */
sealed class LLFirResolveTargetWithDedicatedElement<T : FirElementWithResolveState>(
    firFile: FirFile,
    classPath: List<FirRegularClass>,
    val target: T,
) : LLFirResolveTarget(
    firFile = firFile,
    path = pathWithScript(firFile, classPath, target),
)

private fun pathWithScript(firFile: FirFile, path: List<FirRegularClass>, target: FirElementWithResolveState): List<FirDeclaration> {
    if (target is FirFile || target is FirFileAnnotationsContainer || target is FirScript) return path
    val firScript = firFile.declarations.singleOrNull() as? FirScript ?: return path
    return listOf(firScript) + path
}
