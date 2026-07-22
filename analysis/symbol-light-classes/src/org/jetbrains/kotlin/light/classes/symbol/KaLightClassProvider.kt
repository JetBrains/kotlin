/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*

/**
 * High-level interface for acquiring light elements for given [KaSymbol]s.
 * Should not be used outside of Analysis API implementation.
 * For the proper LC construction, use the top-level endpoints from Analysis API surface.
 */
@KaImplementationDetail
interface KaLightClassProvider {
    context(session: KaSession)
    fun getLightClass(classSymbol: KaClassSymbol): PsiClass?

    context(session: KaSession)
    fun getLightFacade(fileSymbol: KaFileSymbol): PsiClass?

    context(session: KaSession)
    fun getLightFacade(scriptSymbol: KaScriptSymbol): PsiClass?

    context(session: KaSession)
    fun getLightClassParameters(parameterSymbol: KaParameterSymbol): List<PsiParameter>

    context(session: KaSession)
    fun getLightClassTypeParameter(typeParameterSymbol: KaTypeParameterSymbol): List<PsiTypeParameter>

    context(session: KaSession)
    fun getLightClassBackingField(declarationSymbol: KaSymbol): PsiField?

    context(session: KaSession)
    fun getLightClassMethods(functionSymbol: KaFunctionSymbol): List<PsiMethod>
}
