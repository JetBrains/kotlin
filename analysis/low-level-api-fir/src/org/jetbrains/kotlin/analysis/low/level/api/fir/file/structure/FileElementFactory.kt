/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers

internal object FileElementFactory {
    fun createFileStructureElement(
        firDeclaration: FirDeclaration,
        firFile: FirFile,
        moduleComponents: LLFirModuleResolveComponents,
    ): FileStructureElement = when (firDeclaration) {
        is FirRegularClass -> {
            firDeclaration.lazyResolveToPhaseWithCallableMembers(FirResolvePhase.BODY_RESOLVE)
            ClassDeclarationStructureElement(firFile, firDeclaration, moduleComponents)
        }
        is FirScript -> RootScriptStructureElement(firFile, firDeclaration, moduleComponents)
        else -> DeclarationStructureElement(firFile, firDeclaration, moduleComponents)
    }
}
