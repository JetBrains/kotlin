/*
 * @author max
 */
package org.jetbrains.jet.lang.resolve.java.alt;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.compiler.PathUtil;

import java.util.List;

public class AltClassFinder {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.lang.resolve.java.alt.AltClassFinder");

    private final PsiManager psiManager;
    private final List<VirtualFile> roots;
    

    public AltClassFinder(Project project) {
        psiManager = PsiManager.getInstance(project);
        this.roots = PathUtil.getAltHeadersRoots();
    }

    public PsiClass findClass(@NotNull String qualifiedName) {
        for (final VirtualFile classRoot : roots) {
            
            String pathRest = qualifiedName;
            VirtualFile cur = classRoot;

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
            int bucks = className.indexOf('$');

            String rootClassName;
            if (bucks < 0) {
                rootClassName = className;
            }
            else {
                rootClassName = className.substring(0, bucks);
                className = className.substring(bucks + 1);
            }

            final VirtualFile classFile = cur.findChild(rootClassName + ".class");
            if (classFile != null) {
                if (!classFile.isValid()) {
                    LOG.error("Invalid child of valid parent: " + classFile.getPath() + "; " + classRoot.isValid() + " path=" + classRoot.getPath());
                    return null;
                }
                
                final PsiFile file = psiManager.findFile(classFile);
                if (file instanceof PsiClassOwner) {
                    final PsiClass[] classes = ((PsiClassOwner) file).getClasses();
                    if (classes.length == 1) {
                        PsiClass curClass = classes[0];

                        if (bucks > 0) {
                            while (true) {
                                int b = className.indexOf("$");

                                String component = b < 0 ? className : className.substring(0, b);
                                PsiClass inner = curClass.findInnerClassByName(component, false);

                                if (inner == null) return null;
                                curClass = inner;
                                className = className.substring(b + 1);
                                if (b < 0) break;
                            }
                        }


                        return curClass;
                    }
                }
            }
        }

        return null;
    }
}
