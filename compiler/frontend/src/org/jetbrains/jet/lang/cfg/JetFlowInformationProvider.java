package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;

import java.util.*;

/**
* @author svtk
*/
public class JetFlowInformationProvider {

    private final Map<JetElement, Pseudocode> pseudocodeMap;

    public JetFlowInformationProvider(@NotNull JetElement declaration, @NotNull final JetExpression bodyExpression, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory, @NotNull BindingTrace trace) {
        final JetPseudocodeTrace pseudocodeTrace = flowDataTraceFactory.createTrace(declaration);
        pseudocodeMap = new HashMap<JetElement, Pseudocode>();
        final Map<JetElement, Instruction> representativeInstructions = new HashMap<JetElement, Instruction>();
        final Map<JetExpression, LoopInfo> loopInfo = Maps.newHashMap();
        JetPseudocodeTrace wrappedTrace = new JetPseudocodeTrace() {
            @Override
            public void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode) {
                pseudocodeTrace.recordControlFlowData(element, pseudocode);
                pseudocodeMap.put(element, pseudocode);
            }

            @Override
            public void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction) {
                Instruction oldValue = representativeInstructions.put(element, instruction);
//                assert oldValue == null : element.getText();
                pseudocodeTrace.recordRepresentativeInstruction(element, instruction);
            }

            @Override
            public void recordLoopInfo(JetExpression expression, LoopInfo blockInfo) {
                loopInfo.put(expression, blockInfo);
                pseudocodeTrace.recordLoopInfo(expression, blockInfo);
            }

            @Override
            public void close() {
                pseudocodeTrace.close();
                for (Pseudocode pseudocode : pseudocodeMap.values()) {
                    pseudocode.postProcess();
                }
            }
        };
        JetControlFlowInstructionsGenerator instructionsGenerator = new JetControlFlowInstructionsGenerator(wrappedTrace);
        new JetControlFlowProcessor(trace, instructionsGenerator).generate(declaration, bodyExpression);
        wrappedTrace.close();
    }

//        public void collectReturnedInformation(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions, @NotNull Collection<JetElement> elementsReturningUnit) {
//            Pseudocode pseudocode = pseudocodeMap.get(subroutine);
//            assert pseudocode != null;
//
//            SubroutineExitInstruction exitInstruction = pseudocode.getExitInstruction();
//            processPreviousInstructions(exitInstruction, new HashSet<Instruction>(), returnedExpressions, elementsReturningUnit);
//        }

    public void collectReturnExpressions(@NotNull JetElement subroutine, @NotNull final Collection<JetExpression> returnedExpressions) {
        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        SubroutineExitInstruction exitInstruction = pseudocode.getExitInstruction();
        for (Instruction previousInstruction : exitInstruction.getPreviousInstructions()) {
            previousInstruction.accept(new InstructionVisitor() {
                @Override
                public void visitReturnValue(ReturnValueInstruction instruction) {
                    returnedExpressions.add((JetExpression) instruction.getElement());
                }

                @Override
                public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
                    returnedExpressions.add((JetExpression) instruction.getElement());
                }


                @Override
                public void visitJump(AbstractJumpInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitInstruction(Instruction instruction) {
                    if (instruction instanceof JetElementInstruction) {
                        JetElementInstruction elementInstruction = (JetElementInstruction) instruction;
                        returnedExpressions.add((JetExpression) elementInstruction.getElement());
                    }
                    else {
                        throw new IllegalStateException(instruction + " precedes the exit point");
                    }
                }
            });
        }
    }

    public void collectUnreachableExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetElement> unreachableElements) {
        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        SubroutineEnterInstruction enterInstruction = pseudocode.getEnterInstruction();
        Set<Instruction> visited = new HashSet<Instruction>();
        collectReachable(enterInstruction, visited, null);

        for (Instruction instruction : pseudocode.getInstructions()) {
            if (!visited.contains(instruction) &&
                instruction instanceof JetElementInstruction &&
                // TODO : do {return} while (1 > a)
                !(instruction instanceof ReadUnitValueInstruction)) {
                unreachableElements.add(((JetElementInstruction) instruction).getElement());
            }
        }
    }
//        public void collectDominatedExpressions(@NotNull JetExpression dominator, @NotNull Collection<JetElement> dominated) {
//            Instruction dominatorInstruction = representativeInstructions.get(dominator);
//            if (dominatorInstruction == null) {
//                return;
//            }
//            SubroutineEnterInstruction enterInstruction = dominatorInstruction.getOwner().getEnterInstruction();
//
//            Set<Instruction> reachable = new HashSet<Instruction>();
//            collectReachable(enterInstruction, reachable, null);
//
//            Set<Instruction> reachableWithDominatorProhibited = new HashSet<Instruction>();
//            reachableWithDominatorProhibited.add(dominatorInstruction);
//            collectReachable(enterInstruction, reachableWithDominatorProhibited, null);
//
//            for (Instruction instruction : reachable) {
//                if (instruction instanceof JetElementInstruction
//                    && reachable.contains(instruction)
//                    && !reachableWithDominatorProhibited.contains(instruction)) {
//                    JetElementInstruction elementInstruction = (JetElementInstruction) instruction;
//                    dominated.add(elementInstruction.getElement());
//                }
//            }
//        }
//
//        public boolean isBreakable(JetLoopExpression loop) {
//            LoopInfo info = loopInfo.get(loop);
//            Pseudocode.PseudocodeLabel bodyEntryPoint = (Pseudocode.PseudocodeLabel) info.getBodyEntryPoint();
//            Pseudocode.PseudocodeLabel exitPoint = (Pseudocode.PseudocodeLabel) info.getExitPoint();
//            HashSet<Instruction> visited = Sets.newHashSet();
//            Pseudocode.PseudocodeLabel conditionEntryPoint = (Pseudocode.PseudocodeLabel) info.getConditionEntryPoint();
//            visited.add(conditionEntryPoint.resolveToInstruction());
//            return collectReachable(bodyEntryPoint.resolveToInstruction(), visited, exitPoint.resolveToInstruction());
//        }
//
//        public boolean isReachable(JetExpression from, JetExpression to) {
//            Instruction fromInstr = representativeInstructions.get(from);
//            assert fromInstr != null : "No representative instruction for " + from.getText();
//            Instruction toInstr = representativeInstructions.get(to);
//            assert toInstr != null : "No representative instruction for " + to.getText();
//
//            return collectReachable(fromInstr, Sets.<Instruction>newHashSet(), toInstr);
//        }
//    }

    private boolean collectReachable(Instruction current, Set<Instruction> visited, @Nullable Instruction lookFor) {
        if (!visited.add(current)) return false;
        if (current == lookFor) return true;

        for (Instruction nextInstruction : current.getNextInstructions()) {
            if (collectReachable(nextInstruction, visited, lookFor)) {
                return true;
            }
        }
        return false;
    }

//        private void processPreviousInstructions(Instruction previousFor, final Set<Instruction> visited, final Collection<JetExpression> returnedExpressions, final Collection<JetElement> elementsReturningUnit) {
//            if (!visited.add(previousFor)) return;
//
//            Collection<Instruction> previousInstructions = previousFor.getPreviousInstructions();
//            InstructionVisitor visitor = new InstructionVisitor() {
//                @Override
//                public void visitReadValue(ReadValueInstruction instruction) {
//                    returnedExpressions.add((JetExpression) instruction.getElement());
//                }
//
//                @Override
//                public void visitReturnValue(ReturnValueInstruction instruction) {
//                    processPreviousInstructions(instruction, visited, returnedExpressions, elementsReturningUnit);
//                }
//
//                @Override
//                public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
//                    elementsReturningUnit.add(instruction.getElement());
//                }
//
//                @Override
//                public void visitSubroutineEnter(SubroutineEnterInstruction instruction) {
//                    elementsReturningUnit.add(instruction.getSubroutine());
//                }
//
//                @Override
//                public void visitUnsupportedElementInstruction(UnsupportedElementInstruction instruction) {
//                    context.getTrace().report(UNSUPPORTED.on(instruction.getElement(), "Control-flow builder"));
//                }
//
//                @Override
//                public void visitWriteValue(WriteValueInstruction writeValueInstruction) {
//                    elementsReturningUnit.add(writeValueInstruction.getElement());
//                }
//
//                @Override
//                public void visitJump(AbstractJumpInstruction instruction) {
//                    processPreviousInstructions(instruction, visited, returnedExpressions, elementsReturningUnit);
//                }
//
//                @Override
//                public void visitReadUnitValue(ReadUnitValueInstruction instruction) {
//                    returnedExpressions.add((JetExpression) instruction.getElement());
//                }
//
//                @Override
//                public void visitInstruction(Instruction instruction) {
//                    if (instruction instanceof JetElementInstructionImpl) {
//                        JetElementInstructionImpl elementInstruction = (JetElementInstructionImpl) instruction;
//                        context.getTrace().report(UNSUPPORTED.on(elementInstruction.getElement(), "Control-flow builder"));
//                    }
//                    else {
//                        throw new UnsupportedOperationException(instruction.toString());
//                    }
//                }
//            };
//            for (Instruction previousInstruction : previousInstructions) {
//                previousInstruction.accept(visitor);
//            }
//        }
}
