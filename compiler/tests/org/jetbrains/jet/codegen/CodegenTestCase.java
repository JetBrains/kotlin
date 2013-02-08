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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static org.jetbrains.jet.codegen.CodegenTestUtil.*;

public abstract class CodegenTestCase extends UsefulTestCase {

    // for environment and classloader
    protected JetCoreEnvironment myEnvironment;
    protected CodegenTestFiles myFiles;

    private ClassFileFactory classFileFactory;
    protected GeneratedClassLoader initializedClassLoader;

    protected void createEnvironmentWithMockJdkAndIdeaAnnotations(@NotNull ConfigurationKind configurationKind) {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironment twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(), configurationKind);
    }

    @Override
    protected void tearDown() throws Exception {
        myFiles = null;
        myEnvironment = null;
        classFileFactory = null;

        if (initializedClassLoader != null) {
            initializedClassLoader.dispose();
            initializedClassLoader = null;
        }

        super.tearDown();
    }

    protected void loadText(@NotNull String text) {
        myFiles = CodegenTestFiles.create("a.kt", text, myEnvironment.getProject());
    }

    @NotNull
    protected String loadFile(@NotNull String name) {
        return loadFileByFullPath(JetParsingTest.getTestDataDir() + "/codegen/" + name);
    }

    @NotNull
    protected String loadFileByFullPath(@NotNull String fullPath) {
        try {
            File file = new File(fullPath);
            String content = FileUtil.loadFile(file, true);
            myFiles = CodegenTestFiles.create(file.getName(), content, myEnvironment.getProject());
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void loadFiles(@NotNull String... names) {
        myFiles = CodegenTestFiles.create(myEnvironment.getProject(), names);
    }

    protected void loadFilesByFullPath(@NotNull String... fullNames) {
        String[] names = new String[fullNames.length];
        for (int i = 0; i < fullNames.length; i++) {
            names[i] = fullNames[i].substring("compiler/testData/codegen/".length());
        }
        loadFiles(names);
    }

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".kt");
    }

    @NotNull
    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    protected void blackBoxFileByFullPath(@NotNull String filename) {
        loadFileByFullPath(filename);
        blackBox();
    }

    protected void blackBoxMultiFile(@NotNull String... filenames) {
        loadFiles(filenames);
        blackBox();
    }

    protected void blackBoxMultiFileByFullPath(@NotNull String... filenames) {
        loadFilesByFullPath(filenames);
        blackBox();
    }

    private void blackBox() {
        ClassFileFactory factory = generateClassesInFile();
        GeneratedClassLoader loader = createClassLoader(factory);

        JetFile firstFile = myFiles.getPsiFiles().get(0);
        String fqName = NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(firstFile)).getFqName().getFqName();

        try {
            Class<?> namespaceClass = loader.loadClass(fqName);
            Method method = namespaceClass.getMethod("box");

            String r = (String) method.invoke(null);
            assertEquals("OK", r);
        } catch (Throwable e) {
            System.out.println(generateToText());
            ExceptionUtils.rethrow(e);
        }
    }

    protected void blackBoxFileWithJavaByFullPath(@NotNull String ktFile) {
        blackBoxFileWithJava(ktFile.substring("compiler/testData/codegen/".length()));
    }

    protected void blackBoxFileWithJava(@NotNull String ktFile) {
        File javaClassesTempDirectory = CodegenTestUtil.compileJava(ktFile.replaceFirst("\\.kt$", ".java"));

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), javaClassesTempDirectory));

        blackBoxMultiFile(ktFile);
    }

    @NotNull
    protected GeneratedClassLoader createClassLoader(@NotNull ClassFileFactory factory) {
        if (initializedClassLoader != null) {
            fail("Double initialization of class loader in same test");
        }

        ClassLoader parentClassLoader = new URLClassLoader(getClassPathURLs(), CodegenTestCase.class.getClassLoader());
        initializedClassLoader = new GeneratedClassLoader(factory, parentClassLoader);
        return initializedClassLoader;
    }

    @NotNull
    protected URL[] getClassPathURLs() {
        List<URL> urls = Lists.newArrayList();
        for (File file : myEnvironment.getConfiguration().getList(JVMConfigurationKeys.CLASSPATH_KEY)) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return urls.toArray(new URL[urls.size()]);
    }

    @NotNull
    protected String generateToText() {
        if (classFileFactory == null) {
            classFileFactory = generateFiles(myEnvironment, myFiles);
        }
        return classFileFactory.createText();
    }

    @NotNull
    protected Class<?> generateNamespaceClass() {
        JvmClassName name = NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(myFiles.getPsiFile()));
        return generateClass(name.getFqName().getFqName());
    }

    @NotNull
    protected Class generateClass(@NotNull String name) {
        try {
            return createClassLoader(generateClassesInFile()).loadClass(name);
        } catch (ClassNotFoundException e) {
            fail("No class file was generated for: " + name);
            return null;
        }
    }

    @NotNull
    protected ClassFileFactory generateClassesInFile() {
        if (classFileFactory == null) {
            try {
                classFileFactory = generateFiles(myEnvironment, myFiles);

                if (DxChecker.RUN_DX_CHECKER) {
                    DxChecker.check(classFileFactory);
                }
            } catch (Throwable e) {
                System.out.println(generateToText());
                throw ExceptionUtils.rethrow(e);
            }
        }
        return classFileFactory;
    }

    @NotNull
    protected Method generateFunction() {
        Class<?> aClass = generateNamespaceClass();
        try {
            return findTheOnlyMethod(aClass);
        } catch (Error e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    @NotNull
    protected Method generateFunction(@NotNull String name) {
        Class<?> aClass = generateNamespaceClass();
        Method method = findDeclaredMethodByName(aClass, name);
        if (method == null) {
            throw new IllegalArgumentException("Couldn't find method " + name + " in class " + aClass);
        }
        return method;
    }
}
