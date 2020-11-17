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
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirPropertyGetterSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.load.java.JvmAbi.getterName
import org.jetbrains.kotlin.load.java.JvmAbi.setterName
import org.jetbrains.kotlin.psi.KtDeclaration

internal class FirLightAccessorMethodForSymbol(
    private val propertyAccessorSymbol: KtPropertyAccessorSymbol,
    containingPropertySymbol: KtPropertySymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    isTopLevel: Boolean,
) : FirLightMethod(
    lightMemberOrigin,
    containingClass,
    if (propertyAccessorSymbol is KtPropertyGetterSymbol) METHOD_INDEX_FOR_GETTER else METHOD_INDEX_FOR_SETTER
) {
    private fun String.abiName(firPropertyAccessor: KtPropertyAccessorSymbol) =
        if (firPropertyAccessor is KtPropertyGetterSymbol) getterName(this)
        else setterName(this)

    private val _name: String by lazyPub {
        val defaultName = containingPropertySymbol.name.identifier.let {
            if (containingClass.isAnnotationType) it else it.abiName(propertyAccessorSymbol)
        }
        containingPropertySymbol.computeJvmMethodName(defaultName, containingClass, accessorSite)
    }

    override fun getName(): String = _name

    override fun isVarArgs(): Boolean = false

    override val kotlinOrigin: KtDeclaration? =
        (propertyAccessorSymbol.psi ?: containingPropertySymbol.psi) as? KtDeclaration

    private val accessorSite
        get() =
            if (propertyAccessorSymbol is KtPropertyGetterSymbol) AnnotationUseSiteTarget.PROPERTY_GETTER
            else AnnotationUseSiteTarget.PROPERTY_SETTER

    private val _annotations: List<PsiAnnotation> by lazyPub {
        val nullabilityType = containingPropertySymbol.type
            .getTypeNullability(containingPropertySymbol, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        containingPropertySymbol.computeAnnotations(
            parent = this,
            nullability = nullabilityType,
            annotationUseSiteTarget = accessorSite,
        )
    }

    private val _modifiers: Set<String> by lazyPub {
        val isOverrideMethod = propertyAccessorSymbol.isOverride || containingPropertySymbol.isOverride
        val isInterfaceMethod = containingClass.isInterface

        val visibility = isOverrideMethod.ifTrue {
            (containingClass as? FirLightClassForSymbol)
                ?.tryGetEffectiveVisibility(containingPropertySymbol)
                ?.toPsiVisibility(isTopLevel)
        } ?: propertyAccessorSymbol.computeVisibility(isTopLevel)

        val modifiers = containingPropertySymbol.computeModalityForMethod(
            isTopLevel = isTopLevel,
            suppressFinal = isOverrideMethod || isInterfaceMethod
        ) + visibility

        modifiers
            .add(
                what = PsiModifier.STATIC,
                `if` = _annotations.any { it.qualifiedName == "kotlin.jvm.JvmStatic" }
            ).add(
                what = PsiModifier.ABSTRACT,
                `if` = isInterfaceMethod
            )
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