/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.StandardNames;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner;
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor;
import org.jetbrains.kotlin.types.error.ErrorTypeKind;
import org.jetbrains.kotlin.types.error.ErrorUtils;
import org.jetbrains.kotlin.utils.SmartSet;

import java.util.*;

public class TypeUtils {
    public static final SimpleType DONT_CARE = ErrorUtils.createErrorType(ErrorTypeKind.DONT_CARE);
    public static final SimpleType CANNOT_INFER_FUNCTION_PARAM_TYPE =
            ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE);

    public static class SpecialType extends DelegatingSimpleType {
        private final String name;

        public SpecialType(String name) {
            this.name = name;
        }

        @NotNull
        @Override
        protected SimpleType getDelegate() {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public SimpleType replaceAttributes(@NotNull TypeAttributes newAttributes) {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public SimpleType makeNullableAsSpecified(boolean newNullability) {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public String toString() {
            return name;
        }

        @NotNull
        @Override
        @TypeRefinement
        public DelegatingSimpleType replaceDelegate(@NotNull SimpleType delegate) {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        @TypeRefinement
        public SpecialType refine(@NotNull KotlinTypeRefiner kotlinTypeRefiner) {
            return this;
        }
    }

    @NotNull
    public static final SimpleType NO_EXPECTED_TYPE = new SpecialType("NO_EXPECTED_TYPE");

    public static final SimpleType UNIT_EXPECTED_TYPE = new SpecialType("UNIT_EXPECTED_TYPE");

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
        return type.unwrap().makeNullableAsSpecified(nullable);
    }

    @NotNull
    public static SimpleType makeNullableIfNeeded(@NotNull SimpleType type, boolean nullable) {
        if (nullable) {
            return type.makeNullableAsSpecified(true);
        }
        return type;
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
            if (typeProjection.isStarProjection()) return true;

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
    public static SimpleType makeUnsubstitutedType(
            ClassifierDescriptor classifierDescriptor, MemberScope unsubstitutedMemberScope,
            Function1<KotlinTypeRefiner, SimpleType> refinedTypeFactory
    ) {
        if (ErrorUtils.isError(classifierDescriptor)) {
            return ErrorUtils.createErrorType(ErrorTypeKind.UNABLE_TO_SUBSTITUTE_TYPE, classifierDescriptor.toString());
        }
        TypeConstructor typeConstructor = classifierDescriptor.getTypeConstructor();
        return makeUnsubstitutedType(typeConstructor, unsubstitutedMemberScope, refinedTypeFactory);
    }

    @NotNull
    public static SimpleType makeUnsubstitutedType(
            @NotNull TypeConstructor typeConstructor,
            @NotNull MemberScope unsubstitutedMemberScope,
            @NotNull Function1<KotlinTypeRefiner, SimpleType> refinedTypeFactory
    ) {
        List<TypeProjection> arguments = getDefaultTypeProjections(typeConstructor.getParameters());
        return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                TypeAttributes.Companion.getEmpty(),
                typeConstructor,
                arguments,
                false,
                unsubstitutedMemberScope,
                refinedTypeFactory
        );
    }

    @NotNull
    public static List<TypeProjection> getDefaultTypeProjections(@NotNull List<TypeParameterDescriptor> parameters) {
        List<TypeProjection> result = new ArrayList<TypeProjection>(parameters.size());
        for (TypeParameterDescriptor parameterDescriptor : parameters) {
            result.add(new TypeProjectionImpl(parameterDescriptor.getDefaultType()));
        }
        return CollectionsKt.toList(result);
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
        if (FlexibleTypesKt.isFlexible(type) && isNullableType(FlexibleTypesKt.asFlexibleType(type).getUpperBound())) {
            return true;
        }
        if (SpecialTypesKt.isDefinitelyNotNullType(type)) {
            return false;
        }
        if (isTypeParameter(type)) {
            return hasNullableSuperType(type);
        }
        if (type instanceof AbstractStubType) {
            NewTypeVariableConstructor typeVariableConstructor = (NewTypeVariableConstructor) ((AbstractStubType) type).getOriginalTypeVariable();
            TypeParameterDescriptor typeParameter = typeVariableConstructor.getOriginalTypeParameter();
            return typeParameter == null || hasNullableSuperType(typeParameter.getDefaultType());
        }

        TypeConstructor constructor = type.getConstructor();
        if (constructor instanceof IntersectionTypeConstructor) {
            for (KotlinType supertype : constructor.getSupertypes()) {
                if (isNullableType(supertype)) return true;
            }
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
        if (FlexibleTypesKt.isFlexible(type) && acceptsNullable(FlexibleTypesKt.asFlexibleType(type).getUpperBound())) {
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
            if (isNullableType(supertype)) return true;
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
        return KotlinTypeChecker.DEFAULT.equalTypes(a, b);
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
        return contains(type, new Function1<UnwrappedType, Boolean>() {
            @Override
            public Boolean invoke(UnwrappedType type) {
                return specialType.equals(type);
            }
        });
    }

    public static boolean contains(
            @Nullable KotlinType type,
            @NotNull Function1<UnwrappedType, Boolean> isSpecialType
    ) {
        return contains(type, isSpecialType, null);
    }

    private static boolean contains(
            @Nullable KotlinType type,
            @NotNull Function1<UnwrappedType, Boolean> isSpecialType,
            SmartSet<KotlinType> visited
    ) {
        if (type == null) return false;

        UnwrappedType unwrappedType = type.unwrap();

        if (noExpectedType(type)) return isSpecialType.invoke(unwrappedType);
        if (visited != null && visited.contains(type)) return false;
        if (isSpecialType.invoke(unwrappedType)) return true;

        if (visited == null) {
            visited = SmartSet.create();
        }
        visited.add(type);

        FlexibleType flexibleType = unwrappedType instanceof FlexibleType ? (FlexibleType) unwrappedType : null;
        if (flexibleType != null
            && (contains(flexibleType.getLowerBound(), isSpecialType, visited)
                || contains(flexibleType.getUpperBound(), isSpecialType, visited))) {
            return true;
        }

        if (unwrappedType instanceof DefinitelyNotNullType &&
            contains(((DefinitelyNotNullType) unwrappedType).getOriginal(), isSpecialType, visited)) {
            return true;
        }

        TypeConstructor typeConstructor = type.getConstructor();
        if (typeConstructor instanceof IntersectionTypeConstructor) {
            IntersectionTypeConstructor intersectionTypeConstructor = (IntersectionTypeConstructor) typeConstructor;
            for (KotlinType supertype : intersectionTypeConstructor.getSupertypes()) {
                if (contains(supertype, isSpecialType, visited)) return true;
            }
            return false;
        }

        for (TypeProjection projection : type.getArguments()) {
            if (projection.isStarProjection()) continue;
            if (contains(projection.getType(), isSpecialType, visited)) return true;
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

        KotlinType uIntType = findByFqName(supertypes, StandardNames.FqNames.uIntFqName);
        if (uIntType != null) return uIntType;

        KotlinType uLongType = findByFqName(supertypes, StandardNames.FqNames.uLongFqName);
        if (uLongType != null) return uLongType;

        return null;
    }

    @Nullable
    private static KotlinType findByFqName(@NotNull Collection<KotlinType> supertypes, FqName fqName) {
        for (KotlinType supertype : supertypes) {
            ClassifierDescriptor descriptor = supertype.getConstructor().getDeclarationDescriptor();
            if (descriptor == null) continue;

            FqNameUnsafe descriptorFqName = DescriptorUtils.getFqName(descriptor);
            if (descriptorFqName.equals(fqName.toUnsafe())) {
                return supertype;
            }
        }
        return null;
    }

    @NotNull
    public static KotlinType getPrimitiveNumberType(
            @NotNull IntegerValueTypeConstructor numberValueTypeConstructor,
            @NotNull KotlinType expectedType
    ) {
        if (noExpectedType(expectedType) || KotlinTypeKt.isError(expectedType)) {
            return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
        }
        for (KotlinType primitiveNumberType : numberValueTypeConstructor.getSupertypes()) {
            if (KotlinTypeChecker.DEFAULT.isSubtypeOf(primitiveNumberType, expectedType)) {
                return primitiveNumberType;
            }
        }
        return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
    }

    @NotNull
    public static KotlinType getPrimitiveNumberType(
            @NotNull IntegerLiteralTypeConstructor literalTypeConstructor,
            @NotNull KotlinType expectedType
    ) {
        if (noExpectedType(expectedType) || KotlinTypeKt.isError(expectedType)) {
            return literalTypeConstructor.getApproximatedType();
        }

        // If approximated type does not match expected type then expected type is very
        //  specific type (e.g. Comparable<Byte>), so only one of possible types could match it
        KotlinType approximatedType = literalTypeConstructor.getApproximatedType();
        if (KotlinTypeChecker.DEFAULT.isSubtypeOf(approximatedType, expectedType)) {
            return approximatedType;
        }

        for (KotlinType primitiveNumberType : literalTypeConstructor.getPossibleTypes()) {
            if (KotlinTypeChecker.DEFAULT.isSubtypeOf(primitiveNumberType, expectedType)) {
                return primitiveNumberType;
            }
        }
        return literalTypeConstructor.getApproximatedType();
    }

    public static boolean isTypeParameter(@NotNull KotlinType type) {
        return getTypeParameterDescriptorOrNull(type) != null || type.getConstructor() instanceof NewTypeVariableConstructor;
    }

    public static boolean isReifiedTypeParameter(@NotNull KotlinType type) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        return typeParameterDescriptor != null && typeParameterDescriptor.isReified();
    }

    public static boolean isNonReifiedTypeParameter(@NotNull KotlinType type) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        return typeParameterDescriptor != null && !typeParameterDescriptor.isReified();
    }

    @Nullable
    public static TypeParameterDescriptor getTypeParameterDescriptorOrNull(@NotNull KotlinType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            return (TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor();
        }
        return null;
    }
}
