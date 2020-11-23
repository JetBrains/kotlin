/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtPossibleExtensionSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtParameter

internal class FirLightParameterForReceiver private constructor(
    private val annotatedSymbol: KtAnnotatedSymbol,
    type: KtType,
    methodName: String,
    method: FirLightMethod
) : FirLightParameter(method) {

    companion object {
        fun tryGet(
            callableSymbol: KtCallableSymbol,
            method: FirLightMethod
        ): FirLightParameterForReceiver? {

            if (callableSymbol !is KtNamedSymbol) return null
            if (callableSymbol !is KtAnnotatedSymbol) return null
            if (callableSymbol !is KtPossibleExtensionSymbol) return null

            if (!callableSymbol.isExtension) return null
            val receiverType = callableSymbol.receiverType ?: return null

            return FirLightParameterForReceiver(
                annotatedSymbol = callableSymbol,
                type = receiverType,
                methodName = callableSymbol.name.asString(),
                method = method
            )
        }
    }

    private val _name: String by lazyPub {
        AsmUtil.getLabeledThisName(methodName, AsmUtil.LABELED_THIS_PARAMETER, AsmUtil.RECEIVER_PARAMETER_NAME)
    }

    override fun getName(): String = _name

    override fun isVarArgs() = false
    override fun hasModifierProperty(name: String): Boolean = false //TODO()

    override val kotlinOrigin: KtParameter? = null

    private val _annotations: List<PsiAnnotation> by lazyPub {
        annotatedSymbol.computeAnnotations(
            parent = this,
            nullability = type.getTypeNullability(annotatedSymbol, FirResolvePhase.TYPES),
            annotationUseSiteTarget = AnnotationUseSiteTarget.RECEIVER,
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList
    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, emptySet(), _annotations)
    }

    private val _type: PsiType by lazyPub {
        type.asPsiType(annotatedSymbol, method, FirResolvePhase.TYPES)
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightParameterForReceiver &&
                 kotlinOrigin == other.kotlinOrigin &&
                 annotatedSymbol == other.annotatedSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}


