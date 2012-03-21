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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;

/**
 * @author abreslav
 */
public class DelegationResolver {
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private BindingTrace trace;


    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
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
                JetType type = trace.get(BindingContext.TYPE, specifier.getTypeReference());
                if (type != null) {
                    for (DeclarationDescriptor declarationDescriptor : type.getMemberScope().getAllDescriptors()) {
                        if (declarationDescriptor instanceof PropertyDescriptor) {
                            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) declarationDescriptor;
                            if (propertyDescriptor.getModality().isOverridable()) {
                                PropertyDescriptor copy = propertyDescriptor.copy(classDescriptor, true, CallableMemberDescriptor.Kind.DELEGATION, true);
                                classDescriptor.addPropertyDescriptor(copy);
                                trace.record(DELEGATED, copy);
                            }
                        }
                        else if (declarationDescriptor instanceof SimpleFunctionDescriptor) {
                            SimpleFunctionDescriptor functionDescriptor = (SimpleFunctionDescriptor) declarationDescriptor;
                            if (functionDescriptor.getModality().isOverridable()) {
                                SimpleFunctionDescriptor copy = functionDescriptor.copy(classDescriptor, true, CallableMemberDescriptor.Kind.DELEGATION, true);
                                classDescriptor.addFunctionDescriptor(copy);
                                trace.record(DELEGATED, copy);
                            }
                        }
                    }
                }
            }
        }
    }

}
