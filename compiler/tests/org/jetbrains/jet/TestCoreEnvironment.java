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

package org.jetbrains.jet;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.SimpleKotlinModuleManager;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.KotlinModuleManager;
import org.jetbrains.jet.lang.resolve.ModuleSourcesManager;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public class TestCoreEnvironment {
    private final JetCoreEnvironment jetCoreEnvironment;
    private final SimpleKotlinModuleManager moduleManager;

    public TestCoreEnvironment(Disposable parentDisposable, @NotNull CompilerConfiguration configuration) {
        this.jetCoreEnvironment = new JetCoreEnvironment(parentDisposable, configuration);
        this.moduleManager = (SimpleKotlinModuleManager) KotlinModuleManager.SERVICE.getService(getProject());
    }

    @NotNull
    public JetCoreEnvironment getJetCoreEnvironment() {
        return jetCoreEnvironment;
    }

    @NotNull
    public Project getProject() {
        return getJetCoreEnvironment().getProject();
    }

    @NotNull
    public ModuleSourcesManager getModuleSourcesManager() {
        return moduleManager.getSourcesManager();
    }

    @NotNull
    public ModuleDescriptor getModuleDescriptor() {
        Collection<ModuleDescriptor> modules = moduleManager.getModules();
        assert modules.size() == 1 : "Test environment should have only one module";
        return modules.iterator().next();
    }

    @NotNull
    public SubModuleDescriptor getSubModuleDescriptor() {
        Collection<SubModuleDescriptor> subModules = getModuleDescriptor().getSubModules();
        assert subModules.size() == 1 : "Test environment should have only one sub-module";
        return subModules.iterator().next();
    }

    @NotNull
    public JetFile createFile(@NotNull String name, @NotNull String text) {
        return JetTestUtils.createFile(getProject(), name, text);
    }

    @NotNull
    public PsiClassFinder getPsiClassFinder() {
        return moduleManager.getInjectorForJavaDescriptorResolver().getPsiClassFinder();
    }

    @NotNull
    public JavaDescriptorResolver getJavaDescriptorResolver() {
        return moduleManager.getInjectorForJavaDescriptorResolver().getJavaDescriptorResolver();
    }

    @NotNull
    public BindingTrace getBindingTrace() {
        return moduleManager.getInjectorForJavaDescriptorResolver().getBindingTrace();
    }

    @NotNull
    public CompilerConfiguration getConfiguration() {
        return getJetCoreEnvironment().getConfiguration();
    }

    public void newTrace() {
        // let the next analysis use another trace
        CliLightClassGenerationSupport.getInstanceForCli(getJetCoreEnvironment().getProject()).newBindingTrace();
    }

    @NotNull
    public CoreLocalFileSystem getVirtualFileSystem() {
        return getJetCoreEnvironment().getVirtualFileSystem();
    }

    @NotNull
    public Collection<JetFile> getSourceFiles() {
        return getJetCoreEnvironment().getSourceFiles();
    }

    @Nullable
    public PackageViewDescriptor getPackageView(@NotNull FqName fqName) {
        return getSubModuleDescriptor().getPackageView(fqName);
    }
}
