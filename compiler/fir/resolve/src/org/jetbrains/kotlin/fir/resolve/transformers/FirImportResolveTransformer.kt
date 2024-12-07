/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.FqName

class FirImportResolveProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(
    session, scopeSession, FirResolvePhase.IMPORTS
) {
    override val transformer: FirImportResolveTransformer = FirImportResolveTransformer(session)
}

open class FirImportResolveTransformer protected constructor(
    final override val session: FirSession,
    phase: FirResolvePhase
) : FirAbstractTreeTransformer<Any?>(phase) {
    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    constructor(session: FirSession) : this(session, FirResolvePhase.IMPORTS)

    private val symbolProvider: FirSymbolProvider = session.symbolProvider

    private var currentFile: FirFile? = null

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        withFileAnalysisExceptionWrapping(file) {
            val prevValue = currentFile
            currentFile = file
            try {
                file.transformChildren(this, null)
            } finally {
                currentFile = prevValue
            }
        }
        return file
    }

    override fun transformImport(import: FirImport, data: Any?): FirImport {
        val fqName = import.importedFqName?.takeUnless { it.isRoot } ?: return import

        if (!fqName.isAcceptable) return import

        if (import.isAllUnder) {
            return transformImportForFqName(fqName, import)
        }

        currentFile?.let {
            session.lookupTracker?.recordFqNameLookup(fqName, import.source, it.source)
        }
        return transformImportForFqName(fqName.parent(), import)
    }

    protected open val FqName.isAcceptable: Boolean
        get() = true

    private fun transformImportForFqName(fqName: FqName, delegate: FirImport): FirImport {
        val (packageFqName, relativeClassFqName) = findLongestExistingPackage(symbolProvider, fqName)

        return buildResolvedImport {
            this.delegate = delegate
            this.packageFqName = packageFqName
            this.relativeParentClassName = relativeClassFqName
        }
    }
}
