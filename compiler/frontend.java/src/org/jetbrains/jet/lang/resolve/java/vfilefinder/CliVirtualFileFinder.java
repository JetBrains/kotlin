package org.jetbrains.jet.lang.resolve.java.vfilefinder;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.jvm.compiler.ClassPath;
import org.jetbrains.jet.lang.resolve.java.resolver.KotlinClassFileHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class CliVirtualFileFinder implements VirtualFileFinder {

    @NotNull
    private final ClassPath classPath;

    public CliVirtualFileFinder(@NotNull ClassPath path) {
        classPath = path;
    }


    @Nullable
    @Override
    public VirtualFile find(@NotNull FqName className, @NotNull GlobalSearchScope scope) {
        //TODO: use scope
        return find(className);
    }

    @Nullable
    @Override
    public VirtualFile find(@NotNull FqName className) {
        for (VirtualFile root : classPath) {
            VirtualFile fileInRoot = findFileInRoot(className.asString(), root);
            if (fileInRoot != null) {
                return fileInRoot;
            }
        }
        return null;
    }

    //NOTE: copied with some changes from CoreJavaFileManager
    @Nullable
    private static VirtualFile findFileInRoot(@NotNull String qName, @NotNull VirtualFile root) {
        String pathRest = qName;
        VirtualFile cur = root;

        while (true) {
            int dot = pathRest.indexOf('.');
            if (dot < 0) break;

            String pathComponent = pathRest.substring(0, dot);
            VirtualFile child = cur.findChild(pathComponent);

            if (child == null) break;
            pathRest = pathRest.substring(dot + 1);
            cur = child;
        }

        String className = pathRest.replace('.', '$');
        VirtualFile vFile = cur.findChild(className + ".class");
        if (vFile != null) {
            if (!vFile.isValid()) {
                //TODO: log
                return null;
            }
            //NOTE: currently we use VirtualFileFinder to find Kotlin binaries only
            if (KotlinClassFileHeader.readKotlinHeaderFromClassFile(vFile).getType() != KotlinClassFileHeader.HeaderType.NONE) {
                return vFile;
            }
        }
        return null;
    }
}
