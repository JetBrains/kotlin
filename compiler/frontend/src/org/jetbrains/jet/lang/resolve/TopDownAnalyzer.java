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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptorWithResolutionScopes;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.*;

public class TopDownAnalyzer {

    @NotNull
    private BindingTrace trace;
    @NotNull
    private DeclarationResolver declarationResolver;
    @NotNull
    private TypeHierarchyResolver typeHierarchyResolver;
    @NotNull
    private OverrideResolver overrideResolver;
    @NotNull
    private OverloadResolver overloadResolver;
    @NotNull
    private ModuleDescriptor moduleDescriptor;
    @NotNull
    private MutablePackageFragmentProvider packageFragmentProvider;
    @NotNull
    private BodyResolver bodyResolver;
    @NotNull
    private AdditionalCheckerProvider additionalCheckerProvider;
    @NotNull
    private Project project;

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setDeclarationResolver(@NotNull DeclarationResolver declarationResolver) {
        this.declarationResolver = declarationResolver;
    }

    @Inject
    public void setTypeHierarchyResolver(@NotNull TypeHierarchyResolver typeHierarchyResolver) {
        this.typeHierarchyResolver = typeHierarchyResolver;
    }

    @Inject
    public void setOverrideResolver(@NotNull OverrideResolver overrideResolver) {
        this.overrideResolver = overrideResolver;
    }

    @Inject
    public void setOverloadResolver(@NotNull OverloadResolver overloadResolver) {
        this.overloadResolver = overloadResolver;
    }

    @Inject
    public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setPackageFragmentProvider(@NotNull MutablePackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @Inject
    public void setBodyResolver(@NotNull BodyResolver bodyResolver) {
        this.bodyResolver = bodyResolver;
    }

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @Inject
    public void setAdditionalCheckerProvider(@NotNull AdditionalCheckerProvider additionalCheckerProvider) {
        this.additionalCheckerProvider = additionalCheckerProvider;
    }

    public void doProcess(
            @NotNull TopDownAnalysisContext c,
            @NotNull JetScope outerScope,
            @NotNull PackageLikeBuilder owner,
            @NotNull Collection<? extends PsiElement> declarations
    ) {
//        c.enableDebugOutput();
        c.debug("Enter");

        typeHierarchyResolver.process(c, outerScope, owner, declarations);
        declarationResolver.process(c);
        overrideResolver.process(c);
        lockScopes(c);

        overloadResolver.process(c);

        if (!c.getTopDownAnalysisParameters().isAnalyzingBootstrapLibrary()) {
            bodyResolver.resolveBodies(c);
        }

        c.debug("Exit");
        c.printDebugOutput(System.out);
    }

    private static Collection<JetFile> getFiles(Collection<? extends PsiElement> declarations) {
        return new LinkedHashSet<JetFile>(KotlinPackage.map(declarations, new Function1<PsiElement, JetFile>() {
            @Nullable
            @Override
            public JetFile invoke(PsiElement element) {
                return (JetFile) element.getContainingFile();
            }
        }));
    }

    private void lockScopes(@NotNull TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes mutableClassDescriptor : c.getDeclaredClasses().values()) {
            ((MutableClassDescriptor) mutableClassDescriptor).lockScopes();
        }

        // SCRIPT: extra code for scripts
        Set<FqName> scriptFqNames = Sets.newHashSet();
        for (JetFile file : c.getFileScopes().keySet()) {
            if (file.isScript()) {
                scriptFqNames.add(file.getPackageFqName());
            }
        }
        for (MutablePackageFragmentDescriptor fragment : packageFragmentProvider.getAllFragments()) {
            // todo: this is hack in favor of REPL
            if (!scriptFqNames.contains(fragment.getFqName())) {
                fragment.getMemberScope().changeLockLevel(WritableScope.LockLevel.READING);
            }
        }
    }

    @NotNull
    public TopDownAnalysisContext analyzeFiles(
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull Collection<JetFile> files,
            @NotNull PackageFragmentProvider... additionalProviders
    ) {
        return analyzeFiles(topDownAnalysisParameters, files, Arrays.asList(additionalProviders));
    }

    @NotNull
    public TopDownAnalysisContext analyzeFiles(
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull Collection<JetFile> files,
            @NotNull List<PackageFragmentProvider> additionalProviders
    ) {
        assert !topDownAnalysisParameters.isLazy() : "Lazy resolve must be disabled for this method";

        TopDownAnalysisContext c = new TopDownAnalysisContext(topDownAnalysisParameters);
        CompositePackageFragmentProvider provider =
                new CompositePackageFragmentProvider(KotlinPackage.plus(Arrays.asList(packageFragmentProvider), additionalProviders));

        ((ModuleDescriptorImpl) moduleDescriptor).initialize(provider);

        // dummy builder is used because "root" is module descriptor,
        // packages added to module explicitly in
        doProcess(c, JetModuleUtil.getSubpackagesOfRootScope(moduleDescriptor), new PackageLikeBuilderDummy(), files);

        return c;
    }


    @NotNull
    public MutablePackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }
}


