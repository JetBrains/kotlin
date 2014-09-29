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

package org.jetbrains.jet.lang.descriptors;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;
import java.util.Set;

public interface CallableDescriptor extends DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot {
    @Nullable
    ReceiverParameterDescriptor getExtensionReceiverParameter();

    @Nullable
    ReceiverParameterDescriptor getDispatchReceiverParameter();

    @KotlinSignature("fun getTypeParameters(): List<TypeParameterDescriptor>")
    @NotNull
    List<TypeParameterDescriptor> getTypeParameters();

    /**
     * Method may return null for not yet fully initialized object or if error occurred.
     */
    @Nullable
    JetType getReturnType();

    @NotNull
    @Override
    CallableDescriptor getOriginal();

    @Override
    CallableDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    @KotlinSignature("fun getValueParameters(): List<ValueParameterDescriptor>")
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

    // Workaround for KT-4609 Wildcard types (super/extends) shouldn't be loaded as nullable
    @KotlinSignature("fun getOverriddenDescriptors(): Set<out CallableDescriptor>")
    @NotNull
    Set<? extends CallableDescriptor> getOverriddenDescriptors();
}
