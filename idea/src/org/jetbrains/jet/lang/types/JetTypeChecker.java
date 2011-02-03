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
                // TODO : change types according to is and nullability checks
                JetExpression elseBranch = expression.getElse();
                if (elseBranch == null) {
                    // TODO : type-check the branch
                    result[0] = JetStandardClasses.getUnitType();
                } else {
                    Type thenType = getType(scope, expression.getThen());
                    Type elseType = getType(scope, elseBranch);
                    result[0] = commonSupertype(Arrays.asList(thenType, elseType));
                }
            }

            @Override
            public void visitWhenExpression(JetWhenExpression expression) {
                // TODO :change scope according to the bound value in the when header
                List<Type> expressions = new ArrayList<Type>();
                collectAllReturnTypes(expression, scope, expressions);
                result[0] = commonSupertype(expressions);
            }

            @Override
            public void visitTryExpression(JetTryExpression expression) {
                JetExpression tryBlock = expression.getTryBlock();
                List<JetCatchClause> catchClauses = expression.getCatchClauses();
                JetFinallySection finallyBlock = expression.getFinallyBlock();
                List<Type> types = new ArrayList<Type>();
                if (finallyBlock == null) {
                    for (JetCatchClause catchClause : catchClauses) {
                        // TODO: change scope here
                        types.add(getType(scope, catchClause.getCatchBody()));
                    }
                } else {
                    types.add(getType(scope, finallyBlock.getFinalExpression()));
                }
                types.add(getType(scope, tryBlock));
                result[0] = commonSupertype(types);
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
            public void visitBlockExpression(JetBlockExpression expression) {
                // TODO : this is a stub, consider function literals
                List<JetExpression> statements = expression.getStatements();
                if (statements.isEmpty()) {
                    result[0] = JetStandardClasses.getUnitType();
                } else {
                    result[0] = getType(scope, statements.get(statements.size() - 1));
                }
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported element: " + elem);
            }
        });
        return result[0];
    }

    private void collectAllReturnTypes(JetWhenExpression whenExpression, JetScope scope, List<Type> result) {
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetWhenExpression subWhen = entry.getSubWhen();
            if (subWhen != null) {
                collectAllReturnTypes(subWhen, scope, result);
            } else {
                JetExpression resultExpression = entry.getExpression();
                if (resultExpression != null) {
                    result.add(getType(scope, resultExpression));
                }
            }
        }
    }

    public Type commonSupertype(Collection<Type> types) {
        Collection<Type> typeSet = new HashSet<Type>(types);
        assert !typeSet.isEmpty();
        boolean nullable = false;
        for (Iterator<Type> iterator = typeSet.iterator(); iterator.hasNext();) {
            Type type = iterator.next();
            if (JetStandardClasses.isNothing(type)) {
                iterator.remove();
            }
            nullable |= type.isNullable();
        }

        if (typeSet.isEmpty()) {
            // TODO : attributes
            return nullable ? JetStandardClasses.getNullableNothingType() : JetStandardClasses.getNothingType();
        }

        if (typeSet.size() == 1) {
            return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
        }

        Map<TypeConstructor, Set<Type>> commonSupertypes = computeCommonRawSupertypes(typeSet);
        while (commonSupertypes.size() > 1) {
            HashSet<Type> merge = new HashSet<Type>();
            for (Set<Type> supertypes : commonSupertypes.values()) {
                merge.addAll(supertypes);
            }
            commonSupertypes = computeCommonRawSupertypes(merge);
        }
        assert !commonSupertypes.isEmpty();
        Map.Entry<TypeConstructor, Set<Type>> entry = commonSupertypes.entrySet().iterator().next();
        Type result = computeSupertypeProjections(entry.getKey(), entry.getValue());

        return TypeUtils.makeNullableIfNeeded(result, nullable);
    }

    private Type computeSupertypeProjections(TypeConstructor constructor, Set<Type> types) {
        // we assume that all the given types are applications of the same type constructor

        assert !types.isEmpty();

        if (types.size() == 1) {
            return types.iterator().next();
        }

        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        List<TypeProjection> newProjections = new ArrayList<TypeProjection>();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameterDescriptor = parameters.get(i);
            Set<TypeProjection> typeProjections = new HashSet<TypeProjection>();
            for (Type type : types) {
                typeProjections.add(type.getArguments().get(i));
            }
            newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections));
        }

        boolean nullable = false;
        for (Type type : types) {
            nullable |= type.isNullable();
        }

        // TODO : attributes?
        return new TypeImpl(Collections.<Attribute>emptyList(), constructor, nullable, newProjections, JetStandardClasses.STUB);
    }

    private TypeProjection computeSupertypeProjection(TypeParameterDescriptor parameterDescriptor, Set<TypeProjection> typeProjections) {
        if (typeProjections.size() == 1) {
            return typeProjections.iterator().next();
        }

        Set<Type> ins = new HashSet<Type>();
        Set<Type> outs = new HashSet<Type>();

        Variance variance = parameterDescriptor.getVariance();
        switch (variance) {
            case INVARIANT:
                // Nothing
                break;
            case IN_VARIANCE:
                outs = null;
                break;
            case OUT_VARIANCE:
                ins = null;
                break;
        }

        for (TypeProjection projection : typeProjections) {
            Variance projectionKind = projection.getProjectionKind();
            if (projectionKind.allowsInPosition()) {
                if (ins != null) {
                    ins.add(projection.getType());
                }
            } else {
                ins = null;
            }

            if (projectionKind.allowsOutPosition()) {
                if (outs != null) {
                    outs.add(projection.getType());
                }
            } else {
                outs = null;
            }
        }

        if (ins != null) {
            Variance projectionKind = variance == Variance.IN_VARIANCE ? Variance.INVARIANT : Variance.IN_VARIANCE;
            Type intersection = TypeUtils.intersect(ins);
            if (intersection == null) {
                return new TypeProjection(Variance.OUT_VARIANCE, commonSupertype(parameterDescriptor.getUpperBounds()));
            }
            return new TypeProjection(projectionKind, intersection);
        } else if (outs != null) {
            Variance projectionKind = variance == Variance.OUT_VARIANCE ? Variance.INVARIANT : Variance.OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(outs));
        } else {
            Variance projectionKind = variance == Variance.OUT_VARIANCE ? Variance.INVARIANT : Variance.OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(parameterDescriptor.getUpperBounds()));
        }
    }

    private Map<TypeConstructor, Set<Type>> computeCommonRawSupertypes(Collection<Type> types) {
        assert !types.isEmpty();

        final Map<TypeConstructor, Set<Type>> constructorToAllInstances = new HashMap<TypeConstructor, Set<Type>>();
        Set<TypeConstructor> commonSuperclasses = null;

        List<TypeConstructor> order = null;
        for (Iterator<Type> iterator = types.iterator(); iterator.hasNext();) {
            Type type = iterator.next();

            Set<TypeConstructor> visited = new HashSet<TypeConstructor>();

            order = dfs(type, visited, new DfsNodeHandler<List<TypeConstructor>>() {
                public LinkedList<TypeConstructor> list = new LinkedList<TypeConstructor>();

                @Override
                public void beforeChildren(Type current) {
                    TypeConstructor constructor = current.getConstructor();

                    Set<Type> instances = constructorToAllInstances.get(constructor);
                    if (instances == null) {
                        instances = new HashSet<Type>();
                        constructorToAllInstances.put(constructor, instances);
                    }
                    instances.add(current);
                }

                @Override
                public void afterChildren(Type current) {
                    list.addFirst(current.getConstructor());
                }

                @Override
                public List<TypeConstructor> result() {
                    return list;
                }
            });

            if (commonSuperclasses == null) {
                commonSuperclasses = visited;
            } else {
                commonSuperclasses.retainAll(visited);
            }
        }
        assert order != null;

        Set<TypeConstructor> notSource = new HashSet<TypeConstructor>();
        Map<TypeConstructor, Set<Type>> result = new HashMap<TypeConstructor, Set<Type>>();
        for (TypeConstructor superConstructor : order) {
            if (!commonSuperclasses.contains(superConstructor)) {
                continue;
            }

            if (!notSource.contains(superConstructor)) {
                result.put(superConstructor, constructorToAllInstances.get(superConstructor));
                markAll(superConstructor, notSource);
            }
        }

        return result;
    }

    private void markAll(TypeConstructor typeConstructor, Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (Type type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }

    private <R> R dfs(Type current, Set<TypeConstructor> visited, DfsNodeHandler<R> handler) {
        doDfs(current, visited, handler);
        return handler.result();
    }

    private void doDfs(Type current, Set<TypeConstructor> visited, DfsNodeHandler<?> handler) {
        if (!visited.add(current.getConstructor())) {
            return;
        }
        handler.beforeChildren(current);
        Map<TypeConstructor, TypeProjection> substitutionContext = getSubstitutionContext(current);
        for (Type supertype : current.getConstructor().getSupertypes()) {
            TypeConstructor supertypeConstructor = supertype.getConstructor();
            if (visited.contains(supertypeConstructor)) {
                continue;
            }
            Type substitutedSupertype = substituteInType(substitutionContext, supertype);
            dfs(substitutedSupertype, visited, handler);
        }
        handler.afterChildren(current);
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
        @NotNull Type subjectType = subject.getType();
        TypeProjection value = parameterValues.get(subjectType.getConstructor());
        if (value != null) {
            return value;
        }
        List<TypeProjection> newArguments = substituteInArguments(parameterValues, subjectType);
        return new TypeProjection(subject.getProjectionKind(), specializeType(subjectType, newArguments));
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
            switch (parameter.getVariance()) {
                case INVARIANT:
                    switch (superArgument.getProjectionKind()) {
                        case INVARIANT:
                            if (!equalTypes(subArgumentType, superArgumentType)) {
                                return false;
                            }
                            break;
                        case OUT_VARIANCE:
                            if (!subArgument.getProjectionKind().allowsOutPosition()) {
                                return false;
                            }
                            if (!isSubtypeOf(subArgumentType, superArgumentType)) {
                                return false;
                            }
                            break;
                        case IN_VARIANCE:
                            if (!subArgument.getProjectionKind().allowsInPosition()) {
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
                        case INVARIANT:
                        case IN_VARIANCE:
                            if (!isSubtypeOf(superArgumentType, subArgumentType)) {
                                return false;
                            }
                            break;
                        case OUT_VARIANCE:
                            if (!isSubtypeOf(subArgumentType, superArgumentType)) {
                                return false;
                            }
                            break;
                    }
                    break;
                case OUT_VARIANCE:
                    switch (superArgument.getProjectionKind()) {
                        case INVARIANT:
                        case OUT_VARIANCE:
                        case IN_VARIANCE:
                            if (!isSubtypeOf(subArgumentType, superArgumentType)) {
                                return false;
                            }
                            break;
                    }
                    break;
            }
        }
        return true;
    }

    public boolean equalTypes(@NotNull Type type1, @NotNull Type type2) {
        return TypeImpl.equalTypes(type1, type2);
    }

}