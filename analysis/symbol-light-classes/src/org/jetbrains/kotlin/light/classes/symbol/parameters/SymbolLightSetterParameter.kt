/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.SetterParameterAnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.isLateInit
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.SpecialNames

internal class SymbolLightSetterParameter(
    ktAnalysisSession: KaSession,
    private val containingPropertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    parameterSymbol: KaValueParameterSymbol,
    containingMethod: SymbolLightMethodBase,
) : SymbolLightParameterCommon(ktAnalysisSession, parameterSymbol, containingMethod) {
    override fun getName(): String {
        if (isDefaultSetterParameter) return SpecialNames.IMPLICIT_SET_PARAMETER.asString()
        return super.getName()
    }

    private val isDefaultSetterParameter: Boolean by lazyPub {
        containingPropertySymbolPointer.withSymbol(ktModule) {
            it.setter?.isDefault != false
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = CompositeAnnotationsProvider(
                    SymbolAnnotationsProvider(
                        ktModule = ktModule,
                        annotatedSymbolPointer = parameterSymbolPointer,
                        annotationUseSiteTargetFilter = AnnotationUseSiteTarget.SETTER_PARAMETER.toOptionalFilter(),
                    ),
                    SymbolAnnotationsProvider(
                        ktModule = ktModule,
                        annotatedSymbolPointer = containingPropertySymbolPointer,
                        annotationUseSiteTargetFilter = SetterParameterAnnotationUseSiteTargetFilter,
                    ),
                ),
                additionalAnnotationsProvider = NullabilityAnnotationsProvider(::typeNullability),
            ),
        )
    }

    override fun typeNullability(): KaTypeNullability {
        val isLateInit = containingPropertySymbolPointer.withSymbol(ktModule) { it.isLateInit }
        return if (isLateInit) KaTypeNullability.NON_NULLABLE else super.typeNullability()
    }

    override fun isDeclaredAsVararg(): Boolean = false

    override fun isVarArgs() = false
}
