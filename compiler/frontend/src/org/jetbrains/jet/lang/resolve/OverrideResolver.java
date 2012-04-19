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

import com.google.common.collect.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.CommonSuppliers;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;

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
        Set<CallableMemberDescriptor> fakeOverrides = Sets.newHashSet();
        generateOverrides(fakeOverrides);

        //functions and properties visibility can be inherited when overriding, so for overridden members
        //it can be resolved only after overrides resolve is finished
        resolveUnknownVisibilityForMembers(fakeOverrides);

        //invisible overridden descriptors are saved for proper error reporting
        Multimap<CallableDescriptor, CallableDescriptor> invisibleOverriddenDescriptors = LinkedHashMultimap.create();
        removeInvisibleOverriddenDescriptors(invisibleOverriddenDescriptors);

        checkVisibility();
        checkOverrides(invisibleOverriddenDescriptors);
    }

    /**
     * Generate fake overrides and add overridden descriptors to existing descriptors.
     */
    private void generateOverrides(@NotNull Set<CallableMemberDescriptor> fakeOverrides) {
        Set<MutableClassDescriptor> ourClasses = new HashSet<MutableClassDescriptor>();
        ourClasses.addAll(context.getClasses().values());
        ourClasses.addAll(context.getObjects().values());
        
        Set<ClassifierDescriptor> processed = new HashSet<ClassifierDescriptor>();

        for (MutableClassDescriptor clazz : ourClasses) {
            generateOverridesInAClass(clazz, processed, ourClasses, fakeOverrides);
        }
    }

    private void generateOverridesInAClass(@NotNull final MutableClassDescriptor classDescriptor, @NotNull Set<ClassifierDescriptor> processed,
            @NotNull Set<MutableClassDescriptor> ourClasses, @NotNull Set<CallableMemberDescriptor> fakeOverrides) {
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
                generateOverridesInAClass((MutableClassDescriptor) superclass, processed, ourClasses, fakeOverrides);
            }
        }

        List<CallableMemberDescriptor> functionsFromSupertypes = getDescriptorsFromSupertypes(classDescriptor);

        MultiMap<String, CallableMemberDescriptor> functionsFromSupertypesByName = groupDescriptorsByName(functionsFromSupertypes);

        MultiMap<String, CallableMemberDescriptor> functionsFromCurrentByName = groupDescriptorsByName(classDescriptor.getCallableMembers());

        Set<String> functionNames = new LinkedHashSet<String>();
        functionNames.addAll(functionsFromSupertypesByName.keySet());
        functionNames.addAll(functionsFromCurrentByName.keySet());
        
        for (String functionName : functionNames) {
            generateOverridesInFunctionGroup(functionName, fakeOverrides,
                    functionsFromSupertypesByName.get(functionName),
                    functionsFromCurrentByName.get(functionName),
                    classDescriptor,
                    new DescriptorSink() {
                        @Override
                        public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                            if (fakeOverride instanceof PropertyDescriptor) {
                                classDescriptor.getScopeForMemberLookupAsWritableScope().addPropertyDescriptor((PropertyDescriptor) fakeOverride);
                            }
                            else if (fakeOverride instanceof SimpleFunctionDescriptor) {
                                classDescriptor.getScopeForMemberLookupAsWritableScope().addFunctionDescriptor((SimpleFunctionDescriptor) fakeOverride);
                            }
                            else {
                                throw new IllegalStateException(fakeOverride.getClass().getName());
                            }
                        }

                        @Override
                        public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                            JetDeclaration jetProperty = (JetDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), fromCurrent);
                            trace.report(Errors.CONFLICTING_OVERLOADS.on(jetProperty, fromCurrent, fromCurrent.getContainingDeclaration().getName()));
                        }
                    });
        }
    }
    
    public interface DescriptorSink {
        void addToScope(@NotNull CallableMemberDescriptor fakeOverride);
        
        void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent);
    }
    
    public static void generateOverridesInFunctionGroup(
            @NotNull String name,
            @Nullable Set<CallableMemberDescriptor> fakeOverrides,
            @NotNull Collection<? extends CallableMemberDescriptor> functionsFromSupertypes,
            @NotNull Collection<? extends CallableMemberDescriptor> functionsFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink) {
        
        List<CallableMemberDescriptor> fakeOverrideList = Lists.newArrayList();

        for (CallableMemberDescriptor functionFromSupertype : functionsFromSupertypes) {
            boolean overrides = false;
            
            for (CallableMemberDescriptor functionFromCurrent : functionsFromCurrent) {
                OverridingUtil.OverrideCompatibilityInfo.Result result = OverridingUtil.isOverridableBy(functionFromSupertype, functionFromCurrent).getResult();
                if (result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                    functionFromCurrent.addOverriddenDescriptor(functionFromSupertype);
                    overrides = true;
                }
                else if (result == OverridingUtil.OverrideCompatibilityInfo.Result.CONFLICT) {
                    sink.conflict(functionFromSupertype, functionFromCurrent);
                }
            }
            
            for (CallableMemberDescriptor fakeOverride : fakeOverrideList) {
                if (OverridingUtil.isOverridableBy(functionFromSupertype, fakeOverride).getResult() == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                    fakeOverride.addOverriddenDescriptor(functionFromSupertype);
                    overrides = true;
                }
            }

            if (!overrides) {
                CallableMemberDescriptor fakeOverride = functionFromSupertype.copy(current, false, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
                fakeOverride.addOverriddenDescriptor(functionFromSupertype);
                fakeOverrideList.add(fakeOverride);
                if (fakeOverrides != null) {
                    fakeOverrides.add(fakeOverride);
                }
                sink.addToScope(fakeOverride);
            }
        }
    }


    private static <T extends DeclarationDescriptor> MultiMap<String, T> groupDescriptorsByName(Collection<T> properties) {
        MultiMap<String, T> r = new LinkedMultiMap<String, T>();
        for (T property : properties) {
            r.putValue(property.getName(), property);
        }
        return r;
    }


    private static List<CallableMemberDescriptor> getDescriptorsFromSupertypes(ClassDescriptor classDescriptor) {
        Set<CallableMemberDescriptor> r = Sets.newLinkedHashSet();
        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            r.addAll(getDescriptorsOfType(supertype.getMemberScope()));
        }
        return new ArrayList<CallableMemberDescriptor>(r);
    }

    private static <T extends DeclarationDescriptor> List<CallableMemberDescriptor> getDescriptorsOfType(
            JetScope scope) {
        List<CallableMemberDescriptor> r = Lists.newArrayList();
        for (DeclarationDescriptor decl : scope.getAllDescriptors()) {
            if (decl instanceof PropertyDescriptor || decl instanceof SimpleFunctionDescriptor) {
                r.add((CallableMemberDescriptor) decl);
            }
        }
        return r;
    }

    private void checkOverrides(@NotNull Multimap<CallableDescriptor, CallableDescriptor> invisibleOverriddenDescriptors) {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            checkOverridesInAClass(entry.getValue(), entry.getKey(), invisibleOverriddenDescriptors);
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            checkOverridesInAClass(entry.getValue(), entry.getKey(), invisibleOverriddenDescriptors);
        }
    }

    protected void checkOverridesInAClass(@NotNull MutableClassDescriptor classDescriptor, @NotNull JetClassOrObject klass,
            @NotNull Multimap<CallableDescriptor, CallableDescriptor> invisibleOverriddenDescriptors) {
        if (topDownAnalysisParameters.isAnalyzingBootstrapLibrary()) return;

        // Check overrides for internal consistency
        for (CallableMemberDescriptor member : classDescriptor.getCallableMembers()) {
            checkOverrideForMember(member, invisibleOverriddenDescriptors);
        }

        // Check if everything that must be overridden, actually is
        // More than one implementation or no implementations at all
        Set<CallableMemberDescriptor> abstractNoImpl = Sets.newLinkedHashSet();
        Set<CallableMemberDescriptor> manyImpl = Sets.newLinkedHashSet();
        collectMissingImplementations(classDescriptor, abstractNoImpl, manyImpl);

        PsiElement nameIdentifier = null;
        if (klass instanceof JetClass) {
            nameIdentifier = ((JetClass) klass).getNameIdentifier();
        }
        else if (klass instanceof JetObjectDeclaration) {
            nameIdentifier = ((JetObjectDeclaration) klass).getNameIdentifier();
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
        
        for (DeclarationDescriptor descriptor : classDescriptor.getScopeForMemberLookup().getAllDescriptors()) {
            if (descriptor instanceof CallableMemberDescriptor) {
                collectMissingImplementations((CallableMemberDescriptor) descriptor, abstractNoImpl, manyImpl);
            }
        }
    }

    private static void collectMissingImplementations(CallableMemberDescriptor descriptor, Set<CallableMemberDescriptor> abstractNoImpl, Set<CallableMemberDescriptor> manyImpl) {
        if (descriptor.getKind().isReal()) {
            if (descriptor.getModality() == Modality.ABSTRACT) {
                //abstractNoImpl.add(descriptor);
            }
        }
        else {
            Collection<CallableMemberDescriptor> overriddenDeclarations = OverridingUtil.getOverridenDeclarations(descriptor);
            if (overriddenDeclarations.size() == 0) {
                throw new IllegalStateException();
            }
            else if (overriddenDeclarations.size() == 1) {
                CallableMemberDescriptor single = overriddenDeclarations.iterator().next();
                if (single.getModality() == Modality.ABSTRACT) {
                    abstractNoImpl.add(single);
                }
            }
            else {
                List<CallableMemberDescriptor> nonAbstractManyImpl = Lists.newArrayList();
                for (CallableMemberDescriptor overridden : overriddenDeclarations) {
                    if (overridden.getModality() != Modality.ABSTRACT) {
                        nonAbstractManyImpl.add(overridden);
                    }
                }
                if (nonAbstractManyImpl.size() > 1) {
                    manyImpl.addAll(nonAbstractManyImpl);
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
                if (OverridingUtil.isOverridableBy(one, another).getResult() == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
                        || OverridingUtil.isOverridableBy(another, one).getResult() == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                    factoredMembers.put(one, another);
                }
            }
        }
        return factoredMembers;
    }

    private void checkOverrideForMember(@NotNull CallableMemberDescriptor declared,
            @NotNull Multimap<CallableDescriptor, CallableDescriptor> invisibleOverriddenDescriptors) {
        JetNamedDeclaration member = (JetNamedDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), declared);
        if (member == null) {
            assert trace.get(DELEGATED, declared);
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
            if (!invisibleOverriddenDescriptors.get(declared).isEmpty()) {
                CallableDescriptor descriptor = invisibleOverriddenDescriptors.values().iterator().next();
                trace.report(CANNOT_OVERRIDE_INVISIBLE_MEMBER.on(member, declared, descriptor, descriptor.getContainingDeclaration()));
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

    private boolean checkPropertyKind(CallableMemberDescriptor descriptor, boolean isVar) {
        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            return propertyDescriptor.isVar() == isVar;
        }
        return false;
    }

    private void resolveUnknownVisibilityForMembers(@NotNull Set<CallableMemberDescriptor> fakeOverrides) {
        for (CallableMemberDescriptor override : fakeOverrides) {
            resolveUnknownVisibilityForMember(override);
        }
        for (CallableMemberDescriptor memberDescriptor : context.getMembers().values()) {
            resolveUnknownVisibilityForMember(memberDescriptor);
        }
        for (PropertyDescriptor propertyDescriptor : context.getProperties().values()) {
            for (PropertyAccessorDescriptor accessor : propertyDescriptor.getAccessors()) {
                if (accessor != null && accessor.getVisibility() == Visibilities.INHERITED) {
                    accessor.setVisibility(propertyDescriptor.getVisibility());
                }
            }
        }
    }

    private void resolveUnknownVisibilityForMember(@NotNull CallableMemberDescriptor memberDescriptor) {
        resolveUnknownVisibilityForOverriddenDescriptors(memberDescriptor.getOverriddenDescriptors());
        if (memberDescriptor.getVisibility() != Visibilities.INHERITED) {
            return;
        }

        Visibility visibility = findMaxVisibility(memberDescriptor.getOverriddenDescriptors());

        if (memberDescriptor instanceof PropertyDescriptor) {
            ((PropertyDescriptor)memberDescriptor).setVisibility(visibility);
        }
        else {
            assert memberDescriptor instanceof FunctionDescriptorImpl;
            ((FunctionDescriptorImpl)memberDescriptor).setVisibility(visibility);
        }
    }

    private void removeInvisibleOverriddenDescriptors(@NotNull Multimap<CallableDescriptor, CallableDescriptor> invisibleOverriddenDescriptors) {
        for (CallableMemberDescriptor memberDescriptor : context.getMembers().values()) {
            Set<? extends CallableDescriptor> overriddenDescriptors = memberDescriptor.getOverriddenDescriptors();
            for (Iterator<? extends CallableDescriptor> iterator = overriddenDescriptors.iterator(); iterator.hasNext(); ) {
                CallableDescriptor superDescriptor = iterator.next();
                if (!Visibilities.isVisible(superDescriptor, memberDescriptor)) {
                    invisibleOverriddenDescriptors.put(memberDescriptor, superDescriptor);
                    iterator.remove();
                }
            }
        }
    }

    private void resolveUnknownVisibilityForOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> descriptors) {
        for (CallableMemberDescriptor descriptor : descriptors) {
            if (descriptor.getVisibility() == Visibilities.INHERITED) {
                resolveUnknownVisibilityForMember(descriptor);
            }
        }
    }

    @NotNull
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
            Integer compare = Visibilities.compare(visibility, maxVisibility);
            if (compare == null) {
                maxVisibility = Visibilities.PUBLIC; //todo error or warning when inference only from incomparable visibilities
                continue;
            }
            if (compare > 0) {
                maxVisibility = visibility;
            }
        }
        assert maxVisibility != null;
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
