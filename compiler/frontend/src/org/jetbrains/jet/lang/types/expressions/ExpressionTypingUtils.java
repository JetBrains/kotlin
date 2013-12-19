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

package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.types.TypeUtils.noExpectedType;

public class ExpressionTypingUtils {

    private ExpressionTypingUtils() {
    }

    @Nullable
    protected static ExpressionReceiver getExpressionReceiver(@NotNull JetExpression expression, @Nullable JetType type) {
        if (type == null) return null;
        return new ExpressionReceiver(expression, type);
    }

    @Nullable
    protected static ExpressionReceiver getExpressionReceiver(@NotNull ExpressionTypingFacade facade, @NotNull JetExpression expression, ExpressionTypingContext context) {
        return getExpressionReceiver(expression, facade.getTypeInfo(expression, context).getType());
    }

    @NotNull
    protected static ExpressionReceiver safeGetExpressionReceiver(@NotNull ExpressionTypingFacade facade, @NotNull JetExpression expression, ExpressionTypingContext context) {
        JetType type = facade.safeGetTypeInfo(expression, context).getType();
        assert type != null : "safeGetTypeInfo should return @NotNull type";
        return new ExpressionReceiver(expression, type);
    }

    @NotNull
    public static WritableScopeImpl newWritableScopeImpl(ExpressionTypingContext context, @NotNull String scopeDebugName) {
        WritableScopeImpl scope = new WritableScopeImpl(
                context.scope, context.scope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(context.trace), scopeDebugName);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return scope;
    }

    public static boolean isBoolean(@NotNull JetType type) {
        return JetTypeChecker.INSTANCE.isSubtypeOf(type, KotlinBuiltIns.getInstance().getBooleanType());
    }

    public static boolean ensureBooleanResult(JetExpression operationSign, Name name, JetType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    public static boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, ExpressionTypingContext context) {
        if (resultType != null) {
            // TODO : Relax?
            if (!isBoolean(resultType)) {
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, KotlinBuiltIns.getInstance().getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static JetType getDefaultType(IElementType constantType) {
        if (constantType == JetNodeTypes.INTEGER_CONSTANT) {
            return KotlinBuiltIns.getInstance().getIntType();
        }
        else if (constantType == JetNodeTypes.FLOAT_CONSTANT) {
            return KotlinBuiltIns.getInstance().getDoubleType();
        }
        else if (constantType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return KotlinBuiltIns.getInstance().getBooleanType();
        }
        else if (constantType == JetNodeTypes.CHARACTER_CONSTANT) {
            return KotlinBuiltIns.getInstance().getCharType();
        }
        else if (constantType == JetNodeTypes.NULL) {
            return KotlinBuiltIns.getInstance().getNullableNothingType();
        }
        else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    private static boolean isCapturedInInline(
            @NotNull BindingContext context,
            @NotNull DeclarationDescriptor scopeContainer,
            @NotNull DeclarationDescriptor variableParent
    ) {
        PsiElement scopeDeclaration = BindingContextUtils.descriptorToDeclaration(context, scopeContainer);
        if (!(scopeDeclaration instanceof JetFunctionLiteral)) {
            return false;
        }

        PsiElement parent = scopeDeclaration.getParent();
        assert parent instanceof JetFunctionLiteralExpression : "parent of JetFunctionLiteral is " + parent;
        JetCallExpression callExpression = getCallExpression((JetFunctionLiteralExpression) parent);
        if (callExpression == null) {
            return false;
        }

        ResolvedCall<? extends CallableDescriptor> call = context.get(BindingContext.RESOLVED_CALL, callExpression.getCalleeExpression());
        if (call == null) {
            return false;
        }

        CallableDescriptor callable = call.getResultingDescriptor();
        if (callable instanceof SimpleFunctionDescriptor && ((SimpleFunctionDescriptor) callable).isInline()) {
            DeclarationDescriptor scopeContainerParent = scopeContainer.getContainingDeclaration();
            assert scopeContainerParent != null : "parent is null for " + scopeContainer;
            return scopeContainerParent == variableParent || isCapturedInInline(context, scopeContainerParent, variableParent);
        }
        else {
            return false;
        }
    }

    @Nullable
    private static JetCallExpression getCallExpression(@NotNull JetFunctionLiteralExpression functionLiteralExpression) {
        PsiElement parent = functionLiteralExpression.getParent();
        if (parent instanceof JetValueArgument) {
            // foo({ ... })    or     foo(f = { ... })

            PsiElement valueArgumentList = parent.getParent();
            assert valueArgumentList instanceof JetValueArgumentList : "parent of value argument is " + valueArgumentList;

            if (valueArgumentList.getParent() instanceof JetCallExpression) { // may be argument list of annotation
                return (JetCallExpression) valueArgumentList.getParent();
            }
        }
        else if (parent instanceof JetCallExpression) {
            // foo { ... }

            return  (JetCallExpression) parent;
        }
        return null;
    }
    public static void checkCapturingInClosure(JetSimpleNameExpression expression, BindingTrace trace, JetScope scope) {
        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), expression, true);
        if (variable != null) {
            DeclarationDescriptor variableParent = variable.getContainingDeclaration();
            DeclarationDescriptor scopeContainer = scope.getContainingDeclaration();
            if (scopeContainer != variableParent && variableParent instanceof CallableDescriptor) {
                if (trace.get(CAPTURED_IN_CLOSURE, variable) != CaptureKind.NOT_INLINE) {
                    boolean inline = isCapturedInInline(trace.getBindingContext(), scopeContainer, variableParent);
                    trace.record(CAPTURED_IN_CLOSURE, variable, inline ? CaptureKind.INLINE_ONLY : CaptureKind.NOT_INLINE);
                }
            }
        }
    }

    public static boolean isVariableIterable(@NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull Project project, @NotNull VariableDescriptor variableDescriptor, @NotNull JetScope scope) {
        JetExpression expression = JetPsiFactory.createExpression(project, "fake");
        ExpressionReceiver expressionReceiver = new ExpressionReceiver(expression, variableDescriptor.getType());
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices,
                new BindingTraceContext(),
                scope,
                DataFlowInfo.EMPTY,
                TypeUtils.NO_EXPECTED_TYPE
        );
        return ControlStructureTypingVisitor.checkIterableConvention(expressionReceiver, context) != null;
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     *
     * @param callableFQN
     * @param project
     * @param scope
     * @return
     */
    public static List<CallableDescriptor> canFindSuitableCall(
            @NotNull FqName callableFQN,
            @NotNull Project project,
            @NotNull JetExpression receiverExpression,
            @NotNull JetType receiverType,
            @NotNull JetScope scope,
            @NotNull ModuleDescriptor module
    ) {
        JetImportDirective importDirective = JetPsiFactory.createImportDirective(project, callableFQN.asString());

        Collection<? extends DeclarationDescriptor> declarationDescriptors = new QualifiedExpressionResolver()
                .analyseImportReference(importDirective, scope, new BindingTraceContext(), module);

        List<CallableDescriptor> callableExtensionDescriptors = new ArrayList<CallableDescriptor>();
        ReceiverValue receiverValue = new ExpressionReceiver(receiverExpression, receiverType);

        for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
            if (declarationDescriptor instanceof CallableDescriptor) {
                CallableDescriptor callableDescriptor = (CallableDescriptor) declarationDescriptor;

                if (checkIsExtensionCallable(receiverValue, callableDescriptor)) {
                    callableExtensionDescriptors.add(callableDescriptor);
                }
            }
        }

        return callableExtensionDescriptors;
    }

    /*
    * Checks if receiver declaration could be resolved to call expected receiver.
    */
    public static boolean checkIsExtensionCallable (
            @NotNull ReceiverValue receiverArgument,
            @NotNull CallableDescriptor callableDescriptor
    ) {
        JetType type = receiverArgument.getType();

        if (type instanceof NamespaceType) {
            // This fake class ruins standard algorithms
            return false;
        }

        if (checkReceiverResolution(receiverArgument, type, callableDescriptor)) return true;
        if (type.isNullable()) {
            JetType notNullableType = TypeUtils.makeNotNullable(type);
            if (checkReceiverResolution(receiverArgument, notNullableType, callableDescriptor)) return true;
        }
        return false;
    }

    private static boolean checkReceiverResolution (
            @NotNull ReceiverValue receiverArgument,
            @NotNull JetType receiverType,
            @NotNull CallableDescriptor callableDescriptor
    ) {
        ReceiverParameterDescriptor receiverParameter = callableDescriptor.getReceiverParameter();

        if (!receiverArgument.exists() && receiverParameter == null) {
            // Both receivers do not exist
            return true;
        }

        if (!(receiverArgument.exists() && receiverParameter != null)) {
            return false;
        }

        Set<Name> typeNamesInReceiver = collectUsedTypeNames(receiverParameter.getType());

        ConstraintSystem constraintSystem = new ConstraintSystemImpl();
        Map<TypeParameterDescriptor, Variance> typeVariables = Maps.newLinkedHashMap();
        for (TypeParameterDescriptor typeParameterDescriptor : callableDescriptor.getTypeParameters()) {
            if (typeNamesInReceiver.contains(typeParameterDescriptor.getName())) {
                typeVariables.put(typeParameterDescriptor, Variance.INVARIANT);
            }
        }
        constraintSystem.registerTypeVariables(typeVariables);

        constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), ConstraintPosition.RECEIVER_POSITION);
        return constraintSystem.getStatus().isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, true);
    }

    private static Set<Name> collectUsedTypeNames(@NotNull JetType jetType) {
        Set<Name> typeNames = new HashSet<Name>();

        ClassifierDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (descriptor != null) {
            typeNames.add(descriptor.getName());
        }

        for (TypeProjection argument : jetType.getArguments()) {
            typeNames.addAll(collectUsedTypeNames(argument.getType()));
        }

        return typeNames;
    }

    @NotNull
    public static OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(
            @NotNull ExpressionTypingContext context,
            @NotNull ReceiverValue receiver,
            @NotNull Name name,
            @NotNull JetType... argumentTypes
    ) {
        TemporaryBindingTrace traceWithFakeArgumentInfo = TemporaryBindingTrace.create(context.trace, "trace to store fake argument for",
                                                                                       name);
        List<JetExpression> fakeArguments = Lists.newArrayList();
        for (JetType type : argumentTypes) {
            fakeArguments.add(createFakeExpressionOfType(context.expressionTypingServices.getProject(), traceWithFakeArgumentInfo,
                                                         "fakeArgument" + fakeArguments.size(), type));
        }
        return makeAndResolveFakeCall(receiver, context.replaceBindingTrace(traceWithFakeArgumentInfo), fakeArguments, name).getSecond();
    }

    public static JetExpression createFakeExpressionOfType(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull String argumentName,
            @NotNull JetType argumentType
    ) {
        JetExpression fakeExpression = JetPsiFactory.createExpression(project, argumentName);
        trace.record(EXPRESSION_TYPE, fakeExpression, argumentType);
        trace.record(PROCESSED, fakeExpression);
        return fakeExpression;
    }

    @NotNull
    public static OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(
            @NotNull ExpressionTypingContext context,
            @NotNull ReceiverValue receiver,
            @NotNull Name name
    ) {
        return resolveFakeCall(receiver, context, Collections.<JetExpression>emptyList(), name);
    }

    @NotNull
    public static OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(
            @NotNull ReceiverValue receiver,
            @NotNull ExpressionTypingContext context,
            @NotNull List<JetExpression> valueArguments,
            @NotNull Name name
    ) {
        return makeAndResolveFakeCall(receiver, context, valueArguments, name).getSecond();
    }

    @NotNull
    public static Pair<Call, OverloadResolutionResults<FunctionDescriptor>> makeAndResolveFakeCall(
            @NotNull ReceiverValue receiver,
            @NotNull ExpressionTypingContext context,
            @NotNull List<JetExpression> valueArguments,
            @NotNull Name name
    ) {
        final JetReferenceExpression fake = JetPsiFactory.createSimpleName(context.expressionTypingServices.getProject(), "fake");
        TemporaryBindingTrace fakeTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve fake call for", name);
        Call call = CallMaker.makeCallWithExpressions(fake, receiver, null, fake, valueArguments);
        OverloadResolutionResults<FunctionDescriptor> results =
                context.replaceBindingTrace(fakeTrace).resolveCallWithGivenName(call, fake, name);
        if (results.isSuccess()) {
            fakeTrace.commit(new TraceEntryFilter() {
                @Override
                public boolean accept(@Nullable WritableSlice<?, ?> slice, Object key) {
                    // excluding all entries related to fake expression
                    return key != fake;
                }
            }, true);
        }
        return Pair.create(call, results);
    }

    public static void defineLocalVariablesFromMultiDeclaration(
            @NotNull WritableScope writableScope,
            @NotNull JetMultiDeclaration multiDeclaration,
            @NotNull ReceiverValue receiver,
            @NotNull JetExpression reportErrorsOn,
            @NotNull ExpressionTypingContext context
    ) {
        int componentIndex = 1;
        for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
            Name componentName = Name.identifier(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX + componentIndex);
            componentIndex++;

            JetType expectedType = getExpectedTypeForComponent(context, entry);
            OverloadResolutionResults<FunctionDescriptor> results =
                    resolveFakeCall(context.replaceExpectedType(expectedType), receiver, componentName);

            JetType componentType = null;
            if (results.isSuccess()) {
                context.trace.record(COMPONENT_RESOLVED_CALL, entry, results.getResultingCall());
                componentType = results.getResultingDescriptor().getReturnType();
                if (componentType != null && !noExpectedType(expectedType)
                       && !JetTypeChecker.INSTANCE.isSubtypeOf(componentType, expectedType)) {

                    context.trace.report(
                            COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.on(reportErrorsOn, componentName, componentType, expectedType));
                }
            }
            else if (results.isAmbiguity()) {
                context.trace.report(COMPONENT_FUNCTION_AMBIGUITY.on(reportErrorsOn, componentName, results.getResultingCalls()));
            }
            else {
                context.trace.report(COMPONENT_FUNCTION_MISSING.on(reportErrorsOn, componentName, receiver.getType()));
            }
            if (componentType == null) {
                componentType = ErrorUtils.createErrorType(componentName + "() return type");
            }
            VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver().
                resolveLocalVariableDescriptorWithType(writableScope, entry, componentType, context.trace);

            VariableDescriptor olderVariable = writableScope.getLocalVariable(variableDescriptor.getName());
            checkVariableShadowing(context, variableDescriptor, olderVariable);

            writableScope.addVariableDescriptor(variableDescriptor);
        }
    }

    public static void checkVariableShadowing(@NotNull ExpressionTypingContext context, @NotNull VariableDescriptor variableDescriptor, VariableDescriptor oldDescriptor) {
        if (oldDescriptor != null && DescriptorUtils.isLocal(variableDescriptor.getContainingDeclaration(), oldDescriptor)) {
            PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), variableDescriptor);
            if (declaration != null) {
                context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
            }
        }
    }

    @NotNull
    private static JetType getExpectedTypeForComponent(ExpressionTypingContext context, JetMultiDeclarationEntry entry) {
        JetTypeReference entryTypeRef = entry.getTypeRef();
        if (entryTypeRef != null) {
            return context.expressionTypingServices.getTypeResolver().resolveType(context.scope, entryTypeRef, context.trace, true);
        }
        else {
            return TypeUtils.NO_EXPECTED_TYPE;
        }
    }

    public static ObservableBindingTrace makeTraceInterceptingTypeMismatch(@NotNull BindingTrace trace, @NotNull final JetElement expressionToWatch, @NotNull final boolean[] mismatchFound) {
        return new ObservableBindingTrace(trace) {

            @Override
            public void report(@NotNull Diagnostic diagnostic) {
                DiagnosticFactory factory = diagnostic.getFactory();
                if ((factory == TYPE_MISMATCH || factory == CONSTANT_EXPECTED_TYPE_MISMATCH || factory == NULL_FOR_NONNULL_TYPE)
                        && diagnostic.getPsiElement() == expressionToWatch) {
                    mismatchFound[0] = true;
                }
                if (TYPE_INFERENCE_ERRORS.contains(factory) &&
                    PsiTreeUtil.isAncestor(expressionToWatch, diagnostic.getPsiElement(), false)) {
                    mismatchFound[0] = true;
                }
                super.report(diagnostic);
            }
        };
    }

    @NotNull
    public static JetTypeInfo getTypeInfoOrNullType(
            @Nullable JetExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull ExpressionTypingInternals facade
    ) {
        return expression != null
               ? facade.getTypeInfo(expression, context)
               : JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public static boolean isBinaryExpressionDependentOnExpectedType(@NotNull JetBinaryExpression expression) {
        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
        return (operationType == JetTokens.IDENTIFIER || OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)
                || operationType == JetTokens.ELVIS);
    }

    public static boolean  isUnaryExpressionDependentOnExpectedType(@NotNull JetUnaryExpression expression) {
        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
        return JetTokens.LABELS.contains(operationType) || operationType == JetTokens.EXCLEXCL;
    }
}
