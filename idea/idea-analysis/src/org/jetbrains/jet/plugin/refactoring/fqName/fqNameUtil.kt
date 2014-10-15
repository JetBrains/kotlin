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

package org.jetbrains.jet.plugin.refactoring.fqName

import org.jetbrains.jet.asJava.namedUnwrappedElement
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.name.FqName
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.resolve.name.isOneSegmentFQN
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElement
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetUserType
import org.jetbrains.jet.lang.psi

/**
 * Returns FqName for given declaration (either Java or Kotlin)
 */
public fun PsiElement.getKotlinFqName(): FqName? {
    val element = namedUnwrappedElement
    return when (element) {
        is PsiPackage -> FqName(element.getQualifiedName())
        is PsiClass -> element.getQualifiedName()?.let { FqName(it) }
        is PsiMember -> (element : PsiMember).getName()?.let { name ->
            val prefix = element.getContainingClass()?.getQualifiedName()
            FqName(if (prefix != null) "$prefix.$name" else name)
        }
        is JetNamedDeclaration -> element.getFqName()
        else -> null
    }
}

/**
 * Replace [[JetSimpleNameExpression]] (and its enclosing qualifier) with qualified element given by FqName
 * Result is either the same as original element, or [[JetQualifiedExpression]], or [[JetUserType]]
 * Note that FqName may not be empty
 */
fun JetSimpleNameExpression.changeQualifiedName(fqName: FqName): JetElement {
    assert (!fqName.isRoot(), "Can't set empty FqName for element $this")

    val shortName = fqName.shortName().asString()
    val psiFactory = psi.JetPsiFactory(this)
    val fqNameBase = (getParent() as? JetCallExpression)?.let { parent ->
        val callCopy = parent.copy() as JetCallExpression
        callCopy.getCalleeExpression()!!.replace(psiFactory.createSimpleName(shortName)).getParent()!!.getText()
    } ?: shortName

    val text = if (!fqName.isOneSegmentFQN()) "${fqName.parent().asString()}.$fqNameBase" else fqNameBase

    val elementToReplace = getQualifiedElement()
    return when (elementToReplace) {
        is JetUserType -> {
            val typeText = "$text${elementToReplace.getTypeArgumentList()?.getText() ?: ""}"
            elementToReplace.replace(psiFactory.createType(typeText).getTypeElement()!!)
        }
        else -> elementToReplace.replace(psiFactory.createExpression(text))
    } as JetElement
}