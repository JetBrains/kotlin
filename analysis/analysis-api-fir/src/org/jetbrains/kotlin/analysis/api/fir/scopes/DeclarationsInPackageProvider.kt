/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.PsiPackageImpl
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.providers.impl.forEachNonKotlinPsiElementFinder
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm

internal object DeclarationsInPackageProvider {
    internal fun getTopLevelClassifierNamesInPackageProvider(packageFqName: FqName, analysisSession: KtFirAnalysisSession): Set<Name> {
        return buildSet {
            addAll(analysisSession.useSiteScopeDeclarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName))

            when {
                analysisSession.targetPlatform.isJvm() -> {
                    val psiPackage = PsiPackageImpl(PsiManager.getInstance(analysisSession.project), packageFqName.asString())
                    forEachNonKotlinPsiElementFinder(analysisSession.project) { finder ->
                        finder.getClassNames(psiPackage, analysisSession.useSiteAnalysisScope)
                            .mapNotNullTo(this, Name::identifier)
                    }
                }
            }

            addAll(collectGeneratedTopLevelClassifiers(packageFqName, analysisSession.useSiteSession))
        }
    }

    internal fun getTopLevelCallableNamesInPackageProvider(packageFqName: FqName, analysisSession: KtFirAnalysisSession): Set<Name> {
        return buildSet {
            addAll(analysisSession.useSiteScopeDeclarationProvider.getTopLevelCallableNamesInPackage(packageFqName))
            addAll(collectGeneratedTopLevelCallables(packageFqName, analysisSession.useSiteSession))
        }
    }


    private fun collectGeneratedTopLevelClassifiers(packageFqName: FqName, session: FirSession): Set<Name> {
        val declarationGenerators = session.extensionService.declarationGenerators

        val generatedTopLevelClassifiers = declarationGenerators
            .asSequence()
            .flatMap {
                // FIXME this function should be called only once during plugin's lifetime, so this usage is not really correct (2)
                it.getTopLevelClassIds()
            }
            .filter { it.packageFqName == packageFqName }
            .map { it.shortClassName }

        return generatedTopLevelClassifiers.toSet()
    }

    private fun collectGeneratedTopLevelCallables(packageFqName: FqName, session: FirSession): Set<Name> {
        val generators = session.extensionService.declarationGenerators

        val generatedTopLevelDeclarations = generators
            .asSequence()
            .flatMap {
                // FIXME this function should be called only once during plugin's lifetime, so this usage is not really correct (1)
                it.getTopLevelCallableIds()
            }
            .filter { it.packageName == packageFqName }
            .map { it.callableName }

        return generatedTopLevelDeclarations.toSet()
    }
}