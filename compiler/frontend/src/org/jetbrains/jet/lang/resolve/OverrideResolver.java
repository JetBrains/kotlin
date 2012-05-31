/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.CommonSuppliers;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;

/**
 * @author abreslav
 */
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
        generateOverrides();

        checkVisibility();
        checkOverrides();
        checkParameterOverridesForAllClasses();
    }

    /**
     * Generate fake overrides and add overridden descriptors to existing descriptors.
     */
    private void generateOverrides() {
        Set<MutableClassDescriptor> ourClasses = new HashSet<MutableClassDescriptor>();
        ourClasses.addAll(context.getClasses().values());
        ourClasses.addAll(context.getObjects().values());
        
        Set<ClassifierDescriptor> processed = new HashSet<ClassifierDescriptor>();

        for (MutableClassDescriptor clazz : ourClasses) {
            generateOverridesInAClass(clazz, processed, ourClasses);
        }
    }

    private void generateOverridesInAClass(@NotNull final MutableClassDescriptor classDescriptor,
            @NotNull Set<ClassifierDescriptor> processed,
            @NotNull Set<MutableClassDescriptor> ourClasses) {
        if (!processed.add(classDescriptor)) {
            return;
        }

        // avoid processing stdlib classes twice
        if (!ourClasses.contains(classDescriptor)) {
            return;
        }

        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            ClassDescriptor superclass = (ClassDescriptor) supertype.getConstructor().getDeclarationDescriptor();
            if (superclass instanceof MutableClassDescriptor) {
                generateOverridesInAClass((MutableClassDescriptor) superclass, processed, ourClasses);
            }
        }

        doGenerateOverridesInAClass(classDescriptor);
    }

    private void doGenerateOverridesInAClass(final MutableClassDescriptor classDescriptor) {
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
        for (CallableMemberDescriptor memberDescriptor : classDescriptor.getAllCallableMembers()) {
            JetDeclaration declaration = null;
            if (memberDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                PsiElement element = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), memberDescriptor);
                if (element instanceof JetDeclaration) {
                    declaration = (JetDeclaration) element;
                }
            }
            resolveUnknownVisibilityForMember(declaration, memberDescriptor);
        }
    }

    public interface DescriptorSink {
        void addToScope(@NotNull CallableMemberDescriptor fakeOverride);
        
        void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent);
    }
    
    public static void generateOverridesInFunctionGroup(
            @NotNull Name name,
            @NotNull Collection<? extends CallableMemberDescriptor> functionsFromSupertypes,
            @NotNull Collection<? extends CallableMemberDescriptor> functionsFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink) {
        
        List<CallableMemberDescriptor> fakeOverrideList = Lists.newArrayList();

        for (CallableMemberDescriptor functionFromSupertype : functionsFromSupertypes) {
            boolean overrides = false;

            boolean isVisible = Visibilities.isVisible(functionFromSupertype, current);
            
            for (CallableMemberDescriptor functionFromCurrent : functionsFromCurrent) {
                OverridingUtil.OverrideCompatibilityInfo.Result result = OverridingUtil.isOverridableBy(functionFromSupertype, functionFromCurrent).getResult();
                if (result == OVERRIDABLE) {

                    if (isVisible) {
                        OverridingUtil.bindOverride(functionFromCurrent, functionFromSupertype);
                    }
                    overrides = true;
                }
                else if (result == OverridingUtil.OverrideCompatibilityInfo.Result.CONFLICT) {
                    sink.conflict(functionFromSupertype, functionFromCurrent);
                }
            }
            
            for (CallableMemberDescriptor fakeOverride : fakeOverrideList) {
                if (OverridingUtil.isOverridableBy(functionFromSupertype, fakeOverride).getResult() == OVERRIDABLE) {
                    OverridingUtil.bindOverride(fakeOverride, functionFromSupertype);
                    overrides = true;
                }
            }

            if (!overrides) {
                CallableMemberDescriptor fakeOverride = functionFromSupertype.copy(current, false, !isVisible, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
                OverridingUtil.bindOverride(fakeOverride, functionFromSupertype);
                fakeOverrideList.add(fakeOverride);
                sink.addToScope(fakeOverride);
            }
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

    private static void collectMissingImplementations(CallableMemberDescriptor descriptor, Set<CallableMemberDescriptor> abstractNoImpl, Set<CallableMemberDescriptor> manyImpl) {
        if (!descriptor.getKind().isReal()) {
            Collection<CallableMemberDescriptor> overriddenDeclarations = OverridingUtil.getOverriddenDeclarations(descriptor);
            if (overriddenDeclarations.size() == 0) {
                throw new IllegalStateException("A 'fake override' must override something");
            }
            else {
                List<CallableMemberDescriptor> nonAbstractManyImpl = Lists.newArrayList();
                Set<CallableMemberDescriptor> filteredOverriddenDeclarations = OverridingUtil.filterOverrides(Sets.newHashSet(overriddenDeclarations));
                boolean allSuperAbstract = true;
                for (CallableMemberDescriptor overridden : filteredOverriddenDeclarations) {
                    if (overridden.getModality() != Modality.ABSTRACT) {
                        nonAbstractManyImpl.add(overridden);
                        allSuperAbstract = false;
                    }
                }
                if (nonAbstractManyImpl.size() > 1) {
                    manyImpl.addAll(nonAbstractManyImpl);
                }
                else if (allSuperAbstract) {
                    abstractNoImpl.addAll(overriddenDeclarations);
                }
            }
        }
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

    private void checkOverrideForMember(@NotNull CallableMemberDescriptor declared) {
        JetNamedDeclaration member = (JetNamedDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), declared);
        if (member == null) {
            Boolean delegated = trace.get(DELEGATED, declared);
            if (delegated == null || !delegated)
                throw new IllegalStateException(
                        "decriptor is not resolved to declaration" +
                        " and it is not delegate: " + declared + ", DELEGATED: " + delegated);
            return;
        }

        if (declared.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            return;
        }

        JetModifierList modifierList = member.getModifierList();
        ASTNode overrideNode = modifierList != null ? modifierList.getModifierNode(JetTokens.OVERRIDE_KEYWORD) : null;
        boolean hasOverrideModifier = overrideNode != null;

        boolean finalOverriddenError = false;
        boolean typeMismatchError = false;
        boolean kindMismatchError = false;
        for (CallableMemberDescriptor overridden : declared.getOverriddenDescriptors()) {
            if (overridden != null) {
                if (hasOverrideModifier) {
                    if (!overridden.getModality().isOverridable() && !finalOverriddenError) {
                        trace.report(OVERRIDING_FINAL_MEMBER.on(overrideNode.getPsi(), overridden, overridden.getContainingDeclaration()));
                        finalOverriddenError = true;
                    }

                    if (!OverridingUtil.isReturnTypeOkForOverride(JetTypeChecker.INSTANCE, overridden, declared) && !typeMismatchError) {
                        trace.report(RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden));
                        typeMismatchError = true;
                    }

                    if (checkPropertyKind(overridden, true) && checkPropertyKind(declared, false) && !kindMismatchError) {
                        trace.report(VAR_OVERRIDDEN_BY_VAL.on((JetProperty) member, (PropertyDescriptor) declared, (PropertyDescriptor) overridden));
                        kindMismatchError = true;
                    }
                }
            }
        }

        if (hasOverrideModifier && declared.getOverriddenDescriptors().size() == 0) {
            DeclarationDescriptor containingDeclaration = declared.getContainingDeclaration();
            assert containingDeclaration instanceof ClassDescriptor : "Overrides may only be resolved in a class, but " + declared + " comes from " + containingDeclaration;
            ClassDescriptor declaringClass = (ClassDescriptor) containingDeclaration;

            CallableMemberDescriptor invisibleOverriddenDescriptor = findInvisibleOverriddenDescriptor(declared, declaringClass);
            if (invisibleOverriddenDescriptor != null) {
                trace.report(CANNOT_OVERRIDE_INVISIBLE_MEMBER.on(member, declared, invisibleOverriddenDescriptor,
                                                                 invisibleOverriddenDescriptor.getContainingDeclaration()));
            }
            else {
                trace.report(NOTHING_TO_OVERRIDE.on(member, declared));
            }
        }
        PsiElement nameIdentifier = member.getNameIdentifier();
        if (!hasOverrideModifier && declared.getOverriddenDescriptors().size() > 0 && nameIdentifier != null) {
            CallableMemberDescriptor overridden = declared.getOverriddenDescriptors().iterator().next();
            trace.report(VIRTUAL_MEMBER_HIDDEN.on(member, declared, overridden, overridden.getContainingDeclaration()));
        }
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
                        throw new IllegalStateException("Descriptor " + fromSuper + "is overridable by " + declared + " and visible but does not appear in its getOverriddenDescriptors()");
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
        boolean fakeOverride = declared.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
        if (!fakeOverride) {
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
                    fakeOverride ? null :
                            (JetParameter) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), parameterFromSubclass);

            JetClassOrObject classElement = fakeOverride ? (JetClassOrObject) BindingContextUtils
                    .descriptorToDeclaration(trace.getBindingContext(), declared.getContainingDeclaration()) : null;

            if (parameterFromSubclass.declaresDefaultValue() && !fakeOverride) {
                trace.report(DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE.on(parameter));
            }

            boolean superWithDefault = false;
            for (ValueParameterDescriptor parameterFromSuperclass : parameterFromSubclass.getOverriddenDescriptors()) {
                if (parameterFromSuperclass.declaresDefaultValue()) {
                    if (!superWithDefault) {
                        superWithDefault = true;
                    }
                    else {
                        if (fakeOverride) {
                            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE.on(classElement, parameterFromSubclass));
                        }
                        else {
                            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES.on(parameter, parameterFromSubclass));
                        }
                        break;
                    }
                }

                if (!parameterFromSuperclass.getName().equals(parameterFromSubclass.getName())) {
                    if (fakeOverride) {
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

    private void resolveUnknownVisibilityForMember(@Nullable JetDeclaration member, @NotNull CallableMemberDescriptor memberDescriptor) {
        resolveUnknownVisibilityForOverriddenDescriptors(memberDescriptor.getOverriddenDescriptors());
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

        if (memberDescriptor instanceof PropertyDescriptor) {
            ((PropertyDescriptor)memberDescriptor).setVisibility(visibility);
        }
        else {
            assert memberDescriptor instanceof FunctionDescriptorImpl;
            ((FunctionDescriptorImpl)memberDescriptor).setVisibility(visibility);
        }
    }

    private void resolveUnknownVisibilityForOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> descriptors) {
        for (CallableMemberDescriptor descriptor : descriptors) {
            if (descriptor.getVisibility() == Visibilities.INHERITED) {
                PsiElement element = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), descriptor);
                JetDeclaration declaration = (element instanceof JetDeclaration) ? (JetDeclaration) element : null;
                resolveUnknownVisibilityForMember(declaration, descriptor);
            }
        }
    }

    @Nullable
    private Visibility findMaxVisibility(@NotNull Collection<? extends CallableMemberDescriptor> descriptors) {
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
