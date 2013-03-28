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

package org.jetbrains.jet.codegen;

import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.jetbrains.jet.codegen.CodegenTestUtil.compileJava;

public class OuterClassGenTest extends CodegenTestCase {

    public void testClass() throws Exception {
        doTest("foo.Foo");
    }

    public void testClassObject() throws Exception {
        doTest("foo.Foo$object");
    }

    public void testInnerClass() throws Exception {
        doTest("foo.Foo$InnerClass");
    }

    public void testInnerObject() throws Exception {
        doTest("foo.Foo$InnerObject");
    }

    public void testLocalClassInFunction() throws Exception {
        doTest("foo.Foo$foo$LocalClass", "foo.Foo$1LocalClass");
    }

    public void testLocalObjectInFunction() throws Exception {
        doTest("foo.Foo$foo$LocalObject", "foo.Foo$1LocalObject");
    }

    public void testObjectInPackageClass() throws Exception {
        doTest("foo.PackageInnerObject");
    }

    public void testObjectLiteralInPackageClass() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/FooPackage$src$outerClassInfo$", null, null);
        doCustomTest("foo.FooPackage$packageObjectLiteral$1", expectedInfo);
    }

    public void testLocalClassInTopLevelFunction() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/FooPackage$src$outerClassInfo$", "packageMethod", "(Lfoo/Foo;)V");
        doCustomTest("foo.FooPackage$packageMethod$PackageLocalClass", expectedInfo);
    }

    public void testLocalObjectInTopLevelFunction() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/FooPackage$src$outerClassInfo$", "packageMethod", "(Lfoo/Foo;)V");
        doCustomTest("foo.FooPackage$packageMethod$PackageLocalObject", expectedInfo);
    }

    private void doTest(@NotNull String className) throws Exception {
        doTest(className, className);
    }

    private void doTest(@NotNull String kotlinName, @NotNull String javaName) throws Exception {
        File javaClassesTempDirectory = compileJava("outerClassInfo/outerClassInfo.java");

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), javaClassesTempDirectory));

        UrlClassLoader javaClassLoader = new UrlClassLoader(new URL[] {javaClassesTempDirectory.toURI().toURL()}, getClass().getClassLoader());

        String javaClassPath = javaName.replace('.', File.separatorChar) + ".class";
        InputStream javaClassStream = javaClassLoader.getResourceAsStream(javaClassPath);
        ClassReader javaReader =  new ClassReader(javaClassStream);

        ClassReader kotlinReader = getKotlinClassReader(kotlinName);

        checkInfo(kotlinReader, javaReader);
    }

    private void doCustomTest(@NotNull String kotlinName, @NotNull OuterClassInfo expectedInfo) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        ClassReader kotlinReader = getKotlinClassReader(kotlinName);
        OuterClassInfo kotlinInfo = getOuterClassInfo(kotlinReader);
        String message = "Error in enclosingMethodInfo info for: " + kotlinReader.getClassName() + " class";
        if ((kotlinInfo.owner == null) || !kotlinInfo.owner.startsWith(expectedInfo.owner)) {
            fail(message + " expectedOwner=" + expectedInfo.owner + ", actualOwner=" + kotlinInfo.owner);
        }
        assertEquals(message, expectedInfo.method, kotlinInfo.method);
        assertEquals(message, expectedInfo.descriptor, kotlinInfo.descriptor);
    }

    private ClassReader getKotlinClassReader(@NotNull String kotlinClassName) {
        loadFile("outerClassInfo/outerClassInfo.kt");
        ClassFileFactory classFileFactory = generateClassesInFile();
        return new ClassReader(classFileFactory.asBytes(kotlinClassName.replace('.', '/') + ".class"));
    }

    private void checkInfo(ClassReader kotlinReader, ClassReader javaReader) {
        OuterClassInfo kotlinInfo = getOuterClassInfo(kotlinReader);
        OuterClassInfo javaInfo = getOuterClassInfo(javaReader);
        assertEquals("Error in enclosingMethodInfo info for: " + kotlinReader.getClassName() + " class", javaInfo, kotlinInfo);
    }

    public OuterClassInfo getOuterClassInfo(ClassReader reader) {
        final OuterClassInfo info = new OuterClassInfo();
        reader.accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public void visitOuterClass(String owner, String name, String desc) {
                info.owner = owner;
                info.method = name;
                info.descriptor = desc;
            }
        }, 0);
        return info;
    }

    private static class OuterClassInfo {
        @Nullable private String owner;
        @Nullable private String method;
        @Nullable private String descriptor;

        private OuterClassInfo(@Nullable String owner, @Nullable String method, @Nullable String descriptor) {
            this.owner = owner;
            this.method = method;
            this.descriptor = descriptor;
        }

        private OuterClassInfo() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OuterClassInfo)) return false;

            OuterClassInfo info = (OuterClassInfo) o;

            if (descriptor != null ? !descriptor.equals(info.descriptor) : info.descriptor != null) return false;
            if (method != null ? !method.equals(info.method) : info.method != null) return false;
            if (owner != null ? !owner.equals(info.owner) : info.owner != null) return false;

            return true;
        }

        @Override
        public String toString() {
            return "[owner=" + owner + ", method=" + method + ", descriptor="+ descriptor + "]";
        }
    }
}
