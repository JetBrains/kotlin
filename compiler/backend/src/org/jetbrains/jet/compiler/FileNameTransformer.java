package org.jetbrains.jet.compiler;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface FileNameTransformer {
    FileNameTransformer IDENTITY = new FileNameTransformer() {
        @NotNull
        @Override
        public String transformFileName(@NotNull String fileName) {
            return fileName;
        }
    };

    @NotNull
    String transformFileName(@NotNull String fileName);
}
