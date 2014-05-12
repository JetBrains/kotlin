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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;

public class ReadValueInstruction extends InstructionWithReceiver {
    private final PseudoValue resultValue;

    private ReadValueInstruction(
            @NotNull JetElement element,
            @NotNull LexicalScope lexicalScope,
            @Nullable PseudoValue receiverValue,
            @NotNull PseudoValue resultValue) {
        super(element, lexicalScope, receiverValue);
        this.resultValue = resultValue;
    }

    public ReadValueInstruction(
            @NotNull JetElement element,
            @NotNull LexicalScope lexicalScope,
            @Nullable PseudoValue receiverValue,
            @NotNull PseudoValueFactory valueFactory) {
        super(element, lexicalScope, receiverValue);
        this.resultValue = valueFactory.newValue(element, this);
    }

    @NotNull
    @Override
    public PseudoValue getOutputValue() {
        return resultValue;
    }

    @Override
    public void accept(@NotNull InstructionVisitor visitor) {
        visitor.visitReadValue(this);
    }

    @Override
    public <R> R accept(@NotNull InstructionVisitorWithResult<R> visitor) {
        return visitor.visitReadValue(this);
    }

    @Override
    public String toString() {
        return "r(" + render(element) + (receiverValue != null ? ("|" + receiverValue) : "") + ") -> " + resultValue;
    }

    @NotNull
    @Override
    protected Instruction createCopy() {
        return new ReadValueInstruction(element, lexicalScope, receiverValue, resultValue);
    }
}
