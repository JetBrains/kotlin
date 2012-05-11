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

package org.jetbrains.jet.plugin.references;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class StandardLibraryReferenceResolver extends AbstractProjectComponent {
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private BindingContext bindingContext = null;

    public StandardLibraryReferenceResolver(Project project) {
        super(project);
    }

    @Override
    public void initComponent() {
        StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                ensureInitialized();
            }
        });
    }

    private synchronized void ensureInitialized() {
        if (bindingContext != null) {
            return;
        }

        BindingTraceContext context = new BindingTraceContext();
        NamespaceDescriptorImpl fakeRootNamespace = new NamespaceDescriptorImpl(new ModuleDescriptor("<fake_module>"),
                                                                                Collections.<AnnotationDescriptor>emptyList(), "<root>");
        NamespaceDescriptorImpl jetNamespace = new NamespaceDescriptorImpl(fakeRootNamespace,
                                                                           Collections.<AnnotationDescriptor>emptyList(), "jet");
        context.record(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, new FqName("jet"), jetNamespace);

        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, jetNamespace, RedeclarationHandler.THROW_EXCEPTION)
                .setDebugName("Builtin classes scope");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        jetNamespace.initialize(scope);

        TopDownAnalyzer.processStandardLibraryNamespace(myProject, context, scope, jetNamespace, getJetFiles("jet.src"));

        ClassDescriptor tuple0 = context.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, new FqName("jet.Tuple0"));
        assert tuple0 != null;
        scope = new WritableScopeImpl(scope, jetNamespace, RedeclarationHandler.THROW_EXCEPTION).setDebugName("Builtin classes scope #2");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        scope.addClassifierAlias("Unit", tuple0);
        jetNamespace.initialize(scope);

        TopDownAnalyzer.processStandardLibraryNamespace(myProject, context, scope, jetNamespace, getJetFiles("jet"));
        AnalyzingUtils.throwExceptionOnErrors(context.getBindingContext());

        bindingContext = context.getBindingContext();
    }

    private List<JetFile> getJetFiles(String dir) {
        URL url = StandardLibraryReferenceResolver.class.getResource("/" + dir + "/");
        VirtualFile vf = VfsUtil.findFileByURL(url);
        assert vf != null;

        PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(vf);
        assert psiDirectory != null;
        return ContainerUtil.mapNotNull(psiDirectory.getFiles(), new Function<PsiFile, JetFile>() {
            @Override
            public JetFile fun(PsiFile file) {
                return file instanceof JetFile ? (JetFile) file : null;
            }
        });
    }

    @Nullable
    private DeclarationDescriptor findCurrentDescriptor(@NotNull DeclarationDescriptor originalDescriptor) {
        if (originalDescriptor instanceof ClassDescriptor) {
            return bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFQName(originalDescriptor).toSafe());
        }
        else if (originalDescriptor instanceof NamespaceDescriptor) {
            return bindingContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, DescriptorUtils.getFQName(originalDescriptor).toSafe());
        }
        else {
            DeclarationDescriptor parent = originalDescriptor.getContainingDeclaration();
            if (parent == null) return null;
            parent = findCurrentDescriptor(parent);
            JetScope memberScope;
            if (parent instanceof ClassDescriptor) {
                memberScope = ((ClassDescriptor) parent).getDefaultType().getMemberScope();
            }
            else if (parent instanceof NamespaceDescriptor) {
                memberScope = ((NamespaceDescriptor)parent).getMemberScope();
            }
            else {
                return null;
            }
            String renderedOriginal = DescriptorRenderer.TEXT.render(originalDescriptor);
            for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                if (renderedOriginal.equals(DescriptorRenderer.TEXT.render(member))) {
                    return member;
                }
            }
        }
        return null;
    }

    @Nullable
    public PsiElement resolveStandardLibrarySymbol(@NotNull BindingContext originalContext, @Nullable JetReferenceExpression referenceExpression) {
        ensureInitialized();
        DeclarationDescriptor declarationDescriptor = BindingContextUtils.referenceToDescriptor(originalContext, referenceExpression);

        return declarationDescriptor != null ? resolveStandardLibrarySymbol(declarationDescriptor) : null;
    }

    @Nullable
    public PsiElement resolveStandardLibrarySymbol(@NotNull DeclarationDescriptor declarationDescriptor) {
        ensureInitialized();
        DeclarationDescriptor descriptor = declarationDescriptor;

        descriptor = descriptor.getOriginal();
        descriptor = findCurrentDescriptor(descriptor);
        if (descriptor != null) {
            return BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
        }
        return null;
    }
}
