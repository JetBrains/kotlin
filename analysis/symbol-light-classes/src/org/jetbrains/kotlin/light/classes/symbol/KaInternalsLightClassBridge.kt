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
 * The internal bridge for utilities sharing.
 */
@KaImplementationDetail
interface KaInternalsLightClassBridge {
    /**
     * Applies [JvmName] and `internal` mangling to [defaultName].
     *
     * @param ignoreValueClassMangling whether to compute the name as if value classes did not require mangling
     * @return the computed Java method name, or `null` if value-class mangling is required and
     * [ignoreValueClassMangling] is `false`
     */
    context(_: KaSession)
    fun computeJavaMethodName(symbol: KaCallableSymbol, defaultName: String, ignoreValueClassMangling: Boolean): String?

    /**
     * [PsiClass] for [classSymbol] in the context of [KaSession.useSiteModule].
     * For the proper LC construction, use the endpoints from Analysis API surface.
     *
     * @see org.jetbrains.kotlin.analysis.api.javaInterop.asPsiClass
     */
    context(session: KaSession)
    fun getLightClass(classSymbol: KaClassSymbol): PsiClass?

    /**
     * [PsiClass] facade for [fileSymbol] in the context of [KaSession.useSiteModule].
     * For the proper LC construction, use the endpoints from Analysis API surface.
     *
     * @see org.jetbrains.kotlin.analysis.api.javaInterop.asFacadePsiClass
     */
    context(session: KaSession)
    fun getLightFacade(fileSymbol: KaFileSymbol): PsiClass?

    /**
     * [PsiClass] facade for [scriptSymbol] in the context of [KaSession.useSiteModule].
     * For the proper LC construction, use the endpoints from Analysis API surface.
     *
     * @see org.jetbrains.kotlin.analysis.api.javaInterop.asFacadePsiClass
     */
    context(session: KaSession)
    fun getLightFacade(scriptSymbol: KaScriptSymbol): PsiClass?

    /**
     * [PsiParameter]s for [parameterSymbol] in the context of [KaSession.useSiteModule].
     * For the proper LC construction, use the endpoints from Analysis API surface.
     *
     * @see org.jetbrains.kotlin.analysis.api.javaInterop.asPsiParameters
     */
    context(session: KaSession)
    fun getLightClassParameters(parameterSymbol: KaParameterSymbol): List<PsiParameter>

    /**
     * [PsiTypeParameter]s for [typeParameterSymbol] in the context of [KaSession.useSiteModule].
     * For the proper LC construction, use the endpoints from Analysis API surface.
     *
     * @see org.jetbrains.kotlin.analysis.api.javaInterop.asPsiTypeParameters
     */
    context(session: KaSession)
    fun getLightClassTypeParameter(typeParameterSymbol: KaTypeParameterSymbol): List<PsiTypeParameter>

    /**
     * [PsiField]s for [declarationSymbol] in the context of [KaSession.useSiteModule].
     * For the proper LC construction, use the endpoints from Analysis API surface.
     *
     * @see org.jetbrains.kotlin.analysis.api.javaInterop.asPsiField
     */
    context(session: KaSession)
    fun getLightClassBackingField(declarationSymbol: KaSymbol): PsiField?

    /**
     * [PsiMethod]s for [functionSymbol] in the context of [KaSession.useSiteModule].
     * For the proper LC construction, use the endpoints from Analysis API surface.
     *
     * @see org.jetbrains.kotlin.analysis.api.javaInterop.asPsiMethods
     */
    context(session: KaSession)
    fun getLightClassMethods(functionSymbol: KaFunctionSymbol): List<PsiMethod>
}
