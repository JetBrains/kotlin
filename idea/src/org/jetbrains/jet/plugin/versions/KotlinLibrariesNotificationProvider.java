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

package org.jetbrains.jet.plugin.versions;

import com.intellij.ProjectTopics;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.frameworkSupport.AddFrameworkSupportDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Collection;

public class KotlinLibrariesNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("configure.kotlin.library");
    private final Project myProject;
    private final Runnable updateNotifications = new Runnable() {
        @Override
        public void run() {
            updateNotifications();
        }
    };

    public KotlinLibrariesNotificationProvider(Project project) {
        myProject = project;
        MessageBusConnection connection = myProject.getMessageBus().connect();
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                updateNotifications();
            }
        });

        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {}

            @Override
            public void exitDumbMode() {
                updateNotifications();
            }
        });
    }

    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    @Override
    @Nullable
    public EditorNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
        try {
            if (file.getFileType() != JetFileType.INSTANCE) return null;

            if (CompilerManager.getInstance(myProject).isExcludedFromCompilation(file)) return null;

            Module module = ModuleUtilCore.findModuleForFile(file, myProject);
            if (module == null) return null;

            if (!KotlinRuntimeLibraryUtil.isModuleAlreadyConfigured(module)) {
                return createConfigureRuntimeLibraryNotificationPanel(module);
            }

            Collection<VirtualFile> badRoots = KotlinRuntimeLibraryUtil.getLibraryRootsWithAbiIncompatibleKotlinClasses(myProject);
            if (!badRoots.isEmpty()) {
                return createUnsupportedAbiVersionNotificationPanel(badRoots);
            }
        }
        catch (ProcessCanceledException e) {
            // Ignore
        }
        catch (IndexNotReadyException e) {
            DumbService.getInstance(myProject).runWhenSmart(updateNotifications);
            return null;
        }

        return null;
    }

    private static EditorNotificationPanel createConfigureRuntimeLibraryNotificationPanel(final Module module) {
        EditorNotificationPanel answer = new EditorNotificationPanel();

        answer.setText("Kotlin is not configured for module '" + module.getName() + "'");
        answer.createActionLabel("Set up Kotlin framework", new Runnable() {
            @Override
            public void run() {
                // TODO: make choose only from Kotlin frameworks
                AddFrameworkSupportDialog dialog = AddFrameworkSupportDialog.createDialog(module);
                if (dialog != null) {
                    dialog.show();
                }
            }
        });

        return answer;
    }

    private EditorNotificationPanel createUnsupportedAbiVersionNotificationPanel(Collection<VirtualFile> badRoots) {
        EditorNotificationPanel answer = new ErrorNotificationPanel();

        VirtualFile kotlinRuntimeJar = KotlinRuntimeLibraryUtil.getLocalKotlinRuntimeJar(myProject);
        if (kotlinRuntimeJar != null && badRoots.contains(kotlinRuntimeJar)) {
            int otherBadRootsCount = badRoots.size() - 1;
            String kotlinRuntimeJarName = kotlinRuntimeJar.getPresentableName();
            String text = MessageFormat.format("<html>Kotlin <b>runtime library</b> jar <b>''{0}''</b> " +
                                                 "{1,choice,0#|1# and one other jar|1< and {1} other jars} " +
                                                 "{1,choice,0#has|0<have} an unsupported format</html>",
                                               kotlinRuntimeJarName,
                                               otherBadRootsCount);
            answer.setText(text);
            answer.createActionLabel("Update " + kotlinRuntimeJarName, new Runnable() {
                @Override
                public void run() {
                    KotlinRuntimeLibraryUtil.updateRuntime(myProject,
                                                           OutdatedKotlinRuntimeNotification.showRuntimeJarNotFoundDialog(myProject));
                }
            });
            if (otherBadRootsCount > 0) {
                createShowPathsActionLabel(answer, "Show all");
            }
        }
        else if (badRoots.size() == 1) {
            final VirtualFile root = badRoots.iterator().next();
            String presentableName = root.getPresentableName();
            answer.setText("<html>Kotlin library <b>'" + presentableName + "'</b> " +
                           "has an unsupported format. Please update the library or the plugin</html>");

            answer.createActionLabel("Go to " + presentableName, new Runnable() {
                @Override
                public void run() {
                    navigateToLibraryRoot(myProject, root);
                }
            });
        }
        else {
            answer.setText("Some Kotlin libraries attached to this project have unsupported format. Please update the libraries or the plugin");

            createShowPathsActionLabel(answer, "Show paths");
        }
        return answer;
    }

    private void createShowPathsActionLabel(EditorNotificationPanel answer, String labelText) {
        final Ref<Component> label = new Ref<Component>(null);
        Runnable action = new Runnable() {
            @Override
            public void run() {
                Collection<VirtualFile> badRoots =
                        KotlinRuntimeLibraryUtil.getLibraryRootsWithAbiIncompatibleKotlinClasses(myProject);
                assert !badRoots.isEmpty() : "This action should only be called when bad roots are present";

                LibraryRootsPopupModel listPopupModel = new LibraryRootsPopupModel("Unsupported format", myProject, badRoots);
                ListPopup popup = JBPopupFactory.getInstance().createListPopup(listPopupModel);
                popup.showUnderneathOf(label.get());
            }
        };
        label.set(answer.createActionLabel(labelText, action));
    }

    private void updateNotifications() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                EditorNotifications.getInstance(myProject).updateAllNotifications();
            }
        });
    }

    private static void navigateToLibraryRoot(Project project, @NotNull VirtualFile root) {
        new OpenFileDescriptor(project, root).navigate(true);
    }

    private static class LibraryRootsPopupModel extends BaseListPopupStep<VirtualFile> {

        private final Project project;

        public LibraryRootsPopupModel(@NotNull String title, @NotNull Project project, @NotNull Collection<VirtualFile> roots) {
            super(title, roots.toArray(new VirtualFile[roots.size()]));
            this.project = project;
        }

        @NotNull
        @Override
        public String getTextFor(VirtualFile root) {
            String relativePath = VfsUtilCore.getRelativePath(root, project.getBaseDir(), '/');
            return relativePath != null ? relativePath : root.getPath();
        }

        @Override
        public Icon getIconFor(VirtualFile aValue) {
            if (aValue.isDirectory()) {
                return AllIcons.Nodes.Folder;
            }
            return AllIcons.FileTypes.Archive;
        }

        @Override
        public PopupStep onChosen(VirtualFile selectedValue, boolean finalChoice) {
            navigateToLibraryRoot(project, selectedValue);
            return FINAL_CHOICE;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }
    }

    private static class ErrorNotificationPanel extends EditorNotificationPanel {
        public ErrorNotificationPanel() {
            myLabel.setIcon(AllIcons.General.Error);
        }
    }
}
