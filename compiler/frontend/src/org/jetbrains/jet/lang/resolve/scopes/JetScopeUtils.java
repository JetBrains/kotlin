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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Nikolay Krasko
 */
public final class JetScopeUtils {
    private JetScopeUtils() {}

    /**
     * Get receivers in order of locality, so that the closest (the most local) receiver goes first
     * A wrapper for {@link JetScope#getImplicitReceiversHierarchy(List)}
     *
     * @param scope Scope for getting receivers hierarchy.
     * @return receivers hierarchy.
     */
    @NotNull
    public static Collection<ReceiverDescriptor> getImplicitReceiversHierarchy(@NotNull JetScope scope) {
        List<ReceiverDescriptor> descriptors = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(descriptors);
        return descriptors;
    }

    /**
     * Get all extension descriptors among visible descriptors for current scope.
     *
     * @param scope Scope for query extensions.
     * @return extension descriptors.
     */
    public static Collection<CallableDescriptor> getAllExtensions(@NotNull JetScope scope) {
        final Set<CallableDescriptor> result = Sets.newHashSet();

        for (DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
            if (descriptor instanceof CallableDescriptor) {
                CallableDescriptor callDescriptor = (CallableDescriptor) descriptor;
                if (callDescriptor.getReceiverParameter().exists()) {
                    result.add(callDescriptor);
                }
            }
        }

        return result;
    }
}
