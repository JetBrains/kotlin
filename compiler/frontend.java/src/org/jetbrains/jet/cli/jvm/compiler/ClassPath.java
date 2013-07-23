package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ClassPath implements Iterable<VirtualFile> {

    @NotNull
    private final List<VirtualFile> roots = new ArrayList<VirtualFile>();

    @Override
    public Iterator<VirtualFile> iterator() {
        return roots.iterator();
    }

    public void add(@NotNull VirtualFile root) {
        roots.add(root);
    }
}
