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

import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.siblings
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun getTypeReference(declaration: KtCallableDeclaration): KtTypeReference? {
    return declaration.getFirstChild()!!.siblings(forward = true)
            .dropWhile { it.getNode()!!.getElementType() != KtTokens.COLON }
            .firstIsInstanceOrNull<KtTypeReference>()
}

fun setTypeReference(declaration: KtCallableDeclaration, addAfter: PsiElement?, typeRef: KtTypeReference?): KtTypeReference? {
    val oldTypeRef = getTypeReference(declaration)
    if (typeRef != null) {
        if (oldTypeRef != null) {
            return oldTypeRef.replace(typeRef) as KtTypeReference
        }
        else {
            var anchor = addAfter ?: declaration.getNameIdentifier()?.siblings(forward = true)?.firstOrNull { it is PsiErrorElement }
            val newTypeRef = declaration.addAfter(typeRef, anchor) as KtTypeReference
            declaration.addAfter(KtPsiFactory(declaration.getProject()).createColon(), anchor)
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

fun KtCallableDeclaration.setReceiverTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
    val needParentheses = typeRef != null && typeRef.typeElement is KtFunctionType && !typeRef.hasParentheses()
    val oldTypeRef = getReceiverTypeReference()
    if (typeRef != null) {
        val newTypeRef =
                if (oldTypeRef != null) {
                    oldTypeRef.replace(typeRef) as KtTypeReference
                }
                else {
                    val anchor = getNameIdentifier() ?: valueParameterList
                    val newTypeRef = addBefore(typeRef, anchor) as KtTypeReference
                    addAfter(KtPsiFactory(getProject()).createDot(), newTypeRef)
                    newTypeRef
                }
        if (needParentheses) {
            val argList = KtPsiFactory(getProject()).createCallArguments("()")
            newTypeRef.addBefore(argList.getLeftParenthesis()!!, newTypeRef.getFirstChild())
            newTypeRef.add(argList.getRightParenthesis()!!)
        }
        return newTypeRef
    }
    else {
        if (oldTypeRef != null) {
            val dot = oldTypeRef.siblings(forward = true).firstOrNull { it.getNode().getElementType() == KtTokens.DOT }
            deleteChildRange(oldTypeRef, dot ?: oldTypeRef)
        }
        return null
    }
}