/*
 * @author max
 */
package org.jetbrains.jet.plugin;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class JetFileFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(JetFileType.INSTANCE, "jet;jetl;jets");
    }
}
