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

package org.jetbrains.jet.plugin.refactoring.rename

import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod
import org.jetbrains.jet.lang.psi.JetNamedDeclaration

abstract class RenameKotlinPsiProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element is JetNamedDeclaration

    override fun findCollisions(
            element: PsiElement?,
            newName: String?,
            allRenames: Map<out PsiElement?, String>,
            result: MutableList<UsageInfo>
    ) {
        checkConflictsAndReplaceUsageInfos(result)
    }
}
