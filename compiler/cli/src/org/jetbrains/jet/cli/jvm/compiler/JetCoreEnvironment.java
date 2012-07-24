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

import com.intellij.core.CoreJavaFileManager;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JavaElementFinder;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.parsing.JetParser;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.java.extAnnotations.CoreAnnotationsProvider;
import org.jetbrains.jet.lang.resolve.java.extAnnotations.ExternalAnnotationsProvider;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class JetCoreEnvironment extends JavaCoreApplicationEnvironment {
    @NotNull
    public static JetCoreEnvironment createCoreEnvironmentForJS(Disposable disposable, @NotNull CompilerConfiguration configuration) {
        return new JetCoreEnvironment(disposable, configuration);
    }

    @NotNull
    public static JetCoreEnvironment createCoreEnvironmentForJVM(Disposable disposable, @NotNull CompilerConfiguration configuration) {
        return new JetCoreEnvironment(disposable, configuration);
    }

    private final List<JetFile> sourceFiles = new ArrayList<JetFile>();
    private final CoreAnnotationsProvider annotationsProvider;

    private final CompilerConfiguration configuration;

    private boolean initialized = false;

    private final JavaCoreProjectEnvironment projectEnvironment;

    public JetCoreEnvironment(Disposable parentDisposable, @NotNull CompilerConfiguration configuration) {
        super(parentDisposable);
        this.configuration = configuration;

        registerFileType(JetFileType.INSTANCE, "kt");
        registerFileType(JetFileType.INSTANCE, "kts");
        registerFileType(JetFileType.INSTANCE, "ktm");
        registerFileType(JetFileType.INSTANCE, JetParser.KTSCRIPT_FILE_SUFFIX); // should be renamed to kts
        registerFileType(JetFileType.INSTANCE, "jet");
        registerParserDefinition(new JavaParserDefinition());
        registerParserDefinition(new JetParserDefinition());

        projectEnvironment = new JavaCoreProjectEnvironment(parentDisposable, this);


        MockProject project = projectEnvironment.getProject();
        project.registerService(JetFilesProvider.class, new CliJetFilesProvider(this));
        project.registerService(CoreJavaFileManager.class, (CoreJavaFileManager) ServiceManager.getService(project, JavaFileManager.class));
        Extensions.getArea(project)
                .getExtensionPoint(PsiElementFinder.EP_NAME)
                .registerExtension(new JavaElementFinder(project));

        annotationsProvider = new CoreAnnotationsProvider();
        project.registerService(ExternalAnnotationsProvider.class, annotationsProvider);

        for (File path : configuration.getList(JVMConfigurationKeys.CLASSPATH_KEY)) {
            addToClasspath(path);
        }
        for (File path : configuration.getList(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY)) {
            addExternalAnnotationsRoot(PathUtil.jarFileOrDirectoryToVirtualFile(path));
        }
        for (String path : configuration.getList(CommonConfigurationKeys.SOURCE_ROOTS_KEY)) {
            addSources(path);
        }

        JetStandardLibrary.initialize(project);
        initialized = true;
    }

    public CompilerConfiguration getConfiguration() {
        return configuration;
    }

    public MockApplication getApplication() {
        return myApplication;
    }

    @NotNull
    public Project getProject() {
        return projectEnvironment.getProject();
    }

    private void addExternalAnnotationsRoot(VirtualFile root) {
        annotationsProvider.addExternalAnnotationsRoot(root);
    }

    private void addSources(File file) {
        if (file.isDirectory()) {
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
                if (psiFile instanceof JetFile) {
                    sourceFiles.add((JetFile) psiFile);
                }
            }
        }
    }

    private void addSources(String path) {
        if (path == null) {
            return;
        }

        VirtualFile vFile = getLocalFileSystem().findFileByPath(path);
        if (vFile == null) {
            throw new CompileEnvironmentException("File/directory not found: " + path);
        }
        if (!vFile.isDirectory() && vFile.getFileType() != JetFileType.INSTANCE) {
            throw new CompileEnvironmentException("Not a Kotlin file: " + path);
        }

        addSources(new File(path));
    }

    public void addToClasspath(File path) {
        if (initialized) {
            throw new IllegalStateException("Cannot add class path when JetCoreEnvironment is already initialized");
        }
        if (path.isFile()) {
            projectEnvironment.addJarToClassPath(path);
        }
        else {
            final VirtualFile root = getLocalFileSystem().findFileByPath(path.getAbsolutePath());
            if (root == null) {
                throw new IllegalArgumentException("trying to add non-existing file to classpath: " + path);
            }
            projectEnvironment.addSourcesToClasspath(root);
        }
    }

    public List<JetFile> getSourceFiles() {
        return sourceFiles;
    }
}
