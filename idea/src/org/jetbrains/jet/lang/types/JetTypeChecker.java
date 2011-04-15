package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeChecker {

    private final Map<TypeConstructor, Set<TypeConstructor>> conversionMap = new HashMap<TypeConstructor, Set<TypeConstructor>>();
    private final JetStandardLibrary standardLibrary;

    public JetTypeChecker(JetStandardLibrary standardLibrary) {
        this.standardLibrary = standardLibrary;
    }

    @NotNull
    private Map<TypeConstructor, Set<TypeConstructor>> getConversionMap() {
//        if (conversionMap.size() == 0) {
//            addConversion(standardLibrary.getByte(),
//                    standardLibrary.getShort(),
//                    standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getShort(),
//                    standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getChar(),
//                    standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//        }
        return conversionMap;
    }

//    private void addConversion(ClassDescriptor actual, ClassDescriptor... convertedTo) {
//        TypeConstructor[] constructors = new TypeConstructor[convertedTo.length];
//        for (int i = 0, convertedToLength = convertedTo.length; i < convertedToLength; i++) {
//            ClassDescriptor classDescriptor = convertedTo[i];
//            constructors[i] = classDescriptor.getTypeConstructor();
//        }
//        conversionMap.put(actual.getTypeConstructor(), new HashSet<TypeConstructor>(Arrays.asList(constructors)));
//    }
//
    @NotNull
    public JetType commonSupertype(@NotNull JetType... types) {
        return commonSupertype(Arrays.asList(types));
    }

    @NotNull
    public JetType commonSupertype(@NotNull Collection<JetType> types) {
        Collection<JetType> typeSet = new HashSet<JetType>(types);
        assert !typeSet.isEmpty();
        boolean nullable = false;
        for (Iterator<JetType> iterator = typeSet.iterator(); iterator.hasNext();) {
            JetType type = iterator.next();
            // TODO : This admits 'Nothing?'. Review
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

        Map<TypeConstructor, Set<JetType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
        while (commonSupertypes.size() > 1) {
            HashSet<JetType> merge = new HashSet<JetType>();
            for (Set<JetType> supertypes : commonSupertypes.values()) {
                merge.addAll(supertypes);
            }
            commonSupertypes = computeCommonRawSupertypes(merge);
        }
        assert !commonSupertypes.isEmpty() : commonSupertypes;
        Map.Entry<TypeConstructor, Set<JetType>> entry = commonSupertypes.entrySet().iterator().next();
        JetType result = computeSupertypeProjections(entry.getKey(), entry.getValue());

        return TypeUtils.makeNullableIfNeeded(result, nullable);
    }

    @NotNull
    private JetType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<JetType> types) {
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
            for (JetType type : types) {
                typeProjections.add(type.getArguments().get(i));
            }
            newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections));
        }

        boolean nullable = false;
        for (JetType type : types) {
            nullable |= type.isNullable();
        }

        // TODO : attributes?
        return new JetTypeImpl(Collections.<Attribute>emptyList(), constructor, nullable, newProjections, JetStandardClasses.STUB);
    }

    @NotNull
    private TypeProjection computeSupertypeProjection(@NotNull TypeParameterDescriptor parameterDescriptor, @NotNull Set<TypeProjection> typeProjections) {
        if (typeProjections.size() == 1) {
            return typeProjections.iterator().next();
        }

        Set<JetType> ins = new HashSet<JetType>();
        Set<JetType> outs = new HashSet<JetType>();

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
            JetType intersection = TypeUtils.intersect(this, ins);
            if (intersection == null) {
                if (outs != null) {
                    return new TypeProjection(Variance.OUT_VARIANCE, commonSupertype(outs));
                }
                return new TypeProjection(Variance.OUT_VARIANCE, commonSupertype(parameterDescriptor.getUpperBounds()));
            }
            Variance projectionKind = variance == Variance.IN_VARIANCE ? Variance.INVARIANT : Variance.IN_VARIANCE;
            return new TypeProjection(projectionKind, intersection);
        } else if (outs != null) {
            Variance projectionKind = variance == Variance.OUT_VARIANCE ? Variance.INVARIANT : Variance.OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(outs));
        } else {
            Variance projectionKind = variance == Variance.OUT_VARIANCE ? Variance.INVARIANT : Variance.OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(parameterDescriptor.getUpperBounds()));
        }
    }

    @NotNull
    private Map<TypeConstructor, Set<JetType>> computeCommonRawSupertypes(@NotNull Collection<JetType> types) {
        assert !types.isEmpty();

        final Map<TypeConstructor, Set<JetType>> constructorToAllInstances = new HashMap<TypeConstructor, Set<JetType>>();
        Set<TypeConstructor> commonSuperclasses = null;

        List<TypeConstructor> order = null;
        for (JetType type : types) {
            Set<TypeConstructor> visited = new HashSet<TypeConstructor>();

            order = dfs(type, visited, new DfsNodeHandler<List<TypeConstructor>>() {
                public LinkedList<TypeConstructor> list = new LinkedList<TypeConstructor>();

                @Override
                public void beforeChildren(JetType current) {
                    TypeConstructor constructor = current.getConstructor();

                    Set<JetType> instances = constructorToAllInstances.get(constructor);
                    if (instances == null) {
                        instances = new HashSet<JetType>();
                        constructorToAllInstances.put(constructor, instances);
                    }
                    instances.add(current);
                }

                @Override
                public void afterChildren(JetType current) {
                    list.addFirst(current.getConstructor());
                }

                @Override
                public List<TypeConstructor> result() {
                    return list;
                }
            });

            if (commonSuperclasses == null) {
                commonSuperclasses = visited;
            }
            else {
                commonSuperclasses.retainAll(visited);
            }
        }
        assert order != null;

        Set<TypeConstructor> notSource = new HashSet<TypeConstructor>();
        Map<TypeConstructor, Set<JetType>> result = new HashMap<TypeConstructor, Set<JetType>>();
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

    private void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (JetType type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }

    private <R> R dfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<R> handler) {
        doDfs(current, visited, handler);
        return handler.result();
    }

    private void doDfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<?> handler) {
        if (!visited.add(current.getConstructor())) {
            return;
        }
        handler.beforeChildren(current);
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(current);
        for (JetType supertype : current.getConstructor().getSupertypes()) {
            TypeConstructor supertypeConstructor = supertype.getConstructor();
            if (visited.contains(supertypeConstructor)) {
                continue;
            }
            JetType substitutedSupertype = TypeSubstitutor.INSTANCE.safeSubstitute(substitutionContext, supertype, Variance.INVARIANT);
            dfs(substitutedSupertype, visited, handler);
        }
        handler.afterChildren(current);
    }

    public boolean isConvertibleTo(@NotNull JetType actual, @NotNull JetType expected) {
        return isSubtypeOf(actual, expected) ||
               isConvertibleBySpecialConversion(actual, expected);
    }

    public boolean isConvertibleBySpecialConversion(@NotNull JetType actual, @NotNull JetType expected) {
        if (expected.getConstructor().equals(JetStandardClasses.getTuple(0).getTypeConstructor())) {
            return true;
        }
//        if (actual.getValueArguments().isEmpty()) {
//            TypeConstructor actualConstructor = actual.getConstructor();
//            TypeConstructor constructor = expected.getConstructor();
//            Set<TypeConstructor> convertibleTo = getConversionMap().get(actualConstructor);
//            if (convertibleTo != null) {
//                return convertibleTo.contains(constructor);
//            }
//        }
        return false;
    }

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
        if (ErrorUtils.isErrorType(subtype) || ErrorUtils.isErrorType(supertype)) {
            return true;
        }
        if (!supertype.isNullable() && subtype.isNullable()) {
            return false;
        }
        if (JetStandardClasses.isNothing(subtype)) {
            return true;
        }
        @Nullable JetType closestSupertype = findCorrespondingSupertype(subtype, supertype);
        if (closestSupertype == null) {
            return false;
        }

        return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
    }

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    private JetType findCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        if (constructor.equals(supertype.getConstructor())) {
            return subtype;
        }
        for (JetType immediateSupertype : constructor.getSupertypes()) {
            JetType correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype);
            if (correspondingSupertype != null) {
                return TypeSubstitutor.INSTANCE.safeSubstitute(subtype, correspondingSupertype, Variance.INVARIANT);
            }
        }
        return null;
    }

    private boolean checkSubtypeForTheSameConstructor(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        assert constructor.equals(supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

        List<TypeProjection> subArguments = subtype.getArguments();
        List<TypeProjection> superArguments = supertype.getArguments();
        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);
            TypeProjection subArgument = subArguments.get(i);
            TypeProjection superArgument = superArguments.get(i);

            JetType subArgumentType = subArgument.getType();
            JetType superArgumentType = superArgument.getType();
            switch (parameter.getVariance()) {
                case INVARIANT:
                    switch (superArgument.getProjectionKind()) {
                        case INVARIANT:
                            if (!JetTypeImpl.equalTypes(subArgumentType, superArgumentType)) {
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
}