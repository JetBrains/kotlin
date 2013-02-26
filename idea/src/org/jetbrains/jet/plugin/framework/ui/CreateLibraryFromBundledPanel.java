/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.facet.impl.ui.libraries.EditLibraryDialog;
import com.intellij.facet.impl.ui.libraries.LibraryCompositionSettings;
import com.intellij.facet.impl.ui.libraries.LibraryDownloadSettings;
import com.intellij.framework.library.DownloadableLibraryDescription;
import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.framework.library.FrameworkLibraryVersion;
import com.intellij.framework.library.FrameworkLibraryVersionFilter;
import com.intellij.ide.util.frameworkSupport.OldCustomLibraryDescription;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CreateLibraryFromBundledPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance("#com.intellij.framework.impl.ui.libraries.CreateLibraryFromBundledPanel");

    private JBLabel myMessageLabel;
    private JButton myConfigureButton;
    private JPanel myConfigurationPanel;

    private TextFieldWithBrowseButton destinationFolder;
    private JPanel myRootPanel;
    private JLabel descriptionLabel;

    private LibraryCompositionSettings mySettings;
    private final LibrariesContainer myLibrariesContainer;
    private boolean myDisposed;

    public CreateLibraryFromBundledPanel(
            @NotNull final CustomLibraryDescription libraryDescription,
            @NotNull final String baseDirectoryPath,
            @NotNull final FrameworkLibraryVersionFilter versionFilter,
            @NotNull final LibrariesContainer librariesContainer,
            final boolean showDoNotCreateOption
    ) {
        myLibrariesContainer = librariesContainer;
        showSettingsPanel(
                libraryDescription, baseDirectoryPath, versionFilter, showDoNotCreateOption,
                new ArrayList<FrameworkLibraryVersion>());

        destinationFolder.addBrowseFolderListener("Choose Destination Folder", "Choose folder for file", getProject(),
                                                  FileChooserDescriptorFactory.createSingleFolderDescriptor());

        VirtualFile baseDir = getProject().getBaseDir();
        if (baseDir != null) {
            destinationFolder.getTextField().setText(baseDir.getPath().replace('/', File.separatorChar) + File.separatorChar + "lib");
        }
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    @Nullable
    private static DownloadableLibraryDescription getDownloadableDescription(CustomLibraryDescription libraryDescription) {
        final DownloadableLibraryType type = libraryDescription.getDownloadableLibraryType();
        if (type != null) return type.getLibraryDescription();
        if (libraryDescription instanceof OldCustomLibraryDescription) {
            return ((OldCustomLibraryDescription) libraryDescription).getDownloadableDescription();
        }
        return null;
    }

    private void showSettingsPanel(
            CustomLibraryDescription libraryDescription,
            String baseDirectoryPath,
            FrameworkLibraryVersionFilter versionFilter,
            boolean showDoNotCreateOption, final List<? extends FrameworkLibraryVersion> versions
    ) {
        mySettings = new LibraryCompositionSettings(libraryDescription, baseDirectoryPath, versionFilter, versions);
        Disposer.register(this, mySettings);
        List<Library> libraries = calculateSuitableLibraries();

        myConfigureButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doConfigure();
            }
        });

        updateState();
    }

    private void doConfigure() {
        NewLibraryEditor editor = new NewLibraryEditor();
        editor.setName("Testing");

        EditLibraryDialog dialog = new EditLibraryDialog(myConfigurationPanel, mySettings, editor);
        dialog.show();

        updateState();
    }

    public void changeBaseDirectoryPath(@NotNull String directoryForLibrariesPath) {
        if (mySettings != null) {
            mySettings.changeBaseDirectoryPath(directoryForLibrariesPath);
            updateState();
        }
    }

    public void setVersionFilter(@NotNull FrameworkLibraryVersionFilter versionFilter) {
        if (mySettings != null) {
            mySettings.setVersionFilter(versionFilter);
            updateState();
        }
    }

    private void doCreate() {
        //final NewLibraryConfiguration libraryConfiguration = mySettings.getLibraryDescription().createNewLibrary(myPanel, getBaseDirectory());
        //if (libraryConfiguration != null) {
        //    final NewLibraryEditor libraryEditor = new NewLibraryEditor(libraryConfiguration.getLibraryType(), libraryConfiguration.getProperties());
        //    libraryEditor.setName(myLibrariesContainer.suggestUniqueLibraryName(libraryConfiguration.getDefaultLibraryName()));
        //    libraryConfiguration.addRoots(libraryEditor);
        //}
    }

    private List<Library> calculateSuitableLibraries() {
        final CustomLibraryDescription description = mySettings.getLibraryDescription();
        List<Library> suitableLibraries = new ArrayList<Library>();
        for (Library library : myLibrariesContainer.getAllLibraries()) {
            if (description instanceof OldCustomLibraryDescription &&
                ((OldCustomLibraryDescription) description).isSuitableLibrary(library, myLibrariesContainer)
                ||
                LibraryPresentationManager.getInstance()
                        .isLibraryOfKind(library, myLibrariesContainer, description.getSuitableLibraryKinds())) {
                suitableLibraries.add(library);
            }
        }
        return suitableLibraries;
    }

    @Nullable
    private VirtualFile getBaseDirectory() {
        String path = mySettings.getBaseDirectoryPath();
        VirtualFile dir = LocalFileSystem.getInstance().findFileByPath(path);
        if (dir == null) {
            path = path.substring(0, path.lastIndexOf('/'));
            dir = LocalFileSystem.getInstance().findFileByPath(path);
        }
        return dir;
    }

    private void updateState() {
        myMessageLabel.setIcon(null);
        myConfigureButton.setVisible(true);
        final LibraryDownloadSettings settings = mySettings.getDownloadSettings();

        //String message = IdeBundle.message(
        //        "label.library.will.be.created.description.text",
        //        mySettings.getNewLibraryLevel(),
        //        libraryEditor.getName(), libraryEditor.getFiles(OrderRootType.CLASSES).length);
        //
        //((CardLayout)myConfigurationPanel.getLayout()).show(myConfigurationPanel, showConfigurePanel ? "configure" : "empty");
        //myMessageLabel.setText("<html>" + message + "</html>");
    }

    public LibraryCompositionSettings getSettings() {
        return mySettings;
    }

    @Nullable
    public LibraryCompositionSettings apply() {
        if (mySettings == null) return null;

        //final Choice option = myButtonEnumModel.getSelected();
        //mySettings.setDownloadLibraries(option == Choice.DOWNLOAD);
        //
        //final Object item = myExistingLibraryComboBox.getSelectedItem();
        //if (option == Choice.USE_LIBRARY && item instanceof ExistingLibraryEditor) {
        //    mySettings.setSelectedExistingLibrary(((ExistingLibraryEditor)item).getLibrary());
        //}
        //else {
        //    mySettings.setSelectedExistingLibrary(null);
        //}
        //
        //if (option == Choice.USE_LIBRARY && item instanceof NewLibraryEditor) {
        //    mySettings.setNewLibraryEditor((NewLibraryEditor)item);
        //}
        //else {
        //    mySettings.setNewLibraryEditor(null);
        //}
        return mySettings;
    }

    public JComponent getMainPanel() {
        return myRootPanel;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    private Project getProject() {
        Project project = myLibrariesContainer.getProject();
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        return project;
    }
}
