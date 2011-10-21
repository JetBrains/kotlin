package org.jetbrains.jet.cli;

import com.google.common.collect.Lists;
import com.intellij.core.JavaCoreEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
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
import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class KotlinCompiler {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        if (args.length < 1) {
            System.out.println("Usage: KotlinCompiler <filename> or <dirname>");
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
        environment.registerParserDefinition(new JetParserDefinition());

        VirtualFile vFile = environment.getLocalFileSystem().findFileByPath(args [0]);
        if (vFile == null) {
            System.out.print("File/directory not found: " + args[0]);
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

        boolean errors = false;
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            switch (diagnostic.getSeverity()) {
                case ERROR:
                    errors = true;
                    report(diagnostic);
                    break;
                case INFO:
                    report(diagnostic);                            
                    break;
                case WARNING:
                    report(diagnostic);                            
                    break;
            }
        }

        if (!errors) {
            generationState.compileCorrectNamespaces(bindingContext, namespaces);

            final ClassFileFactory factory = generationState.getFactory();
            List<String> files = factory.files();
            for (String file : files) {
                File target = new File(vFile.getParent().getPath(), file);
                try {
                    FileUtil.writeToFile(target, factory.asBytes(file));
                    System.out.println("Generated classfile: " + target);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
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
                    System.out.println(file.getAbsolutePath());
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

    private static void report(Diagnostic diagnostic) {
        System.out.println(diagnostic.getMessage());
    }

    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }
        return null;
    }
}
