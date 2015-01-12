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

package org.jetbrains.kotlin.idea.ktSignature;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public class ShowKotlinSignaturesAction extends ToggleAction {
    public ShowKotlinSignaturesAction() {
        super("Show Kotlin Signatures");
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        Project project = e.getProject();
        return project != null && KotlinSignatureInJavaMarkerProvider.isMarkersEnabled(project);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(false);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile != null && psiFile.getLanguage() == JavaLanguage.INSTANCE) {
            e.getPresentation().setVisible(true);
        }
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Project project = e.getProject();
        assert project != null;
        KotlinSignatureInJavaMarkerProvider.setMarkersEnabled(project, state);
    }
}
