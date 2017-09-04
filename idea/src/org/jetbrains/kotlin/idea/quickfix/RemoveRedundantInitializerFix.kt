/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtProperty

class RemoveRedundantInitializerFix(element: KtProperty) : RemovePartsFromPropertyFix(element, true, false, false) {

    override fun getText(): String = "Remove redundant initializer"

    override fun getFamilyName(): String = "Remove redundant initializer"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtProperty>? {
            val element = diagnostic.psiElement
            val property = PsiTreeUtil.getParentOfType(element, KtProperty::class.java) ?: return null
            return RemoveRedundantInitializerFix(property)
        }
    }

}