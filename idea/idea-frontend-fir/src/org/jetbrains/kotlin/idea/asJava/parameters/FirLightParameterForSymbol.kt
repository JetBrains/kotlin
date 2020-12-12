/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.FirLightIdentifier
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.psi.KtParameter

internal class FirLightParameterForSymbol(
    private val parameterSymbol: KtParameterSymbol,
    containingMethod: FirLightMethod
) : FirLightParameter(containingMethod) {
    private val _name: String = parameterSymbol.name.asString()
    override fun getName(): String = _name

    private val _isVarArgs: Boolean = parameterSymbol.isVararg
    override fun isVarArgs() = _isVarArgs
    override fun hasModifierProperty(name: String): Boolean =
        modifierList.hasModifierProperty(name)

    override val kotlinOrigin: KtParameter? = parameterSymbol.psi as? KtParameter

    private val _annotations: List<PsiAnnotation> by lazyPub {
        val annotationSite = (containingMethod.isConstructor && parameterSymbol.symbolKind == KtSymbolKind.MEMBER).ifTrue {
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
        }

        parameterSymbol.computeAnnotations(
            parent = this,
            nullability = parameterSymbol.type.getTypeNullability(parameterSymbol, FirResolvePhase.TYPES),
            annotationUseSiteTarget = annotationSite,
            includeAnnotationsWithoutSite = false
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList
    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, emptySet(), _annotations)
    }

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, parameterSymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _type by lazyPub {
        val convertedType = parameterSymbol.asPsiType(this, FirResolvePhase.TYPES)

        if (convertedType is PsiArrayType && parameterSymbol.isVararg) {
            PsiEllipsisType(convertedType.componentType, convertedType.annotationProvider)
        } else convertedType
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightParameterForSymbol && parameterSymbol == other.parameterSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}