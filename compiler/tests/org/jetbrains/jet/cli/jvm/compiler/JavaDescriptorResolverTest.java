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

package org.jetbrains.jet.cli.jvm.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;

/**
 * @author Stepan Koltsov
 * @see ReadJavaBinaryClassTest
 */
public class JavaDescriptorResolverTest extends TestCaseWithTmpdir {

    // This test can be implemented in ReadJavaBinaryClassTest, but it is simpler here
    public void testInner() throws Exception {
        compileFileResolveDescriptor("inner.java", new FqName("A"));
    }

    private void compileFileResolveDescriptor(@NotNull String fileRelativePath, @NotNull FqName fqName) throws IOException {
        JetTestUtils.compileJavaFile(new File("compiler/testData/javaDescriptorResolver/" + fileRelativePath), tmpdir);

        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, CompilerSpecialMode.JDK_HEADERS);
        jetCoreEnvironment.addToClasspath(tmpdir);

        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(
                jetCoreEnvironment.getCompilerDependencies(), jetCoreEnvironment.getProject());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(fqName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        Assert.assertNotNull(classDescriptor);
    }
}
