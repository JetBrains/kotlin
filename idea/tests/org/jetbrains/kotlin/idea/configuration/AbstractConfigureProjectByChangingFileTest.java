/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesUpdaterKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;

public abstract class AbstractConfigureProjectByChangingFileTest<C extends KotlinProjectConfigurator> extends LightCodeInsightTestCase {
    private static final String DEFAULT_VERSION = "default_version";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ScriptDependenciesUpdaterKt.setScriptDependenciesUpdaterDisabled(ApplicationManager.getApplication(), true);
    }

    @Override
    protected void tearDown() throws Exception {
        ScriptDependenciesUpdaterKt.setScriptDependenciesUpdaterDisabled(ApplicationManager.getApplication(), false);
        super.tearDown();
    }

    protected void doTest(@NotNull String beforeFile, @NotNull String afterFile, @NotNull C configurator) throws Exception {
        configureByFile(beforeFile);

        String versionFromFile = InTextDirectivesUtils.findStringWithPrefixes(getFile().getText(), "// VERSION:");
        String version = versionFromFile != null ? versionFromFile : DEFAULT_VERSION;

        NotificationMessageCollector collector = NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(getProject());

        runConfigurator(getModule(), getFile(), configurator, version, collector);

        collector.showNotification();

        KotlinTestUtils.assertEqualsToFile(new File(afterFile), getFile().getText().replace(version, "$VERSION$"));
    }

    protected abstract void runConfigurator(
            Module module, @NotNull PsiFile file,
            @NotNull C configurator,
            @NotNull String version,
            @NotNull NotificationMessageCollector collector
    );

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
