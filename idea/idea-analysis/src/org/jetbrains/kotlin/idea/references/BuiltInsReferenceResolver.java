/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.references;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.GlobalContextImpl;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.di.InjectorForLazyTopDownAnalyzerBasic;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getClassId;
import static org.jetbrains.kotlin.serialization.deserialization.DeserializationPackage.findClassAcrossModuleDependencies;

public class BuiltInsReferenceResolver extends AbstractProjectComponent {
    private static final File BUILT_INS_COMPILABLE_SRC_DIR =
            new File("core/builtins/src", KotlinBuiltIns.BUILT_INS_PACKAGE_NAME.asString());

    private volatile ModuleDescriptor moduleDescriptor;
    private volatile Set<JetFile> builtInsSources;
    private volatile PackageFragmentDescriptor builtinsPackageFragment;

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
        assert moduleDescriptor == null : "Attempt to initialize twice";

        final Set<JetFile> jetBuiltInsFiles = getJetBuiltInsFiles();

        final Runnable initializeRunnable = new Runnable() {
            @Override
            public void run() {
                GlobalContextImpl globalContext = ContextPackage.GlobalContext();

                // TODO built-ins and lazy resolve
                TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                        globalContext.getStorageManager(),
                        globalContext.getExceptionTracker(),
                        Predicates.<PsiFile>alwaysFalse(), true, false
                );

                ModuleDescriptorImpl module = new ModuleDescriptorImpl(
                        Name.special("<built-ins resolver module>"), Collections.<ImportPath>emptyList(), PlatformToKotlinClassMap.EMPTY
                );
                module.addDependencyOnModule(module);
                module.seal();

                FileBasedDeclarationProviderFactory declarationFactory =
                        new FileBasedDeclarationProviderFactory(topDownAnalysisParameters.getStorageManager(), jetBuiltInsFiles);

                LazyTopDownAnalyzerForTopLevel analyzer = new InjectorForLazyTopDownAnalyzerBasic(
                        myProject, topDownAnalysisParameters, new BindingTraceContext(), module, declarationFactory
                ).getLazyTopDownAnalyzerForTopLevel();

                analyzer.analyzeFiles(topDownAnalysisParameters, jetBuiltInsFiles, Collections.<PackageFragmentProvider>emptyList());

                List<PackageFragmentDescriptor> fragments =
                        module.getPackageFragmentProvider().getPackageFragments(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);

                moduleDescriptor = module;
                builtinsPackageFragment = KotlinPackage.single(fragments);
                builtInsSources = Sets.newHashSet(jetBuiltInsFiles);
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
        String fromUrl = VfsUtilCore.convertFromUrl(url);
        VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(fromUrl);
        assert vf != null : "Virtual file not found by URL: " + url;

        // Refreshing VFS: in case the plugin jar was updated, the caches may hold the old value
        vf.getChildren();
        vf.refresh(false, true);
        PathUtil.getLocalFile(vf).refresh(false, true);

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
    private DeclarationDescriptor findCurrentDescriptorForMember(@NotNull MemberDescriptor originalDescriptor) {
        if (!isFromBuiltinModule(originalDescriptor)) return null;

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
            return isFromBuiltinModule(originalDescriptor)
                   ? findClassAcrossModuleDependencies(moduleDescriptor, getClassId((ClassDescriptor) originalDescriptor))
                   : null;
        }
        else if (originalDescriptor instanceof PackageFragmentDescriptor) {
            return isFromBuiltinModule(originalDescriptor)
                   && KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(((PackageFragmentDescriptor) originalDescriptor).getFqName())
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

    private static boolean isFromBuiltinModule(@NotNull DeclarationDescriptor originalDescriptor) {
        // TODO This is optimization only
        // It should be rewritten by checking declarationDescriptor.getSource(), when the latter returns something non-trivial for builtins.
        return KotlinBuiltIns.getInstance().getBuiltInsModule() == DescriptorUtils.getContainingModule(originalDescriptor);
    }

    @Nullable
    public PsiElement resolveBuiltInSymbol(@NotNull DeclarationDescriptor declarationDescriptor) {
        if (moduleDescriptor == null) {
            return null;
        }

        DeclarationDescriptor descriptor = findCurrentDescriptor(declarationDescriptor);
        if (descriptor != null) {
            return DescriptorToSourceUtils.getSourceFromDescriptor(descriptor);
        }
        return null;
    }

    public static boolean isFromBuiltIns(@NotNull PsiElement element) {
        //noinspection SuspiciousMethodCalls
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
