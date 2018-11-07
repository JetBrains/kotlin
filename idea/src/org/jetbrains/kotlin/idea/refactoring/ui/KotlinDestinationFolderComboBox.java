/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.ui;

import com.intellij.ide.util.DirectoryChooser;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil;
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination;
import com.intellij.ui.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import static org.jetbrains.kotlin.idea.roots.ProjectRootUtilsKt.getKotlinAwareDestinationSourceRoots;

// Based on com.intellij.refactoring.move.moveClassesOrPackages.DestinationFolderComboBox
public abstract class KotlinDestinationFolderComboBox extends ComboboxWithBrowseButton {
    private static final String LEAVE_IN_SAME_SOURCE_ROOT = "Leave in same source root";
    private static final DirectoryChooser.ItemWrapper NULL_WRAPPER = new DirectoryChooser.ItemWrapper(null, null);
    private PsiDirectory myInitialTargetDirectory;
    private List<VirtualFile> mySourceRoots;

    public KotlinDestinationFolderComboBox() {
        super(new ComboBoxWithWidePopup());
    }

    public abstract String getTargetPackage();

    protected boolean reportBaseInTestSelectionInSource() {
        return false;
    }

    protected boolean reportBaseInSourceSelectionInTest() {
        return false;
    }

    public void setData(
            Project project,
            PsiDirectory initialTargetDirectory,
            EditorComboBox editorComboBox
    ) {
        setData(project, initialTargetDirectory, new Pass<String>() {
            @Override
            public void pass(String s) {
            }
        }, editorComboBox);
    }

    public void setData(
            Project project,
            PsiDirectory initialTargetDirectory,
            Pass<String> errorMessageUpdater, EditorComboBox editorComboBox
    ) {
        myInitialTargetDirectory = initialTargetDirectory;
        mySourceRoots = getKotlinAwareDestinationSourceRoots(project);
        new ComboboxSpeedSearch(getComboBox()) {
            @Override
            protected String getElementText(Object element) {
                if (element == NULL_WRAPPER) return LEAVE_IN_SAME_SOURCE_ROOT;
                if (element instanceof DirectoryChooser.ItemWrapper) {
                    VirtualFile virtualFile = ((DirectoryChooser.ItemWrapper) element).getDirectory().getVirtualFile();
                    Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
                    if (module != null) {
                        return module.getName();
                    }
                }
                return super.getElementText(element);
            }
        };
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        getComboBox().setRenderer(new ListCellRendererWrapper<DirectoryChooser.ItemWrapper>() {
            @Override
            public void customize(
                    JList list,
                    DirectoryChooser.ItemWrapper itemWrapper,
                    int index,
                    boolean selected,
                    boolean hasFocus
            ) {
                if (itemWrapper != NULL_WRAPPER && itemWrapper != null) {
                    setIcon(itemWrapper.getIcon(fileIndex));

                    setText(itemWrapper.getRelativeToProjectPath());
                }
                else {
                    setText(LEAVE_IN_SAME_SOURCE_ROOT);
                }
            }
        });
        VirtualFile initialSourceRoot =
                initialTargetDirectory != null ? fileIndex.getSourceRootForFile(initialTargetDirectory.getVirtualFile()) : null;
        VirtualFile[] selection = new VirtualFile[] {initialSourceRoot};
        addActionListener(e -> {
            VirtualFile root = MoveClassesOrPackagesUtil
                    .chooseSourceRoot(new PackageWrapper(PsiManager.getInstance(project), getTargetPackage()), mySourceRoots,
                                      initialTargetDirectory);
            if (root == null) return;
            ComboBoxModel model = getComboBox().getModel();
            for (int i = 0; i < model.getSize(); i++) {
                DirectoryChooser.ItemWrapper item = (DirectoryChooser.ItemWrapper) model.getElementAt(i);
                if (item != NULL_WRAPPER &&
                    Comparing.equal(fileIndex.getSourceRootForFile(item.getDirectory().getVirtualFile()), root)) {
                    getComboBox().setSelectedItem(item);
                    getComboBox().repaint();
                    return;
                }
            }
            setComboboxModel(getComboBox(), root, root, fileIndex, mySourceRoots, project, true, errorMessageUpdater);
        });

        editorComboBox.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent e) {
                JComboBox comboBox = getComboBox();
                DirectoryChooser.ItemWrapper selectedItem = (DirectoryChooser.ItemWrapper) comboBox.getSelectedItem();
                setComboboxModel(comboBox, selectedItem != null && selectedItem != NULL_WRAPPER ? fileIndex
                                         .getSourceRootForFile(selectedItem.getDirectory().getVirtualFile()) : initialSourceRoot, selection[0], fileIndex,
                                 mySourceRoots, project, false, errorMessageUpdater);
            }
        });
        setComboboxModel(getComboBox(), initialSourceRoot, selection[0], fileIndex, mySourceRoots, project, false, errorMessageUpdater);
        getComboBox().addActionListener(e -> {
            Object selectedItem = getComboBox().getSelectedItem();
            updateErrorMessage(errorMessageUpdater, fileIndex, selectedItem);
            if (selectedItem instanceof DirectoryChooser.ItemWrapper && selectedItem != NULL_WRAPPER) {
                PsiDirectory directory = ((DirectoryChooser.ItemWrapper) selectedItem).getDirectory();
                if (directory != null) {
                    selection[0] = fileIndex.getSourceRootForFile(directory.getVirtualFile());
                }
            }
        });
    }

    @Nullable
    public MoveDestination selectDirectory(PackageWrapper targetPackage, boolean showChooserWhenDefault) {
        DirectoryChooser.ItemWrapper selectedItem = (DirectoryChooser.ItemWrapper) getComboBox().getSelectedItem();
        if (selectedItem == null || selectedItem == NULL_WRAPPER) {
            return new MultipleRootsMoveDestination(targetPackage);
        }
        PsiDirectory selectedPsiDirectory = selectedItem.getDirectory();
        VirtualFile selectedDestination = selectedPsiDirectory.getVirtualFile();
        if (showChooserWhenDefault &&
            myInitialTargetDirectory != null && Comparing.equal(selectedDestination, myInitialTargetDirectory.getVirtualFile()) &&
            mySourceRoots.size() > 1) {
            selectedDestination = MoveClassesOrPackagesUtil.chooseSourceRoot(targetPackage, mySourceRoots, myInitialTargetDirectory);
        }
        if (selectedDestination == null) return null;
        return new AutocreatingSingleSourceRootMoveDestination(targetPackage, selectedDestination);
    }

    private void updateErrorMessage(Pass<String> updateErrorMessage, ProjectFileIndex fileIndex, Object selectedItem) {
        updateErrorMessage.pass(null);
        if (myInitialTargetDirectory != null && selectedItem instanceof DirectoryChooser.ItemWrapper && selectedItem != NULL_WRAPPER) {
            PsiDirectory directory = ((DirectoryChooser.ItemWrapper) selectedItem).getDirectory();
            boolean isSelectionInTestSourceContent = fileIndex.isInTestSourceContent(directory.getVirtualFile());
            boolean inTestSourceContent = fileIndex.isInTestSourceContent(myInitialTargetDirectory.getVirtualFile());
            if (isSelectionInTestSourceContent != inTestSourceContent) {
                if (inTestSourceContent && reportBaseInTestSelectionInSource()) {
                    updateErrorMessage.pass("Source root is selected while the test root is expected");
                }

                if (isSelectionInTestSourceContent && reportBaseInSourceSelectionInTest()) {
                    updateErrorMessage.pass("Test root is selected while the source root is expected");
                }
            }
        }
    }

    private void setComboboxModel(
            JComboBox comboBox,
            VirtualFile initialTargetDirectorySourceRoot,
            VirtualFile oldSelection,
            ProjectFileIndex fileIndex,
            List<VirtualFile> sourceRoots,
            Project project,
            boolean forceIncludeAll,
            Pass<String> updateErrorMessage
    ) {
        LinkedHashSet<PsiDirectory> targetDirectories = new LinkedHashSet<>();
        HashMap<PsiDirectory, String> pathsToCreate = new HashMap<>();
        MoveClassesOrPackagesUtil
                .buildDirectoryList(new PackageWrapper(PsiManager.getInstance(project), getTargetPackage()), sourceRoots, targetDirectories,
                                    pathsToCreate);
        if (!forceIncludeAll && targetDirectories.size() > pathsToCreate.size()) {
            targetDirectories.removeAll(pathsToCreate.keySet());
        }
        ArrayList<DirectoryChooser.ItemWrapper> items = new ArrayList<>();
        DirectoryChooser.ItemWrapper initial = null;
        DirectoryChooser.ItemWrapper oldOne = null;
        for (PsiDirectory targetDirectory : targetDirectories) {
            DirectoryChooser.ItemWrapper itemWrapper =
                    new DirectoryChooser.ItemWrapper(targetDirectory, pathsToCreate.get(targetDirectory));
            items.add(itemWrapper);
            VirtualFile sourceRootForFile = fileIndex.getSourceRootForFile(targetDirectory.getVirtualFile());
            if (Comparing.equal(sourceRootForFile, initialTargetDirectorySourceRoot)) {
                initial = itemWrapper;
            }
            else if (Comparing.equal(sourceRootForFile, oldSelection)) {
                oldOne = itemWrapper;
            }
        }
        if (oldSelection == null || !fileIndex.isInLibrarySource(oldSelection)) {
            items.add(NULL_WRAPPER);
        }
        DirectoryChooser.ItemWrapper selection = chooseSelection(initialTargetDirectorySourceRoot, fileIndex, items, initial, oldOne);
        ComboBoxModel model = comboBox.getModel();
        if (model instanceof CollectionComboBoxModel) {
            boolean sameModel = model.getSize() == items.size();
            if (sameModel) {
                for (int i = 0; i < items.size(); i++) {
                    DirectoryChooser.ItemWrapper oldItem = (DirectoryChooser.ItemWrapper) model.getElementAt(i);
                    DirectoryChooser.ItemWrapper itemWrapper = items.get(i);
                    if (!areItemsEquivalent(oldItem, itemWrapper)) {
                        sameModel = false;
                        break;
                    }
                }
            }
            if (sameModel) {
                if (areItemsEquivalent((DirectoryChooser.ItemWrapper) comboBox.getSelectedItem(), selection)) {
                    return;
                }
            }
        }
        updateErrorMessage(updateErrorMessage, fileIndex, selection);
        items.sort((o1, o2) -> {
            if (o1 == NULL_WRAPPER) return -1;
            if (o2 == NULL_WRAPPER) return 1;
            return o1.getRelativeToProjectPath().compareToIgnoreCase(o2.getRelativeToProjectPath());
        });
        comboBox.setModel(new CollectionComboBoxModel(items, selection));

        Component root = SwingUtilities.getRoot(comboBox);
        if (root instanceof Window) {
            Dimension preferredSize = root.getPreferredSize();
            if (preferredSize.getWidth() > root.getSize().getWidth()) {
                root.setSize(preferredSize);
            }
        }
    }

    @Nullable
    private static DirectoryChooser.ItemWrapper chooseSelection(
            VirtualFile initialTargetDirectorySourceRoot,
            ProjectFileIndex fileIndex,
            ArrayList<DirectoryChooser.ItemWrapper> items,
            DirectoryChooser.ItemWrapper initial,
            DirectoryChooser.ItemWrapper oldOne
    ) {
        if (initial != null ||
            ((initialTargetDirectorySourceRoot == null || items.size() > 2) && items.contains(NULL_WRAPPER)) ||
            items.isEmpty()) {
            return initial;
        }
        else {
            if (oldOne != null) {
                return oldOne;
            }
            else if (initialTargetDirectorySourceRoot != null) {
                boolean inTest = fileIndex.isInTestSourceContent(initialTargetDirectorySourceRoot);
                for (DirectoryChooser.ItemWrapper item : items) {
                    PsiDirectory directory = item.getDirectory();
                    if (directory != null) {
                        VirtualFile virtualFile = directory.getVirtualFile();
                        if (fileIndex.isInTestSourceContent(virtualFile) == inTest) {
                            return item;
                        }
                    }
                }
            }
        }
        return items.get(0);
    }

    private static boolean areItemsEquivalent(DirectoryChooser.ItemWrapper oItem, DirectoryChooser.ItemWrapper itemWrapper) {
        if (oItem == NULL_WRAPPER || itemWrapper == NULL_WRAPPER) {
            if (oItem != itemWrapper) {
                return false;
            }
            return true;
        }
        if (oItem == null) return itemWrapper == null;
        if (itemWrapper == null) return false;
        if (oItem.getDirectory() != itemWrapper.getDirectory()) {
            return false;
        }
        return true;
    }

    public static boolean isAccessible(
            Project project,
            VirtualFile virtualFile,
            VirtualFile targetVirtualFile
    ) {
        boolean inTestSourceContent = ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(virtualFile);
        Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        if (targetVirtualFile != null &&
            module != null &&
            !GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, inTestSourceContent).contains(targetVirtualFile)) {
            return false;
        }
        return true;
    }
}