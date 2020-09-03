/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.util.classIdIfNonLocal
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal fun KtDeclaration.findNonLocalFirDeclaration(
    firFileBuilder: FirFileBuilder,
    provider: FirProvider,
    moduleFileCache: ModuleFileCache
): FirDeclaration {
    require(!KtPsiUtil.isLocal(this))
    return when {
        this is KtClassOrObject -> findFir(provider)
        this is KtNamedDeclaration && (this is KtProperty || this is KtNamedFunction) -> {
            val containerClass = containingClassOrObject
            if (containerClass != null) {
                val containerClassFir = containerClass.findFir(provider)
                containerClassFir.declarations.first { it.psi === this }
            } else {
                val ktFile = containingKtFile
                val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache)
                firFile.declarations.first { it.psi === this }
            }
        }
        this is KtConstructor<*> -> {
            val containerClassFir = containingClassOrObject?.findFir(provider)
                ?: error("Container class should be not null for KtConstructor")
            containerClassFir.declarations.first { it.psi === this }
        }
        else -> error("Invalid container $this")
    }
}

private fun KtClassOrObject.findFir(provider: FirProvider): FirRegularClass {
    val containerClassId = classIdIfNonLocal()
        ?: error("Container classId should not be null for non-local declaration")
    return provider.getFirClassifierByFqName(containerClassId) as? FirRegularClass
        ?: error("Could not find class $containerClassId")
}