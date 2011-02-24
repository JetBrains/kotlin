package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author abreslav
 */
public class FileContentsResolver {

    public static final FileContentsResolver INSTANCE = new FileContentsResolver();

    private FileContentsResolver() {}

    @NotNull
    public JetScope resolveFileContents(@NotNull JetScope scope, @NotNull JetFile file) {
        return new LazyScope(scope, file.getRootNamespace().getDeclarations());
    }

}
