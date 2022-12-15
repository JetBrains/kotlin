/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasInlineOnlyAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.LazyModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.name.JvmNames.STRICTFP_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.SYNCHRONIZED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.util.*

internal class SymbolLightSimpleMethod(
    ktAnalysisSession: KtAnalysisSession,
    functionSymbol: KtFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val isTopLevel: Boolean,
    argumentsSkipMask: BitSet? = null,
    private val suppressStatic: Boolean = false,
) : SymbolLightMethod<KtFunctionSymbol>(
    ktAnalysisSession = ktAnalysisSession,
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask,
) {
    private val _name: String by lazyPub {
        withFunctionSymbol { functionSymbol ->
            functionSymbol.computeJvmMethodName(functionSymbol.name.asString(), containingClass, annotationUseSiteTarget = null)
        }
    }

    override fun getName(): String = _name

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        hasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterPointer = functionSymbolPointer,
                ktModule = ktModule,
                ktDeclaration = functionDeclaration,
            )
        }
    }

    override fun hasTypeParameters(): Boolean = hasTypeParameters(ktModule, functionDeclaration, functionSymbolPointer)

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    private fun computeAnnotations(
        modifierList: PsiModifierList,
    ): List<PsiAnnotation> = withFunctionSymbol { functionSymbol ->
        val nullability = when {
            modifierList.hasModifierProperty(PsiModifier.PRIVATE) -> NullabilityType.Unknown
            functionSymbol.isSuspend -> /* Any? */ NullabilityType.Nullable
            else -> {
                val returnType = functionSymbol.returnType
                if (returnType.isVoidType) NullabilityType.Unknown else getTypeNullability(returnType)
            }
        }

        functionSymbol.computeAnnotations(
            modifierList = modifierList,
            nullability = nullability,
            annotationUseSiteTarget = null,
        )
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in LazyModifiersBox.MODALITY_MODIFIERS -> {
            ifInlineOnly { return modifiersForInlineOnlyCase() }
            val modality = if (isTopLevel) {
                PsiModifier.FINAL
            } else {
                withFunctionSymbol { functionSymbol ->
                    functionSymbol.computeSimpleModality()?.takeUnless { it.isSuppressedFinalModifier(containingClass, functionSymbol) }
                }
            }

            LazyModifiersBox.MODALITY_MODIFIERS_MAP.with(modality)
        }

        in LazyModifiersBox.VISIBILITY_MODIFIERS -> {
            ifInlineOnly { return modifiersForInlineOnlyCase() }
            LazyModifiersBox.computeVisibilityForMember(ktModule, functionSymbolPointer)
        }

        PsiModifier.STATIC -> {
            ifInlineOnly { return null }
            val isStatic = if (suppressStatic) {
                false
            } else {
                isTopLevel || withFunctionSymbol { it.isStatic || it.hasJvmStaticAnnotation() }
            }

            mapOf(modifier to isStatic)
        }

        PsiModifier.NATIVE -> {
            ifInlineOnly { return null }
            val isExternal = functionDeclaration?.hasModifier(KtTokens.EXTERNAL_KEYWORD) ?: withFunctionSymbol { it.isExternal }
            mapOf(modifier to isExternal)
        }

        PsiModifier.STRICTFP -> {
            ifInlineOnly { return null }
            val hasAnnotation = withFunctionSymbol { it.hasAnnotation(STRICTFP_ANNOTATION_CLASS_ID, null) }
            mapOf(modifier to hasAnnotation)
        }

        PsiModifier.SYNCHRONIZED -> {
            ifInlineOnly { return null }
            val hasAnnotation = withFunctionSymbol { it.hasAnnotation(SYNCHRONIZED_ANNOTATION_CLASS_ID, null) }
            mapOf(modifier to hasAnnotation)
        }

        else -> null
    }

    private inline fun ifInlineOnly(action: () -> Unit) {
        if (hasInlineOnlyAnnotation) {
            action()
        }
    }

    private fun modifiersForInlineOnlyCase(): PersistentMap<String, Boolean> = LazyModifiersBox.MODALITY_MODIFIERS_MAP.mutate {
        it.putAll(LazyModifiersBox.VISIBILITY_MODIFIERS_MAP)
        it[PsiModifier.FINAL] = true
        it[PsiModifier.PRIVATE] = true
    }

    private val hasInlineOnlyAnnotation: Boolean by lazyPub { withFunctionSymbol { it.hasInlineOnlyAnnotation() } }

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            lazyModifiersComputer = ::computeModifiers,
            annotationsComputer = ::computeAnnotations,
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val KtType.isVoidType: Boolean get() = isUnit && nullabilityType != NullabilityType.Nullable

    private val _returnedType: PsiType by lazyPub {
        withFunctionSymbol { functionSymbol ->
            val ktType = if (functionSymbol.isSuspend) {
                analysisSession.builtinTypes.NULLABLE_ANY // Any?
            } else {
                functionSymbol.returnType.takeUnless { it.isVoidType } ?: return@withFunctionSymbol PsiType.VOID
            }

            ktType.asPsiType(
                this@SymbolLightSimpleMethod,
                KtTypeMappingMode.RETURN_TYPE,
                containingClass.isAnnotationType,
            )
        } ?: nonExistentType()
    }

    override fun getReturnType(): PsiType = _returnedType
}
