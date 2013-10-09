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

package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import jet.modules.AllModules;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.messages.MessageRenderer;
import org.jetbrains.jet.cli.common.modules.ModuleDescription;
import org.jetbrains.jet.cli.common.modules.ModuleXmlParser;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.utils.KotlinPaths;
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

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompileEnvironmentUtil {
    public static Disposable createMockDisposable() {
        return new Disposable() {
            @Override
            public void dispose() {
            }
        };
    }

    @Nullable
    private static File getRuntimeJarPath() {
        File runtimePath = PathUtil.getKotlinPathsForCompiler().getRuntimePath();
        return runtimePath.exists() ? runtimePath : null;
    }

    @NotNull
    public static ModuleChunk loadModuleDescriptions(KotlinPaths paths, String moduleDefinitionFile, MessageCollector messageCollector) {
        File file = new File(moduleDefinitionFile);
        if (!file.exists()) {
            messageCollector.report(ERROR, "Module definition file does not exist: " + moduleDefinitionFile, NO_LOCATION);
            return ModuleChunk.EMPTY;
        }
        String extension = FileUtilRt.getExtension(moduleDefinitionFile);
        if ("kts".equalsIgnoreCase(extension)) {
            return new ModuleChunk(loadModuleScript(paths, moduleDefinitionFile, messageCollector));
        }
        if ("xml".equalsIgnoreCase(extension)) {
            return new ModuleChunk(ContainerUtil.map(
                    ModuleXmlParser.parse(moduleDefinitionFile, messageCollector),
                    new Function<ModuleDescription, Module>() {
                        @Override
                        public Module fun(ModuleDescription description) {
                            return new DescriptionToModuleAdapter(description);
                        }
                    }));
        }
        messageCollector.report(ERROR, "Unknown module definition type: " + moduleDefinitionFile, NO_LOCATION);
        return ModuleChunk.EMPTY;
    }

    @NotNull
    private static List<Module> loadModuleScript(KotlinPaths paths, String moduleScriptFile, MessageCollector messageCollector) {
        Disposable disposable = new Disposable() {
            @Override
            public void dispose() {

            }
        };
        CompilerConfiguration configuration = new CompilerConfiguration();
        File runtimePath = paths.getRuntimePath();
        if (runtimePath.exists()) {
            configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, runtimePath);
        }
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, PathUtil.findRtJar());
        File jdkAnnotationsPath = paths.getJdkAnnotationsPath();
        if (jdkAnnotationsPath.exists()) {
            configuration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, jdkAnnotationsPath);
        }
        configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, moduleScriptFile);
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);

        List<Module> modules;
        try {
            JetCoreEnvironment scriptEnvironment = new JetCoreEnvironment(disposable, configuration);
            GenerationState generationState = KotlinToJVMBytecodeCompiler.analyzeAndGenerate(scriptEnvironment);
            if (generationState == null) {
                throw new CompileEnvironmentException("Module script " + moduleScriptFile + " analyze failed:\n" +
                                                      loadModuleScriptText(moduleScriptFile));
            }

            modules = runDefineModules(paths, moduleScriptFile, generationState.getFactory());
        }
        finally {
            Disposer.dispose(disposable);
        }

        if (modules == null) {
            throw new CompileEnvironmentException("Module script " + moduleScriptFile + " compilation failed");
        }

        if (modules.isEmpty()) {
            throw new CompileEnvironmentException("No modules where defined by " + moduleScriptFile);
        }
        return modules;
    }

    private static List<Module> runDefineModules(KotlinPaths paths, String moduleFile, ClassFileFactory factory) {
        File stdlibJar = paths.getRuntimePath();
        GeneratedClassLoader loader;
        if (stdlibJar.exists()) {
            try {
                loader = new GeneratedClassLoader(factory, new URLClassLoader(new URL[]{stdlibJar.toURI().toURL()},
                                                                              AllModules.class.getClassLoader()));
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            loader = new GeneratedClassLoader(factory, KotlinToJVMBytecodeCompiler.class.getClassLoader());
        }
        try {
            Class namespaceClass = loader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
            Method method = namespaceClass.getDeclaredMethod("project");
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
    public static void writeToJar(ClassFileFactory factory, OutputStream fos, @Nullable FqName mainClass, boolean includeRuntime) {
        try {
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Created-By", "JetBrains Kotlin");
            if (mainClass != null) {
                mainAttributes.putValue("Main-Class", mainClass.asString());
            }
            JarOutputStream stream = new JarOutputStream(fos, manifest);
            for (String file : factory.files()) {
                stream.putNextEntry(new JarEntry(file));
                stream.write(factory.asBytes(file));
            }
            if (includeRuntime) {
                writeRuntimeToJar(stream);
            }
            stream.finish();
        }
        catch (IOException e) {
            throw new CompileEnvironmentException("Failed to generate jar file", e);
        }
    }

    private static void writeRuntimeToJar(final JarOutputStream stream) throws IOException {
        File runtimeJarPath = getRuntimeJarPath();
        if (runtimeJarPath != null) {
            JarInputStream jis = new JarInputStream(new FileInputStream(runtimeJarPath));
            try {
                while (true) {
                    JarEntry e = jis.getNextJarEntry();
                    if (e == null) {
                        break;
                    }
                    if (FileUtilRt.extensionEquals(e.getName(), "class")) {
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

    public static void writeToOutputDirectory(ClassFileFactory factory, @NotNull File outputDir) {
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

    // Used for debug output only
    private static String loadModuleScriptText(String moduleScriptFile) {
        String moduleScriptText;
        try {
            moduleScriptText = FileUtil.loadFile(new File(moduleScriptFile));
        }
        catch (IOException e) {
            moduleScriptText = "Can't load module script text:\n" + MessageRenderer.PLAIN.renderException(e);
        }
        return moduleScriptText;
    }

    private static class DescriptionToModuleAdapter implements Module {
        private final ModuleDescription description;

        public DescriptionToModuleAdapter(ModuleDescription description) {
            this.description = description;
        }

        @Override
        public String getModuleName() {
            return description.getModuleName();
        }

        @Override
        public String getOutputDirectory() {
            return description.getOutputDir();
        }

        @Override
        public List<String> getSourceFiles() {
            return description.getSourceFiles();
        }

        @Override
        public List<String> getClasspathRoots() {
            return description.getClasspathRoots();
        }

        @Override
        public List<String> getAnnotationsRoots() {
            return description.getAnnotationsRoots();
        }
    }
}
