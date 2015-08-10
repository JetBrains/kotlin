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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.*
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

public class JetPrimaryConstructor : JetConstructor<JetPrimaryConstructor> {
    public constructor(node: ASTNode) : super(node)
    public constructor(stub: KotlinPlaceHolderStub<JetPrimaryConstructor>) : super(stub, JetStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D) = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingClassOrObject() = getParent() as JetClassOrObject

    override fun addModifier(modifier: JetModifierKeywordToken) {
        val modifierList = getModifierList()
        if (modifierList != null) {
            addModifier(modifierList, modifier, JetTokens.PUBLIC_KEYWORD)
        }
        else {
            if (modifier == JetTokens.PUBLIC_KEYWORD) return
            val parameterList = getValueParameterList()!!
            val newModifierList = JetPsiFactory(getProject()).createModifierList(modifier)
            addBefore(newModifierList, parameterList)
        }
    }

    override fun addAnnotationEntry(annotationEntry: JetAnnotationEntry): JetAnnotationEntry {
        val modifierList = getModifierList()
        return if (modifierList != null) {
            modifierList.addBefore(annotationEntry, modifierList.firstChild) as JetAnnotationEntry
        }
        else {
            val parameterList = getValueParameterList()!!
            val newModifierList = JetPsiFactory(getProject()).createModifierList(annotationEntry.text)
            (addBefore(newModifierList, parameterList) as JetModifierList).annotationEntries.first()
        }
    }
}
