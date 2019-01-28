/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedPackageStarImport
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirImportResolveTransformer() : FirAbstractTreeTransformer() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    private lateinit var symbolProvider: FirSymbolProvider

    constructor(session: FirSession) : this() {
        // TODO: clarify this
        symbolProvider = FirSymbolProvider.getInstance(session)
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        symbolProvider = FirSymbolProvider.getInstance(file.session)
        return file.also { it.transformChildren(this, null) }.compose()
    }

    override fun transformImport(import: FirImport, data: Nothing?): CompositeTransformResult<FirImport> {
        val fqName = import.importedFqName ?: return import.compose()

        if (!fqName.isRoot) {
            val lastPart = StringBuilder()
            var firstPart = fqName

            if (import.isAllUnder && symbolProvider.getPackage(firstPart) != null) {
                return FirResolvedPackageStarImport(import, firstPart).compose()
            }

            while (!firstPart.isRoot) {
                if (lastPart.isNotEmpty())
                    lastPart.insert(0, '.')
                lastPart.insert(0, firstPart.shortName().asString())

                firstPart = firstPart.parent()

                val resolvedFqName = ClassId(firstPart, FqName(lastPart.toString()), false)
                val foundSymbol = symbolProvider.getSymbolByFqName(resolvedFqName)

                if (foundSymbol != null) {
                    return FirResolvedImportImpl(import, resolvedFqName).compose()
                }
            }
        }
        return import.compose()
    }
}