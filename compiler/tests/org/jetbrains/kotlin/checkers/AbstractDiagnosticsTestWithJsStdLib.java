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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.resolve.BindingContextSlicesJsKt;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor;
import org.jetbrains.kotlin.serialization.js.ModuleKind;
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
            @Nullable LanguageVersionSettings languageVersionSettings,
            boolean separateModules
    ) {
        // TODO: support LANGUAGE directive in JS diagnostic tests
        assert languageVersionSettings == null
                : BaseDiagnosticsTest.LANGUAGE_DIRECTIVE + " directive is not supported in JS diagnostic tests";
        moduleTrace.record(BindingContextSlicesJsKt.MODULE_KIND, moduleContext.getModule(), getModuleKind(ktFiles));
        return TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(ktFiles, moduleTrace, moduleContext, config);
    }

    @NotNull
    private static ModuleKind getModuleKind(@NotNull List<KtFile> ktFiles) {
        ModuleKind kind = ModuleKind.PLAIN;
        for (KtFile file : ktFiles) {
            String text = file.getText();
            for (String line : StringUtil.splitByLines(text)) {
                line = line.trim();
                if (!line.startsWith("//")) continue;
                line = line.substring(2).trim();
                List<String> parts = StringUtil.split(line, ":");
                if (parts.size() != 2) continue;

                if (!parts.get(0).trim().equals("MODULE_KIND")) continue;
                kind = ModuleKind.valueOf(parts.get(1).trim());
            }
        }

        return kind;
    }

    @Override
    @NotNull
    protected List<ModuleDescriptorImpl> getAdditionalDependencies(@NotNull  ModuleDescriptorImpl module) {
        List<ModuleDescriptorImpl> dependencies = new ArrayList<ModuleDescriptorImpl>();
        for (JsModuleDescriptor<ModuleDescriptorImpl> moduleDescriptor : config.getModuleDescriptors()) {
            dependencies.add(moduleDescriptor.getData());
        }
        return dependencies;
    }

    @Override
    public boolean shouldSkipJvmSignatureDiagnostics(Map<TestModule, List<TestFile>> groupedByModule) {
        return true;
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createModule(@NotNull String moduleName, @NotNull StorageManager storageManager) {
        return new ModuleDescriptorImpl(Name.special("<" + moduleName + ">"), storageManager, JsPlatform.INSTANCE.getBuiltIns());
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createSealedModule(@NotNull StorageManager storageManager) {
        ModuleDescriptorImpl module = createModule("kotlin-js-test-module", storageManager);

        List<ModuleDescriptorImpl> dependencies = new ArrayList<ModuleDescriptorImpl>();
        dependencies.add(module);

        dependencies.addAll(getAdditionalDependencies(module));

        dependencies.add(module.getBuiltIns().getBuiltInsModule());
        module.setDependencies(dependencies);

        return module;
    }

    protected JsConfig getConfig() {
        return config;
    }
}
