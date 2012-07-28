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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;
import java.util.Set;

/**
* @author abreslav
*/
public interface CallableDescriptor extends DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot {
    @NotNull
    ReceiverDescriptor getReceiverParameter();

    @NotNull
    ReceiverDescriptor getExpectedThisObject();

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
    CallableDescriptor substitute(TypeSubstitutor substitutor);

    @NotNull
    List<ValueParameterDescriptor> getValueParameters();

    @NotNull
    Set<? extends CallableDescriptor> getOverriddenDescriptors();
}
