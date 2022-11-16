/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.name.JvmNames.TRANSIENT_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.VOLATILE_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtProperty

internal class SymbolLightFieldForProperty(
    propertySymbol: KtPropertySymbol,
    private val fieldName: String,
    containingClass: SymbolLightClassBase,
    lightMemberOrigin: LightMemberOrigin?,
    isTopLevel: Boolean,
    forceStatic: Boolean,
    takePropertyVisibility: Boolean,
    private val ktModule: KtModule,
) : SymbolLightField(containingClass, lightMemberOrigin) {
    override val kotlinOrigin: KtCallableDeclaration? = propertySymbol.sourcePsiSafe<KtCallableDeclaration>()
    private val propertySymbolPointer: KtSymbolPointer<KtPropertySymbol> = propertySymbol.createPointer()

    private fun <T> withPropertySymbol(action: KtAnalysisSession.(KtPropertySymbol) -> T): T = analyzeForLightClasses(ktModule) {
        action(propertySymbolPointer.restoreSymbolOrThrowIfDisposed())
    }

    private val _returnedType: PsiType by lazyPub {
        withPropertySymbol { propertySymbol ->
            val isDelegated = (propertySymbol as? KtKotlinPropertySymbol)?.isDelegatedProperty == true
            when {
                isDelegated ->
                    (kotlinOrigin as? KtProperty)?.delegateExpression?.let {
                        it.getKtType()?.asPsiType(this@SymbolLightFieldForProperty, KtTypeMappingMode.RETURN_TYPE)
                    }

                else -> propertySymbol.returnType.asPsiType(this@SymbolLightFieldForProperty, KtTypeMappingMode.RETURN_TYPE)
            } ?: nonExistentType()
        }
    }

    private val _isDeprecated: Boolean by lazyPub {
        withPropertySymbol { propertySymbol ->
            propertySymbol.hasDeprecatedAnnotation(AnnotationUseSiteTarget.FIELD)
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getType(): PsiType = _returnedType

    override fun getName(): String = fieldName

    private val _modifierList: PsiModifierList by lazyPub {
        withPropertySymbol { propertySymbol ->
            val modifiers = mutableSetOf<String>()

            val suppressFinal = !propertySymbol.isVal

            propertySymbol.computeModalityForMethod(
                isTopLevel = isTopLevel,
                suppressFinal = suppressFinal,
                result = modifiers
            )

            if (forceStatic) {
                modifiers.add(PsiModifier.STATIC)
            }

            val visibility = if (takePropertyVisibility) propertySymbol.toPsiVisibilityForMember() else PsiModifier.PRIVATE
            modifiers.add(visibility)

            if (!suppressFinal) {
                modifiers.add(PsiModifier.FINAL)
            }

            if (propertySymbol.hasAnnotation(TRANSIENT_ANNOTATION_CLASS_ID, null)) {
                modifiers.add(PsiModifier.TRANSIENT)
            }

            if (propertySymbol.hasAnnotation(VOLATILE_ANNOTATION_CLASS_ID, null)) {
                modifiers.add(PsiModifier.VOLATILE)
            }

            val nullability = if (!(propertySymbol is KtKotlinPropertySymbol && propertySymbol.isLateInit)) {
                getTypeNullability(propertySymbol.returnType)
            } else NullabilityType.Unknown

            val annotations = propertySymbol.computeAnnotations(
                parent = this@SymbolLightFieldForProperty,
                nullability = nullability,
                annotationUseSiteTarget = AnnotationUseSiteTarget.FIELD,
            )

            SymbolLightMemberModifierList(this@SymbolLightFieldForProperty, modifiers, annotations)
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _initializerValue: KtConstantValue? by lazyPub {
        withPropertySymbol { propertySymbol ->
            if (propertySymbol !is KtKotlinPropertySymbol) return@withPropertySymbol null
            (propertySymbol.initializer as? KtConstantInitializerValue)?.constant
        }
    }

    private val _initializer by lazyPub {
        _initializerValue?.createPsiLiteral(this)
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
                        (propertySymbol.returnType.isPrimitive || propertySymbol.returnType.isString)
            }
        }
    }

    override fun computeConstantValue(): Any? = _constantValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightFieldForProperty) return false
        if (kotlinOrigin != null) {
            return kotlinOrigin == other.kotlinOrigin && ktModule == other.ktModule
        }

        return other.kotlinOrigin == null &&
                fieldName == other.fieldName &&
                containingClass == other.containingClass &&
                ktModule == other.ktModule &&
                analyzeForLightClasses(ktModule) {
                    propertySymbolPointer.restoreSymbol() == other.propertySymbolPointer.restoreSymbol()
                }
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: fieldName.hashCode()

    override fun isValid(): Boolean = super.isValid() && kotlinOrigin?.isValid ?: analyzeForLightClasses(ktModule) {
        propertySymbolPointer.restoreSymbol() != null
    }
}
