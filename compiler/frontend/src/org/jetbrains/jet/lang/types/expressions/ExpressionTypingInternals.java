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

package org.jetbrains.jet.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.types.JetTypeInfo;

/*package*/ interface ExpressionTypingInternals extends ExpressionTypingFacade {
    @NotNull
    JetTypeInfo checkInExpression(JetElement callElement, @NotNull JetSimpleNameExpression operationSign, @Nullable JetExpression left, @NotNull JetExpression right, ExpressionTypingContext context);

    void checkStatementType(@NotNull JetExpression expression, ExpressionTypingContext context);

    @NotNull
    ExpressionTypingComponents getComponents();
}
