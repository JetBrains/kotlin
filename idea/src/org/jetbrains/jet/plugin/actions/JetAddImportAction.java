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

package org.jetbrains.jet.plugin.actions;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.actions.AddImportAction;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

import javax.swing.*;
import java.util.List;

/**
 * Automatically adds import directive to the file for resolving reference.
 * Based on {@link AddImportAction}
 *
 * @author Nikolay Krasko
 */
public class JetAddImportAction implements QuestionAction {

    private final Project myProject;
    private final Editor myEditor;
    private final PsiElement myElement;
    private final List<FqName> possibleImports;

    /**
     * @param project Project where action takes place.
     * @param editor Editor where modification should be done.
     * @param element Element with unresolved reference.
     * @param imports Variants for resolution.
     */
    public JetAddImportAction(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull PsiElement element,
            @NotNull Iterable<FqName> imports
    ) {
        myProject = project;
        myEditor = editor;
        myElement = element;
        possibleImports = Lists.newArrayList(imports);
    }

    @Override
    public boolean execute() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        if (!myElement.isValid()){
            return false;
        }

        // TODO: Validate resolution variants. See AddImportAction.execute()

        if (possibleImports.size() == 1){
            addImport(myElement, myProject, possibleImports.get(0));
        }
        else{
            chooseClassAndImport();
        }

        return true;
    }

    protected BaseListPopupStep getImportSelectionPopup() {
        return new BaseListPopupStep<FqName>(JetBundle.message("imports.chooser.title"), possibleImports) {
            @Override
            public boolean isAutoSelectionEnabled() {
                return false;
            }

            @Override
            public PopupStep onChosen(FqName selectedValue, boolean finalChoice) {
                if (selectedValue == null) {
                    return FINAL_CHOICE;
                }

                if (finalChoice) {
                    addImport(myElement, myProject, selectedValue);
                    return FINAL_CHOICE;
                }

                List<String> toExclude = AddImportAction.getAllExcludableStrings(selectedValue.getFqName());

                return new BaseListPopupStep<String>(null, toExclude) {
                    @NotNull
                    @Override
                    public String getTextFor(String value) {
                        return "Exclude '" + value + "' from auto-import";
                    }

                    @Override
                    public PopupStep onChosen(String selectedValue, boolean finalChoice) {
                        if (finalChoice) {
                            AddImportAction.excludeFromImport(myProject, selectedValue);
                        }

                        return super.onChosen(selectedValue, finalChoice);
                    }
                };
            }

            @Override
            public boolean hasSubstep(FqName selectedValue) {
                return true;
            }

            @NotNull
            @Override
            public String getTextFor(FqName value) {
                return value.getFqName();
            }

            @Override
            public Icon getIconFor(FqName aValue) {
                // TODO: change icon
                return PlatformIcons.CLASS_ICON;
            }
        };
    }

    protected static void addImport(final PsiElement element, final Project project, final FqName selectedImport) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        PsiFile file = element.getContainingFile();
                        if (!(file instanceof JetFile)) return;
                        ImportInsertHelper.addImportDirective(selectedImport,
                                (JetFile) file
                        );
                    }
                });
            }
        }, QuickFixBundle.message("add.import"), null);
    }

    private void chooseClassAndImport() {
        JBPopupFactory.getInstance().createListPopup(getImportSelectionPopup()).showInBestPositionFor(myEditor);
    }
}
