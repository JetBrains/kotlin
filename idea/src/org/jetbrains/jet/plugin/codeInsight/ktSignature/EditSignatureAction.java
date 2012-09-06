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

package org.jetbrains.jet.plugin.codeInsight.ktSignature;

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

import static org.jetbrains.jet.plugin.codeInsight.ktSignature.KotlinSignatureUtil.*;

/**
 * @author Evgeny Gerashchenko
 * @since 5 Sep 12
 */
public class EditSignatureAction extends AnAction {
    private final PsiMethod annotationOwner;

    public EditSignatureAction(@NotNull PsiMethod annotationOwner) {
        super("Edit");
        this.annotationOwner = annotationOwner;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        actionPerformed(e.getDataContext(), null);
    }

    public void actionPerformed(@NotNull DataContext dataContext, @Nullable Point point) {
        Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
        assert editor != null;
        invokeEditSignature(annotationOwner, editor, point);
    }

    static void invokeEditSignature(@NotNull PsiMethod element, @NotNull Editor editor, @Nullable Point point) {
        PsiAnnotation annotation = findKotlinSignatureAnnotation(element);
        assert annotation != null;
        if (annotation.getContainingFile() == element.getContainingFile()) {
            // not external, go to
            for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
                if (pair.getName() == null || "value".equals(pair.getName())) {
                    PsiAnnotationMemberValue value = pair.getValue();
                    if (value != null) {
                        VirtualFile virtualFile = value.getContainingFile().getVirtualFile();
                        assert virtualFile != null;

                        PsiElement firstChild = value.getFirstChild();
                        if (firstChild != null && firstChild.getNode().getElementType() == JavaTokenType.STRING_LITERAL) {
                            new OpenFileDescriptor(value.getProject(), virtualFile, value.getTextOffset() + 1).navigate(true);
                        }
                        else {
                            NavigationUtil.activateFileWithPsiElement(value);
                        }
                    }
                }
            }
        }
        else {
            PsiMethod annotationOwner = getAnnotationOwner(element);
            boolean editable = ExternalAnnotationsManager.getInstance(element.getProject())
                    .isExternalAnnotationWritable(annotationOwner, KOTLIN_SIGNATURE_ANNOTATION);
            new EditSignatureBalloon(annotationOwner, getKotlinSignature(annotation), editable).show(point, editor, element);
        }
    }
}
