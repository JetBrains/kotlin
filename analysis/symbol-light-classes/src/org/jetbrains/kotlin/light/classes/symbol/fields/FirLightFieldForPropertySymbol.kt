/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.analysis.api.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtLiteralConstantValue
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty

internal class FirLightFieldForPropertySymbol(
    private val propertySymbol: KtPropertySymbol,
    private val fieldName: String,
    containingClass: FirLightClassBase,
    lightMemberOrigin: LightMemberOrigin?,
    isTopLevel: Boolean,
    forceStatic: Boolean,
    takePropertyVisibility: Boolean
) : FirLightField(containingClass, lightMemberOrigin) {

    override val kotlinOrigin: KtDeclaration? = propertySymbol.psi as? KtDeclaration

    private val _returnedType: PsiType by lazyPub {
        analyzeWithSymbolAsContext(propertySymbol) {
            val isDelegated = (propertySymbol as? KtKotlinPropertySymbol)?.isDelegatedProperty == true
            when {
                isDelegated ->
                    (kotlinOrigin as? KtProperty)?.delegateExpression?.let {
                        it.getKtType()?.asPsiType(this@FirLightFieldForPropertySymbol, KtTypeMappingMode.RETURN_TYPE)
                    }
                else -> {
                    propertySymbol.returnType.asPsiType(this@FirLightFieldForPropertySymbol, KtTypeMappingMode.RETURN_TYPE)
                }
            }
        } ?: nonExistentType()
    }

    private val _isDeprecated: Boolean by lazyPub {
        propertySymbol.hasDeprecatedAnnotation(AnnotationUseSiteTarget.FIELD)
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, propertySymbol)
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
            if (takePropertyVisibility) propertySymbol.toPsiVisibilityForMember(isTopLevel = false) else PsiModifier.PRIVATE
        modifiers.add(visibility)

        if (!suppressFinal) {
            modifiers.add(PsiModifier.FINAL)
        }
        if (propertySymbol.hasAnnotation("kotlin/jvm/Transient", null)) {
            modifiers.add(PsiModifier.TRANSIENT)
        }
        if (propertySymbol.hasAnnotation("kotlin/jvm/Volatile", null)) {
            modifiers.add(PsiModifier.VOLATILE)
        }

        val nullability = if (!(propertySymbol is KtKotlinPropertySymbol && propertySymbol.isLateInit)) {
            analyzeWithSymbolAsContext(propertySymbol) {
                getTypeNullability(propertySymbol.returnType)
            }
        } else NullabilityType.Unknown

        val annotations = propertySymbol.computeAnnotations(
            parent = this,
            nullability = nullability,
            annotationUseSiteTarget = AnnotationUseSiteTarget.FIELD,
        )

        FirLightMemberModifierList(this, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _initializer by lazyPub {
        if (propertySymbol !is KtKotlinPropertySymbol) return@lazyPub null
        if (!propertySymbol.isConst) return@lazyPub null
        if (!propertySymbol.isVal) return@lazyPub null
        val constInitializer = propertySymbol.initializer as? KtConstantInitializerValue ?: return@lazyPub null
        (constInitializer.constant as? KtLiteralConstantValue<*>)?.createPsiLiteral(this)
    }

    override fun getInitializer(): PsiExpression? = _initializer

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightFieldForPropertySymbol &&
                        kotlinOrigin == other.kotlinOrigin &&
                        propertySymbol == other.propertySymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = super.isValid() && propertySymbol.isValid()
}
