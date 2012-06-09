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

package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import jet.modules.AllModules;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.resolve.name.FqName;
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
import java.util.Collections;
import java.util.List;
import java.util.jar.*;

/**
 * @author abreslav
 */
public class CompileEnvironmentUtil {
    public static Disposable createMockDisposable() {
        return new Disposable() {
            @Override
            public void dispose() {
            }
        };
    }

    @Nullable
    public static File getUnpackedRuntimePath() {
        URL url = K2JVMCompileEnvironmentConfiguration.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("file")) {
            return new File(url.getPath()).getParentFile().getParentFile();
        }
        return null;
    }

    @Nullable
    public static File getRuntimeJarPath() {
        URL url = K2JVMCompileEnvironmentConfiguration.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("jar")) {
            String path = url.getPath();
            return new File(path.substring(path.indexOf(":") + 1, path.indexOf("!/")));
        }
        return null;
    }

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
        if (JavaPsiFacade.getInstance(env.getProject()).findClass("java.lang.Object", GlobalSearchScope.allScope(env.getProject())) ==
            null) {
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

    public static List<Module> loadModuleScript(String moduleScriptFile, MessageCollector messageCollector) {
        Disposable disposable = new Disposable() {
            @Override
            public void dispose() {

            }
        };
        CompilerDependencies dependencies = CompilerDependencies.compilerDependenciesForProduction(CompilerSpecialMode.REGULAR);
        JetCoreEnvironment scriptEnvironment = JetCoreEnvironment.getCoreEnvironmentForJVM(disposable, dependencies);
        scriptEnvironment.addSources(moduleScriptFile);

        GenerationState generationState = KotlinToJVMBytecodeCompiler
                .analyzeAndGenerate(new K2JVMCompileEnvironmentConfiguration(scriptEnvironment, messageCollector, false, Collections.<String>emptyList()), false);
        if (generationState == null) {
            return null;
        }

        List<Module> modules = runDefineModules(dependencies, moduleScriptFile, generationState.getFactory());

        Disposer.dispose(disposable);

        if (modules == null) {
            throw new CompileEnvironmentException("Module script " + moduleScriptFile + " compilation failed");
        }

        if (modules.isEmpty()) {
            throw new CompileEnvironmentException("No modules where defined by " + moduleScriptFile);
        }
        return modules;
    }

    private static List<Module> runDefineModules(CompilerDependencies compilerDependencies, String moduleFile, ClassFileFactory factory) {
        File stdlibJar = compilerDependencies.getRuntimeJar();
        GeneratedClassLoader loader;
        if (stdlibJar != null) {
            try {
                loader = new GeneratedClassLoader(factory, new URLClassLoader(new URL[]{stdlibJar.toURI().toURL()},
                                                                              AllModules.class.getClassLoader()));
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            loader = new GeneratedClassLoader(factory, K2JVMCompileEnvironmentConfiguration.class.getClassLoader());
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

    // TODO: includeRuntime should be not a flag but a path to runtime
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
            for (String file : factory.files()) {
                stream.putNextEntry(new JarEntry(file));
                stream.write(factory.asBytes(file));
            }
            if (includeRuntime) {
                writeRuntimeToJar(stream);
            }
            stream.close();
        }
        catch (IOException e) {
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
                }
                finally {
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
            }
            catch (IOException e) {
                throw new CompileEnvironmentException(e);
            }
        }
    }

    /**
     * Add path specified to the compilation environment.
     *
     * @param environment compilation environment to add to
     * @param paths       paths to add
     */
    public static void addToClasspath(JetCoreEnvironment environment, File... paths) {
        for (File path : paths) {
            if (!path.exists()) {
                throw new CompileEnvironmentException("'" + path + "' does not exist");
            }
            environment.addToClasspath(path);
        }
    }

    /**
     * Add path specified to the compilation environment.
     *
     * @param environment compilation environment to add to
     * @param paths       paths to add
     */
    public static void addToClasspath(JetCoreEnvironment environment, String... paths) {
        for (String path : paths) {
            addToClasspath(environment, new File(path));
        }
    }

    public static void addSourcesFromModuleToEnvironment(@NotNull JetCoreEnvironment environment,
            @NotNull Module module,
            @NotNull File moduleDirectory) {
        for (String sourceFile : module.getSourceFiles()) {
            File source = new File(sourceFile);
            if (!source.isAbsolute()) {
                source = new File(moduleDirectory, sourceFile);
            }

            if (!source.exists()) {
                throw new CompileEnvironmentException("'" + source + "' does not exist");
            }

            environment.addSources(source.getPath());
        }
    }
}
