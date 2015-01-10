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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetDelegationSpecifier;
import org.jetbrains.kotlin.psi.JetDelegatorByExpressionSpecifier;
import org.jetbrains.kotlin.psi.JetTypeReference;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.*;

import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION;
import static org.jetbrains.jet.lang.diagnostics.Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;

public final class DelegationResolver<T extends CallableMemberDescriptor> {

    public static void generateDelegatesInAClass(
            @NotNull MutableClassDescriptor classDescriptor,
            @NotNull final BindingTrace trace,
            @NotNull JetClassOrObject jetClassOrObject
    ) {
        TypeResolver eagerTypeResolver = new TypeResolver() {
            @Nullable
            @Override
            public JetType resolve(@NotNull JetTypeReference reference) {
                return trace.get(BindingContext.TYPE, reference);
            }
        };
        MemberExtractor<CallableMemberDescriptor> eagerExtractor = new MemberExtractor<CallableMemberDescriptor>() {
            @NotNull
            @Override
            public Collection<CallableMemberDescriptor> getMembersByType(@NotNull JetType type) {
                //noinspection unchecked
                return (Collection) Collections2.filter(type.getMemberScope().getAllDescriptors(),
                                                        Predicates.instanceOf(CallableMemberDescriptor.class));
            }
        };
        Set<CallableMemberDescriptor> existingMembers = classDescriptor.getAllCallableMembers();
        Collection<CallableMemberDescriptor> delegatedMembers =
                generateDelegatedMembers(jetClassOrObject, classDescriptor, existingMembers, trace, eagerExtractor, eagerTypeResolver);
        for (CallableMemberDescriptor descriptor : delegatedMembers) {
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


    @NotNull
    public static <T extends CallableMemberDescriptor> Collection<T> generateDelegatedMembers(
            @NotNull JetClassOrObject classOrObject,
            @NotNull ClassDescriptor ownerDescriptor,
            @NotNull Collection<? extends CallableDescriptor> existingMembers,
            @NotNull BindingTrace trace,
            @NotNull MemberExtractor<T> memberExtractor,
            @NotNull TypeResolver typeResolver
    ) {
        return new DelegationResolver<T>(classOrObject, ownerDescriptor, existingMembers, trace, memberExtractor, typeResolver)
                .generateDelegatedMembers();
    }

    @NotNull private final JetClassOrObject classOrObject;
    @NotNull private final ClassDescriptor ownerDescriptor;
    @NotNull private final Collection<? extends CallableDescriptor> existingMembers;
    @NotNull private final BindingTrace trace;
    @NotNull private final MemberExtractor<T> memberExtractor;
    @NotNull private final TypeResolver typeResolver;

    private DelegationResolver(
            @NotNull JetClassOrObject classOrObject,
            @NotNull ClassDescriptor ownerDescriptor,
            @NotNull Collection<? extends CallableDescriptor> existingMembers,
            @NotNull BindingTrace trace,
            @NotNull MemberExtractor<T> extractor,
            @NotNull TypeResolver resolver
    ) {

        this.classOrObject = classOrObject;
        this.ownerDescriptor = ownerDescriptor;
        this.existingMembers = existingMembers;
        this.trace = trace;
        this.memberExtractor = extractor;
        this.typeResolver = resolver;
    }

    @NotNull
    private Collection<T> generateDelegatedMembers() {
        Collection<T> delegatedMembers = new HashSet<T>();
        for (JetDelegationSpecifier delegationSpecifier : classOrObject.getDelegationSpecifiers()) {
            if (!(delegationSpecifier instanceof JetDelegatorByExpressionSpecifier)) {
                continue;
            }
            JetDelegatorByExpressionSpecifier specifier = (JetDelegatorByExpressionSpecifier) delegationSpecifier;
            JetTypeReference typeReference = specifier.getTypeReference();
            if (typeReference == null) {
                continue;
            }
            JetType delegatedTraitType = typeResolver.resolve(typeReference);
            if (delegatedTraitType == null || delegatedTraitType.isError()) {
                continue;
            }
            Collection<T> delegatesForTrait = generateDelegatesForTrait(delegatedMembers, delegatedTraitType);
            delegatedMembers.addAll(delegatesForTrait);
        }
        return delegatedMembers;
    }

    @NotNull
    private Collection<T> generateDelegatesForTrait(
            @NotNull Collection<T> existingDelegates,
            @NotNull JetType delegatedTraitType
    ) {
        Collection<T> result = new HashSet<T>();
        Collection<T> candidates = generateDelegationCandidates(delegatedTraitType);
        for (T candidate : candidates) {
            if (existingMemberOverridesDelegatedMember(candidate, existingMembers)) {
                continue;
            }
            //only leave the first delegated member
            if (checkClashWithOtherDelegatedMember(existingDelegates, candidate)) {
                continue;
            }

            result.add(candidate);
        }
        return result;
    }

    @NotNull
    private Collection<T> generateDelegationCandidates(@NotNull JetType delegatedTraitType) {
        Collection<T> descriptorsToDelegate = overridableMembersNotFromSuperClassOfTrait(delegatedTraitType);
        Collection<T> result = new ArrayList<T>(descriptorsToDelegate.size());
        for (T memberDescriptor : descriptorsToDelegate) {
            Modality newModality = memberDescriptor.getModality() == Modality.ABSTRACT ? Modality.OPEN : memberDescriptor.getModality();
            @SuppressWarnings("unchecked")
            T copy = (T) memberDescriptor.copy(ownerDescriptor, newModality, Visibilities.INHERITED, DELEGATION, false);
            result.add(copy);
        }
        return result;
    }

    private static boolean existingMemberOverridesDelegatedMember(
            @NotNull CallableMemberDescriptor candidate,
            @NotNull Collection<? extends CallableDescriptor> existingMembers
    ) {
        for (CallableDescriptor existingDescriptor : existingMembers) {
            if (haveSameSignatures(existingDescriptor, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkClashWithOtherDelegatedMember(@NotNull Collection<T> delegatedMembers, @NotNull T candidate) {
        for (CallableMemberDescriptor alreadyDelegatedMember : delegatedMembers) {
            if (haveSameSignatures(alreadyDelegatedMember, candidate)) {
                //trying to delegate to many traits with the same methods
                trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(classOrObject, classOrObject, alreadyDelegatedMember));
                return true;
            }
        }
        return false;
    }

    @NotNull
    private Collection<T> overridableMembersNotFromSuperClassOfTrait(@NotNull JetType trait) {
        final Collection<T> membersToSkip = getMembersFromClassSupertypeOfTrait(trait);
        return Collections2.filter(
                memberExtractor.getMembersByType(trait),
                new Predicate<CallableMemberDescriptor>() {
                    @Override
                    public boolean apply(CallableMemberDescriptor descriptor) {
                        if (!descriptor.getModality().isOverridable()) {
                            return false;
                        }
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
        //isOverridableBy ignores return types
        return OverridingUtil.DEFAULT.isOverridableBy(memberOne, memberTwo).getResult() == OVERRIDABLE;
    }

    @NotNull
    private Collection<T> getMembersFromClassSupertypeOfTrait(@NotNull JetType traitType) {
        JetType classSupertype = null;
        for (JetType supertype : TypeUtils.getAllSupertypes(traitType)) {
            if (isNotTrait(supertype.getConstructor().getDeclarationDescriptor())) {
                classSupertype = supertype;
                break;
            }
        }
        return classSupertype != null ? memberExtractor.getMembersByType(classSupertype) : Collections.<T>emptyList();
    }

    private static boolean isNotTrait(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind != ClassKind.TRAIT;
        }
        return false;
    }

    public interface MemberExtractor<T extends CallableMemberDescriptor> {
        @NotNull
        Collection<T> getMembersByType(@NotNull JetType type);
    }

    public interface TypeResolver {
        @Nullable
        JetType resolve(@NotNull JetTypeReference reference);
    }
}
