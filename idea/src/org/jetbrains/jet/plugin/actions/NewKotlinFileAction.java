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

package org.jetbrains.jet.plugin.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.util.PlatformIcons;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetIcons;

public class NewKotlinFileAction extends CreateFileFromTemplateAction {
    public NewKotlinFileAction() {
        super(JetBundle.message("new.kotlin.file.action"), "Creates new Kotlin file", JetFileType.INSTANCE.getIcon());
    }

    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder
                .setTitle(JetBundle.message("new.kotlin.file.action"))
                .addKind("Kotlin file", JetFileType.INSTANCE.getIcon(), "Kotlin File")
                .addKind("Class", JetIcons.CLASS, "Kotlin Class")
                .addKind("Trait", JetIcons.TRAIT, "Kotlin Trait")
                .addKind("Enum class", JetIcons.ENUM, "Kotlin Enum");
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
