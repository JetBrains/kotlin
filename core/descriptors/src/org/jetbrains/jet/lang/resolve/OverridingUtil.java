/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import kotlin.Function1;
import kotlin.KotlinPackage;
import kotlin.Unit;
import kotlin.jvm.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyAccessorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.utils.DFS;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*;

public class OverridingUtil {

    private static final List<ExternalOverridabilityCondition> EXTERNAL_CONDITIONS =
            KotlinPackage.toList(ServiceLoader.load(
                    ExternalOverridabilityCondition.class,
                    ExternalOverridabilityCondition.class.getClassLoader()
            ));

    public static final OverridingUtil DEFAULT = new OverridingUtil(new JetTypeChecker.TypeConstructorEquality() {
        @Override
        public boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
            return a.equals(b);
        }
    });

    @NotNull
    public static OverridingUtil createWithEqualityAxioms(@NotNull JetTypeChecker.TypeConstructorEquality equalityAxioms) {
        return new OverridingUtil(equalityAxioms);
    }

    private final JetTypeChecker.TypeConstructorEquality equalityAxioms;

    private OverridingUtil(JetTypeChecker.TypeConstructorEquality axioms) {
        equalityAxioms = axioms;
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableBy(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        return isOverridableBy(superDescriptor, subDescriptor, false);
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableByIncludingReturnType(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        return isOverridableBy(superDescriptor, subDescriptor, true);
    }

    @NotNull
    private OverrideCompatibilityInfo isOverridableBy(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            boolean checkReturnType
    ) {
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

        OverrideCompatibilityInfo receiverAndParameterResult = checkReceiverAndParameterCount(superDescriptor, subDescriptor);
        if (receiverAndParameterResult != null) {
            return receiverAndParameterResult;
        }

        List<JetType> superValueParameters = compiledValueParameters(superDescriptor);
        List<JetType> subValueParameters = compiledValueParameters(subDescriptor);

        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();

        if (superTypeParameters.size() != subTypeParameters.size()) {
            for (int i = 0; i < superValueParameters.size(); ++i) {
                JetType superValueParameterType = getUpperBound(superValueParameters.get(i));
                JetType subValueParameterType = getUpperBound(subValueParameters.get(i));
                // TODO: compare erasure
                if (!JetTypeChecker.DEFAULT.equalTypes(superValueParameterType, subValueParameterType)) {
                    return OverrideCompatibilityInfo.typeParameterNumberMismatch();
                }
            }
            return OverrideCompatibilityInfo.valueParameterTypeMismatch(null, null, OverrideCompatibilityInfo.Result.CONFLICT);
        }

        final Map<TypeConstructor, TypeConstructor> matchingTypeConstructors = new HashMap<TypeConstructor, TypeConstructor>();
        for (int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
            matchingTypeConstructors.put(superTypeParameter.getTypeConstructor(), subTypeParameter.getTypeConstructor());
        }

        JetTypeChecker.TypeConstructorEquality localEqualityAxioms = new JetTypeChecker.TypeConstructorEquality() {
            @Override
            public boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
                if (equalityAxioms.equals(a, b)) return true;
                TypeConstructor img1 = matchingTypeConstructors.get(a);
                TypeConstructor img2 = matchingTypeConstructors.get(b);
                if (!(img1 != null && img1.equals(b)) &&
                    !(img2 != null && img2.equals(a))) {
                    return false;
                }
                return true;
            }
        };

        for (int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);

            if (!areTypesEquivalent(superTypeParameter.getUpperBoundsAsType(), subTypeParameter.getUpperBoundsAsType(), localEqualityAxioms)) {
                return OverrideCompatibilityInfo.boundsMismatch(superTypeParameter, subTypeParameter);
            }
        }

        for (int i = 0, unsubstitutedValueParametersSize = superValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            JetType superValueParameter = superValueParameters.get(i);
            JetType subValueParameter = subValueParameters.get(i);

            if (!areTypesEquivalent(superValueParameter, subValueParameter, localEqualityAxioms)) {
                return OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameter, subValueParameter, INCOMPATIBLE);
            }
        }

        if (checkReturnType) {
            JetType superReturnType = superDescriptor.getReturnType();
            JetType subReturnType = subDescriptor.getReturnType();

            if (superReturnType != null && subReturnType != null) {
                boolean bothErrors = subReturnType.isError() && superReturnType.isError();
                if (!bothErrors && !JetTypeChecker.withAxioms(localEqualityAxioms).isSubtypeOf(subReturnType, superReturnType)) {
                    return OverrideCompatibilityInfo.returnTypeMismatch(superReturnType, subReturnType);
                }
            }
        }


        for (ExternalOverridabilityCondition externalCondition : EXTERNAL_CONDITIONS) {
            if (!externalCondition.isOverridable(superDescriptor, subDescriptor)) {
                return OverrideCompatibilityInfo.externalConditionFailed(externalCondition.getClass());
            }
        }

        return OverrideCompatibilityInfo.success();
    }

    @Nullable
    static OverrideCompatibilityInfo checkReceiverAndParameterCount(
            CallableDescriptor superDescriptor,
            CallableDescriptor subDescriptor
    ) {
        if ((superDescriptor.getReceiverParameter() == null) != (subDescriptor.getReceiverParameter() == null)) {
            return OverrideCompatibilityInfo.receiverPresenceMismatch();
        }

        if (superDescriptor.getValueParameters().size() != subDescriptor.getValueParameters().size()) {
            return OverrideCompatibilityInfo.valueParameterNumberMismatch();
        }

        return null;
    }

    private static boolean areTypesEquivalent(
            @NotNull JetType typeInSuper,
            @NotNull JetType typeInSub,
            @NotNull JetTypeChecker.TypeConstructorEquality axioms
    ) {
        boolean bothErrors = typeInSuper.isError() && typeInSub.isError();
        if (!bothErrors && !JetTypeChecker.withAxioms(axioms).equalTypes(typeInSuper, typeInSub)) {
            return false;
        }
        return true;
    }

    static List<JetType> compiledValueParameters(CallableDescriptor callableDescriptor) {
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

    static JetType getUpperBound(JetType type) {
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
            OverrideCompatibilityInfo.Result result = DEFAULT.isOverridableBy(fromSupertype, fromCurrent).getResult();

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

            return JetTypeChecker.DEFAULT.isSubtypeOf(aReturnType, bReturnType);
        }
        if (a instanceof PropertyDescriptor) {
            assert b instanceof PropertyDescriptor : "b is " + b.getClass();

            if (((PropertyDescriptor) a).isVar() || ((PropertyDescriptor) b).isVar()) {
                return ((PropertyDescriptor) a).isVar();
            }

            // both vals
            return JetTypeChecker.DEFAULT.isSubtypeOf(((PropertyDescriptor) a).getType(), ((PropertyDescriptor) b).getType());
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
        boolean allInvisible = visibleOverridables.isEmpty();
        Collection<CallableMemberDescriptor> effectiveOverridden = allInvisible ? overridables : visibleOverridables;

        Modality modality = getMinimalModality(effectiveOverridden);
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
        return KotlinPackage.filter(toFilter, new Function1<CallableMemberDescriptor, Boolean>() {
            @Override
            public Boolean invoke(CallableMemberDescriptor descriptor) {
                //nested class could capture private member, so check for private visibility added
                return descriptor.getVisibility() != Visibilities.PRIVATE && Visibilities.isVisible(descriptor, current);
            }
        });
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> extractMembersOverridableInBothWays(
            @NotNull CallableMemberDescriptor overrider,
            @NotNull Queue<CallableMemberDescriptor> extractFrom,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> overridable = new ArrayList<CallableMemberDescriptor>();
        overridable.add(overrider);
        for (Iterator<CallableMemberDescriptor> iterator = extractFrom.iterator(); iterator.hasNext(); ) {
            CallableMemberDescriptor candidate = iterator.next();
            if (overrider == candidate) {
                iterator.remove();
                continue;
            }

            OverrideCompatibilityInfo.Result result1 = DEFAULT.isOverridableBy(candidate, overrider).getResult();
            OverrideCompatibilityInfo.Result result2 = DEFAULT.isOverridableBy(overrider, candidate).getResult();
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
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = memberDescriptor.getOverriddenDescriptors();
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
    private static Visibility findMaxVisibility(@NotNull Collection<? extends CallableMemberDescriptor> descriptors) {
        if (descriptors.isEmpty()) {
            return Visibilities.INTERNAL;
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
        // TODO: IDEA seems to issue an incorrect warning here
        //noinspection ConstantConditions
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


    @NotNull
    @KotlinSignature("fun getTopmostOverridenDescriptors(originalDescriptor: CallableDescriptor): List<out CallableDescriptor>")
    public static List<? extends CallableDescriptor> getTopmostOverridenDescriptors(@NotNull CallableDescriptor originalDescriptor) {
        return DFS.dfs(
                Collections.singletonList(originalDescriptor),
                new DFS.Neighbors<CallableDescriptor>() {
                    @NotNull
                    @Override
                    public Iterable<? extends CallableDescriptor> getNeighbors(CallableDescriptor current) {
                        return current.getOverriddenDescriptors();
                    }
                },
                new DFS.CollectingNodeHandler<CallableDescriptor, CallableDescriptor, ArrayList<CallableDescriptor>>(
                        new ArrayList<CallableDescriptor>()
                ) {
                    @Override
                    public void afterChildren(CallableDescriptor current) {
                        if (current.getOverriddenDescriptors().isEmpty()) {
                            result.add(current);
                        }
                    }
                }
        );
    }

    public static boolean traverseOverridenDescriptors(
            @NotNull CallableDescriptor originalDescriptor,
            @NotNull final Function1<CallableDescriptor, Boolean> handler
    ) {
        return DFS.dfs(
                Collections.singletonList(originalDescriptor),
                new DFS.Neighbors<CallableDescriptor>() {
                    @NotNull
                    @Override
                    public Iterable<? extends CallableDescriptor> getNeighbors(CallableDescriptor current) {
                        return current.getOverriddenDescriptors();
                    }
                },
                new DFS.AbstractNodeHandler<CallableDescriptor, Boolean>() {
                    private boolean result = true;

                    @Override
                    public boolean beforeChildren(CallableDescriptor current) {
                        if (!handler.invoke(current)) {
                            result = false;
                        }
                        return result;
                    }

                    @Override
                    public Boolean result() {
                        return result;
                    }
                }
        );
    }

    public interface DescriptorSink {
        void addToScope(@NotNull CallableMemberDescriptor fakeOverride);

        void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent);
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
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "nameMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo typeParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "typeParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo receiverPresenceMismatch() {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "receiverPresenceMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "valueParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo boundsMismatch(TypeParameterDescriptor superTypeParameter, TypeParameterDescriptor subTypeParameter) {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "boundsMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterTypeMismatch(JetType superValueParameter, JetType subValueParameter, Result result) {
            return new OverrideCompatibilityInfo(result, "valueParameterTypeMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo memberKindMismatch() {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "memberKindMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo returnTypeMismatch(JetType substitutedSuperReturnType, JetType unsubstitutedSubReturnType) {
            return new OverrideCompatibilityInfo(Result.CONFLICT, "returnTypeMismatch: " + unsubstitutedSubReturnType + " >< " + substitutedSuperReturnType); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo varOverriddenByVal() {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "varOverriddenByVal"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo externalConditionFailed(Class<? extends ExternalOverridabilityCondition> conditionClass) {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, "externalConditionFailed: " + conditionClass.getName()); // TODO
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
