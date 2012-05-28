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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public abstract class CodegenTestCase extends UsefulTestCase {

    // for environment and classloader
    protected JetCoreEnvironment myEnvironment;
    private List<File> extraClasspath = Lists.newArrayList();
    protected JetFile myFile;

    protected void createEnvironmentWithMockJdkAndIdeaAnnotations() {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironemnt twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable());
    }

    protected void createEnvironmentWithMockJdkAndIdeaAnnotations(@NotNull CompilerSpecialMode compilerSpecialMode) {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironemnt twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(), compilerSpecialMode);
    }

    protected void createEnvironmentWithFullJdk() {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironemnt twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithFullJdk(getTestRootDisposable());
    }

    protected void addToClasspath(@NotNull File file) {
        myEnvironment.addToClasspath(file);
        extraClasspath.add(file);
    }

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
        myEnvironment = null;
        super.tearDown();
    }

    protected void loadText(final String text) {
        myFile = (JetFile) JetTestUtils.createFile("a.jet", text, myEnvironment.getProject());
    }

    protected String loadFile(final String name) {
        try {
            final String content = JetTestUtils.doLoadFile(JetParsingTest.getTestDataDir() + "/codegen/", name);
            myFile = (JetFile) JetTestUtils.createFile(name, content, myEnvironment.getProject());
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
        String content = loadFile(filename);
        Matcher matcher = Pattern.compile("// expected: (.*)").matcher(content);
        String expectedValue = matcher.find() ? matcher.group(1) : "OK";
        Object actual;
        try {
            actual = blackBox();
        } catch (NoClassDefFoundError e) {
            System.out.println(generateToText());
            throw e;
        } catch (Throwable e) {
            System.out.println(generateToText());
            throw new RuntimeException(e);
        }
        if (!Objects.equal(expectedValue, actual)) {
            System.out.println(generateToText());
        }
        assertEquals(expectedValue, actual);
    }

    @NotNull
    protected String blackBox() throws Exception {
        ClassFileFactory codegens = generateClassesInFile();
        GeneratedClassLoader loader = createClassLoader(codegens);

        try {
            if (myFile.isScript()) {
                Class<?> scriptClass = loader.loadClass("Script");
                Object scriptInstance = scriptClass.newInstance();
                Field field = scriptClass.getDeclaredField("rv");
                field.setAccessible(true);
                Object result = field.get(scriptInstance);
                return result != null ? result.toString() : "null";
            }
            else {
                String fqName = NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(myFile)).getFqName().getFqName();
                Class<?> namespaceClass = loader.loadClass(fqName);
                Method method = namespaceClass.getMethod("box");
                return (String) method.invoke(null);
            }
        } finally {
           loader.dispose();
        }
    }

    protected GeneratedClassLoader createClassLoader(ClassFileFactory codegens) throws MalformedURLException {
        List<URL> urls = Lists.newArrayList();
        for (File file : extraClasspath) {
            urls.add(file.toURI().toURL());
        }
        ClassLoader parentClassLoader = new URLClassLoader(urls.toArray(new URL[0]), CodegenTestCase.class.getClassLoader());
        return new GeneratedClassLoader(codegens, parentClassLoader);
    }

    protected String generateToText() {
        return generateCommon(ClassBuilderFactories.TEXT).createText();
    }

    private GenerationState generateCommon(ClassBuilderFactory classBuilderFactory) {
        final AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                myFile, myEnvironment.getCompilerDependencies());
        analyzeExhaust.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());
        GenerationState state = new GenerationState(myEnvironment.getProject(), classBuilderFactory, analyzeExhaust, Collections.singletonList(myFile));
        state.compileCorrectFiles(CompilationErrorHandler.THROW_EXCEPTION);
        return state;
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
        String fqName = NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(myFile)).getFqName().getFqName();
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
            ClassBuilderFactory classBuilderFactory = ClassBuilderFactories.binaries(false);

            return generateCommon(classBuilderFactory).getFactory();
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
            if (r == null) {
                throw new AssertionError();
            }
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
