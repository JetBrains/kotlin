/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.types;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintResolutionListener;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemSolution;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemWithPriorities;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintType;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public class TypeUtils {
    public static final JetType NO_EXPECTED_TYPE = new JetType() {
        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isNullable() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            throw new IllegalStateException();
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            throw new IllegalStateException();
        }

        @Override
        public String toString() {
            return "NO_EXPECTED_TYPE";
        }
    };

    @NotNull
    public static JetType makeNullable(@NotNull JetType type) {
        return makeNullableAsSpecified(type, true);
    }

    @NotNull
    public static JetType makeNotNullable(@NotNull JetType type) {
        return makeNullableAsSpecified(type, false);
    }

    @NotNull
    public static JetType makeNullableAsSpecified(@NotNull JetType type, boolean nullable) {
        if (type.isNullable() == nullable) {
            return type;
        }
        if (ErrorUtils.isErrorType(type)) {
            return type;
        }
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(), nullable, type.getArguments(), type.getMemberScope());
    }

    public static boolean isIntersectionEmpty(@NotNull JetType typeA, @NotNull JetType typeB) {
        return intersect(JetTypeChecker.INSTANCE, Sets.newLinkedHashSet(Lists.newArrayList(typeA, typeB))) == null;
    }

    @Nullable
    public static JetType intersect(@NotNull JetTypeChecker typeChecker, @NotNull Set<JetType> types) {
        if (types.isEmpty()) {
            return KotlinBuiltIns.getInstance().getNullableAnyType();
        }

        if (types.size() == 1) {
            return types.iterator().next();
        }

        // Intersection of T1..Tn is an intersection of their non-null versions,
        //   made nullable is they all were nullable
        boolean allNullable = true;
        boolean nothingTypePresent = false;
        List<JetType> nullabilityStripped = Lists.newArrayList();
        for (JetType type : types) {
            nothingTypePresent |= KotlinBuiltIns.getInstance().isNothingOrNullableNothing(type);
            allNullable &= type.isNullable();
            nullabilityStripped.add(makeNotNullable(type));
        }
        
        if (nothingTypePresent) {
            return allNullable ? KotlinBuiltIns.getInstance().getNullableNothingType() : KotlinBuiltIns.getInstance().getNothingType();
        }

        // Now we remove types that have subtypes in the list
        List<JetType> resultingTypes = Lists.newArrayList();
        outer:
        for (JetType type : nullabilityStripped) {
            if (!canHaveSubtypes(typeChecker, type)) {
                for (JetType other : nullabilityStripped) {
                    // It makes sense to check for subtyping (other <: type), despite that
                    // type is not supposed to be open, for there're enums
                    if (!TypeUnifier.mayBeEqual(type, other) && !typeChecker.isSubtypeOf(type, other) && !typeChecker.isSubtypeOf(other, type)) {
                        return null;
                    }
                }
                return makeNullableAsSpecified(type, allNullable);
            }
            else {
                for (JetType other : nullabilityStripped) {
                    if (!type.equals(other) && typeChecker.isSubtypeOf(other, type)) {
                        continue outer;
                    }

                }
            }

            // Don't add type if it is already present, to avoid trivial type intersections in result
            for (JetType other : resultingTypes) {
                if (typeChecker.equalTypes(other, type)) {
                    continue outer;
                }
            }
            resultingTypes.add(type);
        }
        
        if (resultingTypes.size() == 1) {
            return makeNullableAsSpecified(resultingTypes.get(0), allNullable);
        }


        List<AnnotationDescriptor> noAnnotations = Collections.<AnnotationDescriptor>emptyList();
        TypeConstructor constructor = new IntersectionTypeConstructor(
                noAnnotations,
                resultingTypes);

        JetScope[] scopes = new JetScope[resultingTypes.size()];
        int i = 0;
        for (JetType type : resultingTypes) {
            scopes[i] = type.getMemberScope();
            i++;
        }

        return new JetTypeImpl(
                noAnnotations,
                constructor,
                allNullable,
                Collections.<TypeProjection>emptyList(),
                new ChainedScope(null, scopes)); // TODO : check intersectibility, don't use a chanied scope
    }

    private static class TypeUnifier {
        private static class TypeParameterUsage {
            private final TypeParameterDescriptor typeParameterDescriptor;
            private final Variance howTheTypeParameterIsUsed;

            public TypeParameterUsage(TypeParameterDescriptor typeParameterDescriptor, Variance howTheTypeParameterIsUsed) {
                this.typeParameterDescriptor = typeParameterDescriptor;
                this.howTheTypeParameterIsUsed = howTheTypeParameterIsUsed;
            }
        }

        public static boolean mayBeEqual(@NotNull JetType type, @NotNull JetType other) {
            return unify(type, other);
        }

        private static boolean unify(JetType withParameters, JetType expected) {
            ConstraintSystemWithPriorities constraintSystem = new ConstraintSystemWithPriorities(ConstraintResolutionListener.DO_NOTHING);
            // T -> how T is used
            final Map<TypeParameterDescriptor, Variance> parameters = Maps.newHashMap();
            Processor<TypeParameterUsage> processor = new Processor<TypeParameterUsage>() {
                @Override
                public boolean process(TypeParameterUsage parameterUsage) {
                    Variance howTheTypeIsUsedBefore = parameters.get(parameterUsage.typeParameterDescriptor);
                    if (howTheTypeIsUsedBefore == null) {
                        howTheTypeIsUsedBefore = Variance.INVARIANT;
                    }
                    parameters.put(parameterUsage.typeParameterDescriptor,
                                   parameterUsage.howTheTypeParameterIsUsed.superpose(howTheTypeIsUsedBefore));
                    return true;
                }
            };
            processAllTypeParameters(withParameters, Variance.INVARIANT, processor);
            processAllTypeParameters(expected, Variance.INVARIANT, processor);
            for (Map.Entry<TypeParameterDescriptor, Variance> entry : parameters.entrySet()) {
                constraintSystem.registerTypeVariable(entry.getKey(), entry.getValue());
            }
            constraintSystem.addSubtypingConstraint(ConstraintType.VALUE_ARGUMENT.assertSubtyping(withParameters, expected));

            ConstraintSystemSolution solution = constraintSystem.solve();
            return solution.getStatus().isSuccessful();
        }

        private static void processAllTypeParameters(JetType type, Variance howThiTypeIsUsed, Processor<TypeParameterUsage> result) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof TypeParameterDescriptor) {
                result.process(new TypeParameterUsage((TypeParameterDescriptor)descriptor, howThiTypeIsUsed));
            }
            for (TypeProjection projection : type.getArguments()) {
                processAllTypeParameters(projection.getType(), projection.getProjectionKind(), result);
            }
        }
    }

    public static boolean canHaveSubtypes(JetTypeChecker typeChecker, JetType type) {
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
                    }
                    else {
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
                    }
                    else {
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
        if (ErrorUtils.isError(classDescriptor)) {
            return ErrorUtils.createErrorType("Unsubstituted type for " + classDescriptor);
        }
        List<TypeProjection> arguments = getDefaultTypeProjections(classDescriptor.getTypeConstructor().getParameters());
        return new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                classDescriptor.getTypeConstructor(),
                false,
                arguments,
                unsubstitutedMemberScope
        );
    }

    @NotNull
    public static List<TypeProjection> getDefaultTypeProjections(List<TypeParameterDescriptor> parameters) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor parameterDescriptor : parameters) {
            result.add(new TypeProjection(parameterDescriptor.getDefaultType()));
        }
        return result;
    }

    @NotNull
    public static List<JetType> getDefaultTypes(List<TypeParameterDescriptor> parameters) {
        List<JetType> result = Lists.newArrayList();
        for (TypeParameterDescriptor parameterDescriptor : parameters) {
            result.add(parameterDescriptor.getDefaultType());
        }
        return result;
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

    public static boolean hasNullableLowerBound(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        for (JetType bound : typeParameterDescriptor.getLowerBounds()) {
            if (bound.isNullable()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNullableSuperType(@NotNull JetType type) {
        for (JetType supertype : getAllSupertypes(type)) {
            if (supertype.isNullable()) {
                return true;
            }
        }
        return false;
    }

    public static boolean equalClasses(@NotNull JetType type1, @NotNull JetType type2) {
        DeclarationDescriptor declarationDescriptor1 = type1.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor1 == null) return false; // No class, classes are not equal
        DeclarationDescriptor declarationDescriptor2 = type2.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor2 == null) return false; // Class of type1 is not null
        return declarationDescriptor1.getOriginal().equals(declarationDescriptor2.getOriginal());
    }

    @Nullable
    public static ClassDescriptor getClassDescriptor(@NotNull JetType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof ClassDescriptor) {
            return (ClassDescriptor) declarationDescriptor;
        }
        return null;
    }

    @NotNull
    public static JetType substituteParameters(@NotNull ClassDescriptor clazz, @NotNull List<JetType> actualTypeParameters) {
        List<TypeParameterDescriptor> clazzTypeParameters = clazz.getTypeConstructor().getParameters();

        if (clazzTypeParameters.size() != actualTypeParameters.size()) {
            throw new IllegalArgumentException("type parameter counts do not match: " + clazz + ", " + actualTypeParameters);
        }
        
        Map<TypeConstructor, TypeProjection> substitutions = Maps.newHashMap();
        
        for (int i = 0; i < clazzTypeParameters.size(); ++i) {
            TypeConstructor typeConstructor = clazzTypeParameters.get(i).getTypeConstructor();
            TypeProjection typeProjection = new TypeProjection(actualTypeParameters.get(i));
            substitutions.put(typeConstructor, typeProjection);
        }
        
        return TypeSubstitutor.create(substitutions).substitute(clazz.getDefaultType(), Variance.INVARIANT);
    }

    private static void addAllClassDescriptors(@NotNull JetType type, @NotNull Set<ClassDescriptor> set) {
        ClassDescriptor cd = getClassDescriptor(type);
        if (cd != null) {
            set.add(cd);
        }
        for (TypeProjection projection : type.getArguments()) {
            addAllClassDescriptors(projection.getType(), set);
        }
    }

    @NotNull
    public static List<ClassDescriptor> getAllClassDescriptors(@NotNull JetType type) {
        Set<ClassDescriptor> classDescriptors = new HashSet<ClassDescriptor>();
        addAllClassDescriptors(type, classDescriptors);
        return new ArrayList<ClassDescriptor>(classDescriptors);
    }

    public static boolean equalTypes(@NotNull JetType a, @NotNull JetType b) {
        return JetTypeChecker.INSTANCE.isSubtypeOf(a, b) && JetTypeChecker.INSTANCE.isSubtypeOf(b, a);
    }

    public static boolean typeConstructorUsedInType(@NotNull TypeConstructor key, @NotNull JetType value) {
        if (value.getConstructor() == key) return true;
        for (TypeProjection projection : value.getArguments()) {
            if (typeConstructorUsedInType(key, projection.getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean dependsOnTypeParameters(@NotNull JetType type, @NotNull Collection<TypeParameterDescriptor> typeParameters) {
        return dependsOnTypeParameterConstructors(type, Collections2
                .transform(typeParameters, new Function<TypeParameterDescriptor, TypeConstructor>() {
                    @Override
                    public TypeConstructor apply(@Nullable TypeParameterDescriptor typeParameterDescriptor) {
                        assert typeParameterDescriptor != null;
                        return typeParameterDescriptor.getTypeConstructor();
                    }
                }));
    }

    public static boolean dependsOnTypeParameterConstructors(@NotNull JetType type, @NotNull Collection<TypeConstructor> typeParameterConstructors) {
        if (typeParameterConstructors.contains(type.getConstructor())) return true;
        for (TypeProjection typeProjection : type.getArguments()) {
            if (dependsOnTypeParameterConstructors(typeProjection.getType(), typeParameterConstructors)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equalsOrContainsAsArgument(@Nullable JetType type, @NotNull JetType... possibleArgumentTypes) {
        return equalsOrContainsAsArgument(type, Sets.newHashSet(possibleArgumentTypes));
    }

    private static boolean equalsOrContainsAsArgument(@Nullable JetType type, @NotNull Set<JetType> possibleArgumentTypes) {
        if (type == null) return false;
        if (possibleArgumentTypes.contains(type)) return true;
        if (type instanceof NamespaceType) return false;
        for (TypeProjection projection : type.getArguments()) {
            if (equalsOrContainsAsArgument(projection.getType(), possibleArgumentTypes)) return true;
        }
        return false;
    }

    @NotNull
    public static String getTypeNameAndStarProjectionsString(@NotNull String name, int size) {
        StringBuilder builder = new StringBuilder(name);
        builder.append("<");
        for (int i = 0; i < size; i++) {
            builder.append("*");
            if (i == size - 1) break;
            builder.append(", ");
        }
        builder.append(">");

        return builder.toString();
    }
}
