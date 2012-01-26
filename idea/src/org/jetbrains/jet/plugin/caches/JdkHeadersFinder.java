/*
 * @author max
 */
package org.jetbrains.jet.plugin.caches;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.NonClasspathClassFinder;
import org.jetbrains.jet.plugin.compiler.CompilerUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class JdkHeadersFinder extends NonClasspathClassFinder {
    public JdkHeadersFinder(Project project) {
        super(project);
    }

    @Override
    protected List<VirtualFile> calcClassRoots() {
        File jdk_headers = CompilerUtil.getJdkHeadersPath();
        if (jdk_headers == null) return Collections.emptyList();

        VirtualFile jarVfs = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(jdk_headers);
        if (jarVfs == null) return Collections.emptyList();
        jarVfs.refresh(true, false);

        return Collections.singletonList(JarFileSystem.getInstance().getJarRootForLocalFile(jarVfs));
    }
}
