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

package org.jetbrains.jet.lang.types.expressions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author abreslav
 */
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
        return new ExpressionReceiver(expression, facade.safeGetTypeInfo(expression, context).getType());
    }

    @NotNull
    public static WritableScopeImpl newWritableScopeImpl(ExpressionTypingContext context, @NotNull String scopeDebugName) {
        WritableScopeImpl scope = new WritableScopeImpl(
                context.scope, context.scope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(context.trace), scopeDebugName);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return scope;
    }

    public static boolean isBoolean(@NotNull JetType type) {
        return JetTypeChecker.INSTANCE.isSubtypeOf(type, JetStandardLibrary.getInstance().getBooleanType());
    }

    public static boolean ensureBooleanResult(JetExpression operationSign, Name name, JetType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    public static boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, ExpressionTypingContext context) {
        if (resultType != null) {
            // TODO : Relax?
            if (!isBoolean(resultType)) {
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, JetStandardLibrary.getInstance().getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static JetType getDefaultType(IElementType constantType) {
        if (constantType == JetNodeTypes.INTEGER_CONSTANT) {
            return JetStandardLibrary.getInstance().getIntType();
        }
        else if (constantType == JetNodeTypes.FLOAT_CONSTANT) {
            return JetStandardLibrary.getInstance().getDoubleType();
        }
        else if (constantType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return JetStandardLibrary.getInstance().getBooleanType();
        }
        else if (constantType == JetNodeTypes.CHARACTER_CONSTANT) {
            return JetStandardLibrary.getInstance().getCharType();
        }
        else if (constantType == JetNodeTypes.NULL) {
            return KotlinBuiltIns.getInstance().getNullableNothingType();
        }
        else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    public static boolean isTypeFlexible(@Nullable JetExpression expression) {
        if (expression == null) return false;

        return TokenSet.create(
                JetNodeTypes.INTEGER_CONSTANT,
                JetNodeTypes.FLOAT_CONSTANT
        ).contains(expression.getNode().getElementType());
    }

    public static void checkWrappingInRef(JetSimpleNameExpression expression, BindingTrace trace, JetScope scope) {
        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), expression, true);
        if (variable != null) {
            DeclarationDescriptor containingDeclaration = variable.getContainingDeclaration();
            if (scope.getContainingDeclaration() != containingDeclaration && containingDeclaration instanceof CallableDescriptor) {
                trace.record(CAPTURED_IN_CLOSURE, variable);
            }
        }
    }
    
    @NotNull
    public static JetExpression createStubExpressionOfNecessaryType(@NotNull Project project, @NotNull JetType type, @NotNull BindingTrace trace) {
        JetExpression expression = JetPsiFactory.createExpression(project, "$e");
        trace.record(PROCESSED, expression);
        trace.record(EXPRESSION_TYPE, expression, type);
        return expression;
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
                TypeUtils.NO_EXPECTED_TYPE,
                false
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
            @NotNull ModuleConfiguration moduleConfiguration
    ) {
        JetImportDirective importDirective = JetPsiFactory.createImportDirective(project, callableFQN.getFqName());

        Collection<? extends DeclarationDescriptor> declarationDescriptors = new QualifiedExpressionResolver()
                .analyseImportReference(importDirective, scope, new BindingTraceContext(), moduleConfiguration);

        List<CallableDescriptor> callableExtensionDescriptors = new ArrayList<CallableDescriptor>();
        ReceiverDescriptor receiverDescriptor = new ExpressionReceiver(receiverExpression, receiverType);

        for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
            if (declarationDescriptor instanceof CallableDescriptor) {
                CallableDescriptor callableDescriptor = (CallableDescriptor) declarationDescriptor;

                if (checkIsExtensionCallable(receiverDescriptor, callableDescriptor)) {
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
            @NotNull ReceiverDescriptor expectedReceiver,
            @NotNull CallableDescriptor receiverArgument
    ) {
        JetType type = expectedReceiver.getType();

        if (type instanceof NamespaceType) {
            // This fake class ruins standard algorithms
            return false;
        }

        if (checkReceiverResolution(expectedReceiver, type, receiverArgument)) return true;
        if (type.isNullable()) {
            JetType notNullableType = TypeUtils.makeNotNullable(type);
            if (checkReceiverResolution(expectedReceiver, notNullableType, receiverArgument)) return true;
        }
        return false;
    }

    private static boolean checkReceiverResolution (
            @NotNull ReceiverDescriptor expectedReceiver,
            @NotNull JetType receiverType,
            @NotNull CallableDescriptor receiverArgument
    ) {
        ConstraintSystem constraintSystem = new ConstraintSystemImpl();
        for (TypeParameterDescriptor typeParameterDescriptor : receiverArgument.getTypeParameters()) {
            constraintSystem.registerTypeVariable(typeParameterDescriptor, Variance.INVARIANT);
        }

        ReceiverDescriptor receiverParameter = receiverArgument.getReceiverParameter();
        if (expectedReceiver.exists() && receiverParameter.exists()) {
            constraintSystem.addSupertypeConstraint(receiverParameter.getType(), receiverType, ConstraintPosition.RECEIVER_POSITION);
        }
        else if (expectedReceiver.exists() || receiverParameter.exists()) {
            // Only one of receivers exist
            return false;
        }

        return constraintSystem.isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem);
    }

    @NotNull
    public static OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(
            @NotNull ReceiverDescriptor receiver,
            @NotNull ExpressionTypingContext context,
            @NotNull Name name
    ) {
        return makeAndResolveFakeCall(receiver, context, name).getSecond();
    }

    @NotNull
    public static Pair<Call, OverloadResolutionResults<FunctionDescriptor>> makeAndResolveFakeCall(
            @NotNull ReceiverDescriptor receiver,
            @NotNull ExpressionTypingContext context,
            @NotNull Name name
    ) {
        final JetReferenceExpression fake = JetPsiFactory.createSimpleName(context.expressionTypingServices.getProject(), "fake");
        TemporaryBindingTrace fakeTrace = TemporaryBindingTrace.create(context.trace);
        Call call = CallMaker.makeCall(fake, receiver, null, fake, Collections.<ValueArgument>emptyList());
        OverloadResolutionResults<FunctionDescriptor> results =
                context.replaceBindingTrace(fakeTrace).resolveCallWithGivenName(call, fake, name);
        if (results.isSuccess()) {
            fakeTrace.commit(new TraceEntryFilter() {
                @Override
                public boolean accept(@NotNull WritableSlice<?, ?> slice, Object key) {
                    // excluding all entries related to fake expression
                    // keys in RESOLUTION_RESULTS_FOR_FUNCTION slice have fake expression inside
                    return key != fake && slice != RESOLUTION_RESULTS_FOR_FUNCTION;
                }
            }, false);
        }
        return Pair.create(call, results);
    }

    public static void defineLocalVariablesFromMultiDeclaration(
            @NotNull WritableScope writableScope,
            @NotNull JetMultiDeclaration multiDeclaration,
            @NotNull ReceiverDescriptor receiver,
            @NotNull JetExpression reportErrorsOn,
            @NotNull ExpressionTypingContext context
    ) {
        int componentIndex = 1;
        for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
            final Name componentName = Name.identifier(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX + componentIndex);
            componentIndex++;

            JetType expectedType = getExpectedTypeForComponent(context, entry);
            OverloadResolutionResults<FunctionDescriptor> results =
                    resolveFakeCall(receiver, context.replaceExpectedType(expectedType), componentName);

            JetType componentType = null;
            if (results.isSuccess()) {
                context.trace.record(COMPONENT_RESOLVED_CALL, entry, results.getResultingCall());
                componentType = results.getResultingDescriptor().getReturnType();
                if (componentType != null && expectedType != TypeUtils.NO_EXPECTED_TYPE
                       && !JetTypeChecker.INSTANCE.isSubtypeOf(componentType, expectedType)) {

                    context.trace.report(
                            COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.on(reportErrorsOn, componentName, componentType, expectedType));
                }
            }
            else if (results.isAmbiguity()) {
                context.trace.report(COMPONENT_FUNCTION_AMBIGUITY.on(reportErrorsOn, componentName, results.getResultingCalls()));
            }
            else {
                context.trace.report(COMPONENT_FUNCTION_MISSING.on(reportErrorsOn, componentName));
            }
            if (componentType == null) {
                componentType = ErrorUtils.createErrorType(componentName + "() return type");
            }
            VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver().
                resolveLocalVariableDescriptorWithType(writableScope.getContainingDeclaration(), entry, componentType, context.trace);

            writableScope.addVariableDescriptor(variableDescriptor);
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
}
