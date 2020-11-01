/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction

internal object FileElementFactory {
    /**
     * should be consistent with [isReanalyzableContainer]
     */
    fun createFileStructureElement(
        firDeclaration: FirDeclaration,
        ktDeclaration: KtDeclaration,
        firFile: FirFile,
    ): FileStructureElement = when {
        ktDeclaration is KtNamedFunction && ktDeclaration.name != null && ktDeclaration.hasExplicitTypeOrUnit ->
            IncrementallyReanalyzableFunction(
                firFile,
                ktDeclaration,
                (firDeclaration as FirSimpleFunction).symbol,
                ktDeclaration.modificationStamp
            )

        else -> NonLocalDeclarationFileStructureElement(
            firFile,
            firDeclaration,
            ktDeclaration,
        )
    }

    /**
     * should be consistent with [createFileStructureElement]
     */
    fun isReanalyzableContainer(
        ktDeclaration: KtDeclaration,
    ): Boolean = when {
        ktDeclaration is KtNamedFunction && ktDeclaration.name != null && ktDeclaration.hasExplicitTypeOrUnit -> true
        else -> false
    }

    val KtNamedFunction.hasExplicitTypeOrUnit
        get() = hasBlockBody() || typeReference != null
}