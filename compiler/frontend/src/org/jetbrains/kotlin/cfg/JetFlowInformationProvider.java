/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Function1;
import kotlin.Function3;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableInitState;
import org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableUseState;
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue;
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodePackage;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.JetElementInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.*;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.MarkInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineExitInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.PseudocodeTraverserPackage;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;

import java.util.*;

import static org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableUseState.*;
import static org.jetbrains.kotlin.cfg.TailRecursionKind.*;
import static org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.FORWARD;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.CAPTURED_IN_CLOSURE;
import static org.jetbrains.kotlin.resolve.BindingContext.TAIL_RECURSION_CALL;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getResolvedCall;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;

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

    public void checkForLocalClassOrObjectMode() {
        // Local classes and objects are analyzed twice: when TopDownAnalyzer processes it and as a part of its container.
        // Almost all checks can be done when the container is analyzed
        // except recording initialized variables (this information is needed for DeclarationChecker).
        recordInitializedVariables();
    }

    public void checkDeclaration() {

        recordInitializedVariables();

        checkLocalFunctions();

        markUninitializedVariables();

        markUnusedVariables();

        markStatements();

        markUnusedExpressions();

        markWhenWithoutElse();
    }

    public void checkFunction(@Nullable JetType expectedReturnType) {
        UnreachableCode unreachableCode = collectUnreachableCode();
        reportUnreachableCode(unreachableCode);

        if (subroutine instanceof JetFunctionLiteral) return;

        checkDefiniteReturn(expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE, unreachableCode);

        markTailCalls();
    }

    private void collectReturnExpressions(@NotNull final Collection<JetElement> returnedExpressions) {
        final Set<Instruction> instructions = Sets.newHashSet(pseudocode.getInstructions());
        SubroutineExitInstruction exitInstruction = pseudocode.getExitInstruction();
        for (Instruction previousInstruction : exitInstruction.getPreviousInstructions()) {
            previousInstruction.accept(new InstructionVisitor() {
                @Override
                public void visitReturnValue(@NotNull ReturnValueInstruction instruction) {
                    if (instructions.contains(instruction)) { //exclude non-local return expressions
                        returnedExpressions.add(instruction.getElement());
                    }
                }

                @Override
                public void visitReturnNoValue(@NotNull ReturnNoValueInstruction instruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.getElement());
                    }
                }


                @Override
                public void visitJump(@NotNull AbstractJumpInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitUnconditionalJump(@NotNull UnconditionalJumpInstruction instruction) {
                    redirectToPrevInstructions(instruction);
                }

                private void redirectToPrevInstructions(Instruction instruction) {
                    for (Instruction previousInstruction : instruction.getPreviousInstructions()) {
                        previousInstruction.accept(this);
                    }
                }

                @Override
                public void visitNondeterministicJump(@NotNull NondeterministicJumpInstruction instruction) {
                    redirectToPrevInstructions(instruction);
                }

                @Override
                public void visitMarkInstruction(@NotNull MarkInstruction instruction) {
                    redirectToPrevInstructions(instruction);
                }

                @Override
                public void visitInstruction(@NotNull Instruction instruction) {
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
            if (element instanceof JetDeclarationWithBody) {
                JetDeclarationWithBody localDeclaration = (JetDeclarationWithBody) element;

                CallableDescriptor functionDescriptor =
                        (CallableDescriptor) trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, localDeclaration);
                JetType expectedType = functionDescriptor != null ? functionDescriptor.getReturnType() : null;

                JetFlowInformationProvider providerForLocalDeclaration =
                        new JetFlowInformationProvider(localDeclaration, trace, localDeclarationInstruction.getBody());

                providerForLocalDeclaration.checkFunction(expectedType);
            }
        }
    }

    public void checkDefiniteReturn(final @NotNull JetType expectedReturnType, @NotNull final UnreachableCode unreachableCode) {
        assert subroutine instanceof JetDeclarationWithBody;
        JetDeclarationWithBody function = (JetDeclarationWithBody) subroutine;

        if (!function.hasBody()) return;

        List<JetElement> returnedExpressions = Lists.newArrayList();
        collectReturnExpressions(returnedExpressions);

        final boolean blockBody = function.hasBlockBody();

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
                public void visitJetElement(@NotNull JetElement element) {
                    if (!(element instanceof JetExpression || element instanceof JetWhenCondition)) return;

                    if (blockBody && !noExpectedType(expectedReturnType)
                            && !KotlinBuiltIns.isUnit(expectedReturnType)
                            && !unreachableCode.getElements().contains(element)) {
                        noReturnError[0] = true;
                    }
                }
            });
        }
        if (noReturnError[0]) {
            trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(function));
        }
    }

    private void reportUnreachableCode(@NotNull UnreachableCode unreachableCode) {
        for (JetElement element : unreachableCode.getElements()) {
            trace.report(UNREACHABLE_CODE.on(element, unreachableCode.getUnreachableTextRanges(element)));
            trace.record(BindingContext.UNREACHABLE_CODE, element, true);
        }
    }

    @NotNull
    private UnreachableCode collectUnreachableCode() {
        Set<JetElement> reachableElements = Sets.newHashSet();
        Set<JetElement> unreachableElements = Sets.newHashSet();
        for (Instruction instruction : pseudocode.getInstructionsIncludingDeadCode()) {
            if (!(instruction instanceof JetElementInstruction)
                    || instruction instanceof LoadUnitValueInstruction
                    || instruction instanceof MergeInstruction
                    || (instruction instanceof MagicInstruction && ((MagicInstruction) instruction).getSynthetic())) continue;

            JetElement element = ((JetElementInstruction) instruction).getElement();

            if (instruction instanceof JumpInstruction) {
                boolean isJumpElement = element instanceof JetBreakExpression
                                        || element instanceof JetContinueExpression
                                        || element instanceof JetReturnExpression
                                        || element instanceof JetThrowExpression;
                if (!isJumpElement) continue;
            }

            if (instruction.getDead()) {
                unreachableElements.add(element);
            }
            else {
                reachableElements.add(element);
            }
        }
        return new UnreachableCodeImpl(reachableElements, unreachableElements);
    }

////////////////////////////////////////////////////////////////////////////////
//  Uninitialized variables analysis

    public void markUninitializedVariables() {
        final Collection<VariableDescriptor> varWithUninitializedErrorGenerated = Sets.newHashSet();
        final Collection<VariableDescriptor> varWithValReassignErrorGenerated = Sets.newHashSet();
        final boolean processClassOrObject = subroutine instanceof JetClassOrObject;

        PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> initializers =
                pseudocodeVariablesData.getVariableInitializers();
        final Set<VariableDescriptor> declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, true);
        final LexicalScopeVariableInfo lexicalScopeVariableInfo = pseudocodeVariablesData.getLexicalScopeVariableInfo();

        final Map<Instruction, DiagnosticFactory<?>> reportedDiagnosticMap = Maps.newHashMap();

        PseudocodeTraverserPackage.traverse(
                pseudocode, FORWARD, initializers,
                new InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableInitState>>() {
                    @Override
                    public void execute(
                            @NotNull Instruction instruction,
                            @Nullable Map<VariableDescriptor, VariableInitState> in,
                            @Nullable Map<VariableDescriptor, VariableInitState> out
                    ) {
                        assert in != null && out != null;
                        VariableInitContext ctxt =
                                new VariableInitContext(instruction, reportedDiagnosticMap, in, out, lexicalScopeVariableInfo);
                        if (ctxt.variableDescriptor == null) return;
                        if (instruction instanceof ReadValueInstruction) {
                            ReadValueInstruction readValueInstruction = (ReadValueInstruction) instruction;
                            JetElement element = readValueInstruction.getElement();
                            boolean error = checkBackingField(ctxt, element);
                            if (!error &&
                                PseudocodeUtil.isThisOrNoDispatchReceiver(readValueInstruction, trace.getBindingContext()) &&
                                declaredVariables.contains(ctxt.variableDescriptor)) {
                                checkIsInitialized(ctxt, element, varWithUninitializedErrorGenerated);
                            }
                            return;
                        }
                        if (!(instruction instanceof WriteValueInstruction)) return;
                        WriteValueInstruction writeValueInstruction = (WriteValueInstruction) instruction;
                        JetElement element = writeValueInstruction.getlValue();
                        boolean error = checkBackingField(ctxt, element);
                        if (!(element instanceof JetExpression)) return;
                        if (!error) {
                            error = checkValReassignment(ctxt, (JetExpression) element, writeValueInstruction,
                                                         varWithValReassignErrorGenerated);
                        }
                        if (!error && processClassOrObject) {
                            error = checkAssignmentBeforeDeclaration(ctxt, (JetExpression) element);
                        }
                        if (!error && processClassOrObject) {
                            checkInitializationUsingBackingField(ctxt, (JetExpression) element);
                        }
                    }
                }
        );
    }

    public void recordInitializedVariables() {
        PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Pseudocode pseudocode = pseudocodeVariablesData.getPseudocode();
        Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> initializers =
                pseudocodeVariablesData.getVariableInitializers();
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
            @NotNull WriteValueInstruction writeValueInstruction,
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

            ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilPackage.getResolvedCall(expression, trace.getBindingContext());
            ReceiverValue receiverValue = ReceiverValue.IRRELEVANT_RECEIVER;
            if (resolvedCall != null) {
                receiverValue = ExpressionTypingUtils
                        .normalizeReceiverValueForVisibility(resolvedCall.getDispatchReceiver(), trace.getBindingContext());

            }
            if (Visibilities.isVisible(receiverValue, variableDescriptor, descriptor) && setterDescriptor != null
                    && !Visibilities.isVisible(receiverValue, setterDescriptor, descriptor)) {
                report(Errors.INVISIBLE_SETTER.on(expression, variableDescriptor, setterDescriptor.getVisibility(),
                                                  variableDescriptor.getContainingDeclaration()), ctxt);
                return true;
            }
        }
        boolean isThisOrNoDispatchReceiver =
                PseudocodeUtil.isThisOrNoDispatchReceiver(writeValueInstruction, trace.getBindingContext());
        if ((isInitializedNotHere || !hasBackingField || !isThisOrNoDispatchReceiver) && !variableDescriptor.isVar()) {
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
                    if (KotlinBuiltIns.isUnit(((FunctionDescriptor) descriptor).getReturnType())) {
                        hasReassignMethodReturningUnit = true;
                    }
                }
                if (descriptor == null) {
                    Collection<? extends DeclarationDescriptor> descriptors =
                            trace.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, operationReference);
                    if (descriptors != null) {
                        for (DeclarationDescriptor referenceDescriptor : descriptors) {
                            if (KotlinBuiltIns.isUnit(((FunctionDescriptor) referenceDescriptor).getReturnType())) {
                                hasReassignMethodReturningUnit = true;
                            }
                        }
                    }
                }
            }
            if (!hasReassignMethodReturningUnit) {
                if (!isThisOrNoDispatchReceiver || !varWithValReassignErrorGenerated.contains(variableDescriptor)) {
                    report(Errors.VAL_REASSIGNMENT.on(expression, variableDescriptor), ctxt);
                }
                if (isThisOrNoDispatchReceiver) {
                    // try to get rid of repeating VAL_REASSIGNMENT diagnostic only for vars with no receiver
                    // or when receiver is this
                    varWithValReassignErrorGenerated.add(variableDescriptor);
                }
                return true;
            }
        }
        return false;
    }

    private boolean checkAssignmentBeforeDeclaration(@NotNull VariableInitContext ctxt, @NotNull JetExpression expression) {
        if (!ctxt.enterInitState.isDeclared && !ctxt.exitInitState.isDeclared
                && !ctxt.enterInitState.isInitialized && ctxt.exitInitState.isInitialized) {
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
            PsiElement property = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
            assert property instanceof JetProperty;
            if (((PropertyDescriptor) variableDescriptor).getModality() == Modality.FINAL && ((JetProperty) property).getSetter() == null) {
                return false;
            }
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
        PsiElement property = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
        boolean insideSelfAccessors = PsiTreeUtil.isAncestor(property, element, false);
        if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor) &&
                // not to generate error in accessors of abstract properties, there is one: declared accessor of abstract property
                !insideSelfAccessors) {

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
        if ((containingDeclaration instanceof ClassDescriptor)
                && DescriptorUtils.isAncestor(containingDeclaration, declarationDescriptor, false)) {
            return false;
        }
        report(Errors.INACCESSIBLE_BACKING_FIELD.on(element), cxtx);
        return true;
    }

    private boolean isCorrectBackingFieldReference(
            @Nullable JetElement element,
            VariableContext ctxt,
            boolean[] error,
            boolean reportError
    ) {
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

    private void recordInitializedVariables(
            @NotNull Pseudocode pseudocode,
            @NotNull Map<Instruction, Edges<Map<VariableDescriptor, PseudocodeVariablesData.VariableInitState>>> initializersMap
    ) {
        Edges<Map<VariableDescriptor, VariableInitState>> initializers = initializersMap.get(pseudocode.getExitInstruction());
        Set<VariableDescriptor> declaredVariables = getPseudocodeVariablesData().getDeclaredVariables(pseudocode, false);
        for (VariableDescriptor variable : declaredVariables) {
            if (variable instanceof PropertyDescriptor) {
                PseudocodeVariablesData.VariableInitState variableInitState = initializers.getIncoming().get(variable);
                if (variableInitState != null && variableInitState.isInitialized) continue;
                trace.record(BindingContext.IS_UNINITIALIZED, (PropertyDescriptor) variable);
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////
//  "Unused variable" & "unused value" analyses

    public void markUnusedVariables() {
        final PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> variableStatusData =
                pseudocodeVariablesData.getVariableUseStatusData();
        final Map<Instruction, DiagnosticFactory<?>> reportedDiagnosticMap = Maps.newHashMap();
        InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableUseState>> variableStatusAnalyzeStrategy =
                new InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableUseState>>() {
                    @Override
                    public void execute(
                            @NotNull Instruction instruction,
                            @Nullable Map<VariableDescriptor, VariableUseState> in,
                            @Nullable Map<VariableDescriptor, VariableUseState> out
                    ) {

                        assert in != null && out != null;
                        VariableContext ctxt = new VariableUseContext(instruction, reportedDiagnosticMap, in, out);
                        Set<VariableDescriptor> declaredVariables =
                                pseudocodeVariablesData.getDeclaredVariables(instruction.getOwner(), false);
                        VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                                instruction, false, trace.getBindingContext());
                        if (variableDescriptor == null || !declaredVariables.contains(variableDescriptor)
                                || !ExpressionTypingUtils.isLocal(variableDescriptor.getContainingDeclaration(), variableDescriptor)) {
                            return;
                        }
                        PseudocodeVariablesData.VariableUseState variableUseState = in.get(variableDescriptor);
                        if (instruction instanceof WriteValueInstruction) {
                            if (trace.get(CAPTURED_IN_CLOSURE, variableDescriptor) != null) return;
                            JetElement element = ((WriteValueInstruction) instruction).getElement();
                            if (variableUseState != READ) {
                                if (element instanceof JetBinaryExpression &&
                                    ((JetBinaryExpression) element).getOperationToken() == JetTokens.EQ) {
                                    JetExpression right = ((JetBinaryExpression) element).getRight();
                                    if (right != null) {
                                        report(Errors.UNUSED_VALUE.on(right, right, variableDescriptor), ctxt);
                                    }
                                }
                                else if (element instanceof JetPostfixExpression) {
                                    IElementType operationToken =
                                            ((JetPostfixExpression) element).getOperationReference().getReferencedNameElementType();
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
                                    PsiElement owner = element.getParent().getParent();
                                    if (owner instanceof JetFunction) {
                                        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(trace.getBindingContext());
                                        boolean isMain = (owner instanceof JetNamedFunction) && mainFunctionDetector.isMain((JetNamedFunction) owner);
                                        if (owner instanceof JetFunctionLiteral) return;
                                        DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, owner);
                                        assert descriptor instanceof FunctionDescriptor : owner.getText();
                                        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                                        String functionName = functionDescriptor.getName().asString();
                                        if (isMain
                                            || functionDescriptor.getModality().isOverridable()
                                            || !functionDescriptor.getOverriddenDescriptors().isEmpty()
                                            || "get".equals(functionName) || "set".equals(functionName) || "propertyDelegated".equals(functionName)
                                                ) {
                                            return;
                                        }
                                        report(Errors.UNUSED_PARAMETER.on((JetParameter) element, variableDescriptor), ctxt);
                                    } else if (owner instanceof JetClass) {
                                        if (!((JetParameter) element).hasValOrVarNode() && !((JetClass) owner).isAnnotation()) {
                                            report(Errors.UNUSED_PARAMETER.on((JetParameter) element, variableDescriptor), ctxt);
                                        }
                                    }
                                }
                            }
                            else if (variableUseState == ONLY_WRITTEN_NEVER_READ && JetPsiUtil.isVariableNotParameterDeclaration(element)) {
                                report(Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.on((JetNamedDeclaration) element, variableDescriptor), ctxt);
                            }
                            else if (variableUseState == WRITTEN_AFTER_READ && element instanceof JetVariableDeclaration) {
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
        PseudocodeTraverserPackage.traverse(pseudocode, TraversalOrder.BACKWARD, variableStatusData, variableStatusAnalyzeStrategy);
    }

////////////////////////////////////////////////////////////////////////////////
//  "Unused expressions" in block

    public void markUnusedExpressions() {
        final Map<Instruction, DiagnosticFactory<?>> reportedDiagnosticMap = Maps.newHashMap();
        PseudocodeTraverserPackage.traverse(
                pseudocode, FORWARD, new JetFlowInformationProvider.FunctionVoid1<Instruction>() {
                    @Override
                    public void execute(@NotNull Instruction instruction) {
                        if (!(instruction instanceof JetElementInstruction)) return;

                        JetElement element = ((JetElementInstruction)instruction).getElement();
                        if (!(element instanceof JetExpression)) return;

                        if (BindingContextUtilPackage.isUsedAsStatement((JetExpression) element, trace.getBindingContext())
                                && PseudocodePackage.getSideEffectFree(instruction)) {
                            VariableContext ctxt = new VariableContext(instruction, reportedDiagnosticMap);
                            report(
                                    element instanceof JetFunctionLiteralExpression
                                        ? Errors.UNUSED_FUNCTION_LITERAL.on((JetFunctionLiteralExpression) element)
                                        : Errors.UNUSED_EXPRESSION.on(element),
                                    ctxt
                            );
                        }
                    }
                }
        );
    }

////////////////////////////////////////////////////////////////////////////////
// Statements

    public void markStatements() {
        PseudocodeTraverserPackage.traverse(
                pseudocode, FORWARD, new JetFlowInformationProvider.FunctionVoid1<Instruction>() {
                    @Override
                    public void execute(@NotNull Instruction instruction) {
                        PseudoValue value = instruction instanceof InstructionWithValue
                                            ? ((InstructionWithValue) instruction).getOutputValue()
                                            : null;
                        Pseudocode pseudocode = instruction.getOwner();
                        boolean isUsedAsExpression = !pseudocode.getUsages(value).isEmpty();
                        for (JetElement element : pseudocode.getValueElements(value)) {
                            trace.record(BindingContext.USED_AS_EXPRESSION, element, isUsedAsExpression);
                        }
                    }
                }
        );
    }

    public void markWhenWithoutElse() {
        PseudocodeTraverserPackage.traverse(
                pseudocode, FORWARD, new JetFlowInformationProvider.FunctionVoid1<Instruction>() {
                    @Override
                    public void execute(@NotNull Instruction instruction) {
                        PseudoValue value = instruction instanceof InstructionWithValue
                                            ? ((InstructionWithValue) instruction).getOutputValue()
                                            : null;
                        for (JetElement element : instruction.getOwner().getValueElements(value)) {
                            if (element instanceof JetWhenExpression) {
                                JetWhenExpression whenExpression = (JetWhenExpression) element;
                                if (whenExpression.getElseExpression() == null && WhenChecker.mustHaveElse(whenExpression, trace)) {
                                    trace.report(NO_ELSE_IN_WHEN.on(whenExpression));
                                }
                            }
                        }
                    }
                }
        );
    }

////////////////////////////////////////////////////////////////////////////////
// Tail calls

    public void markTailCalls() {
        final DeclarationDescriptor subroutineDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, subroutine);
        if (!(subroutineDescriptor instanceof FunctionDescriptor)) return;
        if (!KotlinBuiltIns.isTailRecursive(subroutineDescriptor)) return;

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
        PseudocodeTraverserPackage.traverse(
                pseudocode,
                FORWARD,
                new FunctionVoid1<Instruction>() {
                    public void execute(@NotNull Instruction instruction) {
                        if (!(instruction instanceof CallInstruction)) return;
                        CallInstruction callInstruction = (CallInstruction) instruction;

                        ResolvedCall<?> resolvedCall = getResolvedCall(callInstruction.getElement(), trace.getBindingContext());
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

                        boolean isTail = PseudocodeTraverserPackage.traverseFollowingInstructions(
                                callInstruction,
                                new HashSet<Instruction>(),
                                FORWARD,
                                new TailRecursionDetector(subroutine, callInstruction)
                        );

                        // A tail call is not allowed to change dispatch receiver
                        //   class C {
                        //       fun foo(other: C) {
                        //           other.foo(this) // not a tail call
                        //       }
                        //   }
                        boolean sameDispatchReceiver =
                                PseudocodeUtil.isThisOrNoDispatchReceiver(resolvedCall, trace.getBindingContext());

                        TailRecursionKind kind = isTail && sameDispatchReceiver ? TAIL_CALL : NON_TAIL;

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
        Map<Instruction, DiagnosticFactory<?>> previouslyReported = ctxt.reportedDiagnosticMap;
        previouslyReported.put(instruction, diagnostic.getFactory());

        boolean alreadyReported = false;
        boolean sameErrorForAllCopies = true;
        for (Instruction copy : instruction.getCopies()) {
            DiagnosticFactory<?> previouslyReportedErrorFactory = previouslyReported.get(copy);
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

    private static boolean mustBeReportedOnAllCopies(@NotNull DiagnosticFactory<?> diagnosticFactory) {
        return diagnosticFactory == UNUSED_VARIABLE
               || diagnosticFactory == UNUSED_PARAMETER
               || diagnosticFactory == UNUSED_CHANGED_VALUE;
    }


    private class VariableContext {
        final Map<Instruction, DiagnosticFactory<?>> reportedDiagnosticMap;
        final Instruction instruction;
        final VariableDescriptor variableDescriptor;

        private VariableContext(
                @NotNull Instruction instruction,
                @NotNull Map<Instruction, DiagnosticFactory<?>> map
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
                @NotNull Map<Instruction, DiagnosticFactory<?>> map,
                @NotNull Map<VariableDescriptor, VariableInitState> in,
                @NotNull Map<VariableDescriptor, VariableInitState> out,
                @NotNull LexicalScopeVariableInfo lexicalScopeVariableInfo
        ) {
            super(instruction, map);
            enterInitState = initialize(variableDescriptor, lexicalScopeVariableInfo, in);
            exitInitState = initialize(variableDescriptor, lexicalScopeVariableInfo, out);
        }

        private VariableInitState initialize(
                VariableDescriptor variableDescriptor,
                LexicalScopeVariableInfo lexicalScopeVariableInfo,
                Map<VariableDescriptor, VariableInitState> map
        ) {
            if (variableDescriptor == null) return null;
            VariableInitState state = map.get(variableDescriptor);
            if (state != null) return state;
            return PseudocodeVariablesData.getDefaultValueForInitializers(variableDescriptor, instruction, lexicalScopeVariableInfo);
        }
    }

    private class VariableUseContext extends VariableContext {
        final VariableUseState enterUseState;
        final VariableUseState exitUseState;


        private VariableUseContext(
                @NotNull Instruction instruction,
                @NotNull Map<Instruction, DiagnosticFactory<?>> map,
                @NotNull Map<VariableDescriptor, VariableUseState> in,
                @NotNull Map<VariableDescriptor, VariableUseState> out
        ) {
            super(instruction, map);
            enterUseState = variableDescriptor != null ? in.get(variableDescriptor) : null;
            exitUseState = variableDescriptor != null ? out.get(variableDescriptor) : null;
        }
    }

    //TODO after KT-4621 rewrite to Kotlin
    public abstract static class InstructionDataAnalyzeStrategy<D> implements Function3<Instruction, D, D, Unit> {
        @Override
        public Unit invoke(Instruction instruction, D enterData, D exitData) {
            execute(instruction, enterData, exitData);
            return Unit.INSTANCE$;
        }

        public abstract void execute(Instruction instruction, D enterData, D exitData);
    }

    public abstract static class FunctionVoid1<P> implements Function1<P, Unit> {
        @Override
        public Unit invoke(P p) {
            execute(p);
            return Unit.INSTANCE$;
        }

        public abstract void execute(P p);
    }
}
