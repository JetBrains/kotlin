/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.createCompiledDeclarationProvider
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*

internal object FirCompiledDeclarationProvider {
    fun findPsi(fir: FirElement, project: Project): PsiElement? {
        return when (fir) {
            is FirPropertyAccessor -> providePsiForPropertyAccessor(fir, project)
            is FirSimpleFunction -> providePsiForFunction(fir, project)
            is FirProperty -> providePsiForProperty(fir, project)
            is FirClass -> providePsiForClass(fir, project)
            is FirConstructor -> providePsiForConstructor(fir, project)
            // TODO: is FirEnumEntry -> ?
            else -> null
        }
    }

    private fun providePsiForPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        project: Project
    ): PsiElement? {
        return project.createCompiledDeclarationProvider(propertyAccessor.scope(project))
            .getFunctions(propertyAccessor.symbol.callableId)
            .firstOrNull()
    }

    private fun providePsiForFunction(
        function: FirSimpleFunction,
        project: Project
    ): PsiElement? {
        return project.createCompiledDeclarationProvider(function.scope(project))
            .getFunctions(function.symbol.callableId)
            .firstOrNull()
    }

    private fun providePsiForProperty(
        property: FirProperty,
        project: Project
    ): PsiElement? {
        return project.createCompiledDeclarationProvider(property.scope(project))
            .getProperties(property.symbol.callableId)
            .firstOrNull()
    }

    private fun providePsiForClass(
        klass: FirClass,
        project: Project
    ): PsiElement? {
        return project.createCompiledDeclarationProvider(klass.scope(project))
            .getClassesByClassId(klass.symbol.classId)
            .firstOrNull()
    }

    private fun providePsiForConstructor(
        constructor: FirConstructor,
        project: Project
    ): PsiElement? {
        val classId = constructor.containingClass()?.classId ?: return null
        val psiClass = project.createCompiledDeclarationProvider(constructor.scope(project))
            .getClassesByClassId(classId)
            .firstOrNull() ?: return null
        return psiClass.constructors.firstOrNull()
    }

    private fun FirDeclaration.scope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.allScope(project)
        /* TODO:
         val session = session as? FirLibrarySession
         return session?.scope ?: GlobalSearchScope.allScope(project)*/
    }
}
