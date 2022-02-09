/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.KtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction


//todo introduce LibraryModificationTracker based cache?
internal object FirDeserializedDeclarationSourceProvider {
    fun findPsi(fir: FirElement, project: Project): PsiElement? {
        return when (fir) {
            is FirSimpleFunction -> provideSourceForFunction(fir, project)
            is FirProperty -> provideSourceForProperty(fir, project)
            is FirClass -> provideSourceForClass(fir, project)
            is FirTypeAlias -> provideSourceForTypeAlias(fir, project)
            is FirConstructor -> provideSourceForConstructor(fir, project)
            is FirEnumEntry -> provideSourceForEnumEntry(fir, project)
            else -> null
        }
    }

    private fun provideSourceForFunction(
        function: FirSimpleFunction,
        project: Project
    ): PsiElement? {
        val candidates = if (function.isTopLevel) {
            project.createDeclarationProvider(function.scope(project)).getTopLevelFunctions(function.symbol.callableId)
                .filter(KtNamedFunction::isCompiled)
        } else {
            function.containingKtClass(project)?.body?.functions
                ?.filter { it.name == function.name.asString() && it.isCompiled() }
                .orEmpty()
        }

        return function.unwrapFakeOverrides().chooseCorrespondingPsi(candidates)
    }

    private fun provideSourceForProperty(property: FirProperty, project: Project): PsiElement? {
        val candidates = if (property.isTopLevel) {
            project.createDeclarationProvider(property.scope(project)).getTopLevelProperties(property.symbol.callableId)
        } else {
            property.containingKtClass(project)?.declarations
                ?.filter { it.name == property.name.asString() }
                .orEmpty()
        }

        return candidates.firstOrNull(KtElement::isCompiled)
    }

    private fun provideSourceForClass(klass: FirClass, project: Project): PsiElement? =
        classByClassId(klass.symbol.classId, klass.scope(project), project)

    private fun provideSourceForTypeAlias(alias: FirTypeAlias, project: Project): PsiElement? {
        val candidates = project.createDeclarationProvider(alias.scope(project)).getTypeAliasesByClassId(alias.symbol.classId)
        return candidates.firstOrNull(KtElement::isCompiled)
    }

    private fun provideSourceForConstructor(
        constructor: FirConstructor,
        project: Project
    ): PsiElement? {
        val containingKtClass = constructor.containingKtClass(project) ?: return null
        if (constructor.isPrimary) return containingKtClass.primaryConstructor

        return constructor.unwrapFakeOverrides().chooseCorrespondingPsi(containingKtClass.secondaryConstructors)
    }

    private fun FirFunction.chooseCorrespondingPsi(
        candidates: Collection<KtFunction>
    ): KtFunction? {
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

    private fun provideSourceForEnumEntry(
        enumEntry: FirEnumEntry,
        project: Project
    ): PsiElement? {
        val candidates = enumEntry.containingKtClass(project)?.body?.enumEntries
            ?.filter { it.name == enumEntry.name.asString() }
            .orEmpty()

        return candidates.firstOrNull(KtElement::isCompiled)
    }

    private fun FirDeclaration.scope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.allScope(project)
        /* TODO:
         val session = session as? FirLibrarySession
         return session?.scope ?: GlobalSearchScope.allScope(project)*/
    }

    private fun FirCallableDeclaration.containingKtClass(project: Project): KtClassOrObject? =
        unwrapFakeOverrides().containingClass()?.classId?.let { classByClassId(it, scope(project), project) }

    private fun classByClassId(classId: ClassId, scope: GlobalSearchScope, project: Project): KtClassOrObject? {
        val correctedClassId = classIdMapping[classId] ?: classId
        return project.createDeclarationProvider(scope)
            .getClassesByClassId(correctedClassId)
            .firstOrNull(KtElement::isCompiled)
    }

    private val FirCallableDeclaration.isTopLevel
        get() = symbol.callableId.className == null

    private val classIdMapping = (0..23).associate { i ->
        StandardClassIds.FunctionN(i) to ClassId(FqName("kotlin.jvm.functions"), Name.identifier("Function$i"))
    }
}

private fun KtElement.isCompiled(): Boolean = containingKtFile.isCompiled

private val allowedFakeElementKinds = setOf(
    KtFakeSourceElementKind.FromUseSiteTarget,
    KtFakeSourceElementKind.PropertyFromParameter,
    KtFakeSourceElementKind.ItLambdaParameter,
    KtFakeSourceElementKind.DataClassGeneratedMembers,
    KtFakeSourceElementKind.ImplicitConstructor,
)

private fun FirElement.getAllowedPsi() = when (val source = source) {
    null -> null
    is KtRealPsiSourceElement -> source.psi
    is KtFakeSourceElement -> if (source.kind in allowedFakeElementKinds) psi else null
    else -> null
}

fun FirElement.findPsi(project: Project): PsiElement? =
    getAllowedPsi() ?: FirDeserializedDeclarationSourceProvider.findPsi(this, project)


fun FirBasedSymbol<*>.findPsi(): PsiElement? =
    fir.findPsi(fir.moduleData.session)

fun FirElement.findPsi(session: FirSession): PsiElement? =
    findPsi((session as LLFirSession).project)

/**
 * Finds [PsiElement] which will be used as go-to referenced element for [KtPsiReference]
 * For data classes & enums generated members like `copy` `componentN`, `values` it will return corresponding enum/data class
 * Otherwise, behaves the same way as [findPsi] returns exact PSI declaration corresponding to passed [FirDeclaration]
 */
fun FirDeclaration.findReferencePsi(): PsiElement? =
    psi ?: FirDeserializedDeclarationSourceProvider.findPsi(this, (moduleData.session as LLFirSession).project)
