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

public class EditSignatureAction extends AnAction {
    private final PsiModifierListOwner elementInEditor;

    public EditSignatureAction(@NotNull PsiModifierListOwner elementInEditor) {
        super(KotlinSignatureUtil.isAnnotationEditable(elementInEditor) ? "Edit" : "View");
        this.elementInEditor = elementInEditor;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        actionPerformed(e.getDataContext(), null);
    }

    public void actionPerformed(@NotNull DataContext dataContext, @Nullable Point point) {
        Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
        assert editor != null;
        invokeEditSignature(elementInEditor, editor, point);
    }

    static void invokeEditSignature(@NotNull PsiElement elementInEditor, @NotNull Editor editor, @Nullable Point point) {
        PsiAnnotation annotation = KotlinSignatureUtil.findKotlinSignatureAnnotation(elementInEditor);
        assert annotation != null;
        if (annotation.getContainingFile() == elementInEditor.getContainingFile()) {
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
            PsiModifierListOwner annotationOwner = KotlinSignatureUtil.getAnalyzableAnnotationOwner(elementInEditor);
            boolean editable = KotlinSignatureUtil.isAnnotationEditable(elementInEditor);
            //noinspection ConstantConditions
            EditSignatureBalloon balloon = new EditSignatureBalloon(annotationOwner, KotlinSignatureUtil.getKotlinSignature(annotation),
                                                                    editable, annotation.getQualifiedName());
            balloon.show(point, editor, elementInEditor);
        }
    }
}
