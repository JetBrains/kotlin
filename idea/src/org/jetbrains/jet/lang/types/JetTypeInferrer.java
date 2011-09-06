package org.jetbrains.jet.lang.types;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.CompositeErrorHandler;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResult;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author abreslav
 */
public class JetTypeInferrer {
    
    private static final Set<String> numberConversions = new HashSet();

    static {
        numberConversions.add("dbl");
        numberConversions.add("flt");
        numberConversions.add("lng");
        numberConversions.add("sht");
        numberConversions.add("byt");
        numberConversions.add("int");
    }

    private static final JetType FORBIDDEN = new JetType() {
        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public boolean isNullable() {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public String toString() {
            return "FORBIDDEN";
        }
    };
    public static final JetType NO_EXPECTED_TYPE = new JetType() {
        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public boolean isNullable() {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public String toString() {
            return "NO_EXPECTED_TYPE";
        }
    };

    private static final ImmutableMap<IElementType, String> unaryOperationNames = ImmutableMap.<IElementType, String>builder()
            .put(JetTokens.PLUSPLUS, "inc")
            .put(JetTokens.MINUSMINUS, "dec")
            .put(JetTokens.PLUS, "plus")
            .put(JetTokens.MINUS, "minus")
            .put(JetTokens.EXCL, "not")
            .build();
    private static final ImmutableMap<IElementType, String> binaryOperationNames = ImmutableMap.<IElementType, String>builder()
            .put(JetTokens.MUL, "times")
            .put(JetTokens.PLUS, "plus")
            .put(JetTokens.MINUS, "minus")
            .put(JetTokens.DIV, "div")
            .put(JetTokens.PERC, "mod")
            .put(JetTokens.ARROW, "arrow")
            .put(JetTokens.RANGE, "rangeTo")
            .build();

    private static final Set<IElementType> comparisonOperations = Sets.<IElementType>newHashSet(JetTokens.LT, JetTokens.GT, JetTokens.LTEQ, JetTokens.GTEQ);
    private static final Set<IElementType> equalsOperations = Sets.<IElementType>newHashSet(JetTokens.EQEQ, JetTokens.EXCLEQ);

    private static final Set<IElementType> inOperations = Sets.<IElementType>newHashSet(JetTokens.IN_KEYWORD, JetTokens.NOT_IN);
    public static final ImmutableMap<IElementType, String> assignmentOperationNames = ImmutableMap.<IElementType, String>builder()
            .put(JetTokens.MULTEQ, "timesAssign")
            .put(JetTokens.DIVEQ, "divAssign")
            .put(JetTokens.PERCEQ, "modAssign")
            .put(JetTokens.PLUSEQ, "plusAssign")
            .put(JetTokens.MINUSEQ, "minusAssign")
            .build();

    private static final ImmutableMap<IElementType, IElementType> assignmentOperationCounterparts = ImmutableMap.<IElementType, IElementType>builder()
            .put(JetTokens.MULTEQ, JetTokens.MUL)
            .put(JetTokens.DIVEQ, JetTokens.DIV)
            .put(JetTokens.PERCEQ, JetTokens.PERC)
            .put(JetTokens.PLUSEQ, JetTokens.PLUS)
            .put(JetTokens.MINUSEQ, JetTokens.MINUS)
            .build();

    private final JetSemanticServices semanticServices;
    private final JetFlowInformationProvider flowInformationProvider;
    private final Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo = Maps.newHashMap();
    private final Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists = Maps.newHashMap();
    
    public JetTypeInferrer(@NotNull JetFlowInformationProvider flowInformationProvider, @NotNull JetSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
        this.flowInformationProvider = flowInformationProvider;
    }

    public Services getServices(@NotNull BindingTrace trace) {
        return new Services(trace);
    }

    public class Services {
        private final BindingTrace trace;
        private final CompileTimeConstantResolver compileTimeConstantResolver;
        private final CallResolver callResolver;

        private final TypeInferrerVisitor typeInferrerVisitor;
        private final TypeInferrerVisitorWithNamespaces typeInferrerVisitorWithNamespaces;

        private Services(BindingTrace trace) {
            this.trace = trace;
            this.compileTimeConstantResolver = new CompileTimeConstantResolver(semanticServices, trace);
            this.typeInferrerVisitor = new TypeInferrerVisitor();
            this.typeInferrerVisitorWithNamespaces = new TypeInferrerVisitorWithNamespaces();
            this.callResolver = new CallResolver(semanticServices, trace, JetTypeInferrer.this);
        }

        public TypeInferrerVisitorWithWritableScope newTypeInferrerVisitorWithWritableScope(WritableScope scope) {
            return new TypeInferrerVisitorWithWritableScope(scope);
        }

        @NotNull
        public JetType safeGetType(@NotNull final JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType) {
            JetType type = getType(scope, expression, expectedType);
            if (type != null) {
                return type;
            }
            return ErrorUtils.createErrorType("Type for " + expression.getText());
        }

        @Nullable
        public JetType getType(@NotNull final JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType) {
            return typeInferrerVisitor.getType(expression, newContext(trace, scope, DataFlowInfo.getEmpty(), expectedType, FORBIDDEN));
        }

        public JetType getTypeWithNamespaces(@NotNull final JetScope scope, @NotNull JetExpression expression) {
            return typeInferrerVisitorWithNamespaces.getType(expression, newContext(trace, scope, DataFlowInfo.getEmpty(), NO_EXPECTED_TYPE, NO_EXPECTED_TYPE));
        }

        public CallResolver getCallResolver() {
            return callResolver;
        }

        private void checkNullSafety(@Nullable JetType receiverType, @NotNull ASTNode operationTokenNode, @Nullable FunctionDescriptor callee) {
            if (receiverType != null && callee != null) {
                boolean namespaceType = receiverType instanceof NamespaceType;
                JetType calleeReceiverType = callee.getReceiverType();
                boolean nullableReceiver = !namespaceType && receiverType.isNullable();
                boolean calleeForbidsNullableReceiver = calleeReceiverType == null || !calleeReceiverType.isNullable();

                IElementType operationSign = operationTokenNode.getElementType();
                if (nullableReceiver && calleeForbidsNullableReceiver && operationSign == JetTokens.DOT) {
                    trace.getErrorHandler().genericError(operationTokenNode, "Only safe calls (?.) are allowed on a nullable receiver of type " + receiverType);
                }
                else if ((!nullableReceiver || !calleeForbidsNullableReceiver) && operationSign == JetTokens.SAFE_ACCESS) {
                    if (namespaceType) {
                        trace.getErrorHandler().genericError(operationTokenNode, "Safe calls are not allowed on namespaces");
                    }
                    else {
                        trace.getErrorHandler().genericWarning(operationTokenNode, "Unnecessary safe call on a non-null receiver of type  " + receiverType);
                    }
                }
            }
        }

        public void checkFunctionReturnType(@NotNull JetScope outerScope, @NotNull JetDeclarationWithBody function, @NotNull FunctionDescriptor functionDescriptor) {
            checkFunctionReturnType(outerScope, function, functionDescriptor, DataFlowInfo.getEmpty());
        }

        private void checkFunctionReturnType(@NotNull JetScope outerScope, @NotNull JetDeclarationWithBody function, @NotNull FunctionDescriptor functionDescriptor, DataFlowInfo dataFlowInfo) {
            JetType expectedReturnType = functionDescriptor.getReturnType();
            if (!function.hasBlockBody() && !function.hasDeclaredReturnType()) {
                expectedReturnType = NO_EXPECTED_TYPE;
            }
            JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace);
            checkFunctionReturnType(functionInnerScope, function, expectedReturnType, dataFlowInfo);
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
    //                        context.trace.getErrorHandler().typeMismatch(expression, expectedReturnType, actualType);
    //                    }
    //                    else {
    //                        context.trace.getErrorHandler().genericError(element.getNode(), "This function must return a value of type " + expectedReturnType);
    //                    }
    //                }
    //            }
    //        }
        }

        public void checkFunctionReturnType(JetScope functionInnerScope, JetDeclarationWithBody function, @NotNull final JetType expectedReturnType) {
            checkFunctionReturnType(functionInnerScope, function, expectedReturnType, DataFlowInfo.getEmpty());
        }

        private void checkFunctionReturnType(JetScope functionInnerScope, JetDeclarationWithBody function, @NotNull final JetType expectedReturnType, @NotNull DataFlowInfo dataFlowInfo) {
            JetExpression bodyExpression = function.getBodyExpression();
            assert bodyExpression != null;
            
            final boolean blockBody = function.hasBlockBody();
            final TypeInferenceContext context =
                    blockBody
                    ? newContext(trace, functionInnerScope, dataFlowInfo, NO_EXPECTED_TYPE, expectedReturnType)
                    : newContext(trace, functionInnerScope, dataFlowInfo, expectedReturnType, FORBIDDEN);

            if (function instanceof JetFunctionLiteralExpression) {
                JetFunctionLiteralExpression functionLiteralExpression = (JetFunctionLiteralExpression) function;
                getBlockReturnedType(functionInnerScope, functionLiteralExpression.getBodyExpression(), CoercionStrategy.COERCION_TO_UNIT, context);
            }
            else {
                typeInferrerVisitor.getType(bodyExpression, context);
            }

            List<JetElement> unreachableElements = Lists.newArrayList();
            flowInformationProvider.collectUnreachableExpressions(function.asElement(), unreachableElements);

            // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
            final Set<JetElement> rootUnreachableElements = JetPsiUtil.findRootExpressions(unreachableElements);

            // TODO : (return 1) || (return 2) -- only || and right of it is unreachable
            // TODO : try {return 1} finally {return 2}. Currently 'return 1' is reported as unreachable,
            //        though it'd better be reported more specifically

            for (JetElement element : rootUnreachableElements) {
                trace.getErrorHandler().genericError(element.getNode(), "Unreachable code");
            }

            List<JetExpression> returnedExpressions = Lists.newArrayList();
            flowInformationProvider.collectReturnExpressions(function.asElement(), returnedExpressions);

            boolean nothingReturned = returnedExpressions.isEmpty();

            returnedExpressions.remove(function); // This will be the only "expression" if the body is empty

            if (expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && returnedExpressions.isEmpty() && !nothingReturned) {
                trace.getErrorHandler().genericError(bodyExpression.getNode(), "This function must return a value of type " + expectedReturnType);
            }

            for (JetExpression returnedExpression : returnedExpressions) {
                returnedExpression.accept(new JetVisitorVoid() {
                    @Override
                    public void visitReturnExpression(JetReturnExpression expression) {
                        if (!blockBody) {
                            trace.getErrorHandler().genericError(expression.getNode(), "Returns are not allowed for functions with expression body. Use block body in '{...}'");
                        }
                    }

                    @Override
                    public void visitExpression(JetExpression expression) {
                        if (blockBody && !JetStandardClasses.isUnit(expectedReturnType) && !rootUnreachableElements.contains(expression)) {
                            //TODO move to pseudocode
                            JetType type = typeInferrerVisitor.getType(expression, context.replaceExpectedType(NO_EXPECTED_TYPE));
                            if (type == null || !JetStandardClasses.isNothing(type)) {
                                trace.getErrorHandler().genericError(expression.getNode(), "A 'return' expression required in a function with a block body ('{...}')");
                            }
                        }
                    }
                });
            }
        }

        @Nullable
        private JetType getBlockReturnedType(@NotNull JetScope outerScope, @NotNull JetBlockExpression expression, @NotNull CoercionStrategy coercionStrategyForLastExpression, TypeInferenceContext context) {
            List<JetElement> block = expression.getStatements();
            if (block.isEmpty()) {
                return checkType(JetStandardClasses.getUnitType(), expression, context);
            }

            DeclarationDescriptor containingDescriptor = outerScope.getContainingDeclaration();
            WritableScope scope = new WritableScopeImpl(outerScope, containingDescriptor, context.trace.getErrorHandler()).setDebugName("getBlockReturnedType");
            return getBlockReturnedTypeWithWritableScope(scope, block, coercionStrategyForLastExpression, context);
        }

        @NotNull
        public JetType inferFunctionReturnType(@NotNull JetScope outerScope, JetDeclarationWithBody function, FunctionDescriptor functionDescriptor) {
            Map<JetElement, JetType> typeMap = collectReturnedExpressionsWithTypes(trace, outerScope, function, functionDescriptor);
            Collection<JetType> types = typeMap.values();
            return types.isEmpty()
                    ? JetStandardClasses.getNothingType()
                    : semanticServices.getTypeChecker().commonSupertype(types);
        }

        private Map<JetElement, JetType> collectReturnedExpressionsWithTypes(
                @NotNull BindingTrace trace,
                JetScope outerScope,
                JetDeclarationWithBody function,
                FunctionDescriptor functionDescriptor) {
            JetExpression bodyExpression = function.getBodyExpression();
            assert bodyExpression != null;
            JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace);
            typeInferrerVisitor.getType(bodyExpression, newContext(trace, functionInnerScope, DataFlowInfo.getEmpty(), NO_EXPECTED_TYPE, FORBIDDEN));
            Collection<JetExpression> returnedExpressions = new ArrayList<JetExpression>();
            Collection<JetElement> elementsReturningUnit = new ArrayList<JetElement>();
            flowInformationProvider.collectReturnedInformation(function.asElement(), returnedExpressions, elementsReturningUnit);
            Map<JetElement,JetType> typeMap = new HashMap<JetElement, JetType>();
            for (JetExpression returnedExpression : returnedExpressions) {
                JetType cachedType = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, returnedExpression);
                trace.record(STATEMENT, returnedExpression, false);
                if (cachedType != null) {
                    typeMap.put(returnedExpression, cachedType);
                }
            }
            for (JetElement jetElement : elementsReturningUnit) {
                typeMap.put(jetElement, JetStandardClasses.getUnitType());
            }
            return typeMap;
        }

        private JetType getBlockReturnedTypeWithWritableScope(@NotNull WritableScope scope, @NotNull List<? extends JetElement> block, @NotNull CoercionStrategy coercionStrategyForLastExpression, TypeInferenceContext context) {
            if (block.isEmpty()) {
                return JetStandardClasses.getUnitType();
            }

            TypeInferrerVisitorWithWritableScope blockLevelVisitor = newTypeInferrerVisitorWithWritableScope(scope);
            TypeInferenceContext newContext = newContext(trace, scope, context.dataFlowInfo, NO_EXPECTED_TYPE, context.expectedReturnType);

            JetType result = null;
            for (Iterator<? extends JetElement> iterator = block.iterator(); iterator.hasNext(); ) {
                final JetElement statement = iterator.next();
                trace.record(STATEMENT, statement);
                final JetExpression statementExpression = (JetExpression) statement;
                //TODO constructor assert context.expectedType != FORBIDDEN : ""
                if (!iterator.hasNext() && context.expectedType != NO_EXPECTED_TYPE) {
                    if (coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT && JetStandardClasses.isUnit(context.expectedType)) {
                        // This implements coercion to Unit
                        TemporaryBindingTrace temporaryTraceExpectingUnit = TemporaryBindingTrace.create(trace);
                        final boolean[] mismatch = new boolean[1];
                        BindingTraceAdapter errorInterceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceExpectingUnit, statementExpression, mismatch);
                        newContext = newContext(errorInterceptingTrace, scope, newContext.dataFlowInfo, context.expectedType, context.expectedReturnType);
                        result = blockLevelVisitor.getType(statementExpression, newContext);
                        if (mismatch[0]) {
                            TemporaryBindingTrace temporaryTraceNoExpectedType = TemporaryBindingTrace.create(trace);
                            mismatch[0] = false;
                            BindingTraceAdapter interceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceNoExpectedType, statementExpression, mismatch);
                            newContext = newContext(interceptingTrace, scope, newContext.dataFlowInfo, NO_EXPECTED_TYPE, context.expectedReturnType);
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
                        newContext = newContext(trace, scope, newContext.dataFlowInfo, context.expectedType, context.expectedReturnType);
                        result = blockLevelVisitor.getType(statementExpression, newContext);
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
                    newContext = newContext(trace, scope, newDataFlowInfo, NO_EXPECTED_TYPE, context.expectedReturnType);
                }
                blockLevelVisitor.resetResult(); // TODO : maybe it's better to recreate the visitors with the same scope?
            }
            return result;
        }

        private BindingTraceAdapter makeTraceInterceptingTypeMismatch(final BindingTrace trace, final JetExpression expressionToWatch, final boolean[] mismatchFound) {
            return new BindingTraceAdapter(trace) {
                                    @NotNull
                                    @Override
                                    public ErrorHandler getErrorHandler() {
                                        return new CompositeErrorHandler(super.getErrorHandler(), new ErrorHandler() {
                                            @Override
                                            public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
                                                if (expression == expressionToWatch) {
                                                    mismatchFound[0] = true;
                                                }
                                            }
                                        });
                                    }
                                };
        }

        //TODO
        private JetType enrichOutType(JetExpression expression, JetType initialType, @NotNull TypeInferenceContext context) {
            if (expression == null) return initialType;
            VariableDescriptor variableDescriptor = getVariableDescriptorFromSimpleName(expression, context);
            if (variableDescriptor != null) {
                return context.dataFlowInfo.getOutType(variableDescriptor);
            }
            return initialType;
        }

        @Nullable
        private JetType checkType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull TypeInferenceContext context) {
            if (expressionType != null && context.expectedType != null && context.expectedType != NO_EXPECTED_TYPE) {
                if (!semanticServices.getTypeChecker().isSubtypeOf(expressionType, context.expectedType)) {
                    context.trace.getErrorHandler().typeMismatch(expression, context.expectedType, expressionType);
                }
            }
            return expressionType;
        }

        @Nullable
        private JetType checkEnrichedType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull TypeInferenceContext context) {
            if (expressionType == null || context.expectedType == null || context.expectedType == NO_EXPECTED_TYPE ||
                semanticServices.getTypeChecker().isSubtypeOf(expressionType, context.expectedType)) {
                return expressionType;
            }
            VariableDescriptor variableDescriptor = getVariableDescriptorFromSimpleName(expression, context);
            boolean appropriateTypeFound = false;
            if (variableDescriptor != null) {
                List<JetType> possibleTypes = Lists.newArrayList(context.dataFlowInfo.getPossibleTypes(variableDescriptor));
                Collections.reverse(possibleTypes);
                for (JetType possibleType : possibleTypes) {
                    if (semanticServices.getTypeChecker().isSubtypeOf(possibleType, context.expectedType)) {
                        appropriateTypeFound = true;
                        break;
                    }
                }
                if (!appropriateTypeFound) {
                    JetType notnullType = context.dataFlowInfo.getOutType(variableDescriptor);
                    if (notnullType != null && semanticServices.getTypeChecker().isSubtypeOf(notnullType, context.expectedType)) {
                        appropriateTypeFound = true;
                    }
                }
            }
            if (!appropriateTypeFound) {
                context.trace.getErrorHandler().typeMismatch(expression, context.expectedType, expressionType);
                return expressionType;
            }
            checkAutoCast(expression, context.expectedType, variableDescriptor, context.trace);
            return context.expectedType;
        }

        private void checkAutoCast(JetExpression expression, JetType type, VariableDescriptor variableDescriptor, BindingTrace trace) {
            if (variableDescriptor.isVar()) {
                trace.getErrorHandler().genericError(expression.getNode(), "Automatic cast to " + type + " is impossible, because variable " + variableDescriptor.getName() + " is mutable");
            } else {
                trace.record(BindingContext.AUTOCAST, expression, type);
            }
        }

        @NotNull
        private List<JetType> checkArgumentTypes(@NotNull List<JetType> argumentTypes, @NotNull List<JetExpression> arguments, @NotNull List<TypeProjection> expectedArgumentTypes, @NotNull TypeInferenceContext context) {
            if (arguments.size() == 0 || argumentTypes.size() != arguments.size() || expectedArgumentTypes.size() != arguments.size()) {
                return argumentTypes;
            }
            List<JetType> result = Lists.newArrayListWithCapacity(arguments.size());
            for (int i = 0, argumentTypesSize = argumentTypes.size(); i < argumentTypesSize; i++) {
                result.add(checkEnrichedType(argumentTypes.get(i), arguments.get(i), context.replaceExpectedType(expectedArgumentTypes.get(i).getType())));
            }
            return result;
        }

        @Nullable
        private VariableDescriptor getVariableDescriptorFromSimpleName(@NotNull JetExpression receiverExpression, @NotNull TypeInferenceContext context) {
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private enum CoercionStrategy {
        NO_COERCION,
        COERCION_TO_UNIT
    }

    @NotNull
    private TypeInferenceContext newContext(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull JetType expectedReturnType) {
        return new TypeInferenceContext(trace, scope, dataFlowInfo, expectedType, expectedReturnType);
    }

    private class TypeInferenceContext {

        public final BindingTrace trace;

        public final TypeResolver typeResolver;
        public final ClassDescriptorResolver classDescriptorResolver;
        public final JetScope scope;

        public final Services services;
        public final DataFlowInfo dataFlowInfo;

        public final JetType expectedType;
        public final JetType expectedReturnType;

        @Deprecated // Only factory methods
        private TypeInferenceContext(
                @NotNull BindingTrace trace,
                @NotNull JetScope scope,
                @NotNull DataFlowInfo dataFlowInfo,
                @NotNull JetType expectedType,
                @NotNull JetType expectedReturnType) {
            this.trace = trace;
            this.typeResolver = new TypeResolver(semanticServices, trace, true);
            this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
            this.scope = scope;
            this.services = getServices(trace);
            this.dataFlowInfo = dataFlowInfo;
            this.expectedType = expectedType;
            this.expectedReturnType = expectedReturnType;
        }

        public TypeInferenceContext replaceDataFlowInfo(DataFlowInfo newDataFlowInfo) {
            return newContext(trace, scope, newDataFlowInfo, expectedType, expectedReturnType);
        }
        
        public TypeInferenceContext replaceExpectedType(@Nullable JetType newExpectedType) {
            if (newExpectedType == null) return replaceExpectedType(NO_EXPECTED_TYPE);
            if (expectedType == newExpectedType) return this;
            return newContext(trace, scope, dataFlowInfo, newExpectedType, expectedReturnType);
        }
        
        public TypeInferenceContext replaceExpectedReturnType(@Nullable JetType newExpectedReturnType) {
            if (newExpectedReturnType == null) return replaceExpectedReturnType(NO_EXPECTED_TYPE);
            if (expectedReturnType == newExpectedReturnType) return this;
            return newContext(trace, scope, dataFlowInfo, expectedType, newExpectedReturnType);
        }

        public TypeInferenceContext replaceBindingTrace(@NotNull BindingTrace newTrace) {
            if (newTrace == trace) return this;
            return newContext(newTrace, scope, dataFlowInfo, expectedType, expectedReturnType);
        }

        public TypeInferenceContext replaceExpectedTypeAndTrace(@NotNull JetType newExpectedType, @NotNull BindingTrace newTrace) {
            if (newExpectedType == expectedType && newTrace == trace) return this;
            return newContext(newTrace, scope, dataFlowInfo, newExpectedType, expectedReturnType);
        }

        @NotNull
        public TypeInferenceContext replaceScope(@NotNull JetScope newScope) {
            if (newScope == scope) return this;
            return newContext(trace, newScope, dataFlowInfo, expectedType, expectedReturnType);
        }

        @NotNull
        public TypeInferenceContext replaceExpectedTypes(@NotNull JetType newExpectedType, @NotNull JetType newExpectedReturnType) {
            if (expectedType == newExpectedType && expectedReturnType == newExpectedReturnType) return this;
            return newContext(trace, scope, dataFlowInfo, newExpectedType, newExpectedReturnType);
        }
    }

    private class TypeInferrerVisitor extends JetVisitor<JetType, TypeInferenceContext> {

        protected DataFlowInfo resultDataFlowInfo;

        @Nullable
        public DataFlowInfo getResultingDataFlowInfo() {
            return resultDataFlowInfo;
        }

        @NotNull
        public final JetType safeGetType(@NotNull JetExpression expression, TypeInferenceContext context) {
            JetType type = getType(expression, context);
            if (type != null) {
                return type;
            }
            return ErrorUtils.createErrorType("Type for " + expression.getText());
        }

        @Nullable
        public final JetType getType(@NotNull JetExpression expression, TypeInferenceContext context) {
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
                    if (JetStandardClasses.isNothing(result) && !result.isNullable()) {
                        markDominatedExpressionsAsUnreachable(expression, context);
                    }
                }
            }
            catch (ReenteringLazyValueComputationException e) {
                context.trace.getErrorHandler().genericError(expression.getNode(), "Type checking has run into a recursive problem"); // TODO : message
                result = null;
            }

            if (!context.trace.get(BindingContext.PROCESSED, expression)) {
                context.trace.record(BindingContext.RESOLUTION_SCOPE, expression, context.scope);
            }
            context.trace.record(BindingContext.PROCESSED, expression);
            return result;
        }

        private JetType getTypeWithNewScopeAndDataFlowInfo(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull DataFlowInfo newDataFlowInfo, @NotNull TypeInferenceContext context) {
            return getType(expression, newContext(context.trace, scope, newDataFlowInfo, context.expectedType, context.expectedReturnType));
        }


        public void resetResult() {
//            result = null;
            resultDataFlowInfo = null;
//            resultScope = null;
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private void markDominatedExpressionsAsUnreachable(JetExpression expression, TypeInferenceContext context) {
            List<JetElement> dominated = new ArrayList<JetElement>();
            flowInformationProvider.collectDominatedExpressions(expression, dominated);
            Set<JetElement> rootExpressions = JetPsiUtil.findRootExpressions(dominated);
            for (JetElement rootExpression : rootExpressions) {
                context.trace.getErrorHandler().genericError(rootExpression.getNode(),
                        "This code is unreachable, because '" + expression.getText() + "' never terminates normally");
            }
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public JetType visitSimpleNameExpression(JetSimpleNameExpression expression, TypeInferenceContext context) {
            // TODO : other members
            // TODO : type substitutions???
            String referencedName = expression.getReferencedName();
            if (expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER
                    && referencedName != null) {
                PropertyDescriptor property = context.scope.getPropertyByFieldReference(referencedName);
                if (property == null) {
                    context.trace.getErrorHandler().unresolvedReference(expression);
                }
                else {
                    context.trace.record(REFERENCE_TARGET, expression, property);
                    return context.services.checkEnrichedType(property.getOutType(), expression, context);
                }
            }
            else {
                return getSelectorReturnType(null, expression, context); // TODO : Extensions to this
//                assert JetTokens.IDENTIFIER == expression.getReferencedNameElementType();
//                if (referencedName != null) {
//                    VariableDescriptor variable = context.scope.getVariable(referencedName);
//                    if (variable != null) {
//                        context.trace.record(REFERENCE_TARGET, expression, variable);
//                        JetType result = variable.getOutType();
//                        if (result == null) {
//                            context.trace.getErrorHandler().genericError(expression.getNode(), "This variable is not readable in this context");
//                        }
//                        return context.services.checkEnrichedType(result, expression, context);
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
//                            return context.services.checkEnrichedType(result, expression, context);
//                        }
//                        else {
//                            JetType[] result = new JetType[1];
//                            if (furtherNameLookup(expression, referencedName, result, context)) {
//                                return context.services.checkEnrichedType(result[0], expression, context);
//                            }
//
//                        }
//                    }
//                    context.trace.getErrorHandler().unresolvedReference(expression);
//                }
            }
            return null;
        }

        private JetType lookupNamespaceOrClassObject(JetSimpleNameExpression expression, String referencedName, TypeInferenceContext context) {
            ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
            if (classifier != null) {
                JetType classObjectType = classifier.getClassObjectType();
                JetType result = null;
                if (classObjectType != null && (isNamespacePosition() || classifier.isClassObjectAValue())) {
                    result = classObjectType;
                }
                else {
                    context.trace.getErrorHandler().genericError(expression.getNode(), "Classifier " + classifier.getName() +  " does not have a class object");
                }
                context.trace.record(REFERENCE_TARGET, expression, classifier);
                if (result == null) {
                    return ErrorUtils.createErrorType("No class object in " + expression.getReferencedName());
                }
                return context.services.checkEnrichedType(result, expression, context);
            }
            else {
                JetType[] result = new JetType[1];
                if (furtherNameLookup(expression, referencedName, result, context)) {
                    return context.services.checkEnrichedType(result[0], expression, context);
                }

            }
            return null;
        }

        public boolean isNamespacePosition() {
            return false;
        }
        
        protected boolean furtherNameLookup(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName, @NotNull JetType[] result, TypeInferenceContext context) {
            NamespaceType namespaceType = lookupNamespaceType(expression, referencedName, context);
            if (namespaceType != null) {
                context.trace.getErrorHandler().genericError(expression.getNode(), "Expression expected, but a namespace name found");
                return true;
            }
            return false;
        }

        @Nullable
        protected NamespaceType lookupNamespaceType(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName, TypeInferenceContext context) {
            NamespaceDescriptor namespace = context.scope.getNamespace(referencedName);
            if (namespace == null) {
                return null;
            }
            context.trace.record(REFERENCE_TARGET, expression, namespace);
            return namespace.getNamespaceType();
        }

        @Override
        public JetType visitObjectLiteralExpression(final JetObjectLiteralExpression expression, final TypeInferenceContext context) {
            final JetType[] result = new JetType[1];
            BindingTraceAdapter.RecordHandler<PsiElement, DeclarationDescriptor> handler = new BindingTraceAdapter.RecordHandler<PsiElement, DeclarationDescriptor>() {

                @Override
                public void handleRecord(WritableSlice<PsiElement, DeclarationDescriptor> slice, PsiElement declaration, final DeclarationDescriptor descriptor) {
                    if (declaration == expression.getObjectDeclaration()) {
                        JetType defaultType = new DeferredType(new LazyValue<JetType>() {
                            @Override
                            protected JetType compute() {
                                return ((ClassDescriptor) descriptor).getDefaultType();
                            }
                        });
                        result[0] = defaultType;
                        if (!context.trace.get(BindingContext.PROCESSED, expression)) {
                            context.trace.record(BindingContext.EXPRESSION_TYPE, expression, defaultType);
                            context.trace.record(BindingContext.PROCESSED, expression);
                        }
                    }
                }
            };
            BindingTraceAdapter traceAdapter = new BindingTraceAdapter(context.trace);
            for (WritableSlice slice : BindingContext.DECLARATIONS_TO_DESCRIPTORS) {
                //noinspection unchecked
                traceAdapter.addHandler(slice, handler);
            }
            TopDownAnalyzer topDownAnalyzer = new TopDownAnalyzer(semanticServices, traceAdapter);
            topDownAnalyzer.processObject(context.scope, context.scope.getContainingDeclaration(), expression.getObjectDeclaration());
            return context.services.checkType(result[0], expression, context);
        }

        @Override
        public JetType visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, TypeInferenceContext context) {
            JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();

            JetTypeReference receiverTypeRef = functionLiteral.getReceiverTypeRef();
            final JetType receiverType;
            if (receiverTypeRef != null) {
                receiverType = context.typeResolver.resolveType(context.scope, receiverTypeRef);
            } else {
                receiverType = context.scope.getThisType();
            }

            FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(
                    context.scope.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), "<anonymous>");

            List<JetType> parameterTypes = new ArrayList<JetType>();
            List<ValueParameterDescriptor> valueParameterDescriptors = Lists.newArrayList();
            List<JetParameter> parameters = functionLiteral.getValueParameters();
            JetType expectedType = context.expectedType;
            
            List<ValueParameterDescriptor> valueParameters = null;
            boolean functionTypeExpected = expectedType != NO_EXPECTED_TYPE && JetStandardClasses.isFunctionType(expectedType);
            if (functionTypeExpected) {
                valueParameters = JetStandardClasses.getValueParameters(functionDescriptor, expectedType);
            }

            for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
                JetParameter parameter = parameters.get(i);
                JetTypeReference typeReference = parameter.getTypeReference();

                JetType type;
                if (typeReference != null) {
                    type = context.typeResolver.resolveType(context.scope, typeReference);
                }
                else {
                    if (valueParameters != null) {
                        type = valueParameters.get(i).getOutType();
                    }
                    else {
                        context.trace.getErrorHandler().genericError(parameter.getNode(), "Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) => ...} notation");
                        type = ErrorUtils.createErrorType("Cannot be inferred");
                    }
                }
                ValueParameterDescriptor valueParameterDescriptor = context.classDescriptorResolver.resolveValueParameterDescriptor(functionDescriptor, parameter, i, type);
                parameterTypes.add(valueParameterDescriptor.getOutType());
                valueParameterDescriptors.add(valueParameterDescriptor);
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
            functionDescriptor.initialize(effectiveReceiverType, Collections.<TypeParameterDescriptor>emptyList(), valueParameterDescriptors, null);
            context.trace.record(BindingContext.FUNCTION, expression, functionDescriptor);

            JetType returnType = NO_EXPECTED_TYPE;
            JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
            JetTypeReference returnTypeRef = functionLiteral.getReturnTypeRef();
            if (returnTypeRef != null) {
                returnType = context.typeResolver.resolveType(context.scope, returnTypeRef);
                context.services.checkFunctionReturnType(functionInnerScope, expression, returnType, context.dataFlowInfo);
            }
            else {
                if (functionTypeExpected) {
                    returnType = JetStandardClasses.getReturnType(expectedType);
                }
                returnType = context.services.getBlockReturnedType(functionInnerScope, functionLiteral.getBodyExpression(), CoercionStrategy.COERCION_TO_UNIT, context.replaceExpectedType(returnType));
            }
            JetType safeReturnType = returnType == null ? ErrorUtils.createErrorType("<return type>") : returnType;
            functionDescriptor.setReturnType(safeReturnType);

            if (functionTypeExpected) {
                JetType expectedReturnType = JetStandardClasses.getReturnType(expectedType);
                if (JetStandardClasses.isUnit(expectedReturnType)) {
                    return context.services.checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), effectiveReceiverType, parameterTypes, expectedReturnType), expression, context);
                }

            }
            return context.services.checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), effectiveReceiverType, parameterTypes, safeReturnType), expression, context);
        }

        @Override
        public JetType visitParenthesizedExpression(JetParenthesizedExpression expression, TypeInferenceContext context) {
            return context.services.checkType(getType(expression.getExpression(), context.replaceScope(context.scope)), expression, context);
        }

        @Override
        public JetType visitConstantExpression(JetConstantExpression expression, TypeInferenceContext context) {
            ASTNode node = expression.getNode();
            IElementType elementType = node.getElementType();
            String text = node.getText();
            JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
            CompileTimeConstantResolver compileTimeConstantResolver = context.services.compileTimeConstantResolver;

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
                context.trace.getErrorHandler().genericError(node, errorValue.getMessage());
                return getDefaultType(elementType);
            }
            else {
                context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, value);
                return context.services.checkType(value.getType(standardLibrary), expression, context);
            }
        }

        @NotNull
        private JetType getDefaultType(IElementType constantType) {
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
        public JetType visitThrowExpression(JetThrowExpression expression, TypeInferenceContext context) {
            JetExpression thrownExpression = expression.getThrownExpression();
            if (thrownExpression != null) {
                JetType type = getType(thrownExpression, context.replaceExpectedType(NO_EXPECTED_TYPE).replaceScope(context.scope));
                // TODO : check that it inherits Throwable
            }
            return context.services.checkType(JetStandardClasses.getNothingType(), expression, context);
        }

        @Override
        public JetType visitReturnExpression(JetReturnExpression expression, TypeInferenceContext context) {
            if (context.expectedReturnType == FORBIDDEN) {
                context.trace.getErrorHandler().genericError(expression.getNode(), "'return' is not allowed here");
                return null;
            }
            JetExpression returnedExpression = expression.getReturnedExpression();

            JetType returnedType = JetStandardClasses.getUnitType();
            if (returnedExpression != null) {
                getType(returnedExpression, context.replaceExpectedType(context.expectedReturnType).replaceScope(context.scope));
            }
            else {
                if (context.expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(context.expectedReturnType)) {
                    context.trace.getErrorHandler().genericError(expression.getNode(), "This function must return a value of type " + context.expectedReturnType);
                }
            }
            return context.services.checkType(JetStandardClasses.getNothingType(), expression, context);
        }

        @Override
        public JetType visitBreakExpression(JetBreakExpression expression, TypeInferenceContext context) {
            return context.services.checkType(JetStandardClasses.getNothingType(), expression, context);
        }

        @Override
        public JetType visitContinueExpression(JetContinueExpression expression, TypeInferenceContext context) {
            return context.services.checkType(JetStandardClasses.getNothingType(), expression, context);
        }

        @Override
        public JetType visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression, TypeInferenceContext context) {
            JetTypeReference right = expression.getRight();
            JetType result = null;
            if (right != null) {
                JetType targetType = context.typeResolver.resolveType(context.scope, right);

                if (isTypeFlexible(expression.getLeft())) {
                    TemporaryBindingTrace temporaryTraceWithExpectedType = TemporaryBindingTrace.create(context.trace);
                    boolean success = checkBinaryWithTypeRHS(expression, context, targetType, targetType, temporaryTraceWithExpectedType);
                    if (success) {
                        temporaryTraceWithExpectedType.commit();
                    }
                    else {
                        TemporaryBindingTrace temporaryTraceWithoutExpectedType = TemporaryBindingTrace.create(context.trace);
                        checkBinaryWithTypeRHS(expression, context, targetType, NO_EXPECTED_TYPE, temporaryTraceWithoutExpectedType);
                        temporaryTraceWithoutExpectedType.commit();
                    }
                }
                else {
                    TemporaryBindingTrace temporaryTraceWithoutExpectedType = TemporaryBindingTrace.create(context.trace);
                    checkBinaryWithTypeRHS(expression, context, targetType, NO_EXPECTED_TYPE, temporaryTraceWithoutExpectedType);
                    temporaryTraceWithoutExpectedType.commit();
                }

                IElementType operationType = expression.getOperationSign().getReferencedNameElementType();
                result = operationType == JetTokens.AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
            }
            else {
                getType(expression.getLeft(), context.replaceExpectedType(NO_EXPECTED_TYPE));
            }
            return context.services.checkType(result, expression, context);
        }

        private boolean isTypeFlexible(@Nullable JetExpression expression) {
            if (expression == null) return false;

            return TokenSet.create(
                    JetNodeTypes.INTEGER_CONSTANT,
                    JetNodeTypes.FLOAT_CONSTANT
            ).contains(expression.getNode().getElementType());
        }

        private boolean checkBinaryWithTypeRHS(JetBinaryExpressionWithTypeRHS expression, TypeInferenceContext context, @NotNull JetType targetType, @NotNull JetType expectedType, TemporaryBindingTrace temporaryTrace) {
            TypeInferenceContext newContext = context.replaceExpectedTypeAndTrace(expectedType, temporaryTrace);

            JetType actualType = getType(expression.getLeft(), newContext);
            if (actualType == null) return false;
            
            JetSimpleNameExpression operationSign = expression.getOperationSign();
            IElementType operationType = operationSign.getReferencedNameElementType();
            if (operationType == JetTokens.COLON) {
                if (targetType != NO_EXPECTED_TYPE && !semanticServices.getTypeChecker().isSubtypeOf(actualType, targetType)) {
                    context.trace.getErrorHandler().typeMismatch(expression.getLeft(), targetType, actualType);
                    return false;
                }
                return true;
            }
            else if (operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
                checkForCastImpossibility(expression, actualType, targetType, context);
                return true;
            }
            else {
                context.trace.getErrorHandler().genericError(operationSign.getNode(), "Unsupported binary operation");
                return false;
            }
        }

        private void checkForCastImpossibility(JetBinaryExpressionWithTypeRHS expression, JetType actualType, JetType targetType, TypeInferenceContext context) {
            if (actualType == null || targetType == NO_EXPECTED_TYPE) return;

            JetTypeChecker typeChecker = semanticServices.getTypeChecker();
            if (!typeChecker.isSubtypeOf(targetType, actualType)) {
                if (typeChecker.isSubtypeOf(actualType, targetType)) {
                    context.trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "No cast needed, use ':' instead");
                }
                else {
                    // See JET-58 Make 'as never succeeds' a warning, or even never check for Java (external) types
                    context.trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "This cast can never succeed");
                }
            }
            else {
                if (typeChecker.isSubtypeOf(actualType, targetType)) {
                    context.trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "No cast needed");
                }
            }
        }

        @Override
        public JetType visitTupleExpression(JetTupleExpression expression, TypeInferenceContext context) {
            List<JetExpression> entries = expression.getEntries();
            List<JetType> types = new ArrayList<JetType>();
            for (JetExpression entry : entries) {
                types.add(context.services.safeGetType(context.scope, entry, NO_EXPECTED_TYPE)); // TODO
            }
            if (context.expectedType != NO_EXPECTED_TYPE && JetStandardClasses.isTupleType(context.expectedType)) {
                List<JetType> enrichedTypes = context.services.checkArgumentTypes(types, entries, context.expectedType.getArguments(), context);
                if (enrichedTypes != types) {
                    return JetStandardClasses.getTupleType(enrichedTypes);
                }
            }
            // TODO : labels
            return context.services.checkType(JetStandardClasses.getTupleType(types), expression, context);
        }

        @Override
        public JetType visitThisExpression(JetThisExpression expression, TypeInferenceContext context) {
            JetType result = null;
            JetType thisType = null;
            String labelName = expression.getLabelName();
            if (labelName != null) {
                Collection<DeclarationDescriptor> declarationsByLabel = context.scope.getDeclarationsByLabel(labelName);
                int size = declarationsByLabel.size();
                final JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                assert targetLabel != null;
                if (size == 1) {
                    DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
                    if (declarationDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                        thisType = classDescriptor.getDefaultType();
                    }
                    else if (declarationDescriptor instanceof FunctionDescriptor) {
                        FunctionDescriptor functionDescriptor = (FunctionDescriptor) declarationDescriptor;
                        thisType = functionDescriptor.getReceiverType();
                    }
                    else {
                        throw new UnsupportedOperationException(); // TODO
                    }
                    context.trace.record(REFERENCE_TARGET, targetLabel, declarationDescriptor);
                    context.trace.record(REFERENCE_TARGET, expression.getThisReference(), declarationDescriptor);
                }
                else if (size == 0) {
                    // This uses the info written by the control flow processor
                    PsiElement psiElement = BindingContextUtils.resolveToDeclarationPsiElement(context.trace.getBindingContext(), targetLabel);
                    if (psiElement instanceof JetFunctionLiteralExpression) {
                        DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiElement);
                        if (declarationDescriptor instanceof FunctionDescriptor) {
                            thisType = ((FunctionDescriptor) declarationDescriptor).getReceiverType();
                            if (thisType == null) {
                                thisType = JetStandardClasses.getNothingType();
                            }
                            else {
                                context.trace.record(REFERENCE_TARGET, targetLabel, declarationDescriptor);
                                context.trace.record(REFERENCE_TARGET, expression.getThisReference(), declarationDescriptor);
                            }
                        }
                        else {
                            context.trace.getErrorHandler().unresolvedReference(targetLabel);
                        }
                    }
                    else {
                        context.trace.getErrorHandler().unresolvedReference(targetLabel);
                    }
                }
                else {
                    context.trace.getErrorHandler().genericError(targetLabel.getNode(), "Ambiguous label");
                }
            }
            else {
                thisType = context.scope.getThisType();

                DeclarationDescriptor declarationDescriptorForUnqualifiedThis = context.scope.getDeclarationDescriptorForUnqualifiedThis();
                if (declarationDescriptorForUnqualifiedThis != null) {
                    context.trace.record(REFERENCE_TARGET, expression.getThisReference(), declarationDescriptorForUnqualifiedThis);
                }
            }

            if (thisType != null) {
                if (JetStandardClasses.isNothing(thisType)) {
                    context.trace.getErrorHandler().genericError(expression.getNode(), "'this' is not defined in this context");
                }
                else {
                    JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
                    if (superTypeQualifier != null) {
                        JetTypeElement superTypeElement = superTypeQualifier.getTypeElement();
                        // Errors are reported by the parser
                        if (superTypeElement instanceof JetUserType) {
                            JetUserType typeElement = (JetUserType) superTypeElement;

                            ClassifierDescriptor classifierCandidate = context.typeResolver.resolveClass(context.scope, typeElement);
                            if (classifierCandidate instanceof ClassDescriptor) {
                                ClassDescriptor superclass = (ClassDescriptor) classifierCandidate;

                                Collection<? extends JetType> supertypes = thisType.getConstructor().getSupertypes();
                                TypeSubstitutor substitutor = TypeSubstitutor.create(thisType);
                                for (JetType declaredSupertype : supertypes) {
                                    if (declaredSupertype.getConstructor().equals(superclass.getTypeConstructor())) {
                                        result = substitutor.safeSubstitute(declaredSupertype, Variance.INVARIANT);
                                        break;
                                    }
                                }
                                if (result == null) {
                                    context.trace.getErrorHandler().genericError(superTypeElement.getNode(), "Not a superclass");
                                }
                            }
                        }
                    } else {
                        result = thisType;
                    }
                    if (result != null) {
                        context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getThisReference(), result);
                    }
                }
            }
            return context.services.checkType(result, expression, context);
        }

        @Override
        public JetType visitBlockExpression(JetBlockExpression expression, TypeInferenceContext context) {
            return context.services.getBlockReturnedType(context.scope, expression, CoercionStrategy.NO_COERCION, context);
        }

        @Override
        public JetType visitWhenExpression(final JetWhenExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
            // TODO :change scope according to the bound value in the when header
            final JetExpression subjectExpression = expression.getSubjectExpression();

            final JetType subjectType = subjectExpression != null ? context.services.safeGetType(context.scope, subjectExpression, NO_EXPECTED_TYPE) : ErrorUtils.createErrorType("Unknown type");
            final VariableDescriptor variableDescriptor = subjectExpression != null ? context.services.getVariableDescriptorFromSimpleName(subjectExpression, context) : null;

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
                return semanticServices.getTypeChecker().commonSupertype(expressionTypes);
            }
            else if (expression.getEntries().isEmpty()) {
                context.trace.getErrorHandler().genericError(expression.getNode(), "Entries required for when-expression"); // TODO : Scope, and maybe this should not an error
            }
            return null;
        }

        private DataFlowInfo checkWhenCondition(@Nullable final JetExpression subjectExpression, final JetType subjectType, JetWhenCondition condition, final WritableScope scopeToExtend, final TypeInferenceContext context, final VariableDescriptor... subjectVariables) {
            final DataFlowInfo[] newDataFlowInfo = new DataFlowInfo[]{context.dataFlowInfo};
            condition.accept(new JetVisitorVoid() {

                @Override
                public void visitWhenConditionCall(JetWhenConditionCall condition) {
                    JetExpression callSuffixExpression = condition.getCallSuffixExpression();
//                    JetScope compositeScope = new ScopeWithReceiver(context.scope, subjectType, semanticServices.getTypeChecker());
                    if (callSuffixExpression != null) {
//                        JetType selectorReturnType = getType(compositeScope, callSuffixExpression, false, context);
                        JetType selectorReturnType = getSelectorReturnType(subjectType, callSuffixExpression, context);//getType(compositeScope, callSuffixExpression, false, context);
                        ensureBooleanResultWithCustomSubject(callSuffixExpression, selectorReturnType, "This expression", context);
                        context.services.checkNullSafety(subjectType, condition.getOperationTokenNode(), getCalleeFunctionDescriptor(callSuffixExpression, context));
                    }
                }

                @Override
                public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                    JetExpression rangeExpression = condition.getRangeExpression();
                    if (rangeExpression != null) {
                        assert subjectExpression != null;
                        checkInExpression(condition.getOperationReference(), subjectExpression, rangeExpression, context);
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
                    context.trace.getErrorHandler().genericError(element.getNode(), "Unsupported [JetTypeInferrer] : " + element);
                }
            });
            return newDataFlowInfo[0];
        }

        private DataFlowInfo checkPatternType(@NotNull JetPattern pattern, @NotNull final JetType subjectType, @NotNull final WritableScope scopeToExtend, final TypeInferenceContext context, @NotNull final VariableDescriptor... subjectVariables) {
            final DataFlowInfo[] result = new DataFlowInfo[] {context.dataFlowInfo};
            pattern.accept(new JetVisitorVoid() {
                @Override
                public void visitTypePattern(JetTypePattern typePattern) {
                    JetTypeReference typeReference = typePattern.getTypeReference();
                    if (typeReference != null) {
                        JetType type = context.typeResolver.resolveType(context.scope, typeReference);
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
                        context.trace.getErrorHandler().genericError(pattern.getNode(), "Type mismatch: subject is of type " + subjectType + " but the pattern is of type Tuple" + entries.size()); // TODO : message
                    }
                    else {
                        for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
                            JetTuplePatternEntry entry = entries.get(i);
                            JetType type = subjectType.getArguments().get(i).getType();

                            // TODO : is a name always allowed, ie for tuple patterns, not decomposer arg lists?
                            ASTNode nameLabelNode = entry.getNameLabelNode();
                            if (nameLabelNode != null) {
                                context.trace.getErrorHandler().genericError(nameLabelNode, "Unsupported [JetTypeInferrer]");
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
                        JetType selectorReturnType = getSelectorReturnType(subjectType, decomposerExpression, context);

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
                    JetType type = propertyTypeRef == null ? subjectType : context.typeResolver.resolveType(context.scope, propertyTypeRef);
                    VariableDescriptor variableDescriptor = context.classDescriptorResolver.resolveLocalVariableDescriptorWithType(context.scope.getContainingDeclaration(), variableDeclaration, type);
                    scopeToExtend.addVariableDescriptor(variableDescriptor);
                    if (propertyTypeRef != null) {
                        if (!semanticServices.getTypeChecker().isSubtypeOf(subjectType, type)) {
                            context.trace.getErrorHandler().genericError(propertyTypeRef.getNode(), type + " must be a supertype of " + subjectType + ". Use 'is' to match against " + type);
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
                    if (TypeUtils.intersect(semanticServices.getTypeChecker(), Sets.newHashSet(type, subjectType)) == null) {
                        context.trace.getErrorHandler().genericError(reportErrorOn.getNode(), "Incompatible types: " + type + " and " + subjectType);
                    }
                }

                @Override
                public void visitJetElement(JetElement element) {
                    context.trace.getErrorHandler().genericError(element.getNode(), "Unsupported [JetTypeInferrer]");
                }
            });
            return result[0];
        }

        @Override
        public JetType visitTryExpression(JetTryExpression expression, TypeInferenceContext context) {
            JetExpression tryBlock = expression.getTryBlock();
            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            JetFinallySection finallyBlock = expression.getFinallyBlock();
            List<JetType> types = new ArrayList<JetType>();
            for (JetCatchClause catchClause : catchClauses) {
                JetParameter catchParameter = catchClause.getCatchParameter();
                JetExpression catchBody = catchClause.getCatchBody();
                if (catchParameter != null) {
                    VariableDescriptor variableDescriptor = context.classDescriptorResolver.resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, catchParameter);
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
                return semanticServices.getTypeChecker().commonSupertype(types);
            }
        }

        @Override
        public JetType visitIfExpression(JetIfExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
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
                    return context.services.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
                }
                return null;
            }
            if (thenBranch == null) {
                JetType type = getTypeWithNewScopeAndDataFlowInfo(context.scope, elseBranch, elseInfo, context);
                if (type != null && JetStandardClasses.isNothing(type)) {
                    resultDataFlowInfo = thenInfo;
//                    resultScope = thenScope;
                }
                return context.services.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
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
                result = semanticServices.getTypeChecker().commonSupertype(Arrays.asList(thenType, elseType));
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
        private DataFlowInfo extractDataFlowInfoFromCondition(@Nullable JetExpression condition, final boolean conditionValue, @Nullable final WritableScope scopeToExtend, final TypeInferenceContext context) {
            if (condition == null) return context.dataFlowInfo;
            final DataFlowInfo[] result = new DataFlowInfo[] {context.dataFlowInfo};
            condition.accept(new JetVisitorVoid() {
                @Override
                public void visitIsExpression(JetIsExpression expression) {
                    if (conditionValue && !expression.isNegated() || !conditionValue && expression.isNegated()) {
                        JetPattern pattern = expression.getPattern();
                        result[0] = patternsToDataFlowInfo.get(pattern);
                        if (scopeToExtend != null) {
                            List<VariableDescriptor> descriptors = patternsToBoundVariableLists.get(pattern);
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

                        VariableDescriptor variableDescriptor = context.services.getVariableDescriptorFromSimpleName(left, context);
                        if (variableDescriptor == null) return;

                        // TODO : validate that DF makes sense for this variable: local, val, internal w/backing field, etc

                        // Comparison to a non-null expression
                        JetType rhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, right);
                        if (rhsType != null && !rhsType.isNullable()) {
                            extendDataFlowWithNullComparison(operationToken, variableDescriptor, !conditionValue);
                            return;
                        }

                        VariableDescriptor rightVariable = context.services.getVariableDescriptorFromSimpleName(right, context);
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

        private void checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition, TypeInferenceContext context) {
            if (condition != null) {
                JetType conditionType = getType(condition, context.replaceScope(scope));

                if (conditionType != null && !isBoolean(conditionType)) {
                    context.trace.getErrorHandler().genericError(condition.getNode(), "Condition must be of type Boolean, but was of type " + conditionType);
                }
            }
        }

        @Override
        public JetType visitWhileExpression(JetWhileExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
            JetExpression condition = expression.getCondition();
            checkCondition(context.scope, condition, context);
            JetExpression body = expression.getBody();
            if (body != null) {
                WritableScopeImpl scopeToExtend = newWritableScopeImpl(context.scope, context.trace).setDebugName("Scope extended in while's condition");
                DataFlowInfo conditionInfo = condition == null ? context.dataFlowInfo : extractDataFlowInfoFromCondition(condition, true, scopeToExtend, context);
                getTypeWithNewScopeAndDataFlowInfo(scopeToExtend, body, conditionInfo, context);
            }
            if (!flowInformationProvider.isBreakable(expression)) {
//                resultScope = newWritableScopeImpl();
                resultDataFlowInfo = extractDataFlowInfoFromCondition(condition, false, null, context);
            }
            return context.services.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
        }

        @Override
        public JetType visitDoWhileExpression(JetDoWhileExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
            JetExpression body = expression.getBody();
            JetScope conditionScope = context.scope;
            if (body instanceof JetFunctionLiteralExpression) {
                JetFunctionLiteralExpression function = (JetFunctionLiteralExpression) body;
                if (!function.getFunctionLiteral().hasParameterSpecification()) {
                    WritableScope writableScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("do..while body scope");
                    conditionScope = writableScope;
                    context.services.getBlockReturnedTypeWithWritableScope(writableScope, function.getFunctionLiteral().getBodyExpression().getStatements(), CoercionStrategy.NO_COERCION, context);
                    context.trace.record(BindingContext.BLOCK, function);
                } else {
                    getType(body, context.replaceScope(context.scope));
                }
            }
            else if (body != null) {
                WritableScope writableScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("do..while body scope");
                conditionScope = writableScope;
                context.services.getBlockReturnedTypeWithWritableScope(writableScope, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context);
            }
            JetExpression condition = expression.getCondition();
            checkCondition(conditionScope, condition, context);
            if (!flowInformationProvider.isBreakable(expression)) {
//                resultScope = newWritableScopeImpl();
                resultDataFlowInfo = extractDataFlowInfoFromCondition(condition, false, null, context);
            }
            return context.services.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
        }

        protected WritableScopeImpl newWritableScopeImpl(JetScope scope, BindingTrace trace) {
            return new WritableScopeImpl(scope, scope.getContainingDeclaration(), trace.getErrorHandler());
        }

        @Override
        public JetType visitForExpression(JetForExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
            JetParameter loopParameter = expression.getLoopParameter();
            JetExpression loopRange = expression.getLoopRange();
            JetType loopRangeType = null;
            if (loopRange != null) {
                loopRangeType = getType(loopRange, context.replaceScope(context.scope));
            }
            JetType expectedParameterType = null;
            if (loopRangeType != null) {
                expectedParameterType = checkIterableConvention(loopRangeType, loopRange, context);
            }

            WritableScope loopScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("Scope with for-loop index");

            if (loopParameter != null) {
                JetTypeReference typeReference = loopParameter.getTypeReference();
                VariableDescriptor variableDescriptor;
                if (typeReference != null) {
                    variableDescriptor = context.classDescriptorResolver.resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, loopParameter);
                    JetType actualParameterType = variableDescriptor.getOutType();
                    if (expectedParameterType != null &&
                            actualParameterType != null &&
                            !semanticServices.getTypeChecker().isSubtypeOf(expectedParameterType, actualParameterType)) {
                        context.trace.getErrorHandler().genericError(typeReference.getNode(), "The loop iterates over values of type " + expectedParameterType + " but the parameter is declared to be " + actualParameterType);
                    }
                }
                else {
                    if (expectedParameterType == null) {
                        expectedParameterType = ErrorUtils.createErrorType("Error");
                    }
                    variableDescriptor = context.classDescriptorResolver.resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), loopParameter, expectedParameterType);
                }
                loopScope.addVariableDescriptor(variableDescriptor);
            }

            JetExpression body = expression.getBody();
            if (body != null) {
                getType(body, context.replaceScope(loopScope));
            }

            return context.services.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
        }

        @Nullable
        private JetType checkIterableConvention(@NotNull JetType type, @NotNull JetExpression loopRange, TypeInferenceContext context) {
            ASTNode reportErrorsOn = loopRange.getNode();
            OverloadResolutionResult<FunctionDescriptor> iteratorResolutionResult = context.services.callResolver.resolveExactSignature(context.scope, type, "iterator", Collections.<JetType>emptyList());
            if (iteratorResolutionResult.isSuccess()) {
                FunctionDescriptor iteratorFunction = iteratorResolutionResult.getDescriptor();
                
                context.trace.record(LOOP_RANGE_ITERATOR, loopRange, iteratorFunction);
                
                JetType iteratorType = iteratorFunction.getReturnType();
                FunctionDescriptor hasNextFunction = checkHasNextFunctionSupport(loopRange, iteratorType, context);
                boolean hasNextFunctionSupported = hasNextFunction != null;
                VariableDescriptor hasNextProperty = checkHasNextPropertySupport(loopRange, iteratorType, context);
                boolean hasNextPropertySupported = hasNextProperty != null;
                if (hasNextFunctionSupported && hasNextPropertySupported && !ErrorUtils.isErrorType(iteratorType)) {
                    // TODO : overload resolution rules impose priorities here???
                    context.trace.getErrorHandler().genericError(reportErrorsOn, "An ambiguity between 'iterator().hasNext()' function and 'iterator().hasNext' property");
                }
                else if (!hasNextFunctionSupported && !hasNextPropertySupported) {
                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().hasNext()' function or an 'iterator().hasNext' property");
                }
                else {
                    context.trace.record(LOOP_RANGE_HAS_NEXT, loopRange, hasNextFunctionSupported ? hasNextFunction : hasNextProperty);
                }

                OverloadResolutionResult<FunctionDescriptor> nextResolutionResult = context.services.callResolver.resolveExactSignature(context.scope, iteratorType, "next", Collections.<JetType>emptyList());
                if (nextResolutionResult.isAmbiguity()) {
                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Method 'iterator().next()' is ambiguous for this expression");
                } else if (nextResolutionResult.isNothing()) {
                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().next()' method");
                } else {
                    FunctionDescriptor nextFunction = nextResolutionResult.getDescriptor();
                    context.trace.record(LOOP_RANGE_NEXT, loopRange, nextFunction);
                    return nextFunction.getReturnType();
                }
            }
            else {
                String errorMessage = "For-loop range must have an iterator() method";
                if (iteratorResolutionResult.isAmbiguity()) {
                    StringBuffer stringBuffer = new StringBuffer("Method 'iterator()' is ambiguous for this expression: ");
                    for (FunctionDescriptor functionDescriptor : iteratorResolutionResult.getDescriptors()) {
                        stringBuffer.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                    }
                    errorMessage = "Method 'iterator()' is ambiguous for this expression: " + stringBuffer;
                }
                context.trace.getErrorHandler().genericError(reportErrorsOn, errorMessage);
            }
            return null;
        }

        @Nullable
        private FunctionDescriptor checkHasNextFunctionSupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, TypeInferenceContext context) {
            OverloadResolutionResult<FunctionDescriptor> hasNextResolutionResult = context.services.callResolver.resolveExactSignature(context.scope, iteratorType, "hasNext", Collections.<JetType>emptyList());
            if (hasNextResolutionResult.isAmbiguity()) {
                context.trace.getErrorHandler().genericError(loopRange.getNode(), "Method 'iterator().hasNext()' is ambiguous for this expression");
            } else if (hasNextResolutionResult.isNothing()) {
                return null;
            } else {
                assert hasNextResolutionResult.isSuccess();
                JetType hasNextReturnType = hasNextResolutionResult.getDescriptor().getReturnType();
                if (!isBoolean(hasNextReturnType)) {
                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext()' method of the loop range must return Boolean, but returns " + hasNextReturnType);
                }
            }
            return hasNextResolutionResult.getDescriptor();
        }

        @Nullable
        private VariableDescriptor checkHasNextPropertySupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, TypeInferenceContext context) {
            VariableDescriptor hasNextProperty = iteratorType.getMemberScope().getVariable("hasNext");
            // TODO :extension properties
            if (hasNextProperty == null) {
                return null;
            } else {
                JetType hasNextReturnType = hasNextProperty.getOutType();
                if (hasNextReturnType == null) {
                    // TODO : accessibility
                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext' property of the loop range must be readable");
                }
                else if (!isBoolean(hasNextReturnType)) {
                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext' property of the loop range must return Boolean, but returns " + hasNextReturnType);
                }
            }
            return hasNextProperty;
        }

        @Override
        public JetType visitHashQualifiedExpression(JetHashQualifiedExpression expression, TypeInferenceContext context) {
            context.trace.getErrorHandler().genericError(expression.getOperationTokenNode(), "Unsupported");
            return null;
        }

        @Override
        public JetType visitQualifiedExpression(JetQualifiedExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
            // TODO : functions as values
            JetExpression selectorExpression = expression.getSelectorExpression();
            JetExpression receiverExpression = expression.getReceiverExpression();
            JetType receiverType = context.services.typeInferrerVisitorWithNamespaces.getType(receiverExpression, context.replaceExpectedTypes(NO_EXPECTED_TYPE, NO_EXPECTED_TYPE));
            if (selectorExpression == null) return null;
            if (receiverType == null) receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());

            if (selectorExpression instanceof JetSimpleNameExpression) {
                propagateConstantValues(expression, context, (JetSimpleNameExpression) selectorExpression);
            }

            // Clean resolution: no autocasts
            TemporaryBindingTrace cleanResolutionTrace = TemporaryBindingTrace.create(context.trace);
            TypeInferenceContext cleanResolutionContext = context.replaceBindingTrace(cleanResolutionTrace);
            JetType selectorReturnType = getSelectorReturnType(receiverType, selectorExpression, cleanResolutionContext);

            //TODO move further
            if (expression.getOperationSign() == JetTokens.SAFE_ACCESS) {
                if (selectorReturnType != null && !selectorReturnType.isNullable() && !JetStandardClasses.isUnit(selectorReturnType)) {
                    selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
                }
            }
            if (selectorReturnType != null) {
                cleanResolutionTrace.addAllMyDataTo(context.trace);
            }
            else {
                VariableDescriptor variableDescriptor = cleanResolutionContext.services.getVariableDescriptorFromSimpleName(receiverExpression, context);
                boolean somethingFound = false;
                if (variableDescriptor != null) {
                    List<JetType> possibleTypes = Lists.newArrayList(context.dataFlowInfo.getPossibleTypes(variableDescriptor));
                    Collections.reverse(possibleTypes);

                    TemporaryBindingTrace autocastResolutionTrace = TemporaryBindingTrace.create(context.trace);
                    TypeInferenceContext autocastResolutionContext = context.replaceBindingTrace(autocastResolutionTrace);
                    for (JetType possibleType : possibleTypes) {
                        selectorReturnType = getSelectorReturnType(possibleType, selectorExpression, autocastResolutionContext);
                        if (selectorReturnType != null) {
                            context.services.checkAutoCast(receiverExpression, possibleType, variableDescriptor, autocastResolutionTrace);
                            autocastResolutionTrace.commit();
                            somethingFound = true;
                            break;
                        }
                        else {
                            autocastResolutionTrace = TemporaryBindingTrace.create(context.trace);
                            autocastResolutionContext = context.replaceBindingTrace(autocastResolutionTrace);
                        }
                    }
                }
                if (!somethingFound) {
                    cleanResolutionTrace.commit();
                }
            }

            JetType result;
            if (expression.getOperationSign() == JetTokens.QUEST) {
                if (selectorReturnType != null && !isBoolean(selectorReturnType) && selectorExpression != null) {
                    // TODO : more comprehensible error message
                    context.trace.getErrorHandler().typeMismatch(selectorExpression, semanticServices.getStandardLibrary().getBooleanType(), selectorReturnType);
                }
                result = TypeUtils.makeNullable(receiverType);
            }
            else {
                result = selectorReturnType;
            }
            if (selectorExpression != null && result != null) {
                context.trace.record(BindingContext.EXPRESSION_TYPE, selectorExpression, result);
            }
            if (selectorReturnType != null) {
                // TODO : extensions to 'Any?'
                if (selectorExpression != null) {
                    receiverType = context.services.enrichOutType(receiverExpression, receiverType, context);

                    context.services.checkNullSafety(receiverType, expression.getOperationTokenNode(), getCalleeFunctionDescriptor(selectorExpression, context));
                }
            }
            return context.services.checkType(result, expression, contextWithExpectedType);
        }

        private void propagateConstantValues(JetQualifiedExpression expression, TypeInferenceContext context, JetSimpleNameExpression selectorExpression) {
            JetExpression receiverExpression = expression.getReceiverExpression();
            CompileTimeConstant<?> receiverValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, receiverExpression);
            CompileTimeConstant<?> wholeExpressionValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
            if (wholeExpressionValue == null && receiverValue != null && !(receiverValue instanceof ErrorValue) && receiverValue.getValue() instanceof Number) {
                Number value = (Number) receiverValue.getValue();
                String referencedName = selectorExpression.getReferencedName();
                if (numberConversions.contains(referencedName)) {
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


        @NotNull
        private FunctionDescriptor getCalleeFunctionDescriptor(@NotNull JetExpression selectorExpression, final TypeInferenceContext context) {
            final FunctionDescriptor[] result = new FunctionDescriptor[1];
            selectorExpression.accept(new JetVisitorVoid() {
                @Override
                public void visitCallExpression(JetCallExpression callExpression) {
                    JetExpression calleeExpression = callExpression.getCalleeExpression();
                    if (calleeExpression != null) {
                        calleeExpression.accept(this);
                    }
                }

                @Override
                public void visitReferenceExpression(JetReferenceExpression referenceExpression) {
                    DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(REFERENCE_TARGET, referenceExpression);
                    if (declarationDescriptor instanceof FunctionDescriptor) {
                        result[0] = (FunctionDescriptor) declarationDescriptor;
                    }
                }

                @Override
                public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
                    expression.getArrayExpression().accept(this);
                }

                @Override
                public void visitBinaryExpression(JetBinaryExpression expression) {
                    expression.getLeft().accept(this);
                }

                @Override
                public void visitQualifiedExpression(JetQualifiedExpression expression) {
                    expression.getReceiverExpression().accept(this);
                }

                @Override
                public void visitJetElement(JetElement element) {
                    context.trace.getErrorHandler().genericError(element.getNode(), "Unsupported [getCalleeFunctionDescriptor]: " + element);
                }
            });
            if (result[0] == null) {
                result[0] = ErrorUtils.createErrorFunction(0, Collections.<JetType>emptyList());
            }
            return result[0];
        }

        @Nullable
        private JetType getSelectorReturnType(@Nullable JetType receiverType, @NotNull JetExpression selectorExpression, @NotNull TypeInferenceContext context) {
            if (selectorExpression instanceof JetCallExpression) {
                return context.services.callResolver.resolveCall(context.trace, context.scope, receiverType, (JetCallExpression) selectorExpression, context.expectedType);
            }
            else if (selectorExpression instanceof JetSimpleNameExpression) {
                JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) selectorExpression;

                TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
                VariableDescriptor variableDescriptor = context.services.callResolver.resolveSimpleProperty(temporaryTrace, context.scope, receiverType, nameExpression, context.expectedType);
                if (variableDescriptor != null) {
                    temporaryTrace.commit();
                    return context.services.checkEnrichedType(variableDescriptor.getOutType(), nameExpression, context);
                }
                TypeInferenceContext newContext = receiverType == null ? context : context.replaceScope(receiverType.getMemberScope());
                JetType jetType = lookupNamespaceOrClassObject(nameExpression, nameExpression.getReferencedName(), newContext);
                if (jetType == null) {
                    context.trace.getErrorHandler().unresolvedReference(nameExpression);
                }
                return context.services.checkEnrichedType(jetType, nameExpression, context);
//                JetScope scope = receiverType != null ? receiverType.getMemberScope() : context.scope;
//                return getType(selectorExpression, context.replaceScope(scope));
            }
            else if (selectorExpression instanceof JetQualifiedExpression) {
                JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) selectorExpression;
                JetType newReceiverType = getSelectorReturnType(receiverType, qualifiedExpression.getReceiverExpression(), context.replaceExpectedType(NO_EXPECTED_TYPE));
                JetExpression newSelectorExpression = qualifiedExpression.getSelectorExpression();
                if (newReceiverType != null && newSelectorExpression != null) {
                    return getSelectorReturnType(newReceiverType, newSelectorExpression, context);
                }
            }
            else {
                // TODO : not a simple name -> resolve in scope, expect property type or a function type
                context.trace.getErrorHandler().genericError(selectorExpression.getNode(), "Unsupported selector element type: " + selectorExpression);
            }
            return null;
        }

        @Override
        public JetType visitCallExpression(JetCallExpression expression, TypeInferenceContext context) {
            JetType expressionType = context.services.callResolver.resolveCall(context.trace, context.scope, null, expression, context.expectedType);
            return context.services.checkType(expressionType, expression, context);
        }

        @Override
        public JetType visitIsExpression(JetIsExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
            JetType knownType = safeGetType(expression.getLeftHandSide(), context.replaceScope(context.scope));
            JetPattern pattern = expression.getPattern();
            if (pattern != null) {
                WritableScopeImpl scopeToExtend = newWritableScopeImpl(context.scope, context.trace).setDebugName("Scope extended in 'is'");
                DataFlowInfo newDataFlowInfo = checkPatternType(pattern, knownType, scopeToExtend, context, context.services.getVariableDescriptorFromSimpleName(expression.getLeftHandSide(), context));
                patternsToDataFlowInfo.put(pattern, newDataFlowInfo);
                patternsToBoundVariableLists.put(pattern, scopeToExtend.getDeclaredVariables());
            }
            return context.services.checkType(semanticServices.getStandardLibrary().getBooleanType(), expression, contextWithExpectedType);
        }

        @Override
        public JetType visitUnaryExpression(JetUnaryExpression expression, TypeInferenceContext context) {
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression == null) return null;
            JetSimpleNameExpression operationSign = expression.getOperationSign();
            if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
                // TODO : Some processing for the label?
                return context.services.checkType(getType(baseExpression, context.replaceExpectedReturnType(context.expectedType)), expression, context);
            }
            IElementType operationType = operationSign.getReferencedNameElementType();
            String name = unaryOperationNames.get(operationType);
            if (name == null) {
                context.trace.getErrorHandler().genericError(operationSign.getNode(), "Unknown unary operation");
                return null;
            }
            JetType receiverType = getType(baseExpression, context.replaceExpectedType(NO_EXPECTED_TYPE).replaceScope(context.scope));
            if (receiverType == null) return null;

            FunctionDescriptor functionDescriptor = context.services.callResolver.resolveCallWithGivenName(
                    context.trace,
                    context.scope,
                    CallMaker.makeCall(expression),
                    expression.getOperationSign(),
                    name,
                    receiverType,
                    context.expectedType);

            if (functionDescriptor == null) return null;
            JetType returnType = functionDescriptor.getReturnType();
            JetType result;
            if (operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) {
                if (semanticServices.getTypeChecker().isSubtypeOf(returnType, JetStandardClasses.getUnitType())) {
                    result = JetStandardClasses.getUnitType();
                }
                else {
                    if (!semanticServices.getTypeChecker().isSubtypeOf(returnType, receiverType)) {
                        context.trace.getErrorHandler().genericError(operationSign.getNode(), name + " must return " + receiverType + " but returns " + returnType);
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
            return context.services.checkType(result, expression, context);
        }

        @Override
        public JetType visitBinaryExpression(JetBinaryExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
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
            else if (binaryOperationNames.containsKey(operationType)) {
                result = getTypeForBinaryCall(context.scope, binaryOperationNames.get(operationType), context, expression);
            }
            else if (operationType == JetTokens.EQ) {
                result = visitAssignment(expression, context);
            }
            else if (assignmentOperationNames.containsKey(operationType)) {
                result = visitAssignmentOperation(expression, context);
            }
            else if (comparisonOperations.contains(operationType)) {
                JetType compareToReturnType = getTypeForBinaryCall(context.scope, "compareTo", context, expression);
                if (compareToReturnType != null) {
                    TypeConstructor constructor = compareToReturnType.getConstructor();
                    JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
                    TypeConstructor intTypeConstructor = standardLibrary.getInt().getTypeConstructor();
                    if (constructor.equals(intTypeConstructor)) {
                        result = standardLibrary.getBooleanType();
                    } else {
                        context.trace.getErrorHandler().genericError(operationSign.getNode(), "compareTo must return Int, but returns " + compareToReturnType);
                    }
                }
            }
            else if (equalsOperations.contains(operationType)) {
                String name = "equals";
                if (right != null) {
                    JetType leftType = safeGetType(left, context.replaceScope(context.scope));
                    OverloadResolutionResult<FunctionDescriptor> resolutionResult = context.services.callResolver.resolveExactSignature(
                            context.scope, leftType, "equals",
                            Collections.singletonList(JetStandardClasses.getNullableAnyType()));
                    if (resolutionResult.isSuccess()) {
                        FunctionDescriptor equals = resolutionResult.getDescriptor();
                        context.trace.record(REFERENCE_TARGET, operationSign, equals);
                        if (ensureBooleanResult(operationSign, name, equals.getReturnType(), context)) {
                            ensureNonemptyIntersectionOfOperandTypes(expression, context);
                        }
                    }
                    else {
                        if (resolutionResult.isAmbiguity()) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (FunctionDescriptor functionDescriptor : resolutionResult.getDescriptors()) {
                                stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                            }
                            context.trace.getErrorHandler().genericError(operationSign.getNode(), "Ambiguous function: " + stringBuilder);
                        }
                        else {
                            context.trace.getErrorHandler().genericError(operationSign.getNode(), "No method 'equals(Any?) : Boolean' available");
                        }
                    }
                }
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.EQEQEQ || operationType == JetTokens.EXCLEQEQEQ) {
                ensureNonemptyIntersectionOfOperandTypes(expression, context);

                // TODO : Check comparison pointlessness
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (inOperations.contains(operationType)) {
                if (right == null) {
                    result = ErrorUtils.createErrorType("No right argument"); // TODO
                    return null;
                }
                checkInExpression(operationSign, left, right, context);
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.ANDAND || operationType == JetTokens.OROR) {
                JetType leftType = getType(left, context.replaceScope(context.scope));
                WritableScopeImpl leftScope = newWritableScopeImpl(context.scope, context.trace).setDebugName("Left scope of && or ||");
                DataFlowInfo flowInfoLeft = extractDataFlowInfoFromCondition(left, operationType == JetTokens.ANDAND, leftScope, context);  // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
                WritableScopeImpl rightScope = operationType == JetTokens.ANDAND ? leftScope : newWritableScopeImpl(context.scope, context.trace).setDebugName("Right scope of && or ||");
                JetType rightType = right == null ? null : getType(right, context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope));
                if (leftType != null && !isBoolean(leftType)) {
                    context.trace.getErrorHandler().typeMismatch(left, semanticServices.getStandardLibrary().getBooleanType(), leftType);
                }
                if (rightType != null && !isBoolean(rightType)) {
                    context.trace.getErrorHandler().typeMismatch(right, semanticServices.getStandardLibrary().getBooleanType(), rightType);
                }
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.ELVIS) {
                JetType leftType = getType(left, context.replaceScope(context.scope));
                JetType rightType = right == null ? null : getType(right, contextWithExpectedType.replaceScope(context.scope));
                if (leftType != null) {
                    if (!leftType.isNullable()) {
                        context.trace.getErrorHandler().genericWarning(left.getNode(), "Elvis operator (?:) is always returns the left operand of non-nullable type " + leftType);
                    }
                    if (rightType != null) {
                        context.services.checkType(TypeUtils.makeNullableAsSpecified(leftType, rightType.isNullable()), left, contextWithExpectedType);
                        return TypeUtils.makeNullableAsSpecified(semanticServices.getTypeChecker().commonSupertype(leftType, rightType), rightType.isNullable());
                    }
                }
            }
            else {
                context.trace.getErrorHandler().genericError(operationSign.getNode(), "Unknown operation");
            }
            return context.services.checkType(result, expression, contextWithExpectedType);
        }

        private void checkInExpression(JetSimpleNameExpression operationSign, JetExpression left, JetExpression right, TypeInferenceContext context) {
            String name = "contains";
            JetType receiverType = context.services.safeGetType(context.scope, right, NO_EXPECTED_TYPE);
            FunctionDescriptor functionDescriptor = context.services.callResolver.resolveCallWithGivenName(
                    context.trace,
                    context.scope,
                    CallMaker.makeCall(operationSign, Collections.singletonList(left)),
                    operationSign,
                    name, receiverType, context.expectedType);
            JetType containsType = functionDescriptor != null ? functionDescriptor.getReturnType() : null;
            ensureBooleanResult(operationSign, name, containsType, context);
        }

        private void ensureNonemptyIntersectionOfOperandTypes(JetBinaryExpression expression, TypeInferenceContext context) {
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            JetExpression left = expression.getLeft();
            JetExpression right = expression.getRight();

            // TODO : duplicated effort for == and !=
            JetType leftType = getType(left, context.replaceScope(context.scope));
            if (leftType != null && right != null) {
                JetType rightType = getType(right, context.replaceScope(context.scope));

                if (rightType != null) {
                    JetType intersect = TypeUtils.intersect(semanticServices.getTypeChecker(), new HashSet<JetType>(Arrays.asList(leftType, rightType)));
                    if (intersect == null) {
                        context.trace.getErrorHandler().genericError(expression.getNode(), "Operator " + operationSign.getReferencedName() + " cannot be applied to " + leftType + " and " + rightType);
                    }
                }
            }
        }

        protected JetType visitAssignmentOperation(JetBinaryExpression expression, TypeInferenceContext context) {
            return assignmentIsNotAnExpressionError(expression, context);
        }

        protected JetType visitAssignment(JetBinaryExpression expression, TypeInferenceContext context) {
            return assignmentIsNotAnExpressionError(expression, context);
        }

        private JetType assignmentIsNotAnExpressionError(JetBinaryExpression expression, TypeInferenceContext context) {
            context.trace.getErrorHandler().genericError(expression.getNode(), "Assignments are not expressions, and only expressions are allowed in this context");
            return null;
        }

        private boolean ensureBooleanResult(JetExpression operationSign, String name, JetType resultType, TypeInferenceContext context) {
            return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
        }

        private boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, TypeInferenceContext context) {
            if (resultType != null) {
                // TODO : Relax?
                if (!isBoolean(resultType)) {
                    context.trace.getErrorHandler().genericError(operationSign.getNode(), subjectName + " must return Boolean but returns " + resultType);
                    return false;
                }
            }
            return true;
        }

        private boolean isBoolean(@NotNull JetType type) {
            return semanticServices.getTypeChecker().isConvertibleTo(type,  semanticServices.getStandardLibrary().getBooleanType());
        }

        @Override
        public JetType visitArrayAccessExpression(JetArrayAccessExpression expression, TypeInferenceContext contextWithExpectedType) {
            TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
            JetExpression arrayExpression = expression.getArrayExpression();
            JetType receiverType = getType(arrayExpression, context.replaceScope(context.scope));

            if (receiverType != null) {
                FunctionDescriptor functionDescriptor = context.services.callResolver.resolveCallWithGivenName(
                        context.trace,
                        context.scope,
                        CallMaker.makeCall(expression, expression.getIndexExpressions()),
                        expression, 
                        "get",
                        receiverType,
                        context.expectedType);
                if (functionDescriptor != null) {
                    return context.services.checkType(functionDescriptor.getReturnType(), expression, contextWithExpectedType);
                }
            }
            return null;
        }

        @Nullable
        protected JetType getTypeForBinaryCall(JetScope scope, String name, TypeInferenceContext context, JetBinaryExpression binaryExpression) {
            JetType leftType = getType(binaryExpression.getLeft(), context.replaceScope(scope));
            FunctionDescriptor functionDescriptor = context.services.callResolver.resolveCallWithGivenName(
                    context.trace,
                    scope,
                    CallMaker.makeCall(binaryExpression),
                    binaryExpression.getOperationReference(),
                    name,
                    leftType,
                    context.expectedType);
            if (functionDescriptor != null) {
                if (leftType != null && leftType.isNullable()) {
                    // TODO : better error message for '1 + nullableVar' case
                    JetExpression right = binaryExpression.getRight();
                    String rightText = right == null ? "" : right.getText();
                    String leftText = binaryExpression.getLeft().getText();
                    context.trace.getErrorHandler().genericError(binaryExpression.getOperationReference().getNode(),
                                                                 "Infix call corresponds to a dot-qualified call '" +
                                                                 leftText + "." + name + "(" + rightText + ")'" +
                                                                 " which is not allowed on a nullable receiver '" + leftText + "'." +
                                                                 " Use '?.'-qualified call instead");
                }

                return functionDescriptor.getReturnType();
            }
            return null;
        }

        @Override
        public JetType visitDeclaration(JetDeclaration dcl, TypeInferenceContext context) {
            context.trace.getErrorHandler().genericError(dcl.getNode(), "Declarations are not allowed in this position");
            return null;
        }

        @Override
        public JetType visitRootNamespaceExpression(JetRootNamespaceExpression expression, TypeInferenceContext context) {
            context.trace.getErrorHandler().genericError(expression.getNode(), "'namespace' is not an expression");
            return null;
        }


        @Override
        public JetType visitStringTemplateExpression(JetStringTemplateExpression expression, TypeInferenceContext contextWithExpectedType) {
            final TypeInferenceContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
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
                            context.trace.getErrorHandler().genericError(entry.getNode(), "Illegal escape sequence");
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
            return context.services.checkType(semanticServices.getStandardLibrary().getStringType(), expression, contextWithExpectedType);
        }

        @Override
        public JetType visitJetElement(JetElement element, TypeInferenceContext context) {
            context.trace.getErrorHandler().genericError(element.getNode(), "[JetTypeInferrer] Unsupported element: " + element + " " + element.getClass().getCanonicalName());
            return null;
        }
    }

    private class TypeInferrerVisitorWithNamespaces extends TypeInferrerVisitor {
        @Override
        public boolean isNamespacePosition() {
            return true;
        }

        @Override
        public JetType visitRootNamespaceExpression(JetRootNamespaceExpression expression, TypeInferenceContext context) {
            return context.services.checkType(JetModuleUtil.getRootNamespaceType(expression), expression, context);
        }

        @Override
        protected boolean furtherNameLookup(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName, @NotNull JetType[] result, TypeInferenceContext context) {
            result[0] = lookupNamespaceType(expression, referencedName, context);
            return result[0] != null;
        }

    }

    private class TypeInferrerVisitorWithWritableScope extends TypeInferrerVisitor {
        private final WritableScope scope;

        public TypeInferrerVisitorWithWritableScope(@NotNull WritableScope scope) {
            this.scope = scope;
        }

        @Override
        public JetType visitObjectDeclaration(JetObjectDeclaration declaration, TypeInferenceContext context) {
            TopDownAnalyzer topDownAnalyzer = new TopDownAnalyzer(semanticServices, context.trace);
            topDownAnalyzer.processObject(scope, scope.getContainingDeclaration(), declaration);
            ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, declaration);
            if (classDescriptor != null) {
                PropertyDescriptor propertyDescriptor = context.classDescriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(scope.getContainingDeclaration(), declaration, classDescriptor);
                scope.addVariableDescriptor(propertyDescriptor);
            }
            return null;
        }

        @Override
        public JetType visitProperty(JetProperty property, TypeInferenceContext context) {
            JetTypeReference receiverTypeRef = property.getReceiverTypeRef();
            if (receiverTypeRef != null) {
                context.trace.getErrorHandler().genericError(receiverTypeRef.getNode(), "Local receiver-properties are not allowed");
            }

            JetPropertyAccessor getter = property.getGetter();
            if (getter != null) {
                context.trace.getErrorHandler().genericError(getter.getNode(), "Local variables are not allowed to have getters");
            }

            JetPropertyAccessor setter = property.getSetter();
            if (setter != null) {
                context.trace.getErrorHandler().genericError(setter.getNode(), "Local variables are not allowed to have setters");
            }

            VariableDescriptor propertyDescriptor = context.classDescriptorResolver.resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, property);
            JetExpression initializer = property.getInitializer();
            if (property.getPropertyTypeRef() != null && initializer != null) {
                JetType outType = propertyDescriptor.getOutType();
                JetType initializerType = getType(initializer, context.replaceExpectedType(outType).replaceScope(scope));
//                if (outType != null &&
//                    initializerType != null &&
//                    !semanticServices.getTypeChecker().isConvertibleTo(initializerType, outType)) {
//                    context.trace.getErrorHandler().typeMismatch(initializer, outType, initializerType);
//                }
            }

            scope.addVariableDescriptor(propertyDescriptor);
            return null;
        }

        @Override
        public JetType visitNamedFunction(JetNamedFunction function, TypeInferenceContext context) {
            FunctionDescriptorImpl functionDescriptor = context.classDescriptorResolver.resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function);
            scope.addFunctionDescriptor(functionDescriptor);
            context.services.checkFunctionReturnType(context.scope, function, functionDescriptor, context.dataFlowInfo);
            return null; 
        }

        @Override
        public JetType visitClass(JetClass klass, TypeInferenceContext context) {
            return super.visitClass(klass, context); // TODO
        }

        @Override
        public JetType visitTypedef(JetTypedef typedef, TypeInferenceContext context) {
            return super.visitTypedef(typedef, context); // TODO
        }

        @Override
        public JetType visitDeclaration(JetDeclaration dcl, TypeInferenceContext context) {
            return visitJetElement(dcl, context);
        }

        @Override
        protected JetType visitAssignmentOperation(JetBinaryExpression expression, TypeInferenceContext context) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            String name = assignmentOperationNames.get(operationType);

            TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(context.trace);
            JetType assignmentOperationType = getTypeForBinaryCall(scope, name, context.replaceBindingTrace(temporaryBindingTrace), expression);

            if (assignmentOperationType == null) {
                String counterpartName = binaryOperationNames.get(assignmentOperationCounterparts.get(operationType));

                JetType typeForBinaryCall = getTypeForBinaryCall(scope, counterpartName, context, expression);
                if (typeForBinaryCall != null) {
                    context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression);
                }
            }
            else {
                temporaryBindingTrace.commit();
            }
            return null;
        }

        @Override
        protected JetType visitAssignment(JetBinaryExpression expression, TypeInferenceContext context) {
            JetExpression left = expression.getLeft();
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(left);
            JetExpression right = expression.getRight();
            if (deparenthesized instanceof JetArrayAccessExpression) {
                JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) deparenthesized;
                return resolveArrayAccessToLValue(arrayAccessExpression, right, expression.getOperationReference(), context);
            }
            JetType leftType = getType(left, context.replaceExpectedType(NO_EXPECTED_TYPE).replaceScope(scope));
            if (right != null) {
                JetType rightType = getType(right, context.replaceExpectedType(leftType).replaceScope(scope));
//                if (rightType != null &&
//                    leftType != null &&
//                    !semanticServices.getTypeChecker().isConvertibleTo(rightType, leftType)) {
//                    context.trace.getErrorHandler().typeMismatch(right, leftType, rightType);
//                }
            }
            return null;
        }

        private JetType resolveArrayAccessToLValue(JetArrayAccessExpression arrayAccessExpression, JetExpression rightHandSide, JetSimpleNameExpression operationSign, TypeInferenceContext context) {
            JetType receiverType = getType(arrayAccessExpression.getArrayExpression(), context.replaceScope(scope));
            if (receiverType == null) return null;
//
            Call call = CallMaker.makeCall(arrayAccessExpression, rightHandSide);
//            // TODO : nasty hack: effort is duplicated
//            context.services.callResolver.resolveCallWithGivenName(
//                    scope,
//                    call,
//                    arrayAccessExpression,
//                    "set", arrayAccessExpression.getArrayExpression(), NO_EXPECTED_TYPE);
            FunctionDescriptor functionDescriptor = context.services.callResolver.resolveCallWithGivenName(
                    context.trace,
                    scope,
                    call,
                    arrayAccessExpression,
                    "set", receiverType, NO_EXPECTED_TYPE);
            if (functionDescriptor == null) return null;
            context.trace.record(REFERENCE_TARGET, operationSign, functionDescriptor);
            return context.services.checkType(functionDescriptor.getReturnType(), arrayAccessExpression, context);
        }

        @Override
        public JetType visitJetElement(JetElement element, TypeInferenceContext context) {
            context.trace.getErrorHandler().genericError(element.getNode(), "Unsupported element in a block: " + element + " " + element.getClass().getCanonicalName());
            return null;
        }
    }
}
