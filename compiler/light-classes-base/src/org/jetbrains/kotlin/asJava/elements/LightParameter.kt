/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.lang.Language
import com.intellij.psi.*

// Based on com.intellij.psi.impl.light.LightParameter
open class LightParameter @JvmOverloads constructor(
    private val myName: String,
    type: PsiType,
    val method: KtLightMethod,
    language: Language?,
    private val myVarArgs: Boolean = type is PsiEllipsisType
) : LightVariableBuilder(method.manager, myName, type, language),
    PsiParameter {
    override fun getDeclarationScope(): KtLightMethod = method

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitParameter(this)
        }
    }

    override fun toString(): String = "Light Parameter"

    override fun isVarArgs(): Boolean = myVarArgs

    override fun getName(): String = myName

    companion object {
        val EMPTY_ARRAY = arrayOfNulls<LightParameter>(0)
    }

}