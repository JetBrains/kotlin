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

package org.jetbrains.jet.plugin.intentions.branchedTransformations;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetReturnExpression;
import org.jetbrains.jet.plugin.intentions.Transformer;

public enum UnfoldableKind implements Transformer {
    ASSIGNMENT_TO_IF("unfold.assignment.to.if") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, JetFile file) {
            BranchedUnfoldingUtils.unfoldAssignmentToIf((JetBinaryExpression) element, editor);
        }
    },
    PROPERTY_TO_IF("unfold.property.to.if") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, JetFile file) {
            BranchedUnfoldingUtils.unfoldPropertyToIf((JetProperty) element, file, editor);
        }
    },
    RETURN_TO_IF("unfold.return.to.if") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, JetFile file) {
            BranchedUnfoldingUtils.unfoldReturnToIf((JetReturnExpression) element);
        }
    },
    ASSIGNMENT_TO_WHEN("unfold.assignment.to.when") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, JetFile file) {
            BranchedUnfoldingUtils.unfoldAssignmentToWhen((JetBinaryExpression) element, editor);
        }
    },
    PROPERTY_TO_WHEN("unfold.property.to.when") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, JetFile file) {
            BranchedUnfoldingUtils.unfoldPropertyToWhen((JetProperty) element, file, editor);
        }
    },
    RETURN_TO_WHEN("unfold.return.to.when") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, JetFile file) {
            BranchedUnfoldingUtils.unfoldReturnToWhen((JetReturnExpression) element);
        }
    };

    private final String key;

    private UnfoldableKind(String key) {
        this.key = key;
    }

    @NotNull
    @Override
    public String getKey() {
        return key;
    }
}
