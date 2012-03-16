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

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.lang.psi.JetPsiUtil;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

/**
 * @author alex.tkachman
 */
public class StdlibTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithFullJdk();
        myEnvironment.addToClasspath(ForTestCompileStdlib.stdlibJarForTests());
        File junitJar = new File("libraries/testlib/lib/junit-4.9.jar");

        if (!junitJar.exists()) {
            throw new AssertionError();
        }

        myEnvironment.addToClasspath(junitJar);
        CompileEnvironment.ensureRuntime(myEnvironment);
    }

    @Override
    protected GeneratedClassLoader createClassLoader(ClassFileFactory codegens) throws MalformedURLException {
        return new GeneratedClassLoader(
                codegens,
                new URLClassLoader(new URL[]{ForTestCompileStdlib.stdlibJarForTests().toURI().toURL()},
                                   getClass().getClassLoader()));
    }

    public void testKt533 () {
        blackBoxFile("regressions/kt533.kt");
    }

    public void testKt529 () {
        blackBoxFile("regressions/kt529.kt");
    }

    public void testKt528 () {
        blackBoxFile("regressions/kt528.kt");
    }

    public void testKt789 () {
//        blackBoxFile("regressions/kt789.jet");
    }

    public void testKt828 () {
        blackBoxFile("regressions/kt828.kt");
    }

    public void testKt715 () {
        blackBoxFile("regressions/kt715.kt");
    }

    public void testKt864 () {
        blackBoxFile("regressions/kt864.jet");
    }

    public void testKt274 () {
        blackBoxFile("regressions/kt274.kt");
    }

    //from ClassGenTest
    public void testKt344 () throws Exception {
        loadFile("regressions/kt344.jet");
//        System.out.println(generateToText());
        blackBox();
    }

    //from ExtensionFunctionsTest
    public void testGeneric() throws Exception {
        blackBoxFile("extensionFunctions/generic.jet");
    }

    //from NamespaceGenTest
//    public void testPredicateOperator() throws Exception {
//        loadText("fun foo(s: String) = s?startsWith(\"J\")");
//        final Method main = generateFunction();
//        try {
//            assertEquals("JetBrains", main.invoke(null, "JetBrains"));
//            assertNull(main.invoke(null, "IntelliJ"));
//        } catch (Throwable t) {
////            System.out.println(generateToText());
//            t.printStackTrace();
//            throw t instanceof Exception ? (Exception)t : new RuntimeException(t);
//        }
//    }
//
    public void testForInString() throws Exception {
        loadText("fun foo() : Int {        var sum = 0\n" +
                 "        for(c in \"239\")\n" +
                 "            sum += (c.toInt() - '0'.toInt())\n" +
                 "        return sum" +
                 "}" );
        final Method main = generateFunction();
        try {
            assertEquals(14, main.invoke(null));
        } catch (Throwable t) {
            System.out.println(generateToText());
            t.printStackTrace();
            throw t instanceof Exception ? (Exception)t : new RuntimeException(t);
        }
    }

    public void testKt1406() throws Exception {
        blackBoxFile("regressions/kt1406.kt");
    }

    public void testKt1568() throws Exception {
        blackBoxFile("regressions/kt1568.kt");
    }

    public void testKt1515() throws Exception {
        blackBoxFile("regressions/kt1515.kt");
    }

    public void testKt1592 () throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        loadFile("regressions/kt1592.kt");
        ClassFileFactory codegens = generateClassesInFile();
        GeneratedClassLoader loader = createClassLoader(codegens);

        try {
            String fqName = NamespaceCodegen.getJVMClassName(JetPsiUtil.getFQName(myFile), true).replace("/", ".");
            Class<?> namespaceClass = loader.loadClass(fqName);
            Method method = namespaceClass.getMethod("box", Method.class);
            method.setAccessible(true);
            Test annotation = method.getAnnotation(Test.class);
            assertEquals(annotation.timeout(), 0l);
            assertEquals(annotation.expected(), Test.None.class);
        }
        catch (Throwable t) {
            System.out.println(generateToText());
            throw new RuntimeException(t);
        }
        finally {
           loader.dispose();
        }
    }
}
