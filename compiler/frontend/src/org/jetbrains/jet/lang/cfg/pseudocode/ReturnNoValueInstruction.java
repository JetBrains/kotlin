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
import org.jetbrains.jet.lang.psi.JetElement;

public class ReturnNoValueInstruction extends AbstractJumpInstruction implements ReturnInstruction {
    public ReturnNoValueInstruction(@NotNull JetElement element, LexicalScope lexicalScope, Label targetLabel) {
        super(element, targetLabel, lexicalScope);
    }

    @Override
    public void accept(@NotNull InstructionVisitor visitor) {
        visitor.visitReturnNoValue(this);
    }

    @Override
    public <R> R accept(@NotNull InstructionVisitorWithResult<R> visitor) {
        return visitor.visitReturnNoValue(this);
    }

    @Override
    public String toString() {
        return "ret " + getTargetLabel();
    }

    @Override
    protected AbstractJumpInstruction createCopy(@NotNull Label newLabel, @NotNull LexicalScope lexicalScope) {
        return new ReturnNoValueInstruction(element, lexicalScope, newLabel);
    }
}
