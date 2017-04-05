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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui;

import com.intellij.ide.util.DirectoryChooser;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.JavaRefactoringSettings;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoChangeListener;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination;
import com.intellij.refactoring.move.moveClassesOrPackages.DestinationFolderComboBox;
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.UIUtil;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.core.PackageUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtilKt;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionTable;
import org.jetbrains.kotlin.idea.refactoring.move.MoveUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinFileChooserDialog;
import org.jetbrains.kotlin.idea.util.application.ApplicationUtilsKt;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

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
    private KotlinMemberSelectionTable memberTable;
    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            @Nullable KtFile targetFile,
            boolean moveToPackage,
            boolean searchInComments,
            boolean searchForTextOccurences,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        List<KtFile> sourceFiles = getSourceFiles(elementsToMove);

        this.moveCallback = moveCallback;
        this.initialTargetDirectory = targetDirectory;

        init();

        setTitle(MoveHandler.REFACTORING_NAME);

        initSearchOptions(searchInComments, searchForTextOccurences);

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
                        new Function1<KtNamedDeclaration, KtFile>() {
                            @Override
                            public KtFile invoke(KtNamedDeclaration declaration) {
                                return declaration.getContainingKtFile();
                            }
                        }
                )
        );
    }

    @NotNull
    private static PsiDirectory getSourceDirectory(@NotNull Collection<KtFile> sourceFiles) {
        return CollectionsKt.single(
                CollectionsKt.distinct(
                        CollectionsKt.map(
                                sourceFiles,
                                new Function1<KtFile, PsiDirectory>() {
                                    @Override
                                    public PsiDirectory invoke(KtFile jetFile) {
                                        return jetFile.getParent();
                                    }
                                }
                        )
                )
        );
    }

    private static List<KtNamedDeclaration> getAllDeclarations(Collection<KtFile> sourceFiles) {
        return CollectionsKt.filterIsInstance(
                CollectionsKt.flatMap(
                        sourceFiles,
                        new Function1<KtFile, Iterable<?>>() {
                            @Override
                            public Iterable<?> invoke(KtFile jetFile) {
                                return jetFile.getDeclarations();
                            }
                        }
                ),
                KtNamedDeclaration.class
        );
    }

    private static boolean arePackagesAndDirectoryMatched(List<KtFile> sourceFiles) {
        for (KtFile sourceFile : sourceFiles) {
            if (!PackageUtilsKt.packageMatchesDirectory(sourceFile)) return false;
        }
        return true;
    }

    @NotNull
    private static List<PsiFile> getFilesExistingInTargetDir(
            @NotNull List<KtFile> sourceFiles,
            @Nullable String targetFileName,
            @Nullable final PsiDirectory targetDirectory
    ) {
        if (targetDirectory == null) return Collections.emptyList();

        List<String> fileNames =
                targetFileName != null
                ? Collections.singletonList(targetFileName)
                : CollectionsKt.map(
                        sourceFiles,
                        new Function1<KtFile, String>() {
                            @Override
                            public String invoke(KtFile jetFile) {
                                return jetFile.getName();
                            }
                        }
                );

        return CollectionsKt.filterNotNull(
                CollectionsKt.map(
                        fileNames,
                        new Function1<String, PsiFile>() {
                            @Override
                            public PsiFile invoke(String s) {
                                return targetDirectory.findFile(s);
                            }
                        }
                )
        );
    }

    private void initMemberInfo(
            @NotNull final Set<KtNamedDeclaration> elementsToMove,
            @NotNull List<KtFile> sourceFiles
    ) {
        final List<KotlinMemberInfo> memberInfos = CollectionsKt.map(
                getAllDeclarations(sourceFiles),
                new Function1<KtNamedDeclaration, KotlinMemberInfo>() {
                    @Override
                    public KotlinMemberInfo invoke(KtNamedDeclaration declaration) {
                        KotlinMemberInfo memberInfo = new KotlinMemberInfo(declaration, false);
                        memberInfo.setChecked(elementsToMove.contains(declaration));
                        return memberInfo;
                    }
                }
        );
        KotlinMemberSelectionPanel selectionPanel = new KotlinMemberSelectionPanel(getTitle(), memberInfos, null);
        memberTable = selectionPanel.getTable();
        MemberInfoModelImpl memberInfoModel = new MemberInfoModelImpl();
        memberInfoModel.memberInfoChanged(new MemberInfoChange<KtNamedDeclaration, KotlinMemberInfo>(memberInfos));
        selectionPanel.getTable().setMemberInfoModel(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(
                new MemberInfoChangeListener<KtNamedDeclaration, KotlinMemberInfo>() {
                    private boolean shouldUpdateFileNameField(final Collection<KotlinMemberInfo> changedMembers) {
                        if (!tfFileNameInPackage.isEnabled()) return true;

                        Collection<KtNamedDeclaration> previousDeclarations = CollectionsKt.filterNotNull(
                                CollectionsKt.map(
                                        memberInfos,
                                        new Function1<KotlinMemberInfo, KtNamedDeclaration>() {
                                            @Override
                                            public KtNamedDeclaration invoke(KotlinMemberInfo info) {
                                                return changedMembers.contains(info) != info.isChecked() ? info.getMember() : null;
                                            }
                                        }
                                )
                        );
                        String suggestedText = previousDeclarations.isEmpty()
                                               ? ""
                                               : MoveUtilsKt.guessNewFileName(previousDeclarations);
                        return tfFileNameInPackage.getText().equals(suggestedText);
                    }

                    @Override
                    public void memberInfoChanged(MemberInfoChange<KtNamedDeclaration, KotlinMemberInfo> event) {
                        updatePackageDirectiveCheckBox();
                        updateFileNameInPackageField();
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
        tfFileNameInPackage.setText(MoveUtilsKt.guessNewFileName(getSelectedElementsToMove()));

    }

    private void updateFileNameInPackageField() {
        boolean movingSingleFileToPackage = isMoveToPackage()
                                            && getSourceFiles(getSelectedElementsToMove()).size() == 1;
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
                        updateFileNameInPackageField();
                    }
                }
        );

        cbUpdatePackageDirective.setSelected(arePackagesAndDirectoryMatched(sourceFiles));
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

    private void initFileChooser(
            @Nullable KtFile targetFile,
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @NotNull List<KtFile> sourceFiles
    ) {
        final PsiDirectory sourceDir = sourceFiles.get(0).getParent();
        assert sourceDir != null : sourceFiles.get(0).getVirtualFile().getPath();

        fileChooser.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        KotlinFileChooserDialog dialog = new KotlinFileChooserDialog("Choose Containing File", myProject);

                        File targetFile = new File(getTargetFilePath());
                        PsiFile targetPsiFile = KotlinRefactoringUtilKt.toPsiFile(targetFile, myProject);
                        if (targetPsiFile instanceof KtFile) {
                            dialog.select((KtFile) targetPsiFile);
                        }
                        else {
                            PsiDirectory targetDir = KotlinRefactoringUtilKt.toPsiDirectory(targetFile.getParentFile(), myProject);
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
                }
        );

        String initialTargetPath =
                targetFile != null
                ? targetFile.getVirtualFile().getPath()
                : sourceFiles.get(0).getVirtualFile().getParent().getPath() +
                  "/" +
                  MoveUtilsKt.guessNewFileName(elementsToMove);
        fileChooser.setText(initialTargetPath);
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
        updateFileNameInPackageField();
        fileChooser.setEnabled(!moveToPackage);
        updatePackageDirectiveCheckBox();
        UIUtil.setEnabled(targetPanel, moveToPackage && hasAnySourceRoots(), true);
        updateSuggestedFileName();
        validateButtons();
    }

    private boolean isFullFileMove() {
        Map<KtFile, List<KtNamedDeclaration>> fileToElements = CollectionsKt.groupBy(
                getSelectedElementsToMove(),
                new Function1<KtNamedDeclaration, KtFile>() {
                    @Override
                    public KtFile invoke(KtNamedDeclaration declaration) {
                        return declaration.getContainingKtFile();
                    }
                }
        );
        for (Map.Entry<KtFile, List<KtNamedDeclaration>> entry : fileToElements.entrySet()) {
            if (entry.getKey().getDeclarations().size() != entry.getValue().size()) return false;
        }
        return true;
    }

    private void updatePackageDirectiveCheckBox() {
        cbUpdatePackageDirective.setEnabled(isMoveToPackage() && isFullFileMove());
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
    private Pair<VirtualFile, ? extends MoveDestination> selectPackageBasedTargetDirAndDestination(boolean askIfDoesNotExist) {
        String packageName = getTargetPackage();

        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, packageName);
        PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
        if (!targetPackage.exists() && askIfDoesNotExist) {
            int ret = Messages.showYesNoDialog(myProject, RefactoringBundle.message("package.does.not.exist", packageName),
                                               RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
            if (ret != Messages.YES) return null;
        }

        DirectoryChooser.ItemWrapper selectedItem = (DirectoryChooser.ItemWrapper)destinationFolderCB.getComboBox().getSelectedItem();
        PsiDirectory selectedPsiDirectory = selectedItem != null ? selectedItem.getDirectory() : null;
        if (selectedPsiDirectory == null) {
            if (initialTargetDirectory != null) {
                selectedPsiDirectory = initialTargetDirectory;
            }
            else {
                return Pair.create(null, new MultipleRootsMoveDestination(targetPackage));
            }
        }

        VirtualFile targetDirectory = selectedPsiDirectory.getVirtualFile();
        return Pair.create(targetDirectory, new AutocreatingSingleSourceRootMoveDestination(targetPackage, targetDirectory));
    }

    private boolean checkTargetFileName(String fileName) {
        if (FileTypeManager.getInstance().getFileTypeByFileName(fileName) == KotlinFileType.INSTANCE) return true;
        setErrorText("Can't move to non-Kotlin file");
        return false;
    }

    @Nullable
    private KotlinMoveTarget selectMoveTarget() {
        String message = verifyBeforeRun();
        if (message != null) {
            setErrorText(message);
            return null;
        }

        setErrorText(null);

        List<KtFile> sourceFiles = getSourceFiles(getSelectedElementsToMove());
        PsiDirectory sourceDirectory = getSourceDirectory(sourceFiles);

        if (isMoveToPackage()) {
            Pair<VirtualFile, ? extends MoveDestination> targetDirWithMoveDestination = selectPackageBasedTargetDirAndDestination(true);
            if (targetDirWithMoveDestination == null) return null;

            VirtualFile targetDir = targetDirWithMoveDestination.getFirst();
            final MoveDestination moveDestination = targetDirWithMoveDestination.getSecond();

            final String targetFileName = sourceFiles.size() > 1 ? null : tfFileNameInPackage.getText();
            if (targetFileName != null && !checkTargetFileName(targetFileName)) return null;

            PsiDirectory targetDirectory = moveDestination.getTargetIfExists(sourceDirectory);

            List<PsiFile> filesExistingInTargetDir = getFilesExistingInTargetDir(sourceFiles, targetFileName, targetDirectory);
            if (!filesExistingInTargetDir.isEmpty()) {
                if (filesExistingInTargetDir.size() > 1) {
                    String filePathsToReport = StringUtil.join(
                            filesExistingInTargetDir,
                            new Function<PsiFile, String>() {
                                @Override
                                public String fun(PsiFile file) {
                                    return file.getVirtualFile().getPath();
                                }
                            },
                            "\n"
                    );
                    Messages.showErrorDialog(
                            myProject,
                            "Cannot perform refactoring since the following files already exist:\n\n" + filePathsToReport,
                            RefactoringBundle.message("move.title")
                    );
                    return null;
                }

                PsiFile targetFile = filesExistingInTargetDir.get(0);

                if (!sourceFiles.contains(targetFile)) {
                    String question = String.format(
                            "File '%s' already exists. Do you want to move selected declarations to this file?",
                            targetFile.getVirtualFile().getPath()
                    );
                    int ret =
                            Messages.showYesNoDialog(myProject, question, RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
                    if (ret != Messages.YES) return null;
                }
            }

            // All source files must be in the same directory
            return new KotlinMoveTargetForDeferredFile(
                    new FqName(getTargetPackage()),
                    moveDestination.getTargetIfExists(sourceFiles.get(0)),
                    targetDir,
                    new Function1<KtFile, KtFile>() {
                        @Override
                        public KtFile invoke(@NotNull KtFile originalFile) {
                            return KotlinRefactoringUtilKt.getOrCreateKotlinFile(
                                    targetFileName != null ? targetFileName : originalFile.getName(),
                                    moveDestination.getTargetDirectory(originalFile)
                            );
                        }
                    }
            );
        }

        final File targetFile = new File(getTargetFilePath());
        if (!checkTargetFileName(targetFile.getName())) return null;
        KtFile jetFile = (KtFile) KotlinRefactoringUtilKt.toPsiFile(targetFile, myProject);
        if (jetFile != null) {
            if (sourceFiles.size() == 1 && sourceFiles.contains(jetFile)) {
                setErrorText("Can't move to the original file");
                return null;
            }

            return new KotlinMoveTargetForExistingElement(jetFile);
        }

        File targetDir = targetFile.getParentFile();
        final PsiDirectory psiDirectory = targetDir != null ? KotlinRefactoringUtilKt.toPsiDirectory(targetDir, myProject) : null;
        if (psiDirectory == null) {
            setErrorText("No directory found for file: " + targetFile.getPath());
            return null;
        }

        Set<FqName> sourcePackageFqNames = CollectionsKt.mapTo(
                sourceFiles,
                new LinkedHashSet<FqName>(),
                new Function1<KtFile, FqName>() {
                    @Override
                    public FqName invoke(KtFile file) {
                        return file.getPackageFqName();
                    }
                }
        );
        FqName targetPackageFqName = CollectionsKt.singleOrNull(sourcePackageFqNames);
        if (targetPackageFqName == null) {
            PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
            if (psiPackage == null) {
                setErrorText("Could not find package corresponding to " + targetDir.getPath());
                return null;
            }
            targetPackageFqName = new FqName(psiPackage.getQualifiedName());
        }

        final String finalTargetPackageFqName = targetPackageFqName.asString();
        return new KotlinMoveTargetForDeferredFile(
                targetPackageFqName,
                psiDirectory,
                null,
                new Function1<KtFile, KtFile>() {
                    @Override
                    public KtFile invoke(@NotNull KtFile originalFile) {
                        return KotlinRefactoringUtilKt.getOrCreateKotlinFile(targetFile.getName(), psiDirectory, finalTargetPackageFqName);
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
            PsiFile targetFile = KotlinRefactoringUtilKt.toPsiFile(new File(getTargetFilePath()), myProject);
            if (!(targetFile == null || targetFile instanceof KtFile)) {
                return KotlinRefactoringBundle.message("refactoring.move.non.kotlin.file");
            }
        }

        if (getSourceFiles(getSelectedElementsToMove()).size() == 1 && tfFileNameInPackage.getText().isEmpty()) {
            return "File name may not be empty";
        }

        return null;
    }

    private List<KtNamedDeclaration> getSelectedElementsToMove() {
        return CollectionsKt.map(
                memberTable.getSelectedMemberInfos(),
                new Function1<KotlinMemberInfo, KtNamedDeclaration>() {
                    @Override
                    public KtNamedDeclaration invoke(KotlinMemberInfo info) {
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

        List<KtNamedDeclaration> elementsToMove = getSelectedElementsToMove();
        List<KtFile> sourceFiles = getSourceFiles(elementsToMove);
        final PsiDirectory sourceDirectory = getSourceDirectory(sourceFiles);

        for (PsiElement element : elementsToMove) {
            String message = target.verify(element.getContainingFile());
            if (message != null) {
                CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), message, null, myProject);
                return;
            }
        }

        try {
            boolean deleteSourceFile = false;

            if (isFullFileMove()) {
                if (isMoveToPackage()) {
                    Pair<VirtualFile, ? extends MoveDestination> sourceRootWithMoveDestination = selectPackageBasedTargetDirAndDestination(false);
                    //noinspection ConstantConditions
                    final MoveDestination moveDestination = sourceRootWithMoveDestination.getSecond();

                    PsiDirectory targetDir = moveDestination.getTargetIfExists(sourceDirectory);
                    String targetFileName = sourceFiles.size() > 1 ? null : tfFileNameInPackage.getText();
                    List<PsiFile> filesExistingInTargetDir = getFilesExistingInTargetDir(sourceFiles, targetFileName, targetDir);
                    if (filesExistingInTargetDir.isEmpty()
                        || (filesExistingInTargetDir.size() == 1 && sourceFiles.contains(filesExistingInTargetDir.get(0)))) {
                        PsiDirectory targetDirectory = ApplicationUtilsKt.runWriteAction(
                                new Function0<PsiDirectory>() {
                                    @Override
                                    public PsiDirectory invoke() {
                                        return moveDestination.getTargetDirectory(sourceDirectory);
                                    }
                                }
                        );

                        for (KtFile sourceFile : sourceFiles) {
                            MoveUtilsKt.setUpdatePackageDirective(sourceFile, cbUpdatePackageDirective.isSelected());
                        }

                        invokeRefactoring(
                                new MoveFilesWithDeclarationsProcessor(myProject,
                                                                       sourceFiles,
                                                                       targetDirectory,
                                                                       targetFileName,
                                                                       isSearchInComments(),
                                                                       isSearchInNonJavaFiles(),
                                                                       moveCallback)
                        );

                        return;
                    }
                }

                int ret = Messages.showYesNoCancelDialog(
                        myProject,
                        "You are about to move all declarations out of the source file(s). Do you want to delete empty files?",
                        RefactoringBundle.message("move.title"),
                        Messages.getQuestionIcon()
                );
                if (ret == Messages.CANCEL) return;
                deleteSourceFile = ret == Messages.YES;
            }

            MoveDeclarationsDescriptor options = new MoveDeclarationsDescriptor(
                    myProject,
                    elementsToMove,
                    target,
                    MoveDeclarationsDelegate.TopLevel.INSTANCE,
                    isSearchInComments(),
                    isSearchInNonJavaFiles(),
                    false,
                    deleteSourceFile,
                    moveCallback,
                    false
            );
            invokeRefactoring(new MoveKotlinDeclarationsProcessor(options, Mover.Default.INSTANCE));
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

    private static class MemberInfoModelImpl extends AbstractMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo> {

    }
}
