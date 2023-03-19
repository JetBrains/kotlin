/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.KtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
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
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

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

    private fun provideSourceForFunction(
        function: FirSimpleFunction,
        project: Project
    ): PsiElement? {
        val baseFunction = function.delegatedWrapperData?.wrapped ?: function
        val candidates = if (function.isTopLevel) {
            project.createDeclarationProvider(function.scope(project)).getTopLevelFunctions(function.symbol.callableId)
                .filter(KtNamedFunction::isCompiled)
        } else {
            baseFunction.containingKtClass(project)?.body?.functions
                ?.filter { it.name == baseFunction.name.asString() && it.isCompiled() }
                .orEmpty()
        }

        return baseFunction.unwrapFakeOverrides().chooseCorrespondingPsi(candidates)
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
        val candidates = project.createDeclarationProvider(alias.scope(project)).getAllTypeAliasesByClassId(alias.symbol.classId)
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
        val original = when (this) {
            is FirCallableDeclaration -> unwrapFakeOverrides()
            else -> this
        }
        val moduleData = original.llFirModuleData
        return when (val ktModule = moduleData.ktModule) {
            is KtBuiltinsModule -> GlobalSearchScope.allScope(project) // TODO should be some stdlib
            else -> ktModule.contentScope
        }
    }

    private fun FirCallableDeclaration.containingKtClass(project: Project): KtClassOrObject? =
        unwrapFakeOverrides().containingClassLookupTag()?.classId?.let { classByClassId(it, scope(project), project) }

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
