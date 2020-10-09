/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.util.classIdIfNonLocal
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal fun KtDeclaration.findSourceNonLocalFirDeclaration(
    firFileBuilder: FirFileBuilder,
    firSymbolProvider: FirSymbolProvider,
    moduleFileCache: ModuleFileCache
): FirDeclaration {
    require(!KtPsiUtil.isLocal(this))
    return when {
        this is KtClassOrObject -> findFir(firSymbolProvider)
        this is KtNamedDeclaration && (this is KtProperty || this is KtNamedFunction) -> {
            val containerClass = containingClassOrObject
            val declarations = if (containerClass != null) {
                val containerClassFir = containerClass.findFir(firSymbolProvider)
                containerClassFir.declarations
            } else {
                val ktFile = containingKtFile
                val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache, lazyBodiesMode = true)
                firFile.declarations
            }
            val original = originalDeclaration
            declarations.first { it.psi === this || it.psi == original }
        }
        this is KtConstructor<*> -> {
            val containerClassFir = containingClassOrObject?.findFir(firSymbolProvider)
                ?: error("Container class should be not null for KtConstructor")
            containerClassFir.declarations.first { it.psi === this }
        }
        this is KtTypeAlias -> findFir(firSymbolProvider)
        else -> error("Invalid container $this::class")
    }
}

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<KtDeclaration>("ORIGINAL_DECLARATION_KEY")

var KtDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)


private fun KtClassOrObject.findFir(firSymbolProvider: FirSymbolProvider): FirRegularClass {
    val classId = classIdIfNonLocal()
        ?: error("Container classId should not be null for non-local declaration")
    return executeWithoutPCE {
        firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass
            ?: error("Could not find class $classId")
    }
}

private fun KtTypeAlias.findFir(firSymbolProvider: FirSymbolProvider): FirTypeAlias {
    val typeAlias = ClassId(containingKtFile.packageFqName, nameAsSafeName)
    return executeWithoutPCE {
        firSymbolProvider.getClassLikeSymbolByFqName(typeAlias)?.fir as? FirTypeAlias
            ?: error("Could not find type alias $typeAlias")
    }
}