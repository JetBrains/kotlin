/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypeParameterListOwner
import com.intellij.psi.PsiTypes
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.javaInterop.asFacadePsiClass
import org.jetbrains.kotlin.analysis.api.javaInterop.asPsiClass
import org.jetbrains.kotlin.analysis.api.javaInterop.asPsiMethods
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingDeclaration
import org.jetbrains.kotlin.analysis.api.symbols.containingFile
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.decompiled.light.classes.KtLightMethodForDecompiledDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClass

internal fun PsiElement.nonExistentType(): PsiType =
    JavaPsiFacade.getElementFactory(project).createTypeFromText(StandardNames.NON_EXISTENT_CLASS.asString(), this)

@OptIn(KaAllowAnalysisOnEdt::class, KaAllowAnalysisFromWriteAction::class)
private inline fun <E> allowLightClassesOnEdt(crossinline action: () -> E): E = allowAnalysisFromWriteAction {
    allowAnalysisOnEdt(action)
}

internal inline fun <R> analyzeForLightClasses(context: KtElement, crossinline action: context(KaSession) () -> R): R =
    allowLightClassesOnEdt {
        analyze(context, action = action)
    }

internal inline fun <R> analyzeForLightClasses(useSiteKtModule: KaModule, crossinline action: context(KaSession) () -> R): R =
    allowLightClassesOnEdt {
        analyze(useSiteKtModule, action = action)
    }

/**
 * Utilities for acquiring various light declarations that are not classes: methods, field, parameters, etc.
 */
internal object LightClassElementUtils {
    context(session: KaSession)
    fun getLightClassParameters(
        parameterSymbol: KaParameterSymbol,
    ): List<PsiParameter> {
        val enclosingDeclaration = parameterSymbol.containingDeclaration as? KaFunctionSymbol ?: return emptyList()

        val methods = enclosingDeclaration.asPsiMethods()

        val parameterSymbolPointer = parameterSymbol.createPointer()
        val parameterSymbolPsi = parameterSymbol.getPsiForMatching()
        return methods.mapNotNull { method ->
            method.parameterList.parameters.firstOrNull { parameter ->
                parameter.isCreatedFrom(parameterSymbolPsi, parameterSymbolPointer)
            }
        }
    }

    context(session: KaSession)
    fun getLightClassTypeParameter(
        typeParameterSymbol: KaTypeParameterSymbol,
    ): List<PsiTypeParameter> {
        val enclosingDeclaration = typeParameterSymbol.containingDeclaration ?: return emptyList()
        val paramIndex = enclosingDeclaration.typeParameters.indexOf(typeParameterSymbol)

        val lightOwners = when (enclosingDeclaration) {
            is KaClassSymbol -> listOf(enclosingDeclaration.asPsiClass())
            is KaFunctionSymbol -> enclosingDeclaration.asPsiMethods()
            else -> emptyList()
        }

        return lightOwners.mapNotNull { lightOwner ->
            (lightOwner as? PsiTypeParameterListOwner)?.typeParameters?.getOrNull(paramIndex)
        }
    }

    context(session: KaSession)
    fun getLightClassBackingField(
        declarationSymbol: KaSymbol,
    ): PsiField? {
        if (declarationSymbol !is KaClassSymbol && declarationSymbol !is KaEnumEntrySymbol && declarationSymbol !is KaBackingFieldSymbol) return null

        val psiClass: PsiClass = getWrappingClass(declarationSymbol)?.let { wrapper ->
            (wrapper.parent as? PsiClass).takeIf { wrapper.isCreatedFromCompanion() } ?: wrapper
        } ?: return null

        val declarationSymbolPointer = declarationSymbol.createPointer()
        val declarationSymbolPsi = declarationSymbol.getPsiForMatching()

        return psiClass.fields.find { psiField: PsiField ->
            psiField.isCreatedFrom(declarationSymbolPsi, declarationSymbolPointer)
        }
    }

    context(session: KaSession)
    fun getLightClassMethods(
        functionSymbol: KaFunctionSymbol,
    ): List<PsiMethod> {
        val functionSymbolPointer = functionSymbol.createPointer()
        val functionSymbolPsi = functionSymbol.getPsiForMatching()

        return getWrappingClasses(functionSymbol)
            .flatMap { it.methods.asSequence() }
            .filterIsInstance<KtLightMethod>()
            .filter { lightMethod ->
                when {
                    lightMethod.isCreatedFrom(functionSymbolPsi, functionSymbolPointer) -> {
                        if (functionSymbol is KaPropertyAccessorSymbol && lightMethod is KtLightMethodForDecompiledDeclaration) {
                            /**
                             * Decompiled accessors don't have any pointers and always have the corresponding property as their `kotlinOrigin`.
                             * Hence, an additional check is needed to properly match decompiled accessors
                             */
                            val hasVoidReturnType = lightMethod.returnType == PsiTypes.voidType()
                            when (functionSymbol) {
                                is KaPropertyGetterSymbol -> !hasVoidReturnType
                                is KaPropertySetterSymbol -> hasVoidReturnType
                            }
                        } else {
                            true
                        }
                    }
                    functionSymbol is KaConstructorSymbol && functionSymbol.isPrimary && lightMethod.isConstructor -> {
                        // no-arg constructors have the containing class as their origin
                        lightMethod.kotlinOrigin === functionSymbolPsi?.containingClass()
                    }
                    else -> false
                }
            }
    }

    context(_: KaSession)
    private fun getWrappingClass(declaration: KaSymbol): PsiClass? {
        return when (declaration.location) {
            KaSymbolLocation.TOP_LEVEL -> when (declaration) {
                is KaFileSymbol -> declaration.asFacadePsiClass()
                is KaScriptSymbol -> declaration.asFacadePsiClass()
                else -> (declaration.containingFile ?: (declaration.anchorPsi?.containingFile as? KtFile)?.symbol)?.asFacadePsiClass()
            }
            KaSymbolLocation.CLASS -> (declaration.containingDeclaration as? KaClassSymbol)?.asPsiClass()
            KaSymbolLocation.PROPERTY -> declaration.containingDeclaration?.let { property -> getWrappingClass(property) }
            KaSymbolLocation.LOCAL -> null
        }
    }

    context(_: KaSession)
    private fun getWrappingClasses(declaration: KaSymbol): List<PsiClass> {
        val wrapperClass = getWrappingClass(declaration) ?: return emptyList()

        val isCompanion = wrapperClass.isCreatedFromCompanion()

        val wrapperParent = wrapperClass.parent
        return if (isCompanion && wrapperParent is PsiClass) {
            listOf(wrapperClass, wrapperParent)
        } else {
            listOf(wrapperClass)
        }
    }

    private fun PsiElement.isCreatedFrom(otherPsi: KtElement?, otherSymbolPointer: KaSymbolPointer<*>): Boolean {
        if (this !is KaElementJavaView) {
            return false
        }

        val thisSymbolPointer = (this as? KaSymbolJavaView<*>)?.symbolPointer
        if (thisSymbolPointer != null) {
            return thisSymbolPointer.pointsToTheSameSymbolAs(otherSymbolPointer)
        }

        return otherPsi != null && kotlinOrigin == otherPsi
    }

    private fun PsiClass.isCreatedFromCompanion(): Boolean {
        if (this !is KaElementJavaView) {
            return false
        }

        return when (val kotlinOrigin = kotlinOrigin) {
            null -> (this as? KaSymbolJavaView<*>)?.symbolPointer?.withSymbol(useSiteModule) { originSymbol ->
                originSymbol is KaClassSymbol && originSymbol.classKind == KaClassKind.COMPANION_OBJECT
            }
            else -> (kotlinOrigin as? KtObjectDeclaration)?.isCompanion()
        } == true
    }

    /**
     * Returns [KtElement] which should be used for matching [this] against produced light elements.
     *
     * It is needed to handle various inconsistencies in LC `kotlinOrigin`s.
     * E.g., all accessors in LCs have the corresponding property as their origin, even when the accessor is explicit.
     * The default option is [KaSymbol.anchorPsi].
     * It's used instead of [KaSymbol.realPsi] to match synthetic declarations.
     */
    context(_: KaSession)
    private fun KaSymbol.getPsiForMatching(): KtElement? {
        return when (this) {
            is KaPropertyAccessorSymbol -> (containingDeclaration as? KaKotlinPropertySymbol)?.anchorPsi ?: anchorPsi
            else -> anchorPsi
        }?.originalElement as? KtElement
    }
}
