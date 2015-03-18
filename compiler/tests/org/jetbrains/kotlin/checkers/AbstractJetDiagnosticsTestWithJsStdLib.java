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

package org.jetbrains.kotlin.checkers;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfigWithCaching;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingTrace;

import java.util.List;
import java.util.Map;

public abstract class AbstractJetDiagnosticsTestWithJsStdLib extends AbstractJetDiagnosticsTest {
    private LibrarySourcesConfig config;

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
        TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(jetFiles, moduleTrace, module, config);
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

        for(ModuleDescriptorImpl moduleDescriptor : config.getModuleDescriptors()) {
            module.addDependencyOnModule(moduleDescriptor);
        }

        module.seal();

        return module;
    }
}
