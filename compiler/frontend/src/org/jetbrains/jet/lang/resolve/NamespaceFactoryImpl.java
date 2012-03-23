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

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import javax.inject.Inject;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLUTION_SCOPE;

/**
 * @author Stepan Koltsov
 */
public class NamespaceFactoryImpl implements NamespaceFactory {

    private ModuleDescriptor moduleDescriptor;
    private BindingTrace trace;
    private ModuleConfiguration configuration;

    @Inject
    public void setModuleDescriptor(ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setConfiguration(ModuleConfiguration configuration) {
        this.configuration = configuration;
    }



    public NamespaceDescriptorImpl createNamespaceDescriptorPathIfNeeded(JetFile file, JetScope outerScope) {
        JetNamespaceHeader namespaceHeader = file.getNamespaceHeader();

        if (moduleDescriptor.getRootNs() == null) {
            createNamespaceDescriptorIfNeeded(null, moduleDescriptor, "<root>", true);
        }

        NamespaceDescriptorParent currentOwner = moduleDescriptor.getRootNs();
        if (currentOwner == null) {
            throw new IllegalStateException("must be initialized 5 lines above");
        }

        for (JetSimpleNameExpression nameExpression : namespaceHeader.getParentNamespaceNames()) {
            String namespaceName = JetPsiUtil.safeName(nameExpression.getReferencedName());

            NamespaceDescriptorImpl namespaceDescriptor = createNamespaceDescriptorIfNeeded(null, currentOwner, namespaceName, false);
            trace.record(BindingContext.NAMESPACE_IS_SRC, namespaceDescriptor, true);

            currentOwner = namespaceDescriptor;

            trace.record(REFERENCE_TARGET, nameExpression, currentOwner);
            trace.record(RESOLUTION_SCOPE, nameExpression, outerScope);

            outerScope = namespaceDescriptor.getMemberScope();
        }

        String name = JetPsiUtil.safeName(namespaceHeader.getName());
        trace.record(RESOLUTION_SCOPE, namespaceHeader, outerScope);

        NamespaceDescriptorImpl namespaceDescriptor = createNamespaceDescriptorIfNeeded(file, currentOwner, name, false);
        trace.record(BindingContext.NAMESPACE_IS_SRC, namespaceDescriptor, true);

        return namespaceDescriptor;
    }

    @Override
    @NotNull
    public NamespaceDescriptorImpl createNamespaceDescriptorPathIfNeeded(@NotNull FqName fqName) {
        NamespaceDescriptorParent owner = moduleDescriptor;
        for (FqName pathElement : fqName.path()) {
            owner = createNamespaceDescriptorIfNeeded(null,
                    owner, pathElement.isRoot() ? "<root>" : pathElement.shortName(), pathElement.isRoot());
        }
        return (NamespaceDescriptorImpl) owner;
    }

    @NotNull
    public NamespaceDescriptorImpl createNamespaceDescriptorIfNeeded(@Nullable JetFile file, @NotNull NamespaceDescriptorParent owner, @NotNull String name, boolean root) {

        FqName fqName;
        NamespaceDescriptorImpl namespaceDescriptor;
        if (root) {
            if (!(owner instanceof ModuleDescriptor)) {
                throw new IllegalStateException();
            }
            fqName = FqName.ROOT;
            namespaceDescriptor = ((ModuleDescriptor) owner).getRootNs();
        }
        else {
            FqName ownerFqName = DescriptorUtils.getFQName(owner).toSafe();
            fqName = ownerFqName.child(name);
            namespaceDescriptor = ((NamespaceDescriptorImpl) owner).getNamespace(name);
        }

        if (namespaceDescriptor == null) {
            namespaceDescriptor = new NamespaceDescriptorImpl(
                    owner,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO: annotations
                    name
            );
            trace.record(FQNAME_TO_NAMESPACE_DESCRIPTOR, fqName, namespaceDescriptor);
            WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, new TraceBasedRedeclarationHandler(trace)).setDebugName("Namespace member scope");
            scope.changeLockLevel(WritableScope.LockLevel.BOTH);
            namespaceDescriptor.initialize(scope);
            configuration.extendNamespaceScope(trace, namespaceDescriptor, scope);
            owner.addNamespace(namespaceDescriptor);
            if (file != null) {
                trace.record(BindingContext.NAMESPACE, file, namespaceDescriptor);
            }
        }

        if (file != null) {
            trace.record(BindingContext.FILE_TO_NAMESPACE, file, namespaceDescriptor);
        }

        return namespaceDescriptor;
    }


}
