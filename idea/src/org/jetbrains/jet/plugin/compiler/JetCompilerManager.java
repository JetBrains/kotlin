package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.Collections;

/**
 * @author yole
 */
public class JetCompilerManager implements ProjectComponent {
    public JetCompilerManager(CompilerManager manager) {
        manager.addTranslatingCompiler(new JetCompiler(),
                Collections.<FileType>singleton(JetFileType.INSTANCE),
                Collections.singleton(StdFileTypes.CLASS));
        manager.addCompilableFileType(JetFileType.INSTANCE);
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return JetCompilerManager.class.getCanonicalName();
    }
}
