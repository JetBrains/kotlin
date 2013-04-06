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

package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.HashMap;
import java.util.Map;

class AnonymousTemplateEditingListener extends TemplateEditingAdapter {
    private JetReferenceExpression classRef;
    private ClassDescriptor classDescriptor;
    private Editor editor;
    private final PsiFile psiFile;
    
    private static Map<Editor, AnonymousTemplateEditingListener> ourAddedListeners =
            new HashMap<Editor, AnonymousTemplateEditingListener>();

    public AnonymousTemplateEditingListener(PsiFile psiFile, Editor editor) {
        this.psiFile = psiFile;
        this.editor = editor;
    }

    @Override
    public void currentVariableChanged(TemplateState templateState, Template template, int oldIndex, int newIndex) {
        assert templateState.getTemplate() != null;
        TextRange variableRange = templateState.getVariableRange("SUPERTYPE");
        if (variableRange == null) return;
        PsiElement name = psiFile.findElementAt(variableRange.getStartOffset());
        if (name != null && name.getParent() instanceof JetReferenceExpression) {
            JetReferenceExpression ref = (JetReferenceExpression) name.getParent();

            BindingContext bc = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) psiFile)
                    .getBindingContext();
            DeclarationDescriptor descriptor = bc.get(BindingContext.REFERENCE_TARGET, ref);
            if (descriptor instanceof ClassDescriptor) {
                classRef = ref;
                classDescriptor = (ClassDescriptor) descriptor;
            }
        }
    }

    @Override
    public void templateFinished(Template template, boolean brokenOff) {
        ourAddedListeners.remove(editor);

        if (classDescriptor != null) {
            if (classDescriptor.getKind() == ClassKind.CLASS) {
                int placeToInsert = classRef.getTextRange().getEndOffset();
                PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile).insertString(placeToInsert, "()");

                boolean hasConstructorsParameters = false;
                for (ConstructorDescriptor cd : classDescriptor.getConstructors()) {
                    // TODO check for visibility
                    hasConstructorsParameters |= cd.getValueParameters().size() != 0;
                }

                if (hasConstructorsParameters) {
                    editor.getCaretModel().moveToOffset(placeToInsert + 1);
                }
            }

            new ImplementMethodsHandler().invoke(psiFile.getProject(), editor, psiFile, true);
        }
    }
    
    static void registerListener(Editor editor, Project project) {
        if (ourAddedListeners.containsKey(editor)) {
            return;
        }
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        assert psiFile != null;
        TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
        if (templateState != null) {
            AnonymousTemplateEditingListener listener = new AnonymousTemplateEditingListener(psiFile, editor);
            ourAddedListeners.put(editor, listener);
            templateState.addTemplateStateListener(listener);
        }
    }
}
