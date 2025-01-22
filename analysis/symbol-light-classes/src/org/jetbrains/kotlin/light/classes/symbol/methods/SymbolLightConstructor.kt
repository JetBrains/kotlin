/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_DEFAULT_CTOR
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.lexer.KtTokens.INNER_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.SEALED_KEYWORD
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmOverloadsAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.isHiddenOrSynthetic
import org.jetbrains.kotlin.light.classes.symbol.classes.*
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import java.util.*

internal class SymbolLightConstructor private constructor(
    ktAnalysisSession: KaSession,
    constructorSymbol: KaConstructorSymbol,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    argumentsSkipMask: BitSet? = null,
) : SymbolLightMethod<KaConstructorSymbol>(
    ktAnalysisSession = ktAnalysisSession,
    functionSymbol = constructorSymbol,
    lightMemberOrigin = null,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask,
) {
    private val _name: String? = containingClass.name

    override fun getName(): String = _name ?: ""

    override fun isConstructor(): Boolean = true
    override fun isOverride(): Boolean = false

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    private val _modifierList: PsiModifierList by lazyPub {
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
                )
            ),
        )
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when {
        modifier !in GranularModifiersBox.VISIBILITY_MODIFIERS -> null
        (containingClass as? SymbolLightClassForNamedClassLike)?.isSealed == true ->
            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(PsiModifier.PRIVATE)

        else -> withFunctionSymbol { symbol ->
            val visibility = if (hasTypeForValueClassInSignature(symbol)) {
                PsiModifier.PRIVATE
            } else {
                symbol.toPsiVisibilityForMember()
            }

            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(visibility)
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

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

            for (constructor in constructors) {
                ProgressManager.checkCanceled()

                if (isHiddenOrSynthetic(constructor)) continue

                result.add(
                    SymbolLightConstructor(
                        ktAnalysisSession = this@createConstructors,
                        constructorSymbol = constructor,
                        containingClass = lightClass,
                        methodIndex = METHOD_INDEX_BASE
                    )
                )

                createJvmOverloadsIfNeeded(constructor, result) { methodIndex, argumentSkipMask ->
                    SymbolLightConstructor(
                        ktAnalysisSession = this@createConstructors,
                        constructorSymbol = constructor,
                        containingClass = lightClass,
                        methodIndex = methodIndex,
                        argumentsSkipMask = argumentSkipMask
                    )
                }
            }
            val primaryConstructor = constructors.singleOrNull { it.isPrimary }
            if (primaryConstructor != null && shouldGenerateNoArgOverload(lightClass, primaryConstructor, constructors)) {
                result.add(
                    lightClass.noArgConstructor(
                        primaryConstructor.compilerVisibility.externalDisplayName,
                        primaryConstructor.sourcePsiSafe(),
                        METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR
                    )
                )
            }
        }

        private fun KaSession.shouldGenerateNoArgOverload(
            lightClass: SymbolLightClassBase,
            primaryConstructor: KaConstructorSymbol,
            constructors: Iterable<KaConstructorSymbol>,
        ): Boolean {
            val classOrObject = lightClass.kotlinOrigin ?: return false
            return primaryConstructor.visibility != KaSymbolVisibility.PRIVATE &&
                    !classOrObject.hasModifier(INNER_KEYWORD) && !lightClass.isEnum &&
                    !classOrObject.hasModifier(SEALED_KEYWORD) &&
                    primaryConstructor.valueParameters.isNotEmpty() &&
                    primaryConstructor.valueParameters.all { it.hasDefaultValue } &&
                    constructors.none { it.valueParameters.isEmpty() } &&
                    !primaryConstructor.hasJvmOverloadsAnnotation()
        }

        private fun SymbolLightClassBase.defaultConstructor(): KtLightMethod {
            val classOrObject = kotlinOrigin
            val visibility = when {
                this is SymbolLightClassForClassLike<*> && (classKind().let { it.isObject || it == KaClassKind.ENUM_CLASS }) -> PsiModifier.PRIVATE
                classOrObject?.hasModifier(SEALED_KEYWORD) == true -> PsiModifier.PROTECTED
                this is SymbolLightClassForEnumEntry -> PsiModifier.PACKAGE_LOCAL
                else -> PsiModifier.PUBLIC
            }

            return noArgConstructor(visibility, classOrObject, METHOD_INDEX_FOR_DEFAULT_CTOR)
        }

        private fun SymbolLightClassBase.noArgConstructor(
            visibility: String,
            declaration: KtDeclaration?,
            methodIndex: Int,
        ): KtLightMethod = SymbolLightNoArgConstructor(
            declaration?.let {
                LightMemberOriginForDeclaration(
                    originalElement = it,
                    originKind = JvmDeclarationOriginKind.OTHER,
                )
            },
            this,
            visibility,
            methodIndex,
        )
    }
}
