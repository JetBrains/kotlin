/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.name.JvmNames.TRANSIENT_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.VOLATILE_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty

context(KtAnalysisSession)
internal class SymbolLightFieldForProperty(
    private val propertySymbol: KtPropertySymbol,
    private val fieldName: String,
    containingClass: SymbolLightClassBase,
    lightMemberOrigin: LightMemberOrigin?,
    isTopLevel: Boolean,
    forceStatic: Boolean,
    takePropertyVisibility: Boolean
) : SymbolLightField(containingClass, lightMemberOrigin) {

    override val kotlinOrigin: KtDeclaration? = propertySymbol.psi as? KtDeclaration

    private val _returnedType: PsiType by lazyPub {
        val isDelegated = (propertySymbol as? KtKotlinPropertySymbol)?.isDelegatedProperty == true
        when {
            isDelegated ->
                (kotlinOrigin as? KtProperty)?.delegateExpression?.let {
                    it.getKtType()?.asPsiType(this@SymbolLightFieldForProperty, KtTypeMappingMode.RETURN_TYPE)
                }

            else -> {
                propertySymbol.returnType.asPsiType(this@SymbolLightFieldForProperty, KtTypeMappingMode.RETURN_TYPE)
            }
        } ?: nonExistentType()
    }

    private val _isDeprecated: Boolean by lazyPub {
        propertySymbol.hasDeprecatedAnnotation(AnnotationUseSiteTarget.FIELD)
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _identifier: PsiIdentifier by lazyPub {
        SymbolLightIdentifier(this, propertySymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun getType(): PsiType = _returnedType

    override fun getName(): String = fieldName

    private val _modifierList: PsiModifierList by lazyPub {

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

        val visibility =
            if (takePropertyVisibility) propertySymbol.toPsiVisibilityForMember() else PsiModifier.PRIVATE
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
            parent = this,
            nullability = nullability,
            annotationUseSiteTarget = AnnotationUseSiteTarget.FIELD,
        )

        SymbolLightMemberModifierList(this, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _initializerValue by lazyPub {
        if (propertySymbol !is KtKotlinPropertySymbol) return@lazyPub null
        (propertySymbol.initializer as? KtConstantInitializerValue)?.constant
    }

    private val _initializer by lazyPub {
        _initializerValue?.createPsiLiteral(this)
    }

    override fun getInitializer(): PsiExpression? = _initializer

    private val _constantValue by lazyPub {
        _initializerValue?.value?.takeIf {
            // val => final
            propertySymbol.isVal &&
                    // NB: not as?, since _initializerValue already checks that
                    (propertySymbol as KtKotlinPropertySymbol).isConst &&
                    // javac rejects all non-primitive and non String constants
                    (propertySymbol.returnType.isPrimitive || propertySymbol.returnType.isString)
        }
    }

    override fun computeConstantValue(): Any? = _constantValue

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is SymbolLightFieldForProperty &&
                        kotlinOrigin == other.kotlinOrigin &&
                        propertySymbol == other.propertySymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = super.isValid() && propertySymbol.isValid()
}
