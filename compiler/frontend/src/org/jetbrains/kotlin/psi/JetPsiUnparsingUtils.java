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

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JetPsiUnparsingUtils {
    private JetPsiUnparsingUtils() {
    }

    @NotNull
    public static String toIf(@Nullable JetExpression condition, @Nullable JetExpression thenExpression, @Nullable JetExpression elseExpression) {
        return toIf(
                JetPsiUtil.getText(condition),
                JetPsiUtil.getText(thenExpression),
                elseExpression != null ? elseExpression.getText() : null
        );
    }

    @NotNull
    public static String toIf(@NotNull String condition, @NotNull String thenExpression, @Nullable String elseExpression) {
        return "if " + parenthesizeTextIfNeeded(condition) + " " + thenExpression + (elseExpression != null ? " else " + elseExpression : "");
    }

    @NotNull
    public static String toBinaryExpression(@Nullable JetExpression left, @NotNull String op, @Nullable JetElement right) {
        return toBinaryExpression(JetPsiUtil.getText(left), op, JetPsiUtil.getText(right));
    }

    @NotNull
    public static String toBinaryExpression(@NotNull String left, @NotNull String op, @NotNull String right) {
        return left + " " + op + " " + right;
    }

    @NotNull
    public static String parenthesizeIfNeeded(@Nullable JetExpression expression) {
        String text = JetPsiUtil.getText(expression);

        return (expression instanceof JetParenthesizedExpression ||
                expression instanceof JetConstantExpression ||
                expression instanceof JetSimpleNameExpression ||
                expression instanceof JetDotQualifiedExpression)
               ? text : "(" + text + ")";
    }

    @NotNull
    public static String parenthesizeTextIfNeeded(@NotNull String expressionText) {
        return (expressionText.startsWith("(") && expressionText.endsWith(")")) ? expressionText : "(" + expressionText + ")";
    }
}
