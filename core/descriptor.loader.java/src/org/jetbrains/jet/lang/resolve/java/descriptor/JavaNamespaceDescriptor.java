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

package org.jetbrains.jet.lang.resolve.java.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.impl.AbstractNamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.List;

public class JavaNamespaceDescriptor extends AbstractNamespaceDescriptorImpl {
    private JetScope memberScope;
    private final FqName qualifiedName;

    public JavaNamespaceDescriptor(NamespaceDescriptorParent containingDeclaration, List<AnnotationDescriptor> annotations,
            @NotNull FqName qualifiedName) {
        super(containingDeclaration, annotations, qualifiedName.shortNameOrSpecial());
        this.qualifiedName = qualifiedName;
    }

    public void setMemberScope(@NotNull JetScope memberScope) {
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return qualifiedName;
    }
}
