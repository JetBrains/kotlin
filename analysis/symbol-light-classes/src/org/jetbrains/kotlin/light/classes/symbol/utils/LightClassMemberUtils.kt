/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.utils

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.javaInterop.asFacadePsiClass
import org.jetbrains.kotlin.analysis.api.javaInterop.asPsiClass
import org.jetbrains.kotlin.analysis.api.javaInterop.asPsiMethods
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.decompiled.light.classes.KtLightMethodForDecompiledDeclaration
import org.jetbrains.kotlin.asJava.classes.KtExtensibleLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.analysis.api.javaInterop.KaElementJavaView
import org.jetbrains.kotlin.analysis.api.javaInterop.KaSymbolJavaView
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBaseImpl
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * Utilities for acquiring various light declarations that are not classes: methods, field, parameters, etc.
 */
internal object LightClassMemberUtils {
    context(session: KaSession)
    fun getLightClassParameters(
        parameterSymbol: KaParameterSymbol,
    ): List<PsiParameter> {
        parameterSymbol.anchorPsiIfNotKotlin()?.let { return listOfNotNull(it as? PsiParameter) }

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
        typeParameterSymbol.anchorPsiIfNotKotlin()?.let { return listOfNotNull(it as? PsiTypeParameter) }

        val enclosingDeclaration = typeParameterSymbol.containingDeclaration ?: return emptyList()
        val paramIndex = enclosingDeclaration.typeParameters.indexOf(typeParameterSymbol)

        val lightOwners = when (enclosingDeclaration) {
            is KaClassSymbol -> listOf(enclosingDeclaration.asPsiClass())
            is KaFunctionSymbol -> enclosingDeclaration.asPsiMethods()
            is KaKotlinPropertySymbol -> listOfNotNull(enclosingDeclaration.getter, enclosingDeclaration.setter).flatMap {
                it.asPsiMethods()
            }
            else -> emptyList()
        }

        return lightOwners.mapNotNull { lightOwner ->
            (lightOwner as? PsiTypeParameterListOwner)?.typeParameters?.getOrNull(paramIndex)
        }
    }

    @OptIn(KtImplementationDetail::class)
    context(session: KaSession)
    fun getLightClassBackingField(
        declarationSymbol: KaSymbol,
    ): PsiField? {
        if (declarationSymbol !is KaClassSymbol && declarationSymbol !is KaEnumEntrySymbol && declarationSymbol !is KaBackingFieldSymbol) return null
        declarationSymbol.anchorPsiIfNotKotlin()?.let { return it as? PsiField }

        val psiClass: PsiClass = getWrappingClass(declarationSymbol)?.let { wrapper ->
            (wrapper.parent as? PsiClass).takeIf { wrapper.isCreatedFromCompanion() } ?: wrapper
        } ?: return null

        val declarationSymbolPointer = declarationSymbol.createPointer()
        val declarationSymbolPsi = declarationSymbol.getPsiForMatching()

        val fields = psiClass.candidateMembers(
            symbol = declarationSymbol,
            symbolPsi = declarationSymbolPsi,
            allMembers = PsiClass::getFields,
            membersByKotlinOrigin = KtExtensibleLightClass::findOwnFieldsByKotlinOrigin,
        )

        return fields.find { psiField: PsiField ->
            psiField.isCreatedFrom(declarationSymbolPsi, declarationSymbolPointer)
        }
    }

    @OptIn(KtImplementationDetail::class)
    context(session: KaSession)
    fun getLightClassMethods(
        functionSymbol: KaFunctionSymbol,
    ): List<PsiMethod> {
        functionSymbol.anchorPsiIfNotKotlin()?.let { return listOfNotNull(it as? PsiMethod) }

        val functionSymbolPointer = functionSymbol.createPointer()
        val functionSymbolPsi = functionSymbol.getPsiForMatching()

        return getWrappingClasses(functionSymbol)
            .flatMap { wrappingClass ->
                wrappingClass.candidateMembers(
                    symbol = functionSymbol,
                    symbolPsi = functionSymbolPsi,
                    allMembers = PsiClass::getMethods,
                    membersByKotlinOrigin = KtExtensibleLightClass::findOwnMethodsByKotlinOrigin,
                )
            }
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
                        lightMethod.kotlinOrigin === functionSymbolPsi.value?.containingClass()
                    }
                    else -> false
                }
            }
    }

    /**
     * Returns the members from [allMembers] of this class which [isCreatedFrom] can match for [symbol], in their order in [allMembers].
     *
     * A symbol light class answers it from its cached [membersByKotlinOrigin] map by the `kotlinOrigin` of the members of [symbol]
     * ([lightMemberKotlinOrigin]). The other members are skipped, so the lookup is not a scan of all members for each symbol.
     *
     * Members without a pointer match by [symbolPsi] instead, so all members are returned if it differs from that `kotlinOrigin`
     * and some member has it as its `kotlinOrigin`.
     */
    context(_: KaSession)
    private inline fun <T : PsiMember> PsiClass.candidateMembers(
        symbol: KaSymbol,
        symbolPsi: Lazy<KtElement?>,
        allMembers: PsiClass.() -> Array<T>,
        membersByKotlinOrigin: KtExtensibleLightClass.(KtElement) -> List<T>,
    ): List<T> {
        // Light classes of decompiled declarations compute `kotlinOrigin` by a search in the compiled file
        if (this !is SymbolLightClassBaseImpl<*>) return allMembers().asList()
        val kotlinOrigin = symbol.lightMemberKotlinOrigin() ?: return allMembers().asList()

        val psiForMatching = symbolPsi.value
        if (psiForMatching != null && psiForMatching != kotlinOrigin && membersByKotlinOrigin(psiForMatching).isNotEmpty()) {
            return allMembers().asList()
        }

        return membersByKotlinOrigin(kotlinOrigin)
    }

    /**
     * Returns the [kotlinOrigin][KaElementJavaView.kotlinOrigin] of every light member created from this symbol, or `null` if the
     * symbol doesn't fix it.
     *
     * A member which [isCreatedFrom] matches by its pointer was created from the same symbol, and its `kotlinOrigin` is computed
     * from that symbol as here:
     * - named functions: [SymbolLightMethod][org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethod];
     * - property accessors: `SymbolLightAccessorMethod.getLightMemberOriginForAccessor`;
     * - backing fields: [SymbolLightFieldForProperty][org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty];
     * - enum entries: `SymbolLightClassForClassOrObject.addFieldsForEnumEntries`.
     *
     * A change there must be mirrored here.
     * Other symbols, such as constructors and non-source declarations, are matched against all members.
     */
    context(_: KaSession)
    private fun KaSymbol.lightMemberKotlinOrigin(): KtElement? = when (this) {
        is KaNamedFunctionSymbol -> sourcePsiSafe<KtDeclaration>()
        is KaPropertyAccessorSymbol -> (containingDeclaration as? KaPropertySymbol)
            ?.takeIf { it.origin == KaSymbolOrigin.SOURCE }
            ?.psiSafe<KtDeclaration>()
        is KaBackingFieldSymbol -> owningProperty.sourcePsiSafe<KtCallableDeclaration>()
        is KaEnumEntrySymbol -> sourcePsiSafe<KtEnumEntry>()
        else -> null
    }

    context(_: KaSession)
    private fun getWrappingClass(declaration: KaSymbol): PsiClass? {
        return when (declaration.location) {
            KaSymbolLocation.TOP_LEVEL -> when (declaration) {
                is KaFileSymbol -> declaration.asFacadePsiClass()
                is KaScriptSymbol -> declaration.asFacadePsiClass()
                // The PSI-based fallback for library declarations should be removed after KT-85997
                else -> (declaration.containingFile ?: (declaration.realPsi?.containingFile as? KtFile)?.symbol)?.asFacadePsiClass()
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

    private fun PsiElement.isCreatedFrom(otherPsi: Lazy<KtElement?>, otherSymbolPointer: KaSymbolPointer<*>): Boolean {
        if (this !is KaElementJavaView) {
            return false
        }

        val thisSymbolPointer = (this as? KaSymbolJavaView<*>)?.symbolPointer
        if (thisSymbolPointer != null) {
            return thisSymbolPointer.pointsToTheSameSymbolAs(otherSymbolPointer)
        }

        val computedOtherPsi = otherPsi.value
        return computedOtherPsi != null && kotlinOrigin == computedOtherPsi
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
    private fun KaSymbol.getPsiForMatching(): Lazy<KtElement?> {
        return lazy(LazyThreadSafetyMode.NONE) {
            when (this) {
                is KaPropertyAccessorSymbol -> (containingDeclaration as? KaKotlinPropertySymbol)?.anchorPsi ?: anchorPsi
                else -> anchorPsi
            }?.originalElement as? KtElement
        }
    }
}

