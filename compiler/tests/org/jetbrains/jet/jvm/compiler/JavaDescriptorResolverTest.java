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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.forTestCompile.ForTestPackJdkAnnotations;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Stepan Koltsov
 * @see ReadJavaBinaryClassTest
 */
public class JavaDescriptorResolverTest extends TestCaseWithTmpdir {

    // This test can be implemented in ReadJavaBinaryClassTest, but it is simpler here
    public void testInner() throws Exception {
        compileFileResolveDescriptor("inner.java", new FqName("A"));
    }

    // We cannot declare class and namespace with same name in Kotlin
    // http://youtrack.jetbrains.com/issue/KT-1388
    public void testStaticFinal() throws Exception {
        JavaDescriptorResolver javaDescriptorResolver = compileFileGetJavaDescriptorResolver("staticFinal.java");
        NamespaceDescriptor ns = javaDescriptorResolver.resolveNamespace(new FqName("StaticFinal"), DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        Collection<VariableDescriptor> foos = ns.getMemberScope().getProperties(Name.identifier("foo"));
        Assert.assertEquals(1, foos.size());
        VariableDescriptor foo = foos.iterator().next();
        Assert.assertFalse(foo.getType().isNullable());
    }

    private ClassDescriptor compileFileResolveDescriptor(@NotNull String fileRelativePath, @NotNull FqName fqName) throws IOException {
        JavaDescriptorResolver javaDescriptorResolver = compileFileGetJavaDescriptorResolver(fileRelativePath);
        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(fqName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        Assert.assertNotNull(classDescriptor);
        return classDescriptor;
    }

    private JavaDescriptorResolver compileFileGetJavaDescriptorResolver(String fileRelativePath) throws IOException {
        JetTestUtils.compileJavaFile(new File("compiler/testData/javaDescriptorResolver/" + fileRelativePath), tmpdir);

        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(
                myTestRootDisposable, CompilerSpecialMode.JDK_HEADERS);
        jetCoreEnvironment.addToClasspath(tmpdir);

        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(
                jetCoreEnvironment.getCompilerDependencies(), jetCoreEnvironment.getProject());
        return injector.getJavaDescriptorResolver();
    }

    public void testResolveJdkHeaderClassWithoutJdk() {
        JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(myTestRootDisposable, new CompilerDependencies(CompilerSpecialMode.IDEA, null, ForTestPackJdkAnnotations
                .jdkAnnotationsForTests(), null));

        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(
                jetCoreEnvironment.getCompilerDependencies(), jetCoreEnvironment.getProject());

        // java.lang.Iterable must exist in jdk-headers otherwize this test is meaningless, resolveClass always returns null
        injector.getJavaDescriptorResolver().resolveClass(new FqName("java.lang.Iterable"));
    }
}
