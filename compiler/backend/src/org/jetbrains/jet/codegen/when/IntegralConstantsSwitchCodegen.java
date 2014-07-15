/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.when;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.org.objectweb.asm.Label;

public class IntegralConstantsSwitchCodegen extends SwitchCodegen {
    public IntegralConstantsSwitchCodegen(
            @NotNull JetWhenExpression expression,
            boolean isStatement,
            @NotNull ExpressionCodegen codegen
    ) {
        super(expression, isStatement, codegen);
    }

    @Override
    protected void processConstant(
            @NotNull CompileTimeConstant constant, @NotNull Label entryLabel
    ) {
        assert constant.getValue() != null : "constant value should not be null";
        int value = (constant.getValue() instanceof Number)
                    ? ((Number) constant.getValue()).intValue()
                    : ((Character) constant.getValue()).charValue();

        putTransitionOnce(value, entryLabel);
    }
}
