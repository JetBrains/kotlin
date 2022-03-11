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
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

internal object FirCompiledDeclarationProvider {
    fun findPsi(fir: FirElement, project: Project): PsiElement? {
        return when (fir) {
            is FirSimpleFunction -> providePsiForFunction(fir, project)
            is FirSyntheticProperty -> null // That is "Java" synthetic property
            is FirProperty -> providePsiForProperty(fir, project)
            is FirClass -> providePsiForClass(fir, project)
            is FirConstructor -> providePsiForConstructor(fir, project)
            // TODO: is FirEnumEntry -> ?
            else -> null
        }
    }

    private fun providePsiForFunction(
        function: FirSimpleFunction,
        project: Project
    ): PsiElement? {
        val candidates = if (function.isTopLevel) {
            project.createCompiledDeclarationProvider(function.scope(project))
                .getTopLevelFunctions(function.symbol.callableId)
        } else {
            function.containingKtClass(project)?.declarations
                ?.filterIsInstance<KtNamedFunction>()
                ?.filter { it.name == function.name.asString() }
                .orEmpty()
        }

        return candidates.firstOrNull()
    }

    private fun providePsiForProperty(
        property: FirProperty,
        project: Project
    ): PsiElement? {
        val candidates = if (property.isTopLevel) {
            project.createCompiledDeclarationProvider(property.scope(project))
                .getTopLevelProperties(property.symbol.callableId)
        } else {
            property.containingKtClass(project)?.declarations
                ?.filterIsInstance<KtProperty>()
                ?.filter { it.name == property.name.asString() }
                .orEmpty()
        }

        return candidates.firstOrNull()
    }

    private fun providePsiForClass(
        klass: FirClass,
        project: Project
    ): PsiElement? {
        return classByClassId(klass.symbol.classId, klass.scope(project), project)
    }

    private fun providePsiForConstructor(
        constructor: FirConstructor,
        project: Project
    ): PsiElement? {
        val classId = constructor.containingClass()?.classId ?: return null
        val ktClass = project.createCompiledDeclarationProvider(constructor.scope(project))
            .getClassesByClassId(classId)
            .firstOrNull() ?: return null
        return if (constructor.isPrimary)
            ktClass.primaryConstructor
        else
            ktClass.secondaryConstructors.firstOrNull()
    }

    private fun FirDeclaration.scope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.allScope(project)
        /* TODO:
         val session = session as? FirLibrarySession
         return session?.scope ?: GlobalSearchScope.allScope(project)*/
    }

    private fun FirCallableDeclaration.containingKtClass(project: Project): KtClassOrObject? =
        unwrapFakeOverrides().containingClass()?.classId?.let {
            classByClassId(it, scope(project), project)
        }

    private fun classByClassId(classId: ClassId, scope: GlobalSearchScope, project: Project): KtClassOrObject? {
        return project.createCompiledDeclarationProvider(scope)
            .getClassesByClassId(classId)
            .firstOrNull()
    }

    private val FirCallableDeclaration.isTopLevel
        get() = symbol.callableId.className == null
}
