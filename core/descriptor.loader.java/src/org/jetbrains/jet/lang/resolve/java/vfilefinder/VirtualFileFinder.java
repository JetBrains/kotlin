package org.jetbrains.jet.lang.resolve.java.vfilefinder;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;

public interface VirtualFileFinder {

    @Nullable
    VirtualFile find(@NotNull FqName className, @NotNull GlobalSearchScope scope);
    //NOTE: uses all scope by default
    //TODO: should be removed, scope should always be passed
    @Nullable
    VirtualFile find(@NotNull FqName className);
}
