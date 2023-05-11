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

package org.jetbrains.kotlin.descriptors;

import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.mpp.ClassLikeSymbolMarker;
import org.jetbrains.kotlin.mpp.ClassifierSymbolMarker;

import java.util.List;

public interface ClassifierDescriptorWithTypeParameters
        extends ClassifierDescriptor, DeclarationDescriptorWithVisibility, MemberDescriptor,
                Substitutable<ClassifierDescriptorWithTypeParameters>, ClassLikeSymbolMarker, ClassifierSymbolMarker {
    /**
     * @return <code>true</code> if this class contains a reference to its outer class (as opposed to static nested class)
     */
    boolean isInner();

    @ReadOnly
    @NotNull
    List<TypeParameterDescriptor> getDeclaredTypeParameters();
}
