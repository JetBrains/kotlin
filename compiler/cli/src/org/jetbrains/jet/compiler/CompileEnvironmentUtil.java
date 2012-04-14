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

package org.jetbrains.jet.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import jet.modules.AllModules;
import jet.modules.Module;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.compiler.messages.MessageCollector;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.*;

/**
 * @author abreslav
 */
public class CompileEnvironmentUtil {
    @Nullable
    public static File getUnpackedRuntimePath() {
        URL url = CompileEnvironment.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("file")) {
            return new File(url.getPath()).getParentFile().getParentFile();
        }
        return null;
    }

    @Nullable
    public static File getRuntimeJarPath() {
        URL url = CompileEnvironment.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("jar")) {
            String path = url.getPath();
            return new File(path.substring(path.indexOf(":") + 1, path.indexOf("!/")));
        }
        return null;
    }

    //public static void ensureRuntime(@NotNull JetCoreEnvironment env) {
    //    ensureJdkRuntime(env);
    //}

    public static void ensureKotlinRuntime(JetCoreEnvironment env) {
        if (JavaPsiFacade.getInstance(env.getProject()).findClass("jet.JetObject", GlobalSearchScope.allScope(env.getProject())) == null) {
            // TODO: prepend
            File kotlin = PathUtil.getDefaultRuntimePath();
            if (kotlin == null || !kotlin.exists()) {
                kotlin = getUnpackedRuntimePath();
                if (kotlin == null) kotlin = getRuntimeJarPath();
            }
            if (kotlin == null) {
                throw new IllegalStateException("kotlin runtime not found");
            }
            env.addToClasspath(kotlin);
        }
    }

    public static void ensureJdkRuntime(JetCoreEnvironment env) {
        if (JavaPsiFacade.getInstance(env.getProject()).findClass("java.lang.Object", GlobalSearchScope.allScope(env.getProject())) == null) {
            // TODO: prepend
            env.addToClasspath(findRtJar());
        }
    }

    public static File findRtJar() {
        String javaHome = System.getProperty("java.home");
        if ("jre".equals(new File(javaHome).getName())) {
            javaHome = new File(javaHome).getParent();
        }

        File rtJar = findRtJar(javaHome);

        if (rtJar == null || !rtJar.exists()) {
            throw new CompileEnvironmentException("No JDK rt.jar found under " + javaHome);
        }

        return rtJar;
    }

    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }

        File classesJar = new File(new File(javaHome).getParentFile().getAbsolutePath(), "Classes/classes.jar");
        if (classesJar.exists()) {
            return classesJar;
        }
        return null;
    }

    public static void ensureRuntime(JetCoreEnvironment environment, CompilerDependencies compilerDependencies) {
        if (compilerDependencies.getCompilerSpecialMode() == CompilerSpecialMode.REGULAR) {
            ensureJdkRuntime(environment);
            ensureKotlinRuntime(environment);
        }
        else if (compilerDependencies.getCompilerSpecialMode() == CompilerSpecialMode.JDK_HEADERS) {
            ensureJdkRuntime(environment);
        }
        else if (compilerDependencies.getCompilerSpecialMode() == CompilerSpecialMode.STDLIB) {
            ensureJdkRuntime(environment);
        }
        else if (compilerDependencies.getCompilerSpecialMode() == CompilerSpecialMode.BUILTINS) {
            // nop
        }
        else {
            throw new IllegalStateException("unknown mode: " + compilerDependencies.getCompilerSpecialMode());
        }
    }

    //private CompileEnvironment copyEnvironment(boolean verbose) {
    //    CompileEnvironment compileEnvironment = new CompileEnvironment(messageRenderer, verbose, compilerDependencies);
    //    compileEnvironment.setIgnoreErrors(ignoreErrors);
    //    compileEnvironment.setErrorStream(errorStream);
    //    // copy across any compiler plugins
    //    compileEnvironment.getEnvironment().getCompilerPlugins().addAll(environment.getCompilerPlugins());
    //    return compileEnvironment;
    //}
    //
    public static List<Module> loadModuleScript(String moduleFile, MessageCollector messageCollector) {
        Disposable disposable = new Disposable() {
            @Override
            public void dispose() {

            }
        };
        CompilerDependencies dependencies = CompilerDependencies.compilerDependenciesForProduction(CompilerSpecialMode.REGULAR);
        JetCoreEnvironment scriptEnvironment = new JetCoreEnvironment(disposable, dependencies);
        ensureRuntime(scriptEnvironment, dependencies);
        scriptEnvironment.addSources(moduleFile);

        GenerationState generationState = KotlinToJVMBytecodeCompiler
                .analyzeAndGenerate(scriptEnvironment, dependencies, messageCollector, false);
        if (generationState == null) {
            return null;
        }

        List<Module> modules = runDefineModules(dependencies, moduleFile, generationState.getFactory());

        Disposer.dispose(disposable);
        return modules;
    }

    private static List<Module> runDefineModules(CompilerDependencies compilerDependencies, String moduleFile, ClassFileFactory factory) {
        File stdlibJar = compilerDependencies.getRuntimeJar();
        GeneratedClassLoader loader;
        if (stdlibJar != null) {
            try {
                loader = new GeneratedClassLoader(factory, new URLClassLoader(new URL[]{stdlibJar.toURI().toURL()}, AllModules.class.getClassLoader()));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            loader = new GeneratedClassLoader(factory, CompileEnvironment.class.getClassLoader());
        }
        try {
            Class namespaceClass = loader.loadClass(JvmAbi.PACKAGE_CLASS);
            final Method method = namespaceClass.getDeclaredMethod("project");
            if (method == null) {
                throw new CompileEnvironmentException("Module script " + moduleFile + " must define project() function");
            }

            method.setAccessible(true);
            method.invoke(null);

            ArrayList<Module> answer = new ArrayList<Module>(AllModules.modules.get());
            AllModules.modules.get().clear();
            return answer;
        }
        catch (Exception e) {
            throw new ModuleExecutionException(e);
        }
        finally {
            loader.dispose();
        }
    }

    public static void writeToJar(ClassFileFactory factory, final OutputStream fos, @Nullable FqName mainClass, boolean includeRuntime) {
        try {
            Manifest manifest = new Manifest();
            final Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Created-By", "JetBrains Kotlin");
            if (mainClass != null) {
                mainAttributes.putValue("Main-Class", mainClass.getFqName());
            }
            JarOutputStream stream = new JarOutputStream(fos, manifest);
            try {
                for (String file : factory.files()) {
                    stream.putNextEntry(new JarEntry(file));
                    stream.write(factory.asBytes(file));
                }
                if (includeRuntime) {
                    writeRuntimeToJar(stream);
                }
            }
            finally {
                stream.close();
                fos.close();
            }

        } catch (IOException e) {
            throw new CompileEnvironmentException("Failed to generate jar file", e);
        }
    }

    private static void writeRuntimeToJar(final JarOutputStream stream) throws IOException {
        final File unpackedRuntimePath = getUnpackedRuntimePath();
        if (unpackedRuntimePath != null) {
            FileUtil.processFilesRecursively(unpackedRuntimePath, new Processor<File>() {
                @Override
                public boolean process(File file) {
                    if (file.isDirectory()) return true;
                    final String relativePath = FileUtil.getRelativePath(unpackedRuntimePath, file);
                    try {
                        stream.putNextEntry(new JarEntry(FileUtil.toSystemIndependentName(relativePath)));
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            FileUtil.copy(fis, stream);
                        }
                        finally {
                            fis.close();
                        }
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
            });
        }
        else {
            File runtimeJarPath = getRuntimeJarPath();
            if (runtimeJarPath != null) {
                JarInputStream jis = new JarInputStream(new FileInputStream(runtimeJarPath));
                try {
                    while (true) {
                        JarEntry e = jis.getNextJarEntry();
                        if (e == null) {
                            break;
                        }
                        if (FileUtil.getExtension(e.getName()).equals("class")) {
                            stream.putNextEntry(e);
                            FileUtil.copy(jis, stream);
                        }
                    }
                } finally {
                    jis.close();
                }
            }
            else {
                throw new CompileEnvironmentException("Couldn't find runtime library");
            }
        }
    }

    public static void writeToOutputDirectory(ClassFileFactory factory, final String outputDir) {
        List<String> files = factory.files();
        for (String file : files) {
            File target = new File(outputDir, file);
            try {
                FileUtil.writeToFile(target, factory.asBytes(file));
            } catch (IOException e) {
                throw new CompileEnvironmentException(e);
            }
        }
    }
}
