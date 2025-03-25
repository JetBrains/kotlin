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
package org.jetbrains.kotlin.kdoc.parser

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import java.lang.reflect.Constructor

class KDocElementType(debugName: String, psiClass: Class<out PsiElement>) : IElementType(debugName, KotlinLanguage.INSTANCE) {
    private val psiFactory: Constructor<out PsiElement>

    init {
        try {
            psiFactory = psiClass.getConstructor(ASTNode::class.java)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Must have a constructor with ASTNode")
        }
    }

    fun createPsi(node: ASTNode): PsiElement {
        assert(node.getElementType() === this)

        try {
            return psiFactory.newInstance(node)
        } catch (e: Exception) {
            throw RuntimeException("Error creating psi element for node", e)
        }
    }
}
