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
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.CommonSuppliers;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;

public class OverrideResolver {

    private TopDownAnalysisContext context;
    private TopDownAnalysisParameters topDownAnalysisParameters;
    private BindingTrace trace;


    @Inject
    public void setContext(TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setTopDownAnalysisParameters(TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }



    public void process() {
        //all created fake descriptors are stored to resolve visibility on them later
        generateOverridesAndDelegation();

        checkVisibility();
        checkOverrides();
        checkParameterOverridesForAllClasses();
    }

    /**
     * Generate fake overrides and add overridden descriptors to existing descriptors.
     */
    private void generateOverridesAndDelegation() {
        Set<MutableClassDescriptor> ourClasses = new HashSet<MutableClassDescriptor>();
        ourClasses.addAll(context.getClasses().values());
        ourClasses.addAll(context.getObjects().values());
        
        Set<ClassifierDescriptor> processed = new HashSet<ClassifierDescriptor>();

        for (MutableClassDescriptorLite klass : ContainerUtil.reverse(context.getClassesTopologicalOrder())) {
            if (klass instanceof MutableClassDescriptor && ourClasses.contains(klass)) {
                generateOverridesAndDelegationInAClass((MutableClassDescriptor) klass, processed, ourClasses);
            }
        }
    }

    private void generateOverridesAndDelegationInAClass(
            @NotNull final MutableClassDescriptor classDescriptor,
            @NotNull Set<ClassifierDescriptor> processed,
            @NotNull Set<MutableClassDescriptor> classesBeingAnalyzed
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

        PsiElement declaration = BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);
        DelegationResolver.addDelegatedMembers(trace, (JetClassOrObject) declaration, classDescriptor);

        generateOverridesInAClass(classDescriptor);
    }

    private void generateOverridesInAClass(final MutableClassDescriptor classDescriptor) {
        List<CallableMemberDescriptor> membersFromSupertypes = getCallableMembersFromSupertypes(classDescriptor);

        MultiMap<Name, CallableMemberDescriptor> membersFromSupertypesByName = groupDescriptorsByName(membersFromSupertypes);

        MultiMap<Name, CallableMemberDescriptor> membersFromCurrentByName = groupDescriptorsByName(classDescriptor.getDeclaredCallableMembers());

        Set<Name> memberNames = new LinkedHashSet<Name>();
        memberNames.addAll(membersFromSupertypesByName.keySet());
        memberNames.addAll(membersFromCurrentByName.keySet());

        for (Name memberName : memberNames) {
            Collection<CallableMemberDescriptor> fromSupertypes = membersFromSupertypesByName.get(memberName);
            Collection<CallableMemberDescriptor> fromCurrent = membersFromCurrentByName.get(memberName);

            generateOverridesInFunctionGroup(
                    memberName,
                    fromSupertypes,
                    fromCurrent,
                    classDescriptor,
                    new DescriptorSink() {
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
                            JetDeclaration declaration = (JetDeclaration) BindingContextUtils
                                    .descriptorToDeclaration(trace.getBindingContext(), fromCurrent);
                            trace.report(Errors.CONFLICTING_OVERLOADS.on(declaration, fromCurrent, fromCurrent.getContainingDeclaration().getName().getName()));
                        }
                    });
        }
        resolveUnknownVisibilities(classDescriptor.getAllCallableMembers(), trace);
    }

    public static void resolveUnknownVisibilities(
            @NotNull Collection<? extends CallableMemberDescriptor> descriptors,
            @NotNull BindingTrace trace) {
        for (CallableMemberDescriptor memberDescriptor : descriptors) {
            JetDeclaration declaration = null;
            if (memberDescriptor.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
                PsiElement element = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), memberDescriptor);
                if (element instanceof JetDeclaration) {
                    declaration = (JetDeclaration) element;
                }
            }
            resolveUnknownVisibilityForMember(declaration, memberDescriptor, trace);
        }
    }

    public interface DescriptorSink {
        void addToScope(@NotNull CallableMemberDescriptor fakeOverride);
        
        void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent);
    }
    
    public static void generateOverridesInFunctionGroup(
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
            OverridingUtil.OverrideCompatibilityInfo.Result result =
                    OverridingUtil.isOverridableBy(fromSupertype, fromCurrent).getResult();

            boolean isVisible = Visibilities.isVisible(fromSupertype, current);
            switch (result) {
                case OVERRIDABLE:
                    if (isVisible) {
                        OverridingUtil.bindOverride(fromCurrent, fromSupertype);
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
            CallableMemberDescriptor notOverriddenFromSuper = findMemberWithMaxVisibility(fromSuperQueue);
            Collection<CallableMemberDescriptor> overridables = extractMembersOverridableBy(notOverriddenFromSuper, fromSuperQueue, sink);
            createAndBindFakeOverride(notOverriddenFromSuper, overridables, current, sink);
        }
    }

    @NotNull
    private static CallableMemberDescriptor findMemberWithMaxVisibility(@NotNull Queue<CallableMemberDescriptor> descriptors) {
        CallableMemberDescriptor descriptor = descriptors.element();
        for (CallableMemberDescriptor candidate : descriptors) {
            Integer result = Visibilities.compare(descriptor.getVisibility(), candidate.getVisibility());
            if (result != null && result < 0) {
                descriptor = candidate;
            }
        }
        return descriptor;
    }

    private static void createAndBindFakeOverride(
            @NotNull CallableMemberDescriptor notOverriddenFromSuper,
            @NotNull Collection<CallableMemberDescriptor> overridables,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink
    ) {
        Collection<CallableMemberDescriptor> visibleOverridables = filterVisible(current, overridables);
        Modality modality = getMinimalModality(visibleOverridables);
        boolean allInvisible = visibleOverridables.isEmpty();
        Collection<CallableMemberDescriptor> effectiveOverridden = allInvisible ? overridables : visibleOverridables;
        Visibility visibility = allInvisible ? Visibilities.INVISIBLE_FAKE : Visibilities.INHERITED;
        CallableMemberDescriptor fakeOverride =
                notOverriddenFromSuper.copy(current, modality, visibility, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
        for (CallableMemberDescriptor descriptor : effectiveOverridden) {
            OverridingUtil.bindOverride(fakeOverride, descriptor);
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
    private static Collection<CallableMemberDescriptor> filterVisible(
            @NotNull final ClassDescriptor current,
            @NotNull Collection<CallableMemberDescriptor> toFilter
    ) {
        return Collections2.filter(toFilter, new Predicate<CallableMemberDescriptor>() {
            @Override
            public boolean apply(@Nullable CallableMemberDescriptor descriptor) {
                return Visibilities.isVisible(descriptor, current);
            }
        });
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> extractMembersOverridableBy(
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

            OverridingUtil.OverrideCompatibilityInfo.Result result =
                    OverridingUtil.isOverridableBy(candidate, overrider).getResult();
            switch (result) {
                case OVERRIDABLE:
                    overridable.add(candidate);
                    iterator.remove();
                    break;
                case CONFLICT:
                    sink.conflict(overrider, candidate);
                    iterator.remove();
                    break;
                case INCOMPATIBLE:
                    break;
            }
        }
        return overridable;
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

    private void checkOverrides() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            checkOverridesInAClass(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            checkOverridesInAClass(entry.getValue(), entry.getKey());
        }
    }

    protected void checkOverridesInAClass(@NotNull MutableClassDescriptor classDescriptor, @NotNull JetClassOrObject klass) {
        if (topDownAnalysisParameters.isAnalyzingBootstrapLibrary()) return;

        // Check overrides for internal consistency
        for (CallableMemberDescriptor member : classDescriptor.getDeclaredCallableMembers()) {
            checkOverrideForMember(member);
        }

        // Check if everything that must be overridden, actually is
        // More than one implementation or no implementations at all
        Set<CallableMemberDescriptor> abstractNoImpl = Sets.newLinkedHashSet();
        Set<CallableMemberDescriptor> manyImpl = Sets.newLinkedHashSet();
        collectMissingImplementations(classDescriptor, abstractNoImpl, manyImpl);

        PsiElement nameIdentifier = null;
        if (klass instanceof JetClass) {
            nameIdentifier = klass.getNameIdentifier();
        }
        else if (klass instanceof JetObjectDeclaration) {
            nameIdentifier = klass.getNameIdentifier();
            if (nameIdentifier == null) {
                nameIdentifier = ((JetObjectDeclaration) klass).getObjectKeyword();
            }
        }
        if (nameIdentifier == null) return;

        for (CallableMemberDescriptor memberDescriptor : manyImpl) {
            trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(nameIdentifier, klass, memberDescriptor));
            break;
        }


        if (classDescriptor.getModality() == Modality.ABSTRACT) {
            return;
        }

        for (CallableMemberDescriptor memberDescriptor : abstractNoImpl) {
            trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(nameIdentifier, klass, memberDescriptor));
            break;
        }
    }

    public static void collectMissingImplementations(MutableClassDescriptor classDescriptor, Set<CallableMemberDescriptor> abstractNoImpl, Set<CallableMemberDescriptor> manyImpl) {
        for (CallableMemberDescriptor descriptor : classDescriptor.getAllCallableMembers()) {
            collectMissingImplementations(descriptor, abstractNoImpl, manyImpl);
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
        Set<CallableMemberDescriptor> allFilteredOverriddenDeclarations = OverridingUtil.filterOverrides(Sets.newLinkedHashSet(allOverriddenDeclarations));

        Set<CallableMemberDescriptor> relevantDirectlyOverridden =
                getRelevantDirectlyOverridden(overriddenDeclarationsByDirectParent, allFilteredOverriddenDeclarations);

        int implCount = countImplementations(relevantDirectlyOverridden);
        if (implCount == 0) {
            collectDescriptorsByModality(allFilteredOverriddenDeclarations, abstractNoImpl, Modality.ABSTRACT);
        }
        else if (implCount > 1) {
            collectDescriptorsByModality(allFilteredOverriddenDeclarations, manyImpl, Modality.OPEN, Modality.FINAL);
        }
    }

    private static int countImplementations(@NotNull Set<CallableMemberDescriptor> relevantDirectlyOverridden) {
        int implCount = 0;
        for (CallableMemberDescriptor overriddenDescriptor : relevantDirectlyOverridden) {
            if (overriddenDescriptor.getModality() != Modality.ABSTRACT) {
                implCount++;
            }
        }
        return implCount;
    }

    private static void collectDescriptorsByModality(
            @NotNull Set<CallableMemberDescriptor> allOverriddenDeclarations,
            @NotNull Set<CallableMemberDescriptor> result,
            Modality... modalities
    ) {
        Set<Modality> modalitySet = Sets.newHashSet(modalities);
        for (CallableMemberDescriptor overridden : allOverriddenDeclarations) {
            if (modalitySet.contains(overridden.getModality())) {
                result.add(overridden);
            }
        }
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

        Map<CallableMemberDescriptor, Set<CallableMemberDescriptor>> relevantOverriddenByParent = Maps.newLinkedHashMap(overriddenByParent);

        for (Map.Entry<CallableMemberDescriptor, Set<CallableMemberDescriptor>> entry : overriddenByParent.entrySet()) {
            CallableMemberDescriptor directlyOverridden = entry.getKey();
            Set<CallableMemberDescriptor> declarationSet = entry.getValue();
            if (!isRelevant(declarationSet, relevantOverriddenByParent.values(), allFilteredOverriddenDeclarations)) {
                relevantOverriddenByParent.remove(directlyOverridden);
            }
        }
        return relevantOverriddenByParent.keySet();
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
            Collection<CallableMemberDescriptor> overriddenDeclarations = OverridingUtil.getOverriddenDeclarations(descriptor);
            Set<CallableMemberDescriptor> filteredOverrides = OverridingUtil.filterOverrides(Sets.newLinkedHashSet(overriddenDeclarations));
            Set<CallableMemberDescriptor> overridden = Sets.newLinkedHashSet();
            for (CallableMemberDescriptor memberDescriptor : filteredOverrides) {
                overridden.add(memberDescriptor);
            }
            overriddenDeclarationsByDirectParent.put(descriptor, overridden);
        }
        return overriddenDeclarationsByDirectParent;
    }

    public static Multimap<CallableMemberDescriptor, CallableMemberDescriptor> collectSuperMethods(MutableClassDescriptor classDescriptor) {
        Set<CallableMemberDescriptor> inheritedFunctions = Sets.newLinkedHashSet();
        for (JetType supertype : classDescriptor.getSupertypes()) {
            for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                if (descriptor instanceof CallableMemberDescriptor) {
                    CallableMemberDescriptor memberDescriptor = (CallableMemberDescriptor) descriptor;
                    inheritedFunctions.add(memberDescriptor);
                }
            }
        }

        // Only those actually inherited
        Set<CallableMemberDescriptor> filteredMembers = OverridingUtil.filterOverrides(inheritedFunctions);

        // Group members with "the same" signature
        Multimap<CallableMemberDescriptor, CallableMemberDescriptor> factoredMembers = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        for (CallableMemberDescriptor one : filteredMembers) {
            if (factoredMembers.values().contains(one)) continue;
            for (CallableMemberDescriptor another : filteredMembers) {
//                if (one == another) continue;
                factoredMembers.put(one, one);
                if (OverridingUtil.isOverridableBy(one, another).getResult() == OVERRIDABLE
                        || OverridingUtil.isOverridableBy(another, one).getResult() == OVERRIDABLE) {
                    factoredMembers.put(one, another);
                }
            }
        }
        return factoredMembers;
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
            if (declared.getName().getName().startsWith(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX)) {
                checkOverrideForComponentFunction(declared);
            }
            return;
        }

        if (declared.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            return;
        }

        final JetNamedDeclaration member = (JetNamedDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), declared);
        if (member == null) {
            throw new IllegalStateException("declared descriptor is not resolved to declaration: " + declared);
        }

        JetModifierList modifierList = member.getModifierList();
        final ASTNode overrideNode = modifierList != null ? modifierList.getModifierNode(JetTokens.OVERRIDE_KEYWORD) : null;
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = declared.getOverriddenDescriptors();

        if (overrideNode != null) {
            checkOverridesForMemberMarkedOverride(declared, true, new CheckOverrideReportStrategy() {
                private boolean finalOverriddenError = false;
                private boolean typeMismatchError = false;
                private boolean kindMismatchError = false;

                @Override
                public void overridingFinalMember( @NotNull CallableMemberDescriptor overridden) {
                    if (!finalOverriddenError) {
                        finalOverriddenError = true;
                        trace.report(OVERRIDING_FINAL_MEMBER.on(overrideNode.getPsi(), overridden, overridden.getContainingDeclaration()));
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
                    trace.report(CANNOT_OVERRIDE_INVISIBLE_MEMBER.on(member, declared, invisibleOverridden, invisibleOverridden.getContainingDeclaration()));
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

    private void checkOverridesForMemberMarkedOverride(
            @NotNull CallableMemberDescriptor declared,
            boolean checkIfOverridesNothing,
            @NotNull CheckOverrideReportStrategy reportError
    ) {
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = declared.getOverriddenDescriptors();

        for (CallableMemberDescriptor overridden : overriddenDescriptors) {
            if (overridden != null) {
                if (!overridden.getModality().isOverridable()) {
                    reportError.overridingFinalMember(overridden);
                }

                if (declared instanceof PropertyDescriptor && !OverridingUtil.isPropertyTypeOkForOverride(
                        JetTypeChecker.INSTANCE, (PropertyDescriptor) overridden, (PropertyDescriptor) declared)) {
                    reportError.propertyTypeMismatchOnOverride(overridden);
                }
                else if (!OverridingUtil.isReturnTypeOkForOverride(JetTypeChecker.INSTANCE, overridden, declared)) {
                    reportError.returnTypeMismatchOnOverride(overridden);
                }

                if (checkPropertyKind(overridden, true) && checkPropertyKind(declared, false)) {
                    reportError.varOverriddenByVal(overridden);
                }
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
        for (AnnotationDescriptor annotation : dataClass.getAnnotations()) {
            if (stdDataClassAnnotation.equals(annotation.getType().getConstructor().getDeclarationDescriptor())) {
                return BindingContextUtils.getNotNull(trace.getBindingContext(),
                                                      BindingContext.ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT,
                                                      annotation);
            }
        }
        throw new IllegalStateException("No data annotation is found for data class");
    }

    private CallableMemberDescriptor findInvisibleOverriddenDescriptor(CallableMemberDescriptor declared, ClassDescriptor declaringClass) {
        CallableMemberDescriptor invisibleOverride = null;
        outer:
        for (JetType supertype : declaringClass.getTypeConstructor().getSupertypes()) {
            Set<CallableMemberDescriptor> all = Sets.newLinkedHashSet();
            all.addAll(supertype.getMemberScope().getFunctions(declared.getName()));
            all.addAll((Set) supertype.getMemberScope().getProperties(declared.getName()));
            for (CallableMemberDescriptor fromSuper : all) {
                if (OverridingUtil.isOverridableBy(fromSuper, declared).getResult() == OVERRIDABLE) {
                    invisibleOverride = fromSuper;
                    if (Visibilities.isVisible(fromSuper, declared)) {
                        throw new IllegalStateException("Descriptor " + fromSuper + " is overridable by " + declared + " and visible but does not appear in its getOverriddenDescriptors()");
                    }
                    break outer;
                }
            }
        }
        return invisibleOverride;
    }

    private void checkParameterOverridesForAllClasses() {
        List<MutableClassDescriptor> allClasses = Lists.newArrayList(context.getClasses().values());
        allClasses.addAll(context.getObjects().values());
        for (MutableClassDescriptor classDescriptor : allClasses) {
            Collection<CallableMemberDescriptor> members = classDescriptor.getAllCallableMembers();
            for (CallableMemberDescriptor member : members) {
                checkOverridesForParameters(member);
            }
        }
    }

    private void checkOverridesForParameters(CallableMemberDescriptor declared) {
        boolean noDeclaration = declared.getKind() != CallableMemberDescriptor.Kind.DECLARATION;
        if (!noDeclaration) {
            // No check if the function is not marked as 'override'
            JetModifierListOwner declaration =
                    (JetModifierListOwner) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), declared);
            if (!declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) {
                return;
            }
        }

        // Let p1 be a parameter of the overriding function
        // Let p2 be a parameter of the function being overridden
        // Then
        //  a) p1 is not allowed to have a default value declared
        //  b) p1 must have the same name as p2
        for (ValueParameterDescriptor parameterFromSubclass : declared.getValueParameters()) {
            JetParameter parameter =
                    noDeclaration ? null :
                            (JetParameter) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), parameterFromSubclass);

            JetClassOrObject classElement = noDeclaration ? (JetClassOrObject) BindingContextUtils
                    .descriptorToDeclaration(trace.getBindingContext(), declared.getContainingDeclaration()) : null;

            if (parameterFromSubclass.declaresDefaultValue() && !noDeclaration) {
                trace.report(DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE.on(parameter));
            }

            boolean superWithDefault = false;
            for (ValueParameterDescriptor parameterFromSuperclass : parameterFromSubclass.getOverriddenDescriptors()) {
                if (parameterFromSuperclass.declaresDefaultValue()) {
                    if (!superWithDefault) {
                        superWithDefault = true;
                    }
                    else {
                        if (noDeclaration) {
                            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE.on(classElement, parameterFromSubclass));
                        }
                        else {
                            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES.on(parameter, parameterFromSubclass));
                        }
                        break;
                    }
                }

                if (!parameterFromSuperclass.getName().equals(parameterFromSubclass.getName())) {
                    if (noDeclaration) {
                        trace.report(DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES.on(classElement, declared.getOverriddenDescriptors(), parameterFromSuperclass.getIndex() + 1));
                    }
                    else {
                        trace.report(PARAMETER_NAME_CHANGED_ON_OVERRIDE.on(parameter, (ClassDescriptor) parameterFromSuperclass.getContainingDeclaration().getContainingDeclaration(), parameterFromSuperclass));
                    }
                }
            }
        }
    }

    private boolean checkPropertyKind(CallableMemberDescriptor descriptor, boolean isVar) {
        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            return propertyDescriptor.isVar() == isVar;
        }
        return false;
    }

    public static void resolveUnknownVisibilityForMember(@Nullable JetDeclaration member, @NotNull CallableMemberDescriptor memberDescriptor, @NotNull BindingTrace trace) {
        resolveUnknownVisibilityForOverriddenDescriptors(memberDescriptor.getOverriddenDescriptors(), trace);
        if (memberDescriptor.getVisibility() != Visibilities.INHERITED) {
            return;
        }

        Visibility visibility = findMaxVisibility(memberDescriptor.getOverriddenDescriptors());
        if (visibility == null) {
            if (member != null) {
                trace.report(CANNOT_INFER_VISIBILITY.on(member));
            }
            visibility = Visibilities.PUBLIC;
        }

        if (memberDescriptor instanceof PropertyDescriptorImpl) {
            ((PropertyDescriptorImpl)memberDescriptor).setVisibility(visibility);
            for (PropertyAccessorDescriptor accessor : ((PropertyDescriptor) memberDescriptor).getAccessors()) {
                resolveUnknownVisibilityForMember(null, accessor, trace);
            }
        }
        else if (memberDescriptor instanceof FunctionDescriptorImpl) {
            ((FunctionDescriptorImpl)memberDescriptor).setVisibility(visibility);
        }
        else {
            assert memberDescriptor instanceof PropertyAccessorDescriptorImpl;
            ((PropertyAccessorDescriptorImpl) memberDescriptor).setVisibility(visibility);
        }
    }

    private static void resolveUnknownVisibilityForOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> descriptors, @NotNull BindingTrace trace) {
        for (CallableMemberDescriptor descriptor : descriptors) {
            if (descriptor.getVisibility() == Visibilities.INHERITED) {
                PsiElement element = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), descriptor);
                JetDeclaration declaration = (element instanceof JetDeclaration) ? (JetDeclaration) element : null;
                resolveUnknownVisibilityForMember(declaration, descriptor, trace);
            }
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

    private void checkVisibility() {
        for (Map.Entry<JetDeclaration, CallableMemberDescriptor> entry : context.getMembers().entrySet()) {
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
}
