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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JetScopeUtils {
    private JetScopeUtils() {}

    public static List<ReceiverValue> getImplicitReceiversHierarchyValues(@NotNull JetScope scope) {
        Collection<ReceiverParameterDescriptor> hierarchy = scope.getImplicitReceiversHierarchy();

        return Lists.newArrayList(
                Collections2.transform(hierarchy,
                       new Function<ReceiverParameterDescriptor, ReceiverValue>() {
                           @Override
                           public ReceiverValue apply(ReceiverParameterDescriptor receiverParameterDescriptor) {
                               return receiverParameterDescriptor.getValue();
                           }
                       })
        );
    }

    /**
     * Get all extension descriptors among visible descriptors for current scope.
     *
     * @param scope Scope for query extensions.
     * @return extension descriptors.
     */
    public static Collection<CallableDescriptor> getAllExtensions(@NotNull JetScope scope) {
        Set<CallableDescriptor> result = Sets.newHashSet();

        for (DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
            if (descriptor instanceof CallableDescriptor) {
                CallableDescriptor callDescriptor = (CallableDescriptor) descriptor;
                if (callDescriptor.getReceiverParameter() != null) {
                    result.add(callDescriptor);
                }
            }
        }

        return result;
    }

    public static JetScope makeScopeForPropertyAccessor(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope parentScope,
            @NotNull DescriptorResolver descriptorResolver,
            @NotNull BindingTrace trace
    ) {
        JetScope propertyDeclarationInnerScope = descriptorResolver
                .getPropertyDeclarationInnerScope(propertyDescriptor, parentScope,
                                                  propertyDescriptor.getTypeParameters(),
                                                  propertyDescriptor.getReceiverParameter(), trace);
        WritableScope accessorScope = new WritableScopeImpl(propertyDeclarationInnerScope, parentScope.getContainingDeclaration(),
                                                            new TraceBasedRedeclarationHandler(trace), "Accessor Scope");
        accessorScope.changeLockLevel(WritableScope.LockLevel.READING);

        return accessorScope;
    }

}
