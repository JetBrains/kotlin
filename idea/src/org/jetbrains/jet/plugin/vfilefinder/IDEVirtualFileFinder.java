package org.jetbrains.jet.plugin.vfilefinder;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.vfilefinder.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public final class IDEVirtualFileFinder implements VirtualFileFinder {

    private static final Logger LOG = Logger.getInstance(IDEVirtualFileFinder.class);

    @NotNull private final Project project;

    public IDEVirtualFileFinder(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public VirtualFile find(@NotNull FqName className, @NotNull GlobalSearchScope scope) {
        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(KotlinClassFileIndex.KEY, className, scope);
        if (files.isEmpty()) {
            return null;
        }
        if (files.size() > 1) {
            LOG.warn("There are " + files.size() + " classes with same fqName: " + className + " found.");
        }
        return files.iterator().next();
    }

    @Nullable
    @Override
    public VirtualFile find(@NotNull FqName className) {
        return find(className, GlobalSearchScope.allScope(project));
    }
}
