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

package org.jetbrains.jet.compiler.runner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;
import static com.intellij.openapi.util.text.StringUtil.escapeXml;
import static org.jetbrains.jet.cli.common.modules.ModuleXmlParser.*;

public class KotlinModuleXmlGenerator implements KotlinModuleDescriptionGenerator {

    public static final KotlinModuleXmlGenerator INSTANCE = new KotlinModuleXmlGenerator();

    private KotlinModuleXmlGenerator() {}

    @Override
    public CharSequence generateModuleScript(
            String moduleName,
            DependencyProvider dependencyProvider,
            List<File> sourceFiles,
            boolean tests,
            final Set<File> directoriesToFilterOut
    ) {
        StringBuilder xml = new StringBuilder();
        final Printer p = new Printer(xml);

        openTag(p, MODULES);

        if (tests) {
            p.println("<!-- Module script for tests -->");
        }
        else {
            p.println("<!-- Module script for production -->");
        }

        p.println("<", MODULE, " ", NAME, "=\"", escapeXml(moduleName), "\">");
        p.pushIndent();

        for (File sourceFile : sourceFiles) {
            p.println("<", SOURCES, " ", PATH, "=\"", getEscapedPath(sourceFile), "\"/>");
        }

        dependencyProvider.processClassPath(new DependencyProcessor() {
            @Override
            public void processClassPathSection(@NotNull String sectionDescription, @NotNull Collection<File> files) {
                p.println("<!-- ", sectionDescription, " -->");
                for (File file : files) {
                    boolean isOutput = directoriesToFilterOut.contains(file);
                    if (isOutput) {
                        // For IDEA's make (incremental compilation) purposes, output directories of the current module and its dependencies
                        // appear on the class path, so we are at risk of seeing the results of the previous build, i.e. if some class was
                        // removed in the sources, it may still be there in binaries. Thus, we delete these entries from the classpath.
                        p.println("<!-- Output directory, commented out -->");
                        p.println("<!-- ");
                        p.pushIndent();
                    }

                    p.println("<", CLASSPATH, " ", PATH, "=\"", getEscapedPath(file), "\"/>");

                    if (isOutput) {
                        p.popIndent();
                        p.println("-->");
                    }
                }
            }

            @Override
            public void processAnnotationRoots(@NotNull List<File> files) {
                p.println("<!-- External annotations -->");
                for (File file : files) {
                    p.println("<", EXTERNAL_ANNOTATIONS, " ", PATH, "= \"", getEscapedPath(file), "\"/>");
                }
            }
        });

        closeTag(p, MODULE);

        closeTag(p, MODULES);
        return xml;
    }

    private static void openTag(Printer p, String tag) {
        p.println("<" + tag + ">");
        p.pushIndent();
    }

    private static void closeTag(Printer p, String tag) {
        p.popIndent();
        p.println("</" + tag + ">");
    }

    private static String getEscapedPath(File sourceFile) {
        return escapeXml(toSystemIndependentName(sourceFile.getPath()));
    }
}
