/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerBasic;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.unwrapFakeOverride;

public class BuiltInsReferenceResolver extends AbstractProjectComponent {
    private static final File BUILT_INS_COMPILABLE_SRC_DIR =
            new File("core/builtins/src", KotlinBuiltIns.BUILT_INS_PACKAGE_NAME.asString());

    private volatile BindingContext bindingContext;
    private volatile Set<JetFile> builtInsSources;
    private volatile MutablePackageFragmentDescriptor builtinsPackageFragment;

    public BuiltInsReferenceResolver(Project project) {
        super(project);
    }

    @Override
    public void initComponent() {
        StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                initialize();
            }
        });
    }

    @TestOnly
    public Set<JetFile> getBuiltInsSources() {
        return builtInsSources;
    }

    private void initialize() {
        assert bindingContext == null : "Attempt to initialize twice";

        final Set<JetFile> jetBuiltInsFiles = getJetBuiltInsFiles();

        final Runnable initializeRunnable = new Runnable() {
            @Override
            public void run() {
                GlobalContextImpl globalContext = ContextPackage.GlobalContext();
                TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                        globalContext.getStorageManager(),
                        globalContext.getExceptionTracker(),
                        Predicates.<PsiFile>alwaysFalse(), true, false);
                ModuleDescriptorImpl module = new ModuleDescriptorImpl(
                        Name.special("<fake_module>"), Collections.<ImportPath>emptyList(), PlatformToKotlinClassMap.EMPTY);
                BindingTraceContext trace = new BindingTraceContext();
                InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                        myProject, topDownAnalysisParameters, trace, module);

                TopDownAnalyzer analyzer = injector.getTopDownAnalyzer();
                analyzer.analyzeFiles(topDownAnalysisParameters, jetBuiltInsFiles);

                builtinsPackageFragment = analyzer.getPackageFragmentProvider().getOrCreateFragment(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
                builtInsSources = Sets.newHashSet(jetBuiltInsFiles);
                bindingContext = trace.getBindingContext();
            }
        };

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            initializeRunnable.run();
        }
        else {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runReadAction(initializeRunnable);
                }
            });
        }

    }

    @NotNull
    private Set<JetFile> getJetBuiltInsFiles() {
        Set<JetFile> builtIns = getBuiltInSourceFiles(LightClassUtil.getBuiltInsDirUrl());

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            // In production, the above URL is enough as it contains sources for both native and compilable built-ins
            // (it's simply the "kotlin" directory in kotlin-plugin.jar)
            // But in tests, sources of built-ins are not added to the classpath automatically, so we manually specify URLs for both:
            // LightClassUtil.getBuiltInsDirUrl() does so for native built-ins and the code below for compilable built-ins
            try {
                builtIns.addAll(getBuiltInSourceFiles(BUILT_INS_COMPILABLE_SRC_DIR.toURI().toURL()));
            }
            catch (MalformedURLException e) {
                throw UtilsPackage.rethrow(e);
            }
        }

        return builtIns;
    }

    @NotNull
    private Set<JetFile> getBuiltInSourceFiles(@NotNull URL url) {
        VirtualFile vf = VfsUtil.findFileByURL(url);
        assert vf != null : "Virtual file not found by URL: " + url;

        // Refreshing VFS: in case the plugin jar was updated, the caches may hold the old value
        if (vf instanceof NewVirtualFile) {
            NewVirtualFile newVirtualFile = (NewVirtualFile) vf;
            newVirtualFile.markDirtyRecursively(); // This doesn't happen in a JARFS entry, unless we do it manually here
        }
        vf.getChildren();
        vf.refresh(false, true);

        PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(vf);
        assert psiDirectory != null : "No PsiDirectory for " + vf;
        return new HashSet<JetFile>(ContainerUtil.mapNotNull(psiDirectory.getFiles(), new Function<PsiFile, JetFile>() {
            @Override
            public JetFile fun(PsiFile file) {
                return file instanceof JetFile ? (JetFile) file : null;
            }
        }));
    }

    @Nullable
    private DeclarationDescriptor findCurrentDescriptorForClass(@NotNull ClassDescriptor originalDescriptor) {
        // BindingContext doesn't contain an information about class descriptor of class object. For example see testEmptyRange.
        if (DescriptorUtils.isClassObject(originalDescriptor)) {
            DeclarationDescriptor currentParent = findCurrentDescriptor(originalDescriptor.getContainingDeclaration());
            if (currentParent == null) return null;
            return ((ClassDescriptor) currentParent).getClassObjectDescriptor();
        }

        return bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(originalDescriptor));
    }

    @Nullable
    private DeclarationDescriptor findCurrentDescriptorForMember(@NotNull MemberDescriptor originalDescriptor) {
        if (originalDescriptor instanceof CallableMemberDescriptor &&
            ((CallableMemberDescriptor) originalDescriptor).getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return findCurrentDescriptorForMember(unwrapFakeOverride((CallableMemberDescriptor) originalDescriptor).getOriginal());
        }

        DeclarationDescriptor containingDeclaration = findCurrentDescriptor(originalDescriptor.getContainingDeclaration());
        JetScope memberScope = getMemberScope(containingDeclaration);
        if (memberScope == null) return null;

        String renderedOriginal = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(originalDescriptor);
        Collection<? extends DeclarationDescriptor> descriptors;
        if (originalDescriptor instanceof ConstructorDescriptor && containingDeclaration instanceof ClassDescriptor) {
            descriptors = ((ClassDescriptor) containingDeclaration).getConstructors();
        }
        else {
            descriptors = memberScope.getAllDescriptors();
        }
        for (DeclarationDescriptor member : descriptors) {
            if (renderedOriginal.equals(DescriptorRenderer.FQ_NAMES_IN_TYPES.render(member))) {
                return member;
            }
        }
        return null;
    }

    @Nullable
    private DeclarationDescriptor findCurrentDescriptor(@NotNull DeclarationDescriptor originalDescriptor) {
        if (originalDescriptor instanceof ClassDescriptor) {
            return findCurrentDescriptorForClass((ClassDescriptor) originalDescriptor);
        }
        else if (originalDescriptor instanceof PackageFragmentDescriptor) {
            return KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(((PackageFragmentDescriptor) originalDescriptor).getFqName())
                   ? builtinsPackageFragment
                   : null;
        }
        else if (originalDescriptor instanceof MemberDescriptor) {
            return findCurrentDescriptorForMember((MemberDescriptor) originalDescriptor);
        }
        else {
            return null;
        }
    }

    @NotNull
    public Collection<PsiElement> resolveBuiltInSymbol(@NotNull DeclarationDescriptor declarationDescriptor) {
        if (bindingContext == null) {
            return Collections.emptyList();
        }

        DeclarationDescriptor descriptor = findCurrentDescriptor(declarationDescriptor.getOriginal());
        if (descriptor != null) {
            return DescriptorToSourceUtils.descriptorToDeclarations(descriptor);
        }
        return Collections.emptyList();
    }

    public static boolean isFromBuiltIns(@NotNull PsiElement element) {
        return element.getProject().getComponent(BuiltInsReferenceResolver.class).builtInsSources.contains(element.getContainingFile());
    }

    @Nullable
    private static JetScope getMemberScope(@Nullable DeclarationDescriptor parent) {
        if (parent instanceof ClassDescriptor) {
            return ((ClassDescriptor) parent).getDefaultType().getMemberScope();
        }
        else if (parent instanceof PackageFragmentDescriptor) {
            return ((PackageFragmentDescriptor) parent).getMemberScope();
        }
        else {
            return null;
        }
    }
}
