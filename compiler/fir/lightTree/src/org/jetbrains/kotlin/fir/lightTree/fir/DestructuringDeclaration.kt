/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.generateTemporaryVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.lightTree.converter.generateDestructuringBlock

data class DestructuringDeclaration(
    val isVar: Boolean,
    val entries: List<FirVariable<*>>,
    val initializer: FirExpression
) {
    fun toFirDestructingDeclaration(session: FirSession): FirExpression {
        val baseVariable = generateTemporaryVariable(session, null, "destruct", initializer)
        return generateDestructuringBlock(session, this, baseVariable, tmpVariable = true)
    }
}