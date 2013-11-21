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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.utils.Printer;

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
            @NotNull BindingTrace trace
    ) {
        JetScope propertyDeclarationInnerScope =
                getPropertyDeclarationInnerScope(propertyDescriptor, parentScope,
                                                 propertyDescriptor.getTypeParameters(),
                                                 propertyDescriptor.getReceiverParameter(), trace);
        WritableScope accessorScope = new WritableScopeImpl(propertyDeclarationInnerScope, parentScope.getContainingDeclaration(),
                                                            new TraceBasedRedeclarationHandler(trace), "Accessor Scope");
        accessorScope.changeLockLevel(WritableScope.LockLevel.READING);

        return accessorScope;
    }

    public static JetScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace
    ) {
        return getPropertyDeclarationInnerScope(propertyDescriptor, outerScope, typeParameters, receiver, trace, true);
    }

    public static JetScope getPropertyDeclarationInnerScopeForInitializer(
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace
    ) {
        return getPropertyDeclarationInnerScope(null, outerScope, typeParameters, receiver, trace, false);
    }

    private static JetScope getPropertyDeclarationInnerScope(
            @Nullable PropertyDescriptor propertyDescriptor,
            // PropertyDescriptor can be null for property scope which hasn't label to property (in this case addLabelForProperty parameter must be false
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace,
            boolean addLabelForProperty
    ) {
        WritableScopeImpl result = new WritableScopeImpl(
                outerScope, outerScope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(trace),
                "Property declaration inner scope");
        if (addLabelForProperty) {
            assert propertyDescriptor != null : "PropertyDescriptor can be null for property scope which hasn't label to property";
            result.addLabeledDeclaration(propertyDescriptor);
        }
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            result.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        if (receiver != null) {
            result.setImplicitReceiver(receiver);
        }
        result.changeLockLevel(WritableScope.LockLevel.READING);
        return result;
    }

    @TestOnly
    @NotNull
    public static String printStructure(@Nullable JetScope scope) {
        StringBuilder out = new StringBuilder();
        Printer p = new Printer(out);
        if (scope == null) {
            p.println("null");
        }
        else {
            scope.printScopeStructure(p);
        }
        return out.toString();
    }
}
