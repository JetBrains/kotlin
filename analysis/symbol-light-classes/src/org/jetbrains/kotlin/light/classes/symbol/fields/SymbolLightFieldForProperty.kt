/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import kotlinx.collections.immutable.persistentHashMapOf
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
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
    private val isStatic: Boolean,
    override val kotlinOrigin: KtCallableDeclaration?,
    private val backingFieldSymbolPointer: KtSymbolPointer<KtBackingFieldSymbol>?,
) : SymbolLightField(containingClass, lightMemberOrigin) {
    internal constructor(
        ktAnalysisSession: KtAnalysisSession,
        propertySymbol: KtPropertySymbol,
        fieldName: String,
        containingClass: SymbolLightClassBase,
        lightMemberOrigin: LightMemberOrigin?,
        isStatic: Boolean,
    ) : this(
        propertySymbolPointer = with(ktAnalysisSession) { propertySymbol.createPointer() },
        fieldName = fieldName,
        containingClass = containingClass,
        lightMemberOrigin = lightMemberOrigin,
        isStatic = isStatic,
        kotlinOrigin = propertySymbol.sourcePsiSafe<KtCallableDeclaration>(),
        backingFieldSymbolPointer = with(ktAnalysisSession) { propertySymbol.backingFieldSymbol?.createPointer() },
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
            // See [KotlinTypeMapper#writeFieldSignature]
            val typeMappingMode = if (propertySymbol.isVal)
                KtTypeMappingMode.RETURN_TYPE
            else
                KtTypeMappingMode.VALUE_PARAMETER
            ktType?.asPsiType(
                this@SymbolLightFieldForProperty,
                allowErrorTypes = true,
                typeMappingMode,
            )
        } ?: nonExistentType()
    }

    private val _isDeprecated: Boolean by lazyPub {
        withPropertySymbol { propertySymbol ->
            propertySymbol.hasDeprecatedAnnotation(AnnotationUseSiteTarget.FIELD.toOptionalFilter())
        }
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) ||
                basicIsEquivalentTo(this, another as? PsiMethod) ||
                isOriginEquivalentTo(another)
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getType(): PsiType = _returnedType

    override fun getName(): String = fieldName

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in GranularModifiersBox.VISIBILITY_MODIFIERS -> {
            val visibility = withPropertySymbol { propertySymbol ->
                when {
                    propertySymbol.visibility.isPrivateOrPrivateToThis() -> PsiModifier.PRIVATE
                    propertySymbol.canHaveNonPrivateField -> {
                        val declaration = propertySymbol.setter ?: propertySymbol
                        declaration.toPsiVisibilityForMember()
                    }
                    else -> PsiModifier.PRIVATE
                }
            }

            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(visibility)
        }
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

        PsiModifier.VOLATILE -> withPropertySymbol { propertySymbol ->
            val hasAnnotation = propertySymbol.backingFieldSymbol?.hasAnnotation(
                VOLATILE_ANNOTATION_CLASS_ID,
                AnnotationUseSiteTarget.FIELD.toOptionalFilter(),
            ) == true

            mapOf(modifier to hasAnnotation)
        }

        PsiModifier.TRANSIENT -> withPropertySymbol { propertySymbol ->
            val hasAnnotation = propertySymbol.backingFieldSymbol?.hasAnnotation(
                TRANSIENT_ANNOTATION_CLASS_ID,
                AnnotationUseSiteTarget.FIELD.toOptionalFilter(),
            ) == true

            mapOf(modifier to hasAnnotation)
        }

        else -> null
    }

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(
                initialValue = persistentHashMapOf(PsiModifier.STATIC to isStatic),
                computer = ::computeModifiers,
            ),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = backingFieldSymbolPointer ?: propertySymbolPointer,
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
        _initializerValue?.createPsiExpression(this) ?: withPropertySymbol { propertySymbol ->
            if (propertySymbol !is KtKotlinPropertySymbol) return@withPropertySymbol null
            (kotlinOrigin as? KtProperty)?.initializer?.evaluateAsAnnotationValue()
                ?.let(::toPsiExpression)
        }
    }

    private fun toPsiExpression(value: KtAnnotationValue): PsiExpression? =
        project.withElementFactorySafe {
            when (value) {
                is KtConstantAnnotationValue ->
                    value.constantValue.createPsiExpression(this@SymbolLightFieldForProperty)
                is KtEnumEntryAnnotationValue ->
                    value.callableId?.let { createExpressionFromText(it.asSingleFqName().asString(), this@SymbolLightFieldForProperty) }
                is KtArrayAnnotationValue ->
                    createExpressionFromText(
                        value.values
                            .map { toPsiExpression(it)?.text ?: return@withElementFactorySafe null }
                            .joinToString(", ", "{", "}"),
                        this@SymbolLightFieldForProperty
                    )
                else -> null
            }
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
