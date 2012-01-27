/*
 * @author max
 */
package org.jetbrains.jet.lang.resolve.java.alt;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.compiler.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AltClassFinder {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.lang.resolve.java.alt.AltClassFinder");

    private final PsiManager psiManager;
    private final List<VirtualFile> roots = new ArrayList<VirtualFile>();

    public AltClassFinder(Project project) {
        psiManager = PsiManager.getInstance(project);

        File alts = PathUtil.getAltHeadersPath();

        if (alts != null) {
            for (File root : alts.listFiles()) {
                VirtualFile jarRoot = VirtualFileManager.getInstance().findFileByUrl("jar://" + root.getPath() + "!/");
                roots.add(jarRoot);
            }
        }
    }

    public PsiClass findClass(@NotNull String qualifiedName) {
        for (final VirtualFile classRoot : roots) {
            final VirtualFile classFile = classRoot.findFileByRelativePath(qualifiedName.replace('.', '/') + ".class");
            if (classFile != null) {
                if (!classFile.isValid()) {
                    LOG.error("Invalid child of valid parent: " + classFile.getPath() + "; " + classRoot.isValid() + " path=" + classRoot.getPath());
                    return null;
                }
                final PsiFile file = psiManager.findFile(classFile);
                if (file instanceof PsiClassOwner) {
                    final PsiClass[] classes = ((PsiClassOwner) file).getClasses();
                    if (classes.length == 1) {
                        return classes[0];
                    }
                }
            }
        }

        return null;
    }
}
