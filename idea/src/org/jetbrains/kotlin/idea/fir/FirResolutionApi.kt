/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.getCallableSymbols
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

enum class FirStage(val index: Int, val stubMode: Boolean) {
    // Just build FIR without resolve and without function bodies
    BUILD_ONLY(index = 0, stubMode = true) {
        override val transformers: List<FirTransformer<Nothing?>>
            get() = emptyList()
    },
    // Build FIR for declarations without function bodies, resolve imports, explicit types, visibility & modality
    DECLARATIONS(index = 3, stubMode = true) {
        override val transformers: List<FirTransformer<Nothing?>>
            get() = listOf(
                FirImportResolveTransformer(),
                FirTypeResolveTransformer(),
                FirStatusResolveTransformer()
            )
    };

    abstract val transformers: List<FirTransformer<Nothing?>>
}

private fun KtClassOrObject.relativeFqName(): FqName {
    val className = this.nameAsSafeName
    val parentFqName = this.containingClassOrObject?.relativeFqName()
    return parentFqName?.child(className) ?: FqName.topLevel(className)
}

val KtElement.session: FirSession
    get() {
        val moduleInfo = this.getModuleInfo() as ModuleSourceInfo
        val sessionProvider = FirProjectSessionProvider(project)
        return sessionProvider.getSession(moduleInfo) ?: FirJavaModuleBasedSession(
            moduleInfo, sessionProvider, moduleInfo.contentScope(),
            IdeFirDependenciesSymbolProvider(moduleInfo, project, sessionProvider)
        )
    }

fun KtCallableDeclaration.getOrBuildFir(stage: FirStage = FirStage.DECLARATIONS): FirCallableMemberDeclaration {
    val session = this.session

    val file = this.containingKtFile
    val packageFqName = file.packageFqName
    val klassFqName = this.containingClassOrObject?.relativeFqName()
    val declName = this.nameAsSafeName
    val callableId = CallableId(packageFqName, klassFqName, declName)

    val firProvider = FirProvider.getInstance(session) as FirProviderImpl
    // TODO: minor file modifications should not force full rebuild (!)
    val cachedFirFile = firProvider.getFirFilesByPackage(packageFqName).find { it.psi == file }
    // TODO: stage of cached FIR should be taken into account (!)
    if (cachedFirFile == null) {
        println("FIR resolution: start transformation of ${file.name}")
        val builder = RawFirBuilder(session, stubMode = stage.stubMode)
        val firFile = builder.buildFirFile(file)
        firProvider.recordFile(firFile)

        for (transformer in stage.transformers) {
            transformer.transformFile(firFile, null)
        }
    }

    return firProvider.getCallableSymbols(callableId).mapNotNull {
        (it as? FirBasedSymbol<*>)?.fir as? FirCallableMemberDeclaration
    }.find { it.psi == this }!!
}

val FirTypedDeclaration.coneTypeSafe: ConeKotlinType? get() = (this.returnTypeRef as? FirResolvedTypeRef)?.type