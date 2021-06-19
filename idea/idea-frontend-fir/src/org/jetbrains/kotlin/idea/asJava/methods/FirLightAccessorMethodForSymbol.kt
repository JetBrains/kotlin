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
import org.jetbrains.kotlin.asJava.elements.FirLightIdentifier
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.asJava.parameters.FirLightSetterParameterForSymbol
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.load.java.JvmAbi.getterName
import org.jetbrains.kotlin.load.java.JvmAbi.setterName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter

internal class FirLightAccessorMethodForSymbol(
    private val propertyAccessorSymbol: KtPropertyAccessorSymbol,
    private val containingPropertySymbol: KtPropertySymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    private val isTopLevel: Boolean,
) : FirLightMethod(
    lightMemberOrigin,
    containingClass,
    if (propertyAccessorSymbol is KtPropertyGetterSymbol) METHOD_INDEX_FOR_GETTER else METHOD_INDEX_FOR_SETTER
) {
    private val isGetter: Boolean get() = propertyAccessorSymbol is KtPropertyGetterSymbol

    private fun String.abiName() =
        if (isGetter) getterName(this) else setterName(this)

    private val _name: String by lazyPub {
        propertyAccessorSymbol.getJvmNameFromAnnotation() ?: run {
            val defaultName = containingPropertySymbol.name.identifier.let {
                if (containingClass.isAnnotationType) it else it.abiName()
            }
            containingPropertySymbol.computeJvmMethodName(defaultName, containingClass, accessorSite)
        }
    }

    override fun getName(): String = _name

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun isVarArgs(): Boolean = false

    override val kotlinOrigin: KtDeclaration? =
        (propertyAccessorSymbol.psi ?: containingPropertySymbol.psi) as? KtDeclaration

    private val accessorSite
        get() =
            if (propertyAccessorSymbol is KtPropertyGetterSymbol) AnnotationUseSiteTarget.PROPERTY_GETTER
            else AnnotationUseSiteTarget.PROPERTY_SETTER

    //TODO Fix it when KtFirConstructorValueParameterSymbol be ready
    private val isParameter: Boolean get() = containingPropertySymbol.psi.let { it == null || it is KtParameter }

    private fun computeAnnotations(isPrivate: Boolean): List<PsiAnnotation> {
        val nullabilityApplicable = isGetter &&
                !isPrivate &&
                !(isParameter && (containingClass.isAnnotationType || containingClass.isEnum))

        val nullabilityType = if (nullabilityApplicable) containingPropertySymbol.annotatedType.type
            .getTypeNullability(containingPropertySymbol, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        else NullabilityType.Unknown

        val annotationsFromProperty = containingPropertySymbol.computeAnnotations(
            parent = this,
            nullability = nullabilityType,
            annotationUseSiteTarget = accessorSite,
        )

        val annotationsFromAccessor = propertyAccessorSymbol.computeAnnotations(
            parent = this,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = null,
        )

        return annotationsFromProperty + annotationsFromAccessor
    }

    private fun computeModifiers(): Set<String> {
        val isOverrideMethod = propertyAccessorSymbol.isOverride || containingPropertySymbol.isOverride
        val isInterfaceMethod = containingClass.isInterface

        val modifiers = mutableSetOf<String>()

        containingPropertySymbol.computeModalityForMethod(
            isTopLevel = isTopLevel,
            suppressFinal = isOverrideMethod || isInterfaceMethod,
            result = modifiers
        )

        val visibility = isOverrideMethod.ifTrue {
            (containingClass as? FirLightClassForSymbol)
                ?.tryGetEffectiveVisibility(containingPropertySymbol)
                ?.toPsiVisibilityForMember(isTopLevel)
        } ?: propertyAccessorSymbol.toPsiVisibilityForMember(isTopLevel)
        modifiers.add(visibility)

        if (containingPropertySymbol.hasJvmStaticAnnotation(accessorSite)) {
            modifiers.add(PsiModifier.STATIC)
        }

        if (isInterfaceMethod) {
            modifiers.add(PsiModifier.ABSTRACT)
        }

        return modifiers
    }

    private val _modifierList: PsiModifierList by lazyPub {
        val modifiers = computeModifiers()
        val annotations = computeAnnotations(modifiers.contains(PsiModifier.PRIVATE))
        FirLightClassModifierList(this, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _isDeprecated: Boolean by lazyPub {
        containingPropertySymbol.hasDeprecatedAnnotation(accessorSite)
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, containingPropertySymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _returnedType: PsiType? by lazyPub {
        if (!isGetter) return@lazyPub PsiType.VOID
        return@lazyPub containingPropertySymbol.annotatedType.asPsiType(
            context = containingPropertySymbol,
            parent = this@FirLightAccessorMethodForSymbol,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )
    }

    override fun getReturnType(): PsiType? = _returnedType

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightAccessorMethodForSymbol &&
                        isGetter == other.isGetter &&
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
                FirLightSetterParameterForSymbol(
                    parameterSymbol = propertyParameter,
                    containingPropertySymbol = containingPropertySymbol,
                    containingMethod = this@FirLightAccessorMethodForSymbol
                )
            )
        }

        builder
    }

    override fun getParameterList(): PsiParameterList = _parametersList

    override fun isValid(): Boolean = super.isValid() && propertyAccessorSymbol.isValid()
}