/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.PsiFile;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.inline.InlineCodegenUtil;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertTrue;

public class CodegenTestUtil {
    private CodegenTestUtil() {}

    @NotNull
    public static ClassFileFactory generateFiles(@NotNull JetCoreEnvironment environment, @NotNull CodegenTestFiles files) {
        AnalyzeExhaust analyzeExhaust = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                environment.getProject(),
                files.getPsiFiles(),
                Predicates.<PsiFile>alwaysTrue());
        analyzeExhaust.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());
        CompilerConfiguration configuration = environment.getConfiguration();
        BindingTraceContext forExtraDiagnostics = new BindingTraceContext();
        GenerationState state = new GenerationState(
                environment.getProject(), ClassBuilderFactories.TEST, Progress.DEAF,
                analyzeExhaust.getModuleDescriptor(), analyzeExhaust.getBindingContext(), files.getPsiFiles(),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, true),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, true),
                GenerationState.GenerateClassFilter.GENERATE_ALL,
                configuration.get(JVMConfigurationKeys.ENABLE_INLINE, InlineCodegenUtil.DEFAULT_INLINE_FLAG),
                null,
                null,
                forExtraDiagnostics,
                null);
        KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);

        // For JVM-specific errors
        AnalyzingUtils.throwExceptionOnErrors(forExtraDiagnostics.getBindingContext());

        return state.getFactory();
    }

    public static void assertThrows(@NotNull Method foo, @NotNull Class<? extends Throwable> exceptionClass,
            @Nullable Object instance, @NotNull Object... args) throws IllegalAccessException {
        boolean caught = false;
        try {
            foo.invoke(instance, args);
        }
        catch (InvocationTargetException ex) {
            caught = exceptionClass.isInstance(ex.getTargetException());
        }
        assertTrue(caught);
    }

    @NotNull
    public static Method findDeclaredMethodByName(@NotNull Class<?> aClass, @NotNull String name) {
        for (Method method : aClass.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new AssertionError("Method " + name + " is not found in class " + aClass);
    }

    public static void assertIsCurrentTime(long returnValue) {
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(returnValue - currentTime);
        long toleratedDifference = SystemInfo.isWindows ? 15 : 1;
        assertTrue("Difference with current time: " + diff + " (this test is a bad one: it may fail even if the generated code is correct)",
                diff <= toleratedDifference);
    }

    @NotNull
    public static File compileJava(@NotNull String filename, @NotNull String... additionalClasspath) {
        try {
            File javaClassesTempDirectory = JetTestUtils.tmpDir("java-classes");
            List<String> classpath = new ArrayList<String>();
            classpath.add(ForTestCompileRuntime.runtimeJarForTests().getPath());
            classpath.add(JetTestUtils.getAnnotationsJar().getPath());
            classpath.addAll(Arrays.asList(additionalClasspath));
            List<String> options = Arrays.asList(
                    "-classpath", KotlinPackage.join(classpath, File.pathSeparator, "", "", -1, ""),
                    "-d", javaClassesTempDirectory.getPath()
            );

            File javaFile = new File(JetTestCaseBuilder.getTestDataPathBase() + "/codegen/" + filename);
            JetTestUtils.compileJavaFiles(Collections.singleton(javaFile), options);

            return javaClassesTempDirectory;
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
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
            throw UtilsPackage.rethrow(e);
        }
    }
}
