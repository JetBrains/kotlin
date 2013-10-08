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
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.DFS;

import java.util.*;

public class TypeUtils {
    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");
    public static final JetType PLACEHOLDER_FUNCTION_TYPE = ErrorUtils.createErrorTypeWithCustomDebugName("PLACEHOLDER_FUNCTION_TYPE");

    public static final JetType CANT_INFER_LAMBDA_PARAM_TYPE = ErrorUtils.createErrorType("Cannot be inferred");

    public static class SpecialType implements JetType {
        private final String name;

        public SpecialType(String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            throw new IllegalStateException(name);
        }

        @Override
        public boolean isNullable() {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            throw new IllegalStateException(name);
        }

        @Override
        public boolean isError() {
            return false;
        }

        @NotNull
        @Override
        public Annotations getAnnotations() {
            throw new IllegalStateException(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final JetType NO_EXPECTED_TYPE = new SpecialType("NO_EXPECTED_TYPE");
    
    public static final JetType UNIT_EXPECTED_TYPE = new SpecialType("UNIT_EXPECTED_TYPE");

    public static boolean noExpectedType(@NotNull JetType type) {
        return type == NO_EXPECTED_TYPE || type == UNIT_EXPECTED_TYPE;
    }

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
        // Wrapping serves two purposes here
        // 1. It's requires less memory than copying with a changed nullability flag: a copy has many fields, while a wrapper has only one
        // 2. It preserves laziness of types

        // Unwrap to avoid long delegation call chains
        if (type instanceof AbstractTypeWithKnownNullability) {
            return makeNullableAsSpecified(((AbstractTypeWithKnownNullability) type).delegate, nullable);
        }

        // checking to preserve laziness
        if (!(type instanceof LazyType) && type.isNullable() == nullable) {
            return type;
        }

        return nullable ? new NullableType(type) : new NotNullType(type);
    }

    @NotNull
    public static JetType makeNullableIfNeeded(@NotNull JetType type, boolean nullable) {
        if (nullable) {
            return makeNullable(type);
        }
        return type;
    }

    public static boolean isIntersectionEmpty(@NotNull JetType typeA, @NotNull JetType typeB) {
        return intersect(JetTypeChecker.DEFAULT, Sets.newLinkedHashSet(Lists.newArrayList(typeA, typeB))) == null;
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


        TypeConstructor constructor = new IntersectionTypeConstructor(Annotations.EMPTY, resultingTypes);

        JetScope[] scopes = new JetScope[resultingTypes.size()];
        int i = 0;
        for (JetType type : resultingTypes) {
            scopes[i] = type.getMemberScope();
            i++;
        }

        return new JetTypeImpl(
                Annotations.EMPTY,
                constructor,
                allNullable,
                Collections.<TypeProjection>emptyList(),
                new ChainedScope(null, "member scope for intersection type " + constructor, scopes)); // TODO : check intersectibility, don't use a chanied scope
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
            ConstraintSystemImpl constraintSystem = new ConstraintSystemImpl();
            constraintSystem.registerTypeVariables(parameters);
            constraintSystem.addSubtypeConstraint(withParameters, expected, ConstraintPosition.SPECIAL);

            return constraintSystem.getStatus().isSuccessful();
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
        if (!type.getConstructor().isFinal()) {
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

    @NotNull
    public static JetType makeUnsubstitutedType(ClassDescriptor classDescriptor, JetScope unsubstitutedMemberScope) {
        if (ErrorUtils.isError(classDescriptor)) {
            return ErrorUtils.createErrorType("Unsubstituted type for " + classDescriptor);
        }
        TypeConstructor typeConstructor = classDescriptor.getTypeConstructor();
        List<TypeProjection> arguments = getDefaultTypeProjections(typeConstructor.getParameters());
        return new JetTypeImpl(
                Annotations.EMPTY,
                typeConstructor,
                false,
                arguments,
                unsubstitutedMemberScope
        );
    }

    @NotNull
    public static List<TypeProjection> getDefaultTypeProjections(List<TypeParameterDescriptor> parameters) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor parameterDescriptor : parameters) {
            result.add(new TypeProjectionImpl(parameterDescriptor.getDefaultType()));
        }
        return result;
    }

    private static void collectImmediateSupertypes(@NotNull JetType type, @NotNull Collection<JetType> result) {
        boolean isNullable = type.isNullable();
        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        for (JetType supertype : type.getConstructor().getSupertypes()) {
            JetType substitutedType = substitutor.substitute(supertype, Variance.INVARIANT);
            if (substitutedType != null) {
                result.add(makeNullableIfNeeded(substitutedType, isNullable));
            }
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
        // 15 is obtained by experimentation: JDK classes like ArrayList tend to have so many supertypes,
        // the average number is lower
        Set<JetType> result = new LinkedHashSet<JetType>(15);
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

    /**
     * A work-around of the generic nullability problem in the type checker
     * @return true if a value of this type can be null
     */
    public static boolean isNullableType(@NotNull JetType type) {
        if (type.isNullable()) {
            return true;
        }
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            return hasNullableSuperType(type);
        }
        return false;
    }

    public static boolean hasNullableSuperType(@NotNull JetType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
            // A class/trait cannot have a nullable supertype
            return false;
        }

        for (JetType supertype : getImmediateSupertypes(type)) {
            if (supertype.isNullable()) return true;
            if (hasNullableSuperType(supertype)) return true;
        }
        
        return false;
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
    public static JetType substituteParameters(@NotNull ClassDescriptor clazz, @NotNull List<JetType> typeArguments) {
        List<TypeProjection> projections = KotlinPackage.map(typeArguments, new Function1<JetType, TypeProjection>() {
            @Override
            public TypeProjection invoke(JetType type) {
                return new TypeProjectionImpl(type);
            }
        });

        return substituteProjectionsForParameters(clazz, projections);
    }

    @NotNull
    public static JetType substituteProjectionsForParameters(@NotNull ClassDescriptor clazz, @NotNull List<TypeProjection> projections) {
        List<TypeParameterDescriptor> clazzTypeParameters = clazz.getTypeConstructor().getParameters();
        if (clazzTypeParameters.size() != projections.size()) {
            throw new IllegalArgumentException("type parameter counts do not match: " + clazz + ", " + projections);
        }

        Map<TypeConstructor, TypeProjection> substitutions = Maps.newHashMap();

        for (int i = 0; i < clazzTypeParameters.size(); ++i) {
            TypeConstructor typeConstructor = clazzTypeParameters.get(i).getTypeConstructor();
            substitutions.put(typeConstructor, projections.get(i));
        }

        return TypeSubstitutor.create(substitutions).substitute(clazz.getDefaultType(), Variance.INVARIANT);
    }

    public static boolean equalTypes(@NotNull JetType a, @NotNull JetType b) {
        return JetTypeChecker.DEFAULT.isSubtypeOf(a, b) && JetTypeChecker.DEFAULT.isSubtypeOf(b, a);
    }

    public static boolean dependsOnTypeParameters(@NotNull JetType type, @NotNull Collection<TypeParameterDescriptor> typeParameters) {
        return dependsOnTypeConstructors(type, Collections2
                .transform(typeParameters, new Function<TypeParameterDescriptor, TypeConstructor>() {
                    @Override
                    public TypeConstructor apply(@Nullable TypeParameterDescriptor typeParameterDescriptor) {
                        assert typeParameterDescriptor != null;
                        return typeParameterDescriptor.getTypeConstructor();
                    }
                }));
    }

    public static boolean dependsOnTypeConstructors(@NotNull JetType type, @NotNull Collection<TypeConstructor> typeParameterConstructors) {
        if (typeParameterConstructors.contains(type.getConstructor())) return true;
        for (TypeProjection typeProjection : type.getArguments()) {
            if (dependsOnTypeConstructors(typeProjection.getType(), typeParameterConstructors)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSpecialType(@Nullable JetType type, @NotNull final JetType specialType) {
        return containsSpecialType(type, new Function1<JetType, Boolean>() {
            @Override
            public Boolean invoke(JetType type) {
                return specialType.equals(type);
            }
        });
    }

    public static boolean containsSpecialType(
            @Nullable JetType type,
            @NotNull Function1<JetType, Boolean> isSpecialType
    ) {
        if (type == null) return false;
        if (isSpecialType.invoke(type)) return true;
        if (type instanceof PackageType) return false;
        for (TypeProjection projection : type.getArguments()) {
            if (containsSpecialType(projection.getType(), isSpecialType)) return true;
        }
        return false;
    }

    @NotNull
    public static TypeProjection makeStarProjection(@NotNull TypeParameterDescriptor parameterDescriptor) {
        return new TypeProjectionImpl(parameterDescriptor.getVariance() == Variance.OUT_VARIANCE
                                      ? Variance.INVARIANT
                                      : Variance.OUT_VARIANCE, parameterDescriptor.getUpperBoundsAsType());
    }

    @Nullable
    public static JetType commonSupertypeForNumberTypes(@NotNull Collection<JetType> numberLowerBounds) {
        if (numberLowerBounds.isEmpty()) return null;
        Set<JetType> intersectionOfSupertypes = getIntersectionOfSupertypes(numberLowerBounds);
        JetType primitiveNumberType = getDefaultPrimitiveNumberType(intersectionOfSupertypes);
        if (primitiveNumberType != null) {
            return primitiveNumberType;
        }
        return CommonSupertypes.commonSupertype(numberLowerBounds);
    }

    @NotNull
    private static Set<JetType> getIntersectionOfSupertypes(@NotNull Collection<JetType> types) {
        Set<JetType> upperBounds = Sets.newHashSet();
        for (JetType type : types) {
            Collection<JetType> supertypes = type.getConstructor().getSupertypes();
            if (upperBounds.isEmpty()) {
                upperBounds.addAll(supertypes);
            }
            else {
                upperBounds.retainAll(supertypes);
            }
        }
        return upperBounds;
    }

    @NotNull
    public static JetType getDefaultPrimitiveNumberType(@NotNull IntegerValueTypeConstructor numberValueTypeConstructor) {
        JetType type = getDefaultPrimitiveNumberType(numberValueTypeConstructor.getSupertypes());
        assert type != null : "Strange number value type constructor: " + numberValueTypeConstructor + ". " +
                              "Super types doesn't contain double, int or long: " + numberValueTypeConstructor.getSupertypes();
        return type;
    }

    @Nullable
    private static JetType getDefaultPrimitiveNumberType(@NotNull Collection<JetType> supertypes) {
        JetType doubleType = KotlinBuiltIns.getInstance().getDoubleType();
        if (supertypes.contains(doubleType)) {
            return doubleType;
        }
        JetType intType = KotlinBuiltIns.getInstance().getIntType();
        if (supertypes.contains(intType)) {
            return intType;
        }
        JetType longType = KotlinBuiltIns.getInstance().getLongType();
        if (supertypes.contains(longType)) {
            return longType;
        }
        return null;
    }

    @NotNull
    public static JetType getPrimitiveNumberType(
            @NotNull IntegerValueTypeConstructor numberValueTypeConstructor,
            @NotNull JetType expectedType
    ) {
        if (noExpectedType(expectedType) || expectedType.isError()) {
            return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
        }
        for (JetType primitiveNumberType : numberValueTypeConstructor.getSupertypes()) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(primitiveNumberType, expectedType)) {
                return primitiveNumberType;
            }
        }
        return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
    }

    public static List<TypeConstructor> topologicallySortSuperclassesAndRecordAllInstances(
            @NotNull JetType type,
            @NotNull final Map<TypeConstructor, Set<JetType>> constructorToAllInstances,
            @NotNull final Set<TypeConstructor> visited
    ) {
        return DFS.dfs(
                Collections.singletonList(type),
                new DFS.Neighbors<JetType>() {
                    @NotNull
                    @Override
                    public Iterable<JetType> getNeighbors(JetType current) {
                        TypeSubstitutor substitutor = TypeSubstitutor.create(current);
                        List<JetType> result = Lists.newArrayList();
                        for (JetType supertype : current.getConstructor().getSupertypes()) {
                            if (visited.contains(supertype.getConstructor())) {
                                continue;
                            }
                            result.add(substitutor.safeSubstitute(supertype, Variance.INVARIANT));
                        }
                        return result;
                    }
                },
                new DFS.Visited<JetType>() {
                    @Override
                    public boolean checkAndMarkVisited(JetType current) {
                        return visited.add(current.getConstructor());
                    }
                },
                new DFS.NodeHandlerWithListResult<JetType, TypeConstructor>() {
                    @Override
                    public boolean beforeChildren(JetType current) {
                        TypeConstructor constructor = current.getConstructor();

                        Set<JetType> instances = constructorToAllInstances.get(constructor);
                        if (instances == null) {
                            instances = new HashSet<JetType>();
                            constructorToAllInstances.put(constructor, instances);
                        }
                        instances.add(current);

                        return true;
                    }

                    @Override
                    public void afterChildren(JetType current) {
                        result.addFirst(current.getConstructor());
                    }
                }
        );
    }

    public static TypeSubstitutor makeConstantSubstitutor(Collection<TypeParameterDescriptor> typeParameterDescriptors, JetType type) {
        final Set<TypeConstructor> constructors = Sets.newHashSet();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameterDescriptors) {
            constructors.add(typeParameterDescriptor.getTypeConstructor());
        }
        final TypeProjection projection = new TypeProjectionImpl(type);

        return TypeSubstitutor.create(new TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                if (constructors.contains(key)) {
                    return projection;
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
    }

    public static TypeSubstitutor makeSubstitutorForTypeParametersMap(
           @NotNull final Map<TypeParameterDescriptor, TypeProjection> substitutionContext
    ) {
        return TypeSubstitutor.create(new TypeSubstitution() {
            @Nullable
            @Override
            public TypeProjection get(TypeConstructor key) {
                DeclarationDescriptor declarationDescriptor = key.getDeclarationDescriptor();
                if (declarationDescriptor instanceof TypeParameterDescriptor) {
                    TypeParameterDescriptor descriptor = (TypeParameterDescriptor) declarationDescriptor;
                    return substitutionContext.get(descriptor);
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return substitutionContext.isEmpty();
            }

            @Override
            public String toString() {
                return substitutionContext.toString();
            }
        });
    }

    private static abstract class AbstractTypeWithKnownNullability extends AbstractJetType {
        private final JetType delegate;

        private AbstractTypeWithKnownNullability(@NotNull JetType delegate) {
            this.delegate = delegate;
        }

        @Override
        @NotNull
        public TypeConstructor getConstructor() {
            return delegate.getConstructor();
        }

        @Override
        @NotNull
        public List<TypeProjection> getArguments() {
            return delegate.getArguments();
        }

        @Override
        public abstract boolean isNullable();

        @Override
        @NotNull
        public JetScope getMemberScope() {
            return delegate.getMemberScope();
        }

        @Override
        public boolean isError() {
            return delegate.isError();
        }

        @Override
        @NotNull
        public Annotations getAnnotations() {
            return delegate.getAnnotations();
        }
    }

    private static class NullableType extends AbstractTypeWithKnownNullability {

        private NullableType(@NotNull JetType delegate) {
            super(delegate);
        }

        @Override
        public boolean isNullable() {
            return true;
        }
    }

    private static class NotNullType extends AbstractTypeWithKnownNullability {

        private NotNullType(@NotNull JetType delegate) {
            super(delegate);
        }

        @Override
        public boolean isNullable() {
            return false;
        }
    }

}
