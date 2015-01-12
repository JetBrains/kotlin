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

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KOTLIN_SIGNATURE;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.OLD_KOTLIN_SIGNATURE;

public class DeleteSignatureAction extends AnAction {
    private final PsiModifierListOwner annotationOwner;

    public DeleteSignatureAction(@NotNull PsiModifierListOwner elementInEditor) {
        super("Delete");
        this.annotationOwner = KotlinSignatureUtil.getAnalyzableAnnotationOwner(elementInEditor);
        getTemplatePresentation().setVisible(annotationOwner != null && KotlinSignatureUtil.isAnnotationEditable(elementInEditor));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        assert project != null;

        final PsiAnnotation annotation = KotlinSignatureUtil.findKotlinSignatureAnnotation(annotationOwner);
        assert annotation != null;

        if (annotation.getContainingFile() != annotationOwner.getContainingFile()) {
            // external annotation
            new WriteCommandAction(project) {
                @Override
                protected void run(@NotNull Result result) throws Throwable {
                    ExternalAnnotationsManager.getInstance(project).deannotate(annotationOwner, KOTLIN_SIGNATURE.asString());
                    ExternalAnnotationsManager.getInstance(project).deannotate(annotationOwner, OLD_KOTLIN_SIGNATURE.asString());
                }
            }.execute();
        }
        else {
            new WriteCommandAction(project) {
                @Override
                protected void run(@NotNull Result result) throws Throwable {
                    annotation.delete();
                }
            }.execute();
        }


        KotlinSignatureUtil.refreshMarkers(project);
    }
}
