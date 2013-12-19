/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import jet.Function1
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.psi.psiUtil.getParentByTypesAndPredicate

public abstract class JetSelfTargetingIntention<T: JetElement>(val key: String, val elementType: Class<T>) : BaseIntentionAction() {
    {
        setText(JetBundle.message(key))
    }

    protected abstract fun isApplicableTo(element: T): Boolean
    protected abstract fun applyTo(element: T, editor: Editor)

    private fun getTarget(editor: Editor, file: PsiFile): T? {
        val offset = editor.getCaretModel().getOffset()
        return file.findElementAt(offset)?.getParentByTypesAndPredicate(false, elementType) { element -> isApplicableTo(element) }
    }

    public override fun getFamilyName(): String {
        return JetBundle.message(key + ".family")
    }

    public override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        return getTarget(editor, file) != null
    }

    public override fun invoke(project: Project, editor: Editor, file: PsiFile): Unit {
        val target = getTarget(editor, file)
        assert(target != null, "Intention is not applicable")

        applyTo(target!!, editor)
    }
}
