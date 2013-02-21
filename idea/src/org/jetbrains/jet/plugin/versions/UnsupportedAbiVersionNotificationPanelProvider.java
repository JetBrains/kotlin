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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Collection;

public class UnsupportedAbiVersionNotificationPanelProvider {
    private final Project project;

    public UnsupportedAbiVersionNotificationPanelProvider(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    public static EditorNotificationPanel checkAndCreate(@NotNull Project project) {
        Collection<VirtualFile> badRoots = KotlinRuntimeLibraryUtil.getLibraryRootsWithAbiIncompatibleKotlinClasses(project);
        if (!badRoots.isEmpty()) {
            return new UnsupportedAbiVersionNotificationPanelProvider(project).doCreate(badRoots);
        }

        return null;
    }

    private EditorNotificationPanel doCreate(Collection<VirtualFile> badRoots) {
        EditorNotificationPanel answer = new ErrorNotificationPanel();

        VirtualFile kotlinRuntimeJar = KotlinRuntimeLibraryUtil.getLocalKotlinRuntimeJar(project);
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
                    KotlinRuntimeLibraryUtil.updateRuntime(project,
                                                           OutdatedKotlinRuntimeNotification.showRuntimeJarNotFoundDialog(project));
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
                    navigateToLibraryRoot(project, root);
                }
            });
        }
        else {
            answer.setText("Some Kotlin libraries attached to this project have unsupported format. Please update the libraries or the plugin");
            createShowPathsActionLabel(answer, "Show paths");
        }
        return answer;
    }

    private static void navigateToLibraryRoot(Project project, @NotNull VirtualFile root) {
        new OpenFileDescriptor(project, root).navigate(true);
    }

    private void createShowPathsActionLabel(EditorNotificationPanel answer, String labelText) {
        final Ref<Component> label = new Ref<Component>(null);
        Runnable action = new Runnable() {
            @Override
            public void run() {
                Collection<VirtualFile> badRoots =
                        KotlinRuntimeLibraryUtil.getLibraryRootsWithAbiIncompatibleKotlinClasses(project);
                assert !badRoots.isEmpty() : "This action should only be called when bad roots are present";

                LibraryRootsPopupModel listPopupModel = new LibraryRootsPopupModel("Unsupported format", project, badRoots);
                ListPopup popup = JBPopupFactory.getInstance().createListPopup(listPopupModel);
                popup.showUnderneathOf(label.get());
            }
        };
        label.set(answer.createActionLabel(labelText, action));
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
