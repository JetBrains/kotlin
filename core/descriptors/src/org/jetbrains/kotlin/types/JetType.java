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

package org.jetbrains.kotlin.types;

import kotlin.jvm.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.List;

/**
 * @see JetTypeChecker#isSubtypeOf(JetType, JetType)
 */
public interface JetType extends Annotated {
    @NotNull TypeConstructor getConstructor();

    @NotNull
    @ReadOnly
    List<TypeProjection> getArguments();

    @NotNull
    TypeSubstitution getSubstitution();

    boolean isMarkedNullable();

    @NotNull
    JetScope getMemberScope();

    boolean isError();

    @Override
    boolean equals(Object other);

    @KotlinSignature("fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T?")
    @Nullable
    <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass);

    @NotNull
    TypeCapabilities getCapabilities();
}
