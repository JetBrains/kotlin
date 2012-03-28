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
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.RESULT_TYPE_MISMATCH;
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
        return getExpressionReceiver(expression, facade.getType(expression, context));
    }

    @NotNull
    protected static ExpressionReceiver safeGetExpressionReceiver(@NotNull ExpressionTypingFacade facade, @NotNull JetExpression expression, ExpressionTypingContext context) {
        return new ExpressionReceiver(expression, facade.safeGetType(expression, context));
    }

    @NotNull
    public static WritableScopeImpl newWritableScopeImpl(ExpressionTypingContext context) {
        WritableScopeImpl scope = new WritableScopeImpl(context.scope, context.scope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(context.trace));
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return scope;
    }

    public static boolean isBoolean(@NotNull JetType type) {
        return JetTypeChecker.INSTANCE.isSubtypeOf(type, JetStandardLibrary.getInstance().getBooleanType());
    }

    public static boolean ensureBooleanResult(JetExpression operationSign, String name, JetType resultType, ExpressionTypingContext context) {
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
            return JetStandardClasses.getNullableNothingType();
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

    public static void checkWrappingInRef(JetSimpleNameExpression expression, ExpressionTypingContext context) {
        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(context.trace.getBindingContext(), expression, true);
        if (variable != null && variable.isVar()) {
            DeclarationDescriptor containingDeclaration = variable.getContainingDeclaration();
            if (context.scope.getContainingDeclaration() != containingDeclaration && containingDeclaration instanceof CallableDescriptor) {
                context.trace.record(MUST_BE_WRAPPED_IN_A_REF, variable);
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
                new HashMap<JetPattern, DataFlowInfo>(),
                new HashMap<JetPattern, List<VariableDescriptor>>(),
                new LabelResolver(),
                new BindingTraceContext(),
                scope,
                DataFlowInfo.EMPTY,
                TypeUtils.NO_EXPECTED_TYPE,
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
            @NotNull JetScope scope) {

        JetImportDirective importDirective = JetPsiFactory.createImportDirective(project, callableFQN.getFqName());

        Collection<? extends DeclarationDescriptor> declarationDescriptors = new QualifiedExpressionResolver().analyseImportReference(importDirective, scope, new BindingTraceContext());

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
        ConstraintSystem constraintSystem = new ConstraintSystemWithPriorities(ConstraintResolutionListener.DO_NOTHING);
        for (TypeParameterDescriptor typeParameterDescriptor : receiverArgument.getTypeParameters()) {
            constraintSystem.registerTypeVariable(typeParameterDescriptor, Variance.INVARIANT);
        }

        ReceiverDescriptor receiverParameter = receiverArgument.getReceiverParameter();
        if (expectedReceiver.exists() && receiverParameter.exists()) {
            constraintSystem.addSubtypingConstraint(ConstraintType.RECEIVER.assertSubtyping(receiverType, receiverParameter.getType()));
        }
        else if (expectedReceiver.exists() || receiverParameter.exists()) {
            // Only one of receivers exist
            return false;
        }

        ConstraintSystemSolution solution = constraintSystem.solve();
        return solution.getStatus().isSuccessful();
    }
}
