package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.jet.compiler.AbstractCompileEnvironment;

import java.io.File;

/**
 * @author alex.tkachman
 */
public class PluginCompilerEnvironment extends AbstractCompileEnvironment {
    public PluginCompilerEnvironment(Project project) {
        this.project = project;
    }

    @Override
    protected void addToClasspath(File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFileSystem getLocalFileSystem() {
        return LocalFileSystem.getInstance();
    }

    @Override
    public VirtualFileSystem getJarFileSystem() {
        return JarFileSystem.getInstance();
    }
}
