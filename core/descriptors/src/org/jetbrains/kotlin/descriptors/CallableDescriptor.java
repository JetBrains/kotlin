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

package org.jetbrains.kotlin.descriptors;

import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;
import java.util.List;

public interface CallableDescriptor extends DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot,
                                            Substitutable<CallableDescriptor> {
    @NotNull
    @ReadOnly
    List<ReceiverParameterDescriptor> getContextReceiverParameters();

    @Nullable
    ReceiverParameterDescriptor getExtensionReceiverParameter();

    @Nullable
    ReceiverParameterDescriptor getDispatchReceiverParameter();

    @NotNull
    @ReadOnly
    List<TypeParameterDescriptor> getTypeParameters();

    /**
     * Method may return null for not yet fully initialized object or if error occurred.
     */
    @Nullable
    KotlinType getReturnType();

    @NotNull
    @Override
    CallableDescriptor getOriginal();

    @NotNull
    List<ValueParameterDescriptor> getValueParameters();

    /**
     * Kotlin functions always have stable parameter names that can be reliably used when calling them with named arguments.
     * Functions loaded from platform definitions (e.g. Java binaries or JS) may have unstable parameter names that vary from
     * one platform installation to another. These names can not be used reliably for calls with named arguments.
     */
    boolean hasStableParameterNames();

    /**
     * Sometimes parameter names are not available at all (e.g. Java binaries with not enough debug information).
     * In this case, getName() returns synthetic names such as "p0", "p1" etc.
     */
    boolean hasSynthesizedParameterNames();

    @NotNull
    Collection<? extends CallableDescriptor> getOverriddenDescriptors();

    interface UserDataKey<V> {}

    // TODO: pull up userdata related members to DeclarationDescriptor and use more efficient implementation (e.g. THashMap)
    @Nullable
    <V> V getUserData(UserDataKey<V> key);
}
