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

package org.jetbrains.jet.plugin.actions;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.plugin.JetBundle;

import javax.swing.*;
import java.util.List;

/**
 * Changes method signature to one of provided signatures.
 * Based on {@link JetAddImportAction}
 */
public class JetChangeMethodSignatureAction implements QuestionAction {

    private final Project myProject;
    private final Editor myEditor;
    private final JetNamedFunction myElement;
    private final List<JetNamedFunction> possibleSignatures;

    /**
     * @param project Project where action takes place.
     * @param editor Editor where modification should be done.
     * @param element Function element which signature should be changed.
     * @param signatures Variants for new function signature.
     */
    public JetChangeMethodSignatureAction(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull JetNamedFunction element,
            @NotNull Iterable<JetNamedFunction> signatures
    ) {
        myProject = project;
        myEditor = editor;
        myElement = element;
        possibleSignatures = Lists.newArrayList(signatures);
    }

    @Override
    public boolean execute() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        if (!myElement.isValid()) {
            return false;
        }

        if (possibleSignatures.size() == 1) {
            changeSignature(myElement, myProject, possibleSignatures.get(0));
        }
        else {
            chooseSignatureAndChange();
        }

        return true;
    }

    protected BaseListPopupStep getSignaturePopup() {
        return new BaseListPopupStep<JetNamedFunction>(
                JetBundle.message("signature.change.chooser.title"), possibleSignatures) {
            @Override
            public boolean isAutoSelectionEnabled() {
                return false;
            }

            @Override
            public PopupStep onChosen(JetNamedFunction selectedValue, boolean finalChoice) {
                if (finalChoice) {
                    changeSignature(myElement, myProject, selectedValue);
                }
                return FINAL_CHOICE;
            }

            @NotNull
            @Override
            public String getTextFor(JetNamedFunction value) {
                return value.getText().trim();
            }

            @Override
            public Icon getIconFor(JetNamedFunction aValue) {
                return PlatformIcons.FUNCTION_ICON;
            }
        };
    }

    protected static void changeSignature(final JetNamedFunction element, final Project project, final JetNamedFunction signature) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        JetExpression bodyExpression = element.getBodyExpression();
                        JetNamedFunction newElement;

                        if (bodyExpression != null) {
                            if (element.getEqualsToken() != null) {
                                newElement = JetPsiFactory.createFunction(project, signature.getText() + "= 0");
                            }
                            else {
                                newElement = JetPsiFactory.createFunction(project, signature.getText() + "{}");
                            }
                            JetExpression newBodyExpression = newElement.getBodyExpression();
                            assert newBodyExpression != null;
                            newBodyExpression.replace(bodyExpression);
                        } else {
                            newElement = JetPsiFactory.createFunction(project, signature.getText() + ";");
                        }
                        element.replace(newElement);

                    }
                });
            }
        }, JetBundle.message("change.method.signature.action"), null);
    }

    private void chooseSignatureAndChange() {
        JBPopupFactory.getInstance().createListPopup(getSignaturePopup()).showInBestPositionFor(myEditor);
    }
}
