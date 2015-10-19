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
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableControlFlowState;
import org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableUseState;
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue;
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtilsKt;
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
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.PseudocodeTraverserKt;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptorKt;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.ResolvedCallUtilKt;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KtType;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;

import java.util.*;

import static org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableUseState.*;
import static org.jetbrains.kotlin.cfg.TailRecursionKind.*;
import static org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.FORWARD;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.diagnostics.Errors.UNREACHABLE_CODE;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;

public class JetFlowInformationProvider {

    private final KtElement subroutine;
    private final Pseudocode pseudocode;
    private final BindingTrace trace;
    private PseudocodeVariablesData pseudocodeVariablesData;

    private JetFlowInformationProvider(
            @NotNull KtElement declaration,
            @NotNull BindingTrace trace,
            @NotNull Pseudocode pseudocode
    ) {
        this.subroutine = declaration;
        this.trace = trace;
        this.pseudocode = pseudocode;
    }

    public JetFlowInformationProvider(
            @NotNull KtElement declaration,
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

    public void checkFunction(@Nullable KtType expectedReturnType) {
        UnreachableCode unreachableCode = collectUnreachableCode();
        reportUnreachableCode(unreachableCode);

        if (subroutine instanceof KtFunctionLiteral) return;

        checkDefiniteReturn(expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE, unreachableCode);

        markTailCalls();
    }

    private void collectReturnExpressions(@NotNull final Collection<KtElement> returnedExpressions) {
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
            KtElement element = localDeclarationInstruction.getElement();
            if (element instanceof KtDeclarationWithBody) {
                KtDeclarationWithBody localDeclaration = (KtDeclarationWithBody) element;

                CallableDescriptor functionDescriptor =
                        (CallableDescriptor) trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, localDeclaration);
                KtType expectedType = functionDescriptor != null ? functionDescriptor.getReturnType() : null;

                JetFlowInformationProvider providerForLocalDeclaration =
                        new JetFlowInformationProvider(localDeclaration, trace, localDeclarationInstruction.getBody());

                providerForLocalDeclaration.checkFunction(expectedType);
            }
        }
    }

    public void checkDefiniteReturn(final @NotNull KtType expectedReturnType, @NotNull final UnreachableCode unreachableCode) {
        assert subroutine instanceof KtDeclarationWithBody;
        KtDeclarationWithBody function = (KtDeclarationWithBody) subroutine;

        if (!function.hasBody()) return;

        List<KtElement> returnedExpressions = Lists.newArrayList();
        collectReturnExpressions(returnedExpressions);

        final boolean blockBody = function.hasBlockBody();

        final boolean[] noReturnError = new boolean[] { false };
        for (KtElement returnedExpression : returnedExpressions) {
            returnedExpression.accept(new KtVisitorVoid() {
                @Override
                public void visitReturnExpression(@NotNull KtReturnExpression expression) {
                    if (!blockBody) {
                        trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression));
                    }
                }

                @Override
                public void visitJetElement(@NotNull KtElement element) {
                    if (!(element instanceof KtExpression || element instanceof KtWhenCondition)) return;

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
        for (KtElement element : unreachableCode.getElements()) {
            trace.report(UNREACHABLE_CODE.on(element, unreachableCode.getUnreachableTextRanges(element)));
            trace.record(BindingContext.UNREACHABLE_CODE, element, true);
        }
    }

    @NotNull
    private UnreachableCode collectUnreachableCode() {
        Set<KtElement> reachableElements = Sets.newHashSet();
        Set<KtElement> unreachableElements = Sets.newHashSet();
        for (Instruction instruction : pseudocode.getInstructionsIncludingDeadCode()) {
            if (!(instruction instanceof JetElementInstruction)
                    || instruction instanceof LoadUnitValueInstruction
                    || instruction instanceof MergeInstruction
                    || (instruction instanceof MagicInstruction && ((MagicInstruction) instruction).getSynthetic())) continue;

            KtElement element = ((JetElementInstruction) instruction).getElement();

            if (instruction instanceof JumpInstruction) {
                boolean isJumpElement = element instanceof KtBreakExpression
                                        || element instanceof KtContinueExpression
                                        || element instanceof KtReturnExpression
                                        || element instanceof KtThrowExpression;
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
        final boolean processClassOrObject = subroutine instanceof KtClassOrObject;

        PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Map<Instruction, Edges<Map<VariableDescriptor, VariableControlFlowState>>> initializers =
                pseudocodeVariablesData.getVariableInitializers();
        final Set<VariableDescriptor> declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, true);
        final LexicalScopeVariableInfo lexicalScopeVariableInfo = pseudocodeVariablesData.getLexicalScopeVariableInfo();

        final Map<Instruction, DiagnosticFactory<?>> reportedDiagnosticMap = Maps.newHashMap();

        PseudocodeTraverserKt.traverse(
                pseudocode, FORWARD, initializers,
                new InstructionDataAnalyzeStrategy<Map<VariableDescriptor, VariableControlFlowState>>() {
                    @Override
                    public void execute(
                            @NotNull Instruction instruction,
                            @Nullable Map<VariableDescriptor, VariableControlFlowState> in,
                            @Nullable Map<VariableDescriptor, VariableControlFlowState> out
                    ) {
                        assert in != null && out != null;
                        VariableInitContext ctxt =
                                new VariableInitContext(instruction, reportedDiagnosticMap, in, out, lexicalScopeVariableInfo);
                        if (ctxt.variableDescriptor == null) return;
                        if (instruction instanceof ReadValueInstruction) {
                            ReadValueInstruction readValueInstruction = (ReadValueInstruction) instruction;
                            KtElement element = readValueInstruction.getElement();
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
                        KtElement element = writeValueInstruction.getLValue();
                        boolean error = checkBackingField(ctxt, element);
                        if (!(element instanceof KtExpression)) return;
                        if (!error) {
                            error = checkValReassignment(ctxt, (KtExpression) element, writeValueInstruction,
                                                         varWithValReassignErrorGenerated);
                        }
                        if (!error && processClassOrObject) {
                            error = checkAssignmentBeforeDeclaration(ctxt, (KtExpression) element);
                        }
                        if (!error && processClassOrObject) {
                            checkInitializationUsingBackingField(ctxt, (KtExpression) element);
                        }
                    }
                }
        );
    }

    public void recordInitializedVariables() {
        PseudocodeVariablesData pseudocodeVariablesData = getPseudocodeVariablesData();
        Pseudocode pseudocode = pseudocodeVariablesData.getPseudocode();
        Map<Instruction, Edges<Map<VariableDescriptor, VariableControlFlowState>>> initializers =
                pseudocodeVariablesData.getVariableInitializers();
        recordInitializedVariables(pseudocode, initializers);
        for (LocalFunctionDeclarationInstruction instruction : pseudocode.getLocalDeclarations()) {
            recordInitializedVariables(instruction.getBody(), initializers);
        }
    }

    private void checkIsInitialized(
            @NotNull VariableInitContext ctxt,
            @NotNull KtElement element,
            @NotNull Collection<VariableDescriptor> varWithUninitializedErrorGenerated
    ) {
        if (!(element instanceof KtSimpleNameExpression)) return;


        boolean isDefinitelyInitialized = ctxt.exitInitState.definitelyInitialized();
        VariableDescriptor variableDescriptor = ctxt.variableDescriptor;
        if (variableDescriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            if (propertyDescriptor.isLateInit() || !trace.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                isDefinitelyInitialized = true;
            }
        }
        if (!isDefinitelyInitialized && !varWithUninitializedErrorGenerated.contains(variableDescriptor)) {
            if (!(variableDescriptor instanceof PropertyDescriptor)) {
                varWithUninitializedErrorGenerated.add(variableDescriptor);
            }
            if (variableDescriptor instanceof ValueParameterDescriptor) {
                report(Errors.UNINITIALIZED_PARAMETER.on((KtSimpleNameExpression) element,
                                                         (ValueParameterDescriptor) variableDescriptor), ctxt);
            }
            else {
                report(Errors.UNINITIALIZED_VARIABLE.on((KtSimpleNameExpression) element, variableDescriptor), ctxt);
            }
        }
    }

    private boolean checkValReassignment(
            @NotNull VariableInitContext ctxt,
            @NotNull KtExpression expression,
            @NotNull WriteValueInstruction writeValueInstruction,
            @NotNull Collection<VariableDescriptor> varWithValReassignErrorGenerated
    ) {
        VariableDescriptor variableDescriptor = ctxt.variableDescriptor;
        PropertyDescriptor propertyDescriptor = SyntheticFieldDescriptorKt.getReferencedProperty(variableDescriptor);
        if (KtPsiUtil.isBackingFieldReference(expression, variableDescriptor) && propertyDescriptor != null) {
            KtPropertyAccessor accessor = PsiTreeUtil.getParentOfType(expression, KtPropertyAccessor.class);
            if (accessor != null) {
                DeclarationDescriptor accessorDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, accessor);
                if (propertyDescriptor.getGetter() == accessorDescriptor) {
                    //val can be reassigned through backing field inside its own getter
                    return false;
                }
            }
        }

        boolean mayBeInitializedNotHere = ctxt.enterInitState.mayBeInitialized();
        boolean hasBackingField = true;
        if (variableDescriptor instanceof PropertyDescriptor) {
            hasBackingField = trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor);
        }
        if (variableDescriptor.isVar() && variableDescriptor instanceof PropertyDescriptor) {
            DeclarationDescriptor descriptor = BindingContextUtils.getEnclosingDescriptor(trace.getBindingContext(), expression);
            PropertySetterDescriptor setterDescriptor = ((PropertyDescriptor) variableDescriptor).getSetter();

            ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilKt.getResolvedCall(expression, trace.getBindingContext());
            ReceiverValue receiverValue = ReceiverValue.IRRELEVANT_RECEIVER;
            if (resolvedCall != null) {
                receiverValue = ExpressionTypingUtils
                        .normalizeReceiverValueForVisibility(resolvedCall.getDispatchReceiver(), trace.getBindingContext());

            }
            if (Visibilities.isVisible(receiverValue, variableDescriptor, descriptor) && setterDescriptor != null
                    && !Visibilities.isVisible(receiverValue, setterDescriptor, descriptor)) {
                report(Errors.INVISIBLE_SETTER.on(expression, variableDescriptor, setterDescriptor.getVisibility(),
                                                  setterDescriptor), ctxt);
                return true;
            }
        }
        boolean isThisOrNoDispatchReceiver =
                PseudocodeUtil.isThisOrNoDispatchReceiver(writeValueInstruction, trace.getBindingContext());
        if ((mayBeInitializedNotHere || !hasBackingField || !isThisOrNoDispatchReceiver) && !variableDescriptor.isVar()) {
            boolean hasReassignMethodReturningUnit = false;
            KtSimpleNameExpression operationReference = null;
            PsiElement parent = expression.getParent();
            if (parent instanceof KtBinaryExpression) {
                operationReference = ((KtBinaryExpression) parent).getOperationReference();
            }
            else if (parent instanceof KtUnaryExpression) {
                operationReference = ((KtUnaryExpression) parent).getOperationReference();
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

    private boolean checkAssignmentBeforeDeclaration(@NotNull VariableInitContext ctxt, @NotNull KtExpression expression) {
        if (!ctxt.enterInitState.isDeclared && !ctxt.exitInitState.isDeclared
            && !ctxt.enterInitState.mayBeInitialized() && ctxt.exitInitState.mayBeInitialized()) {
            report(Errors.INITIALIZATION_BEFORE_DECLARATION.on(expression, ctxt.variableDescriptor), ctxt);
            return true;
        }
        return false;
    }

    private boolean checkInitializationUsingBackingField(@NotNull VariableInitContext ctxt, @NotNull KtExpression expression) {
        VariableDescriptor variableDescriptor = ctxt.variableDescriptor;
        if (variableDescriptor instanceof PropertyDescriptor
            && !ctxt.enterInitState.mayBeInitialized()
            && ctxt.exitInitState.mayBeInitialized()) {
            if (!variableDescriptor.isVar()) return false;
            if (!trace.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) variableDescriptor)) return false;
            PsiElement property = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
            assert property instanceof KtProperty;
            KtPropertyAccessor setter = ((KtProperty) property).getSetter();
            if (((PropertyDescriptor) variableDescriptor).getModality() == Modality.FINAL && (setter == null || !setter.hasBody())) {
                return false;
            }
            KtExpression variable = expression;
            if (expression instanceof KtDotQualifiedExpression) {
                if (((KtDotQualifiedExpression) expression).getReceiverExpression() instanceof KtThisExpression) {
                    variable = ((KtDotQualifiedExpression) expression).getSelectorExpression();
                }
            }
            if (variable instanceof KtSimpleNameExpression) {
                KtSimpleNameExpression simpleNameExpression = (KtSimpleNameExpression) variable;
                if (simpleNameExpression.getReferencedNameElementType() != KtTokens.FIELD_IDENTIFIER) {
                    trace.record(IS_UNINITIALIZED, (PropertyDescriptor) variableDescriptor);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkBackingField(@NotNull VariableContext cxtx, @NotNull KtElement element) {
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
            if (element instanceof KtSimpleNameExpression) {
                report(Errors.BACKING_FIELD_USAGE_FORBIDDEN.on((KtSimpleNameExpression) element), cxtx);
            }
            return false;
        }
        report(Errors.INACCESSIBLE_BACKING_FIELD.on(element), cxtx);
        return true;
    }

    private boolean isCorrectBackingFieldReference(
            @Nullable KtElement element,
            VariableContext ctxt,
            boolean[] error,
            boolean reportError
    ) {
        error[0] = false;
        if (KtPsiUtil.isBackingFieldReference(element, null)) {
            return true;
        }
        if (element instanceof KtDotQualifiedExpression && isCorrectBackingFieldReference(
                ((KtDotQualifiedExpression) element).getSelectorExpression(), ctxt, error, false)) {
            if (((KtDotQualifiedExpression) element).getReceiverExpression() instanceof KtThisExpression) {
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
            @NotNull Map<Instruction, Edges<Map<VariableDescriptor, VariableControlFlowState>>> initializersMap
    ) {
        Edges<Map<VariableDescriptor, VariableControlFlowState>> initializers = initializersMap.get(pseudocode.getExitInstruction());
        if (initializers == null) return;
        Set<VariableDescriptor> declaredVariables = getPseudocodeVariablesData().getDeclaredVariables(pseudocode, false);
        for (VariableDescriptor variable : declaredVariables) {
            if (variable instanceof PropertyDescriptor) {
                VariableControlFlowState variableControlFlowState = initializers.getIncoming().get(variable);
                if (variableControlFlowState != null && variableControlFlowState.definitelyInitialized()) continue;
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
                            KtElement element = ((WriteValueInstruction) instruction).getElement();
                            if (variableUseState != READ) {
                                if (element instanceof KtBinaryExpression &&
                                    ((KtBinaryExpression) element).getOperationToken() == KtTokens.EQ) {
                                    KtExpression right = ((KtBinaryExpression) element).getRight();
                                    if (right != null) {
                                        report(Errors.UNUSED_VALUE.on((KtBinaryExpression) element, right, variableDescriptor), ctxt);
                                    }
                                }
                                else if (element instanceof KtPostfixExpression) {
                                    IElementType operationToken =
                                            ((KtPostfixExpression) element).getOperationReference().getReferencedNameElementType();
                                    if (operationToken == KtTokens.PLUSPLUS || operationToken == KtTokens.MINUSMINUS) {
                                        report(Errors.UNUSED_CHANGED_VALUE.on(element, element), ctxt);
                                    }
                                }
                            }
                        }
                        else if (instruction instanceof VariableDeclarationInstruction) {
                            KtDeclaration element = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
                            if (!(element instanceof KtNamedDeclaration)) return;
                            PsiElement nameIdentifier = ((KtNamedDeclaration) element).getNameIdentifier();
                            if (nameIdentifier == null) return;
                            if (!VariableUseState.isUsed(variableUseState)) {
                                if (KtPsiUtil.isVariableNotParameterDeclaration(element)) {
                                    report(Errors.UNUSED_VARIABLE.on((KtNamedDeclaration) element, variableDescriptor), ctxt);
                                }
                                else if (element instanceof KtParameter) {
                                    PsiElement owner = element.getParent().getParent();
                                    if (owner instanceof KtPrimaryConstructor) {
                                        if (!((KtParameter) element).hasValOrVar()) {
                                            KtClassOrObject containingClass = ((KtPrimaryConstructor) owner).getContainingClassOrObject();
                                            DeclarationDescriptor containingClassDescriptor = trace.get(
                                                    BindingContext.DECLARATION_TO_DESCRIPTOR, containingClass
                                            );
                                            if (!DescriptorUtils.isAnnotationClass(containingClassDescriptor)) {
                                                report(Errors.UNUSED_PARAMETER.on((KtParameter) element, variableDescriptor), ctxt);
                                            }
                                        }
                                    }
                                    else if (owner instanceof KtFunction) {
                                        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(trace.getBindingContext());
                                        boolean isMain = (owner instanceof KtNamedFunction) && mainFunctionDetector.isMain((KtNamedFunction) owner);
                                        if (owner instanceof KtFunctionLiteral) return;
                                        DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, owner);
                                        assert descriptor instanceof FunctionDescriptor : owner.getText();
                                        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                                        String functionName = functionDescriptor.getName().asString();
                                        if (isMain
                                            || functionDescriptor.getModality().isOverridable()
                                            || !functionDescriptor.getOverriddenDescriptors().isEmpty()
                                            || "getValue".equals(functionName) || "setValue".equals(functionName)
                                            || "propertyDelegated".equals(functionName)
                                                ) {
                                            return;
                                        }
                                        report(Errors.UNUSED_PARAMETER.on((KtParameter) element, variableDescriptor), ctxt);
                                    }
                                }
                            }
                            else if (variableUseState == ONLY_WRITTEN_NEVER_READ && KtPsiUtil.isVariableNotParameterDeclaration(element)) {
                                report(Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.on((KtNamedDeclaration) element, variableDescriptor), ctxt);
                            }
                            else if (variableUseState == WRITTEN_AFTER_READ && element instanceof KtVariableDeclaration) {
                                if (element instanceof KtProperty) {
                                    KtExpression initializer = ((KtProperty) element).getInitializer();
                                    if (initializer != null) {
                                        report(Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER.on(initializer, variableDescriptor), ctxt);
                                    }
                                }
                                else if (element instanceof KtMultiDeclarationEntry) {
                                    report(VARIABLE_WITH_REDUNDANT_INITIALIZER.on(element, variableDescriptor), ctxt);
                                }
                            }
                        }
                    }
                };
        PseudocodeTraverserKt.traverse(pseudocode, TraversalOrder.BACKWARD, variableStatusData, variableStatusAnalyzeStrategy);
    }

////////////////////////////////////////////////////////////////////////////////
//  "Unused expressions" in block

    public void markUnusedExpressions() {
        final Map<Instruction, DiagnosticFactory<?>> reportedDiagnosticMap = Maps.newHashMap();
        PseudocodeTraverserKt.traverse(
                pseudocode, FORWARD, new JetFlowInformationProvider.FunctionVoid1<Instruction>() {
                    @Override
                    public void execute(@NotNull Instruction instruction) {
                        if (!(instruction instanceof JetElementInstruction)) return;

                        KtElement element = ((JetElementInstruction)instruction).getElement();
                        if (!(element instanceof KtExpression)) return;

                        if (BindingContextUtilsKt.isUsedAsStatement((KtExpression) element, trace.getBindingContext())
                            && PseudocodeUtilsKt.getSideEffectFree(instruction)) {
                            VariableContext ctxt = new VariableContext(instruction, reportedDiagnosticMap);
                            report(
                                    element instanceof KtFunctionLiteralExpression
                                        ? Errors.UNUSED_FUNCTION_LITERAL.on((KtFunctionLiteralExpression) element)
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
        PseudocodeTraverserKt.traverse(
                pseudocode, FORWARD, new JetFlowInformationProvider.FunctionVoid1<Instruction>() {
                    @Override
                    public void execute(@NotNull Instruction instruction) {
                        PseudoValue value = instruction instanceof InstructionWithValue
                                            ? ((InstructionWithValue) instruction).getOutputValue()
                                            : null;
                        Pseudocode pseudocode = instruction.getOwner();
                        boolean isUsedAsExpression = !pseudocode.getUsages(value).isEmpty();
                        for (KtElement element : pseudocode.getValueElements(value)) {
                            trace.record(BindingContext.USED_AS_EXPRESSION, element, isUsedAsExpression);
                        }
                    }
                }
        );
    }

    public void markWhenWithoutElse() {
        PseudocodeTraverserKt.traverse(
                pseudocode, FORWARD, new JetFlowInformationProvider.FunctionVoid1<Instruction>() {
                    @Override
                    public void execute(@NotNull Instruction instruction) {
                        PseudoValue value = instruction instanceof InstructionWithValue
                                            ? ((InstructionWithValue) instruction).getOutputValue()
                                            : null;
                        for (KtElement element : instruction.getOwner().getValueElements(value)) {
                            if (!(element instanceof KtWhenExpression)) continue;
                            KtWhenExpression whenExpression = (KtWhenExpression) element;
                            if (whenExpression.getElseExpression() != null) continue;

                            if (WhenChecker.mustHaveElse(whenExpression, trace)) {
                                trace.report(NO_ELSE_IN_WHEN.on(whenExpression));
                            }
                            else if (whenExpression.getSubjectExpression() != null) {
                                ClassDescriptor enumClassDescriptor = WhenChecker.getClassDescriptorOfTypeIfEnum(
                                        trace.getType(whenExpression.getSubjectExpression()));
                                if (enumClassDescriptor != null
                                    && !WhenChecker.isWhenOnEnumExhaustive(whenExpression, trace, enumClassDescriptor)) {
                                    trace.report(NON_EXHAUSTIVE_WHEN.on(whenExpression));
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
        if (!((FunctionDescriptor) subroutineDescriptor).isTailrec()) return;

        // finally blocks are copied which leads to multiple diagnostics reported on one instruction
        class KindAndCall {
            TailRecursionKind kind;
            ResolvedCall<?> call;

            KindAndCall(TailRecursionKind kind, ResolvedCall<?> call) {
                this.kind = kind;
                this.call = call;
            }
        }
        final Map<KtElement, KindAndCall> calls = new HashMap<KtElement, KindAndCall>();
        PseudocodeTraverserKt.traverse(
                pseudocode,
                FORWARD,
                new FunctionVoid1<Instruction>() {
                    public void execute(@NotNull Instruction instruction) {
                        if (!(instruction instanceof CallInstruction)) return;
                        CallInstruction callInstruction = (CallInstruction) instruction;

                        ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(callInstruction.getElement(), trace.getBindingContext());
                        if (resolvedCall == null) return;

                        // is this a recursive call?
                        CallableDescriptor functionDescriptor = resolvedCall.getResultingDescriptor();
                        if (!functionDescriptor.getOriginal().equals(subroutineDescriptor)) return;

                        KtElement element = callInstruction.getElement();
                        //noinspection unchecked
                        KtExpression parent = PsiTreeUtil.getParentOfType(
                                element,
                                KtTryExpression.class, KtFunction.class, KtClassInitializer.class
                        );

                        if (parent instanceof KtTryExpression) {
                            // We do not support tail calls Collections.singletonMap() try-catch-finally, for simplicity of the mental model
                            // very few cases there would be real tail-calls, and it's often not so easy for the user to see why
                            calls.put(element, new KindAndCall(IN_TRY, resolvedCall));
                            return;
                        }

                        boolean isTail = PseudocodeTraverserKt.traverseFollowingInstructions(
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
                                ResolvedCallUtilKt.hasThisOrNoDispatchReceiver(resolvedCall, trace.getBindingContext());

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
        for (Map.Entry<KtElement, KindAndCall> entry : calls.entrySet()) {
            KtElement element = entry.getKey();
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
            trace.report(Errors.NO_TAIL_CALLS_FOUND.on((KtNamedFunction) subroutine));
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
        final VariableControlFlowState enterInitState;
        final VariableControlFlowState exitInitState;

        private VariableInitContext(
                @NotNull Instruction instruction,
                @NotNull Map<Instruction, DiagnosticFactory<?>> map,
                @NotNull Map<VariableDescriptor, VariableControlFlowState> in,
                @NotNull Map<VariableDescriptor, VariableControlFlowState> out,
                @NotNull LexicalScopeVariableInfo lexicalScopeVariableInfo
        ) {
            super(instruction, map);
            enterInitState = initialize(variableDescriptor, lexicalScopeVariableInfo, in);
            exitInitState = initialize(variableDescriptor, lexicalScopeVariableInfo, out);
        }

        private VariableControlFlowState initialize(
                VariableDescriptor variableDescriptor,
                LexicalScopeVariableInfo lexicalScopeVariableInfo,
                Map<VariableDescriptor, VariableControlFlowState> map
        ) {
            if (variableDescriptor == null) return null;
            VariableControlFlowState state = map.get(variableDescriptor);
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
