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

package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.cfg.PseudocodeTraverser.*;
import org.jetbrains.jet.lang.cfg.PseudocodeVariablesData.VariableInitState;
import org.jetbrains.jet.lang.cfg.PseudocodeVariablesData.VariableUseState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.util.*;

import static org.jetbrains.jet.lang.cfg.PseudocodeVariablesData.VariableUseState.*;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.CAPTURED_IN_CLOSURE;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author svtk
 */
public class JetFlowInformationProvider {

    private final JetDeclaration subroutine;
    private final Pseudocode pseudocode;
    private final PseudocodeVariablesData pseudocodeVariablesData;
    private BindingTrace trace;

    public JetFlowInformationProvider(
            @NotNull JetDeclaration declaration,
            @NotNull BindingTrace trace) {

        subroutine = declaration;
        this.trace = trace;
        pseudocode = new JetControlFlowProcessor(trace).generatePseudocode(declaration);
        pseudocodeVariablesData = new PseudocodeVariablesData(pseudocode, trace.getBindingContext());
    }

    private void collectReturnExpressions(@NotNull final Collection<JetElement> returnedExpressions) {
        final Set<Instruction> instructions = Sets.newHashSet(pseudocode.getInstructions());
        SubroutineExitInstruction exitInstruction = pseudocode.getExitInstruction();
        for (Instruction previousInstruction : exitInstruction.getPreviousInstructions()) {
            previousInstruction.accept(new InstructionVisitor() {
                @Override
                public void visitReturnValue(ReturnValueInstruction instruction) {
                    if (instructions.contains(instruction)) { //exclude non-local return expressions
                        returnedExpressions.add(instruction.getElement());
                    }
                }

                @Override
                public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.getElement());
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
                        returnedExpressions.add(elementInstruction.getElement());
                    }
                    else {
                        throw new IllegalStateException(instruction + " precedes the exit point");
                    }
                }
            });
        }
    }

    public void checkDefiniteReturn(final @NotNull JetType expectedReturnType) {
        assert subroutine instanceof JetDeclarationWithBody;
        JetDeclarationWithBody function = (JetDeclarationWithBody) subroutine;

        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;

        List<JetElement> returnedExpressions = Lists.newArrayList();
        collectReturnExpressions(returnedExpressions);

        boolean nothingReturned = returnedExpressions.isEmpty();

        returnedExpressions.remove(function); // This will be the only "expression" if the body is empty

        if (expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && returnedExpressions.isEmpty() && !nothingReturned) {
            trace.report(RETURN_TYPE_MISMATCH.on(bodyExpression, expectedReturnType));
        }
        final boolean blockBody = function.hasBlockBody();

        final Set<JetElement> rootUnreachableElements = collectUnreachableCode();
        for (JetElement element : rootUnreachableElements) {
            trace.report(UNREACHABLE_CODE.on(element));
        }

        final boolean[] noReturnError = new boolean[] { false };
        for (JetElement returnedExpression : returnedExpressions) {
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
                        noReturnError[0] = true;
                    }
                }
            });
        }
        if (noReturnError[0]) {
            trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(function));
        }
    }

    private Set<JetElement> collectUnreachableCode() {
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

    public void markUninitializedVariables(final boolean processLocalDeclaration) {
        final Collection<VariableDescriptor> varWithUninitializedErrorGenerated = Sets.newHashSet();
        final Collection<VariableDescriptor> varWithValReassignErrorGenerated = Sets.newHashSet();
        final boolean processClassOrObject = subroutine instanceof JetClassOrObject;

        Map<Instruction, Edges<Map<VariableDescriptor,VariableInitState>>> initializers = pseudocodeVariablesData.getVariableInitializers();
        final Set<VariableDescriptor> declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode);
        PseudocodeTraverser.traverse(pseudocode, true, true, initializers, new InstructionDataAnalyzeStrategy<Map<VariableDescriptor, PseudocodeVariablesData.VariableInitState>>() {
            @Override
            public void execute(@NotNull Instruction instruction,
                    @Nullable Map<VariableDescriptor, VariableInitState> in,
                    @Nullable Map<VariableDescriptor, PseudocodeVariablesData.VariableInitState> out) {
                assert in != null && out != null;
                VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true, trace.getBindingContext());
                if (variableDescriptor == null) return;
                if (!(instruction instanceof ReadValueInstruction) && !(instruction instanceof WriteValueInstruction)) return;
                VariableInitState outInitState = out.get(variableDescriptor);
                if (instruction instanceof ReadValueInstruction) {
                    JetElement element = ((ReadValueInstruction) instruction).getElement();
                    boolean error = checkBackingField(variableDescriptor, element);
                    if (!error && declaredVariables.contains(variableDescriptor)) {
                        checkIsInitialized(variableDescriptor, element, outInitState, varWithUninitializedErrorGenerated);
                    }
                    return;
                }
                JetElement element = ((WriteValueInstruction) instruction).getlValue();
                boolean error = checkBackingField(variableDescriptor, element);
                if (!(element instanceof JetExpression)) return;
                PseudocodeVariablesData.VariableInitState inInitState = in.get(variableDescriptor);
                if (!error && !processLocalDeclaration) { // error has been generated before, while processing outer function of this local declaration
                    error = checkValReassignment(variableDescriptor, (JetExpression) element, inInitState, varWithValReassignErrorGenerated);
                }
                if (!error && processClassOrObject) {
                    error = checkAssignmentBeforeDeclaration(variableDescriptor, (JetExpression) element, inInitState, outInitState);
                }
                if (!error && processClassOrObject) {
                    checkInitializationUsingBackingField(variableDescriptor, (JetExpression) element, inInitState, outInitState);
                }
            }
        });

        Pseudocode pseudocode = pseudocodeVariablesData.getPseudocode();
        recordInitializedVariables(pseudocode, initializers);
        for (LocalDeclarationInstruction instruction : pseudocode.getLocalDeclarations()) {
            recordInitializedVariables(instruction.getBody(), initializers);
        }
    }

    private void checkIsInitialized(@NotNull VariableDescriptor variableDescriptor,
                                    @NotNull JetElement element,
                                    @NotNull PseudocodeVariablesData.VariableInitState variableInitState,
                                    @NotNull Collection<VariableDescriptor> varWithUninitializedErrorGenerated) {
        if (!(element instanceof JetSimpleNameExpression)) return;

        boolean isInitialized = variableInitState.isInitialized;
        if (variableDescriptor instanceof PropertyDescriptor) {
            if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor)) {
                isInitialized = true;
            }
        }
        if (!isInitialized && !varWithUninitializedErrorGenerated.contains(variableDescriptor)) {
            varWithUninitializedErrorGenerated.add(variableDescriptor);
            if (variableDescriptor instanceof ValueParameterDescriptor) {
                trace.report(Errors.UNINITIALIZED_PARAMETER.on((JetSimpleNameExpression) element,
                                                               (ValueParameterDescriptor) variableDescriptor));
            }
            else {
                trace.report(Errors.UNINITIALIZED_VARIABLE.on((JetSimpleNameExpression) element, variableDescriptor));
            }
        }
    }

    private boolean checkValReassignment(@NotNull VariableDescriptor variableDescriptor,
                                         @NotNull JetExpression expression,
                                         @NotNull VariableInitState enterInitState,
                                         @NotNull Collection<VariableDescriptor> varWithValReassignErrorGenerated) {
        boolean isInitializedNotHere = enterInitState.isInitialized;
        if (expression.getParent() instanceof JetProperty && ((JetProperty)expression).getInitializer() != null) {
            isInitializedNotHere = false;
        }
        boolean hasBackingField = true;
        if (variableDescriptor instanceof PropertyDescriptor) {
            hasBackingField = trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor);
        }
        if ((isInitializedNotHere || !hasBackingField) && !variableDescriptor.isVar() && !varWithValReassignErrorGenerated.contains(variableDescriptor)) {
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
                if (descriptor == null) {
                    Collection<? extends DeclarationDescriptor> descriptors = trace.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, operationReference);
                    if (descriptors != null) {
                        for (DeclarationDescriptor referenceDescriptor : descriptors) {
                            if (JetStandardClasses.isUnit(((FunctionDescriptor) referenceDescriptor).getReturnType())) {
                                hasReassignMethodReturningUnit = true;
                            }
                        }
                    }
                }
            }
            if (!hasReassignMethodReturningUnit) {
                varWithValReassignErrorGenerated.add(variableDescriptor);
                trace.report(Errors.VAL_REASSIGNMENT.on(expression, variableDescriptor));
                return true;
            }
        }
        return false;
    }

    private boolean checkAssignmentBeforeDeclaration(@NotNull VariableDescriptor variableDescriptor, @NotNull JetExpression expression, @NotNull VariableInitState enterInitState, @NotNull VariableInitState exitInitState) {
        if (!enterInitState.isDeclared && !exitInitState.isDeclared && !enterInitState.isInitialized && exitInitState.isInitialized) {
            trace.report(Errors.INITIALIZATION_BEFORE_DECLARATION.on(expression, variableDescriptor));
            return true;
        }
        return false;
    }

    private boolean checkInitializationUsingBackingField(@NotNull VariableDescriptor variableDescriptor, @NotNull JetExpression expression, @NotNull VariableInitState enterInitState, @NotNull VariableInitState exitInitState) {
        if (variableDescriptor instanceof PropertyDescriptor && !enterInitState.isInitialized && exitInitState.isInitialized) {
            if (!variableDescriptor.isVar()) return false;
            if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor)) return false;
            PsiElement property = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), variableDescriptor);
            assert property instanceof JetProperty;
            if (((PropertyDescriptor) variableDescriptor).getModality() == Modality.FINAL && ((JetProperty) property).getSetter() == null) return false;
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
                        trace.report(Errors.INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER.on(expression, variableDescriptor));
                    }
                    else {
                        trace.report(Errors.INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER.on(expression, variableDescriptor));
                    }
                    return true;
                }
            }
        }
        return false;
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
        PsiElement property = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), variableDescriptor);
        boolean insideSelfAccessors = PsiTreeUtil.isAncestor(property, element, false);
        if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor) &&
                !insideSelfAccessors) { // not to generate error in accessors of abstract properties, there is one: declared accessor of abstract property

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
        if (declarationDescriptor == null) return false;

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

    private void recordInitializedVariables(@NotNull Pseudocode pseudocode, @NotNull Map<Instruction, Edges<Map<VariableDescriptor,PseudocodeVariablesData.VariableInitState>>> initializersMap) {
        Edges<Map<VariableDescriptor, VariableInitState>> initializers = initializersMap.get(pseudocode.getExitInstruction());
        Set<VariableDescriptor> usedVariables = pseudocodeVariablesData.getUsedVariables(pseudocode);
        Set<VariableDescriptor> declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode);
        for (VariableDescriptor variable : usedVariables) {
            if (variable instanceof PropertyDescriptor && declaredVariables.contains(variable)) {
                PseudocodeVariablesData.VariableInitState variableInitState = initializers.in.get(variable);
                if (variableInitState == null) return;
                trace.record(BindingContext.IS_INITIALIZED, (PropertyDescriptor) variable, variableInitState.isInitialized);
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////
//  "Unused variable" & "unused value" analyses

    public void markUnusedVariables() {
        Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> variableStatusData = pseudocodeVariablesData.getVariableUseStatusData();
        InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableUseState>> variableStatusAnalyzeStrategy =
                new InstructionDataAnalyzeStrategy<Map<VariableDescriptor, PseudocodeVariablesData.VariableUseState>>() {
            @Override
            public void execute(@NotNull Instruction instruction,
                    @Nullable Map<VariableDescriptor, PseudocodeVariablesData.VariableUseState> in,
                    @Nullable Map<VariableDescriptor, VariableUseState> out) {

                assert in != null && out != null;
                Set<VariableDescriptor> declaredVariables = pseudocodeVariablesData.getDeclaredVariables(instruction.getOwner());
                VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false,
                                                                      trace.getBindingContext());
                if (variableDescriptor == null || !declaredVariables.contains(variableDescriptor) ||
                    !DescriptorUtils.isLocal(variableDescriptor.getContainingDeclaration(), variableDescriptor)) return;
                PseudocodeVariablesData.VariableUseState variableUseState = in.get(variableDescriptor);
                if (instruction instanceof WriteValueInstruction) {
                    if (trace.get(CAPTURED_IN_CLOSURE, variableDescriptor)) return;
                    JetElement element = ((WriteValueInstruction) instruction).getElement();
                    if (variableUseState != LAST_READ) {
                        if (element instanceof JetBinaryExpression &&
                            ((JetBinaryExpression) element).getOperationToken() == JetTokens.EQ) {
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
                        if (nameIdentifier == null) return;
                        if (!VariableUseState.isUsed(variableUseState)) {
                            if (element instanceof JetVariableDeclaration) {
                                trace.report(Errors.UNUSED_VARIABLE.on((JetVariableDeclaration) element, variableDescriptor));
                            }
                            else if (element instanceof JetParameter) {
                                PsiElement psiElement = element.getParent().getParent();
                                if (psiElement instanceof JetFunction) {
                                    boolean isMain = (psiElement instanceof JetNamedFunction) && JetMainDetector.isMain((JetNamedFunction) psiElement);
                                    if (psiElement instanceof JetFunctionLiteral) return;
                                    DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiElement);
                                    assert descriptor instanceof FunctionDescriptor : psiElement.getText();
                                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                                    if (!isMain && !functionDescriptor.getModality().isOverridable() && functionDescriptor.getOverriddenDescriptors().isEmpty()) {
                                        trace.report(Errors.UNUSED_PARAMETER.on((JetParameter) element, variableDescriptor));
                                    }
                                }
                            }
                        }
                        else if (variableUseState == ONLY_WRITTEN_NEVER_READ && element instanceof JetVariableDeclaration) {
                            trace.report(Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.on((JetVariableDeclaration) element, variableDescriptor));
                        }
                        else if (variableUseState == LAST_WRITTEN && element instanceof JetVariableDeclaration) {
                            if (element instanceof JetProperty) {
                                JetExpression initializer = ((JetProperty) element).getInitializer();
                                if (initializer != null) {
                                    trace.report(Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER.on(initializer, variableDescriptor));
                                }
                            }
                            else if (element instanceof JetMultiDeclarationEntry) {
                                trace.report(VARIABLE_WITH_REDUNDANT_INITIALIZER.on(element, variableDescriptor));
                            }
                        }
                    }
                }
            }
        };
        PseudocodeTraverser.traverse(pseudocode, false, true, variableStatusData, variableStatusAnalyzeStrategy);
    }

////////////////////////////////////////////////////////////////////////////////
//  "Unused literals" in block

    public void markUnusedLiteralsInBlock() {
        assert pseudocode != null;
        PseudocodeTraverser.traverse(
                pseudocode, true, new InstructionAnalyzeStrategy() {
            @Override
            public void execute(@NotNull Instruction instruction) {
                if (!(instruction instanceof ReadValueInstruction)) return;
                JetElement element =
                        ((ReadValueInstruction) instruction).getElement();
                if (!(element instanceof JetFunctionLiteralExpression
                      || element instanceof JetConstantExpression
                      || element instanceof JetStringTemplateExpression
                      || element instanceof JetSimpleNameExpression)) {
                    return;
                }
                PsiElement parent = element.getParent();
                if (parent instanceof JetBlockExpression) {
                    if (!JetPsiUtil.isImplicitlyUsed(element)) {
                        if (element instanceof JetFunctionLiteralExpression) {
                            trace.report(Errors.UNUSED_FUNCTION_LITERAL
                                                 .on((JetFunctionLiteralExpression) element));
                        }
                        else {
                            trace.report(Errors.UNUSED_EXPRESSION.on(element));
                        }
                    }
                }
            }
        });
    }
}
