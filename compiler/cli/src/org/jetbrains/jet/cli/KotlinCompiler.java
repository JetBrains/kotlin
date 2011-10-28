package org.jetbrains.jet.cli;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.intellij.core.JavaCoreEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Processor;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithTextRange;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
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
        @Argument(value = "src", description = "source file or directory", required = true)
        public String src;
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

        VirtualFile vFile = environment.getLocalFileSystem().findFileByPath(arguments.src);
        if (vFile == null) {
            System.out.print("File/directory not found: " + arguments.src);
            return;
        }

        Project project = environment.getProject();
        GenerationState generationState = new GenerationState(project, false);
        List<JetNamespace> namespaces = Lists.newArrayList();
        String mainClass = null;
        if(vFile.isDirectory())  {
            File dir = new File(vFile.getPath());
            addFiles(environment, project, namespaces, dir);
        }
        else {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
            if (psiFile instanceof JetFile) {
                final JetNamespace namespace = ((JetFile) psiFile).getRootNamespace();
                if (JetMainDetector.hasMain(namespace.getDeclarations())) {
                    mainClass = namespace.getFQName() + ".namespace";
                }
                namespaces.add(namespace);
            }
            else {
                System.out.print("Not a Kotlin file: " + vFile.getPath());
                return;
            }
        }

        BindingContext bindingContext = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS).analyzeNamespaces(project, namespaces, JetControlFlowDataTraceFactory.EMPTY);

        ErrorCollector errorCollector = new ErrorCollector(bindingContext);
        errorCollector.report();

        if (!errorCollector.hasErrors) {
            generationState.compileCorrectNamespaces(bindingContext, namespaces);

            final ClassFileFactory factory = generationState.getFactory();
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

    private static void writeRuntimeToJar(final JarOutputStream stream) throws IOException {
        URL url = KotlinCompiler.class.getClassLoader().getResource("jet/JetObject.class");
        if (url == null) {
            System.out.println("Couldn't find runtime library");
            return;
        }
        final String protocol = url.getProtocol();
        final String path = url.getPath();
        if (protocol.equals("file")) {   // unpacked runtime
            final File stdlibDir = new File(path).getParentFile().getParentFile();
            FileUtil.processFilesRecursively(stdlibDir, new Processor<File>() {
                @Override
                public boolean process(File file) {
                    if (file.isDirectory()) return true;
                    final String relativePath = FileUtil.getRelativePath(stdlibDir, file);
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
        else if (protocol.equals("jar")) {
            File jar = new File(path.substring(path.indexOf(":") + 1, path.indexOf("!/")));
            JarInputStream jis = new JarInputStream(new FileInputStream(jar));
            try {
                while (true) {
                    JarEntry e = jis.getNextJarEntry();
                    if (e == null) {
                        break;
                    }
                    stream.putNextEntry(e);
                    FileUtil.copy(jis, stream);
                }
            } finally {
                jis.close();
            }
        }
        else {
            System.out.println("Couldn't copy runtime library from " + url + ", protocol " + protocol);
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

    private static class ErrorCollector {
        Multimap<PsiFile,DiagnosticWithTextRange> maps = LinkedHashMultimap.<PsiFile, DiagnosticWithTextRange>create();

        boolean hasErrors;

        public ErrorCollector(BindingContext bindingContext) {
            for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
                report(diagnostic);
            }
        }

        private void report(Diagnostic diagnostic) {
            hasErrors |= diagnostic.getSeverity() == Severity.ERROR;
            if(diagnostic instanceof DiagnosticWithTextRange) {
                DiagnosticWithTextRange diagnosticWithTextRange = (DiagnosticWithTextRange) diagnostic;
                maps.put(diagnosticWithTextRange.getPsiFile(), diagnosticWithTextRange);
            }
            else {
                System.out.println(diagnostic.getSeverity().toString() + ": " + diagnostic.getMessage());
            }
        }

        void report() {
            if(!maps.isEmpty()) {
                for (PsiFile psiFile : maps.keySet()) {
                    System.out.println(psiFile.getVirtualFile().getPath());
                    Collection<DiagnosticWithTextRange> diagnosticWithTextRanges = maps.get(psiFile);
                    for (DiagnosticWithTextRange diagnosticWithTextRange : diagnosticWithTextRanges) {
                        String position = DiagnosticUtils.formatPosition(diagnosticWithTextRange);
                        System.out.println("\t" + diagnosticWithTextRange.getSeverity().toString() + ": " + position + " " + diagnosticWithTextRange.getMessage());
                    }
                }
            }
        }

    }

    private static void addFiles(JavaCoreEnvironment environment, Project project, List<JetNamespace> namespaces, File dir) {
        for(File file : dir.listFiles()) {
            if(!file.isDirectory()) {
                VirtualFile virtualFile = environment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile instanceof JetFile) {
                    namespaces.add(((JetFile) psiFile).getRootNamespace());
                }
            }
            else {
                addFiles(environment, project, namespaces, file);
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
