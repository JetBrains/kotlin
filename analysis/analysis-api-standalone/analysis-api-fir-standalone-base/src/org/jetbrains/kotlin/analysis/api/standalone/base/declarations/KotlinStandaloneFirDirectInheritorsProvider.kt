/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.fir.utils.isSubclassOf
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.contains
import kotlin.collections.filter

@OptIn(LLFirInternals::class, SymbolInternals::class)
internal class KotlinStandaloneFirDirectInheritorsProvider(private val project: Project) : KotlinDirectInheritorsProvider {
    private val standaloneDeclarationProviderFactory by lazy {
        KotlinDeclarationProviderFactory.getInstance(project) as? KotlinStandaloneDeclarationProviderFactory
            ?: error(
                "`${KotlinStandaloneFirDirectInheritorsProvider::class.simpleName}` expects the following declaration provider factory to be" +
                        " registered: `${KotlinStandaloneDeclarationProviderFactory::class.simpleName}`"
            )
    }

    override fun getDirectKotlinInheritors(
        ktClass: KtClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean,
    ): Iterable<KtClassOrObject> {
        val classId = ktClass.getClassId() ?: return emptyList()
        val baseModule = KotlinProjectStructureProvider.getModule(project, ktClass, useSiteModule = null)
        val baseFirClass = classId.toFirSymbol(baseModule)?.fir as? FirClass ?: return emptyList()

        val baseClassNames = mutableSetOf(classId.shortClassName)
        calculateAliases(classId.shortClassName, baseClassNames)

        val possibleInheritors = baseClassNames.flatMap { standaloneDeclarationProviderFactory.getDirectInheritorCandidates(it) }
        if (possibleInheritors.isEmpty()) {
            return emptyList()
        }
        return possibleInheritors.filter { isValidInheritor(it, baseFirClass, scope, includeLocalInheritors) }
    }

    private fun calculateAliases(aliasedName: Name, aliases: MutableSet<Name>) {
        standaloneDeclarationProviderFactory.getInheritableTypeAliases(aliasedName).forEach { alias ->
            val aliasName = alias.nameAsSafeName
            val isNewAliasName = aliases.add(aliasName)
            if (isNewAliasName) {
                calculateAliases(aliasName, aliases)
            }
        }
    }

    @OptIn(KaImplementationDetail::class)
    private fun isValidInheritor(
        candidate: KtClassOrObject,
        baseFirClass: FirClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean,
    ): Boolean {
        if (!includeLocalInheritors && candidate.isLocal) {
            return false
        }

        if (!scope.contains(candidate)) {
            return false
        }

        val candidateClassId = candidate.getClassId() ?: return false
        val candidateModule = KotlinProjectStructureProvider.getModule(project, candidate, useSiteModule = null)
        val candidateFirSymbol = candidateClassId.toFirSymbol(candidateModule) ?: return false
        val candidateFirClass = candidateFirSymbol.fir as? FirClass ?: return false

        // `KotlinDirectInheritorsProvider`'s interface guarantees that `getDirectKotlinInheritors` is only called from lazy resolution to
        // `SEALED_CLASS_INHERITORS` or later, so `isSubClassOf` resolving to `SUPER_TYPES` is legal.
        return isSubclassOf(candidateFirClass, baseFirClass, candidateFirClass.moduleData.session, allowIndirectSubtyping = false)
    }

    private fun ClassId.toFirSymbol(module: KaModule): FirClassLikeSymbol<*>? {
        // Using a resolve session/source-preferred session will cause class stubs from binary libraries to be AST-loaded in IDE mode tests,
        // which results in an exception since we don't have a decompiler for them. See KT-64898, KT-64899, and KT-64900. If not for these
        // issues, we would be able to use `analyze` instead of custom session logic.
        val session = LLFirSessionCache.getInstance(project).getSession(module, preferBinary = true)
        return session.symbolProvider.getClassLikeSymbolByClassId(this)
    }
}
