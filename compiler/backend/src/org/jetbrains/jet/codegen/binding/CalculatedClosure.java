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

package org.jetbrains.jet.codegen.binding;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall;

import java.util.List;
import java.util.Map;

public interface CalculatedClosure {

    @Nullable
    ClassDescriptor getEnclosingClass();

    @Nullable
    JetDelegatorToSuperCall getSuperCall();

    @Nullable
    ClassDescriptor getCaptureThis();

    @Nullable
    ClassifierDescriptor getCaptureReceiver();

    @NotNull
    Map<DeclarationDescriptor, EnclosedValueDescriptor> getCaptureVariables();

    @NotNull
    List<Pair<String, Type>> getRecordedFields();
}
