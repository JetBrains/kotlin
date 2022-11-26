/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
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
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
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

    private fun <T> withPropertySymbol(action: context (KtAnalysisSession) (KtPropertySymbol) -> T): T {
        return propertySymbolPointer.withSymbol(ktModule, action)
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

    private val _isDeprecated: Boolean by lazy {
        withPropertySymbol { propertySymbol ->
            propertySymbol.hasDeprecatedAnnotation(AnnotationUseSiteTarget.FIELD)
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getType(): PsiType = _returnedType

    override fun getName(): String = fieldName

    private fun computeModifiers(): Set<String> = withPropertySymbol { propertySymbol ->
        buildSet {
            val suppressFinal = !propertySymbol.isVal

            propertySymbol.computeModalityForMethod(
                isTopLevel = isTopLevel,
                suppressFinal = suppressFinal,
                result = this
            )

            if (forceStatic) {
                add(PsiModifier.STATIC)
            }

            val visibility = if (takePropertyVisibility) propertySymbol.toPsiVisibilityForMember() else PsiModifier.PRIVATE
            add(visibility)

            if (!suppressFinal) {
                add(PsiModifier.FINAL)
            }

            if (propertySymbol.hasAnnotation(TRANSIENT_ANNOTATION_CLASS_ID, null)) {
                add(PsiModifier.TRANSIENT)
            }

            if (propertySymbol.hasAnnotation(VOLATILE_ANNOTATION_CLASS_ID, null)) {
                add(PsiModifier.VOLATILE)
            }
        }
    }

    private fun computeAnnotations(): List<PsiAnnotation> = withPropertySymbol { propertySymbol ->
        val nullability = if (!(propertySymbol is KtKotlinPropertySymbol && propertySymbol.isLateInit)) {
            getTypeNullability(propertySymbol.returnType)
        } else NullabilityType.Unknown

        propertySymbol.computeAnnotations(
            parent = this@SymbolLightFieldForProperty,
            nullability = nullability,
            annotationUseSiteTarget = AnnotationUseSiteTarget.FIELD,
        )
    }

    private val _modifierList: PsiModifierList by lazy {
        val lazyModifiers = lazyPub { computeModifiers() }
        val lazyAnnotations = lazyPub { computeAnnotations() }
        SymbolLightMemberModifierList(this, lazyModifiers, lazyAnnotations)
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
        if (other !is SymbolLightFieldForProperty || other.ktModule != ktModule || other.fieldName != fieldName) return false
        if (kotlinOrigin != null) {
            return kotlinOrigin == other.kotlinOrigin
        }

        return other.kotlinOrigin == null &&
                containingClass == other.containingClass &&
                compareSymbolPointers(ktModule, propertySymbolPointer, other.propertySymbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: fieldName.hashCode()

    override fun isValid(): Boolean = super.isValid() && kotlinOrigin?.isValid ?: propertySymbolPointer.isValid(ktModule)
}
