package org.jetbrains.jet.lang.types;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class JetTypeChecker {
    public static final JetTypeChecker INSTANCE = new JetTypeChecker();

    public Type getType(JetExpression expression) {
        final Type[] result = new Type[1];
        expression.accept(new JetVisitor() {
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
            public void visitTupleExpression(JetTupleExpression expression) {
                List<JetExpression> entries = expression.getEntries();
                List<Type> types = new ArrayList<Type>();
                for (JetExpression entry : entries) {
                    types.add(getType(entry));
                }
                result[0] = JetStandardClasses.getTupleType(types);
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported element: " + elem);
            }
        });
        return result[0];
    }

    public boolean isConvertibleTo(JetExpression expression, Type type) {
        throw new UnsupportedOperationException(); // TODO
    }

    public boolean isSubtypeOf(Type subtype, Type supertype) {
        if (!supertype.isNullable() && subtype.isNullable()) {
            return false;
        }
        if (subtype.getConstructor() == JetStandardClasses.getNothing().getTypeConstructor()) {
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
                return substituteForParameters(subtype, correspondingSupertype);
            }
        }
        return null;
    }

    private Type substituteForParameters(Type context, Type subject) {
        Map<TypeConstructor, TypeProjection> parameterValues = new HashMap<TypeConstructor, TypeProjection>();

        List<TypeParameterDescriptor> parameters = context.getConstructor().getParameters();
        List<TypeProjection> contextArguments = context.getArguments();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);
            TypeProjection value = contextArguments.get(i);
            parameterValues.put(parameter.getTypeConstructor(), value);
        }

        return substitute(parameterValues, subject);
    }

    @NotNull
    private Type substitute(Map<TypeConstructor, TypeProjection> parameterValues, Type subject) {
        TypeProjection value = parameterValues.get(subject.getConstructor());
        if (value != null) {
            return value.getType();
        }
        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        for (TypeProjection argument : subject.getArguments()) {
            newArguments.add(new TypeProjection(argument.getProjectionKind(), substitute(parameterValues, argument.getType())));
        }
        return specializeType(subject, newArguments);
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

    public boolean equalTypes(Type type1, Type type2) {
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
            if (!equalTypes(typeProjection1.getType(), typeProjection2.getType())) {
                return false;
            }
        }
        return true;
    }

}