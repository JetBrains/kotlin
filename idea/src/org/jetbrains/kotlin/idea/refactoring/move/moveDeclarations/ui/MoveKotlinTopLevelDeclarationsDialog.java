/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui;

import com.intellij.ide.util.DirectoryChooser;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoChangeListener;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.UIUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.core.PackageUtilsKt;
import org.jetbrains.kotlin.idea.core.util.PhysicalFileSystemUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionTable;
import org.jetbrains.kotlin.idea.refactoring.move.MoveUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinDestinationFolderComboBox;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinFileChooserDialog;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtPureElement;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.idea.roots.ProjectRootUtilsKt.getSuitableDestinationSourceRoots;

public class MoveKotlinTopLevelDeclarationsDialog extends RefactoringDialog {
    private static final String RECENTS_KEY = "MoveKotlinTopLevelDeclarationsDialog.RECENTS_KEY";
    private final MoveCallback moveCallback;
    private final PsiDirectory initialTargetDirectory;
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
    private JCheckBox cbUpdatePackageDirective;
    private JCheckBox cbDeleteEmptySourceFiles;
    private JCheckBox cbSearchReferences;
    private KotlinMemberSelectionTable memberTable;

    private class MemberSelectionerInfoChangeListener implements MemberInfoChangeListener<KtNamedDeclaration, KotlinMemberInfo> {

        private final List<KotlinMemberInfo> memberInfos;

        public MemberSelectionerInfoChangeListener(List<KotlinMemberInfo> memberInfos) {
            this.memberInfos = memberInfos;
        }

        private boolean shouldUpdateFileNameField(Collection<KotlinMemberInfo> changedMembers) {
            if (!tfFileNameInPackage.isEnabled()) return true;

            Collection<KtNamedDeclaration> previousDeclarations = CollectionsKt.filterNotNull(
                    CollectionsKt.map(
                            memberInfos,
                            info -> changedMembers.contains(info) != info.isChecked() ? info.getMember() : null
                    )
            );
            String suggestedText = previousDeclarations.isEmpty() ? "" : MoveUtilsKt.guessNewFileName(previousDeclarations);
            return tfFileNameInPackage.getText().equals(suggestedText);
        }

        @Override
        public void memberInfoChanged(@NotNull MemberInfoChange<KtNamedDeclaration, KotlinMemberInfo> event) {
            updatePackageDirectiveCheckBox();
            updateFileNameInPackageField();
            // Update file name field only if it user hasn't changed it to some non-default value
            if (shouldUpdateFileNameField(event.getChangedMembers())) {
                updateSuggestedFileName();
            }
        }
    }

    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            @Nullable KtFile targetFile,
            boolean moveToPackage,
            boolean searchInComments,
            boolean searchForTextOccurrences,
            boolean deleteEmptySourceFiles,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        List<KtFile> sourceFiles = getSourceFiles(elementsToMove);

        this.moveCallback = moveCallback;
        this.initialTargetDirectory = targetDirectory;

        init();

        setTitle(MoveHandler.REFACTORING_NAME);

        initSearchOptions(searchInComments, searchForTextOccurrences, deleteEmptySourceFiles);

        initPackageChooser(targetPackageName, targetDirectory, sourceFiles);

        initFileChooser(targetFile, elementsToMove, sourceFiles);

        initMoveToButtons(moveToPackage);

        initMemberInfo(elementsToMove, sourceFiles);

        updateControls();
    }

    private static List<KtFile> getSourceFiles(@NotNull Collection<KtNamedDeclaration> elementsToMove) {
        return CollectionsKt.distinct(
                CollectionsKt.map(
                        elementsToMove,
                        KtPureElement::getContainingKtFile
                )
        );
    }

    private static List<KtNamedDeclaration> getAllDeclarations(Collection<KtFile> sourceFiles) {
        return CollectionsKt.filterIsInstance(
                CollectionsKt.flatMap(
                        sourceFiles,
                        KtPsiUtilKt::getFileOrScriptDeclarations
                ),
                KtNamedDeclaration.class
        );
    }

    private static boolean arePackagesAndDirectoryMatched(List<KtFile> sourceFiles) {
        for (KtFile sourceFile : sourceFiles) {
            if (!PackageUtilsKt.packageMatchesDirectoryOrImplicit(sourceFile)) return false;
        }
        return true;
    }

    private void initMemberInfo(
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @NotNull List<KtFile> sourceFiles
    ) {
        List<KotlinMemberInfo> memberInfos = CollectionsKt.map(
                getAllDeclarations(sourceFiles),
                declaration -> {
                    KotlinMemberInfo memberInfo = new KotlinMemberInfo(declaration, false);
                    memberInfo.setChecked(elementsToMove.contains(declaration));
                    return memberInfo;
                }
        );
        KotlinMemberSelectionPanel selectionPanel = new KotlinMemberSelectionPanel(getTitle(), memberInfos, null);
        memberTable = selectionPanel.getTable();
        MemberInfoModelImpl memberInfoModel = new MemberInfoModelImpl();
        memberInfoModel.memberInfoChanged(new MemberInfoChange<>(memberInfos));
        selectionPanel.getTable().setMemberInfoModel(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(new MemberSelectionerInfoChangeListener(memberInfos));
        memberInfoPanel.add(selectionPanel, BorderLayout.CENTER);
    }

    private void updateSuggestedFileName() {
        tfFileNameInPackage.setText(MoveUtilsKt.guessNewFileName(getSelectedElementsToMove()));
    }

    private void updateFileNameInPackageField() {
        boolean movingSingleFileToPackage = rbMoveToPackage.isSelected() && getSourceFiles(getSelectedElementsToMove()).size() == 1;
        cbSpecifyFileNameInPackage.setEnabled(movingSingleFileToPackage);
        tfFileNameInPackage.setEnabled(movingSingleFileToPackage && cbSpecifyFileNameInPackage.isSelected());
    }

    private void initPackageChooser(
            String targetPackageName,
            PsiDirectory targetDirectory,
            List<KtFile> sourceFiles
    ) {
        if (targetPackageName != null) {
            classPackageChooser.prependItem(targetPackageName);
        }

        ((KotlinDestinationFolderComboBox) destinationFolderCB).setData(
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

        cbSpecifyFileNameInPackage.addActionListener(e -> updateFileNameInPackageField());

        cbUpdatePackageDirective.setSelected(arePackagesAndDirectoryMatched(sourceFiles));
    }

    private void initSearchOptions(boolean searchInComments, boolean searchForTextOccurences, boolean deleteEmptySourceFiles) {
        cbSearchInComments.setSelected(searchInComments);
        cbSearchTextOccurrences.setSelected(searchForTextOccurences);
        cbDeleteEmptySourceFiles.setSelected(deleteEmptySourceFiles);
    }

    private void initMoveToButtons(boolean moveToPackage) {
        if (moveToPackage) {
            rbMoveToPackage.setSelected(true);
        }
        else {
            rbMoveToFile.setSelected(true);
        }

        rbMoveToPackage.addActionListener(
                e -> {
                    classPackageChooser.requestFocus();
                    updateControls();
                }
        );

        rbMoveToFile.addActionListener(
                e -> {
                    fileChooser.requestFocus();
                    updateControls();
                }
        );
    }

    private void initFileChooser(
            @Nullable KtFile targetFile,
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @NotNull List<KtFile> sourceFiles
    ) {
        PsiDirectory sourceDir = sourceFiles.get(0).getParent();
        if (sourceDir == null) {
            throw new AssertionError("File chooser initialization failed");
        }

        fileChooser.addActionListener(e -> {
                    KotlinFileChooserDialog dialog = new KotlinFileChooserDialog("Choose Containing File", myProject);

                    File targetFile1 = new File(fileChooser.getText());
                    PsiFile targetPsiFile = PhysicalFileSystemUtilsKt.toPsiFile(targetFile1, myProject);
                    if (targetPsiFile instanceof KtFile) {
                        dialog.select((KtFile) targetPsiFile);
                    }
                    else {
                        PsiDirectory targetDir = PhysicalFileSystemUtilsKt.toPsiDirectory(targetFile1.getParentFile(), myProject);
                        if (targetDir == null) {
                            targetDir = sourceDir;
                        }
                        dialog.selectDirectory(targetDir);
                    }

                    dialog.showDialog();
                    KtFile selectedFile = dialog.isOK() ? dialog.getSelected() : null;
                    if (selectedFile != null) {
                        fileChooser.setText(selectedFile.getVirtualFile().getPath());
                    }
                }
        );

        String initialTargetPath = targetFile != null
                ? targetFile.getVirtualFile().getPath()
                : sourceFiles.get(0).getVirtualFile().getParent().getPath() + "/" + MoveUtilsKt.guessNewFileName(elementsToMove);
        fileChooser.setText(initialTargetPath);
    }

    private void createUIComponents() {
        classPackageChooser = createPackageChooser();

        destinationFolderCB = new KotlinDestinationFolderComboBox() {
            @Override
            public String getTargetPackage() {
                return MoveKotlinTopLevelDeclarationsDialog.this.getTargetPackage();
            }
        };
    }

    private ReferenceEditorComboWithBrowseButton createPackageChooser() {
        return new PackageNameReferenceEditorCombo(
                "",
                myProject,
                RECENTS_KEY,
                RefactoringBundle.message("choose.destination.package")
        );
    }

    private void updateControls() {
        boolean moveToPackage = rbMoveToPackage.isSelected();
        classPackageChooser.setEnabled(moveToPackage);
        updateFileNameInPackageField();
        fileChooser.setEnabled(!moveToPackage);
        updatePackageDirectiveCheckBox();
        UIUtil.setEnabled(targetPanel, moveToPackage && hasAnySourceRoots(), true);
        updateSuggestedFileName();
        myHelpAction.setEnabled(false);
    }

    private boolean isFullFileMove() {
        Map<KtFile, List<KtNamedDeclaration>> fileToElements = CollectionsKt.groupBy(
                getSelectedElementsToMove(),
                KtPureElement::getContainingKtFile
        );
        for (Map.Entry<KtFile, List<KtNamedDeclaration>> entry : fileToElements.entrySet()) {
            if (KtPsiUtilKt.getFileOrScriptDeclarations(entry.getKey()).size() != entry.getValue().size()) return false;
        }
        return true;
    }

    private void updatePackageDirectiveCheckBox() {
        cbUpdatePackageDirective.setEnabled(rbMoveToPackage.isSelected() && isFullFileMove());
    }

    private boolean hasAnySourceRoots() {
        return !getSuitableDestinationSourceRoots(myProject).isEmpty();
    }

    private void saveRefactoringSettings() {
        KotlinRefactoringSettings refactoringSettings = KotlinRefactoringSettings.getInstance();
        refactoringSettings.MOVE_SEARCH_IN_COMMENTS = cbSearchInComments.isSelected();
        refactoringSettings.MOVE_SEARCH_FOR_TEXT = cbSearchTextOccurrences.isSelected();
        refactoringSettings.MOVE_DELETE_EMPTY_SOURCE_FILES = cbDeleteEmptySourceFiles.isSelected();
        refactoringSettings.MOVE_PREVIEW_USAGES = isPreviewUsages();

        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, getTargetPackage());
    }

    private List<KtNamedDeclaration> getSelectedElementsToMove() {
        return CollectionsKt.map(
                memberTable.getSelectedMemberInfos(),
                MemberInfoBase::getMember
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

    private String getTargetPackage() {
        return classPackageChooser.getText().trim();
    }

    private List<KtNamedDeclaration> getSelectedElementsToMoveChecked() throws ConfigurationException {
        List<KtNamedDeclaration> elementsToMove = getSelectedElementsToMove();
        if (elementsToMove.isEmpty()) {
            throw new ConfigurationException("No elements to move are selected");
        }
        return elementsToMove;
    }

    private Model<BaseRefactoringProcessor> getModel() throws ConfigurationException  {

        DirectoryChooser.ItemWrapper selectedItem = (DirectoryChooser.ItemWrapper) destinationFolderCB.getComboBox().getSelectedItem();
        PsiDirectory selectedPsiDirectory = selectedItem != null ? selectedItem.getDirectory() : initialTargetDirectory;

        return new MoveKotlinTopLevelDeclarationsModel(
                myProject,
                getSelectedElementsToMoveChecked(),
                getTargetPackage(),
                selectedPsiDirectory,
                tfFileNameInPackage.getText(),
                fileChooser.getText(),
                rbMoveToPackage.isSelected(),
                cbSearchReferences.isSelected(),
                cbSearchInComments.isSelected(),
                cbSearchTextOccurrences.isSelected(),
                cbDeleteEmptySourceFiles.isSelected(),
                cbUpdatePackageDirective.isSelected(),
                isFullFileMove(),
                moveCallback
        );
    }

    @Override
    protected void doAction() {

        BaseRefactoringProcessor processor;
        try {
            processor = getModel().computeModelResult();
        }
        catch (ConfigurationException e) {
            setErrorText(e.getMessage());
            return;
        }

        saveRefactoringSettings();

        try {
            invokeRefactoring(processor);
        } catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), null, myProject);
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return classPackageChooser.getChildComponent();
    }

    private static class MemberInfoModelImpl extends AbstractMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo> { }
}
