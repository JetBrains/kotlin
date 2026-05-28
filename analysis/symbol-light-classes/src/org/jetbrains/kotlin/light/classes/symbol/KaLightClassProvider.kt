/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*

/**
 * High-level service for acquiring light elements for given [KaSymbol]s.
 * The service should not be used outside of Analysis API implementation.
 * For the proper LC construction, use the top-level endpoints from Analysis API surface.
 */
@KaImplementationDetail
interface KaLightClassProvider {
    fun getLightClass(classSymbol: KaClassSymbol, session: KaSession): PsiClass?
    fun getLightFacade(fileSymbol: KaFileSymbol, session: KaSession): PsiClass?
    fun getLightFacade(scriptSymbol: KaScriptSymbol, session: KaSession): PsiClass?
    fun getLightClassParameters(parameterSymbol: KaParameterSymbol, session: KaSession): List<PsiParameter>
    fun getLightClassTypeParameter(typeParameterSymbol: KaTypeParameterSymbol, session: KaSession): List<PsiTypeParameter>
    fun getLightClassBackingField(declarationSymbol: KaSymbol, session: KaSession): PsiField?
    fun getLightClassMethods(functionSymbol: KaFunctionSymbol, session: KaSession): List<PsiMethod>

    @KaImplementationDetail
    companion object {
        fun getInstance(project: Project): KaLightClassProvider? = project.serviceOrNull<KaLightClassProvider>()
    }
}
