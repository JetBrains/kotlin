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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;

public class ScriptCodeDescriptor extends FunctionDescriptorImpl {

    public ScriptCodeDescriptor(@NotNull ScriptDescriptor containingDeclaration) {
        super(containingDeclaration, null, Annotations.EMPTY, Name.special("<script-code>"), Kind.DECLARATION, SourceElement.NO_SOURCE);
    }

    public void initialize(
            @NotNull ReceiverParameterDescriptor expectedThisObject,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull JetType returnType) {
        super.initialize(null, expectedThisObject, Collections.<TypeParameterDescriptor>emptyList(), valueParameters, returnType, Modality.FINAL, Visibilities.INTERNAL);
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind
    ) {
        throw new IllegalStateException("no need to copy script code descriptor");
    }

    @NotNull
    @Override
    public FunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, Visibility visibility, Kind kind, boolean copyOverrides) {
        throw new IllegalStateException("no need to copy script code descriptor");
    }
}
