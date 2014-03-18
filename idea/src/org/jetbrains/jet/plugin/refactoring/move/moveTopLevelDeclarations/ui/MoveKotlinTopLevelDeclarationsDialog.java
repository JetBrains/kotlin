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
package org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameHelper;
import com.intellij.refactoring.JavaRefactoringSettings;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.move.moveClassesOrPackages.DestinationFolderComboBox;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle;
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsOptions;
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsProcessor;

import javax.swing.*;
import java.util.List;

public class MoveKotlinTopLevelDeclarationsDialog extends RefactoringDialog {
    private static final String RECENTS_KEY = "MoveKotlinTopLevelDeclarationsDialog.RECENTS_KEY";

    private JLabel elementDescription;
    private JCheckBox cbSearchInComments;
    private JCheckBox cbSearchTextOccurences;
    private JPanel mainPanel;
    private ReferenceEditorComboWithBrowseButton classPackageChooser;
    private ComboboxWithBrowseButton destinationFolderCB;
    private JPanel targetPanel;
    private JLabel targetDestinationLabel;

    private final List<JetNamedDeclaration> elementsToMove;
    private final MoveCallback moveCallback;

    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull List<JetNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            boolean searchInComments,
            boolean searchForTextOccurences,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        this.elementsToMove = elementsToMove;
        this.moveCallback = moveCallback;

        init();

        setTitle(MoveHandler.REFACTORING_NAME);

        if (elementsToMove.size() == 1) {
            PsiElement element = elementsToMove.get(0);
            elementDescription.setText(
                    JetRefactoringBundle.message(
                            "refactoring.move.specifc.element",
                            UsageViewUtil.getType(element),
                            UsageViewUtil.getLongName(element)
                    )
            );
        }
        else if (elementsToMove.size() > 1) {
            elementDescription.setText(JetRefactoringBundle.message("refactoring.move.selected.elements"));
        }

        if (targetPackageName != null && targetPackageName.length() != 0) {
            classPackageChooser.prependItem(targetPackageName);
        }

        cbSearchInComments.setSelected(searchInComments);
        cbSearchTextOccurences.setSelected(searchForTextOccurences);

        ((DestinationFolderComboBox) destinationFolderCB).setData(
                myProject,
                targetDirectory,
                new Pass<String>() {
                    @Override
                    public void pass(String s) {
                        setErrorText(s);
                    }
                },
                classPackageChooser.getChildComponent()
        );

        UIUtil.setEnabled(targetPanel, hasAnySourceRoots(), true);
        validateButtons();
    }

    private void createUIComponents() {
        classPackageChooser = createPackageChooser();

        destinationFolderCB = new DestinationFolderComboBox() {
            @Override
            public String getTargetPackage() {
                return MoveKotlinTopLevelDeclarationsDialog.this.getTargetPackage();
            }
        };
    }

    private ReferenceEditorComboWithBrowseButton createPackageChooser() {
        ReferenceEditorComboWithBrowseButton packageChooser =
                new PackageNameReferenceEditorCombo("", myProject, RECENTS_KEY, RefactoringBundle.message("choose.destination.package"));
        Document document = packageChooser.getChildComponent().getDocument();
        document.addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                validateButtons();
            }
        });

        return packageChooser;
    }

    private boolean hasAnySourceRoots() {
        return !JavaProjectRootsUtil.getSuitableDestinationSourceRoots(myProject).isEmpty();
    }

    @Override
    protected JComponent createCenterPanel() {
        boolean isDestinationVisible = hasAnySourceRoots();
        destinationFolderCB.setVisible(isDestinationVisible);
        targetDestinationLabel.setVisible(isDestinationVisible);
        return null;
    }

    @Override
    protected JComponent createNorthPanel() {
        return mainPanel;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#com.intellij.refactoring.move.moveClassesOrPackages.MoveKotlinTopLevelDeclarationsDialog.classes";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return classPackageChooser.getChildComponent();
    }

    private void saveRefactoringSettings() {
        JavaRefactoringSettings refactoringSettings = JavaRefactoringSettings.getInstance();
        refactoringSettings.MOVE_SEARCH_IN_COMMENTS = isSearchInComments();
        refactoringSettings.MOVE_SEARCH_FOR_TEXT = isSearchInNonJavaFiles();
        refactoringSettings.MOVE_PREVIEW_USAGES = isPreviewUsages();
    }

    @Nullable
    private MoveDestination selectDestination() {
        String packageName = getTargetPackage().trim();

        if (packageName.length() > 0 && !PsiNameHelper.getInstance(myProject).isQualifiedName(packageName)) {
            Messages.showErrorDialog(myProject, RefactoringBundle.message("please.enter.a.valid.target.package.name"),
                                     RefactoringBundle.message("move.title"));
            return null;
        }

        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, packageName);
        PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
        if (!targetPackage.exists()) {
            int ret = Messages.showYesNoDialog(myProject, RefactoringBundle.message("package.does.not.exist", packageName),
                                               RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
            if (ret != Messages.YES) return null;
        }

        return ((DestinationFolderComboBox) destinationFolderCB).selectDirectory(targetPackage, false);
    }

    @Override
    protected void canRun() throws ConfigurationException {
        String name = getTargetPackage().trim();
        if (name.length() != 0 && !PsiNameHelper.getInstance(myProject).isQualifiedName(name)) {
            throw new ConfigurationException("\'" + name + "\' is invalid destination package name");
        }
    }

    protected String getTargetPackage() {
        return classPackageChooser.getText();
    }

    @Override
    protected void doAction() {
        MoveDestination destination = selectDestination();
        if (destination == null) return;

        saveRefactoringSettings();
        for (PsiElement element : elementsToMove) {
            String message = destination.verify(element.getContainingFile());
            if (message != null) {
                CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), message, null, myProject);
                return;
            }
        }

        try {
            MoveKotlinTopLevelDeclarationsOptions options = new MoveKotlinTopLevelDeclarationsOptions(
                    elementsToMove, destination, isSearchInComments(), isSearchInNonJavaFiles(), moveCallback
            );
            invokeRefactoring(new MoveKotlinTopLevelDeclarationsProcessor(myProject, options));
        }
        catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), null, myProject);
        }
    }

    protected final boolean isSearchInNonJavaFiles() {
        return cbSearchTextOccurences.isSelected();
    }

    protected final boolean isSearchInComments() {
        return cbSearchInComments.isSelected();
    }
}