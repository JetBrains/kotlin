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

package org.jetbrains.jet.jvm.compiler;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.junit.Assert;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author Pavel Talanov
 */
public final class LoadJavaDescriptorUtil {
    private LoadJavaDescriptorUtil() {
    }

    @NotNull
    public static NamespaceDescriptor compileJava(@NotNull Collection<File> javaFiles, File tmpdir, Disposable disposable)
            throws IOException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(javaFiles);
            List<String> options = Arrays.asList(
                    "-classpath", "out/production/runtime" + File.pathSeparator + JetTestUtils.getAnnotationsJar().getPath(),
                    "-d", tmpdir.getPath()
            );
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        }
        finally {
            fileManager.close();
        }

        JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(disposable, CompileCompilerDependenciesTest.
                compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), tmpdir,
                                              new File("out/production/runtime")));

        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(BuiltinsScopeExtensionMode.ALL,
                                                                                       jetCoreEnvironment.getProject());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        return javaDescriptorResolver.resolveNamespace(FqName.topLevel(Name.identifier("test")), DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }
}
