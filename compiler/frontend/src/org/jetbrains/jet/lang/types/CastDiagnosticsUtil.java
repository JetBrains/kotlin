package org.jetbrains.jet.lang.types;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CastDiagnosticsUtil {

    // As this method produces a warning, it must be _complete_ (not sound), i.e. every time it says "cast impossible",
    // it must be really impossible
    public static boolean isCastPossible(
            @NotNull JetType lhsType,
            @NotNull JetType rhsType,
            @NotNull PlatformToKotlinClassMap platformToKotlinClassMap
    ) {
        if (isRelated(lhsType, rhsType, platformToKotlinClassMap)) return true;
        // This is an oversimplification (which does not render the method incomplete):
        // we consider any type parameter capable of taking any value, which may be made more precise if we considered bounds
        if (isTypeParameter(lhsType) || isTypeParameter(rhsType)) return true;
        if (isFinal(lhsType) || isFinal(rhsType)) return false;
        if (isTrait(lhsType) || isTrait(rhsType)) return true;
        return false;
    }

    /**
     * Two types are related, roughly, when one is a subtype or supertype of the other.
     * <p/>
     * Note that some types have platform-specific counterparts, i.e. jet.String is mapped to java.lang.String,
     * such types (and all their sub- and supertypes) are related too.
     * <p/>
     * Due to limitations in PlatformToKotlinClassMap, we only consider mapping of platform classes to Kotlin classed
     * (i.e. java.lang.String -> jet.String) and ignore mappings that go the other way.
     */
    private static boolean isRelated(@NotNull JetType a, @NotNull JetType b, @NotNull PlatformToKotlinClassMap platformToKotlinClassMap) {
        List<JetType> aTypes = mapToPlatformIndependentTypes(a, platformToKotlinClassMap);
        List<JetType> bTypes = mapToPlatformIndependentTypes(b, platformToKotlinClassMap);

        for (JetType aType : aTypes) {
            for (JetType bType : bTypes) {
                if (JetTypeChecker.INSTANCE.isSubtypeOf(aType, bType)) return true;
                if (JetTypeChecker.INSTANCE.isSubtypeOf(bType, aType)) return true;
            }
        }

        return false;
    }

    private static List<JetType> mapToPlatformIndependentTypes(
            @NotNull JetType type,
            @NotNull PlatformToKotlinClassMap platformToKotlinClassMap
    ) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (!(descriptor instanceof ClassDescriptor)) return Collections.singletonList(type);

        ClassDescriptor originalClass = (ClassDescriptor) descriptor;
        Collection<ClassDescriptor> kotlinClasses = platformToKotlinClassMap.mapPlatformClass(originalClass);
        if (kotlinClasses.isEmpty()) return Collections.singletonList(type);

        List<JetType> result = Lists.newArrayListWithCapacity(2);
        result.add(type);
        for (ClassDescriptor classDescriptor : kotlinClasses) {
            JetType kotlinType = TypeUtils.substituteProjectionsForParameters(classDescriptor, type.getArguments());
            result.add(kotlinType);
        }

        return result;
    }

    private static boolean isTypeParameter(@NotNull JetType type) {
        return type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;
    }

    private static boolean isFinal(@NotNull JetType type) {
        return !TypeUtils.canHaveSubtypes(JetTypeChecker.INSTANCE, type);
    }

    private static boolean isTrait(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() == ClassKind.TRAIT;
    }

    /**
     * Check if cast from supertype to subtype is erased.
     * It is an error in "is" statement and warning in "as".
     */
    public static boolean isCastErased(@NotNull JetType supertype, @NotNull JetType subtype, @NotNull JetTypeChecker typeChecker) {
        // cast between T and T? is always OK
        if (supertype.isNullable() || subtype.isNullable()) {
            return isCastErased(TypeUtils.makeNotNullable(supertype), TypeUtils.makeNotNullable(subtype), typeChecker);
        }

        // if it is a upcast, it's never erased
        if (typeChecker.isSubtypeOf(supertype, subtype)) return false;

        // downcasting to a type parameter is always erased
        if (isTypeParameter(subtype)) return true;

        // Check that we are actually casting to a generic type
        // NOTE: this does not account for 'as Array<List<T>>'
        if (allParametersReified(subtype)) return false;

        // Assume we are casting an expression of type Collection<Foo> to List<Bar>
        // First, let's make List<T>, where T is a type variable
        JetType subtypeWithVariables = TypeUtils.makeUnsubstitutedType(
                subtype.getConstructor(),
                ErrorUtils.createErrorScope("Scope for intermediate type. This type shouldn't be used outside isCastErased()", true));

        // Now, let's find a supertype of List<T> that is a Collection of something,
        // in this case it will be Collection<T>
        JetType supertypeWithVariables = TypeCheckingProcedure.findCorrespondingSupertype(subtypeWithVariables, supertype);

        final List<TypeParameterDescriptor> variables = subtypeWithVariables.getConstructor().getParameters();

        Map<TypeConstructor, TypeProjection> substitution;
        if (supertypeWithVariables != null) {
            // Now, let's try to unify Collection<T> and Collection<Foo> solution is a map from T to Foo
            TypeUnifier.UnificationResult solution = TypeUnifier.unify(
                    new TypeProjection(supertype), new TypeProjection(supertypeWithVariables),
                    new Predicate<TypeConstructor>() {
                        @Override
                        public boolean apply(TypeConstructor typeConstructor) {
                            ClassifierDescriptor descriptor = typeConstructor.getDeclarationDescriptor();
                            return descriptor instanceof TypeParameterDescriptor && variables.contains(descriptor);
                        }
                    });
            substitution = Maps.newHashMap(solution.getSubstitution());
        }
        else {
            // If there's no corresponding supertype, no variables are determined
            // This may be OK, e.g. in case 'Any as List<*>'
            substitution = Maps.newHashMapWithExpectedSize(variables.size());
        }

        // If some of the parameters are not determined by unification, it means that these parameters are lost,
        // let's put stars instead, so that we can only cast to something like List<*>, e.g. (a: Any) as List<*>
        for (TypeParameterDescriptor variable : variables) {
            TypeProjection value = substitution.get(variable.getTypeConstructor());
            if (value == null) {
                substitution.put(
                        variable.getTypeConstructor(),
                        SubstitutionUtils.makeStarProjection(variable)
                );
            }
        }

        // At this point we have values for all type parameters of List
        // Let's make a type by substituting them: List<T> -> List<Foo>
        JetType staticallyKnownSubtype = TypeSubstitutor.create(substitution).substitute(subtypeWithVariables, Variance.INVARIANT);

        // If the substitution failed, it means that the result is an impossible type, e.g. something like Out<in Foo>
        // In this case, we can't guarantee anything, so the cast is considered to be erased
        if (staticallyKnownSubtype == null) return true;

        // If the type we calculated is a subtype of the cast target, it's OK to use the cast target instead.
        // If not, it's wrong to use it
        return !typeChecker.isSubtypeOf(staticallyKnownSubtype, subtype);
    }

    private static boolean allParametersReified(JetType subtype) {
        for (TypeParameterDescriptor parameterDescriptor : subtype.getConstructor().getParameters()) {
            if (!parameterDescriptor.isReified()) return false;
        }
        return true;
    }

    private CastDiagnosticsUtil() {}
}
