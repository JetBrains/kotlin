/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtClassOrObject

//todo introduce LibraryModificationTracker based cache?
internal object FirSyntheticFunctionInterfaceSourceProvider {
    fun findPsi(fir: FirDeclaration, scope: GlobalSearchScope): PsiElement? {
        return when (fir) {
            is FirSimpleFunction -> provideSourceForInvokeFunction(fir, scope)
            is FirClass -> provideSourceForFunctionClass(fir, scope)
            else -> null
        }
    }

    private fun provideSourceForInvokeFunction(function: FirSimpleFunction, scope: GlobalSearchScope): PsiElement? {
        val classId = function.containingClassLookupTag()?.classId ?: return null
        val classOrObject = classByClassId(classId, scope, function.llFirSession.ktModule) ?: return null
        return classOrObject.declarations.singleOrNull()
    }

    private fun provideSourceForFunctionClass(klass: FirClass, scope: GlobalSearchScope): PsiElement? {
        return classByClassId(klass.symbol.classId, scope, klass.llFirSession.ktModule)
    }

    private fun classByClassId(classId: ClassId, scope: GlobalSearchScope, ktModule: KaModule): KtClassOrObject? {
        val project = ktModule.project
        val correctedClassId = classIdMapping[classId] ?: return null
        require(ktModule is KaBuiltinsModule) {
            "Expected builtin module but found $ktModule"
        }
        return project.createDeclarationProvider(scope, ktModule)
            .getAllClassesByClassId(correctedClassId)
            .firstOrNull { it.containingKtFile.isCompiled }
    }

    private val classIdMapping = (0..23).associate { i ->
        StandardClassIds.FunctionN(i) to ClassId(FqName("kotlin.jvm.functions"), Name.identifier("Function$i"))
    }
}
