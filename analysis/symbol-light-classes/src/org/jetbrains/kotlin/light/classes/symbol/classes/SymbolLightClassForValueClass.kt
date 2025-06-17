/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class SymbolLightClassForValueClass : SymbolLightClassForClassOrObject {
    @OptIn(KaImplementationDetail::class)
    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KaModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classSymbolPointer = classOrObject.createSymbolPointer(ktModule),
        ktModule = ktModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject.hasModifier(KtTokens.INLINE_KEYWORD) || classOrObject.hasModifier(KtTokens.VALUE_KEYWORD))
    }

    constructor(
        ktModule: KaModule,
        classSymbol: KaNamedClassSymbol,
        manager: PsiManager,
    ) : super(
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    ) {
        require(classSymbol.isInline)
    }

    private constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        ktModule: KaModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    override fun copy(): SymbolLightClassForValueClass =
        SymbolLightClassForValueClass(classOrObjectDeclaration, classSymbolPointer, ktModule, manager)
}
