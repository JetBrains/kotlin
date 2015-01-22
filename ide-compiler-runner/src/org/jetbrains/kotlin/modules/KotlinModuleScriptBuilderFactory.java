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

package org.jetbrains.kotlin.modules;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;

public class KotlinModuleScriptBuilderFactory implements KotlinModuleDescriptionBuilderFactory {

    public static final KotlinModuleScriptBuilderFactory INSTANCE = new KotlinModuleScriptBuilderFactory();

    private KotlinModuleScriptBuilderFactory() {}

    @Override
    public KotlinModuleDescriptionBuilder create() {
        return new Builder();
    }

    @Override
    public String getFileExtension() {
        return "kts";
    }

    private static class Builder implements KotlinModuleDescriptionBuilder {
        private final StringBuilder script = new StringBuilder();
        private boolean done = false;

        {
            script.append("import kotlin.modules.*\n");
            script.append("fun project() {\n");
        }

        @Override
        public KotlinModuleDescriptionBuilder addModule(
                String moduleName,
                String outputDir,
                DependencyProvider dependencyProvider,
                List<File> sourceFiles,
                List<File> javaSourceRoots,
                boolean tests,
                final Set<File> directoriesToFilterOut
        ) {
            assert !done : "Already done";

            if (tests) {
                script.append("// Module script for tests\n");
            }
            else {
                script.append("// Module script for production\n");
            }

            script.append("    module(\"" + moduleName + "\", outputDir = \"" + toSystemIndependentName(outputDir) + "\") {\n");

            for (File sourceFile : sourceFiles) {
                script.append("        sources += \"" + toSystemIndependentName(sourceFile.getPath()) + "\"\n");
            }

            DependencyProcessor processor = new DependencyProcessor() {
                @Override
                public void processClassPathSection(@NotNull String sectionDescription, @NotNull Collection<File> files) {
                    script.append("        // " + sectionDescription + "\n");
                    for (File file : files) {
                        if (directoriesToFilterOut.contains(file)) {
                            // For IDEA's make (incremental compilation) purposes, output directories of the current module and its dependencies
                            // appear on the class path, so we are at risk of seeing the results of the previous build, i.e. if some class was
                            // removed in the sources, it may still be there in binaries. Thus, we delete these entries from the classpath.
                            script.append("        // Output directory, commented out\n");
                            script.append("        // ");
                        }
                        script.append("        classpath += \"" + toSystemIndependentName(file.getPath()) + "\"\n");
                    }
                }

                @Override
                public void processAnnotationRoots(@NotNull List<File> files) {
                    script.append("        // External annotations\n");
                    for (File file : files) {
                        script.append("        annotationsPath += \"").append(toSystemIndependentName(file.getPath())).append("\"\n");
                    }
                }
            };

            if (!javaSourceRoots.isEmpty()) {
                processor.processClassPathSection("Java source roots", javaSourceRoots);
            }

            dependencyProvider.processClassPath(processor);

            script.append("    }\n");
            return this;
        }

        @Override
        public CharSequence asText() {
            if (!done)  {
                script.append("}\n");
                done = true;
            }

            return script;
        }
    }

}
