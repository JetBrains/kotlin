package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class AnnotationResolver {

    private final BindingTrace trace;
    private final JetSemanticServices semanticServices;
    private final CallResolver callResolver;

    public AnnotationResolver(JetSemanticServices semanticServices, BindingTrace trace) {
        this.trace = trace;
        this.callResolver = new CallResolver(semanticServices, DataFlowInfo.EMPTY);
        this.semanticServices = semanticServices;
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @Nullable JetModifierList modifierList) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return resolveAnnotations(scope, modifierList.getAnnotationEntries());
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @NotNull List<JetAnnotationEntry> annotationEntryElements) {
        if (annotationEntryElements.isEmpty()) return Collections.emptyList();
        List<AnnotationDescriptor> result = Lists.newArrayList();
        for (JetAnnotationEntry entryElement : annotationEntryElements) {
            AnnotationDescriptor descriptor = new AnnotationDescriptor();
            resolveAnnotationStub(scope, entryElement, descriptor);
            result.add(descriptor);
        }
        return result;
    }

    public void resolveAnnotationStub(@NotNull JetScope scope, @NotNull JetAnnotationEntry entryElement,
                                      @NotNull AnnotationDescriptor descriptor) {
        OverloadResolutionResults<FunctionDescriptor> results = resolveType(scope, entryElement, descriptor);
        resolveArguments(results, descriptor);
    }

    @NotNull
    private OverloadResolutionResults<FunctionDescriptor> resolveType(@NotNull JetScope scope,
                                                                      @NotNull JetAnnotationEntry entryElement,
                                                                      @NotNull AnnotationDescriptor descriptor) {
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveCall(trace, scope, CallMaker.makeCall(ReceiverDescriptor.NO_RECEIVER, null, entryElement), NO_EXPECTED_TYPE);
        JetType annotationType = results.getResultingDescriptor().getReturnType();
        if (results.isSuccess()) {
            descriptor.setAnnotationType(annotationType);
        } else {
            descriptor.setAnnotationType(ErrorUtils.createErrorType("Unresolved annotation type"));
        }
        return results;
    }

    private void resolveArguments(@NotNull OverloadResolutionResults<FunctionDescriptor> results,
                                  @NotNull AnnotationDescriptor descriptor) {
        List<CompileTimeConstant<?>> arguments = Lists.newArrayList();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> descriptorToArgument :
                results.getResultingCall().getValueArguments().entrySet()) {
            // TODO: are varargs supported here?
            List<JetExpression> argumentExpressions = descriptorToArgument.getValue().getArgumentExpressions();
            ValueParameterDescriptor parameterDescriptor = descriptorToArgument.getKey();
            for (JetExpression argument : argumentExpressions) {
                arguments.add(resolveAnnotationArgument(argument, parameterDescriptor.getOutType()));
            }
        }
        descriptor.setValueArguments(arguments);
    }

    @Nullable
    public CompileTimeConstant<?> resolveAnnotationArgument(@NotNull JetExpression expression, @NotNull final JetType expectedType) {
        JetVisitor<CompileTimeConstant<?>, Void> visitor = new JetVisitor<CompileTimeConstant<?>, Void>() {
            @Override
            public CompileTimeConstant<?> visitConstantExpression(JetConstantExpression expression, Void nothing) {
                ExpressionTypingServices typeInferrerServices = semanticServices.getTypeInferrerServices(trace);
                JetType type = typeInferrerServices.getType(JetScope.EMPTY, expression, expectedType, DataFlowInfo.EMPTY);
                if (type == null) {
                    // TODO:
                    //  trace.report(ANNOTATION_PARAMETER_SHOULD_BE_CONSTANT.on(expression));
                }
                return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
            }


            // @Override
//            public CompileTimeConstant visitAnnotation(JetAnnotation annotation, Void nothing) {
//                super.visitAnnotation(annotation, null); // TODO
//            }
//
//            @Override
//            public CompileTimeConstant visitAnnotationEntry(JetAnnotationEntry annotationEntry, Void nothing) {
//                return super.visitAnnotationEntry(annotationEntry, null); // TODO
//            }

            @Override
            public CompileTimeConstant<?> visitParenthesizedExpression(JetParenthesizedExpression expression, Void nothing) {
                JetExpression innerExpression = expression.getExpression();
                if (innerExpression == null) return null;
                return innerExpression.accept(this, null);
            }

            @Override
            public CompileTimeConstant<?> visitStringTemplateExpression(JetStringTemplateExpression expression,
                                                                        Void nothing) {
                return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
            }

            @Override
            public CompileTimeConstant<?> visitJetElement(JetElement element, Void nothing) {
                // TODO:
                //trace.report(ANNOTATION_PARAMETER_SHOULD_BE_CONSTANT.on(element));
                return null;
            }
        };
        return expression.accept(visitor, null);
    }

    @NotNull
    public List<AnnotationDescriptor> createAnnotationStubs(@Nullable JetModifierList modifierList) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return createAnnotationStubs(modifierList.getAnnotationEntries());
    }

    @NotNull
    public List<AnnotationDescriptor> createAnnotationStubs(List<JetAnnotationEntry> annotations) {
        List<AnnotationDescriptor> result = Lists.newArrayList();
        for (JetAnnotationEntry annotation : annotations) {
            AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor();
            result.add(annotationDescriptor);
            trace.record(BindingContext.ANNOTATION, annotation, annotationDescriptor);
        }
        return result;
    }
}
