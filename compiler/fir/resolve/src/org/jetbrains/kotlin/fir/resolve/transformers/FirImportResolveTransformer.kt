/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildErrorImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.lookupTracker
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeImportFromSingleton
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedParentInImport
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirImportResolveProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirImportResolveTransformer(session)
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
        return file.also {
            val prevValue = currentFile
            currentFile = file
            try {
                it.transformChildren(this, null)
            } finally {
                currentFile = prevValue
            }
        }
    }

    override fun transformImport(import: FirImport, data: Any?): FirImport {
        val fqName = import.importedFqName?.takeUnless { it.isRoot } ?: return import

        if (!fqName.isAcceptable) return import

        if (import.isAllUnder) {
            return transformImportForFqName(fqName, import)
        }

        val parentFqName = fqName.parent()
        currentFile?.let {
            session.lookupTracker?.recordLookup(fqName.shortName(), parentFqName.asString(), import.source, it.source)
        }
        return transformImportForFqName(parentFqName, import)
    }

    protected open val FqName.isAcceptable: Boolean
        get() = true

    private fun transformImportForFqName(fqName: FqName, delegate: FirImport): FirImport {
        val (packageFqName, relativeClassFqName, classSymbol) = when (val result = resolveToPackageOrClass(symbolProvider, fqName)) {
            is PackageResolutionResult.Error -> return buildErrorImport {
                this.delegate = delegate
                this.diagnostic = result.diagnostic
            }
            is PackageResolutionResult.PackageOrClass -> result
        }
        val firClass = classSymbol?.fir as? FirRegularClass
        if (delegate.isAllUnder && firClass?.classKind?.isSingleton == true) {
            return buildErrorImport {
                this.delegate = delegate
                this.diagnostic = ConeImportFromSingleton(firClass.name)
            }
        }
        return buildResolvedImport {
            this.delegate = delegate
            this.packageFqName = packageFqName
            this.relativeParentClassName = relativeClassFqName
        }
    }
}

fun resolveToPackageOrClass(symbolProvider: FirSymbolProvider, fqName: FqName): PackageResolutionResult {
    var currentPackage = fqName

    val pathSegments = fqName.pathSegments()
    var prefixSize = pathSegments.size
    while (!currentPackage.isRoot && prefixSize > 0) {
        if (symbolProvider.getPackage(currentPackage) != null) {
            break
        }
        currentPackage = currentPackage.parent()
        prefixSize--
    }

    if (currentPackage == fqName) return PackageResolutionResult.PackageOrClass(currentPackage, null, null)
    val relativeClassFqName = FqName.fromSegments((prefixSize until pathSegments.size).map { pathSegments[it].asString() })

    val classId = ClassId(currentPackage, relativeClassFqName, false)
    val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return PackageResolutionResult.Error(
        ConeUnresolvedParentInImport(classId)
    )

    return PackageResolutionResult.PackageOrClass(currentPackage, relativeClassFqName, symbol)
}

sealed class PackageResolutionResult {
    data class PackageOrClass(
        val packageFqName: FqName, val relativeClassFqName: FqName?, val classSymbol: FirClassLikeSymbol<*>?
    ) : PackageResolutionResult()

    class Error(val diagnostic: ConeDiagnostic) : PackageResolutionResult()
}
