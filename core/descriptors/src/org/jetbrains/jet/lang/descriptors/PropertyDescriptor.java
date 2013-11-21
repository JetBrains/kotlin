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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;
import java.util.Set;

public interface PropertyDescriptor extends VariableDescriptor, CallableMemberDescriptor {
    @Nullable
    PropertyGetterDescriptor getGetter();

    @Nullable
    PropertySetterDescriptor getSetter();

    /**
     * In the following case, the setter is projected out:
     *
     *     trait Tr<T> { var v: T }
     *     fun test(tr: Tr<out String>) {
     *         tr.v = null!! // the assignment is illegal, although a read would be fine
     *     }
     */
    boolean isSetterProjectedOut();

    @NotNull
    List<PropertyAccessorDescriptor> getAccessors();

    @NotNull
    @Override
    PropertyDescriptor getOriginal();

    @NotNull
    @Override
    Set<? extends PropertyDescriptor> getOverriddenDescriptors();

    @Override
    PropertyDescriptor substitute(@NotNull TypeSubstitutor substitutor);
}
