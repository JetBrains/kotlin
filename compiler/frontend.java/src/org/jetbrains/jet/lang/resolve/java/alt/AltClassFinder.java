/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.plugin.compiler.PathUtil;

import java.util.List;

public class AltClassFinder {
    private final PsiManager psiManager;
    private final List<VirtualFile> roots;
    

    public AltClassFinder(Project project) {
        psiManager = PsiManager.getInstance(project);
        this.roots = PathUtil.getAltHeadersRoots();
    }

    public PsiClass findClass(@NotNull FqName qualifiedName) {
        for (final VirtualFile classRoot : roots) {
            PsiClass answer = CoreJavaFileManager.findClassInClasspathRoot(qualifiedName.getFqName(), classRoot, psiManager);
            if (answer != null) return answer;
        }

        return null;
    }
}
