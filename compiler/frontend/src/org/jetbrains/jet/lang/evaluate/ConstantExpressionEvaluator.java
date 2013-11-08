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

package org.jetbrains.jet.lang.evaluate;

import com.google.common.collect.Lists;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.COMPILE_TIME_INITIALIZER;

@SuppressWarnings("StaticMethodReferencedViaSubclass")
public class ConstantExpressionEvaluator extends JetVisitor<CompileTimeConstant<?>, Void> {
    @NotNull private final BindingTrace trace;
    @NotNull private final JetType expectedType;

    public ConstantExpressionEvaluator(@NotNull BindingTrace trace, @NotNull JetType expectedType) {
        this.trace = trace;
        this.expectedType = expectedType;
    }

    @Override
    public CompileTimeConstant<?> visitConstantExpression(@NotNull JetConstantExpression expression, Void nothing) {
        return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
    }

    @Override
    public CompileTimeConstant<?> visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, Void nothing) {
        JetExpression deparenthesizedExpression = getDeparenthesizedExpression(expression, nothing);
        if (deparenthesizedExpression != null) {
            return deparenthesizedExpression.accept(this, nothing);
        }
        return super.visitParenthesizedExpression(expression, nothing);
    }

    @Override
    public CompileTimeConstant<?> visitPrefixExpression(@NotNull JetPrefixExpression expression, Void data) {
        JetExpression deparenthesizedExpression = getDeparenthesizedExpression(expression, data);
        if (deparenthesizedExpression != null) {
            return deparenthesizedExpression.accept(this, data);
        }
        return super.visitPrefixExpression(expression, data);
    }

    @Nullable
    private JetExpression getDeparenthesizedExpression(JetExpression expression, Void data) {
        JetExpression deparenthesizedExpr = JetPsiUtil.deparenthesize(expression);
        if (deparenthesizedExpr == expression) {
            return null;
        }
        return deparenthesizedExpr;
    }

    @Override
    public CompileTimeConstant<?> visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, Void nothing) {
        CompileTimeConstant<?> compileTimeConstant = trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (compileTimeConstant != null) {
            return compileTimeConstant;
        }

        JetStringTemplateEntry[] stringTemplateEntries = expression.getEntries();
        if (stringTemplateEntries.length == 1) {
            CompileTimeConstant singleEntry = stringTemplateEntries[0].accept(this, nothing);
            if (!(singleEntry instanceof StringValue) && singleEntry != null) {
                return new StringValue(singleEntry.getValue().toString());
            }
            return singleEntry;
        }
        else if (stringTemplateEntries.length > 1) {
            CompileTimeConstant<?> first = stringTemplateEntries[0].accept(this, nothing);
            if (first == null) {
                return null;
            }
            CompileTimeConstant<?> second;
            String tmpResult = null;
            for (int i = 1; i < stringTemplateEntries.length; i++) {
                second = stringTemplateEntries[i].accept(this, nothing);
                if (second == null) {
                    return null;
                }
                Object object = EvaluatePackage.evaluateBinaryExpression(first, second, Name.identifier("plus"));
                if (object instanceof String) {
                    tmpResult = (String) object;
                }
                else {
                    tmpResult = object.toString();
                }
                first = new StringValue(tmpResult);
            }

            return new StringValue(tmpResult);
        }
        return null;
    }

    @Override
    public CompileTimeConstant<?> visitBlockStringTemplateEntry(@NotNull JetBlockStringTemplateEntry entry, Void data) {
        JetExpression expression = entry.getExpression();
        if (expression != null) {
            return expression.accept(this, data);
        }
        return super.visitBlockStringTemplateEntry(entry, data);
    }

    @Override
    public CompileTimeConstant<?> visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry, Void data) {
        return new StringValue(entry.getText());
    }

    @Override
    public CompileTimeConstant<?> visitSimpleNameStringTemplateEntry(@NotNull JetSimpleNameStringTemplateEntry entry, Void data) {
        JetExpression expression = entry.getExpression();
        if (expression != null) {
            return expression.accept(this, data);
        }
        return super.visitSimpleNameStringTemplateEntry(entry, data);
    }

    @Override
    public CompileTimeConstant<?> visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry, Void data) {
        return new StringValue(entry.getUnescapedValue());
    }

    @Override
    public CompileTimeConstant<?> visitBinaryExpression(@NotNull JetBinaryExpression expression, Void data) {
        JetExpression leftExpression = expression.getLeft();
        if (leftExpression == null) {
            return null;
        }

        IElementType operationToken = expression.getOperationToken();
        if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
            JetType booleanType = KotlinBuiltIns.getInstance().getBooleanType();
            CompileTimeConstant<?> leftConstant = leftExpression.accept(new ConstantExpressionEvaluator(trace, booleanType), null);
            if (leftConstant == null) {
                return null;
            }
            JetExpression rightExpression = expression.getRight();
            if (rightExpression == null) {
                return null;
            }
            CompileTimeConstant<?> rightConstant = rightExpression.accept(new ConstantExpressionEvaluator(trace, booleanType), null);
            if (rightConstant == null) {
                return null;
            }

            Name operationName = operationToken == JetTokens.ANDAND ? Name.identifier("andand") : Name.identifier("oror");
            Object result = EvaluatePackage.evaluateBinaryExpression(leftConstant, rightConstant, operationName);
            if (result == null) {
                return null;
            }
            return createCompileTimeConstant(result, expectedType);
        }
        else {
            return getCallConstant(expression.getOperationReference(), leftExpression);
        }

    }

    @Nullable
    private CompileTimeConstant<?> getCallConstant(@NotNull JetExpression resolvedCallExpression, @NotNull JetExpression receiverExpression) {
        ResolvedCall<?> resolvedCall = trace.getBindingContext().get(BindingContext.RESOLVED_CALL, resolvedCallExpression);
        if (resolvedCall != null) {
            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
            JetType receiverExpressionType = getReceiverExpressionType(resolvedCall);
            if (receiverExpressionType == null) {
                return null;
            }
            ConstantExpressionEvaluator evaluator = new ConstantExpressionEvaluator(trace, receiverExpressionType);
            CompileTimeConstant<?> receiverValue = receiverExpression.accept(evaluator, null);
            if (receiverValue == null) {
                return null;
            }
            List<CompileTimeConstant<?>> arguments = Lists.newArrayList();
            for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> argumentEntry : resolvedCall.getValueArguments() .entrySet()) {
                arguments.addAll(resolveArguments(argumentEntry.getValue().getArguments(), argumentEntry.getKey().getType(), trace));
            }
            return ConstantsPackage.resolveCallToCompileTimeValue(resultingDescriptor.getName(), receiverValue, arguments, expectedType);
        }

        return null;
    }

    @Override
    public CompileTimeConstant<?> visitUnaryExpression(@NotNull JetUnaryExpression expression, Void data) {
        JetExpression leftExpression = expression.getBaseExpression();
        if (leftExpression == null) {
            return null;
        }
        return getCallConstant(expression.getOperationReference(), leftExpression);
    }

    public static CompileTimeConstant<?> createCompileTimeConstant(@NotNull Object value, @NotNull JetType expectedType) {
        if (value instanceof Integer) {
            return getIntegerValue(((Integer) value).longValue(), expectedType);
        }
        else if (value instanceof Byte) {
            return getIntegerValue(((Byte) value).longValue(), expectedType);
        }
        else if (value instanceof Short) {
            return getIntegerValue(((Short) value).longValue(), expectedType);
        }
        else if (value instanceof Long) {
            return new LongValue((Long) value);
        }
        else if (value instanceof Float) {
            if (CompileTimeConstantResolver.noExpectedTypeOrError(expectedType) ||
                expectedType.equals(KotlinBuiltIns.getInstance().getDoubleType())) {
                return new DoubleValue(((Float) value).doubleValue());
            }
            else {
                return new FloatValue((Float) value);
            }
        }
        else if (value instanceof Double) {
            return new DoubleValue((Double) value);
        }
        else if (value instanceof Boolean) {
            return ((Boolean) value) ? BooleanValue.TRUE : BooleanValue.FALSE;
        }
        else if (value instanceof Character) {
            return new CharValue((Character) value);
        }
        else if (value instanceof String) {
            return new StringValue((String) value);
        }
        return null;
    }

    @Nullable
    private static CompileTimeConstant<?> getIntegerValue(@NotNull Long value, @NotNull JetType expectedType) {
        if (CompileTimeConstantResolver.noExpectedTypeOrError(expectedType)) {
            if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                return new IntValue(value.intValue());
            }
            return new LongValue(value);
        }
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        if (expectedType.equals(builtIns.getIntType())) {
            return new IntValue(value.intValue());
        }
        else if (expectedType.equals(builtIns.getLongType())) {
            return new LongValue(value);
        }
        else if (expectedType.equals(builtIns.getShortType())) {
            if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
                return new ShortValue(value.shortValue());
            }
            else if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                return new IntValue(value.intValue());
            }
            return new LongValue(value);
        }
        else if (expectedType.equals(builtIns.getByteType())) {
            if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
                return new ByteValue(value.byteValue());
            }
            else if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                return new IntValue(value.intValue());
            }
            return new LongValue(value);
        }
        else if (expectedType.equals(builtIns.getCharType())) {
            return new IntValue(value.intValue());
        }
        return null;
    }

    @Override
    public CompileTimeConstant<?> visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, Void data) {
        DeclarationDescriptor descriptor = trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, expression);
        if (descriptor != null && DescriptorUtils.isEnumEntry(descriptor)) {
            return new EnumValue((ClassDescriptor) descriptor);
        }

        ResolvedCall<? extends CallableDescriptor> resolvedCall =
                trace.getBindingContext().get(BindingContext.RESOLVED_CALL, expression);
        if (resolvedCall != null) {
            CallableDescriptor callableDescriptor = resolvedCall.getResultingDescriptor();
            if (callableDescriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) callableDescriptor;
                if (AnnotationUtils.isPropertyAcceptableAsAnnotationParameter(propertyDescriptor)) {
                    return trace.getBindingContext().get(COMPILE_TIME_INITIALIZER, propertyDescriptor);
                }
            }
        }

        return null;
    }

    /* 1.toInt(); 1.plus(1); MyEnum.A */
    @Override
    public CompileTimeConstant<?> visitQualifiedExpression(@NotNull JetQualifiedExpression expression, Void data) {
        CompileTimeConstant wholeExpressionValue = trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (wholeExpressionValue != null) {
            return wholeExpressionValue;
        }

        JetExpression receiverExpression = expression.getReceiverExpression();
        JetExpression selectorExpression = expression.getSelectorExpression();


        if (selectorExpression instanceof JetCallExpression) {
            JetExpression calleeExpression = ((JetCallExpression) selectorExpression).getCalleeExpression();
            if (!(calleeExpression instanceof JetSimpleNameExpression)) {
                return null;
            }

            if (((JetCallExpression) selectorExpression).getValueArguments().size() < 2) {
                return getCallConstant(calleeExpression, receiverExpression);
            }
        }

        if (selectorExpression != null) {
            return selectorExpression.accept(this, null);
        }
        return super.visitQualifiedExpression(expression, data);
    }

    @Nullable
    private static JetType getReceiverExpressionType(ResolvedCall<? extends CallableDescriptor> resolvedCall) {
        switch(resolvedCall.getExplicitReceiverKind()) {
            case THIS_OBJECT: return resolvedCall.getThisObject().getType();
            case RECEIVER_ARGUMENT: return resolvedCall.getReceiverArgument().getType();
            case NO_EXPLICIT_RECEIVER: return null;
            case BOTH_RECEIVERS: return null;
        }
        return null;
    }

    @Override
    public CompileTimeConstant<?> visitCallExpression(@NotNull JetCallExpression expression, Void data) {
        ResolvedCall<? extends CallableDescriptor> call =
                trace.getBindingContext().get(BindingContext.RESOLVED_CALL, (expression).getCalleeExpression());
        if (call != null) {
            CallableDescriptor resultingDescriptor = call.getResultingDescriptor();
            if (AnnotationUtils.isArrayMethodCall(call)) {
                JetType type = resultingDescriptor.getValueParameters().iterator().next().getVarargElementType();
                List<CompileTimeConstant<?>> arguments = Lists.newArrayList();
                for (ResolvedValueArgument descriptorToArgument : call.getValueArguments().values()) {
                    arguments.addAll(resolveArguments(descriptorToArgument.getArguments(), type, trace));
                }
                return new ArrayValue(arguments, resultingDescriptor.getReturnType());
            }

            if (resultingDescriptor instanceof ConstructorDescriptor) {
                JetType constructorReturnType = resultingDescriptor.getReturnType();
                assert constructorReturnType != null : "Constructor should have return type";
                if (DescriptorUtils.isAnnotationClass(constructorReturnType.getConstructor().getDeclarationDescriptor())) {
                    AnnotationDescriptorImpl descriptor = new AnnotationDescriptorImpl();
                    descriptor.setAnnotationType(constructorReturnType);
                    AnnotationResolver.resolveAnnotationArgument(descriptor, call, trace);
                    return new AnnotationValue(descriptor);
                }
            }

            if (AnnotationUtils.isJavaClassMethodCall(call)) {
                return new JavaClassValue(resultingDescriptor.getReturnType());
            }
        }
        return null;
    }

    @NotNull
    private static List<CompileTimeConstant<?>> resolveArguments(
            @NotNull List<ValueArgument> valueArguments,
            @NotNull JetType expectedType,
            @NotNull BindingTrace trace
    ) {
        List<CompileTimeConstant<?>> constants = Lists.newArrayList();
        for (ValueArgument argument : valueArguments) {
            JetExpression argumentExpression = argument.getArgumentExpression();
            if (argumentExpression != null) {
                CompileTimeConstant<?> constant = argumentExpression.accept(new ConstantExpressionEvaluator(trace, expectedType), null);
                if (constant != null) {
                    constants.add(constant);
                }
            }
        }
        return constants;
    }

    @Override
    public CompileTimeConstant<?> visitJetElement(@NotNull JetElement element, Void nothing) {
        return null;
    }
}
