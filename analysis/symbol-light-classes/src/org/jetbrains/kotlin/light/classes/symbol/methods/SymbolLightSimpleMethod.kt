/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
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
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.name.JvmNames.STRICTFP_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.SYNCHRONIZED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.util.*

internal class SymbolLightSimpleMethod(
    functionSymbol: KtFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val isTopLevel: Boolean,
    argumentsSkipMask: BitSet? = null,
    private val suppressStatic: Boolean = false
) : SymbolLightMethod<KtFunctionSymbol>(
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

    context(KtAnalysisSession)
    private fun computeAnnotations(functionSymbol: KtFunctionSymbol, isPrivate: Boolean): List<PsiAnnotation> {
        val nullability = when {
            isPrivate -> NullabilityType.Unknown
            functionSymbol.isSuspend -> /* Any? */ NullabilityType.Nullable
            else -> {
                val returnType = functionSymbol.returnType
                if (returnType.isVoidType) NullabilityType.Unknown else getTypeNullability(returnType)
            }
        }

        return functionSymbol.computeAnnotations(
            parent = this@SymbolLightSimpleMethod,
            nullability = nullability,
            annotationUseSiteTarget = null,
        )
    }

    context(KtAnalysisSession)
    private fun computeModifiers(functionSymbol: KtFunctionSymbol): Set<String> {
        if (functionSymbol.hasInlineOnlyAnnotation()) return setOf(PsiModifier.FINAL, PsiModifier.PRIVATE)

        val finalModifier = kotlinOrigin?.hasModifier(KtTokens.FINAL_KEYWORD) == true

        val modifiers = mutableSetOf<String>()

        functionSymbol.computeModalityForMethod(
            isTopLevel = isTopLevel,
            suppressFinal = containingClass.isInterface || (!finalModifier && functionSymbol.isOverride),
            result = modifiers
        )

        val visibility: String = functionSymbol.isOverride.ifTrue {
            tryGetEffectiveVisibility(functionSymbol)
                ?.toPsiVisibilityForMember()
        } ?: functionSymbol.toPsiVisibilityForMember()

        modifiers.add(visibility)

        if (!suppressStatic && functionSymbol.hasJvmStaticAnnotation()) {
            modifiers.add(PsiModifier.STATIC)
        }
        if (functionSymbol.hasAnnotation(STRICTFP_ANNOTATION_CLASS_ID, null)) {
            modifiers.add(PsiModifier.STRICTFP)
        }
        if (functionSymbol.hasAnnotation(SYNCHRONIZED_ANNOTATION_CLASS_ID, null)) {
            modifiers.add(PsiModifier.SYNCHRONIZED)
        }

        return modifiers
    }

    private val _modifierList: PsiModifierList by lazyPub {
        withFunctionSymbol { functionSymbol ->
            val modifiers = computeModifiers(functionSymbol)
            val annotations = computeAnnotations(functionSymbol, modifiers.contains(PsiModifier.PRIVATE))
            SymbolLightMemberModifierList(this@SymbolLightSimpleMethod, modifiers, annotations)
        }
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
