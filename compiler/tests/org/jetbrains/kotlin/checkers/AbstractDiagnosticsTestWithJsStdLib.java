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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.TargetPlatformKt;
import org.jetbrains.kotlin.storage.StorageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractDiagnosticsTestWithJsStdLib extends AbstractDiagnosticsTest {
    private Config config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        config = new LibrarySourcesConfig.Builder(getProject(), "module", LibrarySourcesConfig.JS_STDLIB).build();
    }

    @Override
    protected void tearDown() throws Exception {
        config = null;
        super.tearDown();
    }

    @Override
    @NotNull
    protected List<String> getEnvironmentConfigFiles() {
        return EnvironmentConfigFiles.JS_CONFIG_FILES;
    }

    @Override
    protected void analyzeModuleContents(
            @NotNull ModuleContext moduleContext,
            @NotNull List<KtFile> jetFiles,
            @NotNull BindingTrace moduleTrace
    ) {
        TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(jetFiles, moduleTrace, moduleContext, config);
    }

    @Override
    public boolean shouldSkipJvmSignatureDiagnostics(Map<TestModule, List<TestFile>> groupedByModule) {
        return true;
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createModule(@NotNull String moduleName, @NotNull StorageManager storageManager) {
        return TargetPlatformKt.createModule(JsPlatform.INSTANCE, Name.special(moduleName), storageManager);
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createSealedModule(@NotNull StorageManager storageManager) {
        ModuleDescriptorImpl module = createModule("<kotlin-js-test-module>", storageManager);

        List<ModuleDescriptorImpl> dependencies = new ArrayList<ModuleDescriptorImpl>();
        dependencies.add(module);

        for (ModuleDescriptorImpl moduleDescriptor : config.getModuleDescriptors()) {
            dependencies.add(moduleDescriptor);
        }

        dependencies.add(getPlatform().getBuiltIns().getBuiltInsModule());
        module.setDependencies(dependencies);

        return module;
    }

    protected Config getConfig() {
        return config;
    }

    @NotNull
    @Override
    protected TargetPlatform getPlatform() {
        return JsPlatform.INSTANCE;
    }
}
