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
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.StandardGenerationStrategy;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.codegen.state.Progress;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class CodegenTestCase extends UsefulTestCase {

    // for environment and classloader
    protected JetCoreEnvironment myEnvironment;
    protected CodegenTestFiles myFiles;

    protected Object scriptInstance;
    private GenerationState alreadyGenerated;
    private GeneratedClassLoader initializedClassLoader;

    protected void createEnvironmentWithMockJdkAndIdeaAnnotations() {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironemnt twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable());
    }

    protected void createEnvironmentWithMockJdkAndIdeaAnnotations(@NotNull ConfigurationKind configurationKind) {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironemnt twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(), configurationKind);
    }

    protected void createEnvironmentWithFullJdk() {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironemnt twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithFullJdk(getTestRootDisposable());
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
        alreadyGenerated = null;

        if (initializedClassLoader != null) {
            initializedClassLoader.dispose();
            initializedClassLoader = null;
        }

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

    protected String loadFileByFullPath(final String fullPath) {
        try {
            File file = new File(fullPath);
            final String content = FileUtil.loadFile(file, true);
            myFiles = CodegenTestFiles.create(file.getName(), content, myEnvironment.getProject());
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
        blackBoxFile(filename, "OK");
    }

    protected void blackBoxFile(String filename, String expected) {
        blackBoxFile(filename, expected, false);
    }

    protected void blackBoxFile(String filename, String expected, boolean classPathInTheSameClassLoader) {
        loadFile(filename);
        blackBox(expected, classPathInTheSameClassLoader);
    }

    protected void blackBoxFileByFullPath(String filename) {
        loadFileByFullPath(filename);
        blackBox();
    }

    protected void blackBoxMultiFile(String... filenames) {
        loadFiles(filenames);
        blackBox();
    }

    protected void blackBoxMultiFile(String expected, boolean classPathInTheSameClassLoader, String... filenames) {
        loadFiles(filenames);
        blackBox(expected, classPathInTheSameClassLoader);
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

    private Constructor getConstructor(@NotNull Class<?> clazz, org.jetbrains.asm4.commons.Method method) {
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
        blackBox("OK");
    }

    protected void blackBox(String expected) {
        blackBox(expected, false);
    }

    protected void blackBox(String expected, boolean classPathInTheSameClassLoader) {
        GenerationState state = generateClassesInFileGetState();

        GeneratedClassLoader loader = createClassLoader(state.getFactory(), classPathInTheSameClassLoader);

        String r;

        try {
            if (myFiles.isScript()) {
                String scriptClassName = ScriptNameUtil.classNameForScript(myFiles.getPsiFile());
                Class<?> scriptClass = loader.loadClass(scriptClassName);

                Constructor constructor = getConstructor(scriptClass, state.getScriptCodegen().getScriptConstructorMethod());
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
                try {
                    Method method = namespaceClass.getMethod("box");
                    r = (String) method.invoke(null);
                    assertEquals(expected, r);
                }
                catch (NoSuchMethodException e) {
                    Method method = namespaceClass.getMethod("main",String[].class);
                    method.invoke(null,new Object[]{new String[0]});
                }
            }
        } catch (Error e) {
            System.out.println(generateToText());
            throw e;
        } catch (Throwable e) {
            System.out.println(generateToText());
            throw new RuntimeException(e);
        }
    }

    protected void blackBoxFileWithJava(@NotNull String ktFile) throws Exception {
        blackBoxFileWithJava(ktFile, false);
    }

    protected void blackBoxFileWithJava(@NotNull String ktFile, boolean classPathInTheSameClassLoader) throws Exception {
        File javaClassesTempDirectory = compileJava(ktFile.replaceFirst("\\.kt$", ".java"));

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), javaClassesTempDirectory));

        blackBoxFile(ktFile, "OK", classPathInTheSameClassLoader);
    }

    protected File compileJava(@NotNull String filename) throws IOException {
        File javaClassesTempDirectory = new File(FileUtil.getTempDirectory(), "java-classes");
        JetTestUtils.mkdirs(javaClassesTempDirectory);
        String classPath = "out/production/runtime" + File.pathSeparator + JetTestUtils.getAnnotationsJar().getPath();
        List<String> options = Arrays.asList(
                "-classpath", classPath,
                "-d", javaClassesTempDirectory.getPath()
        );

        File javaFile = new File("compiler/testData/codegen/" + filename);
        JetTestUtils.compileJavaFiles(Collections.singleton(javaFile), options);

        return javaClassesTempDirectory;
    }

    protected GeneratedClassLoader createClassLoader(ClassFileFactory codegens) {
        return createClassLoader(codegens, false);
    }

    protected GeneratedClassLoader createClassLoader(ClassFileFactory codegens, boolean classPathInTheSameClassLoader) {
        if (initializedClassLoader != null) {
            fail("Double initialization of class loader in same test");
        }

        List<URL> urls = Lists.newArrayList();
        for (File file : myEnvironment.getConfiguration().getList(JVMConfigurationKeys.CLASSPATH_KEY)) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        final URL[] urlsArray = urls.toArray(new URL[urls.size()]);

        if (!classPathInTheSameClassLoader) {
            ClassLoader parentClassLoader = new URLClassLoader(urlsArray, CodegenTestCase.class.getClassLoader());
            initializedClassLoader = new GeneratedClassLoader(codegens, parentClassLoader);
        }
        else {
            initializedClassLoader = new GeneratedClassLoader(codegens, CodegenTestCase.class.getClassLoader(), urlsArray);
        }

        return initializedClassLoader;
    }

    protected String generateToText() {
        if (alreadyGenerated == null)
            alreadyGenerated = generateCommon(ClassBuilderFactories.TEST, myEnvironment, myFiles);
        return alreadyGenerated.getFactory().createText();
    }

    @NotNull
    protected static GenerationState generateCommon(
            @NotNull ClassBuilderFactory classBuilderFactory,
            @NotNull JetCoreEnvironment environment,
            @NotNull CodegenTestFiles files
    ) {
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                environment.getProject(),
                files.getPsiFiles(),
                files.getScriptParameterTypes(),
                Predicates.<PsiFile>alwaysTrue());
        analyzeExhaust.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());
        CompilerConfiguration configuration = environment.getConfiguration();
        GenerationState state = new GenerationState(
                environment.getProject(), classBuilderFactory, Progress.DEAF, analyzeExhaust.getBindingContext(), files.getPsiFiles(),
                configuration.get(JVMConfigurationKeys.BUILTIN_TO_JAVA_TYPES_MAPPING_KEY, BuiltinToJavaTypesMapping.ENABLED),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, true),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, true),
                /*generateDeclaredClasses = */true
        );
        KotlinCodegenFacade.compileCorrectFiles(state, StandardGenerationStrategy.INSTANCE, CompilationErrorHandler.THROW_EXCEPTION);
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
            generationState = generateCommon(ClassBuilderFactories.TEST, myEnvironment, myFiles);

            if (DxChecker.RUN_DX_CHECKER) {
                DxChecker.check(generationState.getFactory());
            }

        } catch (Throwable e) {
            System.out.println(generateToText());
            throw ExceptionUtils.rethrow(e);
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
        long toleratedDifference = SystemInfo.isWindows ? 15 : 1;
        assertTrue("Difference with current time: " + diff + " (this test is a bad one: it may fail even if the generated code is correct)",
                   diff <= toleratedDifference);
    }

    protected Class loadImplementationClass(@NotNull ClassFileFactory codegens, final String name) {
        return loadClass(name, codegens);
    }
}
