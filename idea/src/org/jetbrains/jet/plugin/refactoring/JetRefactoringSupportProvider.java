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

package org.jetbrains.jet.plugin.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureHandler;
import org.jetbrains.jet.plugin.refactoring.introduceVariable.JetIntroduceVariableHandler;

/**
 * User: Alefas
 * Date: 25.01.12
 */
public class JetRefactoringSupportProvider extends RefactoringSupportProvider {
    @Override
    public RefactoringActionHandler getIntroduceVariableHandler() {
        return new JetIntroduceVariableHandler();
    }

    @Override
    public boolean isInplaceRenameAvailable(PsiElement element, PsiElement context) {
        if (element instanceof JetProperty) {
            JetProperty property = (JetProperty) element;
            if (property.isLocal()) return true;
        }
        else if (element instanceof JetFunction) {
            JetFunction function = (JetFunction) element;
            if (function.isLocal()) return true;
        }
        return false;
    }

    @Nullable
    @Override
    public ChangeSignatureHandler getChangeSignatureHandler() {
        return new JetChangeSignatureHandler();
    }
}
