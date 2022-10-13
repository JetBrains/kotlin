/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KtConstantValueForAnnotation
import org.jetbrains.kotlin.analysis.api.KtNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_GETTER
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_SETTER
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.annotations.getJvmNameFromAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSetterParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.load.java.JvmAbi.getterName
import org.jetbrains.kotlin.load.java.JvmAbi.setterName
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

context(KtAnalysisSession)
internal class SymbolLightAccessorMethod(
    private val propertyAccessorSymbol: KtPropertyAccessorSymbol,
    private val containingPropertySymbol: KtPropertySymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    private val isTopLevel: Boolean,
    private val suppressStatic: Boolean = false,
) : SymbolLightMethodBase(
    lightMemberOrigin,
    containingClass,
    if (propertyAccessorSymbol is KtPropertyGetterSymbol) METHOD_INDEX_FOR_GETTER else METHOD_INDEX_FOR_SETTER
) {
    private val isGetter: Boolean get() = propertyAccessorSymbol is KtPropertyGetterSymbol

    private fun String.abiName() =
        if (isGetter) getterName(this) else setterName(this)

    private val _name: String by lazyPub {
        propertyAccessorSymbol.getJvmNameFromAnnotation(accessorSite) ?: run {
            val defaultName = containingPropertySymbol.name.identifier.let {
                if (containingClass.isAnnotationType) it else it.abiName()
            }
            containingPropertySymbol.computeJvmMethodName(defaultName, containingClass, accessorSite)
        }
    }

    override fun getName(): String = _name

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        hasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterList = containingPropertySymbol,
            )
        }
    }

    override fun hasTypeParameters(): Boolean = containingPropertySymbol.typeParameters.isNotEmpty()
    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun isVarArgs(): Boolean = false

    private val accessorSite
        get() =
            if (propertyAccessorSymbol is KtPropertyGetterSymbol) AnnotationUseSiteTarget.PROPERTY_GETTER
            else AnnotationUseSiteTarget.PROPERTY_SETTER

    //TODO Fix it when SymbolConstructorValueParameter be ready
    private val isParameter: Boolean get() = containingPropertySymbol.psi.let { it == null || it is KtParameter }

    private fun computeAnnotations(isPrivate: Boolean): List<PsiAnnotation> {
        val nullabilityApplicable = isGetter &&
                !isPrivate &&
                !(isParameter && containingClass.isAnnotationType)

        val nullabilityType = if (nullabilityApplicable) {
            getTypeNullability(containingPropertySymbol.returnType)
        } else NullabilityType.Unknown

        val annotationsFromProperty = containingPropertySymbol.computeAnnotations(
            parent = this,
            nullability = nullabilityType,
            annotationUseSiteTarget = accessorSite,
            includeAnnotationsWithoutSite = false
        )

        val annotationsFromAccessor = propertyAccessorSymbol.computeAnnotations(
            parent = this,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = accessorSite,
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
            tryGetEffectiveVisibility(containingPropertySymbol)
                ?.toPsiVisibilityForMember()
        } ?: propertyAccessorSymbol.toPsiVisibilityForMember()
        modifiers.add(visibility)

        if (!suppressStatic &&
            (containingPropertySymbol.hasJvmStaticAnnotation() || propertyAccessorSymbol.hasJvmStaticAnnotation(accessorSite))
        ) {
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
        SymbolLightMemberModifierList(this, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _isDeprecated: Boolean by lazyPub {
        containingPropertySymbol.hasDeprecatedAnnotation(accessorSite)
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _identifier: PsiIdentifier by lazyPub {
        SymbolLightIdentifier(this, containingPropertySymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _returnedType: PsiType by lazyPub {
        if (!isGetter) return@lazyPub PsiType.VOID
        containingPropertySymbol.returnType.asPsiType(
            this@SymbolLightAccessorMethod,
            KtTypeMappingMode.RETURN_TYPE,
            containingClass.isAnnotationType
        ) ?: nonExistentType()
    }

    override fun getReturnType(): PsiType = _returnedType

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is SymbolLightAccessorMethod &&
                        isGetter == other.isGetter &&
                        kotlinOrigin == other.kotlinOrigin &&
                        propertyAccessorSymbol == other.propertyAccessorSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    private val _parametersList by lazyPub {
        SymbolLightParameterList(this, containingPropertySymbol) { builder ->
            val propertyParameter = (propertyAccessorSymbol as? KtPropertySetterSymbol)?.parameter
            if (propertyParameter != null) {
                builder.addParameter(
                    SymbolLightSetterParameter(
                        containingPropertySymbol, propertyParameter, this@SymbolLightAccessorMethod
                    )
                )
            }
        }
    }

    override fun getParameterList(): PsiParameterList = _parametersList

    override fun isValid(): Boolean = super.isValid() && propertyAccessorSymbol.isValid()

    override fun isOverride(): Boolean = propertyAccessorSymbol.isOverride

    private val _defaultValue: PsiAnnotationMemberValue? by lazyPub {
        if (!containingClass.isAnnotationType) return@lazyPub null
        when (val initializer = containingPropertySymbol.initializer) {
            is KtConstantInitializerValue -> initializer.constant.createPsiLiteral(this)
            is KtConstantValueForAnnotation -> initializer.annotationValue.toAnnotationMemberValue(this)
            is KtNonConstantInitializerValue -> null
            null -> null
        }
    }

    override fun getDefaultValue(): PsiAnnotationMemberValue? {
        return _defaultValue
    }

    override fun getText(): String {
        return lightMemberOrigin?.auxiliaryOriginalElement?.text ?: super.getText()
    }

    override fun getTextOffset(): Int {
        return lightMemberOrigin?.auxiliaryOriginalElement?.textOffset ?: super.getTextOffset()
    }

    override fun getTextRange(): TextRange {
        return lightMemberOrigin?.auxiliaryOriginalElement?.textRange ?: super.getTextRange()
    }
}
