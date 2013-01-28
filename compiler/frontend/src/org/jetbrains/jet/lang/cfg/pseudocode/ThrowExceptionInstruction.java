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

package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetThrowExpression;

public class ThrowExceptionInstruction extends AbstractJumpInstruction implements JetElementInstruction {
    private final JetThrowExpression expression;

    public ThrowExceptionInstruction(@NotNull JetThrowExpression expression, @NotNull Label errorLabel) {
        super(errorLabel);
        this.expression = expression;
    }

    @NotNull
    @Override
    public JetExpression getElement() {
        return expression;
    }

    @Override
    public String toString() {
        return "throw (" + expression.getText() + ")";
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitThrowExceptionInstruction(this);
    }

    @Override
    protected AbstractJumpInstruction createCopy(@NotNull Label newLabel) {
        return new ThrowExceptionInstruction(expression, newLabel);
    }
}
