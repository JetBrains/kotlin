/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler.runner;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class KotlinModuleScriptGenerator {

    public interface DependencyProvider {
        void processClassPath(@NotNull DependencyProcessor processor);
    }

    public interface DependencyProcessor {
        void processClassPathSection(@NotNull String sectionDescription, @NotNull Collection<String> paths);
        void processAnnotationRoots(@NotNull List<String> paths);
    }

    public static CharSequence generateModuleScript(String moduleName,
            DependencyProvider dependencyProvider,
            List<String> sourceFilePaths,
            boolean tests,
            final Set<String> directoriesToFilterOut) {
        final StringBuilder script = new StringBuilder();

        if (tests) {
            script.append("// Module script for tests\n");
        }
        else {
            script.append("// Module script for production\n");
        }

        script.append("import kotlin.modules.*\n");
        script.append("fun project() {\n");
        script.append("    module(\"" + moduleName + "\") {\n");

        for (String sourceFile : sourceFilePaths) {
            script.append("        sources += \"" + sourceFile + "\"\n");
        }

        dependencyProvider.processClassPath(new DependencyProcessor() {
            @Override
            public void processClassPathSection(@NotNull String sectionDescription, @NotNull Collection<String> paths) {
                script.append("        // " + sectionDescription + "\n");
                for (String path : paths) {
                    if (directoriesToFilterOut.contains(path)) {
                        // For IDEA's make (incremental compilation) purposes, output directories of the current module and its dependencies
                        // appear on the class path, so we are at risk of seeing the results of the previous build, i.e. if some class was
                        // removed in the sources, it may still be there in binaries. Thus, we delete these entries from the classpath.
                        script.append("        // Output directory, commented out\n");
                        script.append("        // ");
                    }
                    script.append("        classpath += \"" + path + "\"\n");
                }
            }

            @Override
            public void processAnnotationRoots(@NotNull List<String> paths) {
                script.append("        // External annotations\n");
                for (String path : paths) {
                    script.append("        annotationsPath += \"").append(path).append("\"\n");
                }
            }
        });

        script.append("    }\n");
        script.append("}\n");
        return script;
    }
}
