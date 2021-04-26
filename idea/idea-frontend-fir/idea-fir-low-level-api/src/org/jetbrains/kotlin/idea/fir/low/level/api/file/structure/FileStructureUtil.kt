/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.replaceFirst
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal object FileStructureUtil {
    fun isStructureElementContainer(ktDeclaration: KtDeclaration): Boolean = when {
        ktDeclaration !is KtClassOrObject && ktDeclaration !is KtDeclarationWithBody && ktDeclaration !is KtProperty && ktDeclaration !is KtTypeAlias -> false
        ktDeclaration is KtEnumEntry -> false
        ktDeclaration.containingClassOrObject is KtEnumEntry -> false
        else -> !KtPsiUtil.isLocal(ktDeclaration)
    }

    fun replaceDeclaration(firFile: FirFile, from: FirCallableDeclaration<*>, to: FirCallableDeclaration<*>) {
        val declarations = if (from.symbol.callableId.className == null) {
            firFile.declarations as MutableList<FirDeclaration>
        } else {
            val classLikeLookupTag = from.containingClass()
                ?: error("Class name should not be null for non-top-level & non-local declarations, but was null for\n${from.render()}")
            val containingClass = classLikeLookupTag.toSymbol(firFile.declarationSiteSession)?.fir as FirRegularClass
            containingClass.declarations as MutableList<FirDeclaration>
        }
        declarations.replaceFirst(from, to)
    }

    inline fun <R> withDeclarationReplaced(
        firFile: FirFile,
        cache: ModuleFileCache,
        from: FirCallableDeclaration<*>,
        to: FirCallableDeclaration<*>,
        action: () -> R,
    ): R {
        cache.firFileLockProvider.withWriteLock(firFile) { replaceDeclaration(firFile, from, to) }
        return try {
            action()
        } catch (e: Throwable) {
            cache.firFileLockProvider.withWriteLock(firFile) { replaceDeclaration(firFile, to, from) }
            throw e
        }
    }
}
