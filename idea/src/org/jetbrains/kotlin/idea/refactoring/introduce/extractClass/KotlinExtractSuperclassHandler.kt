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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractClass

import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ui.KotlinExtractSuperclassDialog
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

object KotlinExtractSuperclassHandler : KotlinExtractSuperHandlerBase(false) {
    val REFACTORING_NAME = "Extract Superclass"

    override fun getErrorMessage(klass: KtClassOrObject): String? {
        if (klass is KtClass) {
            if (klass.isInterface()) return RefactoringBundle.message("superclass.cannot.be.extracted.from.an.interface")
            if (klass.isEnum()) return RefactoringBundle.message("superclass.cannot.be.extracted.from.an.enum")
            if (klass.isAnnotation()) return "Superclass cannot be extracted from an annotation class"
        }
        return null
    }

    override fun createDialog(klass: KtClassOrObject, targetParent: PsiElement) =
        KotlinExtractSuperclassDialog(
                originalClass = klass,
                targetParent = targetParent,
                conflictChecker = { checkConflicts(klass, it) },
                refactoring = { ExtractSuperRefactoring(it).performRefactoring() }
        )
}