/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.StringsKt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CodegenTestUtil {
    private CodegenTestUtil() {}

    @NotNull
    public static ClassFileFactory generateFiles(@NotNull KotlinCoreEnvironment environment, @NotNull CodegenTestFiles files) {
        return generateFiles(environment, files, ClassBuilderFactories.TEST);
    }
    
    @NotNull
    public static ClassFileFactory generateFiles(
            @NotNull KotlinCoreEnvironment environment, 
            @NotNull CodegenTestFiles files,
            @NotNull ClassBuilderFactory classBuilderFactory) {
        AnalysisResult analysisResult = JvmResolveUtil.analyzeAndCheckForErrors(files.getPsiFiles(), environment);
        analysisResult.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analysisResult.getBindingContext());
        CompilerConfiguration configuration = environment.getConfiguration();
        GenerationState state = new GenerationState(
                environment.getProject(),
                classBuilderFactory,
                analysisResult.getModuleDescriptor(),
                analysisResult.getBindingContext(),
                files.getPsiFiles(),
                configuration
        );

        if (analysisResult.getShouldGenerateCode()) {
            KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);
        }

        // For JVM-specific errors
        AnalyzingUtils.throwExceptionOnErrors(state.getCollectedExtraJvmDiagnostics());

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
    public static File compileJava(
            @NotNull List<String> fileNames,
            @NotNull List<String> additionalClasspath,
            @NotNull List<String> additionalOptions
    ) {
        try {
            File javaClassesTempDirectory = KotlinTestUtils.tmpDir("java-classes");
            List<String> classpath = new ArrayList<String>();
            classpath.add(ForTestCompileRuntime.runtimeJarForTests().getPath());
            classpath.add(ForTestCompileRuntime.reflectJarForTests().getPath());
            classpath.add(KotlinTestUtils.getAnnotationsJar().getPath());
            classpath.addAll(additionalClasspath);

            List<String> options = new ArrayList<String>(Arrays.asList(
                    "-classpath", StringsKt.join(classpath, File.pathSeparator),
                    "-d", javaClassesTempDirectory.getPath()
            ));
            options.addAll(additionalOptions);

            List<File> fileList = CollectionsKt.map(fileNames, new Function1<String, File>() {
                @Override
                public File invoke(String input) {
                    return new File(input);
                }
            });

            KotlinTestUtils.compileJavaFiles(fileList, options);

            return javaClassesTempDirectory;
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
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
            throw ExceptionUtilsKt.rethrow(e);
        }
    }
}
