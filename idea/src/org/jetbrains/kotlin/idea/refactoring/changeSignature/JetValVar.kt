/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.JetTokens

public enum class JetValVar(val name: String) {
    None("none"),
    Val("val"),
    Var("var");

    override fun toString(): String = name
}

fun ASTNode?.toValVar(): JetValVar {
    return when {
        this == null -> JetValVar.None
        getElementType() == JetTokens.VAL_KEYWORD -> JetValVar.Val
        getElementType() == JetTokens.VAR_KEYWORD -> JetValVar.Var
        else -> throw IllegalArgumentException("Unknown val/var token: " + getText())
    }
}
