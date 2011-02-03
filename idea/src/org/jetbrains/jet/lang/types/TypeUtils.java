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
    public static Type makeNullable(Type type) {
        if (type.isNullable()) {
            return type;
        }
        return new TypeImpl(type.getAttributes(), type.getConstructor(), true, type.getArguments(), type.getMemberDomain());
    }

    @Nullable
    public static Type intersect(Set<Type> types) {
        assert !types.isEmpty();

        if (types.size() == 1) {
            return types.iterator().next();
        }

        StringBuilder debugName = new StringBuilder();
        boolean nullable = false;
        for (Iterator<Type> iterator = types.iterator(); iterator.hasNext();) {
            Type type = iterator.next();

            if (!canHaveSubtypes(type)) {
                for (Type other : types) {
                    if (type != other || !JetTypeChecker.INSTANCE.isSubtypeOf(type, other)) {
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
        TypeConstructor constructor = new TypeConstructor(noAttributes, false, debugName.toString(), Collections.<TypeParameterDescriptor>emptyList(), types);
        return new TypeImpl(
                noAttributes,
                constructor,
                nullable,
                Collections.<TypeProjection>emptyList(),
                JetStandardClasses.STUB);
    }

    private static boolean canHaveSubtypes(Type type) {
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
            Type argument = typeProjection.getType();

            switch (parameterDescriptor.getVariance()) {
                case INVARIANT:
                    switch (projectionKind) {
                        case INVARIANT:
                            if (lowerThanBound(argument, parameterDescriptor) || canHaveSubtypes(argument)) {
                                return true;
                            }
                            break;
                        case IN_VARIANCE:
                            if (lowerThanBound(argument, parameterDescriptor)) {
                                return true;
                            }
                            break;
                        case OUT_VARIANCE:
                            if (canHaveSubtypes(argument)) {
                                return true;
                            }
                            break;
                    }
                    break;
                case IN_VARIANCE:
                    if (projectionKind != Variance.OUT_VARIANCE) {
                        if (lowerThanBound(argument, parameterDescriptor)) {
                            return true;
                        }
                    } else {
                        if (canHaveSubtypes(argument)) {
                            return true;
                        }
                    }
                    break;
                case OUT_VARIANCE:
                    if (projectionKind != Variance.IN_VARIANCE) {
                        if (canHaveSubtypes(argument)) {
                            return true;
                        }
                    } else {
                        if (lowerThanBound(argument, parameterDescriptor)) {
                            return true;
                        }
                    }
                    break;
            }
        }
        return false;
    }

    private static boolean lowerThanBound(Type argument, TypeParameterDescriptor parameterDescriptor) {
        for (Type bound : parameterDescriptor.getUpperBounds()) {
            if (JetTypeChecker.INSTANCE.isSubtypeOf(argument, bound)) {
                if (!argument.getConstructor().equals(bound.getConstructor())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Type makeNullableIfNeeded(Type type, boolean nullable) {
        if (nullable) {
            return makeNullable(type);
        }
        return type;
    }
}
