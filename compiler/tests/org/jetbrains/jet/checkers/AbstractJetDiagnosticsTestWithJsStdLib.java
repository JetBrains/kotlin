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

package org.jetbrains.jet.checkers;

import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfigWithCaching;

import java.util.List;
import java.util.Map;

public abstract class AbstractJetDiagnosticsTestWithJsStdLib extends AbstractJetDiagnosticsTest {

    private LibrarySourcesConfigWithCaching config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        config = new LibrarySourcesConfigWithCaching(getProject(), "module", EcmaVersion.defaultVersion(), false, true, false);
    }

    @Override
    protected void tearDown() throws Exception {
        config = null;
        super.tearDown();
    }

    @Override
    @NotNull
    protected JetCoreEnvironment createEnvironment(@NotNull Disposable disposable, @NotNull CompilerConfiguration configuration) {
        return JetCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES);
    }

    @Override
    protected void analyzeModuleContents(
            GlobalContext context,
            List<JetFile> jetFiles,
            ModuleDescriptorImpl module,
            BindingTrace moduleTrace
    ) {
        BindingContext libraryContext = config.getLibraryContext();
        DelegatingBindingTrace trace = new DelegatingBindingTrace(libraryContext, "trace with preanalyzed library");

        TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(jetFiles, moduleTrace, module, Predicates.<PsiFile>alwaysTrue(), config);

        trace.addAllMyDataTo(moduleTrace);
    }

    @Override
    public boolean shouldSkipJvmSignatureDiagnostics(Map<TestModule, List<TestFile>> groupedByModule) {
        return true;
    }

    @Override
    protected ModuleDescriptorImpl createModule(String moduleName) {
        return TopDownAnalyzerFacadeForJS.createJsModule(moduleName);
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createSealedModule() {
        //It's JVM specific thing, so for JS we just create and setup module.

        ModuleDescriptorImpl module = createModule("<kotlin-js-test-module>");

        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());

        ModuleDescriptor libraryModule = config.getLibraryModule();
        assert libraryModule instanceof ModuleDescriptorImpl;
        module.addDependencyOnModule((ModuleDescriptorImpl) libraryModule);

        module.seal();

        return module;
    }
}
