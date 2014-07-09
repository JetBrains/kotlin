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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.hash.EqualityPolicy;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.utils.HashSetUtil;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.*;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;

public class OverrideResolver {

    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }



    public void process(@NotNull TopDownAnalysisContext c) {
        //all created fake descriptors are stored to resolve visibility on them later
        generateOverridesAndDelegation(c);

        check(c);
    }

    public void check(@NotNull TopDownAnalysisContext c) {
        checkVisibility(c);
        checkOverrides(c);
        checkParameterOverridesForAllClasses(c);
    }

    /**
     * Generate fake overrides and add overridden descriptors to existing descriptors.
     */
    private void generateOverridesAndDelegation(@NotNull TopDownAnalysisContext c) {
        Set<ClassDescriptorWithResolutionScopes> ourClasses = new HashSet<ClassDescriptorWithResolutionScopes>(c.getAllClasses());
        Set<ClassifierDescriptor> processed = new HashSet<ClassifierDescriptor>();

        for (MutableClassDescriptor klass : ContainerUtil.reverse(c.getClassesTopologicalOrder())) {
            if (ourClasses.contains(klass)) {
                generateOverridesAndDelegationInAClass(klass, processed, ourClasses);

                ClassDescriptor classObject = klass.getClassObjectDescriptor();
                if (classObject instanceof MutableClassDescriptor) {
                    generateOverridesAndDelegationInAClass((MutableClassDescriptor) classObject, processed, ourClasses);
                }
            }
        }
    }

    private void generateOverridesAndDelegationInAClass(
            @NotNull MutableClassDescriptor classDescriptor,
            @NotNull Set<ClassifierDescriptor> processed,
            @NotNull Set<ClassDescriptorWithResolutionScopes> classesBeingAnalyzed
            // to filter out classes such as stdlib and others that come from dependencies
    ) {
        if (!processed.add(classDescriptor)) {
            return;
        }

        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            ClassDescriptor superclass = (ClassDescriptor) supertype.getConstructor().getDeclarationDescriptor();
            if (superclass instanceof MutableClassDescriptor && classesBeingAnalyzed.contains(superclass)) {
                generateOverridesAndDelegationInAClass((MutableClassDescriptor) superclass, processed, classesBeingAnalyzed);
            }
        }

        JetClassOrObject classOrObject = (JetClassOrObject) DescriptorToSourceUtils.classDescriptorToDeclaration(classDescriptor);
        if (classOrObject != null) {
            DelegationResolver.generateDelegatesInAClass(classDescriptor, trace, classOrObject);
        }

        generateOverridesInAClass(classDescriptor);
    }

    private void generateOverridesInAClass(@NotNull final MutableClassDescriptor classDescriptor) {
        List<CallableMemberDescriptor> membersFromSupertypes = getCallableMembersFromSupertypes(classDescriptor);

        MultiMap<Name, CallableMemberDescriptor> membersFromSupertypesByName = groupDescriptorsByName(membersFromSupertypes);

        MultiMap<Name, CallableMemberDescriptor> membersFromCurrentByName = groupDescriptorsByName(classDescriptor.getDeclaredCallableMembers());

        Set<Name> memberNames = new LinkedHashSet<Name>();
        memberNames.addAll(membersFromSupertypesByName.keySet());
        memberNames.addAll(membersFromCurrentByName.keySet());

        for (Name memberName : memberNames) {
            Collection<CallableMemberDescriptor> fromSupertypes = membersFromSupertypesByName.get(memberName);
            Collection<CallableMemberDescriptor> fromCurrent = membersFromCurrentByName.get(memberName);

            OverridingUtil.generateOverridesInFunctionGroup(
                    memberName,
                    fromSupertypes,
                    fromCurrent,
                    classDescriptor,
                    new OverridingUtil.DescriptorSink() {
                        @Override
                        public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                            if (fakeOverride instanceof PropertyDescriptor) {
                                classDescriptor.getBuilder().addPropertyDescriptor((PropertyDescriptor) fakeOverride);
                            }
                            else if (fakeOverride instanceof SimpleFunctionDescriptor) {
                                classDescriptor.getBuilder().addFunctionDescriptor((SimpleFunctionDescriptor) fakeOverride);
                            }
                            else {
                                throw new IllegalStateException(fakeOverride.getClass().getName());
                            }
                        }

                        @Override
                        public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                            JetDeclaration declaration = (JetDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(fromCurrent);
                            //noinspection ConstantConditions
                            trace.report(CONFLICTING_OVERLOADS.on(declaration, fromCurrent, fromCurrent.getContainingDeclaration().getName().asString()));
                        }
                    });
        }
        resolveUnknownVisibilities(classDescriptor.getAllCallableMembers(), trace);
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
                if (element instanceof JetDeclaration) {
                    trace.report(CANNOT_INFER_VISIBILITY.on((JetDeclaration) element, descriptor));
                }
                return Unit.VALUE;
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

    @NotNull
    private static <D> Set<D> filterOverrides(
            @NotNull Set<D> candidateSet,
            @NotNull final Function<? super D, ? extends CallableDescriptor> transform,
            @NotNull Filtering filtering
    ) {
        if (candidateSet.size() <= 1) return candidateSet;

        // In a multi-module project different "copies" of the same class may be present in different libraries,
        // that's why we use structural equivalence for members (DescriptorEquivalenceForOverrides).
        // Here we filter out structurally equivalent descriptors before processing overrides, because such descriptors
        // "override" each other (overrides(f, g) = overrides(g, f) = true) and the code below removes them all from the
        // candidates, unless we first compute noDuplicates
        Set<D> noDuplicates = HashSetUtil.linkedHashSet(
                candidateSet,
                new EqualityPolicy<D>() {
                    @Override
                    public int getHashCode(D d) {
                        return DescriptorUtils.getFqName(transform.fun(d).getContainingDeclaration()).hashCode();
                    }

                    @Override
                    public boolean isEqual(D d1, D d2) {
                        CallableDescriptor f = transform.fun(d1);
                        CallableDescriptor g = transform.fun(d2);
                        return DescriptorEquivalenceForOverrides.instance$.areEquivalent(f.getOriginal(), g.getOriginal());
                    }
                });

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
                    && OverridingUtil.DEFAULT.isOverridableBy(other, me).getResult() == OVERRIDABLE
                    && OverridingUtil.DEFAULT.isOverridableBy(me, other).getResult() == OVERRIDABLE) {
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
        if (!f.equals(g) && DescriptorEquivalenceForOverrides.instance$.areEquivalent(f.getOriginal(), g.getOriginal())) return true;
        CallableDescriptor originalG = g.getOriginal();
        for (D overriddenFunction : getAllOverriddenDescriptors(f)) {
            if (DescriptorEquivalenceForOverrides.instance$.areEquivalent(originalG, overriddenFunction.getOriginal())) return true;
        }
        return false;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <D extends CallableDescriptor> Set<D> getAllOverriddenDescriptors(@NotNull D f) {
        Set<D> result = new LinkedHashSet<D>();
        collectAllOverriddenDescriptors((D) f.getOriginal(), result);
        return result;
    }

    private static <D extends CallableDescriptor> void collectAllOverriddenDescriptors(@NotNull D current, @NotNull Set<D> result) {
        if (result.contains(current)) return;
        for (CallableDescriptor callableDescriptor : current.getOriginal().getOverriddenDescriptors()) {
            @SuppressWarnings("unchecked")
            D descriptor = (D) callableDescriptor;
            collectAllOverriddenDescriptors(descriptor, result);
            result.add(descriptor);
        }
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
        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            r.addAll(getCallableMembersFromType(supertype.getMemberScope()));
        }
        return new ArrayList<CallableMemberDescriptor>(r);
    }

    private static List<CallableMemberDescriptor> getCallableMembersFromType(JetScope scope) {
        List<CallableMemberDescriptor> r = Lists.newArrayList();
        for (DeclarationDescriptor decl : scope.getAllDescriptors()) {
            if (decl instanceof PropertyDescriptor || decl instanceof SimpleFunctionDescriptor) {
                r.add((CallableMemberDescriptor) decl);
            }
        }
        return r;
    }

    private void checkOverrides(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            checkOverridesInAClass(c, entry.getValue(), entry.getKey());
        }
    }

    private void checkOverridesInAClass(@NotNull TopDownAnalysisContext c, @NotNull ClassDescriptorWithResolutionScopes classDescriptor, @NotNull JetClassOrObject klass) {
        if (c.getTopDownAnalysisParameters().isAnalyzingBootstrapLibrary()) return;

        // Check overrides for internal consistency
        for (CallableMemberDescriptor member : classDescriptor.getDeclaredCallableMembers()) {
            checkOverrideForMember(member);
        }

        // Check if everything that must be overridden, actually is
        // More than one implementation or no implementations at all
        Set<CallableMemberDescriptor> abstractNoImpl = Sets.newLinkedHashSet();
        Set<CallableMemberDescriptor> manyImpl = Sets.newLinkedHashSet();
        collectMissingImplementations(classDescriptor, abstractNoImpl, manyImpl);

        if (!manyImpl.isEmpty()) {
            trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(klass, klass, manyImpl.iterator().next()));
        }

        if (classDescriptor.getModality() != Modality.ABSTRACT && !abstractNoImpl.isEmpty()) {
            trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractNoImpl.iterator().next()));
        }
    }

    @NotNull
    public static Set<CallableMemberDescriptor> getMissingImplementations(@NotNull ClassDescriptor classDescriptor) {
        Set<CallableMemberDescriptor> result = new LinkedHashSet<CallableMemberDescriptor>();
        collectMissingImplementations(classDescriptor, result, result);
        return result;
    }

    private static void collectMissingImplementations(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull Set<CallableMemberDescriptor> abstractNoImpl,
            @NotNull Set<CallableMemberDescriptor> manyImpl
    ) {
        for (DeclarationDescriptor member : classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (member instanceof CallableMemberDescriptor) {
                collectMissingImplementations((CallableMemberDescriptor) member, abstractNoImpl, manyImpl);
            }
        }
    }

    private static void collectMissingImplementations(
            @NotNull CallableMemberDescriptor descriptor,
            @NotNull Set<CallableMemberDescriptor> abstractNoImpl,
            @NotNull Set<CallableMemberDescriptor> manyImpl
    ) {
        if (descriptor.getKind().isReal()) return;
        if (descriptor.getVisibility() == Visibilities.INVISIBLE_FAKE) return;

        Collection<? extends CallableMemberDescriptor> directOverridden = descriptor.getOverriddenDescriptors();
        if (directOverridden.size() == 0) {
            throw new IllegalStateException("A 'fake override' must override something");
        }

        // collects map from the directly overridden descriptor to the set of declarations:
        // -- if directly overridden is not fake, the set consists of one element: this directly overridden
        // -- if it's fake, overridden declarations (non-fake) of this descriptor are collected
        Map<CallableMemberDescriptor, Set<CallableMemberDescriptor>> overriddenDeclarationsByDirectParent = collectOverriddenDeclarations(directOverridden);

        List<CallableMemberDescriptor> allOverriddenDeclarations = ContainerUtil.flatten(overriddenDeclarationsByDirectParent.values());
        Set<CallableMemberDescriptor> allFilteredOverriddenDeclarations = filterOutOverridden(
                Sets.newLinkedHashSet(allOverriddenDeclarations));

        Set<CallableMemberDescriptor> relevantDirectlyOverridden =
                getRelevantDirectlyOverridden(overriddenDeclarationsByDirectParent, allFilteredOverriddenDeclarations);

        List<CallableMemberDescriptor> implementations = collectImplementations(relevantDirectlyOverridden);
        if (implementations.size() == 1 && isReturnTypeOkForOverride(descriptor, implementations.get(0))) return;

        List<CallableMemberDescriptor> abstractOverridden = new ArrayList<CallableMemberDescriptor>(allFilteredOverriddenDeclarations.size());
        List<CallableMemberDescriptor> concreteOverridden = new ArrayList<CallableMemberDescriptor>(allFilteredOverriddenDeclarations.size());
        filterNotSynthesizedDescriptorsByModality(allFilteredOverriddenDeclarations, abstractOverridden, concreteOverridden);

        if (implementations.isEmpty()) {
            abstractNoImpl.addAll(abstractOverridden);
        }
        else if (implementations.size() > 1) {
            manyImpl.addAll(concreteOverridden);
        }
        else {
            abstractNoImpl.addAll(collectAbstractMethodsWithMoreSpecificReturnType(abstractOverridden, implementations.get(0)));
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
            if (!CallResolverUtil.isOrOverridesSynthesized(overridden)) {
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
        void overridingFinalMember(@NotNull CallableMemberDescriptor overridden);

        void returnTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overridden);

        void propertyTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overridden);

        void varOverriddenByVal(@NotNull CallableMemberDescriptor overridden);

        void cannotOverrideInvisibleMember(@NotNull CallableMemberDescriptor invisibleOverridden);

        void nothingToOverride();
    }

    private void checkOverrideForMember(@NotNull final CallableMemberDescriptor declared) {
        if (declared.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            // TODO: this should be replaced soon by a framework of synthesized member generation tools
            if (declared.getName().asString().startsWith(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX)) {
                checkOverrideForComponentFunction(declared);
            }
            return;
        }

        if (declared.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            return;
        }

        final JetNamedDeclaration member = (JetNamedDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(declared);
        if (member == null) {
            throw new IllegalStateException("declared descriptor is not resolved to declaration: " + declared);
        }

        JetModifierList modifierList = member.getModifierList();
        boolean hasOverrideNode = modifierList != null && modifierList.hasModifier(JetTokens.OVERRIDE_KEYWORD);
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = declared.getOverriddenDescriptors();

        if (hasOverrideNode) {
            checkOverridesForMemberMarkedOverride(declared, true, new CheckOverrideReportStrategy() {
                private boolean finalOverriddenError = false;
                private boolean typeMismatchError = false;
                private boolean kindMismatchError = false;

                @Override
                public void overridingFinalMember(@NotNull CallableMemberDescriptor overridden) {
                    if (!finalOverriddenError) {
                        finalOverriddenError = true;
                        trace.report(OVERRIDING_FINAL_MEMBER.on(member, overridden, overridden.getContainingDeclaration()));
                    }
                }

                @Override
                public void returnTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overridden) {
                    if (!typeMismatchError) {
                        typeMismatchError = true;
                        trace.report(RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden));
                    }
                }

                @Override
                public void propertyTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overridden) {
                    if (!typeMismatchError) {
                        typeMismatchError = true;
                        trace.report(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden));
                    }
                }

                @Override
                public void varOverriddenByVal(@NotNull CallableMemberDescriptor overridden) {
                    if (!kindMismatchError) {
                        kindMismatchError = true;
                        trace.report(VAR_OVERRIDDEN_BY_VAL.on((JetProperty) member, (PropertyDescriptor) declared, (PropertyDescriptor) overridden));
                    }
                }

                @Override
                public void cannotOverrideInvisibleMember(@NotNull CallableMemberDescriptor invisibleOverridden) {
                    trace.report(CANNOT_OVERRIDE_INVISIBLE_MEMBER.on(member, declared, invisibleOverridden));
                }

                @Override
                public void nothingToOverride() {
                    trace.report(NOTHING_TO_OVERRIDE.on(member, declared));
                }
            });
        }
        else if (!overriddenDescriptors.isEmpty()) {
            CallableMemberDescriptor overridden = overriddenDescriptors.iterator().next();
            trace.report(VIRTUAL_MEMBER_HIDDEN.on(member, declared, overridden, overridden.getContainingDeclaration()));
        }
    }

    private static void checkOverridesForMemberMarkedOverride(
            @NotNull CallableMemberDescriptor declared,
            boolean checkIfOverridesNothing,
            @NotNull CheckOverrideReportStrategy reportError
    ) {
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = declared.getOverriddenDescriptors();

        for (CallableMemberDescriptor overridden : overriddenDescriptors) {
            if (overridden == null) continue;

            if (!overridden.getModality().isOverridable()) {
                reportError.overridingFinalMember(overridden);
            }

            if (declared instanceof PropertyDescriptor &&
                !isPropertyTypeOkForOverride((PropertyDescriptor) overridden, (PropertyDescriptor) declared)) {
                reportError.propertyTypeMismatchOnOverride(overridden);
            }
            else if (!isReturnTypeOkForOverride(overridden, declared)) {
                reportError.returnTypeMismatchOnOverride(overridden);
            }

            if (checkPropertyKind(overridden, true) && checkPropertyKind(declared, false)) {
                reportError.varOverriddenByVal(overridden);
            }
        }

        if (checkIfOverridesNothing && overriddenDescriptors.isEmpty()) {
            DeclarationDescriptor containingDeclaration = declared.getContainingDeclaration();
            assert containingDeclaration instanceof ClassDescriptor : "Overrides may only be resolved in a class, but " + declared + " comes from " + containingDeclaration;
            ClassDescriptor declaringClass = (ClassDescriptor) containingDeclaration;

            CallableMemberDescriptor invisibleOverriddenDescriptor = findInvisibleOverriddenDescriptor(declared, declaringClass);
            if (invisibleOverriddenDescriptor != null) {
                reportError.cannotOverrideInvisibleMember(invisibleOverriddenDescriptor);
            }
            else {
                reportError.nothingToOverride();
            }
        }
    }

    public static boolean isReturnTypeOkForOverride(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor
    ) {
        TypeSubstitutor typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor);
        if (typeSubstitutor == null) return false;

        JetType superReturnType = superDescriptor.getReturnType();
        assert superReturnType != null;

        JetType subReturnType = subDescriptor.getReturnType();
        assert subReturnType != null;

        JetType substitutedSuperReturnType = typeSubstitutor.substitute(superReturnType, Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;

        return JetTypeChecker.DEFAULT.isSubtypeOf(subReturnType, substitutedSuperReturnType);
    }

    @Nullable
    private static TypeSubstitutor prepareTypeSubstitutor(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor
    ) {
        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();
        if (subTypeParameters.size() != superTypeParameters.size()) return null;

        Map<TypeConstructor, TypeProjection> substitutionContext = Maps.newHashMapWithExpectedSize(superTypeParameters.size());
        for (int i = 0; i < superTypeParameters.size(); i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
            substitutionContext.put(
                    superTypeParameter.getTypeConstructor(),
                    new TypeProjectionImpl(subTypeParameter.getDefaultType())
            );
        }
        return TypeSubstitutor.create(substitutionContext);
    }

    public static boolean isPropertyTypeOkForOverride(
            @NotNull PropertyDescriptor superDescriptor,
            @NotNull PropertyDescriptor subDescriptor
    ) {
        TypeSubstitutor typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor);
        if (typeSubstitutor == null) return false;

        if (!superDescriptor.isVar()) return true;

        JetType substitutedSuperReturnType = typeSubstitutor.substitute(superDescriptor.getType(), Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;
        return JetTypeChecker.DEFAULT.equalTypes(subDescriptor.getType(), substitutedSuperReturnType);
    }

    private void checkOverrideForComponentFunction(@NotNull final CallableMemberDescriptor componentFunction) {
        final JetAnnotationEntry dataAnnotation = findDataAnnotationForDataClass(componentFunction.getContainingDeclaration());

        checkOverridesForMemberMarkedOverride(componentFunction, false, new CheckOverrideReportStrategy() {
            private boolean overrideConflict = false;

            @Override
            public void overridingFinalMember(@NotNull CallableMemberDescriptor overridden) {
                if (!overrideConflict) {
                    overrideConflict = true;
                    trace.report(DATA_CLASS_OVERRIDE_CONFLICT.on(dataAnnotation, componentFunction, overridden.getContainingDeclaration()));
                }
            }

            @Override
            public void returnTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overridden) {
                if (!overrideConflict) {
                    overrideConflict = true;
                    trace.report(DATA_CLASS_OVERRIDE_CONFLICT.on(dataAnnotation, componentFunction, overridden.getContainingDeclaration()));
                }
            }

            @Override
            public void propertyTypeMismatchOnOverride(@NotNull CallableMemberDescriptor overridden) {
                throw new IllegalStateException("Component functions are not properties");
            }

            @Override
            public void varOverriddenByVal(@NotNull CallableMemberDescriptor overridden) {
                throw new IllegalStateException("Component functions are not properties");
            }

            @Override
            public void cannotOverrideInvisibleMember(@NotNull CallableMemberDescriptor invisibleOverridden) {
                throw new IllegalStateException("CANNOT_OVERRIDE_INVISIBLE_MEMBER should be reported on the corresponding property");
            }

            @Override
            public void nothingToOverride() {
                throw new IllegalStateException("Component functions are OK to override nothing");
            }
        });
    }

    @NotNull
    private JetAnnotationEntry findDataAnnotationForDataClass(@NotNull DeclarationDescriptor dataClass) {
        ClassDescriptor stdDataClassAnnotation = KotlinBuiltIns.getInstance().getDataClassAnnotation();
        AnnotationDescriptor annotation = dataClass.getAnnotations().findAnnotation(DescriptorUtils.getFqNameSafe(stdDataClassAnnotation));
        if (annotation == null) {
            throw new IllegalStateException("No data annotation is found for data class " + dataClass);
        }
        return BindingContextUtils.getNotNull(trace.getBindingContext(),
                                              BindingContext.ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT,
                                              annotation);
    }

    @Nullable
    private static CallableMemberDescriptor findInvisibleOverriddenDescriptor(
            @NotNull CallableMemberDescriptor declared,
            @NotNull ClassDescriptor declaringClass
    ) {
        for (JetType supertype : declaringClass.getTypeConstructor().getSupertypes()) {
            Set<CallableMemberDescriptor> all = Sets.newLinkedHashSet();
            all.addAll(supertype.getMemberScope().getFunctions(declared.getName()));
            //noinspection unchecked
            all.addAll((Collection) supertype.getMemberScope().getProperties(declared.getName()));
            for (CallableMemberDescriptor fromSuper : all) {
                if (OverridingUtil.DEFAULT.isOverridableBy(fromSuper, declared).getResult() == OVERRIDABLE) {
                    if (Visibilities.isVisible(fromSuper, declared)) {
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
            for (DeclarationDescriptor member : classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
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
            JetModifierListOwner declaration = (JetModifierListOwner) DescriptorToSourceUtils.descriptorToDeclaration(declared);
            if (declaration != null && !declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) {
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
        JetParameter parameter = (JetParameter) DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
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
        JetClassOrObject classElement = (JetClassOrObject) DescriptorToSourceUtils.descriptorToDeclaration(containingClass);
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

    private static boolean shouldReportParameterNameOverrideWarning(
            @NotNull ValueParameterDescriptor parameterFromSubclass,
            @NotNull ValueParameterDescriptor parameterFromSuperclass
    ) {
        DeclarationDescriptor subFunction = parameterFromSubclass.getContainingDeclaration();
        DeclarationDescriptor superFunction = parameterFromSuperclass.getContainingDeclaration();
        return subFunction instanceof CallableDescriptor && ((CallableDescriptor) subFunction).hasStableParameterNames() &&
               superFunction instanceof CallableDescriptor && ((CallableDescriptor) superFunction).hasStableParameterNames() &&
               !parameterFromSuperclass.getName().equals(parameterFromSubclass.getName());
    }

    private static boolean checkPropertyKind(@NotNull CallableMemberDescriptor descriptor, boolean isVar) {
        return descriptor instanceof PropertyDescriptor && ((PropertyDescriptor) descriptor).isVar() == isVar;
    }

    private void checkVisibility(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetDeclaration, CallableMemberDescriptor> entry : c.getMembers().entrySet()) {
            checkVisibilityForMember(entry.getKey(), entry.getValue());
        }
    }

    private void checkVisibilityForMember(@NotNull JetDeclaration declaration, @NotNull CallableMemberDescriptor memberDescriptor) {
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
    public static <D extends CallableMemberDescriptor> Set<D> getDirectlyOverriddenDeclarations(@NotNull D descriptor) {
        Set<D> result = Sets.newHashSet();
        //noinspection unchecked
        Set<D> overriddenDescriptors = (Set<D>) descriptor.getOverriddenDescriptors();
        for (D overriddenDescriptor : overriddenDescriptors) {
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
    public static <D extends CallableMemberDescriptor> Set<D> getAllOverriddenDeclarations(@NotNull D memberDescriptor) {
        Set<D> result = Sets.newHashSet();
        for (CallableMemberDescriptor overriddenDeclaration : memberDescriptor.getOverriddenDescriptors()) {
            CallableMemberDescriptor.Kind kind = overriddenDeclaration.getKind();
            if (kind == DECLARATION) {
                //noinspection unchecked
                result.add((D) overriddenDeclaration);
            }
            else if (kind == DELEGATION || kind == FAKE_OVERRIDE || kind == SYNTHESIZED) {
                //do nothing
            }
            else {
                throw new AssertionError("Unexpected callable kind " + kind);
            }
            //noinspection unchecked
            result.addAll(getAllOverriddenDeclarations((D) overriddenDeclaration));
        }
        return result;
    }

    @NotNull
    @ReadOnly
    public static <D extends CallableMemberDescriptor> Set<D> getDeepestSuperDeclarations(@NotNull D functionDescriptor) {
        Set<D> overriddenDeclarations = getAllOverriddenDeclarations(functionDescriptor);
        if (overriddenDeclarations.isEmpty()) {
            return Collections.singleton(functionDescriptor);
        }

        return filterOutOverriding(overriddenDeclarations);
    }
}
