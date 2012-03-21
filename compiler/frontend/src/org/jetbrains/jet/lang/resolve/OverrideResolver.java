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
import javax.inject.Inject;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.CommonSuppliers;

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
        generateOverrides();
        checkOverrides();
    }

    /**
     * Generate fake overrides and add overriden descriptors to existing descriptors.
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

    private void generateOverridesInAClass(final MutableClassDescriptor classDescriptor, Set<ClassifierDescriptor> processed, Set<MutableClassDescriptor> ourClasses) {
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

        List<CallableMemberDescriptor> functionsFromSupertypes = getDescriptorsFromSupertypes(classDescriptor);

        MultiMap<String, CallableMemberDescriptor> functionsFromSupertypesByName = groupDescriptorsByName(functionsFromSupertypes);

        MultiMap<String, CallableMemberDescriptor> functionsFromCurrentByName = groupDescriptorsByName(classDescriptor.getCallableMembers());

        Set<String> functionNames = new LinkedHashSet<String>();
        functionNames.addAll(functionsFromSupertypesByName.keySet());
        functionNames.addAll(functionsFromCurrentByName.keySet());
        
        for (String functionName : functionNames) {
            generateOverridesInFunctionGroup(functionName,
                    functionsFromSupertypesByName.get(functionName),
                    functionsFromCurrentByName.get(functionName),
                    classDescriptor,
                    new DescriptorSink() {
                        @Override
                        public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                            if (fakeOverride instanceof PropertyDescriptor) {
                                classDescriptor.getScopeForMemberLookupAsWritableScope().addPropertyDescriptor((PropertyDescriptor) fakeOverride);
                            } else if (fakeOverride instanceof SimpleFunctionDescriptor) {
                                classDescriptor.getScopeForMemberLookupAsWritableScope().addFunctionDescriptor((SimpleFunctionDescriptor) fakeOverride);
                            } else {
                                throw new IllegalStateException(fakeOverride.getClass().getName());
                            }
                        }

                        @Override
                        public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                            JetDeclaration jetProperty = (JetDeclaration) trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, fromCurrent);
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
            @NotNull Collection<? extends CallableMemberDescriptor> functionsFromSupertypes,
            @NotNull Collection<? extends CallableMemberDescriptor> functionsFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink sink) {
        
        List<CallableMemberDescriptor> fakeOverrides = Lists.newArrayList();

        for (CallableMemberDescriptor functionFromSupertype : functionsFromSupertypes) {

            boolean overrides = false;
            
            for (CallableMemberDescriptor functionFromCurrent : functionsFromCurrent) {
                OverridingUtil.OverrideCompatibilityInfo.ErrorKind overridable = OverridingUtil.isOverridableBy(functionFromSupertype, functionFromCurrent).isOverridable();
                if (overridable == OverridingUtil.OverrideCompatibilityInfo.ErrorKind.OVERRIDABLE) {
                    functionFromCurrent.addOverriddenDescriptor(functionFromSupertype);
                    overrides = true;
                } else if (overridable == OverridingUtil.OverrideCompatibilityInfo.ErrorKind.CONFLICT) {
                    sink.conflict(functionFromSupertype, functionFromCurrent);
                }
            }
            
            for (CallableMemberDescriptor fakeOverride : fakeOverrides) {
                if (OverridingUtil.isOverridableBy(functionFromSupertype, fakeOverride).isOverridable() == OverridingUtil.OverrideCompatibilityInfo.ErrorKind.OVERRIDABLE) {
                    fakeOverride.addOverriddenDescriptor(functionFromSupertype);
                    overrides = true;
                }
            }

            if (!overrides) {
                CallableMemberDescriptor fakeOverride = functionFromSupertype.copy(current, false, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
                fakeOverride.addOverriddenDescriptor(functionFromSupertype);
                
                fakeOverrides.add(fakeOverride);

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

    private void checkOverrides() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            checkOverridesInAClass(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            checkOverridesInAClass(entry.getValue(), entry.getKey());
        }
    }

    protected void checkOverridesInAClass(MutableClassDescriptor classDescriptor, JetClassOrObject klass) {
        if (topDownAnalysisParameters.isAnalyzingBootstrapLibrary()) return;

        // Check overrides for internal consistency
        for (CallableMemberDescriptor member : classDescriptor.getCallableMembers()) {
            checkOverride(member);
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

//        Set<FunctionDescriptor> allOverriddenFunctions = Sets.newHashSet();
//        Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors();
//        for (DeclarationDescriptor descriptor : allDescriptors) {
//            if (descriptor instanceof FunctionDescriptor) {
//                FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
//                if (functionDescriptor.getModality() != Modality.ABSTRACT) {
//                    for (FunctionDescriptor overriddenDescriptor : functionDescriptor.getOverriddenDescriptors()) {
//                        allOverriddenFunctions.add(overriddenDescriptor.getOriginal());
//                    }
//                }
//            }
//        }
//        boolean foundError = false;
//        for (DeclarationDescriptor descriptor : allDescriptors) {
//            if (descriptor instanceof FunctionDescriptor) {
//                FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
//                if (functionDescriptor.getModality() == Modality.ABSTRACT && !allOverriddenFunctions.contains(functionDescriptor.getOriginal()) && !foundError && nameIdentifier != null) {
//                    DeclarationDescriptor declarationDescriptor = functionDescriptor.getContainingDeclaration();
//                    if (declarationDescriptor != classDescriptor) {
////                        trace.getErrorHandler().genericError(nameIdentifier.getNode(), "Class '" + klass.getName() + "' must be declared abstract or implement abstract method '" +
////                                                                                                    functionDescriptor.getName() + "' declared in " + declarationDescriptor.getName());
//                        trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(nameIdentifier, klass, functionDescriptor));
//                        foundError = true;
//                    }
//                }
//            }
//        }
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
        } else {
            Collection<CallableMemberDescriptor> overridenDeclarations = OverridingUtil.getOverridenDeclarations(descriptor);
            if (overridenDeclarations.size() == 0) {
                throw new IllegalStateException();
            } else if (overridenDeclarations.size() == 1) {
                CallableMemberDescriptor single = overridenDeclarations.iterator().next();
                if (single.getModality() == Modality.ABSTRACT) {
                    abstractNoImpl.add(single);
                }
            } else {
                List<CallableMemberDescriptor> nonAbstractManyImpl = Lists.newArrayList();
                for (CallableMemberDescriptor overriden : overridenDeclarations) {
                    if (overriden.getModality() != Modality.ABSTRACT) {
                        nonAbstractManyImpl.add(overriden);
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
                if (OverridingUtil.isOverridableBy(one, another).isOverridable() == OverridingUtil.OverrideCompatibilityInfo.ErrorKind.OVERRIDABLE
                        || OverridingUtil.isOverridableBy(another, one).isOverridable() == OverridingUtil.OverrideCompatibilityInfo.ErrorKind.OVERRIDABLE) {
                    factoredMembers.put(one, another);
                }
            }
        }
        return factoredMembers;
    }

    private void checkOverride(CallableMemberDescriptor declared) {
        JetNamedDeclaration member = (JetNamedDeclaration) trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declared);
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
            trace.report(NOTHING_TO_OVERRIDE.on(member, declared));
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
}
