/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.KtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.delegatedWrapperData
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker

//todo introduce LibraryModificationTracker based cache?
internal object FirDeserializedDeclarationSourceProvider {
    fun findPsi(fir: FirElement, project: Project): PsiElement? {
        return when (fir) {
            is FirSimpleFunction -> provideSourceForFunction(fir.delegatedWrapperData?.wrapped ?: fir, project)
            is FirProperty -> provideSourceForProperty(fir, project)
            is FirClass -> provideSourceForClass(fir, project)
            is FirTypeAlias -> provideSourceForTypeAlias(fir, project)
            is FirConstructor -> provideSourceForConstructor(fir, project)
            is FirEnumEntry -> provideSourceForEnumEntry(fir, project)
            else -> null
        }
    }

    fun findClassPsiForGeneratedMembers(fir: FirElement, project: Project): PsiElement? {
        if (fir !is FirCallableDeclaration) {
            return null
        }
        val containingClass = fir.getContainingClass(fir.moduleData.session) ?: return null
        if (isGeneratedMemberNotWrittenToMetadata(fir.symbol, containingClass)) {
            return provideSourceForClass(containingClass, project)
        }
        return null
    }

    private fun isGeneratedMemberNotWrittenToMetadata(symbol: FirCallableSymbol<*>, containingClass: FirRegularClass): Boolean {
        return when {
            containingClass.isEnumClass -> symbol.name in setOf(
                StandardNames.ENUM_VALUES,
                StandardNames.ENUM_VALUE_OF,
                StandardNames.ENUM_ENTRIES,
            )
            containingClass.isData -> symbol.name in setOf(
                OperatorNameConventions.EQUALS,
                OperatorNameConventions.TO_STRING,
                StandardNames.HASHCODE_NAME,
                StandardNames.DATA_CLASS_COPY,
            )
            else -> false
        }
    }

    private fun provideSourceForFunction(function: FirSimpleFunction, project: Project): PsiElement? {
        return provideSourceForCallable(
            function,
            project,
            topLevelSearcher = { declarationProvider -> declarationProvider.getTopLevelFunctions(function.symbol.callableId) },
            predicate = { it is KtFunction && it.name == function.name.asString() }
        )
    }

    private fun provideSourceForProperty(property: FirProperty, project: Project): PsiElement? {
        return provideSourceForCallable(
            property,
            project,
            topLevelSearcher = { declarationProvider -> declarationProvider.getTopLevelProperties(property.symbol.callableId) },
            predicate = { it is KtProperty && it.name == property.name.asString() },
        )
    }

    private fun <T : KtCallableDeclaration> provideSourceForCallable(
        callable: FirCallableDeclaration,
        project: Project,
        topLevelSearcher: (KotlinDeclarationProvider) -> Collection<T>,
        predicate: (KtCallableDeclaration) -> Boolean
    ): PsiElement? {
        fun chooseCandidate(candidates: Collection<KtCallableDeclaration>): PsiElement? {
            return callable.unwrapFakeOverrides().chooseCorrespondingPsi(candidates)
        }

        if (callable.isTopLevel) {
            return getTargetScopes(callable, project)
                .asSequence()
                .map { scope -> topLevelSearcher(project.createDeclarationProvider(scope)).filter(KtElement::isCompiled) }
                .filter { it.isNotEmpty() }
                .firstNotNullOfOrNull { chooseCandidate(it) }
        }

        val containingKtClass = getContainingKtClassOrObject(callable, getTargetScopes(callable, project), project)
        val containingKtFile = containingKtClass?.containingKtFile

        if (containingKtFile == null || !containingKtFile.isCompiled) {
            return null
        }

        val candidates = containingKtClass.declarations.filterIsInstanceWithChecker<KtCallableDeclaration>(predicate)
        return chooseCandidate(candidates)
    }

    private fun provideSourceForClass(klass: FirClass, project: Project): PsiElement? {
        return getTargetScopes(klass, project)
            .asSequence()
            .mapNotNull { scope -> classByClassId(klass.symbol.classId, scope, project) }
            .filter { it.isCompiled() }
            .firstOrNull()
    }

    private fun provideSourceForTypeAlias(typeAlias: FirTypeAlias, project: Project): PsiElement? {
        return getTargetScopes(typeAlias, project)
            .asSequence()
            .flatMap { scope -> project.createDeclarationProvider(scope).getAllTypeAliasesByClassId(typeAlias.symbol.classId) }
            .firstOrNull { it.isCompiled() }
    }

    private fun provideSourceForConstructor(constructor: FirConstructor, project: Project): PsiElement? {
        val scopes = getTargetScopes(constructor, project)
        val containingKtClass = getContainingKtClassOrObject(constructor, scopes, project) ?: return null

        return if (constructor.isPrimary) {
            containingKtClass.primaryConstructor
        } else {
            constructor.unwrapFakeOverrides()
                .chooseCorrespondingPsi(containingKtClass.secondaryConstructors)
        }
    }

    private fun FirCallableDeclaration.chooseCorrespondingPsi(candidates: Collection<KtCallableDeclaration>): KtCallableDeclaration? {
        if (candidates.isEmpty()) return null
        for (candidate in candidates) {
            assert(candidate.isCompiled()) {
                "Candidate should be decompiled from metadata because it should have fqName types as we don't use resolve here"
            }
            if (KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(candidate, this)) {
                return candidate
            }
        }
        return null
    }

    private fun provideSourceForEnumEntry(enumEntry: FirEnumEntry, project: Project): PsiElement? {
        val scopes = getTargetScopes(enumEntry, project)
        val containingKtClass = getContainingKtClassOrObject(enumEntry, scopes, project) ?: return null

        if (containingKtClass.isCompiled()) {
            return containingKtClass.body?.enumEntries?.firstOrNull { it.name == enumEntry.name.asString() }
        }

        return null
    }

    private fun getTargetScopes(declaration: FirDeclaration, project: Project): List<GlobalSearchScope> {
        val originalDeclaration = when (declaration) {
            is FirCallableDeclaration -> declaration.unwrapFakeOverrides()
            else -> declaration
        }

        val moduleData = originalDeclaration.llFirModuleData
        val module = moduleData.ktModule

        return buildList {
            add(module.contentScope)

            when (module) {
                is KtBuiltinsModule -> {
                    add(ProjectScope.getLibrariesScope(project))
                    add(ProjectScope.getContentScope(project))
                }
                is KtLibrarySourceModule, is KtLibraryModule -> {
                    // Search in other libraries (that might be dependencies of this library)
                    add(ProjectScope.getLibrariesScope(project))
                }
                else -> {}
            }
        }
    }

    private fun getContainingKtClassOrObject(
        firCallable: FirCallableDeclaration,
        scopes: List<GlobalSearchScope>,
        project: Project
    ): KtClassOrObject? {
        val classId = firCallable.unwrapFakeOverrides().containingClassLookupTag()?.classId ?: return null
        return scopes.firstNotNullOfOrNull { scope -> classByClassId(classId, scope, project) }
    }

    private fun classByClassId(classId: ClassId, scope: GlobalSearchScope, project: Project): KtClassOrObject? {
        val correctedClassId = classIdMapping[classId] ?: classId
        return project.createDeclarationProvider(scope)
            .getAllClassesByClassId(correctedClassId)
            .firstOrNull(KtElement::isCompiled)
    }

    private val FirCallableDeclaration.isTopLevel
        get() = symbol.callableId.className == null

    private val classIdMapping = (0..23).associate { i ->
        StandardClassIds.FunctionN(i) to ClassId(FqName("kotlin.jvm.functions"), Name.identifier("Function$i"))
    }
}
