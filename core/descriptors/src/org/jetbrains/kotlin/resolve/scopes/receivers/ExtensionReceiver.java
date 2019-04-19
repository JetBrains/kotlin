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

package org.jetbrains.kotlin.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker;

/**
 * [ExtensionReceiver] is a receiver for pure `this` call inside lambda with receiver
 *
 * x.run {
 *     this // ExtensionReceiver
 *     this as String // ExtensionReceiver
 *     this.toString() // ImplicitClassReceiver
 *     toString() // ImplicitClassReceiver
 * }
 */
public class ExtensionReceiver extends AbstractReceiverValue implements ImplicitReceiver {

    private final CallableDescriptor descriptor;

    public ExtensionReceiver(
            @NotNull CallableDescriptor callableDescriptor,
            @NotNull KotlinType receiverType,
            @Nullable ReceiverValue original
    ) {
        super(receiverType, original);
        this.descriptor = callableDescriptor;
    }

    @NotNull
    @Override
    public CallableDescriptor getDeclarationDescriptor() {
        return descriptor;
    }

    @NotNull
    @Override
    public ReceiverValue replaceType(@NotNull KotlinType newType) {
        return new ExtensionReceiver(descriptor, newType, getOriginal());
    }

    @Override
    public String toString() {
        return getType() + ": Ext {" + descriptor + "}";
    }
}
