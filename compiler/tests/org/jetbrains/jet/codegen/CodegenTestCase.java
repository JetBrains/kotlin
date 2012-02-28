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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

/**
 * @author yole
 */
public abstract class CodegenTestCase extends JetLiteFixture {

    protected static void assertThrows(Method foo, Class<? extends Throwable> exceptionClass, Object instance, Object... args) throws IllegalAccessException {
        boolean caught = false;
        try {
            foo.invoke(instance, args);
        }
        catch(InvocationTargetException ex) {
            caught = exceptionClass.isInstance(ex.getTargetException());
        }
        assertTrue(caught);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        myFile = null;
        super.tearDown();
    }

    protected void loadText(final String text) {
        myFile = (JetFile) createFile("a.jet", text);
    }

    @Override
    protected String loadFile(final String name) {
        try {
            final String content = doLoadFile(JetParsingTest.getTestDataDir() + "/codegen/", name);
            myFile = (JetFile) createFile(name, content);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".jet");
    }

    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    protected void blackBoxFile(String filename) {
        loadFile(filename);
        String actual;
        try {
            actual = blackBox();
        } catch (NoClassDefFoundError e) {
            System.out.println(generateToText());
            throw e;
        } catch (Throwable e) {
            System.out.println(generateToText());
            throw new RuntimeException(e);
        }
        if (!"OK".equals(actual)) {
            System.out.println(generateToText());
        }
        assertEquals("OK", actual);
    }

    protected String blackBox() throws Exception {
        ClassFileFactory codegens = generateClassesInFile();
        GeneratedClassLoader loader = createClassLoader(codegens);

        try {
            String fqName = NamespaceCodegen.getJVMClassName(JetPsiUtil.getFQName(myFile), true).replace("/", ".");
            Class<?> namespaceClass = loader.loadClass(fqName);
            Method method = namespaceClass.getMethod("box");
            return (String) method.invoke(null);
        } finally {
           loader.dispose();
        }
    }

    protected GeneratedClassLoader createClassLoader(ClassFileFactory codegens) throws MalformedURLException {
        return new GeneratedClassLoader(codegens);
    }

    protected String generateToText() {
        GenerationState state = new GenerationState(getProject(), ClassBuilderFactories.TEXT);
        AnalyzingUtils.checkForSyntacticErrors(myFile);
        state.compile(myFile);

        return state.createText();
    }

    protected Class generateNamespaceClass() {
        ClassFileFactory state = generateClassesInFile();
        return loadRootNamespaceClass(state);
    }

    protected Class generateClass(String name) {
        ClassFileFactory state = generateClassesInFile();
        return loadClass(name, state);
    }

    protected Class loadRootNamespaceClass(@NotNull ClassFileFactory state) {
        String fqName = NamespaceCodegen.getJVMClassName(JetPsiUtil.getFQName(myFile), true).replace("/", ".");
        try {
            return createClassLoader(state).loadClass(fqName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Class loadClass(String fqName, @NotNull ClassFileFactory state) {
        try {
            return createClassLoader(state).loadClass(fqName);
        } catch (ClassNotFoundException e) {
        } catch (MalformedURLException e) {
        }

        fail("No classfile was generated for: " + fqName);
        return null;
    }

    @NotNull
    protected ClassFileFactory generateClassesInFile() {
        try {
            GenerationState state = new GenerationState(getProject(), ClassBuilderFactories.binaries(false));
            AnalyzingUtils.checkForSyntacticErrors(myFile);
            state.compile(myFile);

            return state.getFactory();
        } catch (RuntimeException e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    protected Method generateFunction() {
        Class aClass = generateNamespaceClass();
        try {
            Method r = null;
            for (Method method : aClass.getMethods()) {
                if (method.getDeclaringClass().equals(Object.class)) {
                    continue;
                }

                if (r != null) {
                    throw new AssertionError("more then one public method in class " + aClass);
                }

                r = method;
            }
            if (r == null)
                throw new AssertionError();
            return r;
        } catch (Error e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    protected Method generateFunction(String name) {
        Class aClass = generateNamespaceClass();
        final Method method = findMethodByName(aClass, name);
        if (method == null) {
            throw new IllegalArgumentException("couldn't find method " + name);
        }
        return method;
    }

    @Nullable
    protected static Method findMethodByName(Class aClass, String name) {
        for (Method method : aClass.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    protected static void assertIsCurrentTime(long returnValue) {
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(returnValue - currentTime);
        assertTrue("Difference with current time: " + diff + " (this test is a bad one: it may fail even if the generated code is correct)", diff <= 1L);
    }

    protected Class loadImplementationClass(@NotNull ClassFileFactory codegens, final String name) {
        return loadClass(name, codegens);
    }

}
