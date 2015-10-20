/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.scopes.KtScope;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.utils.DFS;

import java.util.*;

public class TypeUtils {
    public static final KtType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");
    public static final KtType PLACEHOLDER_FUNCTION_TYPE = ErrorUtils.createErrorTypeWithCustomDebugName("PLACEHOLDER_FUNCTION_TYPE");

    public static final KtType CANT_INFER_FUNCTION_PARAM_TYPE = ErrorUtils.createErrorType("Cannot be inferred");

    public static class SpecialType implements KtType {
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

        @NotNull
        @Override
        public TypeSubstitution getSubstitution() {
            throw new IllegalStateException(name);
        }

        @Override
        public boolean isMarkedNullable() {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public KtScope getMemberScope() {
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

        @Nullable
        @Override
        public <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass) {
            return null;
        }

        @NotNull
        @Override
        public TypeCapabilities getCapabilities() {
            return TypeCapabilities.NONE.INSTANCE;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @NotNull
    public static final KtType NO_EXPECTED_TYPE = new SpecialType("NO_EXPECTED_TYPE");

    public static final KtType UNIT_EXPECTED_TYPE = new SpecialType("UNIT_EXPECTED_TYPE");

    public static boolean noExpectedType(@NotNull KtType type) {
        return type == NO_EXPECTED_TYPE || type == UNIT_EXPECTED_TYPE;
    }

    public static boolean isDontCarePlaceholder(@Nullable KtType type) {
        return type != null && type.getConstructor() == DONT_CARE.getConstructor();
    }

    @NotNull
    public static KtType makeNullable(@NotNull KtType type) {
        return makeNullableAsSpecified(type, true);
    }

    @NotNull
    public static KtType makeNotNullable(@NotNull KtType type) {
        return makeNullableAsSpecified(type, false);
    }

    @NotNull
    public static KtType makeNullableAsSpecified(@NotNull KtType type, boolean nullable) {
        NullAwareness nullAwareness = type.getCapability(NullAwareness.class);
        if (nullAwareness != null) {
            return nullAwareness.makeNullableAsSpecified(nullable);
        }

        // Wrapping serves two purposes here
        // 1. It's requires less memory than copying with a changed nullability flag: a copy has many fields, while a wrapper has only one
        // 2. It preserves laziness of types

        // Unwrap to avoid long delegation call chains
        if (type instanceof AbstractTypeWithKnownNullability) {
            return makeNullableAsSpecified(((AbstractTypeWithKnownNullability) type).delegate, nullable);
        }

        // checking to preserve laziness
        if (!(type instanceof LazyType) && type.isMarkedNullable() == nullable) {
            return type;
        }

        return nullable ? new NullableType(type) : new NotNullType(type);
    }

    @NotNull
    public static KtType makeNullableIfNeeded(@NotNull KtType type, boolean nullable) {
        if (nullable) {
            return makeNullable(type);
        }
        return type;
    }

    public static boolean canHaveSubtypes(KotlinTypeChecker typeChecker, @NotNull KtType type) {
        if (type.isMarkedNullable()) {
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
            KtType argument = typeProjection.getType();

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

    private static boolean lowerThanBound(KotlinTypeChecker typeChecker, KtType argument, TypeParameterDescriptor parameterDescriptor) {
        for (KtType bound : parameterDescriptor.getUpperBounds()) {
            if (typeChecker.isSubtypeOf(argument, bound)) {
                if (!argument.getConstructor().equals(bound.getConstructor())) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public static KtType makeUnsubstitutedType(ClassDescriptor classDescriptor, KtScope unsubstitutedMemberScope) {
        if (ErrorUtils.isError(classDescriptor)) {
            return ErrorUtils.createErrorType("Unsubstituted type for " + classDescriptor);
        }
        TypeConstructor typeConstructor = classDescriptor.getTypeConstructor();
        List<TypeProjection> arguments = getDefaultTypeProjections(typeConstructor.getParameters());
        return KtTypeImpl.create(
                Annotations.Companion.getEMPTY(),
                typeConstructor,
                false,
                arguments,
                unsubstitutedMemberScope
        );
    }

    @NotNull
    public static List<TypeProjection> getDefaultTypeProjections(@NotNull List<TypeParameterDescriptor> parameters) {
        List<TypeProjection> result = new ArrayList<TypeProjection>(parameters.size());
        for (TypeParameterDescriptor parameterDescriptor : parameters) {
            result.add(new TypeProjectionImpl(parameterDescriptor.getDefaultType()));
        }
        return org.jetbrains.kotlin.utils.CollectionsKt.toReadOnlyList(result);
    }

    @NotNull
    public static List<KtType> getImmediateSupertypes(@NotNull KtType type) {
        boolean isNullable = type.isMarkedNullable();
        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        Collection<KtType> originalSupertypes = type.getConstructor().getSupertypes();
        List<KtType> result = new ArrayList<KtType>(originalSupertypes.size());
        for (KtType supertype : originalSupertypes) {
            KtType substitutedType = substitutor.substitute(supertype, Variance.INVARIANT);
            if (substitutedType != null) {
                result.add(makeNullableIfNeeded(substitutedType, isNullable));
            }
        }
        return result;
    }

    private static void collectAllSupertypes(@NotNull KtType type, @NotNull Set<KtType> result) {
        List<KtType> immediateSupertypes = getImmediateSupertypes(type);
        result.addAll(immediateSupertypes);
        for (KtType supertype : immediateSupertypes) {
            collectAllSupertypes(supertype, result);
        }
    }


    @NotNull
    public static Set<KtType> getAllSupertypes(@NotNull KtType type) {
        // 15 is obtained by experimentation: JDK classes like ArrayList tend to have so many supertypes,
        // the average number is lower
        Set<KtType> result = new LinkedHashSet<KtType>(15);
        collectAllSupertypes(type, result);
        return result;
    }

    public static boolean hasNullableLowerBound(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        for (KtType bound : typeParameterDescriptor.getLowerBounds()) {
            if (bound.isMarkedNullable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A work-around of the generic nullability problem in the type checker
     * Semantics should be the same as `!isSubtype(T, Any)`
     * @return true if a value of this type can be null
     */
    public static boolean isNullableType(@NotNull KtType type) {
        if (type.isMarkedNullable()) {
            return true;
        }
        if (FlexibleTypesKt.isFlexible(type) && isNullableType(FlexibleTypesKt.flexibility(type).getUpperBound())) {
            return true;
        }
        if (isTypeParameter(type)) {
            return hasNullableSuperType(type);
        }
        return false;
    }

    /**
     * Differs from `isNullableType` only by treating type parameters: acceptsNullable(T) <=> T has nullable lower bound
     * Semantics should be the same as `isSubtype(Nothing?, T)`
     * @return true if `null` can be assigned to storage of this type
     */
    public static boolean acceptsNullable(@NotNull KtType type) {
        if (type.isMarkedNullable()) {
            return true;
        }
        if (FlexibleTypesKt.isFlexible(type) && acceptsNullable(FlexibleTypesKt.flexibility(type).getUpperBound())) {
            return true;
        }
        if (isTypeParameter(type)) {
            return hasNullableLowerBound((TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor());
        }
        return false;
    }

    public static boolean hasNullableSuperType(@NotNull KtType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
            // A class/trait cannot have a nullable supertype
            return false;
        }

        for (KtType supertype : getImmediateSupertypes(type)) {
            if (supertype.isMarkedNullable()) return true;
            if (hasNullableSuperType(supertype)) return true;
        }

        return false;
    }

    @Nullable
    public static ClassDescriptor getClassDescriptor(@NotNull KtType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof ClassDescriptor) {
            return (ClassDescriptor) declarationDescriptor;
        }
        return null;
    }

    @NotNull
    public static KtType substituteParameters(@NotNull ClassDescriptor clazz, @NotNull List<KtType> typeArguments) {
        List<TypeProjection> projections = CollectionsKt.map(typeArguments, new Function1<KtType, TypeProjection>() {
            @Override
            public TypeProjection invoke(KtType type) {
                return new TypeProjectionImpl(type);
            }
        });

        return substituteProjectionsForParameters(clazz, projections);
    }

    @NotNull
    public static KtType substituteProjectionsForParameters(@NotNull ClassDescriptor clazz, @NotNull List<TypeProjection> projections) {
        List<TypeParameterDescriptor> clazzTypeParameters = clazz.getTypeConstructor().getParameters();
        if (clazzTypeParameters.size() != projections.size()) {
            throw new IllegalArgumentException("type parameter counts do not match: " + clazz + ", " + projections);
        }

        Map<TypeConstructor, TypeProjection> substitutions = org.jetbrains.kotlin.utils.CollectionsKt
                .newHashMapWithExpectedSize(clazzTypeParameters.size());

        for (int i = 0; i < clazzTypeParameters.size(); ++i) {
            TypeConstructor typeConstructor = clazzTypeParameters.get(i).getTypeConstructor();
            substitutions.put(typeConstructor, projections.get(i));
        }

        return TypeSubstitutor.create(substitutions).substitute(clazz.getDefaultType(), Variance.INVARIANT);
    }

    public static boolean equalTypes(@NotNull KtType a, @NotNull KtType b) {
        return KotlinTypeChecker.DEFAULT.isSubtypeOf(a, b) && KotlinTypeChecker.DEFAULT.isSubtypeOf(b, a);
    }

    public static boolean dependsOnTypeParameters(@NotNull KtType type, @NotNull Collection<TypeParameterDescriptor> typeParameters) {
        return dependsOnTypeConstructors(type, CollectionsKt.map(
                typeParameters,
                new Function1<TypeParameterDescriptor, TypeConstructor>() {
                    @Override
                    public TypeConstructor invoke(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
                        return typeParameterDescriptor.getTypeConstructor();
                    }
                }
        ));
    }

    public static boolean dependsOnTypeConstructors(@NotNull KtType type, @NotNull Collection<TypeConstructor> typeParameterConstructors) {
        if (typeParameterConstructors.contains(type.getConstructor())) return true;
        for (TypeProjection typeProjection : type.getArguments()) {
            if (!typeProjection.isStarProjection() && dependsOnTypeConstructors(typeProjection.getType(), typeParameterConstructors)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSpecialType(@Nullable KtType type, @NotNull final KtType specialType) {
        return containsSpecialType(type, new Function1<KtType, Boolean>() {
            @Override
            public Boolean invoke(KtType type) {
                return specialType.equals(type);
            }
        });
    }

    public static boolean containsSpecialType(
            @Nullable KtType type,
            @NotNull Function1<KtType, Boolean> isSpecialType
    ) {
        if (type == null) return false;
        if (isSpecialType.invoke(type)) return true;
        Flexibility flexibility = type.getCapability(Flexibility.class);
        if (flexibility != null
                && (containsSpecialType(flexibility.getLowerBound(), isSpecialType) || containsSpecialType(flexibility.getUpperBound(), isSpecialType))) {
            return true;
        }
        for (TypeProjection projection : type.getArguments()) {
            if (!projection.isStarProjection() && containsSpecialType(projection.getType(), isSpecialType)) return true;
        }
        return false;
    }

    @NotNull
    public static TypeProjection makeStarProjection(@NotNull TypeParameterDescriptor parameterDescriptor) {
        return new StarProjectionImpl(parameterDescriptor);
    }

    @Nullable
    public static KtType commonSupertypeForNumberTypes(@NotNull Collection<KtType> numberLowerBounds) {
        if (numberLowerBounds.isEmpty()) return null;
        Set<KtType> intersectionOfSupertypes = getIntersectionOfSupertypes(numberLowerBounds);
        KtType primitiveNumberType = getDefaultPrimitiveNumberType(intersectionOfSupertypes);
        if (primitiveNumberType != null) {
            return primitiveNumberType;
        }
        return CommonSupertypes.commonSupertype(numberLowerBounds);
    }

    @NotNull
    private static Set<KtType> getIntersectionOfSupertypes(@NotNull Collection<KtType> types) {
        Set<KtType> upperBounds = new HashSet<KtType>();
        for (KtType type : types) {
            Collection<KtType> supertypes = type.getConstructor().getSupertypes();
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
    public static KtType getDefaultPrimitiveNumberType(@NotNull IntegerValueTypeConstructor numberValueTypeConstructor) {
        KtType type = getDefaultPrimitiveNumberType(numberValueTypeConstructor.getSupertypes());
        assert type != null : "Strange number value type constructor: " + numberValueTypeConstructor + ". " +
                              "Super types doesn't contain double, int or long: " + numberValueTypeConstructor.getSupertypes();
        return type;
    }

    @Nullable
    private static KtType getDefaultPrimitiveNumberType(@NotNull Collection<KtType> supertypes) {
        if (supertypes.isEmpty()) {
            return null;
        }

        KotlinBuiltIns builtIns = supertypes.iterator().next().getConstructor().getBuiltIns();
        KtType doubleType = builtIns.getDoubleType();
        if (supertypes.contains(doubleType)) {
            return doubleType;
        }
        KtType intType = builtIns.getIntType();
        if (supertypes.contains(intType)) {
            return intType;
        }
        KtType longType = builtIns.getLongType();
        if (supertypes.contains(longType)) {
            return longType;
        }
        return null;
    }

    @NotNull
    public static KtType getPrimitiveNumberType(
            @NotNull IntegerValueTypeConstructor numberValueTypeConstructor,
            @NotNull KtType expectedType
    ) {
        if (noExpectedType(expectedType) || expectedType.isError()) {
            return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
        }
        for (KtType primitiveNumberType : numberValueTypeConstructor.getSupertypes()) {
            if (KotlinTypeChecker.DEFAULT.isSubtypeOf(primitiveNumberType, expectedType)) {
                return primitiveNumberType;
            }
        }
        return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
    }

    public static List<TypeConstructor> topologicallySortSuperclassesAndRecordAllInstances(
            @NotNull KtType type,
            @NotNull final Map<TypeConstructor, Set<KtType>> constructorToAllInstances,
            @NotNull final Set<TypeConstructor> visited
    ) {
        return DFS.dfs(
                Collections.singletonList(type),
                new DFS.Neighbors<KtType>() {
                    @NotNull
                    @Override
                    public Iterable<KtType> getNeighbors(KtType current) {
                        TypeSubstitutor substitutor = TypeSubstitutor.create(current);
                        Collection<KtType> supertypes = current.getConstructor().getSupertypes();
                        List<KtType> result = new ArrayList<KtType>(supertypes.size());
                        for (KtType supertype : supertypes) {
                            if (visited.contains(supertype.getConstructor())) {
                                continue;
                            }
                            result.add(substitutor.safeSubstitute(supertype, Variance.INVARIANT));
                        }
                        return result;
                    }
                },
                new DFS.Visited<KtType>() {
                    @Override
                    public boolean checkAndMarkVisited(KtType current) {
                        return visited.add(current.getConstructor());
                    }
                },
                new DFS.NodeHandlerWithListResult<KtType, TypeConstructor>() {
                    @Override
                    public boolean beforeChildren(KtType current) {
                        TypeConstructor constructor = current.getConstructor();

                        Set<KtType> instances = constructorToAllInstances.get(constructor);
                        if (instances == null) {
                            instances = new HashSet<KtType>();
                            constructorToAllInstances.put(constructor, instances);
                        }
                        instances.add(current);

                        return true;
                    }

                    @Override
                    public void afterChildren(KtType current) {
                        result.addFirst(current.getConstructor());
                    }
                }
        );
    }

    public static TypeSubstitutor makeConstantSubstitutor(Collection<TypeParameterDescriptor> typeParameterDescriptors, KtType type) {
        final Set<TypeConstructor> constructors = org.jetbrains.kotlin.utils.CollectionsKt
                .newHashSetWithExpectedSize(typeParameterDescriptors.size());
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameterDescriptors) {
            constructors.add(typeParameterDescriptor.getTypeConstructor());
        }
        final TypeProjection projection = new TypeProjectionImpl(type);

        return TypeSubstitutor.create(new TypeConstructorSubstitution() {
            @Override
            public TypeProjection get(@NotNull TypeConstructor key) {
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

    public static boolean isTypeParameter(@NotNull KtType type) {
        return getTypeParameterDescriptorOrNull(type) != null;
    }

    public static boolean isNonReifiedTypeParemeter(@NotNull KtType type) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        return typeParameterDescriptor != null && !typeParameterDescriptor.isReified();
    }

    @Nullable
    public static TypeParameterDescriptor getTypeParameterDescriptorOrNull(@NotNull KtType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            return (TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor();
        }
        return null;
    }

    private static abstract class AbstractTypeWithKnownNullability extends AbstractKtType {
        private final KtType delegate;

        private AbstractTypeWithKnownNullability(@NotNull KtType delegate) {
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
        public abstract boolean isMarkedNullable();

        @Override
        @NotNull
        public KtScope getMemberScope() {
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

        @NotNull
        @Override
        public TypeSubstitution getSubstitution() {
            return delegate.getSubstitution();
        }

        @Nullable
        @Override
        public <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass) {
            return delegate.getCapability(capabilityClass);
        }

        @NotNull
        @Override
        public TypeCapabilities getCapabilities() {
            return delegate.getCapabilities();
        }
    }

    private static class NullableType extends AbstractTypeWithKnownNullability {

        private NullableType(@NotNull KtType delegate) {
            super(delegate);
        }

        @Override
        public boolean isMarkedNullable() {
            return true;
        }
    }

    private static class NotNullType extends AbstractTypeWithKnownNullability {

        private NotNullType(@NotNull KtType delegate) {
            super(delegate);
        }

        @Override
        public boolean isMarkedNullable() {
            return false;
        }
    }

}
