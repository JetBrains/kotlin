package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.ObservableBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.util.lazy.LazyValueWithDefault;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.CANNOT_INFER_PARAMETER_TYPE;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public class ClosureExpressionsTypingVisitor extends ExpressionTypingVisitor {
    protected ClosureExpressionsTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetType visitObjectLiteralExpression(final JetObjectLiteralExpression expression, final ExpressionTypingContext context) {
        final JetType[] result = new JetType[1];
        ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor> handler = new ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor>() {

            @Override
            public void handleRecord(WritableSlice<PsiElement, ClassDescriptor> slice, PsiElement declaration, final ClassDescriptor descriptor) {
                if (slice == CLASS && declaration == expression.getObjectDeclaration()) {
                    JetType defaultType = DeferredType.create(context.trace, new LazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency")) {
                        @Override
                        protected JetType compute() {
                            return descriptor.getDefaultType();
                        }
                    });
                    result[0] = defaultType;
                    if (!context.trace.get(PROCESSED, expression)) {
                        context.trace.record(EXPRESSION_TYPE, expression, defaultType);
                        context.trace.record(PROCESSED, expression);
                    }
                }
            }
        };
        ObservableBindingTrace traceAdapter = new ObservableBindingTrace(context.trace);
        traceAdapter.addHandler(CLASS, handler);
        TopDownAnalyzer.processObject(context.semanticServices, traceAdapter, context.scope, context.scope.getContainingDeclaration(), expression.getObjectDeclaration());
        return ExpressionTypingUtils.checkType(result[0], expression, context);
    }

    @Override
    public JetType visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, ExpressionTypingContext context) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();

        JetTypeReference receiverTypeRef = functionLiteral.getReceiverTypeRef();
        final JetType receiverType;
        if (receiverTypeRef != null) {
            receiverType = context.getTypeResolver().resolveType(context.scope, receiverTypeRef);
        } else {
            ReceiverDescriptor implicitReceiver = context.scope.getImplicitReceiver();
            receiverType = implicitReceiver.exists() ? implicitReceiver.getType() : null;
        }

        FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(
                context.scope.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), "<anonymous>");

        List<JetType> parameterTypes = Lists.newArrayList();
        List<ValueParameterDescriptor> valueParameterDescriptors = Lists.newArrayList();
        List<JetParameter> declaredValueParameters = functionLiteral.getValueParameters();
        JetType expectedType = context.expectedType;

        boolean functionTypeExpected = expectedType != TypeUtils.NO_EXPECTED_TYPE && JetStandardClasses.isFunctionType(expectedType);
        List<ValueParameterDescriptor> expectedValueParameters =  (functionTypeExpected)
                                                          ? JetStandardClasses.getValueParameters(functionDescriptor, expectedType)
                                                          : null;

        if (functionTypeExpected && declaredValueParameters.isEmpty() && expectedValueParameters.size() == 1) {
            ValueParameterDescriptor valueParameterDescriptor = expectedValueParameters.get(0);
            ValueParameterDescriptor it = new ValueParameterDescriptorImpl(
                    functionDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), "it", valueParameterDescriptor.getInType(), valueParameterDescriptor.getOutType(), valueParameterDescriptor.hasDefaultValue(), valueParameterDescriptor.isVararg()
            );
            valueParameterDescriptors.add(it);
            parameterTypes.add(it.getOutType());
            context.trace.record(AUTO_CREATED_IT, it);
        }
        else {
            for (int i = 0; i < declaredValueParameters.size(); i++) {
                JetParameter declaredParameter = declaredValueParameters.get(i);
                JetTypeReference typeReference = declaredParameter.getTypeReference();

                JetType type;
                if (typeReference != null) {
                    type = context.getTypeResolver().resolveType(context.scope, typeReference);
                }
                else {
                    if (expectedValueParameters != null && i < expectedValueParameters.size()) {
                        type = expectedValueParameters.get(i).getOutType();
                    }
                    else {
                        context.trace.report(CANNOT_INFER_PARAMETER_TYPE.on(declaredParameter));
                        type = ErrorUtils.createErrorType("Cannot be inferred");
                    }
                }
                ValueParameterDescriptor valueParameterDescriptor = context.getClassDescriptorResolver().resolveValueParameterDescriptor(functionDescriptor, declaredParameter, i, type);
                parameterTypes.add(valueParameterDescriptor.getOutType());
                valueParameterDescriptors.add(valueParameterDescriptor);
            }
        }

        JetType effectiveReceiverType;
        if (receiverTypeRef == null) {
            if (functionTypeExpected) {
                effectiveReceiverType = JetStandardClasses.getReceiverType(expectedType);
            }
            else {
                effectiveReceiverType = null;
            }
        }
        else {
            effectiveReceiverType = receiverType;
        }
        functionDescriptor.initialize(effectiveReceiverType, NO_RECEIVER, Collections.<TypeParameterDescriptor>emptyList(), valueParameterDescriptors, null, Modality.FINAL, Visibility.LOCAL);
        context.trace.record(BindingContext.FUNCTION, expression, functionDescriptor);

        JetType returnType = TypeUtils.NO_EXPECTED_TYPE;
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        JetTypeReference returnTypeRef = functionLiteral.getReturnTypeRef();
        if (returnTypeRef != null) {
            returnType = context.getTypeResolver().resolveType(context.scope, returnTypeRef);
            context.getServices().checkFunctionReturnType(expression, context.replaceScope(functionInnerScope).replaceExpectedReturnType(returnType).replaceDataFlowInfo(context.dataFlowInfo));
        }
        else {
            if (functionTypeExpected) {
                returnType = JetStandardClasses.getReturnType(expectedType);
            }
            returnType = context.getServices().getBlockReturnedType(functionInnerScope, functionLiteral.getBodyExpression(), CoercionStrategy.COERCION_TO_UNIT, context.replaceExpectedType(returnType));
        }
        JetType safeReturnType = returnType == null ? ErrorUtils.createErrorType("<return type>") : returnType;
        functionDescriptor.setReturnType(safeReturnType);

        if (functionTypeExpected) {
            JetType expectedReturnType = JetStandardClasses.getReturnType(expectedType);
            if (JetStandardClasses.isUnit(expectedReturnType)) {
                functionDescriptor.setReturnType(expectedReturnType);
                return ExpressionTypingUtils.checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), effectiveReceiverType, parameterTypes, expectedReturnType), expression, context);
            }

        }
        return ExpressionTypingUtils.checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), effectiveReceiverType, parameterTypes, safeReturnType), expression, context);
    }

}
