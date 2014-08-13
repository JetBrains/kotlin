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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.psi.psiUtil.getParentByTypesAndPredicate
import com.intellij.codeInsight.intention.IntentionAction

public abstract class JetSelfTargetingIntention<T: JetElement>(val key: String, val elementType: Class<T>) : IntentionAction {
    private var myText:String = JetBundle.message(key);
    protected fun setText(text: String) {
        myText = text
    }
    override fun getText(): String = myText

    abstract fun isApplicableTo(element: T): Boolean
    open fun isApplicableTo(element: T, editor: Editor): Boolean = isApplicableTo(element)
    abstract fun applyTo(element: T, editor: Editor)

    protected fun getTarget(editor: Editor, file: PsiFile): T? {
        val offset = editor.getCaretModel().getOffset()
        return file.findElementAt(offset)?.getParentByTypesAndPredicate(false, elementType) { element -> isApplicableTo(element, editor) }
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

    override fun startInWriteAction(): Boolean = true

    override fun toString(): String = getText()
}