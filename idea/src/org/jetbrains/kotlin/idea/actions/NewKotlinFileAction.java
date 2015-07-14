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

package org.jetbrains.kotlin.idea.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.JetIcons;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils;

import java.util.Map;

public class NewKotlinFileAction extends CreateFileFromTemplateAction implements DumbAware {
    public NewKotlinFileAction() {
        super(JetBundle.message("new.kotlin.file.action"), "Creates new Kotlin file or class", JetFileType.INSTANCE.getIcon());
    }

    @Override
    protected void postProcess(PsiFile createdElement, String templateName, Map<String, String> customProperties) {
        super.postProcess(createdElement, templateName, customProperties);

        Module module = ModuleUtilCore.findModuleForPsiElement(createdElement);
        if (module != null) {
            ConfigureKotlinInProjectUtils.showConfigureKotlinNotificationIfNeeded(module);
        }
    }

    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder
                .setTitle("New Kotlin File/Class")
                .addKind("File", JetFileType.INSTANCE.getIcon(), "Kotlin File")
                .addKind("Class", JetIcons.CLASS, "Kotlin Class")
                .addKind("Interface", JetIcons.TRAIT, "Kotlin Interface")
                .addKind("Enum class", JetIcons.ENUM, "Kotlin Enum")
                .addKind("Object", JetIcons.OBJECT, "Kotlin Object");
    }

    @Override
    protected String getActionName(PsiDirectory directory, String newName, String templateName) {
        return JetBundle.message("new.kotlin.file.action");
    }

    @Override
    protected boolean isAvailable(DataContext dataContext) {
        if (super.isAvailable(dataContext)) {
            IdeView ideView = LangDataKeys.IDE_VIEW.getData(dataContext);
            Project project = PlatformDataKeys.PROJECT.getData(dataContext);
            assert ideView != null && project != null;
            ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            for (PsiDirectory dir : ideView.getDirectories()) {
                if (projectFileIndex.isInSourceContent(dir.getVirtualFile())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NewKotlinFileAction;
    }
}
