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

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * @author yole
 */
public abstract class CodegenTestCase extends UsefulTestCase {

    // for environment and classloader
    protected JetCoreEnvironment myEnvironment;
    private List<File> extraClasspath = Lists.newArrayList();
    protected CodegenTestFiles myFiles;

    protected Object scriptInstance;

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
        myFiles = null;
        myEnvironment = null;
        scriptInstance = null;
        super.tearDown();
    }

    protected void loadText(final String text) {
        myFiles = CodegenTestFiles.create("a.jet", text, myEnvironment.getProject());
    }

    protected String loadFile(final String name) {
        try {
            final String content = JetTestUtils.doLoadFile(JetParsingTest.getTestDataDir() + "/codegen/", name);
            myFiles = CodegenTestFiles.create(name, content, myEnvironment.getProject());
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void loadFiles(final String... names) {
        myFiles = CodegenTestFiles.create(myEnvironment.getProject(), names);
    }

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".jet");
    }

    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    protected void blackBoxFile(String filename) {
        loadFile(filename);
        blackBox();
    }

    protected void blackBoxMultiFile(String... filenames) {
        loadFiles(filenames);
        blackBox();
    }

    @NotNull
    private Class<?> loadClassFromType(@NotNull Type type) {
        try {
            switch (type.getSort()) {
                case Type.OBJECT:
                    return Class.forName(type.getClassName());
                case Type.INT:
                    return int.class;
                case Type.LONG:
                    return long.class;
                default:
                    // AFAIK there is no way to create array class from class
                    if (type.getDescriptor().equals("[Ljava/lang/String;")) {
                        return String[].class;
                    }
                    throw new IllegalStateException("not implemented: " + type.getDescriptor());
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    private Constructor getConstructor(@NotNull Class<?> clazz, org.objectweb.asm.commons.Method method) {
        if (!method.getName().equals("<init>")) {
            throw new IllegalArgumentException("not constructor: " + method);
        }
        Class[] classes = new Class[method.getArgumentTypes().length];
        for (int i = 0; i < classes.length; ++i) {
            classes[i] = loadClassFromType(method.getArgumentTypes()[i]);
        }
        try {
            return clazz.getConstructor(classes);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected void blackBox() {
        GenerationState state = generateClassesInFileGetState();

        GeneratedClassLoader loader = createClassLoader(state.getFactory());

        String r;

        try {
            if (myFiles.isScript()) {
                Class<?> scriptClass = loader.loadClass(ScriptCodegen.SCRIPT_DEFAULT_CLASS_NAME.getFqName().getFqName());

                Constructor constructor = getConstructor(scriptClass, state.getScriptConstructorMethod());
                scriptInstance = constructor.newInstance(myFiles.getScriptParameterValues().toArray());

                assertFalse("expecting at least one expectation", myFiles.getExpectedValues().isEmpty());

                for (Pair<String, String> nameValue : myFiles.getExpectedValues()) {
                    String fieldName = nameValue.first;
                    String expectedValue = nameValue.second;

                    if (expectedValue.equals("<nofield>")) {
                        try {
                            scriptClass.getDeclaredField(fieldName);
                            fail("must have no field " + fieldName);
                        } catch (NoSuchFieldException e) {
                            continue;
                        }
                    }

                    Field field = scriptClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object result = field.get(scriptInstance);
                    String resultString = result != null ? result.toString() : "null";
                    assertEquals("comparing field " + fieldName, expectedValue, resultString);
                }
            }
            else {
                String fqName = NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(myFiles.getPsiFiles().get(0))).getFqName().getFqName();
                Class<?> namespaceClass = loader.loadClass(fqName);
                Method method = namespaceClass.getMethod("box");
                r = (String) method.invoke(null);
                assertEquals("OK", r);
            }
        } catch (Error e) {
            System.out.println(generateToText());
            throw e;
        } catch (Throwable e) {
            System.out.println(generateToText());
            throw new RuntimeException(e);
        } finally {
            loader.dispose();
        }
    }

    protected GeneratedClassLoader createClassLoader(ClassFileFactory codegens) {
        List<URL> urls = Lists.newArrayList();
        for (File file : extraClasspath) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        ClassLoader parentClassLoader = new URLClassLoader(urls.toArray(new URL[0]), CodegenTestCase.class.getClassLoader());
        return new GeneratedClassLoader(codegens, parentClassLoader);
    }

    protected String generateToText() {
        return generateCommon(ClassBuilderFactories.TEXT).createText();
    }

    private GenerationState generateCommon(ClassBuilderFactory classBuilderFactory) {
        final AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                myEnvironment.getProject(),
                myFiles.getPsiFiles(),
                myFiles.getScriptParameterTypes(),
                Predicates.<PsiFile>alwaysTrue(),
                myEnvironment.getCompilerDependencies());
        analyzeExhaust.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());
        GenerationState state = new GenerationState(myEnvironment.getProject(), classBuilderFactory, analyzeExhaust, myFiles.getPsiFiles());
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
        String fqName = NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(myFiles.getPsiFile())).getFqName().getFqName();
        try {
            return createClassLoader(state).loadClass(fqName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Class loadClass(String fqName, @NotNull ClassFileFactory state) {
        try {
            return createClassLoader(state).loadClass(fqName);
        } catch (ClassNotFoundException e) {
        }

        fail("No classfile was generated for: " + fqName);
        return null;
    }

    @NotNull
    protected ClassFileFactory generateClassesInFile() {
        GenerationState generationState = generateClassesInFileGetState();
        return generationState.getFactory();
    }

    @NotNull
    private GenerationState generateClassesInFileGetState() {
        GenerationState generationState;
        try {
            ClassBuilderFactory classBuilderFactory = ClassBuilderFactories.binaries(false);
            generationState = generateCommon(classBuilderFactory);

            if (DxChecker.RUN_DX_CHECKER) {
                DxChecker.check(generationState.getFactory());
            }

        } catch (RuntimeException e) {
            System.out.println(generateToText());
            throw e;
        }
        return generationState;
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
