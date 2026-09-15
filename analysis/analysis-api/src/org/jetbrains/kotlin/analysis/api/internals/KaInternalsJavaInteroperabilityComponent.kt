/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.name.Name

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsJavaInteroperabilityComponent {
    public fun asPsiType(
        type: KaType,
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
        preserveAnnotations: Boolean,
        allowNonJvmPlatforms: Boolean,
    ): PsiType?

    public fun asKaType(psiType: PsiType, useSitePosition: PsiElement): KaType?

    public fun asPsiClass(classSymbol: KaClassSymbol): PsiClass?

    public fun asFacadePsiClass(fileSymbol: KaFileSymbol): PsiClass?

    public fun asFacadePsiClass(scriptSymbol: KaScriptSymbol): PsiClass?

    public fun asPsiMethods(functionSymbol: KaFunctionSymbol): List<PsiMethod>

    public fun asPsiTypeParameters(typeParameterSymbol: KaTypeParameterSymbol): List<PsiTypeParameter>

    public fun asPsiParameters(parameterSymbol: KaParameterSymbol): List<PsiParameter>

    public fun asPsiField(backingFieldSymbol: KaBackingFieldSymbol): PsiField?

    public fun asPsiField(classSymbol: KaClassSymbol): PsiField?

    public fun asPsiField(enumEntrySymbol: KaEnumEntrySymbol): PsiEnumConstant?

    public fun mapToJvmTypeDescriptor(type: KaType): String

    public fun isPrimitiveBacked(type: KaType): Boolean

    public fun namedClassSymbol(psiClass: PsiClass): KaNamedClassSymbol?

    public fun callableSymbol(psiMember: PsiMember): KaCallableSymbol?

    public fun containingJvmClassName(symbol: KaCallableSymbol): String?

    public fun javaGetterName(symbol: KaPropertySymbol): Name

    public fun javaSetterName(symbol: KaPropertySymbol): Name?

    public fun javaMethodName(function: KaFunctionSymbol): String?
}
