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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator;

import java.util.Set;

public class JavaRuntimeLibraryDescription extends CustomLibraryDescriptorWithDeferredConfig {
    public static final LibraryKind KOTLIN_JAVA_RUNTIME_KIND = LibraryKind.create("kotlin-java-runtime");
    public static final String LIBRARY_NAME = "KotlinJavaRuntime";

    public static final String JAVA_RUNTIME_LIBRARY_CREATION = "Java Runtime Library Creation";
    public static final String DIALOG_TITLE = "Create Kotlin Java Runtime Library";
    public static final String LIBRARY_CAPTION = "Kotlin Java Runtime Library";
    public static final Set<LibraryKind> SUITABLE_LIBRARY_KINDS = Sets.newHashSet(KOTLIN_JAVA_RUNTIME_KIND);

    /**
     * @param project null when project doesn't exist yet (called from project wizard)
     */
    public JavaRuntimeLibraryDescription(@Nullable Project project) {
        super(project, KotlinJavaModuleConfigurator.NAME, LIBRARY_NAME, DIALOG_TITLE, LIBRARY_CAPTION, KOTLIN_JAVA_RUNTIME_KIND, SUITABLE_LIBRARY_KINDS);
    }
}
