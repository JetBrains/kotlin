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

package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.PseudocodeTraverser.Edges;
import org.jetbrains.jet.lang.cfg.PseudocodeTraverser.InstructionAnalyzeStrategy;
import org.jetbrains.jet.lang.cfg.PseudocodeTraverser.InstructionDataAnalyzeStrategy;
import org.jetbrains.jet.lang.cfg.PseudocodeVariablesData.VariableInitState;
import org.jetbrains.jet.lang.cfg.PseudocodeVariablesData.VariableUseState;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.TailRecursionKind;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.util.*;

import static org.jetbrains.jet.lang.cfg.PseudocodeTraverser.TraversalOrder.BACKWARD;
import static org.jetbrains.jet.lang.cfg.PseudocodeTraverser.TraversalOrder.FORWARD;
import static org.jetbrains.jet.lang.cfg.PseudocodeVariablesData.VariableUseState.*;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.TailRecursionKind.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.noExpectedType;

public class JetFlowInformationProvider {

    private final JetElement subroutine;
    private final Pseudocode pseudocode;
    private final BindingTrace trace;
    private PseudocodeVariablesData pseudocodeVariablesData;

    private JetFlowInformationProvider(
            @NotNull JetElement declaration,
            @NotNull BindingTrace trace,
            @NotNull Pseudocode pseudocode
    ) {
        this.subroutine = declaration;
        this.trace = trace;
        this.pseudocode = pseudocode;
    }

    public JetFlowInformationProvider(
            @NotNull JetElement declaration,
            @NotNull BindingTrace trace
    ) {
        this(declaration, trace, new JetControlFlowProcessor(trace).generatePseudocode(declaration));
    }

    public PseudocodeVariablesData getPseudocodeVariablesData() {
        if (pseudocodeVariablesData == null) {
            pseudocodeVariablesData = new PseudocodeVariablesData(pseudocode, trace.getBindingContext());
        }
        return pseudocodeVariablesData;
    }

    public void checkFunction(
            @NotNull JetDeclarationWithBody function,
            @NotNull JetType expectedReturnType,
            boolean isLocalObject
    ) {
        boolean isPropertyAccessor = function instanceof JetPropertyAccessor;
        if (!isPropertyAccessor) {
            recordInitializedVariables();
        }

        checkDefiniteReturn(expectedReturnType);
        checkLocalFunctions();

        if (isLocalObject) return;

        if (!isPropertyAccessor) {
            // Property accessor is checked through initialization of a class/object or package properties (at 'checkDeclarationContainer')
            markUninitializedVariables();
        }

        markUnusedVariables();

        markUnusedLiteralsInBlock();

        markTailCalls();
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
                public void visitMarkInstruction(MarkInstruction instruction) {
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

    private void checkLocalFunctions() {
        for (LocalFunctionDeclarationInstruction localDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            JetElement element = localDeclarationInstruction.getElement();
            if (element instanceof JetNamedFunction) {
                JetNamedFunction localFunction = (JetNamedFunction) element;
                SimpleFunctionDescriptor functionDescriptor = trace.getBindingContext().get(BindingContext.FUNCTION, localFunction);
                JetType expectedType = functionDescriptor != null ? functionDescriptor.getReturnType() : null;

                JetFlowInformationProvider providerForLocalDeclaration =
                        new JetFlowInformationProvider(localFunction, trace, localDeclarationInstruction.getBody());

                providerForLocalDeclaration.checkDefiniteReturn(expectedType != null ? expectedType : NO_EXPECTED_TYPE);
                providerForLocalDeclaration.markTailCalls();
            }
        }
    }

    public void checkDefiniteReturn(final @NotNull JetType expectedReturnType) {
        assert subroutine instanceof JetDeclarationWithBody;
        JetDeclarationWithBody function = (JetDeclarationWithBody) subroutine;

        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;

        List<JetElement> returnedExpressions = Lists.newArrayList();
        collectReturnExpressions(returnedExpressions);

        final boolean blockBody = function.hasBlockBody();

        final Set<JetElement> rootUnreachableElements = collectUnreachableCode();
        for (JetElement element : rootUnreachableElements) {
            trace.report(UNREACHABLE_CODE.on(element));
        }

        final boolean[] noReturnError = new boolean[] { false };
        for (JetElement returnedExpression : returnedExpressions) {
            returnedExpression.accept(new JetVisitorVoid() {
                @Override
                public void visitReturnExpression(@NotNull JetReturnExpression expression) {
                    if (!blockBody) {
                        trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression));
                    }
                }

                @Override
                public void visitExpression(@NotNull JetExpression expression) {
                    if (blockBody && !noExpectedType(expectedReturnType) && !KotlinBuiltIns.getInstance().isUnit(expectedReturnType) && !rootUnreachableElements.contains(expression)) {
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
                !(deadInstruction instanceof LoadUnitValueInstruction)) {
                unreachableElements.add(((JetElementInstruction) deadInstruction).getElement());
            }
        }
        // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
        return JetPsiUtil.findRootExpressions(unreachableElements);
    }

////////////////////////////////////////////////////////////////////////////////
//  Uninitialized variables analysis

    public void markUninitializedVariables() {
        final Collection<VariableDescriptor> varWithUninitializedErrorGenerated = Sets.newHashSet();
        final Collection<VariableDescriptor> varWithValReassignErrorGenerated = Sets.newHashSet();
        final boolean processClassOrObject = subroutine instanceof JetClassOrObject;

        PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Map<Instruction, Edges<Map<VariableDescriptor,VariableInitState>>> initializers = pseudocodeVariablesData.getVariableInitializers();
        final Set<VariableDescriptor> declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, true);

        final Map<Instruction, DiagnosticFactory> reportedDiagnosticMap = Maps.newHashMap();

        PseudocodeTraverser.traverse(pseudocode, FORWARD, initializers, new InstructionDataAnalyzeStrategy<Map<VariableDescriptor, PseudocodeVariablesData.VariableInitState>>() {
            @Override
            public void execute(@NotNull Instruction instruction,
                    @Nullable Map<VariableDescriptor, VariableInitState> in,
                    @Nullable Map<VariableDescriptor, VariableInitState> out) {
                assert in != null && out != null;
                VariableInitContext ctxt = new VariableInitContext(instruction, reportedDiagnosticMap, in, out);
                if (ctxt.variableDescriptor == null) return;
                if (instruction instanceof ReadValueInstruction) {
                    JetElement element = ((ReadValueInstruction) instruction).getElement();
                    boolean error = checkBackingField(ctxt, element);
                    if (!error && declaredVariables.contains(ctxt.variableDescriptor)) {
                        checkIsInitialized(ctxt, element, varWithUninitializedErrorGenerated);
                    }
                    return;
                }
                if (!(instruction instanceof WriteValueInstruction)) return;
                JetElement element = ((WriteValueInstruction) instruction).getlValue();
                boolean error = checkBackingField(ctxt, element);
                if (!(element instanceof JetExpression)) return;
                if (!error) {
                    error = checkValReassignment(ctxt, (JetExpression) element, varWithValReassignErrorGenerated);
                }
                if (!error && processClassOrObject) {
                    error = checkAssignmentBeforeDeclaration(ctxt, (JetExpression) element);
                }
                if (!error && processClassOrObject) {
                    checkInitializationUsingBackingField(ctxt, (JetExpression) element);
                }
            }
        });
    }

    public void recordInitializedVariables() {
        PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Pseudocode pseudocode = pseudocodeVariablesData.getPseudocode();
        Map<Instruction, Edges<Map<VariableDescriptor,VariableInitState>>> initializers = pseudocodeVariablesData.getVariableInitializers();
        recordInitializedVariables(pseudocode, initializers);
        for (LocalFunctionDeclarationInstruction instruction : pseudocode.getLocalDeclarations()) {
            recordInitializedVariables(instruction.getBody(), initializers);
        }
    }

    private void checkIsInitialized(
            @NotNull VariableInitContext ctxt,
            @NotNull JetElement element,
            @NotNull Collection<VariableDescriptor> varWithUninitializedErrorGenerated
    ) {
        if (!(element instanceof JetSimpleNameExpression)) return;

        boolean isInitialized = ctxt.exitInitState.isInitialized;
        VariableDescriptor variableDescriptor = ctxt.variableDescriptor;
        if (variableDescriptor instanceof PropertyDescriptor) {
            if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor)) {
                isInitialized = true;
            }
        }
        if (!isInitialized && !varWithUninitializedErrorGenerated.contains(variableDescriptor)) {
            if (!(variableDescriptor instanceof PropertyDescriptor)) {
                varWithUninitializedErrorGenerated.add(variableDescriptor);
            }
            if (variableDescriptor instanceof ValueParameterDescriptor) {
                report(Errors.UNINITIALIZED_PARAMETER.on((JetSimpleNameExpression) element,
                                                         (ValueParameterDescriptor) variableDescriptor), ctxt);
            }
            else {
                report(Errors.UNINITIALIZED_VARIABLE.on((JetSimpleNameExpression) element, variableDescriptor), ctxt);
            }
        }
    }

    private boolean checkValReassignment(
            @NotNull VariableInitContext ctxt,
            @NotNull JetExpression expression,
            @NotNull Collection<VariableDescriptor> varWithValReassignErrorGenerated
    ) {
        VariableDescriptor variableDescriptor = ctxt.variableDescriptor;
        if (JetPsiUtil.isBackingFieldReference(expression) && variableDescriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            JetPropertyAccessor accessor = PsiTreeUtil.getParentOfType(expression, JetPropertyAccessor.class);
            if (accessor != null) {
                DeclarationDescriptor accessorDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, accessor);
                if (propertyDescriptor.getGetter() == accessorDescriptor) {
                    //val can be reassigned through backing field inside its own getter
                    return false;
                }
            }
        }

        boolean isInitializedNotHere = ctxt.enterInitState.isInitialized;
        boolean hasBackingField = true;
        if (variableDescriptor instanceof PropertyDescriptor) {
            hasBackingField = trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor);
        }
        if (variableDescriptor.isVar() && variableDescriptor instanceof PropertyDescriptor) {
            DeclarationDescriptor descriptor = BindingContextUtils.getEnclosingDescriptor(trace.getBindingContext(), expression);
            PropertySetterDescriptor setterDescriptor = ((PropertyDescriptor) variableDescriptor).getSetter();
            if (Visibilities.isVisible(variableDescriptor, descriptor) && setterDescriptor != null && !Visibilities.isVisible(setterDescriptor, descriptor)) {
                report(Errors.INVISIBLE_SETTER.on(expression, variableDescriptor, setterDescriptor.getVisibility(),
                                                  variableDescriptor.getContainingDeclaration()), ctxt);
                return true;
            }
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
                    if (KotlinBuiltIns.getInstance().isUnit(((FunctionDescriptor) descriptor).getReturnType())) {
                        hasReassignMethodReturningUnit = true;
                    }
                }
                if (descriptor == null) {
                    Collection<? extends DeclarationDescriptor> descriptors = trace.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, operationReference);
                    if (descriptors != null) {
                        for (DeclarationDescriptor referenceDescriptor : descriptors) {
                            if (KotlinBuiltIns.getInstance().isUnit(((FunctionDescriptor) referenceDescriptor).getReturnType())) {
                                hasReassignMethodReturningUnit = true;
                            }
                        }
                    }
                }
            }
            if (!hasReassignMethodReturningUnit) {
                varWithValReassignErrorGenerated.add(variableDescriptor);
                report(Errors.VAL_REASSIGNMENT.on(expression, variableDescriptor), ctxt);
                return true;
            }
        }
        return false;
    }

    private boolean checkAssignmentBeforeDeclaration(@NotNull VariableInitContext ctxt, @NotNull JetExpression expression) {
        if (!ctxt.enterInitState.isDeclared && !ctxt.exitInitState.isDeclared && !ctxt.enterInitState.isInitialized && ctxt.exitInitState.isInitialized) {
            report(Errors.INITIALIZATION_BEFORE_DECLARATION.on(expression, ctxt.variableDescriptor), ctxt);
            return true;
        }
        return false;
    }

    private boolean checkInitializationUsingBackingField(@NotNull VariableInitContext ctxt, @NotNull JetExpression expression) {
        VariableDescriptor variableDescriptor = ctxt.variableDescriptor;
        if (variableDescriptor instanceof PropertyDescriptor && !ctxt.enterInitState.isInitialized && ctxt.exitInitState.isInitialized) {
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
                        report(Errors.INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER.on(expression, variableDescriptor), ctxt);
                    }
                    else {
                        report(Errors.INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER.on(expression, variableDescriptor), ctxt);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkBackingField(@NotNull VariableContext cxtx, @NotNull JetElement element) {
        VariableDescriptor variableDescriptor = cxtx.variableDescriptor;
        boolean[] error = new boolean[1];
        if (!isCorrectBackingFieldReference(element, cxtx, error, true)) return false;
        if (error[0]) return true;
        if (!(variableDescriptor instanceof PropertyDescriptor)) {
            report(Errors.NOT_PROPERTY_BACKING_FIELD.on(element), cxtx);
            return true;
        }
        PsiElement property = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), variableDescriptor);
        boolean insideSelfAccessors = PsiTreeUtil.isAncestor(property, element, false);
        if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor) &&
                !insideSelfAccessors) { // not to generate error in accessors of abstract properties, there is one: declared accessor of abstract property

            if (((PropertyDescriptor) variableDescriptor).getModality() == Modality.ABSTRACT) {
                report(NO_BACKING_FIELD_ABSTRACT_PROPERTY.on(element), cxtx);
            }
            else {
                report(NO_BACKING_FIELD_CUSTOM_ACCESSORS.on(element), cxtx);
            }
            return true;
        }
        if (insideSelfAccessors) return false;

        DeclarationDescriptor declarationDescriptor = BindingContextUtils.getEnclosingDescriptor(trace.getBindingContext(), element);

        DeclarationDescriptor containingDeclaration = variableDescriptor.getContainingDeclaration();
        if ((containingDeclaration instanceof ClassDescriptor) && DescriptorUtils.isAncestor(containingDeclaration, declarationDescriptor, false)) {
            return false;
        }
        report(Errors.INACCESSIBLE_BACKING_FIELD.on(element), cxtx);
        return true;
    }

    private boolean isCorrectBackingFieldReference(@Nullable JetElement element, VariableContext ctxt, boolean[] error, boolean reportError) {
        error[0] = false;
        if (JetPsiUtil.isBackingFieldReference(element)) {
            return true;
        }
        if (element instanceof JetDotQualifiedExpression && isCorrectBackingFieldReference(
                ((JetDotQualifiedExpression) element).getSelectorExpression(), ctxt, error, false)) {
            if (((JetDotQualifiedExpression) element).getReceiverExpression() instanceof JetThisExpression) {
                return true;
            }
            error[0] = true;
            if (reportError) {
                report(Errors.INACCESSIBLE_BACKING_FIELD.on(element), ctxt);
            }
        }
        return false;
    }

    private void recordInitializedVariables(@NotNull Pseudocode pseudocode, @NotNull Map<Instruction, Edges<Map<VariableDescriptor,PseudocodeVariablesData.VariableInitState>>> initializersMap) {
        Edges<Map<VariableDescriptor, VariableInitState>> initializers = initializersMap.get(pseudocode.getExitInstruction());
        Set<VariableDescriptor> declaredVariables = getPseudocodeVariablesData().getDeclaredVariables(pseudocode, false);
        for (VariableDescriptor variable : declaredVariables) {
            if (variable instanceof PropertyDescriptor) {
                PseudocodeVariablesData.VariableInitState variableInitState = initializers.in.get(variable);
                if (variableInitState == null) return;
                trace.record(BindingContext.IS_INITIALIZED, (PropertyDescriptor) variable, variableInitState.isInitialized);
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////
//  "Unused variable" & "unused value" analyses

    public void markUnusedVariables() {
        final PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> variableStatusData = pseudocodeVariablesData.getVariableUseStatusData();
        final Map<Instruction, DiagnosticFactory> reportedDiagnosticMap = Maps.newHashMap();
        InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableUseState>> variableStatusAnalyzeStrategy =
                new InstructionDataAnalyzeStrategy<Map<VariableDescriptor, PseudocodeVariablesData.VariableUseState>>() {
            @Override
            public void execute(@NotNull Instruction instruction,
                    @Nullable Map<VariableDescriptor, VariableUseState> in,
                    @Nullable Map<VariableDescriptor, VariableUseState> out) {

                assert in != null && out != null;
                VariableContext ctxt = new VariableUseContext(instruction, reportedDiagnosticMap, in, out);
                Set<VariableDescriptor> declaredVariables = pseudocodeVariablesData.getDeclaredVariables(instruction.getOwner(), false);
                VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false,
                                                                      trace.getBindingContext());
                if (variableDescriptor == null || !declaredVariables.contains(variableDescriptor) ||
                    !DescriptorUtils.isLocal(variableDescriptor.getContainingDeclaration(), variableDescriptor)) return;
                PseudocodeVariablesData.VariableUseState variableUseState = in.get(variableDescriptor);
                if (instruction instanceof WriteValueInstruction) {
                    if (trace.get(CAPTURED_IN_CLOSURE, variableDescriptor) != null) return;
                    JetElement element = ((WriteValueInstruction) instruction).getElement();
                    if (variableUseState != LAST_READ) {
                        if (element instanceof JetBinaryExpression &&
                            ((JetBinaryExpression) element).getOperationToken() == JetTokens.EQ) {
                            JetExpression right = ((JetBinaryExpression) element).getRight();
                            if (right != null) {
                                report(Errors.UNUSED_VALUE.on(right, right, variableDescriptor), ctxt);
                            }
                        }
                        else if (element instanceof JetPostfixExpression) {
                            IElementType operationToken = ((JetPostfixExpression) element).getOperationReference().getReferencedNameElementType();
                            if (operationToken == JetTokens.PLUSPLUS || operationToken == JetTokens.MINUSMINUS) {
                                report(Errors.UNUSED_CHANGED_VALUE.on(element, element), ctxt);
                            }
                        }
                    }
                }
                else if (instruction instanceof VariableDeclarationInstruction) {
                    JetDeclaration element = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
                    if (!(element instanceof JetNamedDeclaration)) return;
                    PsiElement nameIdentifier = ((JetNamedDeclaration) element).getNameIdentifier();
                    if (nameIdentifier == null) return;
                    if (!VariableUseState.isUsed(variableUseState)) {
                        if (JetPsiUtil.isVariableNotParameterDeclaration(element)) {
                            report(Errors.UNUSED_VARIABLE.on((JetNamedDeclaration) element, variableDescriptor), ctxt);
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
                                    report(Errors.UNUSED_PARAMETER.on((JetParameter) element, variableDescriptor), ctxt);
                                }
                            }
                        }
                    }
                    else if (variableUseState == ONLY_WRITTEN_NEVER_READ && JetPsiUtil.isVariableNotParameterDeclaration(element)) {
                        report(Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.on((JetNamedDeclaration) element, variableDescriptor), ctxt);
                    }
                    else if (variableUseState == LAST_WRITTEN && element instanceof JetVariableDeclaration) {
                        if (element instanceof JetProperty) {
                            JetExpression initializer = ((JetProperty) element).getInitializer();
                            if (initializer != null) {
                                report(Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER.on(initializer, variableDescriptor), ctxt);
                            }
                        }
                        else if (element instanceof JetMultiDeclarationEntry) {
                            report(VARIABLE_WITH_REDUNDANT_INITIALIZER.on(element, variableDescriptor), ctxt);
                        }
                    }
                }
            }
        };
        PseudocodeTraverser.traverse(pseudocode, BACKWARD, variableStatusData, variableStatusAnalyzeStrategy);
    }

////////////////////////////////////////////////////////////////////////////////
//  "Unused literals" in block

    public void markUnusedLiteralsInBlock() {
        final Map<Instruction, DiagnosticFactory> reportedDiagnosticMap = Maps.newHashMap();
        PseudocodeTraverser.traverse(
                pseudocode, FORWARD, new InstructionAnalyzeStrategy() {
            @Override
            public void execute(@NotNull Instruction instruction) {
                if (!(instruction instanceof ReadValueInstruction)) return;
                VariableContext ctxt = new VariableContext(instruction, reportedDiagnosticMap);
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
                            report(Errors.UNUSED_FUNCTION_LITERAL.on((JetFunctionLiteralExpression) element), ctxt);
                        }
                        else {
                            report(Errors.UNUSED_EXPRESSION.on(element), ctxt);
                        }
                    }
                }
            }
        });
    }

////////////////////////////////////////////////////////////////////////////////
// Tail calls

    public void markTailCalls() {
        final DeclarationDescriptor subroutineDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, subroutine);
        if (!(subroutineDescriptor instanceof FunctionDescriptor)) return;
        if (!KotlinBuiltIns.getInstance().isTailRecursive(subroutineDescriptor)) return;

        // finally blocks are copied which leads to multiple diagnostics reported on one instruction
        class KindAndCall {
            TailRecursionKind kind;
            ResolvedCall<?> call;

            KindAndCall(TailRecursionKind kind, ResolvedCall<?> call) {
                this.kind = kind;
                this.call = call;
            }
        }
        final Map<JetElement, KindAndCall> calls = new HashMap<JetElement, KindAndCall>();
        PseudocodeTraverser.traverse(
                pseudocode,
                FORWARD,
                new InstructionAnalyzeStrategy() {
                    @Override
                    public void execute(@NotNull Instruction instruction) {
                        if (!(instruction instanceof CallInstruction)) return;
                        CallInstruction callInstruction = (CallInstruction) instruction;

                        ResolvedCall<?> resolvedCall = trace.get(RESOLVED_CALL, callInstruction.getElement());
                        if (resolvedCall == null) return;

                        // is this a recursive call?
                        CallableDescriptor functionDescriptor = resolvedCall.getResultingDescriptor();
                        if (!functionDescriptor.getOriginal().equals(subroutineDescriptor)) return;

                        JetElement element = callInstruction.getElement();
                        //noinspection unchecked
                        JetExpression parent = PsiTreeUtil.getParentOfType(
                                element,
                                JetTryExpression.class, JetFunction.class, JetClassInitializer.class
                        );

                        if (parent instanceof JetTryExpression) {
                            // We do not support tail calls Collections.singletonMap() try-catch-finally, for simplicity of the mental model
                            // very few cases there would be real tail-calls, and it's often not so easy for the user to see why
                            calls.put(element, new KindAndCall(IN_TRY, resolvedCall));
                            return;
                        }

                        boolean isTail = PseudocodeTraverser.traverseFollowingInstructions(
                                callInstruction,
                                new HashSet<Instruction>(),
                                FORWARD,
                                new TailRecursionDetector(subroutine, callInstruction)
                        );

                        boolean sameThisObject = sameThisObject(resolvedCall);

                        TailRecursionKind kind = isTail && sameThisObject ? TAIL_CALL : NON_TAIL;

                        KindAndCall kindAndCall = calls.get(element);
                        calls.put(element,
                                  new KindAndCall(
                                          combineKinds(kind, kindAndCall == null ? null : kindAndCall.kind),
                                          resolvedCall
                                  )
                        );
                    }
                }
        );
        boolean hasTailCalls = false;
        for (Map.Entry<JetElement, KindAndCall> entry : calls.entrySet()) {
            JetElement element = entry.getKey();
            KindAndCall kindAndCall = entry.getValue();
            switch (kindAndCall.kind) {
                case TAIL_CALL:
                    trace.record(TAIL_RECURSION_CALL, kindAndCall.call, TailRecursionKind.TAIL_CALL);
                    hasTailCalls = true;
                    break;
                case IN_TRY:
                    trace.report(Errors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED.on(element));
                    break;
                case NON_TAIL:
                    trace.report(Errors.NON_TAIL_RECURSIVE_CALL.on(element));
                    break;
            }
        }

        if (!hasTailCalls) {
            trace.report(Errors.NO_TAIL_CALLS_FOUND.on((JetNamedFunction) subroutine));
        }
    }

    private boolean sameThisObject(ResolvedCall<?> resolvedCall) {
        // A tail call is not allowed to change dispatch receiver
        //   class C {
        //       fun foo(other: C) {
        //           other.foo(this) // not a tail call
        //       }
        //   }
        ReceiverParameterDescriptor thisObject = resolvedCall.getResultingDescriptor().getExpectedThisObject();
        ReceiverValue thisObjectValue = resolvedCall.getThisObject();
        if (thisObject == null || !thisObjectValue.exists()) return true;

        DeclarationDescriptor classDescriptor = null;
        if (thisObjectValue instanceof ThisReceiver) {
            // foo() -- implicit receiver
            classDescriptor = ((ThisReceiver) thisObjectValue).getDeclarationDescriptor();
        }
        else if (thisObjectValue instanceof ExpressionReceiver) {
            JetExpression expression = JetPsiUtil.deparenthesize(((ExpressionReceiver) thisObjectValue).getExpression());
            if (expression instanceof JetThisExpression) {
                // this.foo() -- explicit receiver
                JetThisExpression thisExpression = (JetThisExpression) expression;
                classDescriptor = trace.get(BindingContext.REFERENCE_TARGET, thisExpression.getInstanceReference());
            }
        }
        return thisObject.getContainingDeclaration() == classDescriptor;
    }

    private static TailRecursionKind combineKinds(TailRecursionKind kind, @Nullable TailRecursionKind existingKind) {
        TailRecursionKind resultingKind;
        if (existingKind == null || existingKind == kind) {
            resultingKind = kind;
        }
        else {
            if (check(kind, existingKind, IN_TRY, TAIL_CALL)) {
                resultingKind = IN_TRY;
            }
            else if (check(kind, existingKind, IN_TRY, NON_TAIL)) {
                resultingKind = IN_TRY;
            }
            else {
                // TAIL_CALL, NON_TAIL
                resultingKind = NON_TAIL;
            }
        }
        return resultingKind;
    }

    private static boolean check(Object a, Object b, Object x, Object y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    ////////////////////////////////////////////////////////////////////////////////
// Utility classes and methods

    /**
     * The method provides reporting of the same diagnostic only once for copied instructions
     * (depends on whether it should be reported for all or only for one of the copies)
     */
    private void report(
            @NotNull Diagnostic diagnostic,
            @NotNull VariableContext ctxt
    ) {
        Instruction instruction = ctxt.instruction;
        if (instruction.getCopies().isEmpty()) {
            trace.report(diagnostic);
            return;
        }
        Map<Instruction, DiagnosticFactory> previouslyReported = ctxt.reportedDiagnosticMap;
        previouslyReported.put(instruction, diagnostic.getFactory());

        boolean alreadyReported = false;
        boolean sameErrorForAllCopies = true;
        for (Instruction copy : instruction.getCopies()) {
            DiagnosticFactory previouslyReportedErrorFactory = previouslyReported.get(copy);
            if (previouslyReportedErrorFactory != null) {
                alreadyReported = true;
            }

            if (previouslyReportedErrorFactory != diagnostic.getFactory()) {
                sameErrorForAllCopies = false;
            }
        }

        if (mustBeReportedOnAllCopies(diagnostic.getFactory())) {
            if (sameErrorForAllCopies) {
                trace.report(diagnostic);
            }
        }
        else {
            //only one reporting required
            if (!alreadyReported) {
                trace.report(diagnostic);
            }
        }
    }

    private static boolean mustBeReportedOnAllCopies(@NotNull DiagnosticFactory diagnosticFactory) {
        return diagnosticFactory == UNUSED_VARIABLE
               || diagnosticFactory == UNUSED_PARAMETER
               || diagnosticFactory == UNUSED_CHANGED_VALUE;
    }



    private class VariableContext {
        final Map<Instruction, DiagnosticFactory> reportedDiagnosticMap;
        final Instruction instruction;
        final VariableDescriptor variableDescriptor;

        private VariableContext(
                @NotNull Instruction instruction,
                @NotNull Map<Instruction, DiagnosticFactory> map
        ) {
            this.instruction = instruction;
            reportedDiagnosticMap = map;
            variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true, trace.getBindingContext());
        }
    }

    private class VariableInitContext extends VariableContext {
        final VariableInitState enterInitState;
        final VariableInitState exitInitState;

        private VariableInitContext(
                @NotNull Instruction instruction,
                @NotNull Map<Instruction, DiagnosticFactory> map,
                @NotNull Map<VariableDescriptor, VariableInitState> in,
                @NotNull Map<VariableDescriptor, VariableInitState> out
        ) {
            super(instruction, map);
            enterInitState = variableDescriptor != null ? in.get(variableDescriptor) : null;
            exitInitState = variableDescriptor != null ? out.get(variableDescriptor) : null;
        }
    }

    private class VariableUseContext extends VariableContext {
        final VariableUseState enterUseState;
        final VariableUseState exitUseState;


        private VariableUseContext(
                @NotNull Instruction instruction,
                @NotNull Map<Instruction, DiagnosticFactory> map,
                @NotNull Map<VariableDescriptor, VariableUseState> in,
                @NotNull Map<VariableDescriptor, VariableUseState> out
        ) {
            super(instruction, map);
            enterUseState = variableDescriptor != null ? in.get(variableDescriptor) : null;
            exitUseState = variableDescriptor != null ? out.get(variableDescriptor) : null;
        }
    }
}
