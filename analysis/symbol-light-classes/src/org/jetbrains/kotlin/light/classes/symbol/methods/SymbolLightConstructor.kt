/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_DEFAULT_CTOR
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.lexer.KtTokens.INNER_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.SEALED_KEYWORD
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.*
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import java.util.*

internal class SymbolLightConstructor private constructor(
    constructorSymbol: KaConstructorSymbol,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    isJvmExposedBoxed: Boolean,
    valueParameterPickMask: BitSet? = null,
) : SymbolLightMethod<KaConstructorSymbol>(
    functionSymbol = constructorSymbol,
    lightMemberOrigin = null,
    containingClass = containingClass,
    methodIndex = methodIndex,
    isJvmExposedBoxed = isJvmExposedBoxed,
    valueParameterPickMask = valueParameterPickMask,
) {
    private val _name: String? = containingClass.name

    override fun getName(): String = _name ?: ""

    override fun isConstructor(): Boolean = true
    override fun isOverride(): Boolean = false

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getModifierList(): PsiModifierList = cachedValue {
        val initialValue = if (this.containingClass is SymbolLightClassForEnumEntry) {
            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(PsiModifier.PACKAGE_LOCAL)
        } else {
            emptyMap()
        }

        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(
                initialValue = initialValue,
                computer = ::computeModifiers,
            ),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = functionSymbolPointer,
                ),
                annotationFilter = jvmExposeBoxedAwareAnnotationFilter,
                additionalAnnotationsProvider = JvmExposeBoxedAdditionalAnnotationsProvider,
            ),
        )
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when {
        modifier !in GranularModifiersBox.VISIBILITY_MODIFIERS -> null
        (containingClass as? SymbolLightClassForNamedClassLike)?.isSealed == true ->
            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(PsiModifier.PRIVATE)

        else -> withFunctionSymbol { symbol ->
            val visibility = if (!isJvmExposedBoxed && hasValueClassInSignature(symbol, valueParameterPickMask = valueParameterPickMask)) {
                PsiModifier.PRIVATE
            } else {
                symbol.toPsiVisibilityForMember()
            }

            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(visibility)
        }
    }

    override fun getReturnType(): PsiType? = null

    companion object {
        internal fun KaSession.createConstructors(
            lightClass: SymbolLightClassBase,
            declarations: Sequence<KaConstructorSymbol>,
            result: MutableList<PsiMethod>,
        ) {
            val constructors = declarations.toList()
            if (constructors.isEmpty()) {
                result.add(lightClass.defaultConstructor())
                return
            }

            val destinationClassIsValueClass = lightClass.isValueClass
            for (constructor in constructors) {
                ProgressManager.checkCanceled()

                if (isHiddenOrSynthetic(constructor)) continue

                val exposeBoxedMode = jvmExposeBoxedMode(constructor)
                createMethodsJvmOverloadsAware(
                    declaration = constructor,
                    methodIndexBase = METHOD_INDEX_BASE,
                ) { methodIndex, valueParameterPickMask, hasValueClassInParameterType ->
                    if (exposeBoxedMode != JvmExposeBoxedMode.NONE && (hasValueClassInParameterType || destinationClassIsValueClass)) {
                        result += SymbolLightConstructor(
                            constructorSymbol = constructor,
                            containingClass = lightClass,
                            methodIndex = methodIndex,
                            valueParameterPickMask = valueParameterPickMask,
                            isJvmExposedBoxed = true,
                        )
                    }

                    if (!destinationClassIsValueClass) {
                        result += SymbolLightConstructor(
                            constructorSymbol = constructor,
                            containingClass = lightClass,
                            methodIndex = methodIndex,
                            valueParameterPickMask = valueParameterPickMask,
                            isJvmExposedBoxed = false,
                        )
                    }
                }
            }

            val primaryConstructor = constructors.singleOrNull { it.isPrimary }
            if (primaryConstructor != null && shouldGenerateNoArgOverload(lightClass, primaryConstructor, constructors)) {
                when {
                    !destinationClassIsValueClass && !hasValueClassInSignature(primaryConstructor) -> {
                        result += lightClass.noArgConstructor(
                            primaryConstructor = primaryConstructor,
                            isJvmExposedBoxed = false,
                        )
                    }

                    jvmExposeBoxedMode(primaryConstructor) != JvmExposeBoxedMode.NONE -> {
                        result += lightClass.noArgConstructor(
                            primaryConstructor = primaryConstructor,
                            isJvmExposedBoxed = true,
                        )
                    }
                }
            }
        }

        private fun shouldGenerateNoArgOverload(
            lightClass: SymbolLightClassBase,
            primaryConstructor: KaConstructorSymbol,
            constructors: Iterable<KaConstructorSymbol>,
        ): Boolean {
            val classOrObject = lightClass.kotlinOrigin ?: return false
            return !classOrObject.hasModifier(INNER_KEYWORD) &&
                    !classOrObject.hasModifier(SEALED_KEYWORD) &&
                    !lightClass.isEnum &&
                    primaryConstructor.valueParameters.all(KaValueParameterSymbol::hasDeclaredDefaultValue) &&
                    constructors.none { it.isEffectivelyParameterless } &&
                    primaryConstructor.visibility != KaSymbolVisibility.PRIVATE
        }

        /**
         * Whether the constructor either has no arguments or has [JvmOverloads] which would result in a method with no arguments.
         * */
        private val KaConstructorSymbol.isEffectivelyParameterless: Boolean
            get() = valueParameters.isEmpty() ||
                    valueParameters.all(KaValueParameterSymbol::hasDeclaredDefaultValue) && hasJvmOverloadsAnnotation()

        private fun SymbolLightClassBase.defaultConstructor(): KtLightMethod {
            val classOrObject = kotlinOrigin
            val visibility = when {
                this is SymbolLightClassForClassLike<*> && (classKind().let { it.isObject || it == KaClassKind.ENUM_CLASS }) -> PsiModifier.PRIVATE
                classOrObject?.hasModifier(SEALED_KEYWORD) == true -> PsiModifier.PROTECTED
                this is SymbolLightClassForEnumEntry -> PsiModifier.PACKAGE_LOCAL
                else -> PsiModifier.PUBLIC
            }

            return noArgConstructor(
                visibility,
                classOrObject,
                METHOD_INDEX_FOR_DEFAULT_CTOR,
                isJvmExposedBoxed = false,
                functionSymbolPointer = null,
            )
        }

        private fun SymbolLightClassBase.noArgConstructor(
            primaryConstructor: KaConstructorSymbol,
            isJvmExposedBoxed: Boolean,
        ): KtLightMethod = noArgConstructor(
            visibility = primaryConstructor.compilerVisibility.externalDisplayName,
            declaration = primaryConstructor.sourcePsiSafe(),
            methodIndex = METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR,
            isJvmExposedBoxed = isJvmExposedBoxed,
            functionSymbolPointer = primaryConstructor.createPointer(),
        )

        private fun SymbolLightClassBase.noArgConstructor(
            visibility: String,
            declaration: KtDeclaration?,
            methodIndex: Int,
            isJvmExposedBoxed: Boolean,
            functionSymbolPointer: KaSymbolPointer<KaConstructorSymbol>?,
        ): KtLightMethod = SymbolLightNoArgConstructor(
            lightMemberOrigin = declaration?.let {
                LightMemberOriginForDeclaration(
                    originalElement = it,
                    originKind = JvmDeclarationOriginKind.OTHER,
                )
            },
            containingClass = this,
            visibility = visibility,
            methodIndex = methodIndex,
            isJvmExposedBoxed = isJvmExposedBoxed,
            functionSymbolPointer = functionSymbolPointer,
        )
    }
}
