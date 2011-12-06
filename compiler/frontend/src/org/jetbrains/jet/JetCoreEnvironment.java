package org.jetbrains.jet;

import com.intellij.core.JavaCoreEnvironment;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.openapi.Disposable;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.plugin.JetFileType;

/**
 * @author yole
 */
public class JetCoreEnvironment extends JavaCoreEnvironment {
    public JetCoreEnvironment(Disposable parentDisposable) {
        super(parentDisposable);
        registerFileType(JetFileType.INSTANCE, "kt");
        registerFileType(JetFileType.INSTANCE, "kts");
        registerFileType(JetFileType.INSTANCE, "ktm");
        registerFileType(JetFileType.INSTANCE, "jet");
        registerParserDefinition(new JavaParserDefinition());
        registerParserDefinition(new JetParserDefinition());
    }
}
