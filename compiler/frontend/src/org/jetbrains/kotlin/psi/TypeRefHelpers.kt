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

package org.jetbrains.kotlin.psi.typeRefHelpers

import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.psiUtil.siblings
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun getTypeReference(declaration: JetCallableDeclaration): JetTypeReference? {
    return declaration.getFirstChild()!!.siblings(forward = true)
            .dropWhile { it.getNode()!!.getElementType() != JetTokens.COLON }
            .firstIsInstanceOrNull<JetTypeReference>()
}

fun setTypeReference(declaration: JetCallableDeclaration, addAfter: PsiElement?, typeRef: JetTypeReference?): JetTypeReference? {
    val oldTypeRef = getTypeReference(declaration)
    if (typeRef != null) {
        if (oldTypeRef != null) {
            return oldTypeRef.replace(typeRef) as JetTypeReference
        }
        else {
            var anchor = addAfter ?: declaration.getNameIdentifier()?.siblings(forward = true)?.firstOrNull { it is PsiErrorElement }
            val newTypeRef = declaration.addAfter(typeRef, anchor) as JetTypeReference
            declaration.addAfter(JetPsiFactory(declaration.getProject()).createColon(), anchor)
            return newTypeRef
        }
    }
    else {
        if (oldTypeRef != null) {
            val colon = declaration.getColon()!!
            val removeFrom = colon.getPrevSibling() as? PsiWhiteSpace ?: colon
            declaration.deleteChildRange(removeFrom, oldTypeRef)
        }
        return null
    }
}

fun JetCallableDeclaration.setReceiverTypeReference(typeRef: JetTypeReference?): JetTypeReference? {
    val oldTypeRef = getReceiverTypeReference()
    if (typeRef != null) {
        if (oldTypeRef != null) {
            return oldTypeRef.replace(typeRef) as JetTypeReference
        }
        else {
            val anchor = getNameIdentifier()
            val newTypeRef = addBefore(typeRef, anchor) as JetTypeReference
            addAfter(JetPsiFactory(getProject()).createDot(), newTypeRef)
            return newTypeRef
        }
    }
    else {
        if (oldTypeRef != null) {
            val dot = oldTypeRef.siblings(forward = true).firstOrNull { it.getNode().getElementType() == JetTokens.DOT }
            deleteChildRange(oldTypeRef, dot ?: oldTypeRef)
        }
        return null
    }
}