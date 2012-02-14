/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.introduceVariable;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;

/**
 * User: Alefas
 * Date: 01.02.12
 */
public class JetInplaceVariableIntroducer extends InplaceVariableIntroducer<JetExpression> {

    private boolean myReplaceOccurrence;
    private JetProperty myProperty;

    public JetInplaceVariableIntroducer(PsiNamedElement elementToRename, Editor editor, Project project,
                                        String title, JetExpression[] occurrences,
                                        @Nullable JetExpression expr, boolean replaceOccurrence,
                                        JetProperty property) {
        super(elementToRename, editor, project, title, occurrences, expr);
        this.myReplaceOccurrence = replaceOccurrence;
        myProperty = property;
    }

    @Override
    protected void moveOffsetAfter(boolean success) {
        if (!myReplaceOccurrence || myExprMarker == null) {
            myEditor.getCaretModel().moveToOffset(myProperty.getTextRange().getEndOffset());
        } else {
            int startOffset = myExprMarker.getStartOffset();
            PsiFile file = myProperty.getContainingFile();
            PsiElement elementAt = file.findElementAt(startOffset);
            if (elementAt != null) {
                myEditor.getCaretModel().moveToOffset(elementAt.getTextRange().getEndOffset());
            } else {
                myEditor.getCaretModel().moveToOffset(myExprMarker.getEndOffset());
            }
        }
    }
}
