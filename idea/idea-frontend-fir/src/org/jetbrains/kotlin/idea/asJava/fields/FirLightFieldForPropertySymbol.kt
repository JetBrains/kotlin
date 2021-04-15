/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.FirLightIdentifier
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSimpleConstantValue
import org.jetbrains.kotlin.psi.KtDeclaration

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
        propertySymbol.annotatedType.asPsiType(
            propertySymbol,
            this@FirLightFieldForPropertySymbol,
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )
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

        val nullability = if (!(propertySymbol is KtKotlinPropertySymbol && propertySymbol.isLateInit))
            propertySymbol.annotatedType.type.getTypeNullability(propertySymbol, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        else NullabilityType.Unknown

        val annotations = propertySymbol.computeAnnotations(
            parent = this,
            nullability = nullability,
            annotationUseSiteTarget = AnnotationUseSiteTarget.FIELD,
        )

        FirLightClassModifierList(this, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _initializer by lazyPub {
        if (propertySymbol !is KtKotlinPropertySymbol) return@lazyPub null
        if (!propertySymbol.isConst) return@lazyPub null
        if (!propertySymbol.isVal) return@lazyPub null
        (propertySymbol.initializer as? KtSimpleConstantValue<*>)?.createPsiLiteral(this)
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