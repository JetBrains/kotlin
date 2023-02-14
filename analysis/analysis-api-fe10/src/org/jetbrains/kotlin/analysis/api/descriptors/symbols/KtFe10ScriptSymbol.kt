/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.Name

internal class KtFe10ScriptSymbol : KtScriptSymbol() {
        override val annotationsList: KtAnnotationsList
            get() = TODO("Not yet implemented")
        override val token: KtLifetimeToken
            get() = TODO("Not yet implemented")
        override val name: Name?
            get() = TODO("Not yet implemented")
        override val origin: KtSymbolOrigin
            get() = TODO("Not yet implemented")
        override val psi: PsiElement?
            get() = TODO("Not yet implemented")

        context(KtAnalysisSession) override fun createPointer(): KtSymbolPointer<KtSymbol> {
            TODO("Not yet implemented")
        }

        override val typeParameters: List<KtTypeParameterSymbol>
            get() = TODO("Not yet implemented")
}