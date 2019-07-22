/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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