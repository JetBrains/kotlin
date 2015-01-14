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
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.io.File;

public abstract class AbstractConfigureProjectByChangingFileTest extends LightCodeInsightTestCase {
    private static final String DEFAULT_VERSION = "default_version";

    public void doTestWithMaven(@NotNull String path) throws Exception {
        String pathWithFile = path + "/" + MavenConstants.POM_XML;
        doTest(pathWithFile, pathWithFile.replace("pom", "pom_after"), new KotlinJavaMavenConfigurator());
    }

    public void doTestWithJSMaven(@NotNull String path) throws Exception {
        String pathWithFile = path + "/" + MavenConstants.POM_XML;
        doTest(pathWithFile, pathWithFile.replace("pom", "pom_after"), new KotlinJavascriptMavenConfigurator());
    }

    public void doTestAndroidGradle(@NotNull String path) throws Exception {
        doTest(path, path.replace("before", "after"), new KotlinAndroidGradleModuleConfigurator());
    }

    public void doTestGradle(@NotNull String path) throws Exception {
        doTest(path, path.replace("before", "after"), new KotlinGradleModuleConfigurator());
    }

    private void doTest(@NotNull String beforeFile, @NotNull String afterFile, @NotNull KotlinProjectConfigurator configurator) throws Exception {
        configureByFile(beforeFile);

        String versionFromFile = InTextDirectivesUtils.findStringWithPrefixes(getFile().getText(), "// VERSION:");
        String version = versionFromFile != null ? versionFromFile : DEFAULT_VERSION;

        if (configurator instanceof KotlinWithGradleConfigurator) {
            ((KotlinWithGradleConfigurator) configurator).changeGradleFile((GroovyFile) getFile(), version);
        }
        else if (configurator instanceof KotlinMavenConfigurator) {
            ((KotlinMavenConfigurator) configurator).changePomFile(getModule(), getFile(), version);
        }

        JetTestUtils.assertEqualsToFile(new File(afterFile), getFile().getText().replace(version, "$VERSION$"));
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
