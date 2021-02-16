/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors;
import org.jetbrains.kotlin.utils.Printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScopeUtils {
    private ScopeUtils() {}

    public static LexicalScope makeScopeForPropertyHeader(
            @NotNull LexicalScope parent,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        return new LexicalScopeImpl(parent, propertyDescriptor, false, Collections.emptyList(), LexicalScopeKind.PROPERTY_HEADER,
                                    // redeclaration on type parameters should be reported early, see: DescriptorResolver.resolvePropertyDescriptor()
                                    LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
                                    handler -> {
                                        for (TypeParameterDescriptor typeParameterDescriptor : propertyDescriptor.getTypeParameters()) {
                                            handler.addClassifierDescriptor(typeParameterDescriptor);
                                        }
                                        return Unit.INSTANCE;
                                    });
    }

    @NotNull
    public static LexicalScope makeScopeForPropertyInitializer(
            @NotNull LexicalScope propertyHeader,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        return new LexicalScopeImpl(propertyHeader, propertyDescriptor, false, Collections.emptyList(), LexicalScopeKind.PROPERTY_INITIALIZER_OR_DELEGATE);
    }

    @NotNull
    public static LexicalScope makeScopeForDelegateConventionFunctions(
            @NotNull LexicalScope parent,
            @NotNull VariableDescriptorWithAccessors variableDescriptor
    ) {
        List<ReceiverParameterDescriptor> implicitReceivers = new ArrayList<>();
        ReceiverParameterDescriptor extensionReceiverParameter = variableDescriptor.getExtensionReceiverParameter();
        if (extensionReceiverParameter != null) {
            implicitReceivers.add(extensionReceiverParameter);
        }
        // todo: very strange scope!
        return new LexicalScopeImpl(parent, variableDescriptor, true, implicitReceivers,
                                    LexicalScopeKind.PROPERTY_DELEGATE_METHOD
        );
    }

    // TestOnly
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
