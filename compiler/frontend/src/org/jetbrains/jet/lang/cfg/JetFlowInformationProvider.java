package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

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
        new JetControlFlowProcessor(trace, instructionsGenerator).generate(declaration);
        wrappedTrace.close();
    }

    private void collectReturnExpressions(@NotNull JetElement subroutine, @NotNull final Collection<JetExpression> returnedExpressions) {
        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        final Set<Instruction> instructions = Sets.newHashSet(pseudocode.getInstructions());
        SubroutineExitInstruction exitInstruction = pseudocode.getExitInstruction();
        for (Instruction previousInstruction : exitInstruction.getPreviousInstructions()) {
            previousInstruction.accept(new InstructionVisitor() {
                @Override
                public void visitReturnValue(ReturnValueInstruction instruction) {
                    if (instructions.contains(instruction)) { //exclude non-local return expressions
                        returnedExpressions.add((JetExpression) instruction.getElement());
                    }
                }

                @Override
                public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add((JetExpression) instruction.getElement());
                    }
                }


                @Override
                public void visitJump(AbstractJumpInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
                    redirectToPrevInstructions(instruction);
                }

                private void redirectToPrevInstructions(Instruction instruction) {
                    for (Instruction previousInstruction : instruction.getPreviousInstructions()) {
                        previousInstruction.accept(this);
                    }
                }

                @Override
                public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
                    redirectToPrevInstructions(instruction);
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
    
    public void checkDefiniteReturn(@NotNull JetDeclarationWithBody function, final @NotNull JetType expectedReturnType) {
        assert function instanceof JetDeclaration;

        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;
        
        List<JetExpression> returnedExpressions = Lists.newArrayList();
        collectReturnExpressions(function.asElement(), returnedExpressions);

        boolean nothingReturned = returnedExpressions.isEmpty();

        returnedExpressions.remove(function); // This will be the only "expression" if the body is empty

        if (expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && returnedExpressions.isEmpty() && !nothingReturned) {
            trace.report(RETURN_TYPE_MISMATCH.on(bodyExpression, expectedReturnType));
        }
        final boolean blockBody = function.hasBlockBody();
        
        final Set<JetElement> rootUnreachableElements = collectUnreachableCode(function.asElement());
        for (JetElement element : rootUnreachableElements) {
            trace.report(UNREACHABLE_CODE.on(element));
        }

        for (JetExpression returnedExpression : returnedExpressions) {
            returnedExpression.accept(new JetVisitorVoid() {
                @Override
                public void visitReturnExpression(JetReturnExpression expression) {
                    if (!blockBody) {
                        trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression));
                    }
                }

                @Override
                public void visitExpression(JetExpression expression) {
                    if (blockBody && expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && !rootUnreachableElements.contains(expression)) {
                        trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(expression));
                    }
                }
            });
        }
    }

    private Set<JetElement> collectUnreachableCode(@NotNull JetElement subroutine) {
        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        Collection<JetElement> unreachableElements = Lists.newArrayList();
        for (Instruction deadInstruction : pseudocode.getDeadInstructions()) {
            if (deadInstruction instanceof JetElementInstruction &&
                !(deadInstruction instanceof ReadUnitValueInstruction)) {
                unreachableElements.add(((JetElementInstruction) deadInstruction).getElement());
            }
        }
        // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
        return JetPsiUtil.findRootExpressions(unreachableElements);
    }

////////////////////////////////////////////////////////////////////////////////
//  Uninitialized variables analysis

    public void markUninitializedVariables(@NotNull JetElement subroutine, final boolean processLocalDeclaration) {
        final Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        JetControlFlowGraphTraverser<Map<VariableDescriptor, VariableInitializers>> traverser = JetControlFlowGraphTraverser.create(pseudocode, false, true);

        Collection<VariableDescriptor> usedVariables = collectUsedVariables(pseudocode);
        final Collection<VariableDescriptor> declaredVariables = collectDeclaredVariables(subroutine);
        Map<VariableDescriptor, VariableInitializers> initialMapForStartInstruction = prepareInitialMapForStartInstruction(usedVariables, declaredVariables);

        JetControlFlowGraphTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableInitializers>> variableInitializersMergeStrategy =
                new JetControlFlowGraphTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableInitializers>>() {
            @Override
            public Pair<Map<VariableDescriptor, VariableInitializers>, Map<VariableDescriptor, VariableInitializers>> execute(
                    Instruction instruction,
                    @NotNull Collection<Map<VariableDescriptor, VariableInitializers>> incomingEdgesData) {

                Map<VariableDescriptor, VariableInitializers> enterInstructionData = mergeIncomingEdgesData(incomingEdgesData);
                Map<VariableDescriptor, VariableInitializers> exitInstructionData = addVariableInitializerFromCurrentInstructionIfAny(instruction, enterInstructionData);
                return Pair.create(enterInstructionData, exitInstructionData);
            }
        };

        traverser.collectInformationFromInstructionGraph(variableInitializersMergeStrategy, Collections.<VariableDescriptor, VariableInitializers>emptyMap(), initialMapForStartInstruction);

        final Collection<VariableDescriptor> varWithUninitializedErrorGenerated = Sets.newHashSet();
        final Collection<VariableDescriptor> varWithValReassignErrorGenerated = Sets.newHashSet();
        final boolean processClassOrObject = subroutine instanceof JetClassOrObject;
        traverser.traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableInitializers>>() {
            @Override
            public void execute(Instruction instruction, @Nullable Map<VariableDescriptor, VariableInitializers> enterData, @Nullable Map<VariableDescriptor, VariableInitializers> exitData) {
                assert enterData != null && exitData != null;
                VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction, true);
                if (variableDescriptor == null) return;
                if (instruction instanceof ReadValueInstruction) {
                    JetElement element = ((ReadValueInstruction) instruction).getElement();
                    boolean error = checkBackingField(variableDescriptor, element);
                    if (!error && declaredVariables.contains(variableDescriptor)) {
                        checkIsInitialized(variableDescriptor, element, exitData.get(variableDescriptor), varWithUninitializedErrorGenerated);
                    }
                }
                else if (instruction instanceof WriteValueInstruction) {
                    JetElement element = ((WriteValueInstruction) instruction).getlValue();
                    boolean error = checkBackingField(variableDescriptor, element);
                    if (!(element instanceof JetExpression)) return;
                    if (!error && !processLocalDeclaration) { // error has been generated before while processing outer function of this local declaration
                        error = checkValReassignment(variableDescriptor, (JetExpression) element, enterData.get(variableDescriptor), varWithValReassignErrorGenerated);
                    }
                    if (!error && processClassOrObject) {
                        checkInitializationUsingBackingField(variableDescriptor, (JetExpression) element, enterData.get(variableDescriptor), exitData.get(variableDescriptor));
                    }
                }
            }
        });

        recordInitializedVariables(declaredVariables, traverser.getResultInfo());
        analyzeLocalDeclarations(processLocalDeclaration, pseudocode);
    }

    private void checkIsInitialized(@NotNull VariableDescriptor variableDescriptor, @NotNull JetElement element, @NotNull VariableInitializers variableInitializers, @NotNull Collection<VariableDescriptor> varWithUninitializedErrorGenerated) {
        if (!(element instanceof JetSimpleNameExpression)) return;

        boolean isInitialized = variableInitializers.isInitialized();
        if (variableDescriptor instanceof PropertyDescriptor) {
            if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor)) {
                isInitialized = true;
            }
        }
        if (!isInitialized && !varWithUninitializedErrorGenerated.contains(variableDescriptor)) {
            varWithUninitializedErrorGenerated.add(variableDescriptor);
            if (variableDescriptor instanceof ValueParameterDescriptor) {
                trace.report(Errors.UNINITIALIZED_PARAMETER.on((JetSimpleNameExpression) element, (ValueParameterDescriptor) variableDescriptor));
            }
            else {
                trace.report(Errors.UNINITIALIZED_VARIABLE.on((JetSimpleNameExpression) element, variableDescriptor));
            }
        }
    }

    private boolean checkValReassignment(@NotNull VariableDescriptor variableDescriptor, @NotNull JetExpression expression, @NotNull VariableInitializers enterInitializers, @NotNull Collection<VariableDescriptor> varWithValReassignErrorGenerated) {
        boolean isInitializedNotHere = enterInitializers.isInitialized();
        Set<JetElement> possibleLocalInitializers = enterInitializers.getPossibleLocalInitializers();
        if (possibleLocalInitializers.size() == 1) {
            JetElement initializer = possibleLocalInitializers.iterator().next();
            if (initializer instanceof JetProperty && initializer == expression.getParent()) {
                isInitializedNotHere = false;
            }
        }
        boolean hasBackingField = true;
        if (variableDescriptor instanceof PropertyDescriptor) {
            hasBackingField = trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor);
        }
        if ((isInitializedNotHere || !hasBackingField) && !variableDescriptor.isVar() && !varWithValReassignErrorGenerated.contains(variableDescriptor)) {
            varWithValReassignErrorGenerated.add(variableDescriptor);

            boolean hasReassignMethodReturningUnit = false;
            JetSimpleNameExpression operationReference = null;
            PsiElement parent = expression.getParent();
            if (parent instanceof JetBinaryExpression) {
                operationReference = ((JetBinaryExpression) parent).getOperationReference();
            }
            else if (parent instanceof JetUnaryExpression) {
                operationReference = ((JetUnaryExpression) parent).getOperationReference();
            }
            if (operationReference != null) {
                DeclarationDescriptor descriptor = trace.get(BindingContext.REFERENCE_TARGET, operationReference);
                if (descriptor instanceof FunctionDescriptor) {
                    if (JetStandardClasses.isUnit(((FunctionDescriptor) descriptor).getReturnType())) {
                        hasReassignMethodReturningUnit = true;
                    }
                }
            }
            if (!hasReassignMethodReturningUnit) {
                trace.report(Errors.VAL_REASSIGNMENT.on(expression, variableDescriptor));
                return true;
            }
        }
        return false;
    }
    
    private void checkInitializationUsingBackingField(@NotNull VariableDescriptor variableDescriptor, @NotNull JetExpression expression, @NotNull VariableInitializers enterInitializers, @NotNull VariableInitializers exitInitializers) {
        if (variableDescriptor instanceof PropertyDescriptor && !enterInitializers.isInitialized() && exitInitializers.isInitialized()) {
            if (!variableDescriptor.isVar()) return;
            if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor)) return;
            PsiElement property = trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, variableDescriptor);
            assert property instanceof JetProperty;
            if (((PropertyDescriptor) variableDescriptor).getModality() == Modality.FINAL && ((JetProperty) property).getSetter() == null) return;
            JetExpression variable = expression;
            if (expression instanceof JetDotQualifiedExpression) {
                if (((JetDotQualifiedExpression) expression).getReceiverExpression() instanceof JetThisExpression) {
                    variable = ((JetDotQualifiedExpression) expression).getSelectorExpression();
                }
            }
            if (variable instanceof JetSimpleNameExpression) {
                JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) variable;
                if (simpleNameExpression.getReferencedNameElementType() != JetTokens.FIELD_IDENTIFIER) {
                    if (((PropertyDescriptor) variableDescriptor).getModality() != Modality.FINAL) {
                        trace.report(Errors.INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER.on(simpleNameExpression, expression, variableDescriptor));
                    }
                    else {
                        trace.report(Errors.INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER.on(simpleNameExpression, expression, variableDescriptor));
                    }
                }
            }
        }
    }

    private boolean checkBackingField(@NotNull VariableDescriptor variableDescriptor, @NotNull JetElement element) {
        boolean[] error = new boolean[1];
        if (isBackingFieldReference((JetElement) element.getParent(), error, false)) return false; // this expression has been already checked
        if (!isBackingFieldReference(element, error, true)) return false;
        if (error[0]) return true;
        if (!(variableDescriptor instanceof PropertyDescriptor)) {
            trace.report(Errors.NOT_PROPERTY_BACKING_FIELD.on(element));
            return true;
        }
        PsiElement property = trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, variableDescriptor);
        boolean insideSelfAccessors = PsiTreeUtil.isAncestor(property, element, false);
        if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor) && !insideSelfAccessors) { // not to generate error in accessors of abstract properties, there is one: declared accessor of abstract property
            if (((PropertyDescriptor) variableDescriptor).getModality() == Modality.ABSTRACT) {
                trace.report(NO_BACKING_FIELD_ABSTRACT_PROPERTY.on(element));
            }
            else {
                trace.report(NO_BACKING_FIELD_CUSTOM_ACCESSORS.on(element));
            }
            return true;
        }
        if (insideSelfAccessors) return false;

        JetNamedDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(element, JetNamedDeclaration.class);
        DeclarationDescriptor declarationDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, parentDeclaration);
        assert declarationDescriptor != null;
        DeclarationDescriptor containingDeclaration = variableDescriptor.getContainingDeclaration();
        if ((containingDeclaration instanceof ClassDescriptor) && DescriptorUtils.isAncestor(containingDeclaration, declarationDescriptor, false)) {
            return false;
        }
        trace.report(Errors.INACCESSIBLE_BACKING_FIELD.on(element));
        return true;
    }

    private boolean isBackingFieldReference(@Nullable JetElement element, boolean[] error, boolean reportError) {
        error[0] = false;
        if (element instanceof JetSimpleNameExpression && ((JetSimpleNameExpression) element).getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
            return true;
        }
        if (element instanceof JetDotQualifiedExpression && isBackingFieldReference(((JetDotQualifiedExpression) element).getSelectorExpression(), error, false)) {
            if (((JetDotQualifiedExpression) element).getReceiverExpression() instanceof JetThisExpression) {
                return true;
            }
            error[0] = true;
            if (reportError) {
                trace.report(Errors.INACCESSIBLE_BACKING_FIELD.on(element));
            }
        }
        return false;
    }

    private void recordInitializedVariables(Collection<VariableDescriptor> declaredVariables, Map<VariableDescriptor, VariableInitializers> resultInfo) {
        for (Map.Entry<VariableDescriptor, VariableInitializers> entry : resultInfo.entrySet()) {
            VariableDescriptor variable = entry.getKey();
            if (variable instanceof PropertyDescriptor && declaredVariables.contains(variable)) {
                VariableInitializers initializers = entry.getValue();
                trace.record(BindingContext.IS_INITIALIZED, (PropertyDescriptor) variable, initializers.isInitialized());
            }
        }
    }

    private void analyzeLocalDeclarations(boolean processLocalDeclaration, Pseudocode pseudocode) {
        for (Instruction instruction : pseudocode.getInstructions()) {
            if (instruction instanceof LocalDeclarationInstruction) {
                JetElement element = ((LocalDeclarationInstruction) instruction).getElement();
                markUninitializedVariables(element, processLocalDeclaration);
            }
        }
    }

    private Map<VariableDescriptor, VariableInitializers> addVariableInitializerFromCurrentInstructionIfAny(Instruction instruction, Map<VariableDescriptor, VariableInitializers> enterInstructionData) {
        Map<VariableDescriptor, VariableInitializers> exitInstructionData = Maps.newHashMap(enterInstructionData);
        if (instruction instanceof WriteValueInstruction) {
            VariableDescriptor variable = extractVariableDescriptorIfAny(instruction, false);
            VariableInitializers initializationAtThisElement = new VariableInitializers(((WriteValueInstruction) instruction).getElement());
            exitInstructionData.put(variable, initializationAtThisElement);
        }
        return exitInstructionData;
    }

    private Map<VariableDescriptor, VariableInitializers> mergeIncomingEdgesData(Collection<Map<VariableDescriptor, VariableInitializers>> incomingEdgesData) {
        Set<VariableDescriptor> variablesInScope = Sets.newHashSet();
        for (Map<VariableDescriptor, VariableInitializers> edgeData : incomingEdgesData) {
            variablesInScope.addAll(edgeData.keySet());
        }

        Map<VariableDescriptor, VariableInitializers> enterInstructionData = Maps.newHashMap();
        for (VariableDescriptor variable : variablesInScope) {
            Set<VariableInitializers> edgesDataForVariable = Sets.newHashSet();
            for (Map<VariableDescriptor, VariableInitializers> edgeData : incomingEdgesData) {
                VariableInitializers initializers = edgeData.get(variable);
                if (initializers != null) {
                    edgesDataForVariable.add(initializers);
                }
            }
            enterInstructionData.put(variable, new VariableInitializers(edgesDataForVariable));
        }
        return enterInstructionData;
    }

    private Map<VariableDescriptor, VariableInitializers> prepareInitialMapForStartInstruction(Collection<VariableDescriptor> usedVariables, Collection<VariableDescriptor> declaredVariables) {
        Map<VariableDescriptor, VariableInitializers> initialMapForStartInstruction = Maps.newHashMap();
        VariableInitializers    isInitializedForExternalVariable = new VariableInitializers(true);
        VariableInitializers isNotInitializedForDeclaredVariable = new VariableInitializers(false);

        for (VariableDescriptor variable : usedVariables) {
            if (declaredVariables.contains(variable)) {
                initialMapForStartInstruction.put(variable, isNotInitializedForDeclaredVariable);
            }
            else {
                initialMapForStartInstruction.put(variable, isInitializedForExternalVariable);
            }
        }
        return initialMapForStartInstruction;
    }

////////////////////////////////////////////////////////////////////////////////

    public void markNotOnlyInvokedFunctionVariables(@NotNull JetElement subroutine, List<? extends VariableDescriptor> variables) {
        final List<VariableDescriptor> functionVariables = Lists.newArrayList();
        for (VariableDescriptor variable : variables) {
            if (JetStandardClasses.isFunctionType(variable.getReturnType())) {
                functionVariables.add(variable);
            }
        }

        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;

        JetControlFlowGraphTraverser.<Void>create(pseudocode, true, true).traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Void>() {
            @Override
            public void execute(Instruction instruction, Void enterData, Void exitData) {
                if (instruction instanceof ReadValueInstruction) {
                    VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction, false);
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

////////////////////////////////////////////////////////////////////////////////
//  "Unused variable" & "unused value" analyses

    public void markUnusedVariables(@NotNull JetElement subroutine) {
        Pseudocode pseudocode = pseudocodeMap.get(subroutine);
        assert pseudocode != null;
        JetControlFlowGraphTraverser<Map<VariableDescriptor, VariableStatus>> traverser = JetControlFlowGraphTraverser.create(pseudocode, true, false);
        final Collection<VariableDescriptor> declaredVariables = collectDeclaredVariables(subroutine);        
        Map<VariableDescriptor, VariableStatus> sinkInstructionData = Maps.newHashMap();
        traverser.collectInformationFromInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableStatus>>() {
            @Override
            public Pair<Map<VariableDescriptor, VariableStatus>, Map<VariableDescriptor, VariableStatus>> execute(Instruction instruction, @NotNull Collection<Map<VariableDescriptor, VariableStatus>> incomingEdgesData) {
                Map<VariableDescriptor, VariableStatus> enterResult = Maps.newHashMap();
                for (Map<VariableDescriptor, VariableStatus> edgeData : incomingEdgesData) {
                    for (Map.Entry<VariableDescriptor, VariableStatus> entry : edgeData.entrySet()) {
                        VariableDescriptor variableDescriptor = entry.getKey();
                        VariableStatus variableStatus = entry.getValue();
                        enterResult.put(variableDescriptor, variableStatus.merge(enterResult.get(variableDescriptor)));
                    }
                }
                Map<VariableDescriptor, VariableStatus> exitResult = Maps.newHashMap(enterResult);
                VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction, true);
                if (variableDescriptor != null) {
                    if (instruction instanceof ReadValueInstruction) {
                        exitResult.put(variableDescriptor, VariableStatus.READ);
                    }
                    else if (instruction instanceof WriteValueInstruction) {
                        VariableStatus variableStatus = enterResult.get(variableDescriptor);
                        if (variableStatus == null) variableStatus = VariableStatus.UNUSED;
                        switch(variableStatus) {
                            case UNUSED:
                            case ONLY_WRITTEN:
                                exitResult.put(variableDescriptor, VariableStatus.ONLY_WRITTEN);
                                break;
                            case WRITTEN:
                            case READ:
                                exitResult.put(variableDescriptor, VariableStatus.WRITTEN);
                        }
                    }
                }
                return new Pair<Map<VariableDescriptor, VariableStatus>, Map<VariableDescriptor, VariableStatus>>(enterResult, exitResult);
            }
        }, Collections.<VariableDescriptor, VariableStatus>emptyMap(), sinkInstructionData);
        traverser.traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableStatus>>() {
            @Override
            public void execute(Instruction instruction, @Nullable Map<VariableDescriptor, VariableStatus> enterData, @Nullable Map<VariableDescriptor, VariableStatus> exitData) {
                assert enterData != null && exitData != null;
                VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction, false);
                if (variableDescriptor == null || !declaredVariables.contains(variableDescriptor) ||
                    !DescriptorUtils.isLocal(variableDescriptor.getContainingDeclaration(), variableDescriptor)) return;
                VariableStatus variableStatus = enterData.get(variableDescriptor);
                if (instruction instanceof WriteValueInstruction) {
                    JetElement element = ((WriteValueInstruction) instruction).getElement();
                    if (variableStatus != VariableStatus.READ) {
                        if (element instanceof JetBinaryExpression && ((JetBinaryExpression) element).getOperationToken() == JetTokens.EQ) {
                            JetExpression right = ((JetBinaryExpression) element).getRight();
                            if (right != null) {
                                trace.report(Errors.UNUSED_VALUE.on(right, right, variableDescriptor));
                            }
                        }
                        else if (element instanceof JetPostfixExpression) {
                            IElementType operationToken = ((JetPostfixExpression) element).getOperationReference().getReferencedNameElementType();
                            if (operationToken == JetTokens.PLUSPLUS || operationToken == JetTokens.MINUSMINUS) {
                                trace.report(Errors.UNUSED_CHANGED_VALUE.on(element, element));
                            }
                        }
                    }
                }
                else if (instruction instanceof VariableDeclarationInstruction) {
                    JetDeclaration element = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
                    if (element instanceof JetNamedDeclaration) {
                        PsiElement nameIdentifier = ((JetNamedDeclaration) element).getNameIdentifier();
                        PsiElement elementToMark = nameIdentifier != null ? nameIdentifier : element;
                        if (variableStatus == null || variableStatus == VariableStatus.UNUSED) {
                            if (element instanceof JetProperty) {
                                trace.report(Errors.UNUSED_VARIABLE.on((JetProperty) element, elementToMark, variableDescriptor));
                            }
                            else if (element instanceof JetParameter) {
                                PsiElement psiElement = element.getParent().getParent();
                                if (psiElement instanceof JetFunction) {
                                    boolean isMain = (psiElement instanceof JetNamedFunction) && JetMainDetector.isMain((JetNamedFunction) psiElement);
                                    DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiElement);
                                    assert descriptor instanceof FunctionDescriptor;
                                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                                    if (!isMain && !functionDescriptor.getModality().isOverridable() && functionDescriptor.getOverriddenDescriptors().isEmpty()) {
                                        trace.report(Errors.UNUSED_PARAMETER.on((JetParameter) element, elementToMark, variableDescriptor));
                                    }
                                }
                            }
                        }
                        else if (variableStatus == VariableStatus.ONLY_WRITTEN && element instanceof JetProperty) {
                            trace.report(Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.on((JetNamedDeclaration) element, elementToMark, variableDescriptor));
                        }
                        else if (variableStatus == VariableStatus.WRITTEN && element instanceof JetProperty) {
                            JetExpression initializer = ((JetProperty) element).getInitializer();
                            if (initializer != null) {
                                trace.report(Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER.on((JetNamedDeclaration) element, initializer, variableDescriptor));
                            }
                        }
                    }

                }
            }
        });
    }

    private static enum VariableStatus { 
        READ(3),
        WRITTEN(2),
        ONLY_WRITTEN(1),
        UNUSED(0);
        
        private int importance;

        private VariableStatus(int importance) {
            this.importance = importance;
        }

        public VariableStatus merge(@Nullable VariableStatus variableStatus) {
            if (variableStatus == null || importance > variableStatus.importance) return this;
            return variableStatus;
        }
    }

////////////////////////////////////////////////////////////////////////////////
//  Util methods

    @Nullable
    private VariableDescriptor extractVariableDescriptorIfAny(Instruction instruction, boolean onlyReference) {
        JetElement element = null;
        if (instruction instanceof ReadValueInstruction) {
            element = ((ReadValueInstruction) instruction).getElement();
        }
        else if (instruction instanceof WriteValueInstruction) {
            element = ((WriteValueInstruction) instruction).getlValue();
        }
        else if (instruction instanceof VariableDeclarationInstruction) {
            element = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
        }
        return BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), element, onlyReference);
    }

    private Collection<VariableDescriptor> collectUsedVariables(Pseudocode pseudocode) {
        final Set<VariableDescriptor> usedVariables = Sets.newHashSet();
        JetControlFlowGraphTraverser.<Void>create(pseudocode, true, true).traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Void>() {
            @Override
            public void execute(Instruction instruction, Void enterData, Void exitData) {
                VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction, false);
                if (variableDescriptor != null) {
                    usedVariables.add(variableDescriptor);
                }
            }
        });
        return usedVariables;
    }

    private Collection<VariableDescriptor> collectDeclaredVariables(JetElement element) {
        final Pseudocode pseudocode = pseudocodeMap.get(element);
        assert pseudocode != null;
        
        final Set<VariableDescriptor> declaredVariables = Sets.newHashSet();
        JetControlFlowGraphTraverser.<Void>create(pseudocode, false, true).traverseAndAnalyzeInstructionGraph(new JetControlFlowGraphTraverser.InstructionDataAnalyzeStrategy<Void>() {
            @Override
            public void execute(Instruction instruction, @Nullable Void enterData, @Nullable Void exitData) {
                if (instruction instanceof VariableDeclarationInstruction) {
                    JetDeclaration variableDeclarationElement = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
                    DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement);
                    if (descriptor != null) {
                        assert descriptor instanceof VariableDescriptor;
                        declaredVariables.add((VariableDescriptor) descriptor);
                    }
                }
            }
        });
        return declaredVariables;
    }

////////////////////////////////////////////////////////////////////////////////
//  Local class for uninitialized variables analysis

    private static class VariableInitializers {
        private Set<JetElement> possibleLocalInitializers = Sets.newHashSet();
        private boolean isInitialized;

        public VariableInitializers(boolean isInitialized) {
            this.isInitialized = isInitialized;
        }

        public VariableInitializers(JetElement element) {
            isInitialized = true;
            possibleLocalInitializers.add(element);
        }
        
        public VariableInitializers(Set<VariableInitializers> edgesData) {
            isInitialized = true;
            for (VariableInitializers edgeData : edgesData) {
                if (!edgeData.isInitialized) {
                    isInitialized = false;
                }
                possibleLocalInitializers.addAll(edgeData.possibleLocalInitializers);
            }
        }

        public Set<JetElement> getPossibleLocalInitializers() {
            return possibleLocalInitializers;
        }

        public boolean isInitialized() {
            return isInitialized;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VariableInitializers)) return false;

            VariableInitializers that = (VariableInitializers) o;

            if (isInitialized != that.isInitialized) return false;
            if (possibleLocalInitializers != null ? !possibleLocalInitializers.equals(that.possibleLocalInitializers) : that.possibleLocalInitializers != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = possibleLocalInitializers != null ? possibleLocalInitializers.hashCode() : 0;
            result = 31 * result + (isInitialized ? 1 : 0);
            return result;
        }
    }
}