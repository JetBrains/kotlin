package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;

/**
 * @author abreslav
 */
public class OverrideResolver {

    private final TopDownAnalysisContext context;

    public OverrideResolver(@NotNull TopDownAnalysisContext context) {
        this.context = context;
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

        List<NamedFunctionDescriptor> functionsFromSupertypes = getDescriptorsFromSupertypes(classDescriptor, NamedFunctionDescriptor.class);
        List<PropertyDescriptor> propertiesFromSupertypes = getDescriptorsFromSupertypes(classDescriptor, PropertyDescriptor.class);

        MultiMap<String, NamedFunctionDescriptor> functionsFromSupertypesByName = groupDescriptorsByName(functionsFromSupertypes);
        MultiMap<String, PropertyDescriptor> propertiesFromSupertypesByName = groupDescriptorsByName(propertiesFromSupertypes);

        MultiMap<String, NamedFunctionDescriptor> functionsFromCurrentByName = groupDescriptorsByName(classDescriptor.getFunctions());
        MultiMap<String, PropertyDescriptor> propertiesFromCurrentByName = groupDescriptorsByName(classDescriptor.getProperties());
        
        Set<String> functionNames = new HashSet<String>();
        functionNames.addAll(functionsFromSupertypesByName.keySet());
        functionNames.addAll(functionsFromCurrentByName.keySet());
        
        Set<String> propertyNames = new HashSet<String>();
        propertyNames.addAll(propertiesFromSupertypesByName.keySet());
        propertyNames.addAll(propertiesFromCurrentByName.keySet());

        
        for (String functionName : functionNames) {
            generateOverridesInFunctionGroup(functionName,
                    functionsFromSupertypesByName.get(functionName),
                    functionsFromCurrentByName.get(functionName),
                    classDescriptor,
                    new DescriptorSink<NamedFunctionDescriptor>() {
                        @Override
                        public void addToScope(NamedFunctionDescriptor fakeOverride) {
                            classDescriptor.getScopeForMemberLookupAsWritableScope().addFunctionDescriptor(fakeOverride);
                        }
                    });
        }
        
        for (String propertyName : propertyNames) {
            generateOverridesInPropertyGroup(propertyName,
                    propertiesFromSupertypesByName.get(propertyName),
                    propertiesFromCurrentByName.get(propertyName),
                    classDescriptor,
                    new DescriptorSink<PropertyDescriptor>() {
                        @Override
                        public void addToScope(PropertyDescriptor fakeOverride) {
                            classDescriptor.getScopeForMemberLookupAsWritableScope().addPropertyDescriptor(fakeOverride);
                        }
                    });
        }
    }
    
    public interface DescriptorSink<D extends CallableMemberDescriptor> {
        void addToScope(D fakeOverride);
    }
    
    public static void generateOverridesInFunctionGroup(
            @NotNull String name,
            @NotNull Collection<NamedFunctionDescriptor> functionsFromSupertypes,
            @NotNull Collection<NamedFunctionDescriptor> functionsFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink<NamedFunctionDescriptor> sink) {
        
        List<NamedFunctionDescriptor> fakeOverrides = Lists.newArrayList();

        for (NamedFunctionDescriptor functionFromSupertype : functionsFromSupertypes) {

            boolean overrides = false;
            
            for (NamedFunctionDescriptor functionFromCurrent : functionsFromCurrent) {
                if (OverridingUtil.isOverridableBy(functionFromSupertype, functionFromCurrent).isSuccess()) {
                    ((FunctionDescriptorImpl) functionFromCurrent).addOverriddenFunction(functionFromSupertype);
                    overrides = true;
                }
            }
            
            for (NamedFunctionDescriptor fakeOverride : fakeOverrides) {
                if (OverridingUtil.isOverridableBy(functionFromSupertype, fakeOverride).isSuccess()) {
                    ((FunctionDescriptorImpl) fakeOverride).addOverriddenFunction(functionFromSupertype);
                    overrides = true;
                }
            }

            if (!overrides) {
                NamedFunctionDescriptor fakeOverride = functionFromSupertype.copy(current, false, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
                ((FunctionDescriptorImpl) fakeOverride).addOverriddenFunction(functionFromSupertype);
                
                fakeOverrides.add(fakeOverride);

                sink.addToScope(fakeOverride);
            }
        }
    }
    
    public static void generateOverridesInPropertyGroup(
            @NotNull String name,
            @NotNull Collection<PropertyDescriptor> propertiesFromSupertypes,
            @NotNull Collection<PropertyDescriptor> propertiesFromCurrent,
            @NotNull ClassDescriptor current,
            @NotNull DescriptorSink<PropertyDescriptor> sink) {

        List<PropertyDescriptor> fakeOverrides = Lists.newArrayList();
        
        for (PropertyDescriptor propertyFromSupertype : propertiesFromSupertypes) {
            boolean overrides = false;
            for (PropertyDescriptor propertyFromCurrent : propertiesFromCurrent) {
                if (OverridingUtil.isOverridableBy(propertyFromSupertype, propertyFromCurrent).isSuccess()) {
                    propertyFromCurrent.addOverriddenDescriptor(propertyFromSupertype);
                    overrides = true;
                }
            }
            
            for (PropertyDescriptor fakeOverride : fakeOverrides) {
                if (OverridingUtil.isOverridableBy(propertyFromSupertype, fakeOverride).isSuccess()) {
                    ((PropertyDescriptor) fakeOverride).addOverriddenDescriptor(propertyFromSupertype);
                    overrides = true;
                }
            }
            
            if (!overrides) {
                PropertyDescriptor fakeOverride = propertyFromSupertype.copy(current, false, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false);
                fakeOverride.addOverriddenDescriptor(propertyFromSupertype);

                fakeOverrides.add(fakeOverride);

                sink.addToScope(fakeOverride);
            }
        }
    }


    private static <T extends DeclarationDescriptor> MultiMap<String, T> groupDescriptorsByName(Collection<T> properties) {
        MultiMap<String, T> r = new MultiMap<String, T>();
        for (T property : properties) {
            r.putValue(property.getName(), property);
        }
        return r;
    }


    private static <T extends DeclarationDescriptor> List<T> getDescriptorsFromSupertypes(
            ClassDescriptor classDescriptor, Class<T> descriptorClass) {
        Set<T> r = Sets.newHashSet();
        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            r.addAll(getDescriptorsOfType(supertype.getMemberScope(), descriptorClass));
        }
        return new ArrayList<T>(r);
    }

    private static <T extends DeclarationDescriptor> List<T> getDescriptorsOfType(
            JetScope scope, Class<T> descriptorClass) {
        List<T> r = Lists.newArrayList();
        for (DeclarationDescriptor decl : scope.getAllDescriptors()) {
            if (descriptorClass.isInstance(decl)) {
                r.add((T) decl);
            }
        }
        return r;
    }

    @NotNull
    private Collection<FunctionDescriptor> findFunctionsOverridableBy(@NotNull FunctionDescriptor declaredFunction, @NotNull JetType supertype) {
        List<FunctionDescriptor> result = Lists.newArrayList();
        List<FunctionDescriptor> result2 = Lists.newArrayList();
        Set<FunctionDescriptor> functionGroup = supertype.getMemberScope().getFunctions(declaredFunction.getName());
        for (FunctionDescriptor functionDescriptor : functionGroup) {
            if (OverridingUtil.isOverridableBy(functionDescriptor, declaredFunction).isSuccess()) {
                result.add(functionDescriptor);
            } else {
                result2.add(functionDescriptor);
            }
        }
        return result;
    }

    @NotNull
    private Collection<PropertyDescriptor> findPropertiesOverridableBy(@NotNull PropertyDescriptor declaredProperty, @NotNull JetType supertype) {
        List<PropertyDescriptor> result = Lists.newArrayList();
        Set<VariableDescriptor> properties = supertype.getMemberScope().getProperties(declaredProperty.getName());
        for (VariableDescriptor property : properties) {
            assert property instanceof PropertyDescriptor;
            if (OverridingUtil.isOverridableBy(property, declaredProperty).isSuccess()) {
                result.add((PropertyDescriptor) property);
            }
        }
        return result;
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
        if (context.analyzingBootstrapLibrary()) return;

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
            context.getTrace().report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(nameIdentifier, klass, memberDescriptor));
            break;
        }


        if (classDescriptor.getModality() == Modality.ABSTRACT) {
            return;
        }

        for (CallableMemberDescriptor memberDescriptor : abstractNoImpl) {
            context.getTrace().report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(nameIdentifier, klass, memberDescriptor));
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
////                        context.getTrace().getErrorHandler().genericError(nameIdentifier.getNode(), "Class '" + klass.getName() + "' must be declared abstract or implement abstract method '" +
////                                                                                                    functionDescriptor.getName() + "' declared in " + declarationDescriptor.getName());
//                        context.getTrace().report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(nameIdentifier, klass, functionDescriptor));
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
                manyImpl.addAll(overridenDeclarations);
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
                if (OverridingUtil.isOverridableBy(one, another).isSuccess()
                        || OverridingUtil.isOverridableBy(another, one).isSuccess()) {
                    factoredMembers.put(one, another);
                }
            }
        }
        return factoredMembers;
    }

    private void checkOverride(CallableMemberDescriptor declared) {
        JetNamedDeclaration member = (JetNamedDeclaration) context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, declared);
        if (member == null) {
            assert context.getTrace().get(DELEGATED, declared);
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
    //                    context.getTrace().getErrorHandler().genericError(overrideNode, "Method " + overridden.getName() + " in " + overridden.getContainingDeclaration().getName() + " is final and cannot be overridden");
                        context.getTrace().report(OVERRIDING_FINAL_MEMBER.on(overrideNode, overridden, overridden.getContainingDeclaration()));
                        finalOverriddenError = true;
                    }

                    if (!OverridingUtil.isReturnTypeOkForOverride(JetTypeChecker.INSTANCE, overridden, declared) && !typeMismatchError) {
                        context.getTrace().report(RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden));
                        typeMismatchError = true;
                    }

                    if (checkPropertyKind(overridden, true) && checkPropertyKind(declared, false) && !kindMismatchError) {
                        context.getTrace().report(VAR_OVERRIDDEN_BY_VAL.on(member, ((JetProperty) member).getValOrVarNode(), (PropertyDescriptor) declared, (PropertyDescriptor) overridden));
                        kindMismatchError = true;
                    }
                }
            }
        }
        if (hasOverrideModifier && declared.getOverriddenDescriptors().size() == 0) {
//            context.getTrace().getErrorHandler().genericError(overrideNode, "Method " + declared.getName() + " overrides nothing");
            context.getTrace().report(NOTHING_TO_OVERRIDE.on(modifierList, overrideNode, declared));
        }
        PsiElement nameIdentifier = member.getNameIdentifier();
        if (!hasOverrideModifier && declared.getOverriddenDescriptors().size() > 0 && nameIdentifier != null) {
            CallableMemberDescriptor overridden = declared.getOverriddenDescriptors().iterator().next();
//            context.getTrace().getErrorHandler().genericError(nameIdentifier.getNode(),
//                                                 "Method '" + declared.getName() + "' overrides method '" + overridden.getName() + "' in class " +
//                                                 overridden.getContainingDeclaration().getName() + " and needs 'override' modifier");
            context.getTrace().report(VIRTUAL_MEMBER_HIDDEN.on(member, nameIdentifier, declared, overridden, overridden.getContainingDeclaration()));
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
