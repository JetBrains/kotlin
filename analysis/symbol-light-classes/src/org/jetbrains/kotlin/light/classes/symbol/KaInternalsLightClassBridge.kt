/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol

/**
 * The internal bridge for utilities sharing.
 */
@KaImplementationDetail
interface KaInternalsLightClassBridge {
    /**
     * The declaration that owns the JVM method for [symbol], or `null` if the method is placed into a file facade class.
     *
     * For a property accessor, the owner of the property is used, as an accessor is never owned by its property on the JVM.
     */
    context(_: KaSession)
    fun jvmMethodOwner(symbol: KaCallableSymbol): KaDeclarationSymbol?

    /**
     * Whether the JVM name of [symbol] is mangled because of value classes.
     *
     * The suffix is either a hash of the signature, as in `classFunInParameter-5lyY9Q4`, or `impl` for a member of a value class,
     * as in `funWithoutParameters-impl`.
     */
    context(_: KaSession)
    fun hasMangledNameDueToValueClasses(symbol: KaCallableSymbol): Boolean

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
