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

package org.jetbrains.jet.plugin.configuration;

import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public abstract class AbstractConfigureGradleProjectTest extends LightCodeInsightTestCase {
    private static final String DEFAULT_VERSION = "default_version";

    public void doTestAndroidGradle(@NotNull String path) throws Exception {
        doTest(path, new KotlinAndroidGradleModuleConfigurator());
    }

    public void doTestGradle(@NotNull String path) throws Exception {
        doTest(path, new KotlinGradleModuleConfigurator());
    }

    private void doTest(@NotNull String path, @NotNull KotlinWithGradleConfigurator configurator) throws Exception {
        configureByFile(path);
        GroovyFile groovyFile = (GroovyFile) getFile();

        String versionFromFile = InTextDirectivesUtils.findStringWithPrefixes(groovyFile.getText(), "// VERSION:");
        String version = versionFromFile != null ? versionFromFile : DEFAULT_VERSION;

        configurator.changeGradleFile(groovyFile, version);

        assertSameLinesWithFile(path.replace("before", "after"), groovyFile.getText().replace(version, "$VERSION$"));
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
