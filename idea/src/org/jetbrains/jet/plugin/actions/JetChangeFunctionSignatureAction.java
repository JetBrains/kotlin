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
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

/**
 * Changes method signature to one of provided signatures.
 * Based on {@link JetAddImportAction}
 */
public class JetChangeFunctionSignatureAction implements QuestionAction {

    private final Project project;
    private final Editor editor;
    private final JetNamedFunction element;
    private final List<FunctionDescriptor> signatures;

    /**
     * @param project Project where action takes place.
     * @param editor Editor where modification should be done.
     * @param element Function element which signature should be changed.
     * @param signatures Variants for new function signature.
     */
    public JetChangeFunctionSignatureAction(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull JetNamedFunction element,
            @NotNull Collection<FunctionDescriptor> signatures
    ) {
        this.project = project;
        this.editor = editor;
        this.element = element;
        this.signatures = new ArrayList<FunctionDescriptor>(signatures);
    }

    @Override
    public boolean execute() {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        if (!element.isValid() || signatures.isEmpty()) {
            return false;
        }

        if (signatures.size() == 1 || !editor.getComponent().isShowing()) {
            changeSignature(element, project, signatures.get(0));
        }
        else {
            chooseSignatureAndChange();
        }

        return true;
    }

    private BaseListPopupStep getSignaturePopup() {
        return new BaseListPopupStep<FunctionDescriptor>(
                JetBundle.message("change.function.signature.chooser.title"), signatures) {
            @Override
            public boolean isAutoSelectionEnabled() {
                return false;
            }

            @Override
            public PopupStep onChosen(FunctionDescriptor selectedValue, boolean finalChoice) {
                if (finalChoice) {
                    changeSignature(element, project, selectedValue);
                }
                return FINAL_CHOICE;
            }

            @Override
            public Icon getIconFor(FunctionDescriptor aValue) {
                return PlatformIcons.FUNCTION_ICON;
            }

            @NotNull
            @Override
            public String getTextFor(FunctionDescriptor aValue) {
                return CodeInsightUtils.createFunctionSignatureStringFromDescriptor(
                        aValue,
                        /* shortTypeNames = */ true);
            }
        };
    }

    private static void changeSignature(final JetNamedFunction element, Project project, FunctionDescriptor signature) {
        final String signatureString = CodeInsightUtils.createFunctionSignatureStringFromDescriptor(
                signature,
                /* shortTypeNames = */ false);

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        final JetPsiFactory psiFactory = JetPsiFactory(element);
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        JetExpression bodyExpression = element.getBodyExpression();
                        JetNamedFunction newElement;

                        if (bodyExpression != null) {
                            if (element.hasBlockBody()) {
                                newElement = psiFactory.createFunction(signatureString + "{}");
                            }
                            else {
                                newElement = psiFactory.createFunction(signatureString + "= \"dummy\"");
                            }
                            JetExpression newBodyExpression = newElement.getBodyExpression();
                            assert newBodyExpression != null;
                            newBodyExpression.replace(bodyExpression);
                        }
                        else {
                            newElement = psiFactory.createFunction(signatureString);
                        }
                        newElement = (JetNamedFunction) element.replace(newElement);
                        ShortenReferences.instance$.process(newElement);
                    }
                });
            }
        }, JetBundle.message("change.function.signature.action"), null);
    }

    private void chooseSignatureAndChange() {
        JBPopupFactory.getInstance().createListPopup(getSignaturePopup()).showInBestPositionFor(editor);
    }
}
