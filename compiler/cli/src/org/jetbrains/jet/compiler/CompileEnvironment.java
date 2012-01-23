package org.jetbrains.jet.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Processor;
import jet.modules.AllModules;
import jet.modules.Module;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.jar.*;

/**
 * The environment for compiling a bunch of source files or
 *
 * @author yole
 */
public class CompileEnvironment {
    private JetCoreEnvironment myEnvironment;
    private final Disposable myRootDisposable;
    private PrintStream myErrorStream = System.out;

    public CompileEnvironment() {
        myRootDisposable = new Disposable() {
            @Override
            public void dispose() {
            }
        };
        myEnvironment = new JetCoreEnvironment(myRootDisposable);
    }

    public void setErrorStream(PrintStream errorStream) {
        myErrorStream = errorStream;
    }

    public void dispose() {
        Disposer.dispose(myRootDisposable);
    }

    public boolean initializeKotlinRuntime() {
        return initializeKotlinRuntime(myEnvironment);
    }

    public static boolean initializeKotlinRuntime(JetCoreEnvironment environment) {
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
                return false;
            }
        }
        return true;
    }

    public static File getUnpackedRuntimePath() {
        URL url = CompileEnvironment.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("file")) {
            return new File(url.getPath()).getParentFile().getParentFile();
        }
        return null;
    }

    public static File getRuntimeJarPath() {
        URL url = CompileEnvironment.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("jar")) {
            String path = url.getPath();
            return new File(path.substring(path.indexOf(":") + 1, path.indexOf("!/")));
        }
        return null;
    }

    public void setJavaRuntime(File rtJarPath) {
        myEnvironment.addToClasspath(rtJarPath);
    }
    
    public static File findRtJar() {
        String javaHome = System.getProperty("java.home");
        if ("jre".equals(new File(javaHome).getName())) {
            javaHome = new File(javaHome).getParent();
        }

        File rtJar = findRtJar(javaHome);

        if (rtJar == null || !rtJar.exists()) {
            throw new CompileEnvironmentException("No JDK rt.jar found under" + javaHome);
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

    public void compileModuleScript(String moduleFile, String jarPath, boolean jarRuntime) {
        final List<Module> modules = loadModuleScript(moduleFile);

        if (modules == null) {
            throw new CompileEnvironmentException("Module script " + moduleFile + " compilation failed");
        }

        final String directory = new File(moduleFile).getParent();
        for (Module moduleBuilder : modules) {
            ClassFileFactory moduleFactory = compileModule(moduleBuilder, directory);
            final String path = jarPath != null ? jarPath : new File(directory, moduleBuilder.getModuleName() + ".jar").getPath();
            try {
                writeToJar(moduleFactory, new FileOutputStream(path), null, jarRuntime);
            } catch (FileNotFoundException e) {
                throw new CompileEnvironmentException("Invalid jar path " + path, e);
            }
        }
    }

    public List<Module> loadModuleScript(String moduleFile) {
        CompileSession scriptCompileSession = new CompileSession(myEnvironment);
        scriptCompileSession.addSources(moduleFile);
        scriptCompileSession.addStdLibSources(true);

        if (!scriptCompileSession.analyze(myErrorStream)) {
            return null;
        }
        final ClassFileFactory factory = scriptCompileSession.generate();

        return runDefineModules(moduleFile, factory);
    }

    private static List<Module> runDefineModules(String moduleFile, ClassFileFactory factory) {
        GeneratedClassLoader loader = new GeneratedClassLoader(factory);
        try {
            Class namespaceClass = loader.loadClass(JvmAbi.PACKAGE_CLASS);
            final Method method = namespaceClass.getDeclaredMethod("project");
            if (method == null) {
                throw new CompileEnvironmentException("Module script " + moduleFile + " must define project() function");
            }

            method.setAccessible(true);
            method.invoke(null);

            return AllModules.modules;
        } catch (Exception e) {
            throw new CompileEnvironmentException(e);
        }
    }

    public ClassFileFactory compileModule(Module moduleBuilder, String directory) {
        CompileSession moduleCompileSession = new CompileSession(myEnvironment);
        if (!"stdlib".equals(moduleBuilder.getModuleName())) {
            moduleCompileSession.addStdLibSources(false);
        }

        for (String sourceFile : moduleBuilder.getSourceFiles()) {
            File source = new File(sourceFile);
            if (!source.isAbsolute()) {
                source = new File(directory, sourceFile);
            }

            moduleCompileSession.addSources(source.getPath());
        }
        for (String classpathRoot : moduleBuilder.getClasspathRoots()) {
            if (classpathRoot.contains("/stdlib/") || classpathRoot.contains("\\stdlib\\")) continue; // TODO: We have source-level dependency on stdlib for now
            myEnvironment.addToClasspath(new File(classpathRoot));
        }
        if (!moduleCompileSession.analyze(myErrorStream)) {
            return null;
        }
        return moduleCompileSession.generate();
    }

    public static void writeToJar(ClassFileFactory factory, final OutputStream fos, @Nullable String mainClass, boolean includeRuntime) {
        try {
            Manifest manifest = new Manifest();
            final Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Created-By", "JetBrains Kotlin");
            if (mainClass != null) {
                mainAttributes.putValue("Main-Class", mainClass);
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
                throw new CompileEnvironmentException("Couldn't find runtime library");
            }
        }
    }

    public boolean compileBunchOfSources(String sourceFileOrDir, String jar, String outputDir, boolean includeRuntime, boolean includeSources) {
        CompileSession session = new CompileSession(myEnvironment);
        session.addSources(sourceFileOrDir);
        if (includeSources) {
            session.addStdLibSources(false);
        }

        String mainClass = null;
        for (JetFile file : session.getSourceFileNamespaces()) {
            if (JetMainDetector.hasMain(file.getDeclarations())) {
                mainClass = JetPsiUtil.getFQName(file) + "." + JvmAbi.PACKAGE_CLASS;
                break;
            }
        }
        if (!session.analyze(myErrorStream)) {
            return false;
        }

        ClassFileFactory factory = session.generate();
        if (jar != null) {
            try {
                writeToJar(factory, new FileOutputStream(jar), mainClass, includeRuntime);
            } catch (FileNotFoundException e) {
                throw new CompileEnvironmentException("Invalid jar path " + jar, e);
            }
        }
        else if (outputDir != null) {
            writeToOutputDirectory(factory, outputDir);
        }
        else {
            throw new CompileEnvironmentException("Output directory or jar file is not specified - no files will be saved to the disk");
        }
        return true;
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
