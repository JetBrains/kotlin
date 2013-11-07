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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyAccessorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.CONFLICT;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;

public class OverridingUtil {

    private static final List<ExternalOverridabilityCondition> EXTERNAL_CONDITIONS =
            ContainerUtil.collect(ServiceLoader.load(
                    ExternalOverridabilityCondition.class,
                    ExternalOverridabilityCondition.class.getClassLoader()).iterator()
            );

    private OverridingUtil() {
    }

    private static enum Filtering {
        RETAIN_OVERRIDING,
        RETAIN_OVERRIDDEN
    }

    @NotNull
    public static <D extends CallableDescriptor> Set<D> filterOutOverridden(@NotNull Set<D> candidateSet) {
        return filterOverrides(candidateSet, Function.ID, Filtering.RETAIN_OVERRIDING);
    }

    @NotNull
    public static <D> Set<D> filterOutOverriding(@NotNull Set<D> candidateSet) {
        return filterOverrides(candidateSet, Function.ID, Filtering.RETAIN_OVERRIDDEN);
    }

    @NotNull
    public static <D> Set<D> filterOutOverridden(
            @NotNull Set<D> candidateSet,
            @NotNull Function<? super D, ? extends CallableDescriptor> transform
    ) {
        return filterOverrides(candidateSet, transform, Filtering.RETAIN_OVERRIDING);
    }

    @NotNull
    private static <D> Set<D> filterOverrides(
            @NotNull Set<D> candidateSet,
            @NotNull Function<? super D, ? extends CallableDescriptor> transform,
            @NotNull Filtering filtering
    ) {
        Set<D> candidates = Sets.newLinkedHashSet();
        outerLoop:
        for (D meD : candidateSet) {
            CallableDescriptor me = transform.fun(meD);
            for (D otherD : candidateSet) {
                CallableDescriptor other = transform.fun(otherD);
                if (me == other) continue;
                if (filtering == Filtering.RETAIN_OVERRIDING) {
                    if (overrides(other, me)) {
                        continue outerLoop;
                    }
                }
                else if (filtering == Filtering.RETAIN_OVERRIDDEN) {
                    if (overrides(me, other)) {
                        continue outerLoop;
                    }
                }
                else {
                    throw new AssertionError("Unexpected Filtering object: " + filtering);
                }
            }
            for (D otherD : candidates) {
                CallableDescriptor other = transform.fun(otherD);
                if (me.getOriginal() == other.getOriginal()
                    && isOverridableBy(other, me).getResult() == OverrideCompatibilityInfo.Result.OVERRIDABLE
                    && isOverridableBy(me, other).getResult() == OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                    continue outerLoop;
                }
            }
            candidates.add(meD);
        }
        return candidates;
    }

    public static <D extends CallableDescriptor> boolean overrides(@NotNull D f, @NotNull D g) {
        CallableDescriptor originalG = g.getOriginal();
        for (CallableDescriptor overriddenFunction : getAllOverriddenDescriptors(f)) {
            if (originalG.equals(overriddenFunction.getOriginal())) return true;
        }
        return false;
    }

    public static Set<CallableDescriptor> getAllOverriddenDescriptors(CallableDescriptor f) {
        Set<CallableDescriptor> overriddenDescriptors = Sets.newHashSet();
        collectAllOverriddenDescriptors(f.getOriginal(), overriddenDescriptors);
        return overriddenDescriptors;
    }

    private static void collectAllOverriddenDescriptors(
            @NotNull CallableDescriptor current,
            @NotNull Set<CallableDescriptor> result
    ) {
        if (result.contains(current)) return;
        for (CallableDescriptor descriptor : current.getOriginal().getOverriddenDescriptors()) {
            collectAllOverriddenDescriptors(descriptor, result);
            result.add(descriptor);
        }
    }

    @NotNull
    public static OverrideCompatibilityInfo isOverridableBy(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        if (superDescriptor instanceof FunctionDescriptor) {
            if (!(subDescriptor instanceof FunctionDescriptor)) return OverrideCompatibilityInfo.memberKindMismatch();
        }
        else if (superDescriptor instanceof PropertyDescriptor) {
            if (!(subDescriptor instanceof PropertyDescriptor)) return OverrideCompatibilityInfo.memberKindMismatch();
        }
        else {
            throw new IllegalArgumentException("This type of CallableDescriptor cannot be checked for overridability: " + superDescriptor);
        }

        // TODO: check outside of this method
        if (!superDescriptor.getName().equals(subDescriptor.getName())) {
            return OverrideCompatibilityInfo.nameMismatch();
        }

        return isOverridableByImpl(superDescriptor, subDescriptor, true);
    }
    
    private static List<JetType> compiledValueParameters(CallableDescriptor callableDescriptor) {
        ReceiverParameterDescriptor receiverParameter = callableDescriptor.getReceiverParameter();
        ArrayList<JetType> parameters = new ArrayList<JetType>();
        if (receiverParameter != null) {
            parameters.add(receiverParameter.getType());
        }
        for (ValueParameterDescriptor valueParameterDescriptor : callableDescriptor.getValueParameters()) {
            parameters.add(valueParameterDescriptor.getType());
        }
        return parameters;
    }

    /**
     * @param forOverride true for override, false for overload
     */
    static OverrideCompatibilityInfo isOverridableByImpl(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor, boolean forOverride) {

        // TODO : Visibility

        if ((superDescriptor.getReceiverParameter() == null) != (subDescriptor.getReceiverParameter() == null)) {
            return OverrideCompatibilityInfo.receiverPresenceMismatch();
        }

        if (superDescriptor.getValueParameters().size() != subDescriptor.getValueParameters().size()) {
            return OverrideCompatibilityInfo.valueParameterNumberMismatch();
        }

        List<JetType> superValueParameters = compiledValueParameters(superDescriptor);
        List<JetType> subValueParameters = compiledValueParameters(subDescriptor);

        if (forOverride) {
            if (superDescriptor.getTypeParameters().size() != subDescriptor.getTypeParameters().size()) {
                for (int i = 0; i < superValueParameters.size(); ++i) {
                    JetType superValueParameterType = getUpperBound(superValueParameters.get(i));
                    JetType subValueParameterType = getUpperBound(subValueParameters.get(i));
                    // TODO: compare erasure
                    if (!JetTypeChecker.INSTANCE.equalTypes(superValueParameterType, subValueParameterType)) {
                        return OverrideCompatibilityInfo.typeParameterNumberMismatch();
                    }
                }
                return OverrideCompatibilityInfo.valueParameterTypeMismatch(null, null, OverrideCompatibilityInfo.Result.CONFLICT);
            }
        }

        if (forOverride) {

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

                if (!JetTypeChecker.INSTANCE.equalTypes(superTypeParameter.getUpperBoundsAsType(), subTypeParameter.getUpperBoundsAsType(), axioms)) {
                    return OverrideCompatibilityInfo.boundsMismatch(superTypeParameter, subTypeParameter);
                }
            }

            for (int i = 0, unsubstitutedValueParametersSize = superValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
                JetType superValueParameter = superValueParameters.get(i);
                JetType subValueParameter = subValueParameters.get(i);

                boolean bothErrors = superValueParameter.isError() && subValueParameter.isError();
                if (!bothErrors && !JetTypeChecker.INSTANCE.equalTypes(superValueParameter, subValueParameter, axioms)) {
                    return OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameter, subValueParameter, OverrideCompatibilityInfo.Result.INCOMPATIBLE);
                }
            }

            for (ExternalOverridabilityCondition externalCondition : EXTERNAL_CONDITIONS) {
                if (!externalCondition.isOverridable(superDescriptor, subDescriptor)) {
                    return OverrideCompatibilityInfo.externalConditionFailed(externalCondition.getClass());
                }
            }
        }
        else {

            for (int i = 0; i < superValueParameters.size(); ++i) {
                JetType superValueParameterType = getUpperBound(superValueParameters.get(i));
                JetType subValueParameterType = getUpperBound(subValueParameters.get(i));
                // TODO: compare erasure
                if (!JetTypeChecker.INSTANCE.equalTypes(superValueParameterType, subValueParameterType)) {
                    return OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameterType, subValueParameterType, OverrideCompatibilityInfo.Result.INCOMPATIBLE);
                }
            }
            
            return OverrideCompatibilityInfo.success();

        }

        // TODO : Default values, varargs etc

        return OverrideCompatibilityInfo.success();
    }
    
    private static JetType getUpperBound(JetType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
            return type;
        }
        else if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            return ((TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor()).getUpperBoundsAsType();
        }
        else {
            throw new IllegalStateException("unknown type constructor: " + type.getConstructor().getClass().getName());
        }
    }

    public static boolean isReturnTypeOkForOverride(@NotNull JetTypeChecker typeChecker, @NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        TypeSubstitutor typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor);
        if (typeSubstitutor == null) return false;

        JetType superReturnType = superDescriptor.getReturnType();
        assert superReturnType != null;

        JetType subReturnType = subDescriptor.getReturnType();
        assert subReturnType != null;

        JetType substitutedSuperReturnType = typeSubstitutor.substitute(superReturnType, Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;

        return typeChecker.isSubtypeOf(subReturnType, substitutedSuperReturnType);
    }

    @Nullable
    private static TypeSubstitutor prepareTypeSubstitutor(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();
        if (subTypeParameters.size() != superTypeParameters.size()) return null;

        Map<TypeConstructor, TypeProjection> substitutionContext = Maps.newHashMap();
        for (int i = 0; i < superTypeParameters.size(); i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
            substitutionContext.put(
                    superTypeParameter.getTypeConstructor(),
                    new TypeProjectionImpl(subTypeParameter.getDefaultType()));
        }
        return TypeSubstitutor.create(substitutionContext);
    }

    public static boolean isPropertyTypeOkForOverride(@NotNull JetTypeChecker typeChecker, @NotNull PropertyDescriptor superDescriptor, @NotNull PropertyDescriptor subDescriptor) {
        TypeSubstitutor typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor);
        JetType substitutedSuperReturnType = typeSubstitutor.substitute(superDescriptor.getReturnType(), Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;
        if (superDescriptor.isVar() && !typeChecker.equalTypes(subDescriptor.getReturnType(), substitutedSuperReturnType)) {
            return false;
        }

        return true;
    }

    /**
     * Get overridden descriptors that are declarations or delegations.
     *
     * @see CallableMemberDescriptor.Kind#isReal()
     */
    public static Collection<CallableMemberDescriptor> getOverriddenDeclarations(CallableMemberDescriptor descriptor) {
        Map<ClassDescriptor, CallableMemberDescriptor> result = Maps.newHashMap();
        getOverriddenDeclarations(descriptor, result);
        return result.values();
    }

    private static void getOverriddenDeclarations(CallableMemberDescriptor descriptor, Map<ClassDescriptor, CallableMemberDescriptor> r) {
        if (descriptor.getKind().isReal()) {
            r.put((ClassDescriptor) descriptor.getContainingDeclaration(), descriptor);
        }
        else {
            if (descriptor.getOverriddenDescriptors().isEmpty()) {
                throw new IllegalStateException("No overridden descriptors found for (fake override) " + descriptor);
            }
            for (CallableMemberDescriptor overridden : descriptor.getOverriddenDescriptors()) {
                getOverriddenDeclarations(overridden, r);
            }
        }
    }

    public static void bindOverride(CallableMemberDescriptor fromCurrent, CallableMemberDescriptor fromSupertype) {
        fromCurrent.addOverriddenDescriptor(fromSupertype);

        for (ValueParameterDescriptor parameterFromCurrent : fromCurrent.getValueParameters()) {
            assert parameterFromCurrent.getIndex() < fromSupertype.getValueParameters().size()
                    : "An override relation between functions implies that they have the same number of value parameters";
            ValueParameterDescriptor parameterFromSupertype = fromSupertype.getValueParameters().get(parameterFromCurrent.getIndex());
            parameterFromCurrent.addOverriddenDescriptor(parameterFromSupertype);
        }
    }

    public static void generateOverridesInFunctionGroup(
            @SuppressWarnings("UnusedParameters")
            @NotNull Name name, //DO NOT DELETE THIS PARAMETER: needed to make sure all descriptors have the same name
            @NotNull Collection<? extends CallableMemberDescriptor> membersFromSupertypes,
            @NotNull Collection<? extends CallableMemberDescriptor> membersFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> notOverridden = Sets.newLinkedHashSet(membersFromSupertypes);

        for (CallableMemberDescriptor fromCurrent : membersFromCurrent) {
            Collection<CallableMemberDescriptor> bound =
                    extractAndBindOverridesForMember(fromCurrent, membersFromSupertypes, current, sink);
            notOverridden.removeAll(bound);
        }

        createAndBindFakeOverrides(current, notOverridden, sink);
    }

    private static Collection<CallableMemberDescriptor> extractAndBindOverridesForMember(
            @NotNull CallableMemberDescriptor fromCurrent,
            @NotNull Collection<? extends CallableMemberDescriptor> descriptorsFromSuper,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> bound = Lists.newArrayList();
        for (CallableMemberDescriptor fromSupertype : descriptorsFromSuper) {
            OverrideCompatibilityInfo.Result result = isOverridableBy(fromSupertype, fromCurrent).getResult();

            boolean isVisible = Visibilities.isVisible(fromSupertype, current);
            switch (result) {
                case OVERRIDABLE:
                    if (isVisible) {
                        bindOverride(fromCurrent, fromSupertype);
                    }
                    bound.add(fromSupertype);
                    break;
                case CONFLICT:
                    if (isVisible) {
                        sink.conflict(fromSupertype, fromCurrent);
                    }
                    bound.add(fromSupertype);
                    break;
                case INCOMPATIBLE:
                    break;
            }
        }
        return bound;
    }

    private static void createAndBindFakeOverrides(
            @NotNull ClassDescriptor current,
            @NotNull Collection<CallableMemberDescriptor> notOverridden,
            @NotNull DescriptorSink sink
    ) {
        Queue<CallableMemberDescriptor> fromSuperQueue = new LinkedList<CallableMemberDescriptor>(notOverridden);
        while (!fromSuperQueue.isEmpty()) {
            CallableMemberDescriptor notOverriddenFromSuper = VisibilityUtil.findMemberWithMaxVisibility(fromSuperQueue);
            Collection<CallableMemberDescriptor> overridables =
                    extractMembersOverridableInBothWays(notOverriddenFromSuper, fromSuperQueue, sink);
            createAndBindFakeOverride(overridables, current, sink);
        }
    }

    private static boolean isMoreSpecific(@NotNull CallableMemberDescriptor a, @NotNull CallableMemberDescriptor b) {
        if (a instanceof SimpleFunctionDescriptor) {
            assert b instanceof SimpleFunctionDescriptor : "b is " + b.getClass();

            JetType aReturnType = a.getReturnType();
            assert aReturnType != null;
            JetType bReturnType = b.getReturnType();
            assert bReturnType != null;

            return JetTypeChecker.INSTANCE.isSubtypeOf(aReturnType, bReturnType);
        }
        if (a instanceof PropertyDescriptor) {
            assert b instanceof PropertyDescriptor : "b is " + b.getClass();

            if (((PropertyDescriptor) a).isVar() || ((PropertyDescriptor) b).isVar()) {
                return ((PropertyDescriptor) a).isVar();
            }

            // both vals
            return JetTypeChecker.INSTANCE.isSubtypeOf(((PropertyDescriptor) a).getType(), ((PropertyDescriptor) b).getType());
        }
        throw new IllegalArgumentException("Unexpected callable: " + a.getClass());
    }

    private static CallableMemberDescriptor selectMostSpecificMemberFromSuper(@NotNull Collection<CallableMemberDescriptor> overridables) {
        CallableMemberDescriptor result = null;
        for (CallableMemberDescriptor overridable : overridables) {
            if (result == null || isMoreSpecific(overridable, result)) {
                result = overridable;
            }
        }
        return result;
    }

    private static void createAndBindFakeOverride(
            @NotNull Collection<CallableMemberDescriptor> overridables,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> visibleOverridables = filterVisibleFakeOverrides(current, overridables);
        Modality modality = getMinimalModality(visibleOverridables);
        boolean allInvisible = visibleOverridables.isEmpty();
        Collection<CallableMemberDescriptor> effectiveOverridden = allInvisible ? overridables : visibleOverridables;
        Visibility visibility = allInvisible ? Visibilities.INVISIBLE_FAKE : Visibilities.INHERITED;
        CallableMemberDescriptor mostSpecific = selectMostSpecificMemberFromSuper(effectiveOverridden);
        CallableMemberDescriptor fakeOverride =
                mostSpecific.copy(current, modality, visibility, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
        for (CallableMemberDescriptor descriptor : effectiveOverridden) {
            bindOverride(fakeOverride, descriptor);
        }
        sink.addToScope(fakeOverride);
    }

    @NotNull
    private static Modality getMinimalModality(@NotNull Collection<CallableMemberDescriptor> descriptors) {
        Modality modality = Modality.ABSTRACT;
        for (CallableMemberDescriptor descriptor : descriptors) {
            if (descriptor.getModality().compareTo(modality) < 0) {
                modality = descriptor.getModality();
            }
        }
        return modality;
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> filterVisibleFakeOverrides(
            @NotNull final ClassDescriptor current,
            @NotNull Collection<CallableMemberDescriptor> toFilter
    ) {
        return Collections2.filter(toFilter, new Predicate<CallableMemberDescriptor>() {
            @Override
            public boolean apply(@Nullable CallableMemberDescriptor descriptor) {
                //nested class could capture private member, so check for private visibility added
                return descriptor != null &&
                       descriptor.getVisibility() != Visibilities.PRIVATE &&
                       Visibilities.isVisible(descriptor, current);
            }
        });
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> extractMembersOverridableInBothWays(
            @NotNull CallableMemberDescriptor overrider,
            @NotNull Queue<CallableMemberDescriptor> extractFrom,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> overridable = Lists.newArrayList();
        overridable.add(overrider);
        for (Iterator<CallableMemberDescriptor> iterator = extractFrom.iterator(); iterator.hasNext(); ) {
            CallableMemberDescriptor candidate = iterator.next();
            if (overrider == candidate) {
                iterator.remove();
                continue;
            }

            OverrideCompatibilityInfo.Result result1 = isOverridableBy(candidate, overrider).getResult();
            OverrideCompatibilityInfo.Result result2 = isOverridableBy(overrider, candidate).getResult();
            if (result1 == OVERRIDABLE && result2 == OVERRIDABLE) {
                overridable.add(candidate);
                iterator.remove();
            }
            else if (result1 == CONFLICT || result2 == CONFLICT) {
                sink.conflict(overrider, candidate);
                iterator.remove();
            }
        }
        return overridable;
    }

    public static void resolveUnknownVisibilityForMember(
            @NotNull CallableMemberDescriptor memberDescriptor,
            @NotNull NotInferredVisibilitySink sink
    ) {
        for (CallableMemberDescriptor descriptor : memberDescriptor.getOverriddenDescriptors()) {
            if (descriptor.getVisibility() == Visibilities.INHERITED) {
                resolveUnknownVisibilityForMember(descriptor, sink);
            }
        }

        if (memberDescriptor.getVisibility() != Visibilities.INHERITED) {
            return;
        }

        Visibility visibility = findMaxVisibility(memberDescriptor.getOverriddenDescriptors());
        if (visibility == null) {
            sink.cannotInferVisibility(memberDescriptor);
            visibility = Visibilities.PUBLIC;
        }

        if (memberDescriptor instanceof PropertyDescriptorImpl) {
            ((PropertyDescriptorImpl) memberDescriptor).setVisibility(visibility.normalize());
            for (PropertyAccessorDescriptor accessor : ((PropertyDescriptor) memberDescriptor).getAccessors()) {
                resolveUnknownVisibilityForMember(accessor, sink);
            }
        }
        else if (memberDescriptor instanceof FunctionDescriptorImpl) {
            ((FunctionDescriptorImpl) memberDescriptor).setVisibility(visibility.normalize());
        }
        else {
            assert memberDescriptor instanceof PropertyAccessorDescriptorImpl;
            ((PropertyAccessorDescriptorImpl) memberDescriptor).setVisibility(visibility.normalize());
        }
    }

    @Nullable
    private static Visibility findMaxVisibility(@NotNull Collection<? extends CallableMemberDescriptor> descriptors) {
        if (descriptors.isEmpty()) {
            return Visibilities.INTERNAL;
        }
        Visibility maxVisibility = null;
        for (CallableMemberDescriptor descriptor : descriptors) {
            Visibility visibility = descriptor.getVisibility();
            assert visibility != Visibilities.INHERITED;
            if (maxVisibility == null) {
                maxVisibility = visibility;
                continue;
            }
            Integer compareResult = Visibilities.compare(visibility, maxVisibility);
            if (compareResult == null) {
                maxVisibility = null;
            }
            else if (compareResult > 0) {
                maxVisibility = visibility;
            }
        }
        if (maxVisibility == null) {
            return null;
        }
        for (CallableMemberDescriptor descriptor : descriptors) {
            Integer compareResult = Visibilities.compare(maxVisibility, descriptor.getVisibility());
            if (compareResult == null || compareResult < 0) {
                return null;
            }
        }
        return maxVisibility;
    }

    public interface DescriptorSink {
        void addToScope(@NotNull CallableMemberDescriptor fakeOverride);

        void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent);
    }

    public interface NotInferredVisibilitySink {
        void cannotInferVisibility(@NotNull CallableMemberDescriptor descriptor);
    }

    public static class OverrideCompatibilityInfo {

        public enum Result {
            OVERRIDABLE,
            INCOMPATIBLE,
            CONFLICT,
        }

        private static final OverrideCompatibilityInfo SUCCESS = new OverrideCompatibilityInfo(Result.OVERRIDABLE, "SUCCESS");

        @NotNull
        public static OverrideCompatibilityInfo success() {
            return SUCCESS;
        }

        @NotNull
        public static OverrideCompatibilityInfo nameMismatch() {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "nameMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo typeParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "typeParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo receiverPresenceMismatch() {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "receiverPresenceMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "valueParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo boundsMismatch(TypeParameterDescriptor superTypeParameter, TypeParameterDescriptor subTypeParameter) {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "boundsMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterTypeMismatch(JetType superValueParameter, JetType subValueParameter, Result result) {
            return new OverrideCompatibilityInfo(result, "valueParameterTypeMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo memberKindMismatch() {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "memberKindMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo returnTypeMismatch(JetType substitutedSuperReturnType, JetType unsubstitutedSubReturnType) {
            return new OverrideCompatibilityInfo(Result.CONFLICT, "returnTypeMismatch: " + unsubstitutedSubReturnType + " >< " + substitutedSuperReturnType); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo varOverriddenByVal() {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "varOverriddenByVal"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo externalConditionFailed(Class<? extends ExternalOverridabilityCondition> conditionClass) {
            return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "externalConditionFailed: " + conditionClass.getName()); // TODO
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private final Result overridable;
        private final String message;

        public OverrideCompatibilityInfo(Result success, String message) {
            this.overridable = success;
            this.message = message;
        }

        public Result getResult() {
            return overridable;
        }

        public String getMessage() {
            return message;
        }
    }
}
