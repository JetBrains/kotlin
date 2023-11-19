/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.providers.PsiDeclarationAndKtSymbolEqualityChecker.representsTheSameDeclaration

object DecompiledPsiDeclarationProvider {
    fun KtAnalysisSession.findPsi(ktSymbol: KtSymbol, project: Project): PsiElement? {
        return when (ktSymbol) {
            is KtConstructorSymbol -> providePsiForConstructor(ktSymbol, project)
            is KtFunctionLikeSymbol -> providePsiForFunction(ktSymbol, project)
            is KtEnumEntrySymbol -> providePsiForEnumEntry(ktSymbol, project)
            is KtVariableLikeSymbol -> providePsiForProperty(ktSymbol, project)
            is KtClassLikeSymbol -> providePsiForClass(ktSymbol, project)
            else -> null
        }
    }

    private fun KtAnalysisSession.providePsiForConstructor(
        constructorSymbol: KtConstructorSymbol,
        project: Project
    ): PsiElement? {
        val classId = constructorSymbol.containingClassIdIfNonLocal ?: return null
        val psiClass = project.createPsiDeclarationProvider(constructorSymbol.scope(project))
            ?.getClassesByClassId(classId)
            ?.firstOrNull() ?: return null
        return psiClass.constructors.find { psiMethod ->
            representsTheSameDeclaration(psiMethod, constructorSymbol)
        }
    }

    private fun KtAnalysisSession.providePsiForFunction(
        functionLikeSymbol: KtFunctionLikeSymbol,
        project: Project
    ): PsiElement? {
        return functionLikeSymbol.callableIdIfNonLocal?.let {
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
        variableLikeSymbol: KtVariableLikeSymbol,
        project: Project
    ): PsiElement? {
        return variableLikeSymbol.callableIdIfNonLocal?.let {
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
        classLikeSymbol: KtClassLikeSymbol,
        project: Project
    ): PsiElement? {
        return classLikeSymbol.classIdIfNonLocal?.let {
            project.createPsiDeclarationProvider(classLikeSymbol.scope(project))
                ?.getClassesByClassId(it)
                ?.firstOrNull()
        }
    }

    private fun providePsiForEnumEntry(
        enumEntrySymbol: KtEnumEntrySymbol,
        project: Project
    ): PsiElement? {
        val classId = enumEntrySymbol.containingEnumClassIdIfNonLocal ?: return null
        val psiClass = project.createPsiDeclarationProvider(enumEntrySymbol.scope(project))
            ?.getClassesByClassId(classId)
            ?.firstOrNull() ?: return null
        return psiClass.fields.find {
            it.name == enumEntrySymbol.name.asString()
        }
    }

    private fun KtSymbol.scope(project: Project): GlobalSearchScope {
        // TODO: finding containing module and use a narrower scope?
        return GlobalSearchScope.allScope(project)
    }
}