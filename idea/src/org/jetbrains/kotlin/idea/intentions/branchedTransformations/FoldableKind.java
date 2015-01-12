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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.intentions.Transformer;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetIfExpression;
import org.jetbrains.kotlin.psi.JetWhenExpression;

public enum FoldableKind implements Transformer {
    IF_TO_ASSIGNMENT("fold.if.to.assignment") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            BranchedFoldingUtils.foldIfExpressionWithAssignments((JetIfExpression) element);
        }
    },
    IF_TO_RETURN("fold.if.to.return") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            BranchedFoldingUtils.foldIfExpressionWithReturns((JetIfExpression) element);
        }
    },
    IF_TO_RETURN_ASYMMETRICALLY("fold.if.to.return") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            BranchedFoldingUtils.foldIfExpressionWithAsymmetricReturns((JetIfExpression) element);
        }
    },
    WHEN_TO_ASSIGNMENT("fold.when.to.assignment") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            BranchedFoldingUtils.foldWhenExpressionWithAssignments((JetWhenExpression) element);
        }
    },
    WHEN_TO_RETURN("fold.when.to.return") {
        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            BranchedFoldingUtils.foldWhenExpressionWithReturns((JetWhenExpression) element);
        }
    };

    private final String key;

    private FoldableKind(String key) {
        this.key = key;
    }

    @NotNull
    @Override
    public String getKey() {
        return key;
    }
}
