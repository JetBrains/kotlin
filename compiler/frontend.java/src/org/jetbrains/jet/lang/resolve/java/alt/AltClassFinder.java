/*
 * @author max
 */
package org.jetbrains.jet.lang.resolve.java.alt;

import com.intellij.core.CoreJavaFileManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.compiler.PathUtil;

import java.util.List;

public class AltClassFinder {
    private final PsiManager psiManager;
    private final List<VirtualFile> roots;
    

    public AltClassFinder(Project project) {
        psiManager = PsiManager.getInstance(project);
        this.roots = PathUtil.getAltHeadersRoots();
    }

    public PsiClass findClass(@NotNull String qualifiedName) {
        for (final VirtualFile classRoot : roots) {
            PsiClass answer = CoreJavaFileManager.findClassInClasspathRoot(qualifiedName, classRoot, psiManager);
            if (answer != null) return answer;
        }

        return null;
    }
}
