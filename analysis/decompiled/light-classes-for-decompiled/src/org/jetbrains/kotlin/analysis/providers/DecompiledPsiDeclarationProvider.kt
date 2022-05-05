/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.symbols.*

public object DecompiledPsiDeclarationProvider {
    public fun findPsi(ktSymbol: KtSymbol, project: Project): PsiElement? {
        return when (ktSymbol) {
            is KtConstructorSymbol -> providePsiForConstructor(ktSymbol, project)
            is KtFunctionLikeSymbol -> providePsiForFunction(ktSymbol, project)
            is KtEnumEntrySymbol -> providePsiForEnumEntry(ktSymbol, project)
            is KtVariableLikeSymbol -> providePsiForProperty(ktSymbol, project)
            is KtClassLikeSymbol -> providePsiForClass(ktSymbol, project)
            else -> null
        }
    }

    private fun providePsiForConstructor(
        constructorSymbol: KtConstructorSymbol,
        project: Project
    ): PsiElement? {
        val classId = constructorSymbol.containingClassIdIfNonLocal ?: return null
        val psiClass = project.createPsiDeclarationProvider(constructorSymbol.scope(project))
            ?.getClassesByClassId(classId)
            ?.firstOrNull() ?: return null
        return psiClass.constructors.firstOrNull()
    }

    private fun providePsiForFunction(
        functionLikeSymbol: KtFunctionLikeSymbol,
        project: Project
    ): PsiElement? {
        return functionLikeSymbol.callableIdIfNonLocal?.let {
            project.createPsiDeclarationProvider(functionLikeSymbol.scope(project))
                ?.getFunctions(it)
                ?.firstOrNull()
        }
    }

    private fun providePsiForProperty(
        variableLikeSymbol: KtVariableLikeSymbol,
        project: Project
    ): PsiElement? {
        return variableLikeSymbol.callableIdIfNonLocal?.let {
            project.createPsiDeclarationProvider(variableLikeSymbol.scope(project))
                ?.getProperties(it)
                // TODO: needs to pick field/getter/setter accordingly?
                ?.firstOrNull()
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