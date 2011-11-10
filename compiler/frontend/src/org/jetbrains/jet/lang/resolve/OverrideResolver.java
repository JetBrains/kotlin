package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.*;
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
            if (OverridingUtil.isOverridableBy(functionDescriptor, declaredFunction).isSuccess()) {
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
        if (OverridingUtil.isOverridableBy(property, declaredProperty).isSuccess()) {
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

        PsiElement nameIdentifier = klass;
        if (klass instanceof JetClass) {
            nameIdentifier = ((JetClass) klass).getNameIdentifier();
        }
        else if (klass instanceof JetObjectDeclaration) {
            nameIdentifier = ((JetObjectDeclaration) klass).getNameIdentifier();
        }

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
        // Everything from supertypes
        Map<CallableMemberDescriptor, CallableMemberDescriptor> implementedWithDelegationBy = Maps.newHashMap();
        Multimap<CallableMemberDescriptor, CallableMemberDescriptor> factoredMembers = collectSuperMethods(classDescriptor);

        for (CallableMemberDescriptor key : factoredMembers.keySet()) {
            Collection<CallableMemberDescriptor> mutuallyOverridable = factoredMembers.get(key);

            int implementationCount = 0;
            for (CallableMemberDescriptor member : mutuallyOverridable) {
                if (member.getModality() != Modality.ABSTRACT) {
                    implementationCount++;
                }
            }

            if (implementationCount == 0) {
                abstractNoImpl.addAll(mutuallyOverridable);
            }
            else if (implementationCount > 1) {
                manyImpl.addAll(mutuallyOverridable);
            }
        }

        // Members actually present (declared) in the class
        Set<CallableMemberDescriptor> actuallyOverridden = Sets.newHashSet(implementedWithDelegationBy.keySet());
        for (CallableMemberDescriptor member : classDescriptor.getCallableMembers()) {
            actuallyOverridden.addAll(member.getOverriddenDescriptors());
        }

        // Those to be overridden that are actually not
        abstractNoImpl.removeAll(actuallyOverridden);
        manyImpl.removeAll(actuallyOverridden);
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
