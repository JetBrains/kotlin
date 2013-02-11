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
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.jetbrains.jet.codegen.CodegenTestUtil.compileJava;

public class OuterClassGenTest extends CodegenTestCase {

    private UrlClassLoader javaClassLoader;

    private ClassFileFactory classFileFactory;

    private final String [] INFO_PARTS = new String [] {"class", "method", "descriptor"};

    protected void setUp() throws Exception {
        super.setUp();
        File javaClassesTempDirectory = compileJava("outerClassInfo/outerClassInfo.java");
        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), javaClassesTempDirectory));
        javaClassLoader = new UrlClassLoader(new URL[] {javaClassesTempDirectory.toURI().toURL()}, getClass().getClassLoader());
        loadFiles("outerClassInfo/outerClassInfo.kt");
        classFileFactory = generateClassesInFile();
    }

    public void testOuterClassInfo() throws ClassNotFoundException, IOException {
        //CLASS SECTION
        compareOuterClassInfo("foo.Foo");
        //class object
        compareOuterClassInfo("foo.Foo$object");
        //inner class
        compareOuterClassInfo("foo.Foo$InnerClass");
        //inner object
        compareOuterClassInfo("foo.Foo$InnerObject");
        //local class in function
        compareOuterClassInfo("foo.Foo$foo$LocalClass", "foo.Foo$1LocalClass");
        //local object in function
        compareOuterClassInfo("foo.Foo$foo$LocalObject", "foo.Foo$1LocalObject");

        //PACKAGE SECTION
        //package object
        compareOuterClassInfo("foo.PackageInnerObject");

        //object literal in package
        compareOuterClassInfo("foo$packageObjectLiteral$1", "foo.FooPackage$1");
        //local class in package function
        compareOuterClassInfo("foo.FooPackage$packageMethod$PackageLocalClass", "foo.FooPackage$1PackageLocalClass");
        //local object in package function
        compareOuterClassInfo("foo.FooPackage$packageMethod$PackageLocalObject", "foo.FooPackage$1PackageLocalObject");
    }

    private void compareOuterClassInfo(String clazzName) throws ClassNotFoundException, IOException {
        compareOuterClassInfo(clazzName, clazzName);
    }

    private void compareOuterClassInfo(String kotlinName, String javaName) throws ClassNotFoundException, IOException {
        String javaClassPath = javaName.replaceAll("\\.", File.separator) + ".class";
        InputStream javaClassStream = javaClassLoader.getResourceAsStream(javaClassPath);
        ClassReader javaReader =  new ClassReader(javaClassStream);

        ClassReader kotlinReader =  new ClassReader(classFileFactory.asBytes(kotlinName.replaceAll("\\.", File.separator) + ".class"));

        checkInfo(kotlinReader, javaReader);
    }


    private void checkInfo(ClassReader kotlinReader, ClassReader javaReader) {
        String [] kotlinInfo = getOuterClasInfo(kotlinReader);
        String [] javaInfo = getOuterClasInfo(javaReader);
        for (int i = 0; i < kotlinInfo.length; i++) {
            String info = kotlinInfo[i];
            assertEquals("Error in enclosingMethodInfo info for: " + kotlinReader.getClassName() + " class in " + INFO_PARTS[i] + " part", javaInfo[i], info);
        }
    }

    public String [] getOuterClasInfo(ClassReader reader) {
        final String [] info = new String [3];
        reader.accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public void visitOuterClass(String owner, String name, String desc) {
                info[0] = owner;
                info[1] = name;
                info[2] = desc;
            }
        }, 0);
        return info;
    }
}
