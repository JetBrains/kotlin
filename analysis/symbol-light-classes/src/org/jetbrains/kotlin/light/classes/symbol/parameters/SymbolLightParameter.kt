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
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
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
            staticModifiers = emptySet(),
        ) { modifierList ->
            val annotationSite = isConstructorParameterSymbol.ifTrue {
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
            }

            parameterSymbolPointer.withSymbol(ktModule) { parameterSymbol ->
                val nullability = if (parameterSymbol.isVararg) NullabilityType.NotNull else super.nullabilityType
                parameterSymbol.computeAnnotations(
                    modifierList = modifierList,
                    nullability = nullability,
                    annotationUseSiteTarget = annotationSite,
                    includeAnnotationsWithoutSite = true,
                )
            }
        }
    }

    private val isVararg: Boolean by lazyPub {
        parameterSymbolPointer.withSymbol(ktModule) { it.isVararg }
    }

    override fun isVarArgs() = isVararg
}