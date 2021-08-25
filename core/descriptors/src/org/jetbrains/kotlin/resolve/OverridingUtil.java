/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import kotlin.Pair;
import kotlin.Unit;
import kotlin.annotations.jvm.Mutable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertyAccessorDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.checker.KotlinTypePreparator;
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner;
import org.jetbrains.kotlin.types.checker.NewKotlinTypeCheckerImpl;
import org.jetbrains.kotlin.utils.SmartSet;

import java.util.*;

import static org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*;

public class OverridingUtil {

    private static final List<ExternalOverridabilityCondition> EXTERNAL_CONDITIONS =
            CollectionsKt.toList(ServiceLoader.load(
                    ExternalOverridabilityCondition.class,
                    ExternalOverridabilityCondition.class.getClassLoader()
            ));

    public static final OverridingUtil DEFAULT;

    private static final KotlinTypeChecker.TypeConstructorEquality DEFAULT_TYPE_CONSTRUCTOR_EQUALITY =
            new KotlinTypeChecker.TypeConstructorEquality() {
                @Override
                public boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
                    return a.equals(b);
                }
            };

    static {

        DEFAULT = new OverridingUtil(DEFAULT_TYPE_CONSTRUCTOR_EQUALITY, KotlinTypeRefiner.Default.INSTANCE);
    }

    @NotNull
    public static OverridingUtil createWithEqualityAxioms(@NotNull KotlinTypeChecker.TypeConstructorEquality equalityAxioms) {
        return new OverridingUtil(equalityAxioms, KotlinTypeRefiner.Default.INSTANCE);
    }

    @NotNull
    public static OverridingUtil createWithTypeRefiner(@NotNull KotlinTypeRefiner kotlinTypeRefiner) {
        return new OverridingUtil(DEFAULT_TYPE_CONSTRUCTOR_EQUALITY, kotlinTypeRefiner);
    }

    @NotNull
    public static OverridingUtil create(
            @NotNull KotlinTypeRefiner kotlinTypeRefiner,
            @NotNull KotlinTypeChecker.TypeConstructorEquality equalityAxioms
    ) {
            return new OverridingUtil(equalityAxioms, kotlinTypeRefiner);
    }

    private final KotlinTypeRefiner kotlinTypeRefiner;
    private final KotlinTypeChecker.TypeConstructorEquality equalityAxioms;

    private OverridingUtil(
            @NotNull KotlinTypeChecker.TypeConstructorEquality axioms,
            @NotNull KotlinTypeRefiner kotlinTypeRefiner
    ) {
        equalityAxioms = axioms;
        this.kotlinTypeRefiner = kotlinTypeRefiner;
    }

    /**
     * Given a set of descriptors, returns a set containing all the given descriptors except those which _are overridden_ by at least
     * one other descriptor from the original set.
     */
    @NotNull
    public static <D extends CallableDescriptor> Set<D> filterOutOverridden(@NotNull Set<D> candidateSet) {
        boolean allowDescriptorCopies = !candidateSet.isEmpty() &&
                                        DescriptorUtilsKt
                                                .isTypeRefinementEnabled(DescriptorUtilsKt.getModule(candidateSet.iterator().next()));

        return filterOverrides(candidateSet, allowDescriptorCopies, null, new Function2<D, D, Pair<CallableDescriptor, CallableDescriptor>>() {
            @Override
            public Pair<CallableDescriptor, CallableDescriptor> invoke(D a, D b) {
                return new Pair<CallableDescriptor, CallableDescriptor>(a, b);
            }
        });
    }

    @NotNull
    public static <D> Set<D> filterOverrides(
            @NotNull Set<D> candidateSet,
            boolean allowDescriptorCopies,
            @Nullable Function0<?> cancellationCallback,
            @NotNull Function2<? super D, ? super D, Pair<CallableDescriptor, CallableDescriptor>> transformFirst
    ) {
        if (candidateSet.size() <= 1) return candidateSet;

        Set<D> result = new LinkedHashSet<D>();
        outerLoop:
        for (D meD : candidateSet) {
            if (cancellationCallback != null) {
                cancellationCallback.invoke();
            }
            for (Iterator<D> iterator = result.iterator(); iterator.hasNext(); ) {
                D otherD = iterator.next();
                Pair<CallableDescriptor, CallableDescriptor> meAndOther = transformFirst.invoke(meD, otherD);
                CallableDescriptor me = meAndOther.component1();
                CallableDescriptor other = meAndOther.component2();
                if (overrides(me, other, allowDescriptorCopies, true)) {
                    iterator.remove();
                }
                else if (overrides(other, me, allowDescriptorCopies, true)) {
                    continue outerLoop;
                }
            }
            result.add(meD);
        }

        assert !result.isEmpty() : "All candidates filtered out from " + candidateSet;

        return result;
    }

    /**
     * @return whether f overrides g
     */
    public static <D extends CallableDescriptor> boolean overrides(
            @NotNull D f,
            @NotNull D g,
            boolean allowDeclarationCopies,
            boolean distinguishExpectsAndNonExpects
    ) {
        // In a multi-module project different "copies" of the same class may be present in different libraries,
        // that's why we use structural equivalence for members (DescriptorEquivalenceForOverrides).

        // This first check cover the case of duplicate classes in different modules:
        // when B is defined in modules m1 and m2, and C (indirectly) inherits from both versions,
        // we'll be getting sets of members that do not override each other, but are structurally equivalent.
        // As other code relies on no equal descriptors passed here, we guard against f == g, but this may not be necessary
        // Note that this is needed for the usage of this function in the IDE code
        if (!f.equals(g)
            && DescriptorEquivalenceForOverrides.INSTANCE.areEquivalent(
                    f.getOriginal(),
                    g.getOriginal(),
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
            )
        ) {
            return true;
        }

        CallableDescriptor originalG = g.getOriginal();
        for (D overriddenFunction : DescriptorUtils.getAllOverriddenDescriptors(f)) {
            if (DescriptorEquivalenceForOverrides.INSTANCE.areEquivalent(
                    originalG,
                    overriddenFunction,
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
            )) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return overridden real descriptors (not fake overrides). Note that most usages of this method should be followed by calling
     * {@link #filterOutOverridden(Set)}, because some of the declarations can override the other.
     */
    @NotNull
    public static Set<CallableMemberDescriptor> getOverriddenDeclarations(@NotNull CallableMemberDescriptor descriptor) {
        Set<CallableMemberDescriptor> result = new LinkedHashSet<CallableMemberDescriptor>();
        collectOverriddenDeclarations(descriptor, result);
        return result;
    }

    private static void collectOverriddenDeclarations(
            @NotNull CallableMemberDescriptor descriptor,
            @NotNull Set<CallableMemberDescriptor> result
    ) {
        if (descriptor.getKind().isReal()) {
            result.add(descriptor);
        }
        else {
            if (descriptor.getOverriddenDescriptors().isEmpty()) {
                throw new IllegalStateException("No overridden descriptors found for (fake override) " + descriptor);
            }
            for (CallableMemberDescriptor overridden : descriptor.getOverriddenDescriptors()) {
                collectOverriddenDeclarations(overridden, result);
            }
        }
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
    public OverrideCompatibilityInfo isOverridableBy(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            @Nullable ClassDescriptor subClassDescriptor,
            boolean checkReturnType
    ) {
        OverrideCompatibilityInfo basicResult = isOverridableByWithoutExternalConditions(superDescriptor, subDescriptor, checkReturnType);
        boolean wasSuccess = basicResult.getResult() == OVERRIDABLE;

        for (ExternalOverridabilityCondition externalCondition : EXTERNAL_CONDITIONS) {
            // Do not run CONFLICTS_ONLY while there was no success
            if (externalCondition.getContract() == ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) continue;
            if (wasSuccess && externalCondition.getContract() == ExternalOverridabilityCondition.Contract.SUCCESS_ONLY) continue;

            ExternalOverridabilityCondition.Result result =
                    externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor);

            switch (result) {
                case OVERRIDABLE:
                    wasSuccess = true;
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

        if (!wasSuccess) {
            return basicResult;
        }

        // Search for conflicts from external conditions
        for (ExternalOverridabilityCondition externalCondition : EXTERNAL_CONDITIONS) {
            // Run all conditions that was not run before (i.e. CONFLICTS_ONLY)
            if (externalCondition.getContract() != ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) continue;

            ExternalOverridabilityCondition.Result result =
                    externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor);
            switch (result) {
                case CONFLICT:
                    return OverrideCompatibilityInfo.conflict("External condition failed");
                case INCOMPATIBLE:
                    return OverrideCompatibilityInfo.incompatible("External condition");
                case OVERRIDABLE:
                    throw new IllegalStateException(
                            "Contract violation in " + externalCondition.getClass().getName() + " condition. It's not supposed to end with success");
                case UNKNOWN:
                    // do nothing
                    // go to the next external condition or default override check
            }
        }

        return OverrideCompatibilityInfo.success();
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableByWithoutExternalConditions(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            boolean checkReturnType
    ) {
        OverrideCompatibilityInfo basicOverridability = getBasicOverridabilityProblem(superDescriptor, subDescriptor);
        if (basicOverridability != null) return basicOverridability;

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

        Pair<NewKotlinTypeCheckerImpl, TypeCheckerState> typeChecker = createTypeChecker(superTypeParameters, subTypeParameters);

        for (int i = 0; i < superTypeParameters.size(); i++) {
            if (!areTypeParametersEquivalent(
                    superTypeParameters.get(i),
                    subTypeParameters.get(i),
                    typeChecker
            )) {
                return OverrideCompatibilityInfo.incompatible("Type parameter bounds mismatch");
            }
        }

        for (int i = 0; i < superValueParameters.size(); i++) {
            if (!areTypesEquivalent(
                    superValueParameters.get(i),
                    subValueParameters.get(i),
                    typeChecker)
            ) {
                return OverrideCompatibilityInfo.incompatible("Value parameter type mismatch");
            }
        }

        if (superDescriptor instanceof FunctionDescriptor && subDescriptor instanceof FunctionDescriptor &&
            ((FunctionDescriptor) superDescriptor).isSuspend() != ((FunctionDescriptor) subDescriptor).isSuspend()) {
            return OverrideCompatibilityInfo.conflict("Incompatible suspendability");
        }

        if (checkReturnType) {
            KotlinType superReturnType = superDescriptor.getReturnType();
            KotlinType subReturnType = subDescriptor.getReturnType();

            if (superReturnType != null && subReturnType != null) {
                boolean bothErrors = KotlinTypeKt.isError(subReturnType) && KotlinTypeKt.isError(superReturnType);
                if (!bothErrors &&
                    !typeChecker.getFirst().isSubtypeOf(
                            typeChecker.getSecond(),
                            subReturnType.unwrap(),
                            superReturnType.unwrap()
                    )
                ) {
                    return OverrideCompatibilityInfo.conflict("Return type mismatch");
                }
            }
        }

        return OverrideCompatibilityInfo.success();
    }

    @Nullable
    public static OverrideCompatibilityInfo getBasicOverridabilityProblem(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor
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

        return null;
    }

    @NotNull
    private Pair<NewKotlinTypeCheckerImpl, TypeCheckerState> createTypeChecker(
            @NotNull List<TypeParameterDescriptor> firstParameters,
            @NotNull List<TypeParameterDescriptor> secondParameters
    ) {
        assert firstParameters.size() == secondParameters.size() :
                "Should be the same number of type parameters: " + firstParameters + " vs " + secondParameters;

        NewKotlinTypeCheckerImpl typeChecker = new NewKotlinTypeCheckerImpl(kotlinTypeRefiner, KotlinTypePreparator.Default.INSTANCE);
        TypeCheckerState state = createTypeCheckerState(firstParameters, secondParameters);

        return new Pair<NewKotlinTypeCheckerImpl, TypeCheckerState>(typeChecker, state);
    }

    @NotNull
    private TypeCheckerState createTypeCheckerState(
            @NotNull List<TypeParameterDescriptor> firstParameters,
            @NotNull List<TypeParameterDescriptor> secondParameters
    ) {
        if (firstParameters.isEmpty()) {
            return new OverridingUtilTypeSystemContext(null, equalityAxioms, kotlinTypeRefiner)
                    .newTypeCheckerState(true, true);
        }

        Map<TypeConstructor, TypeConstructor> matchingTypeConstructors = new HashMap<TypeConstructor, TypeConstructor>();
        for (int i = 0; i < firstParameters.size(); i++) {
            matchingTypeConstructors.put(firstParameters.get(i).getTypeConstructor(), secondParameters.get(i).getTypeConstructor());
        }

        return new OverridingUtilTypeSystemContext(matchingTypeConstructors, equalityAxioms, kotlinTypeRefiner)
                .newTypeCheckerState(true, true);
    }

    @Nullable
    private static OverrideCompatibilityInfo checkReceiverAndParameterCount(
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

    private boolean areTypesEquivalent(
            @NotNull KotlinType typeInSuper,
            @NotNull KotlinType typeInSub,
            @NotNull Pair<NewKotlinTypeCheckerImpl, TypeCheckerState> typeChecker
    ) {
        boolean bothErrors = KotlinTypeKt.isError(typeInSuper) && KotlinTypeKt.isError(typeInSub);
        if (bothErrors) return true;
        return typeChecker.getFirst().equalTypes(typeChecker.getSecond(), typeInSuper.unwrap(), typeInSub.unwrap());
    }

    // See JLS 8, 8.4.4 Generic Methods
    private boolean areTypeParametersEquivalent(
            @NotNull TypeParameterDescriptor superTypeParameter,
            @NotNull TypeParameterDescriptor subTypeParameter,
            @NotNull Pair<NewKotlinTypeCheckerImpl, TypeCheckerState> typeChecker
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

    private static List<KotlinType> compiledValueParameters(CallableDescriptor callableDescriptor) {
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

    public void generateOverridesInFunctionGroup(
            @SuppressWarnings("UnusedParameters")
            @NotNull Name name, //DO NOT DELETE THIS PARAMETER: needed to make sure all descriptors have the same name
            @NotNull Collection<? extends CallableMemberDescriptor> membersFromSupertypes,
            @NotNull Collection<? extends CallableMemberDescriptor> membersFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull OverridingStrategy strategy
    ) {
        Collection<CallableMemberDescriptor> notOverridden = new LinkedHashSet<CallableMemberDescriptor>(membersFromSupertypes);

        for (CallableMemberDescriptor fromCurrent : membersFromCurrent) {
            Collection<CallableMemberDescriptor> bound =
                    extractAndBindOverridesForMember(fromCurrent, membersFromSupertypes, current, strategy);
            notOverridden.removeAll(bound);
        }

        createAndBindFakeOverrides(current, notOverridden, strategy);
    }

    public static boolean isVisibleForOverride(@NotNull MemberDescriptor overriding, @NotNull MemberDescriptor fromSuper) {
        return !DescriptorVisibilities.isPrivate(fromSuper.getVisibility()) &&
               DescriptorVisibilities.isVisibleIgnoringReceiver(fromSuper, overriding);
    }

    private Collection<CallableMemberDescriptor> extractAndBindOverridesForMember(
            @NotNull CallableMemberDescriptor fromCurrent,
            @NotNull Collection<? extends CallableMemberDescriptor> descriptorsFromSuper,
            @NotNull ClassDescriptor current,
            @NotNull OverridingStrategy strategy
    ) {
        Collection<CallableMemberDescriptor> bound = new ArrayList<CallableMemberDescriptor>(descriptorsFromSuper.size());
        Collection<CallableMemberDescriptor> overridden = SmartSet.create();
        for (CallableMemberDescriptor fromSupertype : descriptorsFromSuper) {
            OverrideCompatibilityInfo.Result result = isOverridableBy(fromSupertype, fromCurrent, current).getResult();

            boolean isVisibleForOverride = isVisibleForOverride(fromCurrent, fromSupertype);

            switch (result) {
                case OVERRIDABLE:
                    if (isVisibleForOverride) {
                        overridden.add(fromSupertype);
                    }
                    bound.add(fromSupertype);
                    break;
                case CONFLICT:
                    if (isVisibleForOverride) {
                        strategy.overrideConflict(fromSupertype, fromCurrent);
                    }
                    bound.add(fromSupertype);
                    break;
                case INCOMPATIBLE:
                    break;
            }
        }

        strategy.setOverriddenDescriptors(fromCurrent, overridden);

        return bound;
    }

    private static boolean allHasSameContainingDeclaration(@NotNull Collection<CallableMemberDescriptor> notOverridden) {
        if (notOverridden.size() < 2) return true;

        final DeclarationDescriptor containingDeclaration = notOverridden.iterator().next().getContainingDeclaration();
        return CollectionsKt.all(notOverridden, new Function1<CallableMemberDescriptor, Boolean>() {
            @Override
            public Boolean invoke(CallableMemberDescriptor descriptor) {
                return descriptor.getContainingDeclaration() == containingDeclaration;
            }
        });
    }

    private static void createAndBindFakeOverrides(
            @NotNull ClassDescriptor current,
            @NotNull Collection<CallableMemberDescriptor> notOverridden,
            @NotNull OverridingStrategy strategy
    ) {
        // Optimization: If all notOverridden descriptors have the same containing declaration,
        // then we can just create fake overrides for them, because they should be matched correctly in their containing declaration
        if (allHasSameContainingDeclaration(notOverridden)) {
            for (CallableMemberDescriptor descriptor : notOverridden) {
                createAndBindFakeOverride(Collections.singleton(descriptor), current, strategy);
            }
            return;
        }

        Queue<CallableMemberDescriptor> fromSuperQueue = new LinkedList<CallableMemberDescriptor>(notOverridden);
        while (!fromSuperQueue.isEmpty()) {
            CallableMemberDescriptor notOverriddenFromSuper = VisibilityUtilKt.findMemberWithMaxVisibility(fromSuperQueue);
            Collection<CallableMemberDescriptor> overridables =
                    extractMembersOverridableInBothWays(notOverriddenFromSuper, fromSuperQueue, strategy);
            createAndBindFakeOverride(overridables, current, strategy);
        }
    }

    public static boolean isMoreSpecific(@NotNull CallableDescriptor a, @NotNull CallableDescriptor b) {
        KotlinType aReturnType = a.getReturnType();
        KotlinType bReturnType = b.getReturnType();

        assert aReturnType != null : "Return type of " + a + " is null";
        assert bReturnType != null : "Return type of " + b + " is null";

        if (!isVisibilityMoreSpecific(a, b)) return false;

        Pair<NewKotlinTypeCheckerImpl, TypeCheckerState> checker =
                DEFAULT.createTypeChecker(a.getTypeParameters(), b.getTypeParameters());

        if (a instanceof FunctionDescriptor) {
            assert b instanceof FunctionDescriptor : "b is " + b.getClass();

            return isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType, checker);
        }
        if (a instanceof PropertyDescriptor) {
            assert b instanceof PropertyDescriptor : "b is " + b.getClass();

            PropertyDescriptor pa = (PropertyDescriptor) a;
            PropertyDescriptor pb = (PropertyDescriptor) b;

            if (!isAccessorMoreSpecific(pa.getSetter(), pb.getSetter())) return false;

            if (pa.isVar() && pb.isVar()) {
                // TODO(dsavvinov): using DEFAULT here looks suspicious
                return checker.getFirst().equalTypes(checker.getSecond(), aReturnType.unwrap(), bReturnType.unwrap());
            }
            else {
                // both vals or var vs val: val can't be more specific then var
                return !(!pa.isVar() && pb.isVar()) && isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType, checker);
            }
        }
        throw new IllegalArgumentException("Unexpected callable: " + a.getClass());
    }

    private static boolean isVisibilityMoreSpecific(
            @NotNull DeclarationDescriptorWithVisibility a,
            @NotNull DeclarationDescriptorWithVisibility b
    ) {
        Integer result = DescriptorVisibilities.compare(a.getVisibility(), b.getVisibility());
        return result == null || result >= 0;
    }

    private static boolean isAccessorMoreSpecific(@Nullable PropertyAccessorDescriptor a, @Nullable PropertyAccessorDescriptor b) {
        if (a == null || b == null) return true;
        return isVisibilityMoreSpecific(a, b);
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
            @NotNull KotlinType bReturnType,
            @NotNull Pair<NewKotlinTypeCheckerImpl, TypeCheckerState> typeChecker
    ) {
        return typeChecker.getFirst().isSubtypeOf(typeChecker.getSecond(), aReturnType.unwrap(), bReturnType.unwrap());
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
            //noinspection ConstantConditions
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
            @NotNull OverridingStrategy strategy
    ) {
        Collection<CallableMemberDescriptor> visibleOverridables = filterVisibleFakeOverrides(current, overridables);
        boolean allInvisible = visibleOverridables.isEmpty();
        Collection<CallableMemberDescriptor> effectiveOverridden = allInvisible ? overridables : visibleOverridables;

        Modality modality = determineModalityForFakeOverride(effectiveOverridden, current);
        DescriptorVisibility visibility = allInvisible ? DescriptorVisibilities.INVISIBLE_FAKE : DescriptorVisibilities.INHERITED;

        // FIXME doesn't work as expected for flexible types: should create a refined signature.
        // Current algorithm produces bad results in presence of annotated Java signatures such as:
        //      J: foo(s: String!): String -- @NotNull String foo(String s);
        //      K: foo(s: String): String?
        //  --> 'foo(s: String!): String' as an inherited signature with most specific return type.
        // This is bad because it can be overridden by 'foo(s: String?): String', which is not override-equivalent with K::foo above.
        // Should be 'foo(s: String): String'.
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
        strategy.setOverriddenDescriptors(fakeOverride, effectiveOverridden);
        assert !fakeOverride.getOverriddenDescriptors().isEmpty()
                : "Overridden descriptors should be set for " + CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
        strategy.addFakeOverride(fakeOverride);
    }

    @NotNull
    private static Modality determineModalityForFakeOverride(
            @NotNull Collection<CallableMemberDescriptor> descriptors,
            @NotNull ClassDescriptor current
    ) {
        // Optimization: avoid creating hash sets in frequent cases when modality can be computed trivially
        boolean hasOpen = false;
        boolean hasAbstract = false;
        for (CallableMemberDescriptor descriptor : descriptors) {
            switch (descriptor.getModality()) {
                case FINAL:
                    return Modality.FINAL;
                case SEALED:
                    throw new IllegalStateException("Member cannot have SEALED modality: " + descriptor);
                case OPEN:
                    hasOpen = true;
                    break;
                case ABSTRACT:
                    hasAbstract = true;
                    break;
            }
        }

        // Fake overrides of abstract members in non-abstract expected classes should not be abstract, because otherwise it would be
        // impossible to inherit a non-expected class from that expected class in common code.
        // We're making their modality that of the containing class, because this is the least confusing behavior for the users.
        // However, it may cause problems if we reuse resolution results of common code when compiling platform code (see KT-15220)
        boolean transformAbstractToClassModality =
                current.isExpect() && (current.getModality() != Modality.ABSTRACT && current.getModality() != Modality.SEALED);

        if (hasOpen && !hasAbstract) {
            return Modality.OPEN;
        }
        if (!hasOpen && hasAbstract) {
            return transformAbstractToClassModality ? current.getModality() : Modality.ABSTRACT;
        }

        Set<CallableMemberDescriptor> allOverriddenDeclarations = new HashSet<CallableMemberDescriptor>();
        for (CallableMemberDescriptor descriptor : descriptors) {
            allOverriddenDeclarations.addAll(getOverriddenDeclarations(descriptor));
        }
        return getMinimalModality(filterOutOverridden(allOverriddenDeclarations), transformAbstractToClassModality, current.getModality());
    }

    @NotNull
    private static Modality getMinimalModality(
            @NotNull Collection<CallableMemberDescriptor> descriptors,
            boolean transformAbstractToClassModality,
            @NotNull Modality classModality
    ) {
        Modality result = Modality.ABSTRACT;
        for (CallableMemberDescriptor descriptor : descriptors) {
            Modality effectiveModality =
                    transformAbstractToClassModality && descriptor.getModality() == Modality.ABSTRACT
                    ? classModality
                    : descriptor.getModality();
            if (effectiveModality.compareTo(result) < 0) {
                result = effectiveModality;
            }
        }
        return result;
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
                return !DescriptorVisibilities.isPrivate(descriptor.getVisibility()) &&
                       DescriptorVisibilities.isVisibleIgnoringReceiver(descriptor, current);
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
            @NotNull final OverridingStrategy strategy
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
                        strategy.inheritanceConflict(overrider, descriptor);
                        return Unit.INSTANCE;
                    }
                });
    }


    public static void resolveUnknownVisibilityForMember(
            @NotNull CallableMemberDescriptor memberDescriptor,
            @Nullable Function1<CallableMemberDescriptor, Unit> cannotInferVisibility
    ) {
        for (CallableMemberDescriptor descriptor : memberDescriptor.getOverriddenDescriptors()) {
            if (descriptor.getVisibility() == DescriptorVisibilities.INHERITED) {
                resolveUnknownVisibilityForMember(descriptor, cannotInferVisibility);
            }
        }

        if (memberDescriptor.getVisibility() != DescriptorVisibilities.INHERITED) {
            return;
        }

        DescriptorVisibility maxVisibility = computeVisibilityToInherit(memberDescriptor);
        DescriptorVisibility visibilityToInherit;
        if (maxVisibility == null) {
            if (cannotInferVisibility != null) {
                cannotInferVisibility.invoke(memberDescriptor);
            }
            visibilityToInherit = DescriptorVisibilities.PUBLIC;
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
            PropertyAccessorDescriptorImpl propertyAccessorDescriptor = (PropertyAccessorDescriptorImpl) memberDescriptor;
            propertyAccessorDescriptor.setVisibility(visibilityToInherit);
            if (visibilityToInherit != propertyAccessorDescriptor.getCorrespondingProperty().getVisibility()) {
                propertyAccessorDescriptor.setDefault(false);
            }
        }
    }

    @Nullable
    private static DescriptorVisibility computeVisibilityToInherit(@NotNull CallableMemberDescriptor memberDescriptor) {
        Collection<? extends CallableMemberDescriptor> overriddenDescriptors = memberDescriptor.getOverriddenDescriptors();
        DescriptorVisibility maxVisibility = findMaxVisibility(overriddenDescriptors);
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
    public static DescriptorVisibility findMaxVisibility(@NotNull Collection<? extends CallableMemberDescriptor> descriptors) {
        if (descriptors.isEmpty()) {
            return DescriptorVisibilities.DEFAULT_VISIBILITY;
        }
        DescriptorVisibility maxVisibility = null;
        for (CallableMemberDescriptor descriptor : descriptors) {
            DescriptorVisibility visibility = descriptor.getVisibility();
            assert visibility != DescriptorVisibilities.INHERITED : "Visibility should have been computed for " + descriptor;
            if (maxVisibility == null) {
                maxVisibility = visibility;
                continue;
            }
            Integer compareResult = DescriptorVisibilities.compare(visibility, maxVisibility);
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
            Integer compareResult = DescriptorVisibilities.compare(maxVisibility, descriptor.getVisibility());
            if (compareResult == null || compareResult < 0) {
                return null;
            }
        }
        return maxVisibility;
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
