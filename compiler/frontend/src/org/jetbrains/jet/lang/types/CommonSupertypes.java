package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;

import java.util.*;

import static org.jetbrains.jet.lang.types.Variance.IN_VARIANCE;
import static org.jetbrains.jet.lang.types.Variance.OUT_VARIANCE;

/**
* @author abreslav
*/
public class CommonSupertypes {
    @NotNull
    public static JetType commonSupertype(@NotNull Collection<JetType> types) {
        Collection<JetType> typeSet = new HashSet<JetType>(types);
        assert !typeSet.isEmpty();

        // If any of the types is nullable, the result must be nullable
        // This also removed Nothing and Nothing? because they are subtypes of everything else
        boolean nullable = false;
        for (Iterator<JetType> iterator = typeSet.iterator(); iterator.hasNext();) {
            JetType type = iterator.next();
            assert type != null;
            if (JetStandardClasses.isNothingOrNullableNothing(type)) {
                iterator.remove();
            }
            nullable |= type.isNullable();
        }

        // Everything deleted => it's Nothing or Nothing?
        if (typeSet.isEmpty()) {
            // TODO : attributes
            return nullable ? JetStandardClasses.getNullableNothingType() : JetStandardClasses.getNothingType();
        }

        if (typeSet.size() == 1) {
            return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
        }

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map<TypeConstructor, Set<JetType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
        while (commonSupertypes.size() > 1) {
            Set<JetType> merge = new HashSet<JetType>();
            for (Set<JetType> supertypes : commonSupertypes.values()) {
                merge.addAll(supertypes);
            }
            commonSupertypes = computeCommonRawSupertypes(merge);
        }
        assert !commonSupertypes.isEmpty() : commonSupertypes + " <- " + types;

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map.Entry<TypeConstructor, Set<JetType>> entry = commonSupertypes.entrySet().iterator().next();

        // Reconstructing type arguments if possible
        JetType result = computeSupertypeProjections(entry.getKey(), entry.getValue());
        return TypeUtils.makeNullableIfNeeded(result, nullable);
    }

    // Raw supertypes are superclasses w/o type arguments
    // @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
    @NotNull
    private static Map<TypeConstructor, Set<JetType>> computeCommonRawSupertypes(@NotNull Collection<JetType> types) {
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

    // constructor - type constructor of a supertype to be instantiated
    // types - instantiations of constructor occurring as supertypes of classes we are trying to intersect
    @NotNull
    private static JetType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<JetType> types) {
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
        return new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), constructor, nullable, newProjections, JetStandardClasses.STUB); // TODO : scope
    }

    @NotNull
    private static TypeProjection computeSupertypeProjection(@NotNull TypeParameterDescriptor parameterDescriptor, @NotNull Set<TypeProjection> typeProjections) {
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
            JetType intersection = TypeUtils.intersect(JetTypeChecker.INSTANCE, ins);
            if (intersection == null) {
                if (outs != null) {
                    return new TypeProjection(OUT_VARIANCE, commonSupertype(outs));
                }
                return new TypeProjection(OUT_VARIANCE, commonSupertype(parameterDescriptor.getUpperBounds()));
            }
            Variance projectionKind = variance == IN_VARIANCE ? Variance.INVARIANT : IN_VARIANCE;
            return new TypeProjection(projectionKind, intersection);
        } else if (outs != null) {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(outs));
        } else {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(parameterDescriptor.getUpperBounds()));
        }
    }

    private static void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (JetType type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }

    private static <R> R dfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<R> handler) {
        doDfs(current, visited, handler);
        return handler.result();
    }

    private static void doDfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<?> handler) {
        if (!visited.add(current.getConstructor())) {
            return;
        }
        handler.beforeChildren(current);
//        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(current);
        TypeSubstitutor substitutor = TypeSubstitutor.create(current);
        for (JetType supertype : current.getConstructor().getSupertypes()) {
            TypeConstructor supertypeConstructor = supertype.getConstructor();
            if (visited.contains(supertypeConstructor)) {
                continue;
            }
            JetType substitutedSupertype = substitutor.safeSubstitute(supertype, Variance.INVARIANT);
            dfs(substitutedSupertype, visited, handler);
        }
        handler.afterChildren(current);
    }

    private static class DfsNodeHandler<R> {

        public void beforeChildren(JetType current) {

        }

        public void afterChildren(JetType current) {

        }

        public R result() {
            return null;
        }
    }
}
