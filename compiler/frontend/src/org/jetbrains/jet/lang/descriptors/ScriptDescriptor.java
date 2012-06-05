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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class ScriptDescriptor extends DeclarationDescriptorImpl {
    private static final Name NAME = Name.special("<script>");

    private JetType returnType;
    private List<ValueParameterDescriptor> valueParameters;

    private final ScriptCodeDescriptor scriptCodeDescriptor = new ScriptCodeDescriptor(this);

    public ScriptDescriptor(@Nullable DeclarationDescriptor containingDeclaration) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), NAME);
    }

    public void initialize(@NotNull JetType returnType, @NotNull List<ValueParameterDescriptor> valueParameters) {
        this.returnType = returnType;
        this.valueParameters = valueParameters;
    }

    @NotNull
    public JetType getReturnType() {
        return returnType;
    }

    @NotNull
    public List<ValueParameterDescriptor> getValueParameters() {
        return valueParameters;
    }

    @NotNull
    public ScriptCodeDescriptor getScriptCodeDescriptor() {
        return scriptCodeDescriptor;
    }

    @Override
    public DeclarationDescriptor substitute(TypeSubstitutor substitutor) {
        throw new IllegalStateException("nothing to substitute in script");
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitScriptDescriptor(this, data);
    }
}
