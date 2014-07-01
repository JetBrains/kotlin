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
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.utils.UtilsPackage;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.jetbrains.jet.codegen.CodegenTestUtil.*;
import static org.jetbrains.jet.lang.resolve.java.PackageClassUtils.getPackageClassFqName;

public abstract class CodegenTestCase extends UsefulTestCase {
    protected JetCoreEnvironment myEnvironment;
    protected CodegenTestFiles myFiles;
    protected ClassFileFactory classFileFactory;
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
        return loadFileByFullPath(JetTestCaseBuilder.getTestDataPathBase() + "/codegen/" + name);
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

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".kt");
    }

    @NotNull
    protected String relativePath(@NotNull File file) {
        String stringToCut = "compiler/testData/codegen/";
        String systemIndependentPath = file.getPath().replace(File.separatorChar, '/');
        assert systemIndependentPath.startsWith(stringToCut) : "File path is not absolute: " + file;
        return systemIndependentPath.substring(stringToCut.length());
    }

    @NotNull
    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    protected GeneratedClassLoader createClassLoader(@NotNull ClassFileFactory factory) {
        if (initializedClassLoader != null) {
            fail("Double initialization of class loader in same test");
        }

        initializedClassLoader = new GeneratedClassLoader(factory, null, getClassPathURLs());
        return initializedClassLoader;
    }

    @NotNull
    protected GeneratedClassLoader generateAndCreateClassLoader() {
        ClassFileFactory factory = generateClassesInFile();
        GeneratedClassLoader loader = createClassLoader(factory);

        if (!verifyAllFilesWithAsm(factory, loader)) {
            fail("Verification failed: see exceptions above");
        }

        return loader;
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
        try {
            //add runtime library
            urls.add(ForTestCompileRuntime.runtimeJarForTests().toURI().toURL());
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
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
    protected Class<?> generatePackageClass() {
        FqName packageFqName = myFiles.getPsiFile().getPackageFqName();
        return generateClass(getPackageClassFqName(packageFqName).asString());
    }

    @NotNull
    protected Class<?> generatePackagePartClass() {
        String name = PackagePartClassUtils.getPackagePartInternalName(myFiles.getPsiFile());
        return generateClass(name);
    }

    @NotNull
    protected Class<?> generateClass(@NotNull String name) {
        try {
            return generateAndCreateClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException e) {
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
            }
            catch (Throwable e) {
                e.printStackTrace();
                System.err.println("Generating instructions as text...");
                try {
                    if (classFileFactory == null) {
                        System.out.println("Cannot generate text: exception was thrown during generation");
                    }
                    else {
                        System.out.println(classFileFactory.createText());
                    }
                }
                catch (Throwable e1) {
                    System.err.println("Exception thrown while trying to generate text, the actual exception follows:");
                    e1.printStackTrace();
                    System.err.println("-----------------------------------------------------------------------------");
                }
                fail("See exceptions above");
            }
        }
        return classFileFactory;
    }

    private static boolean verifyAllFilesWithAsm(ClassFileFactory factory, ClassLoader loader) {
        boolean noErrors = true;
        for (OutputFile file : factory.asList()) {
            noErrors &= verifyWithAsm(file, loader);
        }
        return noErrors;
    }

    private static boolean verifyWithAsm(@NotNull OutputFile file, ClassLoader loader) {
        ClassNode classNode = new ClassNode();
        new ClassReader(file.asByteArray()).accept(classNode, 0);

        SimpleVerifier verifier = new SimpleVerifier();
        verifier.setClassLoader(loader);
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(verifier);

        boolean noErrors = true;
        for (MethodNode method : classNode.methods) {
            try {
                analyzer.analyze(classNode.name, method);
            }
            catch (Throwable e) {
                System.out.println(file.asText());
                System.err.println(classNode.name + "::" + method.name + method.desc);

                //noinspection InstanceofCatchParameter
                if (e instanceof AnalyzerException) {
                    // Print the erroneous instruction
                    TraceMethodVisitor tmv = new TraceMethodVisitor(new Textifier());
                    ((AnalyzerException) e).node.accept(tmv);
                    PrintWriter pw = new PrintWriter(System.err);
                    tmv.p.print(pw);
                    pw.flush();
                }

                e.printStackTrace();
                noErrors = false;
            }
        }
        return noErrors;
    }

    @NotNull
    protected Method generateFunction() {
        Class<?> aClass = generatePackageClass();
        try {
            return findTheOnlyMethod(aClass);
        } catch (Error e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    @NotNull
    protected Method generateFunction(@NotNull String name) {
        return findDeclaredMethodByName(generatePackageClass(), name);
    }

    @NotNull
    public Class<? extends Annotation> loadAnnotationClassQuietly(@NotNull String fqName) {
        try {
            //noinspection unchecked
            return (Class<? extends Annotation>) initializedClassLoader.loadClass(fqName);
        }
        catch (ClassNotFoundException e) {
            throw UtilsPackage.rethrow(e);
        }
    }
}
