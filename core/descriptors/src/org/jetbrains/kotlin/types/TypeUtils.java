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

import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType;
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructor;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.*;

public class TypeUtils {
    public static final KotlinType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");
    public static final KotlinType CANT_INFER_FUNCTION_PARAM_TYPE = ErrorUtils.createErrorType("Cannot be inferred");

    public static class SpecialType implements KotlinType {
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
        public MemberScope getMemberScope() {
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
    public static final KotlinType NO_EXPECTED_TYPE = new SpecialType("NO_EXPECTED_TYPE");

    public static final KotlinType UNIT_EXPECTED_TYPE = new SpecialType("UNIT_EXPECTED_TYPE");

    public static boolean noExpectedType(@NotNull KotlinType type) {
        return type == NO_EXPECTED_TYPE || type == UNIT_EXPECTED_TYPE;
    }

    public static boolean isDontCarePlaceholder(@Nullable KotlinType type) {
        return type != null && type.getConstructor() == DONT_CARE.getConstructor();
    }

    @NotNull
    public static KotlinType makeNullable(@NotNull KotlinType type) {
        return makeNullableAsSpecified(type, true);
    }

    @NotNull
    public static KotlinType makeNotNullable(@NotNull KotlinType type) {
        return makeNullableAsSpecified(type, false);
    }

    @NotNull
    public static KotlinType makeNullableAsSpecified(@NotNull KotlinType type, boolean nullable) {
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
    public static KotlinType makeNullableIfNeeded(@NotNull KotlinType type, boolean nullable) {
        if (nullable) {
            return makeNullable(type);
        }
        return type;
    }

    public static boolean canHaveSubtypes(KotlinTypeChecker typeChecker, @NotNull KotlinType type) {
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
            KotlinType argument = typeProjection.getType();

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

    private static boolean lowerThanBound(KotlinTypeChecker typeChecker, KotlinType argument, TypeParameterDescriptor parameterDescriptor) {
        for (KotlinType bound : parameterDescriptor.getUpperBounds()) {
            if (typeChecker.isSubtypeOf(argument, bound)) {
                if (!argument.getConstructor().equals(bound.getConstructor())) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public static KotlinType makeUnsubstitutedType(ClassDescriptor classDescriptor, MemberScope unsubstitutedMemberScope) {
        if (ErrorUtils.isError(classDescriptor)) {
            return ErrorUtils.createErrorType("Unsubstituted type for " + classDescriptor);
        }
        TypeConstructor typeConstructor = classDescriptor.getTypeConstructor();
        List<TypeProjection> arguments = getDefaultTypeProjections(typeConstructor.getParameters());
        return KotlinTypeImpl.create(
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
    public static List<KotlinType> getImmediateSupertypes(@NotNull KotlinType type) {
        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        Collection<KotlinType> originalSupertypes = type.getConstructor().getSupertypes();
        List<KotlinType> result = new ArrayList<KotlinType>(originalSupertypes.size());
        for (KotlinType supertype : originalSupertypes) {
            KotlinType substitutedType = createSubstitutedSupertype(type, supertype, substitutor);
            if (substitutedType != null) {
                result.add(substitutedType);
            }
        }
        return result;
    }

    @Nullable
    public static KotlinType createSubstitutedSupertype(
            @NotNull KotlinType subType,
            @NotNull KotlinType superType,
            @NotNull TypeSubstitutor substitutor
    ) {
        KotlinType substitutedType = substitutor.substitute(superType, Variance.INVARIANT);
        if (substitutedType != null) {
            return makeNullableIfNeeded(substitutedType, subType.isMarkedNullable());
        }
        return null;
    }

    private static void collectAllSupertypes(@NotNull KotlinType type, @NotNull Set<KotlinType> result) {
        List<KotlinType> immediateSupertypes = getImmediateSupertypes(type);
        result.addAll(immediateSupertypes);
        for (KotlinType supertype : immediateSupertypes) {
            collectAllSupertypes(supertype, result);
        }
    }


    @NotNull
    public static Set<KotlinType> getAllSupertypes(@NotNull KotlinType type) {
        // 15 is obtained by experimentation: JDK classes like ArrayList tend to have so many supertypes,
        // the average number is lower
        Set<KotlinType> result = new LinkedHashSet<KotlinType>(15);
        collectAllSupertypes(type, result);
        return result;
    }

    /**
     * A work-around of the generic nullability problem in the type checker
     * Semantics should be the same as `!isSubtype(T, Any)`
     * @return true if a value of this type can be null
     */
    public static boolean isNullableType(@NotNull KotlinType type) {
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
    public static boolean acceptsNullable(@NotNull KotlinType type) {
        if (type.isMarkedNullable()) {
            return true;
        }
        if (FlexibleTypesKt.isFlexible(type) && acceptsNullable(FlexibleTypesKt.flexibility(type).getUpperBound())) {
            return true;
        }
        return false;
    }

    public static boolean hasNullableSuperType(@NotNull KotlinType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
            // A class/trait cannot have a nullable supertype
            return false;
        }

        for (KotlinType supertype : getImmediateSupertypes(type)) {
            if (supertype.isMarkedNullable()) return true;
            if (hasNullableSuperType(supertype)) return true;
        }

        return false;
    }

    @Nullable
    public static ClassDescriptor getClassDescriptor(@NotNull KotlinType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof ClassDescriptor) {
            return (ClassDescriptor) declarationDescriptor;
        }
        return null;
    }

    @NotNull
    public static KotlinType substituteParameters(@NotNull ClassDescriptor clazz, @NotNull List<KotlinType> typeArguments) {
        List<TypeProjection> projections = CollectionsKt.map(typeArguments, new Function1<KotlinType, TypeProjection>() {
            @Override
            public TypeProjection invoke(KotlinType type) {
                return new TypeProjectionImpl(type);
            }
        });

        return substituteProjectionsForParameters(clazz, projections);
    }

    @NotNull
    public static KotlinType substituteProjectionsForParameters(@NotNull ClassDescriptor clazz, @NotNull List<TypeProjection> projections) {
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

    public static boolean equalTypes(@NotNull KotlinType a, @NotNull KotlinType b) {
        return KotlinTypeChecker.DEFAULT.isSubtypeOf(a, b) && KotlinTypeChecker.DEFAULT.isSubtypeOf(b, a);
    }

    public static boolean dependsOnTypeParameters(@NotNull KotlinType type, @NotNull Collection<TypeParameterDescriptor> typeParameters) {
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

    public static boolean dependsOnTypeConstructors(@NotNull KotlinType type, @NotNull Collection<TypeConstructor> typeParameterConstructors) {
        if (typeParameterConstructors.contains(type.getConstructor())) return true;
        for (TypeProjection typeProjection : type.getArguments()) {
            if (!typeProjection.isStarProjection() && dependsOnTypeConstructors(typeProjection.getType(), typeParameterConstructors)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(@Nullable KotlinType type, @NotNull final KotlinType specialType) {
        return contains(type, new Function1<KotlinType, Boolean>() {
            @Override
            public Boolean invoke(KotlinType type) {
                return specialType.equals(type);
            }
        });
    }

    public static boolean contains(
            @Nullable KotlinType type,
            @NotNull Function1<KotlinType, Boolean> isSpecialType
    ) {
        if (type == null) return false;
        if (isSpecialType.invoke(type)) return true;
        Flexibility flexibility = type.getCapability(Flexibility.class);
        if (flexibility != null
                && (contains(flexibility.getLowerBound(), isSpecialType) || contains(flexibility.getUpperBound(), isSpecialType))) {
            return true;
        }
        for (TypeProjection projection : type.getArguments()) {
            if (!projection.isStarProjection() && contains(projection.getType(), isSpecialType)) return true;
        }
        return false;
    }

    @NotNull
    public static TypeProjection makeStarProjection(@NotNull TypeParameterDescriptor parameterDescriptor) {
        return new StarProjectionImpl(parameterDescriptor);
    }

    @NotNull
    public static KotlinType getDefaultPrimitiveNumberType(@NotNull IntegerValueTypeConstructor numberValueTypeConstructor) {
        KotlinType type = getDefaultPrimitiveNumberType(numberValueTypeConstructor.getSupertypes());
        assert type != null : "Strange number value type constructor: " + numberValueTypeConstructor + ". " +
                              "Super types doesn't contain double, int or long: " + numberValueTypeConstructor.getSupertypes();
        return type;
    }

    @Nullable
    public static KotlinType getDefaultPrimitiveNumberType(@NotNull Collection<KotlinType> supertypes) {
        if (supertypes.isEmpty()) {
            return null;
        }

        KotlinBuiltIns builtIns = supertypes.iterator().next().getConstructor().getBuiltIns();
        KotlinType doubleType = builtIns.getDoubleType();
        if (supertypes.contains(doubleType)) {
            return doubleType;
        }
        KotlinType intType = builtIns.getIntType();
        if (supertypes.contains(intType)) {
            return intType;
        }
        KotlinType longType = builtIns.getLongType();
        if (supertypes.contains(longType)) {
            return longType;
        }
        return null;
    }

    @NotNull
    public static KotlinType getPrimitiveNumberType(
            @NotNull IntegerValueTypeConstructor numberValueTypeConstructor,
            @NotNull KotlinType expectedType
    ) {
        if (noExpectedType(expectedType) || expectedType.isError()) {
            return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
        }
        for (KotlinType primitiveNumberType : numberValueTypeConstructor.getSupertypes()) {
            if (KotlinTypeChecker.DEFAULT.isSubtypeOf(primitiveNumberType, expectedType)) {
                return primitiveNumberType;
            }
        }
        return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
    }

    public static boolean isTypeParameter(@NotNull KotlinType type) {
        return getTypeParameterDescriptorOrNull(type) != null;
    }

    public static boolean isNonReifiedTypeParemeter(@NotNull KotlinType type) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        return typeParameterDescriptor != null && !typeParameterDescriptor.isReified();
    }

    public static boolean isReifiedTypeParameter(@NotNull KotlinType type) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        return typeParameterDescriptor != null && typeParameterDescriptor.isReified();
    }

    @NotNull
    public static KotlinType uncaptureTypeForInlineMapping(@NotNull KotlinType type) {
        TypeConstructor constructor = type.getConstructor();
        if (constructor instanceof CapturedTypeConstructor) {
            TypeProjection projection = ((CapturedTypeConstructor) constructor).getTypeProjection();
            if (Variance.IN_VARIANCE == projection.getProjectionKind()) {
                //in variance could be captured only for <T> or <T: Any?> declarations
                return TypeUtilsKt.getBuiltIns(type).getNullableAnyType();
            }
            return uncaptureTypeForInlineMapping(projection.getType());
        }
        return type;
    }
    @Nullable
    public static TypeParameterDescriptor getTypeParameterDescriptorOrNull(@NotNull KotlinType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            return (TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor();
        }
        return null;
    }

    private static abstract class AbstractTypeWithKnownNullability extends AbstractKotlinType {
        private final KotlinType delegate;

        private AbstractTypeWithKnownNullability(@NotNull KotlinType delegate) {
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
        public MemberScope getMemberScope() {
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

        private NullableType(@NotNull KotlinType delegate) {
            super(delegate);
        }

        @Override
        public boolean isMarkedNullable() {
            return true;
        }
    }

    private static class NotNullType extends AbstractTypeWithKnownNullability {

        private NotNullType(@NotNull KotlinType delegate) {
            super(delegate);
        }

        @Override
        public boolean isMarkedNullable() {
            return false;
        }
    }

}
