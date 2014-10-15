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
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.*;
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
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle;
import org.jetbrains.jet.plugin.refactoring.RefactoringPackage;
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
    private JRadioButton rbMoveToPackage;
    private JRadioButton rbMoveToFile;
    private TextFieldWithBrowseButton fileChooser;

    private final List<JetNamedDeclaration> elementsToMove;
    private final MoveCallback moveCallback;

    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull List<JetNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            @Nullable JetFile targetFile,
            boolean moveToPackage,
            boolean searchInComments,
            boolean searchForTextOccurences,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        this.elementsToMove = elementsToMove;
        this.moveCallback = moveCallback;

        init();

        setTitle(MoveHandler.REFACTORING_NAME);

        initElementDescription(elementsToMove);

        initSearchOptions(searchInComments, searchForTextOccurences);

        initPackageChooser(targetPackageName, targetDirectory);

        initFileChooser(targetFile);

        initMoveToButtons(moveToPackage);

        updateControls();
    }

    private void initPackageChooser(String targetPackageName, PsiDirectory targetDirectory) {
        if (targetPackageName != null && targetPackageName.length() != 0) {
            classPackageChooser.prependItem(targetPackageName);
        }

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
    }

    private void initSearchOptions(boolean searchInComments, boolean searchForTextOccurences) {
        cbSearchInComments.setSelected(searchInComments);
        cbSearchTextOccurences.setSelected(searchForTextOccurences);
    }

    private void initMoveToButtons(boolean moveToPackage) {
        if (moveToPackage) {
            rbMoveToPackage.setSelected(true);
        }
        else {
            rbMoveToFile.setSelected(true);
        }

        rbMoveToPackage.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        classPackageChooser.requestFocus();
                        updateControls();
                    }
                }
        );

        rbMoveToFile.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        fileChooser.requestFocus();
                        updateControls();
                    }
                }
        );
    }

    private void initFileChooser(JetFile targetFile) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
        descriptor.setRoots(ProjectRootManager.getInstance(myProject).getContentRoots());
        descriptor.setIsTreeRootVisible(true);

        String title = JetRefactoringBundle.message("refactoring.move.top.level.declaration.file.title");
        fileChooser.addBrowseFolderListener(title, null, myProject, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        if (targetFile != null) {
            fileChooser.setText(targetFile.getVirtualFile().getPath());
        }
    }

    private void initElementDescription(List<JetNamedDeclaration> elementsToMove) {
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

    private void updateControls() {
        boolean moveToPackage = isMoveToPackage();
        classPackageChooser.setEnabled(moveToPackage);
        fileChooser.setEnabled(!moveToPackage);
        UIUtil.setEnabled(targetPanel, moveToPackage && hasAnySourceRoots(), true);
        validateButtons();
    }

    private boolean hasAnySourceRoots() {
        return !JavaProjectRootsUtil.getSuitableDestinationSourceRoots(myProject).isEmpty();
    }

    private void saveRefactoringSettings() {
        JavaRefactoringSettings refactoringSettings = JavaRefactoringSettings.getInstance();
        refactoringSettings.MOVE_SEARCH_IN_COMMENTS = isSearchInComments();
        refactoringSettings.MOVE_SEARCH_FOR_TEXT = isSearchInNonJavaFiles();
        refactoringSettings.MOVE_PREVIEW_USAGES = isPreviewUsages();
    }

    @Nullable
    private KotlinMoveTarget selectMoveTarget() {
        String message = verifyBeforeRun();
        if (message != null) {
            setErrorText(message);
            return null;
        }

        setErrorText(null);

        if (isMoveToPackage()) {
            String packageName = getTargetPackage().trim();

            RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, packageName);
            PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
            if (!targetPackage.exists()) {
                int ret = Messages.showYesNoDialog(myProject, RefactoringBundle.message("package.does.not.exist", packageName),
                                                   RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
                if (ret != Messages.YES) return null;
            }

            MoveDestination moveDestination = ((DestinationFolderComboBox) destinationFolderCB).selectDirectory(targetPackage, false);
            return moveDestination != null ? new MoveDestinationKotlinMoveTarget(moveDestination) : null;
        }

        JetFile jetFile = (JetFile) RefactoringPackage.toPsiFile(new File(getTargetFilePath()), myProject);
        assert jetFile != null : "Non-Kotlin files must be filtered out before starting the refactoring";

        return new JetFileKotlinMoveTarget(jetFile);
    }

    @Nullable
    private String verifyBeforeRun() {
        if (isMoveToPackage()) {
            String name = getTargetPackage().trim();
            if (name.length() != 0 && !PsiNameHelper.getInstance(myProject).isQualifiedName(name)) {
                return "\'" + name + "\' is invalid destination package name";
            }
        }
        else {
            PsiFile targetFile = RefactoringPackage.toPsiFile(new File(getTargetFilePath()), myProject);
            if (!(targetFile instanceof JetFile)) {
                return JetRefactoringBundle.message("refactoring.move.non.kotlin.file");
            }
        }

        return null;
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

    protected final String getTargetPackage() {
        return classPackageChooser.getText();
    }

    protected final String getTargetFilePath() {
        return fileChooser.getText();
    }

    @Override
    protected void canRun() throws ConfigurationException {
        String message = verifyBeforeRun();
        if (message != null) {
            throw new ConfigurationException(message);
        }
    }

    @Override
    protected void doAction() {
        KotlinMoveTarget target = selectMoveTarget();
        if (target == null) return;

        saveRefactoringSettings();
        for (PsiElement element : elementsToMove) {
            String message = target.verify(element.getContainingFile());
            if (message != null) {
                CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), message, null, myProject);
                return;
            }
        }

        try {
            MoveKotlinTopLevelDeclarationsOptions options = new MoveKotlinTopLevelDeclarationsOptions(
                    elementsToMove, target, isSearchInComments(), isSearchInNonJavaFiles(), true, moveCallback
            );
            invokeRefactoring(new MoveKotlinTopLevelDeclarationsProcessor(myProject, options, Mover.Default.INSTANCE$));
        }
        catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), null, myProject);
        }
    }

    private boolean isSearchInNonJavaFiles() {
        return cbSearchTextOccurences.isSelected();
    }

    private boolean isSearchInComments() {
        return cbSearchInComments.isSelected();
    }

    private boolean isMoveToPackage() {
        return rbMoveToPackage.isSelected();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return classPackageChooser.getChildComponent();
    }
}