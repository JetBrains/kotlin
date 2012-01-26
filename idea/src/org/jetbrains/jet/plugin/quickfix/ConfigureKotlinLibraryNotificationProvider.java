/*
 * @author max
 */
package org.jetbrains.jet.plugin.quickfix;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.compiler.CompilerUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ConfigureKotlinLibraryNotificationProvider implements EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("configure.kotlin.library");
    private final Project myProject;

    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    public ConfigureKotlinLibraryNotificationProvider(Project project, final EditorNotifications notifications) {
        myProject = project;
        project.getMessageBus().connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
            @Override
            public void beforeRootsChange(ModuleRootEvent event) {
            }

            @Override
            public void rootsChanged(ModuleRootEvent event) {
                notifications.updateAllNotifications();
            }
        });
    }


    @Override
    public EditorNotificationPanel createNotificationPanel(VirtualFile file) {
        try {
            if (file.getFileType() != JetFileType.INSTANCE) return null;

            if (CompilerManager.getInstance(myProject).isExcludedFromCompilation(file)) return null;

            final Module module = ModuleUtil.findModuleForFile(file, myProject);
            if (module == null) return null;

            if (isMavenModule(module)) return null;

            GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
            if (JavaPsiFacade.getInstance(myProject).findClass("jet.JetObject", scope) == null) {
                return createNotificationPanel(module);
            }

        } catch (ProcessCanceledException e) {
            // Ignore
        } catch (IndexNotReadyException e) {
            // Ignore
        }


        return null;
    }

    private Library findOrCreateRuntimeLibrary(final Module module) {
        LibraryTable table = ProjectLibraryTable.getInstance(myProject);
        final Library kotlinRuntime = table.getLibraryByName("KotlinRuntime");
        if (kotlinRuntime != null) return null;

        File runtimePath = CompilerUtil.getDefaultRuntimePath();
        if (runtimePath == null) {
            Messages.showErrorDialog(myProject, "kotlin-runtime.jar is not found. Make sure plugin is properly installed.", "No Runtime Found");
            return null;
        }

        ChoosePathDialog dlg = new ChoosePathDialog(myProject);
        dlg.show();
        if (!dlg.isOK()) return null;
        String path = dlg.getPath();
        final File targetJar = new File(path, "kotlin-runtime.jar");
        try {
            FileUtil.copy(runtimePath, targetJar);
            VirtualFile jarVfs = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetJar);
            if (jarVfs != null) {
                jarVfs.refresh(false, false);
            }
        } catch (IOException e) {
            Messages.showErrorDialog(myProject, "Error copying jar: " + e.getLocalizedMessage(), "Error Copying File");
            return null;
        }

        final Library answer = table.createLibrary("KotlinRuntime");

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                Library.ModifiableModel model = answer.getModifiableModel();
                model.addRoot(VfsUtil.getUrlForLibraryRoot(targetJar), OrderRootType.CLASSES);
                model.commit();
            }
        });

        return answer;
    }

    private EditorNotificationPanel createNotificationPanel(final Module module) {
        final EditorNotificationPanel answer = new EditorNotificationPanel();

        answer.setText("Kotlin runtime library is not configured for module '" + module.getName() + "'");
        answer.createActionLabel("Setup Kotlin Runtime", new Runnable() {
            @Override
            public void run() {

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        Library library = findOrCreateRuntimeLibrary(module);
                        if (library != null) {
                            ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
                            model.addLibraryEntry(library);
                            model.commit();
                        }
                    }
                });
            }
        });

        return answer;
    }

    private static boolean isMavenModule(@NotNull Module module) {
        for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
            if (root.findChild("pom.xml") != null) return true;
        }

        return false;
    }

    private static class ChoosePathDialog extends DialogWrapper {
        private final Project myProject;
        private TextFieldWithBrowseButton myPathField;

        protected ChoosePathDialog(Project project) {
            super(project);
            myProject = project;
            
            setTitle("Local Kotlin Runtime Path");
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            FileTextField field = FileChooserFactory.getInstance().createFileTextField(descriptor, myDisposable);
            field.getField().setColumns(25);
            myPathField = new TextFieldWithBrowseButton(field.getField());
            myPathField.addBrowseFolderListener("Choose Destination Folder", "Choose folder for file", myProject, descriptor);

            VirtualFile baseDir = myProject.getBaseDir();
            if (baseDir != null) {
                myPathField.setText(baseDir.getPath() + File.separatorChar + "lib");
            }

            return myPathField;
        }
        
        public String getPath() {
            return myPathField.getText();
        }
    }
}
