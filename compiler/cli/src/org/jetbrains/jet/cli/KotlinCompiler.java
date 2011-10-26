package org.jetbrains.jet.cli;

import com.google.common.collect.*;
import com.intellij.core.JavaCoreEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithTextRange;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class KotlinCompiler {
    public static class Arguments {
        @Argument(value = "output", description = "output directory")
        public String outputDir;
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
            System.out.println("Usage: KotlinCompiler -output <outputDir> -src <filename or dirname>");
            t.printStackTrace();
            return;
        }

        Disposable root = new Disposable() {
            @Override
            public void dispose() {
            }
        };
        JavaCoreEnvironment environment = new JavaCoreEnvironment(root);

        File rtJar = initJdk();
        if (rtJar == null) return;

        environment.addToClasspath(rtJar);

        environment.registerFileType(JetFileType.INSTANCE, "kt");
        environment.registerFileType(JetFileType.INSTANCE, "jet");
        environment.registerParserDefinition(new JetParserDefinition());

        VirtualFile vFile = environment.getLocalFileSystem().findFileByPath(arguments.src);
        if (vFile == null) {
            System.out.print("File/directory not found: " + arguments.src);
            return;
        }

        Project project = environment.getProject();
        GenerationState generationState = new GenerationState(project, false);
        List<JetNamespace> namespaces = Lists.newArrayList();
        if(vFile.isDirectory())  {
            File dir = new File(vFile.getPath());
            addFiles(environment, project, namespaces, dir);
        }
        else {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
            if (psiFile instanceof JetFile) {
                namespaces.add(((JetFile) psiFile).getRootNamespace());
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
            if(arguments.outputDir == null) {
                System.out.println("Output directory is not specified - no files will be saved to the disk");
            }
            else {
                List<String> files = factory.files();
                for (String file : files) {
                    File target = new File(arguments.outputDir, file);
                    try {
                        FileUtil.writeToFile(target, factory.asBytes(file));
                        System.out.println("Generated classfile: " + target);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
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
