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

package org.jetbrains.jet.plugin.completion.confidence;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author Nikolay Krasko
 */
public class UnfocusedPossibleFunctionParameter extends CompletionConfidence {
    @NotNull
    @Override
    public ThreeState shouldFocusLookup(@NotNull CompletionParameters parameters) {
        // 1. Do not automatically insert completion for first reference expression in block inside
        // function literal if it has no parameters yet.

        // 2. The same but for the case when first expression is additionally surrounded with brackets

        PsiElement position = parameters.getPosition();
        JetFunctionLiteralExpression functionLiteral = PsiTreeUtil.getParentOfType(
                position, JetFunctionLiteralExpression.class);

        if (functionLiteral != null) {
            PsiElement expectedReference = position.getParent();
            if (expectedReference instanceof JetSimpleNameExpression) {
                if (PsiTreeUtil.findChildOfType(functionLiteral, JetParameterList.class) == null) {
                    {
                        // 1.
                        PsiElement expectedBlock = expectedReference.getParent();
                        if (expectedBlock instanceof JetBlockExpression) {
                            if (expectedReference.getPrevSibling() == null) {
                                return ThreeState.NO;
                            }
                        }
                    }

                    {
                        // 2.
                        PsiElement expectedParenthesized = expectedReference.getParent();
                        if (expectedParenthesized instanceof JetParenthesizedExpression) {
                            PsiElement expectedBlock = expectedParenthesized.getParent();
                            if (expectedBlock instanceof JetBlockExpression) {
                                if (expectedParenthesized.getPrevSibling() == null) {
                                    return ThreeState.NO;
                                }
                            }
                        }
                    }
                }
            }
        }

        return ThreeState.UNSURE;
    }
}

