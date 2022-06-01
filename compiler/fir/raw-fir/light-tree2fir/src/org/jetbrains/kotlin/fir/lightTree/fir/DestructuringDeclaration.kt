/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.generateTemporaryVariable
import org.jetbrains.kotlin.fir.lightTree.converter.generateDestructuringBlock
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.name.SpecialNames

data class DestructuringDeclaration(
    val isVar: Boolean,
    val entries: List<FirVariable?>,
    val initializer: FirExpression,
    val source: KtSourceElement,
    val modifier: Modifier,
) {
    fun toFirDestructingDeclaration(moduleData: FirModuleData): FirExpression {
        val baseVariable = generateTemporaryVariable(
            moduleData,
            source,
            SpecialNames.DESTRUCT,
            initializer,
            extractedAnnotations = modifier.annotations
        )
        return generateDestructuringBlock(moduleData, this, baseVariable, tmpVariable = true)
    }
}
