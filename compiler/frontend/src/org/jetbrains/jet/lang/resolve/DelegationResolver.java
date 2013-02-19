/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetDelegatorByExpressionSpecifier;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.diagnostics.Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED;

public class DelegationResolver {

    private DelegationResolver() {}

    public static void addDelegatedMembers(@NotNull BindingTrace trace, @NotNull JetClassOrObject jetClass, @NotNull MutableClassDescriptor classDescriptor) {
        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            if (delegationSpecifier instanceof JetDelegatorByExpressionSpecifier) {
                JetDelegatorByExpressionSpecifier specifier = (JetDelegatorByExpressionSpecifier) delegationSpecifier;
                JetType type = trace.get(BindingContext.TYPE, specifier.getTypeReference());
                if (type != null) {
                    final Collection<CallableMemberDescriptor> membersToSkip = getMembersFromClassSupertype(type);
                    Collection<CallableMemberDescriptor> descriptorsToDelegate = Collections2.filter(extractCallableMembers(type),
                         new Predicate<CallableMemberDescriptor>() {
                             @Override
                             public boolean apply(@Nullable CallableMemberDescriptor descriptor) {
                                 for (CallableMemberDescriptor memberToSkip : membersToSkip) {
                                    if (haveSameSignatures(memberToSkip, descriptor)) {
                                         return false;
                                    }
                                 }
                                 return true;
                             }
                         });

                    Collection<CallableMemberDescriptor> generatedDescriptors = generateDelegatedMembers(classDescriptor, descriptorsToDelegate);
                    outer:
                    for (CallableMemberDescriptor descriptor : generatedDescriptors) {
                        for (CallableMemberDescriptor existingDescriptor : classDescriptor.getAllCallableMembers()) {
                            if (OverridingUtil.isOverridableBy(existingDescriptor, descriptor).getResult() == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                                if (existingDescriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION) {
                                    //trying to delegate to many traits with the same methods
                                    trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(jetClass.getNameIdentifier(), jetClass, existingDescriptor));
                                }
                                continue outer;
                            }
                        }

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

    private static boolean haveSameSignatures(CallableDescriptor memberOne, CallableDescriptor memberTwo) {
        return OverridingUtil.isOverridableBy(memberOne, memberTwo).getResult() == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;
    }

    private static Collection<CallableMemberDescriptor> getMembersFromClassSupertype(JetType type) {
        JetType classSupertype = null;
        for (JetType supertype : TypeUtils.getAllSupertypes(type)) {
            if (isNotTrait(supertype.getConstructor().getDeclarationDescriptor())) {
                classSupertype = supertype;
                break;
            }
        }

        return classSupertype != null ? extractCallableMembers(classSupertype) : Collections.<CallableMemberDescriptor>emptyList();
    }

    @SuppressWarnings("unchecked")
    private static Collection<CallableMemberDescriptor> extractCallableMembers(JetType type) {
        return (Collection) Collections2.filter(type.getMemberScope().getAllDescriptors(),
                                                Predicates.instanceOf(CallableMemberDescriptor.class));
    }

    private static boolean isNotTrait(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            final ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind != ClassKind.TRAIT;
        }
        return false;
    }

    public static <T extends CallableMemberDescriptor> Collection<T> generateDelegatedMembers(DeclarationDescriptor newOwner, Collection<T> delegatedDescriptors) {
        Collection<CallableMemberDescriptor> result = Lists.newArrayList();
        for (CallableMemberDescriptor memberDescriptor : delegatedDescriptors) {
            if (memberDescriptor.getModality().isOverridable()) {
                Modality modality = DescriptorUtils.convertModality(memberDescriptor.getModality(), true);
                CallableMemberDescriptor copy =
                        memberDescriptor.copy(newOwner, modality, memberDescriptor.getVisibility(), CallableMemberDescriptor.Kind.DELEGATION, false);
                result.add(copy);
            }
        }
        //noinspection unchecked
        return (Collection) result;
    }
}
