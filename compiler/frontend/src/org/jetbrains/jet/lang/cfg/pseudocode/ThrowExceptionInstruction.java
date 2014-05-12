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
import org.jetbrains.jet.lang.psi.JetThrowExpression;

import java.util.Collections;
import java.util.List;

public class ThrowExceptionInstruction extends AbstractJumpInstruction {
    private final PseudoValue usedValue;

    public ThrowExceptionInstruction(
            @NotNull JetThrowExpression expression,
            @NotNull LexicalScope lexicalScope,
            @NotNull Label errorLabel,
            @NotNull PseudoValue usedValue
    ) {
        super(expression, errorLabel, lexicalScope);
        this.usedValue = usedValue;
    }

    @Override
    public String toString() {
        return "throw (" + element.getText() + "|" + usedValue + ")";
    }

    @NotNull
    @Override
    public List<PseudoValue> getInputValues() {
        return Collections.singletonList(usedValue);
    }

    @Override
    public void accept(@NotNull InstructionVisitor visitor) {
        visitor.visitThrowExceptionInstruction(this);
    }

    @Override
    public <R> R accept(@NotNull InstructionVisitorWithResult<R> visitor) {
        return visitor.visitThrowExceptionInstruction(this);
    }

    @Override
    protected AbstractJumpInstruction createCopy(@NotNull Label newLabel, @NotNull LexicalScope lexicalScope) {
        return new ThrowExceptionInstruction((JetThrowExpression) element, lexicalScope, newLabel, usedValue);
    }
}
