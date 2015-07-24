/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core.codeInsight;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.resolve.OverridingUtil;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilPackage;
import org.jetbrains.kotlin.types.JetType;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;

public class OverrideMethodsHandler extends OverrideImplementMethodsHandler {
    @Override
    protected Set<CallableMemberDescriptor> collectMethodsToGenerate(@NotNull ClassDescriptor descriptor) {
        Set<CallableMemberDescriptor> superMethods = collectSuperMethods(descriptor);
        for (DeclarationDescriptor member : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (member instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callable = (CallableMemberDescriptor) member;
                if (callable.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
                    superMethods.removeAll(callable.getOverriddenDescriptors());
                }
            }
        }
        Set<CallableMemberDescriptor> result = new HashSet<CallableMemberDescriptor>();
        for (CallableMemberDescriptor superMethod : superMethods) {
            if (superMethod.getModality().isOverridable()) {
                if (!CallResolverUtilPackage.isOrOverridesSynthesized(superMethod)) {
                    result.add(superMethod);
                }
            }
        }
        return result;
    }

    @NotNull
    private static Set<CallableMemberDescriptor> collectSuperMethods(@NotNull ClassDescriptor classDescriptor) {
        Set<CallableMemberDescriptor> inheritedFunctions = new LinkedHashSet<CallableMemberDescriptor>();
        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                if (descriptor instanceof CallableMemberDescriptor) {
                    inheritedFunctions.add((CallableMemberDescriptor) descriptor);
                }
            }
        }

        // Only those actually inherited
        Set<CallableMemberDescriptor> filteredMembers = OverrideResolver.filterOutOverridden(inheritedFunctions);

        // Group members with "the same" signature
        Multimap<CallableMemberDescriptor, CallableMemberDescriptor> factoredMembers = LinkedHashMultimap.create();
        for (CallableMemberDescriptor one : filteredMembers) {
            if (factoredMembers.values().contains(one)) continue;
            for (CallableMemberDescriptor another : filteredMembers) {
//                if (one == another) continue;
                factoredMembers.put(one, one);
                if (OverridingUtil.DEFAULT.isOverridableBy(one, another).getResult() == OVERRIDABLE
                    || OverridingUtil.DEFAULT.isOverridableBy(another, one).getResult() == OVERRIDABLE) {
                    factoredMembers.put(one, another);
                }
            }
        }

        return factoredMembers.keySet();
    }

    @Override
    protected String getChooserTitle() {
        return "Override Members";
    }

    @Override
    protected String getNoMethodsFoundHint() {
        return "No methods to override have been found";
    }
}
