/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_GETTER
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_SETTER
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.load.java.JvmAbi.getterName
import org.jetbrains.kotlin.load.java.JvmAbi.setterName
import org.jetbrains.kotlin.psi.KtDeclaration

internal class FirLightAccessorMethodForSymbol(
    private val propertyAccessorSymbol: KtPropertyAccessorSymbol,
    containingPropertySymbol: KtPropertySymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    isTopLevel: Boolean
) : FirLightMethod(
    lightMemberOrigin,
    containingClass,
    if (propertyAccessorSymbol is KtPropertyGetterSymbol) METHOD_INDEX_FOR_GETTER else METHOD_INDEX_FOR_SETTER
) {
    private fun String.abiName(firPropertyAccessor: KtPropertyAccessorSymbol) =
        if (firPropertyAccessor is KtPropertyGetterSymbol) getterName(this)
        else setterName(this)

    //TODO add JvmName
    private val _name: String by lazyPub {
        containingPropertySymbol.name.identifier
            .abiName(propertyAccessorSymbol)
    }

    override fun getName(): String = _name

    override fun isVarArgs(): Boolean = false

    override val kotlinOrigin: KtDeclaration? =
        (propertyAccessorSymbol.psi ?: containingPropertySymbol.psi) as? KtDeclaration

    private val _annotations: List<PsiAnnotation> by lazyPub {
        val accessorSite =
            if (propertyAccessorSymbol is KtPropertyGetterSymbol) AnnotationUseSiteTarget.PROPERTY_GETTER
            else AnnotationUseSiteTarget.PROPERTY_SETTER
        containingPropertySymbol.computeAnnotations(
            parent = this,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = accessorSite,
        )
    }

    private val _modifiers: Set<String> by lazyPub {

        val isOverride = propertyAccessorSymbol.isOverride || containingPropertySymbol.isOverride
        val modifiers = propertyAccessorSymbol.computeModalityForMethod(isTopLevel, isOverride) +
                propertyAccessorSymbol.computeVisibility(isTopLevel)

        val isJvmStatic =
            _annotations.any { it.qualifiedName == "kotlin.jvm.JvmStatic" }

        if (isJvmStatic) return@lazyPub modifiers + PsiModifier.STATIC

        modifiers
    }

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, _modifiers, _annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _returnedType: PsiType? by lazyPub {
        if (propertyAccessorSymbol !is KtPropertyGetterSymbol) return@lazyPub PsiType.VOID
        return@lazyPub containingPropertySymbol.type.asPsiType(
            context = containingPropertySymbol,
            parent = this@FirLightAccessorMethodForSymbol,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )
    }

    override fun getReturnType(): PsiType? = _returnedType

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightAccessorMethodForSymbol &&
                        kotlinOrigin == other.kotlinOrigin &&
                        propertyAccessorSymbol == other.propertyAccessorSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()


    private val _parametersList by lazyPub {
        val builder = LightParameterListBuilder(manager, language)

        FirLightParameterForReceiver.tryGet(containingPropertySymbol, this)?.let {
            builder.addParameter(it)
        }

        val propertyParameter = (propertyAccessorSymbol as? KtPropertySetterSymbol)?.parameter

        if (propertyParameter != null) {
            builder.addParameter(
                FirLightParameterForSymbol(
                    parameterSymbol = propertyParameter,
                    containingMethod = this@FirLightAccessorMethodForSymbol
                )
            )
        }

        builder
    }

    override fun getParameterList(): PsiParameterList = _parametersList
}