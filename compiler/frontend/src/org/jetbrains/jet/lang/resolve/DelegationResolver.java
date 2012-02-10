package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;

/**
 * @author abreslav
 */
public class DelegationResolver {
    private final TopDownAnalysisContext context;

    public DelegationResolver(TopDownAnalysisContext context) {
        this.context = context;
    }

    public void process() {
        addDelegatedMembers();
    }

    private void addDelegatedMembers() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            addDelegatedMembers(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            addDelegatedMembers(entry.getKey(), entry.getValue());
        }
    }

    private void addDelegatedMembers(JetClassOrObject jetClass, MutableClassDescriptor classDescriptor) {
        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            if (delegationSpecifier instanceof JetDelegatorByExpressionSpecifier) {
                JetDelegatorByExpressionSpecifier specifier = (JetDelegatorByExpressionSpecifier) delegationSpecifier;
                JetType type = context.getTrace().get(BindingContext.TYPE, specifier.getTypeReference());
                if (type != null) {
                    for (DeclarationDescriptor declarationDescriptor : type.getMemberScope().getAllDescriptors()) {
                        if (declarationDescriptor instanceof PropertyDescriptor) {
                            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) declarationDescriptor;
                            if (propertyDescriptor.getModality().isOverridable()) {
                                PropertyDescriptor copy = propertyDescriptor.copy(classDescriptor, true, CallableMemberDescriptor.Kind.DELEGATION, true);
                                classDescriptor.addPropertyDescriptor(copy);
                                context.getTrace().record(DELEGATED, copy);
                            }
                        }
                        else if (declarationDescriptor instanceof NamedFunctionDescriptor) {
                            NamedFunctionDescriptor functionDescriptor = (NamedFunctionDescriptor) declarationDescriptor;
                            if (functionDescriptor.getModality().isOverridable()) {
                                NamedFunctionDescriptor copy = functionDescriptor.copy(classDescriptor, true, CallableMemberDescriptor.Kind.DELEGATION, true);
                                classDescriptor.addFunctionDescriptor(copy);
                                context.getTrace().record(DELEGATED, copy);
                            }
                        }
                    }
                }
            }
        }
    }

}
