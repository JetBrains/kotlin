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

package org.jetbrains.jet.plugin.refactoring.rename;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.KotlinLightClass;
import org.jetbrains.jet.lang.psi.JetFunction;

public class RenameKotlinFunctionProcessor extends RenamePsiElementProcessor {
    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        if (element instanceof PsiMethod && ((PsiMethod) element).getContainingClass() instanceof KotlinLightClass) {
            return true;
        }
        return element instanceof JetFunction;
    }

    @Override
    public PsiElement substituteElementToRename(PsiElement element, @Nullable Editor editor) {
        if (element instanceof PsiMethod && element instanceof PsiCompiledElement &&
            ((PsiMethod) element).getContainingClass() instanceof KotlinLightClass) {
            return ((PsiCompiledElement) element).getMirror();
        }
        return super.substituteElementToRename(element, editor);
    }
}
