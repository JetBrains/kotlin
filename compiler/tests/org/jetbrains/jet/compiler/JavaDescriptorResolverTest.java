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

package org.jetbrains.jet.compiler;

import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.junit.Assert;

import javax.tools.*;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author Stepan Koltsov
 * @see ReadJavaBinaryClassTest
 */
public class JavaDescriptorResolverTest extends TestCaseWithTmpdir {

    // This test can be implemented in ReadJavaBinaryClass test, but it is simpler here
    public void testInner() throws Exception {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            File file = new File("compiler/testData/javaDescriptorResolver/inner.java");
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(Collections.singleton(file));
            List<String> options = Arrays.asList(
                    "-d", tmpdir.getPath()
            );
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        } finally {
            fileManager.close();
        }

        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable, CompilerSpecialMode.JDK_HEADERS);

        jetCoreEnvironment.addToClasspath(tmpdir);

        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(
                CompileCompilerDependenciesTest.compilerDependenciesForTests(CompilerSpecialMode.JDK_HEADERS, true), jetCoreEnvironment.getProject());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(new FqName("A"), DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        Assert.assertNotNull(classDescriptor);
    }

}
