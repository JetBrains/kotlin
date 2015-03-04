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

package org.jetbrains.kotlin.idea.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeSignatureHandler;
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler;
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty.KotlinIntroducePropertyHandler;
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler;
import org.jetbrains.kotlin.idea.refactoring.safeDelete.SafeDeletePackage;
import org.jetbrains.kotlin.psi.*;

public class JetRefactoringSupportProvider extends RefactoringSupportProvider {
    @Override
    public boolean isSafeDeleteAvailable(@NotNull PsiElement element) {
        return SafeDeletePackage.canDeleteElement(element);
    }

    @Override
    public RefactoringActionHandler getIntroduceVariableHandler() {
        return new KotlinIntroduceVariableHandler();
    }

    @NotNull
    public RefactoringActionHandler getIntroducePropertyHandler() {
        return new KotlinIntroducePropertyHandler();
    }

    @NotNull
    public RefactoringActionHandler getExtractFunctionHandler() {
        return new ExtractKotlinFunctionHandler();
    }

    @NotNull
    public RefactoringActionHandler getExtractFunctionToScopeHandler() {
        return new ExtractKotlinFunctionHandler(true, ExtractKotlinFunctionHandler.InteractiveExtractionHelper.INSTANCE$);
    }

    @Override
    public boolean isInplaceRenameAvailable(@NotNull PsiElement element, PsiElement context) {
        if (element instanceof JetProperty) {
            JetProperty property = (JetProperty) element;
            if (property.isLocal()) return true;
        }
        else if (element instanceof JetFunction) {
            JetFunction function = (JetFunction) element;
            if (function.isLocal()) return true;
        }
        else if (element instanceof JetParameter) {
            PsiElement parent = element.getParent();
            if (parent instanceof JetForExpression) {
                return true;
            }
            if (parent instanceof JetParameterList) {
                PsiElement grandparent = parent.getParent();
                return grandparent instanceof JetCatchClause || grandparent instanceof JetFunctionLiteral;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public ChangeSignatureHandler getChangeSignatureHandler() {
        return new JetChangeSignatureHandler();
    }
}
