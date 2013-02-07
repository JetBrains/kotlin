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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.junit.Test;

import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class StdlibTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File junitJar = new File("libraries/lib/junit-4.9.jar");

        if (!junitJar.exists()) {
            throw new AssertionError();
        }

        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironemnt twice");
        }
        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.FULL_JDK, JetTestUtils.getAnnotationsJar(), junitJar));
    }

    @NotNull
    @Override
    protected GeneratedClassLoader createClassLoader(@NotNull ClassFileFactory codegens) {
        try {
            return new GeneratedClassLoader(
                    codegens,
                    new URLClassLoader(new URL[]{ForTestCompileRuntime.runtimeJarForTests().toURI().toURL()},
                                       getClass().getClassLoader()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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

    public void testKt1592 () throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        loadFile("regressions/kt1592.kt");
        String fqName = NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(myFiles.getPsiFile())).getFqName().getFqName();
        Class<?> namespaceClass = generateClass(fqName);
        Method method = namespaceClass.getMethod("foo", Method.class);
        method.setAccessible(true);
        Test annotation = method.getAnnotation(Test.class);
        assertEquals(annotation.timeout(), 0l);
        assertEquals(annotation.expected(), Test.None.class);
    }

    public void testAnnotationClassWithClassProperty()
        throws
        NoSuchFieldException,
        NoSuchMethodException,
        ClassNotFoundException,
        IllegalAccessException,
        InstantiationException,
        InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: java.lang.Class<*>)\n" +
                 "" +
                 "A(javaClass<String>()) class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals(Class.class, methods[0].getReturnType());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        assertEquals(String.class, methods[0].invoke(bClassAnnotation));
    }

    public void testAnnotationClassWithStringArrayProperty()
        throws
        NoSuchFieldException,
        NoSuchMethodException,
        ClassNotFoundException,
        IllegalAccessException,
        InstantiationException,
        InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: Array<String>)\n" +
                 "" +
                 "A(array(\"239\",\"932\")) class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals(String[].class, methods[0].getReturnType());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        Object invoke = methods[0].invoke(bClassAnnotation);
        assertEquals("239", ((String[])invoke)[0]);
        assertEquals("932", ((String[])invoke)[1]);
    }

    public void testAnnotationClassWithIntArrayProperty()
        throws
        NoSuchFieldException,
        NoSuchMethodException,
        ClassNotFoundException,
        IllegalAccessException,
        InstantiationException,
        InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: IntArray)\n" +
                 "" +
                 "A(intArray(239,932)) class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals(int[].class, methods[0].getReturnType());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        Object invoke = methods[0].invoke(bClassAnnotation);
        assertEquals(239, ((int[])invoke)[0]);
        assertEquals(932, ((int[])invoke)[1]);
    }

    public void testAnnotationClassWithEnumArrayProperty()
        throws
        NoSuchFieldException,
        NoSuchMethodException,
        ClassNotFoundException,
        IllegalAccessException,
        InstantiationException,
        InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Target(ElementType.TYPE, ElementType.METHOD) annotation class A");
        Class aClass = generateClass("A");

        Target annotation = (Target)aClass.getAnnotation(Target.class);
        ElementType[] value = annotation.value();
        assertEquals(2, value.length);

        assertEquals(ElementType.TYPE, value[0]);
        assertEquals(ElementType.METHOD, value[1]);
    }

    public void testAnnotationClassWithAnnotationArrayProperty()
        throws
        NoSuchFieldException,
        NoSuchMethodException,
        ClassNotFoundException,
        IllegalAccessException,
        InstantiationException,
        InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: Array<Retention>)\n" +
                 "" +
                 "A(array(Retention(RetentionPolicy.RUNTIME),Retention(RetentionPolicy.SOURCE))) class B()");
        Class aClass = generateClass("A");

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        Object invoke = methods[0].invoke(bClassAnnotation);
        Retention[] invoke1 = (Retention[])invoke;
        assertEquals(2, invoke1.length);
        assertEquals(invoke1[0].value(), RetentionPolicy.RUNTIME);
        assertEquals(invoke1[1].value(), RetentionPolicy.SOURCE);
    }

    public void testKt1515() {
        blackBoxMultiFile("/multi/kt1515/thisPackage.kt", "/multi/kt1515/otherPackage.kt");
    }
}
