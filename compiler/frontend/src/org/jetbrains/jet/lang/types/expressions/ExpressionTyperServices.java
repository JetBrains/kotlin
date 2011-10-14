package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptorUtil;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.AutoCastUtils;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_MISMATCH;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
* @author abreslav
*/
public class ExpressionTyperServices {
    private final JetSemanticServices semanticServices;
    private final BindingTrace trace;

    private final ExpressionTyperVisitor expressionTyperVisitor;
    private final CallResolver callResolver;

    public ExpressionTyperServices(JetSemanticServices semanticServices, BindingTrace trace) {
        this.semanticServices = semanticServices;
        this.trace = trace;
        this.expressionTyperVisitor = new ExpressionTyperVisitor();
        this.callResolver = new CallResolver(semanticServices, DataFlowInfo.getEmpty());
    }

    public ExpressionTyperVisitorWithWritableScope newTypeInferrerVisitorWithWritableScope(WritableScope scope) {
        return new ExpressionTyperVisitorWithWritableScope(scope);
    }

    @NotNull
    public JetType safeGetType(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType) {
        JetType type = getType(scope, expression, expectedType);
        if (type != null) {
            return type;
        }
        return ErrorUtils.createErrorType("Type for " + expression.getText());
    }

    @Nullable
    public JetType getType(@NotNull final JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType) {
        return expressionTyperVisitor.getType(expression, ExpressionTypingContext.newRootContext(semanticServices, trace, scope, DataFlowInfo.getEmpty(), expectedType, TypeUtils.FORBIDDEN));
    }

    public JetType getTypeWithNamespaces(@NotNull final JetScope scope, @NotNull JetExpression expression) {
        return ExpressionTyperVisitorWithNamespaces.INSTANCE.getType(expression, ExpressionTypingContext.newRootContext(semanticServices, trace, scope, DataFlowInfo.getEmpty(), TypeUtils.NO_EXPECTED_TYPE, TypeUtils.NO_EXPECTED_TYPE));
    }

    public CallResolver getCallResolver() {
        return callResolver;
    }

//        TODO JetElement -> JetWhenConditionCall || JetQualifiedExpression
//        private void checkNullSafety(@Nullable JetType receiverType, @NotNull ASTNode operationTokenNode, @Nullable FunctionDescriptor callee, @NotNull JetElement element) {
//            if (receiverType != null && callee != null) {
//                boolean namespaceType = receiverType instanceof NamespaceType;
//                boolean nullableReceiver = !namespaceType && receiverType.isNullable();
//                ReceiverDescriptor calleeReceiver = callee.getReceiverParameter();
//                boolean calleeForbidsNullableReceiver = !calleeReceiver.exists() || !calleeReceiver.getType().isNullable();
//
//                IElementType operationSign = operationTokenNode.getElementType();
//                if (nullableReceiver && calleeForbidsNullableReceiver && operationSign == JetTokens.DOT) {
////                    trace.getErrorHandler().genericError(operationTokenNode, "Only safe calls (?.) are allowed on a nullable receiver of type " + receiverType);
//                    trace.report(UNSAFE_CALL.on(operationTokenNode, receiverType));
//                }
//                else if ((!nullableReceiver || !calleeForbidsNullableReceiver) && operationSign == JetTokens.SAFE_ACCESS) {
//                    if (namespaceType) {
////                        trace.getErrorHandler().genericError(operationTokenNode, "Safe calls are not allowed on namespaces");
//                        trace.report(SAFE_CALLS_ARE_NOT_ALLOWED_ON_NAMESPACES.on(operationTokenNode));
//                    }
//                    else {
////                        trace.getErrorHandler().genericWarning(operationTokenNode, "Unnecessary safe call on a non-null receiver of type  " + receiverType);
//
//                        trace.report(UNNECESSARY_SAFE_CALL.on(element, operationTokenNode, receiverType));
//
//                    }
//                }
//            }
//        }

    public void checkFunctionReturnType(@NotNull JetScope outerScope, @NotNull JetDeclarationWithBody function, @NotNull FunctionDescriptor functionDescriptor) {
        checkFunctionReturnType(outerScope, function, functionDescriptor, DataFlowInfo.getEmpty());
    }

    /*package*/ void checkFunctionReturnType(@NotNull JetScope outerScope, @NotNull JetDeclarationWithBody function, @NotNull FunctionDescriptor functionDescriptor, DataFlowInfo dataFlowInfo) {
        JetType expectedReturnType = functionDescriptor.getReturnType();
        if (!function.hasBlockBody() && !function.hasDeclaredReturnType()) {
            expectedReturnType = TypeUtils.NO_EXPECTED_TYPE;
        }
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace);
        checkFunctionReturnType(functionInnerScope, function, functionDescriptor, expectedReturnType, dataFlowInfo);
//        Map<JetElement, JetType> typeMap = collectReturnedExpressionsWithTypes(outerScope, function, functionDescriptor, expectedReturnType);
//        if (typeMap.isEmpty()) {
//            return; // The function returns Nothing
//        }
//        for (Map.Entry<JetElement, JetType> entry : typeMap.entrySet()) {
//            JetType actualType = entry.castValue();
//            JetElement element = entry.getKey();
//            JetTypeChecker typeChecker = semanticServices.getTypeChecker();
//            if (!typeChecker.isSubtypeOf(actualType, expectedReturnType)) {
//                if (typeChecker.isConvertibleBySpecialConversion(actualType, expectedReturnType)) {
//                    if (expectedReturnType.getConstructor().equals(JetStandardClasses.getUnitType().getConstructor())
//                        && element.getParent() instanceof JetReturnExpression) {
//                        context.trace.getErrorHandler().genericError(element.getNode(), "This function must return a value of type Unit");
//                    }
//                }
//                else {
//                    if (element == function) {
//                        JetExpression bodyExpression = function.getBodyExpression();
//                        assert bodyExpression != null;
//                        context.trace.getErrorHandler().genericError(bodyExpression.getNode(), "This function must return a value of type " + expectedReturnType);
//                    }
//                    else if (element instanceof JetExpression) {
//                        JetExpression expression = (JetExpression) element;
//                        context.trace.report(TYPE_MISMATCH.on(expression, expectedReturnType, actualType));
//                    }
//                    else {
//                        context.trace.getErrorHandler().genericError(element.getNode(), "This function must return a value of type " + expectedReturnType);
//                    }
//                }
//            }
//        }
    }

    public void checkFunctionReturnType(JetScope functionInnerScope, JetDeclarationWithBody function, FunctionDescriptor functionDescriptor, @NotNull final JetType expectedReturnType) {
        checkFunctionReturnType(functionInnerScope, function, functionDescriptor, expectedReturnType, DataFlowInfo.getEmpty());
    }

    /*package*/ void checkFunctionReturnType(JetScope functionInnerScope, JetDeclarationWithBody function, FunctionDescriptor functionDescriptor, @NotNull final JetType expectedReturnType, @NotNull DataFlowInfo dataFlowInfo) {
        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;

        final boolean blockBody = function.hasBlockBody();
        final ExpressionTypingContext context =
                blockBody
                ? ExpressionTypingContext.newRootContext(semanticServices, trace, functionInnerScope, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE, expectedReturnType)
                : ExpressionTypingContext.newRootContext(semanticServices, trace, functionInnerScope, dataFlowInfo, expectedReturnType, TypeUtils.FORBIDDEN);

        if (function instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression functionLiteralExpression = (JetFunctionLiteralExpression) function;
            getBlockReturnedType(functionInnerScope, functionLiteralExpression.getBodyExpression(), CoercionStrategy.COERCION_TO_UNIT, context);
        }
        else {
            expressionTyperVisitor.getType(bodyExpression, context);
        }

//            List<JetElement> unreachableElements = Lists.newArrayList();
//            flowInformationProvider.collectUnreachableExpressions(function.asElement(), unreachableElements);

        // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
//            final Set<JetElement> rootUnreachableElements = JetPsiUtil.findRootExpressions(unreachableElements);

        // TODO : (return 1) || (return 2) -- only || and right of it is unreachable
        // TODO : try {return 1} finally {return 2}. Currently 'return 1' is reported as unreachable,
        //        though it'd better be reported more specifically

//            for (JetElement element : rootUnreachableElements) {
//                //trace.report(UNREACHABLE_CODE.on(element));
//            }

//            List<JetExpression> returnedExpressions = Lists.newArrayList();
//            flowInformationProvider.collectReturnExpressions(function.asElement(), returnedExpressions);

//            boolean nothingReturned = returnedExpressions.isEmpty();

        //returnedExpressions.remove(function); // This will be the only "expression" if the body is empty
//            Map<JetExpression, JetType> typeMap = collectReturnedExpressionsWithTypes(trace, functionInnerScope, function, functionDescriptor);
//            Set<JetExpression> returnedExpressions = typeMap.keySet();
//
//
//            if (expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && returnedExpressions.isEmpty()) {
//                trace.report(RETURN_TYPE_MISMATCH.on(bodyExpression, expectedReturnType));
//            }

//            if (expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && returnedExpressions.isEmpty() && !nothingReturned) {
////                trace.getErrorHandler().genericError(bodyExpression.getNode(), "This function must return a value of type " + expectedReturnType);
//                trace.report(RETURN_TYPE_MISMATCH.on(bodyExpression, expectedReturnType));
//            }
//
//            for (JetExpression returnedExpression : returnedExpressions) {
//                returnedExpression.accept(new JetVisitorVoid() {
//                    @Override
//                    public void visitReturnExpression(JetReturnExpression expression) {
//                        if (!blockBody) {
////                            trace.getErrorHandler().genericError(expression.getNode(), "Returns are not allowed for functions with expression body. Use block body in '{...}'");
//                            trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression));
//                        }
//                    }
//
//                    @Override
//                    public void visitExpression(JetExpression expression) {
//                        if (blockBody && !JetStandardClasses.isUnit(expectedReturnType) && !rootUnreachableElements.contains(expression)) {
//                            //TODO move to pseudocode
//                            JetType type = expressionTyperVisitor.getType(expression, context.replaceExpectedType(NO_EXPECTED_TYPE));
//                            if (type == null || !JetStandardClasses.isNothing(type)) {
////                                trace.getErrorHandler().genericError(expression.getNode(), "A 'return' expression required in a function with a block body ('{...}')");
//                                trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(expression));
//                            }
//                        }
//                    }
//                });
//            }
    }

    @Nullable
    /*package*/ JetType getBlockReturnedType(@NotNull JetScope outerScope, @NotNull JetBlockExpression expression, @NotNull CoercionStrategy coercionStrategyForLastExpression, ExpressionTypingContext context) {
        List<JetElement> block = expression.getStatements();
        if (block.isEmpty()) {
            return checkType(JetStandardClasses.getUnitType(), expression, context);
        }

        DeclarationDescriptor containingDescriptor = outerScope.getContainingDeclaration();
        WritableScope scope = new WritableScopeImpl(outerScope, containingDescriptor, new TraceBasedRedeclarationHandler(context.trace)).setDebugName("getBlockReturnedType");
        return getBlockReturnedTypeWithWritableScope(scope, block, coercionStrategyForLastExpression, context);
    }

    @NotNull
    public JetType inferFunctionReturnType(@NotNull JetScope outerScope, JetDeclarationWithBody function, FunctionDescriptor functionDescriptor) {
        Map<JetExpression, JetType> typeMap = collectReturnedExpressionsWithTypes(trace, outerScope, function, functionDescriptor);
        Collection<JetType> types = typeMap.values();
        return types.isEmpty()
                ? JetStandardClasses.getNothingType()
                : semanticServices.getTypeChecker().commonSupertype(types);
    }

    private Map<JetExpression, JetType> collectReturnedExpressionsWithTypes(
            final @NotNull BindingTrace trace,
            JetScope outerScope,
            final JetDeclarationWithBody function,
            FunctionDescriptor functionDescriptor) {
        JetExpression bodyExpression = function.getBodyExpression();
        assert bodyExpression != null;
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace);
        expressionTyperVisitor.getType(bodyExpression, ExpressionTypingContext.newRootContext(semanticServices, trace, functionInnerScope, DataFlowInfo.getEmpty(), TypeUtils.NO_EXPECTED_TYPE, TypeUtils.FORBIDDEN));
        //todo function literals
        final Collection<JetExpression> returnedExpressions = new ArrayList<JetExpression>();
        if (function.hasBlockBody()) {
            //now this code is never invoked!, it should be invoked for inference of return type of function literal with local returns
            bodyExpression.visit(new JetTreeVisitor<JetDeclarationWithBody>() {
                @Override
                public Void visitReturnExpression(JetReturnExpression expression, JetDeclarationWithBody outerFunction) {
                    JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                    PsiElement element = targetLabel != null ? trace.get(LABEL_TARGET, targetLabel) : null;
                    if (element == function || (targetLabel == null && outerFunction == function)) {
                        returnedExpressions.add(expression);
                    }
                    return null;
                }

                @Override
                public Void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, JetDeclarationWithBody outerFunction) {
                    return super.visitFunctionLiteralExpression(expression, expression.getFunctionLiteral());
                }

                @Override
                public Void visitNamedFunction(JetNamedFunction function, JetDeclarationWithBody outerFunction) {
                    return super.visitNamedFunction(function, function);
                }
            }, function);
        }
        else {
            returnedExpressions.add(bodyExpression);
        }
        Map<JetExpression, JetType> typeMap = new HashMap<JetExpression, JetType>();
        for (JetExpression returnedExpression : returnedExpressions) {
            JetType cachedType = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, returnedExpression);
            trace.record(STATEMENT, returnedExpression, false);
            if (cachedType != null) {
                typeMap.put(returnedExpression, cachedType);
            }
        }
        return typeMap;
    }

    /*package*/ JetType getBlockReturnedTypeWithWritableScope(@NotNull WritableScope scope, @NotNull List<? extends JetElement> block, @NotNull CoercionStrategy coercionStrategyForLastExpression, ExpressionTypingContext context) {
        if (block.isEmpty()) {
            return JetStandardClasses.getUnitType();
        }

        ExpressionTyperVisitorWithWritableScope blockLevelVisitor = newTypeInferrerVisitorWithWritableScope(scope);
        ExpressionTypingContext newContext = ExpressionTypingContext.newRootContext(semanticServices, trace, scope, context.dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE, context.expectedReturnType);

        JetType result = null;
        for (Iterator<? extends JetElement> iterator = block.iterator(); iterator.hasNext(); ) {
            final JetElement statement = iterator.next();
            trace.record(STATEMENT, statement);
            final JetExpression statementExpression = (JetExpression) statement;
            //TODO constructor assert context.expectedType != FORBIDDEN : ""
            if (!iterator.hasNext()) {
                if (context.expectedType != TypeUtils.NO_EXPECTED_TYPE) {
                    if (coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT && JetStandardClasses.isUnit(context.expectedType)) {
                        // This implements coercion to Unit
                        TemporaryBindingTrace temporaryTraceExpectingUnit = TemporaryBindingTrace.create(trace);
                        final boolean[] mismatch = new boolean[1];
                        ObservableBindingTrace errorInterceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceExpectingUnit, statementExpression, mismatch);
                        newContext = ExpressionTypingContext.newRootContext(semanticServices, errorInterceptingTrace, scope, newContext.dataFlowInfo, context.expectedType, context.expectedReturnType);
                        result = blockLevelVisitor.getType(statementExpression, newContext);
                        if (mismatch[0]) {
                            TemporaryBindingTrace temporaryTraceNoExpectedType = TemporaryBindingTrace.create(trace);
                            mismatch[0] = false;
                            ObservableBindingTrace interceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceNoExpectedType, statementExpression, mismatch);
                            newContext = ExpressionTypingContext.newRootContext(semanticServices, interceptingTrace, scope, newContext.dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE, context.expectedReturnType);
                            result = blockLevelVisitor.getType(statementExpression, newContext);
                            if (mismatch[0]) {
                                temporaryTraceExpectingUnit.commit();
                            }
                            else {
                                temporaryTraceNoExpectedType.commit();
                            }
                        }
                        else {
                            temporaryTraceExpectingUnit.commit();
                        }
                    }
                    else {
                        newContext = ExpressionTypingContext.newRootContext(semanticServices, trace, scope, newContext.dataFlowInfo, context.expectedType, context.expectedReturnType);
                        result = blockLevelVisitor.getType(statementExpression, newContext);
                    }
                }
                else {
                    result = blockLevelVisitor.getType(statementExpression, newContext);
                    if (coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT) {
                        boolean mightBeUnit = false;
                        if (statementExpression instanceof JetDeclaration) {
                            mightBeUnit = true;
                        }
                        if (statementExpression instanceof JetBinaryExpression) {
                            JetBinaryExpression binaryExpression = (JetBinaryExpression) statementExpression;
                            IElementType operationType = binaryExpression.getOperationToken();
                            if (operationType == JetTokens.EQ || OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                                mightBeUnit = true;
                            }
                        }
                        if (mightBeUnit) {
                            // ExpressionTyperVisitorWithWritableScope should return only null or Unit for declarations and assignments
                            assert result == null || JetStandardClasses.isUnit(result);
                            result = JetStandardClasses.getUnitType();
                        }
                    }
                }
            }
            else {
                result = blockLevelVisitor.getType(statementExpression, newContext);
            }

            DataFlowInfo newDataFlowInfo = blockLevelVisitor.getResultingDataFlowInfo();
            if (newDataFlowInfo == null) {
                newDataFlowInfo = context.dataFlowInfo;
            }
            if (newDataFlowInfo != context.dataFlowInfo) {
                newContext = ExpressionTypingContext.newRootContext(semanticServices, trace, scope, newDataFlowInfo, TypeUtils.NO_EXPECTED_TYPE, context.expectedReturnType);
            }
            blockLevelVisitor.resetResult(); // TODO : maybe it's better to recreate the visitors with the same scope?
        }
        return result;
    }

    private ObservableBindingTrace makeTraceInterceptingTypeMismatch(final BindingTrace trace, final JetExpression expressionToWatch, final boolean[] mismatchFound) {
        return new ObservableBindingTrace(trace) {

            @Override
            public void report(@NotNull Diagnostic diagnostic) {
                if (diagnostic.getFactory() == TYPE_MISMATCH && ((DiagnosticWithPsiElement) diagnostic).getPsiElement() == expressionToWatch) {
                    mismatchFound[0] = true;
                }
                super.report(diagnostic);
            }
        };
    }

//        //TODO
//        private JetType enrichOutType(JetExpression expression, JetType initialType, @NotNull ExpressionTypingContext context) {
//            if (expression == null) return initialType;
//            VariableDescriptor variableDescriptor = getVariableDescriptorFromSimpleName(expression, context);
//            if (variableDescriptor != null) {
//                return context.dataFlowInfo.getOutType(variableDescriptor);
//            }
//            return initialType;
//        }

//        @Nullable
//        private JetType checkType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ExpressionTypingContext context) {
//            if (expressionType != null && context.expectedType != null && context.expectedType != NO_EXPECTED_TYPE) {
//                if (!semanticServices.getTypeChecker().isSubtypeOf(expressionType, context.expectedType)) {
//                    context.trace.report(TYPE_MISMATCH.on(expression, context.expectedType, expressionType));
//                }
//            }
//            return expressionType;
//        }

    @Nullable
    /*package*/ JetType checkType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ExpressionTypingContext context) {
        if (expressionType == null || context.expectedType == null || context.expectedType == TypeUtils.NO_EXPECTED_TYPE ||
            semanticServices.getTypeChecker().isSubtypeOf(expressionType, context.expectedType)) {
            return expressionType;
        }
//            VariableDescriptor variableDescriptor = AutoCastUtils.getVariableDescriptorFromSimpleName(context.trace.getBindingContext(), expression);
//            boolean appropriateTypeFound = false;
//            if (variableDescriptor != null) {
//                List<JetType> possibleTypes = Lists.newArrayList(context.dataFlowInfo.getPossibleTypesForVariable(variableDescriptor));
//                Collections.reverse(possibleTypes);
//                for (JetType possibleType : possibleTypes) {
//                    if (semanticServices.getTypeChecker().isSubtypeOf(possibleType, context.expectedType)) {
//                        appropriateTypeFound = true;
//                        break;
//                    }
//                }
//                if (!appropriateTypeFound) {
//                    JetType notnullType = context.dataFlowInfo.getOutType(variableDescriptor);
//                    if (notnullType != null && semanticServices.getTypeChecker().isSubtypeOf(notnullType, context.expectedType)) {
//                        appropriateTypeFound = true;
//                    }
//                }
//            }
        if (AutoCastUtils.castExpression(expression, context.expectedType, context.dataFlowInfo, context.trace) == null) {
//                context.trace.getErrorHandler().typeMismatch(expression, context.expectedType, expressionType);
            context.trace.report(TYPE_MISMATCH.on(expression, context.expectedType, expressionType));
            return expressionType;
        }
//            checkAutoCast(expression, context.expectedType, variableDescriptor, context.trace);
        return context.expectedType;
    }

//        private void checkAutoCast(JetExpression expression, JetType type, VariableDescriptor variableDescriptor, BindingTrace trace) {
//            if (variableDescriptor.isVar()) {
////                trace.getErrorHandler().genericError(expression.getNode(), "Automatic cast to " + type + " is impossible, because variable " + variableDescriptor.getName() + " is mutable");
//                trace.report(AUTOCAST_IMPOSSIBLE.on(expression, type, variableDescriptor));
//            } else {
//                trace.record(BindingContext.AUTOCAST, expression, type);
//            }
//        }

    @NotNull
    /*package*/ List<JetType> checkArgumentTypes(@NotNull List<JetType> argumentTypes, @NotNull List<JetExpression> arguments, @NotNull List<TypeProjection> expectedArgumentTypes, @NotNull ExpressionTypingContext context) {
        if (arguments.size() == 0 || argumentTypes.size() != arguments.size() || expectedArgumentTypes.size() != arguments.size()) {
            return argumentTypes;
        }
        List<JetType> result = Lists.newArrayListWithCapacity(arguments.size());
        for (int i = 0, argumentTypesSize = argumentTypes.size(); i < argumentTypesSize; i++) {
            result.add(checkType(argumentTypes.get(i), arguments.get(i), context.replaceExpectedType(expectedArgumentTypes.get(i).getType())));
        }
        return result;
    }

    @Nullable
    /*package*/ VariableDescriptor getVariableDescriptorFromSimpleName(@NotNull JetExpression receiverExpression, @NotNull ExpressionTypingContext context) {
        if (receiverExpression instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS expression = (JetBinaryExpressionWithTypeRHS) receiverExpression;
            if (expression.getOperationSign().getReferencedNameElementType() == JetTokens.COLON) {
                return getVariableDescriptorFromSimpleName(expression.getLeft(), context);
            }
        }
        VariableDescriptor variableDescriptor = null;
        if (receiverExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) receiverExpression;
            DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(REFERENCE_TARGET, nameExpression);
            if (declarationDescriptor instanceof VariableDescriptor) {
                variableDescriptor = (VariableDescriptor) declarationDescriptor;
            }
        }
        return variableDescriptor;
    }
}
