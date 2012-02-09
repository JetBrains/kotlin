/*
 * @author max
 */
package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PathUtil {
    private PathUtil() {}

    public static File getDefaultCompilerPath() {
        File plugin_jar_path = new File(getJarPathForClass(PathUtil.class));

        if (!plugin_jar_path.exists()) return null;

        if (plugin_jar_path.getName().equals("kotlin-plugin.jar")) {
            File lib = plugin_jar_path.getParentFile();
            File pluginHome = lib.getParentFile();

            File answer = new File(pluginHome, "kotlinc");

            return answer.exists() ? answer : null;
        }

        if (plugin_jar_path.getName().equals("kotlin-compiler.jar")) {
            File lib = plugin_jar_path.getParentFile();
            File answer = lib.getParentFile();
            return answer.exists() ? answer : null;
        }
        
        File current = new File("").getAbsoluteFile(); // CWD

        do {
            File atDevHome = new File(current, "dist/kotlinc");
            if (atDevHome.exists()) return atDevHome;
            current = current.getParentFile();
        } while (current != null);

        return null;
    }

    @Nullable
    public static File getDefaultRuntimePath() {
        File compilerPath = getDefaultCompilerPath();
        if (compilerPath == null) return null;

        File answer = new File(compilerPath, "lib/kotlin-runtime.jar");

        return answer.exists() ? answer : null;
    }

    public static File getAltHeadersPath() {
        File compilerPath = getDefaultCompilerPath();
        if (compilerPath == null) return null;

        File answer = new File(compilerPath, "lib/alt");
        return answer.exists() ? answer : null;
    }

    @NotNull
    public static String getJarPathForClass(@NotNull Class aClass) {
        String resourceRoot = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return new File(resourceRoot).getAbsoluteFile().getAbsolutePath();
    }

    public static List<VirtualFile> getAltHeadersRoots() {
        List<VirtualFile> roots = new ArrayList<VirtualFile>();

        File alts = getAltHeadersPath();

        if (alts != null) {
            for (File root : alts.listFiles()) {
                VirtualFile jarRoot = VirtualFileManager.getInstance().findFileByUrl("jar://" + root.getPath() + "!/");
                roots.add(jarRoot);
            }
        }
        return roots;
    }
}
