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

package org.jetbrains.jet.lang.cfg.pseudocode;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.ArrayList;
import java.util.Collection;

/**
* @author svtk
*/
public class LocalDeclarationInstruction extends InstructionWithNext {

    private final Pseudocode body;
    private Instruction sink;

    public LocalDeclarationInstruction(@NotNull JetDeclaration element, Pseudocode body) {
        super(element);
        this.body = body;
    }

    public Pseudocode getBody() {
        return body;
    }

    @NotNull
    @Override
    public JetDeclaration getElement() {
        return (JetDeclaration) super.getElement();
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        if (sink != null) {
            ArrayList<Instruction> instructions = Lists.newArrayList(sink);
            instructions.addAll(super.getNextInstructions());
            return instructions;
        }
        return super.getNextInstructions();
    }

    public void setSink(SubroutineSinkInstruction sink) {
        this.sink = outgoingEdgeTo(sink);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitLocalDeclarationInstruction(this);
    }

    @Override
    public String toString() {
        return "d" + "(" + element.getText() + ")";
    }
}
