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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;

public class CreateClassObjectFromUsageFix extends CreateFromUsageFixBase {
    private final JetClass klass;

    public CreateClassObjectFromUsageFix(@NotNull PsiElement element, @NotNull JetClass klass) {
        super(element);
        this.klass = klass;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("create.class.object.from.usage");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        PsiFile containingFile = klass.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        assert virtualFile != null;
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(virtualFile, true);

        JetClassBody classBody = klass.getBody();
        if (classBody == null) {
            classBody = (JetClassBody) klass.add(JetPsiFactory.createEmptyClassBody(project));
            klass.addBefore(JetPsiFactory.createWhiteSpace(project), classBody);
        }
        PsiElement lBrace = classBody.getLBrace();
        JetClassObject classObject = (JetClassObject) classBody.addAfter(JetPsiFactory.createEmptyClassObject(project), lBrace);
        classBody.addBefore(JetPsiFactory.createNewLine(project), classObject);
    }

    @NotNull
    public static JetIntentionActionFactory createCreateClassObjectFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetReferenceExpression refExpr = QuickFixUtil.getParentElementOfType(diagnostic, JetReferenceExpression.class);
                if (refExpr == null) return null;
                PsiReference reference = refExpr.getReference();
                if (reference == null) return null;
                PsiElement resolvedReference = reference.resolve();
                if (resolvedReference == null || !(resolvedReference instanceof JetClass)) return null;
                JetClass klass = (JetClass) resolvedReference;
                if (!klass.isWritable()) return null;
                return new CreateClassObjectFromUsageFix(refExpr, (JetClass) resolvedReference);
            }
        };
    }
}
