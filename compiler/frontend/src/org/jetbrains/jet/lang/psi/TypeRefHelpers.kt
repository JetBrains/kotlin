/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.typeRefHelpers

import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.psiUtil.siblings
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.psi.JetNamedDeclaration

fun getTypeRef(declaration: JetDeclaration): JetTypeReference? {
    return declaration.getFirstChild()!!.siblings(forward = true)
            .dropWhile { it.getNode()!!.getElementType() != JetTokens.COLON }
            .filterIsInstance(javaClass<JetTypeReference>())
            .firstOrNull()
}

fun setTypeRef(declaration: JetNamedDeclaration, addAfter: PsiElement?, typeRef: JetTypeReference?): JetTypeReference? {
    val oldTypeRef = getTypeRef(declaration)
    if (typeRef != null) {
        if (oldTypeRef != null) {
            return oldTypeRef.replace(typeRef) as JetTypeReference
        }
        else {
            var anchor = addAfter ?: declaration.getNameIdentifier()
            val newTypeRef = declaration.addAfter(typeRef, anchor) as JetTypeReference
            declaration.addAfter(JetPsiFactory(declaration.getProject()).createColon(), anchor)
            return newTypeRef
        }
    }
    else {
        if (oldTypeRef != null) {
            val colon = declaration.getNode()!!.findChildByType(JetTokens.COLON)!!.getPsi()!!
            val removeFrom = colon.getPrevSibling() as? PsiWhiteSpace ?: colon
            declaration.deleteChildRange(removeFrom, oldTypeRef)
        }
        return null
    }
}
