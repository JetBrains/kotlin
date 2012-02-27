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
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPattern;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.HashMap;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.RESULT_TYPE_MISMATCH;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author abreslav
 */
public class ExpressionTypingUtils {
    
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

    public static boolean isBoolean(@NotNull JetSemanticServices semanticServices, @NotNull JetType type) {
        return semanticServices.getTypeChecker().isSubtypeOf(type, semanticServices.getStandardLibrary().getBooleanType());
    }

    public static boolean ensureBooleanResult(JetExpression operationSign, String name, JetType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    public static boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, ExpressionTypingContext context) {
        if (resultType != null) {
            // TODO : Relax?
            if (!isBoolean(context.semanticServices, resultType)) {
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, context.semanticServices.getStandardLibrary().getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static JetType getDefaultType(JetSemanticServices semanticServices, IElementType constantType) {
        if (constantType == JetNodeTypes.INTEGER_CONSTANT) {
            return semanticServices.getStandardLibrary().getIntType();
        }
        else if (constantType == JetNodeTypes.FLOAT_CONSTANT) {
            return semanticServices.getStandardLibrary().getDoubleType();
        }
        else if (constantType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return semanticServices.getStandardLibrary().getBooleanType();
        }
        else if (constantType == JetNodeTypes.CHARACTER_CONSTANT) {
            return semanticServices.getStandardLibrary().getCharType();
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

    public static void checkWrappingInRef(JetExpression expression, ExpressionTypingContext context) {
        if (!(expression instanceof JetSimpleNameExpression)) return;
        JetSimpleNameExpression simpleName = (JetSimpleNameExpression) expression;
        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(context.trace.getBindingContext(), simpleName, true);
        if (variable != null) {
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

    public static boolean isVariableIterable(@NotNull Project project, @NotNull VariableDescriptor variableDescriptor, @NotNull JetScope scope) {
        JetExpression expression = JetPsiFactory.createExpression(project, "fake");
        ExpressionReceiver expressionReceiver = new ExpressionReceiver(expression, variableDescriptor.getType());
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                project,
                JetSemanticServices.createSemanticServices(project),
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
}
