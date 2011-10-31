package org.jetbrains.jet.cli;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import jet.modules.IModuleBuilder;
import jet.modules.IModuleSetBuilder;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.*;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class KotlinCompiler {
    public static class Arguments {
        @Argument(value = "output", description = "output directory")
        public String outputDir;
        @Argument(value = "jar", description = "jar file name")
        public String jar;
        @Argument(value = "src", description = "source file or directory")
        public String src;
        @Argument(value = "module", description = "module to compile")
        public String module;
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Arguments arguments = new Arguments();
        try {
            Args.parse(arguments, args);
        }
        catch (Throwable t) {
            System.out.println("Usage: KotlinCompiler [-output <outputDir>|-jar <jarFileName>] -src <filename or dirname>");
            t.printStackTrace();
            return;
        }

        Disposable root = new Disposable() {
            @Override
            public void dispose() {
            }
        };
        JetCoreEnvironment environment = new JetCoreEnvironment(root);

        File rtJar = initJdk();
        if (rtJar == null) return;

        environment.addToClasspath(rtJar);
        final File unpackedRuntimePath = getUnpackedRuntimePath();
        if (unpackedRuntimePath != null) {
            environment.addToClasspath(unpackedRuntimePath);
        }
        else {
            final File runtimeJarPath = getRuntimeJarPath();
            if (runtimeJarPath != null && runtimeJarPath.exists()) {
                environment.addToClasspath(runtimeJarPath);
            }
            else {
                System.out.println("No runtime library found");
                return;
            }
        }

        if (arguments.module != null) {
            compileModuleScript(environment, arguments.module);
            return;
        }

        CompileSession session = new CompileSession(environment);
        session.addSources(arguments.src);

        String mainClass = null;
        for (JetNamespace namespace : session.getSourceFileNamespaces()) {
            if (JetMainDetector.hasMain(namespace.getDeclarations())) {
                mainClass = namespace.getFQName() + ".namespace";
                break;
            }
        }
        if (!session.analyze()) {
            return;
        }

        ClassFileFactory factory = session.generate();
        if (arguments.jar != null) {
            writeToJar(factory, arguments.jar, mainClass, true);
        }
        else if (arguments.outputDir != null) {
            writeToOutputDirectory(factory, arguments.outputDir);
        }
        else {
            System.out.println("Output directory or jar file is not specified - no files will be saved to the disk");
        }
    }

    private static void compileModuleScript(JetCoreEnvironment environment, String moduleFile) {
        CompileSession scriptCompileSession = new CompileSession(environment);
        scriptCompileSession.addSources(moduleFile);

        URL url = KotlinCompiler.class.getClassLoader().getResource("ModuleBuilder.kt");
        if (url != null) {
            String path = url.getPath();
            if (path.startsWith("file:")) {
                path = path.substring(5);
            }
            final VirtualFile vFile = environment.getJarFileSystem().findFileByPath(path);
            if (vFile == null) {
                System.out.println("Couldn't load ModuleBuilder.kt from runtime jar: "+ url);
                return;
            }
            scriptCompileSession.addSources(vFile);
        }
        else {
            // building from source
            final String homeDirectory = getHomeDirectory();
            final File file = new File(homeDirectory, "stdlib/ktSrc/ModuleBuilder.kt");
            scriptCompileSession.addSources(environment.getLocalFileSystem().findFileByPath(file.getPath()));
        }

        if (!scriptCompileSession.analyze()) {
            return;
        }
        final ClassFileFactory factory = scriptCompileSession.generate();

        final IModuleSetBuilder moduleSetBuilder = runDefineModules(moduleFile, factory);

        for (IModuleBuilder moduleBuilder : moduleSetBuilder.getModules()) {
            compileModule(environment, moduleBuilder, new File(moduleFile).getParent());
        }
    }

    private static IModuleSetBuilder runDefineModules(String moduleFile, ClassFileFactory factory) {
        GeneratedClassLoader loader = new GeneratedClassLoader(factory);
        try {
            Class moduleSetBuilderClass = loader.loadClass("kotlin.modules.ModuleSetBuilder");
            final IModuleSetBuilder moduleSetBuilder = (IModuleSetBuilder) moduleSetBuilderClass.newInstance();

            Class namespaceClass = loader.loadClass("namespace");
            final Method[] methods = namespaceClass.getMethods();
            boolean modulesDefined = false;
            for (Method method : methods) {
                if (method.getName().equals("defineModules")) {
                    method.invoke(null, moduleSetBuilder);
                    modulesDefined = true;
                    break;
                }
            }
            if (!modulesDefined) {
                System.out.println("Module script " + moduleFile + " must define a defineModules() method");
                return null;
            }
            return moduleSetBuilder;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void compileModule(JetCoreEnvironment environment, IModuleBuilder moduleBuilder, String directory) {
        CompileSession moduleCompileSession = new CompileSession(environment);
        for (String sourceFile : moduleBuilder.getSourceFiles()) {
            moduleCompileSession.addSources(new File(directory, sourceFile).getPath());
        }
        for (String classpathRoot : moduleBuilder.getClasspathRoots()) {
            environment.addToClasspath(new File(classpathRoot));
        }
        if (!moduleCompileSession.analyze()) {
            return;
        }
        ClassFileFactory factory = moduleCompileSession.generate();
        writeToJar(factory, new File(directory, moduleBuilder.getModuleName() + ".jar").getPath(), null, true);
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(KotlinCompiler.class, "/org/jetbrains/jet/cli/KotlinCompiler.class")).getParentFile().getParentFile().getParent();
    }

    private static void writeToJar(ClassFileFactory factory, String jar, String mainClass, boolean includeRuntime) {
        try {
            Manifest manifest = new Manifest();
            final Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Created-By", "JetBrains Kotlin");
            if (mainClass != null) {
                mainAttributes.putValue("Main-Class", mainClass);
            }
            FileOutputStream fos = new FileOutputStream(jar);
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
            System.out.println("Failed to generate jar file: " + e.getMessage());
        }
    }
    
    private static File getUnpackedRuntimePath() {
        URL url = KotlinCompiler.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("file")) {
            return new File(url.getPath()).getParentFile().getParentFile();
        }
        return null;
    }

    private static File getRuntimeJarPath() {
        URL url = KotlinCompiler.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("jar")) {
            String path = url.getPath();
            return new File(path.substring(path.indexOf(":") + 1, path.indexOf("!/")));
        }
        return null;
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
                        } finally {
                            fis.close();
                        }
                    } catch (IOException e) {
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
                System.out.println("Couldn't find runtime library");
            }
        }
    }

    private static void writeToOutputDirectory(ClassFileFactory factory, final String outputDir) {
        List<String> files = factory.files();
        for (String file : files) {
            File target = new File(outputDir, file);
            try {
                FileUtil.writeToFile(target, factory.asBytes(file));
                System.out.println("Generated classfile: " + target);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static File initJdk() {
        String javaHome = System.getenv("JAVA_HOME");
        File rtJar = null;
        if (javaHome == null) {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if(systemClassLoader instanceof URLClassLoader) {
                URLClassLoader loader = (URLClassLoader) systemClassLoader;
                for(URL url: loader.getURLs()) {
                    if("file".equals(url.getProtocol())) {
                        if(url.getFile().endsWith("/lib/rt.jar")) {
                            rtJar = new File(url.getFile());
                            break;
                        }
                        if(url.getFile().endsWith("/Classes/classes.jar")) {
                            rtJar = new File(url.getFile()).getAbsoluteFile();
                            break;
                        }
                    }
                }
            }

            if(rtJar == null) {
                System.out.println("JAVA_HOME environment variable needs to be defined");
                return null;
            }
        }
        else {
            rtJar = findRtJar(javaHome);
        }

        if (rtJar == null || !rtJar.exists()) {
            System.out.print("No rt.jar found under JAVA_HOME=" + javaHome);
            return null;
        }
        return rtJar;
    }

    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }
        return null;
    }
}
