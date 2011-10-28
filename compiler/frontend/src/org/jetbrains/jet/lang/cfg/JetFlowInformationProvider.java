package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.LocalVariableDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetStandardClasses;

import java.util.*;

/**
 * @author svtk
 */
public class JetFlowInformationProvider {

    private final Map<JetElement, Pseudocode> pseudocodeMap;
    private BindingTrace trace;

    public JetFlowInformationProvider(@NotNull JetDeclaration declaration, @NotNull final JetExpression bodyExpression, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory, @NotNull BindingTrace trace) {
        this.trace = trace;
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
                public void visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
                    for (Instruction previousInstruction : instruction.getPreviousInstructions()) {
                        previousInstruction.accept(this);
                    }
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

        for (Instruction deadInstruction : pseudocode.getDeadInstructions()) {
            if (deadInstruction instanceof JetElementInstruction &&
                // TODO : do {return} while (1 > a)
                !(deadInstruction instanceof ReadUnitValueInstruction)) {
                unreachableElements.add(((JetElementInstruction) deadInstruction).getElement());
            }
        }

//        SubroutineEnterInstruction enterInstruction = pseudocode.getEnterInstruction();
//        Set<Instruction> visited = new HashSet<Instruction>();
//        collectReachable(enterInstruction, visited, null);
//
//        for (Instruction instruction : pseudocode.getInstructions()) {
//            if (!visited.contains(instruction) &&
//                instruction instanceof JetElementInstruction &&
//                // TODO : do {return} while (1 > a)
//                !(instruction instanceof ReadUnitValueInstruction)) {
//                unreachableElements.add(((JetElementInstruction) instruction).getElement());
//            }
//        }
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

//    private boolean collectReachable(Instruction current, Set<Instruction> visited, @Nullable Instruction lookFor) {
//        if (!visited.add(current)) return false;
//        if (current == lookFor) return true;
//
//        for (Instruction nextInstruction : current.getNextInstructions()) {
//            if (collectReachable(nextInstruction, visited, lookFor)) {
//                return true;
//            }
//        }
//        return false;
//    }

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

    public void markUninitializedVariables(@NotNull JetElement subroutine, List<? extends VariableDescriptor> initializedVariables) {
        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        JetControlFlowGraphTraverser.InstructionsMergeStrategy<Map<VariableDescriptor, InitializationPoints>> instructionsMergeStrategy = new JetControlFlowGraphTraverser.InstructionsMergeStrategy<Map<VariableDescriptor, InitializationPoints>>() {
            @Override
            public Map<VariableDescriptor, InitializationPoints> execute(
                    Instruction instruction,
                    Collection<Map<VariableDescriptor, InitializationPoints>> incomingEdgesData) {

                Set<VariableDescriptor> variablesInScope = Sets.newHashSet();
                for (Map<VariableDescriptor, InitializationPoints> edgePointsMap : incomingEdgesData) {
                    variablesInScope.addAll(edgePointsMap.keySet());
                }

                Map<VariableDescriptor, InitializationPoints> pointsMap = Maps.newHashMap();
                for (VariableDescriptor variable : variablesInScope) {
                    Set<InitializationPoints> edgesDataForVariable = Sets.newHashSet();
                    for (Map<VariableDescriptor, InitializationPoints> edgePointsMap : incomingEdgesData) {
                        InitializationPoints points = edgePointsMap.get(variable);
                        if (points != null) {
                            edgesDataForVariable.add(points);
                        }
                    }
                    pointsMap.put(variable, new InitializationPoints(edgesDataForVariable));
                }
                
                if (instruction instanceof WriteValueInstruction) {
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                        InitializationPoints initializationAtThisPoint = new InitializationPoints(((WriteValueInstruction) instruction).getElement());
                        pointsMap.put(variableDescriptor, initializationAtThisPoint);
                }
                
                return pointsMap;
            }
        };

        Set<VariableDescriptor> localVariables = collectAllLocalVariables(pseudocode);
        Map<VariableDescriptor, InitializationPoints> initialMapForStartInstruction = Maps.newHashMap();
        InitializationPoints initialPointsForLocalVariable = new InitializationPoints(true);
        for (VariableDescriptor variable : localVariables) {
            initialMapForStartInstruction.put(variable, initialPointsForLocalVariable);
        }
        InitializationPoints initialPointsForParameter = new InitializationPoints(false);
        for (VariableDescriptor initializedVariable : initializedVariables) {
            initialMapForStartInstruction.put(initializedVariable, initialPointsForParameter);
        }
        final Map<Instruction, Map<VariableDescriptor, InitializationPoints>> dataMap =
                JetControlFlowGraphTraverser.traverseInstructionGraphUntilFactsStabilization(pseudocode, instructionsMergeStrategy, Collections.<VariableDescriptor, InitializationPoints>emptyMap(), initialMapForStartInstruction, true);

        JetControlFlowGraphTraverser.traverseAndAnalyzeInstructionGraph(pseudocode, new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy() {
            @Override
            public void execute(Instruction instruction) {
                Map<VariableDescriptor, InitializationPoints> variablesData = dataMap.get(instruction);
                if (instruction instanceof ReadValueInstruction) {
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                    JetElement element = ((ReadValueInstruction) instruction).getElement();
                    if (element instanceof JetSimpleNameExpression && variableDescriptor instanceof LocalVariableDescriptor) {
                        InitializationPoints initializationPoints = variablesData.get(variableDescriptor);
                        assert  initializationPoints != null;
                        if (initializationPoints.canBeUninitialized) {
                            trace.report(Errors.UNINITIALIZED_VARIABLE.on((JetSimpleNameExpression) element, variableDescriptor));
                        }
                    }
                }
                else if (instruction instanceof WriteValueInstruction) {
                    JetElement element = ((WriteValueInstruction) instruction).getlValue();
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                    if (element instanceof JetSimpleNameExpression && variableDescriptor != null) {
                        InitializationPoints initializationPoints = variablesData.get(variableDescriptor);
                        assert initializationPoints != null;
                        if (initializationPoints.hasPossibleInitializers() && !variableDescriptor.isVar()) {
                            trace.report(Errors.VAL_REASSIGNMENT.on((JetSimpleNameExpression) element, variableDescriptor));
                        }
                    }
                }
            }
        });
    }
    
    public void markNotOnlyInvokedFunctionVariables(@NotNull JetElement subroutine, List<? extends VariableDescriptor> variables) {
        final List<VariableDescriptor> functionVariables = Lists.newArrayList();
        for (VariableDescriptor variable : variables) {
            if (JetStandardClasses.isFunctionType(variable.getReturnType())) {
                functionVariables.add(variable);
            }
        }

        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        JetControlFlowGraphTraverser.traverseAndAnalyzeInstructionGraph(pseudocode, new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy() {
            @Override
            public void execute(Instruction instruction) {
                if (instruction instanceof ReadValueInstruction) {
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                    if (variableDescriptor != null && functionVariables.contains(variableDescriptor)) {
                        //check that we only invoke this variable
                        JetElement element = ((ReadValueInstruction) instruction).getElement();
                        if (element instanceof JetSimpleNameExpression && !(element.getParent() instanceof JetCallExpression)) {
                            trace.report(Errors.FUNCTION_PARAMETERS_OF_INLINE_FUNCTION.on((JetSimpleNameExpression)element, variableDescriptor));
                        }
                    }
                }
            }
        });
    }

    @Nullable
    private VariableDescriptor extractVariableDescriptorIfAny(Instruction instruction) {
        VariableDescriptor variableDescriptor = null;
        if (instruction instanceof ReadValueInstruction) {
            JetElement element = ((ReadValueInstruction) instruction).getElement();
            if (element instanceof JetSimpleNameExpression) {
                DeclarationDescriptor descriptor = trace.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) element);
                if (descriptor instanceof VariableDescriptor) {
                    variableDescriptor = (VariableDescriptor) descriptor;
                }
            }
        }
        else if (instruction instanceof WriteValueInstruction) {
            DeclarationDescriptor descriptor = null;
            JetElement lValue = ((WriteValueInstruction) instruction).getlValue();
            if (lValue instanceof JetProperty || lValue instanceof JetParameter) {
                descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, lValue);
            }
            else if (lValue instanceof JetSimpleNameExpression) {
                descriptor = trace.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) lValue);
            }
            if (descriptor instanceof VariableDescriptor) {
                variableDescriptor = (VariableDescriptor) descriptor;
            }
        }
        return variableDescriptor;
    }

    private Set<VariableDescriptor> collectAllLocalVariables(Pseudocode pseudocode) {
        final Set<VariableDescriptor> localVariables = Sets.newHashSet();
        JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy analyzeStrategy = new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy() {
            @Override
            public void execute(Instruction instruction) {
                VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                if (variableDescriptor != null) {
                    localVariables.add(variableDescriptor);
                }
            }
        };
        JetControlFlowGraphTraverser.traverseAndAnalyzeInstructionGraph(pseudocode, analyzeStrategy);
        return localVariables;
    }

    private static class InitializationPoints {
        private Set<JetElement> possiblePoints = Sets.newHashSet();
        private boolean canBeUninitialized;

        public InitializationPoints(boolean canBeUninitialized) {
            this.canBeUninitialized = canBeUninitialized;
        }

        public InitializationPoints(JetElement element) {
            canBeUninitialized = false;
            possiblePoints.add(element);
        }
        
        public InitializationPoints(Set<InitializationPoints> edgesData) {
            canBeUninitialized = false;
            for (InitializationPoints edgeData : edgesData) {
                if (edgeData.canBeUninitialized) {
                    canBeUninitialized = true;
                }
                possiblePoints.addAll(edgeData.possiblePoints);
            }
        }

        public boolean hasPossibleInitializers() {
            return !possiblePoints.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InitializationPoints)) return false;

            InitializationPoints that = (InitializationPoints) o;

            if (canBeUninitialized != that.canBeUninitialized) return false;
            if (possiblePoints != null ? !possiblePoints.equals(that.possiblePoints) : that.possiblePoints != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = possiblePoints != null ? possiblePoints.hashCode() : 0;
            result = 31 * result + (canBeUninitialized ? 1 : 0);
            return result;
        }
    }
}