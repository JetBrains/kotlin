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

package org.jetbrains.kotlin.di;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.GlobalContextImpl;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService;

import java.util.Collections;

public class InjectorForLazyResolveWithJavaUtil {
    @NotNull
    public static InjectorForLazyResolveWithJava create(@NotNull Project project, @NotNull BindingTrace trace, boolean dependOnBuiltins) {
        ModuleDescriptorImpl module = TopDownAnalyzerFacadeForJVM.createJavaModule("<module>");

        GlobalSearchScope moduleContentScope = GlobalSearchScope.allScope(project);
        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        DeclarationProviderFactory declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project, globalContext.getStorageManager(), Collections.<JetFile>emptyList(), moduleContentScope
        );
        SingleModuleClassResolver resolver = new SingleModuleClassResolver();

        InjectorForLazyResolveWithJava injector = new InjectorForLazyResolveWithJava(
                project, globalContext, trace, module, declarationProviderFactory, moduleContentScope, resolver
        );

        resolver.setResolver(injector.getJavaDescriptorResolver());

        module.addDependencyOnModule(module);
        module.initialize(injector.getJavaDescriptorResolver().getPackageFragmentProvider());
        if (dependOnBuiltins) {
            module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        }
        module.seal();

        return injector;
    }
}
