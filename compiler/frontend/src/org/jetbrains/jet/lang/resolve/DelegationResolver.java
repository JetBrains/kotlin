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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetDelegatorByExpressionSpecifier;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author abreslav
 */
public class DelegationResolver {

    private DelegationResolver() {}

    private static final Predicate<CallableMemberDescriptor> DELEGATION_PREDICATE = new Predicate<CallableMemberDescriptor>() {
        private final Predicate<Visibility> visibilityPredicate = Predicates.in(
                /*TODO should be Visibilities.INTERNAL_VISIBLITIES?*/
                Arrays.asList(Visibilities.PRIVATE, Visibilities.LOCAL));

        @Override
        public boolean apply(@Nullable CallableMemberDescriptor descriptor) {
            return descriptor != null && !visibilityPredicate.apply(descriptor.getVisibility()) && descriptor.getModality() == Modality.ABSTRACT;
        }
    };

    public static void addDelegatedMembers(@NotNull BindingTrace trace, @NotNull JetClassOrObject jetClass, @NotNull MutableClassDescriptor classDescriptor) {
        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            if (delegationSpecifier instanceof JetDelegatorByExpressionSpecifier) {
                JetDelegatorByExpressionSpecifier specifier = (JetDelegatorByExpressionSpecifier) delegationSpecifier;
                JetType type = trace.get(BindingContext.TYPE, specifier.getTypeReference());
                if (type != null) {
                    @SuppressWarnings("unchecked")
                    Collection<CallableMemberDescriptor> callableDescriptors = (Collection) Collections2.filter(
                            type.getMemberScope().getAllDescriptors(),
                            Predicates.instanceOf(CallableMemberDescriptor.class));
                    callableDescriptors = Collections2.filter(callableDescriptors, DELEGATION_PREDICATE);

                    Collection<CallableMemberDescriptor> descriptors = generateDelegatedMembers(classDescriptor, callableDescriptors);
                    outer:
                    for (CallableMemberDescriptor descriptor : descriptors) {
                        for (CallableMemberDescriptor existingDescriptor : classDescriptor.getAllCallableMembers()) {
                            if (OverridingUtil.isOverridableBy(existingDescriptor, descriptor).getResult() == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                                continue outer;
                            }
                        }

                        if (descriptor instanceof PropertyDescriptor) {
                            classDescriptor.getBuilder().addPropertyDescriptor((PropertyDescriptor) descriptor);
                        }
                        else if (descriptor instanceof SimpleFunctionDescriptor) {
                            classDescriptor.getBuilder().addFunctionDescriptor((SimpleFunctionDescriptor) descriptor);
                        }
                    }
                }
            }
        }
    }

    public static <T extends CallableMemberDescriptor> Collection<T> generateDelegatedMembers(DeclarationDescriptor newOwner, Collection<T> delegatedDescriptors) {
        Collection<CallableMemberDescriptor> result = Lists.newArrayList();
        for (CallableMemberDescriptor memberDescriptor : delegatedDescriptors) {
            Modality modality = DescriptorUtils.convertModality(memberDescriptor.getModality(), true);
            CallableMemberDescriptor copy =
                    memberDescriptor.copy(newOwner, modality, false, CallableMemberDescriptor.Kind.DELEGATION, true);
            result.add(copy);
        }
        //noinspection unchecked
        return (Collection) result;
    }
}
