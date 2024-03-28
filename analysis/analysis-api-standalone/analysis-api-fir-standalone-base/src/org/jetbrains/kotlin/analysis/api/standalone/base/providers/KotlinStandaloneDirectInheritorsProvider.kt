/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.utils.isSubClassOf
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
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
internal class KotlinStandaloneDirectInheritorsProvider(private val project: Project) : KotlinDirectInheritorsProvider {
    private val staticDeclarationProviderFactory by lazy {
        KotlinDeclarationProviderFactory.getInstance(project) as? KotlinStaticDeclarationProviderFactory
            ?: error(
                "`${KotlinStandaloneDirectInheritorsProvider::class.simpleName}` expects the following declaration provider factory to be" +
                        " registered: `${KotlinStaticDeclarationProviderFactory::class.simpleName}`"
            )
    }

    override fun getDirectKotlinInheritors(
        ktClass: KtClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean,
    ): Iterable<KtClassOrObject> {
        val classId = ktClass.getClassId() ?: return emptyList()

        val aliases = mutableSetOf(classId.shortClassName)
        calculateAliases(classId.shortClassName, aliases)

        val possibleInheritors = aliases.flatMap { staticDeclarationProviderFactory.getDirectInheritorCandidates(it) }

        if (possibleInheritors.isEmpty()) {
            return emptyList()
        }

        // The index provides candidates from an original module, not dangling files. If we resolve the supertypes of a candidate in the
        // context of its session, we will resolve to FIR classes from non-dangling, original modules. If `ktClass` is inside a dangling
        // file, the FIR class for `ktClass` will come from the dangling module. So we'd compare the original FIR class for the supertype
        // with the dangling FIR class for `ktClass`, resulting in a mismatch. To avoid such incompatible comparisons, we need to resolve
        // `ktClass` to the original FIR class.
        //
        // Note that this means we don't support providing inheritors based on the dangling file yet, for example if an inheritor was added
        // or removed only in the dangling file.
        val baseKtModule = when (val ktModule = ProjectStructureProvider.getModule(project, ktClass, contextualModule = null)) {
            is KtDanglingFileModule -> ktModule.contextModule
            else -> ktModule
        }

        val baseFirClass = ktClass.toFirSymbol(classId, baseKtModule)?.fir as? FirClass ?: return emptyList()
        return possibleInheritors.filter { isValidInheritor(it, baseFirClass, scope, includeLocalInheritors) }
    }

    private fun calculateAliases(aliasedName: Name, aliases: MutableSet<Name>) {
        staticDeclarationProviderFactory.getInheritableTypeAliases(aliasedName).forEach { alias ->
            val aliasName = alias.nameAsSafeName
            val isNewAliasName = aliases.add(aliasName)
            if (isNewAliasName) {
                calculateAliases(aliasName, aliases)
            }
        }
    }

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
        val candidateKtModule = ProjectStructureProvider.getModule(project, candidate, contextualModule = null)
        val candidateFirSymbol = candidate.toFirSymbol(candidateClassId, candidateKtModule) ?: return false
        val candidateFirClass = candidateFirSymbol.fir as? FirClass ?: return false

        // `KotlinDirectInheritorsProvider`'s interface guarantees that `getDirectKotlinInheritors` is only called from lazy resolution to
        // `SEALED_CLASS_INHERITORS` or later, so `isSubClassOf` resolving to `SUPER_TYPES` is legal.
        return isSubClassOf(candidateFirClass, baseFirClass, candidateFirClass.moduleData.session, allowIndirectSubtyping = false)
    }

    private fun KtClassOrObject.toFirSymbol(classId: ClassId, ktModule: KtModule): FirClassLikeSymbol<*>? {
        // Using a resolve session/source-preferred session will cause class stubs from binary libraries to be AST-loaded in IDE mode tests,
        // which results in an exception since we don't have a decompiler for them. See KT-64898, KT-64899, and KT-64900. If not for these
        // issues, we would be able to use `analyze` instead of custom session logic.
        val session = LLFirSessionCache.getInstance(project).getSession(ktModule, preferBinary = true)
        return session.symbolProvider.getClassLikeSymbolByClassId(classId)
    }
}
