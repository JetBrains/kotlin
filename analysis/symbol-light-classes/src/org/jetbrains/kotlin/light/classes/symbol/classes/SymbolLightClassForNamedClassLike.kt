/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.isLocal
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.getParentForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.isConstOrJvmField
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal abstract class SymbolLightClassForNamedClassLike : SymbolLightClassForClassLike<KaNamedClassSymbol> {
    constructor(
        ktAnalysisSession: KaSession,
        ktModule: KaModule,
        classSymbol: KaNamedClassSymbol,
        manager: PsiManager
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    )

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        ktModule: KaModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        ktModule = ktModule,
        manager = manager
    )

    protected val isLocal: Boolean by lazyPub {
        classOrObjectDeclaration?.isLocal ?: withClassSymbol { it.isLocal }
    }

    override fun getParent(): PsiElement? {
        if (isLocal) {
            return classOrObjectDeclaration?.let(::getParentForLocalDeclaration)
        }

        return containingClass ?: containingFile
    }

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    protected fun addMethodsFromCompanionIfNeeded(
        result: MutableList<PsiMethod>,
        classSymbol: KaNamedClassSymbol,
    ) {
        val companionObjectSymbol = classSymbol.companionObject ?: return
        val methods = companionObjectSymbol.declaredMemberScope
            .callables
            .filterIsInstance<KaNamedFunctionSymbol>()
            .filter { it.hasJvmStaticAnnotation() }

        createMethods(methods, result)

        companionObjectSymbol.declaredMemberScope
            .callables
            .filterIsInstance<KaPropertySymbol>()
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
        get() = classOrObjectDeclaration?.hasModifier(KtTokens.INNER_KEYWORD) ?: withClassSymbol { it.isInner }

    internal val isSealed: Boolean
        get() = classOrObjectDeclaration?.hasModifier(KtTokens.SEALED_KEYWORD) ?: withClassSymbol { it.modality == KaSymbolModality.SEALED }

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    internal fun addFieldsFromCompanionIfNeeded(
        result: MutableList<PsiField>,
        classSymbol: KaNamedClassSymbol,
        nameGenerator: SymbolLightField.FieldNameGenerator,
    ) {
        classSymbol.companionObject
            ?.declaredMemberScope
            ?.callables
            ?.filterIsInstance<KaPropertySymbol>()
            ?.applyIf(isInterface) {
                filter { it.isConstOrJvmField }
            }
            ?.forEach {
                createAndAddField(
                    declaration = it,
                    nameGenerator = nameGenerator,
                    isStatic = true,
                    result = result
                )
            }
    }

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    protected fun addCompanionObjectFieldIfNeeded(result: MutableList<PsiField>, classSymbol: KaNamedClassSymbol) {
        val companionObjectSymbols: List<KaNamedClassSymbol>? = classOrObjectDeclaration?.companionObjects?.mapNotNull {
            it.namedClassSymbol
        } ?: classSymbol.companionObject?.let(::listOf)

        companionObjectSymbols?.forEach {
            result.add(
                SymbolLightFieldForObject(
                    ktAnalysisSession = this@KaSession,
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
            GranularModifiersBox.computeVisibilityForClass(ktModule, classSymbolPointer, isTopLevel)
        }

        in GranularModifiersBox.MODALITY_MODIFIERS -> {
            GranularModifiersBox.computeSimpleModality(ktModule, classSymbolPointer)
        }

        PsiModifier.STATIC -> {
            val isStatic = !isTopLevel && !isInner
            mapOf(modifier to isStatic)
        }

        else -> null
    }
}
