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

package org.jetbrains.kotlin.resolve;

import kotlin.collections.CollectionsKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertyAccessorDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.FlexibleTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*;

public class OverridingUtil {

    private static final List<ExternalOverridabilityCondition> EXTERNAL_CONDITIONS =
            CollectionsKt.toList(ServiceLoader.load(
                    ExternalOverridabilityCondition.class,
                    ExternalOverridabilityCondition.class.getClassLoader()
            ));

    public static final OverridingUtil DEFAULT = new OverridingUtil(new KotlinTypeChecker.TypeConstructorEquality() {
        @Override
        public boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
            return a.equals(b);
        }
    });

    @NotNull
    public static OverridingUtil createWithEqualityAxioms(@NotNull KotlinTypeChecker.TypeConstructorEquality equalityAxioms) {
        return new OverridingUtil(equalityAxioms);
    }

    private final KotlinTypeChecker.TypeConstructorEquality equalityAxioms;

    private OverridingUtil(KotlinTypeChecker.TypeConstructorEquality axioms) {
        equalityAxioms = axioms;
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableBy(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            @Nullable ClassDescriptor subClassDescriptor
    ) {
        return isOverridableBy(superDescriptor, subDescriptor, subClassDescriptor, false);
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableByIncludingReturnType(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        return isOverridableBy(superDescriptor, subDescriptor, null, true);
    }

    @NotNull
    private OverrideCompatibilityInfo isOverridableBy(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            @Nullable ClassDescriptor subClassDescriptor,
            boolean checkReturnType
    ) {
        boolean wasSuccessfulExternalCondition = false;
        for (ExternalOverridabilityCondition externalCondition : EXTERNAL_CONDITIONS) {
            ExternalOverridabilityCondition.Result result =
                    externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor);
            switch (result) {
                case OVERRIDABLE:
                    wasSuccessfulExternalCondition = true;
                    break;
                case CONFLICT:
                    return OverrideCompatibilityInfo.conflict("External condition failed");
                case INCOMPATIBLE:
                    return OverrideCompatibilityInfo.incompatible("External condition");
                case UNKNOWN:
                    // do nothing
                    // go to the next external condition or default override check
            }
        }

        if (wasSuccessfulExternalCondition) {
            return OverrideCompatibilityInfo.success();
        }

        return isOverridableByWithoutExternalConditions(superDescriptor, subDescriptor, checkReturnType);
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableByWithoutExternalConditions(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            boolean checkReturnType
    ) {
        if (superDescriptor instanceof FunctionDescriptor && !(subDescriptor instanceof FunctionDescriptor) ||
            superDescriptor instanceof PropertyDescriptor && !(subDescriptor instanceof PropertyDescriptor)) {
            return OverrideCompatibilityInfo.incompatible("Member kind mismatch");
        }

        if (!(superDescriptor instanceof FunctionDescriptor) && !(superDescriptor instanceof PropertyDescriptor)) {
            throw new IllegalArgumentException("This type of CallableDescriptor cannot be checked for overridability: " + superDescriptor);
        }

        // TODO: check outside of this method
        if (!superDescriptor.getName().equals(subDescriptor.getName())) {
            return OverrideCompatibilityInfo.incompatible("Name mismatch");
        }

        OverrideCompatibilityInfo receiverAndParameterResult = checkReceiverAndParameterCount(superDescriptor, subDescriptor);
        if (receiverAndParameterResult != null) {
            return receiverAndParameterResult;
        }

        List<KotlinType> superValueParameters = compiledValueParameters(superDescriptor);
        List<KotlinType> subValueParameters = compiledValueParameters(subDescriptor);

        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();

        if (superTypeParameters.size() != subTypeParameters.size()) {
            for (int i = 0; i < superValueParameters.size(); ++i) {
                // TODO: compare erasure
                if (!KotlinTypeChecker.DEFAULT.equalTypes(superValueParameters.get(i), subValueParameters.get(i))) {
                    return OverrideCompatibilityInfo.incompatible("Type parameter number mismatch");
                }
            }
            return OverrideCompatibilityInfo.conflict("Type parameter number mismatch");
        }

        KotlinTypeChecker typeChecker = createTypeChecker(superTypeParameters, subTypeParameters);

        for (int i = 0; i < superTypeParameters.size(); i++) {
            if (!areTypeParametersEquivalent(superTypeParameters.get(i), subTypeParameters.get(i), typeChecker)) {
                return OverrideCompatibilityInfo.incompatible("Type parameter bounds mismatch");
            }
        }

        for (int i = 0; i < superValueParameters.size(); i++) {
            if (!areTypesEquivalent(superValueParameters.get(i), subValueParameters.get(i), typeChecker)) {
                return OverrideCompatibilityInfo.incompatible("Value parameter type mismatch");
            }
        }

        if (checkReturnType) {
            KotlinType superReturnType = superDescriptor.getReturnType();
            KotlinType subReturnType = subDescriptor.getReturnType();

            if (superReturnType != null && subReturnType != null) {
                boolean bothErrors = subReturnType.isError() && superReturnType.isError();
                if (!bothErrors && !typeChecker.isSubtypeOf(subReturnType, superReturnType)) {
                    return OverrideCompatibilityInfo.conflict("Return type mismatch");
                }
            }
        }

        return OverrideCompatibilityInfo.success();
    }

    @NotNull
    private KotlinTypeChecker createTypeChecker(
            @NotNull List<TypeParameterDescriptor> firstParameters,
            @NotNull List<TypeParameterDescriptor> secondParameters
    ) {
        assert firstParameters.size() == secondParameters.size() :
                "Should be the same number of type parameters: " + firstParameters + " vs " + secondParameters;
        if (firstParameters.isEmpty()) return KotlinTypeChecker.withAxioms(equalityAxioms);

        final Map<TypeConstructor, TypeConstructor> matchingTypeConstructors = new HashMap<TypeConstructor, TypeConstructor>();
        for (int i = 0; i < firstParameters.size(); i++) {
            matchingTypeConstructors.put(firstParameters.get(i).getTypeConstructor(), secondParameters.get(i).getTypeConstructor());
        }

        return KotlinTypeChecker.withAxioms(new KotlinTypeChecker.TypeConstructorEquality() {
            @Override
            public boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
                if (equalityAxioms.equals(a, b)) return true;
                TypeConstructor img1 = matchingTypeConstructors.get(a);
                TypeConstructor img2 = matchingTypeConstructors.get(b);
                return (img1 != null && img1.equals(b)) || (img2 != null && img2.equals(a));
            }
        });
    }

    @Nullable
    static OverrideCompatibilityInfo checkReceiverAndParameterCount(
            CallableDescriptor superDescriptor,
            CallableDescriptor subDescriptor
    ) {
        if ((superDescriptor.getExtensionReceiverParameter() == null) != (subDescriptor.getExtensionReceiverParameter() == null)) {
            return OverrideCompatibilityInfo.incompatible("Receiver presence mismatch");
        }

        if (superDescriptor.getValueParameters().size() != subDescriptor.getValueParameters().size()) {
            return OverrideCompatibilityInfo.incompatible("Value parameter number mismatch");
        }

        return null;
    }

    private static boolean areTypesEquivalent(
            @NotNull KotlinType typeInSuper,
            @NotNull KotlinType typeInSub,
            @NotNull KotlinTypeChecker typeChecker
    ) {
        boolean bothErrors = typeInSuper.isError() && typeInSub.isError();
        return bothErrors || typeChecker.equalTypes(typeInSuper, typeInSub);
    }

    // See JLS 8, 8.4.4 Generic Methods
    // TODO: use TypeSubstitutor instead
    private static boolean areTypeParametersEquivalent(
            @NotNull TypeParameterDescriptor superTypeParameter,
            @NotNull TypeParameterDescriptor subTypeParameter,
            @NotNull KotlinTypeChecker typeChecker
    ) {
        List<KotlinType> superBounds = superTypeParameter.getUpperBounds();
        List<KotlinType> subBounds = new ArrayList<KotlinType>(subTypeParameter.getUpperBounds());
        if (superBounds.size() != subBounds.size()) return false;

        outer:
        for (KotlinType superBound : superBounds) {
            ListIterator<KotlinType> it = subBounds.listIterator();
            while (it.hasNext()) {
                KotlinType subBound = it.next();
                if (areTypesEquivalent(superBound, subBound, typeChecker)) {
                    it.remove();
                    continue outer;
                }
            }
            return false;
        }

        return true;
    }

    static List<KotlinType> compiledValueParameters(CallableDescriptor callableDescriptor) {
        ReceiverParameterDescriptor receiverParameter = callableDescriptor.getExtensionReceiverParameter();
        List<KotlinType> parameters = new ArrayList<KotlinType>();
        if (receiverParameter != null) {
            parameters.add(receiverParameter.getType());
        }
        for (ValueParameterDescriptor valueParameterDescriptor : callableDescriptor.getValueParameters()) {
            parameters.add(valueParameterDescriptor.getType());
        }
        return parameters;
    }

    public static void generateOverridesInFunctionGroup(
            @SuppressWarnings("UnusedParameters")
            @NotNull Name name, //DO NOT DELETE THIS PARAMETER: needed to make sure all descriptors have the same name
            @NotNull Collection<? extends CallableMemberDescriptor> membersFromSupertypes,
            @NotNull Collection<? extends CallableMemberDescriptor> membersFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> notOverridden = new LinkedHashSet<CallableMemberDescriptor>(membersFromSupertypes);

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
        Collection<CallableMemberDescriptor> bound = new ArrayList<CallableMemberDescriptor>(descriptorsFromSuper.size());
        for (CallableMemberDescriptor fromSupertype : descriptorsFromSuper) {
            OverrideCompatibilityInfo.Result result = DEFAULT.isOverridableBy(fromSupertype, fromCurrent, current).getResult();

            boolean isVisible = Visibilities.isVisibleWithIrrelevantReceiver(fromSupertype, current);
            switch (result) {
                case OVERRIDABLE:
                    if (isVisible) {
                        fromCurrent.addOverriddenDescriptor(fromSupertype);
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
            CallableMemberDescriptor notOverriddenFromSuper = VisibilityUtilKt.findMemberWithMaxVisibility(fromSuperQueue);
            Collection<CallableMemberDescriptor> overridables =
                    extractMembersOverridableInBothWays(notOverriddenFromSuper, fromSuperQueue, sink);
            createAndBindFakeOverride(overridables, current, sink);
        }
    }

    public static boolean isMoreSpecific(@NotNull CallableDescriptor a, @NotNull CallableDescriptor b) {
        KotlinType aReturnType = a.getReturnType();
        KotlinType bReturnType = b.getReturnType();

        assert aReturnType != null : "Return type of " + a + " is null";
        assert bReturnType != null : "Return type of " + b + " is null";

        if (a instanceof SimpleFunctionDescriptor) {
            assert b instanceof SimpleFunctionDescriptor : "b is " + b.getClass();

            return isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType);
        }
        if (a instanceof PropertyDescriptor) {
            assert b instanceof PropertyDescriptor : "b is " + b.getClass();

            PropertyDescriptor pa = (PropertyDescriptor) a;
            PropertyDescriptor pb = (PropertyDescriptor) b;
            if (pa.isVar() && pb.isVar()) {
                return DEFAULT.createTypeChecker(a.getTypeParameters(), b.getTypeParameters()).equalTypes(aReturnType, bReturnType);
            }
            else {
                // both vals or var vs val: val can't be more specific then var
                return !(!pa.isVar() && pb.isVar()) && isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType);
            }
        }
        throw new IllegalArgumentException("Unexpected callable: " + a.getClass());
    }

    private static boolean isMoreSpecificThenAllOf(@NotNull CallableDescriptor candidate, @NotNull Collection<CallableDescriptor> descriptors) {
        // NB subtyping relation in Kotlin is not transitive in presence of flexible types:
        //  String? <: String! <: String, but not String? <: String
        for (CallableDescriptor descriptor : descriptors) {
            if (!isMoreSpecific(candidate, descriptor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isReturnTypeMoreSpecific(
            @NotNull CallableDescriptor a,
            @NotNull KotlinType aReturnType,
            @NotNull CallableDescriptor b,
            @NotNull KotlinType bReturnType
    ) {
        KotlinTypeChecker typeChecker = DEFAULT.createTypeChecker(a.getTypeParameters(), b.getTypeParameters());
        return typeChecker.isSubtypeOf(aReturnType, bReturnType);
    }

    @NotNull
    public static <H> H selectMostSpecificMember(
            @NotNull Collection<H> overridables,
            @NotNull Function1<H, CallableDescriptor> descriptorByHandle
    ) {
        assert !overridables.isEmpty() : "Should have at least one overridable descriptor";

        if (overridables.size() == 1) {
            return CollectionsKt.first(overridables);
        }

        Collection<H> candidates = new ArrayList<H>(2);
        List<CallableDescriptor> callableMemberDescriptors = CollectionsKt.map(overridables, descriptorByHandle);

        H transitivelyMostSpecific = CollectionsKt.first(overridables);
        CallableDescriptor transitivelyMostSpecificDescriptor = descriptorByHandle.invoke(transitivelyMostSpecific);

        for (H overridable : overridables) {
            CallableDescriptor descriptor = descriptorByHandle.invoke(overridable);
            if (isMoreSpecificThenAllOf(descriptor, callableMemberDescriptors)) {
                candidates.add(overridable);
            }
            if (isMoreSpecific(descriptor, transitivelyMostSpecificDescriptor)
                && !isMoreSpecific(transitivelyMostSpecificDescriptor, descriptor)) {
                transitivelyMostSpecific = overridable;
            }
        }

        if (candidates.isEmpty()) {
            return transitivelyMostSpecific;
        }
        else if (candidates.size() == 1) {
            return CollectionsKt.first(candidates);
        }

        H firstNonFlexible = null;
        for (H candidate : candidates) {
            if (!FlexibleTypesKt.isFlexible(descriptorByHandle.invoke(candidate).getReturnType())) {
                firstNonFlexible = candidate;
                break;
            }
        }
        if (firstNonFlexible != null) {
            return firstNonFlexible;
        }

        return CollectionsKt.first(candidates);
    }

    private static void createAndBindFakeOverride(
            @NotNull Collection<CallableMemberDescriptor> overridables,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> visibleOverridables = filterVisibleFakeOverrides(current, overridables);
        boolean allInvisible = visibleOverridables.isEmpty();
        Collection<CallableMemberDescriptor> effectiveOverridden = allInvisible ? overridables : visibleOverridables;

        // FIXME doesn't work as expected for flexible types: should create a refined signature.
        // Current algorithm produces bad results in presence of annotated Java signatures such as:
        //      J: foo(s: String!): String -- @NotNull String foo(String s);
        //      K: foo(s: String): String?
        //  --> 'foo(s: String!): String' as an inherited signature with most specific return type.
        // This is bad because it can be overridden by 'foo(s: String?): String', which is not override-equivalent with K::foo above.
        // Should be 'foo(s: String): String'.
        Modality modality = getMinimalModality(effectiveOverridden);
        Visibility visibility = allInvisible ? Visibilities.INVISIBLE_FAKE : Visibilities.INHERITED;
        CallableMemberDescriptor mostSpecific =
                selectMostSpecificMember(effectiveOverridden,
                                         new Function1<CallableMemberDescriptor, CallableDescriptor>() {
                                              @Override
                                              public CallableMemberDescriptor invoke(CallableMemberDescriptor descriptor) {
                                                  return descriptor;
                                              }
                                         });
        CallableMemberDescriptor fakeOverride =
                mostSpecific.copy(current, modality, visibility, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
        for (CallableMemberDescriptor descriptor : effectiveOverridden) {
            fakeOverride.addOverriddenDescriptor(descriptor);
        }
        sink.addFakeOverride(fakeOverride);
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
        return CollectionsKt.filter(toFilter, new Function1<CallableMemberDescriptor, Boolean>() {
            @Override
            public Boolean invoke(CallableMemberDescriptor descriptor) {
                //nested class could capture private member, so check for private visibility added
                return !Visibilities.isPrivate(descriptor.getVisibility()) &&
                       Visibilities.isVisibleWithIrrelevantReceiver(descriptor, current);
            }
        });
    }

    /**
     * @param <H> is something that handles CallableDescriptor inside
     * @return
     */
    @NotNull
    public static <H> Collection<H> extractMembersOverridableInBothWays(
            @NotNull H overrider,
            @NotNull @Mutable Collection<H> extractFrom,
            @NotNull Function1<H, CallableDescriptor> descriptorByHandle,
            @NotNull Function1<H, Unit> onConflict
    ) {
        Collection<H> overridable = new ArrayList<H>();
        overridable.add(overrider);
        CallableDescriptor overriderDescriptor = descriptorByHandle.invoke(overrider);
        for (Iterator<H> iterator = extractFrom.iterator(); iterator.hasNext(); ) {
            H candidate = iterator.next();
            CallableDescriptor candidateDescriptor = descriptorByHandle.invoke(candidate);
            if (overrider == candidate) {
                iterator.remove();
                continue;
            }

            OverrideCompatibilityInfo.Result finalResult = getBothWaysOverridability(overriderDescriptor, candidateDescriptor);

            if (finalResult == OVERRIDABLE) {
                overridable.add(candidate);
                iterator.remove();
            }
            else if (finalResult == CONFLICT) {
                onConflict.invoke(candidate);
                iterator.remove();
            }
        }
        return overridable;
    }

    @Nullable
    public static OverrideCompatibilityInfo.Result getBothWaysOverridability(
            CallableDescriptor overriderDescriptor,
            CallableDescriptor candidateDescriptor
    ) {
        OverrideCompatibilityInfo.Result result1 = DEFAULT.isOverridableBy(candidateDescriptor, overriderDescriptor, null).getResult();
        OverrideCompatibilityInfo.Result result2 = DEFAULT.isOverridableBy(overriderDescriptor, candidateDescriptor, null).getResult();

        return result1 == OVERRIDABLE && result2 == OVERRIDABLE
               ? OVERRIDABLE
               : ((result1 == CONFLICT || result2 == CONFLICT) ? CONFLICT : INCOMPATIBLE);
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> extractMembersOverridableInBothWays(
            @NotNull final CallableMemberDescriptor overrider,
            @NotNull Queue<CallableMemberDescriptor> extractFrom,
            @NotNull final DescriptorSink sink
    ) {
        return extractMembersOverridableInBothWays(overrider, extractFrom,
                // ID
                new Function1<CallableMemberDescriptor, CallableDescriptor>() {
                    @Override
                    public CallableDescriptor invoke(CallableMemberDescriptor descriptor) {
                        return descriptor;
                    }
                },
                new Function1<CallableMemberDescriptor, Unit>() {
                    @Override
                    public Unit invoke(CallableMemberDescriptor descriptor) {
                        sink.conflict(overrider, descriptor);
                        return Unit.INSTANCE;
                    }
                });
    }


    public static void resolveUnknownVisibilityForMember(
            @NotNull CallableMemberDescriptor memberDescriptor,
            @Nullable Function1<CallableMemberDescriptor, Unit> cannotInferVisibility
    ) {
        for (CallableMemberDescriptor descriptor : memberDescriptor.getOverriddenDescriptors()) {
            if (descriptor.getVisibility() == Visibilities.INHERITED) {
                resolveUnknownVisibilityForMember(descriptor, cannotInferVisibility);
            }
        }

        if (memberDescriptor.getVisibility() != Visibilities.INHERITED) {
            return;
        }

        Visibility maxVisibility = computeVisibilityToInherit(memberDescriptor);
        Visibility visibilityToInherit;
        if (maxVisibility == null) {
            if (cannotInferVisibility != null) {
                cannotInferVisibility.invoke(memberDescriptor);
            }
            visibilityToInherit = Visibilities.PUBLIC;
        }
        else {
            visibilityToInherit = maxVisibility;
        }

        if (memberDescriptor instanceof PropertyDescriptorImpl) {
            ((PropertyDescriptorImpl) memberDescriptor).setVisibility(visibilityToInherit);
            for (PropertyAccessorDescriptor accessor : ((PropertyDescriptor) memberDescriptor).getAccessors()) {
                // If we couldn't infer visibility for property, the diagnostic is already reported, no need to report it again on accessors
                resolveUnknownVisibilityForMember(accessor, maxVisibility == null ? null : cannotInferVisibility);
            }
        }
        else if (memberDescriptor instanceof FunctionDescriptorImpl) {
            ((FunctionDescriptorImpl) memberDescriptor).setVisibility(visibilityToInherit);
        }
        else {
            assert memberDescriptor instanceof PropertyAccessorDescriptorImpl;
            ((PropertyAccessorDescriptorImpl) memberDescriptor).setVisibility(visibilityToInherit);
        }
    }

    @Nullable
    private static Visibility computeVisibilityToInherit(@NotNull CallableMemberDescriptor memberDescriptor) {
        Collection<? extends CallableMemberDescriptor> overriddenDescriptors = memberDescriptor.getOverriddenDescriptors();
        Visibility maxVisibility = findMaxVisibility(overriddenDescriptors);
        if (maxVisibility == null) {
            return null;
        }
        if (memberDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            for (CallableMemberDescriptor overridden : overriddenDescriptors) {
                // An implementation (a non-abstract overridden member) of a fake override should have the maximum possible visibility
                if (overridden.getModality() != Modality.ABSTRACT && !overridden.getVisibility().equals(maxVisibility)) {
                    return null;
                }
            }
            return maxVisibility;
        }
        return maxVisibility.normalize();
    }

    @Nullable
    public static Visibility findMaxVisibility(@NotNull Collection<? extends CallableMemberDescriptor> descriptors) {
        if (descriptors.isEmpty()) {
            return Visibilities.DEFAULT_VISIBILITY;
        }
        Visibility maxVisibility = null;
        for (CallableMemberDescriptor descriptor : descriptors) {
            Visibility visibility = descriptor.getVisibility();
            assert visibility != Visibilities.INHERITED : "Visibility should have been computed for " + descriptor;
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
        void addFakeOverride(@NotNull CallableMemberDescriptor fakeOverride);

        void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent);
    }

    public static class OverrideCompatibilityInfo {
        public enum Result {
            OVERRIDABLE,
            INCOMPATIBLE,
            CONFLICT,
        }

        private static final OverrideCompatibilityInfo SUCCESS = new OverrideCompatibilityInfo(OVERRIDABLE, "SUCCESS");

        @NotNull
        public static OverrideCompatibilityInfo success() {
            return SUCCESS;
        }

        @NotNull
        public static OverrideCompatibilityInfo incompatible(@NotNull String debugMessage) {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, debugMessage);
        }

        @NotNull
        public static OverrideCompatibilityInfo conflict(@NotNull String debugMessage) {
            return new OverrideCompatibilityInfo(CONFLICT, debugMessage);
        }

        private final Result overridable;
        private final String debugMessage;

        public OverrideCompatibilityInfo(@NotNull Result success, @NotNull String debugMessage) {
            this.overridable = success;
            this.debugMessage = debugMessage;
        }

        @NotNull
        public Result getResult() {
            return overridable;
        }

        @NotNull
        public String getDebugMessage() {
            return debugMessage;
        }
    }
}
