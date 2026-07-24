/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
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

    public fun mapToJvmTypeDescriptor(type: KaType): String

    public fun isPrimitiveBacked(type: KaType): Boolean

    public fun namedClassSymbol(psiClass: PsiClass): KaNamedClassSymbol?

    public fun callableSymbol(psiMember: PsiMember): KaCallableSymbol?

    public fun containingJvmClassName(symbol: KaCallableSymbol): String?

    public fun javaGetterName(symbol: KaPropertySymbol): Name

    public fun javaSetterName(symbol: KaPropertySymbol): Name?
}
