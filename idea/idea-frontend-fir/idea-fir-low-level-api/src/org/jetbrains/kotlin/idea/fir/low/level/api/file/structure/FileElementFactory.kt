/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.LockProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

internal object FileElementFactory {
    /**
     * should be consistent with [isReanalyzableContainer]
     */
    fun createFileStructureElement(
        firDeclaration: FirDeclaration,
        ktDeclaration: KtDeclaration,
        firFile: FirFile,
        firFileLockProvider: LockProvider<FirFile>,
    ): FileStructureElement = when {
        ktDeclaration is KtNamedFunction && ktDeclaration.isReanalyzableContainer() -> ReanalyzableFunctionStructureElement(
            firFile,
            ktDeclaration,
            (firDeclaration as FirSimpleFunction).symbol,
            ktDeclaration.modificationStamp,
            firFileLockProvider,
        )

        ktDeclaration is KtProperty && ktDeclaration.isReanalyzableContainer() -> ReanalyzablePropertyStructureElement(
            firFile,
            ktDeclaration,
            (firDeclaration as FirProperty).symbol,
            ktDeclaration.modificationStamp,
            firFileLockProvider,
        )

        else -> NonReanalyzableDeclarationStructureElement(
            firFile,
            firDeclaration,
            ktDeclaration,
            firFileLockProvider,
        )
    }
}

/**
 * should be consistent with [createFileStructureElement]
 */
//TODO make internal
fun isReanalyzableContainer(
    ktDeclaration: KtDeclaration,
): Boolean = when (ktDeclaration) {
    is KtNamedFunction -> ktDeclaration.isReanalyzableContainer()
    is KtProperty -> ktDeclaration.isReanalyzableContainer()
    else -> false
}

private fun KtNamedFunction.isReanalyzableContainer() =
    name != null && hasExplicitTypeOrUnit

private fun KtProperty.isReanalyzableContainer() =
    name != null && typeReference != null

private val KtNamedFunction.hasExplicitTypeOrUnit
    get() = hasBlockBody() || typeReference != null