/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.light.classes.symbol.annotations.AnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.EmptyAnnotationsBox
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal class SymbolLightClassModifierList<T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    containingDeclaration: T,
    modifiersBox: ModifiersBox = EmptyModifiersBox,
    annotationsBox: AnnotationsBox = EmptyAnnotationsBox,
    override val symbolPointer: KaSymbolPointer<KaSymbol>?,
    override val useSiteModule: KaModule,
) : SymbolLightModifierList<T>(containingDeclaration, modifiersBox, annotationsBox)
