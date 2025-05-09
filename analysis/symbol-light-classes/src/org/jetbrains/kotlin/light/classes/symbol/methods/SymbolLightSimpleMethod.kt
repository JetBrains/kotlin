/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.classes.*
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.name.JvmStandardClassIds.STRICTFP_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.SYNCHRONIZED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.util.*

internal class SymbolLightSimpleMethod private constructor(
    ktAnalysisSession: KaSession,
    functionSymbol: KaNamedFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val isTopLevel: Boolean,
    argumentsSkipMask: BitSet?,
    private val suppressStatic: Boolean,
) : SymbolLightMethod<KaNamedFunctionSymbol>(
    ktAnalysisSession = ktAnalysisSession,
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask,
) {
    private val _name: String by lazyPub {
        withFunctionSymbol { functionSymbol ->
            computeJvmMethodName(
                symbol = functionSymbol,
                defaultName = functionSymbol.name.asString(),
            )
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

    override fun hasTypeParameters(): Boolean {
        return withFunctionSymbol { it.typeParameters.isNotEmpty() } || containingClass.isDefaultImplsForInterfaceWithTypeParameters
    }

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in GranularModifiersBox.MODALITY_MODIFIERS -> {
            ifInlineOnly { return modifiersForInlineOnlyCase() }
            val modality = when {
                isTopLevel -> PsiModifier.FINAL
                containingClass is SymbolLightClassForInterfaceDefaultImpls -> null
                else -> withFunctionSymbol { functionSymbol ->
                    functionSymbol.computeSimpleModality()?.takeUnless { isSuppressedFinalModifier(it, containingClass, functionSymbol) }
                }
            }

            GranularModifiersBox.MODALITY_MODIFIERS_MAP.with(modality)
        }

        in GranularModifiersBox.VISIBILITY_MODIFIERS -> {
            ifInlineOnly { return modifiersForInlineOnlyCase() }
            GranularModifiersBox.computeVisibilityForMember(ktModule, functionSymbolPointer)
        }

        PsiModifier.STATIC -> {
            ifInlineOnly { return null }
            val isStatic = if (suppressStatic) {
                false
            } else {
                isTopLevel
                        || containingClass is SymbolLightClassForInterfaceDefaultImpls
                        || withFunctionSymbol { it.isStatic || it.hasJvmStaticAnnotation() }
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
            val hasAnnotation = withFunctionSymbol { STRICTFP_ANNOTATION_CLASS_ID in it.annotations }
            mapOf(modifier to hasAnnotation)
        }

        PsiModifier.SYNCHRONIZED -> {
            ifInlineOnly { return null }
            val hasAnnotation = withFunctionSymbol { SYNCHRONIZED_ANNOTATION_CLASS_ID in it.annotations }
            mapOf(modifier to hasAnnotation)
        }

        else -> null
    }

    private inline fun ifInlineOnly(action: () -> Unit) {
        if (hasInlineOnlyAnnotation) {
            action()
        }
    }

    private fun modifiersForInlineOnlyCase(): PersistentMap<String, Boolean> = GranularModifiersBox.MODALITY_MODIFIERS_MAP.mutate {
        it.putAll(GranularModifiersBox.VISIBILITY_MODIFIERS_MAP)
        it[PsiModifier.FINAL] = true
        it[PsiModifier.PRIVATE] = true
    }

    private val hasInlineOnlyAnnotation: Boolean by lazyPub { withFunctionSymbol { it.hasInlineOnlyAnnotation() } }

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(computer = ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = functionSymbolPointer,
                ),
                additionalAnnotationsProvider = CompositeAdditionalAnnotationsProvider(
                    NullabilityAnnotationsProvider {
                        if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
                            NullabilityAnnotation.NOT_REQUIRED
                        } else {
                            withFunctionSymbol { functionSymbol ->
                                when {
                                    functionSymbol.isSuspend -> { // Any?
                                        NullabilityAnnotation.NULLABLE
                                    }
                                    forceBoxedReturnType(functionSymbol) -> {
                                        NullabilityAnnotation.NON_NULLABLE
                                    }
                                    else -> {
                                        val returnType = functionSymbol.returnType
                                        if (isVoidType(returnType)) NullabilityAnnotation.NOT_REQUIRED else getRequiredNullabilityAnnotation(returnType)
                                    }
                                }
                            }
                        }
                    },
                    MethodAdditionalAnnotationsProvider,
                ),
            )
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    override fun isOverride(): Boolean = _isOverride

    private val _isOverride: Boolean by lazyPub {
        withFunctionSymbol { it.isOverride }
    }

    // Inspired by KotlinTypeMapper#forceBoxedReturnType
    private fun KaSession.forceBoxedReturnType(functionSymbol: KaNamedFunctionSymbol): Boolean {
        val returnType = functionSymbol.returnType
        // 'invoke' methods for lambdas, function literals, and callable references
        // implicitly override generic 'invoke' from a corresponding base class.
        if (functionSymbol.isBuiltinFunctionInvoke && isInlineClassType(returnType))
            return true

        return returnType.isPrimitiveBacked &&
                functionSymbol.allOverriddenSymbols.any { overriddenSymbol ->
                    !overriddenSymbol.returnType.isPrimitiveBacked
                }
    }

    @Suppress("UnusedReceiverParameter")
    private fun KaSession.isInlineClassType(type: KaType): Boolean {
        return ((type as? KaClassType)?.symbol as? KaNamedClassSymbol)?.isInline == true
    }

    private fun KaSession.isVoidType(type: KaType): Boolean {
        val expandedType = type.fullyExpandedType
        return expandedType.isUnitType && !expandedType.isMarkedNullable
    }

    private val _returnedType: PsiType by lazyPub {
        withFunctionSymbol { functionSymbol ->
            val ktType = if (functionSymbol.isSuspend) {
                useSiteSession.builtinTypes.nullableAny // Any?
            } else {
                functionSymbol.returnType.takeUnless { isVoidType(it) } ?: return@withFunctionSymbol PsiTypes.voidType()
            }

            val typeMappingMode = if (forceBoxedReturnType(functionSymbol))
                KaTypeMappingMode.RETURN_TYPE_BOXED
            else
                KaTypeMappingMode.RETURN_TYPE

            ktType.asPsiType(
                this@SymbolLightSimpleMethod,
                allowErrorTypes = true,
                typeMappingMode,
                this@SymbolLightSimpleMethod.containingClass.isAnnotationType,
                suppressWildcards = suppressWildcards(),
                allowNonJvmPlatforms = true,
            )
        } ?: nonExistentType()
    }

    override fun getReturnType(): PsiType = _returnedType

    companion object {
        internal fun KaSession.createSimpleMethods(
            containingClass: SymbolLightClassBase,
            result: MutableList<PsiMethod>,
            functionSymbol: KaNamedFunctionSymbol,
            lightMemberOrigin: LightMemberOrigin?,
            methodIndex: Int,
            isTopLevel: Boolean,
            suppressStatic: Boolean = false,
        ) {
            ProgressManager.checkCanceled()

            if (functionSymbol.name.isSpecial || functionSymbol.hasReifiedParameters || isHiddenOrSynthetic(functionSymbol)) return
            if (hasTypeForValueClassInSignature(functionSymbol, ignoreReturnType = isTopLevel, ignoreValueParameters = true)) return

            createMethodsJvmOverloadsAware(
                declaration = functionSymbol,
                result = result,
                skipValueClassParameters = true,
                methodIndexBase = methodIndex,
            ) { methodIndex, argumentSkipMask ->
                SymbolLightSimpleMethod(
                    ktAnalysisSession = this,
                    functionSymbol = functionSymbol,
                    lightMemberOrigin = lightMemberOrigin,
                    containingClass = containingClass,
                    methodIndex = methodIndex,
                    isTopLevel = isTopLevel,
                    argumentsSkipMask = argumentSkipMask,
                    suppressStatic = suppressStatic,
                )
            }
        }
    }
}
