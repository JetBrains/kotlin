/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import static org.jetbrains.jet.plugin.project.JsModuleDetector.isJsModule;

public class ConfigureKotlinLibraryNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("configure.kotlin.library");
    public static final String LIBRARY_NAME = "KotlinRuntime";
    public static final String KOTLIN_RUNTIME_JAR = "kotlin-runtime.jar";
    private final Project myProject;

    public ConfigureKotlinLibraryNotificationProvider(Project project) {
        myProject = project;
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

            final Module module = ModuleUtilCore.findModuleForFile(file, myProject);
            if (module == null) return null;

            if (!isModuleAlreadyConfigured(module)) {
                return createNotificationPanel(module);
            }
        }
        catch (ProcessCanceledException e) {
            // Ignore
        }
        catch (IndexNotReadyException e) {
            // Ignore
        }

        return null;
    }

    private Library findOrCreateRuntimeLibrary() {
        LibraryTable table = ProjectLibraryTable.getInstance(myProject);
        Library kotlinRuntime = table.getLibraryByName(LIBRARY_NAME);
        if (kotlinRuntime != null) {
            for (VirtualFile root : kotlinRuntime.getFiles(OrderRootType.CLASSES)) {
                if (root.getName().equals(KOTLIN_RUNTIME_JAR)) {
                    return kotlinRuntime;
                }
            }
        }

        File runtimePath = PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath();
        if (!runtimePath.exists()) {
            Messages.showErrorDialog(myProject, "kotlin-runtime.jar is not found. Make sure plugin is properly installed.",
                                     "No Runtime Found");
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
        }
        catch (IOException e) {
            Messages.showErrorDialog(myProject, "Error copying jar: " + e.getLocalizedMessage(), "Error Copying File");
            return null;
        }

        if (kotlinRuntime == null) {
            kotlinRuntime = table.createLibrary("KotlinRuntime");
        }

        final Library finalKotlinRuntime = kotlinRuntime;
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                Library.ModifiableModel model = finalKotlinRuntime.getModifiableModel();
                model.addRoot(VfsUtil.getUrlForLibraryRoot(targetJar), OrderRootType.CLASSES);
                model.addRoot(VfsUtil.getUrlForLibraryRoot(targetJar) + "src", OrderRootType.SOURCES);
                model.commit();
            }
        });

        return kotlinRuntime;
    }

    private EditorNotificationPanel createNotificationPanel(final Module module) {
        final EditorNotificationPanel answer = new EditorNotificationPanel();

        answer.setText("Kotlin is not configured for module '" + module.getName() + "'");
        answer.createActionLabel("Set up module '" + module.getName() + "' as JVM Kotlin module", new Runnable() {
            @Override
            public void run() {
                setUpKotlinRuntime(module);
            }
        });

        answer.createActionLabel("Set up module '" + module.getName() + "' as JavaScript Kotlin module", new Runnable() {
            @Override
            public void run() {
                setUpJSModule(module);
            }
        });

        return answer;
    }

    private void setUpJSModule(@NotNull Module module) {
        JsModuleSetUp.doSetUpModule(module, new Runnable() {
            @Override
            public void run() {
                updateNotifications();
            }
        });
    }

    private void setUpKotlinRuntime(@NotNull final Module module) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                Library library = findOrCreateRuntimeLibrary();
                if (library != null) {
                    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
                    if (model.findLibraryOrderEntry(library) == null) {
                        model.addLibraryEntry(library);
                        model.commit();
                    }
                    else {
                        model.dispose();
                    }
                    updateNotifications();
                }
                if (!jdkAnnotationsArePresent(module)) {
                    addJdkAnnotations(module);
                }
            }
        });
    }

    /* package */ static void addJdkAnnotations(Module module) {
        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        assert sdk != null;
        File annotationsIoFile = PathUtil.getKotlinPathsForIdeaPlugin().getJdkAnnotationsPath();
        if (annotationsIoFile.exists()) {
            VirtualFile jdkAnnotationsJar = LocalFileSystem.getInstance().findFileByIoFile(annotationsIoFile);
            if (jdkAnnotationsJar != null) {
                SdkModificator modificator = sdk.getSdkModificator();
                modificator.addRoot(JarFileSystem.getInstance().getJarRootForLocalFile(jdkAnnotationsJar),
                                    AnnotationOrderRootType.getInstance());
                modificator.commitChanges();
            }
        }
    }

    /* package */ static boolean jdkAnnotationsArePresent(Module module) {
        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null) return false;
        return ContainerUtil.exists(sdk.getRootProvider().getFiles(AnnotationOrderRootType.getInstance()),
                                    new Condition<VirtualFile>() {
                                        @Override
                                        public boolean value(VirtualFile file) {
                                            return PathUtil.JDK_ANNOTATIONS_JAR.equals(file.getName());
                                        }
                                    });
    }

    private void updateNotifications() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                EditorNotifications.getInstance(myProject).updateAllNotifications();
            }
        });
    }

    private boolean isModuleAlreadyConfigured(Module module) {
        return isMavenModule(module) || isJsModule(module) || isWithJavaModule(module);
    }

    private boolean isWithJavaModule(Module module) {
        // Can find a reference to kotlin class in module scope
        GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
        return (JavaPsiFacade.getInstance(myProject).findClass(JvmStdlibNames.JET_OBJECT.getFqName().getFqName(), scope) != null);
    }

    private static boolean isMavenModule(@NotNull Module module) {
        // This constant could be acquired from MavenProjectsManager, but we don't want to depend on the Maven plugin...
        // See MavenProjectsManager.isMavenizedModule()
        return "true".equals(module.getOptionValue("org.jetbrains.idea.maven.project.MavenProjectsManager.isMavenModule"));
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
                myPathField.setText(baseDir.getPath().replace('/', File.separatorChar) + File.separatorChar + "lib");
            }

            return myPathField;
        }

        public String getPath() {
            return myPathField.getText();
        }
    }
}
