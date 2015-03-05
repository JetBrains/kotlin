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

package org.jetbrains.kotlin.idea.framework;

import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator;
import org.jetbrains.kotlin.idea.configuration.RuntimeLibraryFiles;

import java.util.Set;

import static org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils.getConfiguratorByName;
import static org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator.NAME;

public class JSLibraryStdDescription extends CustomLibraryDescriptorWithDeferredConfig {
    public static final LibraryKind KOTLIN_JAVASCRIPT_KIND = LibraryKind.create("kotlin-js-stdlib");
    public static final String LIBRARY_NAME = "KotlinJavaScript";

    public static final String JAVA_SCRIPT_LIBRARY_CREATION = "JavaScript Library Creation";
    public static final String DIALOG_TITLE = "Create Kotlin JavaScript Library";
    public static final String LIBRARY_CAPTION = "Kotlin JavaScript Library";
    public static final Set<LibraryKind> SUITABLE_LIBRARY_KINDS = Sets.newHashSet(KOTLIN_JAVASCRIPT_KIND);

    /**
     * @param project null when project doesn't exist yet (called from project wizard)
     */
    public JSLibraryStdDescription(@Nullable Project project) {
        super(project, KotlinJsModuleConfigurator.NAME, LIBRARY_NAME, DIALOG_TITLE, LIBRARY_CAPTION, KOTLIN_JAVASCRIPT_KIND, SUITABLE_LIBRARY_KINDS);
    }

    @TestOnly
    public NewLibraryConfiguration createNewLibraryForTests() {
        KotlinJsModuleConfigurator configurator = (KotlinJsModuleConfigurator) getConfiguratorByName(NAME);
        assert configurator != null : "Cannot find configurator with name " + NAME;

        RuntimeLibraryFiles files = configurator.getExistingJarFiles();
        return createConfiguration(files.getRuntimeJar(), files.getRuntimeSourcesJar());
    }
}
