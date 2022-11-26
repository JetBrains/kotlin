/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

abstract class SymbolLightClassForNamedClassOrObject : SymbolLightClassForClassOrObject<KtNamedClassOrObjectSymbol> {
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

    protected fun addMethodsFromCompanionIfNeeded(
        result: MutableList<KtLightMethod>,
    ): Unit = withClassOrObjectSymbol { classOrObjectSymbol ->
        classOrObjectSymbol.companionObject?.run {
            val methods = getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtFunctionSymbol>()
                .filter { it.hasJvmStaticAnnotation() }

            createMethods(methods, result)

            val properties = getDeclaredMemberScope().getCallableSymbols().filterIsInstance<KtPropertySymbol>()
            properties.forEach { property ->
                createPropertyAccessors(
                    result,
                    property,
                    isTopLevel = false,
                    onlyJvmStatic = true
                )
            }
        }
    }

    private val KtPropertySymbol.isConstOrJvmField: Boolean get() = isConst || hasJvmFieldAnnotation()

    private val KtPropertySymbol.isConst: Boolean get() = (this as? KtKotlinPropertySymbol)?.isConst == true

    context(ktAnalysisSession@KtAnalysisSession)
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
                    ktAnalysisSession = this@ktAnalysisSession,
                    propertySymbol = it,
                    fieldName = it.name.asString(),
                    containingClass = this,
                    lightMemberOrigin = null,
                    isTopLevel = false,
                    forceStatic = true,
                    takePropertyVisibility = it.isConstOrJvmField,
                )
            }
    }

    context(ktAnalysisSession@KtAnalysisSession)
    protected fun addCompanionObjectFieldIfNeeded(result: MutableList<KtLightField>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        val companionObjectSymbols: List<KtNamedClassOrObjectSymbol>? = classOrObjectDeclaration?.companionObjects?.mapNotNull {
            it.getNamedClassOrObjectSymbol()
        } ?: classOrObjectSymbol.companionObject?.let(::listOf)

        companionObjectSymbols?.forEach {
            result.add(
                SymbolLightFieldForObject(
                    ktAnalysisSession = this@ktAnalysisSession,
                    objectSymbol = it,
                    containingClass = this,
                    name = it.name.asString(),
                    lightMemberOrigin = null,
                )
            )
        }
    }
}