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

package org.jetbrains.kotlin.idea.refactoring.rename;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KotlinLightClass;
import org.jetbrains.kotlin.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.kotlin.asJava.KotlinLightClassForPackage;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetSecondaryConstructor;

import java.util.Map;

public class RenameJetClassProcessor extends RenameKotlinPsiProcessor {
    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        return element instanceof JetClassOrObject || element instanceof KotlinLightClass || element instanceof JetSecondaryConstructor;
    }

    @Nullable
    @Override
    public PsiElement substituteElementToRename(PsiElement element, @Nullable Editor editor) {
        return getJetClassOrObject(element, true, editor);
    }

    @Override
    public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames) {
        JetClassOrObject classOrObject = getJetClassOrObject(element, false, null);

        if (classOrObject != null) {
            JetFile file = classOrObject.getContainingJetFile();

            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                String nameWithoutExtensions = virtualFile.getNameWithoutExtension();
                if (nameWithoutExtensions.equals(classOrObject.getName())) {
                    allRenames.put(file, newName + "." + virtualFile.getExtension());
                }
            }
        }
    }

    @Nullable
    private static JetClassOrObject getJetClassOrObject(@Nullable PsiElement element, boolean showErrors, @Nullable Editor editor) {
        if (element instanceof KotlinLightClass) {
            if (element instanceof KotlinLightClassForExplicitDeclaration) {
                return ((KotlinLightClassForExplicitDeclaration) element).getOrigin();
            }
            else if (element instanceof KotlinLightClassForPackage) {
                if (showErrors) {
                    CommonRefactoringUtil.showErrorHint(
                            element.getProject(), editor,
                            JetBundle.message("rename.kotlin.package.class.error"),
                            RefactoringBundle.message("rename.title"),
                            null);
                }

                // Cancel rename
                return null;
            }
            else {
                assert false : "Should not be suggested to rename element of type " + element.getClass() + " " + element;
            }
        }
        else if (element instanceof JetSecondaryConstructor) {
            return PsiTreeUtil.getParentOfType(element, JetClassOrObject.class);
        }

        return (JetClassOrObject) element;
    }
}
