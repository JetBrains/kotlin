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

package org.jetbrains.jet.codegen.binding;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public interface CalculatedClosure {
    @Nullable
    ResolvedCall<ConstructorDescriptor> getSuperCall();

    @Nullable
    ClassDescriptor getCaptureThis();

    @Nullable
    JetType getCaptureReceiverType();

    @NotNull
    Map<DeclarationDescriptor, EnclosedValueDescriptor> getCaptureVariables();

    @NotNull
    List<Pair<String, Type>> getRecordedFields();
}
