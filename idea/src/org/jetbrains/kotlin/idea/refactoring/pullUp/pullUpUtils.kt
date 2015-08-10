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

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

fun JetProperty.mustBeAbstractInInterface() =
        hasInitializer() || hasDelegate() || (!hasInitializer() && !hasDelegate() && accessors.isEmpty())

fun JetNamedDeclaration.canMoveMemberToJavaClass(targetClass: PsiClass): Boolean {
    return when (this) {
        is JetProperty -> {
            if (targetClass.isInterface) return false
            if (hasModifier(JetTokens.OPEN_KEYWORD) || hasModifier(JetTokens.ABSTRACT_KEYWORD)) return false
            if (accessors.isNotEmpty() || delegateExpression != null) return false
            true
        }
        is JetNamedFunction -> true
        else -> false
    }
}

fun addMemberToTarget(targetMember: JetNamedDeclaration, targetClass: JetClassOrObject): JetNamedDeclaration {
    val anchor = targetClass.declarations.filterIsInstance(targetMember.javaClass).lastOrNull()
    val movedMember = when {
        anchor == null && targetMember is JetProperty -> targetClass.addDeclarationBefore(targetMember, null)
        else -> targetClass.addDeclarationAfter(targetMember, anchor)
    }
    return movedMember as JetNamedDeclaration
}

fun doAddCallableMember(
        memberCopy: JetCallableDeclaration,
        clashingSuper: JetCallableDeclaration?,
        targetClass: JetClass): JetCallableDeclaration {
    if (clashingSuper != null && clashingSuper.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
        return clashingSuper.replaced(memberCopy)
    }
    return addMemberToTarget(memberCopy, targetClass) as JetCallableDeclaration
}

// TODO: Formatting rules don't apply here for some reason
fun JetNamedDeclaration.addModifierWithSpace(modifier: JetModifierKeywordToken) {
    addModifier(modifier)
    addAfter(JetPsiFactory(this).createWhiteSpace(), modifierList)
}

// TODO: Formatting rules don't apply here for some reason
fun JetNamedDeclaration.addAnnotationWithSpace(annotationEntry: JetAnnotationEntry): JetAnnotationEntry {
    val result = addAnnotationEntry(annotationEntry)
    addAfter(JetPsiFactory(this).createWhiteSpace(), modifierList)
    return result
}

fun JetClass.makeAbstract() {
    if (isInterface() || hasModifier(JetTokens.ABSTRACT_KEYWORD)) return
    addModifierWithSpace(JetTokens.ABSTRACT_KEYWORD)
}