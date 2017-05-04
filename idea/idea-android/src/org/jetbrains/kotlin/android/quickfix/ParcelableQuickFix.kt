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

package org.jetbrains.kotlin.android.quickfix

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.android.inspections.lint.AndroidLintQuickFix
import org.jetbrains.android.inspections.lint.AndroidQuickfixContexts
import org.jetbrains.android.util.AndroidBundle
import org.jetbrains.kotlin.android.canAddParcelable
import org.jetbrains.kotlin.android.implementParcelable
import org.jetbrains.kotlin.psi.KtClass


class ParcelableQuickFix : AndroidLintQuickFix {
    override fun apply(startElement: PsiElement, endElement: PsiElement, context: AndroidQuickfixContexts.Context) {
        startElement.getTargetClass()?.implementParcelable()
    }

    override fun isApplicable(startElement: PsiElement, endElement: PsiElement, contextType: AndroidQuickfixContexts.ContextType): Boolean =
            startElement.getTargetClass()?.canAddParcelable() ?: false

    override fun getName(): String = AndroidBundle.message("implement.parcelable.intention.text")

    private fun PsiElement.getTargetClass(): KtClass? = PsiTreeUtil.getParentOfType(this, KtClass::class.java, false)
}