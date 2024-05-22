/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.providers.PsiDeclarationAndKtSymbolEqualityChecker.representsTheSameDeclaration

object DecompiledPsiDeclarationProvider {
    fun KaSession.findPsi(ktSymbol: KaSymbol, project: Project): PsiElement? {
        return when (ktSymbol) {
            is KaConstructorSymbol -> providePsiForConstructor(ktSymbol, project)
            is KaFunctionLikeSymbol -> providePsiForFunction(ktSymbol, project)
            is KaEnumEntrySymbol -> providePsiForEnumEntry(ktSymbol, project)
            is KaVariableLikeSymbol -> providePsiForProperty(ktSymbol, project)
            is KaClassLikeSymbol -> providePsiForClass(ktSymbol, project)
            else -> null
        }
    }

    private fun KaSession.providePsiForConstructor(
        constructorSymbol: KaConstructorSymbol,
        project: Project
    ): PsiElement? {
        val classId = constructorSymbol.containingClassId ?: return null
        val psiClass = project.createPsiDeclarationProvider(constructorSymbol.scope(project))
            ?.getClassesByClassId(classId)
            ?.firstOrNull() ?: return null
        return psiClass.constructors.find { psiMethod ->
            representsTheSameDeclaration(psiMethod, constructorSymbol)
        }
    }

    private fun KaSession.providePsiForFunction(
        functionLikeSymbol: KaFunctionLikeSymbol,
        project: Project
    ): PsiElement? {
        return functionLikeSymbol.callableId?.let {
            val candidates = project.createPsiDeclarationProvider(functionLikeSymbol.scope(project))
                ?.getFunctions(it)
            if (candidates?.size == 1)
                candidates.single()
            else
                candidates?.find { psiMethod ->
                    representsTheSameDeclaration(psiMethod, functionLikeSymbol)
                }
        }
    }

    private fun providePsiForProperty(
        variableLikeSymbol: KaVariableLikeSymbol,
        project: Project
    ): PsiElement? {
        return variableLikeSymbol.callableId?.let {
            val candidates = project.createPsiDeclarationProvider(variableLikeSymbol.scope(project))
                ?.getProperties(it)
            if (candidates?.size == 1)
                candidates.single()
            else {
                // Weigh [PsiField]
                candidates?.firstOrNull { psiMember -> psiMember is PsiField }
                    ?: candidates?.firstOrNull()
            }
        }
    }

    private fun providePsiForClass(
        classLikeSymbol: KaClassLikeSymbol,
        project: Project
    ): PsiElement? {
        return classLikeSymbol.classId?.let {
            project.createPsiDeclarationProvider(classLikeSymbol.scope(project))
                ?.getClassesByClassId(it)
                ?.firstOrNull()
        }
    }

    private fun providePsiForEnumEntry(
        enumEntrySymbol: KaEnumEntrySymbol,
        project: Project
    ): PsiElement? {
        val classId = enumEntrySymbol.callableId?.classId ?: return null
        val psiClass = project.createPsiDeclarationProvider(enumEntrySymbol.scope(project))
            ?.getClassesByClassId(classId)
            ?.firstOrNull() ?: return null
        return psiClass.fields.find {
            it.name == enumEntrySymbol.name.asString()
        }
    }

    private fun KaSymbol.scope(project: Project): GlobalSearchScope {
        // TODO: finding containing module and use a narrower scope?
        return GlobalSearchScope.allScope(project)
    }
}