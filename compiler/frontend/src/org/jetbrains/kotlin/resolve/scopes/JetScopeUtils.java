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

package org.jetbrains.kotlin.resolve.scopes;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.utils.Printer;

import java.util.Collection;
import java.util.List;

public final class JetScopeUtils {
    private JetScopeUtils() {}

    @NotNull
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

    public static JetScope makeScopeForPropertyAccessor(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope parentScope,
            @NotNull BindingTrace trace
    ) {
        JetScope propertyDeclarationInnerScope =
                getPropertyDeclarationInnerScope(propertyDescriptor, parentScope,
                                                 propertyDescriptor.getTypeParameters(),
                                                 propertyDescriptor.getExtensionReceiverParameter(), trace);
        WritableScope accessorScope = new WritableScopeImpl(propertyDeclarationInnerScope, parentScope.getContainingDeclaration(),
                                                            new TraceBasedRedeclarationHandler(trace), "Accessor Scope");
        accessorScope.changeLockLevel(WritableScope.LockLevel.READING);

        return accessorScope;
    }

    public static JetScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope outerScope,
            @NotNull RedeclarationHandler redeclarationHandler
    ) {
        return getPropertyDeclarationInnerScope(propertyDescriptor,
                                                outerScope,
                                                propertyDescriptor.getTypeParameters(),
                                                propertyDescriptor.getExtensionReceiverParameter(),
                                                redeclarationHandler,
                                                true);
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
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace
    ) {
        return getPropertyDeclarationInnerScope(propertyDescriptor, outerScope, typeParameters, receiver, trace, false);
    }

    private static JetScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace,
            boolean addLabelForProperty
    ) {
        TraceBasedRedeclarationHandler redeclarationHandler = new TraceBasedRedeclarationHandler(trace);
        return getPropertyDeclarationInnerScope(propertyDescriptor, outerScope, typeParameters, receiver, redeclarationHandler,
                                                addLabelForProperty);
    }

    @NotNull
    private static JetScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            @NotNull RedeclarationHandler redeclarationHandler,
            boolean addLabelForProperty
    ) {
        WritableScopeImpl result = new WritableScopeImpl(
                outerScope, propertyDescriptor, redeclarationHandler,
                "Property declaration inner scope");
        if (addLabelForProperty) {
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
