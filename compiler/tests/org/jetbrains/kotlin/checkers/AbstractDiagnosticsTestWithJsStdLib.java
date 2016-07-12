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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.LanguageFeatureSettings;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TargetPlatformKt;
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractDiagnosticsTestWithJsStdLib extends AbstractDiagnosticsTest {
    private JsConfig config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompilerConfiguration configuration = getEnvironment().getConfiguration().copy();
        configuration.put(CommonConfigurationKeys.MODULE_NAME, KotlinTestUtils.TEST_MODULE_NAME);
        configuration.put(JSConfigurationKeys.LIBRARY_FILES, LibrarySourcesConfig.JS_STDLIB);
        config = new LibrarySourcesConfig(getProject(), configuration);
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
    @NotNull
    protected JsAnalysisResult analyzeModuleContents(
            @NotNull ModuleContext moduleContext,
            @NotNull List<KtFile> ktFiles,
            @NotNull BindingTrace moduleTrace,
            @Nullable LanguageFeatureSettings languageFeatureSettings
    ) {
        // TODO: support LANGUAGE directive in JS diagnostic tests
        assert languageFeatureSettings == null
                : BaseDiagnosticsTest.LANGUAGE_DIRECTIVE + " directive is not supported in JS diagnostic tests";
        return TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(ktFiles, moduleTrace, moduleContext, config);
    }

    @Override
    public boolean shouldSkipJvmSignatureDiagnostics(Map<TestModule, List<TestFile>> groupedByModule) {
        return true;
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createModule(@NotNull String moduleName, @NotNull StorageManager storageManager) {
        return TargetPlatformKt.createModule(
                JsPlatform.INSTANCE, Name.special(moduleName), storageManager, JsPlatform.INSTANCE.getBuiltIns());
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createSealedModule(@NotNull StorageManager storageManager) {
        ModuleDescriptorImpl module = createModule("<kotlin-js-test-module>", storageManager);

        List<ModuleDescriptorImpl> dependencies = new ArrayList<ModuleDescriptorImpl>();
        dependencies.add(module);

        for (JsModuleDescriptor<ModuleDescriptorImpl> moduleDescriptor : config.getModuleDescriptors()) {
            dependencies.add(moduleDescriptor.getData());
        }

        dependencies.add(module.getBuiltIns().getBuiltInsModule());
        module.setDependencies(dependencies);

        return module;
    }

    protected JsConfig getConfig() {
        return config;
    }
}
