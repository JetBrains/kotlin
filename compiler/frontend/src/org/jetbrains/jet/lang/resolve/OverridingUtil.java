package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class OverridingUtil {

    private OverridingUtil() {
    }

    public static Set<CallableDescriptor> getEffectiveMembers(@NotNull ClassDescriptor classDescriptor) {
        Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors();
        Set<CallableDescriptor> allMembers = Sets.newLinkedHashSet();
        for (DeclarationDescriptor descriptor : allDescriptors) {
            assert !(descriptor instanceof ConstructorDescriptor);
            if (descriptor instanceof CallableDescriptor && descriptor instanceof MemberDescriptor) {
                CallableDescriptor callableDescriptor = (CallableDescriptor) descriptor;
                if (((MemberDescriptor) descriptor).getModality() != Modality.ABSTRACT) {
                    allMembers.add(callableDescriptor);
                }
            }
        }
        return filterOverrides(allMembers);
    }

    public static <D extends CallableDescriptor> Set<D> filterOverrides(Set<D> candidateSet) {
        return filterOverrides(candidateSet, Function.ID);
    }

    public static <D> Set<D> filterOverrides(Set<D> candidateSet, Function<? super D, ? extends CallableDescriptor> transform) {
        Set<D> candidates = Sets.newLinkedHashSet();
        outerLoop:
        for (D meD : candidateSet) {
            CallableDescriptor me = transform.fun(meD);
            for (D otherD : candidateSet) {
                CallableDescriptor other = transform.fun(otherD);
                if (me == other) continue;
                if (overrides(other, me)) {
                    continue outerLoop;
                }
            }
            for (D otherD : candidates) {
                CallableDescriptor other = transform.fun(otherD);
                if (me.getOriginal() == other.getOriginal()
                    && isOverridableBy(other, me).isSuccess()
                    && isOverridableBy(me, other).isSuccess()) {
                    continue outerLoop;
                }
            }
//            System.out.println(me);
            candidates.add(meD);
        }
//        Set<D> candidates = Sets.newLinkedHashSet(candidateSet);
//        for (D descriptor : candidateSet) {
//            Set<CallableDescriptor> overriddenDescriptors = Sets.newHashSet();
//            getAllOverriddenDescriptors(descriptor.getOriginal(), overriddenDescriptors);
//            candidates.removeAll(overriddenDescriptors);
//        }
        return candidates;
    }

    public static <Descriptor extends CallableDescriptor> boolean overrides(@NotNull Descriptor f, @NotNull Descriptor g) {
        Set<CallableDescriptor> overriddenDescriptors = Sets.newHashSet();
        getAllOverriddenDescriptors(f.getOriginal(), overriddenDescriptors);
        CallableDescriptor originalG = g.getOriginal();
        for (CallableDescriptor overriddenFunction : overriddenDescriptors) {
            if (originalG.equals(overriddenFunction.getOriginal())) return true;
        }
        return false;
    }

    private static void getAllOverriddenDescriptors(@NotNull CallableDescriptor current, @NotNull Set<CallableDescriptor> overriddenDescriptors) {
        if (overriddenDescriptors.contains(current)) return;
        for (CallableDescriptor descriptor : current.getOriginal().getOverriddenDescriptors()) {
            getAllOverriddenDescriptors(descriptor, overriddenDescriptors);
            overriddenDescriptors.add(descriptor);
        }
    }

    @NotNull
    public static OverrideCompatibilityInfo isOverridableBy(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        return isOverridableByImpl(superDescriptor, subDescriptor, true);
    }

    /**
     * @param forOverride true for override, false for overload
     */
    static OverrideCompatibilityInfo isOverridableByImpl(CallableDescriptor superDescriptor, CallableDescriptor subDescriptor, boolean forOverride) {
        if (superDescriptor instanceof FunctionDescriptor) {
            if (subDescriptor instanceof PropertyDescriptor) return OverrideCompatibilityInfo.memberKindMismatch();
        }
        if (superDescriptor instanceof PropertyDescriptor) {
            if (subDescriptor instanceof FunctionDescriptor) return OverrideCompatibilityInfo.memberKindMismatch();
        }
        if (!superDescriptor.getName().equals(subDescriptor.getName())) {
            return OverrideCompatibilityInfo.nameMismatch();
        }

        // TODO : Visibility

        if (superDescriptor.getTypeParameters().size() != subDescriptor.getTypeParameters().size()) {
            return OverrideCompatibilityInfo.typeParameterNumberMismatch();
        }

        if (superDescriptor.getValueParameters().size() != subDescriptor.getValueParameters().size()) {
            return OverrideCompatibilityInfo.valueParameterNumberMismatch();
        }

        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();

        BiMap<TypeConstructor, TypeConstructor> axioms = HashBiMap.create();
        for (int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
            axioms.put(superTypeParameter.getTypeConstructor(), subTypeParameter.getTypeConstructor());
        }

        for (int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);

            if (!JetTypeImpl.equalTypes(superTypeParameter.getUpperBoundsAsType(), subTypeParameter.getUpperBoundsAsType(), axioms)) {
                return OverrideCompatibilityInfo.boundsMismatch(superTypeParameter, subTypeParameter);
            }
        }

        List<ValueParameterDescriptor> superValueParameters = superDescriptor.getValueParameters();
        List<ValueParameterDescriptor> subValueParameters = subDescriptor.getValueParameters();
        for (int i = 0, unsubstitutedValueParametersSize = superValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            ValueParameterDescriptor superValueParameter = superValueParameters.get(i);
            ValueParameterDescriptor subValueParameter = subValueParameters.get(i);

            if (!JetTypeImpl.equalTypes(superValueParameter.getOutType(), subValueParameter.getOutType(), axioms)) {
                return OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameter, subValueParameter);
            }
        }

        // TODO : Default values, varargs etc

        return OverrideCompatibilityInfo.success();
    }

    @NotNull
    public static boolean isReturnTypeOkForOverride(@NotNull JetTypeChecker typeChecker, @NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();
        Map<TypeConstructor, TypeProjection> substitutionContext = Maps.newHashMap();
        for (int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
            substitutionContext.put(
                    superTypeParameter.getTypeConstructor(),
                    new TypeProjection(subTypeParameter.getDefaultType()));
        }

        // This code compares return types, but they are not a part of the signature, so this code does not belong here
        TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitutionContext);
        JetType substitutedSuperReturnType = typeSubstitutor.substitute(superDescriptor.getReturnType(), Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;
        if (!typeChecker.isSubtypeOf(subDescriptor.getReturnType(), substitutedSuperReturnType)) {
            return false;
        }

        return true;
    }

    public static class OverrideCompatibilityInfo {

        private static final OverrideCompatibilityInfo SUCCESS = new OverrideCompatibilityInfo(true, "SUCCESS");

        @NotNull
        public static OverrideCompatibilityInfo success() {
            return SUCCESS;
        }

        @NotNull
        public static OverrideCompatibilityInfo nameMismatch() {
            return new OverrideCompatibilityInfo(false, "nameMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo typeParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(false, "typeParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(false, "valueParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo boundsMismatch(TypeParameterDescriptor superTypeParameter, TypeParameterDescriptor subTypeParameter) {
            return new OverrideCompatibilityInfo(false, "boundsMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterTypeMismatch(ValueParameterDescriptor superValueParameter, ValueParameterDescriptor subValueParameter) {
            return new OverrideCompatibilityInfo(false, "valueParameterTypeMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo memberKindMismatch() {
            return new OverrideCompatibilityInfo(false, "memberKindMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo returnTypeMismatch(JetType substitutedSuperReturnType, JetType unsubstitutedSubReturnType) {
            return new OverrideCompatibilityInfo(true, "returnTypeMismatch: " + unsubstitutedSubReturnType + " >< " + substitutedSuperReturnType); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo varOverriddenByVal() {
            return new OverrideCompatibilityInfo(false, "varOverriddenByVal"); // TODO
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private final boolean isSuccess;
        private final String message;

        public OverrideCompatibilityInfo(boolean success, String message) {
            isSuccess = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public String getMessage() {
            return message;
        }
    }
}
