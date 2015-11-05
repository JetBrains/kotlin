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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.kotlin.utils.Printer;

import java.util.List;

public final class JetScopeUtils {
    private JetScopeUtils() {}

    @NotNull
    public static MemberScope getStaticNestedClassesScope(@NotNull ClassDescriptor descriptor) {
        MemberScope innerClassesScope = descriptor.getUnsubstitutedInnerClassesScope();
        return new FilteringScope(innerClassesScope, new Function1<DeclarationDescriptor, Boolean>() {
            @Override
            public Boolean invoke(DeclarationDescriptor descriptor) {
                return descriptor instanceof ClassDescriptor && !((ClassDescriptor) descriptor).isInner();
            }
        });
    }

    public static LexicalScope makeScopeForPropertyAccessor(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope parentScope,
            @NotNull BindingTrace trace
    ) {
        LexicalScope propertyDeclarationInnerScope =
                getPropertyDeclarationInnerScope(propertyDescriptor, parentScope,
                                                 propertyDescriptor.getTypeParameters(),
                                                 propertyDescriptor.getExtensionReceiverParameter(), trace);
        return new LexicalScopeImpl(propertyDeclarationInnerScope, parentScope.getOwnerDescriptor(), false, null, LexicalScopeKind.UNSORTED);
    }

    public static LexicalScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope outerScope,
            @NotNull RedeclarationHandler redeclarationHandler
    ) {
        return getPropertyDeclarationInnerScope(propertyDescriptor,
                                                outerScope,
                                                propertyDescriptor.getTypeParameters(),
                                                propertyDescriptor.getExtensionReceiverParameter(),
                                                redeclarationHandler,
                                                true);
    }

    public static LexicalScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace
    ) {
        return getPropertyDeclarationInnerScope(propertyDescriptor, outerScope, typeParameters, receiver, trace, true);
    }

    public static LexicalScope getPropertyDeclarationInnerScopeForInitializer(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace
    ) {
        return getPropertyDeclarationInnerScope(propertyDescriptor, outerScope, typeParameters, receiver, trace, false);
    }

    private static LexicalScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope outerScope,
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
    private static LexicalScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope outerScope,
            @NotNull final List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            @NotNull RedeclarationHandler redeclarationHandler,
            boolean addLabelForProperty
    ) {
        return new LexicalScopeImpl(
                outerScope, propertyDescriptor, addLabelForProperty, receiver,
                LexicalScopeKind.UNSORTED,
                redeclarationHandler, new Function1<LexicalScopeImpl.InitializeHandler, Unit>() {
            @Override
            public Unit invoke(LexicalScopeImpl.InitializeHandler handler) {
                for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
                    handler.addClassifierDescriptor(typeParameterDescriptor);
                }
                return Unit.INSTANCE$;
            }
        });
    }

    @TestOnly
    @NotNull
    public static String printStructure(@Nullable MemberScope scope) {
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
