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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.codeInsight.TargetElementEvaluatorEx
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilExtender
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAbstract

class KotlinTargetElementEvaluator : TargetElementEvaluatorEx {
    override fun includeSelfInGotoImplementation(element: PsiElement): Boolean = !(element is KtClass && element.isAbstract())

    override fun getElementByReference(ref: PsiReference, flags: Int): PsiElement? {
        // prefer destructing declaration entry to its target if element name is accepted
        if (ref is KtDestructuringDeclarationReference && flags.and(TargetElementUtil.ELEMENT_NAME_ACCEPTED) != 0) {
            return ref.element
        }
        return null
    }

    override fun isIdentifierPart(file: PsiFile, text: CharSequence?, offset: Int): Boolean {
        // '(' is considered identifier part if it belongs to primary constructor without 'constructor' keyword
        return file.findElementAt(offset)?.getNonStrictParentOfType<KtPrimaryConstructor>()?.textOffset == offset
    }
}
