package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.lazy.LazyValueWithDefault;
import org.jetbrains.jet.util.lazy.ReenteringLazyValueComputationException;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
* @author abreslav
*/
public class ExpressionTyperVisitor extends JetVisitor<JetType, ExpressionTypingContext> {
    protected DataFlowInfo resultDataFlowInfo;

    @Nullable
    public DataFlowInfo getResultingDataFlowInfo() {
        return resultDataFlowInfo;
    }

    @NotNull
    public final JetType safeGetType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        JetType type = getType(expression, context);
        if (type != null) {
            return type;
        }
        return ErrorUtils.createErrorType("Type for " + expression.getText());
    }

    @Nullable
    public final ExpressionReceiver getExpressionReceiver(@NotNull JetExpression expression, ExpressionTypingContext context) {
        JetType type = getType(expression, context);
        if (type == null) {
            return null;
        }
        return new ExpressionReceiver(expression, type);
    }

    @NotNull
    public final ExpressionReceiver safeGetExpressionReceiver(@NotNull JetExpression expression, ExpressionTypingContext context) {
        return new ExpressionReceiver(expression, safeGetType(expression, context));
    }

    @Nullable
    public final JetType getType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        if (context.trace.get(BindingContext.PROCESSED, expression)) {
            return context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
        }
        JetType result;
        try {
            result = expression.visit(this, context);
            // Some recursive definitions (object expressions) must put their types in the cache manually:
            if (context.trace.get(BindingContext.PROCESSED, expression)) {
                return context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
            }

            if (result instanceof DeferredType) {
                result = ((DeferredType) result).getActualType();
            }
            if (result != null) {
                context.trace.record(BindingContext.EXPRESSION_TYPE, expression, result);
//                    if (JetStandardClasses.isNothing(result) && !result.isNullable()) {
//                        markDominatedExpressionsAsUnreachable(expression, context);
//                    }
            }
//            }
        }
        catch (ReenteringLazyValueComputationException e) {
//                context.trace.getErrorHandler().genericError(expression.getNode(), "Type checking has run into a recursive problem"); // TODO : message
            context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
            result = null;
        }

        if (!context.trace.get(BindingContext.PROCESSED, expression)) {
            context.trace.record(BindingContext.RESOLUTION_SCOPE, expression, context.scope);
        }
        context.trace.record(BindingContext.PROCESSED, expression);
        return result;
    }

    private JetType getTypeWithNewScopeAndDataFlowInfo(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull DataFlowInfo newDataFlowInfo, @NotNull ExpressionTypingContext context) {
        return getType(expression, context.replaceScope(scope).replaceDataFlowInfo(newDataFlowInfo));
    }


    public void resetResult() {
//            result = null;
        resultDataFlowInfo = null;
//            resultScope = null;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//        private void markDominatedExpressionsAsUnreachable(JetExpression expression, ExpressionTypingContext context) {
//            List<JetElement> dominated = new ArrayList<JetElement>();
//            flowInformationProvider.collectDominatedExpressions(expression, dominated);
//            Set<JetElement> rootExpressions = JetPsiUtil.findRootExpressions(dominated);
//            for (JetElement rootExpression : rootExpressions) {
////                context.trace.getErrorHandler().genericError(rootExpression.getNode(),
////                        "This code is unreachable, because '" + expression.getText() + "' never terminates normally");
//                context.trace.report(UNREACHABLE_BECAUSE_OF_NOTHING.on(rootExpression, expression.getText()));
//            }
//        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetType visitSimpleNameExpression(JetSimpleNameExpression expression, ExpressionTypingContext context) {
        // TODO : other members
        // TODO : type substitutions???
        String referencedName = expression.getReferencedName();
        if (expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER
                && referencedName != null) {
            PropertyDescriptor property = context.scope.getPropertyByFieldReference(referencedName);
            if (property == null) {
                context.trace.report(UNRESOLVED_REFERENCE.on(expression));
            }
            else {
                context.trace.record(REFERENCE_TARGET, expression, property);
                return context.getServices().checkType(property.getOutType(), expression, context);
            }
        }
        else {
            return getSelectorReturnType(NO_RECEIVER, null, expression, context); // TODO : Extensions to this
//                assert JetTokens.IDENTIFIER == expression.getReferencedNameElementType();
//                if (referencedName != null) {
//                    VariableDescriptor variable = context.scope.getVariable(referencedName);
//                    if (variable != null) {
//                        context.trace.record(REFERENCE_TARGET, expression, variable);
//                        JetType result = variable.getOutType();
//                        if (result == null) {
//                            context.trace.getErrorHandler().genericError(expression.getNode(), "This variable is not readable in this context");
//                        }
//                        return context.getServices().checkType(result, expression, context);
//                    }
//                    else {
//                        return lookupNamespaceOrClassObject(expression, referencedName, context);
//                        ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
//                        if (classifier != null) {
//                            JetType classObjectType = classifier.getClassObjectType();
//                            JetType result = null;
//                            if (classObjectType != null && (isNamespacePosition() || classifier.isClassObjectAValue())) {
//                                result = classObjectType;
//                            }
//                            else {
//                                context.trace.getErrorHandler().genericError(expression.getNode(), "Classifier " + classifier.getName() +  " does not have a class object");
//                            }
//                            context.trace.record(REFERENCE_TARGET, expression, classifier);
//                            return context.getServices().checkType(result, expression, context);
//                        }
//                        else {
//                            JetType[] result = new JetType[1];
//                            if (furtherNameLookup(expression, referencedName, result, context)) {
//                                return context.getServices().checkType(result[0], expression, context);
//                            }
//
//                        }
//                    }
//                    context.trace.report(UNRESOLVED_REFERENCE.on(expression));
//                }
        }
        return null;
    }

    private JetType lookupNamespaceOrClassObject(JetSimpleNameExpression expression, String referencedName, ExpressionTypingContext context) {
        ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
        if (classifier != null) {
            JetType classObjectType = classifier.getClassObjectType();
            JetType result = null;
            if (classObjectType != null) {
                if (isNamespacePosition() || classifier.isClassObjectAValue()) {
                    result = classObjectType;
                }
                else {
//                    context.trace.getErrorHandler().genericError(expression.getNode(), "Classifier " + classifier.getName() +  " does not have a class object");
                    context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
                }
                context.trace.record(REFERENCE_TARGET, expression, classifier);
                if (result == null) {
                    return ErrorUtils.createErrorType("No class object in " + expression.getReferencedName());
                }
                return context.getServices().checkType(result, expression, context);
            }
        }
        JetType[] result = new JetType[1];
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
        if (furtherNameLookup(expression, referencedName, result, context.replaceBindingTrace(temporaryTrace))) {
            temporaryTrace.commit();
            return context.getServices().checkType(result[0], expression, context);
        }
        // To report NO_CLASS_OBJECT when no namespace found
        if (classifier != null) {
            context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
            context.trace.record(REFERENCE_TARGET, expression, classifier);
            return ErrorUtils.createErrorType("No class object in " + expression.getReferencedName());
        }
        temporaryTrace.commit();
        return result[0];
    }

    public boolean isNamespacePosition() {
        return false;
    }

    protected boolean furtherNameLookup(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName, @NotNull JetType[] result, ExpressionTypingContext context) {
        NamespaceType namespaceType = lookupNamespaceType(expression, referencedName, context);
        if (namespaceType != null) {
//                context.trace.getErrorHandler().genericError(expression.getNode(), "Expression expected, but a namespace name found");
            context.trace.report(EXPRESSION_EXPECTED_NAMESPACE_FOUND.on(expression));
            result[0] = ErrorUtils.createErrorType("Type for " + referencedName);
        }
        return false;
    }

    @Nullable
    protected NamespaceType lookupNamespaceType(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName, ExpressionTypingContext context) {
        NamespaceDescriptor namespace = context.scope.getNamespace(referencedName);
        if (namespace == null) {
            return null;
        }
        context.trace.record(REFERENCE_TARGET, expression, namespace);
        return namespace.getNamespaceType();
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
        return context.getServices().checkType(result[0], expression, context);
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

        List<JetType> parameterTypes = new ArrayList<JetType>();
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
//                        context.trace.getErrorHandler().genericError(declaredParameter.getNode(), "Cannot infer a type for this declaredParameter. To specify it explicitly use the {(p : Type) => ...} notation");
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
            context.getServices().checkFunctionReturnType(functionInnerScope, expression, functionDescriptor, returnType, context.dataFlowInfo);
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
                return context.getServices().checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), effectiveReceiverType, parameterTypes, expectedReturnType), expression, context);
            }

        }
        return context.getServices().checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), effectiveReceiverType, parameterTypes, safeReturnType), expression, context);
    }

    @Override
    public JetType visitParenthesizedExpression(JetParenthesizedExpression expression, ExpressionTypingContext context) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression == null) {
            return null;
        }
        return context.getServices().checkType(getType(innerExpression, context.replaceScope(context.scope)), expression, context);
    }

    @Override
    public JetType visitConstantExpression(JetConstantExpression expression, ExpressionTypingContext context) {
        ASTNode node = expression.getNode();
        IElementType elementType = node.getElementType();
        String text = node.getText();
        JetStandardLibrary standardLibrary = context.semanticServices.getStandardLibrary();
        CompileTimeConstantResolver compileTimeConstantResolver = context.getCompileTimeConstantResolver();

        CompileTimeConstant<?> value;
        if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
            value = compileTimeConstantResolver.getIntegerValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
            value = compileTimeConstantResolver.getFloatValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
            value = compileTimeConstantResolver.getBooleanValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
            value = compileTimeConstantResolver.getCharValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.RAW_STRING_CONSTANT) {
            value = compileTimeConstantResolver.getRawStringValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.NULL) {
            value = compileTimeConstantResolver.getNullValue(context.expectedType);
        }
        else {
            throw new IllegalArgumentException("Unsupported constant: " + expression);
        }
        if (value instanceof ErrorValue) {
            ErrorValue errorValue = (ErrorValue) value;
//                context.trace.getErrorHandler().genericError(node, errorValue.getMessage());
            context.trace.report(ERROR_COMPILE_TIME_VALUE.on(node, errorValue.getMessage()));
            return getDefaultType(context.semanticServices, elementType);
        }
        else {
            context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, value);
            return context.getServices().checkType(value.getType(standardLibrary), expression, context);
        }
    }

    @NotNull
    private JetType getDefaultType(JetSemanticServices semanticServices, IElementType constantType) {
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
        else if (constantType == JetNodeTypes.RAW_STRING_CONSTANT) {
            return semanticServices.getStandardLibrary().getStringType();
        }
        else if (constantType == JetNodeTypes.NULL) {
            return JetStandardClasses.getNullableNothingType();
        }
        else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    @Override
    public JetType visitThrowExpression(JetThrowExpression expression, ExpressionTypingContext context) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            JetType type = getType(thrownExpression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceScope(context.scope));
            // TODO : check that it inherits Throwable
        }
        return context.getServices().checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitReturnExpression(JetReturnExpression expression, ExpressionTypingContext context) {
        context.labelResolver.recordLabel(expression, context);
        if (context.expectedReturnType == TypeUtils.FORBIDDEN) {
//                context.trace.getErrorHandler().genericError(expression.getNode(), "'return' is not allowed here");
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
            return null;
        }
        JetExpression returnedExpression = expression.getReturnedExpression();

        JetType returnedType = JetStandardClasses.getUnitType();
        if (returnedExpression != null) {
            getType(returnedExpression, context.replaceExpectedType(context.expectedReturnType).replaceScope(context.scope));
        }
        else {
            if (context.expectedReturnType != TypeUtils.NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(context.expectedReturnType)) {
//                    context.trace.getErrorHandler().genericError(expression.getNode(), "This function must return a value of type " + context.expectedReturnType);
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, context.expectedReturnType));
            }
        }
        return context.getServices().checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitBreakExpression(JetBreakExpression expression, ExpressionTypingContext context) {
        context.labelResolver.recordLabel(expression, context);
        return context.getServices().checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitContinueExpression(JetContinueExpression expression, ExpressionTypingContext context) {
        context.labelResolver.recordLabel(expression, context);
        return context.getServices().checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context) {
        JetTypeReference right = expression.getRight();
        JetType result = null;
        if (right != null) {
            JetType targetType = context.getTypeResolver().resolveType(context.scope, right);

            if (isTypeFlexible(expression.getLeft())) {
                TemporaryBindingTrace temporaryTraceWithExpectedType = TemporaryBindingTrace.create(context.trace);
                boolean success = checkBinaryWithTypeRHS(expression, context, targetType, targetType, temporaryTraceWithExpectedType);
                if (success) {
                    temporaryTraceWithExpectedType.commit();
                }
                else {
                    TemporaryBindingTrace temporaryTraceWithoutExpectedType = TemporaryBindingTrace.create(context.trace);
                    checkBinaryWithTypeRHS(expression, context, targetType, TypeUtils.NO_EXPECTED_TYPE, temporaryTraceWithoutExpectedType);
                    temporaryTraceWithoutExpectedType.commit();
                }
            }
            else {
                TemporaryBindingTrace temporaryTraceWithoutExpectedType = TemporaryBindingTrace.create(context.trace);
                checkBinaryWithTypeRHS(expression, context, targetType, TypeUtils.NO_EXPECTED_TYPE, temporaryTraceWithoutExpectedType);
                temporaryTraceWithoutExpectedType.commit();
            }

            IElementType operationType = expression.getOperationSign().getReferencedNameElementType();
            result = operationType == JetTokens.AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
        }
        else {
            getType(expression.getLeft(), context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
        }
        return context.getServices().checkType(result, expression, context);
    }

    private boolean isTypeFlexible(@Nullable JetExpression expression) {
        if (expression == null) return false;

        return TokenSet.create(
                JetNodeTypes.INTEGER_CONSTANT,
                JetNodeTypes.FLOAT_CONSTANT
        ).contains(expression.getNode().getElementType());
    }

    private boolean checkBinaryWithTypeRHS(JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context, @NotNull JetType targetType, @NotNull JetType expectedType, TemporaryBindingTrace temporaryTrace) {
        ExpressionTypingContext newContext = context.replaceExpectedTypeAndTrace(expectedType, temporaryTrace);

        JetType actualType = getType(expression.getLeft(), newContext);
        if (actualType == null) return false;

        JetSimpleNameExpression operationSign = expression.getOperationSign();
        IElementType operationType = operationSign.getReferencedNameElementType();
        if (operationType == JetTokens.COLON) {
            if (targetType != TypeUtils.NO_EXPECTED_TYPE && !context.semanticServices.getTypeChecker().isSubtypeOf(actualType, targetType)) {
//                    context.trace.getErrorHandler().typeMismatch(expression.getLeft(), targetType, actualType);
                context.trace.report(TYPE_MISMATCH.on(expression.getLeft(), targetType, actualType));
                return false;
            }
            return true;
        }
        else if (operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
            checkForCastImpossibility(expression, actualType, targetType, context);
            return true;
        }
        else {
//                context.trace.getErrorHandler().genericError(operationSign.getNode(), "Unknown binary operation");\
            context.trace.report(UNSUPPORTED.on(operationSign, "binary operation with type RHS"));
            return false;
        }
    }

    private void checkForCastImpossibility(JetBinaryExpressionWithTypeRHS expression, JetType actualType, JetType targetType, ExpressionTypingContext context) {
        if (actualType == null || targetType == TypeUtils.NO_EXPECTED_TYPE) return;

        JetTypeChecker typeChecker = context.semanticServices.getTypeChecker();
        if (!typeChecker.isSubtypeOf(targetType, actualType)) {
            if (typeChecker.isSubtypeOf(actualType, targetType)) {
//                    context.trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "No cast needed, use ':' instead");
                context.trace.report(USELESS_CAST_STATIC_ASSERT_IS_FINE.on(expression, expression.getOperationSign()));
            }
            else {
                // See JET-58 Make 'as never succeeds' a warning, or even never check for Java (external) types
//                    context.trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "This cast can never succeed");
                context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.getOperationSign()));
            }
        }
        else {
            if (typeChecker.isSubtypeOf(actualType, targetType)) {
//                    context.trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "No cast needed");
                context.trace.report(USELESS_CAST.on(expression, expression.getOperationSign()));
            }
        }
    }

    @Override
    public JetType visitTupleExpression(JetTupleExpression expression, ExpressionTypingContext context) {
        List<JetExpression> entries = expression.getEntries();
        List<JetType> types = new ArrayList<JetType>();
        for (JetExpression entry : entries) {
            types.add(context.getServices().safeGetType(context.scope, entry, TypeUtils.NO_EXPECTED_TYPE)); // TODO
        }
        if (context.expectedType != TypeUtils.NO_EXPECTED_TYPE && JetStandardClasses.isTupleType(context.expectedType)) {
            List<JetType> enrichedTypes = context.getServices().checkArgumentTypes(types, entries, context.expectedType.getArguments(), context);
            if (enrichedTypes != types) {
                return JetStandardClasses.getTupleType(enrichedTypes);
            }
        }
        // TODO : labels
        return context.getServices().checkType(JetStandardClasses.getTupleType(types), expression, context);
    }

    @Override
    public JetType visitThisExpression(JetThisExpression expression, ExpressionTypingContext context) {
        JetType result = null;
        ReceiverDescriptor thisReceiver = null;
        String labelName = expression.getLabelName();
        if (labelName != null) {
            thisReceiver = context.labelResolver.resolveThisLabel(expression, context, thisReceiver, labelName);
        }
        else {
            thisReceiver = context.scope.getImplicitReceiver();

            DeclarationDescriptor declarationDescriptorForUnqualifiedThis = context.scope.getDeclarationDescriptorForUnqualifiedThis();
            if (declarationDescriptorForUnqualifiedThis != null) {
                context.trace.record(REFERENCE_TARGET, expression.getThisReference(), declarationDescriptorForUnqualifiedThis);
            }
        }

        if (thisReceiver != null) {
            if (!thisReceiver.exists()) {
//                    context.trace.getErrorHandler().genericError(expression.getNode(), "'this' is not defined in this context");
                context.trace.report(NO_THIS.on(expression));
            }
            else {
                JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
                if (superTypeQualifier != null) {
                    JetTypeElement superTypeElement = superTypeQualifier.getTypeElement();
                    // Errors are reported by the parser
                    if (superTypeElement instanceof JetUserType) {
                        JetUserType typeElement = (JetUserType) superTypeElement;

                        ClassifierDescriptor classifierCandidate = context.getTypeResolver().resolveClass(context.scope, typeElement);
                        if (classifierCandidate instanceof ClassDescriptor) {
                            ClassDescriptor superclass = (ClassDescriptor) classifierCandidate;

                            JetType thisType = thisReceiver.getType();
                            Collection<? extends JetType> supertypes = thisType.getConstructor().getSupertypes();
                            TypeSubstitutor substitutor = TypeSubstitutor.create(thisType);
                            for (JetType declaredSupertype : supertypes) {
                                if (declaredSupertype.getConstructor().equals(superclass.getTypeConstructor())) {
                                    result = substitutor.safeSubstitute(declaredSupertype, Variance.INVARIANT);
                                    break;
                                }
                            }
                            if (result == null) {
//                                    context.trace.getErrorHandler().genericError(superTypeElement.getNode(), "Not a superclass");
                                context.trace.report(NOT_A_SUPERTYPE.on(superTypeElement));
                            }
                        }
                    }
                }
                else {
                    result = thisReceiver.getType();
                }
                if (result != null) {
                    context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getThisReference(), result);
                }
            }
        }
        return context.getServices().checkType(result, expression, context);
    }

    @Override
    public JetType visitBlockExpression(JetBlockExpression expression, ExpressionTypingContext context) {
        return context.getServices().getBlockReturnedType(context.scope, expression, CoercionStrategy.NO_COERCION, context);
    }

    @Override
    public JetType visitWhenExpression(final JetWhenExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        // TODO :change scope according to the bound value in the when header
        final JetExpression subjectExpression = expression.getSubjectExpression();

        final JetType subjectType = subjectExpression != null ? context.getServices().safeGetType(context.scope, subjectExpression, TypeUtils.NO_EXPECTED_TYPE) : ErrorUtils.createErrorType("Unknown type");
        final VariableDescriptor variableDescriptor = subjectExpression != null ? context.getServices().getVariableDescriptorFromSimpleName(subjectExpression, context) : null;

        // TODO : exhaustive patterns

        Set<JetType> expressionTypes = Sets.newHashSet();
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            JetWhenCondition[] conditions = whenEntry.getConditions();
            DataFlowInfo newDataFlowInfo;
            WritableScope scopeToExtend;
            if (conditions.length == 1) {
                scopeToExtend = newWritableScopeImpl(context.scope, context.trace).setDebugName("Scope extended in when entry");
                newDataFlowInfo = context.dataFlowInfo;
                JetWhenCondition condition = conditions[0];
                if (condition != null) {
                    newDataFlowInfo = checkWhenCondition(subjectExpression, subjectType, condition, scopeToExtend, context, variableDescriptor);
                }
            }
            else {
                scopeToExtend = newWritableScopeImpl(context.scope, context.trace); // We don't write to this scope
                newDataFlowInfo = null;
                for (JetWhenCondition condition : conditions) {
                    DataFlowInfo dataFlowInfo = checkWhenCondition(subjectExpression, subjectType, condition, newWritableScopeImpl(context.scope, context.trace), context, variableDescriptor);
                    if (newDataFlowInfo == null) {
                        newDataFlowInfo = dataFlowInfo;
                    }
                    else {
                        newDataFlowInfo = newDataFlowInfo.or(dataFlowInfo);
                    }
                }
                if (newDataFlowInfo == null) {
                    newDataFlowInfo = context.dataFlowInfo;
                }
                else {
                    newDataFlowInfo = newDataFlowInfo.and(context.dataFlowInfo);
                }
            }
            JetExpression bodyExpression = whenEntry.getExpression();
            if (bodyExpression != null) {
                JetType type = getTypeWithNewScopeAndDataFlowInfo(scopeToExtend, bodyExpression, newDataFlowInfo, contextWithExpectedType);
                if (type != null) {
                    expressionTypes.add(type);
                }
            }
        }

        if (!expressionTypes.isEmpty()) {
            return context.semanticServices.getTypeChecker().commonSupertype(expressionTypes);
        }
        else if (expression.getEntries().isEmpty()) {
//                context.trace.getErrorHandler().genericError(expression.getNode(), "Entries required for when-expression");
            context.trace.report(NO_WHEN_ENTRIES.on(expression));
        }
        return null;
    }

    private DataFlowInfo checkWhenCondition(@Nullable final JetExpression subjectExpression, final JetType subjectType, JetWhenCondition condition, final WritableScope scopeToExtend, final ExpressionTypingContext context, final VariableDescriptor... subjectVariables) {
        final DataFlowInfo[] newDataFlowInfo = new DataFlowInfo[]{context.dataFlowInfo};
        condition.accept(new JetVisitorVoid() {

            @Override
            public void visitWhenConditionCall(JetWhenConditionCall condition) {
                JetExpression callSuffixExpression = condition.getCallSuffixExpression();
//                    JetScope compositeScope = new ScopeWithReceiver(context.scope, subjectType, semanticServices.getTypeChecker());
                if (callSuffixExpression != null) {
//                        JetType selectorReturnType = getType(compositeScope, callSuffixExpression, false, context);
                    assert subjectExpression != null;
                    JetType selectorReturnType = getSelectorReturnType(new ExpressionReceiver(subjectExpression, subjectType), condition.getOperationTokenNode(), callSuffixExpression, context);//getType(compositeScope, callSuffixExpression, false, context);
                    ensureBooleanResultWithCustomSubject(callSuffixExpression, selectorReturnType, "This expression", context);
//                        context.getServices().checkNullSafety(subjectType, condition.getOperationTokenNode(), getCalleeFunctionDescriptor(callSuffixExpression, context), condition);
                }
            }

            @Override
            public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                JetExpression rangeExpression = condition.getRangeExpression();
                if (rangeExpression != null) {
                    assert subjectExpression != null;
                    checkInExpression(condition, condition.getOperationReference(), subjectExpression, rangeExpression, context);
                }
            }

            @Override
            public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                JetPattern pattern = condition.getPattern();
                if (pattern != null) {
                    newDataFlowInfo[0] = checkPatternType(pattern, subjectType, scopeToExtend, context, subjectVariables);
                }
            }

            @Override
            public void visitJetElement(JetElement element) {
//                    context.trace.getErrorHandler().genericError(element.getNode(), "Unsupported [OperatorConventions] : " + element);
                context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
            }
        });
        return newDataFlowInfo[0];
    }

    private DataFlowInfo checkPatternType(@NotNull JetPattern pattern, @NotNull final JetType subjectType, @NotNull final WritableScope scopeToExtend, final ExpressionTypingContext context, @NotNull final VariableDescriptor... subjectVariables) {
        final DataFlowInfo[] result = new DataFlowInfo[] {context.dataFlowInfo};
        pattern.accept(new JetVisitorVoid() {
            @Override
            public void visitTypePattern(JetTypePattern typePattern) {
                JetTypeReference typeReference = typePattern.getTypeReference();
                if (typeReference != null) {
                    JetType type = context.getTypeResolver().resolveType(context.scope, typeReference);
                    checkTypeCompatibility(type, subjectType, typePattern);
                    result[0] = context.dataFlowInfo.isInstanceOf(subjectVariables, type);
                }
            }

            @Override
            public void visitTuplePattern(JetTuplePattern pattern) {
                List<JetTuplePatternEntry> entries = pattern.getEntries();
                TypeConstructor typeConstructor = subjectType.getConstructor();
                if (!JetStandardClasses.getTuple(entries.size()).getTypeConstructor().equals(typeConstructor)
                    || typeConstructor.getParameters().size() != entries.size()) {
//                        context.trace.getErrorHandler().genericError(pattern.getNode(), "Type mismatch: subject is of type " + subjectType + " but the pattern is of type Tuple" + entries.size());
                    context.trace.report(TYPE_MISMATCH_IN_TUPLE_PATTERN.on(pattern, subjectType, entries.size()));
                }
                else {
                    for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
                        JetTuplePatternEntry entry = entries.get(i);
                        JetType type = subjectType.getArguments().get(i).getType();

                        // TODO : is a name always allowed, ie for tuple patterns, not decomposer arg lists?
                        ASTNode nameLabelNode = entry.getNameLabelNode();
                        if (nameLabelNode != null) {
//                                context.trace.getErrorHandler().genericError(nameLabelNode, "Unsupported [OperatorConventions]");
                            context.trace.report(UNSUPPORTED.on(nameLabelNode, getClass().getCanonicalName()));
                        }

                        JetPattern entryPattern = entry.getPattern();
                        if (entryPattern != null) {
                            result[0] = result[0].and(checkPatternType(entryPattern, type, scopeToExtend, context));
                        }
                    }
                }
            }

            @Override
            public void visitDecomposerPattern(JetDecomposerPattern pattern) {
                JetExpression decomposerExpression = pattern.getDecomposerExpression();
                if (decomposerExpression != null) {
                    ReceiverDescriptor receiver = new TransientReceiver(subjectType);
                    JetType selectorReturnType = getSelectorReturnType(receiver, null, decomposerExpression, context);

                    result[0] = checkPatternType(pattern.getArgumentList(), selectorReturnType == null ? ErrorUtils.createErrorType("No type") : selectorReturnType, scopeToExtend, context);
                }
            }

            @Override
            public void visitWildcardPattern(JetWildcardPattern pattern) {
                // Nothing
            }

            @Override
            public void visitExpressionPattern(JetExpressionPattern pattern) {
                JetExpression expression = pattern.getExpression();
                if (expression != null) {
                    JetType type = getType(expression, context.replaceScope(scopeToExtend));
                    checkTypeCompatibility(type, subjectType, pattern);
                }
            }

            @Override
            public void visitBindingPattern(JetBindingPattern pattern) {
                JetProperty variableDeclaration = pattern.getVariableDeclaration();
                JetTypeReference propertyTypeRef = variableDeclaration.getPropertyTypeRef();
                JetType type = propertyTypeRef == null ? subjectType : context.getTypeResolver().resolveType(context.scope, propertyTypeRef);
                VariableDescriptor variableDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptorWithType(context.scope.getContainingDeclaration(), variableDeclaration, type);
                scopeToExtend.addVariableDescriptor(variableDescriptor);
                if (propertyTypeRef != null) {
                    if (!context.semanticServices.getTypeChecker().isSubtypeOf(subjectType, type)) {
//                            context.trace.getErrorHandler().genericError(propertyTypeRef.getNode(), type + " must be a supertype of " + subjectType + ". Use 'is' to match against " + type);
                        context.trace.report(TYPE_MISMATCH_IN_BINDING_PATTERN.on(propertyTypeRef, type, subjectType));
                    }
                }

                JetWhenCondition condition = pattern.getCondition();
                if (condition != null) {
                    int oldLength = subjectVariables.length;
                    VariableDescriptor[] newSubjectVariables = new VariableDescriptor[oldLength + 1];
                    System.arraycopy(subjectVariables, 0, newSubjectVariables, 0, oldLength);
                    newSubjectVariables[oldLength] = variableDescriptor;
                    result[0] = checkWhenCondition(null, subjectType, condition, scopeToExtend, context, newSubjectVariables);
                }
            }

            private void checkTypeCompatibility(@Nullable JetType type, @NotNull JetType subjectType, @NotNull JetElement reportErrorOn) {
                // TODO : Take auto casts into account?
                if (type == null) {
                    return;
                }
                if (TypeUtils.intersect(context.semanticServices.getTypeChecker(), Sets.newHashSet(type, subjectType)) == null) {
//                        context.trace.getErrorHandler().genericError(reportErrorOn.getNode(), "Incompatible types: " + type + " and " + subjectType);
                    context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType));
                }
            }

            @Override
            public void visitJetElement(JetElement element) {
//                    context.trace.getErrorHandler().genericError(element.getNode(), "Unsupported [OperatorConventions]");
                context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
            }
        });
        return result[0];
    }

    @Override
    public JetType visitTryExpression(JetTryExpression expression, ExpressionTypingContext context) {
        JetExpression tryBlock = expression.getTryBlock();
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        List<JetType> types = new ArrayList<JetType>();
        for (JetCatchClause catchClause : catchClauses) {
            JetParameter catchParameter = catchClause.getCatchParameter();
            JetExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                VariableDescriptor variableDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, catchParameter);
                if (catchBody != null) {
                    WritableScope catchScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("Catch scope");
                    catchScope.addVariableDescriptor(variableDescriptor);
                    JetType type = getType(catchBody, context.replaceScope(catchScope));
                    if (type != null) {
                        types.add(type);
                    }
                }
            }
        }
        if (finallyBlock != null) {
            types.clear(); // Do not need the list for the check, but need the code above to typecheck catch bodies
            JetType type = getType(finallyBlock.getFinalExpression(), context.replaceScope(context.scope));
            if (type != null) {
                types.add(type);
            }
        }
        JetType type = getType(tryBlock, context.replaceScope(context.scope));
        if (type != null) {
            types.add(type);
        }
        if (types.isEmpty()) {
            return null;
        }
        else {
            return context.semanticServices.getTypeChecker().commonSupertype(types);
        }
    }

    @Override
    public JetType visitIfExpression(JetIfExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);

        JetExpression elseBranch = expression.getElse();
        JetExpression thenBranch = expression.getThen();

        WritableScopeImpl thenScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("Then scope");
        DataFlowInfo thenInfo = extractDataFlowInfoFromCondition(condition, true, thenScope, context);
        DataFlowInfo elseInfo = extractDataFlowInfoFromCondition(condition, false, null, context);

        if (elseBranch == null) {
            if (thenBranch != null) {
                JetType type = getTypeWithNewScopeAndDataFlowInfo(thenScope, thenBranch, thenInfo, context);
                if (type != null && JetStandardClasses.isNothing(type)) {
                    resultDataFlowInfo = elseInfo;
//                        resultScope = elseScope;
                }
                return context.getServices().checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
            }
            return null;
        }
        if (thenBranch == null) {
            JetType type = getTypeWithNewScopeAndDataFlowInfo(context.scope, elseBranch, elseInfo, context);
            if (type != null && JetStandardClasses.isNothing(type)) {
                resultDataFlowInfo = thenInfo;
//                    resultScope = thenScope;
            }
            return context.getServices().checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
        }
        JetType thenType = getTypeWithNewScopeAndDataFlowInfo(thenScope, thenBranch, thenInfo, contextWithExpectedType);
        JetType elseType = getTypeWithNewScopeAndDataFlowInfo(context.scope, elseBranch, elseInfo, contextWithExpectedType);

        JetType result;
        if (thenType == null) {
            result = elseType;
        }
        else if (elseType == null) {
            result = thenType;
        }
        else {
            result = context.semanticServices.getTypeChecker().commonSupertype(Arrays.asList(thenType, elseType));
        }

        boolean jumpInThen = thenType != null && JetStandardClasses.isNothing(thenType);
        boolean jumpInElse = elseType != null && JetStandardClasses.isNothing(elseType);

        if (jumpInThen && !jumpInElse) {
            resultDataFlowInfo = elseInfo;
//                    resultScope = elseScope;
        }
        else if (jumpInElse && !jumpInThen) {
            resultDataFlowInfo = thenInfo;
//                    resultScope = thenScope;
        }
        return result;
    }

    @NotNull
    private DataFlowInfo extractDataFlowInfoFromCondition(@Nullable JetExpression condition, final boolean conditionValue, @Nullable final WritableScope scopeToExtend, final ExpressionTypingContext context) {
        if (condition == null) return context.dataFlowInfo;
        final DataFlowInfo[] result = new DataFlowInfo[] {context.dataFlowInfo};
        condition.accept(new JetVisitorVoid() {
            @Override
            public void visitIsExpression(JetIsExpression expression) {
                if (conditionValue && !expression.isNegated() || !conditionValue && expression.isNegated()) {
                    JetPattern pattern = expression.getPattern();
                    result[0] = context.patternsToDataFlowInfo.get(pattern);
                    if (scopeToExtend != null) {
                        List<VariableDescriptor> descriptors = context.patternsToBoundVariableLists.get(pattern);
                        if (descriptors != null) {
                            for (VariableDescriptor variableDescriptor : descriptors) {
                                scopeToExtend.addVariableDescriptor(variableDescriptor);
                            }
                        }
                    }
                }
            }

            @Override
            public void visitBinaryExpression(JetBinaryExpression expression) {
                IElementType operationToken = expression.getOperationToken();
                if (operationToken == JetTokens.ANDAND || operationToken == JetTokens.OROR) {
                    WritableScope actualScopeToExtend;
                    if (operationToken == JetTokens.ANDAND) {
                        actualScopeToExtend = conditionValue ? scopeToExtend : null;
                    }
                    else {
                        actualScopeToExtend = conditionValue ? null : scopeToExtend;
                    }

                    DataFlowInfo dataFlowInfo = extractDataFlowInfoFromCondition(expression.getLeft(), conditionValue, actualScopeToExtend, context);
                    JetExpression expressionRight = expression.getRight();
                    if (expressionRight != null) {
                        DataFlowInfo rightInfo = extractDataFlowInfoFromCondition(expressionRight, conditionValue, actualScopeToExtend, context);
                        DataFlowInfo.CompositionOperator operator;
                        if (operationToken == JetTokens.ANDAND) {
                            operator = conditionValue ? DataFlowInfo.AND : DataFlowInfo.OR;
                        }
                        else {
                            operator = conditionValue ? DataFlowInfo.OR : DataFlowInfo.AND;
                        }
                        dataFlowInfo = operator.compose(dataFlowInfo, rightInfo);
                    }
                    result[0] = dataFlowInfo;
                }
                else if (operationToken == JetTokens.EQEQ
                         || operationToken == JetTokens.EXCLEQ
                         || operationToken == JetTokens.EQEQEQ
                         || operationToken == JetTokens.EXCLEQEQEQ) {
                    JetExpression left = expression.getLeft();
                    JetExpression right = expression.getRight();
                    if (right == null) return;

                    if (!(left instanceof JetSimpleNameExpression)) {
                        JetExpression tmp = left;
                        left = right;
                        right = tmp;

                        if (!(left instanceof JetSimpleNameExpression)) {
                            return;
                        }
                    }

                    VariableDescriptor variableDescriptor = context.getServices().getVariableDescriptorFromSimpleName(left, context);
                    if (variableDescriptor == null) return;

                    // TODO : validate that DF makes sense for this variable: local, val, internal w/backing field, etc

                    // Comparison to a non-null expression
                    JetType rhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, right);
                    if (rhsType != null && !rhsType.isNullable()) {
                        extendDataFlowWithNullComparison(operationToken, variableDescriptor, !conditionValue);
                        return;
                    }

                    VariableDescriptor rightVariable = context.getServices().getVariableDescriptorFromSimpleName(right, context);
                    if (rightVariable != null) {
                        JetType lhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, left);
                        if (lhsType != null && !lhsType.isNullable()) {
                            extendDataFlowWithNullComparison(operationToken, rightVariable, !conditionValue);
                            return;
                        }
                    }

                    // Comparison to 'null'
                    if (!(right instanceof JetConstantExpression)) {
                        return;
                    }
                    JetConstantExpression constantExpression = (JetConstantExpression) right;
                    if (constantExpression.getNode().getElementType() != JetNodeTypes.NULL) {
                        return;
                    }

                    extendDataFlowWithNullComparison(operationToken, variableDescriptor, conditionValue);
                }
            }

            private void extendDataFlowWithNullComparison(IElementType operationToken, @NotNull VariableDescriptor variableDescriptor, boolean equalsToNull) {
                if (operationToken == JetTokens.EQEQ || operationToken == JetTokens.EQEQEQ) {
                    result[0] = context.dataFlowInfo.equalsToNull(variableDescriptor, !equalsToNull);
                }
                else if (operationToken == JetTokens.EXCLEQ || operationToken == JetTokens.EXCLEQEQEQ) {
                    result[0] = context.dataFlowInfo.equalsToNull(variableDescriptor, equalsToNull);
                }
            }

            @Override
            public void visitUnaryExpression(JetUnaryExpression expression) {
                IElementType operationTokenType = expression.getOperationSign().getReferencedNameElementType();
                if (operationTokenType == JetTokens.EXCL) {
                    JetExpression baseExpression = expression.getBaseExpression();
                    if (baseExpression != null) {
                        result[0] = extractDataFlowInfoFromCondition(baseExpression, !conditionValue, scopeToExtend, context);
                    }
                }
            }

            @Override
            public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
                JetExpression body = expression.getExpression();
                if (body != null) {
                    body.accept(this);
                }
            }
        });
        if (result[0] == null) {
            return context.dataFlowInfo;
        }
        return result[0];
    }

    private void checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition, ExpressionTypingContext context) {
        if (condition != null) {
            JetType conditionType = getType(condition, context.replaceScope(scope));

            if (conditionType != null && !isBoolean(context.semanticServices, conditionType)) {
//                    context.trace.getErrorHandler().genericError(condition.getNode(), "Condition must be of type Boolean, but was of type " + conditionType);
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(condition, conditionType));
            }
        }
    }

    @Override
    public JetType visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);
        JetExpression body = expression.getBody();
        if (body != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context.scope, context.trace).setDebugName("Scope extended in while's condition");
            DataFlowInfo conditionInfo = condition == null ? context.dataFlowInfo : extractDataFlowInfoFromCondition(condition, true, scopeToExtend, context);
            getTypeWithNewScopeAndDataFlowInfo(scopeToExtend, body, conditionInfo, context);
        }
        if (!containsBreak(expression, context)) {
//                resultScope = newWritableScopeImpl();
            resultDataFlowInfo = extractDataFlowInfoFromCondition(condition, false, null, context);
        }
        return context.getServices().checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
    }

    private boolean containsBreak(final JetLoopExpression loopExpression, final ExpressionTypingContext context) {
        final boolean[] result = new boolean[1];
        result[0] = false;
        //todo breaks in inline function literals
        loopExpression.visit(new JetTreeVisitor<JetLoopExpression>() {
            @Override
            public Void visitBreakExpression(JetBreakExpression breakExpression, JetLoopExpression outerLoop) {
                JetSimpleNameExpression targetLabel = breakExpression.getTargetLabel();
                PsiElement element = targetLabel != null ? context.trace.get(LABEL_TARGET, targetLabel) : null;
                if (element == loopExpression || (targetLabel == null && outerLoop == loopExpression)) {
                    result[0] = true;
                }
                return null;
            }

            @Override
            public Void visitLoopExpression(JetLoopExpression loopExpression, JetLoopExpression outerLoop) {
                return super.visitLoopExpression(loopExpression, loopExpression);
            }
        }, loopExpression);

        return result[0];
    }

    @Override
    public JetType visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression body = expression.getBody();
        JetScope conditionScope = context.scope;
        if (body instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression function = (JetFunctionLiteralExpression) body;
            if (!function.getFunctionLiteral().hasParameterSpecification()) {
                WritableScope writableScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("do..while body scope");
                conditionScope = writableScope;
                context.getServices().getBlockReturnedTypeWithWritableScope(writableScope, function.getFunctionLiteral().getBodyExpression().getStatements(), CoercionStrategy.NO_COERCION, context);
                context.trace.record(BindingContext.BLOCK, function);
            } else {
                getType(body, context.replaceScope(context.scope));
            }
        }
        else if (body != null) {
            WritableScope writableScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("do..while body scope");
            conditionScope = writableScope;
            context.getServices().getBlockReturnedTypeWithWritableScope(writableScope, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context);
        }
        JetExpression condition = expression.getCondition();
        checkCondition(conditionScope, condition, context);
        if (!containsBreak(expression, context)) {
//                resultScope = newWritableScopeImpl();
            resultDataFlowInfo = extractDataFlowInfoFromCondition(condition, false, null, context);
        }
        return context.getServices().checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
    }

    protected WritableScopeImpl newWritableScopeImpl(JetScope scope, BindingTrace trace) {
        return new WritableScopeImpl(scope, scope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(trace));
    }

    @Override
    public JetType visitForExpression(JetForExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetParameter loopParameter = expression.getLoopParameter();
        JetExpression loopRange = expression.getLoopRange();
        JetType expectedParameterType = null;
        if (loopRange != null) {
            ExpressionReceiver loopRangeReceiver = getExpressionReceiver(loopRange, context.replaceScope(context.scope));
            if (loopRangeReceiver != null) {
                expectedParameterType = checkIterableConvention(loopRangeReceiver, context);
            }
        }

        WritableScope loopScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("Scope with for-loop index");

        if (loopParameter != null) {
            JetTypeReference typeReference = loopParameter.getTypeReference();
            VariableDescriptor variableDescriptor;
            if (typeReference != null) {
                variableDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, loopParameter);
                JetType actualParameterType = variableDescriptor.getOutType();
                if (expectedParameterType != null &&
                        actualParameterType != null &&
                        !context.semanticServices.getTypeChecker().isSubtypeOf(expectedParameterType, actualParameterType)) {
//                        context.trace.getErrorHandler().genericError(typeReference.getNode(), "The loop iterates over values of type " + expectedParameterType + " but the parameter is declared to be " + actualParameterType);
                    context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
                }
            }
            else {
                if (expectedParameterType == null) {
                    expectedParameterType = ErrorUtils.createErrorType("Error");
                }
                variableDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), loopParameter, expectedParameterType);
            }
            loopScope.addVariableDescriptor(variableDescriptor);
        }

        JetExpression body = expression.getBody();
        if (body != null) {
            getType(body, context.replaceScope(loopScope));
        }

        return context.getServices().checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
    }

    @Nullable
    private JetType checkIterableConvention(@NotNull ExpressionReceiver loopRange, ExpressionTypingContext context) {
        JetExpression loopRangeExpression = loopRange.getExpression();
        OverloadResolutionResults<FunctionDescriptor> iteratorResolutionResults = context.resolveExactSignature(loopRange, "iterator", Collections.<JetType>emptyList());
        if (iteratorResolutionResults.isSuccess()) {
            FunctionDescriptor iteratorFunction = iteratorResolutionResults.getResult().getResultingDescriptor();

            context.trace.record(LOOP_RANGE_ITERATOR, loopRangeExpression, iteratorFunction);

            JetType iteratorType = iteratorFunction.getReturnType();
            FunctionDescriptor hasNextFunction = checkHasNextFunctionSupport(loopRangeExpression, iteratorType, context);
            boolean hasNextFunctionSupported = hasNextFunction != null;
            VariableDescriptor hasNextProperty = checkHasNextPropertySupport(loopRangeExpression, iteratorType, context);
            boolean hasNextPropertySupported = hasNextProperty != null;
            if (hasNextFunctionSupported && hasNextPropertySupported && !ErrorUtils.isErrorType(iteratorType)) {
                // TODO : overload resolution rules impose priorities here???
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "An ambiguity between 'iterator().hasNext()' function and 'iterator().hasNext' property");
                context.trace.report(HAS_NEXT_PROPERTY_AND_FUNCTION_AMBIGUITY.on(loopRangeExpression));
            }
            else if (!hasNextFunctionSupported && !hasNextPropertySupported) {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().hasNext()' function or an 'iterator().hasNext' property");
                context.trace.report(HAS_NEXT_MISSING.on(loopRangeExpression));
            }
            else {
                context.trace.record(LOOP_RANGE_HAS_NEXT, loopRange.getExpression(), hasNextFunctionSupported ? hasNextFunction : hasNextProperty);
            }

            OverloadResolutionResults<FunctionDescriptor> nextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), "next", Collections.<JetType>emptyList());
            if (nextResolutionResults.isAmbiguity()) {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Method 'iterator().next()' is ambiguous for this expression");
                context.trace.report(NEXT_AMBIGUITY.on(loopRangeExpression));
            } else if (nextResolutionResults.isNothing()) {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().next()' method");
                context.trace.report(NEXT_MISSING.on(loopRangeExpression));
            } else {
                FunctionDescriptor nextFunction = nextResolutionResults.getResult().getResultingDescriptor();
                context.trace.record(LOOP_RANGE_NEXT, loopRange.getExpression(), nextFunction);
                return nextFunction.getReturnType();
            }
        }
        else {
            if (iteratorResolutionResults.isAmbiguity()) {
//                    StringBuffer stringBuffer = new StringBuffer("Method 'iterator()' is ambiguous for this expression: ");
//                    for (FunctionDescriptor functionDescriptor : iteratorResolutionResults.getResults()) {
//                        stringBuffer.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
//                    }
//                    errorMessage = stringBuffer.toString();
                context.trace.report(ITERATOR_AMBIGUITY.on(loopRangeExpression, iteratorResolutionResults.getResults()));
            }
            else {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, errorMessage);
                context.trace.report(ITERATOR_MISSING.on(loopRangeExpression));
            }
        }
        return null;
    }

    @Nullable
    private FunctionDescriptor checkHasNextFunctionSupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        OverloadResolutionResults<FunctionDescriptor> hasNextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), "hasNext", Collections.<JetType>emptyList());
        if (hasNextResolutionResults.isAmbiguity()) {
//                context.trace.getErrorHandler().genericError(loopRange.getNode(), "Method 'iterator().hasNext()' is ambiguous for this expression");
            context.trace.report(HAS_NEXT_FUNCTION_AMBIGUITY.on(loopRange));
        } else if (hasNextResolutionResults.isNothing()) {
            return null;
        } else {
            assert hasNextResolutionResults.isSuccess();
            JetType hasNextReturnType = hasNextResolutionResults.getResult().getResultingDescriptor().getReturnType();
            if (!isBoolean(context.semanticServices, hasNextReturnType)) {
//                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext()' method of the loop range must return Boolean, but returns " + hasNextReturnType);
                context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRange, hasNextReturnType));
            }
        }
        return hasNextResolutionResults.getResult().getResultingDescriptor();
    }

    @Nullable
    private VariableDescriptor checkHasNextPropertySupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        VariableDescriptor hasNextProperty = iteratorType.getMemberScope().getVariable("hasNext");
        // TODO :extension properties
        if (hasNextProperty == null) {
            return null;
        } else {
            JetType hasNextReturnType = hasNextProperty.getOutType();
            if (hasNextReturnType == null) {
                // TODO : accessibility
//                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext' property of the loop range must be readable");
                context.trace.report(HAS_NEXT_MUST_BE_READABLE.on(loopRange));
            }
            else if (!isBoolean(context.semanticServices, hasNextReturnType)) {
//                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext' property of the loop range must return Boolean, but returns " + hasNextReturnType);
                context.trace.report(HAS_NEXT_PROPERTY_TYPE_MISMATCH.on(loopRange, hasNextReturnType));
            }
        }
        return hasNextProperty;
    }

    @Override
    public JetType visitHashQualifiedExpression(JetHashQualifiedExpression expression, ExpressionTypingContext context) {
//            context.trace.getErrorHandler().genericError(expression.getOperationTokenNode(), "Unsupported");
        context.trace.report(UNSUPPORTED.on(expression, getClass().getCanonicalName()));
        return null;
    }

    @Override
    public JetType visitQualifiedExpression(JetQualifiedExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        // TODO : functions as values
        JetExpression selectorExpression = expression.getSelectorExpression();
        JetExpression receiverExpression = expression.getReceiverExpression();
        JetType receiverType = ExpressionTyperVisitorWithNamespaces.INSTANCE.getType(receiverExpression, context.replaceExpectedTypes(TypeUtils.NO_EXPECTED_TYPE, TypeUtils.NO_EXPECTED_TYPE));
        if (selectorExpression == null) return null;
        if (receiverType == null) receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());

        if (selectorExpression instanceof JetSimpleNameExpression) {
            propagateConstantValues(expression, context, (JetSimpleNameExpression) selectorExpression);
        }

        // Clean resolution: no autocasts
//            TemporaryBindingTrace cleanResolutionTrace = TemporaryBindingTrace.create(context.trace);
//            ExpressionTypingContext cleanResolutionContext = context.replaceBindingTrace(cleanResolutionTrace);
        JetType selectorReturnType = getSelectorReturnType(new ExpressionReceiver(receiverExpression, receiverType), expression.getOperationTokenNode(), selectorExpression, context);

        //TODO move further
        if (expression.getOperationSign() == JetTokens.SAFE_ACCESS) {
            if (selectorReturnType != null && !selectorReturnType.isNullable() && !JetStandardClasses.isUnit(selectorReturnType)) {
                selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
            }
        }
//            if (selectorReturnType != null) {
//                cleanResolutionTrace.addAllMyDataTo(context.trace);
//            }
//            else {
//                VariableDescriptor variableDescriptor = cleanResolutionContext.services.getVariableDescriptorFromSimpleName(receiverExpression, context);
//                boolean somethingFound = false;
//                if (variableDescriptor != null) {
//                    List<JetType> possibleTypes = Lists.newArrayList(context.dataFlowInfo.getPossibleTypesForVariable(variableDescriptor));
//                    Collections.reverse(possibleTypes);
//
//                    TemporaryBindingTrace autocastResolutionTrace = TemporaryBindingTrace.create(context.trace);
//                    ExpressionTypingContext autocastResolutionContext = context.replaceBindingTrace(autocastResolutionTrace);
//                    for (JetType possibleType : possibleTypes) {
//                        selectorReturnType = getSelectorReturnType(new ExpressionReceiver(receiverExpression, possibleType), selectorExpression, autocastResolutionContext);
//                        if (selectorReturnType != null) {
//                            context.getServices().checkAutoCast(receiverExpression, possibleType, variableDescriptor, autocastResolutionTrace);
//                            autocastResolutionTrace.commit();
//                            somethingFound = true;
//                            break;
//                        }
//                        else {
//                            autocastResolutionTrace = TemporaryBindingTrace.create(context.trace);
//                            autocastResolutionContext = context.replaceBindingTrace(autocastResolutionTrace);
//                        }
//                    }
//                }
//                if (!somethingFound) {
//                    cleanResolutionTrace.commit();
//                }
//            }

        JetType result;
        if (expression.getOperationSign() == JetTokens.QUEST) {
            if (selectorReturnType != null && !isBoolean(context.semanticServices, selectorReturnType)) {
                // TODO : more comprehensible error message
//                    context.trace.getErrorHandler().typeMismatch(selectorExpression, semanticServices.getStandardLibrary().getBooleanType(), selectorReturnType);
                context.trace.report(TYPE_MISMATCH.on(selectorExpression, context.semanticServices.getStandardLibrary().getBooleanType(), selectorReturnType));
            }
            result = TypeUtils.makeNullable(receiverType);
        }
        else {
            result = selectorReturnType;
        }
        // TODO : this is suspicious: remove this code?
        if (result != null) {
            context.trace.record(BindingContext.EXPRESSION_TYPE, selectorExpression, result);
        }
        if (selectorReturnType != null) {
//                // TODO : extensions to 'Any?'
//                receiverType = context.getServices().enrichOutType(receiverExpression, receiverType, context);
//
//                context.getServices().checkNullSafety(receiverType, expression.getOperationTokenNode(), getCalleeFunctionDescriptor(selectorExpression, context), expression);
        }
        return context.getServices().checkType(result, expression, contextWithExpectedType);
    }

    private void propagateConstantValues(JetQualifiedExpression expression, ExpressionTypingContext context, JetSimpleNameExpression selectorExpression) {
        JetExpression receiverExpression = expression.getReceiverExpression();
        CompileTimeConstant<?> receiverValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, receiverExpression);
        CompileTimeConstant<?> wholeExpressionValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (wholeExpressionValue == null && receiverValue != null && !(receiverValue instanceof ErrorValue) && receiverValue.getValue() instanceof Number) {
            Number value = (Number) receiverValue.getValue();
            String referencedName = selectorExpression.getReferencedName();
            if (OperatorConventions.NUMBER_CONVERSIONS.contains(referencedName)) {
                if ("dbl".equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new DoubleValue(value.doubleValue()));
                }
                else if ("flt".equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new FloatValue(value.floatValue()));
                }
                else if ("lng".equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new LongValue(value.longValue()));
                }
                else if ("sht".equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ShortValue(value.shortValue()));
                }
                else if ("byt".equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ByteValue(value.byteValue()));
                }
                else if ("int".equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new IntValue(value.intValue()));
                }
            }
        }
    }

//        @NotNull
//        private FunctionDescriptor getCalleeFunctionDescriptor(@NotNull JetExpression selectorExpression, final ExpressionTypingContext context) {
//            final FunctionDescriptor[] result = new FunctionDescriptor[1];
//            selectorExpression.accept(new JetVisitorVoid() {
//                @Override
//                public void visitCallExpression(JetCallExpression callExpression) {
//                    JetExpression calleeExpression = callExpression.getCalleeExpression();
//                    if (calleeExpression != null) {
//                        calleeExpression.accept(this);
//                    }
//                }
//
//                @Override
//                public void visitReferenceExpression(JetReferenceExpression referenceExpression) {
//                    DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(REFERENCE_TARGET, referenceExpression);
//                    if (declarationDescriptor instanceof FunctionDescriptor) {
//                        result[0] = (FunctionDescriptor) declarationDescriptor;
//                    }
//                }
//
//                @Override
//                public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
//                    expression.getArrayExpression().accept(this);
//                }
//
//                @Override
//                public void visitBinaryExpression(JetBinaryExpression expression) {
//                    expression.getLeft().accept(this);
//                }
//
//                @Override
//                public void visitQualifiedExpression(JetQualifiedExpression expression) {
//                    expression.getReceiverExpression().accept(this);
//                }
//
//                @Override
//                public void visitJetElement(JetElement element) {
////                    context.trace.getErrorHandler().genericError(element.getNode(), "Unsupported [getCalleeFunctionDescriptor]: " + element);
//                    context.trace.report(UNSUPPORTED.on(element, "getCalleeFunctionDescriptor"));
//                }
//            });
//            if (result[0] == null) {
//                result[0] = ErrorUtils.createErrorFunction(0, Collections.<JetType>emptyList());
//            }
//            return result[0];
//        }

    @Nullable
    private JetType getSelectorReturnType(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetExpression selectorExpression, @NotNull ExpressionTypingContext context) {
        if (selectorExpression instanceof JetCallExpression) {
            JetCallExpression callExpression = (JetCallExpression) selectorExpression;
            return context.resolveCall(receiver, callOperationNode, callExpression);
        }
        else if (selectorExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) selectorExpression;

            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
            VariableDescriptor variableDescriptor = context.replaceBindingTrace(temporaryTrace).resolveSimpleProperty(receiver, callOperationNode, nameExpression);
            if (variableDescriptor != null) {
                temporaryTrace.commit();
                return context.getServices().checkType(variableDescriptor.getOutType(), nameExpression, context);
            }
            ExpressionTypingContext newContext = receiver.exists() ? context.replaceScope(receiver.getType().getMemberScope()) : context;
            JetType jetType = lookupNamespaceOrClassObject(nameExpression, nameExpression.getReferencedName(), newContext);
            if (jetType == null) {
                context.trace.report(UNRESOLVED_REFERENCE.on(nameExpression));
            }
            return context.getServices().checkType(jetType, nameExpression, context);
//                JetScope scope = receiverType != null ? receiverType.getMemberScope() : context.scope;
//                return getType(selectorExpression, context.replaceScope(scope));
        }
        else if (selectorExpression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) selectorExpression;
            JetExpression newReceiverExpression = qualifiedExpression.getReceiverExpression();
            JetType newReceiverType = getSelectorReturnType(receiver, callOperationNode, newReceiverExpression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
            JetExpression newSelectorExpression = qualifiedExpression.getSelectorExpression();
            if (newReceiverType != null && newSelectorExpression != null) {
                return getSelectorReturnType(new ExpressionReceiver(newReceiverExpression, newReceiverType), qualifiedExpression.getOperationTokenNode(), newSelectorExpression, context);
            }
        }
        else {
            // TODO : not a simple name -> resolve in scope, expect property type or a function type
//                context.trace.getErrorHandler().genericError(selectorExpression.getNode(), "Unsupported selector element type: " + selectorExpression);
            context.trace.report(UNSUPPORTED.on(selectorExpression, "getSelectorReturnType"));
        }
        return null;
    }

    @Override
    public JetType visitCallExpression(JetCallExpression expression, ExpressionTypingContext context) {
        JetType expressionType = context.resolveCall(NO_RECEIVER, null, expression);
        return context.getServices().checkType(expressionType, expression, context);
    }

    @Override
    public JetType visitIsExpression(JetIsExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetType knownType = safeGetType(expression.getLeftHandSide(), context.replaceScope(context.scope));
        JetPattern pattern = expression.getPattern();
        if (pattern != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context.scope, context.trace).setDebugName("Scope extended in 'is'");
            DataFlowInfo newDataFlowInfo = checkPatternType(pattern, knownType, scopeToExtend, context, context.getServices().getVariableDescriptorFromSimpleName(expression.getLeftHandSide(), context));
            context.patternsToDataFlowInfo.put(pattern, newDataFlowInfo);
            context.patternsToBoundVariableLists.put(pattern, scopeToExtend.getDeclaredVariables());
        }
        return context.getServices().checkType(context.semanticServices.getStandardLibrary().getBooleanType(), expression, contextWithExpectedType);
    }

    @Override
    public JetType visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext context) {
        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return null;
        JetSimpleNameExpression operationSign = expression.getOperationSign();
        if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
            String referencedName = operationSign.getReferencedName();
            referencedName = referencedName == null ? " <?>" : referencedName;
            context.labelResolver.enterLabeledElement(referencedName.substring(1), baseExpression);
            // TODO : Some processing for the label?
            JetType type = context.getServices().checkType(getType(baseExpression, context.replaceExpectedReturnType(context.expectedType)), expression, context);
            context.labelResolver.exitLabeledElement(baseExpression);
            return type;
        }
        IElementType operationType = operationSign.getReferencedNameElementType();
        String name = OperatorConventions.UNARY_OPERATION_NAMES.get(operationType);
        if (name == null) {
//                context.trace.getErrorHandler().genericError(operationSign.getNode(), "Unknown unary operation");
            context.trace.report(UNSUPPORTED.on(operationSign, "visitUnaryExpression"));
            return null;
        }
        ExpressionReceiver receiver = getExpressionReceiver(baseExpression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceScope(context.scope));
        if (receiver == null) return null;

        FunctionDescriptor functionDescriptor = context.resolveCallWithGivenName(
                CallMaker.makeCall(receiver, expression),
                expression.getOperationSign(),
                name,
                receiver);

        if (functionDescriptor == null) return null;
        JetType returnType = functionDescriptor.getReturnType();
        JetType result;
        if (operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) {
            if (context.semanticServices.getTypeChecker().isSubtypeOf(returnType, JetStandardClasses.getUnitType())) {
                result = JetStandardClasses.getUnitType();
            }
            else {
                JetType receiverType = receiver.getType();
                if (!context.semanticServices.getTypeChecker().isSubtypeOf(returnType, receiverType)) {
//                        context.trace.getErrorHandler().genericError(operationSign.getNode(), name + " must return " + receiverType + " but returns " + returnType);
                    context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, name, receiverType, returnType));
                }
                else {
                    context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression);
                }
                // TODO : Maybe returnType?
                result = receiverType;
            }
        }
        else {
            result = returnType;
        }

        return context.getServices().checkType(result, expression, context);
    }

    @Override
    public JetType visitBinaryExpression(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetSimpleNameExpression operationSign = expression.getOperationReference();

        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();

        JetType result = null;
        IElementType operationType = operationSign.getReferencedNameElementType();
        if (operationType == JetTokens.IDENTIFIER) {
            String referencedName = operationSign.getReferencedName();
            if (referencedName != null) {
                result = getTypeForBinaryCall(context.scope, referencedName, context, expression);
            }
        }
        else if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            result = getTypeForBinaryCall(context.scope, OperatorConventions.BINARY_OPERATION_NAMES.get(operationType), context, expression);
        }
        else if (operationType == JetTokens.EQ) {
            result = visitAssignment(expression, contextWithExpectedType);
        }
        else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, contextWithExpectedType);
        }
        else if (OperatorConventions.COMPARISON_OPERATIONS.contains(operationType)) {
            JetType compareToReturnType = getTypeForBinaryCall(context.scope, "compareTo", context, expression);
            if (compareToReturnType != null) {
                TypeConstructor constructor = compareToReturnType.getConstructor();
                JetStandardLibrary standardLibrary = context.semanticServices.getStandardLibrary();
                TypeConstructor intTypeConstructor = standardLibrary.getInt().getTypeConstructor();
                if (constructor.equals(intTypeConstructor)) {
                    result = standardLibrary.getBooleanType();
                } else {
//                        context.trace.getErrorHandler().genericError(operationSign.getNode(), "compareTo() must return Int, but returns " + compareToReturnType);
                    context.trace.report(COMPARE_TO_TYPE_MISMATCH.on(operationSign, compareToReturnType));
                }
            }
        }
        else if (OperatorConventions.EQUALS_OPERATIONS.contains(operationType)) {
            String name = "equals";
            if (right != null) {
                ExpressionReceiver receiver = safeGetExpressionReceiver(left, context.replaceScope(context.scope));
                OverloadResolutionResults<FunctionDescriptor> resolutionResults = context.resolveExactSignature(
                        receiver, "equals",
                        Collections.singletonList(JetStandardClasses.getNullableAnyType()));
                if (resolutionResults.isSuccess()) {
                    FunctionDescriptor equals = resolutionResults.getResult().getResultingDescriptor();
                    context.trace.record(REFERENCE_TARGET, operationSign, equals);
                    if (ensureBooleanResult(operationSign, name, equals.getReturnType(), context)) {
                        ensureNonemptyIntersectionOfOperandTypes(expression, context);
                    }
                }
                else {
                    if (resolutionResults.isAmbiguity()) {
//                            StringBuilder stringBuilder = new StringBuilder();
//                            for (FunctionDescriptor functionDescriptor : resolutionResults.getResults()) {
//                                stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
//                            }
//                            context.trace.getErrorHandler().genericError(operationSign.getNode(), "Ambiguous function: " + stringBuilder);
                        context.trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(operationSign, resolutionResults.getResults()));
                    }
                    else {
//                            context.trace.getErrorHandler().genericError(operationSign.getNode(), "No method 'equals(Any?) : Boolean' available");
                        context.trace.report(EQUALS_MISSING.on(operationSign));
                    }
                }
            }
            result = context.semanticServices.getStandardLibrary().getBooleanType();
        }
        else if (operationType == JetTokens.EQEQEQ || operationType == JetTokens.EXCLEQEQEQ) {
            ensureNonemptyIntersectionOfOperandTypes(expression, context);

            // TODO : Check comparison pointlessness
            result = context.semanticServices.getStandardLibrary().getBooleanType();
        }
        else if (OperatorConventions.IN_OPERATIONS.contains(operationType)) {
            if (right == null) {
                result = ErrorUtils.createErrorType("No right argument"); // TODO
                return null;
            }
            checkInExpression(expression, expression.getOperationReference(), expression.getLeft(), expression.getRight(), context);
            result = context.semanticServices.getStandardLibrary().getBooleanType();
        }
        else if (operationType == JetTokens.ANDAND || operationType == JetTokens.OROR) {
            JetType leftType = getType(left, context.replaceScope(context.scope));
            WritableScopeImpl leftScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("Left scope of && or ||");
            DataFlowInfo flowInfoLeft = extractDataFlowInfoFromCondition(left, operationType == JetTokens.ANDAND, leftScope, context);  // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
            WritableScopeImpl rightScope = operationType == JetTokens.ANDAND ? leftScope : newWritableScopeImpl(context.scope, context.trace).setDebugName("Right scope of && or ||");
            JetType rightType = right == null ? null : getType(right, context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope));
            if (leftType != null && !isBoolean(context.semanticServices, leftType)) {
//                    context.trace.getErrorHandler().typeMismatch(left, semanticServices.getStandardLibrary().getBooleanType(), leftType);
                context.trace.report(TYPE_MISMATCH.on(left, context.semanticServices.getStandardLibrary().getBooleanType(), leftType));
            }
            if (rightType != null && !isBoolean(context.semanticServices, rightType)) {
//                    context.trace.getErrorHandler().typeMismatch(right, semanticServices.getStandardLibrary().getBooleanType(), rightType);
                context.trace.report(TYPE_MISMATCH.on(right, context.semanticServices.getStandardLibrary().getBooleanType(), rightType));
            }
            result = context.semanticServices.getStandardLibrary().getBooleanType();
        }
        else if (operationType == JetTokens.ELVIS) {
            JetType leftType = getType(left, context.replaceScope(context.scope));
            JetType rightType = right == null ? null : getType(right, contextWithExpectedType.replaceScope(context.scope));
            if (leftType != null) {
                if (!leftType.isNullable()) {
//                        context.trace.getErrorHandler().genericWarning(left.getNode(), "Elvis operator (?:) always returns the left operand of non-nullable type " + leftType);
                    context.trace.report(USELESS_ELVIS.on(expression, left, leftType));
                }
                if (rightType != null) {
                    context.getServices().checkType(TypeUtils.makeNullableAsSpecified(leftType, rightType.isNullable()), left, contextWithExpectedType);
                    return TypeUtils.makeNullableAsSpecified(context.semanticServices.getTypeChecker().commonSupertype(leftType, rightType), rightType.isNullable());
                }
            }
        }
        else {
//                context.trace.getErrorHandler().genericError(operationSign.getNode(), "Unknown operation");
            context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"));
        }
        return context.getServices().checkType(result, expression, contextWithExpectedType);
    }

    private void checkInExpression(JetElement callElement, @NotNull JetSimpleNameExpression operationSign, @NotNull JetExpression left, @NotNull JetExpression right, ExpressionTypingContext context) {
        String name = "contains";
        ExpressionReceiver receiver = safeGetExpressionReceiver(right, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
        FunctionDescriptor functionDescriptor = context.resolveCallWithGivenName(
                CallMaker.makeCallWithExpressions(callElement, receiver, null, operationSign, Collections.singletonList(left)),
                operationSign,
                name, receiver);
        JetType containsType = functionDescriptor != null ? functionDescriptor.getReturnType() : null;
        ensureBooleanResult(operationSign, name, containsType, context);
    }

    private void ensureNonemptyIntersectionOfOperandTypes(JetBinaryExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();

        // TODO : duplicated effort for == and !=
        JetType leftType = getType(left, context.replaceScope(context.scope));
        if (leftType != null && right != null) {
            JetType rightType = getType(right, context.replaceScope(context.scope));

            if (rightType != null) {
                JetType intersect = TypeUtils.intersect(context.semanticServices.getTypeChecker(), new HashSet<JetType>(Arrays.asList(leftType, rightType)));
                if (intersect == null) {
//                        context.trace.getErrorHandler().genericError(expression.getNode(), "Operator " + operationSign.getReferencedName() + " cannot be applied to " + leftType + " and " + rightType);
                    context.trace.report(EQUALITY_NOT_APPLICABLE.on(expression, operationSign, leftType, rightType));
                }
            }
        }
    }

    protected JetType visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    protected JetType visitAssignment(JetBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    private JetType assignmentIsNotAnExpressionError(JetBinaryExpression expression, ExpressionTypingContext context) {
//            context.trace.getErrorHandler().genericError(expression.getNode(), "Assignments are not expressions, and only expressions are allowed in this context");
        context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression));
        return null;
    }

    private boolean ensureBooleanResult(JetExpression operationSign, String name, JetType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    private boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, ExpressionTypingContext context) {
        if (resultType != null) {
            // TODO : Relax?
            if (!isBoolean(context.semanticServices, resultType)) {
//                    context.trace.getErrorHandler().genericError(operationSign.getNode(), subjectName + " must return Boolean but returns " + resultType);
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, context.semanticServices.getStandardLibrary().getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    private boolean isBoolean(JetSemanticServices semanticServices, @NotNull JetType type) {
        return semanticServices.getTypeChecker().isConvertibleTo(type, semanticServices.getStandardLibrary().getBooleanType());
    }

    @Override
    public JetType visitArrayAccessExpression(JetArrayAccessExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression arrayExpression = expression.getArrayExpression();
        ExpressionReceiver receiver = getExpressionReceiver(arrayExpression, context.replaceScope(context.scope));

        if (receiver != null) {
            FunctionDescriptor functionDescriptor = context.resolveCallWithGivenName(
                    CallMaker.makeCallWithExpressions(expression, receiver, null, expression, expression.getIndexExpressions()),
                    expression,
                    "get",
                    receiver);
            if (functionDescriptor != null) {
                return context.getServices().checkType(functionDescriptor.getReturnType(), expression, contextWithExpectedType);
            }
        }
        return null;
    }

    @Nullable
    protected JetType getTypeForBinaryCall(JetScope scope, String name, ExpressionTypingContext context, JetBinaryExpression binaryExpression) {
        ExpressionReceiver receiver = safeGetExpressionReceiver(binaryExpression.getLeft(), context.replaceScope(scope));
        FunctionDescriptor functionDescriptor = context.replaceScope(scope).resolveCallWithGivenName(
                CallMaker.makeCall(receiver, binaryExpression),
                binaryExpression.getOperationReference(),
                name,
                receiver);
        if (functionDescriptor != null) {
//                if (receiver.getType().isNullable()) {
//                    // TODO : better error message for '1 + nullableVar' case
//                    JetExpression right = binaryExpression.getRight();
//                    String rightText = right == null ? "" : right.getText();
//                    String leftText = binaryExpression.getLeft().getText();
////                    context.trace.getErrorHandler().genericError(binaryExpression.getOperationReference().getNode(),
////                                                                 "Infix call corresponds to a dot-qualified call '" +
////                                                                 leftText + "." + name + "(" + rightText + ")'" +
////                                                                 " which is not allowed on a nullable receiver '" + leftText + "'." +
////                                                                 " Use '?.'-qualified call instead");
//                    context.trace.report(UNSAFE_INFIX_CALL.on(binaryExpression.getOperationReference(), leftText, name, rightText));
//                }


            return functionDescriptor.getReturnType();
        }
        return null;
    }

    @Override
    public JetType visitDeclaration(JetDeclaration dcl, ExpressionTypingContext context) {
//            context.trace.getErrorHandler().genericError(dcl.getNode(), "Declarations are not allowed in this position");
        context.trace.report(DECLARATION_IN_ILLEGAL_CONTEXT.on(dcl));
        return null;
    }

    @Override
    public JetType visitRootNamespaceExpression(JetRootNamespaceExpression expression, ExpressionTypingContext context) {
//            context.trace.getErrorHandler().genericError(expression.getNode(), "'namespace' is not an expression");
        context.trace.report(NAMESPACE_IS_NOT_AN_EXPRESSION.on(expression));
        return null;
    }


    @Override
    public JetType visitStringTemplateExpression(JetStringTemplateExpression expression, ExpressionTypingContext contextWithExpectedType) {
        final ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        final StringBuilder builder = new StringBuilder();
        final CompileTimeConstant<?>[] value = new CompileTimeConstant<?>[1];
        for (JetStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(new JetVisitorVoid() {

                @Override
                public void visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry) {
                    JetExpression entryExpression = entry.getExpression();
                    if (entryExpression != null) {
                        getType(entryExpression, context.replaceScope(context.scope));
                    }
                    value[0] = CompileTimeConstantResolver.OUT_OF_RANGE;
                }

                @Override
                public void visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry) {
                    builder.append(entry.getText());
                }

                @Override
                public void visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry) {
                    // TODO : Check escape
                    String text = entry.getText();
                    assert text.length() == 2 && text.charAt(0) == '\\';
                    char escaped = text.charAt(1);

                    Character character = CompileTimeConstantResolver.translateEscape(escaped);
                    if (character == null) {
//                            context.trace.getErrorHandler().genericError(entry.getNode(), "Illegal escape sequence");
                        context.trace.report(ILLEGAL_ESCAPE_SEQUENCE.on(entry));
                        value[0] = CompileTimeConstantResolver.OUT_OF_RANGE;
                    }
                    else {
                        builder.append(character);
                    }
                }
            });
        }
        if (value[0] != CompileTimeConstantResolver.OUT_OF_RANGE) {
            context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new StringValue(builder.toString()));
        }
        return context.getServices().checkType(context.semanticServices.getStandardLibrary().getStringType(), expression, contextWithExpectedType);
    }

    @Override
    public JetType visitJetElement(JetElement element, ExpressionTypingContext context) {
//            context.trace.getErrorHandler().genericError(element.getNode(), "[OperatorConventions] Unsupported element: " + element + " " + element.getClass().getCanonicalName());
        context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
        return null;
    }
}
