/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.SmartHashSet;
import com.intellij.util.containers.hash.EqualityPolicy;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactoryWithPsiElement;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilKt;
import org.jetbrains.kotlin.resolve.dataClassUtils.DataClassUtilsKt;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.utils.HashSetUtil;

import java.util.*;

import static kotlin.collections.CollectionsKt.sortedBy;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveAbstractMembers;
import static org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;

public class OverrideResolver {

    @NotNull private final BindingTrace trace;

    public OverrideResolver(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    public void check(@NotNull TopDownAnalysisContext c) {
        checkVisibility(c);
        checkOverrides(c);
        checkParameterOverridesForAllClasses(c);
    }

    public static void generateOverridesInAClass(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull Collection<CallableMemberDescriptor> membersFromCurrent,
            @NotNull OverridingStrategy strategy
    ) {
        List<CallableMemberDescriptor> membersFromSupertypes = getCallableMembersFromSupertypes(classDescriptor);
        MultiMap<Name, CallableMemberDescriptor> membersFromCurrentByName = groupDescriptorsByName(membersFromCurrent);
        MultiMap<Name, CallableMemberDescriptor> membersFromSupertypesByName = groupDescriptorsByName(membersFromSupertypes);

        Set<Name> memberNames = new LinkedHashSet<Name>();
        memberNames.addAll(membersFromSupertypesByName.keySet());
        memberNames.addAll(membersFromCurrentByName.keySet());

        for (Name memberName : memberNames) {
            Collection<CallableMemberDescriptor> fromSupertypes = membersFromSupertypesByName.get(memberName);
            Collection<CallableMemberDescriptor> fromCurrent = membersFromCurrentByName.get(memberName);

            OverridingUtil.generateOverridesInFunctionGroup(memberName, fromSupertypes, fromCurrent, classDescriptor, strategy);
        }
    }

    public static void resolveUnknownVisibilities(
            @NotNull Collection<? extends CallableMemberDescriptor> descriptors,
            @NotNull BindingTrace trace
    ) {
        for (CallableMemberDescriptor descriptor : descriptors) {
            OverridingUtil.resolveUnknownVisibilityForMember(descriptor, createCannotInferVisibilityReporter(trace));
        }
    }

    @NotNull
    public static Function1<CallableMemberDescriptor, Unit> createCannotInferVisibilityReporter(@NotNull final BindingTrace trace) {
        return new Function1<CallableMemberDescriptor, Unit>() {
            @Override
            public Unit invoke(@NotNull CallableMemberDescriptor descriptor) {
                DeclarationDescriptor reportOn;
                if (descriptor.getKind() == FAKE_OVERRIDE || descriptor.getKind() == DELEGATION) {
                    reportOn = DescriptorUtils.getParentOfType(descriptor, ClassDescriptor.class);
                }
                else if (descriptor instanceof PropertyAccessorDescriptor && ((PropertyAccessorDescriptor) descriptor).isDefault()) {
                    reportOn = ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty();
                }
                else {
                    reportOn = descriptor;
                }
                //noinspection ConstantConditions
                PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(reportOn);
                if (element instanceof KtDeclaration) {
                    trace.report(CANNOT_INFER_VISIBILITY.on((KtDeclaration) element, descriptor));
                }
                return Unit.INSTANCE;
            }
        };
    }

    private static enum Filtering {
        RETAIN_OVERRIDING,
        RETAIN_OVERRIDDEN
    }

    @NotNull
    public static <D extends CallableDescriptor> Set<D> filterOutOverridden(@NotNull Set<D> candidateSet) {
        //noinspection unchecked
        return filterOverrides(candidateSet, Function.ID, Filtering.RETAIN_OVERRIDING);
    }

    @NotNull
    public static <D> Set<D> filterOutOverriding(@NotNull Set<D> candidateSet) {
        //noinspection unchecked
        return filterOverrides(candidateSet, Function.ID, Filtering.RETAIN_OVERRIDDEN);
    }

    @NotNull
    public static <D> Set<D> filterOutOverridden(
            @NotNull Set<D> candidateSet,
            @NotNull Function<? super D, ? extends CallableDescriptor> transform
    ) {
        return filterOverrides(candidateSet, transform, Filtering.RETAIN_OVERRIDING);
    }


    // In a multi-module project different "copies" of the same class may be present in different libraries,
    // that's why we use structural equivalence for members (DescriptorEquivalenceForOverrides).
    //
    // Sometimes we should compare "copies" from sources and from binary files.
    // But we cannot compare return types for such copies, because it may lead us to recursive problem (see KT-11995).
    // Because of this we compare them without return type and choose descriptor from source if we found duplicate.
    @NotNull
    private static <D> Set<D> noDuplicates(
            @NotNull Set<D> candidateSet,
            @NotNull final Function<? super D, ? extends CallableDescriptor> transform
    ) {
        List<D> fromSourcesGoesFirst = sortedBy(candidateSet, new Function1<D, Integer>() {
            @Override
            public Integer invoke(D d) {
                return DescriptorToSourceUtils.descriptorToDeclaration(transform.fun(d)) != null ? 0 : 1;
            }
        });

        return HashSetUtil.linkedHashSet(
                fromSourcesGoesFirst,
                new EqualityPolicy<D>() {
                    @Override
                    public int getHashCode(D d) {
                        return DescriptorUtils.getFqName(transform.fun(d).getContainingDeclaration()).hashCode();
                    }

                    @Override
                    public boolean isEqual(D d1, D d2) {
                        CallableDescriptor f = transform.fun(d1).getOriginal();
                        CallableDescriptor g = transform.fun(d2).getOriginal();

                        boolean ignoreReturnType = (DescriptorToSourceUtils.descriptorToDeclaration(f) == null) !=
                                                   (DescriptorToSourceUtils.descriptorToDeclaration(g) == null);

                        return DescriptorEquivalenceForOverrides.INSTANCE.areCallableDescriptorsEquivalent(f, g, ignoreReturnType);
                    }
                });
    }

    @NotNull
    private static <D> Set<D> filterOverrides(
            @NotNull Set<D> candidateSet,
            @NotNull Function<? super D, ? extends CallableDescriptor> transform,
            @NotNull Filtering filtering
    ) {
        if (candidateSet.size() <= 1) return candidateSet;

        // Here we filter out structurally equivalent descriptors before processing overrides, because such descriptors
        // "override" each other (overrides(f, g) = overrides(g, f) = true) and the code below removes them all from the
        // candidates, unless we first compute noDuplicates
        Set<D> noDuplicates = noDuplicates(candidateSet, transform);

        Set<D> candidates = Sets.newLinkedHashSet();
        outerLoop:
        for (D meD : noDuplicates) {
            CallableDescriptor me = transform.fun(meD);
            for (D otherD : noDuplicates) {
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
                    && OverridingUtil.DEFAULT.isOverridableBy(other, me, null).getResult() == OVERRIDABLE
                    && OverridingUtil.DEFAULT.isOverridableBy(me, other, null).getResult() == OVERRIDABLE) {
                    continue outerLoop;
                }
            }
            candidates.add(meD);
        }

        assert !candidates.isEmpty() : "All candidates filtered out from " + candidateSet;

        return candidates;
    }

    // check whether f overrides g
    public static <D extends CallableDescriptor> boolean overrides(@NotNull D f, @NotNull D g) {
        // This first check cover the case of duplicate classes in different modules:
        // when B is defined in modules m1 and m2, and C (indirectly) inherits from both versions,
        // we'll be getting sets of members that do not override each other, but are structurally equivalent.
        // As other code relies on no equal descriptors passed here, we guard against f == g, but this may not be necessary
        if (!f.equals(g) && DescriptorEquivalenceForOverrides.INSTANCE.areEquivalent(f.getOriginal(), g.getOriginal())) return true;
        CallableDescriptor originalG = g.getOriginal();
        for (D overriddenFunction : DescriptorUtils.getAllOverriddenDescriptors(f)) {
            if (DescriptorEquivalenceForOverrides.INSTANCE.areEquivalent(originalG, overriddenFunction.getOriginal())) return true;
        }
        return false;
    }

    private static <T extends DeclarationDescriptor> MultiMap<Name, T> groupDescriptorsByName(Collection<T> properties) {
        MultiMap<Name, T> r = new LinkedMultiMap<Name, T>();
        for (T property : properties) {
            r.putValue(property.getName(), property);
        }
        return r;
    }


    private static List<CallableMemberDescriptor> getCallableMembersFromSupertypes(ClassDescriptor classDescriptor) {
        Set<CallableMemberDescriptor> r = Sets.newLinkedHashSet();
        for (KotlinType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            r.addAll(getCallableMembersFromType(supertype));
        }
        return new ArrayList<CallableMemberDescriptor>(r);
    }

    private static List<CallableMemberDescriptor> getCallableMembersFromType(KotlinType type) {
        List<CallableMemberDescriptor> r = Lists.newArrayList();
        for (DeclarationDescriptor decl : DescriptorUtils.getAllDescriptors(type.getMemberScope())) {
            if (decl instanceof PropertyDescriptor || decl instanceof SimpleFunctionDescriptor) {
                r.add((CallableMemberDescriptor) decl);
            }
        }
        return r;
    }

    private void checkOverrides(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            checkOverridesInAClass(entry.getValue(), entry.getKey());
        }
    }

    private void checkOverridesInAClass(@NotNull ClassDescriptorWithResolutionScopes classDescriptor, @NotNull final KtClassOrObject klass) {
        // Check overrides for internal consistency
        for (CallableMemberDescriptor member : classDescriptor.getDeclaredCallableMembers()) {
            checkOverrideForMember(member);
        }

        CollectErrorInformationForInheritedMembersStrategy inheritedMemberErrors =
                new CollectErrorInformationForInheritedMembersStrategy(klass, classDescriptor);

        checkInheritedAndDelegatedSignatures(classDescriptor, inheritedMemberErrors, inheritedMemberErrors);
        inheritedMemberErrors.doReportErrors();
    }

    @NotNull
    public static Set<CallableMemberDescriptor> getMissingImplementations(@NotNull ClassDescriptor classDescriptor) {
        CollectMissingImplementationsStrategy collector = new CollectMissingImplementationsStrategy();
        checkInheritedAndDelegatedSignatures(classDescriptor, collector, null);
        return collector.shouldImplement;
    }

    private interface CheckInheritedSignaturesReportStrategy {
        void abstractMemberNotImplemented(CallableMemberDescriptor descriptor);
        void abstractBaseClassMemberNotImplemented(CallableMemberDescriptor descriptor);
        void multipleImplementationsMemberNotImplemented(CallableMemberDescriptor descriptor);
        void conflictingInterfaceMemberNotImplemented(CallableMemberDescriptor descriptor);
        void returnTypeMismatchOnInheritance(CallableMemberDescriptor descriptor1, CallableMemberDescriptor descriptor2);
        void propertyTypeMismatchOnInheritance(PropertyDescriptor descriptor1, PropertyDescriptor descriptor2);
    }

    private static class CollectMissingImplementationsStrategy implements CheckInheritedSignaturesReportStrategy {
        private final Set<CallableMemberDescriptor> shouldImplement = new LinkedHashSet<CallableMemberDescriptor>();

        @Override
        public void abstractMemberNotImplemented(CallableMemberDescriptor descriptor) {
            shouldImplement.add(descriptor);
        }

        @Override
        public void abstractBaseClassMemberNotImplemented(CallableMemberDescriptor descriptor) {
            // don't care
        }

        @Override
        public void multipleImplementationsMemberNotImplemented(CallableMemberDescriptor descriptor) {
            shouldImplement.add(descriptor);
        }

        @Override
        public void conflictingInterfaceMemberNotImplemented(CallableMemberDescriptor descriptor) {
            // don't care
        }

        @Override
        public void returnTypeMismatchOnInheritance(CallableMemberDescriptor descriptor1, CallableMemberDescriptor descriptor2) {
            // don't care
        }

        @Override
        public void propertyTypeMismatchOnInheritance(PropertyDescriptor descriptor1, PropertyDescriptor descriptor2) {
            // don't care
        }
    }

    private class CollectErrorInformationForInheritedMembersStrategy
            implements CheckInheritedSignaturesReportStrategy, CheckOverrideReportStrategy {
        private final KtClassOrObject klass;
        private final ClassDescriptor classDescriptor;

        private final Set<CallableMemberDescriptor> abstractNoImpl = Sets.newLinkedHashSet();
        private final Set<CallableMemberDescriptor> multipleImplementations = Sets.newLinkedHashSet();
        private final Set<CallableMemberDescriptor> abstractInBaseClassNoImpl = Sets.newLinkedHashSet();
        private final Set<CallableMemberDescriptor> conflictingInterfaceMembers = Sets.newLinkedHashSet();
        private final Set<CallableMemberDescriptor> conflictingReturnTypes = Sets.newHashSet();

        private final Set<DiagnosticFactoryWithPsiElement> onceErrorsReported = new SmartHashSet<DiagnosticFactoryWithPsiElement>();

        public CollectErrorInformationForInheritedMembersStrategy(
                @NotNull KtClassOrObject klass,
                @NotNull ClassDescriptor classDescriptor
        ) {
            this.klass = klass;
            this.classDescriptor = classDescriptor;
        }

        @Override
        public void abstractMemberNotImplemented(CallableMemberDescriptor descriptor) {
            abstractNoImpl.add(descriptor);
        }

        @Override
        public void abstractBaseClassMemberNotImplemented(CallableMemberDescriptor descriptor) {
            abstractInBaseClassNoImpl.add(descriptor);
        }

        @Override
        public void multipleImplementationsMemberNotImplemented(CallableMemberDescriptor descriptor) {
            multipleImplementations.add(descriptor);
        }

        @Override
        public void conflictingInterfaceMemberNotImplemented(CallableMemberDescriptor descriptor) {
            conflictingInterfaceMembers.add(descriptor);
        }

        @Override
        public void returnTypeMismatchOnInheritance(CallableMemberDescriptor descriptor1, CallableMemberDescriptor descriptor2) {
            conflictingReturnTypes.add(descriptor1);
            conflictingReturnTypes.add(descriptor2);

            reportInheritanceConflictIfRequired(RETURN_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2);
        }

        @Override
        public void propertyTypeMismatchOnInheritance(PropertyDescriptor descriptor1, PropertyDescriptor descriptor2) {
            conflictingReturnTypes.add(descriptor1);
            conflictingReturnTypes.add(descriptor2);

            if (descriptor1.isVar() || descriptor2.isVar()) {
                reportInheritanceConflictIfRequired(VAR_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2);
            }
            else {
                reportInheritanceConflictIfRequired(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2);
            }
        }

        private void reportInheritanceConflictIfRequired(
                @NotNull DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> diagnosticFactory,
                @NotNull CallableMemberDescriptor descriptor1,
                @NotNull CallableMemberDescriptor descriptor2
        ) {
            if (!onceErrorsReported.contains(diagnosticFactory)) {
                onceErrorsReported.add(diagnosticFactory);
                trace.report(diagnosticFactory.on(klass, descriptor1, descriptor2));
            }
        }

        @Override
        public void overridingFinalMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
            reportDelegationProblemIfRequired(OVERRIDING_FINAL_MEMBER_BY_DELEGATION, overriding, overridden);
        }

        @Override
        public void returnTypeMismatchOnOverride(
                @NotNull CallableMemberDescriptor overriding,
                @NotNull CallableMemberDescriptor overridden
        ) {
            // Always reported as RETURN_TYPE_MISMATCH_ON_INHERITANCE
        }

        @Override
        public void propertyTypeMismatchOnOverride(
                @NotNull PropertyDescriptor overriding,
                @NotNull PropertyDescriptor overridden
        ) {
            // Always reported as PROPERTY_TYPE_MISMATCH_ON_INHERITANCE
        }

        @Override
        public void varOverriddenByVal(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
            reportDelegationProblemIfRequired(VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION, overriding, overridden);
        }

        private void reportDelegationProblemIfRequired(
                @NotNull DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> diagnosticFactory,
                @NotNull CallableMemberDescriptor delegate,
                @NotNull CallableMemberDescriptor overridden
        ) {
            assert delegate.getKind() == DELEGATION : "Delegate expected, got " + delegate + " of kind " + delegate.getKind();

            if (!onceErrorsReported.contains(diagnosticFactory)) {
                onceErrorsReported.add(diagnosticFactory);
                trace.report(diagnosticFactory.on(klass, delegate, overridden));
            }
        }

        @Override
        public void cannotOverrideInvisibleMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor invisibleOverridden) {
            assert overriding.getKind() == DELEGATION : "Delegate expected, got " + overriding + " of kind " + overriding.getKind();
            assert overriding.getKind() != DELEGATION : "Delegated member can't override an invisible member; " + invisibleOverridden;
        }

        @Override
        public void nothingToOverride(@NotNull CallableMemberDescriptor overriding) {
            assert overriding.getKind() == DELEGATION : "Delegate expected, got " + overriding + " of kind " + overriding.getKind();
            assert overriding.getKind() != DELEGATION : "Delegated member can't override nothing; " + overriding;
        }

        void doReportErrors() {
            if (!classCanHaveAbstractMembers(classDescriptor)) {
                if (!abstractInBaseClassNoImpl.isEmpty()) {
                    trace.report(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractInBaseClassNoImpl.iterator().next()));
                }
                else if (!abstractNoImpl.isEmpty()) {
                    trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractNoImpl.iterator().next()));
                }
            }

            conflictingInterfaceMembers.removeAll(conflictingReturnTypes);
            multipleImplementations.removeAll(conflictingReturnTypes);
            if (!conflictingInterfaceMembers.isEmpty()) {
                trace.report(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.on(klass, klass, conflictingInterfaceMembers.iterator().next()));
            }
            else if (!multipleImplementations.isEmpty()) {
                trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(klass, klass, multipleImplementations.iterator().next()));
            }
        }
    }

    private static void checkInheritedAndDelegatedSignatures(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull CheckInheritedSignaturesReportStrategy inheritedReportStrategy,
            @Nullable CheckOverrideReportStrategy overrideReportStrategyForDelegates
    ) {
        for (DeclarationDescriptor member : DescriptorUtils.getAllDescriptors(classDescriptor.getDefaultType().getMemberScope())) {
            if (member instanceof CallableMemberDescriptor) {
                checkInheritedAndDelegatedSignatures((CallableMemberDescriptor) member, inheritedReportStrategy, overrideReportStrategyForDelegates);
            }
        }
    }

    private static void checkInheritedAndDelegatedSignatures(
            @NotNull CallableMemberDescriptor descriptor,
            @NotNull CheckInheritedSignaturesReportStrategy reportingStrategy,
            @Nullable CheckOverrideReportStrategy overrideReportStrategyForDelegates
    ) {
        CallableMemberDescriptor.Kind kind = descriptor.getKind();
        if (kind != FAKE_OVERRIDE && kind != DELEGATION) return;
        if (descriptor.getVisibility() == Visibilities.INVISIBLE_FAKE) return;

        Collection<? extends CallableMemberDescriptor> directOverridden = descriptor.getOverriddenDescriptors();
        assert !directOverridden.isEmpty() : kind + " " + descriptor.getName().asString() + " must override something";

        // collects map from the directly overridden descriptor to the set of declarations:
        // -- if directly overridden is not fake, the set consists of one element: this directly overridden
        // -- if it's fake, overridden declarations (non-fake) of this descriptor are collected
        Map<CallableMemberDescriptor, Set<CallableMemberDescriptor>> overriddenDeclarationsByDirectParent = collectOverriddenDeclarations(directOverridden);

        List<CallableMemberDescriptor> allOverriddenDeclarations = ContainerUtil.flatten(overriddenDeclarationsByDirectParent.values());
        Set<CallableMemberDescriptor> allFilteredOverriddenDeclarations = filterOutOverridden(
                Sets.newLinkedHashSet(allOverriddenDeclarations));

        Set<CallableMemberDescriptor> relevantDirectlyOverridden =
                getRelevantDirectlyOverridden(overriddenDeclarationsByDirectParent, allFilteredOverriddenDeclarations);

        checkInheritedDescriptorsGroup(relevantDirectlyOverridden, descriptor, reportingStrategy);

        if (kind == DELEGATION && overrideReportStrategyForDelegates != null) {
            checkOverridesForMember(descriptor, relevantDirectlyOverridden, overrideReportStrategyForDelegates);
        }

        if (kind != DELEGATION) {
            checkMissingOverridesByJava8Restrictions(relevantDirectlyOverridden, reportingStrategy);
        }

        List<CallableMemberDescriptor> implementations = collectImplementations(relevantDirectlyOverridden);

        int numImplementations = implementations.size();

        if (numImplementations == 1 && isReturnTypeOkForOverride(descriptor, implementations.get(0))) return;

        List<CallableMemberDescriptor> abstractOverridden = new ArrayList<CallableMemberDescriptor>(allFilteredOverriddenDeclarations.size());
        List<CallableMemberDescriptor> concreteOverridden = new ArrayList<CallableMemberDescriptor>(allFilteredOverriddenDeclarations.size());
        filterNotSynthesizedDescriptorsByModality(allFilteredOverriddenDeclarations, abstractOverridden, concreteOverridden);

        if (numImplementations == 0) {
            if (kind != DELEGATION) {
                for (CallableMemberDescriptor member : abstractOverridden) {
                    reportingStrategy.abstractMemberNotImplemented(member);
                }
            }
        }
        else if (numImplementations > 1) {
            for (CallableMemberDescriptor member : concreteOverridden) {
                reportingStrategy.multipleImplementationsMemberNotImplemented(member);
            }
        }
        else {
            if (kind != DELEGATION) {
                List<CallableMemberDescriptor> membersWithMoreSpecificReturnType =
                        collectAbstractMethodsWithMoreSpecificReturnType(abstractOverridden, implementations.get(0));
                for (CallableMemberDescriptor member : membersWithMoreSpecificReturnType) {
                    reportingStrategy.abstractMemberNotImplemented(member);
                }
            }
        }
    }

    private static void checkMissingOverridesByJava8Restrictions(
            @NotNull Set<CallableMemberDescriptor> relevantDirectlyOverridden,
            @NotNull CheckInheritedSignaturesReportStrategy reportingStrategy
    ) {
        // Java 8:
        // -- class should implement an abstract member of a super-class,
        //    even if relevant default implementation is provided in one of the super-interfaces;
        // -- inheriting multiple override equivalent methods from an interface is a conflict
        //    regardless of 'default' vs 'abstract'.

        boolean overridesClassMember = false;
        boolean overridesNonAbstractInterfaceMember = false;
        CallableMemberDescriptor overridesAbstractInBaseClass = null;
        List<CallableMemberDescriptor> overriddenInterfaceMembers = new SmartList<CallableMemberDescriptor>();
        for (CallableMemberDescriptor overridden : relevantDirectlyOverridden) {
            DeclarationDescriptor containingDeclaration = overridden.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor baseClassOrInterface = (ClassDescriptor) containingDeclaration;
                if (baseClassOrInterface.getKind() == ClassKind.CLASS) {
                    overridesClassMember = true;
                    if (overridden.getModality() == Modality.ABSTRACT) {
                        overridesAbstractInBaseClass = overridden;
                    }
                }
                else if (baseClassOrInterface.getKind() == ClassKind.INTERFACE) {
                    overriddenInterfaceMembers.add(overridden);
                    if (overridden.getModality() != Modality.ABSTRACT) {
                        overridesNonAbstractInterfaceMember = true;
                    }
                }
            }
        }

        if (overridesAbstractInBaseClass != null) {
            reportingStrategy.abstractBaseClassMemberNotImplemented(overridesAbstractInBaseClass);
        }
        if (!overridesClassMember && overridesNonAbstractInterfaceMember && overriddenInterfaceMembers.size() > 1) {
            for (CallableMemberDescriptor member : overriddenInterfaceMembers) {
                reportingStrategy.conflictingInterfaceMemberNotImplemented(member);
            }
        }
    }

    @NotNull
    private static List<CallableMemberDescriptor> collectImplementations(@NotNull Set<CallableMemberDescriptor> relevantDirectlyOverridden) {
        List<CallableMemberDescriptor> result = new ArrayList<CallableMemberDescriptor>(relevantDirectlyOverridden.size());
        for (CallableMemberDescriptor overriddenDescriptor : relevantDirectlyOverridden) {
            if (overriddenDescriptor.getModality() != Modality.ABSTRACT) {
                result.add(overriddenDescriptor);
            }
        }
        return result;
    }

    private static void filterNotSynthesizedDescriptorsByModality(
            @NotNull Set<CallableMemberDescriptor> allOverriddenDeclarations,
            @NotNull List<CallableMemberDescriptor> abstractOverridden,
            @NotNull List<CallableMemberDescriptor> concreteOverridden
    ) {
        for (CallableMemberDescriptor overridden : allOverriddenDeclarations) {
            if (!CallResolverUtilKt.isOrOverridesSynthesized(overridden)) {
                if (overridden.getModality() == Modality.ABSTRACT) {
                    abstractOverridden.add(overridden);
                }
                else {
                    concreteOverridden.add(overridden);
                }
            }
        }
    }

    @NotNull
    private static List<CallableMemberDescriptor> collectAbstractMethodsWithMoreSpecificReturnType(
            @NotNull List<CallableMemberDescriptor> abstractOverridden,
            @NotNull CallableMemberDescriptor implementation
    ) {
        List<CallableMemberDescriptor> result = new ArrayList<CallableMemberDescriptor>(abstractOverridden.size());
        for (CallableMemberDescriptor abstractMember : abstractOverridden) {
            if (!isReturnTypeOkForOverride(abstractMember, implementation)) {
                result.add(abstractMember);
            }
        }
        assert !result.isEmpty() : "Implementation (" + implementation + ") doesn't have the most specific type, " +
                                   "but none of the other overridden methods does either: " + abstractOverridden;
        return result;
    }

    @NotNull
    private static Set<CallableMemberDescriptor> getRelevantDirectlyOverridden(
            @NotNull Map<CallableMemberDescriptor, Set<CallableMemberDescriptor>> overriddenByParent,
            @NotNull Set<CallableMemberDescriptor> allFilteredOverriddenDeclarations
    ) {
        /* Let the following class hierarchy is declared:

        trait A { fun foo() = 1 }
        trait B : A
        trait C : A
        trait D : A { override fun foo() = 2 }
        trait E : B, C, D {}

        Traits B and C have fake descriptors for function foo.
        The map 'overriddenByParent' is:
        { 'foo in B' (fake) -> { 'foo in A' }, 'foo in C' (fake) -> { 'foo in A' }, 'foo in D' -> { 'foo in D'} }
        This is a map from directly overridden descriptors (functions 'foo' in B, C, D in this example) to the set of declarations (non-fake),
        that are overridden by this descriptor.

        The goal is to leave only relevant directly overridden descriptors to count implementations of our fake function on them.
        In the example above there is no error (trait E inherits only one implementation of 'foo' (from D), because this implementation is more precise).
        So only 'foo in D' is relevant.

        Directly overridden descriptor is not relevant if it doesn't add any more appropriate non-fake declarations of the concerned function.
        More precisely directly overridden descriptor is not relevant if:
        - it's declaration set is a subset of declaration set for other directly overridden descriptor
        ('foo in B' is not relevant because it's declaration set is a subset of 'foo in C' function's declaration set)
        - each member of it's declaration set is overridden by a member of other declaration set
        ('foo in C' is not relevant, because 'foo in A' is overridden by 'foo in D', so 'foo in A' is not appropriate non-fake declaration for 'foo')

        For the last condition allFilteredOverriddenDeclarations helps (for the example above it's { 'foo in A' } only): each declaration set
        is compared with allFilteredOverriddenDeclarations, if they have no intersection, this means declaration set has only functions that
        are overridden by some other function and corresponding directly overridden descriptor is not relevant.
        */

        for (Iterator<Map.Entry<CallableMemberDescriptor, Set<CallableMemberDescriptor>>> iterator =
                     overriddenByParent.entrySet().iterator(); iterator.hasNext(); ) {
            if (!isRelevant(iterator.next().getValue(), overriddenByParent.values(), allFilteredOverriddenDeclarations)) {
                iterator.remove();
            }
        }
        return overriddenByParent.keySet();
    }

    private static boolean isRelevant(
            @NotNull Set<CallableMemberDescriptor> declarationSet,
            @NotNull Collection<Set<CallableMemberDescriptor>> allDeclarationSets,
            @NotNull Set<CallableMemberDescriptor> allFilteredOverriddenDeclarations
    ) {
        for (Set<CallableMemberDescriptor> otherSet : allDeclarationSets) {
            if (otherSet == declarationSet) continue;
            if (otherSet.containsAll(declarationSet)) return false;
            if (Collections.disjoint(allFilteredOverriddenDeclarations, declarationSet)) return false;
        }
        return true;
    }

    @NotNull
    private static Map<CallableMemberDescriptor, Set<CallableMemberDescriptor>> collectOverriddenDeclarations(
            @NotNull Collection<? extends CallableMemberDescriptor> directOverriddenDescriptors
    ) {
        Map<CallableMemberDescriptor, Set<CallableMemberDescriptor>> overriddenDeclarationsByDirectParent = Maps.newLinkedHashMap();
        for (CallableMemberDescriptor descriptor : directOverriddenDescriptors) {
            Set<CallableMemberDescriptor> overriddenDeclarations = getOverriddenDeclarations(descriptor);
            Set<CallableMemberDescriptor> filteredOverrides = filterOutOverridden(overriddenDeclarations);
            overriddenDeclarationsByDirectParent.put(descriptor, new LinkedHashSet<CallableMemberDescriptor>(filteredOverrides));
        }
        return overriddenDeclarationsByDirectParent;
    }

    /**
     * @return overridden real descriptors (not fake overrides). Note that all usages of this method should be followed by calling
     * {@link #filterOutOverridden(java.util.Set)} or {@link #filterOutOverriding(java.util.Set)}, because some of the declarations
     * can override the other
     * TODO: merge this method with filterOutOverridden
     */
    @NotNull
    public static Set<CallableMemberDescriptor> getOverriddenDeclarations(@NotNull CallableMemberDescriptor descriptor) {
        Set<CallableMemberDescriptor> result = new LinkedHashSet<CallableMemberDescriptor>();
        getOverriddenDeclarations(descriptor, result);
        return result;
    }

    private static void getOverriddenDeclarations(
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
                getOverriddenDeclarations(overridden, result);
            }
        }
    }

    private interface CheckOverrideReportStrategy {
        void overridingFinalMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden);
        void returnTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden);
        void propertyTypeMismatchOnOverride(@NotNull PropertyDescriptor overriding, @NotNull PropertyDescriptor overridden);
        void varOverriddenByVal(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden);
        void cannotOverrideInvisibleMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor invisibleOverridden);
        void nothingToOverride(@NotNull CallableMemberDescriptor overriding);
    }

    private void checkOverrideForMember(@NotNull final CallableMemberDescriptor declared) {
        if (declared.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            if (DataClassUtilsKt.isComponentLike(declared.getName())) {
                checkOverrideForComponentFunction(declared);
            }
            return;
        }

        if (declared.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            return;
        }

        final KtNamedDeclaration member = (KtNamedDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(declared);
        if (member == null) {
            throw new IllegalStateException("declared descriptor is not resolved to declaration: " + declared);
        }

        KtModifierList modifierList = member.getModifierList();
        boolean hasOverrideNode = modifierList != null && modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD);
        Collection<? extends CallableMemberDescriptor> overriddenDescriptors = declared.getOverriddenDescriptors();

        if (hasOverrideNode) {
            checkOverridesForMemberMarkedOverride(declared, true, new CheckOverrideReportStrategy() {
                private boolean finalOverriddenError = false;
                private boolean typeMismatchError = false;
                private boolean kindMismatchError = false;

                @Override
                public void overridingFinalMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
                    if (!finalOverriddenError) {
                        finalOverriddenError = true;
                        trace.report(OVERRIDING_FINAL_MEMBER.on(member, overridden, overridden.getContainingDeclaration()));
                    }
                }

                @Override
                public void returnTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
                    if (!typeMismatchError) {
                        typeMismatchError = true;
                        trace.report(RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden));
                    }
                }

                @Override
                public void propertyTypeMismatchOnOverride(@NotNull PropertyDescriptor overriding, @NotNull PropertyDescriptor overridden) {
                    if (!typeMismatchError) {
                        typeMismatchError = true;
                        if (overridden.isVar()) {
                            trace.report(VAR_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden));
                        }
                        else {
                            trace.report(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden));
                        }
                    }
                }

                @Override
                public void varOverriddenByVal(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
                    if (!kindMismatchError) {
                        kindMismatchError = true;
                        trace.report(VAR_OVERRIDDEN_BY_VAL.on(member, (PropertyDescriptor) declared, (PropertyDescriptor) overridden));
                    }
                }

                @Override
                public void cannotOverrideInvisibleMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor invisibleOverridden) {
                    trace.report(CANNOT_OVERRIDE_INVISIBLE_MEMBER.on(member, declared, invisibleOverridden));
                }

                @Override
                public void nothingToOverride(@NotNull CallableMemberDescriptor overriding) {
                    trace.report(NOTHING_TO_OVERRIDE.on(member, declared));
                }
            });
        }
        else if (!overriddenDescriptors.isEmpty()) {
            CallableMemberDescriptor overridden = overriddenDescriptors.iterator().next();
            trace.report(VIRTUAL_MEMBER_HIDDEN.on(member, declared, overridden, overridden.getContainingDeclaration()));
        }
    }

    private static void checkInheritedDescriptorsGroup(
            @NotNull Collection<CallableMemberDescriptor> inheritedDescriptors,
            @NotNull CallableMemberDescriptor mostSpecific,
            @NotNull CheckInheritedSignaturesReportStrategy reportingStrategy
    ) {
        if (inheritedDescriptors.size() > 1) {
            PropertyDescriptor mostSpecificProperty = mostSpecific instanceof PropertyDescriptor ? (PropertyDescriptor) mostSpecific : null;

            for (CallableMemberDescriptor inheritedDescriptor : inheritedDescriptors) {
                if (mostSpecificProperty != null) {
                    assert inheritedDescriptor instanceof PropertyDescriptor
                            : inheritedDescriptor + " inherited from " + mostSpecificProperty + " is not a property";
                    PropertyDescriptor inheritedPropertyDescriptor = (PropertyDescriptor) inheritedDescriptor;

                    if (!isPropertyTypeOkForOverride(inheritedPropertyDescriptor, mostSpecificProperty)) {
                        reportingStrategy.propertyTypeMismatchOnInheritance(mostSpecificProperty, inheritedPropertyDescriptor);
                    }
                }
                else if (!isReturnTypeOkForOverride(inheritedDescriptor, mostSpecific)) {
                    reportingStrategy.returnTypeMismatchOnInheritance(mostSpecific, inheritedDescriptor);
                }
            }
        }
    }

    private static void checkOverridesForMemberMarkedOverride(
            @NotNull CallableMemberDescriptor declared,
            boolean checkIfOverridesNothing,
            @NotNull CheckOverrideReportStrategy reportError
    ) {
        Collection<? extends CallableMemberDescriptor> overriddenDescriptors = declared.getOverriddenDescriptors();

        checkOverridesForMember(declared, overriddenDescriptors, reportError);

        if (checkIfOverridesNothing && overriddenDescriptors.isEmpty()) {
            DeclarationDescriptor containingDeclaration = declared.getContainingDeclaration();
            assert containingDeclaration instanceof ClassDescriptor : "Overrides may only be resolved in a class, but " + declared + " comes from " + containingDeclaration;
            ClassDescriptor declaringClass = (ClassDescriptor) containingDeclaration;

            CallableMemberDescriptor invisibleOverriddenDescriptor = findInvisibleOverriddenDescriptor(declared, declaringClass);
            if (invisibleOverriddenDescriptor != null) {
                reportError.cannotOverrideInvisibleMember(declared, invisibleOverriddenDescriptor);
            }
            else {
                reportError.nothingToOverride(declared);
            }
        }
    }

    private static void checkOverridesForMember(
            @NotNull CallableMemberDescriptor memberDescriptor,
            @NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors,
            @NotNull CheckOverrideReportStrategy reportError
    ) {
        PropertyDescriptor propertyMemberDescriptor =
                memberDescriptor instanceof PropertyDescriptor ? (PropertyDescriptor) memberDescriptor : null;

        for (CallableMemberDescriptor overridden : overriddenDescriptors) {
            if (overridden == null) continue;

            if (!ModalityKt.isOverridable(overridden)) {
                reportError.overridingFinalMember(memberDescriptor, overridden);
            }

            if (propertyMemberDescriptor != null) {
                assert overridden instanceof PropertyDescriptor : overridden + " is overridden by property " + propertyMemberDescriptor;
                PropertyDescriptor overriddenProperty = (PropertyDescriptor) overridden;
                if (!isPropertyTypeOkForOverride(overriddenProperty, propertyMemberDescriptor)) {
                    reportError.propertyTypeMismatchOnOverride(propertyMemberDescriptor, overriddenProperty);
                }
            }
            else if (!isReturnTypeOkForOverride(overridden, memberDescriptor)) {
                reportError.returnTypeMismatchOnOverride(memberDescriptor, overridden);
            }

            if (checkPropertyKind(overridden, true) && checkPropertyKind(memberDescriptor, false)) {
                reportError.varOverriddenByVal(memberDescriptor, overridden);
            }
        }
    }

    public static boolean isReturnTypeOkForOverride(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor
    ) {
        TypeSubstitutor typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor);
        if (typeSubstitutor == null) return false;

        KotlinType superReturnType = superDescriptor.getReturnType();
        assert superReturnType != null;

        KotlinType subReturnType = subDescriptor.getReturnType();
        assert subReturnType != null;

        KotlinType substitutedSuperReturnType = typeSubstitutor.substitute(superReturnType, Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;

        return KotlinTypeChecker.DEFAULT.isSubtypeOf(subReturnType, substitutedSuperReturnType);
    }

    @Nullable
    private static TypeSubstitutor prepareTypeSubstitutor(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor
    ) {
        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();
        if (subTypeParameters.size() != superTypeParameters.size()) return null;

        ArrayList<TypeProjection> arguments = new ArrayList<TypeProjection>(subTypeParameters.size());
        for (int i = 0; i < superTypeParameters.size(); i++) {
            arguments.add(new TypeProjectionImpl(subTypeParameters.get(i).getDefaultType()));
        }

        return new IndexedParametersSubstitution(superTypeParameters, arguments).buildSubstitutor();
    }

    public static boolean isPropertyTypeOkForOverride(
            @NotNull PropertyDescriptor superDescriptor,
            @NotNull PropertyDescriptor subDescriptor
    ) {
        TypeSubstitutor typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor);
        if (typeSubstitutor == null) return false;

        KotlinType substitutedSuperReturnType = typeSubstitutor.substitute(superDescriptor.getType(), Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;

        if (superDescriptor.isVar()) {
            return KotlinTypeChecker.DEFAULT.equalTypes(subDescriptor.getType(), substitutedSuperReturnType);
        }
        else {
            return KotlinTypeChecker.DEFAULT.isSubtypeOf(subDescriptor.getType(), substitutedSuperReturnType);
        }
    }

    private void checkOverrideForComponentFunction(@NotNull final CallableMemberDescriptor componentFunction) {
        final PsiElement dataModifier = findDataModifierForDataClass(componentFunction.getContainingDeclaration());

        checkOverridesForMemberMarkedOverride(componentFunction, false, new CheckOverrideReportStrategy() {
            private boolean overrideConflict = false;

            @Override
            public void overridingFinalMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
                if (!overrideConflict) {
                    overrideConflict = true;
                    trace.report(DATA_CLASS_OVERRIDE_CONFLICT.on(dataModifier, componentFunction, overridden.getContainingDeclaration()));
                }
            }

            @Override
            public void returnTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
                if (!overrideConflict) {
                    overrideConflict = true;
                    trace.report(DATA_CLASS_OVERRIDE_CONFLICT.on(dataModifier, componentFunction, overridden.getContainingDeclaration()));
                }
            }

            @Override
            public void propertyTypeMismatchOnOverride(@NotNull PropertyDescriptor overriding, @NotNull PropertyDescriptor overridden) {
                throw new IllegalStateException("Component functions are not properties");
            }

            @Override
            public void varOverriddenByVal(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor overridden) {
                throw new IllegalStateException("Component functions are not properties");
            }

            @Override
            public void cannotOverrideInvisibleMember(@NotNull CallableMemberDescriptor overriding, @NotNull CallableMemberDescriptor invisibleOverridden) {
                throw new IllegalStateException("CANNOT_OVERRIDE_INVISIBLE_MEMBER should be reported on the corresponding property");
            }

            @Override
            public void nothingToOverride(@NotNull CallableMemberDescriptor overriding) {
                throw new IllegalStateException("Component functions are OK to override nothing");
            }
        });
    }

    @NotNull
    private static PsiElement findDataModifierForDataClass(@NotNull DeclarationDescriptor dataClass) {
        KtClass classDeclaration = (KtClass) DescriptorToSourceUtils.getSourceFromDescriptor(dataClass);
        if (classDeclaration != null && classDeclaration.getModifierList() != null) {
            PsiElement modifier = classDeclaration.getModifierList().getModifier(KtTokens.DATA_KEYWORD);
            if (modifier != null) {
                return modifier;
            }
        }

        throw new IllegalStateException("No data modifier is found for data class " + dataClass);
    }

    @Nullable
    private static CallableMemberDescriptor findInvisibleOverriddenDescriptor(
            @NotNull CallableMemberDescriptor declared,
            @NotNull ClassDescriptor declaringClass
    ) {
        for (KotlinType supertype : declaringClass.getTypeConstructor().getSupertypes()) {
            Set<CallableMemberDescriptor> all = Sets.newLinkedHashSet();
            all.addAll(supertype.getMemberScope().getContributedFunctions(declared.getName(), NoLookupLocation.WHEN_CHECK_OVERRIDES));
            //noinspection unchecked
            all.addAll((Collection) supertype.getMemberScope().getContributedVariables(declared.getName(), NoLookupLocation.WHEN_CHECK_OVERRIDES));
            for (CallableMemberDescriptor fromSuper : all) {
                if (OverridingUtil.DEFAULT.isOverridableBy(fromSuper, declared, null).getResult() == OVERRIDABLE) {
                    if (Visibilities.isVisibleIgnoringReceiver(fromSuper, declared)) {
                        throw new IllegalStateException("Descriptor " + fromSuper + " is overridable by " + declared +
                                                        " and visible but does not appear in its getOverriddenDescriptors()");
                    }
                    return fromSuper;
                }
            }
        }
        return null;
    }

    private void checkParameterOverridesForAllClasses(@NotNull TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes classDescriptor : c.getDeclaredClasses().values()) {
            for (DeclarationDescriptor member : DescriptorUtils.getAllDescriptors(classDescriptor.getDefaultType().getMemberScope())) {
                if (member instanceof CallableMemberDescriptor) {
                    checkOverridesForParameters((CallableMemberDescriptor) member);
                }
            }
        }
    }

    private void checkOverridesForParameters(@NotNull CallableMemberDescriptor declared) {
        boolean isDeclaration = declared.getKind() == CallableMemberDescriptor.Kind.DECLARATION;
        if (isDeclaration) {
            // No check if the function is not marked as 'override'
            KtModifierListOwner declaration = (KtModifierListOwner) DescriptorToSourceUtils.descriptorToDeclaration(declared);
            if (declaration != null && !declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
                return;
            }
        }

        // Let p1 be a parameter of the overriding function
        // Let p2 be a parameter of the function being overridden
        // Then
        //  a) p1 is not allowed to have a default value declared
        //  b) p1 must have the same name as p2
        for (ValueParameterDescriptor parameterFromSubclass : declared.getValueParameters()) {
            int defaultsInSuper = 0;
            for (ValueParameterDescriptor parameterFromSuperclass : parameterFromSubclass.getOverriddenDescriptors()) {
                if (parameterFromSuperclass.declaresDefaultValue()) {
                    defaultsInSuper++;
                }
            }
            boolean multipleDefaultsInSuper = defaultsInSuper > 1;

            if (isDeclaration) {
                checkNameAndDefaultForDeclaredParameter(parameterFromSubclass, multipleDefaultsInSuper);
            }
            else {
                checkNameAndDefaultForFakeOverrideParameter(declared, parameterFromSubclass, multipleDefaultsInSuper);
            }
        }
    }

    private void checkNameAndDefaultForDeclaredParameter(@NotNull ValueParameterDescriptor descriptor, boolean multipleDefaultsInSuper) {
        KtParameter parameter = (KtParameter) DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        assert parameter != null : "Declaration not found for parameter: " + descriptor;

        if (descriptor.declaresDefaultValue()) {
            trace.report(DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE.on(parameter));
        }

        if (multipleDefaultsInSuper) {
            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES.on(parameter, descriptor));
        }

        for (ValueParameterDescriptor parameterFromSuperclass : descriptor.getOverriddenDescriptors()) {
            if (shouldReportParameterNameOverrideWarning(descriptor, parameterFromSuperclass)) {
                //noinspection ConstantConditions
                trace.report(PARAMETER_NAME_CHANGED_ON_OVERRIDE.on(
                        parameter,
                        (ClassDescriptor) parameterFromSuperclass.getContainingDeclaration().getContainingDeclaration(),
                        parameterFromSuperclass)
                );
            }
        }
    }

    private void checkNameAndDefaultForFakeOverrideParameter(
            @NotNull CallableMemberDescriptor containingFunction,
            @NotNull ValueParameterDescriptor descriptor,
            boolean multipleDefaultsInSuper
    ) {
        DeclarationDescriptor containingClass = containingFunction.getContainingDeclaration();
        KtClassOrObject classElement = (KtClassOrObject) DescriptorToSourceUtils.descriptorToDeclaration(containingClass);
        assert classElement != null : "Declaration not found for class: " + containingClass;

        if (multipleDefaultsInSuper) {
            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE.on(classElement, descriptor));
        }

        for (ValueParameterDescriptor parameterFromSuperclass : descriptor.getOverriddenDescriptors()) {
            if (shouldReportParameterNameOverrideWarning(descriptor, parameterFromSuperclass)) {
                trace.report(DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES.on(
                        classElement,
                        containingFunction.getOverriddenDescriptors(),
                        parameterFromSuperclass.getIndex() + 1)
                );
            }
        }
    }

    public static boolean shouldReportParameterNameOverrideWarning(
            @NotNull ValueParameterDescriptor parameterFromSubclass,
            @NotNull ValueParameterDescriptor parameterFromSuperclass
    ) {
        return parameterFromSubclass.getContainingDeclaration().hasStableParameterNames() &&
               parameterFromSuperclass.getContainingDeclaration().hasStableParameterNames() &&
               !parameterFromSuperclass.getName().equals(parameterFromSubclass.getName());
    }

    private static boolean checkPropertyKind(@NotNull CallableMemberDescriptor descriptor, boolean isVar) {
        return descriptor instanceof PropertyDescriptor && ((PropertyDescriptor) descriptor).isVar() == isVar;
    }

    private void checkVisibility(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<KtCallableDeclaration, CallableMemberDescriptor> entry : c.getMembers().entrySet()) {
            checkVisibilityForMember(entry.getKey(), entry.getValue());
            if (entry.getKey() instanceof KtProperty && entry.getValue() instanceof PropertyDescriptor) {
                KtPropertyAccessor setter = ((KtProperty) entry.getKey()).getSetter();
                PropertySetterDescriptor setterDescriptor = ((PropertyDescriptor) entry.getValue()).getSetter();
                if (setter != null && setterDescriptor != null) {
                    checkVisibilityForMember(setter, setterDescriptor);
                }
            }
        }
    }

    private void checkVisibilityForMember(@NotNull KtDeclaration declaration, @NotNull CallableMemberDescriptor memberDescriptor) {
        Visibility visibility = memberDescriptor.getVisibility();
        for (CallableMemberDescriptor descriptor : memberDescriptor.getOverriddenDescriptors()) {
            Integer compare = Visibilities.compare(visibility, descriptor.getVisibility());
            if (compare == null) {
                trace.report(CANNOT_CHANGE_ACCESS_PRIVILEGE.on(declaration, descriptor.getVisibility(), descriptor, descriptor.getContainingDeclaration()));
                return;
            }
            else if (compare < 0) {
                trace.report(CANNOT_WEAKEN_ACCESS_PRIVILEGE.on(declaration, descriptor.getVisibility(), descriptor, descriptor.getContainingDeclaration()));
                return;
            }
        }
    }

    @NotNull
    public static <D extends CallableMemberDescriptor> Collection<D> getDirectlyOverriddenDeclarations(@NotNull D descriptor) {
        Set<D> result = new LinkedHashSet<D>();
        //noinspection unchecked
        for (D overriddenDescriptor : (Collection<D>) descriptor.getOverriddenDescriptors()) {
            CallableMemberDescriptor.Kind kind = overriddenDescriptor.getKind();
            if (kind == DECLARATION) {
                result.add(overriddenDescriptor);
            }
            else if (kind == FAKE_OVERRIDE || kind == DELEGATION) {
                result.addAll(getDirectlyOverriddenDeclarations(overriddenDescriptor));
            }
            else if (kind == SYNTHESIZED) {
                //do nothing
            }
            else {
                throw new AssertionError("Unexpected callable kind " + kind);
            }
        }
        return filterOutOverridden(result);
    }

    @NotNull
    @ReadOnly
    public static <D extends CallableMemberDescriptor> Set<D> getDeepestSuperDeclarations(@NotNull D functionDescriptor) {
        Set<D> overriddenDeclarations = DescriptorUtils.getAllOverriddenDeclarations(functionDescriptor);
        if (overriddenDeclarations.isEmpty()) {
            return Collections.singleton(functionDescriptor);
        }

        return filterOutOverriding(overriddenDeclarations);
    }
}
