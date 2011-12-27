package org.jetbrains.jet.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.Processor;
import jet.ExtensionFunction0;
import jet.modules.IModuleBuilder;
import jet.modules.IModuleSetBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.plugin.JetMainDetector;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.*;

/**
 * The environment for compiling a bunch of source files or
 *
 * @author yole
 * @author alex.tkachman
 */
public class CoreCompileEnvironment extends AbstractCompileEnvironment {
    private final Disposable myRootDisposable;
    private final VirtualFileSystem localFileSystem;
    private VirtualFileSystem jarFileSystem;
    private JetCoreEnvironment myEnvironment;

    public CoreCompileEnvironment() {
        myRootDisposable = new Disposable() {
            @Override
            public void dispose() {
            }
        };
        myEnvironment = new JetCoreEnvironment(myRootDisposable);
        localFileSystem = myEnvironment.getLocalFileSystem();
        jarFileSystem = myEnvironment.getJarFileSystem();
        project = myEnvironment.getProject();
    }

    public CoreCompileEnvironment(JetCoreEnvironment myEnvironment) {
        this.myRootDisposable = null;
        this.myEnvironment = myEnvironment;
        localFileSystem = myEnvironment.getLocalFileSystem();
        jarFileSystem = myEnvironment.getJarFileSystem();
        project = myEnvironment.getProject();
    }

    public void dispose() {
        if(myRootDisposable != null)
            Disposer.dispose(myRootDisposable);
    }

    @Override
    public void addToClasspath(File file) {
        myEnvironment.addToClasspath(file);
    }

    @Override
    public VirtualFileSystem getLocalFileSystem() {
        return localFileSystem;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public VirtualFileSystem getJarFileSystem() {
        return jarFileSystem;
    }
}
