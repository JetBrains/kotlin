/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtElement
import kotlin.reflect.KClass

public abstract class KaPsiBasedSymbolPointerCreator {
    @OptIn(KaImplementationDetail::class)
    @KaExperimentalApi
    public abstract fun symbolPointer(element: KtElement): KaSymbolPointer<KaSymbol>

    @OptIn(KaImplementationDetail::class)
    @KaExperimentalApi
    public abstract fun <S : KaSymbol> symbolPointerOfType(element: KtElement, expectedType: KClass<S>): KaSymbolPointer<S>

    @OptIn(KaImplementationDetail::class)
    @KaExperimentalApi
    public inline fun <reified S : KaSymbol> symbolPointerOfType(element: KtElement): KaSymbolPointer<S> =
        symbolPointerOfType(element, S::class)

    public companion object {
        public fun getInstance(project: Project): KaPsiBasedSymbolPointerCreator = project.service()
    }
}