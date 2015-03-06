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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;

public interface ClassDescriptor extends ClassifierDescriptor, MemberDescriptor, ClassOrPackageFragmentDescriptor {
    @NotNull
    JetScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments);

    @NotNull
    JetScope getUnsubstitutedInnerClassesScope();

    @NotNull
    JetScope getStaticScope();

    @NotNull
    @ReadOnly
    Collection<ConstructorDescriptor> getConstructors();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    /**
     * @return type A&lt;T&gt; for the class A&lt;T&gt;
     */
    @NotNull
    @Override
    JetType getDefaultType();

    @NotNull
    @Override
    ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    @Nullable
    @Override
    JetType getClassObjectType();

    /**
     * @return nested object declared as 'default' if one is present.
     */
    @Nullable
    ClassDescriptor getDefaultObjectDescriptor();

    @NotNull
    ClassKind getKind();

    @Override
    @NotNull
    Modality getModality();

    @Override
    @NotNull
    Visibility getVisibility();

    /**
     * @return <code>true</code> if this class contains a reference to its outer class (as opposed to static nested class)
     */
    boolean isInner();

    boolean isDefaultObject();

    @NotNull
    ReceiverParameterDescriptor getThisAsReceiverParameter();

    @Nullable
    ConstructorDescriptor getUnsubstitutedPrimaryConstructor();
}
