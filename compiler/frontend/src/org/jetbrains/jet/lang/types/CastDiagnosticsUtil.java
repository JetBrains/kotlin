package org.jetbrains.jet.lang.types;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

        for (int i = 0; i < aTypes.size(); i++) {
            JetType aType = aTypes.get(i);
            for (int j = 0; j < bTypes.size(); j++) {
                JetType bType = bTypes.get(j);

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

    private CastDiagnosticsUtil() {}
}
