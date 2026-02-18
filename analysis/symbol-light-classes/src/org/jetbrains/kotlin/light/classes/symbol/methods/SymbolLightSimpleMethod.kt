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
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
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
    functionSymbol: KaNamedFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val isTopLevel: Boolean,
    valueParameterPickMask: BitSet?,
    private val suppressStatic: Boolean,
    isJvmExposedBoxed: Boolean,
) : SymbolLightMethod<KaNamedFunctionSymbol>(
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    valueParameterPickMask = valueParameterPickMask,
    isJvmExposedBoxed = isJvmExposedBoxed,
) {
    private val _name: String by lazyPub {
        withFunctionSymbol { functionSymbol ->
            val defaultName = functionSymbol.name.asString()
            if (isJvmExposedBoxed) {
                computeJvmExposeBoxedMethodName(functionSymbol, defaultName)
            } else {
                computeJvmMethodName(functionSymbol, defaultName)
            }
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

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(computer = ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = functionSymbolPointer,
                ),
                annotationFilter = jvmExposeBoxedAwareAnnotationFilter,
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
                                    shouldEnforceBoxedReturnType(functionSymbol) -> {
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
                    JvmExposeBoxedAdditionalAnnotationsProvider,
                ),
            )
        )
    }

    override fun isConstructor(): Boolean = false

    override fun isOverride(): Boolean = _isOverride

    private val _isOverride: Boolean by lazyPub {
        withFunctionSymbol { it.isOverride }
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

            val typeMappingMode = if (shouldEnforceBoxedReturnType(functionSymbol))
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
        /**
         * @param suppressValueClass whether suppress the [containingClass] check for [isValueClass]
         * @param staticsFromCompanion whether this function was called to materialize static members from a companion object
         *  * inside the containing class
         */
        internal fun KaSession.createSimpleMethods(
            containingClass: SymbolLightClassBase,
            result: MutableList<PsiMethod>,
            functionSymbol: KaNamedFunctionSymbol,
            lightMemberOrigin: LightMemberOrigin?,
            methodIndex: Int,
            isTopLevel: Boolean,
            suppressStatic: Boolean = false,
            suppressValueClass: Boolean = false,
            staticsFromCompanion: Boolean = false,
        ) {
            ProgressManager.checkCanceled()

            if (functionSymbol.name.isSpecial || functionSymbol.hasReifiedParameters || isHiddenOrSynthetic(functionSymbol)) return
            if (staticsFromCompanion && !functionSymbol.hasJvmStaticAnnotation()) return

            val hasJvmNameAnnotation = functionSymbol.hasJvmNameAnnotation()
            val exposeBoxedMode = jvmExposeBoxedMode(functionSymbol)
            val hasValueClassInReturnType = hasValueClassInReturnType(functionSymbol)

            val isNonMaterializableValueClassFunction = !suppressValueClass &&
                    // Static methods should be materialized even inside value classes if possible
                    !staticsFromCompanion &&
                    containingClass.isValueClass &&
                    // Overrides are materialized by default
                    !functionSymbol.isOverride

            val isSuspend = functionSymbol.isSuspend
            val isOverridable = functionSymbol.isOverridable()
            createMethodsJvmOverloadsAware(
                declaration = functionSymbol,
                methodIndexBase = methodIndex,
            ) { methodIndex, valueParameterPickMask, hasValueClassInParameterType ->
                val hasMangledNameDueValueClassesInSignature = hasMangledNameDueValueClassesInSignature(
                    hasValueClassInParameterType = hasValueClassInParameterType,
                    hasValueClassInReturnType = hasValueClassInReturnType,
                    isTopLevel = isTopLevel,
                )

                val generationResult = methodGeneration(
                    exposeBoxedMode = exposeBoxedMode,
                    hasValueClassInParameterType = hasValueClassInParameterType,
                    hasValueClassInReturnType = hasValueClassInReturnType,
                    isAffectedByValueClass = hasMangledNameDueValueClassesInSignature || isNonMaterializableValueClassFunction,
                    hasJvmNameAnnotation = hasJvmNameAnnotation,
                    isSuspend = isSuspend,
                    isOverridable = isOverridable
                )

                if (generationResult.isBoxedMethodRequired) {
                    result += SymbolLightSimpleMethod(
                        functionSymbol = functionSymbol,
                        lightMemberOrigin = lightMemberOrigin,
                        containingClass = containingClass,
                        methodIndex = methodIndex,
                        isTopLevel = isTopLevel,
                        valueParameterPickMask = valueParameterPickMask,
                        suppressStatic = suppressStatic,
                        isJvmExposedBoxed = true,
                    )
                }

                if (generationResult.isRegularMethodRequired) {
                    result += SymbolLightSimpleMethod(
                        functionSymbol = functionSymbol,
                        lightMemberOrigin = lightMemberOrigin,
                        containingClass = containingClass,
                        methodIndex = methodIndex,
                        isTopLevel = isTopLevel,
                        valueParameterPickMask = valueParameterPickMask,
                        suppressStatic = suppressStatic,
                        isJvmExposedBoxed = false,
                    )
                }
            }
        }
    }
}
