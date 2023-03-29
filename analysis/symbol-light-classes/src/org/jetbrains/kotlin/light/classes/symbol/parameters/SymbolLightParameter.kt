/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.NullabilityAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.toOptionalFilter
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class SymbolLightParameter(
    ktAnalysisSession: KtAnalysisSession,
    parameterSymbol: KtValueParameterSymbol,
    containingMethod: SymbolLightMethodBase
) : SymbolLightParameterCommon(ktAnalysisSession, parameterSymbol, containingMethod) {
    private val isConstructorParameterSymbol = containingMethod.isConstructor

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = parameterSymbolPointer,
                    annotationUseSiteTargetFilter = isConstructorParameterSymbol.ifTrue {
                        AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
                    }.toOptionalFilter(),
                ),
                additionalAnnotationsProvider = NullabilityAnnotationsProvider(::nullabilityType),
            ),
        )
    }
}