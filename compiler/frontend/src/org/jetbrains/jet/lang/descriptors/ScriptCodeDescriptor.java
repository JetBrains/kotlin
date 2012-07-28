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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class ScriptCodeDescriptor extends FunctionDescriptorImpl {

    public ScriptCodeDescriptor(@NotNull ScriptDescriptor containingDeclaration) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), Name.special("<script-code>"), Kind.DECLARATION);
        setVisibility(Visibilities.LOCAL);
    }

    public void initialize(
            @NotNull ReceiverDescriptor expectedThisObject,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull JetType returnType) {
        super.initialize(null, expectedThisObject, Collections.<TypeParameterDescriptor>emptyList(), valueParameters, returnType, Modality.FINAL, Visibilities.LOCAL);
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        throw new IllegalStateException("no need to copy script code descriptor");
    }

    @NotNull
    @Override
    public FunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides) {
        throw new IllegalStateException("no need to copy script code descriptor");
    }
}
