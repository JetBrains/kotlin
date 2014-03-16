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

package org.jetbrains.jet.cfg;

import kotlin.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.Instruction;
import org.jetbrains.jet.lang.cfg.pseudocode.InstructionImpl;
import org.jetbrains.jet.lang.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.*;

public abstract class AbstractControlFlowTest extends AbstractPseudocodeTest {

    @Override
    protected void dumpInstructions(
            @NotNull PseudocodeImpl pseudocode,
            @NotNull StringBuilder out,
            @NotNull BindingContext bindingContext
    ) {
        final int nextInstructionsColumnWidth = countNextInstructionsColumnWidth(pseudocode.getAllInstructions());

        dumpInstructions(pseudocode, out, new Function3<Instruction, Instruction, Instruction, String>() {
            @Override
            public String invoke(Instruction instruction, Instruction next, Instruction prev) {
                StringBuilder result = new StringBuilder();
                Collection<Instruction> nextInstructions = instruction.getNextInstructions();

                if (!sameContents(next, nextInstructions)) {
                    result.append("    NEXT:").append(
                            String.format("%1$-" + nextInstructionsColumnWidth + "s", formatInstructionList(nextInstructions)));
                }
                Collection<Instruction> previousInstructions = instruction.getPreviousInstructions();
                if (!sameContents(prev, previousInstructions)) {
                    result.append("    PREV:").append(formatInstructionList(previousInstructions));
                }
                return result.toString();
            }
        });
    }

    private static String formatInstructionList(Collection<Instruction> instructions) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Iterator<Instruction> iterator = instructions.iterator(); iterator.hasNext(); ) {
            Instruction instruction = iterator.next();
            String instructionText = instruction.toString();
            String[] parts = instructionText.split("\n");
            if (parts.length > 1) {
                StringBuilder instructionSb = new StringBuilder();
                for (String part : parts) {
                    instructionSb.append(part.trim()).append(' ');
                }
                if (instructionSb.toString().length() > 30) {
                    sb.append(instructionSb.substring(0, 28)).append("..)");
                }
                else {
                    sb.append(instructionSb);
                }
            }
            else {
                sb.append(instruction);
            }
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }


    private static int countNextInstructionsColumnWidth(List<Instruction> instructions) {
        int maxWidth = 0;
        for (Instruction instruction : instructions) {
            String instructionListText = formatInstructionList(instruction.getNextInstructions());
            if (instructionListText.length() > maxWidth) {
                maxWidth = instructionListText.length();
            }
        }
        return maxWidth;
    }

    private static boolean sameContents(@Nullable Instruction natural, Collection<Instruction> actual) {
        if (natural == null) {
            return actual.isEmpty();
        }
        return Collections.singleton(natural).equals(new HashSet<Instruction>(actual));
    }

    @Override
    protected void checkPseudocode(PseudocodeImpl pseudocode) {
        //check edges directions
        Collection<Instruction> instructions = pseudocode.getAllInstructions();
        for (Instruction instruction : instructions) {
            if (!((InstructionImpl)instruction).isDead()) {
                for (Instruction nextInstruction : instruction.getNextInstructions()) {
                    assertTrue("instruction '" + instruction + "' has '" + nextInstruction + "' among next instructions list, but not vice versa",
                               nextInstruction.getPreviousInstructions().contains(instruction));
                }
                for (Instruction prevInstruction : instruction.getPreviousInstructions()) {
                    assertTrue("instruction '" + instruction + "' has '" + prevInstruction + "' among previous instructions list, but not vice versa",
                               prevInstruction.getNextInstructions().contains(instruction));
                }
            }
        }
    }
}
