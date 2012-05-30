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

package org.jetbrains.jet.lang.resolve.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.AbstractNamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collections;

/**
 * @author abreslav
 */
public class LazyPackageDescriptor extends AbstractNamespaceDescriptorImpl implements NamespaceDescriptor {
    private final LazyPackageMemberScope memberScope;

    public LazyPackageDescriptor(
            @NotNull NamespaceDescriptorParent containingDeclaration,
            @NotNull Name name,
            @NotNull ResolveSession resolveSession,
            @NotNull DeclarationProvider declarationProvider
    ) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), name);
        this.memberScope = new LazyPackageMemberScope(resolveSession, declarationProvider, this);
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public FqName getQualifiedName() {
        return DescriptorUtils.getFQName(this).toSafe();
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        throw new UnsupportedOperationException(); // TODO
    }
}
