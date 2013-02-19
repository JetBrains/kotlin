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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.impl.AbstractNamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;

import java.util.Collections;

public class LazyPackageDescriptor extends AbstractNamespaceDescriptorImpl implements LazyDescriptor, NamespaceDescriptor {
    private final JetScope memberScope;
    private final JetScope lazyScope;

    public LazyPackageDescriptor(
            @NotNull NamespaceDescriptorParent containingDeclaration,
            @NotNull Name name,
            @NotNull ResolveSession resolveSession,
            @NotNull PackageMemberDeclarationProvider declarationProvider
    ) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), name);

        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Package scope");
        resolveSession.getModuleConfiguration().extendNamespaceScope(resolveSession.getTrace(), this, scope);
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        this.lazyScope = new LazyPackageMemberScope(resolveSession, declarationProvider, this);

        this.memberScope = new ChainedScope(this, "Lazy package members scope: " + name, lazyScope, scope);
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return DescriptorUtils.getFQName(this).toSafe();
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(lazyScope);
    }
}
