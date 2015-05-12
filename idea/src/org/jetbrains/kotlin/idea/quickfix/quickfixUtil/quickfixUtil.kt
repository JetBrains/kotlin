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

package org.jetbrains.kotlin.idea.quickfix.quickfixUtil

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.JetPrimaryConstructor
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

inline fun <reified T : PsiElement> Diagnostic.createIntentionForFirstParentOfType(
    factory: (T) -> JetIntentionAction<T>?
) = getPsiElement().getNonStrictParentOfType<T>()?.let(factory)


fun createIntentionFactory(
    factory: (Diagnostic) -> IntentionAction?
) = object : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic) = factory(diagnostic)
}

public fun JetPrimaryConstructor.addConstructorKeyword(): PsiElement? {
    val keyword = JetPsiFactory(this).createConstructorKeyword()
    return addAfter(keyword, getModifierList() ?: return null)
}
