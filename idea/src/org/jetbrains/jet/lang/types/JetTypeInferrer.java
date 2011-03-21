package org.jetbrains.jet.lang.types;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeInferrer {

    private static final Map<IElementType, String> unaryOperationNames = new HashMap<IElementType, String>();
    static {
        unaryOperationNames.put(JetTokens.PLUSPLUS, "inc");
        unaryOperationNames.put(JetTokens.MINUSMINUS, "dec");
        unaryOperationNames.put(JetTokens.PLUS, "plus");
        unaryOperationNames.put(JetTokens.MINUS, "minus");
        unaryOperationNames.put(JetTokens.EXCL, "not");
    }

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

    private static final Map<IElementType, String> assignmentOperationNames = new HashMap<IElementType, String>();
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

    private final BindingTrace trace;
    private final JetSemanticServices semanticServices;
    private final TypeResolver typeResolver;
    private final ClassDescriptorResolver classDescriptorResolver;

    public JetTypeInferrer(BindingTrace trace, JetSemanticServices semanticServices) {
        this.trace = trace;
        this.semanticServices = semanticServices;
        this.typeResolver = new TypeResolver(trace, semanticServices);
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
    }

    @NotNull
    public JetType safeGetType(@NotNull final JetScope scope, @NotNull JetExpression expression, final boolean preferBlock) {
        JetType type = getType(scope, expression, preferBlock);
        if (type != null) {
            return type;
        }
        return ErrorType.createErrorType("Type for " + expression.getText());
    }

    @Nullable
    public JetType getType(@NotNull final JetScope scope, @NotNull JetExpression expression, final boolean preferBlock) {
        TypeInferrerVisitor visitor = new TypeInferrerVisitor(scope, preferBlock);
        expression.accept(visitor);
        JetType result = visitor.getResult();
        if (result != null) {
            trace.recordExpressionType(expression, result);
        }
        return result;
    }

    @NotNull
    private JetExpression deparenthesize(@NotNull JetExpression expression) {
        while (expression instanceof JetParenthesizedExpression) {
            expression = ((JetParenthesizedExpression) expression).getExpression();
        }
        return expression;
    }

    @Nullable
    private FunctionDescriptor lookupFunction(JetScope scope, JetReferenceExpression reference, String name, JetType receiverType, List<JetType> argumentTypes, boolean reportUnresolved) {
        OverloadDomain overloadDomain = semanticServices.getOverloadResolver().getOverloadDomain(receiverType, scope, name);
        overloadDomain = wrapForTracing(overloadDomain, reference, reportUnresolved);
        return overloadDomain.getFunctionDescriptorForPositionedArguments(Collections.<JetType>emptyList(), argumentTypes);
    }

    @Nullable
    private List<JetType> getTypes(JetScope scope, List<JetExpression> indexExpressions) {
        List<JetType> argumentTypes = new ArrayList<JetType>();
        for (JetExpression indexExpression : indexExpressions) {
            JetType type = getType(scope, indexExpression, false);
            if (type == null) {
                return null;
            }
            argumentTypes.add(type);
        }
        return argumentTypes;
    }


    private OverloadDomain getOverloadDomain(final JetScope scope, JetExpression calleeExpression) {
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
                // . or ?.
                JetType receiverType = getType(scope, expression.getReceiverExpression(), false);
                checkNullSafety(receiverType, expression);

                JetExpression selectorExpression = expression.getSelectorExpression();
                if (selectorExpression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression referenceExpression = (JetSimpleNameExpression) selectorExpression;

                    if (receiverType != null) {
                        result[0] = semanticServices.getOverloadResolver().getOverloadDomain(receiverType, scope, referenceExpression.getReferencedName());
                        reference[0] = referenceExpression;
                    }
                } else {
                    throw new UnsupportedOperationException(); // TODO
                }
            }

            @Override
            public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
                // a -- create a hierarchical lookup domain for this.a
                result[0] = semanticServices.getOverloadResolver().getOverloadDomain(null, scope, expression.getReferencedName());
                reference[0] = expression;
            }

            @Override
            public void visitExpression(JetExpression expression) {
                // <e> create a dummy domain for the type of e
                throw new UnsupportedOperationException(expression.getText()); // TODO
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported element: " + elem);
            }
        });
        return wrapForTracing(result[0], reference[0], true);
    }

    private void checkNullSafety(JetType receiverType, JetQualifiedExpression expression) {
        if (receiverType != null) {
            boolean namespaceType = receiverType instanceof NamespaceType;
            boolean nullable = !namespaceType && receiverType.isNullable();
            if (nullable && expression.getOperationSign() == JetTokens.DOT) {
                semanticServices.getErrorHandler().genericError(expression.getOperationTokenNode(), "Only safe calls (?.) are allowed on a nullable receiver of type " + receiverType);
            }
            else if (!nullable && expression.getOperationSign() == JetTokens.SAFE_ACCESS) {
                if (namespaceType) {
                    semanticServices.getErrorHandler().genericError(expression.getOperationTokenNode(), "Safe calls are not allowed on namespaces");
                }
                else {
                    semanticServices.getErrorHandler().genericWarning(expression.getOperationTokenNode(), "Unnecessary safe call on a non-null receiver of type  " + receiverType);
                }
            }
        }
    }

    private OverloadDomain wrapForTracing(final OverloadDomain overloadDomain, @NotNull final JetReferenceExpression referenceExpression, final boolean reportUnresolved) {
        if (overloadDomain == null) return OverloadDomain.EMPTY;
        return new OverloadDomain() {
            @Override
            public FunctionDescriptor getFunctionDescriptorForNamedArguments(@NotNull List<JetType> typeArguments, @NotNull Map<String, JetType> valueArgumentTypes, @Nullable JetType functionLiteralArgumentType) {
                FunctionDescriptor descriptor = overloadDomain.getFunctionDescriptorForNamedArguments(typeArguments, valueArgumentTypes, functionLiteralArgumentType);
                if (descriptor != null) {
                    trace.recordReferenceResolution(referenceExpression, descriptor);
                } else {
                    if (reportUnresolved) {
                        semanticServices.getErrorHandler().unresolvedReference(referenceExpression);
                    }
                }
                return descriptor;
            }

            @Override
            public FunctionDescriptor getFunctionDescriptorForPositionedArguments(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
                FunctionDescriptor descriptor = overloadDomain.getFunctionDescriptorForPositionedArguments(typeArguments, positionedValueArgumentTypes);
                if (descriptor != null) {
                    trace.recordReferenceResolution(referenceExpression, descriptor);
                } else {
                    if (reportUnresolved) {
                        semanticServices.getErrorHandler().unresolvedReference(referenceExpression);
                    }
                }
                return descriptor;
            }
        };
    }

    private JetType getBlockReturnedType(@NotNull JetScope outerScope, List<JetElement> block) {
        if (block.isEmpty()) {
            return JetStandardClasses.getUnitType();
        } else {
            DeclarationDescriptor containingDescriptor = outerScope.getContainingDeclaration();
            WritableScope scope = semanticServices.createWritableScope(outerScope, containingDescriptor);
            return getBlockReturnedTypeWithWritableScope(scope, block);
        }
    }

    private JetType getBlockReturnedTypeWithWritableScope(@NotNull WritableScope scope, @NotNull List<? extends JetElement> block) {
        assert !block.isEmpty();
        TypeInferrerVisitorWithWritableScope blockLevelVisitor = new TypeInferrerVisitorWithWritableScope(scope, true);
        JetType result = null;
        for (JetElement statement : block) {
            statement.accept(blockLevelVisitor);
            result = blockLevelVisitor.getResult();
            if (result != null) {
                trace.recordExpressionType((JetExpression) statement, result);
            }
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
                    result.add(getType(scope, resultExpression, true));
                }
            }
        }
    }

    private class TypeInferrerVisitor extends JetVisitor {
        private final JetScope scope;
        protected JetType result;
        private final boolean preferBlock;

        public TypeInferrerVisitor(JetScope scope, boolean preferBlock) {
            this.scope = scope;
            this.preferBlock = preferBlock;
        }

        public JetType getResult() {
            return result;
        }

        @Override
        public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
            // TODO : other members
            // TODO : type substitutions???
            String referencedName = expression.getReferencedName();
            if (referencedName != null) {
                PropertyDescriptor property = scope.getProperty(referencedName);
                if (property != null) {
                    trace.recordReferenceResolution(expression, property);
                    result = property.getType();
                    return;
                } else {
                    NamespaceDescriptor namespace = scope.getNamespace(referencedName);
                    if (namespace != null) {
                        trace.recordReferenceResolution(expression, namespace);
                        result = namespace.getNamespaceType();
                        return;
                    }
                }
                semanticServices.getErrorHandler().unresolvedReference(expression);
            }
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            if (preferBlock && !expression.hasParameterSpecification()) {
                result = getBlockReturnedType(scope, expression.getBody());
                return;
            }

            FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(scope.getContainingDeclaration(), Collections.<Attribute>emptyList(), "<anonymous>");

            JetTypeReference returnTypeRef = expression.getReturnTypeRef();

            JetTypeReference receiverTypeRef = expression.getReceiverTypeRef();
            final JetType receiverType;
            if (receiverTypeRef != null) {
                receiverType = typeResolver.resolveType(scope, receiverTypeRef);
            } else {
                receiverType = scope.getThisType();
            }

            List<JetElement> body = expression.getBody();
            final Map<String, PropertyDescriptor> parameterDescriptors = new HashMap<String, PropertyDescriptor>();
            List<JetType> parameterTypes = new ArrayList<JetType>();
            for (JetParameter parameter : expression.getParameters()) {
                JetTypeReference typeReference = parameter.getTypeReference();
                if (typeReference == null) {
                    throw new UnsupportedOperationException("Type inference for parameters is not implemented yet");
                }
                PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePropertyDescriptor(functionDescriptor, scope, parameter);
                parameterDescriptors.put(parameter.getName(), propertyDescriptor);
                parameterTypes.add(propertyDescriptor.getType());
            }
            JetType returnType;
            if (returnTypeRef != null) {
                returnType = typeResolver.resolveType(scope, returnTypeRef);
            } else {
                WritableScope writableScope = semanticServices.createWritableScope(scope, functionDescriptor);
                for (PropertyDescriptor propertyDescriptor : parameterDescriptors.values()) {
                    writableScope.addPropertyDescriptor(propertyDescriptor);
                }
                writableScope.setThisType(receiverType);
                returnType = getBlockReturnedType(writableScope, body);
            }
            result = JetStandardClasses.getFunctionType(null, receiverTypeRef == null ? null : receiverType, parameterTypes, returnType);
        }

        @Override
        public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
            result = getType(scope, expression.getExpression(), false);
        }

        @Override
        public void visitConstantExpression(JetConstantExpression expression) {
            IElementType elementType = expression.getNode().getElementType();
            JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
            if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
                result = standardLibrary.getIntType();
            } else if (elementType == JetNodeTypes.LONG_CONSTANT) {
                result = standardLibrary.getLongType();
            } else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
                String text = expression.getText();
                assert text.length() > 0;
                char lastChar = text.charAt(text.length() - 1);
                if (lastChar == 'f' || lastChar == 'F') {
                    result = standardLibrary.getFloatType();
                } else {
                    result = standardLibrary.getDoubleType();
                }
            } else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
                result = standardLibrary.getBooleanType();
            } else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
                result = standardLibrary.getCharType();
            } else if (elementType == JetNodeTypes.STRING_CONSTANT) {
                result = standardLibrary.getStringType();
            } else if (elementType == JetNodeTypes.NULL) {
                result = JetStandardClasses.getNullableNothingType();
            } else {
                throw new IllegalArgumentException("Unsupported constant: " + expression);
            }
        }

        @Override
        public void visitThrowExpression(JetThrowExpression expression) {
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
            throw new UnsupportedOperationException("Return some reflection interface"); // TODO
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
                        semanticServices.getErrorHandler().typeMismatch(expression.getLeft(), targetType, actualType);
                    }
                }
                else if (operationType == JetTokens.AS_KEYWORD) {
                    // TODO : Check for cast impossibility
                }
                else {
                    semanticServices.getErrorHandler().genericError(expression.getOperationSign().getNode(), "Unsupported binary operation");
                }
                result = targetType;
            }
        }

        @Override
        public void visitTupleExpression(JetTupleExpression expression) {
            List<JetExpression> entries = expression.getEntries();
            List<JetType> types = new ArrayList<JetType>();
            for (JetExpression entry : entries) {
                types.add(getType(scope, entry, false));
            }
            // TODO : labels
            result = JetStandardClasses.getTupleType(types);
        }

        @Override
        public void visitThisExpression(JetThisExpression expression) {
            // TODO : qualified this, e.g. Foo.this<Bar>
            JetType thisType = scope.getThisType();
            JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
            if (superTypeQualifier != null) {
                // This cast must be safe (assuming the PSI doesn't contain errors)
                JetUserType typeElement = (JetUserType) superTypeQualifier.getTypeElement();
                ClassDescriptor superclass = typeResolver.resolveClass(scope, typeElement);
                Collection<? extends JetType> supertypes = thisType.getConstructor().getSupertypes();
                Map<TypeConstructor, TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(thisType);
                for (JetType declaredSupertype : supertypes) {
                    if (declaredSupertype.getConstructor().equals(superclass.getTypeConstructor())) {
                        result = TypeSubstitutor.INSTANCE.substitute(substitutionContext, declaredSupertype, Variance.INVARIANT);
                        break;
                    }
                }
                assert result != null;
            } else {
                result = thisType;
            }
        }

        @Override
        public void visitBlockExpression(JetBlockExpression expression) {
            result = getBlockReturnedType(scope, expression.getStatements());
        }

        @Override
        public void visitWhenExpression(JetWhenExpression expression) {
            // TODO :change scope according to the bound value in the when header
            List<JetType> expressions = new ArrayList<JetType>();
            collectAllReturnTypes(expression, scope, expressions);
            result = semanticServices.getTypeChecker().commonSupertype(expressions);
        }

        @Override
        public void visitTryExpression(JetTryExpression expression) {
            JetExpression tryBlock = expression.getTryBlock();
            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            JetFinallySection finallyBlock = expression.getFinallyBlock();
            List<JetType> types = new ArrayList<JetType>();
            if (finallyBlock == null) {
                for (JetCatchClause catchClause : catchClauses) {
                    // TODO: change scope here
                    types.add(getType(scope, catchClause.getCatchBody(), true));
                }
            } else {
                types.add(getType(scope, finallyBlock.getFinalExpression(), true));
            }
            types.add(getType(scope, tryBlock, true));
            result = semanticServices.getTypeChecker().commonSupertype(types);
        }

        @Override
        public void visitIfExpression(JetIfExpression expression) {
            checkCondition(scope, expression.getCondition());

            // TODO : change types according to is and null checks
            JetExpression elseBranch = expression.getElse();
            JetExpression thenBranch = expression.getThen();
            JetType thenType = null;
            if (thenBranch != null) {
                thenType = getType(scope, thenBranch, true);
            }
            if (elseBranch != null) {
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
            else {
                result = JetStandardClasses.getUnitType();
            }
        }

        private void checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition) {
            if (condition != null) {
                JetType conditionType = getType(scope, condition, false);

                if (conditionType != null && !isBoolean(conditionType)) {
                    semanticServices.getErrorHandler().genericError(condition.getNode(), "Condition must be of type Boolean, but was of type " + conditionType);
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
                    WritableScope writableScope = semanticServices.createWritableScope(scope, scope.getContainingDeclaration());
                    conditionScope = writableScope;
                    getBlockReturnedTypeWithWritableScope(writableScope, function.getBody());
                } else {
                    getType(scope, body, true);
                }
            }
            else if (body != null) {
                WritableScope writableScope = semanticServices.createWritableScope(scope, scope.getContainingDeclaration());
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
            JetType loopRangeType = getType(scope, loopRange, false);
            JetType expectedParameterType = null;
            if (loopRangeType != null) {
                if (!semanticServices.getTypeChecker().isSubtypeOf(loopRangeType, semanticServices.getStandardLibrary().getIterableType(JetStandardClasses.getNullableAnyType()))) {
                    semanticServices.getErrorHandler().genericError(loopRange.getNode(), "Expecting an Iterable, but found " + loopRangeType);
                }
                else {
                    TypeProjection typeProjection = loopRangeType.getArguments().get(0);
                    if (!typeProjection.getProjectionKind().allowsOutPosition()) {
                        expectedParameterType = JetStandardClasses.getDefaultBound();
                    }
                    else {
                        expectedParameterType = typeProjection.getType();
                    }
                }
            }

            WritableScope loopScope = semanticServices.createWritableScope(scope, scope.getContainingDeclaration());

            JetTypeReference typeReference = loopParameter.getTypeReference();
            PropertyDescriptor propertyDescriptor;
            if (typeReference != null) {
                propertyDescriptor = semanticServices.getClassDescriptorResolver(trace).resolvePropertyDescriptor(scope.getContainingDeclaration(), scope, loopParameter);
                JetType actualParameterType = propertyDescriptor.getType();
                if (expectedParameterType != null &&
                        !semanticServices.getTypeChecker().isSubtypeOf(expectedParameterType, actualParameterType)) {
                    semanticServices.getErrorHandler().genericError(typeReference.getNode(), "The loop iterates over values of type " + expectedParameterType + " but the parameter is declared to be " + actualParameterType);
                }
            }
            else {
                if (expectedParameterType == null) {
                    expectedParameterType = ErrorType.createErrorType("Error");
                }
                propertyDescriptor = semanticServices.getClassDescriptorResolver(trace).resolvePropertyDescriptor(scope.getContainingDeclaration(), loopParameter, expectedParameterType);
            }
            loopScope.addPropertyDescriptor(propertyDescriptor);
            trace.recordDeclarationResolution(loopParameter, propertyDescriptor);

            JetExpression body = expression.getBody();
            if (body != null) {
                getType(loopScope, body, true); // TODO
            }

            result = JetStandardClasses.getUnitType();
        }

        @Override
        public void visitNewExpression(JetNewExpression expression) {
            // TODO : type argument inference
            JetTypeReference typeReference = expression.getTypeReference();
            if (typeReference != null) {
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

                        JetSimpleNameExpression referenceExpression = userType.getReferenceExpression();
                        if (referenceExpression != null) {
                            FunctionGroup constructors = classDescriptor.getConstructors(receiverType.getArguments());
                            OverloadDomain constructorsOverloadDomain = semanticServices.getOverloadResolver().getOverloadDomain(constructors);
                            result = resolveOverloads(
                                    scope,
                                    wrapForTracing(constructorsOverloadDomain, referenceExpression, true),
                                    Collections.<JetTypeProjection>emptyList(),
                                    expression.getArguments(),
                                    expression.getFunctionLiteralArguments());
                        }
                    }
                    else {
                        semanticServices.getErrorHandler().genericError(expression.getNode(), "Calling a constructor is only supported for ordinary classes"); // TODO : review the message
                    }
                }
                else {
                    semanticServices.getErrorHandler().genericError(typeElement.getNode(), "Calling a constructor is only supported for ordinary classes"); // TODO : Better message
                }
            }
        }

        @Override
        public void visitHashQualifiedExpression(JetHashQualifiedExpression expression) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public void visitQualifiedExpression(JetQualifiedExpression expression) {
            // TODO : functions
            JetExpression selectorExpression = expression.getSelectorExpression();
            JetExpression receiverExpression = expression.getReceiverExpression();
            JetType receiverType = getType(scope, receiverExpression, false);
            if (receiverType != null) {
                checkNullSafety(receiverType, expression);
                JetType selectorReturnType = getSelectorReturnType(receiverType, selectorExpression);
                if (expression.getOperationSign() == JetTokens.QUEST) {
                    if (selectorReturnType != null && !isBoolean(selectorReturnType)) {
                        // TODO : more comprehensible error message
                        semanticServices.getErrorHandler().typeMismatch(selectorExpression, semanticServices.getStandardLibrary().getBooleanType(), selectorReturnType);
                    }
                    result = TypeUtils.makeNullable(receiverType);
                }
                else {
                    result = selectorReturnType;
                }
            }
        }

        private JetType getSelectorReturnType(JetType receiverType, JetExpression selectorExpression) {
            JetScope compositeScope = new ScopeWithReceiver(scope, receiverType);
            if (selectorExpression instanceof JetCallExpression) {
                JetCallExpression callExpression = (JetCallExpression) selectorExpression;
                OverloadDomain overloadDomain = getOverloadDomain(compositeScope, callExpression.getCalleeExpression());
                return resolveOverloads(scope, callExpression, overloadDomain);
            }
            else if (selectorExpression instanceof JetSimpleNameExpression) {
                return getType(compositeScope, selectorExpression, false);
            }
            else if (selectorExpression != null) {
                // TODO : not a simple name -> resolve in scope, expect property type or a function type
                throw new UnsupportedOperationException();
            }
            return receiverType;
        }

        @Override
        public void visitCallExpression(JetCallExpression expression) {
            JetExpression calleeExpression = expression.getCalleeExpression();
            OverloadDomain overloadDomain = getOverloadDomain(scope, calleeExpression);
            result = resolveOverloads(scope, expression, overloadDomain);
        }

        @Nullable
        private JetType resolveOverloads(JetScope scope, JetCallExpression expression, OverloadDomain overloadDomain) {
            List<JetTypeProjection> typeArguments = expression.getTypeArguments();
            List<JetArgument> valueArguments = expression.getValueArguments();
            List<JetExpression> functionLiteralArguments = expression.getFunctionLiteralArguments();
            return resolveOverloads(scope, overloadDomain, typeArguments, valueArguments, functionLiteralArguments);
        }

        @Nullable
        private JetType resolveOverloads(
                @NotNull JetScope scope,
                @NotNull OverloadDomain overloadDomain,
                @NotNull List<JetTypeProjection> typeArguments,
                @NotNull List<JetArgument> valueArguments,
                @NotNull List<JetExpression> functionLiteralArguments) {
            // 1) ends with a name -> (scope, name) to look up
            // 2) ends with something else -> just check types

            for (JetTypeProjection typeArgument : typeArguments) {
                if (typeArgument.getProjectionKind() != JetProjectionKind.NONE) {
                    semanticServices.getErrorHandler().genericError(typeArgument.getNode(), "Projections are not allowed on type parameters for methods"); // TODO : better positioning
                }
            }

            boolean someNamed = false;
            for (JetArgument argument : valueArguments) {
                if (argument.isNamed()) {
                    someNamed = true;
                    break;
                }
            }

//                JetExpression functionLiteralArgument = functionLiteralArguments.isEmpty() ? null : functionLiteralArguments.get(0);
            // TODO : must be a check
            assert functionLiteralArguments.size() <= 1;

            if (someNamed) {
                // TODO : check that all are named
                throw new UnsupportedOperationException(); // TODO

//                    result = overloadDomain.getFunctionDescriptorForNamedArguments(typeArguments, valueArguments, functionLiteralArgument);
            } else {
                List<JetType> types = new ArrayList<JetType>();
                for (JetTypeProjection projection : typeArguments) {
                    // TODO : check that there's no projection
                    types.add(typeResolver.resolveType(scope, projection.getTypeReference()));
                }

                List<JetExpression> positionedValueArguments = new ArrayList<JetExpression>();
                for (JetArgument argument : valueArguments) {
                    positionedValueArguments.add(argument.getArgumentExpression());
                }

                positionedValueArguments.addAll(functionLiteralArguments);

                List<JetType> valueArgumentTypes = new ArrayList<JetType>();
                for (JetExpression valueArgument : positionedValueArguments) {
                    valueArgumentTypes.add(safeGetType(scope, valueArgument, false));
                }

                FunctionDescriptor functionDescriptor = overloadDomain.getFunctionDescriptorForPositionedArguments(types, valueArgumentTypes);
                if (functionDescriptor != null) {
                    return functionDescriptor.getUnsubstitutedReturnType();
                }
            }
            return null;
        }

        @Override
        public void visitIsExpression(JetIsExpression expression) {
            // TODO : patterns and everything
            System.out.println("Pattern matching is not supported yet.");
            result = semanticServices.getStandardLibrary().getBooleanType();
        }

        @Override
        public void visitUnaryExpression(JetUnaryExpression expression) {
            JetSimpleNameExpression operationSign = expression.getOperationSign();
            IElementType operationType = operationSign.getReferencedNameElementType();
            String name = unaryOperationNames.get(operationType);
            if (name == null) {
                semanticServices.getErrorHandler().genericError(operationSign.getNode(), "Unknown unary operation");
            }
            else {
                JetType receiverType = getType(scope, expression.getBaseExpression(), false);
                if (receiverType != null) {
                    FunctionDescriptor functionDescriptor = lookupFunction(scope, expression.getOperationSign(), name, receiverType, Collections.<JetType>emptyList(), true);
                    if (functionDescriptor != null) {
                        JetType returnType = functionDescriptor.getUnsubstitutedReturnType();
                        if (operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) {
                            if (!semanticServices.getTypeChecker().isSubtypeOf(returnType, receiverType)) {
                                 semanticServices.getErrorHandler().genericError(operationSign.getNode(), name + " must return " + receiverType + " but returns " + returnType);
                            }
                            // TODO : Maybe returnType?
                            result = receiverType;
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
                        semanticServices.getErrorHandler().genericError(operationSign.getNode(), "compareTo must return Int, but returns " + compareToReturnType);
                    }
                }
            }
            else if (equalsOperations.contains(operationType)) {
                String name = "equals";
                JetType equalsType = getTypeForBinaryCall(expression, name, scope, true);
                assureBooleanResult(operationSign, name, equalsType);
            }
            else if (inOperations.contains(operationType)) {
                if (right == null) {
                    result = ErrorType.createErrorType("No right argument"); // TODO
                    return;
                }
                String name = "contains";
                JetType containsType = getTypeForBinaryCall(scope, right, expression.getOperationReference(), expression.getLeft(), name, true);
                result = assureBooleanResult(operationSign, name, containsType);
            }
            else if (operationType == JetTokens.EQEQEQ || operationType == JetTokens.EXCLEQEQEQ) {
                JetType leftType = getType(scope, left, false);
                JetType rightType = right == null ? null : getType(scope, right, false);
                // TODO : Check comparison pointlessness
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.ANDAND || operationType == JetTokens.OROR) {
                JetType leftType = getType(scope, left, false);
                JetType rightType = right == null ? null : getType(scope, right, false);
                if (leftType != null && !isBoolean(leftType)) {
                    semanticServices.getErrorHandler().typeMismatch(left, semanticServices.getStandardLibrary().getBooleanType(), leftType);
                }
                if (rightType != null && !isBoolean(rightType)) {
                    semanticServices.getErrorHandler().typeMismatch(left, semanticServices.getStandardLibrary().getBooleanType(), rightType);
                    semanticServices.getErrorHandler().typeMismatch(right, semanticServices.getStandardLibrary().getBooleanType(), rightType);
                }
                result = semanticServices.getStandardLibrary().getBooleanType();
            }
            else if (operationType == JetTokens.ELVIS) {
                JetType leftType = getType(scope, left, false);
                JetType rightType = right == null ? null : getType(scope, right, false);
                if (leftType != null) {
                    if (!leftType.isNullable()) {
                        semanticServices.getErrorHandler().genericWarning(left.getNode(), "Elvis operator (?:) is always returns the left operand of non-nullable type " + leftType);
                    }
                    if (rightType != null) {
                        result = TypeUtils.makeNullableAsSpecified(semanticServices.getTypeChecker().commonSupertype(leftType, rightType), rightType.isNullable());
                    }
                }
            }
            else {
                semanticServices.getErrorHandler().genericError(operationSign.getNode(), "Unknown operation");
            }
        }

        protected void visitAssignmentOperation(JetBinaryExpression expression) {
            assignmentIsNotAnExpressionError(expression);
        }

        protected void visitAssignment(JetBinaryExpression expression) {
            assignmentIsNotAnExpressionError(expression);
        }

        private void assignmentIsNotAnExpressionError(JetBinaryExpression expression) {
            semanticServices.getErrorHandler().genericError(expression.getNode(), "Assignments are not expressions, and only expressions are allowed in this context");
        }

        private JetType assureBooleanResult(JetSimpleNameExpression operationSign, String name, JetType resultType) {
            if (resultType != null) {
                // TODO : Relax?
                if (!isBoolean(resultType)) {
                    semanticServices.getErrorHandler().genericError(operationSign.getNode(), "'" + name + "' must return Boolean but returns " + resultType);
                    return null;
                } else {
                    return resultType;
                }
            }
            return resultType;
        }

        private boolean isBoolean(@NotNull JetType type) {
            TypeConstructor booleanTypeConstructor = semanticServices.getStandardLibrary().getBoolean().getTypeConstructor();
            return type.getConstructor().equals(booleanTypeConstructor) || ErrorType.isErrorType(type);
        }

        @Override
        public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
            JetExpression arrayExpression = expression.getArrayExpression();
            JetType receiverType = getType(scope, arrayExpression, false);
            List<JetExpression> indexExpressions = expression.getIndexExpressions();
            List<JetType> argumentTypes = getTypes(scope, indexExpressions);
            if (argumentTypes == null) return;

            FunctionDescriptor functionDescriptor = lookupFunction(scope, expression, "get", receiverType, argumentTypes, true);
            if (functionDescriptor != null) {
                result = functionDescriptor.getUnsubstitutedReturnType();
            }
        }

        @Nullable
        protected JetType getTypeForBinaryCall(JetBinaryExpression expression, @NotNull String name, JetScope scope, boolean reportUnresolved) {
            JetExpression left = expression.getLeft();
            JetExpression right = expression.getRight();
            if (right == null) {
                return null;
            }
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            return getTypeForBinaryCall(scope, left, operationSign, right, name, reportUnresolved);
        }

        @Nullable
        private JetType getTypeForBinaryCall(JetScope scope, JetExpression left, JetSimpleNameExpression operationSign, @NotNull JetExpression right, String name, boolean reportUnresolved) {
            JetType leftType = safeGetType(scope, left, false);
            JetType rightType = safeGetType(scope, right, false);
            if (ErrorType.isErrorType(leftType)) {
                return null;
            }
            FunctionDescriptor functionDescriptor = lookupFunction(scope, operationSign, name, leftType, Collections.singletonList(rightType), reportUnresolved);
            if (functionDescriptor != null) {
                return functionDescriptor.getUnsubstitutedReturnType();
            }
            return null;
        }

        @Override
        public void visitDeclaration(JetDeclaration dcl) {
            semanticServices.getErrorHandler().genericError(dcl.getNode(), "Declarations are not allowed in this position");
        }

        @Override
        public void visitJetElement(JetElement elem) {
            semanticServices.getErrorHandler().genericError(elem.getNode(), "Unsupported element: " + elem + " " + elem.getClass().getCanonicalName());
        }
    }

    private class TypeInferrerVisitorWithWritableScope extends TypeInferrerVisitor {
        private final WritableScope scope;

        public TypeInferrerVisitorWithWritableScope(@NotNull WritableScope scope, boolean preferBlock) {
            super(scope, preferBlock);
            this.scope = scope;
        }

        public void resetResult() {
            this.result = null;
        }

        @Override
        public void visitProperty(JetProperty property) {
            PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePropertyDescriptor(scope.getContainingDeclaration(), scope, property);
            scope.addPropertyDescriptor(propertyDescriptor);
            trace.recordDeclarationResolution(property, propertyDescriptor);
        }

        @Override
        public void visitFunction(JetFunction function) {
            super.visitFunction(function); // TODO
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
            JetExpression deparenthesized = deparenthesize(left);
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
                        semanticServices.getErrorHandler().typeMismatch(right, leftType, rightType);
                    }
                }
            }
            result = null; // This is not an expression
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
            semanticServices.getErrorHandler().genericError(elem.getNode(), "Unsupported element in a block: " + elem + " " + elem.getClass().getCanonicalName());
        }
    }
}
