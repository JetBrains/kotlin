/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.getParentForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.isConstOrJvmField
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

abstract class SymbolLightClassForNamedClassLike : SymbolLightClassForClassLike<KtNamedClassOrObjectSymbol> {
    constructor(
        ktAnalysisSession: KtAnalysisSession,
        ktModule: KtModule,
        classOrObjectSymbol: KtNamedClassOrObjectSymbol,
        manager: PsiManager
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classOrObjectSymbol = classOrObjectSymbol,
        manager = manager,
    )

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classOrObjectSymbolPointer: KtSymbolPointer<KtNamedClassOrObjectSymbol>,
        ktModule: KtModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classOrObjectSymbolPointer = classOrObjectSymbolPointer,
        ktModule = ktModule,
        manager = manager
    )

    protected val isLocal: Boolean by lazyPub {
        classOrObjectDeclaration?.isLocal ?: withClassOrObjectSymbol { it.symbolKind == KtSymbolKind.LOCAL }
    }

    override fun getParent(): PsiElement? {
        if (isLocal) {
            return classOrObjectDeclaration?.let(::getParentForLocalDeclaration)
        }

        return containingClass ?: containingFile
    }

    context(KtAnalysisSession)
    protected fun addMethodsFromCompanionIfNeeded(
        result: MutableList<KtLightMethod>,
        classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    ) {
        val companionObjectSymbol = classOrObjectSymbol.companionObject ?: return
        val methods = companionObjectSymbol.getDeclaredMemberScope()
            .getCallableSymbols()
            .filterIsInstance<KtFunctionSymbol>()
            .filter { it.hasJvmStaticAnnotation() }

        createMethods(methods, result)

        companionObjectSymbol.getDeclaredMemberScope()
            .getCallableSymbols()
            .filterIsInstance<KtPropertySymbol>()
            .forEach { property ->
                createPropertyAccessors(
                    result,
                    property,
                    isTopLevel = false,
                    onlyJvmStatic = true,
                )
            }
    }

    private val isInner: Boolean
        get() = classOrObjectDeclaration?.hasModifier(KtTokens.INNER_KEYWORD) ?: withClassOrObjectSymbol { it.isInner }

    context(KtAnalysisSession)
    protected fun addFieldsFromCompanionIfNeeded(
        result: MutableList<KtLightField>,
        classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    ) {
        classOrObjectSymbol.companionObject
            ?.getDeclaredMemberScope()
            ?.getCallableSymbols()
            ?.filterIsInstance<KtPropertySymbol>()
            ?.applyIf(isInterface) {
                filter { it.isConstOrJvmField }
            }
            ?.mapTo(result) {
                SymbolLightFieldForProperty(
                    ktAnalysisSession = this@KtAnalysisSession,
                    propertySymbol = it,
                    fieldName = it.name.asString(),
                    containingClass = this,
                    lightMemberOrigin = null,
                    isStatic = true,
                )
            }
    }

    context(KtAnalysisSession)
    protected fun addCompanionObjectFieldIfNeeded(result: MutableList<KtLightField>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        val companionObjectSymbols: List<KtNamedClassOrObjectSymbol>? = classOrObjectDeclaration?.companionObjects?.mapNotNull {
            it.getNamedClassOrObjectSymbol()
        } ?: classOrObjectSymbol.companionObject?.let(::listOf)

        companionObjectSymbols?.forEach {
            result.add(
                SymbolLightFieldForObject(
                    ktAnalysisSession = this@KtAnalysisSession,
                    objectSymbol = it,
                    containingClass = this,
                    name = it.name.asString(),
                    lightMemberOrigin = null,
                    isCompanion = true,
                )
            )
        }
    }

    internal fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in GranularModifiersBox.VISIBILITY_MODIFIERS -> {
            GranularModifiersBox.computeVisibilityForClass(ktModule, classOrObjectSymbolPointer, isTopLevel)
        }

        in GranularModifiersBox.MODALITY_MODIFIERS -> GranularModifiersBox.computeSimpleModality(ktModule, classOrObjectSymbolPointer)
        PsiModifier.STATIC -> {
            val isStatic = !isTopLevel && !isInner
            mapOf(modifier to isStatic)
        }

        else -> null
    }
}