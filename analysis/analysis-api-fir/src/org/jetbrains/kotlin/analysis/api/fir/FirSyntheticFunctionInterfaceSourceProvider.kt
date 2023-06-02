/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtClassOrObject

//todo introduce LibraryModificationTracker based cache?
internal object FirSyntheticFunctionInterfaceSourceProvider {
    fun findPsi(fir: FirDeclaration): PsiElement? {
        return when (fir) {
            is FirSimpleFunction -> provideSourceForInvokeFunction(fir)
            is FirClass -> provideSourceForFunctionClass(fir)
            else -> null
        }
    }

    private fun provideSourceForInvokeFunction(function: FirSimpleFunction): PsiElement? {
        val classId = function.containingClassLookupTag()?.classId ?: return null
        val classOrObject = classByClassId(classId, function.llFirSession.ktModule) ?: return null
        return classOrObject.declarations.singleOrNull()
    }

    private fun provideSourceForFunctionClass(klass: FirClass): PsiElement? {
        return classByClassId(klass.symbol.classId, klass.llFirSession.ktModule)
    }

    private fun classByClassId(classId: ClassId, ktModule: KtModule): KtClassOrObject? {
        val project = ktModule.project
        val correctedClassId = classIdMapping[classId] ?: return null
        require(ktModule is KtBuiltinsModule) {
            "Expected builtin module but found $ktModule"
        }
        return project.createDeclarationProvider(ProjectScope.getLibrariesScope(project), ktModule)
            .getAllClassesByClassId(correctedClassId)
            .firstOrNull { it.containingKtFile.isCompiled }
    }

    private val classIdMapping = (0..23).associate { i ->
        StandardClassIds.FunctionN(i) to ClassId(FqName("kotlin.jvm.functions"), Name.identifier("Function$i"))
    }
}
