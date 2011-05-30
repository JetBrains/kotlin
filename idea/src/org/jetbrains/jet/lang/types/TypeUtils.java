package org.jetbrains.jet.lang.types;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Annotation;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.JetScope;

import java.util.*;

/**
 * @author abreslav
 */
public class TypeUtils {
    public static JetType makeNullable(@NotNull JetType type) {
        return makeNullableAsSpecified(type, true);
    }

    public static JetType makeNotNullable(@NotNull JetType type) {
        return makeNullableAsSpecified(type, false);
    }

    @NotNull
    public static JetType makeNullableAsSpecified(@NotNull JetType type, boolean nullable) {
        if (type.isNullable() == nullable) {
            return type;
        }
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(), nullable, type.getArguments(), type.getMemberScope());
    }

    @NotNull
    public static JetType safeIntersect(JetTypeChecker typeChecker, Set<JetType> types) {
        JetType intersection = intersect(typeChecker, types);
        if (intersection == null) return ErrorUtils.createErrorType("No intersection for " + types); // TODO : message
        return intersection;
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
                    if (!type.equals(other) && !typeChecker.isSubtypeOf(type, other)) {
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

        List<Annotation> noAnnotations = Collections.<Annotation>emptyList();
        TypeConstructor constructor = new TypeConstructorImpl(null, noAnnotations, false, debugName.toString(), Collections.<TypeParameterDescriptor>emptyList(), types);
        return new JetTypeImpl(
                noAnnotations,
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

    @NotNull
    public static JetType makeUnsubstitutedType(ClassDescriptor classDescriptor, JetScope unsubstitutedMemberScope) {
        List<TypeProjection> arguments = getArguments(classDescriptor);
        return new JetTypeImpl(
                Collections.<Annotation>emptyList(),
                classDescriptor.getTypeConstructor(),
                false,
                arguments,
                unsubstitutedMemberScope
        );
    }

    @NotNull
    private static List<TypeProjection> getArguments(@NotNull ClassDescriptor classDescriptor) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor parameterDescriptor : classDescriptor.getTypeConstructor().getParameters()) {
            result.add(new TypeProjection(new JetTypeImpl(parameterDescriptor.getTypeConstructor(), JetScope.EMPTY))); // TODO : scope?
        }
        return result;
    }

    @NotNull
    public static Map<TypeConstructor, TypeProjection> buildSubstitutionContext(@NotNull  JetType context) {
        return buildSubstitutionContext(context.getConstructor().getParameters(), context.getArguments());
    }

    @NotNull
    public static Map<TypeConstructor, TypeProjection> buildSubstitutionContext(@NotNull List<TypeParameterDescriptor> parameters, @NotNull List<TypeProjection> contextArguments) {
        Map<TypeConstructor, TypeProjection> parameterValues = new HashMap<TypeConstructor, TypeProjection>();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);
            TypeProjection value = contextArguments.get(i);
            parameterValues.put(parameter.getTypeConstructor(), value);
        }
        return parameterValues;
    }

    @NotNull
    public static TypeProjection makeStarProjection(@NotNull TypeParameterDescriptor parameterDescriptor) {
        return new TypeProjection(Variance.OUT_VARIANCE, parameterDescriptor.getBoundsAsType());
    }

    private static void collectImmediateSupertypes(@NotNull JetType type, @NotNull Collection<JetType> result) {
        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        for (JetType supertype : type.getConstructor().getSupertypes()) {
            result.add(substitutor.substitute(supertype, Variance.INVARIANT));
        }
    }

    @NotNull
    public static List<JetType> getImmediateSupertypes(@NotNull JetType type) {
        List<JetType> result = Lists.newArrayList();
        collectImmediateSupertypes(type, result);
        return result;
    }

    private static void collectAllSupertypes(@NotNull JetType type, @NotNull Set<JetType> result) {
        List<JetType> immediateSupertypes = getImmediateSupertypes(type);
        result.addAll(immediateSupertypes);
        for (JetType supertype : immediateSupertypes) {
            collectAllSupertypes(supertype, result);
        }
    }


    @NotNull
    public static Set<JetType> getAllSupertypes(@NotNull JetType type) {
        Set<JetType> result = Sets.newLinkedHashSet();
        collectAllSupertypes(type, result);
        return result;
    }

    public static boolean hasNullableBound(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        for (JetType bound : typeParameterDescriptor.getUpperBounds()) {
            if (bound.isNullable()) {
                return true;
            }
        }
        return false;
    }
}
