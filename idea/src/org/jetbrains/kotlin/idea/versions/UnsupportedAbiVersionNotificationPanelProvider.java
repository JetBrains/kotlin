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

package org.jetbrains.kotlin.idea.versions;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.ProjectTopics;
import com.intellij.icons.AllIcons;
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
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider;
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Collection;

public class UnsupportedAbiVersionNotificationPanelProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("unsupported.abi.version");

    private final Project project;

    public UnsupportedAbiVersionNotificationPanelProvider(@NotNull Project project) {
        this.project = project;
        MessageBusConnection connection = project.getMessageBus().connect();
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

    @Nullable
    public static EditorNotificationPanel checkAndCreate(@NotNull Project project) {
        Collection<VirtualFile> badRoots = collectBadRoots(project);
        if (!badRoots.isEmpty()) {
            return new UnsupportedAbiVersionNotificationPanelProvider(project).doCreate(badRoots);
        }

        return null;
    }

    private EditorNotificationPanel doCreate(final Collection<VirtualFile> badRoots) {
        EditorNotificationPanel answer = new ErrorNotificationPanel();

        Collection<Library> kotlinLibraries = KotlinRuntimeLibraryUtil.findKotlinLibraries(project);
        final Collection<Library> badRuntimeLibraries = Collections2.filter(kotlinLibraries, new Predicate<Library>() {
            @Override
            public boolean apply(@Nullable Library library) {
                assert library != null : "library should be non null";
                VirtualFile runtimeJar = KotlinRuntimeLibraryUtil.getLocalJar(JavaRuntimePresentationProvider.getRuntimeJar(library));
                VirtualFile jsLibJar = KotlinRuntimeLibraryUtil.getLocalJar(JSLibraryStdPresentationProvider.getJsStdLibJar(library));
                return badRoots.contains(runtimeJar) || badRoots.contains(jsLibJar);
            }
        });

        if (!badRuntimeLibraries.isEmpty()) {
            int otherBadRootsCount = badRoots.size() - badRuntimeLibraries.size();

            String text = MessageFormat.format("<html><b>{0,choice,0#|1#|1<Some }Kotlin runtime librar{0,choice,0#|1#y|1<ies}</b>" +
                                               "{1,choice,0#|1# and one other jar|1< and {1} other jars} " +
                                               "{1,choice,0#has|0<have} an unsupported format</html>",
                                               badRuntimeLibraries.size(),
                                               otherBadRootsCount);

            String actionLabelText = MessageFormat.format("Update {0,choice,0#|1#|1<all }Kotlin runtime librar{0,choice,0#|1#y|1<ies} ",
                                                badRuntimeLibraries.size());

            answer.setText(text);
            answer.createActionLabel(actionLabelText, new Runnable() {
                @Override
                public void run() {
                    KotlinRuntimeLibraryUtil.updateLibraries(project, badRuntimeLibraries);
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
                DumbService.getInstance(project).tryRunReadActionInSmartMode(new Computable<Object>() {
                    @Override
                    public Object compute() {
                        Collection<VirtualFile> badRoots = collectBadRoots(project);
                        assert !badRoots.isEmpty() : "This action should only be called when bad roots are present";

                        LibraryRootsPopupModel listPopupModel = new LibraryRootsPopupModel("Unsupported format", project, badRoots);
                        ListPopup popup = JBPopupFactory.getInstance().createListPopup(listPopupModel);
                        popup.showUnderneathOf(label.get());

                        return null;
                    }
                }, "Can't show all paths during index update");
            }
        };
        label.set(answer.createActionLabel(labelText, action));
    }

    @NotNull
    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
        try {
            if (DumbService.isDumb(project)) return null;
            if (file.getFileType() != JetFileType.INSTANCE) return null;

            if (CompilerManager.getInstance(project).isExcludedFromCompilation(file)) return null;

            Module module = ModuleUtilCore.findModuleForFile(file, project);
            if (module == null) return null;

            KotlinProjectConfigurator configurator = ConfigureKotlinInProjectUtils.getConfiguratorByName(KotlinJavaModuleConfigurator.NAME);
            assert configurator != null : "Configurator should exists " + KotlinJavaModuleConfigurator.NAME;
            if (!configurator.isConfigured(module)) {
                return null;
            }

            return checkAndCreate(project);
        }
        catch (ProcessCanceledException e) {
            // Ignore
        }
        catch (IndexNotReadyException e) {
            DumbService.getInstance(project).runWhenSmart(updateNotifications);
        }

        return null;
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

    private final Runnable updateNotifications = new Runnable() {
        @Override
        public void run() {
            updateNotifications();
        }
    };

    private void updateNotifications() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!project.isDisposed()) {
                    EditorNotifications.getInstance(project).updateAllNotifications();
                }
            }
        });
    }

    @NotNull
    private static Collection<VirtualFile> collectBadRoots(@NotNull Project project) {
        Collection<VirtualFile> badRoots = KotlinRuntimeLibraryUtil.getLibraryRootsWithAbiIncompatibleKotlinClasses(project);
        badRoots.addAll(KotlinRuntimeLibraryUtil.getLibraryRootsWithAbiIncompatibleForKotlinJs(project));
        return badRoots;
    }
}
