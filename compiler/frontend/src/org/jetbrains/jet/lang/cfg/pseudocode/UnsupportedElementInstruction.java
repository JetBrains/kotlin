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
import org.jetbrains.jet.lang.psi.JetElement;

public class UnsupportedElementInstruction extends InstructionWithNext {

    protected UnsupportedElementInstruction(@NotNull JetElement element, @NotNull LexicalScope lexicalScope) {
        super(element, lexicalScope);
    }

    @Override
    public void accept(@NotNull InstructionVisitor visitor) {
        visitor.visitUnsupportedElementInstruction(this);
    }

    @Override
    public <R> R accept(@NotNull InstructionVisitorWithResult<R> visitor) {
        return visitor.visitUnsupportedElementInstruction(this);
    }

    @Override
    public String toString() {
        return "unsupported(" + element + " : " + render(element) + ")";
    }

    @NotNull
    @Override
    protected Instruction createCopy() {
        return new UnsupportedElementInstruction(element, lexicalScope);
    }
}
