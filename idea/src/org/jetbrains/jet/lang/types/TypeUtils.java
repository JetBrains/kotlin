package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class TypeUtils {
    public static JetType makeNullable(JetType type) {
        if (type.isNullable()) {
            return type;
        }
        return new JetTypeImpl(type.getAttributes(), type.getConstructor(), true, type.getArguments(), type.getMemberScope());
    }

    public static JetType makeNotNullable(JetType type) {
        if (!type.isNullable()) {
            return type;
        }
        return new JetTypeImpl(type.getAttributes(), type.getConstructor(), false, type.getArguments(), type.getMemberScope());
    }

    @Nullable
    public static JetType intersect(JetTypeChecker typeChecker, Set<JetType> types) {
        assert !types.isEmpty();

        if (types.size() == 1) {
            return types.iterator().next();
        }

        StringBuilder debugName = new StringBuilder();
        boolean nullable = false;
        for (Iterator<JetType> iterator = types.iterator(); iterator.hasNext();) {
            JetType type = iterator.next();

            if (!canHaveSubtypes(typeChecker, type)) {
                for (JetType other : types) {
                    if (type != other || !typeChecker.isSubtypeOf(type, other)) {
                        return null;
                    }
                }
                return type;
            }

            nullable |= type.isNullable();

            debugName.append(type.toString());
            if (iterator.hasNext()) {
                debugName.append(" & ");
            }
        }

        List<Attribute> noAttributes = Collections.<Attribute>emptyList();
        TypeConstructor constructor = new TypeConstructorImpl(null, noAttributes, false, debugName.toString(), Collections.<TypeParameterDescriptor>emptyList(), types);
        return new JetTypeImpl(
                noAttributes,
                constructor,
                nullable,
                Collections.<TypeProjection>emptyList(),
                JetStandardClasses.STUB);
    }

    private static boolean canHaveSubtypes(JetTypeChecker typeChecker, JetType type) {
        if (type.isNullable()) {
            return true;
        }
        if (!type.getConstructor().isSealed()) {
            return true;
        }

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        List<TypeProjection> arguments = type.getArguments();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameterDescriptor = parameters.get(i);
            TypeProjection typeProjection = arguments.get(i);
            Variance projectionKind = typeProjection.getProjectionKind();
            JetType argument = typeProjection.getType();

            switch (parameterDescriptor.getVariance()) {
                case INVARIANT:
                    switch (projectionKind) {
                        case INVARIANT:
                            if (lowerThanBound(typeChecker, argument, parameterDescriptor) || canHaveSubtypes(typeChecker, argument)) {
                                return true;
                            }
                            break;
                        case IN_VARIANCE:
                            if (lowerThanBound(typeChecker, argument, parameterDescriptor)) {
                                return true;
                            }
                            break;
                        case OUT_VARIANCE:
                            if (canHaveSubtypes(typeChecker, argument)) {
                                return true;
                            }
                            break;
                    }
                    break;
                case IN_VARIANCE:
                    if (projectionKind != Variance.OUT_VARIANCE) {
                        if (lowerThanBound(typeChecker, argument, parameterDescriptor)) {
                            return true;
                        }
                    } else {
                        if (canHaveSubtypes(typeChecker, argument)) {
                            return true;
                        }
                    }
                    break;
                case OUT_VARIANCE:
                    if (projectionKind != Variance.IN_VARIANCE) {
                        if (canHaveSubtypes(typeChecker, argument)) {
                            return true;
                        }
                    } else {
                        if (lowerThanBound(typeChecker, argument, parameterDescriptor)) {
                            return true;
                        }
                    }
                    break;
            }
        }
        return false;
    }

    private static boolean lowerThanBound(JetTypeChecker typeChecker, JetType argument, TypeParameterDescriptor parameterDescriptor) {
        for (JetType bound : parameterDescriptor.getUpperBounds()) {
            if (typeChecker.isSubtypeOf(argument, bound)) {
                if (!argument.getConstructor().equals(bound.getConstructor())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static JetType makeNullableIfNeeded(JetType type, boolean nullable) {
        if (nullable) {
            return makeNullable(type);
        }
        return type;
    }
}
