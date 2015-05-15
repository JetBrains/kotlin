/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.ui;

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
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.*;
import com.intellij.refactoring.*;
import com.intellij.refactoring.classMembers.DependencyMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoChangeListener;
import com.intellij.refactoring.classMembers.UsesMemberDependencyGraph;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.move.moveClassesOrPackages.DestinationFolderComboBox;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.UniqueNameGenerator;
import com.intellij.util.ui.UIUtil;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.core.refactoring.RefactoringPackage;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;
import org.jetbrains.kotlin.idea.refactoring.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.move.MovePackage;
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.*;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinMemberSelectionPanel;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinMemberSelectionTable;
import org.jetbrains.kotlin.idea.util.application.ApplicationPackage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MoveKotlinTopLevelDeclarationsDialog extends RefactoringDialog {
    private static final String RECENTS_KEY = "MoveKotlinTopLevelDeclarationsDialog.RECENTS_KEY";

    private class MemberInfoModelImpl extends DependencyMemberInfoModel<JetNamedDeclaration, KotlinMemberInfo> {
        public MemberInfoModelImpl() {
            super(new UsesMemberDependencyGraph<JetNamedDeclaration, JetFile, KotlinMemberInfo>(sourceFile, null, false), WARNING);
        }

        @Override
        @Nullable
        public Boolean isFixedAbstract(KotlinMemberInfo member) {
            return null;
        }

        @Override
        public boolean isCheckedWhenDisabled(KotlinMemberInfo member) {
            return false;
        }
    }

    private JCheckBox cbSearchInComments;
    private JCheckBox cbSearchTextOccurrences;
    private JPanel mainPanel;
    private ReferenceEditorComboWithBrowseButton classPackageChooser;
    private ComboboxWithBrowseButton destinationFolderCB;
    private JPanel targetPanel;
    private JRadioButton rbMoveToPackage;
    private JRadioButton rbMoveToFile;
    private TextFieldWithBrowseButton fileChooser;
    private JPanel memberInfoPanel;
    private JTextField tfFileNameInPackage;
    private JCheckBox cbSpecifyFileNameInPackage;
    private KotlinMemberSelectionTable memberTable;

    private final JetFile sourceFile;
    private final MoveCallback moveCallback;

    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull JetFile sourceFile,
            @NotNull Set<JetNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            @Nullable JetFile targetFile,
            boolean moveToPackage,
            boolean searchInComments,
            boolean searchForTextOccurences,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        this.sourceFile = sourceFile;
        this.moveCallback = moveCallback;

        init();

        setTitle(MoveHandler.REFACTORING_NAME);

        initSearchOptions(searchInComments, searchForTextOccurences);

        initPackageChooser(targetPackageName, targetDirectory);

        initFileChooser(targetFile);

        initMoveToButtons(moveToPackage);

        initMemberInfo(elementsToMove);

        updateControls();

        pack();
    }

    private void initMemberInfo(@NotNull final Set<JetNamedDeclaration> elementsToMove) {
        final List<KotlinMemberInfo> memberInfos = KotlinPackage.map(
                KotlinPackage.filterIsInstance(sourceFile.getDeclarations(), JetNamedDeclaration.class),
                new Function1<JetNamedDeclaration, KotlinMemberInfo>() {
                    @Override
                    public KotlinMemberInfo invoke(JetNamedDeclaration declaration) {
                        KotlinMemberInfo memberInfo = new KotlinMemberInfo(declaration);
                        memberInfo.setChecked(elementsToMove.contains(declaration));
                        return memberInfo;
                    }
                }
        );
        KotlinMemberSelectionPanel selectionPanel = new KotlinMemberSelectionPanel(getTitle(), memberInfos, null);
        memberTable = selectionPanel.getTable();
        MemberInfoModelImpl memberInfoModel = new MemberInfoModelImpl();
        memberInfoModel.memberInfoChanged(new MemberInfoChange<JetNamedDeclaration, KotlinMemberInfo>(memberInfos));
        selectionPanel.getTable().setMemberInfoModel(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(
                new MemberInfoChangeListener<JetNamedDeclaration, KotlinMemberInfo>() {
                    private boolean shouldUpdateFileNameField(final Collection<KotlinMemberInfo> changedMembers) {
                        if (!tfFileNameInPackage.isEnabled()) return true;

                        Collection<JetNamedDeclaration> previousDeclarations = KotlinPackage.filterNotNull(
                                KotlinPackage.map(
                                        memberInfos,
                                        new Function1<KotlinMemberInfo, JetNamedDeclaration>() {
                                            @Override
                                            public JetNamedDeclaration invoke(KotlinMemberInfo info) {
                                                return changedMembers.contains(info) != info.isChecked() ? info.getMember() : null;
                                            }
                                        }
                                )
                        );
                        String suggestedText = previousDeclarations.isEmpty()
                                               ? ""
                                               : MovePackage.guessNewFileName(sourceFile, previousDeclarations);
                        return tfFileNameInPackage.getText().equals(suggestedText);
                    }

                    @Override
                    public void memberInfoChanged(MemberInfoChange<JetNamedDeclaration, KotlinMemberInfo> event) {
                        // Update file name field only if it user hasn't changed it to some non-default value
                        if (shouldUpdateFileNameField(event.getChangedMembers())) {
                            updateSuggestedFileName();
                        }
                    }
                }
        );
        memberInfoPanel.add(selectionPanel, BorderLayout.CENTER);

    }

    private void updateSuggestedFileName() {
        tfFileNameInPackage.setText(MovePackage.guessNewFileName(sourceFile, getSelectedElementsToMove()));
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

        cbSpecifyFileNameInPackage.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        tfFileNameInPackage.setEnabled(cbSpecifyFileNameInPackage.isSelected());
                    }
                }
        );
    }

    private void initSearchOptions(boolean searchInComments, boolean searchForTextOccurences) {
        cbSearchInComments.setSelected(searchInComments);
        cbSearchTextOccurrences.setSelected(searchForTextOccurences);
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
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withRoots(ProjectRootManager.getInstance(myProject).getContentRoots())
                .withTreeRootVisible(true);

        String title = JetRefactoringBundle.message("refactoring.move.top.level.declaration.file.title");
        fileChooser.addBrowseFolderListener(title, null, myProject, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        if (targetFile != null) {
            fileChooser.setText(targetFile.getVirtualFile().getPath());
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
        updateSuggestedFileName();
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
    private MoveDestination selectPackageBasedMoveDestination() {
        String packageName = getTargetPackage();

        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, packageName);
        PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
        if (!targetPackage.exists()) {
            int ret = Messages.showYesNoDialog(myProject, RefactoringBundle.message("package.does.not.exist", packageName),
                                               RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
            if (ret != Messages.YES) return null;
        }

        return ((DestinationFolderComboBox) destinationFolderCB).selectDirectory(targetPackage, false);
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
            final MoveDestination moveDestination = selectPackageBasedMoveDestination();
            if (moveDestination == null) return null;

            final String targetFileName = tfFileNameInPackage.getText();

            PsiDirectory directory = moveDestination.getTargetIfExists(sourceFile);
            PsiFile targetFile = directory != null ? directory.findFile(targetFileName) : null;
            if (targetFile != null) {
                if (targetFile == sourceFile) {
                    setErrorText("Can't move to the original file");
                    return null;
                }

                String question = "File '" +
                                  directory.getVirtualFile().getPath() +
                                  "/" +
                                  targetFileName +
                                  "' already exists. Do you want to move selected declarations to this file?";
                int ret = Messages.showYesNoDialog(myProject, question, RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
                if (ret != Messages.YES) return null;
            }

            return new DeferredJetFileKotlinMoveTarget(
                    myProject,
                    new FqName(getTargetPackage()),
                    new Function0<JetFile>() {
                        @Override
                        public JetFile invoke() {
                            PsiDirectory directory = moveDestination.getTargetDirectory(sourceFile);
                            return RefactoringPackage.getOrCreateKotlinFile(targetFileName, directory);
                        }
                    }
            );
        }

        final File targetFile = new File(getTargetFilePath());
        JetFile jetFile = (JetFile) RefactoringPackage.toPsiFile(targetFile, myProject);
        if (jetFile != null) {
            if (jetFile == sourceFile) {
                setErrorText("Can't move to the original file");
                return null;
            }

            return new JetFileKotlinMoveTarget(jetFile);
        }

        int ret = Messages.showYesNoDialog(
                myProject,
                JetRefactoringBundle.message("file.does.not.exist", targetFile.getName()),
                RefactoringBundle.message("move.title"),
                Messages.getQuestionIcon()
        );
        if (ret != Messages.YES) return null;

        File targetDir = targetFile.getParentFile();
        final PsiDirectory psiDirectory = RefactoringPackage.toPsiDirectory(targetDir, myProject);
        assert psiDirectory != null : "No directory found: " + targetDir.getPath();

        PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
        if (psiPackage == null) {
            setErrorText("Could not find package corresponding to " + targetDir.getPath());
            return null;
        }

        return new DeferredJetFileKotlinMoveTarget(
                myProject,
                new FqName(psiPackage.getQualifiedName()),
                new Function0<JetFile>() {
                    @Override
                    public JetFile invoke() {
                        return RefactoringPackage.getOrCreateKotlinFile(targetFile.getName(), psiDirectory);
                    }
                }
        );
    }

    @Nullable
    private String verifyBeforeRun() {
        if (memberTable.getSelectedMemberInfos().isEmpty()) return "At least one member must be selected";

        if (isMoveToPackage()) {
            String name = getTargetPackage();
            if (name.length() != 0 && !PsiNameHelper.getInstance(myProject).isQualifiedName(name)) {
                return "\'" + name + "\' is invalid destination package name";
            }
        }
        else {
            PsiFile targetFile = RefactoringPackage.toPsiFile(new File(getTargetFilePath()), myProject);
            if (!(targetFile == null || targetFile instanceof JetFile)) {
                return JetRefactoringBundle.message("refactoring.move.non.kotlin.file");
            }
        }

        if (tfFileNameInPackage.getText().isEmpty()) {
            return "File name may not be empty";
        }

        return null;
    }

    private List<JetNamedDeclaration> getSelectedElementsToMove() {
        return KotlinPackage.map(
                memberTable.getSelectedMemberInfos(),
                new Function1<KotlinMemberInfo, JetNamedDeclaration>() {
                    @Override
                    public JetNamedDeclaration invoke(KotlinMemberInfo info) {
                        return info.getMember();
                    }
                }
        );
    }

    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#" + getClass().getName();
    }

    protected final String getTargetPackage() {
        return classPackageChooser.getText().trim();
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

        List<JetNamedDeclaration> elementsToMove = getSelectedElementsToMove();

        for (PsiElement element : elementsToMove) {
            String message = target.verify(element.getContainingFile());
            if (message != null) {
                CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), message, null, myProject);
                return;
            }
        }

        try {
            boolean deleteSourceFile = false;
            
            if (elementsToMove.size() == sourceFile.getDeclarations().size()) {
                if (isMoveToPackage()) {
                    final MoveDestination moveDestination = selectPackageBasedMoveDestination();
                    //noinspection ConstantConditions
                    PsiDirectory targetDir = moveDestination.getTargetIfExists(sourceFile);
                    final String targetFileName = tfFileNameInPackage.getText();
                    if (targetDir == null || targetDir.findFile(targetFileName) == null) {
                        //noinspection ConstantConditions
                        final String temporaryName = UniqueNameGenerator.generateUniqueName(
                                "temp",
                                "",
                                ".kt",
                                KotlinPackage.map(
                                        sourceFile.getContainingDirectory().getFiles(),
                                        new Function1<PsiFile, String>() {
                                            @Override
                                            public String invoke(PsiFile file) {
                                                return file.getName();
                                            }
                                        }
                                )
                        );
                        PsiDirectory targetDirectory = ApplicationPackage.runWriteAction(
                                new Function0<PsiDirectory>() {
                                    @Override
                                    public PsiDirectory invoke() {
                                        sourceFile.setName(temporaryName);
                                        return moveDestination.getTargetDirectory(sourceFile);
                                    }
                                }
                        );
                        invokeRefactoring(
                                new MoveFilesOrDirectoriesProcessor(
                                        myProject,
                                        new PsiElement[] {sourceFile},
                                        targetDirectory,
                                        RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE,
                                        isSearchInComments(),
                                        isSearchInNonJavaFiles(),
                                        new MoveCallback() {
                                            @Override
                                            public void refactoringCompleted() {
                                                try {
                                                    sourceFile.setName(targetFileName);
                                                }
                                                finally {
                                                    if (moveCallback != null) {
                                                        moveCallback.refactoringCompleted();
                                                    }
                                                }
                                            }
                                        },
                                        EmptyRunnable.INSTANCE
                                )
                        );
    
                        return;
                    }
    
                    int ret = Messages.showYesNoCancelDialog(
                            myProject,
                            "You are going to move all declarations out of '" + sourceFile.getVirtualFile().getPath() + "'. Do you want to delete this file?",
                            RefactoringBundle.message("move.title"),
                            Messages.getQuestionIcon()
                    );
                    if (ret == Messages.CANCEL) return;
                    deleteSourceFile = ret == Messages.YES;
                }
            }

            MoveKotlinTopLevelDeclarationsOptions options = new MoveKotlinTopLevelDeclarationsOptions(
                    sourceFile, elementsToMove, target, isSearchInComments(), isSearchInNonJavaFiles(), true, deleteSourceFile, moveCallback
            );
            invokeRefactoring(new MoveKotlinTopLevelDeclarationsProcessor(myProject, options, Mover.Default.INSTANCE$));
        }
        catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), null, myProject);
        }
    }

    private boolean isSearchInNonJavaFiles() {
        return cbSearchTextOccurrences.isSelected();
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
