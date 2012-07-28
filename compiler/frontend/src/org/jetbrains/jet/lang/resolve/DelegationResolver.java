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

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

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
                    @SuppressWarnings("unchecked")
                    Collection<CallableMemberDescriptor> callableDescriptors = (Collection) Collections2.filter(
                            type.getMemberScope().getAllDescriptors(),
                            Predicates.instanceOf(CallableMemberDescriptor.class));
                    Collection<CallableMemberDescriptor> descriptors = generateDelegatedMembers(classDescriptor, callableDescriptors);
                    for (CallableMemberDescriptor descriptor : descriptors) {
                        if (descriptor instanceof PropertyDescriptor) {
                            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
                            classDescriptor.getBuilder().addPropertyDescriptor(propertyDescriptor);
                        }
                        else if (descriptor instanceof SimpleFunctionDescriptor) {
                            SimpleFunctionDescriptor functionDescriptor = (SimpleFunctionDescriptor) descriptor;
                            classDescriptor.getBuilder().addFunctionDescriptor(functionDescriptor);
                        }
                    }
                }
            }
        }
    }

    public static <T extends CallableMemberDescriptor> Collection<T> generateDelegatedMembers(DeclarationDescriptor newOwner, Collection<T> delegatedDescriptors) {
        Collection<CallableMemberDescriptor> result = Lists.newArrayList();
        for (CallableMemberDescriptor memberDescriptor : delegatedDescriptors) {
            if (memberDescriptor.getModality().isOverridable()) {
                Modality modality = DescriptorUtils.convertModality(memberDescriptor.getModality(), true);
                CallableMemberDescriptor copy =
                        memberDescriptor.copy(newOwner, modality, false, CallableMemberDescriptor.Kind.DELEGATION, true);
                result.add(copy);
            }
        }
        //noinspection unchecked
        return (Collection) result;
    }
}
