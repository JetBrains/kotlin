package org.jetbrains.jet.lang.types;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeInferrer {

    private static final Map<IElementType, String> unaryOperationNames = ImmutableMap.<IElementType, String>builder()
            .put(JetTokens.PLUSPLUS, "inc")
            .put(JetTokens.MINUSMINUS, "dec")
            .put(JetTokens.PLUS, "plus")
            .put(JetTokens.MINUS, "minus")
            .put(JetTokens.EXCL, "not")
            .build();

    private static final Map<IElementType, String> binaryOperationNames = new HashMap<IElementType, String>();
    static {
        binaryOperationNames.put(JetTokens.MUL, "times");
        binaryOperationNames.put(JetTokens.PLUS, "plus");
        binaryOperationNames.put(JetTokens.MINUS, "minus");
        binaryOperationNames.put(JetTokens.DIV, "div");
        binaryOperationNames.put(JetTokens.PERC, "mod");
        binaryOperationNames.put(JetTokens.ARROW, "arrow");
        binaryOperationNames.put(JetTokens.RANGE, "rangeTo");
    }

    private static final Set<IElementType> comparisonOperations = new HashSet<IElementType>(Arrays.asList(JetTokens.LT, JetTokens.GT, JetTokens.LTEQ, JetTokens.GTEQ));
    private static final Set<IElementType> equalsOperations = new HashSet<IElementType>(Arrays.asList(JetTokens.EQEQ, JetTokens.EXCLEQ));
    private static final Set<IElementType> inOperations = new HashSet<IElementType>(Arrays.asList(JetTokens.IN_KEYWORD, JetTokens.NOT_IN));

    public static final Map<IElementType, String> assignmentOperationNames = new HashMap<IElementType, String>();
    static {
        assignmentOperationNames.put(JetTokens.MULTEQ, "timesAssign");
        assignmentOperationNames.put(JetTokens.DIVEQ, "divAssign");
        assignmentOperationNames.put(JetTokens.PERCEQ, "modAssign");
        assignmentOperationNames.put(JetTokens.PLUSEQ, "plusAssign");
        assignmentOperationNames.put(JetTokens.MINUSEQ, "minusAssign");
    }

    private static final Map<IElementType, IElementType> assignmentOperationCounterparts = new HashMap<IElementType, IElementType>();
    static {
        assignmentOperationCounterparts.put(JetTokens.MULTEQ, JetTokens.MUL);
        assignmentOperationCounterparts.put(JetTokens.DIVEQ, JetTokens.DIV);
        assignmentOperationCounterparts.put(JetTokens.PERCEQ, JetTokens.PERC);
        assignmentOperationCounterparts.put(JetTokens.PLUSEQ, JetTokens.PLUS);
        assignmentOperationCounterparts.put(JetTokens.MINUSEQ, JetTokens.MINUS);
    }

//    private final Map<JetExpression, JetType> typeCache = new HashMap<JetExpression, JetType>();

    private final BindingTrace trace;
    private final JetSemanticServices semanticServices;
    private final TypeResolver typeResolver;
    private final ClassDescriptorResolver classDescriptorResolver;
    private final JetFlowInformationProvider flowInformationProvider;

    public JetTypeInferrer(@NotNull BindingTrace trace, @NotNull JetFlowInformationProvider flowInformationProvider, @NotNull JetSemanticServices semanticServices) {
        this.trace = trace; //new CachedBindingTrace(trace);
        this.semanticServices = semanticServices;
        this.typeResolver = new TypeResolver(semanticServices, trace, true);
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
        this.flowInformationProvider = flowInformationProvider;
    }

    @NotNull
    public JetType safeGetType(@NotNull final JetScope scope, @NotNull JetExpression expression, final boolean preferBlock) {
        JetType type = getType(scope, expression, preferBlock);
        if (type != null) {
            return type;
        }
        return ErrorUtils.createErrorType("Type for " + expression.getText());
    }

    @Nullable
    public JetType getType(@NotNull final JetScope scope, @NotNull JetExpression expression, final boolean preferBlock) {
        return new TypeInferrerVisitor(scope, preferBlock).getType(expression);
    }

    public JetType getTypeWithNamespaces(@NotNull final JetScope scope, @NotNull JetExpression expression, final boolean preferBlock) {
        return new TypeInferrerVisitorWithNamespaces(scope, preferBlock).getType(expression);
    }

    @Nullable
    private List<JetType> getTypes(JetScope scope, List<JetExpression> indexExpressions) {
        List<JetType> argumentTypes = new ArrayList<JetType>();
        TypeInferrerVisitor typeInferrerVisitor = new TypeInferrerVisitor(scope, false);
        for (JetExpression indexExpression : indexExpressions) {
            JetType type = typeInferrerVisitor.getType(indexExpression);
            if (type == null) {
                return null;
            }
            argumentTypes.add(type);
            typeInferrerVisitor.resetResult(); // TODO : recreate?
        }
        return argumentTypes;
    }

    @Nullable
    private FunctionDescriptor lookupFunction(
            @NotNull JetScope scope,
            @NotNull JetReferenceExpression reference,
            @NotNull String name,
            @NotNull JetType receiverType,
            @NotNull List<JetType> argumentTypes,
            boolean reportUnresolved) {
        OverloadDomain overloadDomain = semanticServices.getOverloadResolver().getOverloadDomain(receiverType, scope, name);
        // No generics. Guaranteed
        overloadDomain = wrapForTracing(overloadDomain, reference, null, reportUnresolved);
        OverloadResolutionResult resolutionResult = overloadDomain.getFunctionDescriptorForPositionedArguments(Collections.<JetType>emptyList(), argumentTypes);
        return resolutionResult.isSuccess() ? resolutionResult.getFunctionDescriptor() : null;
    }

    @NotNull
    private OverloadResolutionResult resolveNoParametersFunction(@NotNull JetType receiverType, @NotNull JetScope scope, @NotNull String name) {
        OverloadDomain overloadDomain = semanticServices.getOverloadResolver().getOverloadDomain(receiverType, scope, name);
        // No generics. Guaranteed
        return overloadDomain.getFunctionDescriptorForPositionedArguments(Collections.<JetType>emptyList(), Collections.<JetType>emptyList());
    }

    private OverloadDomain getOverloadDomain(
            @Nullable final JetType receiverType,
            @NotNull final JetScope scope,
            @NotNull JetExpression calleeExpression,
            @Nullable PsiElement argumentList
    ) {
        final OverloadDomain[] result = new OverloadDomain[1];
        final JetSimpleNameExpression[] reference = new JetSimpleNameExpression[1];
        calleeExpression.accept(new JetVisitor() {

            @Override
            public void visitHashQualifiedExpression(JetHashQualifiedExpression expression) {
                // a#b -- create a domain for all overloads of b in a
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public void visitPredicateExpression(JetPredicateExpression expression) {
                // overload lookup for checking, but the type is receiver's type + nullable
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public void visitQualifiedExpression(JetQualifiedExpression expression) {
                trace.getErrorHandler().genericError(expression.getNode(), "Unsupported [JetTypeInferrer]");

                // . or ?.
                JetType receiverType = getType(scope, expression.getReceiverExpression(), false);
                checkNullSafety(receiverType, expression.getOperationTokenNode());

                JetExpression selectorExpression = expression.getSelectorExpression();
                if (selectorExpression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression referenceExpression = (JetSimpleNameExpression) selectorExpression;
                    String referencedName = referenceExpression.getReferencedName();

                    if (receiverType != null && referencedName != null) {
                        // No generics. Guaranteed
                        result[0] = semanticServices.getOverloadResolver().getOverloadDomain(receiverType, scope, referencedName);
                        reference[0] = referenceExpression;
                    }
                } else {
                    throw new UnsupportedOperationException(); // TODO
                }
            }

            @Override
            public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
                // a -- create a hierarchical lookup domain for this.a
                String referencedName = expression.getReferencedName();
                if (referencedName != null) {
                    // No generics. Guaranteed
                    result[0] = semanticServices.getOverloadResolver().getOverloadDomain(receiverType, scope, referencedName);
                    reference[0] = expression;
                }
            }

            @Override
            public void visitExpression(JetExpression expression) {
                // <e> create a dummy domain for the type of e
                throw new UnsupportedOperationException(expression.getText()); // TODO
            }

            @Override
            public void visitJetElement(JetElement elem) {
                trace.getErrorHandler().genericError(elem.getNode(), "Unsupported in call element"); // TODO : Message
            }
        });
        return wrapForTracing(result[0], reference[0], argumentList, true);
    }

    private void checkNullSafety(JetType receiverType, ASTNode operationTokenNode) {
        if (receiverType != null) {
            boolean namespaceType = receiverType instanceof NamespaceType;
            boolean nullable = !namespaceType && receiverType.isNullable();
            IElementType operationSign = operationTokenNode.getElementType();
            if (nullable && operationSign == JetTokens.DOT) {
                trace.getErrorHandler().genericError(operationTokenNode, "Only safe calls (?.) are allowed on a nullable receiver of type " + receiverType);
            }
            else if (!nullable && operationSign == JetTokens.SAFE_ACCESS) {
                if (namespaceType) {
                    trace.getErrorHandler().genericError(operationTokenNode, "Safe calls are not allowed on namespaces");
                }
                else {
                    trace.getErrorHandler().genericWarning(operationTokenNode, "Unnecessary safe call on a non-null receiver of type  " + receiverType);
                }
            }
        }
    }

    private OverloadDomain wrapForTracing(
            @NotNull final OverloadDomain overloadDomain,
            @NotNull final JetReferenceExpression referenceExpression,
            @Nullable final PsiElement argumentList,
            final boolean reportErrors) {
        return new OverloadDomain() {
            @NotNull
            @Override
            public OverloadResolutionResult getFunctionDescriptorForNamedArguments(@NotNull List<JetType> typeArguments, @NotNull Map<String, JetType> valueArgumentTypes, @Nullable JetType functionLiteralArgumentType) {
                OverloadResolutionResult resolutionResult = overloadDomain.getFunctionDescriptorForNamedArguments(typeArguments, valueArgumentTypes, functionLiteralArgumentType);
                report(resolutionResult);
                return resolutionResult;
            }

            @NotNull
            @Override
            public OverloadResolutionResult getFunctionDescriptorForPositionedArguments(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
                OverloadResolutionResult resolutionResult = overloadDomain.getFunctionDescriptorForPositionedArguments(typeArguments, positionedValueArgumentTypes);
                report(resolutionResult);
                return resolutionResult;
            }

            private void report(OverloadResolutionResult resolutionResult) {
                if (resolutionResult.isSuccess() || resolutionResult.singleFunction()) {
                    trace.recordReferenceResolution(referenceExpression, resolutionResult.getFunctionDescriptor());
                }
                if (reportErrors) {
                    switch (resolutionResult.getResultCode()) {
                        case NAME_NOT_FOUND:
                            trace.getErrorHandler().unresolvedReference(referenceExpression);
                            break;
                        case SINGLE_FUNCTION_ARGUMENT_MISMATCH:
                            if (argumentList != null) {
                                // TODO : More helpful message. NOTE: there's a separate handling for this for constructors
                                trace.getErrorHandler().genericError(argumentList.getNode(), "Arguments do not match " + DescriptorRenderer.TEXT.render(resolutionResult.getFunctionDescriptor()));
                            }
                            else {
                                trace.getErrorHandler().unresolvedReference(referenceExpression);
                            }
                            break;
                        case AMBIGUITY:
                            if (argumentList != null) {
                                // TODO : More helpful message. NOTE: there's a separate handling for this for constructors
                                trace.getErrorHandler().genericError(argumentList.getNode(), "Overload ambiguity [TODO : more helpful message]");
                            }
                            else {
                                trace.getErrorHandler().unresolvedReference(referenceExpression);
                            }
                            break;
                        default:
                            // Not a success
                    }
                }
            }

            @Override
            public boolean isEmpty() {
                return overloadDomain.isEmpty();
            }
        };
    }

    @NotNull
    public JetType getFunctionReturnType(@NotNull JetScope outerScope, JetDeclarationWithBody function, FunctionDescriptor functionDescriptor) {
        Map<JetElement, JetType> typeMap = collectReturnedExpressions(outerScope, function, functionDescriptor);
        Collection<JetType> types = typeMap.values();
        return types.isEmpty() ? JetStandardClasses.getNothingType() : semanticServices.getTypeChecker().commonSupertype(types);
    }

//    private JetType getCachedType(@NotNull JetExpression expression) {
////        assert typeCache.containsKey(expression) : "No type cached for " + expression.getText();
//        return typeCache.get(expression);
//    }

    public void checkFunctionReturnType(@NotNull JetScope outerScope, @NotNull JetDeclarationWithBody function, @NotNull FunctionDescriptor functionDescriptor) {
        Map<JetElement, JetType> typeMap = collectReturnedExpressions(outerScope, function, functionDescriptor);
        if (typeMap.isEmpty()) {
            return; // The function returns Nothing
        }
        JetType expectedReturnType = functionDescriptor.getUnsubstitutedReturnType();
        for (Map.Entry<JetElement, JetType> entry : typeMap.entrySet()) {
            JetType actualType = entry.getValue();
            JetElement element = entry.getKey();
            JetTypeChecker typeChecker = semanticServices.getTypeChecker();
            if (!typeChecker.isSubtypeOf(actualType, expectedReturnType)) {
                if (typeChecker.isConvertibleBySpecialConversion(actualType, expectedReturnType)) {
                    if (expectedReturnType.getConstructor().equals(JetStandardClasses.getUnitType().getConstructor())
                        && element.getParent() instanceof JetReturnExpression) {
                        trace.getErrorHandler().genericError(element.getNode(), "This function must return a value of type Unit");
                    }
                }
                else {
                    if (element == function) {
                        JetExpression bodyExpression = function.getBodyExpression();
                        assert bodyExpression != null;
                        trace.getErrorHandler().genericError(bodyExpression.getNode(), "This function must return a value of type " + expectedReturnType);
                    }
                    else if (element instanceof JetExpression) {
                        JetExpression expression = (JetExpression) element;
                        trace.getErrorHandler().typeMismatch(expression, expectedReturnType, actualType);
                    }
                    else {
                        trace.getErrorHandler().genericError(element.getNode(), "This function must return a value of type " + expectedReturnType);
                    }
                }
            }
        }
    }

    private Map<JetElement, JetType> collectReturnedExpressions(JetScope outerScope, JetDeclarationWithBody function, FunctionDescriptor functionDescriptor) {
        JetExpression bodyExpression = function.getBodyExpression();
        assert bodyExpression != null;
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace);
        getType(functionInnerScope, bodyExpression, function.hasBlockBody());
        Collection<JetExpression> returnedExpressions = new ArrayList<JetExpression>();
        Collection<JetElement> elementsReturningUnit = new ArrayList<JetElement>();
        flowInformationProvider.collectReturnedInformation(function.asElement(), returnedExpressions, elementsReturningUnit);
        Map<JetElement,JetType> typeMap = new HashMap<JetElement, JetType>();
        for (JetExpression returnedExpression : returnedExpressions) {
            JetType cachedType = trace.getBindingContext().getExpressionType(returnedExpression);// getCachedType(returnedExpression);
            trace.removeStatementRecord(returnedExpression);
            if (cachedType != null) {
                typeMap.put(returnedExpression, cachedType);
            }
        }
        for (JetElement jetElement : elementsReturningUnit) {
            typeMap.put(jetElement, JetStandardClasses.getUnitType());
        }
        return typeMap;
    }

    @Nullable
    private JetType getBlockReturnedType(@NotNull JetScope outerScope, @NotNull List<JetElement> block) {
        if (block.isEmpty()) {
            return JetStandardClasses.getUnitType();
        }

        DeclarationDescriptor containingDescriptor = outerScope.getContainingDeclaration();
        WritableScope scope = new WritableScopeImpl(outerScope, containingDescriptor, trace.getErrorHandler());
        return getBlockReturnedTypeWithWritableScope(scope, block);
    }

    private JetType getBlockReturnedTypeWithWritableScope(@NotNull WritableScope scope, @NotNull List<? extends JetElement> block) {
        if (block.isEmpty()) {
            return JetStandardClasses.getUnitType();
        }

        TypeInferrerVisitorWithWritableScope blockLevelVisitor = new TypeInferrerVisitorWithWritableScope(scope, true);

        JetType result = null;
        for (JetElement statement : block) {
            trace.recordStatement(statement);
            JetExpression statementExpression = (JetExpression) statement;
            result = blockLevelVisitor.getType(statementExpression);
            blockLevelVisitor.resetResult(); // TODO : maybe it's better to recreate the visitors with the same scope?
        }
        return result;
    }

    private void collectAllReturnTypes(JetWhenExpression whenExpression, JetScope scope, List<JetType> result) {
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetWhenExpression subWhen = entry.getSubWhen();
            if (subWhen != null) {
                collectAllReturnTypes(subWhen, scope, result);
            } else {
                JetExpression resultExpression = entry.getExpression();
                if (resultExpression != null) {
                    JetType type = getType(scope, resultExpression, true);
                    if (type != null) {
                        result.add(type);
                    }
                }
            }
        }
    }

    @Nullable
    private JetType resolveCall(
            @NotNull JetScope scope,
            @NotNull OverloadDomain overloadDomain,
            @NotNull JetCall call) {
        // 1) ends with a name -> (scope, name) to look up
        // 2) ends with something else -> just check types

        final List<JetTypeProjection> jetTypeArguments = call.getTypeArguments();

        for (JetTypeProjection typeArgument : jetTypeArguments) {
            if (typeArgument.getProjectionKind() != JetProjectionKind.NONE) {
                trace.getErrorHandler().genericError(typeArgument.getNode(), "Projections are not allowed on type parameters for methods"); // TODO : better positioning
            }
        }

        final List<JetArgument> valueArguments = call.getValueArguments();

        boolean someNamed = false;
        for (JetArgument argument : valueArguments) {
            if (argument.isNamed()) {
                someNamed = true;
                break;
            }
        }

        final List<JetExpression> functionLiteralArguments = call.getFunctionLiteralArguments();

        // TODO : must be a check
        assert functionLiteralArguments.size() <= 1;

        if (someNamed) {
            // TODO : check that all are named
            throw new UnsupportedOperationException(); // TODO

//                    result = overloadDomain.getFunctionDescriptorForNamedArguments(typeArguments, valueArguments, functionLiteralArgument);
        } else {
            List<JetType> typeArguments = new ArrayList<JetType>();
            for (JetTypeProjection projection : jetTypeArguments) {
                // TODO : check that there's no projection
                JetTypeReference typeReference = projection.getTypeReference();
                if (typeReference != null) {
                    typeArguments.add(typeResolver.resolveType(scope, typeReference));
                }
            }

            List<JetExpression> positionedValueArguments = new ArrayList<JetExpression>();
            for (JetArgument argument : valueArguments) {
                JetExpression argumentExpression = argument.getArgumentExpression();
                if (argumentExpression != null) {
                    positionedValueArguments.add(argumentExpression);
                }
            }

            positionedValueArguments.addAll(functionLiteralArguments);

            List<JetType> valueArgumentTypes = new ArrayList<JetType>();
            for (JetExpression valueArgument : positionedValueArguments) {
                valueArgumentTypes.add(safeGetType(scope, valueArgument, false));
            }

            OverloadResolutionResult resolutionResult = overloadDomain.getFunctionDescriptorForPositionedArguments(typeArguments, valueArgumentTypes);
            if (resolutionResult.isSuccess()) {
                final FunctionDescriptor functionDescriptor = resolutionResult.getFunctionDescriptor();

                List<TypeParameterDescriptor> typeParameters = functionDescriptor.getOriginal().getTypeParameters();
                for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
                    TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
                    final JetType typeArgument = typeArguments.get(i);
                    if (!semanticServices.getTypeChecker().isSubtypeOf(typeArgument, typeParameterDescriptor.getBoundsAsType())) {
                        trace.getErrorHandler().genericError(jetTypeArguments.get(i).getNode(), "Bound of the type parameter " + DescriptorRenderer.TEXT.render(typeParameterDescriptor) + " is not respected by the type " + typeArgument);
                    }

                }
                return functionDescriptor.getUnsubstitutedReturnType();
            }
        }
        return null;
    }

    @Nullable
    public JetType checkConstructorCall(JetScope scope, @NotNull JetTypeReference typeReference, @NotNull JetCall call) {
        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement instanceof JetUserType) {
            JetUserType userType = (JetUserType) typeElement;
            // TODO : to infer constructor parameters, one will need to
            //  1) resolve a _class_ from the typeReference
            //  2) rely on the overload domain of constructors of this class to infer type arguments
            // For now we assume that the type arguments are provided, and thus the typeReference can be
            // resolved into a valid type
            JetType receiverType = typeResolver.resolveType(scope, typeReference);
            DeclarationDescriptor declarationDescriptor = receiverType.getConstructor().getDeclarationDescriptor();
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;

                for (JetTypeProjection typeProjection : userType.getTypeArguments()) {
                    switch (typeProjection.getProjectionKind()) {
                        case IN:
                        case OUT:
                        case STAR:
                            // TODO : Bug in the editor
                            trace.getErrorHandler().genericError(typeProjection.getProjectionNode(), "Projections are not allowed in constructor type arguments");
                            break;
                        case NONE:
                            break;
                    }
                }

                JetSimpleNameExpression referenceExpression = userType.getReferenceExpression();
                if (referenceExpression != null) {
                    return checkClassConstructorCall(scope, referenceExpression, classDescriptor, receiverType, call);
                }
            }
            else {
                trace.getErrorHandler().genericError(((JetElement) call).getNode(), "Calling a constructor is only supported for ordinary classes"); // TODO : review the message
            }
            return null;
        }
        else {
            if (typeElement != null) {
                trace.getErrorHandler().genericError(typeElement.getNode(), "Calling a constructor is only supported for ordinary classes"); // TODO : Better message
            }
        }
        return null;
    }

    @Nullable
    public JetType checkClassConstructorCall(
            @NotNull JetScope scope,
            @NotNull JetReferenceExpression referenceExpression,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetType receiverType,
            @NotNull JetCall call) {
        // When one writes 'new Array<in T>(...)' this does not make much sense, and an instance
        // of 'Array<T>' must be created anyway.
        // Thus, we should either prohibit projections in type arguments in such contexts,
        // or treat them as an automatic upcast to the desired type, i.e. for the user not
        // to be forced to write
        //   val a : Array<in T> = new Array<T>(...)
        // NOTE: Array may be a bad example here, some classes may have substantial functionality
        //       not involving their type parameters
        //
        // The code below upcasts the type automatically

        List<TypeProjection> typeArguments = receiverType.getArguments();

        List<TypeProjection> projectionsStripped = new ArrayList<TypeProjection>();
        for (TypeProjection typeArgument : typeArguments) {
            if (typeArgument.getProjectionKind() != Variance.INVARIANT) {
                projectionsStripped.add(new TypeProjection(typeArgument.getType()));
            }
            else
                projectionsStripped.add(typeArgument);
        }

        FunctionGroup constructors = classDescriptor.getConstructors(projectionsStripped);
        OverloadDomain constructorsOverloadDomain = semanticServices.getOverloadResolver().getOverloadDomain(null, constructors);
        JetType constructorReturnedType = resolveCall(
                scope,
                wrapForTracing(constructorsOverloadDomain, referenceExpression, call.getValueArgumentList(), false),
                call);
        if (constructorReturnedType == null && !ErrorUtils.isErrorType(receiverType)) {
            DeclarationDescriptor declarationDescriptor = receiverType.getConstructor().getDeclarationDescriptor();
            assert declarationDescriptor != null;
            trace.recordReferenceResolution(referenceExpression, declarationDescriptor);
            // TODO : more helpful message
            JetArgumentList argumentList = call.getValueArgumentList();
            if (argumentList != null) {
                trace.getErrorHandler().genericError(argumentList.getNode(), "Cannot find an overload for these arguments");
            }
            else {
                trace.getErrorHandler().genericError(call.asElement().getNode(), "Cannot find an overload for these arguments");
            }
            constructorReturnedType = receiverType;
        }
        // If no upcast needed:
        return constructorReturnedType;

        // Automatic upcast:
//                            result = receiverType;
    }


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class TypeInferrerVisitor extends JetVisitor {
        protected final JetScope scope;
        private final boolean preferBlock;

        protected JetType result;

        private TypeInferrerVisitor(@NotNull JetScope scope, boolean preferBlock) {
            this.scope = scope;
            this.preferBlock = preferBlock;
        }

        @Nullable
        public JetType getType(@NotNull final JetScope scope, @NotNull JetExpression expression, final boolean preferBlock) {
            TypeInferrerVisitor visitor;
            if (this.scope == scope && this.preferBlock == preferBlock && result == null) {
                visitor = this;
            }
            else {
                visitor = createNew(scope, preferBlock);
            }
            JetType type = visitor.getType(expression);
            visitor.result = null;
            return type;
        }

        @NotNull
        public TypeInferrerVisitor createNew(JetScope scope, boolean preferBlock) {
            return new TypeInferrerVisitor(scope, preferBlock);
        }

        @Nullable
        public final JetType getType(@NotNull JetExpression expression) {
            assert result == null;
            if (trace.isProcessed(expression)) {
                return trace.getBindingContext().getExpressionType(expression);
            }
            try {
                expression.accept(this);
                if (result instanceof DeferredType) {
                    result = ((DeferredType) result).getActualType();
                }
                if (result != null) {
                    trace.recordExpressionType(expression, result);
                    if (JetStandardClasses.isNothing(result) && !result.isNullable()) {
                        markDominatedExpressionsAsUnreachable(expression);
                    }
                }
            }
            catch (ReenteringLazyValueComputationException e) {
                trace.getErrorHandler().genericError(expression.getNode(), "Type inference has run into a recursive problem"); // TODO : message
                result = null;
            }

            trace.markAsProcessed(expression);
            return result;
        }

        public void resetResult() {
            result = null;
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private void markDominatedExpressionsAsUnreachable(JetExpression expression) {
            List<JetElement> dominated = new ArrayList<JetElement>();
            flowInformationProvider.collectDominatedExpressions(expression, dominated);
            Set<JetElement> rootExpressions = JetPsiUtil.findRootExpressions(dominated);
            for (JetElement rootExpression : rootExpressions) {
                trace.getErrorHandler().genericError(rootExpression.getNode(),
                        "This code is unreachable, because '" + expression.getText() + "' never terminates normally");
            }
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
            // TODO : other members
            // TODO : type substitutions???
            String referencedName = expression.getReferencedName();
            if (expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER
                    && referencedName != null) {
                PropertyDescriptor property = scope.getPropertyByFieldReference(referencedName);
                if (property == null) {
                    trace.getErrorHandler().unresolvedReference(expression);
                }
                else {
                    trace.recordReferenceResolution(expression, property);
                    result = property.getOutType();
                }
            }
            else {
                assert  expression.getReferencedNameElementType() == JetTokens.IDENTIFIER;
                if (referencedName != null) {
                    VariableDescriptor variable = scope.getVariable(referencedName);
                    if (variable != null) {
                        trace.recordReferenceResolution(expression, variable);
                        result = variable.getOutType();
                        if (result == null) {
                            trace.getErrorHandler().genericError(expression.getNode(), "This variable is not readable in this context");
                        }
                        return;
                    } else if (furtherNameLookup(expression, referencedName)) {
                        return;
                    }
                    trace.getErrorHandler().unresolvedReference(expression);
                }
            }
        }

        protected boolean furtherNameLookup(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName) {
            NamespaceType namespaceType = lookupNamespaceType(expression, referencedName);
            if (namespaceType != null) {
                trace.getErrorHandler().genericError(expression.getNode(), "Expression expected, but a namespace name found");
                return true;
            }
            return false;
        }

        @Nullable
        protected NamespaceType lookupNamespaceType(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName) {
            NamespaceDescriptor namespace = scope.getNamespace(referencedName);
            if (namespace == null) {
                return null;
            }
            trace.recordReferenceResolution(expression, namespace);
            return namespace.getNamespaceType();
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            if (preferBlock && !expression.hasParameterSpecification()) {
                trace.recordBlock(expression);
                result = getBlockReturnedType(scope, expression.getBody());
                return;
            }

            FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(scope.getContainingDeclaration(), Collections.<Annotation>emptyList(), "<anonymous>");

            JetTypeReference returnTypeRef = expression.getReturnTypeRef();

            JetTypeReference receiverTypeRef = expression.getReceiverTypeRef();
            final JetType receiverType;
            if (receiverTypeRef != null) {
                receiverType = typeResolver.resolveType(scope, receiverTypeRef);
            } else {
                receiverType = scope.getThisType();
            }

            List<JetElement> body = expression.getBody();
            final Map<String, VariableDescriptor> parameterDescriptors = new HashMap<String, VariableDescriptor>();
            List<JetType> parameterTypes = new ArrayList<JetType>();
            for (JetParameter parameter : expression.getParameters()) {
                JetTypeReference typeReference = parameter.getTypeReference();
                if (typeReference == null) {
                    throw new UnsupportedOperationException("Type inference for parameters is not implemented yet");
                }
                VariableDescriptor variableDescriptor = classDescriptorResolver.resolveLocalVariableDescriptor(functionDescriptor, scope, parameter);
                parameterDescriptors.put(parameter.getName(), variableDescriptor);
                parameterTypes.add(variableDescriptor.getOutType());
            }
            JetType returnType;
            if (returnTypeRef != null) {
                returnType = typeResolver.resolveType(scope, returnTypeRef);
            } else {
                WritableScope writableScope = new WritableScopeImpl(scope, functionDescriptor, trace.getErrorHandler());
                for (VariableDescriptor variableDescriptor : parameterDescriptors.values()) {
                    writableScope.addVariableDescriptor(variableDescriptor);
                }
                writableScope.setThisType(receiverType);
                returnType = getBlockReturnedType(writableScope, body);
            }
            JetType effectiveReceiverType = receiverTypeRef == null ? null : receiverType;
            JetType safeReturnType = returnType == null ? ErrorUtils.createErrorType("<return type>") : returnType;
            result = JetStandardClasses.getFunctionType(Collections.<Annotation>emptyList(), effectiveReceiverType, parameterTypes, safeReturnType);
        }

        @Override
        public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
            JetExpression inner = expression.getExpression();
            if (inner != null) {
                result = getType(scope, inner, false);
            }
        }

        @Override
        public void visitConstantExpression(JetConstantExpression expression) {
            IElementType elementType = expression.getNode().getElementType();
            JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
            if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
                Object value = expression.getValue();
                if (value == null) {
                    trace.getErrorHandler().genericError(expression.getNode(), "Number is of range for Long");
                }
                else if (value instanceof Long) {
                    result = standardLibrary.getLongType();
                }
                else {
                    result = standardLibrary.getIntType();
                }
                // TODO : other ranges
            }
            else if (elementType == JetNodeTypes.LONG_CONSTANT) {
                result = standardLibrary.getLongType();
            }
            else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
                String text = expression.getText();
                assert text.length() > 0;
                char lastChar = text.charAt(text.length() - 1);
                if (lastChar == 'f' || lastChar == 'F') {
                    result = standardLibrary.getFloatType();
                }
                else {
                    result = standardLibrary.getDoubleType();
                }
            }
            else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
                result = standardLibrary.getBooleanType();
            }
            else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
                result = standardLibrary.getCharType();
            }
            else if (elementType == JetNodeTypes.STRING_CONSTANT) {
                result = standardLibrary.getStringType();
            }
            else if (elementType == JetNodeTypes.NULL) {
                result = JetStandardClasses.getNullableNothingType();
            }
            else {
                throw new IllegalArgumentException("Unsupported constant: " + expression);
            }
        }

        @Override
        public void visitThrowExpression(JetThrowExpression expression) {
            JetExpression thrownExpression = expression.getThrownExpression();
            if (thrownExpression != null) {
                JetType type = getType(scope, thrownExpression, false);
                // TODO : check that it inherits Throwable
            }
            result = JetStandardClasses.getNothingType();
        }

        @Override
        public void visitReturnExpression(JetReturnExpression expression) {
            JetExpression returnedExpression = expression.getReturnedExpression();

            if (returnedExpression != null) {
                getType(scope, returnedExpression, false);
            }

            result = JetStandardClasses.getNothingType();
        }

        @Override
        public void visitBreakExpression(JetBreakExpression expression) {
            result = JetStandardClasses.getNothingType();
        }

        @Override
        public void visitContinueExpression(JetContinueExpression expression) {
            result = JetStandardClasses.getNothingType();
        }

        @Override
        public void visitTypeofExpression(JetTypeofExpression expression) {
            JetType type = safeGetType(scope, expression.getBaseExpression(), false);
            result = semanticServices.getStandardLibrary().getTypeInfoType(type);
        }

        @Override
        public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
            IElementType operationType = expression.getOperationSign().getReferencedNameElementType();
            JetType actualType = getType(scope, expression.getLeft(), false);
            JetTypeReference right = expression.getRight();
            if (right != null) {
                JetType targetType = typeResolver.resolveType(scope, right);
                if (operationType == JetTokens.COLON) {
                    if (actualType != null && !semanticServices.getTypeChecker().isSubtypeOf(actualType, targetType)) {
                        trace.getErrorHandler().typeMismatch(expression.getLeft(), targetType, actualType);
                    }
                    result = targetType;
                }
                else if (operationType == JetTokens.AS_KEYWORD) {
                    checkForCastImpossibility(expression, actualType, targetType);
                    result = targetType;
                }
                else if (operationType == JetTokens.AS_SAFE) {
                    checkForCastImpossibility(expression, actualType, targetType);
                    result = TypeUtils.makeNullable(targetType);
                }
                else {
                    trace.getErrorHandler().genericError(expression.getOperationSign().getNode(), "Unsupported binary operation");
                }
            }
        }

        private void checkForCastImpossibility(JetBinaryExpressionWithTypeRHS expression, JetType actualType, JetType targetType) {
            if (actualType == null) return;

            JetTypeChecker typeChecker = semanticServices.getTypeChecker();
            if (!typeChecker.isSubtypeOf(targetType, actualType)) {
                if (typeChecker.isSubtypeOf(actualType, targetType)) {
                    trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "No cast needed, use ':' instead");
                }
                else {
                    trace.getErrorHandler().genericError(expression.getOperationSign().getNode(), "This cast can never succeed");
                }
            }
            else {
                if (typeChecker.isSubtypeOf(actualType, targetType)) {
                    trace.getErrorHandler().genericWarning(expression.getOperationSign().getNode(), "No cast needed");
                }
            }
        }

        @Override
        public void visitTupleExpression(JetTupleExpression expression) {
            List<JetExpression> entries = expression.getEntries();
            List<JetType> types = new ArrayList<JetType>();
            for (JetExpression entry : entries) {
                types.add(safeGetType(scope, entry, false));
            }
            // TODO : labels
            result = JetStandardClasses.getTupleType(types);
        }

        @Override
        public void visitThisExpression(JetThisExpression expression) {
            // TODO : qualified this, e.g. this@Foo<Bar>
            JetType thisType = null;
            String labelName = expression.getLabelName();
            if (labelName != null) {
                Collection<DeclarationDescriptor> declarationsByLabel = scope.getDeclarationsByLabel(labelName);
                int size = declarationsByLabel.size();
                if (size == 1) {
                    DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
                    if (declarationDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                        thisType = classDescriptor.getDefaultType();
                    }
                    else {
                        throw new UnsupportedOperationException(); // TODO
                    }
                }
                else if (size == 0) {
                    trace.getErrorHandler().unresolvedReference(expression.getTargetLabel());
                }
                else {
                    JetSimpleNameExpression labelElement = expression.getTargetLabel();
                    assert labelElement != null;
                    trace.getErrorHandler().genericError(labelElement.getNode(), "Ambiguous label");
                }
            }
            else {
                thisType = scope.getThisType();
            }

            if (thisType != null) {
                if (JetStandardClasses.isNothing(thisType)) {
                    trace.getErrorHandler().genericError(expression.getNode(), "'this' is not defined in this context");
                }
                else {
                    JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
                    if (superTypeQualifier != null) {
                        JetTypeElement superTypeElement = superTypeQualifier.getTypeElement();
                        // Errors are reported by the parser
                        if (superTypeElement instanceof JetUserType) {
                            JetUserType typeElement = (JetUserType) superTypeElement;

                            ClassifierDescriptor classifierCandidate = typeResolver.resolveClass(scope, typeElement);
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
                                    trace.getErrorHandler().genericError(superTypeElement.getNode(), "Not a superclass");
                                }
                            }
                        }
                    } else {
                        result = thisType;
                    }
                }
            }
        }

        @Override
        public void visitBlockExpression(JetBlockExpression expression) {
            result = getBlockReturnedType(scope, expression.getStatements());
        }

        @Override
        public void visitWhenExpression(final JetWhenExpression expression) {
            // TODO :change scope according to the bound value in the when header
            final JetExpression subjectExpression = expression.getSubjectExpression();

            JetType subjectType = null;
            if (subjectExpression != null) {
                subjectType = getType(scope, subjectExpression, false);
            }

            // TODO : exhaustive patterns

            for (JetWhenEntry whenEntry : expression.getEntries()) {
                final JetType finalSubjectType = subjectType;
                JetWhenCondition condition = whenEntry.getCondition();
                if (condition != null) {
                    condition.accept(new JetVisitor() {
                        @Override
                        public void visitWhenConditionWithExpression(JetWhenConditionWithExpression condition) {
                            JetExpression conditionExpression = condition.getExpression();
                            if (conditionExpression != null) {
                                JetType type = getType(scope, conditionExpression, false);
                                if (type != null && finalSubjectType != null) {
                                    if (TypeUtils.intersect(semanticServices.getTypeChecker(), Sets.newHashSet(finalSubjectType, type)) == null) {
                                        trace.getErrorHandler().genericError(conditionExpression.getNode(), "This condition can never hold");
                                    }
                                }
                            }
                        }

                        @Override
                        public void visitWhenConditionCall(JetWhenConditionCall condition) {
                            checkNullSafety(finalSubjectType, condition.getOperationTokenNode());
                            JetExpression callSuffixExpression = condition.getCallSuffixExpression();
                            JetScope compositeScope = new ScopeWithReceiver(scope, finalSubjectType, semanticServices.getTypeChecker());
                            if (callSuffixExpression != null) {
                                JetType selectorReturnType = getType(compositeScope, callSuffixExpression, false);
                                ensureBooleanResultWithCustomSubject(callSuffixExpression, selectorReturnType, "This expression");
                            }
                        }

                        @Override
                        public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                            JetExpression rangeExpression = condition.getRangeExpression();
                            if (rangeExpression != null) {
                                checkInExpression(condition.getOperationReference(), subjectExpression, rangeExpression);
                            }
                        }

                        @Override
                        public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                            JetPattern pattern = condition.getPattern();
                            if (pattern != null) {
                                checkPatternType(pattern, finalSubjectType);
                            }
                        }

                        @Override
                        public void visitJetElement(JetElement elem) {
                            trace.getErrorHandler().genericError(elem.getNode(), "Unsupported [JetTypeInferrer] : " + elem);
                        }
                    });
                }
            }

            List<JetType> expressionTypes = new ArrayList<JetType>();
            collectAllReturnTypes(expression, scope, expressionTypes);
            if (!expressionTypes.isEmpty()) {
                result = semanticServices.getTypeChecker().commonSupertype(expressionTypes);
            }
            else {
                trace.getErrorHandler().genericError(expression.getNode(), "Entries required for when-expression"); // TODO : Scope, and maybe this should not an error
            }
        }

        private void checkPatternType(@NotNull JetPattern pattern, @NotNull final JetType subjectType) {
            pattern.accept(new JetVisitor() {
                @Override
                public void visitTypePattern(JetTypePattern typePattern) {
                    JetTypeReference typeReference = typePattern.getTypeReference();
                    if (typeReference != null) {
                        JetType type = typeResolver.resolveType(scope, typeReference);
                        checkTypeCompatibility(type, subjectType, typePattern);
                    }
                }

                @Override
                public void visitWildcardPattern(JetWildcardPattern pattern) {
                    // Nothing
                }

                @Override
                public void visitExpressionPattern(JetExpressionPattern pattern) {
                    JetType type = getType(scope, pattern.getExpression(), false);
                    checkTypeCompatibility(type, subjectType, pattern);
                }

                @Override
                public void visitTuplePattern(JetTuplePattern pattern) {
                    List<JetTuplePatternEntry> entries = pattern.getEntries();
                    TypeConstructor typeConstructor = subjectType.getConstructor();
                    if (!JetStandardClasses.getTuple(entries.size()).getTypeConstructor().equals(typeConstructor)
                            || typeConstructor.getParameters().size() != entries.size()) {
                        trace.getErrorHandler().genericError(pattern.getNode(), "Type mismatch: subject is of type " + subjectType + " but the pattern is of type Tuple" + entries.size()); // TODO : message
                    }
                    else {
                        for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
                            JetTuplePatternEntry entry = entries.get(i);
                            JetType type = subjectType.getArguments().get(i).getType();

                            // TODO : is a name always allowed, ie for tuple patterns, not decomposer arg lists?
                            ASTNode nameLabelNode = entry.getNameLabelNode();
                            if (nameLabelNode != null) {
                                trace.getErrorHandler().genericError(nameLabelNode, "Unsupported [JetTypeInferrer]");
                            }

                            JetPattern entryPattern = entry.getPattern();
                            if (entryPattern != null) {
                                checkPatternType(entryPattern, type);
                            }
                        }
                    }
                }

                @Override
                public void visitDecomposerPattern(JetDecomposerPattern pattern) {
                    JetType selectorReturnType = getSelectorReturnType(subjectType, pattern.getDecomposerExpression());

                    checkPatternType(pattern.getArgumentList(), selectorReturnType == null ? ErrorUtils.createErrorType("No type") : selectorReturnType);
                }

                private void checkTypeCompatibility(@Nullable JetType type, @NotNull JetType subjectType, @NotNull JetElement reportErrorOn) {
                    if (type == null) {
                        return;
                    }
                    if (TypeUtils.intersect(semanticServices.getTypeChecker(), Sets.newHashSet(type, subjectType)) == null) {
                        trace.getErrorHandler().genericError(reportErrorOn.getNode(), "Incompatible types: " + type + " and " + subjectType); // TODO : message
                    }
                }

                @Override
                public void visitJetElement(JetElement elem) {
                    trace.getErrorHandler().genericError(elem.getNode(), "Unsupported [JetTypeInferrer]");
                }
            });
        }

        @Override
        public void visitTryExpression(JetTryExpression expression) {
            JetExpression tryBlock = expression.getTryBlock();
            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            JetFinallySection finallyBlock = expression.getFinallyBlock();
            List<JetType> types = new ArrayList<JetType>();
            for (JetCatchClause catchClause : catchClauses) {
                JetParameter catchParameter = catchClause.getCatchParameter();
                JetExpression catchBody = catchClause.getCatchBody();
                if (catchParameter != null) {
                    VariableDescriptor variableDescriptor = classDescriptorResolver.resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, catchParameter);
                    if (catchBody != null) {
                        WritableScope catchScope = new WritableScopeImpl(scope, scope.getContainingDeclaration(), trace.getErrorHandler());
                        catchScope.addVariableDescriptor(variableDescriptor);
                        JetType type = getType(catchScope, catchBody, true);
                        if (type != null) {
                            types.add(type);
                        }
                    }
                }
            }
            if (finallyBlock != null) {
                types.clear(); // Do not need the list for the check, but need the code above to typecheck catch bodies
                JetType type = getType(scope, finallyBlock.getFinalExpression(), true);
                if (type != null) {
                    types.add(type);
                }
            }
            JetType type = getType(scope, tryBlock, true);
            if (type != null) {
                types.add(type);
            }
            if (types.isEmpty()) {
                result = null;
            }
            else {
                result = semanticServices.getTypeChecker().commonSupertype(types);
            }
        }

        @Override
        public void visitIfExpression(JetIfExpression expression) {
            checkCondition(scope, expression.getCondition());

            // TODO : change types according to is and null checks
            JetExpression elseBranch = expression.getElse();
            JetExpression thenBranch = expression.getThen();

            if (elseBranch == null) {
                if (thenBranch != null) {
                    getType(scope, thenBranch, true);
                    result = JetStandardClasses.getUnitType();
                }
            }
            else if (thenBranch == null) {
                getType(scope, elseBranch, true);
                result = JetStandardClasses.getUnitType();
            }
            else {
                JetType thenType = getType(scope, thenBranch, true);
                JetType elseType = getType(scope, elseBranch, true);
                if (thenType == null) {
                    result = elseType;
                }
                else if (elseType == null) {
                    result = thenType;
                }
                else {
                    result = semanticServices.getTypeChecker().commonSupertype(Arrays.asList(thenType, elseType));
                }
            }
        }

        private void checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition) {
            if (condition != null) {
                JetType conditionType = getType(scope, condition, false);

                if (conditionType != null && !isBoolean(conditionType)) {
                    trace.getErrorHandler().genericError(condition.getNode(), "Condition must be of type Boolean, but was of type " + conditionType);
                }
            }
        }

        @Override
        public void visitWhileExpression(JetWhileExpression expression) {
            checkCondition(scope, expression.getCondition());
            JetExpression body = expression.getBody();
            if (body != null) {
                getType(scope, body, true);
            }
            result = JetStandardClasses.getUnitType();
        }

        @Override
        public void visitDoWhileExpression(JetDoWhileExpression expression) {
            JetExpression body = expression.getBody();
            JetScope conditionScope = scope;
            if (body instanceof JetFunctionLiteralExpression) {
                JetFunctionLiteralExpression function = (JetFunctionLiteralExpression) body;
                if (!function.hasParameterSpecification()) {
                    WritableScope writableScope = new WritableScopeImpl(scope, scope.getContainingDeclaration(), trace.getErrorHandler());
                    conditionScope = writableScope;
                    getBlockReturnedTypeWithWritableScope(writableScope, function.getBody());
                    trace.recordBlock(function);
                } else {
                    getType(scope, body, true);
                }
            }
            else if (body != null) {
                WritableScope writableScope = new WritableScopeImpl(scope, scope.getContainingDeclaration(), trace.getErrorHandler());
                conditionScope = writableScope;
                getBlockReturnedTypeWithWritableScope(writableScope, Collections.singletonList(body));
            }
            checkCondition(conditionScope, expression.getCondition());
            result = JetStandardClasses.getUnitType();
        }

        @Override
        public void visitForExpression(JetForExpression expression) {
            JetParameter loopParameter = expression.getLoopParameter();
            JetExpression loopRange = expression.getLoopRange();
            JetType loopRangeType = null;
            if (loopRange != null) {
                loopRangeType = getType(scope, loopRange, false);
            }
            JetType expectedParameterType = null;
            if (loopRangeType != null) {
                expectedParameterType = checkIterableConvention(loopRangeType, loopRange.getNode());
            }

            WritableScope loopScope = new WritableScopeImpl(scope, scope.getContainingDeclaration(), trace.getErrorHandler());

            if (loopParameter != null) {
                JetTypeReference typeReference = loopParameter.getTypeReference();
                VariableDescriptor variableDescriptor;
                if (typeReference != null) {
                    variableDescriptor = classDescriptorResolver.resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, loopParameter);
                    JetType actualParameterType = variableDescriptor.getOutType();
                    if (expectedParameterType != null &&
                            actualParameterType != null &&
                            !semanticServices.getTypeChecker().isSubtypeOf(expectedParameterType, actualParameterType)) {
                        trace.getErrorHandler().genericError(typeReference.getNode(), "The loop iterates over values of type " + expectedParameterType + " but the parameter is declared to be " + actualParameterType);
                    }
                }
                else {
                    if (expectedParameterType == null) {
                        expectedParameterType = ErrorUtils.createErrorType("Error");
                    }
                    variableDescriptor = classDescriptorResolver.resolveLocalVariableDescriptor(scope.getContainingDeclaration(), loopParameter, expectedParameterType);
                }
                loopScope.addVariableDescriptor(variableDescriptor);
            }

            JetExpression body = expression.getBody();
            if (body != null) {
                getType(loopScope, body, true); // TODO
            }

            result = JetStandardClasses.getUnitType();
        }

        @Nullable
        private JetType checkIterableConvention(@NotNull JetType type, @NotNull ASTNode reportErrorsOn) {
            OverloadResolutionResult iteratorResolutionResult = resolveNoParametersFunction(type, scope, "iterator");
            if (iteratorResolutionResult.isSuccess()) {
                JetType iteratorType = iteratorResolutionResult.getFunctionDescriptor().getUnsubstitutedReturnType();
                boolean hasNextFunctionSupported = checkHasNextFunctionSupport(reportErrorsOn, iteratorType);
                boolean hasNextPropertySupported = checkHasNextPropertySupport(reportErrorsOn, iteratorType);
                if (hasNextFunctionSupported && hasNextPropertySupported && !ErrorUtils.isErrorType(iteratorType)) {
                    // TODO : overload resolution rules impose priorities here???
                    trace.getErrorHandler().genericError(reportErrorsOn, "An ambiguity between 'iterator().hasNext()' function and 'iterator().hasNext()' property");
                }
                else if (!hasNextFunctionSupported && !hasNextPropertySupported) {
                    trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().hasNext()' function or an 'iterator().hasNext' property");
                }

                OverloadResolutionResult nextResolutionResult = resolveNoParametersFunction(iteratorType, scope, "next");
                if (nextResolutionResult.isAmbiguity()) {
                    trace.getErrorHandler().genericError(reportErrorsOn, "Method 'iterator().next()' is ambiguous for this expression");
                } else if (nextResolutionResult.isNothing()) {
                    trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().next()' method");
                } else {
                    return nextResolutionResult.getFunctionDescriptor().getUnsubstitutedReturnType();
                }
            }
            else {
                String errorMessage = "For-loop range must have an iterator() method";
                if (iteratorResolutionResult.isAmbiguity()) {
                    errorMessage = "Method 'iterator()' is ambiguous for this expression";
                }
                trace.getErrorHandler().genericError(reportErrorsOn, errorMessage);
            }
            return null;
        }

        private boolean checkHasNextFunctionSupport(@NotNull ASTNode reportErrorsOn, @NotNull JetType iteratorType) {
            OverloadResolutionResult hasNextResolutionResult = resolveNoParametersFunction(iteratorType, scope, "hasNext");
            if (hasNextResolutionResult.isAmbiguity()) {
                trace.getErrorHandler().genericError(reportErrorsOn, "Method 'iterator().hasNext()' is ambiguous for this expression");
            } else if (hasNextResolutionResult.isNothing()) {
                return false;
            } else {
                JetType hasNextReturnType = hasNextResolutionResult.getFunctionDescriptor().getUnsubstitutedReturnType();
                if (!isBoolean(hasNextReturnType)) {
                    trace.getErrorHandler().genericError(reportErrorsOn, "The 'iterator().hasNext()' method of the loop range must return Boolean, but returns " + hasNextReturnType);
                }
            }
            return true;
        }

        private boolean checkHasNextPropertySupport(@NotNull ASTNode reportErrorsOn, @NotNull JetType iteratorType) {
            VariableDescriptor hasNextProperty = iteratorType.getMemberScope().getVariable("hasNext");
            // TODO :extension properties
            if (hasNextProperty == null) {
                return false;
            } else {
                JetType hasNextReturnType = hasNextProperty.getOutType();
                if (hasNextReturnType == null) {
                    // TODO : accessibility
                    trace.getErrorHandler().genericError(reportErrorsOn, "The 'iterator().hasNext' property of the loop range must be readable");
                }
                else if (!isBoolean(hasNextReturnType)) {
                    trace.getErrorHandler().genericError(reportErrorsOn, "The 'iterator().hasNext' property of the loop range must return Boolean, but returns " + hasNextReturnType);
                }
            }
            return true;
        }

        @Override
        public void visitNewExpression(JetNewExpression expression) {
            // TODO : type argument inference
            JetTypeReference typeReference = expression.getTypeReference();
            if (typeReference != null) {
                result = checkConstructorCall(scope, typeReference, expression);
            }
        }

        @Override
        public void visitHashQualifiedExpression(JetHashQualifiedExpression expression) {
            trace.getErrorHandler().genericError(expression.getOperationTokenNode(), "Unsupported");
        }

        @Override
        public void visitQualifiedExpression(JetQualifiedExpression expression) {
            // TODO : functions as values
            JetExpression selectorExpression = expression.getSelectorExpression();
            JetExpression receiverExpression = expression.getReceiverExpression();
            JetType receiverType = new TypeInferrerVisitorWithNamespaces(scope, false).getType(receiverExpression);
            if (receiverType != null) {
                // TODO : extensions to 'Any?'
                checkNullSafety(receiverType, expression.getOperationTokenNode());
                JetType selectorReturnType = getSelectorReturnType(receiverType, selectorExpression);
                if (expression.getOperationSign() == JetTokens.QUEST) {
                    if (selectorReturnType != null && !isBoolean(selectorReturnType) && selectorExpression != null) {
                        // TODO : more comprehensible error message
                        trace.getErrorHandler().typeMismatch(selectorExpression, semanticServices.getStandardLibrary().getBooleanType(), selectorReturnType);
                    }
                    result = TypeUtils.makeNullable(receiverType);
                }
                else {
                    result = selectorReturnType;
                }
                if (selectorExpression != null && result != null) {
                    trace.recordExpressionType(selectorExpression, result);
                }
            }
        }

        private JetType getCallExpressionType(@Nullable JetType receiverType, @NotNull JetCallExpression callExpression) {
            OverloadDomain overloadDomain = getOverloadDomain(receiverType, scope, callExpression.getCalleeExpression(), callExpression.getValueArgumentList());
            return resolveCall(scope, overloadDomain, callExpression);
        }

        private JetType getSelectorReturnType(JetType receiverType, JetExpression selectorExpression) {
            if (selectorExpression instanceof JetCallExpression) {
                return getCallExpressionType(receiverType, (JetCallExpression) selectorExpression);
            }
            else if (selectorExpression instanceof JetSimpleNameExpression) {
                JetScope compositeScope = new ScopeWithReceiver(scope, receiverType, semanticServices.getTypeChecker());
                return getType(compositeScope, selectorExpression, false);
            }
            else if (selectorExpression != null) {
                // TODO : not a simple name -> resolve in scope, expect property type or a function type
                trace.getErrorHandler().genericError(selectorExpression.getNode(), "Unsupported selector element type: " + selectorExpression);
            }
            return receiverType;
        }

        @Override
        public void visitCallExpression(JetCallExpression expression) {
            result = getCallExpressionType(null, expression);
        }

        @Override
        public void visitIsExpression(JetIsExpression expression) {
            // TODO : patterns and everything
            JetType knownType = getType(scope, expression.getLeftHandSide(), false);
            JetPattern pattern = expression.getPattern();
            if (pattern instanceof JetTypePattern) {
                JetTypePattern typePattern = (JetTypePattern) pattern;
                JetTypeReference typeReference = typePattern.getTypeReference();
                if (typeReference != null && knownType != null) {
                    JetType targetType = typeResolver.resolveType(scope, typeReference);
                    if (!semanticServices.getTypeChecker().isSubtypeOf(targetType, knownType)) {
                        trace.getErrorHandler().genericWarning(expression.getNode(), "Expression always evaluates to false"); // TODO : make an error?
                    }
                }
            }
            else {
                trace.getErrorHandler().genericError(pattern.getNode(), "Unsupported [JetTypeInferrer]");
            }
            result = semanticServices.getStandardLibrary().getBooleanType();
        }

        @Override
        public void visitUnaryExpression(JetUnaryExpression expression) {
            JetSimpleNameExpression operationSign = expression.getOperationSign();
            if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
                // TODO : Some processing for the label?
                result = getType(expression.getBaseExpression());
                return;
            }
            IElementType operationType = operationSign.getReferencedNameElementType();
            String name = unaryOperationNames.get(operationType);
            if (name == null) {
                trace.getErrorHandler().genericError(operationSign.getNode(), "Unknown unary operation");
            }
            else {
                JetType receiverType = getType(scope, expression.getBaseExpression(), false);
                if (receiverType != null) {
                    FunctionDescriptor functionDescriptor = lookupFunction(scope, expression.getOperationSign(), name, receiverType, Collections.<JetType>emptyList(), true);
                    if (functionDescriptor != null) {
                        JetType returnType = functionDescriptor.getUnsubstitutedReturnType();
                        if (operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) {
                            if (semanticServices.getTypeChecker().isSubtypeOf(returnType, JetStandardClasses.getUnitType())) {
                                 result = JetStandardClasses.getUnitType();
                            }
                            else {
                                if (!semanticServices.getTypeChecker().isSubtypeOf(returnType, receiverType)) {
                                    trace.getErrorHandler().genericError(operationSign.getNode(), name + " must return " + receiverType + " but returns " + returnType);
                                }
                                // TODO : Maybe returnType?
                                result = receiverType;
                            }
                        } else {
                            result = returnType;
                        }
                    }
                }
            }
        }

        @Override
        public void visitBinaryExpression(JetBinaryExpression expression) {
            JetSimpleNameExpression operationSign = expression.getOperationReference();

            JetExpression left = expression.getLeft();
            JetExpression right = expression.getRight();

            IElementType operationType = operationSign.getReferencedNameElementType();
            if (operationType == JetTokens.IDENTIFIER) {
                String referencedName = operationSign.getReferencedName();
                if (referencedName != null) {
                    result = getTypeForBinaryCall(expression, referencedName, scope, true);
                }
            }
            else if (binaryOperationNames.containsKey(operationType)) {
                result = getTypeForBinaryCall(expression, binaryOperationNames.get(operationType), scope, true);
            }
            else if (operationType == JetTokens.EQ) {
                visitAssignment(expression);
            }
            else if (assignmentOperationNames.containsKey(operationType)) {
                visitAssignmentOperation(expression);
            }
            else if (comparisonOperations.contains(operationType)) {
                JetType compareToReturnType = getTypeForBinaryCall(expression, "compareTo", scope, true);
                if (compareToReturnType != null) {
                    TypeConstructor constructor = compareToReturnType.getConstructor();
                    JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
                    TypeConstructor intTypeConstructor = standardLibrary.getInt().getTypeConstructor();
                    if (constructor.equals(intTypeConstructor)) {
                        result = standardLibrary.getBooleanType();
                    } else {
                        trace.getErrorHandler().genericError(operationSign.getNode(), "compareTo must return Int, but returns " + compareToReturnType);
                    }
                }
            }
            else if (equalsOperations.contains(operationType)) {
                String name = "equals";
                if (right != null) {
                    JetType leftType = getType(scope, left, false);
                    if (leftType != null) {
                        JetType rightType = getType(scope, right, false);
                        if (rightType != null) {
                            FunctionDescriptor equals = lookupFunction(
                                    scope, operationSign, "equals",
                                    leftType, Collections.singletonList(JetStandardClasses.getNullableAnyType()), false);
                            if (equals != null) {
                                if (ensureBooleanResult(operationSign, name, equals.getUnsubstitutedReturnType())) {
                                    ensureNonemptyIntersectionOfOperandTypes(expression);
                                }
                            }
                            else {
                                trace.getErrorHandler().genericError(operationSign.getNode(), "No method 'equals(Any?) : Boolean' available");
                            }
                        }
                    }
                }
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.EQEQEQ || operationType == JetTokens.EXCLEQEQEQ) {
                ensureNonemptyIntersectionOfOperandTypes(expression);

                // TODO : Check comparison pointlessness
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (inOperations.contains(operationType)) {
                if (right == null) {
                    result = ErrorUtils.createErrorType("No right argument"); // TODO
                    return;
                }
                checkInExpression(operationSign, left, right);
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.ANDAND || operationType == JetTokens.OROR) {
                JetType leftType = getType(scope, left, false);
                JetType rightType = right == null ? null : getType(scope, right, false);
                if (leftType != null && !isBoolean(leftType)) {
                    trace.getErrorHandler().typeMismatch(left, semanticServices.getStandardLibrary().getBooleanType(), leftType);
                }
                if (rightType != null && !isBoolean(rightType)) {
                    trace.getErrorHandler().typeMismatch(right, semanticServices.getStandardLibrary().getBooleanType(), rightType);
                }
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.ELVIS) {
                JetType leftType = getType(scope, left, false);
                JetType rightType = right == null ? null : getType(scope, right, false);
                if (leftType != null) {
                    if (!leftType.isNullable()) {
                        trace.getErrorHandler().genericWarning(left.getNode(), "Elvis operator (?:) is always returns the left operand of non-nullable type " + leftType);
                    }
                    if (rightType != null) {
                        result = TypeUtils.makeNullableAsSpecified(semanticServices.getTypeChecker().commonSupertype(leftType, rightType), rightType.isNullable());
                    }
                }
            }
            else {
                trace.getErrorHandler().genericError(operationSign.getNode(), "Unknown operation");
            }
        }

        private void checkInExpression(JetSimpleNameExpression operationSign, JetExpression left, JetExpression right) {
            String name = "contains";
            JetType containsType = getTypeForBinaryCall(scope, right, operationSign, left, name, true);
            ensureBooleanResult(operationSign, name, containsType);
        }

        private void ensureNonemptyIntersectionOfOperandTypes(JetBinaryExpression expression) {
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            JetExpression left = expression.getLeft();
            JetExpression right = expression.getRight();

            // TODO : duplicated effort for == and !=
            JetType leftType = getType(scope, left, false);
            if (leftType != null && right != null) {
                JetType rightType = getType(scope, right, false);

                if (rightType != null) {
                    JetType intersect = TypeUtils.intersect(semanticServices.getTypeChecker(), new HashSet<JetType>(Arrays.asList(leftType, rightType)));
                    if (intersect == null) {
                        trace.getErrorHandler().genericError(expression.getNode(), "Operator " + operationSign.getReferencedName() + " cannot be applied to " + leftType + " and " + rightType);
                    }
                }
            }
        }

        protected void visitAssignmentOperation(JetBinaryExpression expression) {
            assignmentIsNotAnExpressionError(expression);
        }

        protected void visitAssignment(JetBinaryExpression expression) {
            assignmentIsNotAnExpressionError(expression);
        }

        private void assignmentIsNotAnExpressionError(JetBinaryExpression expression) {
            trace.getErrorHandler().genericError(expression.getNode(), "Assignments are not expressions, and only expressions are allowed in this context");
        }

        private boolean ensureBooleanResult(JetExpression operationSign, String name, JetType resultType) {
            return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'");
        }

        private boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName) {
            if (resultType != null) {
                // TODO : Relax?
                if (!isBoolean(resultType)) {
                    trace.getErrorHandler().genericError(operationSign.getNode(), subjectName + " must return Boolean but returns " + resultType);
                    return false;
                }
            }
            return true;
        }

        private boolean isBoolean(@NotNull JetType type) {
            return semanticServices.getTypeChecker().isConvertibleTo(type,  semanticServices.getStandardLibrary().getBooleanType());
        }

        @Override
        public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
            JetExpression arrayExpression = expression.getArrayExpression();
            JetType receiverType = getType(scope, arrayExpression, false);
            List<JetExpression> indexExpressions = expression.getIndexExpressions();
            List<JetType> argumentTypes = getTypes(scope, indexExpressions);
            if (argumentTypes == null) return;

            if (receiverType != null) {
                FunctionDescriptor functionDescriptor = lookupFunction(scope, expression, "get", receiverType, argumentTypes, true);
                if (functionDescriptor != null) {
                    result = functionDescriptor.getUnsubstitutedReturnType();
                }
            }
        }

        @Nullable
        protected JetType getTypeForBinaryCall(
                @NotNull JetBinaryExpression expression,
                @NotNull String name,
                @NotNull JetScope scope,
                boolean reportUnresolved) {
            JetExpression left = expression.getLeft();
            JetExpression right = expression.getRight();
            if (right == null) {
                return null;
            }
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            return getTypeForBinaryCall(scope, left, operationSign, right, name, reportUnresolved);
        }

        @Nullable
        private JetType getTypeForBinaryCall(
                @NotNull JetScope scope,
                @NotNull JetExpression left,
                @NotNull JetSimpleNameExpression operationSign,
                @NotNull JetExpression right,
                @NotNull String name,
                boolean reportUnresolved) {
            JetType leftType = getType(scope, left, false);
            JetType rightType = getType(scope, right, false);
            if (leftType == null || rightType == null) {
                return null;
            }
            FunctionDescriptor functionDescriptor = lookupFunction(scope, operationSign, name, leftType, Collections.singletonList(rightType), reportUnresolved);
            if (functionDescriptor != null) {
                if (leftType.isNullable()) {
                    // TODO : better error message for '1 + nullableVar' case
                    trace.getErrorHandler().genericError(operationSign.getNode(),
                            "Infix call corresponds to a dot-qualified call '" +
                            left.getText() + "." + name + "(" + right.getText() + ")'" +
                            " which is not allowed on a nullable receiver '" + right.getText() + "'." +
                            " Use '?.'-qualified call instead");
                }

                return functionDescriptor.getUnsubstitutedReturnType();
            }
            return null;
        }

        @Override
        public void visitDeclaration(JetDeclaration dcl) {
            trace.getErrorHandler().genericError(dcl.getNode(), "Declarations are not allowed in this position");
        }

        @Override
        public void visitRootNamespaceExpression(JetRootNamespaceExpression expression) {
            trace.getErrorHandler().genericError(expression.getNode(), "'namespace' is not an expression");
            result = null;
        }

        @Override
        public void visitJetElement(JetElement elem) {
            trace.getErrorHandler().genericError(elem.getNode(), "[JetTypeInferrer] Unsupported element: " + elem + " " + elem.getClass().getCanonicalName());
        }
    }

    private class TypeInferrerVisitorWithNamespaces extends TypeInferrerVisitor {
        private TypeInferrerVisitorWithNamespaces(@NotNull JetScope scope, boolean preferBlock) {
            super(scope, preferBlock);
        }

        @NotNull
        @Override
        public TypeInferrerVisitor createNew(JetScope scope, boolean preferBlock) {
            return new TypeInferrerVisitorWithNamespaces(scope, preferBlock);
        }

        @Override
        public void visitRootNamespaceExpression(JetRootNamespaceExpression expression) {
            result = JetModuleUtil.getRootNamespaceType(expression);
        }

        @Override
        protected boolean furtherNameLookup(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName) {
            result = lookupNamespaceType(expression, referencedName);
            return result != null;
        }

    }

    private class TypeInferrerVisitorWithWritableScope extends TypeInferrerVisitor {
        private final WritableScope scope;

        public TypeInferrerVisitorWithWritableScope(@NotNull WritableScope scope, boolean preferBlock) {
            super(scope, preferBlock);
            this.scope = scope;
        }

        @Override
        public void visitProperty(JetProperty property) {

            JetTypeReference receiverTypeRef = property.getReceiverTypeRef();
            if (receiverTypeRef != null) {
                trace.getErrorHandler().genericError(receiverTypeRef.getNode(), "Local receiver-properties are not allowed");
            }

            JetPropertyAccessor getter = property.getGetter();
            if (getter != null) {
                trace.getErrorHandler().genericError(getter.getNode(), "Local variables are not allowed to have getters");
            }

            JetPropertyAccessor setter = property.getSetter();
            if (setter != null) {
                trace.getErrorHandler().genericError(setter.getNode(), "Local variables are not allowed to have setters");
            }

            VariableDescriptor propertyDescriptor = classDescriptorResolver.resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, property);
            JetExpression initializer = property.getInitializer();
            if (property.getPropertyTypeRef() != null && initializer != null) {
                JetType initializerType = getType(scope, initializer, false);
                JetType outType = propertyDescriptor.getOutType();
                if (outType != null &&
                    initializerType != null &&
                    !semanticServices.getTypeChecker().isConvertibleTo(initializerType, outType)) {
                    trace.getErrorHandler().typeMismatch(initializer, outType, initializerType);
                }
            }

            scope.addVariableDescriptor(propertyDescriptor);
        }

        @Override
        public void visitFunction(JetFunction function) {
            scope.addFunctionDescriptor(classDescriptorResolver.resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function));
        }

        @Override
        public void visitClass(JetClass klass) {
            super.visitClass(klass); // TODO
        }

        @Override
        public void visitExtension(JetExtension extension) {
            super.visitExtension(extension); // TODO
        }

        @Override
        public void visitTypedef(JetTypedef typedef) {
            super.visitTypedef(typedef); // TODO
        }

        @Override
        public void visitDeclaration(JetDeclaration dcl) {
            visitJetElement(dcl);
        }

        @Override
        protected void visitAssignmentOperation(JetBinaryExpression expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            String name = assignmentOperationNames.get(operationType);
            JetType assignmentOperationType = getTypeForBinaryCall(expression, name, scope, false);

            if (assignmentOperationType == null) {
                String counterpartName = binaryOperationNames.get(assignmentOperationCounterparts.get(operationType));
                getTypeForBinaryCall(expression, counterpartName, scope, true);
            }
            result = null; // not an expression
        }

        @Override
        protected void visitAssignment(JetBinaryExpression expression) {
            JetExpression left = expression.getLeft();
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(left);
            JetExpression right = expression.getRight();
            if (deparenthesized instanceof JetArrayAccessExpression) {
                JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) deparenthesized;
                resolveArrayAccessToLValue(arrayAccessExpression, right, expression.getOperationReference());
            }
            else {
                JetType leftType = getType(scope, left, false);
                if (right != null) {
                    JetType rightType = getType(scope, right, false);
                    if (rightType != null &&
                        leftType != null &&
                            !semanticServices.getTypeChecker().isConvertibleTo(rightType, leftType)) {
                        trace.getErrorHandler().typeMismatch(right, leftType, rightType);
                    }
                }
            }
            result = null; // This is not an element
        }

        private void resolveArrayAccessToLValue(JetArrayAccessExpression arrayAccessExpression, JetExpression rightHandSide, JetSimpleNameExpression operationSign) {
            List<JetType> argumentTypes = getTypes(scope, arrayAccessExpression.getIndexExpressions());
            if (argumentTypes == null) return;
            JetType rhsType = getType(scope, rightHandSide, false);
            if (rhsType == null) return;
            argumentTypes.add(rhsType);

            JetType receiverType = getType(scope, arrayAccessExpression.getArrayExpression(), false);
            if (receiverType == null) return;

            // TODO : nasty hack: effort is duplicated
            lookupFunction(scope, arrayAccessExpression, "set", receiverType, argumentTypes, true);
            FunctionDescriptor functionDescriptor = lookupFunction(scope, operationSign, "set", receiverType, argumentTypes, true);
            if (functionDescriptor != null) {
                result = functionDescriptor.getUnsubstitutedReturnType();
            }
        }

        @Override
        public void visitJetElement(JetElement elem) {
            trace.getErrorHandler().genericError(elem.getNode(), "Unsupported element in a block: " + elem + " " + elem.getClass().getCanonicalName());
        }
    }

//    private class CachedBindingTrace extends BindingTraceAdapter {
//
//        public CachedBindingTrace(BindingTrace originalTrace) {
//            super(originalTrace);
//        }
//
//        @Override
//        public void recordExpressionType(@NotNull JetExpression expression, @NotNull JetType type) {
//            super.recordExpressionType(expression, type);
//            typeCache.put(expression, type);
//        }
//    }
}
