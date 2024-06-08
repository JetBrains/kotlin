/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule

public interface KaSymbolRelationProvider {
    /**
     * Returns containing declaration for symbol:
     *   for top-level declarations returns null
     *   for class members returns containing class
     *   for local declaration returns declaration it was declared it
     */
    public val KaSymbol.containingSymbol: KaDeclarationSymbol?

    /**
     * Returns containing [KtFile] as [KaFileSymbol]
     *
     * Caveat: returns `null` if the given symbol is already [KaFileSymbol], since there is no containing file.
     *  Similarly, no containing file for libraries and Java, hence `null`.
     */
    public val KaSymbol.containingFile: KaFileSymbol?

    public val KaSymbol.containingModule: KtModule

    /**
     * Returns [KaSamConstructorSymbol] if the given [KaClassLikeSymbol] is a functional interface type, a.k.a. SAM.
     */
    public val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?
}