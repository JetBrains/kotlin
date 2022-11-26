/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightParameterListBuilder
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

    private val _typeParameterList: PsiTypeParameterList? by lazy {
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

    private fun computeAnnotations(isPrivate: Boolean): List<PsiAnnotation> = analyzeForLightClasses(ktModule) {
        val nullabilityApplicable = isGetter && !isPrivate && !(isParameter && containingClass.isAnnotationType)

        val propertySymbol = propertySymbol()
        val nullabilityType = if (nullabilityApplicable) getTypeNullability(propertySymbol.returnType) else NullabilityType.Unknown
        val annotationsFromProperty = propertySymbol.computeAnnotations(
            parent = this@SymbolLightAccessorMethod,
            nullability = nullabilityType,
            annotationUseSiteTarget = accessorSite,
            includeAnnotationsWithoutSite = false,
        )

        val propertyAccessorSymbol = propertyAccessorSymbol()
        val annotationsFromAccessor = propertyAccessorSymbol.computeAnnotations(
            parent = this@SymbolLightAccessorMethod,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = accessorSite,
        )

        annotationsFromProperty + annotationsFromAccessor
    }

    private fun computeModifiers(): Set<String> = analyzeForLightClasses(ktModule) {
        val propertySymbol = propertySymbol()
        val propertyAccessorSymbol = propertyAccessorSymbol()
        val isOverrideMethod = propertyAccessorSymbol.isOverride || propertySymbol.isOverride
        val isInterfaceMethod = containingClass.isInterface

        val modifiers = mutableSetOf<String>()

        propertySymbol.computeModalityForMethod(
            isTopLevel = isTopLevel,
            suppressFinal = isOverrideMethod || isInterfaceMethod,
            result = modifiers,
        )

        val visibility = isOverrideMethod.ifTrue {
            tryGetEffectiveVisibility(propertySymbol)?.toPsiVisibilityForMember()
        } ?: propertyAccessorSymbol.toPsiVisibilityForMember()
        modifiers.add(visibility)

        if (!suppressStatic &&
            (propertySymbol.hasJvmStaticAnnotation() || propertyAccessorSymbol.hasJvmStaticAnnotation(accessorSite))
        ) {
            modifiers.add(PsiModifier.STATIC)
        }

        if (isInterfaceMethod) {
            modifiers.add(PsiModifier.ABSTRACT)
        }

        modifiers
    }

    private val _modifierList: PsiModifierList by lazy {
        val lazyModifiers = lazyPub { computeModifiers() }
        val lazyAnnotations = lazyPub { computeAnnotations(PsiModifier.PRIVATE in lazyModifiers.value) }
        SymbolLightMemberModifierList(this, lazyModifiers, lazyAnnotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _isDeprecated: Boolean by lazy {
        analyzeForLightClasses(ktModule) {
            propertySymbol().hasDeprecatedAnnotation(accessorSite)
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _identifier: PsiIdentifier by lazy {
        KtLightIdentifier(this, containingPropertyDeclaration)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _returnedType: PsiType by lazyPub {
        if (!isGetter) return@lazyPub PsiType.VOID
        analyzeForLightClasses(ktModule) {
            propertySymbol().returnType.asPsiType(
                this@SymbolLightAccessorMethod,
                KtTypeMappingMode.RETURN_TYPE,
                containingClass.isAnnotationType,
            )
        } ?: nonExistentType()
    }

    override fun getReturnType(): PsiType = _returnedType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightAccessorMethod || other.ktModule != ktModule || other.isGetter != isGetter) return false
        if (propertyAccessorDeclaration != null) {
            return propertyAccessorDeclaration == other.propertyAccessorDeclaration
        }

        return other.propertyAccessorDeclaration == null &&
                compareSymbolPointers(ktModule, propertyAccessorSymbolPointer, other.propertyAccessorSymbolPointer)
    }

    override fun hashCode(): Int = propertyAccessorDeclaration.hashCode()

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

    override fun isOverride(): Boolean = analyzeForLightClasses(ktModule) { propertyAccessorSymbol().isOverride }

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
