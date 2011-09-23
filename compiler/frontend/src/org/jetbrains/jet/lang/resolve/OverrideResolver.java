package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
 * @author abreslav
 */
public class OverrideResolver {

    private final TopDownAnalysisContext context;

    public OverrideResolver(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    public void process() {
        bindOverrides();
        checkOverrides();
    }

    private void bindOverrides() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            bindOverridesInAClass(entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            bindOverridesInAClass(entry.getValue());
        }
    }

    private void bindOverridesInAClass(MutableClassDescriptor classDescriptor) {
        for (FunctionDescriptor declaredFunction : classDescriptor.getFunctions()) {
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                FunctionDescriptor overridden = findFunctionOverridableBy(declaredFunction, supertype);
                if (overridden != null) {
                    ((FunctionDescriptorImpl) declaredFunction).addOverriddenFunction(overridden);
                }
            }
        }

        for (PropertyDescriptor propertyDescriptor : classDescriptor.getProperties()) {
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                PropertyDescriptor overridden = findPropertyOverridableBy(propertyDescriptor, supertype);
                if (overridden != null) {
                    propertyDescriptor.addOverriddenDescriptor(overridden);
                }
            }
        }
    }

    @Nullable
    private FunctionDescriptor findFunctionOverridableBy(@NotNull FunctionDescriptor declaredFunction, @NotNull JetType supertype) {
        Set<FunctionDescriptor> functionGroup = supertype.getMemberScope().getFunctions(declaredFunction.getName());
        for (FunctionDescriptor functionDescriptor : functionGroup) {
            if (OverridingUtil.isOverridableBy(context.getSemanticServices().getTypeChecker(), functionDescriptor, declaredFunction).isSuccess()) {
                return functionDescriptor;
            }
        }
        return null;
    }

    @Nullable
    private PropertyDescriptor findPropertyOverridableBy(@NotNull PropertyDescriptor declaredProperty, @NotNull JetType supertype) {
        PropertyDescriptor property = (PropertyDescriptor) supertype.getMemberScope().getVariable(declaredProperty.getName());
        if (property == null) {
            return null;
        }
        if (OverridingUtil.isOverridableBy(context.getSemanticServices().getTypeChecker(), property, declaredProperty).isSuccess()) {
            return property;
        }
        return null;
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
        Set<CallableMemberDescriptor> inheritedFunctions = Sets.newLinkedHashSet();
        for (JetType supertype : classDescriptor.getSupertypes()) {
            for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                if (descriptor instanceof CallableMemberDescriptor) {
                    CallableMemberDescriptor memberDescriptor = (CallableMemberDescriptor) descriptor;
                    inheritedFunctions.add(memberDescriptor);
                }
            }
        }

        Set<CallableMemberDescriptor> filteredMembers = OverridingUtil.filterOverrides(inheritedFunctions);

        Multimap<CallableMemberDescriptor, CallableMemberDescriptor> factoredMembers = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        JetTypeChecker typeChecker = context.getSemanticServices().getTypeChecker();
        for (CallableMemberDescriptor one : filteredMembers) {
            if (factoredMembers.values().contains(one)) continue;
            for (CallableMemberDescriptor another : filteredMembers) {
                if (one == another) continue;
                factoredMembers.put(one, one);
                if (OverridingUtil.isOverridableBy(typeChecker, one, another).isSuccess()
                        || OverridingUtil.isOverridableBy(typeChecker, another, one).isSuccess()) {
                    factoredMembers.put(one, another);
                }
            }
        }

        Set<CallableMemberDescriptor> mustBeOverridden = Sets.newLinkedHashSet();

        for (CallableMemberDescriptor key : factoredMembers.keySet()) {
            Collection<CallableMemberDescriptor> mutuallyOverridable = factoredMembers.get(key);

//            System.out.println(key);
//            println(mutuallyOverridable);

            int implementationCount = 0;
            for (CallableMemberDescriptor member : mutuallyOverridable) {
                if (member.getModality() != Modality.ABSTRACT) {
                    implementationCount++;
                }
            }
            
            if (implementationCount != 1) {
                mustBeOverridden.addAll(mutuallyOverridable);
            }
        }
        
        Set<CallableDescriptor> actuallyOverridden = Sets.newHashSet();
        for (FunctionDescriptor declaredFunction : classDescriptor.getFunctions()) {
            actuallyOverridden.addAll(declaredFunction.getOverriddenDescriptors());
        }

        for (PropertyDescriptor declaredProperty : classDescriptor.getProperties()) {
            actuallyOverridden.addAll(declaredProperty.getOverriddenDescriptors());
        }

        mustBeOverridden.removeAll(actuallyOverridden);

        System.out.println(classDescriptor);
        println(mustBeOverridden);
        System.out.println("Actually overridden:");
        println(actuallyOverridden);

        for (FunctionDescriptor declaredFunction : classDescriptor.getFunctions()) {
            checkOverrideForFunction(declaredFunction);
        }
        if (classDescriptor.getModality() == Modality.ABSTRACT) {
            return;
        }
        Set<FunctionDescriptor> allOverriddenFunctions = Sets.newHashSet();
        Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors();
        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (descriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                if (functionDescriptor.getModality() != Modality.ABSTRACT) {
                    for (FunctionDescriptor overriddenDescriptor : functionDescriptor.getOverriddenDescriptors()) {
                        allOverriddenFunctions.add(overriddenDescriptor.getOriginal());
                    }
                }
            }
        }
        boolean foundError = false;
        PsiElement nameIdentifier = null;
        if (klass instanceof JetClass) {
            nameIdentifier = ((JetClass) klass).getNameIdentifier();
        }
        else if (klass instanceof JetObjectDeclaration) {
            nameIdentifier = ((JetObjectDeclaration) klass).getNameIdentifier();
        }
        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (descriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                if (functionDescriptor.getModality() == Modality.ABSTRACT && !allOverriddenFunctions.contains(functionDescriptor.getOriginal()) && !foundError && nameIdentifier != null) {
                    DeclarationDescriptor declarationDescriptor = functionDescriptor.getContainingDeclaration();
                    if (declarationDescriptor != classDescriptor) {
//                        context.getTrace().getErrorHandler().genericError(nameIdentifier.getNode(), "Class '" + klass.getName() + "' must be declared abstract or implement abstract method '" +
//                                                                                                    functionDescriptor.getName() + "' declared in " + declarationDescriptor.getName());
                        context.getTrace().report(ABSTRACT_METHOD_NOT_IMPLEMENTED.on(nameIdentifier, klass, functionDescriptor, declarationDescriptor));
                        foundError = true;
                    }
                }
            }
        }
    }

    private void println(Collection<? extends CallableDescriptor> inheritedProperties) {
        for (CallableDescriptor inheritedProperty : inheritedProperties) {
            System.out.println("  " + inheritedProperty);
        }
    }


    private void checkOverrideForFunction(FunctionDescriptor declaredFunction) {
        JetFunction function = (JetFunction) context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, declaredFunction);
        assert function != null;
        JetModifierList modifierList = function.getModifierList();
        ASTNode overrideNode = modifierList != null ? modifierList.getModifierNode(JetTokens.OVERRIDE_KEYWORD) : null;
        boolean hasOverrideModifier = overrideNode != null;
        boolean foundError = false;

        for (FunctionDescriptor overridden : declaredFunction.getOverriddenDescriptors()) {
            if (overridden != null) {
                if (hasOverrideModifier && !overridden.getModality().isOpen() && !foundError) {
//                    context.getTrace().getErrorHandler().genericError(overrideNode, "Method " + overridden.getName() + " in " + overridden.getContainingDeclaration().getName() + " is final and cannot be overridden");
                    context.getTrace().report(OVERRIDING_FINAL_FUNCTION.on(overrideNode, overridden, overridden.getContainingDeclaration()));
                    foundError = true;
                }
            }
        }
        if (hasOverrideModifier && declaredFunction.getOverriddenDescriptors().size() == 0) {
//            context.getTrace().getErrorHandler().genericError(overrideNode, "Method " + declaredFunction.getName() + " overrides nothing");
            context.getTrace().report(NOTHING_TO_OVERRIDE.on(function, overrideNode, declaredFunction));
        }
        PsiElement nameIdentifier = function.getNameIdentifier();
        if (!hasOverrideModifier && declaredFunction.getOverriddenDescriptors().size() > 0 && nameIdentifier != null) {
            FunctionDescriptor overriddenFunction = declaredFunction.getOverriddenDescriptors().iterator().next();
//            context.getTrace().getErrorHandler().genericError(nameIdentifier.getNode(),
//                                                 "Method '" + declaredFunction.getName() + "' overrides method '" + overriddenFunction.getName() + "' in class " +
//                                                 overriddenFunction.getContainingDeclaration().getName() + " and needs 'override' modifier");
            context.getTrace().report(VIRTUAL_METHOD_HIDDEN.on(function, nameIdentifier, declaredFunction, overriddenFunction, overriddenFunction.getContainingDeclaration()));
        }
    }
}
