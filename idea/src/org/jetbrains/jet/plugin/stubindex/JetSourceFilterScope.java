package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nikolay Krasko
 */
public class JetSourceFilterScope extends DelegatingGlobalSearchScope {
    private final ProjectFileIndex myIndex;

    public JetSourceFilterScope(@NotNull final GlobalSearchScope delegate) {
        super(delegate);
        myIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
    }

    @Override
    public boolean contains(final VirtualFile file) {
        if (!super.contains(file)) {
            return false;
        }

        if (StdFileTypes.CLASS == file.getFileType()) {
            return myIndex.isInLibraryClasses(file);
        }

        return myIndex.isInSourceContent(file);
    }
}
