package org.jetbrains.jet.lang.types;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeChecker {
    public static final JetTypeChecker INSTANCE = new JetTypeChecker();

    /*
       : if
       : when
       : try

       : SimpleName
       : "this" ("<" type ">")?

       : "new" constructorInvocation

       : objectLiteral

       : "typeof" "(" expression ")"

       : declaration
       : loop

       : functionLiteral

       : "namespace" // for the root namespace
     */
    public Type getType(@NotNull final JetScope scope, @NotNull JetExpression expression) {
        final Type[] result = new Type[1];
        expression.accept(new JetVisitor() {
            @Override
            public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
                result[0] = getType(scope, expression.getExpression());
            }

            @Override
            public void visitConstantExpression(JetConstantExpression expression) {
                IElementType elementType = expression.getNode().getElementType();
                if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
                    result[0] = JetStandardClasses.getIntType();
                } else if (elementType == JetNodeTypes.LONG_CONSTANT) {
                    result[0] = JetStandardClasses.getLongType();
                } else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
                    String text = expression.getText();
                    assert text.length() > 0;
                    char lastChar = text.charAt(text.length() - 1);
                    if (lastChar == 'f' || lastChar == 'F') {
                        result[0] = JetStandardClasses.getFloatType();
                    } else {
                        result[0] = JetStandardClasses.getDoubleType();
                    }
                } else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
                    result[0] = JetStandardClasses.getBooleanType();
                } else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
                    result[0] = JetStandardClasses.getCharType();
                } else if (elementType == JetNodeTypes.STRING_CONSTANT) {
                    result[0] = JetStandardClasses.getStringType();
                } else if (elementType == JetNodeTypes.NULL) {
                    result[0] = JetStandardClasses.getNullableNothingType();
                } else {
                    throw new IllegalArgumentException("Unsupported constant: " + expression);
                }
            }

            @Override
            public void visitThrowExpression(JetThrowExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitReturnExpression(JetReturnExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitBreakExpression(JetBreakExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitContinueExpression(JetContinueExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitTypeofExpression(JetTypeofExpression expression) {
                throw new UnsupportedOperationException("Return some reflection interface"); // TODO
            }

            @Override
            public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
                if (expression.getOperationSign() == JetTokens.COLON) {
                    Type actualType = getType(scope, expression.getLeft());
                    Type expectedType = TypeResolver.INSTANCE.resolveType(scope, expression.getRight());
                    if (isSubtypeOf(actualType, expectedType)) {
                        result[0] = expectedType;
                        return;
                    } else {
                         // TODO
                        throw new UnsupportedOperationException("Type mismatch: expected " + expectedType + " but found " + actualType);
                    }
                }
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public void visitIfExpression(JetIfExpression expression) {
                // TODO : check condition type
                // TODO : change types according to is and nullability according to null comparisons
                JetExpression elseBranch = expression.getElse();
                if (elseBranch == null) {
                    // TODO : type-check the branch
                    result[0] = JetStandardClasses.getUnitType();
                } else {
                    Type thenType = getType(scope, expression.getThen());
                    Type elseType = getType(scope, elseBranch);
                    result[0] = commonSupertype(thenType, elseType);
                }
            }

            @Override
            public void visitTupleExpression(JetTupleExpression expression) {
                List<JetExpression> entries = expression.getEntries();
                List<Type> types = new ArrayList<Type>();
                for (JetExpression entry : entries) {
                    types.add(getType(scope, entry));
                }
                // TODO : labels
                result[0] = JetStandardClasses.getTupleType(types);
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported element: " + elem);
            }
        });
        return result[0];
    }

    private Type commonSupertype(Type thenType, Type elseType) {
        if (JetStandardClasses.isNothing(thenType)) {
            return elseType;
        }
        if (JetStandardClasses.isNothing(elseType)) {
            return thenType;
        }

        List<Type> thenOrder = new LinkedList<Type>(); // adding to the beginning
        Map<TypeConstructor, Type> visited = new HashMap<TypeConstructor, Type>();
        topologicallySort(thenType, visited, thenOrder, null);

        List<Type> elseOrder = new LinkedList<Type>(); // adding to the beginning
        topologicallySort(elseType, new HashMap<TypeConstructor, Type>(), elseOrder, visited);

        assert !elseOrder.isEmpty() : "No common supertype";
        // TODO: support multiple common supertypes

        Type result = elseOrder.get(0);

        if (thenType.isNullable() || elseType.isNullable()) {
            return TypeUtils.makeNullable(result);
        }
        assert !result.isNullable();
        return result;
    }

    private void topologicallySort(Type current, Map<TypeConstructor, Type> visited, List<Type> topologicalOrder, @Nullable Map<TypeConstructor, Type> filter) {
        Type visitedOccurrence = visited.put(current.getConstructor(), current);
        if (visitedOccurrence != null) {
            assert equalTypes(visitedOccurrence, current);
            return;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = getSubstitutionContext(current);
        for (Type supertype : current.getConstructor().getSupertypes()) {
            TypeConstructor supertypeConstructor = supertype.getConstructor();
            if (visited.containsKey(supertypeConstructor)) {
                assert equalTypes(visited.get(supertypeConstructor), supertype);
                continue;
            }
            Type substitutedSupertype = substituteInType(substitutionContext, supertype);
            topologicallySort(substitutedSupertype, visited, topologicalOrder, filter);
        }
        if (filter != null && filter.containsKey(current.getConstructor())) {
            assert equalTypes(filter.get(current.getConstructor()), current) : filter.get(current.getConstructor()) + " != " + current;
            topologicalOrder.add(0, current);
        }
    }

    public boolean isConvertibleTo(JetExpression expression, Type type) {
        throw new UnsupportedOperationException(); // TODO
    }

    public boolean isSubtypeOf(Type subtype, Type supertype) {
        if (!supertype.isNullable() && subtype.isNullable()) {
            return false;
        }
        if (JetStandardClasses.isNothing(subtype)) {
            return true;
        }
        @Nullable Type closestSupertype = findCorrespondingSupertype(subtype, supertype);
        if (closestSupertype == null) {
            return false;
        }

        return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
    }

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    private Type findCorrespondingSupertype(Type subtype, Type supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        if (constructor.equals(supertype.getConstructor())) {
            return subtype;
        }
        for (Type immediateSupertype : constructor.getSupertypes()) {
            Type correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype);
            if (correspondingSupertype != null) {
                return substituteInType(getSubstitutionContext(subtype), correspondingSupertype);
            }
        }
        return null;
    }

    private Map<TypeConstructor, TypeProjection> getSubstitutionContext(Type context) {
        Map<TypeConstructor, TypeProjection> parameterValues = new HashMap<TypeConstructor, TypeProjection>();

        List<TypeParameterDescriptor> parameters = context.getConstructor().getParameters();
        List<TypeProjection> contextArguments = context.getArguments();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);
            TypeProjection value = contextArguments.get(i);
            parameterValues.put(parameter.getTypeConstructor(), value);
        }
        return parameterValues;
    }

    private Type substituteInType(Map<TypeConstructor, TypeProjection> substitutionContext, Type type) {
        TypeProjection value = substitutionContext.get(type.getConstructor());
        assert value == null : "For now this is used only for supertypes, thus no variables";

        return specializeType(type, substituteInArguments(substitutionContext, type));
    }

    @NotNull
    private TypeProjection substitute(Map<TypeConstructor, TypeProjection> parameterValues, TypeProjection subject) {
        ProjectionKind projectionKind = subject.getProjectionKind();
        if (projectionKind == ProjectionKind.NEITHER_OUT_NOR_IN) {
            return subject;
        }
        @NotNull Type subjectType = subject.getType();
        TypeProjection value = parameterValues.get(subjectType.getConstructor());
        if (value != null) {
            return value;
        }
        List<TypeProjection> newArguments = substituteInArguments(parameterValues, subjectType);
        return new TypeProjection(projectionKind, specializeType(subjectType, newArguments));
    }

    private List<TypeProjection> substituteInArguments(Map<TypeConstructor, TypeProjection> parameterValues, Type subjectType) {
        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        for (TypeProjection argument : subjectType.getArguments()) {
            newArguments.add(substitute(parameterValues, argument));
        }
        return newArguments;
    }

    private Type specializeType(Type type, List<TypeProjection> newArguments) {
        return new TypeImpl(type.getAttributes(), type.getConstructor(), type.isNullable(), newArguments, type.getMemberDomain());
    }

    private boolean checkSubtypeForTheSameConstructor(Type subtype, Type supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        assert constructor.equals(supertype.getConstructor());

        List<TypeProjection> subArguments = subtype.getArguments();
        List<TypeProjection> superArguments = supertype.getArguments();
        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);
            TypeProjection subArgument = subArguments.get(i);
            TypeProjection superArgument = superArguments.get(i);

            Type subArgumentType = subArgument.getType();
            Type superArgumentType = superArgument.getType();
            if (superArgument.getProjectionKind() != ProjectionKind.NEITHER_OUT_NOR_IN) {
                switch (parameter.getVariance()) {
                    case INVARIANT:
                        switch (superArgument.getProjectionKind()) {
                            case NO_PROJECTION:
                                if (!equalTypes(subArgumentType, superArgumentType)) {
                                    return false;
                                }
                                break;
                            case OUT_ONLY:
                                if (!subArgument.getProjectionKind().allowsOutCalls()) {
                                    return false;
                                }
                                if (!isSubtypeOf(subArgumentType, superArgumentType)) {
                                    return false;
                                }
                                break;
                            case IN_ONLY:
                                if (!subArgument.getProjectionKind().allowsInCalls()) {
                                    return false;
                                }
                                if (!isSubtypeOf(superArgumentType, subArgumentType)) {
                                    return false;
                                }
                                break;
                        }
                        break;
                    case IN_VARIANCE:
                        switch (superArgument.getProjectionKind()) {
                            case NO_PROJECTION:
                            case IN_ONLY:
                                if (!isSubtypeOf(superArgumentType, subArgumentType)) {
                                    return false;
                                }
                                break;
                            case OUT_ONLY:
                                if (!isSubtypeOf(subArgumentType, superArgumentType)) {
                                    return false;
                                }
                                break;
                        }
                        break;
                    case OUT_VARIANCE:
                        switch (superArgument.getProjectionKind()) {
                            case NO_PROJECTION:
                            case OUT_ONLY:
                            case IN_ONLY:
                                if (!isSubtypeOf(subArgumentType, superArgumentType)) {
                                    return false;
                                }
                                break;
                        }
                        break;
                }
            } else {
                // C< anything > is always a subtype of C<*>
            }
        }
        return true;
    }

    public boolean equalTypes(@NotNull Type type1, @NotNull Type type2) {
        if (!type1.getConstructor().equals(type2.getConstructor())) {
            return false;
        }
        List<TypeProjection> type1Arguments = type1.getArguments();
        List<TypeProjection> type2Arguments = type2.getArguments();
        if (type1Arguments.size() != type2Arguments.size()) {
            return false;
        }
        for (int i = 0; i < type1Arguments.size(); i++) {
            TypeProjection typeProjection1 = type1Arguments.get(i);
            TypeProjection typeProjection2 = type2Arguments.get(i);
            if (typeProjection1.getProjectionKind() != typeProjection2.getProjectionKind()) {
                return false;
            }
            if (typeProjection1.getProjectionKind() != ProjectionKind.NEITHER_OUT_NOR_IN) {
                if (!equalTypes(typeProjection1.getType(), typeProjection2.getType())) {
                    return false;
                }
            }
        }
        return true;
    }

}