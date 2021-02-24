/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.lexer.KtTokens

class TypeModifier(var hasSuspend: Boolean = false) {
    val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    fun addModifier(modifier: LighterASTNode) {
        when (modifier.tokenType) {
            KtTokens.SUSPEND_KEYWORD -> this.hasSuspend = true
        }
    }

    fun hasNoAnnotations(): Boolean {
        return annotations.isEmpty()
    }
}
