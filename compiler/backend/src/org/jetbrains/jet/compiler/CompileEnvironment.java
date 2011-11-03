package org.jetbrains.jet.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import jet.modules.IModuleBuilder;
import jet.modules.IModuleSetBuilder;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
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
        final File unpackedRuntimePath = getUnpackedRuntimePath();
        if (unpackedRuntimePath != null) {
            myEnvironment.addToClasspath(unpackedRuntimePath);
        }
        else {
            final File runtimeJarPath = getRuntimeJarPath();
            if (runtimeJarPath != null && runtimeJarPath.exists()) {
                myEnvironment.addToClasspath(runtimeJarPath);
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
    
    public static File findRtJar(boolean failOnError) {
        String javaHome = System.getenv("JAVA_HOME");
        File rtJar;
        if (javaHome == null) {
            rtJar = findActiveRtJar(failOnError);

            if(rtJar == null && failOnError) {
                throw new CompileEnvironmentException("JAVA_HOME environment variable needs to be defined");
            }
        }
        else {
            rtJar = findRtJar(javaHome);
        }

        if ((rtJar == null || !rtJar.exists()) && failOnError) {
            throw new CompileEnvironmentException("No rt.jar found under JAVA_HOME=" + javaHome);
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

    public static File findActiveRtJar(boolean failOnError) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (systemClassLoader instanceof URLClassLoader) {
            URLClassLoader loader = (URLClassLoader) systemClassLoader;
            for (URL url: loader.getURLs()) {
                if("file".equals(url.getProtocol())) {
                    if(url.getFile().endsWith("/lib/rt.jar")) {
                        return new File(url.getFile());
                    }
                    if(url.getFile().endsWith("/Classes/classes.jar")) {
                        return new File(url.getFile()).getAbsoluteFile();
                    }
                }
            }
            if (failOnError) {
                throw new CompileEnvironmentException("Could not find rt.jar in system class loader: " + StringUtil.join(loader.getURLs(), new Function<URL, String>() {
                    @Override
                    public String fun(URL url) {
                        return url.toString();
                    }
                }, ", "));
            }
        }
        else if (failOnError) {
            throw new CompileEnvironmentException("System class loader is not an URLClassLoader: " + systemClassLoader);
        }
        return null;
    }

    public void compileModuleScript(String moduleFile) {
        final IModuleSetBuilder moduleSetBuilder = loadModuleScript(moduleFile);
        if (moduleSetBuilder == null) {
            return;
        }

        final String directory = new File(moduleFile).getParent();
        for (IModuleBuilder moduleBuilder : moduleSetBuilder.getModules()) {
            ClassFileFactory moduleFactory = compileModule(moduleBuilder, directory);
            writeToJar(moduleFactory, new File(directory, moduleBuilder.getModuleName() + ".jar").getPath(), null, true);
        }
    }

    public IModuleSetBuilder loadModuleScript(String moduleFile) {
        CompileSession scriptCompileSession = new CompileSession(myEnvironment);
        scriptCompileSession.addSources(moduleFile);

        URL url = CompileEnvironment.class.getClassLoader().getResource("ModuleBuilder.kt");
        if (url != null) {
            String path = url.getPath();
            if (path.startsWith("file:")) {
                path = path.substring(5);
            }
            final VirtualFile vFile = myEnvironment.getJarFileSystem().findFileByPath(path);
            if (vFile == null) {
                throw new CompileEnvironmentException("Couldn't load ModuleBuilder.kt from runtime jar: "+ url);
            }
            scriptCompileSession.addSources(vFile);
        }
        else {
            // building from source
            final String homeDirectory = getHomeDirectory();
            final File file = new File(homeDirectory, "stdlib/ktSrc/ModuleBuilder.kt");
            scriptCompileSession.addSources(myEnvironment.getLocalFileSystem().findFileByPath(file.getPath()));
        }

        if (!scriptCompileSession.analyze(myErrorStream)) {
            return null;
        }
        final ClassFileFactory factory = scriptCompileSession.generate();

        return runDefineModules(moduleFile, factory);
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
                throw new CompileEnvironmentException("Module script " + moduleFile + " must define a defineModules() method");
            }
            return moduleSetBuilder;
        } catch (Exception e) {
            throw new CompileEnvironmentException(e);
        }
    }

    public ClassFileFactory compileModule(IModuleBuilder moduleBuilder, String directory) {
        CompileSession moduleCompileSession = new CompileSession(myEnvironment);
        for (String sourceFile : moduleBuilder.getSourceFiles()) {
            moduleCompileSession.addSources(new File(directory, sourceFile).getPath());
        }
        for (String classpathRoot : moduleBuilder.getClasspathRoots()) {
            myEnvironment.addToClasspath(new File(classpathRoot));
        }
        if (!moduleCompileSession.analyze(myErrorStream)) {
            return null;
        }
        return moduleCompileSession.generate();
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(CompileEnvironment.class, "/org/jetbrains/jet/compiler/CompileEnvironment.class")).getParentFile().getParentFile().getParent();
    }

    public static void writeToJar(ClassFileFactory factory, String jar, String mainClass, boolean includeRuntime) {
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

    public void compileBunchOfSources(String sourceFileOrDir, String jar, String outputDir) {
        CompileSession session = new CompileSession(myEnvironment);
        session.addSources(sourceFileOrDir);

        String mainClass = null;
        for (JetNamespace namespace : session.getSourceFileNamespaces()) {
            if (JetMainDetector.hasMain(namespace.getDeclarations())) {
                mainClass = namespace.getFQName() + ".namespace";
                break;
            }
        }
        if (!session.analyze(myErrorStream)) {
            return;
        }

        ClassFileFactory factory = session.generate();
        if (jar != null) {
            writeToJar(factory, jar, mainClass, true);
        }
        else if (outputDir != null) {
            writeToOutputDirectory(factory, outputDir);
        }
        else {
            throw new CompileEnvironmentException("Output directory or jar file is not specified - no files will be saved to the disk");
        }
    }

    private static void writeToOutputDirectory(ClassFileFactory factory, final String outputDir) {
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
