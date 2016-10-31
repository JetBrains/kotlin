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

package org.jetbrains.kotlin.psi.typeRefHelpers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun getTypeReference(declaration: KtCallableDeclaration): KtTypeReference? {
    return declaration.firstChild!!.siblings(forward = true)
            .dropWhile { it.node!!.elementType != KtTokens.COLON }
            .firstIsInstanceOrNull<KtTypeReference>()
}

fun setTypeReference(declaration: KtCallableDeclaration, addAfter: PsiElement?, typeRef: KtTypeReference?): KtTypeReference? {
    val oldTypeRef = getTypeReference(declaration)
    if (typeRef != null) {
        if (oldTypeRef != null) {
            return oldTypeRef.replace(typeRef) as KtTypeReference
        }
        else {
            val anchor = addAfter ?: declaration.nameIdentifier?.siblings(forward = true)?.firstOrNull { it is PsiErrorElement }
            val newTypeRef = declaration.addAfter(typeRef, anchor) as KtTypeReference
            declaration.addAfter(KtPsiFactory(declaration.project).createColon(), anchor)
            return newTypeRef
        }
    }
    else {
        if (oldTypeRef != null) {
            val colon = declaration.colon!!
            val removeFrom = colon.prevSibling as? PsiWhiteSpace ?: colon
            declaration.deleteChildRange(removeFrom, oldTypeRef)
        }
        return null
    }
}

fun KtCallableDeclaration.setReceiverTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
    val needParentheses = typeRef != null && typeRef.typeElement is KtFunctionType && !typeRef.hasParentheses()
    val oldTypeRef = receiverTypeReference
    if (typeRef != null) {
        val newTypeRef =
                if (oldTypeRef != null) {
                    oldTypeRef.replace(typeRef) as KtTypeReference
                }
                else {
                    val anchor = nameIdentifier ?: valueParameterList
                    val newTypeRef = addBefore(typeRef, anchor) as KtTypeReference
                    addAfter(KtPsiFactory(project).createDot(), newTypeRef)
                    newTypeRef
                }
        if (needParentheses) {
            val argList = KtPsiFactory(project).createCallArguments("()")
            newTypeRef.addBefore(argList.leftParenthesis!!, newTypeRef.firstChild)
            newTypeRef.add(argList.rightParenthesis!!)
        }
        return newTypeRef
    }
    else {
        if (oldTypeRef != null) {
            val dot = oldTypeRef.siblings(forward = true).firstOrNull { it.node.elementType == KtTokens.DOT }
            deleteChildRange(oldTypeRef, dot ?: oldTypeRef)
        }
        return null
    }
}