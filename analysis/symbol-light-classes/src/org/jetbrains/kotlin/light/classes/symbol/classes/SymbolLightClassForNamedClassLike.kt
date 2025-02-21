/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.getParentForLocalDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.isConstOrJvmField
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod.Companion.createPropertyAccessors
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

    protected val isLocal: Boolean get() = withClassSymbol { it.isLocal }

    override fun getParent(): PsiElement? {
        if (isLocal) {
            return classOrObjectDeclaration?.let(::getParentForLocalDeclaration)
        }

        return containingClass ?: containingFile
    }

    protected fun KaSession.addMethodsFromCompanionIfNeeded(
        result: MutableList<PsiMethod>,
        classSymbol: KaNamedClassSymbol,
    ) {
        val companionObjectSymbol = classSymbol.companionObject ?: return
        val methods = companionObjectSymbol.declaredMemberScope
            .callables
            .filterIsInstance<KaNamedFunctionSymbol>()
            .filter { it.hasJvmStaticAnnotation() }

        createMethods(this@SymbolLightClassForNamedClassLike, methods, result)

        companionObjectSymbol.declaredMemberScope
            .callables
            .filterIsInstance<KaPropertySymbol>()
            .forEach { property ->
                createPropertyAccessors(
                    this@SymbolLightClassForNamedClassLike,
                    result,
                    property,
                    isTopLevel = false,
                    onlyJvmStatic = true,
                )
            }
    }

    private val isInner: Boolean get() = withClassSymbol { it.isInner }

    internal val isSealed: Boolean
        get() = classOrObjectDeclaration?.hasModifier(KtTokens.SEALED_KEYWORD) ?: withClassSymbol { it.modality == KaSymbolModality.SEALED }

    internal fun KaSession.addFieldsFromCompanionIfNeeded(
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
                    lightClass = this@SymbolLightClassForNamedClassLike,
                    declaration = it,
                    nameGenerator = nameGenerator,
                    isStatic = true,
                    result = result
                )
            }
    }

    protected fun KaSession.addCompanionObjectFieldIfNeeded(result: MutableList<PsiField>, classSymbol: KaNamedClassSymbol) {
        val companionObjectSymbols: List<KaNamedClassSymbol>? = classOrObjectDeclaration?.companionObjects?.mapNotNull {
            it.namedClassSymbol
        } ?: classSymbol.companionObject?.let(::listOf)

        companionObjectSymbols?.forEach {
            result.add(
                SymbolLightFieldForObject(
                    ktAnalysisSession = this,
                    objectSymbol = it,
                    containingClass = this@SymbolLightClassForNamedClassLike,
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
