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

import com.intellij.core.JavaCoreEnvironment;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JavaElementFinder;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class JetCoreEnvironment extends JavaCoreEnvironment {
    private final List<JetFile> sourceFiles = new ArrayList<JetFile>();

    public JetCoreEnvironment(Disposable parentDisposable, @NotNull CompilerDependencies compilerDependencies) {
        super(parentDisposable);
        registerFileType(JetFileType.INSTANCE, "kt");
        registerFileType(JetFileType.INSTANCE, "kts");
        registerFileType(JetFileType.INSTANCE, "ktm");
        registerFileType(JetFileType.INSTANCE, "jet");
        registerParserDefinition(new JavaParserDefinition());
        registerParserDefinition(new JetParserDefinition());


        myProject.registerService(JetFilesProvider.class, new CliJetFilesProvider(this));
        Extensions.getArea(myProject)
                .getExtensionPoint(PsiElementFinder.EP_NAME)
                .registerExtension(new JavaElementFinder(myProject));

        CompilerSpecialMode compilerSpecialMode = compilerDependencies.getCompilerSpecialMode();

        addToClasspath(compilerDependencies.getJdkJar());

        if (compilerSpecialMode.includeJdkHeaders()) {
            for (VirtualFile root : compilerDependencies.getJdkHeaderRoots()) {
                addLibraryRoot(root);
            }
        }
        if (compilerSpecialMode.includeKotlinRuntime()) {
            for (VirtualFile root : compilerDependencies.getRuntimeRoots()) {
                addLibraryRoot(root);
            }
        }

        JetStandardLibrary.initialize(getProject());
    }

    public MockApplication getApplication() {
        return myApplication;
    }

    private void addSources(File file) {
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    addSources(child);
                }
            }
        }
        else {
            VirtualFile fileByPath = getLocalFileSystem().findFileByPath(file.getAbsolutePath());
            if (fileByPath != null) {
                PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(fileByPath);
                if(psiFile instanceof JetFile) {
                    sourceFiles.add((JetFile)psiFile);
                }
            }
        }
    }

    public void addSources(VirtualFile vFile) {
        if  (vFile.isDirectory())  {
            for (VirtualFile virtualFile : vFile.getChildren()) {
                addSources(virtualFile);
            }
        }
        else {
            if (vFile.getFileType() == JetFileType.INSTANCE) {
                PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(vFile);
                if (psiFile instanceof JetFile) {
                    sourceFiles.add((JetFile)psiFile);
                }
            }
        }
    }

    public void addSources(String path) {
        if(path == null)
            return;

        VirtualFile vFile = getLocalFileSystem().findFileByPath(path);
        if (vFile == null) {
            throw new CompileEnvironmentException("File/directory not found: " + path);
        }
        if (!vFile.isDirectory() && vFile.getFileType() != JetFileType.INSTANCE) {
            throw new CompileEnvironmentException("Not a Kotlin file: " + path);
        }

        addSources(new File(path));
    }

    public List<JetFile> getSourceFiles() {
        return sourceFiles;
    }

    public void addToClasspathFromClassLoader(ClassLoader loader) {
        ClassLoader parent = loader.getParent();
        if(parent != null)
            addToClasspathFromClassLoader(parent);

        if(loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) loader).getURLs()) {
                File file = new File(url.getPath());
                if(file.exists() && (!file.isFile() || file.getPath().endsWith(".jar")))
                    addToClasspath(file);
            }
        }
    }
}
