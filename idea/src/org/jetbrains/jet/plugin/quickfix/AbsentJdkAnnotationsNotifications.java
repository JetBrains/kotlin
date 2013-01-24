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

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;

public class AbsentJdkAnnotationsNotifications extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("add.kotlin.jdk.annotations");

    private final Project project;

    public AbsentJdkAnnotationsNotifications(Project project) {
        this.project = project;
    }

    @Override
    @Nullable
    public EditorNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
        if (file.getFileType() != JetFileType.INSTANCE) return null;
        if (CompilerManager.getInstance(project).isExcludedFromCompilation(file)) return null;

        final Module module = ModuleUtil.findModuleForFile(file, project);
        if (module == null) return null;

        GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
        if (JavaPsiFacade.getInstance(project).findClass("jet.JetObject", scope) == null) return null;
        if (KotlinRuntimeLibraryUtil.jdkAnnotationsArePresent(module)) return null;

        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null) return null;

        EditorNotificationPanel panel = new EditorNotificationPanel();
        panel.setText("Kotlin external annotations for JDK are not set for '" + sdk.getName() + "'.");
        panel.createActionLabel("Set up Kotlin JDK annotations", new Runnable() {
            @Override
            public void run() {
                KotlinRuntimeLibraryUtil.addJdkAnnotations(module);
            }
        });

        return panel;
    }

    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }
}
