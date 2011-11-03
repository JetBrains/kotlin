package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;

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
    }

    public void markUninitializedVariables(@NotNull JetElement subroutine, final boolean inAnonymousInitializers, final boolean declaredLocally) {
        final Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        JetControlFlowGraphTraverser<Map<VariableDescriptor, InitializationPoints>> traverser = JetControlFlowGraphTraverser.create(pseudocode, true);

        JetControlFlowGraphTraverser.InstructionsMergeStrategy<Map<VariableDescriptor, InitializationPoints>> instructionsMergeStrategy =
                new JetControlFlowGraphTraverser.InstructionsMergeStrategy<Map<VariableDescriptor, InitializationPoints>>() {
            @Override
            public Pair<Map<VariableDescriptor, InitializationPoints>, Map<VariableDescriptor, InitializationPoints>> execute(
                    Instruction instruction,
                    @NotNull Collection<Map<VariableDescriptor, InitializationPoints>> incomingEdgesData) {

                Set<VariableDescriptor> variablesInScope = Sets.newHashSet();
                for (Map<VariableDescriptor, InitializationPoints> edgePointsMap : incomingEdgesData) {
                    variablesInScope.addAll(edgePointsMap.keySet());
                }

                Map<VariableDescriptor, InitializationPoints> enterInstructionPointsMap = Maps.newHashMap();
                for (VariableDescriptor variable : variablesInScope) {
                    Set<InitializationPoints> edgesDataForVariable = Sets.newHashSet();
                    for (Map<VariableDescriptor, InitializationPoints> edgePointsMap : incomingEdgesData) {
                        InitializationPoints points = edgePointsMap.get(variable);
                        if (points != null) {
                            edgesDataForVariable.add(points);
                        }
                    }
                    enterInstructionPointsMap.put(variable, new InitializationPoints(edgesDataForVariable));
                }
                
                Map<VariableDescriptor, InitializationPoints> exitInstructionPointsMap = Maps.newHashMap(enterInstructionPointsMap);
                if (instruction instanceof WriteValueInstruction) {
                    VariableDescriptor variable = extractVariableDescriptorIfAny(instruction);
                    InitializationPoints initializationAtThisPoint = new InitializationPoints(((WriteValueInstruction) instruction).getElement());
                    exitInstructionPointsMap.put(variable, initializationAtThisPoint);
                }
                
                return Pair.create(enterInstructionPointsMap, exitInstructionPointsMap);
            }
        };

        Collection<VariableDescriptor> usedVariables = collectUsedVariables(pseudocode);
        Collection<VariableDescriptor> declaredVariables = collectDeclaredVariables(subroutine);
        Map<VariableDescriptor, InitializationPoints> initialMapForStartInstruction = prepareInitialMapForStartInstruction(usedVariables, declaredVariables);
        
        traverser.collectInformationFromInstructionGraph(instructionsMergeStrategy,
                                                         Collections.<VariableDescriptor, InitializationPoints>emptyMap(),
                                                         initialMapForStartInstruction,
                                                         true);

        traverser.traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Map<VariableDescriptor, InitializationPoints>>() {
            @Override
            public void execute(Instruction instruction, @Nullable Map<VariableDescriptor, InitializationPoints> enterData, @Nullable Map<VariableDescriptor, InitializationPoints> exitData) {
                assert enterData != null && exitData != null;
                if (instruction instanceof ReadValueInstruction) {
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                    JetElement element = ((ReadValueInstruction) instruction).getElement();
                    if (element instanceof JetSimpleNameExpression && variableDescriptor != null &&
                        (inAnonymousInitializers || variableDescriptor instanceof LocalVariableDescriptor)) {

                        InitializationPoints exitInitializationPoints = exitData.get(variableDescriptor);
                        assert exitInitializationPoints != null;

                        boolean isInitialized = exitInitializationPoints.isInitialized();
                        if (variableDescriptor instanceof PropertyDescriptor) {
                            if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor)) {
                                isInitialized = true;
                            }
                        }
                        if (!declaredLocally && !isInitialized) {
                            trace.report(Errors.UNINITIALIZED_VARIABLE.on((JetSimpleNameExpression) element, variableDescriptor));
                        }
                    }
                }
                else if (instruction instanceof WriteValueInstruction) {
                    JetElement element = ((WriteValueInstruction) instruction).getlValue();
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                    if (element instanceof JetSimpleNameExpression && variableDescriptor != null) {
                        InitializationPoints enterInitializationPoints = enterData.get(variableDescriptor);
                        assert enterInitializationPoints != null;
                        InitializationPoints exitInitializationPoints = exitData.get(variableDescriptor);
                        assert exitInitializationPoints != null;
                        Set<JetElement> possiblePoints = enterInitializationPoints.getPossiblePoints();
                        boolean hasInitializer = !possiblePoints.isEmpty() || enterInitializationPoints.isInitialized();
                        if (possiblePoints.size() == 1) {
                            JetElement initializer = possiblePoints.iterator().next();
                            if (initializer == element.getParent()) {
                                hasInitializer = false;
                            }
                        }
                        JetSimpleNameExpression expression = (JetSimpleNameExpression) element;
                        if (!declaredLocally && hasInitializer && !variableDescriptor.isVar()) {
                            PsiElement psiElement = trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, variableDescriptor);
                            JetProperty property = psiElement instanceof JetProperty ? (JetProperty) psiElement : null;
                            trace.report(Errors.VAL_REASSIGNMENT.on(expression, variableDescriptor, property == null ? new JetProperty[0] : new JetProperty[] { property }));
                        }
                        if (!declaredLocally && inAnonymousInitializers && variableDescriptor instanceof PropertyDescriptor &&
                            !enterInitializationPoints.isInitialized() && exitInitializationPoints.isInitialized()) {
                            if (expression.getReferencedNameElementType() != JetTokens.FIELD_IDENTIFIER) {
                                trace.report(Errors.INITIALIZATION_USING_BACKING_FIELD.on(expression, variableDescriptor));
                            }
                        }
                    }
                }
            }
        });

        Map<VariableDescriptor, InitializationPoints> lastInfo = traverser.getResultInfo();
        for (Map.Entry<VariableDescriptor, InitializationPoints> entry : lastInfo.entrySet()) {
            VariableDescriptor variable = entry.getKey();
            if (variable instanceof PropertyDescriptor && declaredVariables.contains(variable)) {
                InitializationPoints initializationPoints = entry.getValue();
                trace.record(BindingContext.IS_INITIALIZED, (PropertyDescriptor) variable, initializationPoints.isInitialized());
            }
        }
    }

    private Map<VariableDescriptor, InitializationPoints> prepareInitialMapForStartInstruction(Collection<VariableDescriptor> usedVariables, Collection<VariableDescriptor> declaredVariables) {
        Map<VariableDescriptor, InitializationPoints> initialMapForStartInstruction = Maps.newHashMap();
        InitializationPoints initialPointsForDeclaredVariable = new InitializationPoints(false);
        InitializationPoints initialPointsForExternalVariable = new InitializationPoints(true);

        for (VariableDescriptor variable : usedVariables) {
            if (declaredVariables.contains(variable)) {
                initialMapForStartInstruction.put(variable, initialPointsForDeclaredVariable);
            }
            else {
                initialMapForStartInstruction.put(variable, initialPointsForExternalVariable);
            }
        }
        return initialMapForStartInstruction;
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

        JetControlFlowGraphTraverser.<Void>create(pseudocode, true).traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Void>() {
            @Override
            public void execute(Instruction instruction, Void enterData, Void exitData) {
                if (instruction instanceof ReadValueInstruction) {
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                    if (variableDescriptor != null && functionVariables.contains(variableDescriptor)) {
                        //check that we only invoke this variable
                        JetElement element = ((ReadValueInstruction) instruction).getElement();
                        if (element instanceof JetSimpleNameExpression && !(element.getParent() instanceof JetCallExpression)) {
                            trace.report(Errors.FUNCTION_PARAMETERS_OF_INLINE_FUNCTION.on((JetSimpleNameExpression) element, variableDescriptor));
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

    private Collection<VariableDescriptor> collectUsedVariables(Pseudocode pseudocode) {
        final Set<VariableDescriptor> usedVariables = Sets.newHashSet();
        JetControlFlowGraphTraverser.<Void>create(pseudocode, true).traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Void>() {
            @Override
            public void execute(Instruction instruction, Void enterData, Void exitData) {
                VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction);
                if (variableDescriptor != null) {
                    usedVariables.add(variableDescriptor);
                }
            }
        });
        return usedVariables;
    }

    private Collection<VariableDescriptor> collectDeclaredVariables(JetElement element) {
        final Set<VariableDescriptor> declaredVariables = Sets.newHashSet();
        element.accept(new JetTreeVisitor<Void>() {
            @Override
            public Void visitProperty(JetProperty property, Void data) {
                DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
                if (descriptor != null) {
                    assert descriptor instanceof VariableDescriptor;
                    declaredVariables.add((VariableDescriptor) descriptor);
                }
                return super.visitProperty(property, data);
            }

            @Override
            public Void visitForExpression(JetForExpression expression, Void data) {
                JetParameter loopParameter = expression.getLoopParameter();
                if (loopParameter != null) {
                    DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, loopParameter);
                    if (descriptor != null) {
                        assert descriptor instanceof VariableDescriptor;
                        declaredVariables.add((VariableDescriptor) descriptor);
                    }
                }
                return super.visitForExpression(expression, data);
            }
        }, null);
        return declaredVariables;
    }

    private static class InitializationPoints {
        private Set<JetElement> possiblePoints = Sets.newHashSet();
        private boolean isInitialized;

        public InitializationPoints(boolean isInitialized) {
            this.isInitialized = isInitialized;
        }

        public InitializationPoints(JetElement element) {
            isInitialized = true;
            possiblePoints.add(element);
        }
        
        public InitializationPoints(Set<InitializationPoints> edgesData) {
            isInitialized = true;
            for (InitializationPoints edgeData : edgesData) {
                if (!edgeData.isInitialized) {
                    isInitialized = false;
                }
                possiblePoints.addAll(edgeData.possiblePoints);
            }
        }

        public Set<JetElement> getPossiblePoints() {
            return possiblePoints;
        }

        public boolean isInitialized() {
            return isInitialized;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InitializationPoints)) return false;

            InitializationPoints that = (InitializationPoints) o;

            if (isInitialized != that.isInitialized) return false;
            if (possiblePoints != null ? !possiblePoints.equals(that.possiblePoints) : that.possiblePoints != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = possiblePoints != null ? possiblePoints.hashCode() : 0;
            result = 31 * result + (isInitialized ? 1 : 0);
            return result;
        }
    }
}