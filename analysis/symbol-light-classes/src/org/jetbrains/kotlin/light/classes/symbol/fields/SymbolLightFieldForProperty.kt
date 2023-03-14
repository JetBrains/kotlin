/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.hasAnnotation
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.name.JvmNames.TRANSIENT_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.VOLATILE_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtProperty

internal class SymbolLightFieldForProperty private constructor(
    private val propertySymbolPointer: KtSymbolPointer<KtPropertySymbol>,
    private val fieldName: String,
    containingClass: SymbolLightClassBase,
    lightMemberOrigin: LightMemberOrigin?,
    private val isTopLevel: Boolean,
    private val forceStatic: Boolean,
    private val takePropertyVisibility: Boolean,
    override val kotlinOrigin: KtCallableDeclaration?,
) : SymbolLightField(containingClass, lightMemberOrigin) {
    internal constructor(
        ktAnalysisSession: KtAnalysisSession,
        propertySymbol: KtPropertySymbol,
        fieldName: String,
        containingClass: SymbolLightClassBase,
        lightMemberOrigin: LightMemberOrigin?,
        isTopLevel: Boolean,
        forceStatic: Boolean,
        takePropertyVisibility: Boolean,
    ) : this(
        propertySymbolPointer = with(ktAnalysisSession) { propertySymbol.createPointer() },
        fieldName = fieldName,
        containingClass = containingClass,
        lightMemberOrigin = lightMemberOrigin,
        isTopLevel = isTopLevel,
        forceStatic = forceStatic,
        takePropertyVisibility = takePropertyVisibility,
        kotlinOrigin = propertySymbol.sourcePsiSafe<KtCallableDeclaration>(),
    )

    private inline fun <T> withPropertySymbol(crossinline action: context (KtAnalysisSession) (KtPropertySymbol) -> T): T {
        return propertySymbolPointer.withSymbol(ktModule, action)
    }

    private val _returnedType: PsiType by lazyPub {
        withPropertySymbol { propertySymbol ->
            val isDelegated = (propertySymbol as? KtKotlinPropertySymbol)?.isDelegatedProperty == true
            val ktType = if (isDelegated)
                (kotlinOrigin as? KtProperty)?.delegateExpression?.getKtType()
            else
                propertySymbol.returnType
            ktType?.asPsiType(
                this@SymbolLightFieldForProperty,
                allowErrorTypes = true,
                KtTypeMappingMode.RETURN_TYPE
            )
        } ?: nonExistentType()
    }

    private val _isDeprecated: Boolean by lazyPub {
        withPropertySymbol { propertySymbol ->
            propertySymbol.hasDeprecatedAnnotation(AnnotationUseSiteTarget.FIELD.toOptionalFilter())
        }
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) || isOriginEquivalentTo(another)
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getType(): PsiType = _returnedType

    override fun getName(): String = fieldName

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in GranularModifiersBox.VISIBILITY_MODIFIERS -> GranularModifiersBox.computeVisibilityForMember(ktModule, propertySymbolPointer)
        in GranularModifiersBox.MODALITY_MODIFIERS -> {
            val modality = withPropertySymbol { propertySymbol ->
                if (propertySymbol.isVal || propertySymbol.isDelegatedProperty) {
                    PsiModifier.FINAL
                } else {
                    propertySymbol.computeSimpleModality()?.takeIf { it != PsiModifier.FINAL }
                }
            }

            GranularModifiersBox.MODALITY_MODIFIERS_MAP.with(modality)
        }

        PsiModifier.STATIC -> {
            val isStatic = forceStatic || isTopLevel
            mapOf(modifier to isStatic)
        }

        PsiModifier.VOLATILE -> withPropertySymbol { propertySymbol ->
            val hasAnnotation = propertySymbol.hasAnnotation(
                VOLATILE_ANNOTATION_CLASS_ID,
                AnnotationUseSiteTarget.FIELD.toOptionalFilter(),
            )

            mapOf(modifier to hasAnnotation)
        }

        PsiModifier.TRANSIENT -> withPropertySymbol { propertySymbol ->
            val hasAnnotation = propertySymbol.hasAnnotation(
                TRANSIENT_ANNOTATION_CLASS_ID,
                AnnotationUseSiteTarget.FIELD.toOptionalFilter(),
            )

            mapOf(modifier to hasAnnotation)
        }

        else -> null
    }

    private val _modifierList: PsiModifierList by lazyPub {
        val initializerValue = if (takePropertyVisibility) {
            emptyMap()
        } else {
            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(PsiModifier.PRIVATE)
        }

        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(initializerValue, ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = propertySymbolPointer,
                    annotationUseSiteTargetFilter = AnnotationUseSiteTarget.FIELD.toOptionalFilter(),
                ),
                additionalAnnotationsProvider = NullabilityAnnotationsProvider {
                    withPropertySymbol { propertySymbol ->
                        when {
                            propertySymbol.isDelegatedProperty -> NullabilityType.NotNull
                            !(propertySymbol is KtKotlinPropertySymbol && propertySymbol.isLateInit) -> getTypeNullability(propertySymbol.returnType)
                            else -> NullabilityType.Unknown
                        }
                    }
                }
            ),
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _initializerValue: KtConstantValue? by lazyPub {
        withPropertySymbol { propertySymbol ->
            if (propertySymbol !is KtKotlinPropertySymbol) return@withPropertySymbol null
            (propertySymbol.initializer as? KtConstantInitializerValue)?.constant
        }
    }

    private val _initializer by lazyPub {
        _initializerValue?.createPsiExpression(this)
    }

    override fun getInitializer(): PsiExpression? = _initializer

    private val _constantValue by lazyPub {
        _initializerValue?.value?.takeIf {
            withPropertySymbol { propertySymbol ->
                // val => final
                propertySymbol.isVal &&
                        // NB: not as?, since _initializerValue already checks that
                        (propertySymbol as KtKotlinPropertySymbol).isConst &&
                        // javac rejects all non-primitive and non String constants
                        (propertySymbol.returnType.isPrimitiveBacked || propertySymbol.returnType.isString)
            }
        }
    }

    override fun computeConstantValue(): Any? = _constantValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightFieldForProperty || other.ktModule != ktModule || other.fieldName != fieldName) return false
        if (kotlinOrigin != null || other.kotlinOrigin != null) {
            return kotlinOrigin == other.kotlinOrigin
        }

        return containingClass == other.containingClass &&
                compareSymbolPointers(propertySymbolPointer, other.propertySymbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: fieldName.hashCode()

    override fun isValid(): Boolean = super.isValid() && kotlinOrigin?.isValid ?: propertySymbolPointer.isValid(ktModule)
}
