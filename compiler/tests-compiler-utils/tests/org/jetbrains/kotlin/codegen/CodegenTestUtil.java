/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.util.io.FileUtil;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.test.KtAssert;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.StringsKt;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicVerifier;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CodegenTestUtil {
    private CodegenTestUtil() {
    }

    @NotNull
    public static ClassFileFactory generateFiles(@NotNull KotlinCoreEnvironment environment, @NotNull CodegenTestFiles files) {
        return GenerationUtils.compileFiles(files.getPsiFiles(), environment).getFactory();
    }

    public static void assertThrows(
            @NotNull Method foo, @NotNull Class<? extends Throwable> exceptionClass, @Nullable Object instance, @NotNull Object... args
    ) throws IllegalAccessException {
        boolean caught = false;
        try {
            foo.invoke(instance, args);
        }
        catch (InvocationTargetException ex) {
            caught = exceptionClass.isInstance(ex.getTargetException());
        }
        KtAssert.assertTrue(String.format("Exception of class %s must be thrown", exceptionClass.getName()), caught);
    }

    @NotNull
    public static Method findDeclaredMethodByName(@NotNull Class<?> aClass, @NotNull String name) {
        Method result = findDeclaredMethodByNameOrNull(aClass, name);
        if (result == null) {
            throw new AssertionError("Method " + name + " is not found in " + aClass);
        }
        return result;
    }

    public static Method findDeclaredMethodByNameOrNull(@NotNull Class<?> aClass, @NotNull String name) {
        for (Method method : aClass.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    @NotNull
    public static List<String> prepareJavacOptions(
            @NotNull List<String> additionalClasspath,
            @NotNull List<String> additionalOptions,
            @NotNull File outDirectory,
            boolean isJava9Module
    ) {
        List<String> classpath = new ArrayList<>();
        classpath.add(ForTestCompileRuntime.runtimeJarForTests().getPath());
        classpath.add(ForTestCompileRuntime.reflectJarForTests().getPath());
        classpath.add(KtTestUtil.getAnnotationsJar().getPath());
        classpath.addAll(additionalClasspath);

        String classPathOrModulePath = isJava9Module ? "--module-path" : "-classpath";
        List<String> options = new ArrayList<>(Arrays.asList(
                classPathOrModulePath, StringsKt.join(classpath, File.pathSeparator),
                "-d", outDirectory.getPath()
        ));
        options.addAll(additionalOptions);
        return options;
    }


    @NotNull
    public static Method findTheOnlyMethod(@NotNull Class<?> aClass) {
        Method r = null;
        for (Method method : aClass.getMethods()) {
            if (method.getDeclaringClass().equals(Object.class)) {
                continue;
            }

            if (r != null) {
                throw new AssertionError("More than one public method in class " + aClass);
            }

            r = method;
        }
        if (r == null) {
            throw new AssertionError("No public methods in class " + aClass);
        }
        return r;
    }

    @Nullable
    public static Object getAnnotationAttribute(@NotNull Object annotation, @NotNull String name) {
        try {
            return annotation.getClass().getMethod(name).invoke(annotation);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    public static List<String> findJavaSourcesInDirectory(@NotNull File directory) {
        List<String> javaFilePaths = new ArrayList<>(1);

        FileUtil.processFilesRecursively(directory, file -> {
            if (file.isFile() && FilesKt.getExtension(file).equals(JavaFileType.DEFAULT_EXTENSION)) {
                javaFilePaths.add(file.getPath());
            }
            return true;
        });

        return javaFilePaths;
    }

    public static boolean verifyAllFilesWithAsm(ClassFileFactory factory, boolean reportProblems) {
        boolean noErrors = true;
        for (OutputFile file : ClassFileUtilsKt.getClassFiles(factory)) {
            noErrors &= verifyWithAsm(file, reportProblems);
        }
        return noErrors;
    }

    private static boolean verifyWithAsm(@NotNull OutputFile file, boolean reportProblems) {
        ClassNode classNode = new ClassNode();
        new ClassReader(file.asByteArray()).accept(classNode, 0);

        BasicVerifier verifier = new BasicVerifier();
        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);

        boolean noErrors = true;
        for (MethodNode method : classNode.methods) {
            try {
                analyzer.analyze(classNode.name, method);
            }
            catch (Throwable e) {
                if (reportProblems) {
                    try {
                        System.err.println(file.asText());
                        System.err.println(classNode.name + "::" + method.name + method.desc);
                    } catch (Throwable ex) {
                        // In FIR we have factory which can't print bytecode
                        //   and it throws exception otherwise. So we need
                        //   ignore that exception to report original one
                        // TODO: fix original problem
                    }

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
                }
                noErrors = false;
            }
        }
        return noErrors;
    }
}
