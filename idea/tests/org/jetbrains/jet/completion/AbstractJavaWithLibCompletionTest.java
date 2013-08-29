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

package org.jetbrains.jet.completion;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.forTestCompile.ForTestPackJdkAnnotations;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.jet.testing.ConfigLibraryUtil;

import java.io.File;
import java.io.IOException;

public abstract class AbstractJavaWithLibCompletionTest extends JetFixtureCompletionBaseTestCase {
    public void doTestWithJar(String testPath) throws IOException {

        File jarDependency = getOrCompileJarDependency(testPath);
        assert jarDependency.exists() : "Library file should exist: " + jarDependency.getAbsolutePath();
        NewLibraryEditor editor = new NewLibraryEditor();
        editor.setName("doTestWithJarLib");
        editor.addRoot(VfsUtil.getUrlForLibraryRoot(jarDependency), OrderRootType.CLASSES);

        try {
            ConfigLibraryUtil.configureLibrary(myModule, getProjectDescriptor().getSdk(), editor);
            CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
            doTest(testPath);
        }
        finally {
            ConfigLibraryUtil.unConfigureLibrary(myModule, getProjectDescriptor().getSdk(), editor.getName());
        }
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new LightProjectDescriptor() {
            @Override
            public ModuleType getModuleType() {
                return StdModuleTypes.JAVA;
            }

            @Override
            public Sdk getSdk() {
                return PluginTestCaseBase.jdkFromIdeaHome();
            }

            @Override
            public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
                // Do nothing
            }
        };
    }

    @Override
    public TargetPlatform getPlatform() {
        return TargetPlatform.JVM;
    }

    //NOTE: you can just delete existing jar to recompile data for tests that have kotlin dependency
    @NotNull
    private File getOrCompileJarDependency(@NotNull String testPath) throws IOException {
        File fullDirectoryPath = new File(testPath).getParentFile();
        String jarLibName = getTestName(false) + ".jar";
        File jarFile = new File(fullDirectoryPath, jarLibName);
        if (!jarFile.exists()) {
            compileDependencySources(jarFile, fullDirectoryPath);
        }
        return jarFile;
    }

    private void compileDependencySources(@NotNull File outputFile, @NotNull File fullDirectoryPath) {
        File sourcesDir = new File(fullDirectoryPath, getTestName(true) + "Src");
        File stdlib = ForTestCompileRuntime.runtimeJarForTests();
        File jdkAnnotations = ForTestPackJdkAnnotations.jdkAnnotationsForTests();
        ExitCode rv = new K2JVMCompiler().exec(System.out,
                                               "-src", sourcesDir.getAbsolutePath(),
                                               "-jar", outputFile.getAbsolutePath(),
                                               "-noStdlib",
                                               "-classpath", stdlib.getAbsolutePath(),
                                               "-noJdkAnnotations",
                                               "-annotations", jdkAnnotations.getAbsolutePath());
        Assert.assertEquals("compilation completed with non-zero code: " + rv.getCode(), ExitCode.OK, rv);
    }
}
