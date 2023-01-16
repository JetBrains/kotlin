/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KtConstantValueForAnnotation
import org.jetbrains.kotlin.analysis.api.KtNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_GETTER
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_SETTER
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.LazyModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSetterParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.load.java.JvmAbi.getterName
import org.jetbrains.kotlin.load.java.JvmAbi.setterName
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class SymbolLightAccessorMethod private constructor(
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val isGetter: Boolean,
    private val propertyAccessorDeclaration: KtPropertyAccessor?,
    private val propertyAccessorSymbolPointer: KtSymbolPointer<KtPropertyAccessorSymbol>,
    private val containingPropertyDeclaration: KtCallableDeclaration?,
    private val containingPropertySymbolPointer: KtSymbolPointer<KtPropertySymbol>,
    private val isTopLevel: Boolean,
    private val suppressStatic: Boolean,
) : SymbolLightMethodBase(
    lightMemberOrigin,
    containingClass,
    methodIndex,
) {
    internal constructor(
        ktAnalysisSession: KtAnalysisSession,
        propertyAccessorSymbol: KtPropertyAccessorSymbol,
        containingPropertySymbol: KtPropertySymbol,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassBase,
        isTopLevel: Boolean,
        suppressStatic: Boolean = false,
    ) : this(
        lightMemberOrigin,
        containingClass,
        methodIndex = if (propertyAccessorSymbol is KtPropertyGetterSymbol) METHOD_INDEX_FOR_GETTER else METHOD_INDEX_FOR_SETTER,
        isGetter = propertyAccessorSymbol is KtPropertyGetterSymbol,
        propertyAccessorDeclaration = propertyAccessorSymbol.sourcePsiSafe(),
        propertyAccessorSymbolPointer = with(ktAnalysisSession) { propertyAccessorSymbol.createPointer() },
        containingPropertyDeclaration = containingPropertySymbol.sourcePsiSafe(),
        containingPropertySymbolPointer = with(ktAnalysisSession) { containingPropertySymbol.createPointer() },
        isTopLevel = isTopLevel,
        suppressStatic = suppressStatic,
    )

    context(KtAnalysisSession)
    private fun propertyAccessorSymbol(): KtPropertyAccessorSymbol {
        return propertyAccessorSymbolPointer.restoreSymbolOrThrowIfDisposed()
    }

    context(KtAnalysisSession)
    private fun propertySymbol(): KtPropertySymbol {
        return containingPropertySymbolPointer.restoreSymbolOrThrowIfDisposed()
    }

    private fun String.abiName() = if (isGetter) getterName(this) else setterName(this)

    private val _name: String by lazyPub {
        analyzeForLightClasses(ktModule) {
            propertyAccessorSymbol().getJvmNameFromAnnotation(accessorSite) ?: run {
                val symbol = propertySymbol()
                val defaultName = symbol.name.identifier.let {
                    if (containingClass.isAnnotationType) it else it.abiName()
                }

                symbol.computeJvmMethodName(defaultName, containingClass, accessorSite)
            }
        }
    }

    override fun getName(): String = _name

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        hasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterPointer = containingPropertySymbolPointer,
                ktModule = ktModule,
                ktDeclaration = containingPropertyDeclaration,
            )
        }
    }

    override fun hasTypeParameters(): Boolean = hasTypeParameters(ktModule, containingPropertyDeclaration, containingPropertySymbolPointer)

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun isVarArgs(): Boolean = false

    override val kotlinOrigin: KtDeclaration? get() = containingPropertyDeclaration

    private val accessorSite
        get() = if (isGetter) AnnotationUseSiteTarget.PROPERTY_GETTER else AnnotationUseSiteTarget.PROPERTY_SETTER

    //TODO Fix it when SymbolConstructorValueParameter be ready
    private val isParameter: Boolean get() = containingPropertyDeclaration == null || containingPropertyDeclaration is KtParameter

    override fun computeThrowsList(builder: LightReferenceListBuilder) {
        analyzeForLightClasses(ktModule) {
            propertyAccessorSymbol().computeThrowsList(
                builder,
                accessorSite,
                this@SymbolLightAccessorMethod,
                containingClass,
                acceptAnnotationsWithoutUseSite = true,
            )
        }
    }

    private fun computeAnnotations(modifierList: PsiModifierList): List<PsiAnnotation> = analyzeForLightClasses(ktModule) {
        val nullabilityApplicable = isGetter &&
                !modifierList.hasModifierProperty(PsiModifier.PRIVATE) &&
                !(isParameter && containingClass.isAnnotationType)

        val propertySymbol = propertySymbol()
        val nullabilityType = if (nullabilityApplicable) getTypeNullability(propertySymbol.returnType) else NullabilityType.Unknown
        val annotationsFromProperty = propertySymbol.computeAnnotations(
            modifierList = modifierList,
            nullability = nullabilityType,
            annotationUseSiteTarget = accessorSite,
            includeAnnotationsWithoutSite = false,
        )

        val propertyAccessorSymbol = propertyAccessorSymbol()
        val annotationsFromAccessor = propertyAccessorSymbol.computeAnnotations(
            modifierList = modifierList,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = accessorSite,
        )

        annotationsFromProperty + annotationsFromAccessor
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in LazyModifiersBox.VISIBILITY_MODIFIERS -> LazyModifiersBox.computeVisibilityForMember(ktModule, propertyAccessorSymbolPointer)

        in LazyModifiersBox.MODALITY_MODIFIERS -> {
            val modality = if (containingClass.isInterface) {
                PsiModifier.ABSTRACT
            } else {
                analyzeForLightClasses(ktModule) {
                    val propertySymbol = propertySymbol()
                    propertySymbol.computeSimpleModality()?.takeUnless { it.isSuppressedFinalModifier(containingClass, propertySymbol) }
                }
            }

            LazyModifiersBox.MODALITY_MODIFIERS_MAP.with(modality)
        }

        PsiModifier.STATIC -> {
            val isStatic = if (suppressStatic) {
                false
            } else {
                isTopLevel || isStatic()
            }

            mapOf(modifier to isStatic)
        }

        else -> null
    }

    private fun isStatic(): Boolean = analyzeForLightClasses(ktModule) {
        val propertySymbol = propertySymbol()
        propertySymbol.isStatic ||
                propertySymbol.hasJvmStaticAnnotation(accessorSite, acceptAnnotationsWithoutUseSite = true) ||
                propertyAccessorSymbol().hasJvmStaticAnnotation(accessorSite, acceptAnnotationsWithoutUseSite = true)
    }

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            lazyModifiersComputer = ::computeModifiers,
            annotationsComputer = ::computeAnnotations,
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _isDeprecated: Boolean by lazyPub {
        analyzeForLightClasses(ktModule) {
            propertySymbol().hasDeprecatedAnnotation(accessorSite, acceptAnnotationsWithoutUseSite = true) ||
                    propertyAccessorSymbol().hasDeprecatedAnnotation(accessorSite, acceptAnnotationsWithoutUseSite = true)
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _identifier: PsiIdentifier by lazyPub {
        KtLightIdentifier(this, containingPropertyDeclaration)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _returnedType: PsiType by lazyPub {
        if (!isGetter) return@lazyPub PsiType.VOID
        analyzeForLightClasses(ktModule) {
            propertySymbol().returnType.asPsiType(
                this@SymbolLightAccessorMethod,
                allowErrorTypes = true,
                KtTypeMappingMode.RETURN_TYPE,
                containingClass.isAnnotationType,
            )
        } ?: nonExistentType()
    }

    override fun getReturnType(): PsiType = _returnedType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightAccessorMethod ||
            other.isGetter != isGetter ||
            other.isTopLevel != isTopLevel ||
            other.suppressStatic != suppressStatic ||
            other.ktModule != ktModule
        ) return false

        if (propertyAccessorDeclaration != null || other.propertyAccessorDeclaration != null) {
            return propertyAccessorDeclaration == other.propertyAccessorDeclaration
        }

        if (containingPropertyDeclaration != null || other.containingPropertyDeclaration != null) {
            return containingPropertyDeclaration == other.containingPropertyDeclaration
        }

        return compareSymbolPointers(propertyAccessorSymbolPointer, other.propertyAccessorSymbolPointer)
    }

    override fun hashCode(): Int = propertyAccessorDeclaration?.hashCode() ?: containingPropertyDeclaration.hashCode()

    private val _parametersList by lazyPub {
        val parameterPopulator: (LightParameterListBuilder) -> Unit = if (!isGetter) {
            { builder ->
                analyzeForLightClasses(ktModule) {
                    val propertyParameter = (propertyAccessorSymbol() as? KtPropertySetterSymbol)?.parameter
                    if (propertyParameter != null) {
                        builder.addParameter(
                            SymbolLightSetterParameter(
                                ktAnalysisSession = this,
                                containingPropertySymbolPointer = containingPropertySymbolPointer,
                                parameterSymbol = propertyParameter,
                                containingMethod = this@SymbolLightAccessorMethod,
                            )
                        )
                    }
                }
            }
        } else {
            { }
        }

        SymbolLightParameterList(
            parent = this@SymbolLightAccessorMethod,
            callableWithReceiverSymbolPointer = containingPropertySymbolPointer,
            parameterPopulator = parameterPopulator,
        )
    }

    override fun getParameterList(): PsiParameterList = _parametersList

    override fun isValid(): Boolean =
        super.isValid() && propertyAccessorDeclaration?.isValid ?: propertyAccessorSymbolPointer.isValid(ktModule)

    private val _isOverride: Boolean by lazyPub {
        if (isTopLevel) false else analyzeForLightClasses(ktModule) { propertyAccessorSymbol().isOverride }
    }

    override fun isOverride(): Boolean = _isOverride

    private val _defaultValue: PsiAnnotationMemberValue? by lazyPub {
        if (!containingClass.isAnnotationType) return@lazyPub null

        analyzeForLightClasses(ktModule) {
            when (val initializer = propertySymbol().initializer) {
                is KtConstantInitializerValue -> initializer.constant.createPsiLiteral(this@SymbolLightAccessorMethod)
                is KtConstantValueForAnnotation -> initializer.annotationValue.toAnnotationMemberValue(this@SymbolLightAccessorMethod)
                is KtNonConstantInitializerValue -> null
                null -> null
            }
        }
    }

    override fun getDefaultValue(): PsiAnnotationMemberValue? = _defaultValue

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
