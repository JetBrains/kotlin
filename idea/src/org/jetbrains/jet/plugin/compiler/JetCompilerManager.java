package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.Collections;

/**
 * @author yole
 */
public class JetCompilerManager {
    public JetCompilerManager(CompilerManager manager) {
        manager.addTranslatingCompiler(new JetCompiler(),
                Collections.<FileType>singleton(JetFileType.INSTANCE),
                Collections.singleton(StdFileTypes.CLASS));
        manager.addCompilableFileType(JetFileType.INSTANCE);
    }
}
