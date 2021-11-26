/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.analysis.api.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol

internal class FirLightSetterParameterForSymbol(
    private val containingPropertySymbol: KtPropertySymbol,
    private val parameterSymbol: KtValueParameterSymbol,
    containingMethod: FirLightMethod
) : FirLightParameterBaseForSymbol(parameterSymbol, containingMethod) {

    private val _annotations: List<PsiAnnotation> by lazyPub {
        val annotationsFromSetter = parameterSymbol.computeAnnotations(
            parent = this,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = AnnotationUseSiteTarget.SETTER_PARAMETER,
        )

        val annotationsFromProperty = containingPropertySymbol.computeAnnotations(
            parent = this,
            nullability = nullabilityType,
            annotationUseSiteTarget = AnnotationUseSiteTarget.SETTER_PARAMETER,
            includeAnnotationsWithoutSite = false
        )

        annotationsFromSetter + annotationsFromProperty
    }

    override fun getModifierList(): PsiModifierList = _modifierList
    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, emptySet(), _annotations)
    }

    override fun isVarArgs() = false

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightSetterParameterForSymbol && parameterSymbol == other.parameterSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = super.isValid() && parameterSymbol.isValid()
}
