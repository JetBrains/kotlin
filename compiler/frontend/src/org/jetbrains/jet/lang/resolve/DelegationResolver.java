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
import java.util.HashSet;

import static org.jetbrains.jet.lang.diagnostics.Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*;

public class DelegationResolver {

    private DelegationResolver() {
    }

    @NotNull
    public static Collection<CallableMemberDescriptor> generateDelegatedMembers(
            @NotNull BindingTrace trace,
            @NotNull JetClassOrObject jetClass,
            @NotNull MutableClassDescriptor classDescriptor
    ) {
        Collection<CallableMemberDescriptor> delegatedMembers = new HashSet<CallableMemberDescriptor>();
        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            if (!(delegationSpecifier instanceof JetDelegatorByExpressionSpecifier)) {
                continue;
            }
            JetDelegatorByExpressionSpecifier specifier = (JetDelegatorByExpressionSpecifier) delegationSpecifier;
            JetType type = trace.get(BindingContext.TYPE, specifier.getTypeReference());
            if (type == null) {
                continue;
            }
            Collection<CallableMemberDescriptor> candidates =
                    generateDelegationCandidates(classDescriptor, type, extractCallableMembers(type));
            for (CallableMemberDescriptor candidate : candidates) {
                if (existingMemberOverridesDelegatedMember(classDescriptor, candidate)) {
                    continue;
                }
                //only leave the first delegated member
                if (checkClashWithOtherDelegatedMember(trace, jetClass, delegatedMembers, candidate)) {
                    continue;
                }

                delegatedMembers.add(candidate);
            }
        }
        return delegatedMembers;
    }

    private static boolean checkClashWithOtherDelegatedMember(
            @NotNull BindingTrace trace,
            @NotNull JetClassOrObject jetClass,
            @NotNull Collection<CallableMemberDescriptor> delegatedMembers,
            @NotNull CallableMemberDescriptor candidate
    ) {
        for (CallableMemberDescriptor alreadyDelegatedMember : delegatedMembers) {
            if (haveSameSignatures(alreadyDelegatedMember, candidate)) {
                //trying to delegate to many traits with the same methods
                trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(jetClass.getNameIdentifier(), jetClass, alreadyDelegatedMember));
                return true;
            }
        }
        return false;
    }

    private static boolean existingMemberOverridesDelegatedMember(
            MutableClassDescriptor classDescriptor,
            CallableMemberDescriptor candidate
    ) {
        for (CallableMemberDescriptor existingDescriptor : classDescriptor.getAllCallableMembers()) {
            if (haveSameSignatures(existingDescriptor, candidate)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static <T extends CallableMemberDescriptor> Collection<T> generateDelegationCandidates(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetType delegatedTraitType,
            @NotNull Collection<T> membersFromTrait
    ) {
        Collection<T> descriptorsToDelegate = filterMembersFromSuperClassOfDelegatedTrait(delegatedTraitType, membersFromTrait);
        Collection<T> result = Lists.newArrayList();
        for (T memberDescriptor : descriptorsToDelegate) {
            if (memberDescriptor.getModality().isOverridable()) {
                Modality modality = DescriptorUtils.convertModality(memberDescriptor.getModality(), true);
                @SuppressWarnings("unchecked")
                T copy = (T) memberDescriptor.copy(classDescriptor, modality, memberDescriptor.getVisibility(),
                                                   CallableMemberDescriptor.Kind.DELEGATION, false);
                result.add(copy);
            }
        }
        return result;
    }

    @NotNull
    private static <T extends CallableMemberDescriptor> Collection<T> filterMembersFromSuperClassOfDelegatedTrait(
            @NotNull JetType delegatedTraitType,
            @NotNull Collection<T> membersFromTrait
    ) {
        final Collection<CallableMemberDescriptor> membersToSkip = getMembersFromClassSupertypeOfTrait(delegatedTraitType);
        return Collections2.filter(
                membersFromTrait,
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
    }

    private static boolean haveSameSignatures(@NotNull CallableDescriptor memberOne, @NotNull CallableDescriptor memberTwo) {
        return OverridingUtil.isOverridableBy(memberOne, memberTwo).getResult() == OVERRIDABLE;
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> getMembersFromClassSupertypeOfTrait(@NotNull JetType delegateTraitType) {
        JetType classSupertype = null;
        for (JetType supertype : TypeUtils.getAllSupertypes(delegateTraitType)) {
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
            ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind != ClassKind.TRAIT;
        }
        return false;
    }

    public static void generateDelegatesInAClass(
            @NotNull MutableClassDescriptor classDescriptor,
            @NotNull BindingTrace trace,
            @NotNull JetClassOrObject jetClassOrObject
    ) {
        for (CallableMemberDescriptor descriptor : generateDelegatedMembers(trace, jetClassOrObject, classDescriptor)) {
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
